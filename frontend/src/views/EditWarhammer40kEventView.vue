<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getWarhammer40kEvent, updateWarhammer40kEvent } from '@/services/warhammer40kEventApi'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import Warhammer40kGameSetupSection from '@/components/warhammer40k/Warhammer40kGameSetupSection.vue'
import Warhammer40kMissionSection from '@/components/warhammer40k/Warhammer40kMissionSection.vue'
import Warhammer40kArmyRulesSection from '@/components/warhammer40k/Warhammer40kArmyRulesSection.vue'
import Warhammer40kTerrainSection from '@/components/warhammer40k/Warhammer40kTerrainSection.vue'
import Warhammer40kPrizesSection from '@/components/warhammer40k/Warhammer40kPrizesSection.vue'
import type { CreateWarhammer40kEventInput, Warhammer40kGameType, Warhammer40kEventType, Warhammer40kPlayerMode, Warhammer40kScoringType, Warhammer40kMissionSelection, Warhammer40kSecondaryObjectives } from '@/types/warhammer40k'
import { DEFAULT_WARHAMMER40K_CONFIG, DEFAULT_MAX_PLAYERS, isTournamentEventType, getTableSizeForPoints, ROUNDS_PRESETS, ROUND_TIME_PRESETS, SCORING_TYPE_LABELS } from '@/types/warhammer40k'
import type { EventLocation } from '@/types/social'
import { getEffectiveTier } from '@/types/user'
import type { SubscriptionTier } from '@/config/subscriptionLimits'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Get event ID from route
const eventId = computed(() => route.params.id as string)

// Get user tier for location features
const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

const loading = ref(false)
const loadingEvent = ref(true)
const showVenueModal = ref(false)
const errorMessage = ref('')

// Form state
const form = reactive<CreateWarhammer40kEventInput>({
  title: '',
  description: '',
  eventDate: '',
  startTime: '10:00',
  timezone: 'America/Phoenix',
  durationMinutes: 180,
  setupMinutes: 30,
  maxPlayers: 8,
  hostIsPlaying: false,
  isPublic: true,
  isCharityEvent: false,
  groupId: undefined,
  // Location
  eventLocationId: undefined,
  addressLine1: '',
  city: '',
  state: '',
  postalCode: '',
  locationDetails: '',
  venueHall: undefined,
  venueRoom: undefined,
  venueTable: undefined,
  // Warhammer 40k config
  warhammer40kConfig: { ...DEFAULT_WARHAMMER40K_CONFIG },
})

// Track if form has been submitted (for showing validation errors)
const hasAttemptedSubmit = ref(false)

// Load existing event data
onMounted(async () => {
  try {
    const token = await authStore.getIdToken()
    if (!token) {
      errorMessage.value = 'Not authenticated'
      return
    }

    const event = await getWarhammer40kEvent(token, eventId.value)

    // Populate form with existing data
    form.title = event.title
    form.description = event.description || ''
    form.eventDate = event.eventDate
    form.startTime = event.startTime || '10:00'
    form.timezone = event.timezone || 'America/Phoenix'
    form.durationMinutes = event.durationMinutes || 360
    form.setupMinutes = event.setupMinutes || 30
    form.maxPlayers = event.maxPlayers || 8
    form.hostIsPlaying = event.hostIsPlaying || false
    form.isPublic = event.isPublic
    form.isCharityEvent = event.isCharityEvent || false
    form.groupId = event.groupId || undefined

    // Location
    form.eventLocationId = event.eventLocationId || undefined
    form.addressLine1 = event.addressLine1 || ''
    form.city = event.city || ''
    form.state = event.state || ''
    form.postalCode = event.postalCode || ''
    form.locationDetails = event.locationDetails || ''
    form.venueHall = event.venueHall || undefined
    form.venueRoom = event.venueRoom || undefined
    form.venueTable = event.venueTable || undefined

    // Warhammer 40k config
    if (event.warhammer40kConfig) {
      form.warhammer40kConfig.gameType = event.warhammer40kConfig.gameType || DEFAULT_WARHAMMER40K_CONFIG.gameType
      form.warhammer40kConfig.pointsLimit = event.warhammer40kConfig.pointsLimit ?? DEFAULT_WARHAMMER40K_CONFIG.pointsLimit
      form.warhammer40kConfig.playerMode = event.warhammer40kConfig.playerMode || DEFAULT_WARHAMMER40K_CONFIG.playerMode
      form.warhammer40kConfig.missionPack = event.warhammer40kConfig.missionPack || null
      form.warhammer40kConfig.missionNotes = event.warhammer40kConfig.missionNotes || null
      form.warhammer40kConfig.missionSelection = event.warhammer40kConfig.missionSelection || null
      form.warhammer40kConfig.preSelectedMissions = event.warhammer40kConfig.preSelectedMissions || null
      form.warhammer40kConfig.secondaryObjectives = event.warhammer40kConfig.secondaryObjectives || null
      form.warhammer40kConfig.battleReadyRequired = event.warhammer40kConfig.battleReadyRequired ?? false
      form.warhammer40kConfig.wysiwygRequired = event.warhammer40kConfig.wysiwygRequired ?? false
      form.warhammer40kConfig.forgeWorldAllowed = event.warhammer40kConfig.forgeWorldAllowed ?? true
      form.warhammer40kConfig.legendsAllowed = event.warhammer40kConfig.legendsAllowed ?? false
      form.warhammer40kConfig.armyRulesNotes = event.warhammer40kConfig.armyRulesNotes || null
      form.warhammer40kConfig.requireArmyList = event.warhammer40kConfig.requireArmyList ?? false
      form.warhammer40kConfig.armyListDeadline = event.warhammer40kConfig.armyListDeadline || null
      form.warhammer40kConfig.armyListNotes = event.warhammer40kConfig.armyListNotes || null
      form.warhammer40kConfig.allowProxies = event.warhammer40kConfig.allowProxies ?? false
      form.warhammer40kConfig.proxyNotes = event.warhammer40kConfig.proxyNotes || null
      form.warhammer40kConfig.terrainType = event.warhammer40kConfig.terrainType || DEFAULT_WARHAMMER40K_CONFIG.terrainType
      form.warhammer40kConfig.tableSize = event.warhammer40kConfig.tableSize || DEFAULT_WARHAMMER40K_CONFIG.tableSize
      form.warhammer40kConfig.timeLimitMinutes = event.warhammer40kConfig.timeLimitMinutes || null
      form.warhammer40kConfig.eventType = event.warhammer40kConfig.eventType || DEFAULT_WARHAMMER40K_CONFIG.eventType
      form.warhammer40kConfig.tournamentStyle = event.warhammer40kConfig.tournamentStyle || null
      form.warhammer40kConfig.roundsCount = event.warhammer40kConfig.roundsCount || null
      form.warhammer40kConfig.roundTimeMinutes = event.warhammer40kConfig.roundTimeMinutes || null
      form.warhammer40kConfig.includeTopCut = event.warhammer40kConfig.includeTopCut ?? false
      form.warhammer40kConfig.scoringType = event.warhammer40kConfig.scoringType || null
      form.warhammer40kConfig.startingSupplyLimit = event.warhammer40kConfig.startingSupplyLimit || null
      form.warhammer40kConfig.startingCrusadePoints = event.warhammer40kConfig.startingCrusadePoints || null
      form.warhammer40kConfig.crusadeProgressionNotes = event.warhammer40kConfig.crusadeProgressionNotes || null
      form.warhammer40kConfig.entryFee = event.warhammer40kConfig.entryFee || null
      form.warhammer40kConfig.entryFeeCurrency = event.warhammer40kConfig.entryFeeCurrency || 'USD'
      form.warhammer40kConfig.hasPrizes = event.warhammer40kConfig.hasPrizes || false
      form.warhammer40kConfig.prizeStructure = event.warhammer40kConfig.prizeStructure || null
      form.warhammer40kConfig.allowSpectators = event.warhammer40kConfig.allowSpectators ?? true
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load event'
  } finally {
    loadingEvent.value = false
  }
})

// Location validation helper
const hasValidLocation = computed(() => {
  return form.eventLocationId || (form.city?.trim() && form.state?.trim())
})

// Validation
const validationErrors = computed(() => {
  const errors: string[] = []
  if (!form.title.trim()) errors.push('Event title is required')
  if (!form.eventDate) errors.push('Event date is required')
  if (!hasValidLocation.value) errors.push('Location is required (select a venue or enter city/state)')
  if (!form.warhammer40kConfig.gameType) errors.push('Game type is required')
  return errors
})

const isValid = computed(() => validationErrors.value.length === 0)

// Section-specific error tracking for visual highlighting
const sectionErrors = computed(() => ({
  title: hasAttemptedSubmit.value && !form.title.trim(),
  date: hasAttemptedSubmit.value && !form.eventDate,
  location: hasAttemptedSubmit.value && !hasValidLocation.value,
  gameSetup: hasAttemptedSubmit.value && !form.warhammer40kConfig.gameType,
}))

// Scroll to first invalid field
function scrollToFirstError() {
  const selectors = [
    { error: 'Event title is required', selector: 'input[placeholder*="Saturday Warhammer"]' },
    { error: 'Event date is required', selector: 'input[type="date"]' },
    { error: 'Location is required', selector: '#location-section' },
    { error: 'Game type is required', selector: '#game-setup-section' },
  ]

  for (const { error, selector } of selectors) {
    if (validationErrors.value.some(e => e.includes(error))) {
      const element = document.querySelector(selector) as HTMLElement
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' })
        setTimeout(() => element.focus?.(), 500)
        break
      }
    }
  }
}

// Handle game type change with smart defaults
function handleGameTypeChange(gameType: Warhammer40kGameType) {
  form.warhammer40kConfig.gameType = gameType

  // Set smart defaults based on game type
  if (gameType === 'matched') {
    form.warhammer40kConfig.pointsLimit = 2000
  } else {
    form.warhammer40kConfig.pointsLimit = 2000
  }
  // Auto-select table size based on points
  form.warhammer40kConfig.tableSize = getTableSizeForPoints(form.warhammer40kConfig.pointsLimit)
}

// Handle points limit change with auto table size
function handlePointsLimitChange(points: number) {
  form.warhammer40kConfig.pointsLimit = points
  form.warhammer40kConfig.tableSize = getTableSizeForPoints(points)
}

// Handle event type change
function handleEventTypeChange(eventType: Warhammer40kEventType) {
  form.warhammer40kConfig.eventType = eventType
  form.maxPlayers = DEFAULT_MAX_PLAYERS[eventType] || 8

  if (!isTournamentEventType(eventType)) {
    form.warhammer40kConfig.tournamentStyle = null
    form.warhammer40kConfig.roundsCount = null
    form.warhammer40kConfig.timeLimitMinutes = null
    form.warhammer40kConfig.roundTimeMinutes = null
    form.warhammer40kConfig.includeTopCut = false
    form.warhammer40kConfig.scoringType = null
  }

  // Casual: reset army submission and scoring
  if (eventType === 'casual') {
    form.warhammer40kConfig.requireArmyList = false
    form.warhammer40kConfig.armyListDeadline = null
    form.warhammer40kConfig.armyListNotes = null
    form.warhammer40kConfig.scoringType = null
  }
}

// Handle location selection
function handleLocationSelect(location: EventLocation | null) {
  if (location) {
    form.eventLocationId = location.id
    form.addressLine1 = ''
    form.city = location.city
    form.state = location.state
    form.postalCode = location.postalCode || ''
  } else {
    form.eventLocationId = undefined
  }
}

// Submit form
async function handleSubmit() {
  hasAttemptedSubmit.value = true

  if (!isValid.value) {
    scrollToFirstError()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const token = await authStore.getIdToken()
    if (!token) {
      throw new Error('Not authenticated')
    }

    await updateWarhammer40kEvent(token, eventId.value, form)
    router.push(`/games/${eventId.value}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update event'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-3xl mx-auto px-4">
      <!-- Back link -->
      <button
        @click="router.push(`/games/${eventId}`)"
        class="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
      >
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
        </svg>
        Back to Event
      </button>

      <!-- Loading state -->
      <div v-if="loadingEvent" class="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600 mx-auto mb-4"></div>
        <p class="text-gray-500">Loading event...</p>
      </div>

      <!-- Form card -->
      <div v-else class="bg-white rounded-xl shadow-sm border border-gray-200">
        <!-- Header -->
        <div class="p-6 border-b border-gray-100">
          <h1 class="text-xl font-bold flex items-center gap-2 text-red-700">
            <img src="/icons/warhammer40k-logo.png" alt="Warhammer 40k" class="h-16 object-contain" />
            Edit Warhammer 40k Event
          </h1>
          <p class="text-gray-500 text-sm mt-1">Update your Warhammer 40,000 event settings</p>
        </div>

        <form @submit.prevent="handleSubmit" class="p-6 space-y-8">
          <!-- Error message -->
          <div v-if="errorMessage" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
            {{ errorMessage }}
          </div>

          <!-- Basic Information -->
          <section>
            <h3 class="text-lg font-semibold mb-4">Basic Information</h3>
            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">
                  Event Title <span class="text-red-500">*</span>
                </label>
                <input
                  v-model="form.title"
                  type="text"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  placeholder="e.g., Saturday Warhammer 40k Battle"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  v-model="form.description"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  placeholder="Tell people about your event..."
                />
              </div>
            </div>
          </section>

          <!-- Date & Time -->
          <EventDateTimeSection
            v-model:date="form.eventDate"
            v-model:time="form.startTime"
            v-model:timezone="form.timezone"
            v-model:duration="form.durationMinutes"
            v-model:setup="form.setupMinutes"
          />

          <!-- Location -->
          <section
            id="location-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.location }"
          >
            <EventLocationSection
              v-model:event-location-id="form.eventLocationId"
              v-model:address-line1="form.addressLine1"
              v-model:city="form.city"
              v-model:state="form.state"
              v-model:postal-code="form.postalCode"
              v-model:location-details="form.locationDetails"
              v-model:venue-hall="form.venueHall"
              v-model:venue-room="form.venueRoom"
              v-model:venue-table="form.venueTable"
              :current-tier="currentTier"
              @location-select="handleLocationSelect"
              @show-venue-modal="showVenueModal = true"
            />
          </section>

          <!-- Game Setup -->
          <section
            id="game-setup-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.gameSetup }"
          >
            <Warhammer40kGameSetupSection
              :game-type="form.warhammer40kConfig.gameType"
              :points-limit="form.warhammer40kConfig.pointsLimit"
              :player-mode="form.warhammer40kConfig.playerMode"
              :has-error="sectionErrors.gameSetup"
              @update:game-type="handleGameTypeChange"
              @update:points-limit="handlePointsLimitChange"
              @update:player-mode="(v: Warhammer40kPlayerMode) => form.warhammer40kConfig.playerMode = v"
            />
          </section>

          <!-- Mission -->
          <Warhammer40kMissionSection
            :mission-pack="form.warhammer40kConfig.missionPack"
            :mission-notes="form.warhammer40kConfig.missionNotes"
            :mission-selection="form.warhammer40kConfig.missionSelection"
            :pre-selected-missions="form.warhammer40kConfig.preSelectedMissions"
            :secondary-objectives="form.warhammer40kConfig.secondaryObjectives"
            @update:mission-pack="(v: string | null) => form.warhammer40kConfig.missionPack = v"
            @update:mission-notes="(v: string | null) => form.warhammer40kConfig.missionNotes = v"
            @update:mission-selection="(v: Warhammer40kMissionSelection | null) => form.warhammer40kConfig.missionSelection = v"
            @update:pre-selected-missions="(v: string[] | null) => form.warhammer40kConfig.preSelectedMissions = v"
            @update:secondary-objectives="(v: Warhammer40kSecondaryObjectives | null) => form.warhammer40kConfig.secondaryObjectives = v"
          />

          <!-- Army Rules -->
          <Warhammer40kArmyRulesSection
            :battle-ready-required="form.warhammer40kConfig.battleReadyRequired"
            :wysiwyg-required="form.warhammer40kConfig.wysiwygRequired"
            :forge-world-allowed="form.warhammer40kConfig.forgeWorldAllowed"
            :legends-allowed="form.warhammer40kConfig.legendsAllowed"
            :army-rules-notes="form.warhammer40kConfig.armyRulesNotes"
            :allow-proxies="form.warhammer40kConfig.allowProxies"
            :proxy-notes="form.warhammer40kConfig.proxyNotes"
            @update:battle-ready-required="(v: boolean) => form.warhammer40kConfig.battleReadyRequired = v"
            @update:wysiwyg-required="(v: boolean) => form.warhammer40kConfig.wysiwygRequired = v"
            @update:forge-world-allowed="(v: boolean) => form.warhammer40kConfig.forgeWorldAllowed = v"
            @update:legends-allowed="(v: boolean) => form.warhammer40kConfig.legendsAllowed = v"
            @update:army-rules-notes="(v: string | null) => form.warhammer40kConfig.armyRulesNotes = v"
            @update:allow-proxies="(v: boolean) => form.warhammer40kConfig.allowProxies = v"
            @update:proxy-notes="(v: string | null) => form.warhammer40kConfig.proxyNotes = v"
          />

          <!-- Terrain & Table -->
          <Warhammer40kTerrainSection
            :terrain-type="form.warhammer40kConfig.terrainType"
            :table-size="form.warhammer40kConfig.tableSize"
            @update:terrain-type="(v: string) => form.warhammer40kConfig.terrainType = v as any"
            @update:table-size="(v: string) => form.warhammer40kConfig.tableSize = v"
          />

          <!-- Event Structure -->
          <section>
            <h3 class="text-lg font-semibold mb-4">Event Structure</h3>
            <div class="space-y-4">
              <!-- Event Type Buttons -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Event Type</label>
                <div class="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  <button
                    v-for="type in (['casual', 'tournament', 'campaign', 'league'] as Warhammer40kEventType[])"
                    :key="type"
                    type="button"
                    @click="handleEventTypeChange(type)"
                    class="px-3 py-2 text-sm font-medium rounded-lg border transition-colors capitalize"
                    :class="form.warhammer40kConfig.eventType === type
                      ? 'bg-red-600 text-white border-red-600'
                      : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'"
                  >
                    {{ type }}
                  </button>
                </div>
              </div>

              <!-- Tournament Settings (shown for tournament and league events) -->
              <div v-if="isTournamentEventType(form.warhammer40kConfig.eventType)" class="space-y-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h4 class="text-sm font-semibold text-gray-700">Tournament Settings</h4>

                <!-- League note -->
                <p v-if="form.warhammer40kConfig.eventType === 'league'" class="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded px-3 py-2">
                  League events use tournament settings applied across recurring sessions.
                </p>

                <!-- Pairing System -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Pairing System</label>
                  <div class="px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-700">
                    Swiss
                  </div>
                  <p class="text-xs text-gray-500 mt-1">Swiss pairing is the standard system for Warhammer 40k tournaments.</p>
                </div>

                <!-- Rounds -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">Number of Rounds</label>
                  <div class="flex flex-wrap gap-2 mb-2">
                    <button
                      v-for="preset in ROUNDS_PRESETS"
                      :key="preset"
                      type="button"
                      class="px-4 py-2 text-sm font-medium rounded-lg border transition-colors"
                      :class="form.warhammer40kConfig.roundsCount === preset
                        ? 'bg-red-600 text-white border-red-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'"
                      @click="form.warhammer40kConfig.roundsCount = preset"
                    >
                      {{ preset }}
                    </button>
                  </div>
                  <div class="flex items-center gap-3">
                    <label class="text-sm text-gray-600">Custom:</label>
                    <input
                      type="number"
                      :value="form.warhammer40kConfig.roundsCount && !ROUNDS_PRESETS.includes(form.warhammer40kConfig.roundsCount) ? form.warhammer40kConfig.roundsCount : ''"
                      @input="(e) => form.warhammer40kConfig.roundsCount = Number((e.target as HTMLInputElement).value) || null"
                      min="1"
                      max="10"
                      class="w-24 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                      placeholder="e.g., 6"
                    />
                  </div>
                </div>

                <!-- Round Time -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">Round Time</label>
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="preset in ROUND_TIME_PRESETS"
                      :key="preset.minutes"
                      type="button"
                      class="px-4 py-2 text-sm font-medium rounded-lg border transition-colors"
                      :class="form.warhammer40kConfig.roundTimeMinutes === preset.minutes
                        ? 'bg-red-600 text-white border-red-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'"
                      @click="form.warhammer40kConfig.roundTimeMinutes = preset.minutes"
                    >
                      {{ preset.label }}
                    </button>
                  </div>
                </div>

                <!-- Include Top Cut -->
                <div class="flex items-center gap-2">
                  <input
                    id="includeTopCut"
                    v-model="form.warhammer40kConfig.includeTopCut"
                    type="checkbox"
                    class="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                  />
                  <label for="includeTopCut" class="text-sm font-medium text-gray-700">
                    Include Top Cut
                  </label>
                </div>

                <!-- Scoring Type -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">Scoring Type</label>
                  <div class="flex flex-wrap gap-2">
                    <button
                      v-for="(label, key) in SCORING_TYPE_LABELS"
                      :key="key"
                      type="button"
                      class="px-4 py-2 text-sm font-medium rounded-lg border transition-colors"
                      :class="form.warhammer40kConfig.scoringType === key
                        ? 'bg-red-600 text-white border-red-600'
                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'"
                      @click="form.warhammer40kConfig.scoringType = key as Warhammer40kScoringType"
                    >
                      {{ label }}
                    </button>
                  </div>
                </div>
              </div>

              <!-- Crusade Enhancements (shown for campaign events with crusade game type) -->
              <div v-if="form.warhammer40kConfig.eventType === 'campaign' && form.warhammer40kConfig.gameType === 'crusade'" class="space-y-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h4 class="text-sm font-semibold text-gray-700">Crusade Settings</h4>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Starting Supply Limit</label>
                    <input
                      type="number"
                      :value="form.warhammer40kConfig.startingSupplyLimit ?? 1000"
                      @input="(e) => form.warhammer40kConfig.startingSupplyLimit = Number((e.target as HTMLInputElement).value) || null"
                      min="0"
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                      placeholder="1000"
                    />
                  </div>

                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Starting Crusade Points</label>
                    <input
                      type="number"
                      :value="form.warhammer40kConfig.startingCrusadePoints ?? 5"
                      @input="(e) => form.warhammer40kConfig.startingCrusadePoints = Number((e.target as HTMLInputElement).value) || null"
                      min="0"
                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                      placeholder="5"
                    />
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Progression Notes</label>
                  <textarea
                    :value="form.warhammer40kConfig.crusadeProgressionNotes ?? ''"
                    @input="(e) => form.warhammer40kConfig.crusadeProgressionNotes = (e.target as HTMLTextAreaElement).value || null"
                    rows="3"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                    placeholder="Describe progression rules, battle honour limits, etc."
                  />
                </div>
              </div>
            </div>
          </section>

          <!-- Army Submission (hidden for casual events) -->
          <section v-if="form.warhammer40kConfig.eventType !== 'casual'">
            <h3 class="text-lg font-semibold mb-4">Army List Submission</h3>
            <div class="space-y-4">
              <div class="flex items-center gap-2">
                <input
                  id="requireArmyList"
                  v-model="form.warhammer40kConfig.requireArmyList"
                  type="checkbox"
                  class="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                />
                <label for="requireArmyList" class="text-sm font-medium text-gray-700">
                  Require Army List Submission
                </label>
              </div>

              <div v-if="form.warhammer40kConfig.requireArmyList" class="space-y-4 ml-6">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Submission Deadline</label>
                  <input
                    type="date"
                    :value="form.warhammer40kConfig.armyListDeadline ?? ''"
                    @input="(e) => form.warhammer40kConfig.armyListDeadline = (e.target as HTMLInputElement).value || null"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  />
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Submission Notes</label>
                  <textarea
                    :value="form.warhammer40kConfig.armyListNotes ?? ''"
                    @input="(e) => form.warhammer40kConfig.armyListNotes = (e.target as HTMLTextAreaElement).value || null"
                    rows="2"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                    placeholder="e.g., Submit via BattleScribe or New Recruit format"
                  />
                </div>
              </div>
            </div>
          </section>

          <!-- Entry & Prizes -->
          <Warhammer40kPrizesSection
            :event-type="form.warhammer40kConfig.eventType"
            :entry-fee="form.warhammer40kConfig.entryFee"
            :entry-fee-currency="form.warhammer40kConfig.entryFeeCurrency"
            :has-prizes="form.warhammer40kConfig.hasPrizes"
            :prize-structure="form.warhammer40kConfig.prizeStructure"
            @update:entry-fee="(v) => form.warhammer40kConfig.entryFee = v"
            @update:entry-fee-currency="(v) => form.warhammer40kConfig.entryFeeCurrency = v"
            @update:has-prizes="(v) => form.warhammer40kConfig.hasPrizes = v"
            @update:prize-structure="(v) => form.warhammer40kConfig.prizeStructure = v"
          />

          <!-- Player Settings -->
          <EventPlayerSettingsSection
            v-model:max-players="form.maxPlayers"
            v-model:host-is-playing="form.hostIsPlaying"
            v-model:is-public="form.isPublic"
            v-model:is-charity-event="form.isCharityEvent"
          >
            <template #extra-settings>
              <div class="flex items-center gap-2">
                <input
                  id="allowSpectators"
                  v-model="form.warhammer40kConfig.allowSpectators"
                  type="checkbox"
                  class="w-4 h-4 text-red-600 border-gray-300 rounded focus:ring-red-500"
                />
                <label for="allowSpectators" class="text-sm">
                  <span class="font-medium">Allow Spectators</span>
                  <span class="text-gray-500 ml-1">Let non-players watch</span>
                </label>
              </div>
            </template>
          </EventPlayerSettingsSection>

          <!-- Validation Feedback -->
          <div v-if="validationErrors.length > 0 && !isValid" class="bg-amber-50 border border-amber-200 rounded-lg p-4">
            <h4 class="text-sm font-medium text-amber-800 mb-2 flex items-center gap-2">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
              </svg>
              Please complete the following:
            </h4>
            <ul class="text-sm text-amber-700 space-y-1 ml-6">
              <li v-for="error in validationErrors" :key="error" class="list-disc">
                {{ error }}
              </li>
            </ul>
          </div>

          <!-- Actions -->
          <div class="flex justify-end gap-3 pt-4 border-t border-gray-100">
            <button
              type="button"
              @click="router.push(`/games/${eventId}`)"
              class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="!isValid || loading"
              class="px-6 py-2 bg-red-600 text-white font-medium rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :title="!isValid ? validationErrors.join(', ') : ''"
            >
              {{ loading ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

    <SubmitVenueModal
      :visible="showVenueModal"
      @close="showVenueModal = false"
      @submitted="showVenueModal = false"
    />
</template>
