<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import type { MtgFormat } from '@/types/mtg'
import { FORMAT_DESCRIPTIONS, FORMAT_CATEGORIES } from '@/types/mtg'
import { getFormats } from '@/services/scryfallApi'

const props = defineProps<{
  modelValue: string | null
  customFormatName: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'update:customFormatName', value: string | null): void
  (e: 'formatSelected', format: MtgFormat | null): void
}>()

const formats = ref<MtgFormat[]>([])
const loading = ref(true)
const loadError = ref(false)

// Fallback formats if API fails
const fallbackFormats: MtgFormat[] = [
  { id: 'commander', name: 'Commander (EDH)', description: null, minDeckSize: 100, maxDeckSize: 100, maxCopies: 1, hasCommander: true, hasSideboard: false, sideboardSize: 0, isConstructed: true },
  { id: 'standard', name: 'Standard', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'modern', name: 'Modern', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'pioneer', name: 'Pioneer', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'legacy', name: 'Legacy', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'vintage', name: 'Vintage', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'pauper', name: 'Pauper', description: null, minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
  { id: 'draft', name: 'Booster Draft', description: null, minDeckSize: 40, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: false },
  { id: 'sealed', name: 'Sealed Deck', description: null, minDeckSize: 40, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: false },
  { id: 'cube', name: 'Cube Draft', description: null, minDeckSize: 40, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: false },
  { id: 'oathbreaker', name: 'Oathbreaker', description: null, minDeckSize: 60, maxDeckSize: 60, maxCopies: 1, hasCommander: true, hasSideboard: false, sideboardSize: 0, isConstructed: true },
  { id: 'brawl', name: 'Brawl', description: null, minDeckSize: 60, maxDeckSize: 60, maxCopies: 1, hasCommander: true, hasSideboard: false, sideboardSize: 0, isConstructed: true },
  { id: 'casual', name: 'Casual / Kitchen Table', description: null, minDeckSize: null, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: true },
  { id: 'custom', name: 'Custom Format', description: null, minDeckSize: null, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: true },
]

onMounted(async () => {
  try {
    const result = await getFormats()
    formats.value = result.length > 0 ? result : fallbackFormats
    loadError.value = result.length === 0
  } catch {
    formats.value = fallbackFormats
    loadError.value = true
  } finally {
    loading.value = false
  }
})

const selectedFormat = computed(() =>
  formats.value.find(f => f.id === props.modelValue) || null
)

const isCustomFormat = computed(() => props.modelValue === 'custom')

const isLimitedFormat = computed(() =>
  props.modelValue && ['draft', 'sealed', 'cube'].includes(props.modelValue)
)

const formatDescription = computed(() => {
  if (!props.modelValue) return null
  return FORMAT_DESCRIPTIONS[props.modelValue] || selectedFormat.value?.description || null
})

// Category icons
const categoryIcons: Record<string, string> = {
  'Constructed': 'M4,6H2V20A2,2 0 0,0 4,22H18V20H4M18,2H12L10.5,3.5L12,5L11,6H8A2,2 0 0,0 6,8V16A2,2 0 0,0 8,18H18A2,2 0 0,0 20,16V4A2,2 0 0,0 18,2Z', // cards
  'Commander': 'M5,16L3,5L8.5,12L12,5L15.5,12L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z', // crown
  'Limited': 'M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5V9H9V5H7M11,5V11H13V5H11M15,5V7H17V5H15M7,11V13H9V11H7M11,13V15H13V13H11M15,9V13H17V9H15M7,15V19H9V15H7M11,17V19H13V17H11M15,15V19H17V15H15Z', // booster pack
  'Casual': 'M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z', // home
}

// Group formats by category for display
const groupedFormats = computed(() => {
  const groups: { category: string; formats: MtgFormat[]; icon: string }[] = []

  for (const [category, formatIds] of Object.entries(FORMAT_CATEGORIES)) {
    const categoryFormats = formatIds
      .map(id => formats.value.find(f => f.id === id))
      .filter((f): f is MtgFormat => f !== undefined)

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

  // Clear custom format name when switching away from custom
  if (formatId !== 'custom' && props.customFormatName) {
    emit('update:customFormatName', null)
  }
}

// Emit format when selection changes
watch(() => props.modelValue, (newVal) => {
  if (newVal && formats.value.length > 0) {
    const format = formats.value.find(f => f.id === newVal) || null
    emit('formatSelected', format)
  }
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Format</h3>

    <!-- Loading state -->
    <div v-if="loading" class="animate-pulse space-y-3">
      <div class="h-10 bg-gray-200 rounded"></div>
      <div class="h-4 bg-gray-200 rounded w-2/3"></div>
    </div>

    <template v-else>
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

      <!-- Format description -->
      <div v-if="formatDescription" class="bg-blue-50 border border-blue-100 rounded-lg p-3">
        <p class="text-sm text-blue-800">
          <span class="font-medium">{{ selectedFormat?.name }}:</span>
          {{ formatDescription }}
        </p>
      </div>

      <!-- Limited format helper -->
      <div v-if="isLimitedFormat" class="bg-amber-50 border border-amber-100 rounded-lg p-3">
        <p class="text-sm text-amber-800">
          <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5V9H9V5H7M11,5V11H13V5H11M15,5V7H17V5H15M7,11V13H9V11H7M11,13V15H13V13H11M15,9V13H17V9H15M7,15V19H9V15H7M11,17V19H13V17H11M15,15V19H17V15H15Z" />
          </svg>
          Decks are built on-site from provided packs. Players don't need to bring a deck.
        </p>
      </div>

      <!-- Custom format name input -->
      <div v-if="isCustomFormat" class="space-y-2">
        <label class="block text-sm font-medium text-gray-700">
          Custom Format Name <span class="text-red-500">*</span>
        </label>
        <input
          type="text"
          :value="customFormatName"
          :disabled="disabled"
          class="input"
          placeholder="e.g., House Commander, Pauper EDH"
          @input="$emit('update:customFormatName', ($event.target as HTMLInputElement).value)"
        />
        <p class="text-xs text-gray-500">
          Describe your custom format so players know what to expect.
        </p>
      </div>
    </template>
  </div>
</template>
