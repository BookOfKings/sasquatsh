import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import * as jose from 'https://esm.sh/jose@5'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Google Play package name
const PACKAGE_NAME = 'com.sasquatsh.app'

// Google service account credentials (base64-encoded JSON)
const GOOGLE_SERVICE_ACCOUNT_KEY_BASE64 = Deno.env.get('GOOGLE_SERVICE_ACCOUNT_KEY') || ''

// Secret for RTDN Pub/Sub push verification
const RTDN_SECRET = Deno.env.get('RTDN_SECRET') || ''

// Product ID → tier mapping (same as iOS / Apple IAP)
const PRODUCT_TO_TIER: Record<string, string> = {
  'com.sasquatsh.basic.monthly': 'basic',
  'com.sasquatsh.basic.annual': 'basic',
  'com.sasquatsh.pro.monthly': 'pro',
  'com.sasquatsh.pro.annual': 'pro',
}

const TIER_RANK: Record<string, number> = {
  'free': 0,
  'basic': 1,
  'pro': 2,
  'premium': 3,
}

// Google Play RTDN subscription notification types
const NOTIFICATION_TYPE = {
  SUBSCRIPTION_RECOVERED: 1,
  SUBSCRIPTION_RENEWED: 2,
  SUBSCRIPTION_CANCELED: 3,
  SUBSCRIPTION_PURCHASED: 4,
  SUBSCRIPTION_ON_HOLD: 5,
  SUBSCRIPTION_IN_GRACE_PERIOD: 6,
  SUBSCRIPTION_RESTARTED: 7,
  SUBSCRIPTION_PRICE_CHANGE_CONFIRMED: 8,
  SUBSCRIPTION_DEFERRED: 9,
  SUBSCRIPTION_PAUSED: 10,
  SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED: 11,
  SUBSCRIPTION_REVOKED: 12,
  SUBSCRIPTION_EXPIRED: 13,
}

// Cache for Google OAuth2 access token
let cachedAccessToken: { token: string; expiresAt: number } | null = null

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const action = url.searchParams.get('action')

  // ============================================================
  // CLIENT-INITIATED: Verify purchase after Google Play purchase
  // POST ?action=verify
  // ============================================================
  if (req.method === 'POST' && action === 'verify') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    // Look up user
    const { data: user } = await supabase
      .from('users')
      .select('id, subscription_tier, subscription_source, stripe_subscription_id, apple_original_transaction_id, google_purchase_token, subscription_override_tier')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { purchaseToken, productId, orderId } = body

    if (!purchaseToken || !productId) {
      return errorResponse('purchaseToken and productId are required', 400)
    }

    try {
      console.log(`[GPB] Verifying purchase for product ${productId}, order ${orderId}`)

      if (!GOOGLE_SERVICE_ACCOUNT_KEY_BASE64) {
        console.error('[GPB] Missing Google service account key')
        return errorResponse('Google Play API credentials not configured', 500)
      }

      // Get OAuth2 access token
      const accessToken = await getGoogleAccessToken()

      // Verify subscription with Google Play Developer API (v2)
      const apiUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/purchases/subscriptionsv2/tokens/${purchaseToken}`

      const gpResponse = await fetch(apiUrl, {
        headers: { 'Authorization': `Bearer ${accessToken}` },
      })

      if (!gpResponse.ok) {
        const errText = await gpResponse.text()
        console.error(`[GPB] Google Play API error ${gpResponse.status}:`, errText)
        return errorResponse(`Google Play verification failed (${gpResponse.status}): ${errText}`, 502)
      }

      const purchaseData = await gpResponse.json()

      console.log(`[GPB] Purchase state: ${purchaseData.subscriptionState}, ack: ${purchaseData.acknowledgementState}`)

      // Validate purchase state
      // subscriptionState: SUBSCRIPTION_STATE_ACTIVE, SUBSCRIPTION_STATE_CANCELED, etc.
      const validStates = ['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD']
      if (!validStates.includes(purchaseData.subscriptionState)) {
        return errorResponse(`Purchase is not active (state: ${purchaseData.subscriptionState})`, 400)
      }

      // Map product to tier
      const tier = PRODUCT_TO_TIER[productId]
      if (!tier) {
        return errorResponse(`Unknown product: ${productId}`, 400)
      }

      // Get expiration from line items
      let expiresAt: string | null = null
      if (purchaseData.lineItems?.length > 0) {
        const lineItem = purchaseData.lineItems[0]
        if (lineItem.expiryTime) {
          expiresAt = new Date(lineItem.expiryTime).toISOString()
        }
      }

      // Acknowledge the subscription if not already acknowledged
      if (purchaseData.acknowledgementState !== 'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED') {
        console.log('[GPB] Acknowledging subscription...')
        // Use v1 acknowledge endpoint
        const ackUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/purchases/subscriptions/${productId}/tokens/${purchaseToken}:acknowledge`
        const ackResponse = await fetch(ackUrl, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({}),
        })

        if (!ackResponse.ok) {
          console.error(`[GPB] Acknowledge failed: ${ackResponse.status}`, await ackResponse.text())
          // Don't fail the verify — acknowledgment can be retried
        } else {
          console.log('[GPB] Subscription acknowledged')
        }
      }

      // Conflict resolution: highest active tier wins
      const currentTierRank = TIER_RANK[user.subscription_override_tier || user.subscription_tier || 'free']
      const newTierRank = TIER_RANK[tier]

      const updateData: Record<string, unknown> = {
        google_purchase_token: purchaseToken,
        google_order_id: orderId || null,
        subscription_status: 'active',
      }

      if (expiresAt) {
        updateData.subscription_expires_at = expiresAt
      }

      // Only upgrade, never downgrade
      if (newTierRank >= currentTierRank && !user.subscription_override_tier) {
        updateData.subscription_tier = tier
        updateData.subscription_source = 'google'
      }

      const { error: updateError } = await supabase
        .from('users')
        .update(updateData)
        .eq('id', user.id)

      if (updateError) {
        console.error('Failed to update user:', updateError)
        return errorResponse('Failed to update subscription', 500)
      }

      // Log event
      await supabase.from('subscription_events').insert({
        user_id: user.id,
        event_type: 'google_purchase',
        tier,
        metadata: {
          product_id: productId,
          order_id: orderId,
          subscription_state: purchaseData.subscriptionState,
        },
      })

      return jsonResponse({ success: true, tier })
    } catch (err) {
      console.error('Verification error:', err)
      return errorResponse(`Verification failed: ${err.message}`, 500)
    }
  }

  // ============================================================
  // GOOGLE REAL-TIME DEVELOPER NOTIFICATIONS (RTDN)
  // POST (no action param) — Pub/Sub push
  // ============================================================
  if (req.method === 'POST' && !action) {
    // Verify RTDN secret if configured
    const secret = url.searchParams.get('secret')
    if (RTDN_SECRET && secret !== RTDN_SECRET) {
      console.error('[RTDN] Invalid secret')
      return errorResponse('Unauthorized', 401)
    }

    try {
      const body = await req.json()

      // Pub/Sub push message format
      const messageData = body.message?.data
      if (!messageData) {
        console.log('[RTDN] No message data, acknowledging')
        return jsonResponse({ ok: true })
      }

      // Decode base64 Pub/Sub message
      const decodedData = JSON.parse(atob(messageData))
      const notification = decodedData.subscriptionNotification

      if (!notification) {
        console.log('[RTDN] No subscription notification in message, acknowledging')
        return jsonResponse({ ok: true })
      }

      const { purchaseToken, subscriptionId, notificationType } = notification

      console.log(`[RTDN] Notification type ${notificationType} for subscription ${subscriptionId}`)

      if (!purchaseToken) {
        console.error('[RTDN] Missing purchaseToken')
        return jsonResponse({ ok: true })
      }

      // Find user by Google purchase token
      const { data: user } = await supabase
        .from('users')
        .select('id, subscription_tier, subscription_source, stripe_subscription_id, apple_original_transaction_id, subscription_override_tier')
        .eq('google_purchase_token', purchaseToken)
        .single()

      if (!user) {
        console.error(`[RTDN] No user found for purchaseToken (first 20 chars): ${purchaseToken.substring(0, 20)}...`)
        return jsonResponse({ ok: true }) // Acknowledge to Pub/Sub
      }

      const productId = subscriptionId || ''
      const tier = PRODUCT_TO_TIER[productId] || 'free'

      // Get current subscription status from Google Play API
      let purchaseData: Record<string, unknown> | null = null
      try {
        const accessToken = await getGoogleAccessToken()
        const apiUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/purchases/subscriptionsv2/tokens/${purchaseToken}`
        const gpResponse = await fetch(apiUrl, {
          headers: { 'Authorization': `Bearer ${accessToken}` },
        })
        if (gpResponse.ok) {
          purchaseData = await gpResponse.json()
        }
      } catch (err) {
        console.error('[RTDN] Failed to fetch purchase data from Google:', err)
      }

      // Get expiration from purchase data
      let expiresAt: string | null = null
      if (purchaseData?.lineItems) {
        const lineItems = purchaseData.lineItems as Array<Record<string, unknown>>
        if (lineItems.length > 0 && lineItems[0].expiryTime) {
          expiresAt = new Date(lineItems[0].expiryTime as string).toISOString()
        }
      }

      switch (notificationType) {
        // Active / renewed
        case NOTIFICATION_TYPE.SUBSCRIPTION_RECOVERED:
        case NOTIFICATION_TYPE.SUBSCRIPTION_RENEWED:
        case NOTIFICATION_TYPE.SUBSCRIPTION_PURCHASED:
        case NOTIFICATION_TYPE.SUBSCRIPTION_RESTARTED: {
          const updateData: Record<string, unknown> = {
            subscription_status: 'active',
          }
          if (expiresAt) {
            updateData.subscription_expires_at = expiresAt
          }
          if (!user.subscription_override_tier) {
            const currentRank = TIER_RANK[user.subscription_tier || 'free']
            const newRank = TIER_RANK[tier]
            if (newRank >= currentRank || user.subscription_source === 'google') {
              updateData.subscription_tier = tier
              updateData.subscription_source = 'google'
            }
          }
          await supabase.from('users').update(updateData).eq('id', user.id)

          await supabase.from('subscription_events').insert({
            user_id: user.id,
            event_type: `google_${notificationType === NOTIFICATION_TYPE.SUBSCRIPTION_RENEWED ? 'renewed' : 'activated'}`,
            tier,
            metadata: { product_id: productId, notification_type: notificationType },
          })
          break
        }

        // Canceled (will expire at period end)
        case NOTIFICATION_TYPE.SUBSCRIPTION_CANCELED: {
          await supabase.from('subscription_events').insert({
            user_id: user.id,
            event_type: 'google_cancel_scheduled',
            tier,
            metadata: { product_id: productId },
          })
          break
        }

        // Expired or revoked — downgrade with fallback check
        case NOTIFICATION_TYPE.SUBSCRIPTION_EXPIRED:
        case NOTIFICATION_TYPE.SUBSCRIPTION_REVOKED: {
          // Check for other active subscriptions before downgrading
          const hasStripe = !!user.stripe_subscription_id
          const hasApple = !!user.apple_original_transaction_id

          if ((hasStripe || hasApple) && user.subscription_source !== 'google') {
            // Another source is controlling, just clear Google data
            await supabase.from('users').update({
              google_purchase_token: null,
              google_order_id: null,
            }).eq('id', user.id)
          } else if (hasStripe || hasApple) {
            // Google was controlling but another source exists — switch
            const newSource = hasApple ? 'apple' : 'stripe'
            await supabase.from('users').update({
              subscription_source: newSource,
              google_purchase_token: null,
              google_order_id: null,
            }).eq('id', user.id)
          } else {
            // No other subscription, downgrade to free
            await supabase.from('users').update({
              subscription_tier: 'free',
              subscription_status: 'canceled',
              subscription_source: null,
              google_purchase_token: null,
              google_order_id: null,
            }).eq('id', user.id)
          }

          await supabase.from('subscription_events').insert({
            user_id: user.id,
            event_type: `google_${notificationType === NOTIFICATION_TYPE.SUBSCRIPTION_EXPIRED ? 'expired' : 'revoked'}`,
            tier: 'free',
            metadata: { product_id: productId },
          })
          break
        }

        // Grace period / on hold — past due
        case NOTIFICATION_TYPE.SUBSCRIPTION_IN_GRACE_PERIOD:
        case NOTIFICATION_TYPE.SUBSCRIPTION_ON_HOLD: {
          await supabase.from('users').update({
            subscription_status: 'past_due',
          }).eq('id', user.id)
          break
        }

        // Paused
        case NOTIFICATION_TYPE.SUBSCRIPTION_PAUSED: {
          await supabase.from('subscription_events').insert({
            user_id: user.id,
            event_type: 'google_paused',
            tier,
            metadata: { product_id: productId },
          })
          break
        }

        default:
          console.log(`[RTDN] Unhandled notification type: ${notificationType}`)
      }

      return jsonResponse({ ok: true })
    } catch (err) {
      console.error('[RTDN] Error processing notification:', err)
      // Always return 200 to Pub/Sub to prevent retries
      return jsonResponse({ ok: true })
    }
  }

  return errorResponse('Method not allowed', 405)
})

// ============================================================
// Helper: Get Google OAuth2 access token from service account
// ============================================================
async function getGoogleAccessToken(): Promise<string> {
  // Return cached token if still valid (with 60s buffer)
  if (cachedAccessToken && Date.now() < cachedAccessToken.expiresAt - 60000) {
    return cachedAccessToken.token
  }

  const keyJson = JSON.parse(atob(GOOGLE_SERVICE_ACCOUNT_KEY_BASE64))
  const privateKey = await jose.importPKCS8(keyJson.private_key, 'RS256')

  const now = Math.floor(Date.now() / 1000)
  const jwt = await new jose.SignJWT({
    scope: 'https://www.googleapis.com/auth/androidpublisher',
  })
    .setProtectedHeader({ alg: 'RS256', typ: 'JWT' })
    .setIssuer(keyJson.client_email)
    .setSubject(keyJson.client_email)
    .setAudience('https://oauth2.googleapis.com/token')
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(privateKey)

  const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: jwt,
    }),
  })

  if (!tokenResponse.ok) {
    const errText = await tokenResponse.text()
    throw new Error(`Failed to get Google access token: ${errText}`)
  }

  const tokenData = await tokenResponse.json()

  cachedAccessToken = {
    token: tokenData.access_token,
    expiresAt: Date.now() + (tokenData.expires_in * 1000),
  }

  return cachedAccessToken.token
}
