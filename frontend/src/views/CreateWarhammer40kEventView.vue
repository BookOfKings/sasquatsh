<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { createWarhammer40kEvent } from '@/services/warhammer40kEventApi'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import Warhammer40kGameSetupSection from '@/components/warhammer40k/Warhammer40kGameSetupSection.vue'
import Warhammer40kMissionSection from '@/components/warhammer40k/Warhammer40kMissionSection.vue'
import Warhammer40kArmyRulesSection from '@/components/warhammer40k/Warhammer40kArmyRulesSection.vue'
import Warhammer40kTerrainSection from '@/components/warhammer40k/Warhammer40kTerrainSection.vue'
import Warhammer40kPrizesSection from '@/components/warhammer40k/Warhammer40kPrizesSection.vue'
import type { CreateWarhammer40kEventInput, Warhammer40kGameType, Warhammer40kEventType, Warhammer40kPlayerMode } from '@/types/warhammer40k'
import { DEFAULT_WARHAMMER40K_CONFIG, DEFAULT_MAX_PLAYERS, isTournamentEventType } from '@/types/warhammer40k'
import type { EventLocation } from '@/types/social'
import { getEffectiveTier } from '@/types/user'
import type { SubscriptionTier } from '@/config/subscriptionLimits'

const router = useRouter()
const authStore = useAuthStore()

// Get user tier for location features
const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

const loading = ref(false)
const errorMessage = ref('')

// Form state
const form = reactive<CreateWarhammer40kEventInput>({
  title: '',
  description: '',
  eventDate: '',
  startTime: '10:00',
  timezone: 'America/Phoenix',
  durationMinutes: 360,
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
    form.warhammer40kConfig.tableSize = '44x60'
  } else {
    form.warhammer40kConfig.pointsLimit = 2000
    form.warhammer40kConfig.tableSize = '44x60'
  }
}

// Handle event type change
function handleEventTypeChange(eventType: Warhammer40kEventType) {
  form.warhammer40kConfig.eventType = eventType
  form.maxPlayers = DEFAULT_MAX_PLAYERS[eventType] || 8

  if (!isTournamentEventType(eventType)) {
    form.warhammer40kConfig.tournamentStyle = null
    form.warhammer40kConfig.roundsCount = null
    form.warhammer40kConfig.timeLimitMinutes = null
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

    const event = await createWarhammer40kEvent(token, form)
    router.push(`/games/${event.id}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create event'
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
        @click="router.push('/games')"
        class="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-4"
      >
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
        </svg>
        Back to Games
      </button>

      <!-- Form card -->
      <div class="bg-white rounded-xl shadow-sm border border-gray-200">
        <!-- Header -->
        <div class="p-6 border-b border-gray-100">
          <h1 class="text-xl font-bold flex items-center gap-2 text-red-700">
            <img src="/icons/warhammer40k-logo.png" alt="Warhammer 40k" class="h-6 object-contain" />
            Host Warhammer 40k Event
          </h1>
          <p class="text-gray-500 text-sm mt-1">Create a Warhammer 40,000 event with army and mission settings</p>
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
              @update:points-limit="(v: number) => form.warhammer40kConfig.pointsLimit = v"
              @update:player-mode="(v: Warhammer40kPlayerMode) => form.warhammer40kConfig.playerMode = v"
            />
          </section>

          <!-- Mission -->
          <Warhammer40kMissionSection
            :mission-pack="form.warhammer40kConfig.missionPack"
            :mission-notes="form.warhammer40kConfig.missionNotes"
            @update:mission-pack="(v: string | null) => form.warhammer40kConfig.missionPack = v"
            @update:mission-notes="(v: string | null) => form.warhammer40kConfig.missionNotes = v"
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

              <!-- Tournament Settings (shown only for tournament events) -->
              <div v-if="isTournamentEventType(form.warhammer40kConfig.eventType)" class="space-y-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
                <h4 class="text-sm font-semibold text-gray-700">Tournament Settings</h4>

                <!-- Tournament Style -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Tournament Style</label>
                  <select
                    :value="form.warhammer40kConfig.tournamentStyle"
                    @change="(e) => form.warhammer40kConfig.tournamentStyle = ((e.target as HTMLSelectElement).value || null) as any"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                  >
                    <option value="">Select style...</option>
                    <option value="swiss">Swiss Pairing</option>
                    <option value="round_robin">Round Robin</option>
                    <option value="single_elimination">Single Elimination</option>
                    <option value="double_elimination">Double Elimination</option>
                  </select>
                </div>

                <!-- Rounds -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Number of Rounds</label>
                  <input
                    type="number"
                    :value="form.warhammer40kConfig.roundsCount"
                    @input="(e) => form.warhammer40kConfig.roundsCount = Number((e.target as HTMLInputElement).value) || null"
                    min="1"
                    max="10"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                    placeholder="e.g., 3"
                  />
                </div>

                <!-- Round Time Limit -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">Round Time Limit (minutes)</label>
                  <input
                    type="number"
                    :value="form.warhammer40kConfig.timeLimitMinutes"
                    @input="(e) => form.warhammer40kConfig.timeLimitMinutes = Number((e.target as HTMLInputElement).value) || null"
                    min="30"
                    max="300"
                    class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500"
                    placeholder="e.g., 150"
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
              @click="router.push('/games')"
              class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="loading"
              class="px-6 py-2 bg-red-600 text-white font-medium rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ loading ? 'Creating...' : 'Create Warhammer 40k Event' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
