<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useGroupStore } from '@/stores/useGroupStore'
import { useEventStore } from '@/stores/useEventStore'
import { createMtgEvent } from '@/services/mtgEventApi'
import { getMyProfile } from '@/services/profileApi'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import UpgradePrompt from '@/components/billing/UpgradePrompt.vue'
import MtgFormatSelector from '@/components/mtg/MtgFormatSelector.vue'
import MtgPowerLevelSection from '@/components/mtg/MtgPowerLevelSection.vue'
import MtgEventStructureSection from '@/components/mtg/MtgEventStructureSection.vue'
import MtgDeckRulesSection from '@/components/mtg/MtgDeckRulesSection.vue'
import MtgPrizesSection from '@/components/mtg/MtgPrizesSection.vue'
import MtgDraftSection from '@/components/mtg/MtgDraftSection.vue'
import EventDateTimeSection from '@/components/events/shared/EventDateTimeSection.vue'
import EventLocationSection from '@/components/events/shared/EventLocationSection.vue'
import EventPlayerSettingsSection from '@/components/events/shared/EventPlayerSettingsSection.vue'
import { TIER_LIMITS, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'
import { POWER_LEVEL_FORMATS } from '@/types/mtg'
import type { CreateMtgEventInput, MtgFormat, MtgEventType, DraftStyle, PowerLevelRange, MatchStyle, PlayMode } from '@/types/mtg'
import type { GroupSummary } from '@/types/groups'
import type { EventLocation } from '@/types/social'

const router = useRouter()
const authStore = useAuthStore()
const groupStore = useGroupStore()
const eventStore = useEventStore()

// Groups where user is owner/admin (can create events for)
const userGroups = ref<GroupSummary[]>([])

// Venue selection state
const showVenueModal = ref(false)

const loading = ref(false)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})
const hasAttemptedSubmit = ref(false)

// Format info
const selectedFormat = ref<MtgFormat | null>(null)

// Show power level only for Commander/casual formats
const showPowerLevel = computed(() => {
  const formatId = form.mtgConfig.formatId
  return formatId && POWER_LEVEL_FORMATS.includes(formatId)
})

// Format-first enforcement: check if format is selected
const formatSelected = computed(() => !!form.mtgConfig.formatId)

// Tier limit checking
const showUpgradePrompt = ref(false)
const activeEventCount = ref(0)

const currentTier = computed((): SubscriptionTier => {
  if (!authStore.user.value) return 'free'
  return getEffectiveTier(authStore.user.value)
})

const eventLimit = computed(() => TIER_LIMITS[currentTier.value].gamesPerEvent)
const isAtLimit = computed(() => activeEventCount.value >= eventLimit.value)

// Is limited format (draft/sealed)?
const isLimitedFormat = computed(() => selectedFormat.value && !selectedFormat.value.isConstructed)

// Location validation helper
const hasValidLocation = computed(() => {
  return !!form.eventLocationId || (form.city?.trim() && form.postalCode?.trim())
})

// Section error highlighting
const sectionErrors = computed(() => ({
  location: hasAttemptedSubmit.value && !hasValidLocation.value,
  format: hasAttemptedSubmit.value && !form.mtgConfig.formatId,
}))

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

onMounted(async () => {
  // Load groups where user is owner/admin
  await groupStore.loadMyGroups()
  userGroups.value = groupStore.myGroups.value.filter(
    g => g.userRole === 'owner' || g.userRole === 'admin'
  )

  // Load hosted events to check current count
  await eventStore.loadHostedEvents()
  const todayStr = new Date().toISOString().split('T')[0] ?? ''
  activeEventCount.value = eventStore.hostedEvents.value.filter(
    e => e.eventDate >= todayStr
  ).length

  // Show upgrade prompt if at limit
  if (isAtLimit.value) {
    showUpgradePrompt.value = true
  }

  // Load user's default timezone
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

// Handle format selection from MtgFormatSelector
function handleFormatSelected(format: MtgFormat | null) {
  selectedFormat.value = format

  // Set Commander-specific defaults
  if (format?.id === 'commander' || format?.id === 'oathbreaker' || format?.id === 'brawl') {
    // Default to Pod Play for Commander formats
    if (form.mtgConfig.eventType === 'casual') {
      form.mtgConfig.eventType = 'pods'
      form.mtgConfig.podsSize = 4
      form.mtgConfig.roundTimeMinutes = 90
      form.mtgConfig.playMode = 'assigned_pods'
    }
    // Set default power level to mid if not set
    if (!form.mtgConfig.powerLevelRange) {
      form.mtgConfig.powerLevelRange = 'mid'
      form.mtgConfig.powerLevelMin = 5
      form.mtgConfig.powerLevelMax = 6
    }
  }

  // Set tournament defaults for competitive formats
  if (format?.id === 'standard' || format?.id === 'modern' || format?.id === 'pioneer' || format?.id === 'legacy' || format?.id === 'vintage') {
    if (form.mtgConfig.eventType === 'casual' || form.mtgConfig.eventType === 'pods') {
      form.mtgConfig.eventType = 'swiss'
      form.mtgConfig.matchStyle = 'bo3'
      form.mtgConfig.roundTimeMinutes = 50
      form.mtgConfig.playMode = 'tournament_pairings'
    }
  }

  // Set draft/sealed defaults
  if (format?.id === 'draft' || format?.id === 'sealed' || format?.id === 'cube') {
    if (form.mtgConfig.eventType === 'casual' || form.mtgConfig.eventType === 'pods') {
      form.mtgConfig.eventType = 'swiss'
      form.mtgConfig.matchStyle = 'bo3'
      form.mtgConfig.roundTimeMinutes = 50
      form.mtgConfig.playMode = 'tournament_pairings'
    }
  }
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
  hasAttemptedSubmit.value = true

  if (!validate()) {
    // Scroll to first error
    const firstErrorSection = document.querySelector('.ring-red-300') ||
                              document.querySelector('.input-error') ||
                              document.querySelector('.text-red-500')
    if (firstErrorSection) {
      firstErrorSection.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const token = await authStore.getIdToken()
    if (!token) {
      errorMessage.value = 'You must be logged in to create an event'
      loading.value = false
      return
    }

    const event = await createMtgEvent(token, form)
    router.push(`/games/${event.id}`)
  } catch (err) {
    // Check if this is a tier limit error
    const error = err as Error & { code?: string; data?: { currentCount?: number } }
    if (error.code === 'TIER_LIMIT_REACHED') {
      activeEventCount.value = error.data?.currentCount ?? activeEventCount.value
      showUpgradePrompt.value = true
    } else {
      errorMessage.value = error.message || 'Failed to create event'
    }
  }

  loading.value = false
}

function goBack() {
  router.push('/games')
}

// Handle venue selection from EventLocationSection (for side effects like timezone)
function handleVenueSelected(venue: EventLocation) {
  // Copy address info from venue to form
  form.city = venue.city
  form.state = venue.state
  form.postalCode = venue.postalCode || ''
  // Use venue's timezone if available
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
      Back to Games
    </button>

    <div class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
            <path d="M11.5,1L2,6V8H21V6M16,10V17H19V10M2,22H21V19H2M10,10V17H13V10M4,10V17H7V10H4Z" />
          </svg>
          Host MTG Event
        </h1>
        <p class="text-sm text-gray-500 mt-1">
          Create a Magic: The Gathering event with format-specific settings
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
            </div>

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
          <section
            id="location-section"
            class="rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.location }"
          >
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
          </section>

          <!-- MTG Format Section -->
          <section
            id="format-section"
            class="border-t border-gray-200 pt-6 rounded-lg transition-all"
            :class="{ 'ring-2 ring-red-300 bg-red-50/50 p-4 -m-4 mb-8': sectionErrors.format }"
          >
            <MtgFormatSelector
              :model-value="form.mtgConfig.formatId ?? null"
              :custom-format-name="form.mtgConfig.customFormatName ?? null"
              :disabled="loading"
              :has-error="sectionErrors.format"
              @update:model-value="form.mtgConfig.formatId = $event"
              @update:custom-format-name="form.mtgConfig.customFormatName = $event"
              @format-selected="handleFormatSelected"
            />
            <p v-if="errors.format" class="text-sm text-red-500 mt-2">{{ errors.format }}</p>
          </section>

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
            <!-- Format-first prompt -->
            <div v-if="!formatSelected" class="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-4">
              <p class="text-sm text-gray-600 flex items-center gap-2">
                <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
                </svg>
                Select a format above to configure event details.
              </p>
            </div>
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
              Create MTG Event
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
      :blocking="isAtLimit"
      @close="showUpgradePrompt = false"
    />
  </div>
</template>
