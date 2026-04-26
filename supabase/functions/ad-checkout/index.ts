import Stripe from 'https://esm.sh/stripe@14.14.0?target=deno'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')!
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const AD_PRICE_IDS: Record<string, string> = {
  starter: Deno.env.get('STRIPE_AD_PRICE_STARTER') || '',
  standard: Deno.env.get('STRIPE_AD_PRICE_STANDARD') || '',
  premium: Deno.env.get('STRIPE_AD_PRICE_PREMIUM') || '',
  featured: Deno.env.get('STRIPE_AD_PRICE_FEATURED') || '',
}

const stripe = new Stripe(stripeSecretKey, {
  apiVersion: '2023-10-16',
  httpClient: Stripe.createFetchHttpClient(),
})

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const action = url.searchParams.get('action')

  // GET /ad-checkout?action=pending — Get all pending ads (admin only)
  if (req.method === 'GET' && action === 'pending') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: adminUser } = await supabase
      .from('users')
      .select('id, is_admin')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!adminUser?.is_admin) return errorResponse('Admin required', 403)

    const { data } = await supabase
      .from('advertiser_ads')
      .select('*')
      .eq('status', 'pending_review')
      .order('created_at', { ascending: true })

    return jsonResponse({ ads: data || [] })
  }

  // GET /ad-checkout?action=my-ads — Get current user's ads
  if (req.method === 'GET' && action === 'my-ads') {
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
      .from('advertiser_ads')
      .select('*')
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })

    if (error) return errorResponse('Failed to fetch ads', 500)
    return jsonResponse({ ads: data })
  }

  // POST /ad-checkout — Create ad + Stripe checkout session
  if (req.method === 'POST' && !action) {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id, email, stripe_customer_id')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { tier, title, description, linkUrl, imageUrl, targetCity, targetState, successUrl, cancelUrl } = body

    if (!tier || !AD_PRICE_IDS[tier]) {
      return errorResponse('Invalid ad tier', 400)
    }
    if (!title?.trim() || !description?.trim() || !linkUrl?.trim()) {
      return errorResponse('Title, description, and link URL are required', 400)
    }
    if (title.length > 100) return errorResponse('Title must be 100 characters or less', 400)
    if (description.length > 500) return errorResponse('Description must be 500 characters or less', 400)

    // Validate targeting based on tier
    if (tier === 'starter' && !targetCity?.trim()) {
      return errorResponse('Starter tier requires a target city', 400)
    }
    if (tier === 'standard' && !targetState?.trim()) {
      return errorResponse('Standard tier requires a target state', 400)
    }

    // Create the ad record (pending payment)
    const { data: ad, error: adError } = await supabase
      .from('advertiser_ads')
      .insert({
        user_id: user.id,
        ad_tier: tier,
        title: title.trim(),
        description: description.trim(),
        link_url: linkUrl.trim(),
        image_url: imageUrl?.trim() || null,
        target_city: targetCity?.trim() || null,
        target_state: targetState?.trim() || null,
        status: 'pending_payment',
      })
      .select()
      .single()

    if (adError) {
      console.error('Failed to create ad:', adError)
      return errorResponse('Failed to create ad', 500)
    }

    // Get or create Stripe customer
    let customerId = user.stripe_customer_id
    if (!customerId) {
      const customer = await stripe.customers.create({
        email: user.email,
        metadata: {
          supabase_user_id: user.id,
          firebase_uid: decoded.uid,
        },
      })
      customerId = customer.id
      await supabase
        .from('users')
        .update({ stripe_customer_id: customerId })
        .eq('id', user.id)
    }

    // Create Stripe Checkout session
    const session = await stripe.checkout.sessions.create({
      customer: customerId,
      mode: 'subscription',
      line_items: [{ price: AD_PRICE_IDS[tier], quantity: 1 }],
      success_url: successUrl || 'https://sasquatsh.com/advertise?checkout=success',
      cancel_url: cancelUrl || 'https://sasquatsh.com/advertise?checkout=cancelled',
      allow_promotion_codes: true,
      subscription_data: {
        metadata: {
          advertiser_ad_id: ad.id,
          ad_tier: tier,
          supabase_user_id: user.id,
        },
      },
      metadata: {
        advertiser_ad_id: ad.id,
        type: 'ad_purchase',
      },
    })

    return jsonResponse({ sessionId: session.id, url: session.url, adId: ad.id })
  }

  // POST /ad-checkout?action=approve — Approve an advertiser ad (admin only)
  if (req.method === 'POST' && action === 'approve') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: adminUser } = await supabase.from('users').select('id, is_admin').eq('firebase_uid', decoded.uid).single()
    if (!adminUser?.is_admin) return errorResponse('Admin required', 403)

    const body = await req.json()
    if (!body.adId) return errorResponse('adId required', 400)

    const { data: ad } = await supabase.from('advertiser_ads').select('*').eq('id', body.adId).single()
    if (!ad) return errorResponse('Ad not found', 404)

    // Update advertiser ad status
    await supabase.from('advertiser_ads').update({
      status: 'active',
      started_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    }).eq('id', ad.id)

    // Create entry in the main ads table so it gets served
    await supabase.from('ads').insert({
      name: `advertiser_${ad.id}`,
      advertiser_name: ad.title,
      ad_type: 'banner',
      placement: ad.ad_tier === 'featured' ? 'dashboard' : 'general',
      image_url: ad.image_url,
      title: ad.title,
      description: ad.description,
      link_url: ad.link_url,
      target_city: ad.target_city,
      target_state: ad.target_state,
      is_active: true,
      is_house_ad: false,
      priority: ad.ad_tier === 'featured' ? 20 : ad.ad_tier === 'premium' ? 15 : 10,
    })

    return jsonResponse({ approved: true })
  }

  // POST /ad-checkout?action=reject — Reject an advertiser ad (admin only)
  if (req.method === 'POST' && action === 'reject') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: adminUser } = await supabase.from('users').select('id, is_admin').eq('firebase_uid', decoded.uid).single()
    if (!adminUser?.is_admin) return errorResponse('Admin required', 403)

    const body = await req.json()
    if (!body.adId) return errorResponse('adId required', 400)

    await supabase.from('advertiser_ads').update({
      status: 'rejected',
      updated_at: new Date().toISOString(),
    }).eq('id', body.adId)

    return jsonResponse({ rejected: true })
  }

  // POST /ad-checkout?action=webhook — Stripe webhook for ad subscriptions
  if (req.method === 'POST' && action === 'webhook') {
    const webhookSecret = Deno.env.get('STRIPE_WEBHOOK_SECRET')
    if (!webhookSecret) return errorResponse('Webhook not configured', 500)

    const body = await req.text()
    const sig = req.headers.get('stripe-signature')
    if (!sig) return errorResponse('Missing signature', 400)

    let event: Stripe.Event
    try {
      event = await stripe.webhooks.constructEventAsync(body, sig, webhookSecret)
    } catch {
      return errorResponse('Invalid webhook signature', 400)
    }

    if (event.type === 'checkout.session.completed') {
      const session = event.data.object as Stripe.Checkout.Session
      if (session.metadata?.type === 'ad_purchase') {
        const adId = session.metadata.advertiser_ad_id
        const subscriptionId = session.subscription as string

        await supabase
          .from('advertiser_ads')
          .update({
            status: 'pending_review',
            stripe_subscription_id: subscriptionId,
            stripe_customer_id: session.customer as string,
            updated_at: new Date().toISOString(),
          })
          .eq('id', adId)
      }
    }

    if (event.type === 'customer.subscription.deleted') {
      const subscription = event.data.object as Stripe.Subscription
      const adId = subscription.metadata?.advertiser_ad_id
      if (adId) {
        await supabase
          .from('advertiser_ads')
          .update({
            status: 'expired',
            expires_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          })
          .eq('id', adId)

        // Also deactivate the corresponding ad in the ads table if it was synced
        await supabase
          .from('ads')
          .update({ is_active: false })
          .eq('name', `advertiser_${adId}`)
      }
    }

    return jsonResponse({ received: true })
  }

  return errorResponse('Method not allowed', 405)
})
