import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import * as jose from 'https://esm.sh/jose@5'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Apple App Store Server API credentials
const APPLE_BUNDLE_ID = 'com.sasquatsh.ios'
const APPLE_ISSUER_ID = Deno.env.get('APPLE_ISSUER_ID') || ''
const APPLE_KEY_ID = Deno.env.get('APPLE_KEY_ID') || ''
const APPLE_PRIVATE_KEY_BASE64 = Deno.env.get('APPLE_PRIVATE_KEY') || ''

// Product ID → tier mapping
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

// Apple App Store Server API base URLs
const APPLE_API_PRODUCTION = 'https://api.storekit.itunes.apple.com'
const APPLE_API_SANDBOX = 'https://api.storekit-sandbox.itunes.apple.com'

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const action = url.searchParams.get('action')

  // ============================================================
  // CLIENT-INITIATED: Verify transaction after purchase
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
      .select('id, subscription_tier, subscription_source, stripe_subscription_id, apple_original_transaction_id, subscription_override_tier')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { transactionId, originalTransactionId, productId, environment } = body

    if (!transactionId || !productId) {
      return errorResponse('transactionId and productId are required', 400)
    }

    // Verify transaction with Apple
    try {
      console.log(`[IAP] Verifying transaction ${transactionId}, product ${productId}, env ${environment}`)

      if (!APPLE_ISSUER_ID || !APPLE_KEY_ID || !APPLE_PRIVATE_KEY_BASE64) {
        console.error('[IAP] Missing Apple credentials:', {
          hasIssuerId: !!APPLE_ISSUER_ID,
          hasKeyId: !!APPLE_KEY_ID,
          hasPrivateKey: !!APPLE_PRIVATE_KEY_BASE64,
        })
        return errorResponse('Apple API credentials not configured', 500)
      }

      const appleBaseUrl = environment === 'Sandbox' ? APPLE_API_SANDBOX : APPLE_API_PRODUCTION
      console.log(`[IAP] Using Apple API: ${appleBaseUrl}`)

      let appleJwt: string
      try {
        appleJwt = await generateAppleJwt()
        console.log('[IAP] JWT generated successfully')
      } catch (jwtErr) {
        console.error('[IAP] JWT generation failed:', jwtErr)
        return errorResponse(`JWT generation failed: ${jwtErr.message}`, 500)
      }

      const appleResponse = await fetch(
        `${appleBaseUrl}/inApps/v1/transactions/${transactionId}`,
        {
          headers: { 'Authorization': `Bearer ${appleJwt}` },
        }
      )

      if (!appleResponse.ok) {
        const errText = await appleResponse.text()
        console.error(`[IAP] Apple API error ${appleResponse.status}:`, errText)
        return errorResponse(`Apple verification failed (${appleResponse.status}): ${errText}`, 502)
      }

      const appleData = await appleResponse.json()
      const signedTransaction = appleData.signedTransactionInfo

      // Decode the JWS (we trust Apple's signature since we got it from their API)
      const transactionPayload = decodeJwsPayload(signedTransaction)

      // Validate
      if (transactionPayload.bundleId !== APPLE_BUNDLE_ID) {
        return errorResponse('Bundle ID mismatch', 400)
      }

      if (transactionPayload.revocationDate) {
        return errorResponse('Transaction has been revoked', 400)
      }

      const tier = PRODUCT_TO_TIER[transactionPayload.productId]
      if (!tier) {
        return errorResponse(`Unknown product: ${transactionPayload.productId}`, 400)
      }

      // Conflict resolution: highest active tier wins
      const currentTierRank = TIER_RANK[user.subscription_override_tier || user.subscription_tier || 'free']
      const newTierRank = TIER_RANK[tier]

      const updateData: Record<string, unknown> = {
        apple_original_transaction_id: String(transactionPayload.originalTransactionId),
        subscription_status: 'active',
        subscription_expires_at: new Date(transactionPayload.expiresDate).toISOString(),
      }

      // Only upgrade, never downgrade
      if (newTierRank >= currentTierRank && !user.subscription_override_tier) {
        updateData.subscription_tier = tier
        updateData.subscription_source = 'apple'
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
        event_type: 'apple_purchase',
        tier,
        metadata: {
          product_id: transactionPayload.productId,
          transaction_id: String(transactionPayload.transactionId),
          original_transaction_id: String(transactionPayload.originalTransactionId),
          environment,
        },
      })

      return jsonResponse({ success: true, tier })
    } catch (err) {
      console.error('Verification error:', err)
      return errorResponse(`Verification failed: ${err.message}`, 500)
    }
  }

  // ============================================================
  // APPLE SERVER NOTIFICATIONS V2
  // POST (no action param, no Firebase auth)
  // ============================================================
  if (req.method === 'POST' && !action) {
    try {
      const body = await req.json()
      const { signedPayload } = body

      if (!signedPayload) {
        return errorResponse('Missing signedPayload', 400)
      }

      // Decode notification payload (Apple-signed JWS)
      const notificationPayload = decodeJwsPayload(signedPayload)
      const notificationType = notificationPayload.notificationType
      const subtype = notificationPayload.subtype

      console.log(`Apple notification: ${notificationType} / ${subtype || 'none'}`)

      // Decode the transaction info from the notification
      const signedTransactionInfo = notificationPayload.data?.signedTransactionInfo
      if (!signedTransactionInfo) {
        console.log('No transaction info in notification, acknowledging')
        return jsonResponse({ ok: true })
      }

      const transactionInfo = decodeJwsPayload(signedTransactionInfo)
      const originalTransactionId = String(transactionInfo.originalTransactionId)

      // Find user by Apple transaction ID
      const { data: user } = await supabase
        .from('users')
        .select('id, subscription_tier, subscription_source, stripe_subscription_id, subscription_override_tier')
        .eq('apple_original_transaction_id', originalTransactionId)
        .single()

      if (!user) {
        console.error(`No user found for originalTransactionId: ${originalTransactionId}`)
        return jsonResponse({ ok: true }) // Acknowledge to Apple
      }

      const productId = transactionInfo.productId
      const tier = PRODUCT_TO_TIER[productId] || 'free'

      switch (notificationType) {
        case 'DID_RENEW':
        case 'SUBSCRIBED': {
          const updateData: Record<string, unknown> = {
            subscription_status: 'active',
            subscription_expires_at: new Date(transactionInfo.expiresDate).toISOString(),
          }
          if (!user.subscription_override_tier) {
            const currentRank = TIER_RANK[user.subscription_tier || 'free']
            const newRank = TIER_RANK[tier]
            if (newRank >= currentRank || user.subscription_source === 'apple') {
              updateData.subscription_tier = tier
              updateData.subscription_source = 'apple'
            }
          }
          await supabase.from('users').update(updateData).eq('id', user.id)
          break
        }

        case 'EXPIRED':
        case 'REVOKE':
        case 'GRACE_PERIOD_EXPIRED': {
          // Check if user has active Stripe subscription before downgrading
          if (user.stripe_subscription_id && user.subscription_source !== 'apple') {
            // Stripe is the controlling source, just clear Apple data
            await supabase.from('users').update({
              apple_original_transaction_id: null,
            }).eq('id', user.id)
          } else if (user.stripe_subscription_id) {
            // Apple was controlling but Stripe exists — switch to Stripe
            // We don't know the Stripe tier here, so set source to stripe
            // The next billing check will resolve the correct tier
            await supabase.from('users').update({
              subscription_source: 'stripe',
              apple_original_transaction_id: null,
            }).eq('id', user.id)
          } else {
            // No other subscription, downgrade to free
            await supabase.from('users').update({
              subscription_tier: 'free',
              subscription_status: 'canceled',
              subscription_source: null,
              apple_original_transaction_id: null,
            }).eq('id', user.id)
          }

          await supabase.from('subscription_events').insert({
            user_id: user.id,
            event_type: `apple_${notificationType.toLowerCase()}`,
            tier: 'free',
            metadata: { product_id: productId, original_transaction_id: originalTransactionId },
          })
          break
        }

        case 'DID_FAIL_TO_RENEW': {
          await supabase.from('users').update({
            subscription_status: 'past_due',
          }).eq('id', user.id)
          break
        }

        case 'DID_CHANGE_RENEWAL_INFO': {
          // User changed auto-renew product (upgrade/downgrade pending)
          // The actual change happens on next renewal, log it
          const renewalInfo = notificationPayload.data?.signedRenewalInfo
          if (renewalInfo) {
            const renewalPayload = decodeJwsPayload(renewalInfo)
            await supabase.from('subscription_events').insert({
              user_id: user.id,
              event_type: 'apple_renewal_change',
              tier,
              metadata: {
                auto_renew_product_id: renewalPayload.autoRenewProductId,
                original_transaction_id: originalTransactionId,
              },
            })
          }
          break
        }

        case 'DID_CHANGE_RENEWAL_STATUS': {
          // Auto-renew turned on/off
          const renewalInfo = notificationPayload.data?.signedRenewalInfo
          if (renewalInfo) {
            const renewalPayload = decodeJwsPayload(renewalInfo)
            if (!renewalPayload.autoRenewStatus) {
              // User turned off auto-renew (will expire at period end)
              await supabase.from('subscription_events').insert({
                user_id: user.id,
                event_type: 'apple_cancel_scheduled',
                tier,
                metadata: { original_transaction_id: originalTransactionId },
              })
            }
          }
          break
        }

        default:
          console.log(`Unhandled Apple notification type: ${notificationType}`)
      }

      return jsonResponse({ ok: true })
    } catch (err) {
      console.error('Apple notification error:', err)
      // Always return 200 to Apple so they don't retry
      return jsonResponse({ ok: true })
    }
  }

  return errorResponse('Method not allowed', 405)
})

// ============================================================
// Helper: Generate JWT for Apple App Store Server API
// ============================================================
async function generateAppleJwt(): Promise<string> {
  const privateKeyPem = atob(APPLE_PRIVATE_KEY_BASE64)
  const privateKey = await jose.importPKCS8(privateKeyPem, 'ES256')

  const jwt = await new jose.SignJWT({ bid: APPLE_BUNDLE_ID })
    .setProtectedHeader({ alg: 'ES256', kid: APPLE_KEY_ID, typ: 'JWT' })
    .setIssuer(APPLE_ISSUER_ID)
    .setIssuedAt()
    .setExpirationTime('20m')
    .setAudience('appstoreconnect-v1')
    .sign(privateKey)

  return jwt
}

// ============================================================
// Helper: Decode JWS payload without verification
// (Used for Apple-signed payloads that come directly from Apple's API)
// ============================================================
function decodeJwsPayload(jws: string): Record<string, any> {
  const parts = jws.split('.')
  if (parts.length !== 3) throw new Error('Invalid JWS format')
  const payload = parts[1]
  const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
  return JSON.parse(decoded)
}
