<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useEventStore } from '@/stores/useEventStore'
import { useGroupStore } from '@/stores/useGroupStore'
import { useRouter } from 'vue-router'
import type { EventSummary } from '@/types/events'

const auth = useAuthStore()
const eventStore = useEventStore()
const groupStore = useGroupStore()
const router = useRouter()

const myGames = ref<EventSummary[]>([])
const hostedGames = ref<EventSummary[]>([])
const loadingMy = ref(true)
const loadingHosted = ref(true)
const loadingGroups = ref(true)

// Split groups into managed (owner/admin) and member-only
const managedGroups = computed(() =>
  groupStore.myGroups.value.filter(g => ['owner', 'admin'].includes(g.userRole as string))
)
const memberGroups = computed(() =>
  groupStore.myGroups.value.filter(g => g.userRole === 'member')
)

onMounted(async () => {
  // Load my registered games
  await eventStore.loadMyEvents()
  myGames.value = eventStore.myEvents.value
  loadingMy.value = false

  // Load my hosted games
  await eventStore.loadHostedEvents()
  hostedGames.value = eventStore.hostedEvents.value
  loadingHosted.value = false

  // Load my groups
  await groupStore.loadMyGroups()
  loadingGroups.value = false
})

async function handleLogout() {
  await auth.logout()
  router.push('/')
}

function goToGames() {
  router.push('/games')
}

function goToCreateGame() {
  router.push('/games/create')
}

function goToGame(id: string) {
  router.push(`/games/${id}`)
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

function formatTime(timeStr: string | undefined) {
  if (!timeStr) return ''
  const parts = timeStr.split(':')
  const hours = parts[0] ?? '0'
  const minutes = parts[1] ?? '00'
  const hour = parseInt(hours)
  const ampm = hour >= 12 ? 'PM' : 'AM'
  const hour12 = hour % 12 || 12
  return `${hour12}:${minutes} ${ampm}`
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

        <button @click="goToCreateGame" class="btn-primary">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Host a Game
        </button>
      </div>

      <hr class="border-gray-200 my-6" />

      <!-- My Games Section -->
      <div class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">My Upcoming Games</h2>
        </div>

        <div v-if="loadingMy" class="bg-gray-50 rounded-xl p-6">
          <div class="animate-pulse flex gap-4">
            <div class="h-12 w-12 bg-gray-200 rounded-lg"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-1/2"></div>
              <div class="h-3 bg-gray-200 rounded w-1/3"></div>
            </div>
          </div>
        </div>

        <div v-else-if="myGames.length === 0" class="bg-primary-50 rounded-xl p-6 text-center">
          <p class="text-gray-600 mb-3">You haven't signed up for any games yet.</p>
          <button @click="goToGames" class="btn-primary">
            Browse Games
          </button>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="game in myGames"
            :key="game.id"
            @click="goToGame(game.id)"
            class="bg-primary-50 rounded-xl p-4 cursor-pointer hover:bg-primary-100 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="font-semibold text-gray-900">{{ game.title }}</p>
                <p class="text-sm text-gray-600">
                  {{ formatDate(game.eventDate) }} at {{ formatTime(game.startTime) }}
                  <span v-if="game.city">&bull; {{ game.city }}<span v-if="game.state">, {{ game.state }}</span></span>
                </p>
              </div>
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- Hosted Games Section -->
      <div class="mb-6">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Games I'm Hosting</h2>
        </div>

        <div v-if="loadingHosted" class="bg-gray-50 rounded-xl p-6">
          <div class="animate-pulse flex gap-4">
            <div class="h-12 w-12 bg-gray-200 rounded-lg"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-1/2"></div>
              <div class="h-3 bg-gray-200 rounded w-1/3"></div>
            </div>
          </div>
        </div>

        <div v-else-if="hostedGames.length === 0" class="bg-secondary-50 rounded-xl p-6 text-center">
          <p class="text-gray-600 mb-3">You haven't hosted any games yet.</p>
          <button @click="goToCreateGame" class="btn-secondary">
            Host Your First Game
          </button>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="game in hostedGames"
            :key="game.id"
            @click="goToGame(game.id)"
            class="bg-secondary-50 rounded-xl p-4 cursor-pointer hover:bg-secondary-100 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <p class="font-semibold text-gray-900">{{ game.title }}</p>
                  <span
                    :class="game.status === 'published' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'"
                    class="text-xs px-2 py-0.5 rounded-full"
                  >
                    {{ game.status }}
                  </span>
                </div>
                <p class="text-sm text-gray-600">
                  {{ formatDate(game.eventDate) }} at {{ formatTime(game.startTime) }}
                  &bull; {{ game.confirmedCount }}/{{ game.maxPlayers }} players
                </p>
              </div>
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <hr class="border-gray-200 my-6" />

      <!-- Groups I Manage Section -->
      <div class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,1L21,5V11C21,16.55 17.16,21.74 12,23C6.84,21.74 3,16.55 3,11V5L12,1M12,5A3,3 0 0,0 9,8A3,3 0 0,0 12,11A3,3 0 0,0 15,8A3,3 0 0,0 12,5M17.13,17C15.92,18.85 14.11,20.24 12,20.92C9.89,20.24 8.08,18.85 6.87,17C6.53,16.5 6.24,16 6,15.47C6,13.82 8.71,12.47 12,12.47C15.29,12.47 18,13.79 18,15.47C17.76,16 17.47,16.5 17.13,17Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Groups I Manage</h2>
        </div>

        <div v-if="loadingGroups" class="bg-gray-50 rounded-xl p-6">
          <div class="animate-pulse flex gap-4">
            <div class="h-12 w-12 bg-gray-200 rounded-lg"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-1/2"></div>
              <div class="h-3 bg-gray-200 rounded w-1/3"></div>
            </div>
          </div>
        </div>

        <div v-else-if="managedGroups.length === 0" class="bg-gray-50 rounded-xl p-6 text-center">
          <p class="text-gray-600">You're not managing any groups yet.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="group in managedGroups"
            :key="group.id"
            @click="router.push(`/groups/${group.id}`)"
            class="bg-primary-50 rounded-xl p-4 cursor-pointer hover:bg-primary-100 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center overflow-hidden">
                  <img
                    v-if="group.logoUrl"
                    :src="group.logoUrl"
                    class="w-full h-full object-cover"
                  />
                  <svg v-else class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
                  </svg>
                </div>
                <div>
                  <p class="font-semibold text-gray-900">{{ group.name }}</p>
                  <p class="text-sm text-gray-600">
                    <span class="capitalize">{{ group.userRole }}</span>
                    &bull; {{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}
                  </p>
                </div>
              </div>
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- Groups I'm In Section -->
      <div class="mb-6">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Groups I'm In</h2>
        </div>

        <div v-if="loadingGroups" class="bg-gray-50 rounded-xl p-6">
          <div class="animate-pulse flex gap-4">
            <div class="h-12 w-12 bg-gray-200 rounded-lg"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-1/2"></div>
              <div class="h-3 bg-gray-200 rounded w-1/3"></div>
            </div>
          </div>
        </div>

        <div v-else-if="memberGroups.length === 0" class="bg-secondary-50 rounded-xl p-6 text-center">
          <p class="text-gray-600 mb-3">You haven't joined any groups yet.</p>
          <button @click="router.push('/groups')" class="btn-secondary">
            Browse Groups
          </button>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="group in memberGroups"
            :key="group.id"
            @click="router.push(`/groups/${group.id}`)"
            class="bg-secondary-50 rounded-xl p-4 cursor-pointer hover:bg-secondary-100 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg bg-secondary-100 flex items-center justify-center overflow-hidden">
                  <img
                    v-if="group.logoUrl"
                    :src="group.logoUrl"
                    class="w-full h-full object-cover"
                  />
                  <svg v-else class="w-5 h-5 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
                  </svg>
                </div>
                <div>
                  <p class="font-semibold text-gray-900">{{ group.name }}</p>
                  <p class="text-sm text-gray-600">
                    {{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}
                  </p>
                </div>
              </div>
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <hr class="border-gray-200 my-6" />

      <!-- Actions -->
      <div class="flex flex-col sm:flex-row sm:justify-between gap-3">
        <button class="btn-outline" @click="goToGames">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
          </svg>
          Browse Games
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
