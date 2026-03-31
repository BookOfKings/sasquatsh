import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// ── Utility: compute next occurrence date ──────────────────────────────
function computeNextOccurrence(
  frequency: string,
  dayOfWeek: number,
  monthlyWeek: number | null,
  afterDate: Date
): string {
  if (frequency === 'weekly') {
    // Next matching dayOfWeek on or after afterDate
    const d = new Date(afterDate)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + (diff === 0 ? 0 : diff))
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'biweekly') {
    // Next matching dayOfWeek at least 14 days after afterDate
    const d = new Date(afterDate)
    d.setUTCDate(d.getUTCDate() + 14)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + diff)
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'monthly') {
    // Nth dayOfWeek of next month (monthlyWeek=1 means 1st, -1 means last)
    const d = new Date(afterDate)
    let year = d.getUTCFullYear()
    let month = d.getUTCMonth() + 1
    if (month > 11) {
      month = 0
      year++
    }

    if (monthlyWeek === -1) {
      // Last occurrence of dayOfWeek in the month
      const lastDay = new Date(Date.UTC(year, month + 1, 0))
      const diff = (lastDay.getUTCDay() - dayOfWeek + 7) % 7
      lastDay.setUTCDate(lastDay.getUTCDate() - diff)
      return lastDay.toISOString().slice(0, 10)
    }

    // Nth occurrence (1-4)
    const week = monthlyWeek ?? 1
    const firstOfMonth = new Date(Date.UTC(year, month, 1))
    const firstDayDiff = (dayOfWeek - firstOfMonth.getUTCDay() + 7) % 7
    const nthDate = 1 + firstDayDiff + (week - 1) * 7
    const result = new Date(Date.UTC(year, month, nthDate))
    return result.toISOString().slice(0, 10)
  }

  // Fallback: weekly
  return computeNextOccurrence('weekly', dayOfWeek, null, afterDate)
}

// ── Transform: snake_case DB row → camelCase response ──────────────────
function transformRecurringGame(row: Record<string, unknown>) {
  return {
    id: row.id,
    groupId: row.group_id,
    title: row.title,
    description: row.description,
    frequency: row.frequency,
    dayOfWeek: row.day_of_week,
    monthlyWeek: row.monthly_week,
    startTime: row.start_time,
    durationMinutes: row.duration_minutes,
    maxPlayers: row.max_players,
    hostIsPlaying: row.host_is_playing ?? true,
    locationDetails: row.location_details,
    eventLocationId: row.event_location_id,
    addressLine1: row.address_line1,
    city: row.city,
    state: row.state,
    postalCode: row.postal_code,
    timezone: row.timezone,
    gameSystem: row.game_system ?? 'board_game',
    gameTitle: row.game_title,
    isPublic: row.is_public,
    isActive: row.is_active,
    nextOccurrenceDate: row.next_occurrence_date,
    lastGeneratedDate: row.last_generated_date,
    hostUserId: row.host_user_id,
    createdByUserId: row.created_by_user_id,
    createdAt: row.created_at,
  }
}

// ── Tier limits for recurring games per group ──────────────────────────
const TIER_LIMITS: Record<string, number> = {
  free: 0,
  basic: 1,
  pro: Infinity,
  premium: Infinity,
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // Get Firebase token from custom header
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, subscription_tier, subscription_override_tier, is_admin')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)

  // ── GET - List recurring games for a group ───────────────────────────
  if (req.method === 'GET') {
    const groupId = url.searchParams.get('groupId')
    if (!groupId) {
      return errorResponse('groupId is required', 400)
    }

    // Verify user is a member of the group
    const { data: membership, error: memberErr } = await supabase
      .from('group_memberships')
      .select('id')
      .eq('user_id', user.id)
      .eq('group_id', groupId)
      .maybeSingle()

    if (memberErr) {
      return errorResponse('Failed to verify group membership', 500)
    }

    // Allow site admins to view even if not a member
    if (!membership && !user.is_admin) {
      return errorResponse('You must be a member of this group', 403)
    }

    const { data, error } = await supabase
      .from('recurring_games')
      .select('*')
      .eq('group_id', groupId)
      .order('day_of_week', { ascending: true })
      .order('start_time', { ascending: true })

    if (error) {
      return errorResponse('Failed to fetch recurring games', 500)
    }

    return jsonResponse((data || []).map(transformRecurringGame))
  }

  // ── POST - Create a recurring game ───────────────────────────────────
  if (req.method === 'POST') {
    const body = await req.json()
    const { groupId, ...input } = body

    if (!groupId) {
      return errorResponse('groupId is required', 400)
    }
    if (!input.title) {
      return errorResponse('title is required', 400)
    }
    if (input.dayOfWeek === undefined || input.dayOfWeek === null) {
      return errorResponse('dayOfWeek is required', 400)
    }
    if (!input.startTime) {
      return errorResponse('startTime is required', 400)
    }

    // Verify user is admin/owner of the group
    const { data: membership, error: memberErr } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('user_id', user.id)
      .eq('group_id', groupId)
      .maybeSingle()

    if (memberErr) {
      return errorResponse('Failed to verify group membership', 500)
    }

    if (!membership || !['admin', 'owner'].includes(membership.role)) {
      if (!user.is_admin) {
        return errorResponse('Only group admins and owners can create recurring games', 403)
      }
    }

    // Tier check: look up group owner's effective tier
    const { data: ownerMembership } = await supabase
      .from('group_memberships')
      .select('user_id, user:users!user_id(subscription_override_tier, subscription_tier)')
      .eq('group_id', groupId)
      .eq('role', 'owner')
      .maybeSingle()

    const ownerUser = ownerMembership?.user as Record<string, unknown> | null
    const effectiveTier = (
      ownerUser?.subscription_override_tier ||
      ownerUser?.subscription_tier ||
      'free'
    ) as string

    const limit = TIER_LIMITS[effectiveTier] ?? 0

    if (limit !== Infinity) {
      // Count active recurring games for this group
      const { count, error: countErr } = await supabase
        .from('recurring_games')
        .select('id', { count: 'exact', head: true })
        .eq('group_id', groupId)
        .eq('is_active', true)

      if (countErr) {
        return errorResponse('Failed to check recurring game count', 500)
      }

      if ((count ?? 0) >= limit) {
        return errorResponse(
          limit === 0
            ? 'Recurring games require a Basic subscription or higher'
            : `Your plan allows a maximum of ${limit} active recurring game(s)`,
          403
        )
      }
    }

    // Compute next_occurrence_date
    const frequency = input.frequency || 'weekly'
    const nextOccurrenceDate = computeNextOccurrence(
      frequency,
      input.dayOfWeek,
      input.monthlyWeek ?? null,
      new Date()
    )

    const insertData: Record<string, unknown> = {
      group_id: groupId,
      title: input.title,
      description: input.description ?? null,
      frequency,
      day_of_week: input.dayOfWeek,
      monthly_week: input.monthlyWeek ?? null,
      start_time: input.startTime,
      duration_minutes: input.durationMinutes ?? 120,
      max_players: input.maxPlayers ?? 4,
      host_is_playing: input.hostIsPlaying ?? true,
      location_details: input.locationDetails ?? null,
      event_location_id: input.eventLocationId ?? null,
      address_line1: input.addressLine1 ?? null,
      city: input.city ?? null,
      state: input.state ?? null,
      postal_code: input.postalCode ?? null,
      timezone: input.timezone ?? 'America/New_York',
      game_system: input.gameSystem ?? 'board_game',
      game_title: input.gameTitle ?? null,
      is_public: input.isPublic ?? true,
      is_active: true,
      next_occurrence_date: nextOccurrenceDate,
      host_user_id: user.id,
      created_by_user_id: user.id,
    }

    const { data, error } = await supabase
      .from('recurring_games')
      .insert(insertData)
      .select('*')
      .single()

    if (error) {
      return errorResponse(`Failed to create recurring game: ${error.message}`, 500)
    }

    return jsonResponse(transformRecurringGame(data), 201)
  }

  // ── PUT - Update a recurring game ────────────────────────────────────
  if (req.method === 'PUT') {
    const id = url.searchParams.get('id')
    if (!id) {
      return errorResponse('id is required', 400)
    }

    // Fetch the existing record to check ownership
    const { data: existing, error: fetchErr } = await supabase
      .from('recurring_games')
      .select('*')
      .eq('id', id)
      .single()

    if (fetchErr || !existing) {
      return errorResponse('Recurring game not found', 404)
    }

    // Verify user is admin/owner of the game's group
    const { data: membership, error: memberErr } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('user_id', user.id)
      .eq('group_id', existing.group_id)
      .maybeSingle()

    if (memberErr) {
      return errorResponse('Failed to verify group membership', 500)
    }

    if (!membership || !['admin', 'owner'].includes(membership.role)) {
      if (!user.is_admin) {
        return errorResponse('Only group admins and owners can update recurring games', 403)
      }
    }

    const body = await req.json()

    // Build update object from allowed fields
    const updateData: Record<string, unknown> = {}
    const fieldMap: Record<string, string> = {
      title: 'title',
      description: 'description',
      frequency: 'frequency',
      dayOfWeek: 'day_of_week',
      monthlyWeek: 'monthly_week',
      startTime: 'start_time',
      durationMinutes: 'duration_minutes',
      maxPlayers: 'max_players',
      hostIsPlaying: 'host_is_playing',
      locationDetails: 'location_details',
      eventLocationId: 'event_location_id',
      addressLine1: 'address_line1',
      city: 'city',
      state: 'state',
      postalCode: 'postal_code',
      timezone: 'timezone',
      gameSystem: 'game_system',
      gameTitle: 'game_title',
      isPublic: 'is_public',
      isActive: 'is_active',
    }

    for (const [camel, snake] of Object.entries(fieldMap)) {
      if (body[camel] !== undefined) {
        updateData[snake] = body[camel]
      }
    }

    if (Object.keys(updateData).length === 0) {
      return errorResponse('No valid fields to update', 400)
    }

    // Recompute next_occurrence_date if schedule fields changed
    const scheduleChanged =
      updateData.frequency !== undefined ||
      updateData.day_of_week !== undefined ||
      updateData.monthly_week !== undefined

    if (scheduleChanged) {
      const freq = (updateData.frequency ?? existing.frequency) as string
      const dow = (updateData.day_of_week ?? existing.day_of_week) as number
      const mw = (updateData.monthly_week ?? existing.monthly_week) as number | null
      updateData.next_occurrence_date = computeNextOccurrence(freq, dow, mw, new Date())
    }

    const { data, error } = await supabase
      .from('recurring_games')
      .update(updateData)
      .eq('id', id)
      .select('*')
      .single()

    if (error) {
      return errorResponse(`Failed to update recurring game: ${error.message}`, 500)
    }

    return jsonResponse(transformRecurringGame(data))
  }

  // ── DELETE - Delete a recurring game ─────────────────────────────────
  if (req.method === 'DELETE') {
    const id = url.searchParams.get('id')
    if (!id) {
      return errorResponse('id is required', 400)
    }

    // Fetch the existing record to check ownership
    const { data: existing, error: fetchErr } = await supabase
      .from('recurring_games')
      .select('group_id')
      .eq('id', id)
      .single()

    if (fetchErr || !existing) {
      return errorResponse('Recurring game not found', 404)
    }

    // Verify user is admin/owner of the game's group
    const { data: membership, error: memberErr } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('user_id', user.id)
      .eq('group_id', existing.group_id)
      .maybeSingle()

    if (memberErr) {
      return errorResponse('Failed to verify group membership', 500)
    }

    if (!membership || !['admin', 'owner'].includes(membership.role)) {
      if (!user.is_admin) {
        return errorResponse('Only group admins and owners can delete recurring games', 403)
      }
    }

    // Optionally delete future events generated from this recurring game
    const deleteFutureEvents = url.searchParams.get('deleteFutureEvents') === 'true'
    if (deleteFutureEvents) {
      const today = new Date().toISOString().slice(0, 10)
      const { error: deleteEventsErr } = await supabase
        .from('events')
        .delete()
        .eq('from_recurring_game_id', id)
        .gte('event_date', today)

      if (deleteEventsErr) {
        return errorResponse('Failed to delete future events', 500)
      }
    }

    // Delete the recurring game record
    const { error: deleteErr } = await supabase
      .from('recurring_games')
      .delete()
      .eq('id', id)

    if (deleteErr) {
      return errorResponse(`Failed to delete recurring game: ${deleteErr.message}`, 500)
    }

    return jsonResponse({ success: true })
  }

  return errorResponse('Method not allowed', 405)
})
