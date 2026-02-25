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
        <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
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
