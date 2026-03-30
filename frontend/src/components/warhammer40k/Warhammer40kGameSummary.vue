<script setup lang="ts">
import { computed } from 'vue'
import type { Warhammer40kEventConfig } from '@/types/warhammer40k'
import {
  GAME_TYPE_LABELS,
  GAME_TYPE_DESCRIPTIONS,
  PLAYER_MODE_LABELS,
  isTournamentEventType,
  TOURNAMENT_STYLE_LABELS,
  getTableSize,
  getMissionPack,
} from '@/types/warhammer40k'

const props = defineProps<{
  config: Warhammer40kEventConfig
}>()

const gameTypeLabel = computed(() =>
  GAME_TYPE_LABELS[props.config.gameType] ?? props.config.gameType
)

const gameTypeDescription = computed(() =>
  GAME_TYPE_DESCRIPTIONS[props.config.gameType] ?? null
)

const pointsDisplay = computed(() =>
  `${props.config.pointsLimit.toLocaleString()}pts`
)

const playerModeDisplay = computed(() =>
  PLAYER_MODE_LABELS[props.config.playerMode] ?? props.config.playerMode
)

const tableSizeDisplay = computed(() => {
  const size = getTableSize(props.config.tableSize)
  return size?.label ?? props.config.tableSize
})

const isTournament = computed(() =>
  isTournamentEventType(props.config.eventType)
)

const missionPack = computed(() =>
  getMissionPack(props.config.missionPack)
)

const tournamentPills = computed(() => {
  const pills: string[] = []
  if (props.config.tournamentStyle) {
    pills.push(TOURNAMENT_STYLE_LABELS[props.config.tournamentStyle])
  }
  if (props.config.roundsCount) {
    pills.push(`${props.config.roundsCount} rounds`)
  }
  return pills
})
</script>

<template>
  <div class="bg-gradient-to-r from-gray-50 to-red-50 rounded-lg border border-red-100 p-4">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center gap-3 mb-3">
      <div class="flex-shrink-0 w-12 h-12 rounded-lg flex items-center justify-center overflow-hidden">
        <img src="/icons/warhammer40k-logo.png" alt="Warhammer 40,000" class="w-12 h-12 object-contain" />
      </div>

      <div class="flex-1">
        <p class="text-xs font-semibold text-red-600 uppercase tracking-wide">Warhammer 40,000</p>
        <h3 class="text-xl font-bold text-red-900">{{ gameTypeLabel }}</h3>
        <p v-if="gameTypeDescription" class="text-sm text-red-700">{{ gameTypeDescription }}</p>
      </div>
    </div>

    <!-- Pills Row -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium">
        {{ pointsDisplay }}
      </span>
      <span class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium">
        {{ playerModeDisplay }}
      </span>
      <span class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium">
        {{ tableSizeDisplay }}
      </span>
      <span
        v-if="isTournament"
        class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium"
      >
        {{ config.eventType === 'tournament' ? 'Tournament' : 'League' }}
      </span>
      <span
        v-if="missionPack"
        class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium"
      >
        {{ missionPack.name }}
      </span>
    </div>

    <!-- Tournament Details -->
    <div v-if="isTournament && tournamentPills.length > 0" class="flex flex-wrap gap-2">
      <span
        v-for="pill in tournamentPills"
        :key="pill"
        class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-xs font-medium"
      >
        {{ pill }}
      </span>
    </div>
  </div>
</template>
