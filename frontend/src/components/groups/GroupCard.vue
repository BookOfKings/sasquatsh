<script setup lang="ts">
import type { GroupSummary } from '@/types/groups'

defineProps<{
  group: GroupSummary
}>()

const emit = defineEmits<{
  (e: 'click', group: GroupSummary): void
}>()

function formatGroupType(type: string): string {
  const labels: Record<string, string> = {
    geographic: 'Local',
    interest: 'Interest',
    both: 'Community',
  }
  return labels[type] || type
}
</script>

<template>
  <div
    class="card-hover p-4 cursor-pointer"
    @click="emit('click', group)"
  >
    <!-- Header with logo -->
    <div class="flex items-start gap-3 mb-3">
      <div class="w-12 h-12 rounded-lg bg-primary-100 flex items-center justify-center overflow-hidden flex-shrink-0">
        <img
          v-if="group.logoUrl"
          :src="group.logoUrl"
          class="w-full h-full object-cover"
        />
        <svg v-else class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
        </svg>
      </div>
      <div class="flex-1 min-w-0">
        <h3 class="font-semibold text-gray-900 truncate">{{ group.name }}</h3>
        <p v-if="group.description" class="text-sm text-gray-500 truncate">
          {{ group.description }}
        </p>
      </div>
    </div>

    <!-- Tags -->
    <div class="flex flex-wrap gap-2 mb-3">
      <span class="chip-primary">
        {{ formatGroupType(group.groupType) }}
      </span>
      <span v-if="!group.isPublic" class="chip-warning">
        <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
        </svg>
        Private
      </span>
    </div>

    <!-- Location -->
    <div v-if="group.locationCity || group.locationState" class="flex items-center text-sm text-gray-600 mb-2">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
      </svg>
      <span>{{ [group.locationCity, group.locationState].filter(Boolean).join(', ') }}</span>
    </div>

    <!-- Members -->
    <div class="flex items-center text-sm text-gray-600">
      <svg class="w-4 h-4 mr-2 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
        <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
      </svg>
      <span>{{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}</span>
    </div>
  </div>
</template>
