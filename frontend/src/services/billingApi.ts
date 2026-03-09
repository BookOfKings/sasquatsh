import type { SubscriptionTier } from '@/config/subscriptionLimits'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  return response.json() as Promise<T>
}

// ============ Types ============

export interface SubscriptionInfo {
  subscription: {
    tier: SubscriptionTier
    effectiveTier: SubscriptionTier
    status: 'active' | 'past_due' | 'canceled' | 'incomplete'
    expiresAt: string | null
    hasOverride: boolean
    cancelAtPeriodEnd: boolean
    cancelAt: string | null
  }
  paymentMethod: {
    brand: string
    last4: string
    expMonth: number
    expYear: number
  } | null
  hasStripeAccount: boolean
  hasActiveSubscription: boolean
}

export interface Invoice {
  id: string
  stripeInvoiceId: string
  amountCents: number
  taxCents: number
  currency: string
  status: 'paid' | 'open' | 'draft' | 'void' | 'uncollectible'
  invoiceDate: string
  periodStart: string | null
  periodEnd: string | null
  hostedInvoiceUrl: string | null
  invoicePdfUrl: string | null
  paymentMethodBrand: string | null
  paymentMethodLast4: string | null
}

export interface InvoicesResponse {
  invoices: Invoice[]
  total: number
  page: number
  pageSize: number
  hasMore: boolean
}

export interface CheckoutResponse {
  sessionId: string
  url: string
}

export interface PortalResponse {
  url: string
}

// ============ API Functions ============

// Get current subscription info
export async function getSubscriptionInfo(token: string): Promise<SubscriptionInfo> {
  return authenticatedRequest<SubscriptionInfo>('/billing', token)
}

// Get paginated invoice history
export async function getInvoices(token: string, page = 1): Promise<InvoicesResponse> {
  return authenticatedRequest<InvoicesResponse>(`/billing?include=invoices&page=${page}`, token)
}

// Get single invoice detail
export async function getInvoice(token: string, invoiceId: string): Promise<{ invoice: Invoice }> {
  return authenticatedRequest<{ invoice: Invoice }>(`/billing?invoiceId=${invoiceId}`, token)
}

// Create checkout session for upgrade
export async function createCheckoutSession(
  token: string,
  tier: 'basic' | 'pro',
  successUrl?: string,
  cancelUrl?: string
): Promise<CheckoutResponse> {
  return authenticatedRequest<CheckoutResponse>('/stripe-checkout', token, {
    method: 'POST',
    body: JSON.stringify({ tier, successUrl, cancelUrl }),
  })
}

// Create customer portal session
export async function createPortalSession(
  token: string,
  returnUrl?: string
): Promise<PortalResponse> {
  return authenticatedRequest<PortalResponse>('/stripe-portal', token, {
    method: 'POST',
    body: JSON.stringify({ returnUrl }),
  })
}

// Cancel subscription (at period end)
export async function cancelSubscription(
  token: string
): Promise<{ message: string; cancelAt: string }> {
  return authenticatedRequest<{ message: string; cancelAt: string }>(
    '/billing?action=cancel',
    token,
    { method: 'POST' }
  )
}

// Reactivate cancelled subscription
export async function reactivateSubscription(
  token: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(
    '/billing?action=reactivate',
    token,
    { method: 'POST' }
  )
}

// ============ Helpers ============

// Format currency amount
export function formatAmount(cents: number, currency = 'usd'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency.toUpperCase(),
  }).format(cents / 100)
}

// Format date
export function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

// Get status badge color
export function getStatusColor(status: Invoice['status']): string {
  switch (status) {
    case 'paid':
      return 'bg-green-100 text-green-800'
    case 'open':
      return 'bg-yellow-100 text-yellow-800'
    case 'draft':
      return 'bg-gray-100 text-gray-800'
    case 'void':
    case 'uncollectible':
      return 'bg-red-100 text-red-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}
