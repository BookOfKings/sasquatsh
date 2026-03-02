import Stripe from 'https://esm.sh/stripe@14.14.0?target=deno'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')!
const webhookSecret = Deno.env.get('STRIPE_WEBHOOK_SECRET')!
const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const stripe = new Stripe(stripeSecretKey, {
  apiVersion: '2023-10-16',
  httpClient: Stripe.createFetchHttpClient(),
})

// Map Stripe price IDs to tiers
const PRICE_TO_TIER: Record<string, string> = {
  [Deno.env.get('STRIPE_PRICE_BASIC') || '']: 'basic',
  [Deno.env.get('STRIPE_PRICE_PRO') || '']: 'pro',
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 })
  }

  const signature = req.headers.get('stripe-signature')
  if (!signature) {
    return new Response('Missing stripe-signature header', { status: 400 })
  }

  const body = await req.text()
  let event: Stripe.Event

  try {
    event = await stripe.webhooks.constructEventAsync(
      body,
      signature,
      webhookSecret
    )
  } catch (err) {
    console.error('Webhook signature verification failed:', err)
    return new Response(`Webhook Error: ${err instanceof Error ? err.message : 'Unknown error'}`, { status: 400 })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  console.log(`Processing webhook event: ${event.type}`)

  try {
    switch (event.type) {
      case 'checkout.session.completed': {
        const session = event.data.object as Stripe.Checkout.Session
        await handleCheckoutCompleted(supabase, session, event.id)
        break
      }

      case 'customer.subscription.created':
      case 'customer.subscription.updated': {
        const subscription = event.data.object as Stripe.Subscription
        await handleSubscriptionUpdate(supabase, subscription, event.id)
        break
      }

      case 'customer.subscription.deleted': {
        const subscription = event.data.object as Stripe.Subscription
        await handleSubscriptionDeleted(supabase, subscription, event.id)
        break
      }

      case 'invoice.paid': {
        const invoice = event.data.object as Stripe.Invoice
        await handleInvoicePaid(supabase, invoice)
        break
      }

      case 'invoice.payment_failed': {
        const invoice = event.data.object as Stripe.Invoice
        await handleInvoicePaymentFailed(supabase, invoice)
        break
      }

      default:
        console.log(`Unhandled event type: ${event.type}`)
    }

    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  } catch (err) {
    console.error('Error processing webhook:', err)
    return new Response(
      JSON.stringify({ error: err instanceof Error ? err.message : 'Unknown error' }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})

async function handleCheckoutCompleted(
  supabase: ReturnType<typeof createClient>,
  session: Stripe.Checkout.Session,
  eventId: string
) {
  const userId = session.metadata?.supabase_user_id
  const tier = session.metadata?.tier

  if (!userId || !tier) {
    console.error('Missing metadata in checkout session')
    return
  }

  // Get user's current tier for logging
  const { data: user } = await supabase
    .from('users')
    .select('subscription_tier')
    .eq('id', userId)
    .single()

  const oldTier = user?.subscription_tier || 'free'

  // Update user subscription
  await supabase
    .from('users')
    .update({
      subscription_tier: tier,
      stripe_subscription_id: session.subscription as string,
      subscription_status: 'active',
      updated_at: new Date().toISOString(),
    })
    .eq('id', userId)

  // Log subscription event
  await supabase.from('subscription_events').insert({
    user_id: userId,
    event_type: 'upgrade',
    old_tier: oldTier,
    new_tier: tier,
    stripe_event_id: eventId,
    notes: 'Checkout completed',
  })

  console.log(`User ${userId} upgraded from ${oldTier} to ${tier}`)
}

async function handleSubscriptionUpdate(
  supabase: ReturnType<typeof createClient>,
  subscription: Stripe.Subscription,
  eventId: string
) {
  const customerId = subscription.customer as string

  // Find user by Stripe customer ID
  const { data: user } = await supabase
    .from('users')
    .select('id, subscription_tier')
    .eq('stripe_customer_id', customerId)
    .single()

  if (!user) {
    console.error('User not found for customer:', customerId)
    return
  }

  // Determine tier from price
  const priceId = subscription.items.data[0]?.price?.id
  const newTier = priceId ? PRICE_TO_TIER[priceId] : null

  // Map Stripe status to our status
  let status = 'active'
  if (subscription.status === 'past_due') status = 'past_due'
  else if (subscription.status === 'canceled') status = 'canceled'
  else if (subscription.status === 'incomplete') status = 'incomplete'

  const updates: Record<string, unknown> = {
    stripe_subscription_id: subscription.id,
    subscription_status: status,
    subscription_expires_at: subscription.current_period_end
      ? new Date(subscription.current_period_end * 1000).toISOString()
      : null,
    updated_at: new Date().toISOString(),
  }

  // Only update tier if we can determine it
  if (newTier) {
    updates.subscription_tier = newTier
  }

  await supabase.from('users').update(updates).eq('id', user.id)

  // Log if tier changed
  if (newTier && newTier !== user.subscription_tier) {
    await supabase.from('subscription_events').insert({
      user_id: user.id,
      event_type: newTier > user.subscription_tier ? 'upgrade' : 'downgrade',
      old_tier: user.subscription_tier,
      new_tier: newTier,
      stripe_event_id: eventId,
      notes: `Subscription ${subscription.status}`,
    })
  }
}

async function handleSubscriptionDeleted(
  supabase: ReturnType<typeof createClient>,
  subscription: Stripe.Subscription,
  eventId: string
) {
  const customerId = subscription.customer as string

  const { data: user } = await supabase
    .from('users')
    .select('id, subscription_tier')
    .eq('stripe_customer_id', customerId)
    .single()

  if (!user) {
    console.error('User not found for customer:', customerId)
    return
  }

  const oldTier = user.subscription_tier

  // Downgrade to free
  await supabase
    .from('users')
    .update({
      subscription_tier: 'free',
      stripe_subscription_id: null,
      subscription_status: 'canceled',
      updated_at: new Date().toISOString(),
    })
    .eq('id', user.id)

  // Log event
  await supabase.from('subscription_events').insert({
    user_id: user.id,
    event_type: 'cancel',
    old_tier: oldTier,
    new_tier: 'free',
    stripe_event_id: eventId,
    notes: 'Subscription deleted/canceled',
  })

  console.log(`User ${user.id} subscription canceled, downgraded to free`)
}

async function handleInvoicePaid(
  supabase: ReturnType<typeof createClient>,
  invoice: Stripe.Invoice
) {
  const customerId = invoice.customer as string

  const { data: user } = await supabase
    .from('users')
    .select('id')
    .eq('stripe_customer_id', customerId)
    .single()

  if (!user) {
    console.log('User not found for invoice, may be new customer')
    return
  }

  // Get payment method details
  let paymentMethodBrand: string | null = null
  let paymentMethodLast4: string | null = null

  if (invoice.payment_intent && typeof invoice.payment_intent === 'string') {
    try {
      const paymentIntent = await stripe.paymentIntents.retrieve(invoice.payment_intent, {
        expand: ['payment_method'],
      })
      const pm = paymentIntent.payment_method as Stripe.PaymentMethod | null
      if (pm?.card) {
        paymentMethodBrand = pm.card.brand
        paymentMethodLast4 = pm.card.last4
      }
    } catch (err) {
      console.error('Failed to get payment method details:', err)
    }
  }

  // Cache invoice
  await supabase.from('stripe_invoices').upsert({
    user_id: user.id,
    stripe_invoice_id: invoice.id,
    stripe_subscription_id: invoice.subscription as string | null,
    amount_cents: invoice.amount_paid,
    tax_cents: invoice.tax || 0,
    currency: invoice.currency,
    status: 'paid',
    invoice_date: new Date((invoice.created || 0) * 1000).toISOString(),
    period_start: invoice.period_start
      ? new Date(invoice.period_start * 1000).toISOString()
      : null,
    period_end: invoice.period_end
      ? new Date(invoice.period_end * 1000).toISOString()
      : null,
    hosted_invoice_url: invoice.hosted_invoice_url,
    invoice_pdf_url: invoice.invoice_pdf,
    payment_method_brand: paymentMethodBrand,
    payment_method_last4: paymentMethodLast4,
  }, {
    onConflict: 'stripe_invoice_id',
  })

  // Reset subscription status to active if it was past_due
  await supabase
    .from('users')
    .update({
      subscription_status: 'active',
      updated_at: new Date().toISOString(),
    })
    .eq('id', user.id)
    .eq('subscription_status', 'past_due')

  console.log(`Invoice ${invoice.id} paid for user ${user.id}`)
}

async function handleInvoicePaymentFailed(
  supabase: ReturnType<typeof createClient>,
  invoice: Stripe.Invoice
) {
  const customerId = invoice.customer as string

  const { data: user } = await supabase
    .from('users')
    .select('id')
    .eq('stripe_customer_id', customerId)
    .single()

  if (!user) {
    return
  }

  // Update subscription status
  await supabase
    .from('users')
    .update({
      subscription_status: 'past_due',
      updated_at: new Date().toISOString(),
    })
    .eq('id', user.id)

  // Cache failed invoice
  await supabase.from('stripe_invoices').upsert({
    user_id: user.id,
    stripe_invoice_id: invoice.id,
    stripe_subscription_id: invoice.subscription as string | null,
    amount_cents: invoice.amount_due,
    tax_cents: invoice.tax || 0,
    currency: invoice.currency,
    status: 'open',
    invoice_date: new Date((invoice.created || 0) * 1000).toISOString(),
    hosted_invoice_url: invoice.hosted_invoice_url,
    invoice_pdf_url: invoice.invoice_pdf,
  }, {
    onConflict: 'stripe_invoice_id',
  })

  console.log(`Invoice payment failed for user ${user.id}`)
}
