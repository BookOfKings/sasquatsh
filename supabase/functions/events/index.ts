import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

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
  const eventId = url.searchParams.get('id')
  const type = url.searchParams.get('type')

  // GET - List events
  if (req.method === 'GET') {
    if (type === 'hosted') {
      // Get events hosted by user
      const { data, error } = await supabase
        .from('events')
        .select(`
          id, title, game_title, game_category, event_date, start_time,
          duration_minutes, city, state, difficulty_level, max_players,
          is_public, is_charity_event, status,
          host:users!host_user_id(id, display_name, avatar_url),
          registrations:event_registrations(count)
        `)
        .eq('host_user_id', user.id)
        .order('event_date', { ascending: true })

      if (error) return errorResponse(error.message, 500)
      return jsonResponse(data.map(transformEventSummary))
    }

    if (type === 'registered') {
      // Get events user is registered for
      const { data, error } = await supabase
        .from('event_registrations')
        .select(`
          event:events(
            id, title, game_title, game_category, event_date, start_time,
            duration_minutes, city, state, difficulty_level, max_players,
            is_public, is_charity_event, status,
            host:users!host_user_id(id, display_name, avatar_url),
            registrations:event_registrations(count)
          )
        `)
        .eq('user_id', user.id)
        .in('status', ['pending', 'confirmed'])

      if (error) return errorResponse(error.message, 500)
      return jsonResponse(
        data
          .map((r) => r.event)
          .filter((e): e is NonNullable<typeof e> => e !== null)
          .map(transformEventSummary)
      )
    }

    return errorResponse('Invalid type parameter', 400)
  }

  // POST - Create event
  if (req.method === 'POST') {
    const body = await req.json()

    const { data, error } = await supabase
      .from('events')
      .insert({
        host_user_id: user.id,
        title: body.title,
        description: body.description,
        game_title: body.gameTitle,
        game_category: body.gameCategory,
        event_date: body.eventDate,
        start_time: body.startTime,
        duration_minutes: body.durationMinutes ?? 120,
        setup_minutes: body.setupMinutes ?? 15,
        address_line1: body.addressLine1,
        city: body.city,
        state: body.state,
        postal_code: body.postalCode,
        location_details: body.locationDetails,
        difficulty_level: body.difficultyLevel,
        max_players: body.maxPlayers ?? 4,
        is_public: body.isPublic ?? true,
        is_charity_event: body.isCharityEvent ?? false,
        status: body.status ?? 'draft',
      })
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url)
      `)
      .single()

    if (error) return errorResponse(error.message, 500)
    return jsonResponse(transformEvent(data))
  }

  // PUT - Update event
  if (req.method === 'PUT') {
    if (!eventId) return errorResponse('Event ID required', 400)

    // Verify ownership
    const { data: existing } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!existing || existing.host_user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    const body = await req.json()

    const { data, error } = await supabase
      .from('events')
      .update({
        title: body.title,
        description: body.description,
        game_title: body.gameTitle,
        game_category: body.gameCategory,
        event_date: body.eventDate,
        start_time: body.startTime,
        duration_minutes: body.durationMinutes,
        setup_minutes: body.setupMinutes,
        address_line1: body.addressLine1,
        city: body.city,
        state: body.state,
        postal_code: body.postalCode,
        location_details: body.locationDetails,
        difficulty_level: body.difficultyLevel,
        max_players: body.maxPlayers,
        is_public: body.isPublic,
        is_charity_event: body.isCharityEvent,
        status: body.status,
        updated_at: new Date().toISOString(),
      })
      .eq('id', eventId)
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url),
        registrations:event_registrations(
          id, user_id, status, registered_at,
          user:users(id, display_name, avatar_url)
        ),
        items:event_items(
          id, item_name, item_category, quantity_needed,
          claimed_by_user_id, claimed_at,
          claimed_by:users!claimed_by_user_id(display_name)
        ),
        games:event_games(
          id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time,
          is_primary, is_alternative
        )
      `)
      .single()

    if (error) return errorResponse(error.message, 500)
    return jsonResponse(transformEvent(data))
  }

  // DELETE - Delete event
  if (req.method === 'DELETE') {
    if (!eventId) return errorResponse('Event ID required', 400)

    // Verify ownership
    const { data: existing } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!existing || existing.host_user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    const { error } = await supabase
      .from('events')
      .delete()
      .eq('id', eventId)

    if (error) return errorResponse(error.message, 500)
    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})

function transformEventSummary(row: Record<string, unknown>) {
  return {
    id: row.id,
    title: row.title,
    gameTitle: row.game_title,
    gameCategory: row.game_category,
    eventDate: row.event_date,
    startTime: row.start_time,
    durationMinutes: row.duration_minutes,
    city: row.city,
    state: row.state,
    difficultyLevel: row.difficulty_level,
    maxPlayers: row.max_players,
    confirmedCount: (row.registrations as { count: number }[])?.[0]?.count ?? 0,
    isPublic: row.is_public,
    isCharityEvent: row.is_charity_event,
    status: row.status,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id,
          displayName: (row.host as Record<string, unknown>).display_name,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url,
        }
      : null,
  }
}

function transformEvent(row: Record<string, unknown>) {
  return {
    id: row.id,
    hostUserId: row.host_user_id,
    title: row.title,
    description: row.description,
    gameTitle: row.game_title,
    gameCategory: row.game_category,
    eventDate: row.event_date,
    startTime: row.start_time,
    durationMinutes: row.duration_minutes,
    setupMinutes: row.setup_minutes,
    addressLine1: row.address_line1,
    city: row.city,
    state: row.state,
    postalCode: row.postal_code,
    locationDetails: row.location_details,
    difficultyLevel: row.difficulty_level,
    maxPlayers: row.max_players,
    confirmedCount: Array.isArray(row.registrations) ? row.registrations.length : 0,
    isPublic: row.is_public,
    isCharityEvent: row.is_charity_event,
    status: row.status,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id,
          displayName: (row.host as Record<string, unknown>).display_name,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url,
        }
      : null,
    registrations: Array.isArray(row.registrations)
      ? (row.registrations as Record<string, unknown>[]).map((r) => ({
          id: r.id,
          userId: r.user_id,
          status: r.status,
          registeredAt: r.registered_at,
          user: r.user
            ? {
                id: (r.user as Record<string, unknown>).id,
                displayName: (r.user as Record<string, unknown>).display_name,
                avatarUrl: (r.user as Record<string, unknown>).avatar_url,
              }
            : null,
        }))
      : null,
    items: Array.isArray(row.items)
      ? (row.items as Record<string, unknown>[]).map((i) => ({
          id: i.id,
          itemName: i.item_name,
          itemCategory: i.item_category,
          quantityNeeded: i.quantity_needed,
          claimedByUserId: i.claimed_by_user_id,
          claimedByName: i.claimed_by
            ? ((i.claimed_by as Record<string, unknown>).display_name as string)
            : null,
          claimedAt: i.claimed_at,
        }))
      : null,
    games: Array.isArray(row.games)
      ? (row.games as Record<string, unknown>[]).map((g) => ({
          id: g.id,
          bggId: g.bgg_id,
          gameName: g.game_name,
          thumbnailUrl: g.thumbnail_url,
          minPlayers: g.min_players,
          maxPlayers: g.max_players,
          playingTime: g.playing_time,
          isPrimary: g.is_primary,
          isAlternative: g.is_alternative,
        }))
      : null,
    createdAt: row.created_at,
  }
}
