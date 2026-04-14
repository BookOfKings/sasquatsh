import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)

  // GET /collections?userId={id} - Get a user's collection (public)
  // GET /collections - Get current user's collection (authenticated)
  // GET /collections?action=top-games - Get top 50 BGG games for browsing
  if (req.method === 'GET') {
    const action = url.searchParams.get('action')

    // Top games for browsing/adding
    if (action === 'top-games') {
      const { data, error } = await supabase
        .from('bgg_games_cache')
        .select('bgg_id, name, thumbnail_url, image_url, min_players, max_players, playing_time, year_published, bgg_rank, average_rating')
        .not('bgg_rank', 'is', null)
        .order('bgg_rank', { ascending: true })
        .limit(50)

      if (error) {
        console.error('Database error:', error)
        return errorResponse('Failed to fetch top games', 500)
      }

      return jsonResponse({ games: data })
    }

    // Public collection view by userId
    const userId = url.searchParams.get('userId')
    if (userId) {
      // Check visibility setting
      const { data: owner } = await supabase
        .from('users')
        .select('collection_visibility')
        .eq('id', userId)
        .single()

      if (!owner) return errorResponse('User not found', 404)
      if (owner.collection_visibility !== 'public') {
        return errorResponse('This collection is private', 403)
      }

      const { data, error } = await supabase
        .from('user_game_collections')
        .select('*')
        .eq('user_id', userId)
        .order('game_name', { ascending: true })

      if (error) {
        console.error('Database error:', error)
        return errorResponse('Failed to fetch collection', 500)
      }

      return jsonResponse({ games: data })
    }

    // Authenticated: get own collection
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const { data, error } = await supabase
      .from('user_game_collections')
      .select('*')
      .eq('user_id', user.id)
      .order('game_name', { ascending: true })

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to fetch collection', 500)
    }

    return jsonResponse({ games: data })
  }

  // POST /collections - Add game(s) to collection
  if (req.method === 'POST') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { games } = body

    if (!games || !Array.isArray(games) || games.length === 0) {
      return errorResponse('games array is required', 400)
    }

    if (games.length > 100) {
      return errorResponse('Cannot add more than 100 games at once', 400)
    }

    const rows = games.map((g: any) => ({
      user_id: user.id,
      bgg_id: g.bggId || g.bgg_id,
      game_name: g.gameName || g.game_name || g.name,
      thumbnail_url: g.thumbnailUrl || g.thumbnail_url,
      image_url: g.imageUrl || g.image_url,
      min_players: g.minPlayers || g.min_players,
      max_players: g.maxPlayers || g.max_players,
      playing_time: g.playingTime || g.playing_time,
      year_published: g.yearPublished || g.year_published,
      bgg_rank: g.bggRank || g.bgg_rank,
      average_rating: g.averageRating || g.average_rating,
    }))

    const { data, error } = await supabase
      .from('user_game_collections')
      .upsert(rows, { onConflict: 'user_id,bgg_id' })
      .select()

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to add games', 500)
    }

    return jsonResponse({ games: data, added: data?.length ?? 0 }, 201)
  }

  // DELETE /collections?bggId={id} - Remove game from collection
  if (req.method === 'DELETE') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const bggId = url.searchParams.get('bggId')
    if (!bggId) return errorResponse('bggId parameter required', 400)

    const { error } = await supabase
      .from('user_game_collections')
      .delete()
      .eq('user_id', user.id)
      .eq('bgg_id', Number(bggId))

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to remove game', 500)
    }

    return jsonResponse({ removed: true })
  }

  return errorResponse('Method not allowed', 405)
})
