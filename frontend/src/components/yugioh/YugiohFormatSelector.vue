<script setup lang="ts">
import { computed, ref } from 'vue'
import type { YugiohFormat } from '@/types/yugioh'
import { YUGIOH_FORMATS } from '@/types/yugioh'

const props = defineProps<{
  modelValue: string | null
  customFormatName: string | null
  disabled?: boolean
  hasError?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'update:customFormatName', value: string | null): void
  (e: 'formatSelected', format: YugiohFormat | null): void
}>()

const formats = ref<YugiohFormat[]>(YUGIOH_FORMATS)

const selectedFormat = computed(() =>
  formats.value.find(f => f.id === props.modelValue) || null
)

const isCustomFormat = computed(() => props.modelValue === 'casual')

// Format categories for grouping
const formatCategories = {
  'Competitive': ['advanced', 'traditional'],
  'Speed': ['speed_duel'],
  'Casual': ['time_wizard', 'casual'],
}

// Category icons (SVG paths)
const categoryIcons: Record<string, string> = {
  'Competitive': 'M12,8A4,4 0 0,1 16,12A4,4 0 0,1 12,16A4,4 0 0,1 8,12A4,4 0 0,1 12,8M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10M10,22C9.75,22 9.54,21.82 9.5,21.58L9.13,18.93C8.5,18.68 7.96,18.34 7.44,17.94L4.95,18.95C4.73,19.03 4.46,18.95 4.34,18.73L2.34,15.27C2.21,15.05 2.27,14.78 2.46,14.63L4.57,12.97L4.5,12L4.57,11L2.46,9.37C2.27,9.22 2.21,8.95 2.34,8.73L4.34,5.27C4.46,5.05 4.73,4.96 4.95,5.05L7.44,6.05C7.96,5.66 8.5,5.32 9.13,5.07L9.5,2.42C9.54,2.18 9.75,2 10,2H14C14.25,2 14.46,2.18 14.5,2.42L14.87,5.07C15.5,5.32 16.04,5.66 16.56,6.05L19.05,5.05C19.27,4.96 19.54,5.05 19.66,5.27L21.66,8.73C21.79,8.95 21.73,9.22 21.54,9.37L19.43,11L19.5,12L19.43,13L21.54,14.63C21.73,14.78 21.79,15.05 21.66,15.27L19.66,18.73C19.54,18.95 19.27,19.04 19.05,18.95L16.56,17.95C16.04,18.34 15.5,18.68 14.87,18.93L14.5,21.58C14.46,21.82 14.25,22 14,22H10Z',
  'Speed': 'M12,16A3,3 0 0,1 9,13C9,11.88 9.61,10.9 10.5,10.39L20.21,4.77L14.68,14.35C14.18,15.33 13.17,16 12,16M12,3C13.81,3 15.5,3.5 16.97,4.32L14.87,5.53C14,5.19 13.03,5 12,5A8,8 0 0,0 4,13C4,15.21 4.89,17.21 6.34,18.65H6.35C6.74,19.04 6.74,19.67 6.35,20.06C5.96,20.45 5.32,20.45 4.93,20.07V20.07C3.12,18.26 2,15.76 2,13A10,10 0 0,1 12,3M22,13C22,15.76 20.88,18.26 19.07,20.07V20.07C18.68,20.45 18.05,20.45 17.66,20.06C17.27,19.67 17.27,19.04 17.66,18.65V18.65C19.11,17.2 20,15.21 20,13C20,12 19.81,11.03 19.5,10.13L20.71,8.03C21.5,9.5 22,11.18 22,13Z',
  'Casual': 'M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z',
}

// Group formats by category
const groupedFormats = computed(() => {
  const groups: { category: string; formats: YugiohFormat[]; icon: string }[] = []

  for (const [category, formatIds] of Object.entries(formatCategories)) {
    const categoryFormats = formatIds
      .map(id => formats.value.find(f => f.id === id))
      .filter((f): f is YugiohFormat => f !== undefined)

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
      <!-- Yu-Gi-Oh! Card icon -->
      <svg class="w-5 h-5" :class="hasError && !modelValue ? 'text-red-500' : 'text-blue-600'" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M12,6L8,18H10L10.75,16H13.25L14,18H16L12,6M10.83,14L12,10.5L13.17,14H10.83Z" />
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
                ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400 hover:bg-blue-50',
              disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
            ]"
            @click="selectFormat(format.id)"
          >
            {{ format.name }}
          </button>
        </div>
      </div>
    </div>

    <!-- Format description with deck requirements -->
    <div v-if="selectedFormat" class="bg-blue-50 border border-blue-100 rounded-lg p-3">
      <p class="text-sm text-blue-800 font-medium mb-2">{{ selectedFormat.name }}:</p>
      <ul class="text-sm text-blue-700 space-y-1 ml-4">
        <li class="list-disc">Main Deck: {{ selectedFormat.mainDeckMin }}-{{ selectedFormat.mainDeckMax }} cards</li>
        <li class="list-disc">Extra Deck: up to {{ selectedFormat.extraDeckMax }} cards</li>
        <li class="list-disc">Side Deck: up to {{ selectedFormat.sideDeckMax }} cards</li>
        <li class="list-disc">Starting LP: {{ selectedFormat.startingLP.toLocaleString() }}</li>
        <li v-if="selectedFormat.id === 'advanced'" class="list-disc">Uses Forbidden/Limited list</li>
        <li v-if="selectedFormat.id === 'traditional'" class="list-disc">Forbidden cards are Limited instead</li>
        <li v-if="selectedFormat.id === 'speed_duel'" class="list-disc">Uses Skill Cards</li>
      </ul>
    </div>

    <!-- Official format notice -->
    <div v-if="selectedFormat?.isOfficial" class="bg-indigo-50 border border-indigo-100 rounded-lg p-3">
      <p class="text-sm text-indigo-800">
        <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z" />
        </svg>
        This is an official Konami-sanctioned format.
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
        placeholder="e.g., Goat Format, Edison Format, No hand traps..."
        @input="$emit('update:customFormatName', ($event.target as HTMLTextAreaElement).value)"
      />
      <p class="text-xs text-gray-500">
        Describe any special rules or the era/card pool being used.
      </p>
    </div>
  </div>
</template>
