<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import StateSelect from '@/components/common/StateSelect.vue'
import { getMyAds, type AdvertiserAd } from '@/services/advertiserApi'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

const step = ref<'tiers' | 'create' | 'success'>( route.query.checkout === 'success' ? 'success' : 'tiers')
const selectedTier = ref<string | null>(null)
const myAds = ref<AdvertiserAd[]>([])

onMounted(async () => {
  if (auth.isAuthenticated.value) {
    try {
      const token = await auth.getIdToken()
      if (token) {
        myAds.value = await getMyAds(token)
      }
    } catch { /* silent */ }
  }
})
const loading = ref(false)
const error = ref('')

const tiers = [
  { id: 'starter', name: 'Starter', price: '$9.99', period: '/mo', targeting: 'One city', description: 'Perfect for local game stores and meetups', features: ['Targeted to one city', 'Banner ad placement', 'Impression & click tracking', 'Monthly analytics'] },
  { id: 'standard', name: 'Standard', price: '$24.99', period: '/mo', targeting: 'One state', description: 'Great for conventions and regional stores', features: ['Targeted to one state', 'Banner ad placement', 'Impression & click tracking', 'Monthly analytics', 'Priority over Starter'], highlighted: true },
  { id: 'premium', name: 'Premium', price: '$49.99', period: '/mo', targeting: 'Nationwide', description: 'Ideal for publishers and online retailers', features: ['Shown nationwide', 'Banner ad placement', 'Impression & click tracking', 'Monthly analytics', 'Priority placement'] },
  { id: 'featured', name: 'Featured', price: '$99.99', period: '/mo', targeting: 'Nationwide + Priority', description: 'Maximum visibility for major campaigns', features: ['Shown nationwide', 'Top priority placement', 'Dashboard + browse pages', 'Impression & click tracking', 'Monthly analytics', 'Dedicated support'] },
]

const adForm = ref({
  title: '',
  description: '',
  linkUrl: '',
  imageUrl: '',
  targetCity: '',
  targetState: '',
})

const needsCity = computed(() => selectedTier.value === 'starter')
const needsState = computed(() => selectedTier.value === 'standard' || selectedTier.value === 'starter')

function selectTier(tierId: string) {
  if (!auth.isAuthenticated.value) {
    router.push(`/login?redirect=/advertise`)
    return
  }
  selectedTier.value = tierId
  step.value = 'create'
}

async function handleSubmit() {
  if (!selectedTier.value) return
  if (!adForm.value.title.trim() || !adForm.value.description.trim() || !adForm.value.linkUrl.trim()) {
    error.value = 'Title, description, and link URL are required'
    return
  }
  if (needsCity.value && !adForm.value.targetCity.trim()) {
    error.value = 'Target city is required for the Starter plan'
    return
  }
  if (needsState.value && !adForm.value.targetState) {
    error.value = 'Target state is required'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const response = await fetch(`${FUNCTIONS_URL}/ad-checkout`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'X-Firebase-Token': token,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        tier: selectedTier.value,
        title: adForm.value.title.trim(),
        description: adForm.value.description.trim(),
        linkUrl: adForm.value.linkUrl.trim(),
        imageUrl: adForm.value.imageUrl.trim() || undefined,
        targetCity: adForm.value.targetCity.trim() || undefined,
        targetState: adForm.value.targetState || undefined,
        successUrl: `${window.location.origin}/advertise?checkout=success`,
        cancelUrl: `${window.location.origin}/advertise?checkout=cancelled`,
      }),
    })

    if (!response.ok) {
      const data = await response.json().catch(() => ({}))
      throw new Error(data.error || 'Failed to start checkout')
    }

    const result = await response.json()
    window.location.href = result.url
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Something went wrong'
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-12">
    <div class="max-w-5xl mx-auto px-4">

      <!-- Success State -->
      <div v-if="step === 'success'" class="max-w-lg mx-auto text-center py-12">
        <svg class="w-20 h-20 mx-auto text-green-500 mb-6" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <h1 class="text-3xl font-bold text-gray-900 mb-4">Payment Received!</h1>
        <p class="text-lg text-gray-600 mb-2">Your ad has been submitted for review.</p>
        <p class="text-gray-500 mb-8">We'll review your ad within 1-2 business days and notify you when it goes live.</p>
        <button @click="router.push('/dashboard')" class="btn-primary px-8 py-3">Go to Dashboard</button>
      </div>

      <!-- My Ads Dashboard -->
      <div v-if="myAds.length > 0 && step === 'tiers'" class="card p-6 mb-8">
        <h2 class="text-xl font-bold text-gray-900 mb-4">My Ads</h2>
        <div class="space-y-3">
          <div
            v-for="ad in myAds"
            :key="ad.id"
            class="flex items-start gap-4 p-4 bg-gray-50 rounded-lg"
          >
            <img v-if="ad.image_url" :src="ad.image_url" class="w-16 h-16 rounded object-cover flex-shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h3 class="font-semibold text-sm truncate">{{ ad.title }}</h3>
                <span
                  class="text-[10px] px-2 py-0.5 rounded-full font-medium"
                  :class="{
                    'bg-yellow-100 text-yellow-700': ad.status === 'pending_payment',
                    'bg-blue-100 text-blue-700': ad.status === 'pending_review',
                    'bg-green-100 text-green-700': ad.status === 'active',
                    'bg-gray-100 text-gray-600': ad.status === 'paused' || ad.status === 'expired',
                    'bg-red-100 text-red-700': ad.status === 'rejected',
                  }"
                >{{ ad.status.replace('_', ' ') }}</span>
                <span class="text-[10px] px-2 py-0.5 rounded-full bg-primary-50 text-primary-700">{{ ad.ad_tier }}</span>
              </div>
              <p class="text-xs text-gray-600 truncate">{{ ad.description }}</p>
              <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
                <span>{{ ad.impression_count }} impressions</span>
                <span>{{ ad.click_count }} clicks</span>
                <span v-if="ad.target_city || ad.target_state">
                  {{ [ad.target_city, ad.target_state].filter(Boolean).join(', ') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 1: Choose Tier -->
      <template v-if="step === 'tiers'">
        <div class="text-center mb-12">
          <h1 class="text-4xl font-bold text-gray-900 mb-4">Advertise on Sasquatsh</h1>
          <p class="text-xl text-gray-600 max-w-2xl mx-auto">
            Reach active tabletop gamers who host and attend game nights every week.
          </p>
        </div>

        <!-- Who Should Advertise -->
        <div class="card p-8 mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">Who Should Advertise?</h2>
          <div class="grid md:grid-cols-2 gap-6">
            <div class="flex items-start gap-3">
              <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
              </svg>
              <div>
                <h3 class="font-semibold text-gray-900">Game Stores</h3>
                <p class="text-sm text-gray-600">Promote your store to local players looking for game nights and supplies.</p>
              </div>
            </div>
            <div class="flex items-start gap-3">
              <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <div>
                <h3 class="font-semibold text-gray-900">Conventions & Expos</h3>
                <p class="text-sm text-gray-600">Get your gaming convention in front of players who actively attend events.</p>
              </div>
            </div>
            <div class="flex items-start gap-3">
              <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
              </svg>
              <div>
                <h3 class="font-semibold text-gray-900">Game Publishers</h3>
                <p class="text-sm text-gray-600">Promote new releases directly to people who buy and play board games.</p>
              </div>
            </div>
            <div class="flex items-start gap-3">
              <svg class="w-6 h-6 text-primary-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,3L1,9L12,15L21,10.09V17H23V9M5,13.18V17.18L12,21L19,17.18V13.18L12,17L5,13.18Z"/>
              </svg>
              <div>
                <h3 class="font-semibold text-gray-900">Gaming Accessories</h3>
                <p class="text-sm text-gray-600">Dice, sleeves, playmats, storage — reach buyers who game weekly.</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Pricing Tiers -->
        <h2 class="text-2xl font-bold text-gray-900 mb-6 text-center">Choose Your Plan</h2>
        <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          <div
            v-for="tier in tiers"
            :key="tier.id"
            class="card p-6 flex flex-col border-2 transition-all"
            :class="tier.highlighted ? 'border-primary-500 shadow-lg' : 'border-transparent'"
          >
            <div v-if="tier.highlighted" class="text-xs font-medium text-primary-600 mb-2">Most Popular</div>
            <h3 class="text-lg font-bold text-gray-900">{{ tier.name }}</h3>
            <div class="mt-2 mb-1">
              <span class="text-3xl font-bold text-gray-900">{{ tier.price }}</span>
              <span class="text-gray-500">{{ tier.period }}</span>
            </div>
            <p class="text-sm text-gray-500 mb-1">{{ tier.targeting }}</p>
            <p class="text-sm text-gray-600 mb-4">{{ tier.description }}</p>
            <ul class="space-y-2 mb-6 flex-1">
              <li v-for="feature in tier.features" :key="feature" class="flex items-start gap-2 text-sm">
                <svg class="w-4 h-4 text-green-500 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                {{ feature }}
              </li>
            </ul>
            <button
              @click="selectTier(tier.id)"
              class="w-full py-2.5 rounded-lg font-semibold transition-colors"
              :class="tier.highlighted ? 'bg-primary-500 text-white hover:bg-primary-600' : 'bg-gray-100 text-gray-900 hover:bg-gray-200'"
            >
              Get Started
            </button>
          </div>
        </div>
      </template>

      <!-- Step 2: Create Ad -->
      <template v-else-if="step === 'create'">
        <div class="max-w-2xl mx-auto">
          <button @click="step = 'tiers'; selectedTier = null" class="text-sm text-gray-500 hover:text-gray-700 mb-4 flex items-center gap-1">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
            </svg>
            Back to plans
          </button>

          <div class="card p-8">
            <h2 class="text-2xl font-bold text-gray-900 mb-2">Create Your Ad</h2>
            <p class="text-gray-600 mb-6">
              {{ tiers.find(t => t.id === selectedTier)?.name }} Plan — {{ tiers.find(t => t.id === selectedTier)?.price }}{{ tiers.find(t => t.id === selectedTier)?.period }}
            </p>

            <div v-if="error" class="alert-error mb-4">{{ error }}</div>

            <form @submit.prevent="handleSubmit" class="space-y-4">
              <div>
                <label class="label">Ad Title *</label>
                <input v-model="adForm.title" type="text" class="input" placeholder="e.g., Visit Dragon's Lair Games!" maxlength="100" required />
                <p class="text-xs text-gray-400 mt-1">{{ adForm.title.length }}/100</p>
              </div>

              <div>
                <label class="label">Description *</label>
                <textarea v-model="adForm.description" class="input" rows="3" placeholder="e.g., Your friendly local game store in Phoenix. Weekly game nights, tournaments, and 10% off your first purchase!" maxlength="500" required></textarea>
                <p class="text-xs text-gray-400 mt-1">{{ adForm.description.length }}/500</p>
              </div>

              <div>
                <label class="label">Link URL *</label>
                <input v-model="adForm.linkUrl" type="url" class="input" placeholder="https://your-website.com" required />
                <p class="text-xs text-gray-400 mt-1">Where users go when they click your ad</p>
              </div>

              <div>
                <label class="label">Image URL</label>
                <input v-model="adForm.imageUrl" type="url" class="input" placeholder="https://your-website.com/ad-image.jpg" />
                <p class="text-xs text-gray-400 mt-1">Optional banner image (recommended: 600x200px)</p>
              </div>

              <!-- Targeting -->
              <div v-if="needsState" class="grid grid-cols-2 gap-4">
                <div v-if="needsCity">
                  <label class="label">Target City *</label>
                  <input v-model="adForm.targetCity" type="text" class="input" placeholder="e.g., Phoenix" required />
                </div>
                <div>
                  <label class="label">Target State *</label>
                  <StateSelect v-model="adForm.targetState" required />
                </div>
              </div>

              <!-- Preview -->
              <div v-if="adForm.title" class="border border-gray-200 rounded-lg p-4 bg-gray-50">
                <p class="text-xs text-gray-400 mb-2">Preview</p>
                <div class="flex items-start gap-3">
                  <img v-if="adForm.imageUrl" :src="adForm.imageUrl" class="w-16 h-16 rounded object-cover" />
                  <div>
                    <div class="font-semibold text-sm">{{ adForm.title }}</div>
                    <div class="text-xs text-gray-600">{{ adForm.description }}</div>
                  </div>
                </div>
              </div>

              <button type="submit" class="btn-primary w-full py-3 text-lg" :disabled="loading">
                <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-5 w-5" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Continue to Payment — {{ tiers.find(t => t.id === selectedTier)?.price }}{{ tiers.find(t => t.id === selectedTier)?.period }}
              </button>
              <p class="text-xs text-gray-400 text-center">You'll be redirected to Stripe for secure payment. Your ad will be reviewed before going live.</p>
            </form>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
