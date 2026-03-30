<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventConfig } from '@/types/mtg'
import { FORMAT_DESCRIPTIONS, POWER_LEVEL_RANGES, POWER_LEVEL_DESCRIPTIONS } from '@/types/mtg'

const props = defineProps<{
  config: MtgEventConfig
  formatName?: string | null
}>()

// Get display name for format
const formatDisplayName = computed(() => {
  if (props.formatName) return props.formatName
  if (props.config.customFormatName) return props.config.customFormatName
  if (!props.config.formatId) return 'Magic: The Gathering'

  // Capitalize and format common format names
  const formatNames: Record<string, string> = {
    commander: 'Commander (EDH)',
    standard: 'Standard',
    modern: 'Modern',
    pioneer: 'Pioneer',
    legacy: 'Legacy',
    vintage: 'Vintage',
    pauper: 'Pauper',
    draft: 'Booster Draft',
    sealed: 'Sealed Deck',
    cube: 'Cube',
    cube_draft: 'Cube Draft',
    oathbreaker: 'Oathbreaker',
    brawl: 'Brawl',
    casual: 'Casual',
    custom: 'Custom Format',
  }
  return formatNames[props.config.formatId] || props.config.formatId
})

// Get event type display name
const eventTypeDisplay = computed(() => {
  const types: Record<string, string> = {
    casual: 'Casual Play',
    pods: 'Pod Play',
    swiss: 'Swiss Tournament',
    single_elim: 'Single Elimination',
    double_elim: 'Double Elimination',
    round_robin: 'Round Robin',
  }
  return types[props.config.eventType] || props.config.eventType
})

// Is tournament type?
const isTournament = computed(() => {
  return ['swiss', 'single_elim', 'double_elim', 'round_robin'].includes(props.config.eventType)
})

// Is pod play?
const isPodPlay = computed(() => {
  return props.config.eventType === 'pods'
})

// Get structure details - conditionally based on event type
const structureDetails = computed(() => {
  const details: string[] = []

  // Pod Play: show pod size
  if (isPodPlay.value && props.config.podsSize) {
    details.push(`${props.config.podsSize}-player pods`)
  }

  // Tournament: show rounds and round time
  if (isTournament.value) {
    if (props.config.roundsCount) {
      details.push(`${props.config.roundsCount} rounds`)
    }
    if (props.config.roundTimeMinutes) {
      details.push(`${props.config.roundTimeMinutes} min rounds`)
    }
    if (props.config.matchStyle) {
      details.push(props.config.matchStyle === 'bo3' ? 'Best of 3' : 'Best of 1')
    }
  }

  return details
})

// Power level display with range in parentheses
const powerLevelDisplay = computed(() => {
  if (!props.config.powerLevelRange && !props.config.powerLevelMin) return null

  if (props.config.powerLevelRange && props.config.powerLevelRange !== 'custom') {
    const range = POWER_LEVEL_RANGES[props.config.powerLevelRange]
    return {
      label: range.label,
      rangeText: `(${range.min}–${range.max})`,
      description: range.description,
      min: range.min,
      max: range.max,
    }
  }

  if (props.config.powerLevelMin !== null && props.config.powerLevelMax !== null) {
    const min = props.config.powerLevelMin
    const max = props.config.powerLevelMax
    if (min === max) {
      return {
        label: `Power Level`,
        rangeText: `(${min})`,
        description: POWER_LEVEL_DESCRIPTIONS[min] || '',
        min,
        max,
      }
    }
    return {
      label: `Power Level`,
      rangeText: `(${min}–${max})`,
      description: '',
      min,
      max,
    }
  }

  return null
})

// Format description
const formatDescription = computed(() => {
  if (!props.config.formatId) return null
  return FORMAT_DESCRIPTIONS[props.config.formatId] || null
})

// Power level color based on range
const powerLevelColor = computed(() => {
  const range = props.config.powerLevelRange
  switch (range) {
    case 'casual': return 'text-green-600 bg-green-50 border-green-200'
    case 'mid': return 'text-blue-600 bg-blue-50 border-blue-200'
    case 'high': return 'text-orange-600 bg-orange-50 border-orange-200'
    case 'cedh': return 'text-red-600 bg-red-50 border-red-200'
    default: return 'text-purple-600 bg-purple-50 border-purple-200'
  }
})
</script>

<template>
  <div class="bg-gradient-to-r from-purple-50 to-indigo-50 rounded-lg border border-purple-100 p-4">
    <!-- Format Header -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-3 mb-3">
      <!-- MTG Logo -->
      <div class="flex-shrink-0 w-12 h-12 rounded-lg flex items-center justify-center overflow-hidden">
        <img src="/icons/mtg-logo.png" alt="Magic: The Gathering" class="w-full h-full object-contain" />
      </div>

      <div class="flex-1">
        <h3 class="text-xl font-bold text-purple-900">{{ formatDisplayName }}</h3>
        <p v-if="formatDescription" class="text-sm text-purple-700">{{ formatDescription }}</p>
      </div>
    </div>

    <!-- Event Structure Pills -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-purple-200 text-purple-800">
        {{ eventTypeDisplay }}
      </span>
      <span
        v-for="detail in structureDetails"
        :key="detail"
        class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-purple-200 text-purple-800"
      >
        {{ detail }}
      </span>
    </div>

    <!-- Power Level Badge -->
    <div v-if="powerLevelDisplay" class="flex items-center gap-2">
      <span
        class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold border"
        :class="powerLevelColor"
      >
        <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M11,21H5V19H11M15,4H9V13H15M17,13H19V11H17M7,13V11H5V13M17,9V7H19V9M5,9H7V7H5M3,5A2,2 0 0,1 5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H13V19H19V5H5V9H3M3,11V15H5V11"/>
        </svg>
        {{ powerLevelDisplay.label }} {{ powerLevelDisplay.rangeText }}
      </span>
      <span v-if="powerLevelDisplay.description" class="text-sm text-gray-600">
        {{ powerLevelDisplay.description }}
      </span>
    </div>
  </div>
</template>
