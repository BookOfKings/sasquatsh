<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventConfig, PokemonEventType, PokemonTournamentStyle } from '@/types/pokemon'
import { POKEMON_FORMATS, TOURNAMENT_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  config: PokemonEventConfig
}>()

// Format display name
const formatDisplayName = computed(() => {
  const format = POKEMON_FORMATS.find(f => f.id === props.config.formatId)
  return format?.name ?? props.config.formatId ?? 'Pokemon TCG'
})

// Format description
const formatDescription = computed(() => {
  const format = POKEMON_FORMATS.find(f => f.id === props.config.formatId)
  return format?.description ?? null
})

// Event type labels
const eventTypeLabels: Record<PokemonEventType, string> = {
  casual: 'Casual Play',
  league: 'Pokemon League',
  league_cup: 'League Cup',
  league_challenge: 'League Challenge',
  regional: 'Regional Championship',
  international: 'International Championship',
  worlds: 'World Championship',
  prerelease: 'Prerelease',
  draft: 'Booster Draft',
}

const eventTypeDisplay = computed(() =>
  eventTypeLabels[props.config.eventType] ?? props.config.eventType
)

// Tournament style labels
const tournamentStyleLabels: Record<PokemonTournamentStyle, string> = {
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

// Championship Points eligible
const hasChampionshipPoints = computed(() =>
  props.config.usePlayPoints && isTournament.value
)
</script>

<template>
  <div class="bg-gradient-to-r from-yellow-50 to-amber-50 rounded-lg border border-yellow-200 p-4">
    <!-- Format Header -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-3 mb-3">
      <!-- Pokemon Logo -->
      <div class="flex-shrink-0 w-12 h-12 rounded-lg flex items-center justify-center overflow-hidden">
        <img src="/icons/pokemon-logo.png" alt="Pokemon TCG" class="w-full h-full object-contain" />
      </div>

      <div class="flex-1">
        <h3 class="text-xl font-bold text-yellow-900">{{ formatDisplayName }}</h3>
        <p v-if="formatDescription" class="text-sm text-yellow-700">{{ formatDescription }}</p>
      </div>
    </div>

    <!-- Event Type + Structure Pills -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-yellow-200 text-yellow-800">
        {{ eventTypeDisplay }}
      </span>
      <span
        v-for="detail in structureDetails"
        :key="detail"
        class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-white border border-yellow-200 text-yellow-800"
      >
        {{ detail }}
      </span>
    </div>

    <!-- Championship Points Badge -->
    <div v-if="hasChampionshipPoints" class="flex items-center gap-2">
      <span class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm font-semibold border text-red-600 bg-red-50 border-red-200">
        <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,16L3,5L8.5,10L12,4L15.5,10L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z"/>
        </svg>
        Championship Points
      </span>
      <span class="text-sm text-gray-600">Play! Pokemon official event</span>
    </div>
  </div>
</template>
