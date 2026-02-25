import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Transform database row to PlayerRequest
function toPlayerRequest(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    userId: row.user_id as string,
    title: row.title as string,
    description: row.description as string | null,
    gamePreferences: row.game_preferences as string | null,
    city: row.city as string | null,
    state: row.state as string | null,
    availableDays: row.available_days as string | null,
    playerCountNeeded: row.player_count_needed as number,
    isActive: row.is_active as boolean,
    createdAt: row.created_at as string,
    expiresAt: row.expires_at as string | null,
    user: row.user ? {
      id: (row.user as Record<string, unknown>).id as string,
      displayName: (row.user as Record<string, unknown>).display_name as string | null,
      avatarUrl: (row.user as Record<string, unknown>).avatar_url as string | null,
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

  // GET - List player requests (public, no auth required)
  if (req.method === 'GET' && !requestId) {
    const city = url.searchParams.get('city')
    const state = url.searchParams.get('state')

    let query = supabase
      .from('player_requests')
      .select(`
        *,
        user:users(id, display_name, avatar_url)
      `)
      .eq('is_active', true)
      .or('expires_at.is.null,expires_at.gt.' + new Date().toISOString())
      .order('created_at', { ascending: false })
      .limit(50)

    if (city) {
      query = query.ilike('city', `%${city}%`)
    }
    if (state) {
      query = query.eq('state', state)
    }

    const { data, error } = await query

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toPlayerRequest))
  }

  // All other operations require authentication
  const authHeader = req.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) {
    return errorResponse('Unauthorized', 401)
  }

  const token = authHeader.slice(7)
  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid token', 401)
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

  // GET - Get my requests
  if (req.method === 'GET' && requestId === 'mine') {
    const { data, error } = await supabase
      .from('player_requests')
      .select(`
        *,
        user:users(id, display_name, avatar_url)
      `)
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toPlayerRequest))
  }

  // POST - Create player request
  if (req.method === 'POST') {
    const body = await req.json()

    if (!body.title?.trim()) {
      return errorResponse('Title is required', 400)
    }

    // Calculate expiry (default 30 days)
    const expiresAt = new Date()
    expiresAt.setDate(expiresAt.getDate() + (body.expiresInDays || 30))

    const { data, error } = await supabase
      .from('player_requests')
      .insert({
        user_id: user.id,
        title: body.title.trim(),
        description: body.description?.trim() || null,
        game_preferences: body.gamePreferences?.trim() || null,
        city: body.city?.trim() || null,
        state: body.state?.trim() || null,
        available_days: body.availableDays?.trim() || null,
        player_count_needed: body.playerCountNeeded || 1,
        expires_at: expiresAt.toISOString(),
      })
      .select(`
        *,
        user:users(id, display_name, avatar_url)
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toPlayerRequest(data), 201)
  }

  // PUT - Update player request
  if (req.method === 'PUT') {
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

    const body = await req.json()
    const updates: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    if (body.title !== undefined) updates.title = body.title.trim()
    if (body.description !== undefined) updates.description = body.description?.trim() || null
    if (body.gamePreferences !== undefined) updates.game_preferences = body.gamePreferences?.trim() || null
    if (body.city !== undefined) updates.city = body.city?.trim() || null
    if (body.state !== undefined) updates.state = body.state?.trim() || null
    if (body.availableDays !== undefined) updates.available_days = body.availableDays?.trim() || null
    if (body.playerCountNeeded !== undefined) updates.player_count_needed = body.playerCountNeeded
    if (body.isActive !== undefined) updates.is_active = body.isActive

    const { data, error } = await supabase
      .from('player_requests')
      .update(updates)
      .eq('id', requestId)
      .select(`
        *,
        user:users(id, display_name, avatar_url)
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
