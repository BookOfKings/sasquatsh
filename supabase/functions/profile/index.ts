import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Transform database row to UserProfile
function toUserProfile(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    firebaseUid: row.firebase_uid as string,
    email: row.email as string,
    displayName: row.display_name as string | null,
    avatarUrl: row.avatar_url as string | null,
    maxTravelMiles: row.max_travel_miles as number | null,
    homeCity: row.home_city as string | null,
    homeState: row.home_state as string | null,
    homePostalCode: row.home_postal_code as string | null,
    bio: row.bio as string | null,
    favoriteGames: row.favorite_games as string[] | null,
    preferredGameTypes: row.preferred_game_types as string[] | null,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  }
}

// Transform for public profile (limited info)
function toPublicProfile(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    displayName: row.display_name as string | null,
    avatarUrl: row.avatar_url as string | null,
    homeCity: row.home_city as string | null,
    homeState: row.home_state as string | null,
    bio: row.bio as string | null,
    favoriteGames: row.favorite_games as string[] | null,
    preferredGameTypes: row.preferred_game_types as string[] | null,
    createdAt: row.created_at as string,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const userId = url.searchParams.get('id')

  // GET public profile by ID (no auth required)
  if (req.method === 'GET' && userId) {
    const { data, error } = await supabase
      .from('users')
      .select('*')
      .eq('id', userId)
      .single()

    if (error || !data) {
      return errorResponse('User not found', 404)
    }

    return jsonResponse(toPublicProfile(data))
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
    .select('*')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // GET - Get current user's full profile
  if (req.method === 'GET') {
    // Get user's group memberships
    const { data: memberships } = await supabase
      .from('group_memberships')
      .select(`
        role, joined_at,
        group:groups(id, name, slug, logo_url, group_type)
      `)
      .eq('user_id', user.id)

    // Get user's upcoming events (registered)
    const { data: registrations } = await supabase
      .from('event_registrations')
      .select(`
        status,
        event:events(id, title, event_date, start_time, city, state)
      `)
      .eq('user_id', user.id)
      .in('status', ['pending', 'confirmed'])
      .gte('events.event_date', new Date().toISOString().split('T')[0])
      .order('events(event_date)', { ascending: true })
      .limit(5)

    // Get user's hosted events count
    const { count: hostedCount } = await supabase
      .from('events')
      .select('*', { count: 'exact', head: true })
      .eq('host_user_id', user.id)

    // Get user's past events count
    const { count: attendedCount } = await supabase
      .from('event_registrations')
      .select('*', { count: 'exact', head: true })
      .eq('user_id', user.id)
      .eq('status', 'confirmed')

    const profile = toUserProfile(user)

    return jsonResponse({
      ...profile,
      groups: memberships?.map(m => ({
        role: m.role,
        joinedAt: m.joined_at,
        group: m.group ? {
          id: (m.group as Record<string, unknown>).id,
          name: (m.group as Record<string, unknown>).name,
          slug: (m.group as Record<string, unknown>).slug,
          logoUrl: (m.group as Record<string, unknown>).logo_url,
          groupType: (m.group as Record<string, unknown>).group_type,
        } : null,
      })) ?? [],
      upcomingEvents: registrations?.map(r => ({
        status: r.status,
        event: r.event ? {
          id: (r.event as Record<string, unknown>).id,
          title: (r.event as Record<string, unknown>).title,
          eventDate: (r.event as Record<string, unknown>).event_date,
          startTime: (r.event as Record<string, unknown>).start_time,
          city: (r.event as Record<string, unknown>).city,
          state: (r.event as Record<string, unknown>).state,
        } : null,
      })).filter(r => r.event !== null) ?? [],
      stats: {
        hostedCount: hostedCount ?? 0,
        attendedCount: attendedCount ?? 0,
        groupCount: memberships?.length ?? 0,
      },
    })
  }

  // PUT - Update current user's profile
  if (req.method === 'PUT') {
    const body = await req.json()

    const updates: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    // Only update fields that are provided
    if (body.displayName !== undefined) updates.display_name = body.displayName?.trim() || null
    if (body.avatarUrl !== undefined) updates.avatar_url = body.avatarUrl || null
    if (body.maxTravelMiles !== undefined) updates.max_travel_miles = body.maxTravelMiles
    if (body.homeCity !== undefined) updates.home_city = body.homeCity?.trim() || null
    if (body.homeState !== undefined) updates.home_state = body.homeState?.trim() || null
    if (body.homePostalCode !== undefined) updates.home_postal_code = body.homePostalCode?.trim() || null
    if (body.bio !== undefined) updates.bio = body.bio?.trim() || null
    if (body.favoriteGames !== undefined) updates.favorite_games = body.favoriteGames || null
    if (body.preferredGameTypes !== undefined) updates.preferred_game_types = body.preferredGameTypes || null

    const { data, error } = await supabase
      .from('users')
      .update(updates)
      .eq('id', user.id)
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toUserProfile(data))
  }

  return errorResponse('Method not allowed', 405)
})
