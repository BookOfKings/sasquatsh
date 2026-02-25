import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

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

    // Create new user
    const { data, error } = await supabase
      .from('users')
      .insert({
        firebase_uid: firebaseUser.uid,
        email: firebaseUser.email,
        display_name: firebaseUser.name,
        avatar_url: firebaseUser.picture,
      })
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(transformUser(data))
  }

  if (req.method === 'PUT') {
    // Update user profile
    const body = await req.json()

    const { data, error } = await supabase
      .from('users')
      .update({
        display_name: body.displayName,
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
    displayName: row.display_name,
    avatarUrl: row.avatar_url,
    subscriptionTier: row.subscription_tier,
    subscriptionExpiresAt: row.subscription_expires_at,
    isAdmin: row.is_admin ?? false,
    blockedUserIds: row.blocked_user_ids ?? [],
    createdAt: row.created_at,
  }
}
