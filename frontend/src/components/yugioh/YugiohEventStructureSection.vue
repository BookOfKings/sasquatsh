<script setup lang="ts">
import { computed, watch } from 'vue'
import type { YugiohEventType, YugiohTournamentStyle } from '@/types/yugioh'
import {
  YUGIOH_EVENT_TYPE_DESCRIPTIONS,
  TOURNAMENT_EVENT_TYPES,
  DEFAULT_MAX_PLAYERS,
  DEFAULT_TOP_CUT,
} from '@/types/yugioh'

const props = defineProps<{
  formatId: string | null
  eventType: YugiohEventType | null
  tournamentStyle: YugiohTournamentStyle | null
  roundsCount: number | null
  roundTimeMinutes: number
  bestOf: 1 | 3
  topCut: number | null
  disabled?: boolean
  hasError?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:eventType', value: YugiohEventType): void
  (e: 'update:tournamentStyle', value: YugiohTournamentStyle | null): void
  (e: 'update:roundsCount', value: number | null): void
  (e: 'update:roundTimeMinutes', value: number): void
  (e: 'update:bestOf', value: 1 | 3): void
  (e: 'update:topCut', value: number | null): void
  (e: 'update:maxPlayers', value: number): void
}>()

// Event type categories with icons
const eventTypeCategories = [
  {
    label: 'Casual',
    types: [
      { value: 'casual' as YugiohEventType, label: 'Casual Play', icon: 'M2,21H20V19H2M20,8H18V5H20M20,3H4V13A4,4 0 0,0 8,17H14A4,4 0 0,0 18,13V10H20A2,2 0 0,0 22,8V5C22,3.89 21.1,3 20,3Z' },
      { value: 'locals' as YugiohEventType, label: 'Locals', icon: 'M12,3L2,12H5V20H19V12H22L12,3M12,8.75A2.25,2.25 0 0,1 14.25,11A2.25,2.25 0 0,1 12,13.25A2.25,2.25 0 0,1 9.75,11A2.25,2.25 0 0,1 12,8.75Z' },
    ]
  },
  {
    label: 'Official',
    types: [
      { value: 'ots' as YugiohEventType, label: 'OTS Tournament', icon: 'M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z' },
      { value: 'regional' as YugiohEventType, label: 'Regional', icon: 'M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z' },
    ]
  },
  {
    label: 'Premier',
    types: [
      { value: 'ycs' as YugiohEventType, label: 'YCS', icon: 'M5,16L3,5L8.5,12L12,5L15.5,12L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z' },
      { value: 'nationals' as YugiohEventType, label: 'Nationals', icon: 'M14.4,6L14,4H5V21H7V14H12.6L13,16H20V6H14.4Z' },
      { value: 'worlds' as YugiohEventType, label: 'Worlds', icon: 'M17.9,17.39C17.64,16.59 16.89,16 16,16H15V13A1,1 0 0,0 14,12H8V10H10A1,1 0 0,0 11,9V7H13A2,2 0 0,0 15,5V4.59C17.93,5.77 20,8.64 20,12C20,14.08 19.2,15.97 17.9,17.39M11,19.93C7.05,19.44 4,16.08 4,12C4,11.38 4.08,10.79 4.21,10.21L9,15V16A2,2 0 0,0 11,18M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z' },
    ]
  }
]

// Computed: is this a tournament event?
const isTournament = computed(() =>
  props.eventType ? TOURNAMENT_EVENT_TYPES.includes(props.eventType) : false
)

// Only show tournament settings for tournament events
const showTournamentSettings = computed(() => isTournament.value)

// Get event type description
const selectedTypeDescription = computed(() =>
  props.eventType ? YUGIOH_EVENT_TYPE_DESCRIPTIONS[props.eventType] : ''
)

const hasTopCut = computed(() => props.topCut !== null && props.topCut > 0)

const topCutOptions = [4, 8, 16, 32, 64]

function handleEventTypeChange(newType: YugiohEventType) {
  emit('update:eventType', newType)

  // Set intelligent defaults based on event type
  if (TOURNAMENT_EVENT_TYPES.includes(newType)) {
    // Tournament: Swiss, Bo3, 40 min
    if (!props.tournamentStyle) {
      emit('update:tournamentStyle', 'swiss')
    }
    emit('update:bestOf', 3)
    emit('update:roundTimeMinutes', 40)

    // Set default top cut based on event type
    const defaultTopCut = DEFAULT_TOP_CUT[newType]
    emit('update:topCut', defaultTopCut)

    // Suggest max players
    emit('update:maxPlayers', DEFAULT_MAX_PLAYERS[newType])
  } else {
    // Casual: no tournament structure
    emit('update:tournamentStyle', null)
    emit('update:topCut', null)
    emit('update:roundsCount', null)

    // Suggest max players
    emit('update:maxPlayers', DEFAULT_MAX_PLAYERS[newType])
  }
}

// Sync round time with Best of selection
watch(() => props.bestOf, (newBestOf) => {
  if (isTournament.value) {
    // Yu-Gi-Oh!: 40 min for Bo3, 20 min for Bo1
    emit('update:roundTimeMinutes', newBestOf === 3 ? 40 : 20)
  }
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Event Structure</h3>

    <!-- Event Type Selection -->
    <div class="space-y-3">
      <label class="block text-sm font-medium" :class="hasError ? 'text-red-600' : 'text-gray-700'">
        Event Type <span class="text-red-500">*</span>
      </label>

      <!-- Error message when no selection -->
      <p v-if="hasError && !eventType" class="text-sm text-red-600 flex items-center gap-1">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
        </svg>
        Please select an event type
      </p>

      <div v-for="category in eventTypeCategories" :key="category.label" class="space-y-2">
        <span class="text-xs font-medium text-gray-500 uppercase tracking-wider">
          {{ category.label }}
        </span>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="type in category.types"
            :key="type.value"
            type="button"
            :disabled="disabled"
            class="px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 border"
            :class="[
              eventType === type.value
                ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400 hover:bg-blue-50',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="handleEventTypeChange(type.value)"
          >
            <svg class="w-4 h-4 mr-1.5 inline-block" viewBox="0 0 24 24" fill="currentColor">
              <path :d="type.icon" />
            </svg>
            {{ type.label }}
          </button>
        </div>
      </div>

      <!-- Event type description -->
      <p v-if="selectedTypeDescription" class="text-sm text-gray-500 italic">
        {{ selectedTypeDescription }}
      </p>

      <!-- OTS info -->
      <div v-if="eventType === 'ots'" class="bg-blue-50 border border-blue-100 rounded-lg p-4">
        <h4 class="text-sm font-medium text-blue-800 mb-2 flex items-center gap-2">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z" />
          </svg>
          Official Tournament Store Event
        </h4>
        <p class="text-sm text-blue-700 mb-2">OTS events are sanctioned by Konami and may award OTS packs as prizes.</p>
        <ul class="text-sm text-blue-700 space-y-1">
          <li class="flex items-start gap-2">
            <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
            </svg>
            Players must have a CARD GAME ID
          </li>
          <li class="flex items-start gap-2">
            <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
            </svg>
            Deck registration is required
          </li>
          <li class="flex items-start gap-2">
            <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
            </svg>
            Advanced format with current Forbidden/Limited list
          </li>
        </ul>
      </div>
    </div>

    <!-- Tournament-specific options -->
    <div v-if="showTournamentSettings" class="bg-gray-50 rounded-lg p-4 space-y-4">
      <h4 class="text-sm font-medium text-gray-900">Tournament Settings</h4>

      <!-- Tournament Style -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Tournament Style</label>
        <div class="flex gap-2">
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              tournamentStyle === 'swiss'
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:tournamentStyle', 'swiss')"
          >
            Swiss
          </button>
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              tournamentStyle === 'single_elimination'
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:tournamentStyle', 'single_elimination')"
          >
            Single Elim
          </button>
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              tournamentStyle === 'double_elimination'
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:tournamentStyle', 'double_elimination')"
          >
            Double Elim
          </button>
        </div>
      </div>

      <!-- Match Style (Best of) -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Match Style</label>
        <div class="flex gap-2">
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              bestOf === 1
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:bestOf', 1)"
          >
            Best of 1
          </button>
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              bestOf === 3
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:bestOf', 3)"
          >
            Best of 3
          </button>
        </div>
        <p class="text-xs text-gray-500 mt-1">
          Standard Yu-Gi-Oh! matches are Best of 3 (first to 2 duel wins).
        </p>
      </div>

      <!-- Rounds Count -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Number of Rounds
        </label>
        <input
          type="number"
          :value="roundsCount ?? ''"
          :disabled="disabled"
          class="input w-32"
          min="1"
          max="15"
          placeholder="Auto"
          @input="$emit('update:roundsCount', ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="text-xs text-gray-500 mt-1">
          Leave blank to auto-calculate based on player count.
        </p>
      </div>

      <!-- Round Time -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Round Time (minutes)
        </label>
        <div class="flex items-center gap-3">
          <input
            type="number"
            :value="roundTimeMinutes"
            :disabled="disabled"
            class="input w-32"
            min="20"
            max="60"
            @input="$emit('update:roundTimeMinutes', parseInt(($event.target as HTMLInputElement).value) || 40)"
          />
          <div class="flex gap-2">
            <button
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
              :class="[
                roundTimeMinutes === 40 ? 'border-blue-500 text-blue-600 bg-blue-50' : 'border-gray-300',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:roundTimeMinutes', 40)"
            >
              40 min
            </button>
            <button
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
              :class="[
                roundTimeMinutes === 45 ? 'border-blue-500 text-blue-600 bg-blue-50' : 'border-gray-300',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:roundTimeMinutes', 45)"
            >
              45 min
            </button>
            <button
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
              :class="[
                roundTimeMinutes === 50 ? 'border-blue-500 text-blue-600 bg-blue-50' : 'border-gray-300',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:roundTimeMinutes', 50)"
            >
              50 min
            </button>
          </div>
        </div>
        <p class="text-xs text-gray-500 mt-1">
          Standard: 40 minutes per round. Use 45-50 min for larger events.
        </p>
      </div>

      <!-- Top Cut -->
      <div>
        <div class="flex items-center gap-3 mb-2">
          <input
            type="checkbox"
            id="hasTopCut"
            :checked="hasTopCut"
            :disabled="disabled"
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            @change="$emit('update:topCut', ($event.target as HTMLInputElement).checked ? 8 : null)"
          />
          <label for="hasTopCut" class="text-sm font-medium text-gray-700">
            Include Top Cut
          </label>
        </div>
        <div v-if="hasTopCut" class="flex gap-2 ml-7">
          <button
            v-for="size in topCutOptions"
            :key="size"
            type="button"
            :disabled="disabled"
            class="px-3 py-1 text-sm rounded border transition-all"
            :class="[
              topCut === size
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:topCut', size)"
          >
            Top {{ size }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
