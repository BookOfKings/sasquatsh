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

const PAGE_SIZE = 15

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

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
    .select('id, email, subscription_tier, subscription_expires_at, subscription_status, subscription_override_tier, stripe_customer_id, stripe_subscription_id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)
  const include = url.searchParams.get('include')
  const invoiceId = url.searchParams.get('invoiceId')
  const action = url.searchParams.get('action')
  const page = parseInt(url.searchParams.get('page') || '1', 10)

  // GET - Get billing info
  if (req.method === 'GET') {
    // Get single invoice detail
    if (invoiceId) {
      const { data: invoice, error } = await supabase
        .from('stripe_invoices')
        .select('*')
        .eq('user_id', user.id)
        .eq('id', invoiceId)
        .single()

      if (error || !invoice) {
        return errorResponse('Invoice not found', 404)
      }

      return jsonResponse({
        invoice: transformInvoice(invoice),
      })
    }

    // Get invoices list
    if (include === 'invoices') {
      const offset = (page - 1) * PAGE_SIZE

      const { data: invoices, error, count } = await supabase
        .from('stripe_invoices')
        .select('*', { count: 'exact' })
        .eq('user_id', user.id)
        .order('invoice_date', { ascending: false })
        .range(offset, offset + PAGE_SIZE - 1)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        invoices: (invoices || []).map(transformInvoice),
        total: count || 0,
        page,
        pageSize: PAGE_SIZE,
        hasMore: (count || 0) > offset + PAGE_SIZE,
      })
    }

    // Get subscription info with payment method
    let paymentMethod = null
    let cancelAtPeriodEnd = false
    let cancelAt: string | null = null

    if (user.stripe_customer_id) {
      try {
        // First try to get payment method from customer's invoice settings
        const customer = await stripe.customers.retrieve(user.stripe_customer_id, {
          expand: ['invoice_settings.default_payment_method'],
        }) as Stripe.Customer

        let pm = customer.invoice_settings?.default_payment_method as Stripe.PaymentMethod | null

        // If not found, try to get it from the active subscription
        if (user.stripe_subscription_id) {
          const subscription = await stripe.subscriptions.retrieve(user.stripe_subscription_id, {
            expand: ['default_payment_method'],
          })

          // Check if subscription is scheduled for cancellation
          cancelAtPeriodEnd = subscription.cancel_at_period_end
          if (subscription.cancel_at) {
            cancelAt = new Date(subscription.cancel_at * 1000).toISOString()
          }

          if (!pm?.card) {
            pm = subscription.default_payment_method as Stripe.PaymentMethod | null
          }
        }

        if (pm?.card) {
          paymentMethod = {
            brand: pm.card.brand,
            last4: pm.card.last4,
            expMonth: pm.card.exp_month,
            expYear: pm.card.exp_year,
          }
        }
      } catch (err) {
        console.error('Failed to get payment method:', err)
      }
    }

    // Calculate effective tier
    const effectiveTier = user.subscription_override_tier || user.subscription_tier || 'free'

    return jsonResponse({
      subscription: {
        tier: user.subscription_tier || 'free',
        effectiveTier,
        status: user.subscription_status || 'active',
        expiresAt: user.subscription_expires_at,
        hasOverride: !!user.subscription_override_tier,
        cancelAtPeriodEnd,
        cancelAt,
      },
      paymentMethod,
      hasStripeAccount: !!user.stripe_customer_id,
      hasActiveSubscription: !!user.stripe_subscription_id,
    })
  }

  // POST - Actions
  if (req.method === 'POST') {
    // Cancel subscription
    if (action === 'cancel') {
      if (!user.stripe_subscription_id) {
        return errorResponse('No active subscription to cancel', 400)
      }

      try {
        // Cancel at period end (not immediately)
        await stripe.subscriptions.update(user.stripe_subscription_id, {
          cancel_at_period_end: true,
        })

        // Log event
        await supabase.from('subscription_events').insert({
          user_id: user.id,
          event_type: 'cancel_scheduled',
          old_tier: user.subscription_tier,
          new_tier: user.subscription_tier, // Still active until period end
          notes: 'User requested cancellation at period end',
        })

        return jsonResponse({
          message: 'Subscription will be cancelled at the end of the current billing period',
          cancelAt: user.subscription_expires_at,
        })
      } catch (err) {
        console.error('Cancel subscription error:', err)
        return errorResponse(err instanceof Error ? err.message : 'Failed to cancel subscription', 500)
      }
    }

    // Reactivate cancelled subscription
    if (action === 'reactivate') {
      if (!user.stripe_subscription_id) {
        return errorResponse('No subscription to reactivate', 400)
      }

      try {
        await stripe.subscriptions.update(user.stripe_subscription_id, {
          cancel_at_period_end: false,
        })

        // Log event
        await supabase.from('subscription_events').insert({
          user_id: user.id,
          event_type: 'reactivate',
          old_tier: user.subscription_tier,
          new_tier: user.subscription_tier,
          notes: 'User reactivated subscription',
        })

        return jsonResponse({
          message: 'Subscription reactivated',
        })
      } catch (err) {
        console.error('Reactivate subscription error:', err)
        return errorResponse(err instanceof Error ? err.message : 'Failed to reactivate subscription', 500)
      }
    }

    return errorResponse('Invalid action', 400)
  }

  return errorResponse('Method not allowed', 405)
})

function transformInvoice(row: Record<string, unknown>) {
  return {
    id: row.id,
    stripeInvoiceId: row.stripe_invoice_id,
    amountCents: row.amount_cents,
    taxCents: row.tax_cents,
    currency: row.currency,
    status: row.status,
    invoiceDate: row.invoice_date,
    periodStart: row.period_start,
    periodEnd: row.period_end,
    hostedInvoiceUrl: row.hosted_invoice_url,
    invoicePdfUrl: row.invoice_pdf_url,
    paymentMethodBrand: row.payment_method_brand,
    paymentMethodLast4: row.payment_method_last4,
  }
}
