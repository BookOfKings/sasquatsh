<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { createCheckoutSession } from '@/services/billingApi'
import { TIER_FEATURES, TIER_PRICES, TIER_NAMES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'

const router = useRouter()
const auth = useAuthStore()

const loading = ref<SubscriptionTier | null>(null)
const error = ref('')

const currentTier = computed(() => {
  if (!auth.user.value) return 'free'
  return getEffectiveTier(auth.user.value)
})

const plans = [
  {
    tier: 'free' as SubscriptionTier,
    name: TIER_NAMES.free,
    price: TIER_PRICES.free,
    features: TIER_FEATURES.free,
    highlighted: false,
  },
  {
    tier: 'basic' as SubscriptionTier,
    name: TIER_NAMES.basic,
    price: TIER_PRICES.basic,
    features: TIER_FEATURES.basic,
    highlighted: true,
  },
  {
    tier: 'pro' as SubscriptionTier,
    name: TIER_NAMES.pro,
    price: TIER_PRICES.pro,
    features: TIER_FEATURES.pro,
    highlighted: false,
  },
]

async function handleSelectPlan(tier: SubscriptionTier) {
  if (tier === 'free') {
    // Free plan - just go to signup if not logged in
    if (!auth.isAuthenticated.value) {
      router.push('/signup')
    }
    return
  }

  if (!auth.isAuthenticated.value) {
    // Redirect to login with return URL
    router.push(`/login?redirect=/pricing&plan=${tier}`)
    return
  }

  if (tier === currentTier.value) {
    // Already on this plan
    return
  }

  loading.value = tier
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const result = await createCheckoutSession(
      token,
      tier as 'basic' | 'pro',
      `${window.location.origin}/profile?checkout=success`,
      `${window.location.origin}/pricing?checkout=cancelled`
    )

    // Redirect to Stripe Checkout
    window.location.href = result.url
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to start checkout'
    loading.value = null
  }
}

function getButtonText(tier: SubscriptionTier): string {
  if (!auth.isAuthenticated.value) {
    return tier === 'free' ? 'Get Started' : 'Sign Up'
  }

  if (tier === currentTier.value) {
    return 'Current Plan'
  }

  const tierOrder = ['free', 'basic', 'pro', 'premium']
  const currentIndex = tierOrder.indexOf(currentTier.value)
  const targetIndex = tierOrder.indexOf(tier)

  if (targetIndex > currentIndex) {
    return 'Upgrade'
  } else if (targetIndex < currentIndex) {
    return 'Downgrade'
  }

  return 'Select'
}

function isCurrentPlan(tier: SubscriptionTier): boolean {
  return auth.isAuthenticated.value && tier === currentTier.value
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-12">
    <div class="container-wide">
      <!-- Header -->
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-gray-900 mb-4">Simple, Transparent Pricing</h1>
        <p class="text-xl text-gray-600 max-w-2xl mx-auto">
          Choose the plan that's right for you. Upgrade or downgrade anytime.
        </p>
      </div>

      <!-- Error Message -->
      <div v-if="error" class="max-w-md mx-auto mb-8">
        <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {{ error }}
        </div>
      </div>

      <!-- Pricing Cards -->
      <div class="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto">
        <div
          v-for="plan in plans"
          :key="plan.tier"
          class="relative bg-white rounded-2xl shadow-sm border-2 transition-all duration-200"
          :class="[
            plan.highlighted
              ? 'border-primary-500 shadow-lg scale-105'
              : 'border-gray-200 hover:border-gray-300',
            isCurrentPlan(plan.tier) ? 'ring-2 ring-primary-500 ring-offset-2' : ''
          ]"
        >
          <!-- Popular Badge -->
          <div
            v-if="plan.highlighted"
            class="absolute -top-4 left-1/2 -translate-x-1/2 bg-primary-500 text-white text-sm font-medium px-4 py-1 rounded-full"
          >
            Most Popular
          </div>

          <!-- Current Plan Badge -->
          <div
            v-if="isCurrentPlan(plan.tier)"
            class="absolute -top-4 right-4 bg-green-500 text-white text-sm font-medium px-3 py-1 rounded-full"
          >
            Current
          </div>

          <div class="p-8">
            <!-- Plan Name -->
            <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ plan.name }}</h2>

            <!-- Price -->
            <div class="mb-6">
              <span class="text-4xl font-bold text-gray-900">
                ${{ plan.price }}
              </span>
              <span v-if="plan.price > 0" class="text-gray-500">/month</span>
              <span v-else class="text-gray-500">forever</span>
            </div>

            <!-- CTA Button -->
            <button
              @click="handleSelectPlan(plan.tier)"
              :disabled="loading === plan.tier || isCurrentPlan(plan.tier)"
              class="w-full py-3 px-6 rounded-lg font-semibold transition-colors mb-8"
              :class="[
                plan.highlighted
                  ? 'bg-primary-500 text-white hover:bg-primary-600 disabled:bg-primary-300'
                  : 'bg-gray-100 text-gray-900 hover:bg-gray-200 disabled:bg-gray-100 disabled:text-gray-400',
                isCurrentPlan(plan.tier) ? 'cursor-default' : ''
              ]"
            >
              <span v-if="loading === plan.tier" class="flex items-center justify-center gap-2">
                <svg class="animate-spin h-5 w-5" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                Processing...
              </span>
              <span v-else>{{ getButtonText(plan.tier) }}</span>
            </button>

            <!-- Features List -->
            <ul class="space-y-3">
              <li
                v-for="feature in plan.features"
                :key="feature"
                class="flex items-start gap-3"
              >
                <svg class="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                <span class="text-gray-600">{{ feature }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Enterprise Section -->
      <div class="mt-16 max-w-3xl mx-auto">
        <div class="bg-gradient-to-r from-primary-500 to-secondary-500 rounded-2xl p-8 text-white text-center">
          <h2 class="text-2xl font-bold mb-4">Need More?</h2>
          <p class="text-lg mb-6 opacity-90">
            Running a large gaming convention or need custom features? Let's talk about an Enterprise plan.
          </p>
          <router-link
            to="/contact"
            class="inline-flex items-center gap-2 bg-white text-primary-600 font-semibold px-6 py-3 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
            </svg>
            Contact Us
          </router-link>
        </div>
      </div>

      <!-- FAQ Section -->
      <div class="mt-16 max-w-3xl mx-auto">
        <h2 class="text-2xl font-bold text-center text-gray-900 mb-8">Frequently Asked Questions</h2>

        <div class="space-y-6">
          <div class="bg-white rounded-lg p-6 shadow-sm">
            <h3 class="font-semibold text-gray-900 mb-2">Can I change plans anytime?</h3>
            <p class="text-gray-600">Yes! You can upgrade or downgrade your plan at any time. Changes take effect immediately.</p>
          </div>

          <div class="bg-white rounded-lg p-6 shadow-sm">
            <h3 class="font-semibold text-gray-900 mb-2">What happens if I cancel?</h3>
            <p class="text-gray-600">Your subscription will remain active until the end of your current billing period. After that, you'll be downgraded to the Free plan.</p>
          </div>

          <div class="bg-white rounded-lg p-6 shadow-sm">
            <h3 class="font-semibold text-gray-900 mb-2">Is my payment information secure?</h3>
            <p class="text-gray-600">Absolutely. We use Stripe to process payments. Your card details are never stored on our servers.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
