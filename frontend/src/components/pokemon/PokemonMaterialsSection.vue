<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventType } from '@/types/pokemon'
import { LIMITED_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  eventType: PokemonEventType
  providesBasicEnergy: boolean
  providesDamageCounters: boolean
  sleevesRecommended: boolean
  providesBuildBattleKits: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:providesBasicEnergy', value: boolean): void
  (e: 'update:providesDamageCounters', value: boolean): void
  (e: 'update:sleevesRecommended', value: boolean): void
  (e: 'update:providesBuildBattleKits', value: boolean): void
}>()

// Is this a limited event (prerelease/draft)?
const isLimitedEvent = computed(() =>
  LIMITED_EVENT_TYPES.includes(props.eventType)
)

// Is this specifically a prerelease?
const isPrerelease = computed(() =>
  props.eventType === 'prerelease'
)
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Event Materials</h3>
    <p class="text-sm text-gray-500">
      Let players know what materials will be available at the event.
    </p>

    <div class="space-y-3">
      <!-- Build & Battle Kits (Prerelease only) -->
      <label v-if="isPrerelease" class="flex items-start gap-3 cursor-pointer p-3 rounded-lg border border-gray-200 hover:border-yellow-300 transition-colors">
        <input
          type="checkbox"
          :checked="providesBuildBattleKits"
          :disabled="disabled"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:providesBuildBattleKits', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <svg class="w-4 h-4 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5V9H9V5H7M11,5V11H13V5H11M15,5V7H17V5H15M7,11V13H9V11H7M11,13V15H13V13H11M15,9V13H17V9H15M7,15V19H9V15H7M11,17V19H13V17H11M15,15V19H17V15H15Z" />
            </svg>
            Build & Battle Kits Provided
          </span>
          <p class="text-xs text-gray-500 mt-1">
            Prerelease kits will be provided to each player (included in entry fee)
          </p>
        </div>
      </label>

      <!-- Basic Energy -->
      <label class="flex items-start gap-3 cursor-pointer p-3 rounded-lg border border-gray-200 hover:border-yellow-300 transition-colors">
        <input
          type="checkbox"
          :checked="providesBasicEnergy"
          :disabled="disabled"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:providesBasicEnergy', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <svg class="w-4 h-4 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M11.5,2C9.56,2 8,3.56 8,5.5V8H6A2,2 0 0,0 4,10V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V10A2,2 0 0,0 18,8H16V5.5C16,3.56 14.44,2 12.5,2H11.5M11.5,4H12.5A1.5,1.5 0 0,1 14,5.5V8H10V5.5A1.5,1.5 0 0,1 11.5,4M12,10A4,4 0 0,1 16,14A4,4 0 0,1 12,18A4,4 0 0,1 8,14A4,4 0 0,1 12,10M12,12A2,2 0 0,0 10,14A2,2 0 0,0 12,16A2,2 0 0,0 14,14A2,2 0 0,0 12,12Z" />
            </svg>
            Basic Energy Provided
          </span>
          <p class="text-xs text-gray-500 mt-1">
            <template v-if="isLimitedEvent">
              Basic Energy cards will be available for deck building
            </template>
            <template v-else>
              Basic Energy cards available if players need them
            </template>
          </p>
        </div>
      </label>

      <!-- Damage Counters / Dice -->
      <label class="flex items-start gap-3 cursor-pointer p-3 rounded-lg border border-gray-200 hover:border-yellow-300 transition-colors">
        <input
          type="checkbox"
          :checked="providesDamageCounters"
          :disabled="disabled"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:providesDamageCounters', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <svg class="w-4 h-4 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z" />
            </svg>
            Damage Counters &amp; Dice Available
          </span>
          <p class="text-xs text-gray-500 mt-1">
            Damage counters, dice, and other play accessories provided
          </p>
        </div>
      </label>

      <!-- Sleeves Recommended -->
      <label class="flex items-start gap-3 cursor-pointer p-3 rounded-lg border border-gray-200 hover:border-yellow-300 transition-colors">
        <input
          type="checkbox"
          :checked="sleevesRecommended"
          :disabled="disabled"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:sleevesRecommended', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700 flex items-center gap-2">
            <svg class="w-4 h-4 text-yellow-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,3H14.82C14.25,1.44 12.53,0.64 11,1.2C10.14,1.5 9.5,2.16 9.18,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M12,3A1,1 0 0,1 13,4A1,1 0 0,1 12,5A1,1 0 0,1 11,4A1,1 0 0,1 12,3M7,7H17V5H19V19H5V5H7V7Z" />
            </svg>
            Sleeves Recommended
          </span>
          <p class="text-xs text-gray-500 mt-1">
            Players should bring card sleeves to protect their cards
          </p>
        </div>
      </label>
    </div>

    <!-- Info box for limited events -->
    <div v-if="isLimitedEvent" class="bg-blue-50 border border-blue-100 rounded-lg p-3">
      <p class="text-sm text-blue-800">
        <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
        </svg>
        For limited events, make sure to have enough Basic Energy on hand. Players typically need 10-15 of each type.
      </p>
    </div>
  </div>
</template>
