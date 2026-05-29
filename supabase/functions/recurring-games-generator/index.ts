import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { getCorsHeaders } from '../_shared/firebase.ts'

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
    const d = new Date(afterDate)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + (diff === 0 ? 0 : diff))
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'biweekly') {
    const d = new Date(afterDate)
    d.setUTCDate(d.getUTCDate() + 14)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + diff)
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'monthly') {
    const d = new Date(afterDate)
    let year = d.getUTCFullYear()
    let month = d.getUTCMonth() + 1
    if (month > 11) {
      month = 0
      year++
    }

    if (monthlyWeek === -1) {
      const lastDay = new Date(Date.UTC(year, month + 1, 0))
      const diff = (lastDay.getUTCDay() - dayOfWeek + 7) % 7
      lastDay.setUTCDate(lastDay.getUTCDate() - diff)
      return lastDay.toISOString().slice(0, 10)
    }

    const week = monthlyWeek ?? 1
    const firstOfMonth = new Date(Date.UTC(year, month, 1))
    const firstDayDiff = (dayOfWeek - firstOfMonth.getUTCDay() + 7) % 7
    const nthDate = 1 + firstDayDiff + (week - 1) * 7
    const result = new Date(Date.UTC(year, month, nthDate))
    return result.toISOString().slice(0, 10)
  }

  return computeNextOccurrence('weekly', dayOfWeek, null, afterDate)
}

// ── Advance next_occurrence_date for a recurring game ──────────────────
async function advanceNextDate(
  supabase: ReturnType<typeof createClient>,
  rg: Record<string, unknown>,
  nextDate: string
) {
  const afterDate = new Date(nextDate)
  afterDate.setUTCDate(afterDate.getUTCDate() + 1)
  const newNextDate = computeNextOccurrence(
    rg.frequency as string,
    rg.day_of_week as number,
    rg.monthly_week as number | null,
    afterDate
  )

  await supabase
    .from('recurring_games')
    .update({
      last_generated_date: nextDate,
      next_occurrence_date: newNextDate,
    })
    .eq('id', rg.id)
}

// ── Generate a planning session from a recurring game ──────────────────
async function generatePlanningSession(
  supabase: ReturnType<typeof createClient>,
  rg: Record<string, unknown>,
  nextDate: string
): Promise<boolean> {
  // Skip if there are already 2+ open sessions for this recurring game
  // (allows overlapping sessions for weekly games where deadlines span multiple weeks)
  const { data: openSessions } = await supabase
    .from('planning_sessions')
    .select('id')
    .eq('from_recurring_game_id', rg.id)
    .eq('status', 'open')

  if (openSessions && openSessions.length >= 2) return false // limit concurrent open sessions

  // Idempotency check: planning session already exists for this date?
  const { data: existingSession } = await supabase
    .from('planning_sessions')
    .select('id')
    .eq('from_recurring_game_id', rg.id)
    .eq('target_event_date', nextDate)
    .maybeSingle()

  if (existingSession) return false // skip

  // Compute response deadline: event date minus deadline_day_offset at 23:59
  const deadlineOffset = (rg.deadline_day_offset as number) ?? 1
  const deadlineDate = new Date(nextDate + 'T23:59:00Z')
  deadlineDate.setUTCDate(deadlineDate.getUTCDate() - deadlineOffset)

  // Determine max_games based on host's tier
  let maxGames = 5
  if (rg.host_user_id) {
    const { data: hostUser } = await supabase
      .from('users')
      .select('subscription_tier, subscription_override_tier')
      .eq('id', rg.host_user_id)
      .single()
    if (hostUser) {
      const tier = hostUser.subscription_override_tier || hostUser.subscription_tier || 'free'
      maxGames = tier === 'pro' || tier === 'premium' ? 10 : 5
    }
  }

  // Create planning session
  const { data: session, error: sessionError } = await supabase
    .from('planning_sessions')
    .insert({
      group_id: rg.group_id,
      created_by_user_id: rg.host_user_id,
      title: rg.title,
      description: rg.description,
      response_deadline: deadlineDate.toISOString(),
      status: 'open',
      max_participants: rg.max_players,
      max_games: maxGames,
      table_count: rg.table_count,
      open_to_group: true,
      from_recurring_game_id: rg.id,
      target_event_date: nextDate,
      // Location fields
      event_location_id: rg.event_location_id,
      address_line1: rg.address_line1,
      city: rg.city,
      state: rg.state,
      postal_code: rg.postal_code,
      location_details: rg.location_details,
    })
    .select('id')
    .single()

  if (sessionError) {
    console.error(`Failed to create planning session for recurring game ${rg.id}: ${sessionError.message}`)
    return false
  }

  // Add proposed date
  await supabase
    .from('planning_dates')
    .insert({
      session_id: session.id,
      proposed_date: nextDate,
      start_time: rg.start_time || '19:00',
    })

  // Invite all group members
  const { data: members } = await supabase
    .from('group_memberships')
    .select('user_id')
    .eq('group_id', rg.group_id)

  if (members && members.length > 0) {
    const invitees = members.map(m => ({
      session_id: session.id,
      user_id: m.user_id,
      has_slot: true, // All group members get a participation slot
      // Host is auto-accepted
      accepted_at: m.user_id === rg.host_user_id ? new Date().toISOString() : null,
    }))

    await supabase.from('planning_invitees').insert(invitees)
  }

  console.log(`Created planning session ${session.id} for recurring game ${rg.id} (event date: ${nextDate}, deadline: ${deadlineDate.toISOString()})`)
  return true
}

// ── Generate a direct event from a recurring game (existing behavior) ──
async function generateEvent(
  supabase: ReturnType<typeof createClient>,
  rg: Record<string, unknown>,
  nextDate: string
): Promise<boolean> {
  // Idempotency check
  const { data: existing } = await supabase
    .from('events')
    .select('id')
    .eq('from_recurring_game_id', rg.id)
    .eq('event_date', nextDate)
    .maybeSingle()

  if (existing) return false // skip

  const { error: insertError } = await supabase
    .from('events')
    .insert({
      host_user_id: rg.host_user_id,
      group_id: rg.group_id,
      title: rg.title,
      description: rg.description,
      game_system: rg.game_system ?? 'board_game',
      game_title: rg.game_title,
      event_date: nextDate,
      start_time: rg.start_time,
      timezone: rg.timezone,
      duration_minutes: rg.duration_minutes ?? 120,
      max_players: rg.max_players ?? 4,
      host_is_playing: rg.host_is_playing ?? true,
      is_public: rg.is_public ?? true,
      status: 'published',
      event_location_id: rg.event_location_id,
      address_line1: rg.address_line1,
      city: rg.city,
      state: rg.state,
      postal_code: rg.postal_code,
      location_details: rg.location_details,
      from_recurring_game_id: rg.id,
    })

  if (insertError) {
    console.error(`Failed to create event for recurring game ${rg.id}: ${insertError.message}`)
    return false
  }

  return true
}

// ── Auto-finalize expired planning sessions ────────────────────────────
async function autoFinalizeSessions(
  supabase: ReturnType<typeof createClient>
): Promise<{ finalized: number; errors: string[] }> {
  const errors: string[] = []
  let finalized = 0

  // Find open planning sessions from recurring games where deadline has passed
  const { data: expiredSessions, error: fetchError } = await supabase
    .from('planning_sessions')
    .select('*')
    .not('from_recurring_game_id', 'is', null)
    .eq('status', 'open')
    .lt('response_deadline', new Date().toISOString())

  if (fetchError || !expiredSessions || expiredSessions.length === 0) {
    return { finalized: 0, errors: fetchError ? [fetchError.message] : [] }
  }

  for (const session of expiredSessions) {
    try {
      // Get the proposed date (should be exactly one for recurring sessions)
      const { data: dates } = await supabase
        .from('planning_dates')
        .select('id, proposed_date, start_time')
        .eq('session_id', session.id)
        .limit(1)

      const finalDate = dates?.[0]
      if (!finalDate) {
        errors.push(`No proposed date for session ${session.id}`)
        continue
      }

      // Get game suggestions with vote counts
      const { data: suggestions } = await supabase
        .from('planning_game_suggestions')
        .select('id, bgg_id, game_name, thumbnail_url, min_players, max_players, playing_time')
        .eq('session_id', session.id)

      // Count votes per suggestion
      let qualifyingGames: Record<string, unknown>[] = []
      let topGame: Record<string, unknown> | null = null

      if (suggestions && suggestions.length > 0) {
        const suggestionIds = suggestions.map(s => s.id)
        const { data: allVotes } = await supabase
          .from('planning_game_votes')
          .select('suggestion_id')
          .in('suggestion_id', suggestionIds)

        const voteCounts: Record<string, number> = {}
        for (const v of allVotes ?? []) {
          voteCounts[v.suggestion_id] = (voteCounts[v.suggestion_id] || 0) + 1
        }

        // Qualifying games: 2+ votes (or all if none qualify)
        const gamesWithVotes = suggestions.map(s => ({
          ...s,
          voteCount: voteCounts[s.id] || 0,
        }))
        gamesWithVotes.sort((a, b) => b.voteCount - a.voteCount)

        qualifyingGames = gamesWithVotes
          .filter(g => g.voteCount >= 2)
          .map(g => ({
            bgg_id: g.bgg_id,
            game_name: g.game_name,
            thumbnail_url: g.thumbnail_url,
            min_players: g.min_players,
            max_players: g.max_players,
            playing_time: g.playing_time,
            votes: g.voteCount,
          }))

        topGame = gamesWithVotes[0] || null
      }

      // Determine multi-table
      const isMultiTable = (session.table_count ?? 0) >= 2 && !!session.scheduled_sessions

      // Get available voters for auto-registration
      const { data: dateVotes } = await supabase
        .from('planning_date_votes')
        .select('user_id')
        .eq('date_id', finalDate.id)
        .eq('is_available', true)

      const availableUserIds = (dateVotes ?? []).map(v => v.user_id)

      // Determine max players
      const maxPlayers = session.max_participants || Math.max(availableUserIds.length + 1, 8)

      // Create the event
      const { data: event, error: eventError } = await supabase
        .from('events')
        .insert({
          host_user_id: session.created_by_user_id,
          group_id: session.group_id,
          title: session.title,
          description: session.description,
          game_title: topGame?.game_name || null,
          event_date: finalDate.proposed_date,
          start_time: finalDate.start_time || '19:00',
          duration_minutes: 180,
          max_players: maxPlayers,
          status: 'published',
          is_public: false,
          from_planning_session_id: session.id,
          from_recurring_game_id: session.from_recurring_game_id,
          planned_games: qualifyingGames.length > 0 ? qualifyingGames : null,
          is_multi_table: isMultiTable,
          // Location
          event_location_id: session.event_location_id,
          venue_hall: session.venue_hall,
          venue_room: session.venue_room,
          venue_table: session.venue_table,
          address_line1: session.address_line1,
          city: session.city,
          state: session.state,
          postal_code: session.postal_code,
          location_details: session.location_details,
        })
        .select('id')
        .single()

      if (eventError) {
        errors.push(`Failed to create event for session ${session.id}: ${eventError.message}`)
        continue
      }

      // Register available voters as attendees
      if (availableUserIds.length > 0) {
        const registrations = availableUserIds.map(userId => ({
          event_id: event.id,
          user_id: userId,
          status: 'confirmed',
        }))
        await supabase.from('event_registrations').upsert(registrations, { onConflict: 'event_id,user_id' })
      }

      // Handle multi-table setup
      if (isMultiTable && session.table_count) {
        const tableRows = []
        for (let i = 1; i <= session.table_count; i++) {
          tableRows.push({
            event_id: event.id,
            table_number: i,
            table_name: `Table ${i}`,
          })
        }
        await supabase.from('event_tables').insert(tableRows)
      }

      // Copy planning items to event items
      const { data: planningItems } = await supabase
        .from('planning_session_items')
        .select('item_name, item_category, quantity_needed, claimed_by_user_id, claimed_at')
        .eq('session_id', session.id)

      if (planningItems && planningItems.length > 0) {
        const eventItems = planningItems.map(item => ({
          event_id: event.id,
          item_name: item.item_name,
          item_category: item.item_category,
          quantity_needed: item.quantity_needed,
          claimed_by_user_id: item.claimed_by_user_id,
          claimed_at: item.claimed_at,
        }))
        await supabase.from('event_items').insert(eventItems)
      }

      // Mark session as finalized
      await supabase
        .from('planning_sessions')
        .update({
          status: 'finalized',
          finalized_date: finalDate.proposed_date,
          finalized_game_id: topGame?.id || null,
          created_event_id: event.id,
          updated_at: new Date().toISOString(),
        })
        .eq('id', session.id)

      finalized++
      console.log(`Auto-finalized session ${session.id} → event ${event.id}`)
    } catch (err) {
      errors.push(`Unexpected error finalizing session ${session.id}: ${(err as Error).message}`)
    }
  }

  return { finalized, errors }
}

// ── Auto-cancel expired non-recurring planning sessions ──────────────
async function autoCancelExpiredSessions(
  supabase: ReturnType<typeof createClient>
): Promise<{ cancelled: number; errors: string[] }> {
  const errors: string[] = []

  // Find open planning sessions (without a recurring game) where deadline has passed
  const { data: expiredSessions, error: fetchError } = await supabase
    .from('planning_sessions')
    .select('id, title')
    .is('from_recurring_game_id', null)
    .eq('status', 'open')
    .lt('response_deadline', new Date().toISOString())

  if (fetchError || !expiredSessions || expiredSessions.length === 0) {
    return { cancelled: 0, errors: fetchError ? [fetchError.message] : [] }
  }

  const { error: updateError } = await supabase
    .from('planning_sessions')
    .update({ status: 'cancelled', updated_at: new Date().toISOString() })
    .in('id', expiredSessions.map(s => s.id))

  if (updateError) {
    errors.push(`Failed to cancel expired sessions: ${updateError.message}`)
    return { cancelled: 0, errors }
  }

  for (const s of expiredSessions) {
    console.log(`Auto-cancelled expired session ${s.id} (${s.title})`)
  }

  return { cancelled: expiredSessions.length, errors }
}

// ── Main handler ───────────────────────────────────────────────────────
Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const corsHeaders = getCorsHeaders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  const generated: string[] = []
  let skipped = 0
  const errors: string[] = []

  try {
    // ── Pass 1: Generate new events/planning sessions ──────────────
    const { data: recurringGames, error: fetchError } = await supabase
      .from('recurring_games')
      .select('*')
      .eq('is_active', true)
      .lte('next_occurrence_date', new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10))

    if (fetchError) {
      return new Response(
        JSON.stringify({ error: `Failed to fetch recurring games: ${fetchError.message}` }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    for (const rg of recurringGames ?? []) {
      try {
        const nextDate = rg.next_occurrence_date as string

        let created: boolean
        if (rg.generates_planning_session) {
          created = await generatePlanningSession(supabase, rg, nextDate)
        } else {
          created = await generateEvent(supabase, rg, nextDate)
        }

        if (created) {
          generated.push(rg.id)
          // Only advance next_occurrence_date when a session/event was actually created
          await advanceNextDate(supabase, rg, nextDate)
        } else {
          skipped++
        }
      } catch (err) {
        errors.push(`Unexpected error for recurring game ${rg.id}: ${(err as Error).message}`)
      }
    }

    // ── Pass 2: Auto-finalize expired recurring planning sessions ───
    const finalizeResult = await autoFinalizeSessions(supabase)
    errors.push(...finalizeResult.errors)

    // ── Pass 3: Auto-cancel expired non-recurring planning sessions ─
    const cancelResult = await autoCancelExpiredSessions(supabase)
    errors.push(...cancelResult.errors)

    return new Response(
      JSON.stringify({
        generated: generated.length,
        skipped,
        autoFinalized: finalizeResult.finalized,
        autoCancelled: cancelResult.cancelled,
        errors,
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  } catch (err) {
    return new Response(
      JSON.stringify({ error: `Unexpected error: ${(err as Error).message}` }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
