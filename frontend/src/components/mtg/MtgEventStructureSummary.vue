<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventConfig } from '@/types/mtg'

const props = defineProps<{
  config: MtgEventConfig
}>()

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

// Get play mode display
const playModeDisplay = computed(() => {
  if (!props.config.playMode) return null
  const modes: Record<string, string> = {
    open_play: 'Open Seating',
    assigned_pods: 'Assigned Pods',
    tournament_pairings: 'Tournament Pairings',
  }
  return modes[props.config.playMode] || null
})

// Is tournament type? (Swiss, Single Elim, Double Elim, Round Robin)
const isTournament = computed(() => {
  return ['swiss', 'single_elim', 'double_elim', 'round_robin'].includes(props.config.eventType)
})

// Is pod play?
const isPodPlay = computed(() => {
  return props.config.eventType === 'pods'
})

// Is casual?
const isCasual = computed(() => {
  return props.config.eventType === 'casual'
})

// Show Pod Size only for Pod Play (not for tournaments even if value exists)
const showPodSize = computed(() => {
  return isPodPlay.value && props.config.podsSize
})

// Show Match Style only for tournaments
const showMatchStyle = computed(() => {
  return isTournament.value && props.config.matchStyle
})

// Show Rounds only for tournaments
const showRounds = computed(() => {
  return isTournament.value && props.config.roundsCount
})

// Show Round Time for tournaments, Suggested Duration for pods
const showRoundTime = computed(() => {
  return (isTournament.value || isPodPlay.value) && props.config.roundTimeMinutes
})

// Show Seating for Pod Play
const showSeating = computed(() => {
  return (isPodPlay.value || isCasual.value) && props.config.playMode
})

// Show Top Cut only for tournaments
const showTopCut = computed(() => {
  return isTournament.value && props.config.topCut
})

// Check if we have structure details to show (based on event type logic)
const hasStructureDetails = computed(() => {
  if (isCasual.value) {
    // Casual: minimal, only show if we have seating or round time
    return props.config.playMode || props.config.roundTimeMinutes
  }
  if (isPodPlay.value) {
    // Pod Play: pod size, seating, duration
    return props.config.podsSize || props.config.playMode || props.config.roundTimeMinutes
  }
  if (isTournament.value) {
    // Tournament: match style, rounds, round time, top cut
    return props.config.matchStyle || props.config.roundsCount || props.config.roundTimeMinutes || props.config.topCut
  }
  return false
})
</script>

<template>
  <div v-if="hasStructureDetails" class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M4,2H20A2,2 0 0,1 22,4V16A2,2 0 0,1 20,18H16L12,22L8,18H4A2,2 0 0,1 2,16V4A2,2 0 0,1 4,2M4,4V16H8.83L12,19.17L15.17,16H20V4H4M6,7H18V9H6V7M6,11H16V13H6V11Z"/>
        </svg>
        Event Structure
      </h3>
    </div>

    <div class="p-4">
      <dl class="grid grid-cols-2 gap-4">
        <!-- Event Type (always show) -->
        <div>
          <dt class="text-sm text-gray-500">Event Type</dt>
          <dd class="font-medium text-gray-900">{{ eventTypeDisplay }}</dd>
        </div>

        <!-- Seating Mode (Pod Play / Casual only) -->
        <div v-if="showSeating && playModeDisplay">
          <dt class="text-sm text-gray-500">Seating</dt>
          <dd class="font-medium text-gray-900">{{ playModeDisplay }}</dd>
        </div>

        <!-- Pod Size (Pod Play only) -->
        <div v-if="showPodSize">
          <dt class="text-sm text-gray-500">Pod Size</dt>
          <dd class="font-medium text-gray-900">{{ config.podsSize }} players</dd>
        </div>

        <!-- Match Style (Tournament only) -->
        <div v-if="showMatchStyle">
          <dt class="text-sm text-gray-500">Match Style</dt>
          <dd class="font-medium text-gray-900">
            {{ config.matchStyle === 'bo3' ? 'Best of 3' : 'Best of 1' }}
          </dd>
        </div>

        <!-- Rounds (Tournament only) -->
        <div v-if="showRounds">
          <dt class="text-sm text-gray-500">Rounds</dt>
          <dd class="font-medium text-gray-900">{{ config.roundsCount }} rounds</dd>
        </div>

        <!-- Round Time / Suggested Duration -->
        <div v-if="showRoundTime">
          <dt class="text-sm text-gray-500">
            {{ isPodPlay ? 'Suggested Duration' : 'Round Time' }}
          </dt>
          <dd class="font-medium text-gray-900">{{ config.roundTimeMinutes }} minutes</dd>
        </div>

        <!-- Top Cut (Tournament only) -->
        <div v-if="showTopCut">
          <dt class="text-sm text-gray-500">Top Cut</dt>
          <dd class="font-medium text-gray-900">Top {{ config.topCut }}</dd>
        </div>

        <!-- Spectators (always show) -->
        <div>
          <dt class="text-sm text-gray-500">Spectators</dt>
          <dd class="font-medium text-gray-900">
            {{ config.allowSpectators ? 'Welcome' : 'Not allowed' }}
          </dd>
        </div>
      </dl>
    </div>
  </div>
</template>
