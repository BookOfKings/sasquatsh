<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventConfig } from '@/types/pokemon'
import { TOURNAMENT_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  config: PokemonEventConfig
}>()

// Check if we have entry/prizes info to show
const hasInfo = computed(() => {
  return (props.config.entryFee !== null && props.config.entryFee !== undefined && props.config.entryFee > 0) ||
         props.config.hasPrizes ||
         showChampionshipPoints.value
})

// Show Championship Points for official tournament events
const showChampionshipPoints = computed(() =>
  props.config.usePlayPoints && TOURNAMENT_EVENT_TYPES.includes(props.config.eventType)
)
</script>

<template>
  <div v-if="hasInfo" class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20.2,2H19.5H18C17.1,2 16,3 16,4H8C8,3 6.9,2 6,2H4.5H3.8H2V11C2,12 3,13 4,13H6.2C6.4,14 7,15.7 8.2,16.6C9.7,17.8 11.3,18 12,18C12.7,18 14.3,17.8 15.8,16.6C17,15.7 17.6,14 17.8,13H20C21,13 22,12 22,11V2H20.2M4,11V4H6V6V11C5.1,11 4.3,11 4,11M20,11C19.7,11 18.9,11 18,11V6V4H20V11M12,16C11.6,16 10.4,15.9 9.4,15.1C8.3,14.2 8,12.5 8,12V8H16V12C16,12.5 15.7,14.2 14.6,15.1C13.6,15.9 12.4,16 12,16M12,19A2,2 0 0,0 10,21V22H14V21A2,2 0 0,0 12,19Z"/>
        </svg>
        Entry & Prizes
      </h3>
    </div>

    <div class="p-4">
      <div class="flex flex-wrap gap-6">
        <!-- Entry Fee -->
        <div v-if="config.entryFee !== null && config.entryFee !== undefined && config.entryFee > 0" class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-green-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-green-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7,15H9C9,16.08 10.37,17 12,17C13.63,17 15,16.08 15,15C15,13.9 13.96,13.5 11.76,12.97C9.64,12.44 7,11.78 7,9C7,7.21 8.47,5.69 10.5,5.18V3H13.5V5.18C15.53,5.69 17,7.21 17,9H15C15,7.92 13.63,7 12,7C10.37,7 9,7.92 9,9C9,10.1 10.04,10.5 12.24,11.03C14.36,11.56 17,12.22 17,15C17,16.79 15.53,18.31 13.5,18.82V21H10.5V18.82C8.47,18.31 7,16.79 7,15Z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">Entry Fee</p>
            <p class="text-xl font-bold text-gray-900">${{ config.entryFee.toFixed(2) }}</p>
          </div>
        </div>

        <!-- Prize Support -->
        <div v-if="config.hasPrizes" class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-amber-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-amber-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z"/>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">Prize Support</p>
            <p class="font-semibold text-gray-900">Prizes Available</p>
          </div>
        </div>

        <!-- Championship Points -->
        <div v-if="showChampionshipPoints" class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-lg bg-red-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-red-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,16L3,5L8.5,10L12,4L15.5,10L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z" />
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-500">Play! Pokemon</p>
            <p class="font-semibold text-gray-900">Championship Points</p>
          </div>
        </div>
      </div>

      <!-- Prize Structure Details -->
      <div v-if="config.hasPrizes && config.prizeStructure" class="mt-4 bg-amber-50 border border-amber-200 rounded-lg p-3">
        <p class="text-sm font-medium text-amber-800 mb-1">Prize Structure</p>
        <p class="text-sm text-amber-700 whitespace-pre-wrap">{{ config.prizeStructure }}</p>
      </div>

      <!-- Disclaimer -->
      <p class="text-xs text-gray-500 mt-4">
        <svg class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
        </svg>
        Entry fees and prizes are collected at the event. Sasquatsh does not process payments.
      </p>
    </div>
  </div>
</template>
