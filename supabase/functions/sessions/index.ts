import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // All sessions operations require authentication
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
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)
  const eventId = url.searchParams.get('eventId')
  const sessionId = url.searchParams.get('sessionId')
  const action = url.searchParams.get('action')

  // GET - Fetch sessions for an event
  if (req.method === 'GET') {
    if (!eventId) {
      return errorResponse('eventId required', 400)
    }

    // Verify event exists and user has access
    const { data: event, error: eventError } = await supabase
      .from('events')
      .select('id, host_user_id, group_id, is_multi_table')
      .eq('id', eventId)
      .single()

    if (eventError || !event) {
      return errorResponse('Event not found', 404)
    }

    if (!event.is_multi_table) {
      return errorResponse('This event does not use multi-table sessions', 400)
    }

    // Fetch tables
    const { data: tables } = await supabase
      .from('event_tables')
      .select('id, table_number, table_name')
      .eq('event_id', eventId)
      .order('table_number')

    // Fetch sessions with registration counts
    const { data: sessions } = await supabase
      .from('event_game_sessions')
      .select(`
        id, table_id, bgg_id, game_name, thumbnail_url,
        min_players, max_players, slot_index, start_time,
        duration_minutes, status,
        registrations:game_session_registrations(
          id, user_id, is_host_reserved,
          user:users(id, display_name, avatar_url)
        )
      `)
      .eq('event_id', eventId)
      .order('slot_index')

    // Get user's registered session IDs
    const { data: userRegistrations } = await supabase
      .from('game_session_registrations')
      .select('session_id')
      .eq('user_id', user.id)

    const userSessionIds = new Set(userRegistrations?.map(r => r.session_id) || [])

    // Transform sessions
    const transformedSessions = (sessions ?? []).map(s => {
      const registrations = (s.registrations as Array<{
        id: string
        user_id: string
        is_host_reserved: boolean
        user: { id: string; display_name: string | null; avatar_url: string | null } | null
      }>) ?? []

      // Find table number from table_id
      const table = (tables ?? []).find(t => t.id === s.table_id)

      return {
        id: s.id,
        tableId: s.table_id,
        tableNumber: table?.table_number ?? 0,
        bggId: s.bgg_id,
        gameName: s.game_name,
        thumbnailUrl: s.thumbnail_url,
        minPlayers: s.min_players,
        maxPlayers: s.max_players,
        slotIndex: s.slot_index,
        startTime: s.start_time,
        durationMinutes: s.duration_minutes,
        status: s.status,
        registeredCount: registrations.length,
        isFull: s.max_players ? registrations.length >= s.max_players : false,
        isUserRegistered: userSessionIds.has(s.id),
        registrations: registrations.map(r => ({
          userId: r.user_id,
          displayName: r.user?.display_name ?? null,
          avatarUrl: r.user?.avatar_url ?? null,
          isHostReserved: r.is_host_reserved,
        })),
      }
    })

    return jsonResponse({
      tables: (tables ?? []).map(t => ({
        id: t.id,
        tableNumber: t.table_number,
        tableName: t.table_name,
      })),
      sessions: transformedSessions,
    })
  }

  // POST - Register for session or create new session
  if (req.method === 'POST') {
    // Register for a session
    if (action === 'register') {
      if (!sessionId) {
        return errorResponse('sessionId required', 400)
      }

      // Get session with event info
      const { data: session, error: sessionError } = await supabase
        .from('event_game_sessions')
        .select(`
          id, event_id, table_id, max_players, slot_index,
          event:events(id, host_user_id, group_id, is_multi_table)
        `)
        .eq('id', sessionId)
        .single()

      if (sessionError || !session) {
        return errorResponse('Session not found', 404)
      }

      const event = session.event as { id: string; host_user_id: string; group_id: string | null; is_multi_table: boolean }

      if (!event.is_multi_table) {
        return errorResponse('This event does not use multi-table sessions', 400)
      }

      // Check if user is in the group (if group event)
      if (event.group_id) {
        const { data: membership } = await supabase
          .from('group_memberships')
          .select('id')
          .eq('group_id', event.group_id)
          .eq('user_id', user.id)
          .single()

        if (!membership) {
          return errorResponse('You must be a group member to register', 403)
        }
      }

      // Check if session is full
      const { count: registrationCount } = await supabase
        .from('game_session_registrations')
        .select('id', { count: 'exact', head: true })
        .eq('session_id', sessionId)

      if (session.max_players && (registrationCount ?? 0) >= session.max_players) {
        return errorResponse('Session is full', 400)
      }

      // Check if user is already registered
      const { data: existing } = await supabase
        .from('game_session_registrations')
        .select('id')
        .eq('session_id', sessionId)
        .eq('user_id', user.id)
        .single()

      if (existing) {
        return errorResponse('Already registered for this session', 400)
      }

      // Check if user is registered for another session at the same slot
      const { data: conflicting } = await supabase
        .from('game_session_registrations')
        .select(`
          id,
          session:event_game_sessions(slot_index, event_id)
        `)
        .eq('user_id', user.id)

      const hasConflict = (conflicting ?? []).some(r => {
        const s = r.session as { slot_index: number; event_id: string } | null
        return s && s.event_id === session.event_id && s.slot_index === session.slot_index
      })

      if (hasConflict) {
        return errorResponse('You are already registered for another session at this time slot', 400)
      }

      // Register user
      const { error: insertError } = await supabase
        .from('game_session_registrations')
        .insert({
          session_id: sessionId,
          user_id: user.id,
          is_host_reserved: false,
        })

      if (insertError) return errorResponse(insertError.message, 500)

      return jsonResponse({ message: 'Registered successfully' })
    }

    // Create a new session (host only)
    if (action === 'create') {
      const body = await req.json()

      if (!body.eventId || !body.tableId || !body.gameName) {
        return errorResponse('eventId, tableId, and gameName required', 400)
      }

      // Verify user is host
      const { data: event, error: eventError } = await supabase
        .from('events')
        .select('id, host_user_id, is_multi_table')
        .eq('id', body.eventId)
        .single()

      if (eventError || !event) {
        return errorResponse('Event not found', 404)
      }

      if (event.host_user_id !== user.id) {
        return errorResponse('Only the host can create sessions', 403)
      }

      if (!event.is_multi_table) {
        return errorResponse('This event does not use multi-table sessions', 400)
      }

      // Verify table belongs to event
      const { data: table } = await supabase
        .from('event_tables')
        .select('id')
        .eq('id', body.tableId)
        .eq('event_id', body.eventId)
        .single()

      if (!table) {
        return errorResponse('Table not found', 404)
      }

      // Find next slot index for this table
      const { data: existingSessions } = await supabase
        .from('event_game_sessions')
        .select('slot_index')
        .eq('event_id', body.eventId)
        .eq('table_id', body.tableId)
        .order('slot_index', { ascending: false })
        .limit(1)

      const nextSlotIndex = existingSessions?.length ? existingSessions[0].slot_index + 1 : 0

      // Create session
      const { data: newSession, error: insertError } = await supabase
        .from('event_game_sessions')
        .insert({
          event_id: body.eventId,
          table_id: body.tableId,
          bgg_id: body.bggId || null,
          game_name: body.gameName,
          thumbnail_url: body.thumbnailUrl || null,
          min_players: body.minPlayers || null,
          max_players: body.maxPlayers || null,
          slot_index: nextSlotIndex,
          start_time: body.startTime || null,
          duration_minutes: body.durationMinutes || 60,
          status: 'scheduled',
        })
        .select()
        .single()

      if (insertError) return errorResponse(insertError.message, 500)

      return jsonResponse({
        message: 'Session created',
        session: {
          id: newSession.id,
          tableId: newSession.table_id,
          slotIndex: newSession.slot_index,
          gameName: newSession.game_name,
        },
      })
    }

    return errorResponse('Invalid action', 400)
  }

  // PUT - Update session (host only)
  if (req.method === 'PUT') {
    if (!sessionId) {
      return errorResponse('sessionId required', 400)
    }

    // Get session with event info
    const { data: session, error: sessionError } = await supabase
      .from('event_game_sessions')
      .select(`
        id, event_id,
        event:events(host_user_id)
      `)
      .eq('id', sessionId)
      .single()

    if (sessionError || !session) {
      return errorResponse('Session not found', 404)
    }

    const event = session.event as { host_user_id: string }

    if (event.host_user_id !== user.id) {
      return errorResponse('Only the host can update sessions', 403)
    }

    const body = await req.json()

    const updates: Record<string, unknown> = {}
    if (body.gameName !== undefined) updates.game_name = body.gameName
    if (body.bggId !== undefined) updates.bgg_id = body.bggId
    if (body.thumbnailUrl !== undefined) updates.thumbnail_url = body.thumbnailUrl
    if (body.minPlayers !== undefined) updates.min_players = body.minPlayers
    if (body.maxPlayers !== undefined) updates.max_players = body.maxPlayers
    if (body.startTime !== undefined) updates.start_time = body.startTime
    if (body.durationMinutes !== undefined) updates.duration_minutes = body.durationMinutes
    if (body.status !== undefined) updates.status = body.status

    if (Object.keys(updates).length === 0) {
      return errorResponse('No updates provided', 400)
    }

    const { error: updateError } = await supabase
      .from('event_game_sessions')
      .update(updates)
      .eq('id', sessionId)

    if (updateError) return errorResponse(updateError.message, 500)

    return jsonResponse({ message: 'Session updated' })
  }

  // DELETE - Cancel registration or delete session
  if (req.method === 'DELETE') {
    if (!sessionId) {
      return errorResponse('sessionId required', 400)
    }

    // Delete entire session (host only)
    if (action === 'delete-session') {
      // Get session with event info
      const { data: session, error: sessionError } = await supabase
        .from('event_game_sessions')
        .select(`
          id, event_id,
          event:events(host_user_id)
        `)
        .eq('id', sessionId)
        .single()

      if (sessionError || !session) {
        return errorResponse('Session not found', 404)
      }

      const event = session.event as { host_user_id: string }

      if (event.host_user_id !== user.id) {
        return errorResponse('Only the host can delete sessions', 403)
      }

      // Delete registrations first
      await supabase
        .from('game_session_registrations')
        .delete()
        .eq('session_id', sessionId)

      // Delete session
      const { error: deleteError } = await supabase
        .from('event_game_sessions')
        .delete()
        .eq('id', sessionId)

      if (deleteError) return errorResponse(deleteError.message, 500)

      return jsonResponse({ message: 'Session deleted' })
    }

    // Cancel user's registration
    const { data: registration } = await supabase
      .from('game_session_registrations')
      .select('id, is_host_reserved')
      .eq('session_id', sessionId)
      .eq('user_id', user.id)
      .single()

    if (!registration) {
      return errorResponse('Not registered for this session', 404)
    }

    // Don't allow cancelling host-reserved spots (they chose during planning)
    if (registration.is_host_reserved) {
      return errorResponse('Cannot cancel host-reserved registration', 400)
    }

    const { error: deleteError } = await supabase
      .from('game_session_registrations')
      .delete()
      .eq('id', registration.id)

    if (deleteError) return errorResponse(deleteError.message, 500)

    return jsonResponse({ message: 'Registration cancelled' })
  }

  return errorResponse('Method not allowed', 405)
})
