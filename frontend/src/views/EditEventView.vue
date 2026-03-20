<script setup lang="ts">
import { reactive, ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { getLocationById } from '@/services/venuesApi'

// Timezone options
const timezoneOptions = [
  { value: 'America/New_York', label: 'Eastern Time (ET)' },
  { value: 'America/Chicago', label: 'Central Time (CT)' },
  { value: 'America/Denver', label: 'Mountain Time (MT)' },
  { value: 'America/Phoenix', label: 'Arizona (no DST)' },
  { value: 'America/Los_Angeles', label: 'Pacific Time (PT)' },
  { value: 'America/Anchorage', label: 'Alaska Time (AKT)' },
  { value: 'Pacific/Honolulu', label: 'Hawaii Time (HT)' },
  { value: 'Europe/London', label: 'UK (GMT/BST)' },
  { value: 'Europe/Paris', label: 'Central Europe (CET)' },
  { value: 'Asia/Tokyo', label: 'Japan (JST)' },
  { value: 'Australia/Sydney', label: 'Sydney (AEST)' },
]
import GameSearch from '@/components/common/GameSearch.vue'
import GameCard from '@/components/common/GameCard.vue'
import HotLocationsBar from '@/components/venues/HotLocationsBar.vue'
import VenueSelector from '@/components/venues/VenueSelector.vue'
import VenueDetailsFields from '@/components/venues/VenueDetailsFields.vue'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import { hasFeature, TIER_NAMES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'
import type { UpdateEventInput } from '@/types/events'
import type { PlannedGame } from '@/types/planning'
import type { BggGame } from '@/types/bgg'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const route = useRoute()
const eventStore = useEventStore()
const authStore = useAuthStore()

const eventId = computed(() => route.params.id as string)

const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

// Today's date for min date validation (YYYY-MM-DD format)
const today = computed(() => new Date().toISOString().split('T')[0])

// Selected games for this event
const selectedGames = ref<BggGame[]>([])
const gameSearchQuery = ref('')

// Planned games from planning session
const plannedGames = ref<PlannedGame[]>([])
const showAddPlannedGameDialog = ref(false)
const editingPlannedGameIndex = ref<number | null>(null)
const plannedGameForm = ref({
  name: '',
  interestedCount: 2,
  minPlayers: null as number | null,
  maxPlayers: null as number | null,
  playingTime: null as number | null,
})
const plannedGameSearchQuery = ref('')
const failedPlannedGameImages = ref<Set<string>>(new Set())

// Venue selection state
const locationMode = ref<'venue' | 'custom'>('custom')
const selectedVenue = ref<EventLocation | null>(null)
const showVenueModal = ref(false)

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
  timezone: null,
  durationMinutes: 120,
  setupMinutes: 15,
  addressLine1: null,
  city: null,
  state: null,
  postalCode: null,
  locationDetails: null,
  eventLocationId: null,
  venueHall: null,
  venueRoom: null,
  venueTable: null,
  difficultyLevel: null,
  maxPlayers: 4,
  hostIsPlaying: true,
  isPublic: true,
  isCharityEvent: false,
  minAge: null,
  status: 'published',
})

onMounted(async () => {
  // Wait for auth to be fully initialized before loading
  await authStore.initializeAuth()
  await loadEvent()
})

async function loadEvent() {
  loadingEvent.value = true
  const event = await eventStore.loadEvent(eventId.value)

  if (event) {
    // Check if user is the host or site admin
    const isHost = event.hostUserId === authStore.user.value?.id
    const isAdmin = authStore.user.value?.isAdmin ?? false
    if (!isHost && !isAdmin) {
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
    form.timezone = event.timezone ?? null
    form.durationMinutes = event.durationMinutes
    form.setupMinutes = event.setupMinutes ?? 15
    form.addressLine1 = event.addressLine1 ?? null
    form.city = event.city ?? null
    form.state = event.state ?? null
    form.postalCode = event.postalCode ?? null
    form.locationDetails = event.locationDetails ?? null
    form.eventLocationId = event.eventLocationId ?? null
    form.venueHall = event.venueHall ?? null
    form.venueRoom = event.venueRoom ?? null
    form.venueTable = event.venueTable ?? null
    form.difficultyLevel = event.difficultyLevel ?? null
    form.maxPlayers = event.maxPlayers
    form.hostIsPlaying = event.hostIsPlaying ?? true
    form.isPublic = event.isPublic
    form.isCharityEvent = event.isCharityEvent
    form.minAge = event.minAge ?? null
    form.status = event.status

    // Load venue details if set
    if (event.eventLocationId) {
      locationMode.value = 'venue'
      try {
        selectedVenue.value = await getLocationById(event.eventLocationId)
      } catch (err) {
        console.error('Failed to load venue details:', err)
        selectedVenue.value = null
        locationMode.value = 'custom'
      }
    }

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

    // Load planned games from planning session
    if (event.plannedGames) {
      plannedGames.value = [...event.plannedGames]
    }
  } else {
    errorMessage.value = 'Event not found'
  }

  loadingEvent.value = false
}

// When a venue is selected, use its timezone if available
watch(selectedVenue, (venue) => {
  if (venue?.timezone) {
    form.timezone = venue.timezone
  }
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
  { title: 'Cancelled', value: 'cancelled' },
]

function validate(): boolean {
  errors.title = ''
  errors.gameTitle = ''
  errors.eventDate = ''
  errors.startTime = ''
  errors.durationMinutes = ''
  errors.maxPlayers = ''

  let valid = true

  if (!form.title?.trim()) {
    errors.title = 'Game title is required'
    valid = false
  }

  if (!form.gameTitle?.trim()) {
    errors.gameTitle = 'Primary game name is required'
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

  // Check if event date/time is in the past
  if (form.eventDate && form.startTime) {
    const eventDateTime = new Date(`${form.eventDate}T${form.startTime}`)
    if (eventDateTime < new Date()) {
      errors.eventDate = 'Event cannot be scheduled in the past'
      valid = false
    }
  }

  if (!form.durationMinutes || form.durationMinutes <= 0) {
    errors.durationMinutes = 'Duration must be greater than 0'
    valid = false
  }

  if (!form.maxPlayers || form.maxPlayers <= 0) {
    errors.maxPlayers = 'Max players must be greater than 0'
    valid = false
  }

  // Require either a venue or a custom address with zip code
  const hasVenue = locationMode.value === 'venue' && selectedVenue.value
  const hasCustomAddress = form.city?.trim() && form.postalCode?.trim()
  if (!hasVenue && !hasCustomAddress) {
    if (form.city?.trim() && !form.postalCode?.trim()) {
      errors.location = 'Zip code is required for games to appear in nearby searches'
    } else {
      errors.location = 'Please select a venue or enter a city and zip code'
    }
    valid = false
  } else {
    errors.location = ''
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
    plannedGames: plannedGames.value.length > 0 ? plannedGames.value : null,
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

// Planned games management
function openAddPlannedGameDialog() {
  plannedGameForm.value = {
    name: '',
    interestedCount: 2,
    minPlayers: null,
    maxPlayers: null,
    playingTime: null,
  }
  plannedGameSearchQuery.value = ''
  editingPlannedGameIndex.value = null
  showAddPlannedGameDialog.value = true
}

function openEditPlannedGameDialog(index: number) {
  const game = plannedGames.value[index]
  if (!game) return
  plannedGameForm.value = {
    name: game.name,
    interestedCount: game.interestedCount,
    minPlayers: game.minPlayers,
    maxPlayers: game.maxPlayers,
    playingTime: game.playingTime,
  }
  editingPlannedGameIndex.value = index
  showAddPlannedGameDialog.value = true
}

function handlePlannedGameSelect(game: BggGame) {
  plannedGameForm.value.name = game.name
  plannedGameForm.value.minPlayers = game.minPlayers
  plannedGameForm.value.maxPlayers = game.maxPlayers
  plannedGameForm.value.playingTime = game.playingTime
  plannedGameSearchQuery.value = ''

  // If adding new, add it directly
  if (editingPlannedGameIndex.value === null) {
    plannedGames.value.push({
      bggId: game.bggId,
      name: game.name,
      image: game.thumbnailUrl,
      interestedCount: plannedGameForm.value.interestedCount,
      minPlayers: game.minPlayers,
      maxPlayers: game.maxPlayers,
      playingTime: game.playingTime,
    })
    showAddPlannedGameDialog.value = false
  }
}

function savePlannedGame() {
  if (!plannedGameForm.value.name.trim()) return

  if (editingPlannedGameIndex.value !== null) {
    // Update existing
    const existing = plannedGames.value[editingPlannedGameIndex.value]
    if (!existing) return
    plannedGames.value[editingPlannedGameIndex.value] = {
      bggId: existing.bggId,
      image: existing.image,
      name: plannedGameForm.value.name,
      interestedCount: plannedGameForm.value.interestedCount,
      minPlayers: plannedGameForm.value.minPlayers,
      maxPlayers: plannedGameForm.value.maxPlayers,
      playingTime: plannedGameForm.value.playingTime,
    }
  } else {
    // Add new (manual entry without BGG)
    plannedGames.value.push({
      bggId: null,
      name: plannedGameForm.value.name,
      image: null,
      interestedCount: plannedGameForm.value.interestedCount,
      minPlayers: plannedGameForm.value.minPlayers,
      maxPlayers: plannedGameForm.value.maxPlayers,
      playingTime: plannedGameForm.value.playingTime,
    })
  }
  showAddPlannedGameDialog.value = false
}

function removePlannedGame(index: number) {
  plannedGames.value.splice(index, 1)
}

function handleVenueSelect(venue: EventLocation) {
  selectedVenue.value = venue
  form.eventLocationId = venue.id
  form.city = venue.city
  form.state = venue.state
  form.postalCode = venue.postalCode || null
  locationMode.value = 'venue'
}

function handleVenueSelectorSelect(venue: EventLocation | null) {
  if (venue) {
    handleVenueSelect(venue)
  } else {
    clearVenueSelection()
  }
}

function clearVenueSelection() {
  selectedVenue.value = null
  form.eventLocationId = null
  form.venueHall = null
  form.venueRoom = null
  form.venueTable = null
}

function handleVenueSubmitted(venue: EventLocation) {
  console.log('Venue submitted:', venue.name)
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
                  <label for="gameTitle" class="label">Primary Game Name *</label>
                  <input
                    id="gameTitle"
                    v-model="form.gameTitle"
                    type="text"
                    class="input"
                    :class="{ 'input-error': errors.gameTitle }"
                    placeholder="e.g., Catan, Ticket to Ride"
                    :disabled="loading"
                  />
                  <p v-if="errors.gameTitle" class="text-sm text-red-500 mt-1">{{ errors.gameTitle }}</p>
                  <p v-else class="text-sm text-gray-500 mt-1">
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

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label for="eventDate" class="label">Date *</label>
                <input
                  id="eventDate"
                  v-model="form.eventDate"
                  type="date"
                  class="input"
                  :class="{ 'input-error': errors.eventDate }"
                  :min="today"
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
              <div>
                <label for="timezone" class="label">Time Zone</label>
                <select
                  id="timezone"
                  v-model="form.timezone"
                  class="input"
                  :disabled="loading"
                >
                  <option v-for="tz in timezoneOptions" :key="tz.value" :value="tz.value">
                    {{ tz.label }}
                  </option>
                </select>
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

            <!-- Hot Locations Quick Select -->
            <HotLocationsBar
              :selected-id="form.eventLocationId ?? undefined"
              @select="handleVenueSelect"
            />

            <!-- Location Mode Toggle -->
            <div class="flex gap-4 mb-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  v-model="locationMode"
                  value="venue"
                  class="w-4 h-4 text-primary-500 focus:ring-primary-500"
                  :disabled="loading"
                />
                <span class="text-sm text-gray-700">Select a venue</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  v-model="locationMode"
                  value="custom"
                  class="w-4 h-4 text-primary-500 focus:ring-primary-500"
                  :disabled="loading"
                  @change="clearVenueSelection"
                />
                <span class="text-sm text-gray-700">Enter custom address</span>
              </label>
            </div>

            <!-- Venue Selection Mode -->
            <div v-if="locationMode === 'venue'" class="space-y-4">
              <div>
                <label class="label">Select a Venue</label>
                <VenueSelector
                  :model-value="form.eventLocationId"
                  :disabled="loading"
                  @update:model-value="(v) => form.eventLocationId = v"
                  @select="handleVenueSelectorSelect"
                />
                <div class="flex items-center gap-2 mt-2">
                  <button
                    type="button"
                    class="text-sm text-primary-500 hover:text-primary-600"
                    @click="showVenueModal = true"
                  >
                    + Submit a new venue
                  </button>
                </div>
              </div>

              <!-- Venue Details (Hall/Room/Table) when venue selected -->
              <div v-if="selectedVenue">
                <template v-if="hasFeature(currentTier, 'tableInfo')">
                  <label class="label">Location Details (optional)</label>
                  <VenueDetailsFields
                    :hall="form.venueHall"
                    :room="form.venueRoom"
                    :table="form.venueTable"
                    :disabled="loading"
                    @update:hall="(v) => form.venueHall = v"
                    @update:room="(v) => form.venueRoom = v"
                    @update:table="(v) => form.venueTable = v"
                  />
                </template>
                <div v-else class="rounded-lg bg-gray-50 border border-gray-200 p-4 text-center">
                  <svg class="w-8 h-8 mx-auto text-gray-400 mb-2" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
                  </svg>
                  <p class="text-sm text-gray-500">Hall, room, and table details require {{ TIER_NAMES.basic }} plan</p>
                  <button type="button" class="text-sm text-primary-500 hover:text-primary-600 mt-1" @click="$router.push('/pricing')">Upgrade</button>
                </div>
              </div>

              <div>
                <label for="locationDetails" class="label">Additional Details</label>
                <input
                  id="locationDetails"
                  v-model="form.locationDetails"
                  type="text"
                  class="input"
                  placeholder="e.g., Meet at the registration desk"
                  :disabled="loading"
                />
              </div>
            </div>

            <!-- Custom Address Mode -->
            <div v-else class="space-y-4">
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

            <!-- Location Error -->
            <p v-if="errors.location" class="text-sm text-red-500 mt-2">{{ errors.location }}</p>
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
                <p class="text-sm text-gray-500 mt-1">
                  {{ form.hostIsPlaying ? `${form.maxPlayers - 1} spots for others` : `${form.maxPlayers} spots (you're not playing)` }}
                </p>
              </div>
              <div class="flex items-center">
                <label class="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    v-model="form.hostIsPlaying"
                    class="w-5 h-5 rounded text-primary-500 border-gray-300 focus:ring-primary-500"
                    :disabled="loading"
                  />
                  <div>
                    <span class="label">I am playing</span>
                    <p class="text-sm text-gray-500">Include yourself as a player</p>
                  </div>
                </label>
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

          <!-- Planned Games Section -->
          <div v-if="plannedGames.length > 0 || true" class="border-t border-gray-200 pt-6">
            <div class="flex items-center justify-between mb-4">
              <div>
                <h3 class="font-semibold text-gray-900">Planned Games</h3>
                <p class="text-sm text-gray-500">Games from planning session or added manually</p>
              </div>
              <button
                type="button"
                class="btn-outline text-sm"
                @click="openAddPlannedGameDialog"
                :disabled="loading"
              >
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
                </svg>
                Add Game
              </button>
            </div>

            <div v-if="plannedGames.length === 0" class="text-center py-8 text-gray-500 border-2 border-dashed border-gray-200 rounded-lg">
              <svg class="w-12 h-12 mx-auto text-gray-300 mb-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
              <p>No planned games yet</p>
              <p class="text-sm">Click "Add Game" to add games for this event</p>
            </div>

            <div v-else class="space-y-2">
              <div
                v-for="(game, index) in plannedGames"
                :key="game.bggId || index"
                class="flex items-center gap-3 p-3 border border-green-200 rounded-lg bg-green-50"
              >
                <div class="w-12 h-12 rounded bg-white flex-shrink-0 overflow-hidden">
                  <img
                    v-if="game.image && !failedPlannedGameImages.has(game.image)"
                    :src="game.image"
                    :alt="game.name"
                    class="w-full h-full object-cover"
                    @error="() => game.image && failedPlannedGameImages.add(game.image)"
                  />
                  <div v-if="!game.image || failedPlannedGameImages.has(game.image || '')" class="w-full h-full flex items-center justify-center">
                    <svg class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                    </svg>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-gray-900">{{ game.name }}</div>
                  <div class="flex flex-wrap gap-x-3 gap-y-1 text-sm">
                    <span class="text-green-600">{{ game.interestedCount }} interested</span>
                    <span v-if="game.minPlayers || game.maxPlayers" class="text-gray-500">
                      {{ game.minPlayers === game.maxPlayers ? `${game.minPlayers}p` : `${game.minPlayers}-${game.maxPlayers}p` }}
                    </span>
                    <span v-if="game.playingTime" class="text-gray-500">
                      {{ game.playingTime }}min
                    </span>
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <button
                    type="button"
                    class="p-2 text-gray-400 hover:text-primary-600 hover:bg-white rounded"
                    title="Edit game"
                    @click="openEditPlannedGameDialog(index)"
                    :disabled="loading"
                  >
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                    </svg>
                  </button>
                  <button
                    type="button"
                    class="p-2 text-gray-400 hover:text-red-600 hover:bg-white rounded"
                    title="Remove game"
                    @click="removePlannedGame(index)"
                    :disabled="loading"
                  >
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                  </button>
                </div>
              </div>
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

    <!-- Add/Edit Planned Game Dialog -->
    <Teleport to="body">
      <div
        v-if="showAddPlannedGameDialog"
        class="fixed inset-0 z-50 flex items-center justify-center"
      >
        <div class="absolute inset-0 bg-black/50" @click="showAddPlannedGameDialog = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
          <div class="p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-semibold text-gray-900">
                {{ editingPlannedGameIndex !== null ? 'Edit Planned Game' : 'Add Planned Game' }}
              </h3>
              <button
                type="button"
                class="p-1 text-gray-400 hover:text-gray-600"
                @click="showAddPlannedGameDialog = false"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </div>

            <!-- BGG Search (only for new games) -->
            <div v-if="editingPlannedGameIndex === null" class="mb-4">
              <label class="label">Search BoardGameGeek</label>
              <GameSearch
                v-model="plannedGameSearchQuery"
                @select="handlePlannedGameSelect"
                placeholder="Search for a board game..."
              />
              <p class="text-sm text-gray-500 mt-1">Or enter game details manually below</p>
            </div>

            <div class="space-y-4">
              <div>
                <label class="label">Game Name *</label>
                <input
                  v-model="plannedGameForm.name"
                  type="text"
                  class="input"
                  placeholder="Enter game name"
                />
              </div>

              <div>
                <label class="label">Interested Players</label>
                <input
                  v-model.number="plannedGameForm.interestedCount"
                  type="number"
                  class="input"
                  min="1"
                  placeholder="Number of interested players"
                />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="label">Min Players</label>
                  <input
                    v-model.number="plannedGameForm.minPlayers"
                    type="number"
                    class="input"
                    min="1"
                    placeholder="Min"
                  />
                </div>
                <div>
                  <label class="label">Max Players</label>
                  <input
                    v-model.number="plannedGameForm.maxPlayers"
                    type="number"
                    class="input"
                    min="1"
                    placeholder="Max"
                  />
                </div>
              </div>

              <div>
                <label class="label">Playing Time (minutes)</label>
                <input
                  v-model.number="plannedGameForm.playingTime"
                  type="number"
                  class="input"
                  min="1"
                  placeholder="e.g., 60"
                />
              </div>
            </div>

            <div class="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button
                type="button"
                class="btn-ghost"
                @click="showAddPlannedGameDialog = false"
              >
                Cancel
              </button>
              <button
                type="button"
                class="btn-primary"
                :disabled="!plannedGameForm.name.trim()"
                @click="savePlannedGame"
              >
                {{ editingPlannedGameIndex !== null ? 'Save Changes' : 'Add Game' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Submit Venue Modal -->
    <SubmitVenueModal
      :visible="showVenueModal"
      @close="showVenueModal = false"
      @submitted="handleVenueSubmitted"
    />
  </div>
</template>
