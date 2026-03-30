<script setup lang="ts">
import { computed } from 'vue'
import type { YugiohEventConfig, YugiohEventType, YugiohTournamentStyle } from '@/types/yugioh'
import { YUGIOH_FORMATS, TOURNAMENT_EVENT_TYPES } from '@/types/yugioh'

const props = defineProps<{
  config: YugiohEventConfig
}>()

// Format display name
const formatDisplayName = computed(() => {
  const format = YUGIOH_FORMATS.find(f => f.id === props.config.formatId)
  return format?.name ?? props.config.formatId ?? 'Yu-Gi-Oh! TCG'
})

// Format description
const formatDescription = computed(() => {
  const format = YUGIOH_FORMATS.find(f => f.id === props.config.formatId)
  return format?.description ?? null
})

// Event type labels
const eventTypeLabels: Record<YugiohEventType, string> = {
  casual: 'Casual Play',
  locals: 'Locals',
  ots: 'OTS Tournament',
  regional: 'Regional Championship',
  ycs: 'YCS',
  nationals: 'Nationals',
  worlds: 'World Championship',
}

const eventTypeDisplay = computed(() =>
  eventTypeLabels[props.config.eventType] ?? props.config.eventType
)

// Tournament style labels
const tournamentStyleLabels: Record<YugiohTournamentStyle, string> = {
  swiss: 'Swiss',
  single_elimination: 'Single Elimination',
  double_elimination: 'Double Elimination',
}

// Is tournament type?
const isTournament = computed(() =>
  TOURNAMENT_EVENT_TYPES.includes(props.config.eventType)
)

// Structure details as pills
const structureDetails = computed(() => {
  const details: string[] = []

  if (props.config.tournamentStyle) {
    details.push(tournamentStyleLabels[props.config.tournamentStyle])
  }

  if (props.config.roundsCount) {
    details.push(`${props.config.roundsCount} rounds`)
  }

  if (props.config.roundTimeMinutes) {
    details.push(`${props.config.roundTimeMinutes} min`)
  }

  if (props.config.bestOf) {
    details.push(props.config.bestOf === 3 ? 'Best of 3' : 'Best of 1')
  }

  return details
})

// OTS Points eligible
const hasOtsPoints = computed(() =>
  props.config.awardsOtsPoints && isTournament.value
)
</script>

<template>
  <div class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border border-blue-200 p-4">
    <!-- Format Header -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-3 mb-3">
      <!-- Yu-Gi-Oh! Icon (Card) -->
      <div class="flex-shrink-0 w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
        <svg class="w-7 h-7 text-white" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M12,6L8,18H10L10.75,16H13.25L14,18H16L12,6M10.83,14L12,10.5L13.17,14H10.83Z"/>
        </svg>
      </div>

      <div class="flex-1">
        <h3 class="text-xl font-bold text-blue-900">{{ formatDisplayName }}</h3>
        <p v-if="formatDescription" class="text-sm text-blue-700">{{ formatDescription }}</p>
      </div>
    </div>

    <!-- Event Type + Structure Pills -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-blue-200 text-blue-800">
        {{ eventTypeDisplay }}
      </span>
      <span
        v-for="detail in structureDetails"
        :key="detail"
        class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-blue-200 text-blue-800"
      >
        {{ detail }}
      </span>
    </div>

    <!-- OTS Points Badge -->
    <div v-if="hasOtsPoints" class="flex items-center gap-2">
      <span class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold border text-indigo-600 bg-indigo-50 border-indigo-200">
        <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,16L3,5L8.5,10L12,4L15.5,10L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z"/>
        </svg>
        OTS Points
      </span>
      <span class="text-sm text-gray-600">Konami-sanctioned event</span>
    </div>
  </div>
</template>
