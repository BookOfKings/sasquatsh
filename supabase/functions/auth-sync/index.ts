import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const recaptchaSecretKey = Deno.env.get('RECAPTCHA_SECRET_KEY')
const internalServiceKey = Deno.env.get('INTERNAL_SERVICE_KEY')

// Username validation regex: 3-30 chars, starts with letter, alphanumeric + underscores
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/

// Reserved usernames
const RESERVED_USERNAMES = ['admin', 'administrator', 'root', 'system', 'support', 'help', 'info', 'sasquatsh', 'moderator', 'mod']

async function sendWelcomeEmail(email: string, displayName: string | null, username: string): Promise<void> {
  if (!internalServiceKey) {
    console.warn('INTERNAL_SERVICE_KEY not configured, skipping welcome email')
    return
  }

  try {
    const response = await fetch(`${supabaseUrl}/functions/v1/send-email`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Internal-Key': internalServiceKey,
      },
      body: JSON.stringify({
        type: 'welcome',
        to: email,
        displayName: displayName || '',
        username: username,
      }),
    })

    if (!response.ok) {
      const error = await response.json().catch(() => ({}))
      console.error('Failed to send welcome email:', error)
    }
  } catch (err) {
    console.error('Error sending welcome email:', err)
  }
}

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
    return new Response(null, { headers: getCorsHeaders() })
  }

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
      // Update last seen and return existing user
      const { data, error } = await supabase
        .from('users')
        .update({
          updated_at: new Date().toISOString(),
          display_name: firebaseUser.name || existingUser.display_name,
          avatar_url: firebaseUser.picture || existingUser.avatar_url,
        })
        .eq('firebase_uid', firebaseUser.uid)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(transformUser(data))
    }

    // New user - parse body for username and recaptcha token
    let body: { username?: string; recaptchaToken?: string } = {}
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

    // Create new user
    const { data, error } = await supabase
      .from('users')
      .insert({
        firebase_uid: firebaseUser.uid,
        email: firebaseUser.email,
        display_name: firebaseUser.name,
        avatar_url: firebaseUser.picture,
        username: username,
      })
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Send welcome email (non-blocking)
    if (firebaseUser.email) {
      sendWelcomeEmail(firebaseUser.email, firebaseUser.name || null, username)
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

    const { data, error } = await supabase
      .from('users')
      .update({
        display_name: body.displayName,
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

function transformUser(row: Record<string, unknown>) {
  return {
    id: row.id,
    email: row.email,
    username: row.username,
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    subscriptionTier: row.subscription_tier,
    subscriptionExpiresAt: row.subscription_expires_at,
    isAdmin: row.is_admin ?? false,
    blockedUserIds: row.blocked_user_ids ?? [],
    createdAt: row.created_at,
  }
}
