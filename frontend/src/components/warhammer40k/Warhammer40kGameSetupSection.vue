<script setup lang="ts">
import { computed } from 'vue'
import type { Warhammer40kGameType, Warhammer40kPlayerMode } from '@/types/warhammer40k'
import {
  GAME_TYPE_LABELS,
  GAME_TYPE_DESCRIPTIONS,
  POINTS_PRESETS,
  PLAYER_MODE_LABELS,
} from '@/types/warhammer40k'

const props = defineProps<{
  gameType: Warhammer40kGameType
  pointsLimit: number
  playerMode: Warhammer40kPlayerMode
  hasError?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:gameType', value: Warhammer40kGameType): void
  (e: 'update:pointsLimit', value: number): void
  (e: 'update:playerMode', value: Warhammer40kPlayerMode): void
}>()

const gameTypes = Object.keys(GAME_TYPE_LABELS) as Warhammer40kGameType[]
const playerModes = Object.keys(PLAYER_MODE_LABELS) as Warhammer40kPlayerMode[]

const isCustomPoints = computed(() => !POINTS_PRESETS.includes(props.pointsLimit))

function handlePresetClick(preset: number) {
  emit('update:pointsLimit', preset)
}

function handleCustomPointsInput(event: Event) {
  const value = parseInt((event.target as HTMLInputElement).value)
  if (value > 0) {
    emit('update:pointsLimit', value)
  }
}
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Game Setup</h3>

    <!-- Game Type -->
    <div class="space-y-3">
      <label class="block text-sm font-medium" :class="hasError ? 'text-red-600' : 'text-gray-700'">
        Game Type <span class="text-red-500">*</span>
      </label>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          v-for="type in gameTypes"
          :key="type"
          type="button"
          class="px-4 py-3 rounded-lg text-left border transition-all duration-150"
          :class="[
            gameType === type
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:gameType', type)"
        >
          <span class="block text-sm font-medium">{{ GAME_TYPE_LABELS[type] }}</span>
          <span
            class="block text-xs mt-0.5"
            :class="gameType === type ? 'text-red-100' : 'text-gray-500'"
          >
            {{ GAME_TYPE_DESCRIPTIONS[type] }}
          </span>
        </button>
      </div>
    </div>

    <!-- Points Limit -->
    <div class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Points Limit</label>

      <div class="flex flex-wrap gap-2">
        <button
          v-for="preset in POINTS_PRESETS"
          :key="preset"
          type="button"
          class="px-4 py-2 rounded-lg text-sm font-medium border transition-all duration-150"
          :class="[
            pointsLimit === preset
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="handlePresetClick(preset)"
        >
          {{ preset }}pts
        </button>
      </div>

      <div class="flex items-center gap-3">
        <label class="text-sm text-gray-600">Custom:</label>
        <input
          type="number"
          :value="isCustomPoints ? pointsLimit : ''"
          class="input w-32"
          min="1"
          step="50"
          placeholder="e.g. 750"
          @input="handleCustomPointsInput"
        />
        <span class="text-sm text-gray-500">points</span>
      </div>
    </div>

    <!-- Player Mode -->
    <div class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Player Mode</label>

      <div class="flex flex-wrap gap-2">
        <button
          v-for="mode in playerModes"
          :key="mode"
          type="button"
          class="px-4 py-2 rounded-lg text-sm font-medium border transition-all duration-150"
          :class="[
            playerMode === mode
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:playerMode', mode)"
        >
          {{ PLAYER_MODE_LABELS[mode] }}
        </button>
      </div>
    </div>
  </div>
</template>
