<script setup lang="ts">
import EventCard from './EventCard.vue'
import type { EventSummary } from '@/types/events'

defineProps<{
  events: EventSummary[]
  emptyText?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', event: EventSummary): void
}>()
</script>

<template>
  <div>
    <!-- Loading bar -->
    <div v-if="loading" class="mb-4">
      <div class="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
        <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 60%"></div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="events.length === 0 && !loading" class="text-center py-12">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
      </svg>
      <p class="text-gray-500">
        {{ emptyText || 'No games found' }}
      </p>
    </div>

    <!-- Event grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <EventCard
        v-for="event in events"
        :key="event.id"
        :event="event"
        @click="emit('select', event)"
      />
    </div>
  </div>
</template>
