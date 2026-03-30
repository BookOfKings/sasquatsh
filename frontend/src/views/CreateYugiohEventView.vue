<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { createYugiohEvent } from '@/services/yugiohEventApi'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import YugiohFormatSelector from '@/components/yugioh/YugiohFormatSelector.vue'
import YugiohEventStructureSection from '@/components/yugioh/YugiohEventStructureSection.vue'
import YugiohDeckRulesSection from '@/components/yugioh/YugiohDeckRulesSection.vue'
import YugiohWhatToBring from '@/components/yugioh/YugiohWhatToBring.vue'
import YugiohPrizesSection from '@/components/yugioh/YugiohPrizesSection.vue'
import type { CreateYugiohEventInput, YugiohEventType, YugiohTournamentStyle, YugiohFormat } from '@/types/yugioh'
import { YUGIOH_FORMATS, DEFAULT_MAX_PLAYERS, OFFICIAL_EVENT_TYPES } from '@/types/yugioh'
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

// Selected format object (for passing to components)
const selectedFormat = ref<YugiohFormat | null>(null)

// Form state
const form = reactive<CreateYugiohEventInput>({
  title: '',
  description: '',
  eventDate: '',
  startTime: '14:00',
  timezone: 'America/Phoenix',
  durationMinutes: 240,
  setupMinutes: 15,
  maxPlayers: 16,
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
  // Yu-Gi-Oh! config
  yugiohConfig: {
    formatId: 'advanced',
    customFormatName: null,
    eventType: null as unknown as YugiohEventType, // Require explicit selection
    tournamentStyle: null,
    roundsCount: null,
    roundTimeMinutes: 40,
    bestOf: 3,
    topCut: null,
    allowProxies: false,
    proxyLimit: null,
    requireDeckRegistration: false,
    deckSubmissionDeadline: null,
    allowSideDeck: true,
    enforceFormatLegality: true,
    houseRulesNotes: null,
    hasPrizes: false,
    prizeStructure: null,
    entryFee: null,
    entryFeeCurrency: 'USD',
    isOfficialEvent: false,
    awardsOtsPoints: false,
    allowSpectators: true,
  },
})

// Track if form has been submitted (for showing validation errors)
const hasAttemptedSubmit = ref(false)

// Location validation helper
const hasValidLocation = computed(() => {
  // Either a venue is selected OR manual address with city+state
  return form.eventLocationId || (form.city?.trim() && form.state?.trim())
})

// Validation
const validationErrors = computed(() => {
  const errors: string[] = []
  if (!form.title.trim()) errors.push('Event title is required')
  if (!form.eventDate) errors.push('Event date is required')
  if (!hasValidLocation.value) errors.push('Location is required (select a venue or enter city/state)')
  if (!form.yugiohConfig.formatId) errors.push('Format selection is required')
  if (!form.yugiohConfig.eventType) errors.push('Event type is required')
  return errors
})

const isValid = computed(() => validationErrors.value.length === 0)

// Section-specific error tracking for visual highlighting
const sectionErrors = computed(() => ({
  title: hasAttemptedSubmit.value && !form.title.trim(),
  date: hasAttemptedSubmit.value && !form.eventDate,
  location: hasAttemptedSubmit.value && !hasValidLocation.value,
  format: hasAttemptedSubmit.value && !form.yugiohConfig.formatId,
  eventType: hasAttemptedSubmit.value && !form.yugiohConfig.eventType,
}))

// Max player guidance based on event type
const maxPlayerGuidance = computed(() => {
  const type = form.yugiohConfig.eventType
  switch (type) {
    case 'casual':
      return 'Recommended: 4-8 players'
    case 'locals':
    case 'ots':
      return 'Recommended: 8-32 players'
    case 'regional':
      return 'Large event - typically 256+ players'
    case 'ycs':
      return 'Premier event - typically 1000+ players'
    case 'nationals':
    case 'worlds':
      return 'Major event - no typical limit'
    default:
      return 'Recommended: 8-32 players'
  }
})

// Scroll to first invalid field
function scrollToFirstError() {
  const selectors = [
    { error: 'Event title is required', selector: 'input[placeholder*="Saturday Yu-Gi-Oh! Locals"]' },
    { error: 'Event date is required', selector: 'input[type="date"]' },
    { error: 'Location is required', selector: '#location-section' },
    { error: 'Format selection is required', selector: '#format-section' },
    { error: 'Event type is required', selector: '#event-structure-section' },
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

// Watch format changes to update selectedFormat object
watch(() => form.yugiohConfig.formatId, (newFormatId) => {
  selectedFormat.value = newFormatId
    ? YUGIOH_FORMATS.find(f => f.id === newFormatId) || null
    : null
})

// Initialize selected format
selectedFormat.value = YUGIOH_FORMATS.find(f => f.id === 'advanced') || null

// Handle format selection
function handleFormatChange(formatId: string | null) {
  form.yugiohConfig.formatId = formatId

  // Format-driven defaults
  if (formatId === 'advanced' || formatId === 'traditional') {
    form.yugiohConfig.enforceFormatLegality = true
  } else if (formatId === 'speed_duel') {
    // Speed Duel has different defaults
    form.yugiohConfig.enforceFormatLegality = true
  } else if (formatId === 'casual' || formatId === 'time_wizard') {
    form.yugiohConfig.enforceFormatLegality = false
  }
}

// Handle event type changes with format-aware defaults
function handleEventTypeChange(eventType: YugiohEventType) {
  form.yugiohConfig.eventType = eventType
  form.maxPlayers = DEFAULT_MAX_PLAYERS[eventType] || 16

  // Set official event defaults
  if (OFFICIAL_EVENT_TYPES.includes(eventType)) {
    form.yugiohConfig.isOfficialEvent = true
    form.yugiohConfig.requireDeckRegistration = true
    form.yugiohConfig.enforceFormatLegality = true
    form.yugiohConfig.allowProxies = false
  } else {
    form.yugiohConfig.isOfficialEvent = false
    form.yugiohConfig.awardsOtsPoints = false
  }
}

function handleTournamentStyleChange(style: YugiohTournamentStyle | null) {
  form.yugiohConfig.tournamentStyle = style
}

function handleRoundsChange(rounds: number | null) {
  form.yugiohConfig.roundsCount = rounds
}

function handleRoundTimeChange(minutes: number) {
  form.yugiohConfig.roundTimeMinutes = minutes
}

function handleBestOfChange(bestOf: 1 | 3) {
  form.yugiohConfig.bestOf = bestOf
}

function handleTopCutChange(topCut: number | null) {
  form.yugiohConfig.topCut = topCut
}

function handleMaxPlayersChange(maxPlayers: number) {
  form.maxPlayers = maxPlayers
}

// Handle deck rules changes
function handleProxiesChange(allow: boolean) {
  form.yugiohConfig.allowProxies = allow
}

function handleProxyLimitChange(limit: number | null) {
  form.yugiohConfig.proxyLimit = limit
}

function handleDeckRegistrationChange(required: boolean) {
  form.yugiohConfig.requireDeckRegistration = required
}

function handleDeckDeadlineChange(deadline: string | null) {
  form.yugiohConfig.deckSubmissionDeadline = deadline
}

function handleAllowSideDeckChange(allow: boolean) {
  form.yugiohConfig.allowSideDeck = allow
}

function handleEnforceLegalityChange(enforce: boolean) {
  form.yugiohConfig.enforceFormatLegality = enforce
}

function handleHouseRulesChange(notes: string | null) {
  form.yugiohConfig.houseRulesNotes = notes
}

// Handle prizes changes
function handleEntryFeeChange(fee: number | null) {
  form.yugiohConfig.entryFee = fee
}

function handleCurrencyChange(currency: string) {
  form.yugiohConfig.entryFeeCurrency = currency
}

function handleOfficialEventChange(isOfficial: boolean) {
  form.yugiohConfig.isOfficialEvent = isOfficial
}

function handleOtsPointsChange(awards: boolean) {
  form.yugiohConfig.awardsOtsPoints = awards
}

function handleHasPrizesChange(has: boolean) {
  form.yugiohConfig.hasPrizes = has
}

function handlePrizeStructureChange(structure: string | null) {
  form.yugiohConfig.prizeStructure = structure
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

    const event = await createYugiohEvent(token, form)
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
          <h1 class="text-xl font-bold flex items-center gap-2 text-blue-700">
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M12,6L8,18H10L10.75,16H13.25L14,18H16L12,6M10.83,14L12,10.5L13.17,14H10.83Z"/>
            </svg>
            Host Yu-Gi-Oh! TCG Event
          </h1>
          <p class="text-gray-500 text-sm mt-1">Create a Yu-Gi-Oh! Trading Card Game event with format-specific settings</p>
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
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="e.g., Saturday Yu-Gi-Oh! Locals"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  v-model="form.description"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
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

          <!-- Format Selection -->
          <section
            id="format-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.format }"
          >
            <YugiohFormatSelector
              :model-value="form.yugiohConfig.formatId"
              :custom-format-name="form.yugiohConfig.customFormatName"
              :has-error="sectionErrors.format"
              @update:model-value="handleFormatChange"
              @update:custom-format-name="(v) => form.yugiohConfig.customFormatName = v"
            />
          </section>

          <!-- Event Structure -->
          <section
            id="event-structure-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.eventType }"
          >
            <YugiohEventStructureSection
              :format-id="form.yugiohConfig.formatId"
              :event-type="form.yugiohConfig.eventType"
              :tournament-style="form.yugiohConfig.tournamentStyle"
              :rounds-count="form.yugiohConfig.roundsCount"
              :round-time-minutes="form.yugiohConfig.roundTimeMinutes"
              :best-of="form.yugiohConfig.bestOf"
              :top-cut="form.yugiohConfig.topCut"
              :has-error="sectionErrors.eventType"
              @update:event-type="handleEventTypeChange"
              @update:tournament-style="handleTournamentStyleChange"
              @update:rounds-count="handleRoundsChange"
              @update:round-time-minutes="handleRoundTimeChange"
              @update:best-of="handleBestOfChange"
              @update:top-cut="handleTopCutChange"
              @update:max-players="handleMaxPlayersChange"
            />
          </section>

          <!-- Deck Rules -->
          <YugiohDeckRulesSection
            :selected-format="selectedFormat"
            :format-id="form.yugiohConfig.formatId"
            :event-type="form.yugiohConfig.eventType"
            :allow-proxies="form.yugiohConfig.allowProxies"
            :proxy-limit="form.yugiohConfig.proxyLimit"
            :require-deck-registration="form.yugiohConfig.requireDeckRegistration"
            :deck-submission-deadline="form.yugiohConfig.deckSubmissionDeadline"
            :allow-side-deck="form.yugiohConfig.allowSideDeck"
            :enforce-format-legality="form.yugiohConfig.enforceFormatLegality"
            :house-rules-notes="form.yugiohConfig.houseRulesNotes"
            @update:allow-proxies="handleProxiesChange"
            @update:proxy-limit="handleProxyLimitChange"
            @update:require-deck-registration="handleDeckRegistrationChange"
            @update:deck-submission-deadline="handleDeckDeadlineChange"
            @update:allow-side-deck="handleAllowSideDeckChange"
            @update:enforce-format-legality="handleEnforceLegalityChange"
            @update:house-rules-notes="handleHouseRulesChange"
          />

          <!-- What to Bring -->
          <YugiohWhatToBring
            :selected-format="selectedFormat"
            :event-type="form.yugiohConfig.eventType"
            :allow-side-deck="form.yugiohConfig.allowSideDeck"
          />

          <!-- Entry & Prizes -->
          <YugiohPrizesSection
            :event-type="form.yugiohConfig.eventType"
            :entry-fee="form.yugiohConfig.entryFee"
            :entry-fee-currency="form.yugiohConfig.entryFeeCurrency"
            :is-official-event="form.yugiohConfig.isOfficialEvent"
            :awards-ots-points="form.yugiohConfig.awardsOtsPoints"
            :has-prizes="form.yugiohConfig.hasPrizes"
            :prize-structure="form.yugiohConfig.prizeStructure"
            @update:entry-fee="handleEntryFeeChange"
            @update:entry-fee-currency="handleCurrencyChange"
            @update:is-official-event="handleOfficialEventChange"
            @update:awards-ots-points="handleOtsPointsChange"
            @update:has-prizes="handleHasPrizesChange"
            @update:prize-structure="handlePrizeStructureChange"
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
                  v-model="form.yugiohConfig.allowSpectators"
                  type="checkbox"
                  class="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
                <label for="allowSpectators" class="text-sm">
                  <span class="font-medium">Allow Spectators</span>
                  <span class="text-gray-500 ml-1">Let non-players watch</span>
                </label>
              </div>
            </template>
            <template v-if="maxPlayerGuidance" #extra-row>
              <p class="text-xs text-gray-500 -mt-2">
                {{ maxPlayerGuidance }}
              </p>
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
              class="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ loading ? 'Creating...' : 'Create Yu-Gi-Oh! Event' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
