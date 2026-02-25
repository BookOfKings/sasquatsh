<script setup lang="ts">
import type { BggGame } from '@/types/bgg'

defineProps<{
  game: BggGame
  removable?: boolean
  primary?: boolean
}>()

const emit = defineEmits<{
  (e: 'remove'): void
  (e: 'setPrimary'): void
}>()

function formatPlayerCount(min: number | null, max: number | null): string {
  if (!min && !max) return ''
  if (min === max) return `${min} players`
  if (!min) return `Up to ${max} players`
  if (!max) return `${min}+ players`
  return `${min}-${max} players`
}

function formatPlaytime(time: number | null): string {
  if (!time) return ''
  if (time < 60) return `${time} min`
  const hours = Math.floor(time / 60)
  const mins = time % 60
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
}

function formatWeight(weight: number | null): string {
  if (!weight) return ''
  if (weight < 2) return 'Light'
  if (weight < 3) return 'Medium Light'
  if (weight < 4) return 'Medium'
  if (weight < 4.5) return 'Medium Heavy'
  return 'Heavy'
}

function getWeightColor(weight: number | null): string {
  if (!weight) return 'bg-gray-100 text-gray-700'
  if (weight < 2) return 'chip-success'
  if (weight < 3) return 'chip bg-green-100 text-green-700'
  if (weight < 4) return 'chip-warning'
  return 'chip-error'
}
</script>

<template>
  <div class="border border-gray-200 rounded-lg p-3 flex gap-3" :class="{ 'ring-2 ring-primary-500': primary }">
    <!-- Thumbnail -->
    <div class="w-16 h-16 rounded bg-gray-100 flex-shrink-0 overflow-hidden">
      <img
        v-if="game.thumbnailUrl"
        :src="game.thumbnailUrl"
        :alt="game.name"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <svg class="w-8 h-8 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
        </svg>
      </div>
    </div>

    <!-- Info -->
    <div class="flex-1 min-w-0">
      <div class="flex items-start justify-between gap-2">
        <div>
          <h4 class="font-medium text-gray-900 truncate">
            {{ game.name }}
            <span v-if="game.yearPublished" class="text-gray-500 font-normal">({{ game.yearPublished }})</span>
          </h4>
          <div class="flex flex-wrap gap-2 mt-1">
            <span v-if="game.minPlayers || game.maxPlayers" class="text-xs text-gray-500">
              <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
              </svg>
              {{ formatPlayerCount(game.minPlayers, game.maxPlayers) }}
            </span>
            <span v-if="game.playingTime" class="text-xs text-gray-500">
              <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
              </svg>
              {{ formatPlaytime(game.playingTime) }}
            </span>
            <span v-if="game.weight" :class="getWeightColor(game.weight)" class="text-xs">
              {{ formatWeight(game.weight) }}
            </span>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex items-center gap-1">
          <button
            v-if="!primary"
            type="button"
            class="p-1 text-gray-400 hover:text-primary-500 transition-colors"
            title="Set as primary game"
            @click="emit('setPrimary')"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
            </svg>
          </button>
          <span v-if="primary" class="chip-primary text-xs">Primary</span>
          <button
            v-if="removable"
            type="button"
            class="p-1 text-gray-400 hover:text-red-500 transition-colors"
            title="Remove game"
            @click="emit('remove')"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
