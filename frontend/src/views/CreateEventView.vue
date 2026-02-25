<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { addEventGame } from '@/services/bggApi'
import GameSearch from '@/components/common/GameSearch.vue'
import GameCard from '@/components/common/GameCard.vue'
import type { CreateEventInput } from '@/types/events'
import type { BggGame } from '@/types/bgg'

const router = useRouter()
const eventStore = useEventStore()
const authStore = useAuthStore()

// Selected games for this event
const selectedGames = ref<BggGame[]>([])
const gameSearchQuery = ref('')

const loading = ref(false)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})

const form = reactive<CreateEventInput>({
  title: '',
  description: '',
  gameTitle: '',
  gameCategory: undefined,
  eventDate: '',
  startTime: '19:00',
  durationMinutes: 120,
  setupMinutes: 15,
  addressLine1: '',
  city: '',
  state: '',
  postalCode: '',
  locationDetails: '',
  difficultyLevel: undefined,
  maxPlayers: 4,
  isPublic: true,
  isCharityEvent: false,
  status: 'draft',
})

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
]

function validate(): boolean {
  errors.title = ''
  errors.eventDate = ''
  errors.startTime = ''
  errors.durationMinutes = ''
  errors.maxPlayers = ''

  let valid = true

  if (!form.title.trim()) {
    errors.title = 'Game night title is required'
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
    gameCategory: form.gameCategory || undefined,
    difficultyLevel: form.difficultyLevel || undefined,
  }

  const createResult = await eventStore.createEvent(submitData)

  if (createResult.ok && createResult.event) {
    // Add selected games to the event
    const token = await authStore.getIdToken()
    if (token && selectedGames.value.length > 0) {
      for (let i = 0; i < selectedGames.value.length; i++) {
        const game = selectedGames.value[i]
        try {
          await addEventGame(token, createResult.event.id, {
            bggId: game.bggId,
            gameName: game.name,
            thumbnailUrl: game.thumbnailUrl ?? undefined,
            minPlayers: game.minPlayers ?? undefined,
            maxPlayers: game.maxPlayers ?? undefined,
            playingTime: game.playingTime ?? undefined,
            isPrimary: i === 0, // First game is primary
            isAlternative: i > 0, // Others are alternatives
          })
        } catch (err) {
          console.error('Failed to add game:', err)
        }
      }
    }
    router.push(`/games/${createResult.event.id}`)
  } else {
    errorMessage.value = createResult.message
  }

  loading.value = false
}

function goBack() {
  router.push('/games')
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
      Back to Games
    </button>

    <div class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
          </svg>
          Create Game Night
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
                <label for="title" class="label">Game Night Title *</label>
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

          <!-- Game Night Settings -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Game Night Settings</h3>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
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
              Create Game Night
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
