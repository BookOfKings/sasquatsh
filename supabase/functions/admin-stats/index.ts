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

interface TodaySignup {
  id: string
  username: string
  email: string
  displayName: string | null
  avatarUrl: string | null
  createdAt: string
}

interface AdminStats {
  users: {
    total: number
    last7Days: number
    last30Days: number
    byTier: {
      free: number
      basic: number
      pro: number
    }
    tierBreakdown: {
      basicPaid: number
      basicUpgraded: number
      proPaid: number
      proUpgraded: number
    }
    todaySignups: TodaySignup[]
  }
  revenue: {
    projectedMonthly: number
    basicPaidCount: number
    proPaidCount: number
    basicCount: number
    proCount: number
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
  popularGames: {
    name: string
    bggId: number | null
    count: number
    thumbnailUrl: string | null
  }[]
}

async function getStats(supabase: ReturnType<typeof createClient>): Promise<AdminStats> {
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString()
  const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString()

  // Pricing constants
  const BASIC_PRICE = 7.99
  const PRO_PRICE = 14.99

  try {
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
      supabase.from('groups').select('*', { count: 'exact', head: true }).eq('join_policy', 'open'),

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

    // Get tier counts separately with error handling
    let basicCount = 0
    let proCount = 0
    let basicPaid = 0
    let basicUpgraded = 0
    let proPaid = 0
    let proUpgraded = 0

    try {
      // Basic tier - paid (has subscription_tier=basic, no override)
      const basicPaidResult = await supabase.from('users').select('*', { count: 'exact', head: true })
        .eq('subscription_tier', 'basic')
        .is('subscription_override_tier', null)
      basicPaid = basicPaidResult.count ?? 0

      // Basic tier - upgraded (has override to basic)
      const basicUpgradedResult = await supabase.from('users').select('*', { count: 'exact', head: true })
        .eq('subscription_override_tier', 'basic')
      basicUpgraded = basicUpgradedResult.count ?? 0

      basicCount = basicPaid + basicUpgraded
    } catch (e) {
      console.error('Error counting basic tier users:', e)
    }

    try {
      // Pro tier - paid (has subscription_tier=pro, no override)
      const proPaidResult = await supabase.from('users').select('*', { count: 'exact', head: true })
        .eq('subscription_tier', 'pro')
        .is('subscription_override_tier', null)
      proPaid = proPaidResult.count ?? 0

      // Pro tier - upgraded (has override to pro)
      const proUpgradedResult = await supabase.from('users').select('*', { count: 'exact', head: true })
        .eq('subscription_override_tier', 'pro')
      proUpgraded = proUpgradedResult.count ?? 0

      proCount = proPaid + proUpgraded
    } catch (e) {
      console.error('Error counting pro tier users:', e)
    }

    // Get today's signups (using America/Phoenix timezone - MST, no DST)
    // MST is UTC-7, so midnight MST = 7:00 AM UTC
    const todayStart = new Date(now)
    todayStart.setUTCHours(7, 0, 0, 0)

    // If current UTC time is before 7 AM, we're still in "yesterday" MST, so go back a day
    if (now.getUTCHours() < 7) {
      todayStart.setUTCDate(todayStart.getUTCDate() - 1)
    }

    let todaySignups: TodaySignup[] = []
    try {
      const { data: signups } = await supabase
        .from('users')
        .select('id, username, email, display_name, avatar_url, created_at')
        .gte('created_at', todayStart.toISOString())
        .order('created_at', { ascending: false })

      if (signups) {
        todaySignups = signups.map(u => ({
          id: u.id,
          username: u.username,
          email: u.email,
          displayName: u.display_name,
          avatarUrl: u.avatar_url,
          createdAt: u.created_at,
        }))
      }
    } catch (e) {
      console.error('Error getting today signups:', e)
    }

    // Get popular games separately
    let popularGames: AdminStats['popularGames'] = []
    try {
      const popularGamesResult = await supabase.rpc('get_popular_games', { limit_count: 10 })
      if (popularGamesResult.data && Array.isArray(popularGamesResult.data)) {
        popularGames = popularGamesResult.data.map((g: { game_name: string; bgg_id: number | null; usage_count: number; thumbnail_url: string | null }) => ({
          name: g.game_name,
          bggId: g.bgg_id,
          count: g.usage_count,
          thumbnailUrl: g.thumbnail_url,
        }))
      }
    } catch (e) {
      console.error('Error getting popular games:', e)
    }

    const totalUsers = usersTotal.count ?? 0
    const freeCount = Math.max(0, totalUsers - basicCount - proCount)

    // Calculate projected monthly revenue - only count PAID subscribers, not promoted/override accounts
    const projectedMonthly = (basicPaid * BASIC_PRICE) + (proPaid * PRO_PRICE)

    return {
      users: {
        total: totalUsers,
        last7Days: usersLast7.count ?? 0,
        last30Days: usersLast30.count ?? 0,
        byTier: {
          free: freeCount,
          basic: basicCount,
          pro: proCount,
        },
        tierBreakdown: {
          basicPaid,
          basicUpgraded,
          proPaid,
          proUpgraded,
        },
        todaySignups,
      },
      revenue: {
        projectedMonthly: Math.round(projectedMonthly * 100) / 100,
        basicPaidCount: basicPaid,  // Only actual paying customers
        proPaidCount: proPaid,      // Only actual paying customers
        basicCount,  // Total including promoted
        proCount,    // Total including promoted
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
      popularGames,
    }
  } catch (error) {
    console.error('Error in getStats:', error)
    // Return default stats on error
    return {
      users: {
        total: 0, last7Days: 0, last30Days: 0,
        byTier: { free: 0, basic: 0, pro: 0 },
        tierBreakdown: { basicPaid: 0, basicUpgraded: 0, proPaid: 0, proUpgraded: 0 },
        todaySignups: [],
      },
      revenue: { projectedMonthly: 0, basicPaidCount: 0, proPaidCount: 0, basicCount: 0, proCount: 0 },
      groups: { total: 0, public: 0, private: 0 },
      events: { total: 0, upcoming: 0 },
      planningSessions: { total: 0, open: 0 },
      playerRequests: { total: 0, active: 0 },
      bggCache: { total: 0 },
      popularGames: [],
    }
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
      const showBanned = url.searchParams.get('banned') === 'true'
      const page = parseInt(url.searchParams.get('page') || '1')
      const limit = Math.min(parseInt(url.searchParams.get('limit') || '50'), 100)
      const offset = (page - 1) * limit

      let query = supabase
        .from('users')
        .select(`
          id, email, username, display_name, avatar_url, is_admin, is_founding_member,
          is_suspended, suspension_reason, suspended_at,
          account_status, banned_at, ban_reason,
          subscription_tier, subscription_override_tier, subscription_override_reason,
          subscription_status, subscription_expires_at,
          created_at
        `, { count: 'exact' })

      if (search) {
        query = query.or(`email.ilike.%${search}%,username.ilike.%${search}%,display_name.ilike.%${search}%`)
      }
      if (showSuspended) {
        query = query.eq('is_suspended', true)
      }
      if (showBanned) {
        query = query.eq('account_status', 'banned')
      }

      const { data: users, count, error } = await query
        .order('created_at', { ascending: false })
        .range(offset, offset + limit - 1)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        users: (users || []).map(u => ({
          id: u.id,
          email: u.email,
          username: u.username,
          displayName: u.display_name,
          avatarUrl: u.avatar_url,
          isAdmin: u.is_admin,
          isFoundingMember: u.is_founding_member ?? false,
          isSuspended: u.is_suspended,
          suspensionReason: u.suspension_reason,
          suspendedAt: u.suspended_at,
          accountStatus: u.account_status || 'active',
          bannedAt: u.banned_at,
          banReason: u.ban_reason,
          subscriptionTier: u.subscription_tier || 'free',
          subscriptionOverrideTier: u.subscription_override_tier,
          subscriptionOverrideReason: u.subscription_override_reason,
          subscriptionStatus: u.subscription_status,
          subscriptionExpiresAt: u.subscription_expires_at,
          effectiveTier: u.subscription_override_tier || u.subscription_tier || 'free',
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
          id, name, slug, description, logo_url, join_policy, group_type, created_at,
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
        groups: (groups || []).map(g => ({
          id: g.id,
          name: g.name,
          slug: g.slug,
          description: g.description,
          logoUrl: g.logo_url,
          isPublic: g.join_policy === 'open',
          joinPolicy: g.join_policy,
          groupType: g.group_type,
          memberCount: (g.memberships as { count: number }[])?.[0]?.count ?? 0,
          createdAt: g.created_at,
        })),
        total: count ?? 0,
        page,
        limit,
      })
    }

    // Get group members (admin)
    if (action === 'group-members') {
      const groupId = url.searchParams.get('groupId')
      if (!groupId) {
        return errorResponse('groupId is required', 400)
      }

      const { data: members, error } = await supabase
        .from('group_memberships')
        .select(`
          id, role, joined_at,
          user:user_id(id, username, display_name, email, avatar_url)
        `)
        .eq('group_id', groupId)
        .order('role', { ascending: true })
        .order('joined_at', { ascending: true })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        members: (members || []).map(m => ({
          id: m.id,
          userId: (m.user as { id: string }).id,
          username: (m.user as { username: string }).username,
          displayName: (m.user as { display_name: string | null }).display_name,
          email: (m.user as { email: string }).email,
          avatarUrl: (m.user as { avatar_url: string | null }).avatar_url,
          role: m.role,
          joinedAt: m.joined_at,
        })) || [],
      })
    }

    // List notes
    if (action === 'notes') {
      const category = url.searchParams.get('category') || ''

      let query = supabase
        .from('admin_notes')
        .select(`
          id, title, content, category, is_pinned, is_implemented, created_at, updated_at,
          creator:created_by_user_id(id, username, display_name)
        `)

      if (category) {
        query = query.eq('category', category)
      }

      const { data: notes, error } = await query
        .order('is_pinned', { ascending: false })
        .order('updated_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        notes: (notes || []).map(n => ({
          id: n.id,
          title: n.title,
          content: n.content,
          category: n.category,
          isPinned: n.is_pinned,
          isImplemented: n.is_implemented,
          createdAt: n.created_at,
          updatedAt: n.updated_at,
          createdBy: n.creator ? {
            id: (n.creator as { id: string }).id,
            username: (n.creator as { username: string }).username,
            displayName: (n.creator as { display_name: string }).display_name,
          } : null,
        })),
      })
    }

    // ============ Ads ============

    // List all ads
    if (action === 'ads') {
      const { data: ads, error } = await supabase
        .from('ads')
        .select('*')
        .order('created_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        ads: (ads || []).map(a => ({
          id: a.id,
          name: a.name,
          advertiserName: a.advertiser_name,
          adType: a.ad_type,
          placement: a.placement,
          imageUrl: a.image_url,
          title: a.title,
          description: a.description,
          linkUrl: a.link_url,
          targetCity: a.target_city,
          targetState: a.target_state,
          startDate: a.start_date,
          endDate: a.end_date,
          isActive: a.is_active,
          isHouseAd: a.is_house_ad,
          priority: a.priority,
          createdAt: a.created_at,
          updatedAt: a.updated_at,
        })),
      })
    }

    // Get ad stats
    if (action === 'ad-stats') {
      const { data: stats, error } = await supabase
        .from('ad_stats')
        .select('*')
        .order('impression_count', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        stats: (stats || []).map(s => ({
          id: s.id,
          name: s.name,
          advertiserName: s.advertiser_name,
          isHouseAd: s.is_house_ad,
          isActive: s.is_active,
          startDate: s.start_date,
          endDate: s.end_date,
          impressionCount: s.impression_count,
          clickCount: s.click_count,
          ctrPercent: s.ctr_percent,
        })),
      })
    }

    // List bugs
    if (action === 'bugs') {
      const status = url.searchParams.get('status') || ''
      const priority = url.searchParams.get('priority') || ''

      let query = supabase
        .from('admin_bugs')
        .select(`
          id, title, description, steps_to_reproduce, status, priority, resolved_at, created_at, updated_at,
          reporter:reported_by_user_id(id, username, display_name),
          assignee:assigned_to_user_id(id, username, display_name)
        `)

      if (status) {
        query = query.eq('status', status)
      }
      if (priority) {
        query = query.eq('priority', priority)
      }

      const { data: bugs, error } = await query
        .order('priority', { ascending: true }) // critical first
        .order('created_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        bugs: (bugs || []).map(b => ({
          id: b.id,
          title: b.title,
          description: b.description,
          stepsToReproduce: b.steps_to_reproduce,
          status: b.status,
          priority: b.priority,
          resolvedAt: b.resolved_at,
          createdAt: b.created_at,
          updatedAt: b.updated_at,
          reportedBy: b.reporter ? {
            id: (b.reporter as { id: string }).id,
            username: (b.reporter as { username: string }).username,
            displayName: (b.reporter as { display_name: string }).display_name,
          } : null,
          assignedTo: b.assignee ? {
            id: (b.assignee as { id: string }).id,
            username: (b.assignee as { username: string }).username,
            displayName: (b.assignee as { display_name: string }).display_name,
          } : null,
        })),
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

    // Ban user (permanent)
    if (action === 'ban-user') {
      const userId = body.userId as string
      const reason = body.reason as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      // Can't ban yourself
      if (userId === adminUser.id) {
        return errorResponse('Cannot ban yourself', 400)
      }

      // Can't ban other admins
      const { data: targetUser } = await supabase
        .from('users')
        .select('is_admin, username, email')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      if (targetUser.is_admin) {
        return errorResponse('Cannot ban admin users', 400)
      }

      const { error } = await supabase
        .from('users')
        .update({
          account_status: 'banned',
          banned_at: new Date().toISOString(),
          banned_by_user_id: adminUser.id,
          ban_reason: reason || null,
          // Also suspend them
          is_suspended: true,
          suspension_reason: reason ? `Banned: ${reason}` : 'Permanently banned',
          suspended_at: new Date().toISOString(),
          suspended_by_user_id: adminUser.id,
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Log the event
      await supabase.from('subscription_events').insert({
        user_id: userId,
        event_type: 'account_banned',
        notes: reason ? `Banned by admin: ${reason}` : 'Banned by admin',
        admin_user_id: adminUser.id,
      })

      return jsonResponse({ message: `User ${targetUser.username || targetUser.email} has been banned` })
    }

    // Unban user
    if (action === 'unban-user') {
      const userId = body.userId as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      const { data: targetUser } = await supabase
        .from('users')
        .select('username, email, account_status')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      if (targetUser.account_status !== 'banned') {
        return errorResponse('User is not banned', 400)
      }

      const { error } = await supabase
        .from('users')
        .update({
          account_status: 'active',
          banned_at: null,
          banned_by_user_id: null,
          ban_reason: null,
          // Also unsuspend them
          is_suspended: false,
          suspension_reason: null,
          suspended_at: null,
          suspended_by_user_id: null,
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Log the event
      await supabase.from('subscription_events').insert({
        user_id: userId,
        event_type: 'account_unbanned',
        notes: 'Unbanned by admin',
        admin_user_id: adminUser.id,
      })

      return jsonResponse({ message: `User ${targetUser.username || targetUser.email} has been unbanned` })
    }

    // Set subscription tier override (grant tier without payment)
    if (action === 'set-tier') {
      const userId = body.userId as string
      const tier = body.tier as string
      const reason = body.reason as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      const validTiers = ['free', 'basic', 'pro', 'premium', null]
      if (!validTiers.includes(tier)) {
        return errorResponse('Invalid tier. Must be free, basic, pro, premium, or null to remove override', 400)
      }

      const { data: targetUser } = await supabase
        .from('users')
        .select('username, email, subscription_tier, subscription_override_tier')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      const oldTier = targetUser.subscription_override_tier || targetUser.subscription_tier || 'free'
      const newTier = tier || targetUser.subscription_tier || 'free'

      // If tier is null or 'free', remove the override
      const overrideTier = tier === 'free' || tier === null ? null : tier

      const { error } = await supabase
        .from('users')
        .update({
          subscription_override_tier: overrideTier,
          subscription_override_reason: overrideTier ? (reason || 'Set by admin') : null,
          subscription_override_by_user_id: overrideTier ? adminUser.id : null,
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Log the event
      await supabase.from('subscription_events').insert({
        user_id: userId,
        event_type: 'admin_tier_override',
        old_tier: oldTier,
        new_tier: newTier,
        notes: reason || (overrideTier ? `Admin set tier to ${tier}` : 'Admin removed tier override'),
        admin_user_id: adminUser.id,
      })

      return jsonResponse({
        message: overrideTier
          ? `User ${targetUser.username || targetUser.email} tier set to ${tier}`
          : `User ${targetUser.username || targetUser.email} tier override removed`,
        effectiveTier: overrideTier || targetUser.subscription_tier || 'free',
      })
    }

    // Toggle founding member status
    if (action === 'toggle-founding') {
      const userId = body.userId as string
      const isFoundingMember = body.isFoundingMember as boolean

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      if (typeof isFoundingMember !== 'boolean') {
        return errorResponse('isFoundingMember must be a boolean', 400)
      }

      const { data: targetUser } = await supabase
        .from('users')
        .select('username, email, is_founding_member')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      const { error } = await supabase
        .from('users')
        .update({
          is_founding_member: isFoundingMember,
          updated_at: new Date().toISOString(),
        })
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        message: isFoundingMember
          ? `${targetUser.username || targetUser.email} is now a Founding Member`
          : `${targetUser.username || targetUser.email} is no longer a Founding Member`,
        isFoundingMember,
      })
    }

    // Update user (admin edit)
    if (action === 'update-user') {
      const { userId, displayName, username, isAdmin, isFoundingMember } = body

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      // Can't change your own admin status (to prevent accidental lockout)
      if (userId === adminUser.id && isAdmin !== undefined && isAdmin !== adminUser.is_admin) {
        return errorResponse('Cannot change your own admin status', 400)
      }

      const updates: Record<string, unknown> = { updated_at: new Date().toISOString() }
      if (displayName !== undefined) updates.display_name = displayName?.trim() || null
      if (username !== undefined) {
        const trimmedUsername = username?.trim()
        if (!trimmedUsername || !/^[a-zA-Z][a-zA-Z0-9_]{2,29}$/.test(trimmedUsername)) {
          return errorResponse('Invalid username format (3-30 chars, starts with letter, alphanumeric + underscore)', 400)
        }
        // Check uniqueness
        const { data: existing } = await supabase
          .from('users')
          .select('id')
          .ilike('username', trimmedUsername)
          .neq('id', userId)
          .single()
        if (existing) {
          return errorResponse('Username already taken', 400)
        }
        updates.username = trimmedUsername
      }
      if (isAdmin !== undefined) updates.is_admin = isAdmin
      if (isFoundingMember !== undefined) updates.is_founding_member = isFoundingMember

      const { data: user, error } = await supabase
        .from('users')
        .update(updates)
        .eq('id', userId)
        .select('id, email, username, display_name, is_admin')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ user, message: 'User updated' })
    }

    // Delete user and all their data
    if (action === 'delete-user') {
      const userId = body.userId as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      // Can't delete yourself
      if (userId === adminUser.id) {
        return errorResponse('Cannot delete yourself', 400)
      }

      // Can't delete other admins
      const { data: targetUser } = await supabase
        .from('users')
        .select('id, email, username, is_admin, firebase_uid')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      if (targetUser.is_admin) {
        return errorResponse('Cannot delete admin users', 400)
      }

      // Delete in order to handle foreign key constraints
      // 1. Planning votes and suggestions
      await supabase.from('planning_game_votes').delete().eq('user_id', userId)
      await supabase.from('planning_date_votes').delete().eq('user_id', userId)
      await supabase.from('planning_game_suggestions').delete().eq('suggested_by_user_id', userId)

      // 2. Planning session invitations
      await supabase.from('planning_session_invitations').delete().eq('user_id', userId)

      // 3. Event registrations
      await supabase.from('event_registrations').delete().eq('user_id', userId)

      // 4. Player requests
      await supabase.from('player_requests').delete().eq('user_id', userId)

      // 5. Group memberships
      await supabase.from('group_memberships').delete().eq('user_id', userId)

      // 6. Admin notes - nullify created_by
      await supabase.from('admin_notes').update({ created_by_user_id: adminUser.id }).eq('created_by_user_id', userId)

      // 7. Admin bugs - nullify reporter/assignee (if columns allow null, otherwise reassign)
      await supabase.from('admin_bugs').update({ assigned_to_user_id: null }).eq('assigned_to_user_id', userId)
      // For reported_by, transfer to admin who's deleting
      await supabase.from('admin_bugs').update({ reported_by_user_id: adminUser.id }).eq('reported_by_user_id', userId)

      // 8. Events created by user - transfer ownership to admin
      await supabase.from('events').update({ created_by_user_id: adminUser.id }).eq('created_by_user_id', userId)

      // 9. Planning sessions created by user - transfer ownership
      await supabase.from('planning_sessions').update({ created_by_user_id: adminUser.id }).eq('created_by_user_id', userId)

      // 10. Groups created by user - transfer ownership
      await supabase.from('groups').update({ created_by_user_id: adminUser.id }).eq('created_by_user_id', userId)

      // Finally delete the user
      const { error } = await supabase
        .from('users')
        .delete()
        .eq('id', userId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        message: `User ${targetUser.username || targetUser.email} deleted`,
        deletedUser: {
          id: targetUser.id,
          email: targetUser.email,
          username: targetUser.username,
        }
      })
    }

    // Send password reset email
    if (action === 'send-password-reset') {
      const userId = body.userId as string

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      const { data: targetUser } = await supabase
        .from('users')
        .select('id, email')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      // Use Firebase REST API to send password reset
      const firebaseApiKey = Deno.env.get('FIREBASE_API_KEY')
      if (!firebaseApiKey) {
        return errorResponse('Firebase API key not configured', 500)
      }

      const resetResponse = await fetch(
        `https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${firebaseApiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            requestType: 'PASSWORD_RESET',
            email: targetUser.email,
          }),
        }
      )

      if (!resetResponse.ok) {
        const errorData = await resetResponse.json().catch(() => ({}))
        const errorMessage = errorData?.error?.message || 'Failed to send password reset'
        // Common Firebase errors
        if (errorMessage.includes('EMAIL_NOT_FOUND')) {
          return errorResponse('Email not registered with Firebase (may be OAuth-only user)', 400)
        }
        return errorResponse(errorMessage, 400)
      }

      return jsonResponse({ message: `Password reset email sent to ${targetUser.email}` })
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

    // Add member to group (admin)
    if (action === 'add-group-member') {
      const { groupId, userId, role = 'member' } = body

      if (!groupId || !userId) {
        return errorResponse('groupId and userId are required', 400)
      }

      // Verify group exists
      const { data: group } = await supabase
        .from('groups')
        .select('id, name')
        .eq('id', groupId)
        .single()

      if (!group) {
        return errorResponse('Group not found', 404)
      }

      // Verify user exists
      const { data: targetUser } = await supabase
        .from('users')
        .select('id, username')
        .eq('id', userId)
        .single()

      if (!targetUser) {
        return errorResponse('User not found', 404)
      }

      // Check if already a member
      const { data: existing } = await supabase
        .from('group_memberships')
        .select('id')
        .eq('group_id', groupId)
        .eq('user_id', userId)
        .single()

      if (existing) {
        return errorResponse('User is already a member of this group', 400)
      }

      // Add member
      const { error } = await supabase
        .from('group_memberships')
        .insert({
          group_id: groupId,
          user_id: userId,
          role: role,
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: `User @${targetUser.username} added to group "${group.name}"` })
    }

    // Remove member from group (admin)
    if (action === 'remove-group-member') {
      const { groupId, userId } = body

      if (!groupId || !userId) {
        return errorResponse('groupId and userId are required', 400)
      }

      // Verify group exists
      const { data: group } = await supabase
        .from('groups')
        .select('id, name, created_by_user_id')
        .eq('id', groupId)
        .single()

      if (!group) {
        return errorResponse('Group not found', 404)
      }

      // Verify membership exists
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('id, role, user:user_id(username)')
        .eq('group_id', groupId)
        .eq('user_id', userId)
        .single()

      if (!membership) {
        return errorResponse('User is not a member of this group', 404)
      }

      // Prevent removing owner unless they are also the creator - need to transfer ownership first
      if ((membership.role === 'owner') && group.created_by_user_id === userId) {
        return errorResponse('Cannot remove the group owner. Transfer ownership first.', 400)
      }

      // Remove member
      const { error } = await supabase
        .from('group_memberships')
        .delete()
        .eq('id', membership.id)

      if (error) {
        return errorResponse(error.message, 500)
      }

      const username = (membership.user as { username: string })?.username || 'Unknown'
      return jsonResponse({ message: `User @${username} removed from group "${group.name}"` })
    }

    // Change member role (admin)
    if (action === 'change-member-role') {
      const { groupId, userId, role } = body

      if (!groupId || !userId || !role) {
        return errorResponse('groupId, userId, and role are required', 400)
      }

      if (!['owner', 'admin', 'member'].includes(role)) {
        return errorResponse('Invalid role. Must be owner, admin, or member', 400)
      }

      // Verify membership exists
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('id, role, user:user_id(username)')
        .eq('group_id', groupId)
        .eq('user_id', userId)
        .single()

      if (!membership) {
        return errorResponse('User is not a member of this group', 404)
      }

      // Update role
      const { error } = await supabase
        .from('group_memberships')
        .update({ role })
        .eq('id', membership.id)

      if (error) {
        return errorResponse(error.message, 500)
      }

      const username = (membership.user as { username: string })?.username || 'Unknown'
      return jsonResponse({ message: `User @${username} role changed to ${role}` })
    }

    // ============ Notes ============

    // Create note
    if (action === 'create-note') {
      const { title, content, category } = body

      if (!title?.trim() || !content?.trim()) {
        return errorResponse('Title and content are required', 400)
      }

      const { data: note, error } = await supabase
        .from('admin_notes')
        .insert({
          title: title.trim(),
          content: content.trim(),
          category: category?.trim() || 'general',
          created_by_user_id: adminUser.id,
        })
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ note, message: 'Note created' })
    }

    // Update note
    if (action === 'update-note') {
      const { noteId, title, content, category, isPinned, isImplemented } = body

      if (!noteId) {
        return errorResponse('noteId required', 400)
      }

      const updates: Record<string, unknown> = { updated_at: new Date().toISOString() }
      if (title !== undefined) updates.title = title.trim()
      if (content !== undefined) updates.content = content.trim()
      if (category !== undefined) updates.category = category.trim()
      if (isPinned !== undefined) updates.is_pinned = isPinned
      if (isImplemented !== undefined) updates.is_implemented = isImplemented

      const { data: note, error } = await supabase
        .from('admin_notes')
        .update(updates)
        .eq('id', noteId)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ note, message: 'Note updated' })
    }

    // Delete note
    if (action === 'delete-note') {
      const { noteId } = body

      if (!noteId) {
        return errorResponse('noteId required', 400)
      }

      const { error } = await supabase
        .from('admin_notes')
        .delete()
        .eq('id', noteId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Note deleted' })
    }

    // ============ Bugs ============

    // Create bug
    if (action === 'create-bug') {
      const { title, description, stepsToReproduce, priority } = body

      if (!title?.trim()) {
        return errorResponse('Title is required', 400)
      }

      const { data: bug, error } = await supabase
        .from('admin_bugs')
        .insert({
          title: title.trim(),
          description: description?.trim() || null,
          steps_to_reproduce: stepsToReproduce?.trim() || null,
          priority: priority || 'medium',
          reported_by_user_id: adminUser.id,
        })
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ bug, message: 'Bug reported' })
    }

    // Update bug
    if (action === 'update-bug') {
      const { bugId, title, description, stepsToReproduce, status, priority, assignedToUserId } = body

      if (!bugId) {
        return errorResponse('bugId required', 400)
      }

      const updates: Record<string, unknown> = { updated_at: new Date().toISOString() }
      if (title !== undefined) updates.title = title.trim()
      if (description !== undefined) updates.description = description?.trim() || null
      if (stepsToReproduce !== undefined) updates.steps_to_reproduce = stepsToReproduce?.trim() || null
      if (status !== undefined) {
        updates.status = status
        if (status === 'resolved' || status === 'closed') {
          updates.resolved_at = new Date().toISOString()
        }
      }
      if (priority !== undefined) updates.priority = priority
      if (assignedToUserId !== undefined) updates.assigned_to_user_id = assignedToUserId || null

      const { data: bug, error } = await supabase
        .from('admin_bugs')
        .update(updates)
        .eq('id', bugId)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ bug, message: 'Bug updated' })
    }

    // Delete bug
    if (action === 'delete-bug') {
      const { bugId } = body

      if (!bugId) {
        return errorResponse('bugId required', 400)
      }

      const { error } = await supabase
        .from('admin_bugs')
        .delete()
        .eq('id', bugId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Bug deleted' })
    }

    // ============ Ads ============

    // Create ad
    if (action === 'create-ad') {
      const {
        name, advertiserName, adType, placement, imageUrl, title,
        description, linkUrl, targetCity, targetState, startDate,
        endDate, isActive, isHouseAd, priority
      } = body

      if (!name?.trim()) {
        return errorResponse('Name is required', 400)
      }
      if (!linkUrl?.trim()) {
        return errorResponse('Link URL is required', 400)
      }

      const { data: ad, error } = await supabase
        .from('ads')
        .insert({
          name: name.trim(),
          advertiser_name: advertiserName?.trim() || null,
          ad_type: adType || 'banner',
          placement: placement || 'general',
          image_url: imageUrl?.trim() || null,
          title: title?.trim() || null,
          description: description?.trim() || null,
          link_url: linkUrl.trim(),
          target_city: targetCity?.trim() || null,
          target_state: targetState?.trim() || null,
          start_date: startDate || null,
          end_date: endDate || null,
          is_active: isActive ?? true,
          is_house_ad: isHouseAd ?? false,
          priority: priority ?? 0,
        })
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        ad: {
          id: ad.id,
          name: ad.name,
          advertiserName: ad.advertiser_name,
          adType: ad.ad_type,
          placement: ad.placement,
          imageUrl: ad.image_url,
          title: ad.title,
          description: ad.description,
          linkUrl: ad.link_url,
          targetCity: ad.target_city,
          targetState: ad.target_state,
          startDate: ad.start_date,
          endDate: ad.end_date,
          isActive: ad.is_active,
          isHouseAd: ad.is_house_ad,
          priority: ad.priority,
          createdAt: ad.created_at,
          updatedAt: ad.updated_at,
        },
        message: 'Ad created',
      })
    }

    // Update ad
    if (action === 'update-ad') {
      const {
        adId, name, advertiserName, adType, placement, imageUrl, title,
        description, linkUrl, targetCity, targetState, startDate,
        endDate, isActive, isHouseAd, priority
      } = body

      if (!adId) {
        return errorResponse('adId required', 400)
      }

      const updates: Record<string, unknown> = { updated_at: new Date().toISOString() }
      if (name !== undefined) updates.name = name.trim()
      if (advertiserName !== undefined) updates.advertiser_name = advertiserName?.trim() || null
      if (adType !== undefined) updates.ad_type = adType
      if (placement !== undefined) updates.placement = placement
      if (imageUrl !== undefined) updates.image_url = imageUrl?.trim() || null
      if (title !== undefined) updates.title = title?.trim() || null
      if (description !== undefined) updates.description = description?.trim() || null
      if (linkUrl !== undefined) updates.link_url = linkUrl.trim()
      if (targetCity !== undefined) updates.target_city = targetCity?.trim() || null
      if (targetState !== undefined) updates.target_state = targetState?.trim() || null
      if (startDate !== undefined) updates.start_date = startDate || null
      if (endDate !== undefined) updates.end_date = endDate || null
      if (isActive !== undefined) updates.is_active = isActive
      if (isHouseAd !== undefined) updates.is_house_ad = isHouseAd
      if (priority !== undefined) updates.priority = priority

      const { data: ad, error } = await supabase
        .from('ads')
        .update(updates)
        .eq('id', adId)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        ad: {
          id: ad.id,
          name: ad.name,
          advertiserName: ad.advertiser_name,
          adType: ad.ad_type,
          placement: ad.placement,
          imageUrl: ad.image_url,
          title: ad.title,
          description: ad.description,
          linkUrl: ad.link_url,
          targetCity: ad.target_city,
          targetState: ad.target_state,
          startDate: ad.start_date,
          endDate: ad.end_date,
          isActive: ad.is_active,
          isHouseAd: ad.is_house_ad,
          priority: ad.priority,
          createdAt: ad.created_at,
          updatedAt: ad.updated_at,
        },
        message: 'Ad updated',
      })
    }

    // Delete ad
    if (action === 'delete-ad') {
      const { adId } = body

      if (!adId) {
        return errorResponse('adId required', 400)
      }

      const { data: ad } = await supabase
        .from('ads')
        .select('name')
        .eq('id', adId)
        .single()

      if (!ad) {
        return errorResponse('Ad not found', 404)
      }

      const { error } = await supabase
        .from('ads')
        .delete()
        .eq('id', adId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: `Ad "${ad.name}" deleted` })
    }

    // Refresh BGG cache - fetch missing thumbnails from BGG API
    if (action === 'refresh-bgg-cache') {
      const BGG_API_BASE = 'https://boardgamegeek.com/xmlapi2'

      // Get all bgg_ids from event_games that have NULL thumbnail but have a bgg_id
      const { data: gamesNeedingThumbnails } = await supabase
        .from('event_games')
        .select('bgg_id')
        .not('bgg_id', 'is', null)
        .is('thumbnail_url', null)

      if (!gamesNeedingThumbnails || gamesNeedingThumbnails.length === 0) {
        return jsonResponse({ message: 'No games need thumbnail refresh', refreshed: 0 })
      }

      // Get unique bgg_ids
      const uniqueBggIds = [...new Set(gamesNeedingThumbnails.map(g => g.bgg_id))]
      let refreshedCount = 0
      const errors: string[] = []

      for (const bggId of uniqueBggIds) {
        try {
          // Fetch from BGG API
          const headers: Record<string, string> = {}
          if (bggApiToken) {
            headers['Authorization'] = `Bearer ${bggApiToken}`
          }

          const response = await fetch(`${BGG_API_BASE}/thing?id=${bggId}`, { headers })

          if (!response.ok) {
            errors.push(`BGG API error for ${bggId}: ${response.status}`)
            continue
          }

          const xml = await response.text()

          // Parse thumbnail URL from XML
          const thumbnailMatch = xml.match(/<thumbnail>([^<]+)<\/thumbnail>/)
          const imageMatch = xml.match(/<image>([^<]+)<\/image>/)
          const thumbnailUrl = thumbnailMatch?.[1]?.trim() || null
          const imageUrl = imageMatch?.[1]?.trim() || null

          if (!thumbnailUrl) {
            errors.push(`No thumbnail found for bgg_id ${bggId}`)
            continue
          }

          // Update bgg_games_cache
          await supabase
            .from('bgg_games_cache')
            .update({ thumbnail_url: thumbnailUrl, image_url: imageUrl })
            .eq('bgg_id', bggId)

          // Update event_games
          await supabase
            .from('event_games')
            .update({ thumbnail_url: thumbnailUrl })
            .eq('bgg_id', bggId)

          refreshedCount++

          // Small delay to avoid rate limiting
          await new Promise(resolve => setTimeout(resolve, 500))
        } catch (err) {
          errors.push(`Error refreshing ${bggId}: ${err}`)
        }
      }

      return jsonResponse({
        message: `Refreshed ${refreshedCount} of ${uniqueBggIds.length} games`,
        refreshed: refreshedCount,
        total: uniqueBggIds.length,
        errors: errors.length > 0 ? errors : undefined,
      })
    }

    return errorResponse('Unknown action', 400)
  }

  return errorResponse('Method not allowed', 405)
})
