import Stripe from 'https://esm.sh/stripe@14.14.0?target=deno'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')!
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Price IDs from Stripe Dashboard
const PRICE_IDS: Record<string, string> = {
  basic: Deno.env.get('STRIPE_PRICE_BASIC') || '',
  pro: Deno.env.get('STRIPE_PRICE_PRO') || '',
}

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
    .select('id, email, stripe_customer_id, subscription_tier')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // Parse request body
  let body: { tier?: string; successUrl?: string; cancelUrl?: string }
  try {
    body = await req.json()
  } catch {
    return errorResponse('Invalid request body', 400)
  }

  const { tier, successUrl, cancelUrl } = body

  if (!tier || !['basic', 'pro'].includes(tier)) {
    return errorResponse('Invalid tier. Must be "basic" or "pro"', 400)
  }

  const priceId = PRICE_IDS[tier]
  if (!priceId) {
    return errorResponse(`Price ID not configured for tier: ${tier}`, 500)
  }

  try {
    // Get or create Stripe customer
    let customerId = user.stripe_customer_id

    if (!customerId) {
      const customer = await stripe.customers.create({
        email: user.email,
        metadata: {
          supabase_user_id: user.id,
          firebase_uid: firebaseUser.uid,
        },
      })
      customerId = customer.id

      // Save customer ID to database
      await supabase
        .from('users')
        .update({ stripe_customer_id: customerId })
        .eq('id', user.id)
    }

    // Create checkout session
    const session = await stripe.checkout.sessions.create({
      customer: customerId,
      payment_method_types: ['card'],
      line_items: [
        {
          price: priceId,
          quantity: 1,
        },
      ],
      mode: 'subscription',
      success_url: successUrl || 'https://sasquatsh.com/profile?checkout=success',
      cancel_url: cancelUrl || 'https://sasquatsh.com/pricing?checkout=cancelled',
      metadata: {
        supabase_user_id: user.id,
        tier: tier,
      },
      subscription_data: {
        metadata: {
          supabase_user_id: user.id,
          tier: tier,
        },
      },
      allow_promotion_codes: true,
    })

    return jsonResponse({
      sessionId: session.id,
      url: session.url,
    })
  } catch (err) {
    console.error('Stripe checkout error:', err)
    return errorResponse(err instanceof Error ? err.message : 'Failed to create checkout session', 500)
  }
})
