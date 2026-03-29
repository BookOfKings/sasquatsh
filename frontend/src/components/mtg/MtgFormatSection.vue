<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import FormatSelector from './FormatSelector.vue'
import PowerLevelSelector from './PowerLevelSelector.vue'
import type { MtgFormat } from '@/types/mtg'

const props = defineProps<{
  formatId: string | null
  customFormatName: string | null
  powerLevelMin: number | null
  powerLevelMax: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:formatId', value: string | null): void
  (e: 'update:customFormatName', value: string | null): void
  (e: 'update:powerLevelMin', value: number | null): void
  (e: 'update:powerLevelMax', value: number | null): void
}>()

const selectedFormat = ref<MtgFormat | null>(null)

const isCustomFormat = computed(() => props.formatId === 'custom')
const isCommanderFormat = computed(() =>
  selectedFormat.value?.hasCommander ||
  props.formatId === 'commander' ||
  props.formatId === 'oathbreaker' ||
  props.formatId === 'brawl'
)
const isLimitedFormat = computed(() => selectedFormat.value && !selectedFormat.value.isConstructed)

function handleFormatChange(format: MtgFormat | null) {
  selectedFormat.value = format
  // Clear custom format name if switching away from custom
  if (format && format.id !== 'custom') {
    emit('update:customFormatName', null)
  }
}

// Validate power level range
watch(() => [props.powerLevelMin, props.powerLevelMax] as const, ([min, max]) => {
  if (min !== null && max !== null && typeof min === 'number' && typeof max === 'number' && min > max) {
    // Swap if min > max
    emit('update:powerLevelMin', max)
    emit('update:powerLevelMax', min)
  }
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Format & Power Level</h3>

    <!-- Format Selection -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Format <span class="text-red-500">*</span>
      </label>
      <FormatSelector
        :model-value="formatId"
        :disabled="disabled"
        show-description
        @update:model-value="$emit('update:formatId', $event)"
        @change="handleFormatChange"
      />
    </div>

    <!-- Custom Format Name (only when custom is selected) -->
    <div v-if="isCustomFormat">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Custom Format Name <span class="text-red-500">*</span>
      </label>
      <input
        type="text"
        :value="customFormatName"
        :disabled="disabled"
        class="input"
        placeholder="e.g., Pauper Commander, Canadian Highlander"
        @input="$emit('update:customFormatName', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <!-- Power Level Range (emphasized for Commander) -->
    <div v-if="isCommanderFormat || powerLevelMin !== null || powerLevelMax !== null" class="space-y-3">
      <div class="flex items-center gap-2">
        <label class="block text-sm font-medium text-gray-700">
          Power Level Range
        </label>
        <span v-if="isCommanderFormat" class="text-xs text-amber-600 bg-amber-50 px-2 py-0.5 rounded">
          Recommended for Commander
        </span>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-xs text-gray-500 mb-1">Minimum</label>
          <PowerLevelSelector
            :model-value="powerLevelMin"
            :disabled="disabled"
            @update:model-value="$emit('update:powerLevelMin', $event)"
          />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">Maximum</label>
          <PowerLevelSelector
            :model-value="powerLevelMax"
            :disabled="disabled"
            @update:model-value="$emit('update:powerLevelMax', $event)"
          />
        </div>
      </div>

      <p class="text-xs text-gray-500">
        Setting a power level range helps players bring appropriately matched decks.
      </p>
    </div>

    <!-- Add power level button for non-Commander formats -->
    <button
      v-if="!isCommanderFormat && powerLevelMin === null && powerLevelMax === null"
      type="button"
      class="text-sm text-blue-600 hover:text-blue-700"
      @click="$emit('update:powerLevelMin', 5); $emit('update:powerLevelMax', 7)"
    >
      + Add power level range
    </button>

    <!-- Limited format note -->
    <div v-if="isLimitedFormat" class="bg-green-50 border border-green-200 rounded-lg p-3">
      <p class="text-sm text-green-800">
        This is a limited format. Additional draft options are available in the Draft Settings section below.
      </p>
    </div>
  </div>
</template>
