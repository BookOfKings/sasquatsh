<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PokemonFormat } from '@/types/pokemon'
import { POKEMON_FORMATS } from '@/types/pokemon'

const props = defineProps<{
  modelValue: string | null
  customFormatName: string | null
  disabled?: boolean
  hasError?: boolean  // Show error state when format required but not selected
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'update:customFormatName', value: string | null): void
  (e: 'formatSelected', format: PokemonFormat | null): void
}>()

// Use local formats (no API call needed for Pokemon)
const formats = ref<PokemonFormat[]>(POKEMON_FORMATS)

const selectedFormat = computed(() =>
  formats.value.find(f => f.id === props.modelValue) || null
)

const isCustomFormat = computed(() => props.modelValue === 'casual')

// Format categories for grouping
const formatCategories = {
  'Competitive': ['standard', 'expanded'],
  'Classic': ['unlimited', 'retro'],
  'Casual': ['theme', 'gym_leader_challenge', 'casual'],
}

// Category icons
const categoryIcons: Record<string, string> = {
  'Competitive': 'M12,8A4,4 0 0,1 16,12A4,4 0 0,1 12,16A4,4 0 0,1 8,12A4,4 0 0,1 12,8M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10M10,22C9.75,22 9.54,21.82 9.5,21.58L9.13,18.93C8.5,18.68 7.96,18.34 7.44,17.94L4.95,18.95C4.73,19.03 4.46,18.95 4.34,18.73L2.34,15.27C2.21,15.05 2.27,14.78 2.46,14.63L4.57,12.97L4.5,12L4.57,11L2.46,9.37C2.27,9.22 2.21,8.95 2.34,8.73L4.34,5.27C4.46,5.05 4.73,4.96 4.95,5.05L7.44,6.05C7.96,5.66 8.5,5.32 9.13,5.07L9.5,2.42C9.54,2.18 9.75,2 10,2H14C14.25,2 14.46,2.18 14.5,2.42L14.87,5.07C15.5,5.32 16.04,5.66 16.56,6.05L19.05,5.05C19.27,4.96 19.54,5.05 19.66,5.27L21.66,8.73C21.79,8.95 21.73,9.22 21.54,9.37L19.43,11L19.5,12L19.43,13L21.54,14.63C21.73,14.78 21.79,15.05 21.66,15.27L19.66,18.73C19.54,18.95 19.27,19.04 19.05,18.95L16.56,17.95C16.04,18.34 15.5,18.68 14.87,18.93L14.5,21.58C14.46,21.82 14.25,22 14,22H10Z', // gear
  'Classic': 'M12,3L2,12H5V20H19V12H22L12,3M12,8.75A2.25,2.25 0 0,1 14.25,11A2.25,2.25 0 0,1 12,13.25A2.25,2.25 0 0,1 9.75,11A2.25,2.25 0 0,1 12,8.75M12,15C13.5,15 16.5,15.75 16.5,17.25V18H7.5V17.25C7.5,15.75 10.5,15 12,15Z', // retro
  'Casual': 'M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z', // home
}

// Group formats by category
const groupedFormats = computed(() => {
  const groups: { category: string; formats: PokemonFormat[]; icon: string }[] = []

  for (const [category, formatIds] of Object.entries(formatCategories)) {
    const categoryFormats = formatIds
      .map(id => formats.value.find(f => f.id === id))
      .filter((f): f is PokemonFormat => f !== undefined)

    if (categoryFormats.length > 0) {
      groups.push({ category, formats: categoryFormats, icon: categoryIcons[category] || '' })
    }
  }

  return groups
})

function selectFormat(formatId: string) {
  emit('update:modelValue', formatId)
  const format = formats.value.find(f => f.id === formatId) || null
  emit('formatSelected', format)

  // Clear custom format name when switching away from casual
  if (formatId !== 'casual' && props.customFormatName) {
    emit('update:customFormatName', null)
  }
}
</script>

<template>
  <div
    class="space-y-4 p-4 -m-4 rounded-lg transition-colors"
    :class="{ 'bg-red-50 border border-red-200': hasError && !modelValue }"
  >
    <h3
      class="text-lg font-semibold flex items-center gap-2"
      :class="hasError && !modelValue ? 'text-red-700' : 'text-gray-900'"
    >
      <svg class="w-5 h-5" :class="hasError && !modelValue ? 'text-red-500' : 'text-yellow-500'" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M12,6A6,6 0 0,1 18,12A6,6 0 0,1 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6M12,8A4,4 0 0,0 8,12A4,4 0 0,0 12,16A4,4 0 0,0 16,12A4,4 0 0,0 12,8Z" />
      </svg>
      Format
      <span v-if="hasError && !modelValue" class="text-sm font-normal text-red-600">(required)</span>
    </h3>

    <!-- Format selection grid -->
    <div class="space-y-4">
      <div v-for="group in groupedFormats" :key="group.category" class="space-y-2">
        <label class="flex items-center gap-1.5 text-xs font-medium text-gray-500 uppercase tracking-wider">
          <svg v-if="group.icon" class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path :d="group.icon" />
          </svg>
          {{ group.category }}
        </label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="format in group.formats"
            :key="format.id"
            type="button"
            :disabled="disabled"
            class="px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 border"
            :class="[
              modelValue === format.id
                ? 'bg-yellow-500 text-white border-yellow-500 shadow-sm'
                : 'bg-white text-gray-700 border-gray-300 hover:border-yellow-400 hover:bg-yellow-50',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="selectFormat(format.id)"
          >
            {{ format.name }}
          </button>
        </div>
      </div>
    </div>

    <!-- Format description with summary -->
    <div v-if="selectedFormat" class="bg-yellow-50 border border-yellow-100 rounded-lg p-3">
      <p class="text-sm text-yellow-800 font-medium mb-2">{{ selectedFormat.name }}:</p>
      <ul class="text-sm text-yellow-700 space-y-1 ml-4">
        <li class="list-disc">{{ selectedFormat.minDeckSize }}-card decks</li>
        <li v-if="selectedFormat.isRotating" class="list-disc">Rotating format</li>
        <li v-else class="list-disc">Non-rotating format</li>
        <li v-if="modelValue === 'standard'" class="list-disc">Uses most recent sets</li>
        <li v-if="modelValue === 'expanded'" class="list-disc">Larger card pool (Black & White onwards)</li>
        <li v-if="modelValue === 'unlimited'" class="list-disc">All cards ever printed are legal</li>
        <li v-if="modelValue === 'theme'" class="list-disc">Pre-constructed theme decks only</li>
      </ul>
    </div>

    <!-- Rotating format notice -->
    <div v-if="selectedFormat?.isRotating" class="bg-amber-50 border border-amber-100 rounded-lg p-3">
      <p class="text-sm text-amber-800">
        <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,6V9L16,5L12,1V4A8,8 0 0,0 4,12C4,13.57 4.46,15.03 5.24,16.26L6.7,14.8C6.25,13.97 6,13 6,12A6,6 0 0,1 12,6M18.76,7.74L17.3,9.2C17.74,10.04 18,11 18,12A6,6 0 0,1 12,18V15L8,19L12,23V20A8,8 0 0,0 20,12C20,10.43 19.54,8.97 18.76,7.74Z" />
        </svg>
        This format rotates when new sets release. Check current legal sets before the event.
      </p>
    </div>

    <!-- Custom format name input for casual -->
    <div v-if="isCustomFormat" class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">
        House Rules / Format Notes
      </label>
      <textarea
        :value="customFormatName"
        :disabled="disabled"
        class="input"
        rows="2"
        placeholder="e.g., No EX/GX cards, only Base Set through Neo era..."
        @input="$emit('update:customFormatName', ($event.target as HTMLTextAreaElement).value)"
      />
      <p class="text-xs text-gray-500">
        Describe any special rules so players know what to expect.
      </p>
    </div>
  </div>
</template>
