<script setup lang="ts">
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { useGroupStore } from '@/stores/useGroupStore'
import { addEventGame } from '@/services/bggApi'
import { getMyProfile } from '@/services/profileApi'
import GameSearch from '@/components/common/GameSearch.vue'
import GameCard from '@/components/common/GameCard.vue'
import HotLocationsBar from '@/components/venues/HotLocationsBar.vue'
import VenueSelector from '@/components/venues/VenueSelector.vue'
import VenueDetailsFields from '@/components/venues/VenueDetailsFields.vue'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import UpgradePrompt from '@/components/billing/UpgradePrompt.vue'
import { TIER_LIMITS, hasFeature, TIER_NAMES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'
import type { CreateEventInput } from '@/types/events'
import type { BggGame } from '@/types/bgg'
import type { GroupSummary } from '@/types/groups'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const eventStore = useEventStore()
const authStore = useAuthStore()
const groupStore = useGroupStore()

// Groups where user is owner/admin (can create events for)
const userGroups = ref<GroupSummary[]>([])

// Selected games for this event
const selectedGames = ref<BggGame[]>([])
const gameSearchQuery = ref('')

// Venue selection state
const locationMode = ref<'venue' | 'custom'>('custom')
const selectedVenue = ref<EventLocation | null>(null)
const showVenueModal = ref(false)

const loading = ref(false)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})

// Tier limit checking
const showUpgradePrompt = ref(false)
const activeEventCount = ref(0)

const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

const eventLimit = computed(() => TIER_LIMITS[currentTier.value].gamesPerEvent)
const isAtLimit = computed(() => activeEventCount.value >= eventLimit.value)

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

const form = reactive<CreateEventInput>({
  title: '',
  description: '',
  gameTitle: '',
  gameCategory: undefined,
  eventDate: '',
  startTime: '19:00',
  timezone: 'America/New_York',
  durationMinutes: 120,
  setupMinutes: 15,
  addressLine1: '',
  city: '',
  state: '',
  postalCode: '',
  locationDetails: '',
  eventLocationId: undefined,
  venueHall: undefined,
  venueRoom: undefined,
  venueTable: undefined,
  difficultyLevel: undefined,
  maxPlayers: 4,
  hostIsPlaying: true,
  isPublic: true,
  isCharityEvent: false,
  minAge: undefined,
  status: 'published',
  groupId: undefined,
})

onMounted(async () => {
  // Load groups where user is owner/admin
  await groupStore.loadMyGroups()
  // Filter to only groups where user can create events
  userGroups.value = groupStore.myGroups.value.filter(
    g => g.userRole === 'owner' || g.userRole === 'admin'
  )

  // Load hosted events to check current count
  await eventStore.loadHostedEvents()
  // Count active (upcoming) events
  const today = new Date().toISOString().split('T')[0] ?? ''
  activeEventCount.value = eventStore.hostedEvents.value.filter(
    e => e.eventDate >= today
  ).length

  // Show upgrade prompt immediately if already at limit
  if (isAtLimit.value) {
    showUpgradePrompt.value = true
  }

  // Load user's profile to get their default timezone
  try {
    const token = await authStore.getIdToken()
    if (token) {
      const profile = await getMyProfile(token)
      if (profile.timezone) {
        form.timezone = profile.timezone
      }
    }
  } catch (err) {
    console.error('Failed to load profile for timezone:', err)
  }
})

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
]

function validate(): boolean {
  errors.title = ''
  errors.eventDate = ''
  errors.startTime = ''
  errors.durationMinutes = ''
  errors.maxPlayers = ''

  let valid = true

  if (!form.title.trim()) {
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
        if (!game) continue
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
    // Check if this is a tier limit error
    try {
      const errorData = JSON.parse(createResult.message)
      if (errorData.code === 'TIER_LIMIT_REACHED') {
        activeEventCount.value = errorData.currentCount
        showUpgradePrompt.value = true
        return
      }
    } catch {
      // Not a JSON error, show as-is
    }
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
  if (!game) return
  selectedGames.value.splice(index, 1)
  selectedGames.value.unshift(game)
  form.gameTitle = game.name
}

function handleVenueSelect(venue: EventLocation) {
  selectedVenue.value = venue
  form.eventLocationId = venue.id
  form.city = venue.city
  form.state = venue.state
  form.postalCode = venue.postalCode || ''
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
  form.eventLocationId = undefined
  form.venueHall = undefined
  form.venueRoom = undefined
  form.venueTable = undefined
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
      Back to Games
    </button>

    <div class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
          </svg>
          Host a Game
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
              <!-- Group Selector -->
              <div v-if="userGroups.length > 0">
                <label for="groupId" class="label">Host for Group (optional)</label>
                <select
                  id="groupId"
                  v-model="form.groupId"
                  class="input"
                  :disabled="loading"
                >
                  <option :value="undefined">No group - personal event</option>
                  <option
                    v-for="group in userGroups"
                    :key="group.id"
                    :value="group.id"
                  >
                    {{ group.name }}
                  </option>
                </select>
                <p class="text-sm text-gray-500 mt-1">
                  Group members will see this event on the group page.
                </p>
              </div>

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

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
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
              :selected-id="form.eventLocationId"
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
                  :model-value="form.eventLocationId ?? null"
                  :disabled="loading"
                  @update:model-value="(v) => form.eventLocationId = v ?? undefined"
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
                    @update:hall="(v) => form.venueHall = v ?? undefined"
                    @update:room="(v) => form.venueRoom = v ?? undefined"
                    @update:table="(v) => form.venueTable = v ?? undefined"
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
                  {{ form.hostIsPlaying ? `${(form.maxPlayers || 4) - 1} spots for others` : `${form.maxPlayers || 4} spots (you're not playing)` }}
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
              Host Game
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Submit Venue Modal -->
    <SubmitVenueModal
      :visible="showVenueModal"
      @close="showVenueModal = false"
      @submitted="handleVenueSubmitted"
    />

    <!-- Upgrade Prompt -->
    <UpgradePrompt
      :visible="showUpgradePrompt"
      :current-tier="currentTier"
      limit-type="games"
      :current-count="activeEventCount"
      :limit="eventLimit"
      @close="showUpgradePrompt = false"
    />
  </div>
</template>
