<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventType, MatchStyle } from '@/types/mtg'

const props = defineProps<{
  eventType: MtgEventType
  roundsCount: number | null
  roundTimeMinutes: number
  podsSize: number | null
  matchStyle: MatchStyle | null
  topCut: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:eventType', value: MtgEventType): void
  (e: 'update:roundsCount', value: number | null): void
  (e: 'update:roundTimeMinutes', value: number): void
  (e: 'update:podsSize', value: number | null): void
  (e: 'update:matchStyle', value: MatchStyle | null): void
  (e: 'update:topCut', value: number | null): void
}>()

// Primary event type categories with icons
const eventTypeCategories = [
  {
    label: 'Casual',
    types: [
      { value: 'casual' as MtgEventType, label: 'Casual Play', description: 'Free play, no structured rounds', icon: 'M2,21H20V19H2M20,8H18V5H20M20,3H4V13A4,4 0 0,0 8,17H14A4,4 0 0,0 18,13V10H20A2,2 0 0,0 22,8V5C22,3.89 21.1,3 20,3Z' }, // coffee
      { value: 'pods' as MtgEventType, label: 'Pod Play', description: 'Small groups play together (great for Commander)', icon: 'M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z' }, // account-group
    ]
  },
  {
    label: 'Competitive',
    types: [
      { value: 'swiss' as MtgEventType, label: 'Swiss', description: 'Everyone plays all rounds, paired by record', icon: 'M3,3H9V7H3V3M15,10H21V14H15V10M15,17H21V21H15V17M13,13H7V18H13V20H7L5,20V9H7V11H13V13M21,3V8H15V3H21M11,3H13V8H11V3Z' }, // tournament bracket
      { value: 'single_elim' as MtgEventType, label: 'Single Elimination', description: 'Lose once and you\'re out', icon: 'M5,3H7V5H5V10A2,2 0 0,0 7,12H10V17A2,2 0 0,0 12,19H19V21H12A4,4 0 0,1 8,17V12H7A4,4 0 0,1 3,8V5A2,2 0 0,1 5,3M17,3H19A2,2 0 0,1 21,5V8A4,4 0 0,1 17,12H14V17A4,4 0 0,1 10,21H5V19H10A2,2 0 0,0 12,17V12H17A2,2 0 0,0 19,10V5H17V3Z' }, // tournament
      { value: 'double_elim' as MtgEventType, label: 'Double Elimination', description: 'Two losses and you\'re out', icon: 'M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,5A3,3 0 0,1 15,8A3,3 0 0,1 12,11A3,3 0 0,1 9,8A3,3 0 0,1 12,5M17.13,17C15.92,18.85 14.11,20.24 12,20.92C9.89,20.24 8.08,18.85 6.87,17C6.53,16.5 6.24,16 6,15.47C6,13.82 8.71,12.47 12,12.47C15.29,12.47 18,13.79 18,15.47C17.76,16 17.47,16.5 17.13,17Z' }, // shield-account
      { value: 'round_robin' as MtgEventType, label: 'Round Robin', description: 'Everyone plays everyone', icon: 'M12,18A6,6 0 0,1 6,12C6,11 6.25,10.03 6.7,9.2L5.24,7.74C4.46,8.97 4,10.43 4,12A8,8 0 0,0 12,20V23L16,19L12,15M12,4V1L8,5L12,9V6A6,6 0 0,1 18,12C18,13 17.75,13.97 17.3,14.8L18.76,16.26C19.54,15.03 20,13.57 20,12A8,8 0 0,0 12,4Z' }, // sync/refresh
    ]
  }
]

const isTournament = computed(() =>
  ['swiss', 'single_elim', 'double_elim', 'round_robin'].includes(props.eventType)
)

const isPods = computed(() => props.eventType === 'pods')

const showRoundTime = computed(() => isTournament.value || isPods.value)

const selectedTypeDescription = computed(() => {
  for (const category of eventTypeCategories) {
    const found = category.types.find(t => t.value === props.eventType)
    if (found) return found.description
  }
  return ''
})

const hasTopCut = computed(() => props.topCut !== null && props.topCut > 0)

const topCutOptions = [4, 8, 16, 32]

function handleEventTypeChange(newType: MtgEventType) {
  emit('update:eventType', newType)

  // Set sensible defaults when switching types
  if (['swiss', 'single_elim', 'double_elim', 'round_robin'].includes(newType)) {
    // Tournament: default to Bo3
    if (!props.matchStyle) {
      emit('update:matchStyle', 'bo3')
    }
  } else {
    // Non-tournament: clear tournament-specific fields
    emit('update:matchStyle', null)
    emit('update:topCut', null)
  }

  if (newType === 'pods' && !props.podsSize) {
    emit('update:podsSize', 4)
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

      <p v-if="selectedTypeDescription" class="text-sm text-gray-500">
        {{ selectedTypeDescription }}
      </p>
    </div>

    <!-- Tournament-specific options -->
    <div v-if="isTournament" class="bg-gray-50 rounded-lg p-4 space-y-4">
      <h4 class="text-sm font-medium text-gray-900">Tournament Settings</h4>

      <!-- Match Style -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Match Style</label>
        <div class="flex gap-2">
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              matchStyle === 'bo1'
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:matchStyle', 'bo1')"
          >
            <svg class="w-4 h-4 mr-1.5 inline-block" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z" />
            </svg>
            Best of 1
          </button>
          <button
            type="button"
            :disabled="disabled"
            class="px-4 py-2 rounded-lg text-sm font-medium border transition-all"
            :class="[
              matchStyle === 'bo3'
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:matchStyle', 'bo3')"
          >
            <svg class="w-4 h-4 mr-1.5 inline-block" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M12,6A6,6 0 0,1 18,12A6,6 0 0,1 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6M12,8A4,4 0 0,0 8,12A4,4 0 0,0 12,16A4,4 0 0,0 16,12A4,4 0 0,0 12,8Z" />
            </svg>
            Best of 3
          </button>
        </div>
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
          max="20"
          placeholder="Auto"
          @input="$emit('update:roundsCount', ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) : null)"
        />
        <p class="text-xs text-gray-500 mt-1">
          Leave blank to auto-calculate based on player count.
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

    <!-- Round Time (for tournaments and pods) -->
    <div v-if="showRoundTime">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Round Time (minutes)
      </label>
      <div class="flex items-center gap-3">
        <input
          type="number"
          :value="roundTimeMinutes"
          :disabled="disabled"
          class="input w-32"
          min="10"
          max="180"
          @input="$emit('update:roundTimeMinutes', parseInt(($event.target as HTMLInputElement).value) || 50)"
        />
        <div class="flex gap-2">
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
          <button
            type="button"
            :disabled="disabled"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
            :class="[
              roundTimeMinutes === 60 ? 'border-blue-500 text-blue-600 bg-blue-50' : 'border-gray-300',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:roundTimeMinutes', 60)"
          >
            60 min
          </button>
          <button
            type="button"
            :disabled="disabled"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50 transition-all"
            :class="[
              roundTimeMinutes === 90 ? 'border-blue-500 text-blue-600 bg-blue-50' : 'border-gray-300',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="$emit('update:roundTimeMinutes', 90)"
          >
            90 min
          </button>
        </div>
      </div>
    </div>

    <!-- Pod Size (for pod play) -->
    <div v-if="isPods">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Pod Size
      </label>
      <select
        :value="podsSize ?? 4"
        :disabled="disabled"
        class="input w-40"
        @change="$emit('update:podsSize', parseInt(($event.target as HTMLSelectElement).value))"
      >
        <option :value="3">3 players per pod</option>
        <option :value="4">4 players per pod</option>
        <option :value="5">5 players per pod</option>
        <option :value="6">6 players per pod</option>
      </select>
      <p class="text-xs text-gray-500 mt-1">
        Standard Commander pods are 4 players.
      </p>
    </div>
  </div>
</template>
