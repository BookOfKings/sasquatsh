<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { getMyProfile } from '@/services/profileApi'
import EventList from '@/components/events/EventList.vue'
import AdBanner from '@/components/ads/AdBanner.vue'
import HotLocationsBar from '@/components/venues/HotLocationsBar.vue'
import type { EventSummary, EventSearchFilter } from '@/types/events'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const eventStore = useEventStore()
const auth = useAuthStore()

// Filter state
const showFilters = ref(false)
const searchText = ref('')
const city = ref('')
const state = ref('')
const gameCategory = ref<string | null>(null)
const difficulty = ref<string | null>(null)
const selectedVenue = ref<EventLocation | null>(null)

// Nearby search state
const nearbyEnabled = ref(false)
const userPostalCode = ref<string | null>(null)
const radiusMiles = ref(25)

const radiusOptions = [
  { label: '10 miles', value: 10 },
  { label: '25 miles', value: 25 },
  { label: '50 miles', value: 50 },
  { label: '100 miles', value: 100 },
]

const gameCategoryOptions = [
  { title: 'All Categories', value: null },
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

const difficultyOptions = [
  { title: 'All Levels', value: null },
  { title: 'Beginner', value: 'beginner' },
  { title: 'Intermediate', value: 'intermediate' },
  { title: 'Advanced', value: 'advanced' },
]

function buildFilter(): EventSearchFilter | undefined {
  const filter: EventSearchFilter = {}
  if (searchText.value.trim()) filter.search = searchText.value.trim()

  // If venue is selected, filter by venue (ignore location filters)
  if (selectedVenue.value) {
    filter.venueId = selectedVenue.value.id
  } else if (nearbyEnabled.value && userPostalCode.value) {
    // Use nearby search if enabled and user has postal code
    filter.nearbyZip = userPostalCode.value
    filter.radiusMiles = radiusMiles.value
  } else {
    // Otherwise use city/state text filtering
    if (city.value.trim()) filter.city = city.value.trim()
    if (state.value.trim()) filter.state = state.value.trim()
  }

  if (gameCategory.value) filter.gameCategory = gameCategory.value as EventSearchFilter['gameCategory']
  if (difficulty.value) filter.difficulty = difficulty.value as EventSearchFilter['difficulty']

  return Object.keys(filter).length > 0 ? filter : undefined
}

async function applyFilters() {
  await eventStore.loadPublicEvents(buildFilter())
}

function clearFilters() {
  searchText.value = ''
  city.value = ''
  state.value = ''
  gameCategory.value = null
  difficulty.value = null
  nearbyEnabled.value = false
  selectedVenue.value = null
  applyFilters()
}

function selectVenue(venue: EventLocation) {
  // Toggle selection - if same venue is clicked, deselect it
  if (selectedVenue.value?.id === venue.id) {
    selectedVenue.value = null
  } else {
    selectedVenue.value = venue
    // Clear other location filters when selecting a venue
    nearbyEnabled.value = false
    city.value = ''
    state.value = ''
  }
  applyFilters()
}

const hasActiveFilters = () => {
  return searchText.value || city.value || state.value || gameCategory.value || difficulty.value || nearbyEnabled.value || selectedVenue.value
}

function toggleNearby() {
  if (!userPostalCode.value) {
    // Can't enable nearby without a postal code
    return
  }
  nearbyEnabled.value = !nearbyEnabled.value
  if (nearbyEnabled.value) {
    // Clear city/state when using nearby
    city.value = ''
    state.value = ''
  }
  applyFilters()
}

const activeFilterChips = () => {
  const chips: string[] = []
  if (searchText.value) chips.push(`"${searchText.value}"`)
  if (selectedVenue.value) {
    chips.push(`Venue: ${selectedVenue.value.name}`)
  } else if (nearbyEnabled.value && userPostalCode.value) {
    chips.push(`Within ${radiusMiles.value} miles`)
  } else {
    if (city.value) chips.push(`City: ${city.value}`)
    if (state.value) chips.push(`State: ${state.value}`)
  }
  if (gameCategory.value) {
    const cat = gameCategoryOptions.find(o => o.value === gameCategory.value)
    if (cat) chips.push(cat.title)
  }
  if (difficulty.value) {
    const diff = difficultyOptions.find(o => o.value === difficulty.value)
    if (diff) chips.push(diff.title)
  }
  return chips
}

// Debounce search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchText, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    applyFilters()
  }, 400)
})

onMounted(async () => {
  // If user is logged in, default to their profile location
  if (auth.isAuthenticated.value) {
    try {
      const token = await auth.getIdToken()
      if (token) {
        const profile = await getMyProfile(token)
        // Store postal code for nearby search
        userPostalCode.value = profile.homePostalCode || null

        // Use active location if set, otherwise fall back to home location
        const defaultCity = profile.activeCity || profile.homeCity
        const defaultState = profile.activeState || profile.homeState
        if (defaultCity) city.value = defaultCity
        if (defaultState) state.value = defaultState

        // If user has a postal code, enable nearby by default
        if (userPostalCode.value) {
          nearbyEnabled.value = true
          city.value = ''
          state.value = ''
        }
      }
    } catch (err) {
      console.error('Failed to load profile for location defaults:', err)
    }
  }
  // Load events with location filter (if set)
  eventStore.loadPublicEvents(buildFilter())
})

function handleSelectEvent(event: EventSummary) {
  router.push(`/games/${event.id}`)
}

function goToCreateGame() {
  router.push('/games/create')
}
</script>

<template>
  <div class="container-wide py-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Find Games</h1>
        <p class="text-gray-500">Discover games near you</p>
      </div>

      <button
        v-if="auth.isAuthenticated.value"
        class="btn-primary"
        @click="goToCreateGame"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Host a Game
      </button>
    </div>

    <!-- Hot Venues Bar -->
    <HotLocationsBar
      :selected-id="selectedVenue?.id"
      @select="selectVenue"
    />

    <!-- Search and Filter Bar -->
    <div class="card p-4 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
        <!-- Search -->
        <div class="md:col-span-4 relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
          </svg>
          <input
            v-model="searchText"
            type="text"
            class="input pl-10"
            placeholder="Search for games..."
          />
        </div>

        <!-- Nearby toggle (only show if user has postal code) -->
        <div v-if="userPostalCode" class="md:col-span-2">
          <button
            class="btn w-full"
            :class="nearbyEnabled ? 'bg-primary-500 text-white hover:bg-primary-600' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            @click="toggleNearby"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4M12,6A6,6 0 0,0 6,12A6,6 0 0,0 12,18A6,6 0 0,0 18,12A6,6 0 0,0 12,6M12,8A4,4 0 0,1 16,12A4,4 0 0,1 12,16A4,4 0 0,1 8,12A4,4 0 0,1 12,8Z"/>
            </svg>
            Nearby
          </button>
        </div>

        <!-- Radius selector (show when nearby is enabled) -->
        <div v-if="nearbyEnabled && userPostalCode" class="md:col-span-2">
          <select
            v-model="radiusMiles"
            class="input"
            @change="applyFilters"
          >
            <option
              v-for="option in radiusOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </div>

        <!-- City (hide when nearby is enabled) -->
        <div v-if="!nearbyEnabled" class="md:col-span-2 relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
          </svg>
          <input
            v-model="city"
            type="text"
            class="input pl-10"
            placeholder="City"
            @blur="applyFilters"
            @keyup.enter="applyFilters"
          />
        </div>

        <!-- State (hide when nearby is enabled) -->
        <div v-if="!nearbyEnabled" class="md:col-span-2">
          <input
            v-model="state"
            type="text"
            class="input"
            placeholder="State"
            @blur="applyFilters"
            @keyup.enter="applyFilters"
          />
        </div>

        <!-- Filter toggle -->
        <div class="md:col-span-2">
          <button
            class="btn w-full"
            :class="showFilters ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            @click="showFilters = !showFilters"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M14,12V19.88C14.04,20.18 13.94,20.5 13.71,20.71C13.32,21.1 12.69,21.1 12.3,20.71L10.29,18.7C10.06,18.47 9.96,18.16 10,17.87V12H9.97L4.21,4.62C3.87,4.19 3.95,3.56 4.38,3.22C4.57,3.08 4.78,3 5,3V3H19V3C19.22,3 19.43,3.08 19.62,3.22C20.05,3.56 20.13,4.19 19.79,4.62L14.03,12H14Z"/>
            </svg>
            Filters
            <span
              v-if="hasActiveFilters()"
              class="ml-2 w-2 h-2 rounded-full bg-primary-500"
            />
          </button>
        </div>
      </div>

      <!-- Expanded Filters -->
      <div v-if="showFilters" class="mt-4 pt-4 border-t border-gray-200">
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
          <div>
            <label class="label">Game Category</label>
            <select
              v-model="gameCategory"
              class="input"
              @change="applyFilters"
            >
              <option
                v-for="option in gameCategoryOptions"
                :key="option.value ?? 'all'"
                :value="option.value"
              >
                {{ option.title }}
              </option>
            </select>
          </div>

          <div>
            <label class="label">Difficulty Level</label>
            <select
              v-model="difficulty"
              class="input"
              @change="applyFilters"
            >
              <option
                v-for="option in difficultyOptions"
                :key="option.value ?? 'all'"
                :value="option.value"
              >
                {{ option.title }}
              </option>
            </select>
          </div>

          <div class="flex items-end">
            <button
              v-if="hasActiveFilters()"
              class="btn-ghost text-secondary-500"
              @click="clearFilters"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
              Clear All Filters
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Results Summary -->
    <div class="flex flex-wrap items-center gap-2 mb-4">
      <span class="text-sm text-gray-500">
        {{ eventStore.publicEvents.value.length }} game{{ eventStore.publicEvents.value.length !== 1 ? 's' : '' }} found
      </span>
      <div class="flex-1" />
      <span
        v-for="(chipLabel, idx) in activeFilterChips()"
        :key="idx"
        class="chip-primary flex items-center gap-1"
      >
        {{ chipLabel }}
        <button @click="clearFilters" class="ml-1 hover:text-primary-900">
          <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </button>
      </span>
    </div>

    <!-- Error Alert -->
    <div v-if="eventStore.error.value" class="alert-error mb-6">
      <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <span class="flex-1">{{ eventStore.error.value }}</span>
    </div>

    <!-- Event List -->
    <EventList
      :events="eventStore.publicEvents.value"
      :loading="eventStore.loading.value"
      empty-text="No games found. Try adjusting your filters or be the first to host one!"
      @select="handleSelectEvent"
    />

    <!-- Ad Banner for free tier users -->
    <AdBanner placement="events" />
  </div>
</template>
