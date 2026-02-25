<script setup lang="ts">
import type { EventSummary } from '@/types/events'

defineProps<{
  event: EventSummary
}>()

const emit = defineEmits<{
  (e: 'click', event: EventSummary): void
}>()

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

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

function getDifficultyClasses(level: string | null): string {
  switch (level) {
    case 'beginner':
      return 'chip-success'
    case 'intermediate':
      return 'chip-warning'
    case 'advanced':
      return 'chip-error'
    default:
      return 'chip bg-gray-100 text-gray-700'
  }
}

function getStatusClasses(status: string): string {
  switch (status) {
    case 'published':
      return 'chip-success'
    case 'draft':
      return 'chip bg-gray-100 text-gray-700'
    case 'cancelled':
      return 'chip-error'
    case 'completed':
      return 'chip bg-blue-100 text-blue-700'
    default:
      return 'chip bg-gray-100 text-gray-700'
  }
}

function formatCategory(category: string | null): string {
  if (!category) return ''
  const labels: Record<string, string> = {
    strategy: 'Strategy',
    party: 'Party',
    cooperative: 'Co-op',
    deckbuilding: 'Deck Building',
    workerplacement: 'Worker Placement',
    areacontrol: 'Area Control',
    dice: 'Dice',
    trivia: 'Trivia',
    roleplaying: 'RPG',
    miniatures: 'Miniatures',
    card: 'Card',
    family: 'Family',
    abstract: 'Abstract',
    other: 'Other',
  }
  return labels[category] || category
}

function getPlayerProgress(confirmed: number, max: number): number {
  return Math.min((confirmed / max) * 100, 100)
}
</script>

<template>
  <div
    class="card-hover p-4 cursor-pointer"
    @click="emit('click', event)"
  >
    <!-- Header with avatar -->
    <div class="flex items-start gap-3 mb-3">
      <div class="w-12 h-12 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden flex-shrink-0">
        <img
          v-if="event.host?.avatarUrl"
          :src="event.host.avatarUrl"
          class="w-full h-full object-cover"
        />
        <svg v-else class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
        </svg>
      </div>
      <div class="flex-1 min-w-0">
        <h3 class="font-semibold text-gray-900 truncate">{{ event.title }}</h3>
        <p class="text-sm text-gray-500 truncate">
          <span v-if="event.gameTitle">{{ event.gameTitle }} &bull; </span>
          Hosted by {{ event.host?.displayName || 'Unknown' }}
        </p>
      </div>
    </div>

    <!-- Tags -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span :class="getStatusClasses(event.status)">
        {{ event.status }}
      </span>

      <span
        v-if="event.gameCategory"
        class="chip border border-primary-500 text-primary-500"
      >
        {{ formatCategory(event.gameCategory) }}
      </span>

      <span
        v-if="event.difficultyLevel"
        :class="getDifficultyClasses(event.difficultyLevel)"
      >
        {{ event.difficultyLevel }}
      </span>

      <span
        v-if="event.isCharityEvent"
        class="chip-secondary"
      >
        <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>
        </svg>
        Charity
      </span>

      <span
        v-if="event.minAge"
        class="chip bg-orange-100 text-orange-700"
      >
        {{ event.minAge }}+
      </span>
    </div>

    <!-- Date & Time -->
    <div class="flex items-center text-sm text-gray-600 mb-2">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
      </svg>
      <span>{{ formatDate(event.eventDate) }}</span>
      <svg class="w-4 h-4 ml-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
      </svg>
      <span>{{ formatTime(event.startTime) }}</span>
    </div>

    <!-- Location -->
    <div v-if="event.city || event.state" class="flex items-center text-sm text-gray-600 mb-2">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
      </svg>
      <span>{{ [event.city, event.state].filter(Boolean).join(', ') }}</span>
    </div>

    <!-- Players -->
    <div class="flex items-center text-sm text-gray-600">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
      </svg>
      <span>{{ event.confirmedCount }} / {{ event.maxPlayers }} players</span>
      <div class="ml-3 flex-1 max-w-[100px] h-1.5 bg-gray-200 rounded-full overflow-hidden">
        <div
          class="h-full rounded-full transition-all"
          :class="event.confirmedCount >= event.maxPlayers ? 'bg-red-500' : 'bg-primary-500'"
          :style="{ width: `${getPlayerProgress(event.confirmedCount, event.maxPlayers)}%` }"
        />
      </div>
    </div>
  </div>
</template>
