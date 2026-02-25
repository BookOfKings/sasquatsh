<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getInvitationByCode, acceptInvitation } from '@/services/socialApi'
import type { GameInvitation } from '@/types/social'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const accepting = ref(false)
const invitation = ref<GameInvitation | null>(null)
const errorMessage = ref('')
const successMessage = ref('')

const inviteCode = computed(() => route.params.code as string)

onMounted(async () => {
  await loadInvitation()
})

async function loadInvitation() {
  loading.value = true
  errorMessage.value = ''

  try {
    invitation.value = await getInvitationByCode(inviteCode.value)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Invitation not found'
  } finally {
    loading.value = false
  }
}

async function handleAccept() {
  if (!auth.isAuthenticated.value) {
    // Redirect to login with return URL
    router.push({
      name: 'login',
      query: { redirect: route.fullPath },
    })
    return
  }

  accepting.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    const result = await acceptInvitation(token, inviteCode.value)
    successMessage.value = result.message

    // Redirect to the event after a short delay
    setTimeout(() => {
      router.push(`/games/${result.eventId}`)
    }, 1500)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to accept invitation'
  } finally {
    accepting.value = false
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

function formatTime(timeStr: string): string {
  const parts = timeStr.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const date = new Date()
  date.setHours(hours, minutes)
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  })
}

function goToGames() {
  router.push('/games')
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <!-- Loading -->
      <div v-if="loading" class="card p-8 text-center">
        <svg class="w-12 h-12 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <p class="mt-4 text-gray-500">Loading invitation...</p>
      </div>

      <!-- Error -->
      <div v-else-if="errorMessage && !invitation" class="card p-8 text-center">
        <svg class="w-16 h-16 mx-auto text-red-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
        </svg>
        <h2 class="text-xl font-semibold text-gray-900 mb-2">Invitation Not Found</h2>
        <p class="text-gray-500 mb-6">{{ errorMessage }}</p>
        <button class="btn-primary" @click="goToGames">
          Browse Games
        </button>
      </div>

      <!-- Success -->
      <div v-else-if="successMessage" class="card p-8 text-center">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <h2 class="text-xl font-semibold text-gray-900 mb-2">You're In!</h2>
        <p class="text-gray-500">{{ successMessage }}</p>
        <p class="text-sm text-gray-400 mt-2">Redirecting to the game...</p>
      </div>

      <!-- Invitation Card -->
      <div v-else-if="invitation" class="card overflow-hidden">
        <!-- Header -->
        <div class="bg-primary-500 text-white p-6 text-center">
          <svg class="w-12 h-12 mx-auto mb-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
          </svg>
          <h1 class="text-2xl font-bold">You're Invited!</h1>
          <p class="opacity-90 mt-1">{{ invitation.event?.host?.displayName }} invited you to join</p>
        </div>

        <!-- Event Details -->
        <div class="p-6">
          <h2 class="text-xl font-bold text-gray-900 mb-4">{{ invitation.event?.title }}</h2>

          <div class="space-y-3">
            <!-- Date & Time -->
            <div class="flex items-center gap-3 text-gray-600">
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <div>
                <div class="font-medium">{{ invitation.event?.eventDate ? formatDate(invitation.event.eventDate) : '' }}</div>
                <div class="text-sm text-gray-500">{{ invitation.event?.startTime ? formatTime(invitation.event.startTime) : '' }}</div>
              </div>
            </div>

            <!-- Location -->
            <div v-if="invitation.event?.city" class="flex items-center gap-3 text-gray-600">
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
              </svg>
              <span>{{ [invitation.event.city, invitation.event.state].filter(Boolean).join(', ') }}</span>
            </div>

            <!-- Players -->
            <div class="flex items-center gap-3 text-gray-600">
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
              </svg>
              <span>Max {{ invitation.event?.maxPlayers }} players</span>
            </div>
          </div>

          <!-- Error -->
          <div v-if="errorMessage" class="alert-error mt-4">
            {{ errorMessage }}
          </div>

          <!-- Accept Button -->
          <button
            class="btn-primary w-full mt-6"
            :disabled="accepting"
            @click="handleAccept"
          >
            <svg v-if="accepting" class="animate-spin -ml-1 mr-2 h-5 w-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            <template v-if="auth.isAuthenticated.value">
              Accept Invitation
            </template>
            <template v-else>
              Sign In to Accept
            </template>
          </button>

          <p class="text-center text-sm text-gray-500 mt-4">
            <template v-if="!auth.isAuthenticated.value">
              You'll need to sign in or create an account to join this game.
            </template>
            <template v-else>
              By accepting, you'll be registered for this game night.
            </template>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
