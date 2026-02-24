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
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M9,10H7V12H9V10M13,10H11V12H13V10M17,10H15V12H17V10M9,14H7V16H9V14M13,14H11V16H13V14M17,14H15V16H17V14Z"/>
      </svg>
      <p class="text-gray-500">
        {{ emptyText || 'No events found' }}
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
