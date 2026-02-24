<script setup lang="ts">
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function goToLogin() {
  router.push('/login')
}

function goToSignup() {
  router.push('/signup')
}

function goToDashboard() {
  router.push('/dashboard')
}

function goToEvents() {
  router.push('/events')
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
      <p class="text-lg text-gray-600 mb-6">Plan legendary game nights with your crew</p>

      <hr class="border-gray-200 mb-6" />

      <template v-if="auth.isAuthenticated.value">
        <p class="text-gray-700 mb-4">
          Welcome back, <strong>{{ auth.user.value?.displayName || auth.user.value?.email }}</strong>!
        </p>
        <button @click="goToDashboard" class="btn-primary w-full mb-3">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,3V9H21V3M13,21H21V11H13M3,21H11V15H3M3,13H11V3H3V13Z"/>
          </svg>
          Go to Dashboard
        </button>
        <button @click="goToEvents" class="btn-outline w-full">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
          </svg>
          Browse Events
        </button>
      </template>

      <template v-else>
        <p class="text-gray-600 mb-6">
          Create events, invite friends, and organize epic board game sessions.
        </p>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-6">
          <button @click="goToSignup" class="btn-primary">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Get Started
          </button>
          <button @click="goToLogin" class="btn-outline">
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M10,17V14H3V10H10V7L15,12L10,17M10,2H19A2,2 0 0,1 21,4V20A2,2 0 0,1 19,22H10A2,2 0 0,1 8,20V18H10V20H19V4H10V6H8V4A2,2 0 0,1 10,2Z"/>
            </svg>
            Sign In
          </button>
        </div>

        <hr class="border-gray-200 mb-4" />

        <button @click="goToEvents" class="btn-ghost text-primary-500">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4.5C7,4.5 2.73,7.61 1,12C2.73,16.39 7,19.5 12,19.5C17,19.5 21.27,16.39 23,12C21.27,7.61 17,4.5 12,4.5Z"/>
          </svg>
          Browse public events
        </button>
      </template>
    </div>
  </div>
</template>
