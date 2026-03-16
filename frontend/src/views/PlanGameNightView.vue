<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getGroup, getGroupMembers } from '@/services/groupsApi'
import { createPlanningSession } from '@/services/planningApi'
import { hasFeature, TIER_NAMES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import type { Group, GroupMember } from '@/types/groups'
import type { BggSearchResult } from '@/types/bgg'
import type { SuggestGameInput } from '@/types/planning'
import UserAvatar from '@/components/common/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const currentTier = computed((): SubscriptionTier => {
  if (!auth.user.value) return 'free'
  return getEffectiveTier(auth.user.value)
})

const canUsePlanning = computed(() => hasFeature(currentTier.value, 'planning'))

const loading = ref(true)
const creating = ref(false)
const group = ref<Group | null>(null)
const members = ref<GroupMember[]>([])
const errorMessage = ref('')

// Game search state
const showGameSearch = ref(false)
const gameSearchQuery = ref('')
const gameSearchResults = ref<BggSearchResult[]>([])
const searchingGames = ref(false)
const suggestedGames = ref<SuggestGameInput[]>([])
let searchTimeout: ReturnType<typeof setTimeout> | null = null

const form = reactive({
  title: '',
  description: '',
  responseDeadline: '',
  selectedMemberIds: new Set<string>(),
  proposedDates: [{ date: '', startTime: '19:00' }] as { date: string; startTime: string }[],
  sendEmailInvites: false,
  hasParticipantLimit: false,
  maxParticipants: 8,
  isMultiTable: false,
  tableCount: 2,
})

const slug = computed(() => route.params.slug as string)

const isValid = computed(() => {
  return (
    form.title.trim() &&
    form.responseDeadline &&
    form.selectedMemberIds.size > 0 &&
    form.proposedDates.filter(d => d.date).length >= 1
  )
})

onMounted(async () => {
  // Wait for auth to be fully initialized before checking tier
  await auth.initializeAuth()
  await loadGroup()
})

async function loadGroup() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    group.value = await getGroup(slug.value)
    members.value = await getGroupMembers(token, group.value.id)

    // Set default deadline to 3 days from now
    const deadline = new Date()
    deadline.setDate(deadline.getDate() + 3)
    deadline.setHours(23, 59, 0, 0)
    form.responseDeadline = deadline.toISOString().slice(0, 16)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load group'
  } finally {
    loading.value = false
  }
}

function toggleMember(userId: string) {
  if (form.selectedMemberIds.has(userId)) {
    form.selectedMemberIds.delete(userId)
  } else {
    form.selectedMemberIds.add(userId)
  }
}

function selectAllMembers() {
  members.value.forEach(m => form.selectedMemberIds.add(m.userId))
}

function deselectAllMembers() {
  form.selectedMemberIds.clear()
}

function addDate() {
  form.proposedDates.push({ date: '', startTime: '19:00' })
}

function removeDate(index: number) {
  if (form.proposedDates.length > 1) {
    form.proposedDates.splice(index, 1)
  }
}

// Game search functions
function handleGameSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)

  if (!gameSearchQuery.value.trim()) {
    gameSearchResults.value = []
    return
  }

  searchTimeout = setTimeout(async () => {
    searchingGames.value = true
    try {
      gameSearchResults.value = await searchBggGames(gameSearchQuery.value)
    } catch {
      gameSearchResults.value = []
    } finally {
      searchingGames.value = false
    }
  }, 300)
}

async function selectGame(result: BggSearchResult) {
  // Check if already added
  if (suggestedGames.value.some(g => g.bggId === result.bggId)) {
    return
  }

  try {
    const game = await getBggGame(result.bggId)
    suggestedGames.value.push({
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
  } catch (err) {
    errorMessage.value = 'Failed to add game'
  }
}

function removeGame(index: number) {
  suggestedGames.value.splice(index, 1)
}

async function handleSubmit() {
  if (!isValid.value || !group.value) return

  creating.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    const session = await createPlanningSession(token, {
      groupId: group.value.id,
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      responseDeadline: new Date(form.responseDeadline).toISOString(),
      inviteeUserIds: Array.from(form.selectedMemberIds),
      proposedDates: form.proposedDates
        .filter(d => d.date)
        .map(d => ({
          date: d.date,
          startTime: d.startTime || undefined,
        })),
      sendEmailInvites: form.sendEmailInvites,
      initialGameSuggestions: suggestedGames.value.length > 0 ? suggestedGames.value : undefined,
      maxParticipants: form.hasParticipantLimit ? form.maxParticipants : undefined,
      tableCount: form.isMultiTable ? form.tableCount : undefined,
    })

    router.push(`/planning/${session.id}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create planning session'
  } finally {
    creating.value = false
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
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Loading -->
    <div v-if="loading" class="card p-8 text-center">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading group...</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage && !group" class="alert-error">
      {{ errorMessage }}
    </div>

    <!-- Form -->
    <template v-else-if="group">
      <div class="mb-6">
        <button
          class="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1"
          @click="router.push(`/groups/${slug}`)"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
          Back to {{ group.name }}
        </button>
      </div>

      <h1 class="text-2xl font-bold mb-6">Plan a Game</h1>

      <!-- Feature locked for free tier -->
      <div v-if="!canUsePlanning" class="card p-8 text-center">
        <svg class="w-12 h-12 mx-auto text-gray-400 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
        </svg>
        <h2 class="text-xl font-semibold mb-2">Planning Requires {{ TIER_NAMES.basic }} Plan</h2>
        <p class="text-gray-500 mb-6">Game night planning with date voting and group coordination is available on the Basic plan and above.</p>
        <button class="btn-primary" @click="router.push('/pricing')">View Plans</button>
      </div>

      <template v-if="canUsePlanning">
      <div v-if="errorMessage" class="alert-error mb-6">
        {{ errorMessage }}
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-6">
        <!-- Basic Info -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Basic Info</h2>
          <div class="space-y-4">
            <div>
              <label for="title" class="label">Title *</label>
              <input
                id="title"
                v-model="form.title"
                type="text"
                class="input"
                placeholder="e.g., Friday Game"
                required
              />
            </div>

            <div>
              <label for="description" class="label">Description</label>
              <textarea
                id="description"
                v-model="form.description"
                class="input"
                rows="2"
                placeholder="Optional details about this game night..."
              ></textarea>
            </div>

            <div>
              <label for="deadline" class="label">Response Deadline *</label>
              <input
                id="deadline"
                v-model="form.responseDeadline"
                type="datetime-local"
                class="input"
                required
              />
              <p class="text-sm text-gray-500 mt-1">Members have until this time to submit their availability</p>
            </div>
          </div>
        </div>

        <!-- Select Members -->
        <div class="card p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="font-semibold">Invite Members *</h2>
            <div class="flex gap-2">
              <button type="button" class="text-sm text-primary-500 hover:text-primary-600" @click="selectAllMembers">
                Select All
              </button>
              <span class="text-gray-300">|</span>
              <button type="button" class="text-sm text-gray-500 hover:text-gray-600" @click="deselectAllMembers">
                Deselect All
              </button>
            </div>
          </div>

          <div v-if="members.length === 0" class="text-gray-500 text-center py-4">
            No members in this group yet.
          </div>

          <div v-else class="space-y-2 max-h-64 overflow-y-auto">
            <label
              v-for="member in members"
              :key="member.userId"
              class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer"
              :class="{ 'bg-primary-50': form.selectedMemberIds.has(member.userId) }"
            >
              <input
                type="checkbox"
                :checked="form.selectedMemberIds.has(member.userId)"
                class="w-5 h-5 text-primary-500 rounded border-gray-300 focus:ring-primary-500"
                @change="toggleMember(member.userId)"
              />
              <UserAvatar
                :avatar-url="member.avatarUrl"
                :display-name="member.displayName"
                :is-founding-member="member.isFoundingMember"
                :is-admin="member.isAdmin"
                size="md"
                class="flex-shrink-0"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium">{{ member.displayName || member.username || 'Unknown' }}</div>
                <div class="text-sm text-gray-500 capitalize">{{ member.role }}</div>
              </div>
            </label>
          </div>

          <p class="text-sm text-gray-500 mt-3">
            {{ form.selectedMemberIds.size }} member{{ form.selectedMemberIds.size === 1 ? '' : 's' }} selected
          </p>
        </div>

        <!-- Propose Dates -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Propose Dates *</h2>
          <p class="text-sm text-gray-500 mb-4">Add one or more potential dates for the game night</p>

          <div class="space-y-3">
            <div
              v-for="(dateOption, index) in form.proposedDates"
              :key="index"
              class="flex items-center gap-3"
            >
              <div class="flex-1">
                <input
                  v-model="dateOption.date"
                  type="date"
                  class="input"
                  :min="new Date().toISOString().split('T')[0]"
                />
              </div>
              <div class="w-32">
                <input
                  v-model="dateOption.startTime"
                  type="time"
                  class="input"
                />
              </div>
              <button
                v-if="form.proposedDates.length > 1"
                type="button"
                class="p-2 text-gray-400 hover:text-red-500"
                @click="removeDate(index)"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
              <div v-else class="w-9"></div>
            </div>
          </div>

          <button
            type="button"
            class="btn-outline mt-4"
            @click="addDate"
          >
            <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Date Option
          </button>

          <div v-if="form.proposedDates.filter(d => d.date).length < 1" class="text-sm text-orange-600 mt-3">
            Please add at least 1 date option
          </div>
        </div>

        <!-- Preview -->
        <div v-if="form.proposedDates.some(d => d.date)" class="card p-6 bg-gray-50">
          <h2 class="font-semibold mb-3">Preview</h2>
          <div class="space-y-2">
            <div v-for="(dateOption, index) in form.proposedDates.filter(d => d.date)" :key="index" class="flex items-center gap-2 text-sm">
              <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <span>{{ formatDate(dateOption.date) }}</span>
              <span v-if="dateOption.startTime" class="text-gray-500">at {{ dateOption.startTime }}</span>
            </div>
          </div>
        </div>

        <!-- Game Suggestions (Optional) -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Suggest Games (Optional)</h2>
          <p class="text-sm text-gray-500 mb-4">Pre-populate game suggestions for the group to vote on</p>

          <!-- Game Search -->
          <div v-if="!showGameSearch" class="mb-4">
            <button type="button" class="btn-outline text-sm" @click="showGameSearch = true">
              <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
              </svg>
              Add a Game
            </button>
          </div>
          <div v-else class="mb-4">
            <div class="relative">
              <input
                v-model="gameSearchQuery"
                type="text"
                class="input pr-10"
                placeholder="Search BoardGameGeek..."
                @input="handleGameSearch"
              />
              <button
                type="button"
                class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
                @click="showGameSearch = false; gameSearchQuery = ''; gameSearchResults = []"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </div>
            <!-- Powered by BGG -->
            <div class="flex items-center justify-end mt-1">
              <a href="https://boardgamegeek.com" target="_blank" rel="noopener noreferrer" class="hover:opacity-80 transition-opacity">
                <img src="/powered-by-bgg.svg" alt="Powered by BoardGameGeek" class="h-5" />
              </a>
            </div>
            <div v-if="searchingGames" class="mt-2 text-gray-500 text-sm">Searching...</div>
            <div v-else-if="gameSearchResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
              <button
                v-for="result in gameSearchResults"
                :key="result.bggId"
                type="button"
                class="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left border-b border-gray-100 last:border-0"
                @click="selectGame(result)"
              >
                <img
                  v-if="result.thumbnailUrl"
                  :src="result.thumbnailUrl"
                  :alt="result.name"
                  class="w-10 h-10 object-cover rounded flex-shrink-0 bg-gray-100"
                />
                <div v-else class="w-10 h-10 flex items-center justify-center bg-gray-100 rounded flex-shrink-0">
                  <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="font-medium truncate">{{ result.name }}</div>
                  <div v-if="result.yearPublished" class="text-sm text-gray-500">{{ result.yearPublished }}</div>
                </div>
              </button>
            </div>
          </div>

          <!-- Suggested Games List -->
          <div v-if="suggestedGames.length > 0" class="space-y-2">
            <div
              v-for="(game, index) in suggestedGames"
              :key="game.bggId"
              class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
            >
              <img
                v-if="game.thumbnailUrl"
                :src="game.thumbnailUrl"
                :alt="game.gameName"
                class="w-10 h-10 object-cover rounded flex-shrink-0"
              />
              <div v-else class="w-10 h-10 flex items-center justify-center bg-gray-200 rounded flex-shrink-0">
                <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium truncate">{{ game.gameName }}</div>
                <div v-if="game.minPlayers && game.maxPlayers" class="text-sm text-gray-500">
                  {{ game.minPlayers }}-{{ game.maxPlayers }} players
                  <span v-if="game.playingTime"> · {{ game.playingTime }} min</span>
                </div>
              </div>
              <button
                type="button"
                class="p-1 text-gray-400 hover:text-red-500"
                @click="removeGame(index)"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </div>
          </div>
          <div v-else class="text-sm text-gray-500">
            No games suggested yet. You can add them now or let the group suggest them later.
          </div>
        </div>

        <!-- Email Notification Option -->
        <div class="card p-6">
          <label class="flex items-center gap-3 cursor-pointer">
            <input
              v-model="form.sendEmailInvites"
              type="checkbox"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
            />
            <div>
              <span class="font-medium text-gray-900">Send email invites</span>
              <p class="text-sm text-gray-500">Notify selected members via email about this planning session</p>
            </div>
          </label>
        </div>

        <!-- Participant Limit Option -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Participant Limit (Optional)</h2>
          <label class="flex items-center gap-3 cursor-pointer mb-4">
            <input
              v-model="form.hasParticipantLimit"
              type="checkbox"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
            />
            <div>
              <span class="font-medium text-gray-900">Limit participants</span>
              <p class="text-sm text-gray-500">First-come-first-served slot allocation</p>
            </div>
          </label>
          <div v-if="form.hasParticipantLimit" class="ml-8">
            <div class="flex items-center gap-3">
              <label for="maxParticipants" class="text-sm text-gray-600">Maximum participants:</label>
              <input
                id="maxParticipants"
                v-model.number="form.maxParticipants"
                type="number"
                min="2"
                max="100"
                class="input w-24"
              />
            </div>
            <p class="text-sm text-gray-500 mt-2">
              You automatically get 1 spot. {{ form.maxParticipants - 1 }} spots available for others.
            </p>
          </div>
        </div>

        <!-- Multi-Table Mode -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Multi-Table Mode (Optional)</h2>
          <label class="flex items-center gap-3 cursor-pointer mb-4">
            <input
              v-model="form.isMultiTable"
              type="checkbox"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
            />
            <div>
              <span class="font-medium text-gray-900">Enable multi-table scheduling</span>
              <p class="text-sm text-gray-500">Run multiple game sessions at different tables during the event</p>
            </div>
          </label>
          <div v-if="form.isMultiTable" class="ml-8">
            <div class="flex items-center gap-3">
              <label for="tableCount" class="text-sm text-gray-600">Number of tables:</label>
              <input
                id="tableCount"
                v-model.number="form.tableCount"
                type="number"
                min="2"
                max="20"
                class="input w-24"
              />
            </div>
            <p class="text-sm text-gray-500 mt-2">
              You'll be able to schedule different games at each table before finalizing.
              Members will then sign up for specific game sessions.
            </p>
          </div>
        </div>

        <!-- Submit -->
        <div class="flex justify-end gap-3">
          <button
            type="button"
            class="btn-ghost"
            @click="router.push(`/groups/${slug}`)"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="!isValid || creating"
          >
            <svg v-if="creating" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Create Planning Session
          </button>
        </div>
      </form>
      </template>
    </template>
  </div>
</template>
