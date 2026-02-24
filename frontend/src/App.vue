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

function goToEvents() {
  router.push('/events')
}

function goToDashboard() {
  router.push('/dashboard')
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
            <button @click="goToEvents" class="flex items-center gap-2 px-4 py-2 hover:bg-primary-600 rounded-lg transition-colors">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              Events
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
                    @click="goToDashboard"
                    class="w-full flex items-center gap-3 px-4 py-2 text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                    </svg>
                    Profile
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
  </div>
</template>
