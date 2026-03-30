<script setup lang="ts">
import { computed } from 'vue'
import type { Warhammer40kEventConfig } from '@/types/warhammer40k'
import {
  EVENT_TYPE_LABELS,
  GAME_TYPE_LABELS,
  PLAYER_MODE_LABELS,
  TOURNAMENT_STYLE_LABELS,
  TERRAIN_TYPE_LABELS,
  isTournamentEventType,
  getTableSize,
  getMissionPack,
} from '@/types/warhammer40k'

const props = defineProps<{
  config: Warhammer40kEventConfig
}>()

const isTournament = computed(() =>
  isTournamentEventType(props.config.eventType)
)

const tableSizeDisplay = computed(() => {
  const size = getTableSize(props.config.tableSize)
  return size?.label ?? props.config.tableSize
})

const missionPackDisplay = computed(() => {
  const pack = getMissionPack(props.config.missionPack)
  return pack?.name ?? null
})
</script>

<template>
  <div class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
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
          <dd class="font-medium text-gray-900">{{ EVENT_TYPE_LABELS[config.eventType] ?? config.eventType }}</dd>
        </div>

        <!-- Game Type -->
        <div>
          <dt class="text-sm text-gray-500">Game Type</dt>
          <dd class="font-medium text-gray-900">{{ GAME_TYPE_LABELS[config.gameType] ?? config.gameType }}</dd>
        </div>

        <!-- Points Limit -->
        <div>
          <dt class="text-sm text-gray-500">Points Limit</dt>
          <dd class="font-medium text-gray-900">{{ config.pointsLimit.toLocaleString() }} pts</dd>
        </div>

        <!-- Player Mode -->
        <div>
          <dt class="text-sm text-gray-500">Player Mode</dt>
          <dd class="font-medium text-gray-900">{{ PLAYER_MODE_LABELS[config.playerMode] ?? config.playerMode }}</dd>
        </div>

        <!-- Tournament Style -->
        <div v-if="isTournament && config.tournamentStyle">
          <dt class="text-sm text-gray-500">Tournament Style</dt>
          <dd class="font-medium text-gray-900">{{ TOURNAMENT_STYLE_LABELS[config.tournamentStyle] }}</dd>
        </div>

        <!-- Rounds -->
        <div v-if="isTournament && config.roundsCount">
          <dt class="text-sm text-gray-500">Rounds</dt>
          <dd class="font-medium text-gray-900">{{ config.roundsCount }} rounds</dd>
        </div>

        <!-- Time Limit -->
        <div v-if="config.timeLimitMinutes">
          <dt class="text-sm text-gray-500">Time Limit</dt>
          <dd class="font-medium text-gray-900">{{ config.timeLimitMinutes }} minutes</dd>
        </div>

        <!-- Table Size -->
        <div>
          <dt class="text-sm text-gray-500">Table Size</dt>
          <dd class="font-medium text-gray-900">{{ tableSizeDisplay }}</dd>
        </div>

        <!-- Terrain -->
        <div>
          <dt class="text-sm text-gray-500">Terrain</dt>
          <dd class="font-medium text-gray-900">{{ TERRAIN_TYPE_LABELS[config.terrainType] ?? config.terrainType }}</dd>
        </div>

        <!-- Mission Pack -->
        <div v-if="missionPackDisplay">
          <dt class="text-sm text-gray-500">Mission Pack</dt>
          <dd class="font-medium text-gray-900">{{ missionPackDisplay }}</dd>
        </div>
      </dl>
    </div>
  </div>
</template>
