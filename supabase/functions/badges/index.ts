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
  const action = url.searchParams.get('action')

  // GET /badges - List all badge definitions
  // GET /badges?action=user&userId={id} - Get a user's earned badges
  // GET /badges?action=my-badges - Get current user's badges
  // POST /badges?action=compute - Compute and award badges for current user
  // PUT /badges?action=pin&badgeId={id} - Toggle pin on a badge
  if (req.method === 'GET') {
    // All badge definitions
    if (!action) {
      const { data, error } = await supabase
        .from('badges')
        .select('*')
        .eq('is_active', true)
        .order('sort_order', { ascending: true })

      if (error) return errorResponse('Failed to fetch badges', 500)
      return jsonResponse({ badges: data })
    }

    // A specific user's badges
    if (action === 'user') {
      const userId = url.searchParams.get('userId')
      if (!userId) return errorResponse('userId required', 400)

      const { data, error } = await supabase
        .from('user_badges')
        .select('*, badge:badges(*)')
        .eq('user_id', userId)
        .order('earned_at', { ascending: false })

      if (error) return errorResponse('Failed to fetch user badges', 500)
      return jsonResponse({ badges: data })
    }

    // Current user's badges
    if (action === 'my-badges') {
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
        .from('user_badges')
        .select('*, badge:badges(*)')
        .eq('user_id', user.id)
        .order('earned_at', { ascending: false })

      if (error) return errorResponse('Failed to fetch badges', 500)
      return jsonResponse({ badges: data })
    }
  }

  // POST /badges?action=compute - Compute badges for the authenticated user
  if (req.method === 'POST' && action === 'compute') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id, is_founding_member, collection_visibility, created_at')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!user) return errorResponse('User not found', 404)

    // Get all badge definitions
    const { data: allBadges } = await supabase
      .from('badges')
      .select('*')
      .eq('is_active', true)

    if (!allBadges) return errorResponse('Failed to load badges', 500)

    // Get already earned badges
    const { data: earnedBadges } = await supabase
      .from('user_badges')
      .select('badge_id')
      .eq('user_id', user.id)

    const earnedIds = new Set((earnedBadges ?? []).map(b => b.badge_id))

    // Compute activity counts
    const counts = await computeActivityCounts(supabase, user.id)

    // Add special flags
    counts.is_founding_member = user.is_founding_member ? 1 : 0
    counts.collection_public = user.collection_visibility === 'public' ? 1 : 0

    // Check early signup (within first 30 days - adjust cutoff as needed)
    const launchDate = new Date('2026-02-01')
    const signupDate = new Date(user.created_at)
    const daysSinceLaunch = (signupDate.getTime() - launchDate.getTime()) / (1000 * 60 * 60 * 24)
    counts.early_signup = daysSinceLaunch <= 30 ? 1 : 0

    // Award new badges
    const newBadges: Array<{ user_id: string; badge_id: number }> = []
    for (const badge of allBadges) {
      if (earnedIds.has(badge.id)) continue
      const count = counts[badge.requirement_type] ?? 0
      if (count >= badge.requirement_count) {
        newBadges.push({ user_id: user.id, badge_id: badge.id })
      }
    }

    if (newBadges.length > 0) {
      const { error } = await supabase
        .from('user_badges')
        .upsert(newBadges, { onConflict: 'user_id,badge_id' })

      if (error) {
        console.error('Failed to award badges:', error)
        return errorResponse('Failed to award badges', 500)
      }
    }

    // Return all badges (including newly earned)
    const { data: updatedBadges } = await supabase
      .from('user_badges')
      .select('*, badge:badges(*)')
      .eq('user_id', user.id)
      .order('earned_at', { ascending: false })

    return jsonResponse({
      badges: updatedBadges,
      newlyEarned: newBadges.length,
    })
  }

  // PUT /badges?action=pin&badgeId={id} - Toggle pin on a badge
  if (req.method === 'PUT' && action === 'pin') {
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

    const badgeId = url.searchParams.get('badgeId')
    if (!badgeId) return errorResponse('badgeId required', 400)

    // Get current state
    const { data: userBadge } = await supabase
      .from('user_badges')
      .select('id, is_pinned')
      .eq('user_id', user.id)
      .eq('badge_id', Number(badgeId))
      .single()

    if (!userBadge) return errorResponse('Badge not earned', 404)

    // If unpinning, just toggle
    if (userBadge.is_pinned) {
      await supabase
        .from('user_badges')
        .update({ is_pinned: false })
        .eq('id', userBadge.id)
      return jsonResponse({ pinned: false })
    }

    // Check pin limit (max 3)
    const { count } = await supabase
      .from('user_badges')
      .select('id', { count: 'exact', head: true })
      .eq('user_id', user.id)
      .eq('is_pinned', true)

    if ((count ?? 0) >= 3) {
      return errorResponse('Maximum 3 pinned badges', 400)
    }

    await supabase
      .from('user_badges')
      .update({ is_pinned: true })
      .eq('id', userBadge.id)

    return jsonResponse({ pinned: true })
  }

  // POST /badges?action=compute-all - Compute badges for ALL users (admin only)
  if (req.method === 'POST' && action === 'compute-all') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    // Verify admin
    const { data: adminUser } = await supabase
      .from('users')
      .select('id, is_admin')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!adminUser?.is_admin) return errorResponse('Admin access required', 403)

    // Get all users
    const { data: allUsers } = await supabase
      .from('users')
      .select('id, is_founding_member, collection_visibility, created_at')

    if (!allUsers) return errorResponse('Failed to load users', 500)

    // Get all badge definitions
    const { data: allBadges } = await supabase
      .from('badges')
      .select('*')
      .eq('is_active', true)

    if (!allBadges) return errorResponse('Failed to load badges', 500)

    const launchDate = new Date('2026-02-01')
    let totalAwarded = 0

    for (const user of allUsers) {
      // Get already earned
      const { data: earnedBadges } = await supabase
        .from('user_badges')
        .select('badge_id')
        .eq('user_id', user.id)
      const earnedIds = new Set((earnedBadges ?? []).map(b => b.badge_id))

      // Compute counts
      const counts = await computeActivityCounts(supabase, user.id)
      counts.is_founding_member = user.is_founding_member ? 1 : 0
      counts.collection_public = user.collection_visibility === 'public' ? 1 : 0
      const signupDate = new Date(user.created_at)
      counts.early_signup = ((signupDate.getTime() - launchDate.getTime()) / (1000 * 60 * 60 * 24)) <= 30 ? 1 : 0

      // Award new badges
      const newBadges: Array<{ user_id: string; badge_id: number }> = []
      for (const badge of allBadges) {
        if (earnedIds.has(badge.id)) continue
        if ((counts[badge.requirement_type] ?? 0) >= badge.requirement_count) {
          newBadges.push({ user_id: user.id, badge_id: badge.id })
        }
      }

      if (newBadges.length > 0) {
        await supabase.from('user_badges').upsert(newBadges, { onConflict: 'user_id,badge_id' })
        totalAwarded += newBadges.length
      }
    }

    return jsonResponse({ usersProcessed: allUsers.length, badgesAwarded: totalAwarded })
  }

  return errorResponse('Method not allowed', 405)
})

// Compute all activity counts for a user
async function computeActivityCounts(
  supabase: ReturnType<typeof createClient>,
  userId: string
): Promise<Record<string, number>> {
  const counts: Record<string, number> = {}

  // Games hosted
  const { count: hostedCount } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)

  counts.games_hosted = hostedCount ?? 0

  // Games attended (confirmed registrations)
  const { count: attendedCount } = await supabase
    .from('event_registrations')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)
    .eq('status', 'confirmed')

  counts.games_attended = attendedCount ?? 0

  // Cancellations
  const { count: cancelCount } = await supabase
    .from('event_registrations')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)
    .eq('status', 'cancelled')

  counts.zero_cancellations = (cancelCount ?? 0) === 0 ? 1 : 0

  // Multi-table events hosted
  const { count: multiTableCount } = await supabase
    .from('planning_sessions')
    .select('id', { count: 'exact', head: true })
    .eq('created_by_user_id', userId)
    .gt('table_count', 1)

  counts.multi_table_events_hosted = multiTableCount ?? 0

  // Planning sessions created
  const { count: plansCount } = await supabase
    .from('planning_sessions')
    .select('id', { count: 'exact', head: true })
    .eq('created_by_user_id', userId)

  counts.plans_created = plansCount ?? 0

  // Groups created
  const { count: groupsCreatedCount } = await supabase
    .from('groups')
    .select('id', { count: 'exact', head: true })
    .eq('created_by', userId)

  counts.groups_created = groupsCreatedCount ?? 0

  // Groups joined
  const { count: groupsJoinedCount } = await supabase
    .from('group_members')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)
    .eq('status', 'active')

  counts.groups_joined = groupsJoinedCount ?? 0

  // Collection size
  const { count: collectionCount } = await supabase
    .from('user_game_collections')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)

  counts.collection_size = collectionCount ?? 0

  // Bugs submitted
  const { count: bugsCount } = await supabase
    .from('admin_bugs')
    .select('id', { count: 'exact', head: true })
    .eq('submitted_by_user_id', userId)

  counts.bugs_submitted = bugsCount ?? 0

  // Game system events (hosted + attended)
  // Board games
  const { count: bgHosted } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)
    .eq('game_type', 'board_game')

  const { count: bgAttended } = await supabase
    .from('event_registrations')
    .select('id', { count: 'exact', head: true })
    .eq('user_id', userId)
    .eq('status', 'confirmed')
    .in('event_id',
      supabase.from('events').select('id').eq('game_type', 'board_game')
    )

  counts.board_game_events = (bgHosted ?? 0) + (bgAttended ?? 0)

  // MTG
  const { count: mtgHosted } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)
    .eq('game_type', 'mtg')

  counts.mtg_events = (mtgHosted ?? 0)

  // Pokemon
  const { count: pokemonHosted } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)
    .eq('game_type', 'pokemon')

  counts.pokemon_events = (pokemonHosted ?? 0)

  // Yu-Gi-Oh
  const { count: yugiohHosted } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)
    .eq('game_type', 'yugioh')

  counts.yugioh_events = (yugiohHosted ?? 0)

  // Warhammer
  const { count: warhammerHosted } = await supabase
    .from('events')
    .select('id', { count: 'exact', head: true })
    .eq('host_user_id', userId)
    .eq('game_type', 'warhammer40k')

  counts.warhammer_events = (warhammerHosted ?? 0)

  // TODO: host_streak_weeks, attend_streak_weeks, plan_votes_cast,
  // invites_sent, new_players_invited, events_with_items,
  // events_games_brought, events_snacks_brought
  // These require more complex queries or additional tracking tables

  return counts
}
