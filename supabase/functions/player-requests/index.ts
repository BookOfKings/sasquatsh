import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Max duration for a player request (15 minutes)
const MAX_REQUEST_MINUTES = 15

// Transform database row to PlayerRequest
function toPlayerRequest(row: Record<string, unknown>) {
  const event = row.event as Record<string, unknown> | null
  const host = row.user as Record<string, unknown> | null
  return {
    id: row.id as string,
    userId: row.user_id as string,
    eventId: row.event_id as string,
    description: row.description as string | null,
    playerCountNeeded: row.player_count_needed as number,
    status: row.status as string,
    isActive: row.is_active as boolean,
    createdAt: row.created_at as string,
    expiresAt: row.expires_at as string,
    event: event ? {
      id: event.id as string,
      title: event.title as string,
      gameTitle: event.game_title as string | null,
      eventDate: event.event_date as string,
      startTime: event.start_time as string,
      city: event.city as string | null,
      state: event.state as string | null,
      addressLine1: event.address_line1 as string | null,
      locationDetails: event.location_details as string | null,
    } : null,
    host: host ? {
      id: host.id as string,
      displayName: host.display_name as string | null,
      username: host.username as string | null,
      avatarUrl: host.avatar_url as string | null,
    } : null,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const requestId = url.searchParams.get('id')
  const action = url.searchParams.get('action')

  // GET - List active player requests (public)
  if (req.method === 'GET' && !requestId) {
    const eventId = url.searchParams.get('eventId')

    // Get blocked user IDs if user is authenticated
    let blockedUserIds: string[] = []
    const token = getFirebaseToken(req)
    if (token) {
      const firebaseUser = await verifyFirebaseToken(token)
      if (firebaseUser) {
        const { data: currentUser } = await supabase
          .from('users')
          .select('blocked_user_ids')
          .eq('firebase_uid', firebaseUser.uid)
          .single()
        blockedUserIds = currentUser?.blocked_user_ids ?? []
      }
    }

    let query = supabase
      .from('player_requests')
      .select(`
        *,
        user:users(id, display_name, username, avatar_url),
        event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
      `)
      .eq('status', 'open')
      .eq('is_active', true)
      .gt('expires_at', new Date().toISOString())
      .order('created_at', { ascending: false })
      .limit(50)

    // Filter out blocked users
    if (blockedUserIds.length > 0) {
      query = query.not('user_id', 'in', `(${blockedUserIds.join(',')})`)
    }

    // Filter by specific event
    if (eventId) {
      query = query.eq('event_id', eventId)
    }

    const { data, error } = await query

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toPlayerRequest))
  }

  // All other operations require authentication
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // GET - Get my requests (as host)
  if (req.method === 'GET' && requestId === 'mine') {
    const { data, error } = await supabase
      .from('player_requests')
      .select(`
        *,
        user:users(id, display_name, username, avatar_url),
        event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
      `)
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })
      .limit(20)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toPlayerRequest))
  }

  // POST - Create player request (host needs players for their event)
  if (req.method === 'POST' && !action) {
    const body = await req.json()

    if (!body.eventId) {
      return errorResponse('eventId is required', 400)
    }

    // Verify the user is the host of this event
    const { data: event, error: eventError } = await supabase
      .from('events')
      .select('id, host_user_id, title, event_date, start_time')
      .eq('id', body.eventId)
      .single()

    if (eventError || !event) {
      return errorResponse('Event not found', 404)
    }

    if (event.host_user_id !== user.id) {
      return errorResponse('Only the event host can request players', 403)
    }

    // Check if there's already an active request for this event
    const { data: existingRequest } = await supabase
      .from('player_requests')
      .select('id')
      .eq('event_id', body.eventId)
      .eq('status', 'open')
      .gt('expires_at', new Date().toISOString())
      .single()

    if (existingRequest) {
      return errorResponse('There is already an active player request for this event', 400)
    }

    // Calculate expiry (max 15 minutes)
    const expiresAt = new Date()
    expiresAt.setMinutes(expiresAt.getMinutes() + MAX_REQUEST_MINUTES)

    const { data, error } = await supabase
      .from('player_requests')
      .insert({
        user_id: user.id,
        event_id: body.eventId,
        description: body.description?.trim() || null,
        player_count_needed: body.playerCountNeeded || 1,
        status: 'open',
        expires_at: expiresAt.toISOString(),
      })
      .select(`
        *,
        user:users(id, display_name, username, avatar_url),
        event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toPlayerRequest(data), 201)
  }

  // POST with action - Mark as filled or cancelled
  if (req.method === 'POST' && action) {
    if (!requestId) {
      return errorResponse('Request ID required', 400)
    }

    // Verify ownership
    const { data: existing } = await supabase
      .from('player_requests')
      .select('user_id, status')
      .eq('id', requestId)
      .single()

    if (!existing) {
      return errorResponse('Request not found', 404)
    }

    if (existing.user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    if (action === 'fill') {
      // Mark as filled (found players)
      const { data, error } = await supabase
        .from('player_requests')
        .update({
          status: 'filled',
          is_active: false,
          updated_at: new Date().toISOString(),
        })
        .eq('id', requestId)
        .select(`
          *,
          user:users(id, display_name, username, avatar_url),
          event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
        `)
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(toPlayerRequest(data))
    }

    if (action === 'cancel') {
      // Cancel the request
      const { data, error } = await supabase
        .from('player_requests')
        .update({
          status: 'cancelled',
          is_active: false,
          updated_at: new Date().toISOString(),
        })
        .eq('id', requestId)
        .select(`
          *,
          user:users(id, display_name, username, avatar_url),
          event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
        `)
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(toPlayerRequest(data))
    }

    return errorResponse('Invalid action', 400)
  }

  // PUT - Update player request (only description and player count)
  if (req.method === 'PUT') {
    if (!requestId) {
      return errorResponse('Request ID required', 400)
    }

    // Verify ownership
    const { data: existing } = await supabase
      .from('player_requests')
      .select('user_id, status')
      .eq('id', requestId)
      .single()

    if (!existing) {
      return errorResponse('Request not found', 404)
    }

    if (existing.user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    if (existing.status !== 'open') {
      return errorResponse('Cannot update a closed request', 400)
    }

    const body = await req.json()
    const updates: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    if (body.description !== undefined) updates.description = body.description?.trim() || null
    if (body.playerCountNeeded !== undefined) updates.player_count_needed = body.playerCountNeeded

    const { data, error } = await supabase
      .from('player_requests')
      .update(updates)
      .eq('id', requestId)
      .select(`
        *,
        user:users(id, display_name, username, avatar_url),
        event:events(id, title, game_title, event_date, start_time, city, state, address_line1, location_details)
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toPlayerRequest(data))
  }

  // DELETE - Delete player request
  if (req.method === 'DELETE') {
    if (!requestId) {
      return errorResponse('Request ID required', 400)
    }

    // Verify ownership
    const { data: existing } = await supabase
      .from('player_requests')
      .select('user_id')
      .eq('id', requestId)
      .single()

    if (!existing || existing.user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    const { error } = await supabase
      .from('player_requests')
      .delete()
      .eq('id', requestId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})
