<script setup lang="ts">
import { computed } from 'vue'
import type { YugiohFormat, YugiohEventType } from '@/types/yugioh'
import { OFFICIAL_EVENT_TYPES } from '@/types/yugioh'

const props = defineProps<{
  selectedFormat: YugiohFormat | null
  eventType: YugiohEventType | null
  allowSideDeck: boolean
}>()

// Is this an official event?
const isOfficialEvent = computed(() =>
  props.eventType ? OFFICIAL_EVENT_TYPES.includes(props.eventType) : false
)

// Build the items list based on format and event type
const items = computed(() => {
  const list = []

  // Main deck - always required
  if (props.selectedFormat) {
    list.push({
      name: `Main Deck (${props.selectedFormat.mainDeckMin}-${props.selectedFormat.mainDeckMax} cards)`,
      required: true,
      icon: 'M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19Z',
    })
  } else {
    list.push({
      name: 'Main Deck (40-60 cards)',
      required: true,
      icon: 'M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19Z',
    })
  }

  // Extra Deck
  const extraMax = props.selectedFormat?.extraDeckMax ?? 15
  list.push({
    name: `Extra Deck (up to ${extraMax} cards)`,
    required: false,
    icon: 'M12,2L2,7V17L12,22L22,17V7L12,2M12,4.15L19.85,8L12,11.85L4.15,8L12,4.15Z',
  })

  // Side Deck (if allowed)
  if (props.allowSideDeck) {
    const sideMax = props.selectedFormat?.sideDeckMax ?? 15
    list.push({
      name: `Side Deck (up to ${sideMax} cards)`,
      required: isOfficialEvent.value,
      icon: 'M3,5H21V7H3V5M3,9H21V11H3V9M3,13H21V15H3V13M3,17H21V19H3V17Z',
    })
  }

  // Sleeves
  list.push({
    name: 'Card sleeves',
    required: isOfficialEvent.value,
    icon: 'M12,3A9,9 0 0,0 3,12H0L4,16L8,12H5A7,7 0 0,1 12,5A7,7 0 0,1 19,12A7,7 0 0,1 12,19C10.5,19 9.09,18.5 7.94,17.7L6.5,19.14C8.04,20.3 9.94,21 12,21A9,9 0 0,0 21,12A9,9 0 0,0 12,3Z',
  })

  // Dice/counters
  list.push({
    name: 'Dice / LP counters',
    required: false,
    icon: 'M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z',
  })

  // Playmat (optional)
  list.push({
    name: 'Playmat',
    required: false,
    optional: true,
    icon: 'M21,16H3V4H21M21,2H3C1.89,2 1,2.89 1,4V16A2,2 0 0,0 3,18H10V20H8V22H16V20H14V18H21A2,2 0 0,0 23,16V4C23,2.89 22.1,2 21,2Z',
  })

  // CARD GAME ID for official events
  if (isOfficialEvent.value) {
    list.push({
      name: 'CARD GAME ID',
      required: true,
      icon: 'M4,4H20A2,2 0 0,1 22,6V18A2,2 0 0,1 20,20H4A2,2 0 0,1 2,18V6A2,2 0 0,1 4,4M4,6V18H20V6H4M6,9H18V11H6V9M6,13H16V15H6V13Z',
    })
  }

  return list
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
      <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
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

      <!-- Official event note -->
      <div v-if="isOfficialEvent" class="mt-4 pt-3 border-t border-gray-200">
        <p class="text-xs text-gray-600 flex items-start gap-2">
          <svg class="w-4 h-4 mt-0.5 flex-shrink-0 text-amber-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <span>
            For official tournaments, all cards must be in the same language or have official translations available.
            Sleeves must be identical and in good condition.
          </span>
        </p>
      </div>
    </div>
  </div>
</template>
