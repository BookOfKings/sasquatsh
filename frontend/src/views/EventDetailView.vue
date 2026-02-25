<script setup lang="ts">
import { onMounted, computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import ShareModal from '@/components/common/ShareModal.vue'

const route = useRoute()
const router = useRouter()
const eventStore = useEventStore()
const auth = useAuthStore()

const toast = reactive({
  visible: false,
  message: '',
  type: 'success' as 'success' | 'error',
})

const addItemDialog = ref(false)
const newItemName = ref('')
const newItemCategory = ref('other')
const showShareModal = ref(false)

const eventId = computed(() => route.params.id as string)
const event = computed(() => eventStore.currentEvent.value)
const isHost = computed(() => event.value?.hostUserId === auth.user.value?.id)
const isRegistered = computed(() => {
  if (!auth.user.value || !event.value?.registrations) return false
  return event.value.registrations.some(
    (r) => r.userId === auth.user.value?.id && r.status === 'confirmed'
  )
})
const spotsLeft = computed(() => {
  if (!event.value) return 0
  return event.value.maxPlayers - event.value.confirmedCount
})

onMounted(() => {
  eventStore.loadEvent(eventId.value)
})

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

function getDifficultyClasses(level: string | null): string {
  switch (level) {
    case 'beginner': return 'chip-success'
    case 'intermediate': return 'chip-warning'
    case 'advanced': return 'chip-error'
    default: return 'chip bg-gray-100 text-gray-700'
  }
}

function showMessage(ok: boolean, message: string) {
  toast.message = message
  toast.type = ok ? 'success' : 'error'
  toast.visible = true
  setTimeout(() => {
    toast.visible = false
  }, 3500)
}

async function handleRegister() {
  const result = await eventStore.registerForEvent(eventId.value)
  showMessage(result.ok, result.message)
}

async function handleCancelRegistration() {
  const result = await eventStore.cancelRegistration(eventId.value)
  showMessage(result.ok, result.message)
}

async function handleAddItem() {
  if (!newItemName.value.trim()) return

  const result = await eventStore.addItem(eventId.value, {
    itemName: newItemName.value.trim(),
    itemCategory: newItemCategory.value,
  })
  showMessage(result.ok, result.message)

  if (result.ok) {
    addItemDialog.value = false
    newItemName.value = ''
    newItemCategory.value = 'other'
  }
}

async function handleClaimItem(itemId: string) {
  const result = await eventStore.claimItem(eventId.value, itemId)
  showMessage(result.ok, result.message)
}

async function handleUnclaimItem(itemId: string) {
  const result = await eventStore.unclaimItem(eventId.value, itemId)
  showMessage(result.ok, result.message)
}

function goToEdit() {
  router.push(`/games/${eventId.value}/edit`)
}

function goBack() {
  router.push('/games')
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Back Button -->
    <button class="btn-ghost mb-4" @click="goBack">
      <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
      </svg>
      Back to Games
    </button>

    <!-- Loading -->
    <div v-if="eventStore.loading.value" class="h-1 w-full bg-gray-200 rounded-full overflow-hidden mb-4">
      <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 60%"></div>
    </div>

    <template v-if="event">
      <!-- Main Event Card -->
      <div class="card p-6 mb-6">
        <!-- Header -->
        <div class="flex items-start gap-4 mb-4">
          <div class="w-14 h-14 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden flex-shrink-0">
            <img
              v-if="event.host?.avatarUrl"
              :src="event.host.avatarUrl"
              class="w-full h-full object-cover"
            />
            <svg v-else class="w-8 h-8 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <h1 class="text-2xl font-bold text-gray-900">{{ event.title }}</h1>
            <p class="text-gray-500">
              <span v-if="event.gameTitle">{{ event.gameTitle }} &bull; </span>
              Hosted by {{ event.host?.displayName || 'Unknown' }}
            </p>
          </div>
          <div class="flex gap-2">
            <button
              v-if="auth.isAuthenticated.value"
              class="btn-outline"
              @click="showShareModal = true"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M18,16.08C17.24,16.08 16.56,16.38 16.04,16.85L8.91,12.7C8.96,12.47 9,12.24 9,12C9,11.76 8.96,11.53 8.91,11.3L15.96,7.19C16.5,7.69 17.21,8 18,8A3,3 0 0,0 21,5A3,3 0 0,0 18,2A3,3 0 0,0 15,5C15,5.24 15.04,5.47 15.09,5.7L8.04,9.81C7.5,9.31 6.79,9 6,9A3,3 0 0,0 3,12A3,3 0 0,0 6,15C6.79,15 7.5,14.69 8.04,14.19L15.16,18.34C15.11,18.55 15.08,18.77 15.08,19C15.08,20.61 16.39,21.91 18,21.91C19.61,21.91 20.92,20.61 20.92,19A2.92,2.92 0 0,0 18,16.08Z"/>
              </svg>
              Share
            </button>
            <button
              v-if="isHost"
              class="btn-outline"
              @click="goToEdit"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
              </svg>
              Edit
            </button>
          </div>
        </div>

        <!-- Tags -->
        <div class="flex flex-wrap gap-2 mb-6">
          <span
            v-if="event.difficultyLevel"
            :class="getDifficultyClasses(event.difficultyLevel)"
          >
            {{ event.difficultyLevel }}
          </span>
          <span
            v-if="event.isCharityEvent"
            class="chip-secondary"
          >
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>
            </svg>
            Charity Event
          </span>
          <span
            v-if="!event.isPublic"
            class="chip-warning"
          >
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
            </svg>
            Private
          </span>
        </div>

        <!-- Info Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <div class="space-y-4">
            <!-- Date & Time -->
            <div class="flex items-start gap-3">
              <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <div>
                <div class="font-medium">{{ formatDate(event.eventDate) }}</div>
                <div class="text-sm text-gray-500">{{ formatTime(event.startTime) }} ({{ event.durationMinutes }} min)</div>
              </div>
            </div>

            <!-- Location -->
            <div v-if="event.addressLine1 || event.city" class="flex items-start gap-3">
              <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
              </svg>
              <div>
                <div v-if="event.addressLine1" class="font-medium">{{ event.addressLine1 }}</div>
                <div class="text-sm text-gray-500">
                  {{ [event.city, event.state, event.postalCode].filter(Boolean).join(', ') }}
                </div>
                <div v-if="event.locationDetails" class="text-sm text-gray-500 mt-1">{{ event.locationDetails }}</div>
              </div>
            </div>
          </div>

          <!-- Players -->
          <div class="flex items-start gap-3">
            <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
            </svg>
            <div>
              <div class="font-medium">{{ event.confirmedCount }} / {{ event.maxPlayers }} players</div>
              <div class="text-sm text-gray-500">
                {{ spotsLeft > 0 ? `${spotsLeft} spots left` : 'Game is full' }}
              </div>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div v-if="event.description" class="mb-6">
          <h3 class="font-semibold mb-2">About this game night</h3>
          <p class="text-gray-600">{{ event.description }}</p>
        </div>

        <!-- Games -->
        <div v-if="event.games && event.games.length > 0" class="mb-6">
          <h3 class="font-semibold mb-3">Games</h3>
          <div class="space-y-3">
            <div
              v-for="game in event.games"
              :key="game.id"
              class="flex items-center gap-3 p-3 border border-gray-200 rounded-lg"
              :class="{ 'ring-2 ring-primary-500 bg-primary-50': game.isPrimary }"
            >
              <div class="w-12 h-12 rounded bg-gray-100 flex-shrink-0 overflow-hidden">
                <img
                  v-if="game.thumbnailUrl"
                  :src="game.thumbnailUrl"
                  :alt="game.gameName"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ game.gameName }}</span>
                  <span v-if="game.isPrimary" class="chip-primary text-xs">Primary</span>
                  <span v-else-if="game.isAlternative" class="chip text-xs bg-gray-100 text-gray-600">Alternative</span>
                </div>
                <div class="flex flex-wrap gap-2 mt-1 text-xs text-gray-500">
                  <span v-if="game.minPlayers || game.maxPlayers">
                    <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
                    </svg>
                    {{ game.minPlayers === game.maxPlayers ? `${game.minPlayers}` : `${game.minPlayers}-${game.maxPlayers}` }} players
                  </span>
                  <span v-if="game.playingTime">
                    <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
                    </svg>
                    {{ game.playingTime }} min
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Registration Actions -->
        <div v-if="auth.isAuthenticated.value && !isHost">
          <button
            v-if="!isRegistered && spotsLeft > 0"
            class="btn-primary"
            @click="handleRegister"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Join Game
          </button>
          <button
            v-else-if="isRegistered"
            class="btn border-2 border-red-500 text-red-500 hover:bg-red-50"
            @click="handleCancelRegistration"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C17.67,14 23,15.33 23,18V20H7V18C7,15.33 12.33,14 15,14M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12M5,9.59L7.12,7.46L8.54,8.88L6.41,11L8.54,13.12L7.12,14.54L5,12.41L2.88,14.54L1.46,13.12L3.59,11L1.46,8.88L2.88,7.46L5,9.59Z"/>
            </svg>
            Cancel Registration
          </button>
          <span v-else-if="spotsLeft <= 0" class="chip-error">
            Game is Full
          </span>
        </div>

        <div v-else-if="!auth.isAuthenticated.value">
          <button class="btn-outline" @click="goToLogin">
            Sign in to join
          </button>
        </div>
      </div>

      <!-- Registered Players -->
      <div v-if="event.registrations && event.registrations.length > 0" class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
            </svg>
            Players ({{ event.registrations.length }})
          </h2>
        </div>
        <div class="divide-y divide-gray-100">
          <div
            v-for="reg in event.registrations"
            :key="reg.id"
            class="flex items-center gap-3 p-4"
          >
            <div class="w-10 h-10 rounded-full bg-secondary-500 flex items-center justify-center overflow-hidden">
              <img v-if="reg.user?.avatarUrl" :src="reg.user.avatarUrl" class="w-full h-full object-cover" />
              <svg v-else class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>
            <div class="flex-1">
              <div class="font-medium">{{ reg.user?.displayName || 'Anonymous' }}</div>
              <div class="text-sm text-gray-500">{{ reg.status }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Items to Bring -->
      <div class="card">
        <div class="p-4 border-b border-gray-100 flex items-center justify-between">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,21H19V19H5M19,10H15V3H9V10H5L12,17L19,10Z"/>
            </svg>
            Items to Bring
          </h2>
          <button
            v-if="auth.isAuthenticated.value"
            class="btn-ghost text-primary-500"
            @click="addItemDialog = true"
          >
            <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Item
          </button>
        </div>
        <div v-if="event.items && event.items.length > 0" class="divide-y divide-gray-100">
          <div
            v-for="item in event.items"
            :key="item.id"
            class="flex items-center gap-3 p-4"
          >
            <svg
              class="w-5 h-5"
              :class="item.claimedByUserId ? 'text-green-500' : 'text-gray-300'"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path v-if="item.claimedByUserId" d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              <path v-else d="M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
            </svg>
            <div class="flex-1">
              <div class="font-medium">{{ item.itemName }}</div>
              <div class="flex items-center gap-2 text-sm text-gray-500">
                <span class="chip bg-gray-100 text-gray-600 text-xs">{{ item.itemCategory }}</span>
                <span v-if="item.claimedByName">Claimed by {{ item.claimedByName }}</span>
                <span v-else>Unclaimed</span>
              </div>
            </div>
            <button
              v-if="auth.isAuthenticated.value && !item.claimedByUserId"
              class="btn-sm btn-ghost text-primary-500"
              @click="handleClaimItem(item.id)"
            >
              Claim
            </button>
            <button
              v-else-if="item.claimedByUserId === auth.user.value?.id"
              class="btn-sm btn-ghost text-red-500"
              @click="handleUnclaimItem(item.id)"
            >
              Unclaim
            </button>
          </div>
        </div>
        <p v-else class="text-gray-500 text-center py-8">
          No items added yet
        </p>
      </div>
    </template>

    <!-- Add Item Dialog -->
    <div v-if="addItemDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="addItemDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Add Item</h3>
        <div class="space-y-4">
          <div>
            <label class="label">Item name</label>
            <input
              v-model="newItemName"
              type="text"
              class="input"
              placeholder="What should someone bring?"
            />
          </div>
          <div>
            <label class="label">Category</label>
            <select v-model="newItemCategory" class="input">
              <option value="food">Food</option>
              <option value="drinks">Drinks</option>
              <option value="supplies">Supplies</option>
              <option value="other">Other</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="addItemDialog = false">Cancel</button>
          <button class="btn-primary" @click="handleAddItem">Add</button>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div
      v-if="toast.visible"
      class="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 px-4 py-3 rounded-lg shadow-lg"
      :class="toast.type === 'success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- Share Modal -->
    <ShareModal
      v-if="event"
      :event-id="event.id"
      :event-title="event.title"
      :visible="showShareModal"
      @close="showShareModal = false"
    />
  </div>
</template>
