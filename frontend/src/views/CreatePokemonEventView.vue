<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { createPokemonEvent } from '@/services/pokemonEventApi'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import PokemonFormatSelector from '@/components/pokemon/PokemonFormatSelector.vue'
import PokemonEventStructureSection from '@/components/pokemon/PokemonEventStructureSection.vue'
import PokemonDeckRulesSection from '@/components/pokemon/PokemonDeckRulesSection.vue'
import PokemonPrizesSection from '@/components/pokemon/PokemonPrizesSection.vue'
import PokemonMaterialsSection from '@/components/pokemon/PokemonMaterialsSection.vue'
import type { CreatePokemonEventInput, PokemonEventType, PokemonTournamentStyle, PokemonFormat } from '@/types/pokemon'
import { POKEMON_FORMATS, LIMITED_EVENT_TYPES, DEFAULT_MAX_PLAYERS } from '@/types/pokemon'
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
const showVenueModal = ref(false)
const errorMessage = ref('')

// Selected format object (for passing to components)
const selectedFormat = ref<PokemonFormat | null>(null)

// Form state
const form = reactive<CreatePokemonEventInput>({
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
  // Pokemon config
  pokemonConfig: {
    formatId: null,
    customFormatName: null,
    eventType: 'casual',
    tournamentStyle: null,
    roundsCount: null,
    roundTimeMinutes: 50,
    bestOf: 3,
    topCut: null,
    allowProxies: false,
    proxyLimit: null,
    requireDeckRegistration: false,
    deckSubmissionDeadline: null,
    allowDeckChanges: true,
    enforceFormatLegality: true,
    houseRulesNotes: null,
    hasPrizes: false,
    prizeStructure: null,
    entryFee: null,
    entryFeeCurrency: 'USD',
    usePlayPoints: false,
    organizerConfirmedOfficialLocation: false,
    providesBasicEnergy: false,
    providesDamageCounters: false,
    sleevesRecommended: true,
    providesBuildBattleKits: false,
    hasJuniorDivision: false,
    hasSeniorDivision: false,
    hasMastersDivision: true,
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
  if (!form.pokemonConfig.formatId && !LIMITED_EVENT_TYPES.includes(form.pokemonConfig.eventType)) {
    errors.push('Format selection is required')
  }
  if (!form.pokemonConfig.eventType) errors.push('Event type is required')
  return errors
})

const isValid = computed(() => validationErrors.value.length === 0)

// Section-level error tracking for visual highlighting
const sectionErrors = computed(() => ({
  title: hasAttemptedSubmit.value && !form.title.trim(),
  date: hasAttemptedSubmit.value && !form.eventDate,
  location: hasAttemptedSubmit.value && !hasValidLocation.value,
  format: hasAttemptedSubmit.value && !form.pokemonConfig.formatId && !LIMITED_EVENT_TYPES.includes(form.pokemonConfig.eventType),
  eventType: hasAttemptedSubmit.value && !form.pokemonConfig.eventType,
}))

// Max player guidance based on event type
const maxPlayerGuidance = computed(() => {
  const type = form.pokemonConfig.eventType
  switch (type) {
    case 'draft':
      return 'Recommended: 8 players (standard draft pod)'
    case 'prerelease':
      return 'Recommended: 24-32 players'
    case 'casual':
      return 'Recommended: 4-8 players'
    case 'league':
      return 'Recommended: 8-16 players'
    case 'league_challenge':
    case 'league_cup':
      return 'Recommended: 16-32 players'
    case 'regional':
    case 'international':
    case 'worlds':
      return 'Large event - no typical limit'
    default:
      return null
  }
})

// Scroll to first invalid field
function scrollToFirstError() {
  const selectors = [
    { error: 'Event title is required', selector: 'input[placeholder*="Saturday Pokemon League"]' },
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
watch(() => form.pokemonConfig.formatId, (newFormatId) => {
  selectedFormat.value = newFormatId
    ? POKEMON_FORMATS.find(f => f.id === newFormatId) || null
    : null
})

// Handle format selection
function handleFormatChange(formatId: string | null) {
  form.pokemonConfig.formatId = formatId

  // Format-driven defaults
  if (formatId === 'standard' || formatId === 'expanded') {
    // Competitive formats: default to League Challenge, Swiss, Bo3
    if (form.pokemonConfig.eventType === 'casual' || form.pokemonConfig.eventType === 'league') {
      form.pokemonConfig.eventType = 'league_challenge'
      form.pokemonConfig.tournamentStyle = 'swiss'
      form.pokemonConfig.bestOf = 3
      form.pokemonConfig.roundTimeMinutes = 50
      form.maxPlayers = DEFAULT_MAX_PLAYERS['league_challenge']
    }
    form.pokemonConfig.enforceFormatLegality = true
  } else if (formatId === 'theme' || formatId === 'casual') {
    // Casual formats: default to casual play
    form.pokemonConfig.eventType = 'casual'
    form.pokemonConfig.tournamentStyle = null
    form.pokemonConfig.enforceFormatLegality = formatId === 'theme'
  }
}

// Handle event type changes with format-aware defaults
function handleEventTypeChange(eventType: PokemonEventType) {
  form.pokemonConfig.eventType = eventType

  // Update max players default
  form.maxPlayers = DEFAULT_MAX_PLAYERS[eventType] || 16

  // Set material defaults based on event type
  if (eventType === 'prerelease') {
    form.pokemonConfig.providesBasicEnergy = true
    form.pokemonConfig.providesBuildBattleKits = true
  } else if (eventType === 'draft') {
    form.pokemonConfig.providesBasicEnergy = true
  }
}

function handleTournamentStyleChange(style: PokemonTournamentStyle | null) {
  form.pokemonConfig.tournamentStyle = style
}

function handleRoundsChange(rounds: number | null) {
  form.pokemonConfig.roundsCount = rounds
}

function handleRoundTimeChange(minutes: number) {
  form.pokemonConfig.roundTimeMinutes = minutes
}

function handleBestOfChange(bestOf: 1 | 3) {
  form.pokemonConfig.bestOf = bestOf
}

function handleTopCutChange(topCut: number | null) {
  form.pokemonConfig.topCut = topCut
}

function handleMaxPlayersChange(maxPlayers: number) {
  form.maxPlayers = maxPlayers
}

// Handle deck rules changes
function handleProxiesChange(allow: boolean) {
  form.pokemonConfig.allowProxies = allow
}

function handleProxyLimitChange(limit: number | null) {
  form.pokemonConfig.proxyLimit = limit
}

function handleDeckRegistrationChange(required: boolean) {
  form.pokemonConfig.requireDeckRegistration = required
}

function handleDeckDeadlineChange(deadline: string | null) {
  form.pokemonConfig.deckSubmissionDeadline = deadline
}

function handleDeckChangesChange(allow: boolean) {
  form.pokemonConfig.allowDeckChanges = allow
}

function handleEnforceLegalityChange(enforce: boolean) {
  form.pokemonConfig.enforceFormatLegality = enforce
}

function handleHouseRulesChange(notes: string | null) {
  form.pokemonConfig.houseRulesNotes = notes
}

// Handle prizes changes
function handleEntryFeeChange(fee: number | null) {
  form.pokemonConfig.entryFee = fee
}

function handleCurrencyChange(currency: string) {
  form.pokemonConfig.entryFeeCurrency = currency
}

function handlePlayPointsChange(use: boolean) {
  form.pokemonConfig.usePlayPoints = use
}

function handleOfficialLocationConfirmChange(confirmed: boolean) {
  form.pokemonConfig.organizerConfirmedOfficialLocation = confirmed
}

function handleHasPrizesChange(has: boolean) {
  form.pokemonConfig.hasPrizes = has
}

function handlePrizeStructureChange(structure: string | null) {
  form.pokemonConfig.prizeStructure = structure
}

// Handle materials changes
function handleProvidesEnergyChange(provides: boolean) {
  form.pokemonConfig.providesBasicEnergy = provides
}

function handleProvidesCountersChange(provides: boolean) {
  form.pokemonConfig.providesDamageCounters = provides
}

function handleSleevesRecommendedChange(recommended: boolean) {
  form.pokemonConfig.sleevesRecommended = recommended
}

function handleProvidesBuildBattleChange(provides: boolean) {
  form.pokemonConfig.providesBuildBattleKits = provides
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

    const event = await createPokemonEvent(token, form)
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
          <h1 class="text-xl font-bold flex items-center gap-2 text-yellow-700">
            <img src="/icons/pokemon-logo.png" alt="Pokemon TCG" class="h-6 object-contain" />
            Host Pokemon TCG Event
          </h1>
          <p class="text-gray-500 text-sm mt-1">Create a Pokemon Trading Card Game event with format-specific settings</p>
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
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500"
                  placeholder="e.g., Saturday Pokemon League"
                />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  v-model="form.description"
                  rows="3"
                  class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-yellow-500"
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

          <!-- Format Selection -->
          <section
            id="format-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.format }"
          >
            <PokemonFormatSelector
              :model-value="form.pokemonConfig.formatId"
              :custom-format-name="form.pokemonConfig.customFormatName"
              :has-error="hasAttemptedSubmit && !form.pokemonConfig.formatId && !LIMITED_EVENT_TYPES.includes(form.pokemonConfig.eventType)"
              @update:model-value="handleFormatChange"
              @update:custom-format-name="(v) => form.pokemonConfig.customFormatName = v"
            />
          </section>

          <!-- Event Structure -->
          <section
            id="event-structure-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.eventType }"
          >
            <PokemonEventStructureSection
            :format-id="form.pokemonConfig.formatId"
            :event-type="form.pokemonConfig.eventType"
            :tournament-style="form.pokemonConfig.tournamentStyle"
            :rounds-count="form.pokemonConfig.roundsCount"
            :round-time-minutes="form.pokemonConfig.roundTimeMinutes"
            :best-of="form.pokemonConfig.bestOf"
            :top-cut="form.pokemonConfig.topCut"
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
          <PokemonDeckRulesSection
            :selected-format="selectedFormat"
            :format-id="form.pokemonConfig.formatId"
            :event-type="form.pokemonConfig.eventType"
            :allow-proxies="form.pokemonConfig.allowProxies"
            :proxy-limit="form.pokemonConfig.proxyLimit"
            :require-deck-registration="form.pokemonConfig.requireDeckRegistration"
            :deck-submission-deadline="form.pokemonConfig.deckSubmissionDeadline"
            :allow-deck-changes="form.pokemonConfig.allowDeckChanges"
            :enforce-format-legality="form.pokemonConfig.enforceFormatLegality"
            :house-rules-notes="form.pokemonConfig.houseRulesNotes"
            @update:allow-proxies="handleProxiesChange"
            @update:proxy-limit="handleProxyLimitChange"
            @update:require-deck-registration="handleDeckRegistrationChange"
            @update:deck-submission-deadline="handleDeckDeadlineChange"
            @update:allow-deck-changes="handleDeckChangesChange"
            @update:enforce-format-legality="handleEnforceLegalityChange"
            @update:house-rules-notes="handleHouseRulesChange"
          />

          <!-- Event Materials -->
          <PokemonMaterialsSection
            :event-type="form.pokemonConfig.eventType"
            :provides-basic-energy="form.pokemonConfig.providesBasicEnergy"
            :provides-damage-counters="form.pokemonConfig.providesDamageCounters"
            :sleeves-recommended="form.pokemonConfig.sleevesRecommended"
            :provides-build-battle-kits="form.pokemonConfig.providesBuildBattleKits"
            @update:provides-basic-energy="handleProvidesEnergyChange"
            @update:provides-damage-counters="handleProvidesCountersChange"
            @update:sleeves-recommended="handleSleevesRecommendedChange"
            @update:provides-build-battle-kits="handleProvidesBuildBattleChange"
          />

          <!-- Entry & Prizes -->
          <PokemonPrizesSection
            :event-type="form.pokemonConfig.eventType"
            :entry-fee="form.pokemonConfig.entryFee"
            :entry-fee-currency="form.pokemonConfig.entryFeeCurrency"
            :use-play-points="form.pokemonConfig.usePlayPoints"
            :organizer-confirmed-official-location="form.pokemonConfig.organizerConfirmedOfficialLocation"
            :has-prizes="form.pokemonConfig.hasPrizes"
            :prize-structure="form.pokemonConfig.prizeStructure"
            @update:entry-fee="handleEntryFeeChange"
            @update:entry-fee-currency="handleCurrencyChange"
            @update:use-play-points="handlePlayPointsChange"
            @update:organizer-confirmed-official-location="handleOfficialLocationConfirmChange"
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
                  v-model="form.pokemonConfig.allowSpectators"
                  type="checkbox"
                  class="w-4 h-4 text-yellow-600 border-gray-300 rounded focus:ring-yellow-500"
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
              class="px-6 py-2 bg-yellow-500 text-white font-medium rounded-lg hover:bg-yellow-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :title="!isValid ? validationErrors.join(', ') : ''"
            >
              {{ loading ? 'Creating...' : 'Create Pokemon Event' }}
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
