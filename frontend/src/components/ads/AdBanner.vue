<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { getEffectiveTier } from '@/types/user'

const props = defineProps<{
  placement?: string
}>()

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

const auth = useAuthStore()

interface Ad {
  id: string
  title: string
  description: string | null
  imageUrl: string | null
  linkUrl: string
  adType: string
}

const ad = ref<Ad | null>(null)
const loading = ref(true)
const tracked = ref(false)

// Only show ads to free tier users
const shouldShowAds = computed(() => {
  if (!auth.isAuthenticated.value) return true // Show to anonymous users
  if (!auth.user.value) return true
  const tier = getEffectiveTier(auth.user.value)
  return tier === 'free'
})

async function fetchAd() {
  if (!shouldShowAds.value) {
    loading.value = false
    return
  }

  try {
    const response = await fetch(
      `${FUNCTIONS_URL}/ads?placement=${props.placement || 'general'}`,
      {
        headers: {
          'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        },
      }
    )

    if (response.ok) {
      const data = await response.json()
      if (data) {
        ad.value = data
        // Track impression
        trackImpression()
      }
    }
  } catch (err) {
    console.error('Failed to fetch ad:', err)
  } finally {
    loading.value = false
  }
}

async function trackImpression() {
  if (!ad.value || tracked.value) return
  tracked.value = true

  try {
    await fetch(
      `${FUNCTIONS_URL}/ads?action=impression&id=${ad.value.id}&page=${encodeURIComponent(window.location.pathname)}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        },
      }
    )
  } catch (err) {
    // Silently fail - don't break the page for tracking
  }
}

async function handleClick() {
  if (!ad.value) return

  // Track click
  try {
    await fetch(
      `${FUNCTIONS_URL}/ads?action=click&id=${ad.value.id}&page=${encodeURIComponent(window.location.pathname)}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        },
      }
    )
  } catch (err) {
    // Silently fail
  }

  // Navigate to the ad link
  if (ad.value.linkUrl.startsWith('/')) {
    // Internal link
    window.location.href = ad.value.linkUrl
  } else {
    // External link - open in new tab
    window.open(ad.value.linkUrl, '_blank', 'noopener')
  }
}

onMounted(() => {
  fetchAd()
})
</script>

<template>
  <div v-if="shouldShowAds && ad" class="ad-banner">
    <button
      @click="handleClick"
      class="w-full text-left bg-gradient-to-r from-primary-50 to-secondary-50 border border-primary-200 rounded-lg p-4 hover:border-primary-400 hover:shadow-md transition-all cursor-pointer group"
    >
      <div class="flex items-center gap-4">
        <!-- Ad Image (if available) -->
        <div v-if="ad.imageUrl" class="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0 bg-white">
          <img :src="ad.imageUrl" :alt="ad.title" class="w-full h-full object-cover" />
        </div>

        <!-- Ad Icon (fallback) -->
        <div v-else class="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
          </svg>
        </div>

        <!-- Ad Content -->
        <div class="flex-1 min-w-0">
          <h4 class="font-semibold text-gray-900 group-hover:text-primary-600 transition-colors">
            {{ ad.title }}
          </h4>
          <p v-if="ad.description" class="text-sm text-gray-600 line-clamp-2">
            {{ ad.description }}
          </p>
        </div>

        <!-- Arrow -->
        <svg class="w-5 h-5 text-gray-400 group-hover:text-primary-500 transition-colors flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
        </svg>
      </div>

      <!-- Sponsored label -->
      <div class="mt-2 text-xs text-gray-400 text-right">
        Sponsored
      </div>
    </button>
  </div>
</template>

<style scoped>
.ad-banner {
  margin: 1rem 0;
}
</style>
