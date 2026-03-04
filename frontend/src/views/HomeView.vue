<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'
import { getStats, type Stats } from '@/services/statsApi'
import { getEffectiveTier } from '@/types/user'

const auth = useAuthStore()
const router = useRouter()

const stats = ref<Stats>({ gamesToday: 0, gamesEver: 0 })
const statsLoading = ref(true)

const isFreeTier = computed(() => {
  if (!auth.user.value) return true
  return getEffectiveTier(auth.user.value) === 'free'
})

onMounted(async () => {
  try {
    stats.value = await getStats()
  } finally {
    statsLoading.value = false
  }
})

function goToLogin() {
  router.push('/login')
}

function goToSignup() {
  router.push('/signup')
}

function goToDashboard() {
  router.push('/dashboard')
}

function goToGames() {
  router.push('/games')
}

function goToCreateGame() {
  router.push('/games/create')
}

function goToPricing() {
  router.push('/pricing')
}
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] flex items-center justify-center p-4">
    <div class="card p-8 text-center max-w-lg w-full">
      <!-- Logo -->
      <div class="mb-6">
        <img src="/logo.png" alt="Sasquatsh" class="w-36 h-36 mx-auto" />
      </div>

      <h1 class="text-3xl font-bold text-primary-500 mb-2">Sasquatsh</h1>
      <p class="text-lg text-gray-600 mb-4">Plan legendary games with your crew</p>

      <!-- Stats Cards -->
      <div class="grid grid-cols-2 gap-4 mb-6">
        <div class="bg-primary-50 rounded-xl p-4">
          <div class="flex items-center justify-center gap-2 mb-1">
            <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
            </svg>
            <span class="text-sm font-medium text-primary-700">Today</span>
          </div>
          <div v-if="statsLoading" class="h-8 bg-primary-100 rounded animate-pulse"></div>
          <div v-else class="text-2xl font-bold text-primary-600">
            {{ stats.gamesToday }} <span class="text-sm font-normal">game{{ stats.gamesToday !== 1 ? 's' : '' }}</span>
          </div>
        </div>
        <div class="bg-secondary-50 rounded-xl p-4">
          <div class="flex items-center justify-center gap-2 mb-1">
            <svg class="w-5 h-5 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
            </svg>
            <span class="text-sm font-medium text-secondary-700">All Time</span>
          </div>
          <div v-if="statsLoading" class="h-8 bg-secondary-100 rounded animate-pulse"></div>
          <div v-else class="text-2xl font-bold text-secondary-600">
            {{ stats.gamesEver }} <span class="text-sm font-normal">game{{ stats.gamesEver !== 1 ? 's' : '' }}</span>
          </div>
        </div>
      </div>

      <!-- Mission Statement -->
      <div class="bg-gradient-to-r from-primary-50 to-secondary-50 rounded-xl p-4 mb-6">
        <p class="text-gray-700 text-sm leading-relaxed">
          Our mission is to bring board game lovers together. Whether you're hosting a casual game night
          or looking for players nearby, Sasquatsh makes it easy to connect with fellow enthusiasts
          and share your passion for tabletop gaming.
        </p>
      </div>

      <hr class="border-gray-200 mb-6" />

      <template v-if="auth.isAuthenticated.value">
        <p class="text-gray-700 mb-4">
          Welcome back, <strong>{{ auth.user.value?.displayName || auth.user.value?.email }}</strong>!
        </p>
        <button @click="goToCreateGame" class="btn-primary w-full mb-3">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Host a Game
        </button>
        <div class="grid grid-cols-2 gap-3">
          <button @click="goToDashboard" class="btn-outline">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
            </svg>
            Dashboard
          </button>
          <button @click="goToGames" class="btn-outline">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
            </svg>
            Browse
          </button>
        </div>

        <!-- Upgrade CTA for free tier users -->
        <div v-if="isFreeTier" class="bg-gradient-to-r from-primary-50 to-purple-50 border border-primary-200 rounded-xl p-4 mt-4">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-900 text-sm">Unlock more features</p>
              <p class="text-xs text-gray-600">Host multiple games, create more groups, and access planning tools.</p>
            </div>
            <button @click="goToPricing" class="btn-primary text-sm whitespace-nowrap">
              Upgrade
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <p class="text-gray-600 mb-6">
          Create games, invite friends, and organize epic board game sessions.
        </p>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-6">
          <button @click="goToSignup" class="btn-primary">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Get Started Free
          </button>
          <button @click="goToLogin" class="btn-outline">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M10,17V14H3V10H10V7L15,12L10,17M10,2H19A2,2 0 0,1 21,4V20A2,2 0 0,1 19,22H10A2,2 0 0,1 8,20V18H10V20H19V4H10V6H8V4A2,2 0 0,1 10,2Z"/>
            </svg>
            Sign In
          </button>
        </div>

        <hr class="border-gray-200 mb-4" />

        <button @click="goToGames" class="btn-ghost text-primary-500">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4.5C7,4.5 2.73,7.61 1,12C2.73,16.39 7,19.5 12,19.5C17,19.5 21.27,16.39 23,12C21.27,7.61 17,4.5 12,4.5Z"/>
          </svg>
          Browse public games
        </button>
      </template>
    </div>

    <!-- Subscription Tiers Section (shown to non-authenticated users) -->
    <div v-if="!auth.isAuthenticated.value" class="card p-6 max-w-4xl w-full mt-6">
      <h2 class="text-xl font-bold text-center mb-2">Choose Your Plan</h2>
      <p class="text-gray-600 text-center mb-6">Start free and upgrade when you need more features</p>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Free Tier -->
        <div class="border border-gray-200 rounded-xl p-4 hover:border-primary-300 transition-colors">
          <div class="text-center mb-4">
            <h3 class="font-bold text-lg">Free</h3>
            <div class="text-3xl font-bold text-primary-500">$0</div>
            <div class="text-sm text-gray-500">forever</div>
          </div>
          <ul class="space-y-2 text-sm mb-4">
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Host 1 game per event
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Create 1 group
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Join unlimited events
            </li>
          </ul>
          <button @click="goToSignup" class="btn-outline w-full text-sm">
            Get Started
          </button>
        </div>

        <!-- Basic Tier -->
        <div class="border-2 border-primary-500 rounded-xl p-4 relative bg-primary-50/30">
          <div class="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary-500 text-white text-xs px-3 py-1 rounded-full">
            Popular
          </div>
          <div class="text-center mb-4">
            <h3 class="font-bold text-lg">Basic</h3>
            <div class="text-3xl font-bold text-primary-500">$7.99</div>
            <div class="text-sm text-gray-500">per month</div>
          </div>
          <ul class="space-y-2 text-sm mb-4">
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Up to 5 games per event
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Create up to 5 groups
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Game night planning
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              No ads
            </li>
          </ul>
          <button @click="goToSignup" class="btn-primary w-full text-sm">
            Start Free Trial
          </button>
        </div>

        <!-- Pro Tier -->
        <div class="border border-gray-200 rounded-xl p-4 hover:border-purple-300 transition-colors bg-gradient-to-b from-purple-50/50 to-transparent">
          <div class="text-center mb-4">
            <h3 class="font-bold text-lg text-purple-700">Pro</h3>
            <div class="text-3xl font-bold text-purple-600">$14.99</div>
            <div class="text-sm text-gray-500">per month</div>
          </div>
          <ul class="space-y-2 text-sm mb-4">
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Up to 10 games per event
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Create up to 10 groups
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Items to bring lists
            </li>
            <li class="flex items-center gap-2">
              <svg class="w-4 h-4 text-green-500 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              Priority support
            </li>
          </ul>
          <button @click="goToSignup" class="btn-outline w-full text-sm border-purple-300 text-purple-700 hover:bg-purple-50">
            Start Free Trial
          </button>
        </div>
      </div>

      <p class="text-center text-sm text-gray-500 mt-4">
        <button @click="goToPricing" class="text-primary-500 hover:underline">
          View full plan comparison
        </button>
      </p>
    </div>
  </div>
</template>
