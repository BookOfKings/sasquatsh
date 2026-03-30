<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getMtgEvent, updateMtgEvent } from '@/services/mtgEventApi'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import MtgFormatSelector from '@/components/mtg/MtgFormatSelector.vue'
import MtgPowerLevelSection from '@/components/mtg/MtgPowerLevelSection.vue'
import MtgEventStructureSection from '@/components/mtg/MtgEventStructureSection.vue'
import MtgDeckRulesSection from '@/components/mtg/MtgDeckRulesSection.vue'
import MtgPrizesSection from '@/components/mtg/MtgPrizesSection.vue'
import MtgDraftSection from '@/components/mtg/MtgDraftSection.vue'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import { type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'
import { POWER_LEVEL_FORMATS, MTG_FORMATS } from '@/types/mtg'
import type { CreateMtgEventInput, MtgFormat, MtgEventType, DraftStyle, PowerLevelRange, MatchStyle, PlayMode } from '@/types/mtg'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Get event ID from route
const eventId = computed(() => route.params.id as string)

// Venue selection state
const showVenueModal = ref(false)

const loading = ref(false)
const loadingEvent = ref(true)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})

// Format info
const selectedFormat = ref<MtgFormat | null>(null)

// Show power level only for Commander/casual formats
const showPowerLevel = computed(() => {
  const formatId = form.mtgConfig.formatId
  return formatId && POWER_LEVEL_FORMATS.includes(formatId)
})

// Format-first enforcement: check if format is selected
const formatSelected = computed(() => !!form.mtgConfig.formatId)

const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

// Is limited format (draft/sealed)?
const isLimitedFormat = computed(() => selectedFormat.value && !selectedFormat.value.isConstructed)

// Form state
const form = reactive<CreateMtgEventInput>({
  title: '',
  description: '',
  eventDate: '',
  startTime: '19:00',
  timezone: 'America/New_York',
  durationMinutes: 180,
  setupMinutes: 15,
  maxPlayers: 8,
  hostIsPlaying: true,
  isPublic: true,
  isCharityEvent: false,
  groupId: undefined,
  eventLocationId: undefined,
  addressLine1: '',
  city: '',
  state: '',
  postalCode: '',
  locationDetails: '',
  venueHall: undefined,
  venueRoom: undefined,
  venueTable: undefined,
  mtgConfig: {
    formatId: null,
    customFormatName: null,
    eventType: 'casual',
    roundsCount: null,
    roundTimeMinutes: 50,
    podsSize: 4,
    matchStyle: null,
    topCut: null,
    playMode: null,
    allowProxies: false,
    proxyLimit: null,
    powerLevelMin: null,
    powerLevelMax: null,
    powerLevelRange: null,
    bannedCards: [],
    houseRulesNotes: null,
    packsPerPlayer: null,
    draftStyle: null,
    cubeId: null,
    hasPrizes: false,
    prizeStructure: null,
    entryFee: null,
    entryFeeCurrency: 'USD',
    requireDeckRegistration: false,
    deckSubmissionDeadline: null,
    allowSpectators: true,
  },
})

// Load existing event data
onMounted(async () => {
  try {
    const token = await authStore.getIdToken()
    if (!token) {
      errorMessage.value = 'Not authenticated'
      return
    }

    const event = await getMtgEvent(token, eventId.value)

    // Populate form with existing data
    form.title = event.title
    form.description = event.description || ''
    form.eventDate = event.eventDate
    form.startTime = event.startTime || '19:00'
    form.timezone = event.timezone || 'America/New_York'
    form.durationMinutes = event.durationMinutes || 180
    form.setupMinutes = event.setupMinutes || 15
    form.maxPlayers = event.maxPlayers || 8
    form.hostIsPlaying = event.hostIsPlaying ?? true
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

    // MTG config
    if (event.mtgConfig) {
      form.mtgConfig.formatId = event.mtgConfig.formatId || null
      form.mtgConfig.customFormatName = event.mtgConfig.customFormatName || null
      form.mtgConfig.eventType = event.mtgConfig.eventType || 'casual'
      form.mtgConfig.roundsCount = event.mtgConfig.roundsCount || null
      form.mtgConfig.roundTimeMinutes = event.mtgConfig.roundTimeMinutes || 50
      form.mtgConfig.podsSize = event.mtgConfig.podsSize || 4
      form.mtgConfig.matchStyle = event.mtgConfig.matchStyle || null
      form.mtgConfig.topCut = event.mtgConfig.topCut || null
      form.mtgConfig.playMode = event.mtgConfig.playMode || null
      form.mtgConfig.allowProxies = event.mtgConfig.allowProxies || false
      form.mtgConfig.proxyLimit = event.mtgConfig.proxyLimit || null
      form.mtgConfig.powerLevelMin = event.mtgConfig.powerLevelMin || null
      form.mtgConfig.powerLevelMax = event.mtgConfig.powerLevelMax || null
      form.mtgConfig.powerLevelRange = event.mtgConfig.powerLevelRange || null
      form.mtgConfig.bannedCards = event.mtgConfig.bannedCards || []
      form.mtgConfig.houseRulesNotes = event.mtgConfig.houseRulesNotes || null
      form.mtgConfig.packsPerPlayer = event.mtgConfig.packsPerPlayer || null
      form.mtgConfig.draftStyle = event.mtgConfig.draftStyle || null
      form.mtgConfig.cubeId = event.mtgConfig.cubeId || null
      form.mtgConfig.hasPrizes = event.mtgConfig.hasPrizes || false
      form.mtgConfig.prizeStructure = event.mtgConfig.prizeStructure || null
      form.mtgConfig.entryFee = event.mtgConfig.entryFee || null
      form.mtgConfig.entryFeeCurrency = event.mtgConfig.entryFeeCurrency || 'USD'
      form.mtgConfig.requireDeckRegistration = event.mtgConfig.requireDeckRegistration || false
      form.mtgConfig.deckSubmissionDeadline = event.mtgConfig.deckSubmissionDeadline || null
      form.mtgConfig.allowSpectators = event.mtgConfig.allowSpectators ?? true

      // Update selected format
      selectedFormat.value = MTG_FORMATS.find(f => f.id === form.mtgConfig.formatId) || null
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load event'
  } finally {
    loadingEvent.value = false
  }
})

// Handle format selection from MtgFormatSelector
function handleFormatSelected(format: MtgFormat | null) {
  selectedFormat.value = format
}

function validate(): boolean {
  Object.keys(errors).forEach(key => errors[key] = '')
  let valid = true

  if (!form.title.trim()) {
    errors.title = 'Event title is required'
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

  // Location validation
  const hasVenue = !!form.eventLocationId
  const hasCustomAddress = form.city?.trim() && form.postalCode?.trim()
  if (!hasVenue && !hasCustomAddress) {
    if (form.city?.trim() && !form.postalCode?.trim()) {
      errors.location = 'Zip code is required for events to appear in nearby searches'
    } else {
      errors.location = 'Please select a venue or enter a city and zip code'
    }
    valid = false
  }

  // Format validation
  if (!form.mtgConfig.formatId) {
    errors.format = 'Please select a format'
    valid = false
  } else if (form.mtgConfig.formatId === 'custom' && !form.mtgConfig.customFormatName?.trim()) {
    errors.format = 'Please enter a custom format name'
    valid = false
  }

  // Power level validation
  const minPower = form.mtgConfig.powerLevelMin
  const maxPower = form.mtgConfig.powerLevelMax
  if (typeof minPower === 'number' && typeof maxPower === 'number' && minPower > maxPower) {
    errors.powerLevel = 'Minimum power level cannot be higher than maximum'
    valid = false
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  errorMessage.value = ''

  try {
    const token = await authStore.getIdToken()
    if (!token) {
      errorMessage.value = 'You must be logged in to update an event'
      loading.value = false
      return
    }

    await updateMtgEvent(token, eventId.value, form)
    router.push(`/games/${eventId.value}`)
  } catch (err) {
    errorMessage.value = (err as Error).message || 'Failed to update event'
  }

  loading.value = false
}

function goBack() {
  router.push(`/games/${eventId.value}`)
}

// Handle venue selection from EventLocationSection
function handleVenueSelected(venue: EventLocation) {
  form.city = venue.city
  form.state = venue.state
  form.postalCode = venue.postalCode || ''
  if (venue.timezone) {
    form.timezone = venue.timezone
  }
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

    <!-- Loading state -->
    <div v-if="loadingEvent" class="card p-8 text-center">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto mb-4"></div>
      <p class="text-gray-500">Loading event...</p>
    </div>

    <div v-else class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <img src="/icons/mtg-logo.png" alt="MTG" class="h-6 object-contain" />
          Edit MTG Event
        </h1>
        <p class="text-sm text-gray-500 mt-1">
          Update your Magic: The Gathering event settings
        </p>
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

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <!-- Basic Info Section -->
          <div class="space-y-4">
            <h3 class="text-lg font-semibold text-gray-900">Basic Information</h3>

            <div>
              <label for="title" class="label">Event Title *</label>
              <input
                id="title"
                v-model="form.title"
                type="text"
                class="input"
                :class="{ 'input-error': errors.title }"
                placeholder="e.g., Friday Night Commander"
                :disabled="loading"
              />
              <p v-if="errors.title" class="text-sm text-red-500 mt-1">{{ errors.title }}</p>
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

          <!-- Date & Time Section -->
          <EventDateTimeSection
            :event-date="form.eventDate"
            :start-time="form.startTime"
            :timezone="form.timezone"
            :duration-minutes="form.durationMinutes"
            :setup-minutes="form.setupMinutes"
            :disabled="loading"
            :errors="{ eventDate: errors.eventDate, startTime: errors.startTime, durationMinutes: errors.durationMinutes }"
            @update:event-date="form.eventDate = $event"
            @update:start-time="form.startTime = $event"
            @update:timezone="form.timezone = $event"
            @update:duration-minutes="form.durationMinutes = $event"
            @update:setup-minutes="form.setupMinutes = $event"
          />

          <!-- Location Section -->
          <EventLocationSection
            :event-location-id="form.eventLocationId"
            :venue-hall="form.venueHall"
            :venue-room="form.venueRoom"
            :venue-table="form.venueTable"
            :address-line1="form.addressLine1"
            :city="form.city"
            :state="form.state"
            :postal-code="form.postalCode"
            :location-details="form.locationDetails"
            :disabled="loading"
            :current-tier="currentTier"
            :errors="{ location: errors.location }"
            @update:event-location-id="form.eventLocationId = $event"
            @update:venue-hall="form.venueHall = $event"
            @update:venue-room="form.venueRoom = $event"
            @update:venue-table="form.venueTable = $event"
            @update:address-line1="form.addressLine1 = $event"
            @update:city="form.city = $event"
            @update:state="form.state = $event"
            @update:postal-code="form.postalCode = $event"
            @update:location-details="form.locationDetails = $event"
            @show-venue-modal="showVenueModal = true"
            @venue-selected="handleVenueSelected"
          />

          <!-- MTG Format Section -->
          <div class="border-t border-gray-200 pt-6">
            <MtgFormatSelector
              :model-value="form.mtgConfig.formatId ?? null"
              :custom-format-name="form.mtgConfig.customFormatName ?? null"
              :disabled="loading"
              @update:model-value="form.mtgConfig.formatId = $event"
              @update:custom-format-name="form.mtgConfig.customFormatName = $event"
              @format-selected="handleFormatSelected"
            />
            <p v-if="errors.format" class="text-sm text-red-500 mt-2">{{ errors.format }}</p>
          </div>

          <!-- MTG Power Level Section (Commander/casual formats only) -->
          <div v-if="showPowerLevel" class="border-t border-gray-200 pt-6">
            <MtgPowerLevelSection
              :power-level-range="(form.mtgConfig.powerLevelRange as PowerLevelRange) ?? null"
              :power-level-min="form.mtgConfig.powerLevelMin ?? null"
              :power-level-max="form.mtgConfig.powerLevelMax ?? null"
              :format-id="form.mtgConfig.formatId"
              :disabled="loading"
              @update:power-level-range="form.mtgConfig.powerLevelRange = $event"
              @update:power-level-min="form.mtgConfig.powerLevelMin = $event"
              @update:power-level-max="form.mtgConfig.powerLevelMax = $event"
            />
            <p v-if="errors.powerLevel" class="text-sm text-red-500 mt-2">{{ errors.powerLevel }}</p>
          </div>

          <!-- MTG Event Structure Section -->
          <div class="border-t border-gray-200 pt-6">
            <MtgEventStructureSection
              :event-type="(form.mtgConfig.eventType as MtgEventType) ?? 'casual'"
              :rounds-count="form.mtgConfig.roundsCount ?? null"
              :round-time-minutes="form.mtgConfig.roundTimeMinutes ?? 50"
              :pods-size="form.mtgConfig.podsSize ?? null"
              :match-style="(form.mtgConfig.matchStyle as MatchStyle) ?? null"
              :top-cut="form.mtgConfig.topCut ?? null"
              :play-mode="(form.mtgConfig.playMode as PlayMode) ?? null"
              :disabled="loading"
              @update:event-type="form.mtgConfig.eventType = $event"
              @update:rounds-count="form.mtgConfig.roundsCount = $event"
              @update:round-time-minutes="form.mtgConfig.roundTimeMinutes = $event"
              @update:pods-size="form.mtgConfig.podsSize = $event"
              @update:match-style="form.mtgConfig.matchStyle = $event"
              @update:top-cut="form.mtgConfig.topCut = $event"
              @update:play-mode="form.mtgConfig.playMode = $event"
            />
          </div>

          <!-- MTG Deck Rules Section -->
          <div v-if="formatSelected" class="border-t border-gray-200 pt-6">
            <MtgDeckRulesSection
              :selected-format="selectedFormat"
              :format-id="form.mtgConfig.formatId ?? null"
              :allow-proxies="form.mtgConfig.allowProxies ?? false"
              :proxy-limit="form.mtgConfig.proxyLimit ?? null"
              :banned-cards="(form.mtgConfig.bannedCards as string[]) ?? []"
              :require-deck-registration="form.mtgConfig.requireDeckRegistration ?? false"
              :deck-submission-deadline="form.mtgConfig.deckSubmissionDeadline ?? null"
              :house-rules-notes="form.mtgConfig.houseRulesNotes ?? null"
              :disabled="loading"
              @update:allow-proxies="form.mtgConfig.allowProxies = $event"
              @update:proxy-limit="form.mtgConfig.proxyLimit = $event"
              @update:banned-cards="form.mtgConfig.bannedCards = $event"
              @update:require-deck-registration="form.mtgConfig.requireDeckRegistration = $event"
              @update:deck-submission-deadline="form.mtgConfig.deckSubmissionDeadline = $event"
              @update:house-rules-notes="form.mtgConfig.houseRulesNotes = $event"
            />
          </div>

          <!-- MTG Draft Section (only for limited formats) -->
          <div v-if="isLimitedFormat" class="border-t border-gray-200 pt-6">
            <MtgDraftSection
              :packs-per-player="form.mtgConfig.packsPerPlayer ?? null"
              :draft-style="(form.mtgConfig.draftStyle as DraftStyle) ?? null"
              :cube-id="form.mtgConfig.cubeId ?? null"
              :format-id="form.mtgConfig.formatId ?? null"
              :disabled="loading"
              @update:packs-per-player="form.mtgConfig.packsPerPlayer = $event"
              @update:draft-style="form.mtgConfig.draftStyle = $event"
              @update:cube-id="form.mtgConfig.cubeId = $event"
            />
          </div>

          <!-- MTG Prizes Section -->
          <div class="border-t border-gray-200 pt-6">
            <MtgPrizesSection
              :has-prizes="form.mtgConfig.hasPrizes ?? false"
              :prize-structure="form.mtgConfig.prizeStructure ?? null"
              :entry-fee="form.mtgConfig.entryFee ?? null"
              :entry-fee-currency="form.mtgConfig.entryFeeCurrency ?? 'USD'"
              :disabled="loading"
              @update:has-prizes="form.mtgConfig.hasPrizes = $event"
              @update:prize-structure="form.mtgConfig.prizeStructure = $event"
              @update:entry-fee="form.mtgConfig.entryFee = $event"
              @update:entry-fee-currency="form.mtgConfig.entryFeeCurrency = $event"
            />
          </div>

          <!-- Player Settings Section -->
          <div class="border-t border-gray-200 pt-6">
            <EventPlayerSettingsSection
              :max-players="form.maxPlayers"
              :host-is-playing="form.hostIsPlaying"
              :is-public="form.isPublic"
              :is-charity-event="form.isCharityEvent"
              :disabled="loading"
              :default-max-players="8"
              :errors="{ maxPlayers: errors.maxPlayers }"
              @update:max-players="form.maxPlayers = $event"
              @update:host-is-playing="form.hostIsPlaying = $event"
              @update:is-public="form.isPublic = $event"
              @update:is-charity-event="form.isCharityEvent = $event"
            >
              <!-- MTG specific: Allow Spectators -->
              <template #extra-settings>
                <div class="flex items-center">
                  <label class="flex items-center gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      v-model="form.mtgConfig.allowSpectators"
                      class="w-5 h-5 rounded text-primary-500 border-gray-300 focus:ring-primary-500"
                      :disabled="loading"
                    />
                    <div>
                      <span class="label">Allow Spectators</span>
                      <p class="text-sm text-gray-500">Let non-players watch</p>
                    </div>
                  </label>
                </div>
              </template>
            </EventPlayerSettingsSection>
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
              {{ loading ? 'Saving...' : 'Save Changes' }}
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
  </div>
</template>
