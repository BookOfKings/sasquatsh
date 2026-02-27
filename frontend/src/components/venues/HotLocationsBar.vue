<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHotLocations } from '@/services/venuesApi'
import type { EventLocation } from '@/types/social'

defineProps<{
  selectedId?: string | null
}>()

const emit = defineEmits<{
  (e: 'select', location: EventLocation): void
}>()

const hotLocations = ref<EventLocation[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    hotLocations.value = await getHotLocations()
  } catch (err) {
    console.error('Failed to load hot locations:', err)
    error.value = 'Failed to load venues'
  } finally {
    loading.value = false
  }
})

function selectLocation(location: EventLocation) {
  emit('select', location)
}

function formatDateRange(start: string, end: string): string {
  const startDate = new Date(start)
  const endDate = new Date(end)
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }

  if (startDate.getMonth() === endDate.getMonth()) {
    return `${startDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}-${endDate.getDate()}`
  }
  return `${startDate.toLocaleDateString('en-US', options)} - ${endDate.toLocaleDateString('en-US', options)}`
}
</script>

<template>
  <div v-if="!loading && hotLocations.length > 0" class="mb-4">
    <div class="flex items-center gap-2 mb-2">
      <svg class="w-4 h-4 text-orange-500" viewBox="0 0 24 24" fill="currentColor">
        <path d="M17.66 11.2C17.43 10.9 17.15 10.64 16.89 10.38C16.22 9.78 15.46 9.35 14.82 8.72C13.33 7.26 13 4.85 13.95 3C13 3.23 12.17 3.75 11.46 4.32C8.87 6.4 7.85 10.07 9.07 13.22C9.11 13.32 9.15 13.42 9.15 13.55C9.15 13.77 9 13.97 8.8 14.05C8.57 14.15 8.33 14.09 8.14 13.93C8.08 13.88 8.04 13.83 8 13.76C6.87 12.33 6.69 10.28 7.45 8.64C5.78 10 4.87 12.3 5 14.47C5.06 14.97 5.12 15.47 5.29 15.97C5.43 16.57 5.7 17.17 6 17.7C7.08 19.43 8.95 20.67 10.96 20.92C13.1 21.19 15.39 20.8 17.03 19.32C18.86 17.66 19.5 15 18.56 12.72L18.43 12.46C18.22 12 17.66 11.2 17.66 11.2M14.5 17.5C14.22 17.74 13.76 18 13.4 18.1C12.28 18.5 11.16 17.94 10.5 17.28C11.69 17 12.4 16.12 12.61 15.23C12.78 14.43 12.46 13.77 12.33 13C12.21 12.26 12.23 11.63 12.5 10.94C12.69 11.32 12.89 11.7 13.13 12C13.9 13 15.11 13.44 15.37 14.8C15.41 14.94 15.43 15.08 15.43 15.23C15.46 16.05 15.1 16.95 14.5 17.5H14.5Z"/>
      </svg>
      <span class="text-sm font-medium text-gray-700">Hot Venues</span>
    </div>
    <div class="flex flex-wrap gap-2">
      <button
        v-for="location in hotLocations"
        :key="location.id"
        type="button"
        class="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full transition-colors"
        :class="selectedId === location.id
          ? 'bg-primary-100 text-primary-700 border-2 border-primary-500'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border-2 border-transparent'"
        @click="selectLocation(location)"
      >
        <span class="font-medium">{{ location.name }}</span>
        <span class="text-xs opacity-75">{{ formatDateRange(location.startDate, location.endDate) }}</span>
        <span v-if="location.eventCount && location.eventCount > 0" class="bg-primary-500 text-white text-xs px-1.5 rounded-full">
          {{ location.eventCount }}
        </span>
      </button>
    </div>
  </div>

  <div v-else-if="loading" class="mb-4">
    <div class="flex items-center gap-2">
      <svg class="w-4 h-4 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <span class="text-sm text-gray-500">Loading venues...</span>
    </div>
  </div>
</template>
