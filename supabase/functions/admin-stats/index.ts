import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders, verifyFirebaseToken, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const bggApiToken = Deno.env.get('BGG_API_TOKEN')

interface ServiceHealth {
  name: string
  status: 'healthy' | 'degraded' | 'unhealthy'
  latencyMs?: number
  message?: string
}

interface AdminStats {
  users: {
    total: number
    last7Days: number
    last30Days: number
  }
  groups: {
    total: number
    public: number
    private: number
  }
  events: {
    total: number
    upcoming: number
  }
  planningSessions: {
    total: number
    open: number
  }
  playerRequests: {
    total: number
    active: number
  }
  bggCache: {
    total: number
  }
}

async function getStats(supabase: ReturnType<typeof createClient>): Promise<AdminStats> {
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString()
  const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString()

  // Run all queries in parallel for performance
  const [
    usersTotal,
    usersLast7,
    usersLast30,
    groupsTotal,
    groupsPublic,
    eventsTotal,
    eventsUpcoming,
    planningTotal,
    planningOpen,
    requestsTotal,
    requestsActive,
    bggCacheTotal,
  ] = await Promise.all([
    // Users
    supabase.from('users').select('*', { count: 'exact', head: true }),
    supabase.from('users').select('*', { count: 'exact', head: true }).gte('created_at', sevenDaysAgo),
    supabase.from('users').select('*', { count: 'exact', head: true }).gte('created_at', thirtyDaysAgo),

    // Groups
    supabase.from('groups').select('*', { count: 'exact', head: true }),
    supabase.from('groups').select('*', { count: 'exact', head: true }).eq('is_public', true),

    // Events
    supabase.from('events').select('*', { count: 'exact', head: true }),
    supabase.from('events').select('*', { count: 'exact', head: true }).gte('date', now.toISOString().split('T')[0]),

    // Planning Sessions
    supabase.from('planning_sessions').select('*', { count: 'exact', head: true }),
    supabase.from('planning_sessions').select('*', { count: 'exact', head: true }).eq('status', 'open'),

    // Player Requests (LFP)
    supabase.from('player_requests').select('*', { count: 'exact', head: true }),
    supabase.from('player_requests').select('*', { count: 'exact', head: true }).eq('status', 'open'),

    // BGG Cache
    supabase.from('bgg_games_cache').select('*', { count: 'exact', head: true }),
  ])

  return {
    users: {
      total: usersTotal.count ?? 0,
      last7Days: usersLast7.count ?? 0,
      last30Days: usersLast30.count ?? 0,
    },
    groups: {
      total: groupsTotal.count ?? 0,
      public: groupsPublic.count ?? 0,
      private: (groupsTotal.count ?? 0) - (groupsPublic.count ?? 0),
    },
    events: {
      total: eventsTotal.count ?? 0,
      upcoming: eventsUpcoming.count ?? 0,
    },
    planningSessions: {
      total: planningTotal.count ?? 0,
      open: planningOpen.count ?? 0,
    },
    playerRequests: {
      total: requestsTotal.count ?? 0,
      active: requestsActive.count ?? 0,
    },
    bggCache: {
      total: bggCacheTotal.count ?? 0,
    },
  }
}

async function checkServiceHealth(): Promise<ServiceHealth[]> {
  const results: ServiceHealth[] = []

  // 1. Database Health - Simple query
  const dbStart = Date.now()
  try {
    const supabase = createClient(supabaseUrl, supabaseServiceKey)
    const { error } = await supabase.from('users').select('id', { head: true }).limit(1)
    const latency = Date.now() - dbStart

    if (error) {
      results.push({
        name: 'Database',
        status: 'unhealthy',
        latencyMs: latency,
        message: error.message,
      })
    } else {
      results.push({
        name: 'Database',
        status: latency > 1000 ? 'degraded' : 'healthy',
        latencyMs: latency,
        message: latency > 1000 ? 'High latency detected' : undefined,
      })
    }
  } catch (err) {
    results.push({
      name: 'Database',
      status: 'unhealthy',
      latencyMs: Date.now() - dbStart,
      message: err instanceof Error ? err.message : 'Connection failed',
    })
  }

  // 2. BGG API Health
  const bggStart = Date.now()
  try {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 10000)

    const headers: Record<string, string> = {}
    if (bggApiToken) {
      headers['Authorization'] = `Bearer ${bggApiToken}`
    }

    // Use hot list as a lightweight health check
    const response = await fetch('https://boardgamegeek.com/xmlapi2/hot?type=boardgame', {
      signal: controller.signal,
      headers,
    })
    clearTimeout(timeoutId)

    const latency = Date.now() - bggStart

    if (response.ok) {
      results.push({
        name: 'BGG API',
        status: latency > 5000 ? 'degraded' : 'healthy',
        latencyMs: latency,
        message: bggApiToken ? 'API token configured' : 'No API token (may be rate limited)',
      })
    } else if (response.status === 202) {
      results.push({
        name: 'BGG API',
        status: 'degraded',
        latencyMs: latency,
        message: 'API queued (202) - retry needed',
      })
    } else {
      results.push({
        name: 'BGG API',
        status: 'unhealthy',
        latencyMs: latency,
        message: `HTTP ${response.status}`,
      })
    }
  } catch (err) {
    results.push({
      name: 'BGG API',
      status: 'unhealthy',
      latencyMs: Date.now() - bggStart,
      message: err instanceof Error ? err.message : 'Connection failed',
    })
  }

  // 3. Firebase Auth (check Google public keys endpoint)
  const firebaseStart = Date.now()
  try {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 5000)

    const response = await fetch(
      'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com',
      { signal: controller.signal }
    )
    clearTimeout(timeoutId)

    const latency = Date.now() - firebaseStart

    if (response.ok) {
      results.push({
        name: 'Firebase Auth',
        status: latency > 2000 ? 'degraded' : 'healthy',
        latencyMs: latency,
      })
    } else {
      results.push({
        name: 'Firebase Auth',
        status: 'unhealthy',
        latencyMs: latency,
        message: `HTTP ${response.status}`,
      })
    }
  } catch (err) {
    results.push({
      name: 'Firebase Auth',
      status: 'unhealthy',
      latencyMs: Date.now() - firebaseStart,
      message: err instanceof Error ? err.message : 'Connection failed',
    })
  }

  // 4. Supabase Edge Functions (self-check)
  results.push({
    name: 'Edge Functions',
    status: 'healthy',
    message: 'This endpoint is responding',
  })

  return results
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  // Verify admin access
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Authentication required', 401)
  }

  const decoded = await verifyFirebaseToken(token)
  if (!decoded) {
    return errorResponse('Invalid token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Check if user is admin and get admin user info
  const { data: adminUser } = await supabase
    .from('users')
    .select('id, is_admin')
    .eq('firebase_uid', decoded.uid)
    .single()

  if (!adminUser?.is_admin) {
    return errorResponse('Admin access required', 403)
  }

  const url = new URL(req.url)
  const action = url.searchParams.get('action')

  // GET requests - stats, health, list users/groups
  if (req.method === 'GET') {
    // Health check only
    if (action === 'health') {
      const health = await checkServiceHealth()
      return jsonResponse({ services: health })
    }

    // Stats only
    if (action === 'stats') {
      const stats = await getStats(supabase)
      return jsonResponse({ stats })
    }

    // List users
    if (action === 'users') {
      const search = url.searchParams.get('search') || ''
      const showSuspended = url.searchParams.get('suspended') === 'true'
      const page = parseInt(url.searchParams.get('page') || '1')
      const limit = Math.min(parseInt(url.searchParams.get('limit') || '50'), 100)
      const offset = (page - 1) * limit

      let query = supabase
        .from('users')
        .select('id, email, username, display_name, avatar_url, is_admin, is_suspended, suspension_reason, suspended_at, created_at', { count: 'exact' })

      if (search) {
        query = query.or(`email.ilike.%${search}%,username.ilike.%${search}%,display_name.ilike.%${search}%`)
      }
      if (showSuspended) {
        query = query.eq('is_suspended', true)
      }

      const { data: users, count, error } = await query
        .order('created_at', { ascending: false })
        .range(offset, offset + limit - 1)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        users: users?.map(u => ({
          id: u.id,
          email: u.email,
          username: u.username,
          displayName: u.display_name,
          avatarUrl: u.avatar_url,
          isAdmin: u.is_admin,
          isSuspended: u.is_suspended,
          suspensionReason: u.suspension_reason,
          suspendedAt: u.suspended_at,
          createdAt: u.created_at,
        })),
        total: count ?? 0,
        page,
        limit,
      })
    }

    // List groups
    if (action === 'groups') {
      const search = url.searchParams.get('search') || ''
      const page = parseInt(url.searchParams.get('page') || '1')
      const limit = Math.min(parseInt(url.searchParams.get('limit') || '50'), 100)
      const offset = (page - 1) * limit

      let query = supabase
        .from('groups')
        .select(`
          id, name, slug, description, logo_url, is_public, group_type, created_at,
          memberships:group_memberships(count)
        `, { count: 'exact' })

      if (search) {
        query = query.or(`name.ilike.%${search}%,slug.ilike.%${search}%`)
      }

      const { data: groups, count, error } = await query
        .order('created_at', { ascending: false })
        .range(offset, offset + limit - 1)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        groups: groups?.map(g => ({
          id: g.id,
          name: g.name,
          slug: g.slug,
          description: g.description,
          logoUrl: g.logo_url,
          isPublic: g.is_public,
          groupType: g.group_type,
          memberCount: (g.memberships as { count: number }[])?.[0]?.count ?? 0,
          createdAt: g.created_at,
        })),
        total: count ?? 0,
        page,
        limit,
      })
    }

    // Default: return stats and health
    const [stats, health] = await Promise.all([
      getStats(supabase),
      checkServiceHealth(),
    ])

    return jsonResponse({
      stats,
      services: health,
    })
  }

  // POST requests - management actions
  if (req.method === 'POST') {
    const body = await req.json().catch(() => ({}))

    // Suspend user
    if (action === 'suspend-user') {
      const userId = body.userId as string
      const reason = body.reason as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      // Can't suspend yourself
      if (userId === adminUser.id) {
        return errorResponse('Cannot suspend yourself', 400)
      }

      // Can't suspend other admins
      const { data: targetUser } = await supabase
        .from('users')
        .select('is_admin')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      if (targetUser.is_admin) {
        return errorResponse('Cannot suspend admin users', 400)
      }

      const { error } = await supabase
        .from('users')
        .update({
          is_suspended: true,
          suspension_reason: reason || null,
          suspended_at: new Date().toISOString(),
          suspended_by_user_id: adminUser.id,
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'User suspended' })
    }

    // Unsuspend user
    if (action === 'unsuspend-user') {
      const userId = body.userId as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      const { error } = await supabase
        .from('users')
        .update({
          is_suspended: false,
          suspension_reason: null,
          suspended_at: null,
          suspended_by_user_id: null,
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'User unsuspended' })
    }

    // Delete group
    if (action === 'delete-group') {
      const groupId = body.groupId as string

      if (!groupId) {
        return errorResponse('groupId required', 400)
      }

      const { data: group } = await supabase
        .from('groups')
        .select('id, name')
        .eq('id', groupId)
        .single()

      if (!group) {
        return errorResponse('Group not found', 404)
      }

      const { error } = await supabase
        .from('groups')
        .delete()
        .eq('id', groupId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: `Group "${group.name}" deleted` })
    }

    return errorResponse('Unknown action', 400)
  }

  return errorResponse('Method not allowed', 405)
})
