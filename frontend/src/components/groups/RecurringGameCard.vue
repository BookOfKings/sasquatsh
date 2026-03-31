<script setup lang="ts">
import { computed } from 'vue'
import type { RecurringGame } from '@/types/groups'

const props = defineProps<{
  game: RecurringGame
  isAdmin: boolean
}>()

const emit = defineEmits<{
  (e: 'edit', game: RecurringGame): void
  (e: 'delete', game: RecurringGame): void
  (e: 'toggle-active', game: RecurringGame): void
}>()

const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

function formatTime(timeStr: string): string {
  const parts = timeStr.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const date = new Date()
  date.setHours(hours, minutes)
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  })
}

function weekLabel(week: number): string {
  switch (week) {
    case 1: return '1st'
    case 2: return '2nd'
    case 3: return '3rd'
    case 4: return '4th'
    case -1: return 'Last'
    default: return ''
  }
}

const scheduleSummary = computed(() => {
  const day = dayNames[props.game.dayOfWeek] ?? 'Unknown'
  const time = formatTime(props.game.startTime)

  switch (props.game.frequency) {
    case 'weekly':
      return `Every ${day} at ${time}`
    case 'biweekly':
      return `Every other ${day} at ${time}`
    case 'monthly': {
      const week = props.game.monthlyWeek ? weekLabel(props.game.monthlyWeek) : '1st'
      return `${week} ${day} of each month at ${time}`
    }
    default:
      return `${day} at ${time}`
  }
})

const durationLabel = computed(() => {
  const hours = Math.floor(props.game.durationMinutes / 60)
  const mins = props.game.durationMinutes % 60
  if (mins === 0) return `${hours}h`
  if (hours === 0) return `${mins}m`
  return `${hours}h ${mins}m`
})

const locationDisplay = computed(() => {
  if (props.game.locationDetails) return props.game.locationDetails
  return [props.game.city, props.game.state].filter(Boolean).join(', ') || null
})

const nextOccurrenceFormatted = computed(() => {
  if (!props.game.nextOccurrenceDate) return null
  const date = new Date(props.game.nextOccurrenceDate + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
})

const gameSystemInfo = computed(() => {
  switch (props.game.gameSystem) {
    case 'mtg':
      return { label: 'MTG', icon: '/icons/mtg-logo.png', bg: 'bg-purple-100 text-purple-700 border-purple-200' }
    case 'pokemon_tcg':
      return { label: 'Pokemon', icon: '/icons/pokemon-logo.png', bg: 'bg-yellow-100 text-yellow-700 border-yellow-200' }
    case 'yugioh':
      return { label: 'Yu-Gi-Oh!', icon: '/icons/yugioh-logo.png', bg: 'bg-blue-100 text-blue-700 border-blue-200' }
    case 'warhammer40k':
      return { label: '40k', icon: '/icons/warhammer40k-logo.png', bg: 'bg-red-100 text-red-700 border-red-200' }
    default:
      return null
  }
})
</script>

<template>
  <div class="card-hover p-4">
    <!-- Header -->
    <div class="flex items-start justify-between mb-2">
      <h3 class="font-semibold text-gray-900 truncate flex-1">{{ game.title }}</h3>
      <span
        :class="game.isActive
          ? 'chip-success'
          : 'chip bg-gray-100 text-gray-500'"
        class="ml-2 flex-shrink-0"
      >
        {{ game.isActive ? 'Active' : 'Paused' }}
      </span>
    </div>

    <!-- Schedule -->
    <div class="flex items-center text-sm text-gray-600 mb-2">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
      </svg>
      <span>{{ scheduleSummary }}</span>
    </div>

    <!-- Location -->
    <div v-if="locationDisplay" class="flex items-center text-sm text-gray-600 mb-2">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
      </svg>
      <span>{{ locationDisplay }}</span>
    </div>

    <!-- Tags row -->
    <div class="flex flex-wrap items-center gap-2 mb-2">
      <!-- Game system badge -->
      <span
        v-if="gameSystemInfo"
        :class="gameSystemInfo.bg"
        class="chip border"
      >
        <img :src="gameSystemInfo.icon" :alt="gameSystemInfo.label" class="h-3.5 mr-1 object-contain" />
        {{ gameSystemInfo.label }}
      </span>

      <!-- Game title -->
      <span v-if="game.gameTitle" class="chip border border-primary-500 text-primary-500">
        {{ game.gameTitle }}
      </span>

      <!-- Duration -->
      <span class="chip bg-gray-100 text-gray-600">
        {{ durationLabel }}
      </span>

      <!-- Max players -->
      <span class="chip bg-gray-100 text-gray-600">
        <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
        </svg>
        {{ game.maxPlayers }} max
      </span>
    </div>

    <!-- Next occurrence -->
    <div v-if="nextOccurrenceFormatted && game.isActive" class="flex items-center text-sm text-gray-500 mb-3">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
      </svg>
      <span>Next: {{ nextOccurrenceFormatted }}</span>
    </div>

    <!-- Admin actions -->
    <div v-if="isAdmin" class="flex items-center gap-2 pt-2 border-t border-gray-100">
      <button
        type="button"
        class="btn-sm btn-secondary"
        @click.stop="emit('edit', game)"
      >
        <svg class="w-3.5 h-3.5 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
        </svg>
        Edit
      </button>
      <button
        type="button"
        class="btn-sm"
        :class="game.isActive ? 'btn-secondary' : 'btn-primary'"
        @click.stop="emit('toggle-active', game)"
      >
        {{ game.isActive ? 'Pause' : 'Resume' }}
      </button>
      <button
        type="button"
        class="btn-sm text-red-600 hover:bg-red-50"
        @click.stop="emit('delete', game)"
      >
        <svg class="w-3.5 h-3.5 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
        </svg>
        Delete
      </button>
    </div>
  </div>
</template>
