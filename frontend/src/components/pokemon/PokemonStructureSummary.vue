<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventConfig, PokemonTournamentStyle, PokemonEventType } from '@/types/pokemon'
import { TOURNAMENT_EVENT_TYPES, LIMITED_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  config: PokemonEventConfig
}>()

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

// Is limited event?
const isLimited = computed(() =>
  LIMITED_EVENT_TYPES.includes(props.config.eventType)
)

// Check if we have structure details to show
const hasStructureDetails = computed(() => {
  return props.config.tournamentStyle ||
         props.config.roundsCount ||
         props.config.roundTimeMinutes ||
         props.config.bestOf ||
         props.config.topCut ||
         isLimited.value
})
</script>

<template>
  <div v-if="hasStructureDetails" class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M4,2H20A2,2 0 0,1 22,4V16A2,2 0 0,1 20,18H16L12,22L8,18H4A2,2 0 0,1 2,16V4A2,2 0 0,1 4,2M4,4V16H8.83L12,19.17L15.17,16H20V4H4M6,7H18V9H6V7M6,11H16V13H6V11Z"/>
        </svg>
        Event Structure
      </h3>
    </div>

    <div class="p-4">
      <dl class="grid grid-cols-2 gap-4">
        <!-- Event Type -->
        <div>
          <dt class="text-sm text-gray-500">Event Type</dt>
          <dd class="font-medium text-gray-900">{{ eventTypeDisplay }}</dd>
        </div>

        <!-- Tournament Style (tournaments only) -->
        <div v-if="isTournament && config.tournamentStyle">
          <dt class="text-sm text-gray-500">Tournament Style</dt>
          <dd class="font-medium text-gray-900">{{ tournamentStyleLabels[config.tournamentStyle] }}</dd>
        </div>

        <!-- Match Style (tournaments only) -->
        <div v-if="isTournament && config.bestOf">
          <dt class="text-sm text-gray-500">Match Style</dt>
          <dd class="font-medium text-gray-900">Best of {{ config.bestOf }}</dd>
        </div>

        <!-- Rounds (tournaments only) -->
        <div v-if="isTournament && config.roundsCount">
          <dt class="text-sm text-gray-500">Rounds</dt>
          <dd class="font-medium text-gray-900">{{ config.roundsCount }} rounds</dd>
        </div>

        <!-- Round Time -->
        <div v-if="config.roundTimeMinutes">
          <dt class="text-sm text-gray-500">Round Time</dt>
          <dd class="font-medium text-gray-900">{{ config.roundTimeMinutes }} minutes</dd>
        </div>

        <!-- Top Cut (tournaments only) -->
        <div v-if="isTournament && config.topCut">
          <dt class="text-sm text-gray-500">Top Cut</dt>
          <dd class="font-medium text-gray-900">Top {{ config.topCut }}</dd>
        </div>

        <!-- Deck Size (limited events) -->
        <div v-if="isLimited">
          <dt class="text-sm text-gray-500">Deck Size</dt>
          <dd class="font-medium text-gray-900">40+ cards</dd>
        </div>

        <!-- Build & Battle Kits (prerelease only) -->
        <div v-if="config.eventType === 'prerelease' && config.providesBuildBattleKits">
          <dt class="text-sm text-gray-500">Build & Battle</dt>
          <dd class="font-medium text-gray-900">Kits Provided</dd>
        </div>
      </dl>
    </div>
  </div>
</template>
