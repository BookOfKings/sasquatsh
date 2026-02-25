<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const userMenuOpen = ref(false)

const showNavigation = computed(() => {
  return !['login', 'signup'].includes(route.name as string)
})

function goHome() {
  router.push('/')
}

function goToGames() {
  router.push('/games')
}

function goToGroups() {
  router.push('/groups')
}

function goToLFP() {
  router.push('/looking-for-players')
}

function goToDashboard() {
  router.push('/dashboard')
  userMenuOpen.value = false
}

function goToProfile() {
  router.push('/profile')
  userMenuOpen.value = false
}

function goToLogin() {
  router.push('/login')
}

async function handleLogout() {
  userMenuOpen.value = false
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <!-- Navigation -->
    <header v-if="showNavigation" class="bg-primary-500 text-white shadow-sm">
      <nav class="container-wide py-3">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <div class="flex items-center gap-3">
            <button @click="goHome" class="p-2 hover:bg-primary-600 rounded-lg transition-colors">
              <img src="/logo-white.png" alt="Sasquatsh" class="w-8 h-8" />
            </button>
            <span class="font-semibold text-lg cursor-pointer" @click="goHome">Sasquatsh</span>
          </div>

          <!-- Nav Links -->
          <div class="flex items-center gap-2">
            <button @click="goToGames" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
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
              <button @click="goToDashboard" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
                </svg>
                Dashboard
              </button>

              <!-- User Menu -->
              <div class="relative ml-2">
                <button
                  @click="userMenuOpen = !userMenuOpen"
                  class="flex items-center p-1 hover:bg-primary-600 rounded-full transition-colors"
                >
                  <div class="w-8 h-8 rounded-full bg-secondary-500 flex items-center justify-center overflow-hidden">
                    <img
                      v-if="auth.user.value?.avatarUrl"
                      :src="auth.user.value.avatarUrl"
                      class="w-full h-full object-cover"
                    />
                    <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                    </svg>
                  </div>
                </button>

                <!-- Dropdown -->
                <div
                  v-if="userMenuOpen"
                  class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-50"
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
                    @click="goToDashboard"
                    class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
                    </svg>
                    Dashboard
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
              </div>
            </template>

            <template v-else>
              <button @click="goToLogin" class="btn-outline border-white text-white hover:bg-white hover:text-primary-500 ml-2">
                Sign In
              </button>
            </template>
          </div>
        </div>
      </nav>
    </header>

    <!-- Click outside to close menu -->
    <div v-if="userMenuOpen" class="fixed inset-0 z-40" @click="userMenuOpen = false"></div>

    <!-- Main Content -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Footer -->
    <footer v-if="showNavigation" class="bg-gray-100 border-t border-gray-200 py-6">
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
          </nav>
        </div>
      </div>
    </footer>
  </div>
</template>
