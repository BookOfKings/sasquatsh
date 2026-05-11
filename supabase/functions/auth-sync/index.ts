import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const recaptchaSecretKey = Deno.env.get('RECAPTCHA_SECRET_KEY')

// Username validation regex: 3-30 chars, starts with letter, alphanumeric + underscores
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/

// Reserved usernames
const RESERVED_USERNAMES = ['admin', 'administrator', 'root', 'system', 'support', 'help', 'info', 'sasquatsh', 'moderator', 'mod']

async function verifyRecaptcha(token: string): Promise<{ success: boolean; score?: number; error?: string }> {
  if (!recaptchaSecretKey) {
    console.warn('RECAPTCHA_SECRET_KEY not configured, skipping verification')
    return { success: true }
  }

  try {
    const response = await fetch('https://www.google.com/recaptcha/api/siteverify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `secret=${recaptchaSecretKey}&response=${token}`,
    })

    const data = await response.json()

    if (!data.success) {
      return { success: false, error: 'reCAPTCHA verification failed' }
    }

    // v3 returns a score from 0.0 to 1.0, higher is more likely human
    if (data.score !== undefined && data.score < 0.5) {
      return { success: false, score: data.score, error: 'Request appears automated' }
    }

    return { success: true, score: data.score }
  } catch (err) {
    console.error('reCAPTCHA verification error:', err)
    return { success: false, error: 'Failed to verify reCAPTCHA' }
  }
}

async function validateUsername(supabase: ReturnType<typeof createClient>, username: string, excludeUserId?: string): Promise<{ valid: boolean; error?: string }> {
  if (!USERNAME_REGEX.test(username)) {
    return { valid: false, error: 'Username must be 3-30 characters, start with a letter, and contain only letters, numbers, and underscores' }
  }

  if (RESERVED_USERNAMES.includes(username.toLowerCase())) {
    return { valid: false, error: 'This username is reserved' }
  }

  // Check uniqueness (case-insensitive)
  let query = supabase
    .from('users')
    .select('id')
    .ilike('username', username)
    .limit(1)

  if (excludeUserId) {
    query = query.neq('id', excludeUserId)
  }

  const { data } = await query

  if (data && data.length > 0) {
    return { valid: false, error: 'Username is already taken' }
  }

  return { valid: true }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // Get Firebase token from custom header
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  if (req.method === 'POST') {
    // Sync/create user
    const { data: existingUser } = await supabase
      .from('users')
      .select('*')
      .eq('firebase_uid', firebaseUser.uid)
      .single()

    if (existingUser) {
      // Parse body for optional fcmToken
      let syncBody: { fcmToken?: string } = {}
      try {
        syncBody = await req.json()
      } catch {
        // No body is fine
      }

      // Update last seen and return existing user
      // Don't overwrite avatar_url if user has uploaded their own avatar
      const updateFields: Record<string, unknown> = {
        updated_at: new Date().toISOString(),
        display_name: firebaseUser.name || existingUser.display_name,
        // Only set avatar from Firebase if user doesn't have one
        avatar_url: existingUser.avatar_url || firebaseUser.picture,
        // Update auth_provider if not set (for existing users)
        auth_provider: existingUser.auth_provider || firebaseUser.signInProvider || 'password',
      }

      // Store FCM token if provided (from iOS app)
      if (syncBody.fcmToken) {
        updateFields.fcm_token = syncBody.fcmToken
        updateFields.fcm_token_updated_at = new Date().toISOString()
      }

      const { data, error } = await supabase
        .from('users')
        .update(updateFields)
        .eq('firebase_uid', firebaseUser.uid)
        .select('id, firebase_uid, email, username, display_name, avatar_url, subscription_tier, subscription_expires_at, subscription_status, subscription_override_tier, subscription_override_expires_at, account_status, is_admin, is_founding_member, blocked_user_ids, created_at, auth_provider')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Debug: Log subscription fields
      console.log('User sync - subscription fields:', {
        userId: data.id,
        username: data.username,
        subscription_tier: data.subscription_tier,
        subscription_override_tier: data.subscription_override_tier,
      })

      return jsonResponse(transformUser(data))
    }

    // New user - parse body for username, recaptcha token, and referrer
    let body: { username?: string; recaptchaToken?: string; referrerUsername?: string } = {}
    try {
      body = await req.json()
    } catch {
      // No body or invalid JSON is ok for Google OAuth
    }

    // Verify reCAPTCHA for new signups (skip for OAuth providers like Google)
    const isOAuthSignup = firebaseUser.signInProvider && firebaseUser.signInProvider !== 'password'

    if (body.recaptchaToken) {
      const recaptchaResult = await verifyRecaptcha(body.recaptchaToken)
      if (!recaptchaResult.success) {
        return errorResponse(recaptchaResult.error || 'reCAPTCHA verification failed', 400)
      }
    } else if (recaptchaSecretKey && !isOAuthSignup) {
      // If secret is configured but no token provided, require it (except for OAuth)
      return errorResponse('reCAPTCHA token required', 400)
    }

    // Validate username if provided
    let username = body.username?.trim()
    if (username) {
      const usernameValidation = await validateUsername(supabase, username)
      if (!usernameValidation.valid) {
        return errorResponse(usernameValidation.error || 'Invalid username', 400)
      }
    } else {
      // Generate a unique username for users signing up via Google OAuth
      const baseUsername = (firebaseUser.name || 'user').toLowerCase().replace(/[^a-z0-9_]/g, '_').slice(0, 20)
      let suffix = Math.floor(Math.random() * 9999)
      username = `${baseUsername}${suffix}`

      // Make sure it's unique
      let attempts = 0
      while (attempts < 10) {
        const { data: existing } = await supabase
          .from('users')
          .select('id')
          .ilike('username', username)
          .limit(1)

        if (!existing || existing.length === 0) break

        suffix = Math.floor(Math.random() * 99999)
        username = `${baseUsername}${suffix}`
        attempts++
      }
    }

    // Look up referrer if provided
    let referrerUserId: string | null = null
    if (body.referrerUsername?.trim()) {
      const { data: referrer } = await supabase
        .from('users')
        .select('id')
        .ilike('username', body.referrerUsername.trim())
        .single()

      if (referrer) {
        referrerUserId = referrer.id
      }
    }

    // Create new user - use username as display_name if not provided
    // Use upsert to handle race conditions and provider linking
    const insertData: Record<string, unknown> = {
      firebase_uid: firebaseUser.uid,
      email: firebaseUser.email,
      display_name: firebaseUser.name || username,
      avatar_url: firebaseUser.picture,
      username: username,
      auth_provider: firebaseUser.signInProvider || 'password',
    }
    if (referrerUserId) {
      insertData.referred_by_user_id = referrerUserId
    }

    const { data, error } = await supabase
      .from('users')
      .upsert(insertData, { onConflict: 'firebase_uid' })
      .select()
      .single()

    if (error) {
      // If upsert fails (e.g. username conflict), try fetching existing user
      const { data: existing } = await supabase
        .from('users')
        .select('*')
        .eq('firebase_uid', firebaseUser.uid)
        .single()
      if (existing) {
        return jsonResponse(transformUser(existing))
      }
      return errorResponse(error.message, 500)
    }

    // Process referral credit if referrer was found
    if (referrerUserId && data) {
      try {
        // Prevent self-referral
        if (referrerUserId !== data.id) {
          await processReferralCredit(supabase, referrerUserId, data.id as string)
        }
      } catch (err) {
        console.error('Failed to process referral:', err)
        // Don't fail signup over referral errors
      }
    }

    return jsonResponse(transformUser(data))
  }

  if (req.method === 'PUT') {
    // Update user profile
    const body = await req.json()

    // Get current user for validation
    const { data: currentUser } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', firebaseUser.uid)
      .single()

    if (!currentUser) {
      return errorResponse('User not found', 404)
    }

    // Validate username if being updated
    if (body.username !== undefined) {
      const usernameValidation = await validateUsername(supabase, body.username, currentUser.id)
      if (!usernameValidation.valid) {
        return errorResponse(usernameValidation.error || 'Invalid username', 400)
      }
    }

    // Use username as display_name fallback if display_name is empty
    const displayName = body.displayName || body.username

    const { data, error } = await supabase
      .from('users')
      .update({
        display_name: displayName,
        username: body.username,
        updated_at: new Date().toISOString(),
      })
      .eq('firebase_uid', firebaseUser.uid)
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(transformUser(data))
  }

  return errorResponse('Method not allowed', 405)
})

async function processReferralCredit(
  supabase: ReturnType<typeof createClient>,
  referrerUserId: string,
  referredUserId: string
) {
  // Create referral record
  const { data: referral, error: refError } = await supabase
    .from('referrals')
    .insert({
      referrer_user_id: referrerUserId,
      referred_user_id: referredUserId,
      status: 'credited',
      credited_at: new Date().toISOString(),
    })
    .select('id')
    .single()

  if (refError) {
    console.error('Failed to create referral:', refError)
    return
  }

  // Award 2 raffle entries to the referrer for the active raffle
  const now = new Date().toISOString()
  const { data: activeRaffle } = await supabase
    .from('raffles')
    .select('id')
    .eq('status', 'active')
    .lte('start_date', now)
    .gt('end_date', now)
    .order('start_date', { ascending: false })
    .limit(1)
    .maybeSingle()

  if (activeRaffle) {
    await supabase
      .from('raffle_entries')
      .insert({
        raffle_id: activeRaffle.id,
        user_id: referrerUserId,
        entry_type: 'referral',
        source_id: referral.id,
        entry_count: 2,
      })
    console.log(`Awarded 2 raffle entries to referrer ${referrerUserId} for referral ${referral.id}`)
  }

  // Check if referrer has hit a 10-referral milestone
  const { count: totalReferrals } = await supabase
    .from('referrals')
    .select('id', { count: 'exact', head: true })
    .eq('referrer_user_id', referrerUserId)
    .eq('status', 'credited')

  const referralCount = totalReferrals ?? 0

  if (referralCount > 0 && referralCount % 10 === 0) {
    // Check if this milestone was already rewarded
    const { data: existingReward } = await supabase
      .from('referral_rewards')
      .select('id')
      .eq('user_id', referrerUserId)
      .eq('referral_count_at_reward', referralCount)
      .maybeSingle()

    if (!existingReward) {
      // Get current override expiry to stack
      const { data: referrerUser } = await supabase
        .from('users')
        .select('subscription_override_tier, subscription_override_expires_at')
        .eq('id', referrerUserId)
        .single()

      // Calculate new expiry: extend from current expiry or start from now
      let baseDate = new Date()
      if (referrerUser?.subscription_override_expires_at) {
        const currentExpiry = new Date(referrerUser.subscription_override_expires_at)
        if (currentExpiry > baseDate) {
          baseDate = currentExpiry
        }
      }

      const newExpiry = new Date(baseDate)
      newExpiry.setMonth(newExpiry.getMonth() + 3)

      // Only upgrade, don't downgrade (e.g., don't overwrite 'premium' with 'pro')
      const currentOverride = referrerUser?.subscription_override_tier
      const shouldSetOverride = !currentOverride || currentOverride === 'free' || currentOverride === 'basic' || currentOverride === 'pro'

      if (shouldSetOverride) {
        await supabase
          .from('users')
          .update({
            subscription_override_tier: 'pro',
            subscription_override_expires_at: newExpiry.toISOString(),
          })
          .eq('id', referrerUserId)

        console.log(`Awarded 3 months Pro to ${referrerUserId} for ${referralCount} referrals (expires ${newExpiry.toISOString()})`)
      }

      // Record the milestone
      await supabase
        .from('referral_rewards')
        .insert({
          user_id: referrerUserId,
          referral_count_at_reward: referralCount,
          pro_expires_at: newExpiry.toISOString(),
        })
    }
  }
}

function transformUser(row: Record<string, unknown>) {
  return {
    id: row.id,
    email: row.email,
    username: row.username,
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    subscriptionTier: row.subscription_tier ?? 'free',
    subscriptionExpiresAt: row.subscription_expires_at,
    subscriptionStatus: row.subscription_status ?? 'active',
    subscriptionOverrideTier: row.subscription_override_tier,
    subscriptionOverrideExpiresAt: row.subscription_override_expires_at,
    accountStatus: row.account_status ?? 'active',
    isAdmin: row.is_admin ?? false,
    isFoundingMember: row.is_founding_member ?? false,
    blockedUserIds: row.blocked_user_ids ?? [],
    createdAt: row.created_at,
    authProvider: row.auth_provider ?? 'password',
  }
}
