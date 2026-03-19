<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { getActiveRaffle } from '@/services/raffleApi'
import type { RaffleWithDetails } from '@/types/raffle'
import { ENTRY_TYPE_LABELS } from '@/types/raffle'
import UserAvatar from '@/components/common/UserAvatar.vue'

const auth = useAuthStore()
const raffle = ref<RaffleWithDetails | null>(null)
const loading = ref(true)
const error = ref('')
const showTerms = ref(false)

// Check if user is a founding member (they don't participate in raffles)
const isFoundingMember = computed(() => auth.user.value?.isFoundingMember)

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

// Format prize value
const formattedPrizeValue = computed(() => {
  if (!raffle.value?.prizeValueCents) return null
  return `$${(raffle.value.prizeValueCents / 100).toFixed(2)}`
})

onMounted(async () => {
  try {
    const token = await auth.getIdToken()
    raffle.value = await getActiveRaffle(token)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load raffle'
    console.error('Failed to load raffle:', err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- Don't show for founding members -->
  <div v-if="!isFoundingMember && !loading && raffle" class="card overflow-hidden mb-6">
    <!-- Custom Banner Image (if set) -->
    <div v-if="raffle.bannerImageUrl" class="relative">
      <img :src="raffle.bannerImageUrl" :alt="raffle.title" class="w-full h-auto" />
      <!-- Entry count overlay -->
      <div v-if="raffle.userTotalEntries !== undefined" class="absolute top-3 right-3 bg-black/60 text-white rounded-lg px-3 py-2 text-center">
        <p class="text-2xl font-bold leading-none">{{ raffle.userTotalEntries }}</p>
        <p class="text-xs text-white/80">{{ raffle.userTotalEntries === 1 ? 'entry' : 'entries' }}</p>
      </div>
    </div>

    <!-- Default Header (no custom banner) -->
    <div v-else class="bg-gradient-to-r from-primary-500 to-primary-600 text-white p-5">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <svg class="w-10 h-10 text-yellow-300" viewBox="0 0 24 24" fill="currentColor">
            <path d="M9.06,1.93C7.17,1.92 5.33,3.74 6.17,6H3A2,2 0 0,0 1,8V10A1,1 0 0,0 2,11H11V8H13V11H22A1,1 0 0,0 23,10V8A2,2 0 0,0 21,6H17.83C18.67,3.74 16.83,1.92 14.94,1.93C13.5,1.93 12.71,2.71 12,3.5C11.29,2.71 10.5,1.93 9.06,1.93M9.06,3.93C9.57,3.93 10.13,4.26 10.74,4.87L12,6.13L13.26,4.87C13.87,4.26 14.43,3.93 14.94,3.93C15.46,3.93 16,4.5 16,5.19C16,5.9 15.5,6.5 15,6.87L14.5,7.2L13.4,8H10.6L9.5,7.2L9,6.87C8.5,6.5 8,5.9 8,5.19C8,4.5 8.54,3.93 9.06,3.93M2,12V20A2,2 0 0,0 4,22H20A2,2 0 0,0 22,20V12H13V20H11V12H2Z"/>
          </svg>
          <div>
            <h3 class="font-bold text-xl">{{ raffle.title }}</h3>
            <p class="text-white/90">{{ timeRemaining }}</p>
          </div>
        </div>
        <div v-if="raffle.userTotalEntries !== undefined" class="text-right">
          <p class="text-4xl font-bold">{{ raffle.userTotalEntries }}</p>
          <p class="text-sm text-white/80">{{ raffle.userTotalEntries === 1 ? 'entry' : 'entries' }}</p>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="p-5">
      <!-- Prize Info -->
      <div class="flex items-start gap-4 mb-5">
        <div v-if="raffle.prizeImageUrl" class="w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100 shadow-sm">
          <img :src="raffle.prizeImageUrl" :alt="raffle.prizeName" class="w-full h-full object-cover" />
        </div>
        <div class="flex-1 min-w-0">
          <p class="font-semibold text-gray-900 text-lg">Prize: {{ raffle.prizeName }}</p>
          <p v-if="raffle.prizeDescription" class="text-sm text-gray-600 mt-1 line-clamp-2">{{ raffle.prizeDescription }}</p>
          <p v-if="formattedPrizeValue" class="text-sm text-primary-600 font-semibold mt-2">
            Value: {{ formattedPrizeValue }}
          </p>
        </div>
      </div>

      <!-- Stats -->
      <div v-if="raffle.stats" class="flex items-center gap-4 text-sm text-gray-500 mb-5 pb-5 border-b border-gray-100">
        <span>{{ raffle.stats.totalEntries }} total entries</span>
        <span>&bull;</span>
        <span>{{ raffle.stats.uniqueParticipants }} participants</span>
      </div>

      <!-- User's entries breakdown -->
      <div v-if="raffle.userEntries && raffle.userEntries.length > 0" class="mb-5">
        <p class="text-sm font-medium text-gray-700 mb-3">Your entries:</p>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="entry in raffle.userEntries"
            :key="entry.id"
            class="text-sm bg-primary-50 text-primary-700 px-3 py-1.5 rounded-full font-medium"
          >
            {{ ENTRY_TYPE_LABELS[entry.entryType] }} (+{{ entry.entryCount }})
          </span>
        </div>
      </div>

      <!-- How to earn more entries -->
      <div class="bg-gray-50 rounded-xl p-4 mb-5">
        <p class="text-sm font-semibold text-gray-700 mb-3">Earn more entries:</p>
        <ul class="text-sm text-gray-600 space-y-2">
          <li class="flex items-center gap-3">
            <svg class="w-5 h-5 text-primary-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Host a game night (1-2 entries)
          </li>
          <li class="flex items-center gap-3">
            <svg class="w-5 h-5 text-primary-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
            </svg>
            Plan a group session (1-2 entries)
          </li>
          <li class="flex items-center gap-3">
            <svg class="w-5 h-5 text-primary-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
            </svg>
            Attend a game (1 entry)
          </li>
        </ul>
        <p class="text-xs text-gray-400 mt-3">Paid subscribers earn 2x entries for hosting and planning!</p>
      </div>

      <!-- Terms & Conditions Accordion -->
      <div v-if="raffle.termsConditions || raffle.mailInInstructions" class="border border-gray-200 rounded-xl overflow-hidden">
        <button
          @click="showTerms = !showTerms"
          class="w-full flex items-center justify-between p-4 text-left hover:bg-gray-50 transition-colors"
        >
          <span class="text-sm font-medium text-gray-700">Terms & Conditions</span>
          <svg
            class="w-5 h-5 text-gray-400 transition-transform duration-200"
            :class="{ 'rotate-180': showTerms }"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z"/>
          </svg>
        </button>
        <div
          v-show="showTerms"
          class="border-t border-gray-200 p-4 bg-gray-50 text-sm text-gray-600"
        >
          <div v-if="raffle.termsConditions" class="whitespace-pre-wrap">{{ raffle.termsConditions }}</div>
          <div v-if="raffle.mailInInstructions" class="mt-4 pt-4 border-t border-gray-200">
            <p class="font-semibold text-gray-700 mb-2">No Purchase Necessary - Mail-in Entry:</p>
            <p class="whitespace-pre-wrap">{{ raffle.mailInInstructions }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Winner announcement (if raffle ended with winner) -->
    <div v-if="raffle.status === 'ended' && raffle.winner" class="bg-green-50 border-t border-green-200 p-5">
      <div class="flex items-center gap-4">
        <svg class="w-8 h-8 text-green-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
        </svg>
        <div class="flex-1">
          <p class="font-semibold text-green-800 text-lg">Winner Selected!</p>
          <div class="flex items-center gap-2 mt-2">
            <UserAvatar
              :user="{ avatarUrl: raffle.winner.avatarUrl, displayName: raffle.winner.displayName }"
              size="sm"
              :show-badge="false"
            />
            <span class="text-green-700 font-medium">{{ raffle.winner.displayName || 'Unknown' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
