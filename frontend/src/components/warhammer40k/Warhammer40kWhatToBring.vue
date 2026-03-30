<script setup lang="ts">
import { computed } from 'vue'
import type { Warhammer40kEventConfig } from '@/types/warhammer40k'
import { isTournamentEventType } from '@/types/warhammer40k'

const props = defineProps<{
  config: Warhammer40kEventConfig
}>()

const isTournament = computed(() =>
  isTournamentEventType(props.config.eventType)
)

interface BringItem {
  name: string
  required: boolean
  optional?: boolean
  icon: string
}

const items = computed<BringItem[]>(() => {
  const list: BringItem[] = []

  // Always: Your Army
  list.push({
    name: `Your Army (${props.config.pointsLimit.toLocaleString()}pts)`,
    required: true,
    icon: 'M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1Z',
  })

  // Always: Dice
  list.push({
    name: 'Dice (lots of D6)',
    required: true,
    icon: 'M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z',
  })

  // Always: Measuring tape
  list.push({
    name: 'Measuring tape or range rulers',
    required: true,
    icon: 'M1.39,18.36L3.16,16.6L4.58,18L5.64,16.95L4.22,15.54L5.64,14.12L8.11,16.6L9.17,15.54L6.7,13.06L8.11,11.65L9.53,13.06L10.59,12L9.17,10.59L10.59,9.17L13.06,11.65L14.12,10.59L11.65,8.11L13.06,6.7L14.48,8.11L15.54,7.05L14.12,5.64L15.54,4.22L18,6.7L19.07,5.64L16.6,3.16L18.36,1.39L22.61,5.64L5.64,22.61L1.39,18.36Z',
  })

  // Always: Rulebook
  list.push({
    name: 'Rulebook or app',
    required: true,
    icon: 'M18,2A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2H18M18,4H13V12L10.5,9.75L8,12V4H6V20H18V4Z',
  })

  // Always: Objective markers
  list.push({
    name: 'Objective markers',
    required: true,
    icon: 'M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z',
  })

  // Tournament: Printed army list
  if (isTournament.value) {
    list.push({
      name: 'Printed army list (2 copies)',
      required: true,
      icon: 'M7,6H17V8H7V6M7,10H17V12H7V10M7,14H14V16H7V14M5,2H19A2,2 0 0,1 21,4V20A2,2 0 0,1 19,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2M5,4V20H19V4H5Z',
    })
  }

  // Tournament + time limit: Chess clock
  if (isTournament.value && props.config.timeLimitMinutes) {
    list.push({
      name: 'Chess clock',
      required: true,
      icon: 'M12,20A7,7 0 0,1 5,13A7,7 0 0,1 12,6A7,7 0 0,1 19,13A7,7 0 0,1 12,20M12,4A9,9 0 0,0 3,13A9,9 0 0,0 12,22A9,9 0 0,0 21,13A9,9 0 0,0 12,4M12.5,8H11V14L15.75,16.85L16.5,15.62L12.5,13.25V8M7.88,3.39L6.6,1.86L2,5.71L3.29,7.24L7.88,3.39M22,5.72L17.4,1.86L16.11,3.39L20.71,7.25L22,5.72Z',
    })
  }

  // Bring your own terrain
  if (props.config.terrainType === 'bring_your_own') {
    list.push({
      name: 'Terrain pieces',
      required: true,
      icon: 'M15,9H5V5H15M15,3H5A2,2 0 0,0 3,5V9A2,2 0 0,0 5,11H15A2,2 0 0,0 17,9V5A2,2 0 0,0 15,3M19,19H9V15H19M19,13H9A2,2 0 0,0 7,15V19A2,2 0 0,0 9,21H19A2,2 0 0,0 21,19V15A2,2 0 0,0 19,13Z',
    })
  }

  // Forge World allowed
  if (props.config.forgeWorldAllowed) {
    list.push({
      name: 'Forge World indexes/datasheets',
      required: false,
      icon: 'M6,2H18A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2M6,4V20H18V4H6M7,6H17V8H7V6M7,10H17V12H7V10M7,14H14V16H7V14Z',
    })
  }

  // Optional items
  list.push({
    name: 'Tokens and markers',
    required: false,
    optional: true,
    icon: 'M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z',
  })

  list.push({
    name: 'Playmat / game mat',
    required: false,
    optional: true,
    icon: 'M21,16H3V4H21M21,2H3C1.89,2 1,2.89 1,4V16A2,2 0 0,0 3,18H10V20H8V22H16V20H14V18H21A2,2 0 0,0 23,16V4C23,2.89 22.1,2 21,2Z',
  })

  return list
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
      <svg class="w-5 h-5 text-red-600" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M7,7H9V9H7V7M7,11H9V17H7V11M11,7H17V9H11V7M11,11H17V13H11V11M11,15H14V17H11V15Z"/>
      </svg>
      What to Bring
    </h3>

    <div class="bg-gray-50 rounded-lg p-4">
      <ul class="space-y-2">
        <li
          v-for="item in items"
          :key="item.name"
          class="flex items-center gap-3 text-sm"
        >
          <svg
            class="w-4 h-4 flex-shrink-0"
            :class="item.required ? 'text-blue-600' : 'text-gray-400'"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path :d="item.icon" />
          </svg>
          <span :class="item.optional ? 'text-gray-500' : 'text-gray-700'">
            {{ item.name }}
            <span v-if="item.required" class="text-xs text-blue-600 ml-1">(required)</span>
            <span v-else-if="item.optional" class="text-xs text-gray-400 ml-1">(optional)</span>
          </span>
        </li>
      </ul>

      <!-- Tournament note -->
      <div v-if="isTournament" class="mt-4 pt-3 border-t border-gray-200">
        <p class="text-xs text-gray-600 flex items-start gap-2">
          <svg class="w-4 h-4 mt-0.5 flex-shrink-0 text-amber-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <span>
            For tournament games, ensure your army list is printed clearly and includes all unit costs,
            enhancements, and detachment rules. Arrive early to allow time for list verification.
          </span>
        </p>
      </div>
    </div>
  </div>
</template>
