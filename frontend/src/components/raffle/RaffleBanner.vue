<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getActiveRaffle } from '@/services/raffleApi'
import type { RaffleWithDetails } from '@/types/raffle'

const router = useRouter()
const auth = useAuthStore()
const raffle = ref<RaffleWithDetails | null>(null)
const loading = ref(true)

// Check if custom banner is set
const hasCustomBanner = computed(() => !!raffle.value?.bannerImageUrl)

// Time remaining until raffle ends
const timeRemaining = computed(() => {
  if (!raffle.value) return null
  const end = new Date(raffle.value.endDate)
  const now = new Date()
  const diff = end.getTime() - now.getTime()

  if (diff <= 0) return 'Ended'

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))

  if (days > 0) return `${days}d ${hours}h left`
  if (hours > 0) return `${hours}h left`
  return 'Ending soon'
})

onMounted(async () => {
  try {
    // Fetch without token for public view
    raffle.value = await getActiveRaffle(null)
  } catch (err) {
    console.error('Failed to load raffle:', err)
  } finally {
    loading.value = false
  }
})

function handleClick() {
  if (auth.isAuthenticated.value) {
    router.push('/dashboard')
  } else {
    router.push('/signup')
  }
}
</script>

<template>
  <div v-if="!loading && raffle" class="w-full max-w-lg mx-auto mb-6">
    <!-- Custom Banner Image -->
    <button
      v-if="hasCustomBanner"
      @click="handleClick"
      class="w-full rounded-xl overflow-hidden shadow-lg hover:shadow-xl transition-shadow cursor-pointer"
    >
      <img
        :src="raffle.bannerImageUrl!"
        :alt="raffle.title"
        class="w-full h-auto"
      />
    </button>

    <!-- Dynamic Banner (fallback) -->
    <button
      v-else
      @click="handleClick"
      class="w-full bg-gradient-to-r from-yellow-400 via-orange-500 to-red-500 rounded-xl p-4 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer text-left"
    >
      <div class="flex items-center gap-4">
        <!-- Prize Image -->
        <div class="relative flex-shrink-0">
          <div v-if="raffle.prizeImageUrl" class="w-16 h-16 rounded-lg overflow-hidden bg-white/20 shadow-inner">
            <img :src="raffle.prizeImageUrl" :alt="raffle.prizeName" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-16 h-16 rounded-lg bg-white/20 flex items-center justify-center">
            <svg class="w-8 h-8 text-white/80" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9.06,1.93C7.17,1.92 5.33,3.74 6.17,6H3A2,2 0 0,0 1,8V10A1,1 0 0,0 2,11H11V8H13V11H22A1,1 0 0,0 23,10V8A2,2 0 0,0 21,6H17.83C18.67,3.74 16.83,1.92 14.94,1.93C13.5,1.93 12.71,2.71 12,3.5C11.29,2.71 10.5,1.93 9.06,1.93M9.06,3.93C9.57,3.93 10.13,4.26 10.74,4.87L12,6.13L13.26,4.87C13.87,4.26 14.43,3.93 14.94,3.93C15.46,3.93 16,4.5 16,5.19C16,5.9 15.5,6.5 15,6.87L14.5,7.2L13.4,8H10.6L9.5,7.2L9,6.87C8.5,6.5 8,5.9 8,5.19C8,4.5 8.54,3.93 9.06,3.93M2,12V20A2,2 0 0,0 4,22H20A2,2 0 0,0 22,20V12H13V20H11V12H2Z"/>
            </svg>
          </div>
          <!-- Sparkle effect -->
          <div class="absolute -top-1 -right-1 w-4 h-4 bg-yellow-300 rounded-full animate-pulse"></div>
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-xs font-bold uppercase tracking-wider text-yellow-200">Monthly Raffle</span>
            <span class="text-xs bg-white/20 px-2 py-0.5 rounded-full">{{ timeRemaining }}</span>
          </div>
          <p class="font-bold text-lg truncate">Win: {{ raffle.prizeName }}</p>
          <p class="text-sm text-white/80">
            {{ auth.isAuthenticated.value ? 'View your entries' : 'Sign up to enter' }}
            <svg class="w-4 h-4 inline ml-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4,11V13H16L10.5,18.5L11.92,19.92L19.84,12L11.92,4.08L10.5,5.5L16,11H4Z"/>
            </svg>
          </p>
        </div>

        <!-- Entry count for logged-in users -->
        <div v-if="auth.isAuthenticated.value && raffle.userTotalEntries !== undefined" class="text-right flex-shrink-0">
          <p class="text-2xl font-bold">{{ raffle.userTotalEntries }}</p>
          <p class="text-xs text-white/80">entries</p>
        </div>
      </div>
    </button>
  </div>
</template>
