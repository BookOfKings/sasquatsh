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

  if (req.method !== 'GET') {
    return errorResponse('Method not allowed', 405)
  }

  // Public: Check if a referrer username exists
  if (action === 'check-referrer') {
    const username = url.searchParams.get('username')?.trim()
    if (!username) {
      return errorResponse('username parameter required', 400)
    }

    const { data: user } = await supabase
      .from('users')
      .select('id, display_name, avatar_url, username')
      .ilike('username', username)
      .single()

    if (!user) {
      return jsonResponse({ valid: false, reason: 'Username not found' })
    }

    return jsonResponse({
      valid: true,
      displayName: user.display_name,
      avatarUrl: user.avatar_url,
      username: user.username,
    })
  }

  // Authenticated: Get referral stats
  if (action === 'stats') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id, subscription_override_tier, subscription_override_expires_at')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!user) return errorResponse('User not found', 404)

    // Get credited referral count
    const { count: totalReferrals } = await supabase
      .from('referrals')
      .select('id', { count: 'exact', head: true })
      .eq('referrer_user_id', user.id)
      .eq('status', 'credited')

    // Get referred users list
    const { data: referrals } = await supabase
      .from('referrals')
      .select(`
        id, status, created_at,
        referred:users!referred_user_id(id, username, display_name, avatar_url)
      `)
      .eq('referrer_user_id', user.id)
      .order('created_at', { ascending: false })
      .limit(50)

    // Get reward history
    const { data: rewards } = await supabase
      .from('referral_rewards')
      .select('*')
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })

    const count = totalReferrals ?? 0
    const nextMilestone = Math.ceil((count + 1) / 10) * 10
    const progressToNext = count % 10

    return jsonResponse({
      totalReferrals: count,
      progressToNext,
      nextMilestone,
      referrals: (referrals || []).map((r: Record<string, unknown>) => ({
        id: r.id,
        status: r.status,
        createdAt: r.created_at,
        referred: r.referred,
      })),
      rewards: (rewards || []).map((r: Record<string, unknown>) => ({
        referralCountAtReward: r.referral_count_at_reward,
        proExpiresAt: r.pro_expires_at,
        createdAt: r.created_at,
      })),
      activeProReward: user.subscription_override_tier === 'pro' && user.subscription_override_expires_at
        ? { expiresAt: user.subscription_override_expires_at }
        : null,
    })
  }

  return errorResponse('Missing action parameter', 400)
})
