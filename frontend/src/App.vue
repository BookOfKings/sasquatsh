<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getEffectiveTier } from '@/types/user'
import { TIER_NAMES } from '@/config/subscriptionLimits'
import CookieConsent from '@/components/common/CookieConsent.vue'
import D6Spinner from '@/components/common/D6Spinner.vue'
import UpdateNotification from '@/components/common/UpdateNotification.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const userMenuOpen = ref(false)
const mobileMenuOpen = ref(false)
const userMenuButton = ref<HTMLButtonElement | null>(null)
const dropdownPosition = ref({ top: 0, right: 0 })

// User's subscription tier
const userTier = computed(() => {
  if (!auth.user.value) return 'free'
  return getEffectiveTier(auth.user.value)
})

const tierDisplayName = computed(() => TIER_NAMES[userTier.value] || 'Free')

const isFreeTier = computed(() => userTier.value === 'free')

// Admins have full access, so don't show upgrade prompts to them
const isAdmin = computed(() => auth.user.value?.isAdmin ?? false)
const showUpgradePrompt = computed(() => isFreeTier.value && !isAdmin.value)

async function toggleUserMenu() {
  if (!userMenuOpen.value && userMenuButton.value) {
    const rect = userMenuButton.value.getBoundingClientRect()
    dropdownPosition.value = {
      top: rect.bottom + 8,
      right: window.innerWidth - rect.right
    }
  }
  userMenuOpen.value = !userMenuOpen.value
}

const showNavigation = computed(() => {
  return !['login', 'signup'].includes(route.name as string)
})

function goHome() {
  router.push('/')
}

function goToGames() {
  router.push('/games')
  mobileMenuOpen.value = false
}

function goToGroups() {
  router.push('/groups')
  mobileMenuOpen.value = false
}

function goToLFP() {
  router.push('/looking-for-players')
  mobileMenuOpen.value = false
}

function goToDashboard() {
  router.push('/dashboard')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
}

function goToProfile() {
  router.push('/profile')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
}

function goToMyCollection() {
  router.push('/my-collection')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
}

function goToBadges() {
  router.push('/badges')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
}

function goToLogin() {
  router.push('/login')
}

function goToPricing() {
  router.push('/pricing')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
}

async function handleLogout() {
  userMenuOpen.value = false
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <!-- Update Notification Banner -->
    <UpdateNotification />

    <!-- Background D6 dice -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden z-0">
      <div class="absolute -top-10 -left-10">
        <D6Spinner :size="200" :opacity="0.08" :duration="25" />
      </div>
      <div class="absolute top-1/4 -right-20">
        <D6Spinner :size="150" :opacity="0.06" :duration="30" />
      </div>
      <div class="absolute bottom-20 left-1/4">
        <D6Spinner :size="120" :opacity="0.05" :duration="35" />
      </div>
      <div class="absolute -bottom-10 right-1/3">
        <D6Spinner :size="180" :opacity="0.07" :duration="22" />
      </div>
    </div>
    <!-- Navigation -->
    <header v-if="showNavigation" class="bg-primary-500 text-white shadow-sm safe-top relative z-50">
      <nav class="container-wide py-3">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <div class="flex items-center gap-3">
            <button @click="goHome" class="p-2 hover:bg-primary-600 rounded-lg transition-colors">
              <img src="/logo-white.png" alt="Sasquatsh" class="w-8 h-8" />
            </button>
            <span class="font-semibold text-lg cursor-pointer hidden sm:inline" @click="goHome">Sasquatsh</span>
          </div>

          <!-- Desktop Nav Links (hidden on mobile) -->
          <div class="hidden md:flex items-center gap-2">
            <template v-if="auth.isAuthenticated.value">
              <button @click="goToDashboard" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
                </svg>
                Dashboard
              </button>
            </template>

            <button @click="goToGames" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
              Games
            </button>

            <button @click="goToGroups" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
              </svg>
              Groups
            </button>

            <button @click="goToLFP" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15.5,12C18,12 20,14 20,16.5C20,17.38 19.75,18.21 19.31,18.9L22.39,22L21,23.39L17.88,20.32C17.19,20.75 16.37,21 15.5,21C13,21 11,19 11,16.5C11,14 13,12 15.5,12M15.5,14A2.5,2.5 0 0,0 13,16.5A2.5,2.5 0 0,0 15.5,19A2.5,2.5 0 0,0 18,16.5A2.5,2.5 0 0,0 15.5,14M10,4A4,4 0 0,1 14,8C14,8.91 13.69,9.75 13.18,10.43C12.32,10.75 11.55,11.26 10.91,11.9L10,12A4,4 0 0,1 6,8A4,4 0 0,1 10,4M2,18V16C2,13.88 5.31,12.14 9.5,12C9.18,12.78 9,13.62 9,14.5C9,16.21 9.68,17.77 10.79,18.93L10,19H2V18Z"/>
              </svg>
              LFP
            </button>

            <template v-if="auth.isAuthenticated.value">

              <!-- Plan Badge with Upgrade -->
              <div class="flex items-center gap-1 ml-2">
                <span
                  class="px-2 py-1 text-xs font-medium rounded-full"
                  :class="{
                    'bg-gray-200 text-gray-600': userTier === 'free',
                    'bg-blue-100 text-blue-700': userTier === 'basic',
                    'bg-purple-100 text-purple-700': userTier === 'pro',
                    'bg-yellow-100 text-yellow-700': userTier === 'premium',
                  }"
                >
                  {{ tierDisplayName }}
                </span>
                <button
                  v-if="showUpgradePrompt"
                  @click="goToPricing"
                  class="text-xs text-white/80 hover:text-white underline"
                >
                  Upgrade
                </button>
              </div>

              <!-- User Menu -->
              <div class="relative ml-2">
                <button
                  ref="userMenuButton"
                  @click="toggleUserMenu"
                  class="flex items-center p-1 hover:bg-primary-600 rounded-full transition-colors"
                >
                  <UserAvatar
                    :avatar-url="auth.user.value?.avatarUrl"
                    :display-name="auth.user.value?.displayName"
                    :is-founding-member="auth.user.value?.isFoundingMember"
                    :is-admin="auth.user.value?.isAdmin"
                    size="sm"
                  />
                </button>

                <!-- Dropdown (teleported to body to escape stacking contexts) -->
                <Teleport to="body">
                  <div
                    v-if="userMenuOpen"
                    class="fixed w-48 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-[9999]"
                    :style="{ top: dropdownPosition.top + 'px', right: dropdownPosition.right + 'px' }"
                  >
                    <button
                      @click="goToProfile"
                      class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                      </svg>
                      My Profile
                    </button>
                    <button
                      @click="goToMyCollection"
                      class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                      </svg>
                      My Collection
                    </button>
                    <button
                      @click="goToBadges"
                      class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                      </svg>
                      Achievements
                    </button>
                    <button
                      @click="goToDashboard"
                      class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
                      </svg>
                      Dashboard
                    </button>
                    <hr class="my-1 border-gray-100" />
                    <!-- Plan Info -->
                    <div class="px-4 py-2">
                      <div class="flex items-center justify-between">
                        <span class="text-xs text-gray-500">Plan:</span>
                        <span
                          class="text-xs font-medium px-2 py-0.5 rounded-full"
                          :class="{
                            'bg-gray-100 text-gray-600': userTier === 'free',
                            'bg-blue-100 text-blue-700': userTier === 'basic',
                            'bg-purple-100 text-purple-700': userTier === 'pro',
                            'bg-yellow-100 text-yellow-700': userTier === 'premium',
                          }"
                        >
                          {{ tierDisplayName }}
                        </span>
                      </div>
                    </div>
                    <button
                      v-if="showUpgradePrompt"
                      @click="goToPricing"
                      class="w-full flex items-center gap-3 px-4 py-2 text-primary-600 hover:bg-primary-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M16,6L18.29,8.29L13.41,13.17L9.41,9.17L2,16.59L3.41,18L9.41,12L13.41,16L19.71,9.71L22,12V6H16Z"/>
                      </svg>
                      Upgrade Plan
                    </button>
                    <hr class="my-1 border-gray-100" />
                    <button
                      @click="handleLogout"
                      class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M16,17V14H9V10H16V7L21,12L16,17M14,2A2,2 0 0,1 16,4V6H14V4H5V20H14V18H16V20A2,2 0 0,1 14,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2H14Z"/>
                      </svg>
                      Sign Out
                    </button>
                  </div>
                </Teleport>
              </div>
            </template>

            <template v-else>
              <button @click="goToLogin" class="btn-outline border-white text-white hover:bg-white hover:text-primary-500 ml-2">
                Sign In
              </button>
            </template>
          </div>

          <!-- Mobile Menu Button & User Avatar -->
          <div class="flex md:hidden items-center gap-2">
            <template v-if="auth.isAuthenticated.value">
              <button
                @click="goToProfile"
                class="p-1 hover:bg-primary-600 rounded-full transition-colors"
              >
                <UserAvatar
                  :avatar-url="auth.user.value?.avatarUrl"
                  :display-name="auth.user.value?.displayName"
                  :is-founding-member="auth.user.value?.isFoundingMember"
                  :is-admin="auth.user.value?.isAdmin"
                  size="sm"
                />
              </button>
            </template>

            <button
              @click="mobileMenuOpen = !mobileMenuOpen"
              class="p-2 hover:bg-primary-600 rounded-lg transition-colors"
            >
              <svg v-if="!mobileMenuOpen" class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3,6H21V8H3V6M3,11H21V13H3V11M3,16H21V18H3V16Z"/>
              </svg>
              <svg v-else class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Mobile Menu -->
        <div v-if="mobileMenuOpen" class="md:hidden mt-4 pb-2 border-t border-primary-400 pt-4">
          <div class="flex flex-col gap-1">
            <template v-if="auth.isAuthenticated.value">
              <button @click="goToDashboard" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
                </svg>
                Dashboard
              </button>
            </template>

            <button @click="goToGames" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
              Games
            </button>

            <button @click="goToGroups" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
              </svg>
              Groups
            </button>

            <button @click="goToLFP" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15.5,12C18,12 20,14 20,16.5C20,17.38 19.75,18.21 19.31,18.9L22.39,22L21,23.39L17.88,20.32C17.19,20.75 16.37,21 15.5,21C13,21 11,19 11,16.5C11,14 13,12 15.5,12M15.5,14A2.5,2.5 0 0,0 13,16.5A2.5,2.5 0 0,0 15.5,19A2.5,2.5 0 0,0 18,16.5A2.5,2.5 0 0,0 15.5,14M10,4A4,4 0 0,1 14,8C14,8.91 13.69,9.75 13.18,10.43C12.32,10.75 11.55,11.26 10.91,11.9L10,12A4,4 0 0,1 6,8A4,4 0 0,1 10,4M2,18V16C2,13.88 5.31,12.14 9.5,12C9.18,12.78 9,13.62 9,14.5C9,16.21 9.68,17.77 10.79,18.93L10,19H2V18Z"/>
              </svg>
              Looking for Players
            </button>

            <template v-if="auth.isAuthenticated.value">

              <button @click="goToProfile" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                </svg>
                My Profile
              </button>

              <button @click="goToMyCollection" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                </svg>
                My Collection
              </button>

              <button @click="goToBadges" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                </svg>
                Achievements
              </button>

              <!-- Plan info in mobile menu -->
              <div class="flex items-center justify-between px-4 py-2 text-white/80">
                <span class="text-sm">Your plan:</span>
                <span
                  class="text-xs font-medium px-2 py-0.5 rounded-full"
                  :class="{
                    'bg-white/20': userTier === 'free',
                    'bg-blue-400/30': userTier === 'basic',
                    'bg-purple-400/30': userTier === 'pro',
                    'bg-yellow-400/30': userTier === 'premium',
                  }"
                >
                  {{ tierDisplayName }}
                </span>
              </div>

              <button
                v-if="showUpgradePrompt"
                @click="goToPricing"
                class="flex items-center gap-3 px-4 py-3 bg-secondary-500 hover:bg-secondary-600 rounded-lg transition-colors text-left font-medium"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M16,6L18.29,8.29L13.41,13.17L9.41,9.17L2,16.59L3.41,18L9.41,12L13.41,16L19.71,9.71L22,12V6H16Z"/>
                </svg>
                Upgrade Plan
              </button>

              <hr class="border-primary-400 my-2" />

              <button @click="handleLogout" class="flex items-center gap-3 px-4 py-3 hover:bg-primary-600 rounded-lg transition-colors text-left text-red-200">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M16,17V14H9V10H16V7L21,12L16,17M14,2A2,2 0 0,1 16,4V6H14V4H5V20H14V18H16V20A2,2 0 0,1 14,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2H14Z"/>
                </svg>
                Sign Out
              </button>
            </template>

            <template v-else>
              <button @click="goToLogin" class="flex items-center gap-3 px-4 py-3 bg-white text-primary-500 rounded-lg transition-colors text-left font-medium">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10,17V14H3V10H10V7L15,12L10,17M10,2H19A2,2 0 0,1 21,4V20A2,2 0 0,1 19,22H10A2,2 0 0,1 8,20V18H10V20H19V4H10V6H8V4A2,2 0 0,1 10,2Z"/>
                </svg>
                Sign In
              </button>
            </template>
          </div>
        </div>
      </nav>
    </header>

    <!-- Click outside to close user dropdown (teleported to body for proper stacking) -->
    <Teleport to="body">
      <div v-if="userMenuOpen" class="fixed inset-0 z-[9000]" @click="userMenuOpen = false"></div>
    </Teleport>

    <!-- Mobile menu backdrop - closes menu when clicking outside -->
    <div
      v-if="mobileMenuOpen"
      class="fixed inset-0 z-40 md:hidden"
      @click="mobileMenuOpen = false"
    ></div>

    <!-- Main Content -->
    <main class="flex-1 relative z-0">
      <router-view />
    </main>

    <!-- Footer -->
    <footer v-if="showNavigation" class="bg-gray-100 border-t border-gray-200 py-6 relative z-10">
      <div class="container-wide">
        <div class="flex flex-col md:flex-row items-center justify-between gap-4">
          <div class="flex items-center gap-2 text-gray-600">
            <img src="/logo.png" alt="Sasquatsh" class="w-6 h-6" />
            <span class="text-sm">&copy; {{ new Date().getFullYear() }} Sasquatsh. All rights reserved.</span>
          </div>
          <nav class="flex items-center gap-6 text-sm">
            <router-link to="/terms" class="text-gray-600 hover:text-primary-500 transition-colors">
              Terms of Service
            </router-link>
            <router-link to="/privacy" class="text-gray-600 hover:text-primary-500 transition-colors">
              Privacy Policy
            </router-link>
            <router-link to="/cookies" class="text-gray-600 hover:text-primary-500 transition-colors">
              Cookie Policy
            </router-link>
            <router-link to="/contact" class="text-gray-600 hover:text-primary-500 transition-colors">
              Contact Us
            </router-link>
            <router-link to="/support" class="text-gray-400 hover:text-gray-600 transition-colors">
              Support
            </router-link>
          </nav>
        </div>
      </div>
    </footer>

    <!-- Cookie Consent Banner -->
    <CookieConsent />
  </div>
</template>
