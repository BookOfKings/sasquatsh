import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Transform database row to EventGame
function toEventGame(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    eventId: row.event_id as string,
    bggId: row.bgg_id as number | null,
    gameName: row.game_name as string,
    thumbnailUrl: row.thumbnail_url as string | null,
    minPlayers: row.min_players as number | null,
    maxPlayers: row.max_players as number | null,
    playingTime: row.playing_time as number | null,
    isPrimary: row.is_primary as boolean,
    isAlternative: row.is_alternative as boolean,
    addedByUserId: row.added_by_user_id as string | null,
    createdAt: row.created_at as string,
  }
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
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)
  const gameId = url.searchParams.get('id')
  const action = url.searchParams.get('action')
  const suggestionId = url.searchParams.get('suggestionId')

  // POST - Action-based routes for suggestions
  if (req.method === 'POST' && action) {
    // Helper: verify user is host, registered, or group admin
    async function verifyEventAccess(eventId: string): Promise<{ event: { host_user_id: string; group_id: string | null } | null; isHost: boolean; isRegistered: boolean; isGroupAdmin: boolean; error?: Response }> {
      const { data: event } = await supabase
        .from('events')
        .select('host_user_id, group_id')
        .eq('id', eventId)
        .single()

      if (!event) {
        return { event: null, isHost: false, isRegistered: false, isGroupAdmin: false, error: errorResponse('Event not found', 404) }
      }

      const isHost = event.host_user_id === user.id
      const { data: registration } = await supabase
        .from('event_registrations')
        .select('id')
        .eq('event_id', eventId)
        .eq('user_id', user.id)
        .single()

      // Check if user is a group admin/owner
      let isGroupAdmin = false
      if (event.group_id) {
        const { data: membership } = await supabase
          .from('group_memberships')
          .select('role')
          .eq('group_id', event.group_id)
          .eq('user_id', user.id)
          .single()
        isGroupAdmin = membership?.role === 'owner' || membership?.role === 'admin'
      }

      return { event, isHost, isRegistered: !!registration, isGroupAdmin }
    }

    // Suggest a game
    if (action === 'suggest') {
      const body = await req.json()
      const eventId = body.eventId
      if (!eventId) return errorResponse('Event ID required', 400)
      if (!body.gameName?.trim()) return errorResponse('Game name required', 400)

      const access = await verifyEventAccess(eventId)
      if (access.error) return access.error
      if (!access.isHost && !access.isRegistered && !access.isGroupAdmin) return errorResponse('Only host, registered users, or group admins can suggest games', 403)

      const { data, error } = await supabase
        .from('event_game_suggestions')
        .insert({
          event_id: eventId,
          suggested_by_user_id: user.id,
          bgg_id: body.bggId || null,
          game_name: body.gameName.trim(),
          thumbnail_url: body.thumbnailUrl || null,
          min_players: body.minPlayers || null,
          max_players: body.maxPlayers || null,
          playing_time: body.playingTime || null,
        })
        .select(`
          id, event_id, suggested_by_user_id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time, created_at,
          suggested_by:users!suggested_by_user_id(display_name, avatar_url)
        `)
        .single()

      if (error) {
        if (error.code === '23505') return errorResponse('This game has already been suggested', 409)
        return errorResponse(error.message, 500)
      }

      const suggestedBy = data.suggested_by as { display_name: string | null; avatar_url: string | null } | null
      return jsonResponse({
        id: data.id,
        suggestedByUserId: data.suggested_by_user_id,
        bggId: data.bgg_id,
        gameName: data.game_name,
        thumbnailUrl: data.thumbnail_url,
        minPlayers: data.min_players,
        maxPlayers: data.max_players,
        playingTime: data.playing_time,
        createdAt: data.created_at,
        voteCount: 0,
        hasVoted: false,
        suggestedBy: suggestedBy ? { displayName: suggestedBy.display_name, avatarUrl: suggestedBy.avatar_url } : null,
      }, 201)
    }

    // Vote/unvote a suggestion
    if (action === 'vote') {
      if (!suggestionId) return errorResponse('Suggestion ID required', 400)

      // Get the suggestion to find the event
      const { data: suggestion } = await supabase
        .from('event_game_suggestions')
        .select('event_id')
        .eq('id', suggestionId)
        .single()

      if (!suggestion) return errorResponse('Suggestion not found', 404)

      const access = await verifyEventAccess(suggestion.event_id)
      if (access.error) return access.error
      if (!access.isHost && !access.isRegistered && !access.isGroupAdmin) return errorResponse('Only host, registered users, or group admins can vote', 403)

      // Toggle vote
      const { data: existingVote } = await supabase
        .from('event_game_votes')
        .select('id')
        .eq('suggestion_id', suggestionId)
        .eq('user_id', user.id)
        .single()

      if (existingVote) {
        await supabase.from('event_game_votes').delete().eq('id', existingVote.id)
        return jsonResponse({ voted: false })
      } else {
        await supabase.from('event_game_votes').insert({ suggestion_id: suggestionId, user_id: user.id })
        return jsonResponse({ voted: true })
      }
    }

    // Remove a suggestion
    if (action === 'remove-suggestion') {
      if (!suggestionId) return errorResponse('Suggestion ID required', 400)

      const { data: suggestion } = await supabase
        .from('event_game_suggestions')
        .select('event_id, suggested_by_user_id')
        .eq('id', suggestionId)
        .single()

      if (!suggestion) return errorResponse('Suggestion not found', 404)

      const access = await verifyEventAccess(suggestion.event_id)
      if (access.error) return access.error

      // Only suggester, host, or group admin can remove
      if (suggestion.suggested_by_user_id !== user.id && !access.isHost && !access.isGroupAdmin) {
        return errorResponse('Not authorized to remove this suggestion', 403)
      }

      await supabase.from('event_game_suggestions').delete().eq('id', suggestionId)
      return new Response(null, { status: 204, headers: getCorsHeaders(req) })
    }

    // Approve a suggestion (host only) - converts to confirmed event_game
    if (action === 'approve') {
      if (!suggestionId) return errorResponse('Suggestion ID required', 400)

      const { data: suggestion } = await supabase
        .from('event_game_suggestions')
        .select('*')
        .eq('id', suggestionId)
        .single()

      if (!suggestion) return errorResponse('Suggestion not found (may have already been approved)', 404)

      const access = await verifyEventAccess(suggestion.event_id)
      if (access.error) return access.error
      if (!access.isHost && !access.isGroupAdmin) return errorResponse('Only the host or group admins can approve suggestions', 403)

      // Insert as confirmed event game
      const { data: eventGame, error: insertError } = await supabase
        .from('event_games')
        .insert({
          event_id: suggestion.event_id,
          bgg_id: suggestion.bgg_id,
          game_name: suggestion.game_name,
          thumbnail_url: suggestion.thumbnail_url,
          min_players: suggestion.min_players,
          max_players: suggestion.max_players,
          playing_time: suggestion.playing_time,
          is_primary: false,
          is_alternative: false,
          added_by_user_id: suggestion.suggested_by_user_id,
        })
        .select()
        .single()

      if (insertError) return errorResponse(insertError.message, 500)

      // Delete the suggestion (cascade deletes votes)
      await supabase.from('event_game_suggestions').delete().eq('id', suggestionId)

      return jsonResponse(toEventGame(eventGame), 201)
    }

    return errorResponse('Invalid action', 400)
  }

  // POST - Add game to event (no action = direct add, existing behavior)
  if (req.method === 'POST') {
    const body = await req.json()
    const eventId = body.eventId

    if (!eventId) {
      return errorResponse('Event ID required', 400)
    }

    if (!body.gameName?.trim()) {
      return errorResponse('Game name required', 400)
    }

    // Verify user is host or registered for the event
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!event) {
      return errorResponse('Event not found', 404)
    }

    const isHost = event.host_user_id === user.id

    // Check if registered
    const { data: registration } = await supabase
      .from('event_registrations')
      .select('id')
      .eq('event_id', eventId)
      .eq('user_id', user.id)
      .single()

    if (!isHost && !registration) {
      return errorResponse('Only host or registered users can add games', 403)
    }

    // If setting as primary, unset other primaries first
    if (body.isPrimary) {
      await supabase
        .from('event_games')
        .update({ is_primary: false })
        .eq('event_id', eventId)
        .eq('is_primary', true)
    }

    const { data, error } = await supabase
      .from('event_games')
      .insert({
        event_id: eventId,
        bgg_id: body.bggId || null,
        game_name: body.gameName.trim(),
        thumbnail_url: body.thumbnailUrl || null,
        min_players: body.minPlayers || null,
        max_players: body.maxPlayers || null,
        playing_time: body.playingTime || null,
        is_primary: body.isPrimary || false,
        is_alternative: body.isAlternative || false,
        added_by_user_id: user.id,
      })
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventGame(data), 201)
  }

  // PUT - Update game in event
  if (req.method === 'PUT') {
    if (!gameId) {
      return errorResponse('Game ID required', 400)
    }

    // Get the game and verify permissions
    const { data: game } = await supabase
      .from('event_games')
      .select('event_id, added_by_user_id')
      .eq('id', gameId)
      .single()

    if (!game) {
      return errorResponse('Game not found', 404)
    }

    // Verify user is host or added the game
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', game.event_id)
      .single()

    const isHost = event?.host_user_id === user.id
    const isAdder = game.added_by_user_id === user.id

    if (!isHost && !isAdder) {
      return errorResponse('Not authorized to update this game', 403)
    }

    const body = await req.json()
    const updates: Record<string, unknown> = {}

    if (body.gameName !== undefined) updates.game_name = body.gameName.trim()
    if (body.isPrimary !== undefined) {
      updates.is_primary = body.isPrimary
      // If setting as primary, unset other primaries
      if (body.isPrimary) {
        await supabase
          .from('event_games')
          .update({ is_primary: false })
          .eq('event_id', game.event_id)
          .eq('is_primary', true)
          .neq('id', gameId)
      }
    }
    if (body.isAlternative !== undefined) updates.is_alternative = body.isAlternative

    const { data, error } = await supabase
      .from('event_games')
      .update(updates)
      .eq('id', gameId)
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventGame(data))
  }

  // DELETE - Remove game from event
  if (req.method === 'DELETE') {
    if (!gameId) {
      return errorResponse('Game ID required', 400)
    }

    // Get the game and verify permissions
    const { data: game } = await supabase
      .from('event_games')
      .select('event_id, added_by_user_id')
      .eq('id', gameId)
      .single()

    if (!game) {
      return errorResponse('Game not found', 404)
    }

    // Verify user is host or added the game
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', game.event_id)
      .single()

    const isHost = event?.host_user_id === user.id
    const isAdder = game.added_by_user_id === user.id

    if (!isHost && !isAdder) {
      return errorResponse('Not authorized to remove this game', 403)
    }

    const { error } = await supabase
      .from('event_games')
      .delete()
      .eq('id', gameId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  return errorResponse('Method not allowed', 405)
})
