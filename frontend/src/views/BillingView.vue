<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getSubscriptionInfo,
  getInvoices,
  createPortalSession,
  cancelSubscription,
  reactivateSubscription,
  formatAmount,
  formatDate,
  getStatusColor,
  type SubscriptionInfo,
  type Invoice,
} from '@/services/billingApi'
import { TIER_FEATURES, TIER_NAMES, TIER_PRICES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const actionLoading = ref<string | null>(null)
const error = ref('')
const subscriptionInfo = ref<SubscriptionInfo | null>(null)
const invoices = ref<Invoice[]>([])
const invoicePage = ref(1)
const invoiceTotal = ref(0)
const hasMoreInvoices = ref(false)
const loadingInvoices = ref(false)
const showCancelModal = ref(false)
const selectedInvoice = ref<Invoice | null>(null)

const currentTier = computed(() => {
  if (!auth.user.value) return 'free'
  return getEffectiveTier(auth.user.value)
})

const isCancelled = computed(() => {
  return subscriptionInfo.value?.subscription.status === 'canceled'
})

const isPastDue = computed(() => {
  return subscriptionInfo.value?.subscription.status === 'past_due'
})

onMounted(async () => {
  if (!auth.isAuthenticated.value) {
    router.push('/login?redirect=/billing')
    return
  }
  await loadBillingInfo()
})

async function loadBillingInfo() {
  loading.value = true
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const [subInfo, invoiceData] = await Promise.all([
      getSubscriptionInfo(token),
      getInvoices(token, 1),
    ])

    subscriptionInfo.value = subInfo
    invoices.value = invoiceData.invoices
    invoiceTotal.value = invoiceData.total
    hasMoreInvoices.value = invoiceData.hasMore
    invoicePage.value = 1
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load billing info'
  } finally {
    loading.value = false
  }
}

async function loadMoreInvoices() {
  if (loadingInvoices.value || !hasMoreInvoices.value) return

  loadingInvoices.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const data = await getInvoices(token, invoicePage.value + 1)
    invoices.value = [...invoices.value, ...data.invoices]
    hasMoreInvoices.value = data.hasMore
    invoicePage.value = data.page
  } catch (err) {
    console.error('Failed to load more invoices:', err)
  } finally {
    loadingInvoices.value = false
  }
}

async function handleManagePayment() {
  actionLoading.value = 'portal'
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const result = await createPortalSession(token, window.location.href)
    window.location.href = result.url
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to open billing portal'
    actionLoading.value = null
  }
}

async function handleCancelSubscription() {
  actionLoading.value = 'cancel'
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await cancelSubscription(token)
    showCancelModal.value = false
    await loadBillingInfo()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to cancel subscription'
  } finally {
    actionLoading.value = null
  }
}

async function handleReactivate() {
  actionLoading.value = 'reactivate'
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await reactivateSubscription(token)
    await loadBillingInfo()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to reactivate subscription'
  } finally {
    actionLoading.value = null
  }
}

function formatCardBrand(brand: string): string {
  const brands: Record<string, string> = {
    visa: 'Visa',
    mastercard: 'Mastercard',
    amex: 'American Express',
    discover: 'Discover',
    diners: 'Diners Club',
    jcb: 'JCB',
    unionpay: 'UnionPay',
  }
  return brands[brand.toLowerCase()] || brand
}

function getTierColor(tier: SubscriptionTier): string {
  switch (tier) {
    case 'pro':
      return 'bg-purple-100 text-purple-800 border-purple-200'
    case 'basic':
      return 'bg-blue-100 text-blue-800 border-blue-200'
    case 'premium':
      return 'bg-yellow-100 text-yellow-800 border-yellow-200'
    default:
      return 'bg-gray-100 text-gray-800 border-gray-200'
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="container-wide max-w-4xl">
      <!-- Header -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900">Billing & Subscription</h1>
        <p class="text-gray-600 mt-1">Manage your subscription and payment methods</p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex justify-center py-12">
        <svg class="animate-spin h-8 w-8 text-primary-500" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
      </div>

      <!-- Error Message -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6">
        {{ error }}
        <button @click="loadBillingInfo" class="ml-2 underline">Retry</button>
      </div>

      <template v-else>
        <!-- Current Plan Card -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h2 class="text-xl font-semibold text-gray-900 mb-1">Current Plan</h2>
              <div class="flex items-center gap-3">
                <span
                  class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border"
                  :class="getTierColor(currentTier)"
                >
                  {{ TIER_NAMES[currentTier] }}
                </span>
                <span v-if="subscriptionInfo?.subscription.hasOverride" class="text-sm text-gray-500">
                  (Complimentary)
                </span>
                <span v-if="isCancelled" class="text-sm text-orange-600 font-medium">
                  Cancels {{ subscriptionInfo?.subscription.expiresAt ? formatDate(subscriptionInfo.subscription.expiresAt) : 'soon' }}
                </span>
                <span v-if="isPastDue" class="text-sm text-red-600 font-medium">
                  Payment past due
                </span>
              </div>
            </div>
            <div class="text-right">
              <div class="text-2xl font-bold text-gray-900">
                ${{ TIER_PRICES[currentTier] }}
                <span v-if="TIER_PRICES[currentTier] > 0" class="text-base font-normal text-gray-500">/month</span>
              </div>
            </div>
          </div>

          <!-- Features List -->
          <div class="mb-6">
            <h3 class="text-sm font-medium text-gray-700 mb-3">Your plan includes:</h3>
            <ul class="grid md:grid-cols-2 gap-2">
              <li
                v-for="feature in TIER_FEATURES[currentTier]"
                :key="feature"
                class="flex items-center gap-2 text-sm text-gray-600"
              >
                <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                {{ feature }}
              </li>
            </ul>
          </div>

          <!-- Plan Actions -->
          <div class="flex flex-wrap gap-3">
            <router-link
              to="/pricing"
              class="btn btn-primary"
            >
              {{ currentTier === 'free' ? 'Upgrade Plan' : 'Change Plan' }}
            </router-link>

            <button
              v-if="subscriptionInfo?.hasActiveSubscription && !isCancelled && !subscriptionInfo?.subscription.hasOverride"
              @click="showCancelModal = true"
              class="btn btn-secondary text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              Cancel Subscription
            </button>

            <button
              v-if="isCancelled && subscriptionInfo?.hasActiveSubscription"
              @click="handleReactivate"
              :disabled="actionLoading === 'reactivate'"
              class="btn btn-primary"
            >
              <span v-if="actionLoading === 'reactivate'" class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Reactivating...
              </span>
              <span v-else>Reactivate Subscription</span>
            </button>
          </div>
        </div>

        <!-- Payment Method Card -->
        <div v-if="subscriptionInfo?.hasStripeAccount" class="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div class="flex items-start justify-between">
            <div>
              <h2 class="text-xl font-semibold text-gray-900 mb-3">Payment Method</h2>
              <div v-if="subscriptionInfo.paymentMethod" class="flex items-center gap-3">
                <div class="w-12 h-8 bg-gray-100 rounded flex items-center justify-center">
                  <svg class="w-8 h-5 text-gray-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20,8H4V6H20M20,18H4V12H20M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
                  </svg>
                </div>
                <div>
                  <p class="font-medium text-gray-900">
                    {{ formatCardBrand(subscriptionInfo.paymentMethod.brand) }} ending in {{ subscriptionInfo.paymentMethod.last4 }}
                  </p>
                  <p class="text-sm text-gray-500">
                    Expires {{ subscriptionInfo.paymentMethod.expMonth }}/{{ subscriptionInfo.paymentMethod.expYear }}
                  </p>
                </div>
              </div>
              <p v-else class="text-gray-500">No payment method on file</p>
            </div>
            <button
              @click="handleManagePayment"
              :disabled="actionLoading === 'portal'"
              class="btn btn-secondary"
            >
              <span v-if="actionLoading === 'portal'" class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Loading...
              </span>
              <span v-else>{{ subscriptionInfo.paymentMethod ? 'Update' : 'Add Payment Method' }}</span>
            </button>
          </div>
        </div>

        <!-- Invoice History -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 class="text-xl font-semibold text-gray-900 mb-4">Invoice History</h2>

          <div v-if="invoices.length === 0" class="text-center py-8 text-gray-500">
            No invoices yet
          </div>

          <div v-else>
            <!-- Invoice Table -->
            <div class="overflow-x-auto">
              <table class="w-full">
                <thead>
                  <tr class="text-left text-sm text-gray-500 border-b">
                    <th class="pb-3 font-medium">Date</th>
                    <th class="pb-3 font-medium">Amount</th>
                    <th class="pb-3 font-medium">Status</th>
                    <th class="pb-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-100">
                  <tr
                    v-for="invoice in invoices"
                    :key="invoice.id"
                    class="hover:bg-gray-50"
                  >
                    <td class="py-3 text-gray-900">
                      {{ formatDate(invoice.invoiceDate) }}
                    </td>
                    <td class="py-3 text-gray-900">
                      {{ formatAmount(invoice.amountCents, invoice.currency) }}
                      <span v-if="invoice.taxCents > 0" class="text-xs text-gray-500">
                        (incl. {{ formatAmount(invoice.taxCents, invoice.currency) }} tax)
                      </span>
                    </td>
                    <td class="py-3">
                      <span
                        class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium capitalize"
                        :class="getStatusColor(invoice.status)"
                      >
                        {{ invoice.status }}
                      </span>
                    </td>
                    <td class="py-3 text-right">
                      <div class="flex items-center justify-end gap-2">
                        <button
                          @click="selectedInvoice = invoice"
                          class="text-sm text-primary-600 hover:text-primary-700"
                        >
                          View
                        </button>
                        <a
                          v-if="invoice.invoicePdfUrl"
                          :href="invoice.invoicePdfUrl"
                          target="_blank"
                          class="text-sm text-gray-600 hover:text-gray-700"
                        >
                          PDF
                        </a>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Load More -->
            <div v-if="hasMoreInvoices" class="mt-4 text-center">
              <button
                @click="loadMoreInvoices"
                :disabled="loadingInvoices"
                class="btn btn-secondary"
              >
                <span v-if="loadingInvoices" class="flex items-center gap-2">
                  <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                  </svg>
                  Loading...
                </span>
                <span v-else>Load More</span>
              </button>
            </div>

            <p class="mt-4 text-sm text-gray-500 text-center">
              Showing {{ invoices.length }} of {{ invoiceTotal }} invoices
            </p>
          </div>
        </div>
      </template>
    </div>

    <!-- Cancel Subscription Modal -->
    <Teleport to="body">
      <div
        v-if="showCancelModal"
        class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
        @click.self="showCancelModal = false"
      >
        <div class="bg-white rounded-xl shadow-xl max-w-md w-full p-6">
          <h3 class="text-xl font-semibold text-gray-900 mb-2">Cancel Subscription?</h3>
          <p class="text-gray-600 mb-4">
            Your subscription will remain active until the end of your current billing period
            <span v-if="subscriptionInfo?.subscription.expiresAt" class="font-medium">
              ({{ formatDate(subscriptionInfo.subscription.expiresAt) }})
            </span>.
            After that, you'll be downgraded to the Free plan.
          </p>
          <p class="text-gray-600 mb-6">
            You can reactivate your subscription at any time before it ends.
          </p>
          <div class="flex gap-3 justify-end">
            <button
              @click="showCancelModal = false"
              class="btn btn-secondary"
            >
              Keep Subscription
            </button>
            <button
              @click="handleCancelSubscription"
              :disabled="actionLoading === 'cancel'"
              class="btn bg-red-600 text-white hover:bg-red-700"
            >
              <span v-if="actionLoading === 'cancel'" class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Cancelling...
              </span>
              <span v-else>Cancel Subscription</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Invoice Detail Modal -->
    <Teleport to="body">
      <div
        v-if="selectedInvoice"
        class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
        @click.self="selectedInvoice = null"
      >
        <div class="bg-white rounded-xl shadow-xl max-w-lg w-full p-6">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h3 class="text-xl font-semibold text-gray-900">Invoice Details</h3>
              <p class="text-sm text-gray-500">{{ selectedInvoice.stripeInvoiceId }}</p>
            </div>
            <button
              @click="selectedInvoice = null"
              class="text-gray-400 hover:text-gray-600"
            >
              <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
            </button>
          </div>

          <div class="space-y-4">
            <div class="flex justify-between py-2 border-b border-gray-100">
              <span class="text-gray-600">Date</span>
              <span class="text-gray-900 font-medium">{{ formatDate(selectedInvoice.invoiceDate) }}</span>
            </div>
            <div v-if="selectedInvoice.periodStart && selectedInvoice.periodEnd" class="flex justify-between py-2 border-b border-gray-100">
              <span class="text-gray-600">Period</span>
              <span class="text-gray-900">
                {{ formatDate(selectedInvoice.periodStart) }} - {{ formatDate(selectedInvoice.periodEnd) }}
              </span>
            </div>
            <div class="flex justify-between py-2 border-b border-gray-100">
              <span class="text-gray-600">Status</span>
              <span
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium capitalize"
                :class="getStatusColor(selectedInvoice.status)"
              >
                {{ selectedInvoice.status }}
              </span>
            </div>
            <div v-if="selectedInvoice.paymentMethodBrand" class="flex justify-between py-2 border-b border-gray-100">
              <span class="text-gray-600">Payment Method</span>
              <span class="text-gray-900">
                {{ formatCardBrand(selectedInvoice.paymentMethodBrand) }}
                <span v-if="selectedInvoice.paymentMethodLast4">**** {{ selectedInvoice.paymentMethodLast4 }}</span>
              </span>
            </div>
            <div v-if="selectedInvoice.taxCents > 0" class="flex justify-between py-2 border-b border-gray-100">
              <span class="text-gray-600">Tax</span>
              <span class="text-gray-900">{{ formatAmount(selectedInvoice.taxCents, selectedInvoice.currency) }}</span>
            </div>
            <div class="flex justify-between py-2 text-lg">
              <span class="text-gray-900 font-medium">Total</span>
              <span class="text-gray-900 font-bold">{{ formatAmount(selectedInvoice.amountCents, selectedInvoice.currency) }}</span>
            </div>
          </div>

          <div class="mt-6 flex gap-3">
            <a
              v-if="selectedInvoice.hostedInvoiceUrl"
              :href="selectedInvoice.hostedInvoiceUrl"
              target="_blank"
              class="btn btn-primary flex-1 text-center"
            >
              View Receipt
            </a>
            <a
              v-if="selectedInvoice.invoicePdfUrl"
              :href="selectedInvoice.invoicePdfUrl"
              target="_blank"
              class="btn btn-secondary flex-1 text-center"
            >
              Download PDF
            </a>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
