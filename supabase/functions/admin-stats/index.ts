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
        groups: groups?.map(g => ({
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

    // List notes
    if (action === 'notes') {
      const category = url.searchParams.get('category') || ''

      let query = supabase
        .from('admin_notes')
        .select(`
          id, title, content, category, is_pinned, created_at, updated_at,
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
        notes: notes?.map(n => ({
          id: n.id,
          title: n.title,
          content: n.content,
          category: n.category,
          isPinned: n.is_pinned,
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
        bugs: bugs?.map(b => ({
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

    // Update user (admin edit)
    if (action === 'update-user') {
      const { userId, displayName, username, isAdmin } = body

      if (!userId) {
        return errorResponse('userId required', 400)
      }

      // Can't modify yourself through this endpoint
      if (userId === adminUser.id) {
        return errorResponse('Cannot modify your own account through admin panel', 400)
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
      const { noteId, title, content, category, isPinned } = body

      if (!noteId) {
        return errorResponse('noteId required', 400)
      }

      const updates: Record<string, unknown> = { updated_at: new Date().toISOString() }
      if (title !== undefined) updates.title = title.trim()
      if (content !== undefined) updates.content = content.trim()
      if (category !== undefined) updates.category = category.trim()
      if (isPinned !== undefined) updates.is_pinned = isPinned

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

    return errorResponse('Unknown action', 400)
  }

  return errorResponse('Method not allowed', 405)
})
