<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { createPokemonEvent } from '@/services/pokemonEventApi'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import PokemonFormatSelector from '@/components/pokemon/PokemonFormatSelector.vue'
import PokemonEventStructureSection from '@/components/pokemon/PokemonEventStructureSection.vue'
import PokemonDeckRulesSection from '@/components/pokemon/PokemonDeckRulesSection.vue'
import PokemonPrizesSection from '@/components/pokemon/PokemonPrizesSection.vue'
import type { CreatePokemonEventInput, PokemonEventConfigInput, PokemonEventType, PokemonTournamentStyle } from '@/types/pokemon'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')

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
    hasPrizes: false,
    prizeStructure: null,
    entryFee: null,
    entryFeeCurrency: 'USD',
    usePlayPoints: false,
    hasJuniorDivision: false,
    hasSeniorDivision: false,
    hasMastersDivision: true,
    allowSpectators: true,
  },
})

// Computed for validation
const isValid = computed(() => {
  return form.title.trim() !== '' && form.eventDate !== ''
})

// Handle format selection
function handleFormatChange(formatId: string | null) {
  form.pokemonConfig.formatId = formatId
}

// Handle event structure changes
function handleEventTypeChange(eventType: PokemonEventType) {
  form.pokemonConfig.eventType = eventType
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

function handleHasPrizesChange(has: boolean) {
  form.pokemonConfig.hasPrizes = has
}

function handlePrizeStructureChange(structure: string | null) {
  form.pokemonConfig.prizeStructure = structure
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
  if (!isValid.value) return

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
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M12,6A6,6 0 0,1 18,12A6,6 0 0,1 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6M12,8A4,4 0 0,0 8,12A4,4 0 0,0 12,16A4,4 0 0,0 16,12A4,4 0 0,0 12,8Z"/>
            </svg>
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
                <label class="block text-sm font-medium text-gray-700 mb-1">Event Title *</label>
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
          <EventLocationSection
            v-model:event-location-id="form.eventLocationId"
            v-model:address="form.addressLine1"
            v-model:city="form.city"
            v-model:state="form.state"
            v-model:postal-code="form.postalCode"
            v-model:location-details="form.locationDetails"
            v-model:venue-hall="form.venueHall"
            v-model:venue-room="form.venueRoom"
            v-model:venue-table="form.venueTable"
            @location-select="handleLocationSelect"
          />

          <!-- Format Selection -->
          <PokemonFormatSelector
            :model-value="form.pokemonConfig.formatId"
            @update:model-value="handleFormatChange"
          />

          <!-- Event Structure -->
          <PokemonEventStructureSection
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
          />

          <!-- Entry & Prizes -->
          <PokemonPrizesSection
            :entry-fee="form.pokemonConfig.entryFee"
            :entry-fee-currency="form.pokemonConfig.entryFeeCurrency"
            :use-play-points="form.pokemonConfig.usePlayPoints"
            :has-prizes="form.pokemonConfig.hasPrizes"
            :prize-structure="form.pokemonConfig.prizeStructure"
            @update:entry-fee="handleEntryFeeChange"
            @update:entry-fee-currency="handleCurrencyChange"
            @update:use-play-points="handlePlayPointsChange"
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
          </EventPlayerSettingsSection>

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
              :disabled="!isValid || loading"
              class="px-6 py-2 bg-yellow-500 text-white font-medium rounded-lg hover:bg-yellow-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {{ loading ? 'Creating...' : 'Create Pokemon Event' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
