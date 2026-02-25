import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

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
    return new Response(null, { headers: getCorsHeaders() })
  }

  // Get authorization token
  const authHeader = req.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) {
    return errorResponse('Unauthorized', 401)
  }

  const token = authHeader.slice(7)
  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid token', 401)
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

  // POST - Add game to event
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

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})
