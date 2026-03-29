<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventType, PokemonTournamentStyle } from '@/types/pokemon'

const props = defineProps<{
  eventType: PokemonEventType
  tournamentStyle: PokemonTournamentStyle | null
  roundsCount: number | null
  roundTimeMinutes: number
  bestOf: 1 | 3
  topCut: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:eventType', value: PokemonEventType): void
  (e: 'update:tournamentStyle', value: PokemonTournamentStyle | null): void
  (e: 'update:roundsCount', value: number | null): void
  (e: 'update:roundTimeMinutes', value: number): void
  (e: 'update:bestOf', value: 1 | 3): void
  (e: 'update:topCut', value: number | null): void
}>()

// Event type categories with icons
const eventTypeCategories = [
  {
    label: 'Casual',
    types: [
      { value: 'casual' as PokemonEventType, label: 'Casual Play', description: 'Free play, no structured rounds', icon: 'M2,21H20V19H2M20,8H18V5H20M20,3H4V13A4,4 0 0,0 8,17H14A4,4 0 0,0 18,13V10H20A2,2 0 0,0 22,8V5C22,3.89 21.1,3 20,3Z' },
      { value: 'league' as PokemonEventType, label: 'League Play', description: 'Pokemon League with Play! Points', icon: 'M12,8L10.67,8.09C9.81,7.07 7.4,4.5 5,4.5C5,4.5 3.03,7.46 4.96,11.41C4.41,12.24 4.07,12.67 4,13.66L2.07,13.95L2.28,14.93L4.04,14.67L4.18,15.38L2.61,16.32L3.08,17.21L4.53,16.32C5.68,18.76 8.59,20 12,20C15.41,20 18.32,18.76 19.47,16.32L20.92,17.21L21.39,16.32L19.82,15.38L19.96,14.67L21.72,14.93L21.93,13.95L20,13.66C19.93,12.67 19.59,12.24 19.04,11.41C20.97,7.46 19,4.5 19,4.5C16.6,4.5 14.19,7.07 13.33,8.09L12,8Z' },
    ]
  },
  {
    label: 'Competitive',
    types: [
      { value: 'league_challenge' as PokemonEventType, label: 'League Challenge', description: 'Local-level competitive event', icon: 'M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z' },
      { value: 'league_cup' as PokemonEventType, label: 'League Cup', description: 'Higher-level competitive event', icon: 'M5,16L3,5L8.5,12L12,5L15.5,12L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z' },
      { value: 'regional' as PokemonEventType, label: 'Regional', description: 'Regional Championship', icon: 'M12,3L2,12H5V20H19V12H22L12,3M12,8.75A2.25,2.25 0 0,1 14.25,11A2.25,2.25 0 0,1 12,13.25A2.25,2.25 0 0,1 9.75,11A2.25,2.25 0 0,1 12,8.75Z' },
    ]
  },
  {
    label: 'Special',
    types: [
      { value: 'prerelease' as PokemonEventType, label: 'Prerelease', description: 'Build a deck from new set packs', icon: 'M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5V9H9V5H7M11,5V11H13V5H11M15,5V7H17V5H15M7,11V13H9V11H7M11,13V15H13V13H11M15,9V13H17V9H15M7,15V19H9V15H7M11,17V19H13V17H11M15,15V19H17V15H15Z' },
      { value: 'draft' as PokemonEventType, label: 'Draft', description: 'Draft cards from packs', icon: 'M19,3H14.82C14.25,1.44 12.53,0.64 11,1.2C10.14,1.5 9.5,2.16 9.18,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M12,3A1,1 0 0,1 13,4A1,1 0 0,1 12,5A1,1 0 0,1 11,4A1,1 0 0,1 12,3' },
    ]
  }
]

const isTournament = computed(() =>
  ['league_challenge', 'league_cup', 'regional', 'international', 'worlds'].includes(props.eventType)
)

const isLimitedEvent = computed(() =>
  ['prerelease', 'draft'].includes(props.eventType)
)

const showTournamentSettings = computed(() => isTournament.value || isLimitedEvent.value)

const selectedTypeDescription = computed(() => {
  for (const category of eventTypeCategories) {
    const found = category.types.find(t => t.value === props.eventType)
    if (found) return found.description
  }
  return ''
})

const hasTopCut = computed(() => props.topCut !== null && props.topCut > 0)

const topCutOptions = [4, 8, 16, 32]

function handleEventTypeChange(newType: PokemonEventType) {
  emit('update:eventType', newType)

  // Set sensible defaults when switching types
  if (['league_challenge', 'league_cup', 'regional', 'prerelease', 'draft'].includes(newType)) {
    // Tournament: default to Swiss and Bo3
    if (!props.tournamentStyle) {
      emit('update:tournamentStyle', 'swiss')
    }
    emit('update:bestOf', 3)
    emit('update:roundTimeMinutes', 50)
  } else {
    // Casual/League: no tournament structure
    emit('update:tournamentStyle', null)
    emit('update:topCut', null)
    emit('update:roundsCount', null)
  }
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Event Structure</h3>

    <!-- Event Type Selection -->
    <div class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">
        Event Type <span class="text-red-500">*</span>
      </label>

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
                ? 'bg-yellow-500 text-white border-yellow-500 shadow-sm'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400 hover:bg-yellow-50',
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

      <p v-if="selectedTypeDescription" class="text-sm text-gray-500">
        {{ selectedTypeDescription }}
      </p>

      <!-- Prerelease/Draft info -->
      <div v-if="isLimitedEvent" class="bg-amber-50 border border-amber-100 rounded-lg p-3">
        <p class="text-sm text-amber-800">
          <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5V9H9V5H7M11,5V11H13V5H11M15,5V7H17V5H15M7,11V13H9V11H7M11,13V15H13V13H11M15,9V13H17V9H15M7,15V19H9V15H7M11,17V19H13V17H11M15,15V19H17V15H15Z" />
          </svg>
          Players build decks on-site from provided packs. No need to bring a deck!
        </p>
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:bestOf', 3)"
          >
            Best of 3
          </button>
        </div>
        <p class="text-xs text-gray-500 mt-1">
          Official Pokemon events use Best of 3 with 50-minute rounds.
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
            max="90"
            @input="$emit('update:roundTimeMinutes', parseInt(($event.target as HTMLInputElement).value) || 50)"
          />
          <div class="flex gap-2">
            <button
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
              :class="[
                roundTimeMinutes === 25 ? 'border-yellow-500 text-yellow-600 bg-yellow-50' : 'border-gray-300',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:roundTimeMinutes', 25)"
            >
              25 min
            </button>
            <button
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
              :class="[
                roundTimeMinutes === 50 ? 'border-yellow-500 text-yellow-600 bg-yellow-50' : 'border-gray-300',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:roundTimeMinutes', 50)"
            >
              50 min
            </button>
          </div>
        </div>
        <p class="text-xs text-gray-500 mt-1">
          Standard: 50 min for Bo3, 25 min for Bo1.
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
            class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
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
                ? 'bg-yellow-500 text-white border-yellow-500'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400',
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
