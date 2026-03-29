<script setup lang="ts">
import { computed, watch } from 'vue'
import type { PowerLevelRange } from '@/types/mtg'
import { POWER_LEVEL_RANGES } from '@/types/mtg'

const props = defineProps<{
  powerLevelRange: PowerLevelRange | null
  powerLevelMin: number | null
  powerLevelMax: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:powerLevelRange', value: PowerLevelRange | null): void
  (e: 'update:powerLevelMin', value: number | null): void
  (e: 'update:powerLevelMax', value: number | null): void
}>()

const powerLevelOptions: { value: PowerLevelRange; label: string; description: string; color: string; icon: string }[] = [
  { value: 'casual', label: 'Casual', description: 'Precons & jank (1-4)', color: 'green', icon: 'M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,7A5,5 0 0,0 7,12A5,5 0 0,0 12,17A5,5 0 0,0 17,12A5,5 0 0,0 12,7M12,15A3,3 0 0,1 9,12A3,3 0 0,1 12,9A3,3 0 0,1 15,12A3,3 0 0,1 12,15Z' }, // flower
  { value: 'mid', label: 'Mid', description: 'Balanced play (5-6)', color: 'blue', icon: 'M12,3C10.73,3 9.6,3.8 9.18,5H6V7H9.18C9.6,8.2 10.73,9 12,9C13.27,9 14.4,8.2 14.82,7H18V5H14.82C14.4,3.8 13.27,3 12,3M12,5A1,1 0 0,1 13,6A1,1 0 0,1 12,7A1,1 0 0,1 11,6A1,1 0 0,1 12,5M6,11V13H9.18C9.6,14.2 10.73,15 12,15C13.27,15 14.4,14.2 14.82,13H18V11H14.82C14.4,9.8 13.27,9 12,9C10.73,9 9.6,9.8 9.18,11H6M12,11A1,1 0 0,1 13,12A1,1 0 0,1 12,13A1,1 0 0,1 11,12A1,1 0 0,1 12,11M6,17V19H9.18C9.6,20.2 10.73,21 12,21C13.27,21 14.4,20.2 14.82,19H18V17H14.82C14.4,15.8 13.27,15 12,15C10.73,15 9.6,15.8 9.18,17H6M12,17A1,1 0 0,1 13,18A1,1 0 0,1 12,19A1,1 0 0,1 11,18A1,1 0 0,1 12,17Z' }, // tune/sliders
  { value: 'high', label: 'High Power', description: 'Optimized (7-8)', color: 'amber', icon: 'M11,21H5V19H11V21M15.5,9.5L12,6L8.5,9.5L9.91,10.91L11,9.82V16H13V9.82L14.09,10.91L15.5,9.5M19,3H5C3.89,3 3,3.89 3,5V19C3,20.11 3.89,21 5,21H9V19H5V5H19V19H15V21H19C20.11,21 21,20.11 21,19V5C21,3.89 20.11,3 19,3Z' }, // trending up
  { value: 'cedh', label: 'cEDH', description: 'Competitive (9-10)', color: 'red', icon: 'M18,2C17.1,2 16,3 16,4H8C8,3 6.9,2 6,2C4.9,2 4,2.9 4,4C4,5.1 4.9,6 6,6C6.2,6 6.3,6 6.5,5.9L7.4,10.2C6.6,11 6,12.2 6,13.5C6,16.5 8.5,19 11.5,19L11,22H13L12.5,19C15.5,19 18,16.5 18,13.5C18,12.2 17.4,11 16.6,10.2L17.5,5.9C17.7,6 17.8,6 18,6C19.1,6 20,5.1 20,4C20,2.9 19.1,2 18,2M12,17A3.5,3.5 0 0,1 8.5,13.5C8.5,11.6 10.1,10 12,10C13.9,10 15.5,11.6 15.5,13.5A3.5,3.5 0 0,1 12,17Z' }, // trophy
  { value: 'custom', label: 'Custom', description: 'Set your range', color: 'gray', icon: 'M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.34 19.43,11L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.96 19.05,5.05L16.56,6.05C16.04,5.66 15.5,5.32 14.87,5.07L14.5,2.42C14.46,2.18 14.25,2 14,2H10C9.75,2 9.54,2.18 9.5,2.42L9.13,5.07C8.5,5.32 7.96,5.66 7.44,6.05L4.95,5.05C4.73,4.96 4.46,5.05 4.34,5.27L2.34,8.73C2.21,8.95 2.27,9.22 2.46,9.37L4.57,11C4.53,11.34 4.5,11.67 4.5,12C4.5,12.33 4.53,12.65 4.57,12.97L2.46,14.63C2.27,14.78 2.21,15.05 2.34,15.27L4.34,18.73C4.46,18.95 4.73,19.03 4.95,18.95L7.44,17.94C7.96,18.34 8.5,18.68 9.13,18.93L9.5,21.58C9.54,21.82 9.75,22 10,22H14C14.25,22 14.46,21.82 14.5,21.58L14.87,18.93C15.5,18.67 16.04,18.34 16.56,17.94L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z' }, // cog/settings
]

const isCustomRange = computed(() => props.powerLevelRange === 'custom')

const currentRangeInfo = computed(() => {
  if (!props.powerLevelRange) return null
  return POWER_LEVEL_RANGES[props.powerLevelRange]
})

function selectRange(range: PowerLevelRange) {
  emit('update:powerLevelRange', range)

  // Auto-set min/max based on selected range
  const rangeInfo = POWER_LEVEL_RANGES[range]
  emit('update:powerLevelMin', rangeInfo.min)
  emit('update:powerLevelMax', rangeInfo.max)
}

function getColorClasses(option: typeof powerLevelOptions[0], isSelected: boolean): string {
  if (isSelected) {
    switch (option.color) {
      case 'green': return 'bg-green-600 text-white border-green-600'
      case 'blue': return 'bg-blue-600 text-white border-blue-600'
      case 'amber': return 'bg-amber-600 text-white border-amber-600'
      case 'red': return 'bg-red-600 text-white border-red-600'
      default: return 'bg-gray-600 text-white border-gray-600'
    }
  }
  switch (option.color) {
    case 'green': return 'border-green-300 hover:border-green-400 hover:bg-green-50'
    case 'blue': return 'border-blue-300 hover:border-blue-400 hover:bg-blue-50'
    case 'amber': return 'border-amber-300 hover:border-amber-400 hover:bg-amber-50'
    case 'red': return 'border-red-300 hover:border-red-400 hover:bg-red-50'
    default: return 'border-gray-300 hover:border-gray-400 hover:bg-gray-50'
  }
}

// Validate custom range
watch(() => [props.powerLevelMin, props.powerLevelMax] as const, ([min, max]) => {
  if (min !== null && max !== null && min > max) {
    emit('update:powerLevelMin', max)
    emit('update:powerLevelMax', min)
  }
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center gap-2">
      <h3 class="text-lg font-semibold text-gray-900">Power Level</h3>
      <span class="text-xs text-gray-500">(Commander/Casual)</span>
    </div>

    <p class="text-sm text-gray-600">
      Help players bring appropriately matched decks by setting a power level expectation.
    </p>

    <!-- Power level selection -->
    <div class="grid grid-cols-2 sm:grid-cols-5 gap-2">
      <button
        v-for="option in powerLevelOptions"
        :key="option.value"
        type="button"
        :disabled="disabled"
        class="flex flex-col items-center p-3 rounded-lg border transition-all duration-150"
        :class="[
          getColorClasses(option, powerLevelRange === option.value),
          powerLevelRange === option.value ? 'shadow-sm' : 'bg-white',
          disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
        ]"
        @click="selectRange(option.value)"
      >
        <svg class="w-6 h-6 mb-1" viewBox="0 0 24 24" fill="currentColor">
          <path :d="option.icon" />
        </svg>
        <span class="font-medium text-sm">{{ option.label }}</span>
        <span
          class="text-xs mt-1"
          :class="powerLevelRange === option.value ? 'opacity-90' : 'text-gray-500'"
        >
          {{ option.description }}
        </span>
      </button>
    </div>

    <!-- Custom range inputs -->
    <div v-if="isCustomRange" class="bg-gray-50 rounded-lg p-4 space-y-3">
      <div class="flex items-center gap-4">
        <div class="flex-1">
          <label class="block text-sm font-medium text-gray-700 mb-1">Min Power</label>
          <select
            :value="powerLevelMin ?? ''"
            :disabled="disabled"
            class="input"
            @change="$emit('update:powerLevelMin', parseInt(($event.target as HTMLSelectElement).value) || null)"
          >
            <option value="">Any</option>
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div class="flex-1">
          <label class="block text-sm font-medium text-gray-700 mb-1">Max Power</label>
          <select
            :value="powerLevelMax ?? ''"
            :disabled="disabled"
            class="input"
            @change="$emit('update:powerLevelMax', parseInt(($event.target as HTMLSelectElement).value) || null)"
          >
            <option value="">Any</option>
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>
      <p class="text-xs text-gray-500">
        1 = Precon level, 10 = Fully competitive cEDH
      </p>
    </div>

    <!-- Selected range summary -->
    <div v-if="currentRangeInfo && !isCustomRange" class="bg-gray-50 rounded-lg p-3">
      <p class="text-sm text-gray-700">
        <span class="font-medium">{{ currentRangeInfo.label }}:</span>
        {{ currentRangeInfo.description }}
      </p>
    </div>
  </div>
</template>
