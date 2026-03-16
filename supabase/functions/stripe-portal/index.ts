import Stripe from 'https://esm.sh/stripe@14.14.0?target=deno'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')!
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const stripe = new Stripe(stripeSecretKey, {
  apiVersion: '2023-10-16',
  httpClient: Stripe.createFetchHttpClient(),
})

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  if (req.method !== 'POST') {
    return errorResponse('Method not allowed', 405)
  }

  // Verify Firebase token
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, email, stripe_customer_id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  if (!user.stripe_customer_id) {
    return errorResponse('No billing account found. Subscribe to a plan first.', 400)
  }

  // Parse optional return URL from body
  let returnUrl = 'https://sasquatsh.com/profile'
  try {
    const body = await req.json()
    if (body.returnUrl) {
      returnUrl = body.returnUrl
    }
  } catch {
    // No body or invalid JSON, use default
  }

  try {
    // Create customer portal session
    const session = await stripe.billingPortal.sessions.create({
      customer: user.stripe_customer_id,
      return_url: returnUrl,
    })

    return jsonResponse({
      url: session.url,
    })
  } catch (err) {
    console.error('Stripe portal error:', err)
    return errorResponse(err instanceof Error ? err.message : 'Failed to create portal session', 500)
  }
})
