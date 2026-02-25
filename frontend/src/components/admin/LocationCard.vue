<script setup lang="ts">
import type { EventLocation } from '@/types/social'

defineProps<{
  location: EventLocation
  showActions?: boolean
}>()

const emit = defineEmits<{
  (e: 'approve', id: string): void
  (e: 'reject', id: string): void
  (e: 'delete', id: string): void
}>()

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function getStatusClass(status: string): string {
  switch (status) {
    case 'approved':
      return 'bg-green-100 text-green-700'
    case 'rejected':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-yellow-100 text-yellow-700'
  }
}
</script>

<template>
  <div class="card p-4">
    <div class="flex items-start justify-between gap-4">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2">
          <h3 class="font-semibold text-gray-900">{{ location.name }}</h3>
          <span
            class="text-xs px-2 py-0.5 rounded-full capitalize"
            :class="getStatusClass(location.status)"
          >
            {{ location.status }}
          </span>
        </div>
        <p class="text-sm text-gray-500 mt-1">
          {{ location.city }}, {{ location.state }}
          <span v-if="location.venue"> &bull; {{ location.venue }}</span>
        </p>
        <p class="text-sm text-gray-500">
          {{ formatDate(location.startDate) }} - {{ formatDate(location.endDate) }}
        </p>
      </div>

      <div v-if="showActions" class="flex gap-2 flex-shrink-0">
        <template v-if="location.status === 'pending'">
          <button
            class="btn-ghost text-green-600 text-sm"
            @click="emit('approve', location.id)"
          >
            Approve
          </button>
          <button
            class="btn-ghost text-red-600 text-sm"
            @click="emit('reject', location.id)"
          >
            Reject
          </button>
        </template>
        <button
          class="btn-ghost text-gray-500 text-sm"
          @click="emit('delete', location.id)"
        >
          Delete
        </button>
      </div>
    </div>
  </div>
</template>
