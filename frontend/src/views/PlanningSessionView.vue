<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getPlanningSession,
  respondToPlanningSession,
  suggestGame,
  voteForGame,
  unvoteGame,
  removeSuggestion,
  finalizePlanningSession,
  cancelPlanningSession,
} from '@/services/planningApi'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import type { PlanningSession, GameSuggestion } from '@/types/planning'
import type { BggSearchResult } from '@/types/bgg'
import DateAvailabilityMatrix from '@/components/planning/DateAvailabilityMatrix.vue'
import GameSuggestionCard from '@/components/planning/GameSuggestionCard.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const session = ref<PlanningSession | null>(null)
const errorMessage = ref('')
const successMessage = ref('')

// Response form state
const responding = ref(false)
const responseForm = reactive({
  cannotAttendAny: false,
  dateAvailability: {} as Record<string, boolean>,
})

// Game suggestion state
const gameSearchQuery = ref('')
const gameSearchResults = ref<BggSearchResult[]>([])
const searchingGames = ref(false)
const addingGame = ref(false)
const showGameSearch = ref(false)

// Finalization state
const finalizing = ref(false)
const selectedDateId = ref<string | null>(null)
const selectedGameId = ref<string | null>(null)

const sessionId = computed(() => route.params.id as string)

const isCreator = computed(() => {
  if (!session.value || !auth.user.value) return false
  return session.value.createdByUserId === auth.user.value.id
})

const currentUserInvitee = computed(() => {
  if (!session.value?.invitees || !auth.user.value) return null
  return session.value.invitees.find(i => i.userId === auth.user.value!.id)
})

const hasResponded = computed(() => currentUserInvitee.value?.hasResponded ?? false)

const isOpen = computed(() => session.value?.status === 'open')

const deadlinePassed = computed(() => {
  if (!session.value) return false
  return new Date(session.value.responseDeadline) < new Date()
})

const respondedCount = computed(() => {
  return session.value?.invitees?.filter(i => i.hasResponded).length ?? 0
})

const totalInvitees = computed(() => session.value?.invitees?.length ?? 0)

const bestDate = computed(() => {
  if (!session.value?.dates) return null
  const sorted = [...session.value.dates].sort((a, b) => (b.availableCount ?? 0) - (a.availableCount ?? 0))
  return sorted[0]
})

const topGame = computed(() => {
  if (!session.value?.gameSuggestions) return null
  const sorted = [...session.value.gameSuggestions].sort((a, b) => b.voteCount - a.voteCount)
  return sorted[0]
})

onMounted(async () => {
  await loadSession()
})

async function loadSession() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    session.value = await getPlanningSession(token, sessionId.value)

    // Initialize response form with existing votes
    if (session.value.dates) {
      for (const date of session.value.dates) {
        const userVote = date.votes?.find(v => v.userId === auth.user.value?.id)
        responseForm.dateAvailability[date.id] = userVote?.isAvailable ?? false
      }
    }

    // Pre-select best date/game for finalization
    if (bestDate.value) {
      selectedDateId.value = bestDate.value.id
    }
    if (topGame.value) {
      selectedGameId.value = topGame.value.id
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load session'
  } finally {
    loading.value = false
  }
}

async function submitResponse() {
  if (!session.value) return

  responding.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await respondToPlanningSession(token, session.value.id, {
      cannotAttendAny: responseForm.cannotAttendAny,
      dateAvailability: responseForm.cannotAttendAny
        ? []
        : Object.entries(responseForm.dateAvailability).map(([dateId, isAvailable]) => ({
            dateId,
            isAvailable,
          })),
    })

    successMessage.value = 'Your response has been recorded!'
    await loadSession()
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to submit response'
  } finally {
    responding.value = false
  }
}

let searchTimeout: ReturnType<typeof setTimeout> | null = null

function handleGameSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)

  if (gameSearchQuery.value.trim().length < 2) {
    gameSearchResults.value = []
    return
  }

  searchTimeout = setTimeout(async () => {
    searchingGames.value = true
    try {
      gameSearchResults.value = await searchBggGames(gameSearchQuery.value)
    } catch (err) {
      console.error('Search error:', err)
    } finally {
      searchingGames.value = false
    }
  }, 300)
}

async function selectGameToSuggest(result: BggSearchResult) {
  if (!session.value) return

  addingGame.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const game = await getBggGame(result.bggId)

    await suggestGame(token, session.value.id, {
      gameName: game.name,
      bggId: game.bggId,
      thumbnailUrl: game.thumbnailUrl ?? undefined,
      minPlayers: game.minPlayers ?? undefined,
      maxPlayers: game.maxPlayers ?? undefined,
      playingTime: game.playingTime ?? undefined,
    })

    gameSearchQuery.value = ''
    gameSearchResults.value = []
    showGameSearch.value = false
    await loadSession()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to add game suggestion'
  } finally {
    addingGame.value = false
  }
}

async function handleVoteGame(suggestion: GameSuggestion) {
  if (!session.value) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (suggestion.hasVoted) {
      await unvoteGame(token, session.value.id, suggestion.id)
    } else {
      await voteForGame(token, session.value.id, suggestion.id)
    }

    await loadSession()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to vote'
  }
}

function canRemoveSuggestion(suggestion: GameSuggestion): boolean {
  if (!session.value || !auth.user.value) return false
  if (session.value.status !== 'open') return false
  // Allow suggester or session creator to remove
  return suggestion.suggestedByUserId === auth.user.value.id || isCreator.value
}

async function handleRemoveSuggestion(suggestion: GameSuggestion) {
  if (!session.value) return
  if (!confirm(`Remove "${suggestion.gameName}" from suggestions?`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await removeSuggestion(token, session.value.id, suggestion.id)
    await loadSession()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to remove suggestion'
  }
}

async function handleFinalize() {
  if (!session.value || !selectedDateId.value) return

  finalizing.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const result = await finalizePlanningSession(
      token,
      session.value.id,
      selectedDateId.value,
      selectedGameId.value || undefined
    )

    router.push(`/games/${result.eventId}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to finalize session'
  } finally {
    finalizing.value = false
  }
}

async function handleCancel() {
  if (!session.value) return
  if (!confirm('Are you sure you want to cancel this planning session?')) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await cancelPlanningSession(token, session.value.id)
    await loadSession()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to cancel session'
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'open':
      return 'chip-success'
    case 'finalized':
      return 'chip bg-blue-100 text-blue-700'
    case 'cancelled':
      return 'chip-error'
    default:
      return 'chip bg-gray-100 text-gray-700'
  }
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Loading -->
    <div v-if="loading" class="card p-8 text-center">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading planning session...</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage && !session" class="alert-error">
      {{ errorMessage }}
    </div>

    <!-- Session Content -->
    <template v-else-if="session">
      <!-- Back link -->
      <div class="mb-6">
        <button
          v-if="session.group"
          class="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1"
          @click="router.push(`/groups/${session.group.slug}`)"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
          Back to {{ session.group.name }}
        </button>
      </div>

      <!-- Messages -->
      <div v-if="successMessage" class="alert-success mb-6">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="alert-error mb-6">
        {{ errorMessage }}
      </div>

      <!-- Header -->
      <div class="card mb-6">
        <div class="p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-3 mb-2">
                <h1 class="text-2xl font-bold">{{ session.title }}</h1>
                <span :class="getStatusBadgeClass(session.status)">{{ session.status }}</span>
              </div>
              <p v-if="session.description" class="text-gray-600 mb-3">{{ session.description }}</p>
              <div class="flex items-center gap-4 text-sm text-gray-500">
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                  </svg>
                  Created by {{ session.createdBy?.displayName || 'Unknown' }}
                </span>
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
                  </svg>
                  Deadline: {{ formatDateTime(session.responseDeadline) }}
                  <span v-if="deadlinePassed" class="text-red-500 font-medium">(Passed)</span>
                </span>
              </div>
            </div>
            <div v-if="isCreator && isOpen" class="flex gap-2">
              <button class="btn-ghost text-red-500" @click="handleCancel">
                Cancel
              </button>
            </div>
          </div>

          <!-- Response Progress -->
          <div class="mt-6 pt-6 border-t border-gray-100">
            <div class="flex items-center justify-between text-sm mb-2">
              <span class="font-medium">Responses</span>
              <span class="text-gray-500">{{ respondedCount }} of {{ totalInvitees }}</span>
            </div>
            <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full bg-primary-500 rounded-full transition-all"
                :style="{ width: `${totalInvitees > 0 ? (respondedCount / totalInvitees) * 100 : 0}%` }"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Finalized State -->
      <div v-if="session.status === 'finalized'" class="card mb-6 bg-blue-50 border-blue-200">
        <div class="p-6">
          <h2 class="font-semibold text-blue-800 mb-2">Session Finalized</h2>
          <p class="text-blue-700">
            The game night has been scheduled for {{ session.finalizedDate ? formatDate(session.finalizedDate) : 'TBD' }}.
          </p>
          <button
            v-if="session.createdEventId"
            class="btn-primary mt-4"
            @click="router.push(`/games/${session.createdEventId}`)"
          >
            View Event
          </button>
        </div>
      </div>

      <!-- Cancelled State -->
      <div v-else-if="session.status === 'cancelled'" class="card mb-6 bg-red-50 border-red-200">
        <div class="p-6">
          <h2 class="font-semibold text-red-800">Session Cancelled</h2>
          <p class="text-red-700">This planning session has been cancelled.</p>
        </div>
      </div>

      <!-- Active Session -->
      <template v-else>
        <!-- Your Response (for invitees) -->
        <div v-if="currentUserInvitee && !isCreator" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Your Availability</h2>
          </div>
          <div class="p-6">
            <div v-if="hasResponded" class="text-green-600 flex items-center gap-2 mb-4">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              You have already responded. You can update your response below.
            </div>

            <!-- Cannot Attend Checkbox -->
            <label class="flex items-center gap-3 mb-6 p-4 rounded-lg border border-gray-200 cursor-pointer hover:bg-gray-50">
              <input
                v-model="responseForm.cannotAttendAny"
                type="checkbox"
                class="w-5 h-5 text-red-500 rounded border-gray-300"
              />
              <div>
                <span class="font-medium">I can't make any of these dates</span>
                <p class="text-sm text-gray-500">Check this if none of the proposed dates work for you</p>
              </div>
            </label>

            <!-- Date Selection -->
            <div v-if="!responseForm.cannotAttendAny" class="space-y-3">
              <p class="text-sm text-gray-600 mb-2">Select the dates you're available:</p>
              <label
                v-for="date in session.dates"
                :key="date.id"
                class="flex items-center gap-3 p-4 rounded-lg border cursor-pointer hover:bg-gray-50 transition-colors"
                :class="responseForm.dateAvailability[date.id] ? 'border-green-500 bg-green-50' : 'border-gray-200'"
              >
                <input
                  v-model="responseForm.dateAvailability[date.id]"
                  type="checkbox"
                  class="w-5 h-5 text-green-500 rounded border-gray-300"
                />
                <div class="flex-1">
                  <span class="font-medium">{{ formatDate(date.proposedDate) }}</span>
                  <span v-if="date.startTime" class="text-gray-500 ml-2">at {{ date.startTime }}</span>
                </div>
                <span class="text-sm text-gray-500">{{ date.availableCount ?? 0 }} available</span>
              </label>
            </div>

            <button
              class="btn-primary mt-6"
              :disabled="responding"
              @click="submitResponse"
            >
              <svg v-if="responding" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              {{ hasResponded ? 'Update Response' : 'Submit Response' }}
            </button>
          </div>
        </div>

        <!-- Date Availability Matrix (for creator) -->
        <div v-if="isCreator && session.dates && session.invitees" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Availability Matrix</h2>
          </div>
          <div class="p-6 overflow-x-auto">
            <DateAvailabilityMatrix
              :dates="session.dates"
              :invitees="session.invitees"
              :selected-date-id="selectedDateId"
              @select-date="selectedDateId = $event"
            />
          </div>
        </div>

        <!-- Game Suggestions -->
        <div class="card mb-6">
          <div class="p-6 border-b border-gray-100 flex items-center justify-between">
            <h2 class="font-semibold">Game Suggestions</h2>
            <button
              v-if="!showGameSearch"
              class="btn-outline text-sm"
              @click="showGameSearch = true"
            >
              <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
              </svg>
              Suggest Game
            </button>
          </div>
          <div class="p-6">
            <!-- Game Search -->
            <div v-if="showGameSearch" class="mb-6">
              <div class="relative">
                <input
                  v-model="gameSearchQuery"
                  type="text"
                  class="input pr-10"
                  placeholder="Search for a game..."
                  @input="handleGameSearch"
                />
                <button
                  class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
                  @click="showGameSearch = false; gameSearchQuery = ''; gameSearchResults = []"
                >
                  <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                  </svg>
                </button>
              </div>

              <!-- Search Results -->
              <div v-if="searchingGames" class="mt-2 text-gray-500 text-sm">Searching...</div>
              <div v-else-if="gameSearchResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
                <button
                  v-for="result in gameSearchResults"
                  :key="result.bggId"
                  class="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left border-b border-gray-100 last:border-0"
                  :disabled="addingGame"
                  @click="selectGameToSuggest(result)"
                >
                  <svg class="w-5 h-5 text-gray-400 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium truncate">{{ result.name }}</div>
                    <div v-if="result.yearPublished" class="text-sm text-gray-500">{{ result.yearPublished }}</div>
                  </div>
                </button>
              </div>
            </div>

            <!-- Suggested Games List -->
            <div v-if="session.gameSuggestions && session.gameSuggestions.length > 0" class="space-y-3">
              <GameSuggestionCard
                v-for="suggestion in [...session.gameSuggestions].sort((a, b) => b.voteCount - a.voteCount)"
                :key="suggestion.id"
                :suggestion="suggestion"
                :selectable="isCreator"
                :selected="selectedGameId === suggestion.id"
                :removable="canRemoveSuggestion(suggestion)"
                @vote="handleVoteGame(suggestion)"
                @select="selectedGameId = suggestion.id"
                @remove="handleRemoveSuggestion(suggestion)"
              />
            </div>
            <div v-else class="text-gray-500 text-center py-6">
              No games suggested yet. Be the first to suggest a game!
            </div>
          </div>
        </div>

        <!-- Invitees List -->
        <div class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Invited Members</h2>
          </div>
          <div class="p-6">
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              <div
                v-for="invitee in session.invitees"
                :key="invitee.id"
                class="flex flex-col items-center text-center"
              >
                <div class="relative">
                  <div class="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden">
                    <img
                      v-if="invitee.user?.avatarUrl"
                      :src="invitee.user.avatarUrl"
                      class="w-full h-full object-cover"
                    />
                    <svg v-else class="w-6 h-6 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                    </svg>
                  </div>
                  <div
                    v-if="invitee.hasResponded"
                    class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center"
                    :class="invitee.cannotAttendAny ? 'bg-red-500' : 'bg-green-500'"
                  >
                    <svg v-if="invitee.cannotAttendAny" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                    <svg v-else class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                </div>
                <span class="text-sm mt-2 truncate w-full">{{ invitee.user?.displayName || 'Unknown' }}</span>
                <span class="text-xs text-gray-400">
                  {{ invitee.hasResponded ? (invitee.cannotAttendAny ? 'Unavailable' : 'Responded') : 'Pending' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Finalize Section (for creator) -->
        <div v-if="isCreator" class="card">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Finalize Game Night</h2>
          </div>
          <div class="p-6">
            <p class="text-gray-600 mb-4">
              When you're ready, finalize this session to create a draft event with the selected date and game.
            </p>

            <div class="grid grid-cols-2 gap-4 mb-6">
              <div>
                <label class="label">Selected Date</label>
                <select v-model="selectedDateId" class="input">
                  <option v-for="date in session.dates" :key="date.id" :value="date.id">
                    {{ formatDate(date.proposedDate) }} ({{ date.availableCount ?? 0 }} available)
                  </option>
                </select>
              </div>
              <div>
                <label class="label">Selected Game</label>
                <select v-model="selectedGameId" class="input">
                  <option :value="null">No game selected</option>
                  <option v-for="game in session.gameSuggestions" :key="game.id" :value="game.id">
                    {{ game.gameName }} ({{ game.voteCount }} votes)
                  </option>
                </select>
              </div>
            </div>

            <button
              class="btn-primary"
              :disabled="!selectedDateId || finalizing"
              @click="handleFinalize"
            >
              <svg v-if="finalizing" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Create Game Night Event
            </button>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
