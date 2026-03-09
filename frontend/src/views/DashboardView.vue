<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useEventStore } from '@/stores/useEventStore'
import { useGroupStore } from '@/stores/useGroupStore'
import { useRouter } from 'vue-router'
import type { EventSummary } from '@/types/events'
import type { PlanningInvitation } from '@/types/planning'
import { getMyPlanningInvitations, respondToPlanningSession, acceptPlanningInvitation } from '@/services/planningApi'
import D20Spinner from '@/components/common/D20Spinner.vue'
import AdBanner from '@/components/ads/AdBanner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

const auth = useAuthStore()
const eventStore = useEventStore()
const groupStore = useGroupStore()
const router = useRouter()

const allMyGames = ref<EventSummary[]>([])
const allHostedGames = ref<EventSummary[]>([])
const planningInvitations = ref<PlanningInvitation[]>([])
const loadingMy = ref(true)
const loadingHosted = ref(true)
const loadingGroups = ref(true)
const loadingInvitations = ref(true)
const loadingPlanningInvitations = ref(true)
const respondingTo = ref<string | null>(null)
const decliningPlanningSession = ref<string | null>(null)
const acceptingPlanningSession = ref<string | null>(null)

// Check if initial page load is still in progress
const isInitialLoading = computed(() => loadingMy.value && loadingHosted.value && loadingGroups.value)

// Filter to pending planning invitations (open sessions where user hasn't accepted yet)
const pendingPlanningInvitations = computed(() =>
  planningInvitations.value.filter(p => p.status === 'open' && !p.acceptedAt && !p.hasResponded)
)

// Needs response sessions (accepted but not responded yet)
const needsResponseSessions = computed(() =>
  planningInvitations.value.filter(p => p.status === 'open' && p.acceptedAt && !p.hasResponded)
)

// Active planning sessions (user has responded and is attending, session still open)
const myActivePlanningSessions = computed(() =>
  planningInvitations.value.filter(p => p.status === 'open' && p.hasResponded && !p.cannotAttendAny)
)

// Get today's date for filtering
const today = new Date().toISOString().split('T')[0] ?? ''

// Filter to only upcoming games (today or future)
const myGames = computed(() =>
  allMyGames.value.filter(g => g.eventDate >= today)
)
const hostedGames = computed(() =>
  allHostedGames.value.filter(g => g.eventDate >= today)
)

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
  allMyGames.value = eventStore.myEvents.value
  loadingMy.value = false

  // Load my hosted games
  await eventStore.loadHostedEvents()
  allHostedGames.value = eventStore.hostedEvents.value
  loadingHosted.value = false

  // Load my groups and pending invitations in parallel
  await Promise.all([
    groupStore.loadMyGroups(),
    groupStore.loadPendingInvitations(),
  ])
  loadingGroups.value = false
  loadingInvitations.value = false

  // Load planning invitations
  try {
    const token = await auth.getIdToken()
    if (token) {
      planningInvitations.value = await getMyPlanningInvitations(token)
    }
  } catch (err) {
    console.error('Failed to load planning invitations:', err)
  } finally {
    loadingPlanningInvitations.value = false
  }
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

function goToCreateGroup() {
  router.push('/groups/create')
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

async function handleInvitationResponse(invitationId: string, response: 'accept' | 'decline') {
  respondingTo.value = invitationId
  const result = await groupStore.respondToInvitation(invitationId, response)
  respondingTo.value = null

  if (result.ok && response === 'accept' && result.groupId) {
    router.push(`/groups/${result.groupId}`)
  }
}

async function handleDeclinePlanningSession(sessionId: string) {
  decliningPlanningSession.value = sessionId
  try {
    const token = await auth.getIdToken()
    if (token) {
      // Mark as cannot attend any dates
      await respondToPlanningSession(token, sessionId, {
        dateAvailability: [],
        cannotAttendAny: true,
      })
      // Remove from local list
      planningInvitations.value = planningInvitations.value.filter(p => p.id !== sessionId)
    }
  } catch (err) {
    console.error('Failed to decline planning session:', err)
  } finally {
    decliningPlanningSession.value = null
  }
}

async function handleAcceptPlanningSession(sessionId: string) {
  acceptingPlanningSession.value = sessionId
  try {
    const token = await auth.getIdToken()
    if (token) {
      await acceptPlanningInvitation(token, sessionId)
      // Update local state to move from pending to needs-response
      const invitation = planningInvitations.value.find(p => p.id === sessionId)
      if (invitation) {
        invitation.acceptedAt = new Date().toISOString()
      }
    }
  } catch (err) {
    console.error('Failed to accept planning invitation:', err)
  } finally {
    acceptingPlanningSession.value = null
  }
}
</script>

<template>
  <!-- Initial Loading State -->
  <div v-if="isInitialLoading" class="container-wide py-8">
    <div class="card p-12 flex flex-col items-center justify-center">
      <D20Spinner size="xl" />
      <p class="mt-4 text-gray-500">Loading your dashboard...</p>
    </div>
  </div>

  <div v-else class="container-wide py-8">
    <div class="card p-6">
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div class="flex items-center gap-4">
          <UserAvatar
            :avatar-url="auth.user.value?.avatarUrl"
            :display-name="auth.user.value?.displayName"
            :is-founding-member="auth.user.value?.isFoundingMember"
            size="lg"
          />
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

      <!-- Pending Group Invitations -->
      <div v-if="groupStore.pendingInvitations.value.length > 0" class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,4H4A2,2 0 0,0 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6A2,2 0 0,0 20,4M20,18H4V8L12,13L20,8V18M20,6L12,11L4,6V6H20V6Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Group Invitations</h2>
          <span class="bg-yellow-100 text-yellow-700 text-xs font-medium px-2 py-0.5 rounded-full">
            {{ groupStore.pendingInvitations.value.length }} pending
          </span>
        </div>

        <div class="space-y-3">
          <div
            v-for="invitation in groupStore.pendingInvitations.value"
            :key="invitation.id"
            class="bg-yellow-50 border border-yellow-200 rounded-xl p-4"
          >
            <div class="flex items-start gap-4">
              <!-- Group Logo -->
              <div class="w-12 h-12 rounded-lg bg-yellow-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  v-if="invitation.group?.logoUrl"
                  :src="invitation.group.logoUrl"
                  class="w-full h-full object-cover"
                />
                <svg v-else class="w-6 h-6 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
                </svg>
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-gray-900">{{ invitation.group?.name || 'Unknown Group' }}</p>
                <p v-if="invitation.group?.description" class="text-sm text-gray-600 line-clamp-1">
                  {{ invitation.group.description }}
                </p>
                <p class="text-xs text-gray-500 mt-1">
                  Invited by {{ invitation.invitedBy?.displayName || 'someone' }}
                  <span v-if="invitation.group?.memberCount" class="ml-2">
                    &bull; {{ invitation.group.memberCount }} member{{ invitation.group.memberCount !== 1 ? 's' : '' }}
                  </span>
                </p>
              </div>

              <!-- Actions -->
              <div class="flex gap-2 flex-shrink-0">
                <button
                  @click="handleInvitationResponse(invitation.id, 'decline')"
                  :disabled="respondingTo === invitation.id"
                  class="btn-outline text-sm px-3 py-1.5"
                >
                  Decline
                </button>
                <button
                  @click="handleInvitationResponse(invitation.id, 'accept')"
                  :disabled="respondingTo === invitation.id"
                  class="btn-primary text-sm px-3 py-1.5"
                >
                  <svg v-if="respondingTo === invitation.id" class="w-4 h-4 mr-1 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  Accept
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Pending Planning Invitations -->
      <div v-if="pendingPlanningInvitations.length > 0" class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M11,9H9V12H6V14H9V17H11V14H14V12H11V9Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Game Invitations</h2>
          <span class="bg-purple-100 text-purple-700 text-xs font-medium px-2 py-0.5 rounded-full">
            {{ pendingPlanningInvitations.length }} pending
          </span>
        </div>

        <div class="space-y-3">
          <div
            v-for="invitation in pendingPlanningInvitations"
            :key="invitation.id"
            class="bg-purple-50 border border-purple-200 rounded-xl p-4"
          >
            <div class="flex items-start gap-4">
              <!-- Icon -->
              <div class="w-12 h-12 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
                <svg class="w-6 h-6 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
                </svg>
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-gray-900">{{ invitation.title }}</p>
                <p v-if="invitation.group" class="text-sm text-gray-600">
                  {{ invitation.group.name }}
                </p>
                <p class="text-xs text-gray-500 mt-1">
                  Hosted by {{ invitation.createdBy?.displayName || invitation.createdBy?.username || 'someone' }}
                  <span class="ml-2">&bull; Deadline: {{ formatDate(invitation.responseDeadline.split('T')[0] || '') }}</span>
                </p>
              </div>

              <!-- Actions -->
              <div class="flex gap-2 flex-shrink-0">
                <button
                  @click="handleDeclinePlanningSession(invitation.id)"
                  :disabled="decliningPlanningSession === invitation.id || acceptingPlanningSession === invitation.id"
                  class="btn-outline text-sm px-3 py-1.5"
                >
                  <svg v-if="decliningPlanningSession === invitation.id" class="w-4 h-4 mr-1 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  Decline
                </button>
                <button
                  @click="handleAcceptPlanningSession(invitation.id)"
                  :disabled="decliningPlanningSession === invitation.id || acceptingPlanningSession === invitation.id"
                  class="btn-primary text-sm px-3 py-1.5"
                >
                  <svg v-if="acceptingPlanningSession === invitation.id" class="w-4 h-4 mr-1 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  Accept
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Needs Your Response Section (Accepted but not responded) -->
      <div v-if="needsResponseSessions.length > 0" class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-blue-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Needs Your Response</h2>
          <span class="bg-blue-100 text-blue-700 text-xs font-medium px-2 py-0.5 rounded-full">
            {{ needsResponseSessions.length }}
          </span>
        </div>

        <div class="space-y-3">
          <div
            v-for="session in needsResponseSessions"
            :key="session.id"
            class="bg-blue-50 border border-blue-200 rounded-xl p-4"
          >
            <div class="flex items-start gap-4">
              <!-- Icon -->
              <div class="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                <svg class="w-6 h-6 text-blue-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                </svg>
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-gray-900">{{ session.title }}</p>
                <p v-if="session.group" class="text-sm text-gray-600">
                  {{ session.group.name }}
                </p>
                <p class="text-xs text-gray-500 mt-1">
                  Hosted by {{ session.createdBy?.displayName || session.createdBy?.username || 'someone' }}
                  <span class="ml-2">&bull; Respond by {{ formatDate(session.responseDeadline.split('T')[0] || '') }}</span>
                </p>
              </div>

              <!-- Actions -->
              <div class="flex gap-2 flex-shrink-0">
                <button
                  @click="router.push(`/planning/${session.id}`)"
                  class="btn-primary text-sm px-3 py-1.5"
                >
                  Respond
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- My Active Planning Sessions -->
      <div v-if="myActivePlanningSessions.length > 0" class="mb-8">
        <div class="flex items-center gap-3 mb-4">
          <svg class="w-6 h-6 text-green-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
          </svg>
          <h2 class="text-lg font-semibold text-gray-900">Games Being Planned</h2>
        </div>

        <div class="space-y-3">
          <div
            v-for="session in myActivePlanningSessions"
            :key="session.id"
            @click="router.push(`/planning/${session.id}`)"
            class="bg-green-50 rounded-xl p-4 cursor-pointer hover:bg-green-100 transition-colors"
          >
            <div class="flex items-center justify-between">
              <div>
                <p class="font-semibold text-gray-900">{{ session.title }}</p>
                <p class="text-sm text-gray-600">
                  {{ session.group?.name }}
                  <span class="ml-2">&bull; Deadline: {{ formatDate(session.responseDeadline.split('T')[0] || '') }}</span>
                </p>
              </div>
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

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
          <p class="text-gray-600 mb-3">You're not managing any groups yet.</p>
          <button @click="goToCreateGroup" class="btn-primary">
            Create Group
          </button>
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

    <!-- Ad Banner for free tier users -->
    <AdBanner placement="dashboard" />
  </div>
</template>
