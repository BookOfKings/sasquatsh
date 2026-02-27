<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import GameSearch from '@/components/common/GameSearch.vue'
import GameCard from '@/components/common/GameCard.vue'
import type { UpdateEventInput } from '@/types/events'
import type { BggGame } from '@/types/bgg'

const router = useRouter()
const route = useRoute()
const eventStore = useEventStore()
const authStore = useAuthStore()

const eventId = computed(() => route.params.id as string)

// Selected games for this event
const selectedGames = ref<BggGame[]>([])
const gameSearchQuery = ref('')

const loading = ref(false)
const loadingEvent = ref(true)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})

const form = reactive<UpdateEventInput>({
  title: '',
  description: null,
  gameTitle: null,
  gameCategory: null,
  eventDate: '',
  startTime: '',
  durationMinutes: 120,
  setupMinutes: 15,
  addressLine1: null,
  city: null,
  state: null,
  postalCode: null,
  locationDetails: null,
  difficultyLevel: null,
  maxPlayers: 4,
  isPublic: true,
  isCharityEvent: false,
  minAge: null,
  status: 'published',
})

onMounted(async () => {
  await loadEvent()
})

async function loadEvent() {
  loadingEvent.value = true
  const event = await eventStore.loadEvent(eventId.value)

  if (event) {
    // Check if user is the host
    if (event.hostUserId !== authStore.user.value?.id) {
      errorMessage.value = 'You are not authorized to edit this event'
      loadingEvent.value = false
      return
    }

    // Populate form with existing data
    form.title = event.title
    form.description = event.description ?? null
    form.gameTitle = event.gameTitle ?? null
    form.gameCategory = event.gameCategory ?? null
    form.eventDate = event.eventDate
    form.startTime = event.startTime
    form.durationMinutes = event.durationMinutes
    form.setupMinutes = event.setupMinutes ?? 15
    form.addressLine1 = event.addressLine1 ?? null
    form.city = event.city ?? null
    form.state = event.state ?? null
    form.postalCode = event.postalCode ?? null
    form.locationDetails = event.locationDetails ?? null
    form.difficultyLevel = event.difficultyLevel ?? null
    form.maxPlayers = event.maxPlayers
    form.isPublic = event.isPublic
    form.isCharityEvent = event.isCharityEvent
    form.minAge = event.minAge ?? null
    form.status = event.status

    // Load existing games
    if (event.games) {
      selectedGames.value = event.games
        .filter(g => g.bggId !== null)
        .map(g => ({
          bggId: g.bggId!,
          name: g.gameName,
          yearPublished: null,
          thumbnailUrl: g.thumbnailUrl ?? null,
          imageUrl: null,
          minPlayers: g.minPlayers ?? null,
          maxPlayers: g.maxPlayers ?? null,
          minPlaytime: null,
          maxPlaytime: null,
          playingTime: g.playingTime ?? null,
          weight: null,
          description: null,
          categories: [],
          mechanics: [],
        }))
    }
  } else {
    errorMessage.value = 'Event not found'
  }

  loadingEvent.value = false
}

const difficultyOptions = [
  { title: 'None', value: '' },
  { title: 'Beginner', value: 'beginner' },
  { title: 'Intermediate', value: 'intermediate' },
  { title: 'Advanced', value: 'advanced' },
]

const gameCategoryOptions = [
  { title: 'None', value: '' },
  { title: 'Strategy', value: 'strategy' },
  { title: 'Party', value: 'party' },
  { title: 'Cooperative', value: 'cooperative' },
  { title: 'Deck Building', value: 'deckbuilding' },
  { title: 'Worker Placement', value: 'workerplacement' },
  { title: 'Area Control', value: 'areacontrol' },
  { title: 'Dice', value: 'dice' },
  { title: 'Trivia', value: 'trivia' },
  { title: 'Role Playing', value: 'roleplaying' },
  { title: 'Miniatures', value: 'miniatures' },
  { title: 'Card', value: 'card' },
  { title: 'Family', value: 'family' },
  { title: 'Abstract', value: 'abstract' },
  { title: 'Other', value: 'other' },
]

const statusOptions = [
  { title: 'Draft', value: 'draft' },
  { title: 'Published', value: 'published' },
  { title: 'Cancelled', value: 'cancelled' },
]

function validate(): boolean {
  errors.title = ''
  errors.eventDate = ''
  errors.startTime = ''
  errors.durationMinutes = ''
  errors.maxPlayers = ''

  let valid = true

  if (!form.title?.trim()) {
    errors.title = 'Game title is required'
    valid = false
  }

  if (!form.eventDate) {
    errors.eventDate = 'Date is required'
    valid = false
  }

  if (!form.startTime) {
    errors.startTime = 'Start time is required'
    valid = false
  }

  if (!form.durationMinutes || form.durationMinutes <= 0) {
    errors.durationMinutes = 'Duration must be greater than 0'
    valid = false
  }

  if (!form.maxPlayers || form.maxPlayers <= 0) {
    errors.maxPlayers = 'Max players must be greater than 0'
    valid = false
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  errorMessage.value = ''

  const submitData = {
    ...form,
    gameCategory: form.gameCategory || null,
    difficultyLevel: form.difficultyLevel || null,
  }

  const result = await eventStore.updateEvent(eventId.value, submitData)

  if (result.ok) {
    router.push(`/games/${eventId.value}`)
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

function goBack() {
  router.push(`/games/${eventId.value}`)
}

function handleGameSelect(game: BggGame) {
  // Don't add duplicates
  if (selectedGames.value.some(g => g.bggId === game.bggId)) {
    return
  }
  selectedGames.value.push(game)
  gameSearchQuery.value = ''

  // Update form with primary game info if first game
  if (selectedGames.value.length === 1) {
    form.gameTitle = game.name
  }
}

function removeGame(index: number) {
  selectedGames.value.splice(index, 1)
  // Update form gameTitle with new primary (first) game
  const primaryGame = selectedGames.value[0]
  form.gameTitle = primaryGame?.name ?? ''
}

function setPrimaryGame(index: number) {
  if (index <= 0 || index >= selectedGames.value.length) return
  // Move the game to the front
  const game = selectedGames.value[index]
  if (!game) return
  selectedGames.value.splice(index, 1)
  selectedGames.value.unshift(game)
  form.gameTitle = game.name
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Back Button -->
    <button class="btn-ghost mb-4" @click="goBack">
      <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
      </svg>
      Back to Event
    </button>

    <!-- Loading State -->
    <div v-if="loadingEvent" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading event...</p>
    </div>

    <div v-else class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
          </svg>
          Edit Game
        </h1>
      </div>

      <div class="p-6">
        <!-- Error Alert -->
        <div v-if="errorMessage" class="alert-error mb-6">
          <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <span class="flex-1">{{ errorMessage }}</span>
          <button @click="errorMessage = ''" class="text-red-600 hover:text-red-800">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- Basic Info -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Basic Information</h3>

            <div class="space-y-4">
              <div>
                <label for="title" class="label">Game Title *</label>
                <input
                  id="title"
                  v-model="form.title"
                  type="text"
                  class="input"
                  :class="{ 'input-error': errors.title }"
                  placeholder="e.g., Friday Night Catan"
                  :disabled="loading"
                />
                <p v-if="errors.title" class="text-sm text-red-500 mt-1">{{ errors.title }}</p>
              </div>

              <!-- Game Search -->
              <div>
                <label class="label">Search BoardGameGeek</label>
                <GameSearch
                  v-model="gameSearchQuery"
                  placeholder="Search for a board game..."
                  :disabled="loading"
                  @select="handleGameSelect"
                />
                <p class="text-sm text-gray-500 mt-1">
                  Search BGG to add games with details, or type a name manually below.
                </p>
              </div>

              <!-- Selected Games -->
              <div v-if="selectedGames.length > 0" class="space-y-2">
                <label class="label">Selected Games</label>
                <GameCard
                  v-for="(game, index) in selectedGames"
                  :key="game.bggId"
                  :game="game"
                  :primary="index === 0"
                  removable
                  @remove="removeGame(index)"
                  @set-primary="setPrimaryGame(index)"
                />
              </div>

              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="md:col-span-2">
                  <label for="gameTitle" class="label">Primary Game Name</label>
                  <input
                    id="gameTitle"
                    v-model="form.gameTitle"
                    type="text"
                    class="input"
                    placeholder="e.g., Catan, Ticket to Ride"
                    :disabled="loading"
                  />
                  <p class="text-sm text-gray-500 mt-1">
                    {{ selectedGames.length > 0 ? 'Auto-filled from BGG selection' : 'Or enter manually' }}
                  </p>
                </div>
                <div>
                  <label for="gameCategory" class="label">Category</label>
                  <select
                    id="gameCategory"
                    v-model="form.gameCategory"
                    class="input"
                    :disabled="loading"
                  >
                    <option
                      v-for="opt in gameCategoryOptions"
                      :key="opt.value"
                      :value="opt.value || undefined"
                    >
                      {{ opt.title }}
                    </option>
                  </select>
                </div>
              </div>

              <div>
                <label for="description" class="label">Description</label>
                <textarea
                  id="description"
                  v-model="form.description"
                  rows="3"
                  class="input"
                  placeholder="Tell people about your event..."
                  :disabled="loading"
                />
              </div>
            </div>
          </div>

          <!-- Date & Time -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Date & Time</h3>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label for="eventDate" class="label">Date *</label>
                <input
                  id="eventDate"
                  v-model="form.eventDate"
                  type="date"
                  class="input"
                  :class="{ 'input-error': errors.eventDate }"
                  :disabled="loading"
                />
                <p v-if="errors.eventDate" class="text-sm text-red-500 mt-1">{{ errors.eventDate }}</p>
              </div>
              <div>
                <label for="startTime" class="label">Start Time *</label>
                <input
                  id="startTime"
                  v-model="form.startTime"
                  type="time"
                  class="input"
                  :class="{ 'input-error': errors.startTime }"
                  :disabled="loading"
                />
                <p v-if="errors.startTime" class="text-sm text-red-500 mt-1">{{ errors.startTime }}</p>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              <div>
                <label for="durationMinutes" class="label">Duration (minutes) *</label>
                <input
                  id="durationMinutes"
                  v-model.number="form.durationMinutes"
                  type="number"
                  class="input"
                  :class="{ 'input-error': errors.durationMinutes }"
                  min="1"
                  :disabled="loading"
                />
                <p v-if="errors.durationMinutes" class="text-sm text-red-500 mt-1">{{ errors.durationMinutes }}</p>
              </div>
              <div>
                <label for="setupMinutes" class="label">Setup Time (minutes)</label>
                <input
                  id="setupMinutes"
                  v-model.number="form.setupMinutes"
                  type="number"
                  class="input"
                  min="0"
                  :disabled="loading"
                />
              </div>
            </div>
          </div>

          <!-- Location -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Location</h3>

            <div class="space-y-4">
              <div>
                <label for="addressLine1" class="label">Address</label>
                <input
                  id="addressLine1"
                  v-model="form.addressLine1"
                  type="text"
                  class="input"
                  placeholder="123 Main St"
                  :disabled="loading"
                />
              </div>

              <div class="grid grid-cols-12 gap-4">
                <div class="col-span-5">
                  <label for="city" class="label">City</label>
                  <input
                    id="city"
                    v-model="form.city"
                    type="text"
                    class="input"
                    :disabled="loading"
                  />
                </div>
                <div class="col-span-4">
                  <label for="state" class="label">State</label>
                  <input
                    id="state"
                    v-model="form.state"
                    type="text"
                    class="input"
                    :disabled="loading"
                  />
                </div>
                <div class="col-span-3">
                  <label for="postalCode" class="label">Zip</label>
                  <input
                    id="postalCode"
                    v-model="form.postalCode"
                    type="text"
                    class="input"
                    :disabled="loading"
                  />
                </div>
              </div>

              <div>
                <label for="locationDetails" class="label">Location Details</label>
                <input
                  id="locationDetails"
                  v-model="form.locationDetails"
                  type="text"
                  class="input"
                  placeholder="e.g., Ring doorbell, upstairs apartment"
                  :disabled="loading"
                />
              </div>
            </div>
          </div>

          <!-- Game Settings -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Game Settings</h3>

            <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div>
                <label for="maxPlayers" class="label">Max Players *</label>
                <input
                  id="maxPlayers"
                  v-model.number="form.maxPlayers"
                  type="number"
                  class="input"
                  :class="{ 'input-error': errors.maxPlayers }"
                  min="1"
                  :disabled="loading"
                />
                <p v-if="errors.maxPlayers" class="text-sm text-red-500 mt-1">{{ errors.maxPlayers }}</p>
              </div>
              <div>
                <label for="minAge" class="label">Minimum Age</label>
                <input
                  id="minAge"
                  v-model.number="form.minAge"
                  type="number"
                  class="input"
                  min="0"
                  max="100"
                  placeholder="Any"
                  :disabled="loading"
                />
                <p class="text-sm text-gray-500 mt-1">Leave blank for all ages</p>
              </div>
              <div>
                <label for="difficultyLevel" class="label">Difficulty Level</label>
                <select
                  id="difficultyLevel"
                  v-model="form.difficultyLevel"
                  class="input"
                  :disabled="loading"
                >
                  <option
                    v-for="opt in difficultyOptions"
                    :key="opt.value"
                    :value="opt.value || undefined"
                  >
                    {{ opt.title }}
                  </option>
                </select>
              </div>
              <div>
                <label for="status" class="label">Status</label>
                <select
                  id="status"
                  v-model="form.status"
                  class="input"
                  :disabled="loading"
                >
                  <option
                    v-for="opt in statusOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.title }}
                  </option>
                </select>
              </div>
            </div>

            <div class="flex flex-col sm:flex-row gap-4 mt-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  v-model="form.isPublic"
                  type="checkbox"
                  class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
                  :disabled="loading"
                />
                <span class="text-sm text-gray-700">Public game (visible to everyone)</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  v-model="form.isCharityEvent"
                  type="checkbox"
                  class="w-4 h-4 rounded border-gray-300 text-secondary-500 focus:ring-secondary-500"
                  :disabled="loading"
                />
                <span class="text-sm text-gray-700">Charity event</span>
              </label>
            </div>
          </div>

          <!-- Actions -->
          <div class="border-t border-gray-200 pt-6 flex justify-end gap-3">
            <button
              type="button"
              class="btn-ghost"
              :disabled="loading"
              @click="goBack"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="loading"
            >
              <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
