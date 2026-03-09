<script setup lang="ts">
import type { GameSuggestion } from '@/types/planning'

defineProps<{
  suggestion: GameSuggestion
  selectable?: boolean
  selected?: boolean
  removable?: boolean
  votingDisabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'vote'): void
  (e: 'select'): void
  (e: 'remove'): void
}>()
</script>

<template>
  <div
    class="flex items-center gap-4 p-4 rounded-lg border transition-colors"
    :class="{
      'border-primary-500 bg-primary-50': selected,
      'border-gray-200 hover:border-gray-300': !selected,
      'cursor-pointer': selectable,
    }"
    @click="selectable && emit('select')"
  >
    <!-- Thumbnail -->
    <div class="w-16 h-16 rounded-lg bg-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0">
      <img
        v-if="suggestion.thumbnailUrl"
        :src="suggestion.thumbnailUrl"
        class="w-full h-full object-cover"
      />
      <svg v-else class="w-8 h-8 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
        <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
      </svg>
    </div>

    <!-- Info -->
    <div class="flex-1 min-w-0">
      <div class="flex items-center gap-2">
        <span class="font-medium">{{ suggestion.gameName }}</span>
        <!-- "Will fire" indicator for games with 2+ interested -->
        <span
          v-if="suggestion.voteCount >= 2"
          class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700"
        >
          <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.66,11.2C17.43,10.9 17.15,10.64 16.89,10.38C16.22,9.78 15.46,9.35 14.82,8.72C13.33,7.26 13,4.85 13.95,3C13,3.23 12.17,3.75 11.46,4.32C8.87,6.4 7.85,10.07 9.07,13.22C9.11,13.32 9.15,13.42 9.15,13.55C9.15,13.77 9,13.97 8.8,14.05C8.57,14.15 8.33,14.09 8.14,13.93C8.08,13.88 8.04,13.83 8,13.76C6.87,12.33 6.69,10.28 7.45,8.64C5.78,10 4.87,12.3 5,14.47C5.06,14.97 5.12,15.47 5.29,15.97C5.43,16.57 5.7,17.17 6,17.7C7.08,19.43 8.95,20.67 10.96,20.92C13.1,21.19 15.39,20.8 17.03,19.32C18.86,17.66 19.5,15 18.56,12.72L18.43,12.46C18.22,12 17.66,11.2 17.66,11.2M14.5,17.5C14.22,17.74 13.76,18 13.4,18.1C12.28,18.5 11.16,17.94 10.5,17.28C11.69,17 12.4,16.12 12.61,15.23C12.78,14.43 12.46,13.77 12.33,13C12.21,12.26 12.23,11.63 12.5,10.94C12.69,11.32 12.89,11.7 13.13,12C13.9,13 15.11,13.44 15.37,14.8C15.41,14.94 15.43,15.08 15.43,15.23C15.46,16.05 15.1,16.95 14.5,17.5H14.5Z"/>
          </svg>
          Will fire!
        </span>
      </div>
      <div class="flex items-center gap-3 text-sm text-gray-500 mt-1">
        <span v-if="suggestion.minPlayers && suggestion.maxPlayers" class="flex items-center gap-1">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
          </svg>
          {{ suggestion.minPlayers }}-{{ suggestion.maxPlayers }}
        </span>
        <span v-if="suggestion.playingTime" class="flex items-center gap-1">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
          </svg>
          {{ suggestion.playingTime }} min
        </span>
      </div>
      <div v-if="suggestion.suggestedBy" class="text-xs text-gray-400 mt-1">
        Suggested by {{ suggestion.suggestedBy.displayName || 'Unknown' }}
      </div>
    </div>

    <!-- Selection indicator -->
    <div v-if="selectable" class="flex-shrink-0">
      <div
        class="w-6 h-6 rounded-full border-2 flex items-center justify-center"
        :class="selected ? 'border-primary-500 bg-primary-500' : 'border-gray-300'"
      >
        <svg v-if="selected" class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
        </svg>
      </div>
    </div>

    <!-- Vote Button -->
    <button
      class="flex items-center gap-2 px-3 py-2 rounded-lg transition-colors flex-shrink-0"
      :class="votingDisabled
        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
        : suggestion.hasVoted
          ? 'bg-primary-100 text-primary-700 hover:bg-primary-200'
          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
      :disabled="votingDisabled"
      :title="votingDisabled ? 'You need a participation slot to vote' : ''"
      @click.stop="!votingDisabled && emit('vote')"
    >
      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
        <path v-if="suggestion.hasVoted" d="M23,10C23,8.89 22.1,8 21,8H14.68L15.64,3.43C15.66,3.33 15.67,3.22 15.67,3.11C15.67,2.7 15.5,2.32 15.23,2.05L14.17,1L7.59,7.58C7.22,7.95 7,8.45 7,9V19A2,2 0 0,0 9,21H18C18.83,21 19.54,20.5 19.84,19.78L22.86,12.73C22.95,12.5 23,12.26 23,12V10M1,21H5V9H1V21Z"/>
        <path v-else d="M5,9V21H1V9H5M9,21A2,2 0 0,1 7,19V9C7,8.45 7.22,7.95 7.59,7.59L14.17,1L15.23,2.06C15.5,2.33 15.67,2.7 15.67,3.11L15.64,3.43L14.69,8H21C22.11,8 23,8.9 23,10V12C23,12.26 22.95,12.5 22.86,12.73L19.84,19.78C19.54,20.5 18.83,21 18,21H9M9,19H18.03L21,12V10H12.21L13.34,4.68L9,9.03V19Z"/>
      </svg>
      <span class="font-medium">{{ suggestion.voteCount }}</span>
    </button>

    <!-- Remove Button -->
    <button
      v-if="removable"
      class="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors flex-shrink-0"
      title="Remove suggestion"
      @click.stop="emit('remove')"
    >
      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
      </svg>
    </button>
  </div>
</template>
