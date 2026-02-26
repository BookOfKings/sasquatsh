<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getPlayerRequests,
  getMyPlayerRequests,
  createPlayerRequest,
  fillPlayerRequest,
  cancelPlayerRequest,
  deletePlayerRequest,
} from '@/services/socialApi'
import { getMyEvents } from '@/services/eventsApi'
import type { PlayerRequest, CreatePlayerRequestInput } from '@/types/social'
import type { EventSummary } from '@/types/events'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const requests = ref<PlayerRequest[]>([])
const myRequests = ref<PlayerRequest[]>([])
const myEvents = ref<EventSummary[]>([])
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref<'browse' | 'mine'>('browse')
const showCreateDialog = ref(false)
const creating = ref(false)
const actionInProgress = ref<string | null>(null)

// Auto-refresh interval
let refreshInterval: ReturnType<typeof setInterval> | null = null

const form = ref<CreatePlayerRequestInput>({
  eventId: '',
  description: '',
  playerCountNeeded: 1,
})

// Filter to only show events that are today or in the future
const upcomingEvents = computed(() => {
  const today = new Date().toISOString().split('T')[0] ?? ''
  return myEvents.value.filter(e => e.eventDate >= today && e.status !== 'cancelled')
})

onMounted(async () => {
  await loadRequests()

  if (auth.isAuthenticated.value) {
    await Promise.all([loadMyRequests(), loadMyEvents()])
  }

  // Auto-refresh every 30 seconds to keep countdown accurate
  refreshInterval = setInterval(() => {
    loadRequests()
  }, 30000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})

async function loadRequests() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = auth.isAuthenticated.value ? await auth.getIdToken() : undefined
    requests.value = await getPlayerRequests(undefined, token ?? undefined)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load requests'
  } finally {
    loading.value = false
  }
}

async function loadMyRequests() {
  try {
    const token = await auth.getIdToken()
    if (token) {
      myRequests.value = await getMyPlayerRequests(token)
    }
  } catch (err) {
    console.error('Failed to load my requests:', err)
  }
}

async function loadMyEvents() {
  try {
    const token = await auth.getIdToken()
    if (token) {
      myEvents.value = await getMyEvents(token)
    }
  } catch (err) {
    console.error('Failed to load my events:', err)
  }
}

function openCreateDialog() {
  if (!auth.isAuthenticated.value) {
    router.push({ name: 'login', query: { redirect: '/looking-for-players' } })
    return
  }

  if (upcomingEvents.value.length === 0) {
    errorMessage.value = 'You need to create an event first before requesting players'
    return
  }

  form.value = {
    eventId: upcomingEvents.value[0]?.id || '',
    description: '',
    playerCountNeeded: 1,
  }
  showCreateDialog.value = true
}

async function handleCreate() {
  if (!form.value.eventId) {
    errorMessage.value = 'Please select an event'
    return
  }

  creating.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    const newRequest = await createPlayerRequest(token, form.value)
    myRequests.value.unshift(newRequest)
    requests.value.unshift(newRequest)
    showCreateDialog.value = false
    successMessage.value = 'Request posted! It will expire in 15 minutes.'
    setTimeout(() => {
      successMessage.value = ''
    }, 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create request'
  } finally {
    creating.value = false
  }
}

async function handleFill(request: PlayerRequest) {
  actionInProgress.value = request.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const updated = await fillPlayerRequest(token, request.id)

    // Update in both lists
    const myIdx = myRequests.value.findIndex(r => r.id === request.id)
    if (myIdx !== -1) myRequests.value[myIdx] = updated

    requests.value = requests.value.filter(r => r.id !== request.id)

    successMessage.value = 'Great! Marked as filled.'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to mark as filled'
  } finally {
    actionInProgress.value = null
  }
}

async function handleCancel(request: PlayerRequest) {
  actionInProgress.value = request.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const updated = await cancelPlayerRequest(token, request.id)

    // Update in both lists
    const myIdx = myRequests.value.findIndex(r => r.id === request.id)
    if (myIdx !== -1) myRequests.value[myIdx] = updated

    requests.value = requests.value.filter(r => r.id !== request.id)

    successMessage.value = 'Request cancelled'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to cancel'
  } finally {
    actionInProgress.value = null
  }
}

async function handleDelete(request: PlayerRequest) {
  if (!confirm('Are you sure you want to delete this request?')) return

  actionInProgress.value = request.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deletePlayerRequest(token, request.id)
    myRequests.value = myRequests.value.filter(r => r.id !== request.id)
    requests.value = requests.value.filter(r => r.id !== request.id)
    successMessage.value = 'Request deleted'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete'
  } finally {
    actionInProgress.value = null
  }
}

function formatTime(timeStr: string): string {
  const parts = timeStr.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const period = hours >= 12 ? 'PM' : 'AM'
  const hour12 = hours % 12 || 12
  return `${hour12}:${minutes.toString().padStart(2, '0')} ${period}`
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

function getTimeRemaining(expiresAt: string): string {
  const expires = new Date(expiresAt)
  const now = new Date()
  const diffMs = expires.getTime() - now.getTime()

  if (diffMs <= 0) return 'Expired'

  const diffMinutes = Math.floor(diffMs / 60000)
  const diffSeconds = Math.floor((diffMs % 60000) / 1000)

  if (diffMinutes > 0) {
    return `${diffMinutes}m ${diffSeconds}s`
  }
  return `${diffSeconds}s`
}

function getStatusBadge(status: string) {
  switch (status) {
    case 'open': return { class: 'bg-green-100 text-green-700', text: 'Active' }
    case 'filled': return { class: 'bg-blue-100 text-blue-700', text: 'Filled' }
    case 'cancelled': return { class: 'bg-gray-100 text-gray-600', text: 'Cancelled' }
    default: return { class: 'bg-gray-100 text-gray-600', text: status }
  }
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">Need Players?</h1>
        <p class="text-gray-500">Urgent requests from hosts who need fill-in players</p>
      </div>
      <button class="btn-primary" @click="openCreateDialog">
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Need Players
      </button>
    </div>

    <!-- Info Banner -->
    <div class="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-6">
      <div class="flex gap-3">
        <svg class="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
        </svg>
        <div class="text-sm text-amber-800">
          <strong>How it works:</strong> If someone bails on your game at the last minute, post a request here.
          Requests are visible for <strong>15 minutes</strong> to help you find fill-in players quickly.
        </div>
      </div>
    </div>

    <!-- Success/Error Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- Tabs (if authenticated) -->
    <div v-if="auth.isAuthenticated.value" class="flex gap-2 mb-6">
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'browse' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'browse'"
      >
        Active Requests
      </button>
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'mine' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'mine'"
      >
        My Requests ({{ myRequests.length }})
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Browse Tab -->
    <template v-else-if="activeTab === 'browse'">
      <div v-if="requests.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <p class="text-lg font-medium">No active requests right now</p>
        <p>Check back later or post your own request if you need players!</p>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="request in requests"
          :key="request.id"
          class="card p-4 border-l-4 border-l-amber-500"
        >
          <div class="flex items-start gap-4">
            <!-- Host Avatar -->
            <div class="w-12 h-12 rounded-full bg-secondary-500 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="request.host?.avatarUrl"
                :src="request.host.avatarUrl"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-2">
                <div>
                  <h3 class="font-semibold text-gray-900">
                    {{ request.event?.title || 'Game Night' }}
                  </h3>
                  <p class="text-sm text-gray-500">
                    Hosted by {{ request.host?.displayName || request.host?.username || 'Unknown' }}
                  </p>
                </div>
                <div class="text-right flex-shrink-0">
                  <div class="text-sm font-medium text-amber-600">
                    {{ getTimeRemaining(request.expiresAt) }} left
                  </div>
                  <div class="chip bg-amber-100 text-amber-700 text-xs mt-1">
                    Needs {{ request.playerCountNeeded }} player{{ request.playerCountNeeded > 1 ? 's' : '' }}
                  </div>
                </div>
              </div>

              <!-- Event Details -->
              <div class="mt-3 p-3 bg-gray-50 rounded-lg text-sm">
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-gray-600">
                  <span v-if="request.event?.gameTitle" class="font-medium text-primary-700">
                    {{ request.event.gameTitle }}
                  </span>
                  <span v-if="request.event?.eventDate">
                    {{ formatDate(request.event.eventDate) }}
                    <span v-if="request.event?.startTime"> at {{ formatTime(request.event.startTime) }}</span>
                  </span>
                  <span v-if="request.event?.location">
                    {{ request.event.location }}
                  </span>
                </div>
              </div>

              <p v-if="request.description" class="mt-3 text-gray-600 italic">
                "{{ request.description }}"
              </p>

              <!-- Action to join -->
              <div class="mt-3">
                <button
                  v-if="request.event?.id"
                  class="btn-primary text-sm"
                  @click="router.push(`/games/${request.event.id}`)"
                >
                  View Event & Join
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- My Requests Tab -->
    <template v-else-if="activeTab === 'mine'">
      <div v-if="myRequests.length === 0" class="text-center py-12 text-gray-500">
        <p class="text-lg font-medium">You haven't posted any requests</p>
        <p class="mb-4">Need players for your game? Post a request!</p>
        <button class="btn-primary" @click="openCreateDialog">
          Need Players
        </button>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="request in myRequests"
          :key="request.id"
          class="card p-4"
          :class="{ 'opacity-60': request.status !== 'open' }"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="font-semibold text-gray-900">
                  {{ request.event?.title || 'Game Night' }}
                </h3>
                <span
                  class="chip text-xs"
                  :class="getStatusBadge(request.status).class"
                >
                  {{ getStatusBadge(request.status).text }}
                </span>
                <span v-if="request.status === 'open'" class="text-sm text-amber-600">
                  {{ getTimeRemaining(request.expiresAt) }} left
                </span>
              </div>
              <p class="text-sm text-gray-500 mt-1">
                {{ request.event?.gameTitle || 'No game specified' }}
                &bull; Needs {{ request.playerCountNeeded }} player{{ request.playerCountNeeded > 1 ? 's' : '' }}
              </p>
              <p v-if="request.description" class="mt-2 text-gray-600 text-sm">
                {{ request.description }}
              </p>
            </div>

            <div v-if="request.status === 'open'" class="flex gap-2 flex-shrink-0">
              <button
                class="btn-primary text-sm"
                :disabled="actionInProgress === request.id"
                @click="handleFill(request)"
              >
                <svg v-if="actionInProgress === request.id" class="animate-spin -ml-1 mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Found Players
              </button>
              <button
                class="btn-ghost text-gray-500 text-sm"
                :disabled="actionInProgress === request.id"
                @click="handleCancel(request)"
              >
                Cancel
              </button>
            </div>
            <div v-else class="flex-shrink-0">
              <button
                class="btn-ghost text-red-500 text-sm"
                :disabled="actionInProgress === request.id"
                @click="handleDelete(request)"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showCreateDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-2">Need Players?</h3>
        <p class="text-sm text-gray-500 mb-4">
          Post an urgent request for fill-in players. Your request will be visible for 15 minutes.
        </p>

        <div class="space-y-4">
          <div>
            <label class="label">Select Event *</label>
            <select v-model="form.eventId" class="input">
              <option value="">Choose an event...</option>
              <option v-for="event in upcomingEvents" :key="event.id" :value="event.id">
                {{ event.title }} - {{ formatDate(event.eventDate) }}
                <template v-if="event.gameTitle"> ({{ event.gameTitle }})</template>
              </option>
            </select>
            <p v-if="upcomingEvents.length === 0" class="text-sm text-amber-600 mt-1">
              You need to create an event first.
              <router-link to="/games/new" class="underline">Create an event</router-link>
            </p>
          </div>

          <div>
            <label class="label">Players Needed</label>
            <input
              v-model.number="form.playerCountNeeded"
              type="number"
              class="input w-24"
              min="1"
              max="20"
            />
          </div>

          <div>
            <label class="label">Message (optional)</label>
            <textarea
              v-model="form.description"
              rows="2"
              class="input"
              placeholder="e.g., Someone just bailed, need 1 more for Catan!"
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showCreateDialog = false" :disabled="creating">
            Cancel
          </button>
          <button
            class="btn-primary"
            @click="handleCreate"
            :disabled="creating || !form.eventId"
          >
            <svg v-if="creating" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Post Request
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
