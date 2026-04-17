<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { getLocationsForEventCreation } from '@/services/venuesApi'
import type { EventLocation } from '@/types/social'

const props = defineProps<{
  modelValue: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'select', location: EventLocation | null): void
}>()

const locations = ref<EventLocation[]>([])
const loading = ref(true)
const showDropdown = ref(false)
const searchQuery = ref('')

const selectedLocation = computed(() => {
  if (!props.modelValue) return null
  return locations.value.find(l => l.id === props.modelValue) || null
})

const filteredLocations = computed(() => {
  if (!searchQuery.value.trim()) return locations.value
  const query = searchQuery.value.toLowerCase()
  return locations.value.filter(l =>
    l.name.toLowerCase().includes(query) ||
    l.city.toLowerCase().includes(query) ||
    l.state.toLowerCase().includes(query) ||
    (l.venue && l.venue.toLowerCase().includes(query))
  )
})

onMounted(async () => {
  try {
    // Use getLocationsForEventCreation to show all approved venues
    // including recurring venues regardless of current day-of-week
    locations.value = await getLocationsForEventCreation()
  } catch (err) {
    console.error('Failed to load locations:', err)
  } finally {
    loading.value = false
  }
})

watch(() => props.modelValue, (newVal) => {
  if (!newVal) {
    searchQuery.value = ''
  }
})

function selectLocation(location: EventLocation) {
  emit('update:modelValue', location.id)
  emit('select', location)
  showDropdown.value = false
  searchQuery.value = ''
}

function clearSelection() {
  emit('update:modelValue', null)
  emit('select', null)
  searchQuery.value = ''
}

function handleBlur() {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

function getLocationSchedule(location: EventLocation): string {
  if (location.isPermanent) return 'Always open'
  if (location.recurringDays && location.recurringDays.length > 0) {
    return location.recurringDays.map(d => dayNames[d]).join(', ')
  }
  if (location.startDate && location.endDate) {
    return formatDateRange(location.startDate, location.endDate)
  }
  return ''
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
  <div class="relative">
    <!-- Selected venue display -->
    <div v-if="selectedLocation" class="flex items-center gap-2 p-3 bg-primary-50 border border-primary-200 rounded-lg">
      <div class="flex-1">
        <div class="font-medium text-primary-900">{{ selectedLocation.name }}</div>
        <div class="text-sm text-primary-700">
          <span v-if="selectedLocation.addressLine1">{{ selectedLocation.addressLine1 }}, </span>{{ selectedLocation.city }}, {{ selectedLocation.state }}
          <span class="text-primary-500">{{ getLocationSchedule(selectedLocation) }}</span>
        </div>
      </div>
      <button
        type="button"
        class="p-1 text-primary-400 hover:text-primary-600"
        @click="clearSelection"
        :disabled="disabled"
      >
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
        </svg>
      </button>
    </div>

    <!-- Search input -->
    <div v-else class="relative">
      <input
        v-model="searchQuery"
        type="text"
        class="input pr-10"
        placeholder="Search venues by name or location..."
        :disabled="disabled || loading"
        @focus="showDropdown = true"
        @blur="handleBlur"
      />
      <div class="absolute right-3 top-1/2 -translate-y-1/2">
        <svg v-if="loading" class="w-5 h-5 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
        </svg>
      </div>
    </div>

    <!-- Dropdown -->
    <div
      v-if="showDropdown && !selectedLocation && !loading"
      class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto"
    >
      <div v-if="filteredLocations.length === 0" class="px-4 py-3 text-gray-500 text-sm">
        {{ searchQuery ? 'No venues found' : 'No active venues' }}
      </div>
      <button
        v-for="location in filteredLocations"
        :key="location.id"
        type="button"
        class="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
        @click="selectLocation(location)"
      >
        <div class="w-10 h-10 flex items-center justify-center bg-primary-100 rounded-lg flex-shrink-0">
          <svg class="w-5 h-5 text-primary-600" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
          </svg>
        </div>
        <div class="flex-1 min-w-0">
          <div class="font-medium text-gray-900 truncate">{{ location.name }}</div>
          <div class="text-sm text-gray-500">
            <span v-if="location.addressLine1">{{ location.addressLine1 }}, </span>{{ location.city }}, {{ location.state }}
            <span class="mx-1">-</span>
            <span class="text-gray-400">{{ getLocationSchedule(location) }}</span>
          </div>
        </div>
        <div v-if="location.eventCount && location.eventCount > 0" class="flex items-center gap-1 text-xs text-gray-500">
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
          </svg>
          {{ location.eventCount }}
        </div>
      </button>
    </div>
  </div>
</template>
