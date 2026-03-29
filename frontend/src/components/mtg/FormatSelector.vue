<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getFormats } from '@/services/scryfallApi'
import type { MtgFormat } from '@/types/mtg'

const props = defineProps<{
  modelValue: string | null
  showDescription?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', format: MtgFormat | null): void
}>()

const formats = ref<MtgFormat[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    formats.value = await getFormats()
  } catch (err) {
    console.error('Failed to load formats:', err)
    error.value = 'Failed to load formats'
    // Provide fallback formats
    formats.value = [
      { id: 'commander', name: 'Commander (EDH)', description: '100-card singleton format', minDeckSize: 100, maxDeckSize: 100, maxCopies: 1, hasCommander: true, hasSideboard: false, sideboardSize: 0, isConstructed: true },
      { id: 'standard', name: 'Standard', description: 'Rotating format with recent sets', minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
      { id: 'modern', name: 'Modern', description: 'Non-rotating format from 8th Edition forward', minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: true, sideboardSize: 15, isConstructed: true },
      { id: 'draft', name: 'Booster Draft', description: 'Limited format - draft cards from booster packs', minDeckSize: 40, maxDeckSize: null, maxCopies: null, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: false },
      { id: 'casual', name: 'Casual / Kitchen Table', description: 'No format restrictions', minDeckSize: 60, maxDeckSize: null, maxCopies: 4, hasCommander: false, hasSideboard: false, sideboardSize: 0, isConstructed: true },
    ]
  } finally {
    loading.value = false
  }
})

const selectedFormat = computed(() => {
  return formats.value.find(f => f.id === props.modelValue) || null
})

function handleChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value || null
  emit('update:modelValue', value)
  emit('change', formats.value.find(f => f.id === value) || null)
}

// Group formats by type
const constructedFormats = computed(() => formats.value.filter(f => f.isConstructed && f.id !== 'custom'))
const limitedFormats = computed(() => formats.value.filter(f => !f.isConstructed))
const customFormat = computed(() => formats.value.find(f => f.id === 'custom'))
</script>

<template>
  <div>
    <select
      :value="modelValue || ''"
      class="input"
      :disabled="disabled || loading"
      @change="handleChange"
    >
      <option value="">Select a format...</option>

      <optgroup label="Constructed">
        <option v-for="format in constructedFormats" :key="format.id" :value="format.id">
          {{ format.name }}
        </option>
      </optgroup>

      <optgroup label="Limited">
        <option v-for="format in limitedFormats" :key="format.id" :value="format.id">
          {{ format.name }}
        </option>
      </optgroup>

      <option v-if="customFormat" :value="customFormat.id">
        {{ customFormat.name }}
      </option>
    </select>

    <!-- Format description -->
    <p v-if="showDescription && selectedFormat?.description" class="text-sm text-gray-500 mt-1">
      {{ selectedFormat.description }}
    </p>

    <!-- Format details badges -->
    <div v-if="selectedFormat" class="flex flex-wrap gap-2 mt-2">
      <span v-if="selectedFormat.minDeckSize" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 text-gray-700">
        {{ selectedFormat.minDeckSize }}{{ selectedFormat.maxDeckSize ? '' : '+' }} cards
      </span>
      <span v-if="selectedFormat.maxCopies" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-gray-100 text-gray-700">
        Max {{ selectedFormat.maxCopies }} copies
      </span>
      <span v-if="selectedFormat.maxCopies === 1" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-purple-100 text-purple-700">
        Singleton
      </span>
      <span v-if="selectedFormat.hasCommander" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-amber-100 text-amber-700">
        Commander
      </span>
      <span v-if="selectedFormat.hasSideboard" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-700">
        {{ selectedFormat.sideboardSize }}-card sideboard
      </span>
      <span v-if="!selectedFormat.isConstructed" class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-green-100 text-green-700">
        Limited
      </span>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="flex items-center gap-2 mt-2 text-gray-500 text-sm">
      <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      Loading formats...
    </div>

    <!-- Error state -->
    <p v-if="error" class="text-sm text-amber-600 mt-1">
      {{ error }} - using fallback list
    </p>
  </div>
</template>
