<script setup lang="ts">
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push('/')
}

function goToEvents() {
  router.push('/events')
}
</script>

<template>
  <div class="container-wide py-8">
    <div class="card p-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden">
            <img
              v-if="auth.user.value?.avatarUrl"
              :src="auth.user.value.avatarUrl"
              class="w-full h-full object-cover"
            />
            <svg v-else class="w-8 h-8 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
            </svg>
          </div>
          <div>
            <h1 class="text-xl font-bold text-gray-900">
              {{ auth.user.value?.displayName || 'Welcome!' }}
            </h1>
            <p class="text-sm text-gray-500">
              {{ auth.user.value?.email }}
            </p>
          </div>
        </div>

        <span class="chip-primary self-start sm:self-auto">
          {{ auth.user.value?.subscriptionTier || 'Free' }} Plan
        </span>
      </div>

      <hr class="border-gray-200 my-6" />

      <!-- Feature Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <!-- My Events Card -->
        <div class="bg-primary-50 rounded-xl p-5">
          <div class="flex items-center gap-4 mb-4">
            <svg class="w-10 h-10 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M12,10A3,3 0 0,0 9,13A3,3 0 0,0 12,16A3,3 0 0,0 15,13A3,3 0 0,0 12,10Z"/>
            </svg>
            <div>
              <p class="font-semibold text-gray-900">My Events</p>
              <p class="text-sm text-gray-600">Events you're registered for</p>
            </div>
          </div>
          <button class="btn-primary w-full opacity-50 cursor-not-allowed" disabled>
            Coming Soon
          </button>
        </div>

        <!-- Host Events Card -->
        <div class="bg-secondary-50 rounded-xl p-5">
          <div class="flex items-center gap-4 mb-4">
            <svg class="w-10 h-10 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
            </svg>
            <div>
              <p class="font-semibold text-gray-900">Host Events</p>
              <p class="text-sm text-gray-600">Events you're hosting</p>
            </div>
          </div>
          <button class="btn-secondary w-full opacity-50 cursor-not-allowed" disabled>
            Coming Soon
          </button>
        </div>
      </div>

      <hr class="border-gray-200 my-6" />

      <!-- Actions -->
      <div class="flex flex-col sm:flex-row sm:justify-between gap-3">
        <button class="btn-outline" @click="goToEvents">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
          </svg>
          Browse Events
        </button>

        <button class="btn-ghost text-red-600 hover:bg-red-50" @click="handleLogout">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M16,17V14H9V10H16V7L21,12L16,17M14,2A2,2 0 0,1 16,4V6H14V4H5V20H14V18H16V20A2,2 0 0,1 14,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2H14Z"/>
          </svg>
          Sign Out
        </button>
      </div>
    </div>
  </div>
</template>
