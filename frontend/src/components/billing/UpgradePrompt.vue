<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { TIER_NAMES, TIER_PRICES, type SubscriptionTier } from '@/config/subscriptionLimits'

const props = defineProps<{
  visible: boolean
  currentTier: SubscriptionTier
  limitType: 'games' | 'groups'
  currentCount: number
  limit: number
  blocking?: boolean // When true, user cannot dismiss - must upgrade or go back
}>()

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()

const title = computed(() => {
  if (props.limitType === 'games') {
    return 'Game Limit Reached'
  }
  return 'Group Limit Reached'
})

const message = computed(() => {
  if (props.limitType === 'games') {
    return `You've reached your limit of ${props.limit} active game${props.limit === 1 ? '' : 's'} on the ${TIER_NAMES[props.currentTier]} plan.`
  }
  return `You've reached your limit of ${props.limit} group${props.limit === 1 ? '' : 's'} on the ${TIER_NAMES[props.currentTier]} plan.`
})

const recommendedTier = computed((): SubscriptionTier => {
  if (props.currentTier === 'free') return 'basic'
  if (props.currentTier === 'basic') return 'pro'
  return 'pro'
})

const recommendedLimit = computed(() => {
  const limits: Record<SubscriptionTier, { games: number; groups: number }> = {
    free: { games: 1, groups: 1 },
    basic: { games: 5, groups: 5 },
    pro: { games: 10, groups: 10 },
    premium: { games: Infinity, groups: Infinity },
  }
  return limits[recommendedTier.value][props.limitType]
})

function goToPricing() {
  emit('close')
  router.push('/pricing')
}

function goBack() {
  router.back()
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
      @click.self="!blocking && emit('close')"
    >
      <div class="bg-white rounded-xl shadow-xl max-w-md w-full overflow-hidden">
        <!-- Header -->
        <div class="bg-gradient-to-r from-primary-500 to-secondary-500 p-6 text-white">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16,6L18.29,8.29L13.41,13.17L9.41,9.17L2,16.59L3.41,18L9.41,12L13.41,16L19.71,9.71L22,12V6H16Z"/>
              </svg>
            </div>
            <div>
              <h2 class="text-xl font-bold">{{ title }}</h2>
              <p class="text-white/80 text-sm">Upgrade to unlock more</p>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="p-6">
          <p class="text-gray-600 mb-6">{{ message }}</p>

          <!-- Upgrade Card -->
          <div class="bg-gray-50 rounded-lg p-4 mb-6">
            <div class="flex items-center justify-between mb-3">
              <span class="font-semibold text-gray-900">{{ TIER_NAMES[recommendedTier] }} Plan</span>
              <span class="text-lg font-bold text-primary-600">
                ${{ TIER_PRICES[recommendedTier] }}/mo
              </span>
            </div>
            <ul class="space-y-2 text-sm text-gray-600">
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                {{ recommendedLimit === Infinity ? 'Unlimited' : `Up to ${recommendedLimit}` }} active {{ limitType }}
              </li>
              <li v-if="recommendedTier === 'basic' || recommendedTier === 'pro'" class="flex items-center gap-2">
                <svg class="w-4 h-4 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                Game night planning features
              </li>
              <li v-if="recommendedTier === 'pro'" class="flex items-center gap-2">
                <svg class="w-4 h-4 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                Items to bring lists
              </li>
              <li class="flex items-center gap-2">
                <svg class="w-4 h-4 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
                No ads
              </li>
            </ul>
          </div>

          <!-- Actions -->
          <div class="flex gap-3">
            <button
              v-if="blocking"
              @click="goBack"
              class="flex-1 btn btn-secondary"
            >
              Go Back
            </button>
            <button
              v-else
              @click="emit('close')"
              class="flex-1 btn btn-secondary"
            >
              Maybe Later
            </button>
            <button
              @click="goToPricing"
              class="flex-1 btn btn-primary"
            >
              View Plans
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
