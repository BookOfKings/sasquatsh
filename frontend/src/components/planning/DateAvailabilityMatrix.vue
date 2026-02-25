<script setup lang="ts">
import { computed } from 'vue'
import type { PlanningDate, PlanningInvitee } from '@/types/planning'

const props = defineProps<{
  dates: PlanningDate[]
  invitees: PlanningInvitee[]
  selectedDateId?: string | null
}>()

const emit = defineEmits<{
  (e: 'select-date', dateId: string): void
}>()

const maxAvailable = computed(() => {
  return Math.max(...props.dates.map(d => d.availableCount ?? 0))
})

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

function getVoteStatus(date: PlanningDate, invitee: PlanningInvitee): 'available' | 'unavailable' | 'pending' | 'cannot-attend' {
  if (invitee.cannotAttendAny) return 'cannot-attend'
  if (!invitee.hasResponded) return 'pending'
  const vote = date.votes?.find(v => v.userId === invitee.userId)
  if (!vote) return 'pending'
  return vote.isAvailable ? 'available' : 'unavailable'
}

function getVoteIcon(status: 'available' | 'unavailable' | 'pending' | 'cannot-attend') {
  switch (status) {
    case 'available':
      return 'M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z'
    case 'unavailable':
    case 'cannot-attend':
      return 'M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z'
    default:
      return 'M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4Z'
  }
}

function getVoteColor(status: 'available' | 'unavailable' | 'pending' | 'cannot-attend') {
  switch (status) {
    case 'available':
      return 'text-green-500'
    case 'unavailable':
    case 'cannot-attend':
      return 'text-red-400'
    default:
      return 'text-gray-300'
  }
}
</script>

<template>
  <div class="min-w-max">
    <table class="w-full border-collapse">
      <thead>
        <tr>
          <th class="text-left text-sm font-medium text-gray-500 pb-3 pr-4">Date</th>
          <th
            v-for="invitee in invitees"
            :key="invitee.id"
            class="text-center pb-3 px-2"
          >
            <div class="flex flex-col items-center">
              <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden mb-1">
                <img
                  v-if="invitee.user?.avatarUrl"
                  :src="invitee.user.avatarUrl"
                  class="w-full h-full object-cover"
                />
                <svg v-else class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                </svg>
              </div>
              <span class="text-xs text-gray-500 max-w-[60px] truncate">
                {{ invitee.user?.displayName?.split(' ')[0] || '?' }}
              </span>
            </div>
          </th>
          <th class="text-center text-sm font-medium text-gray-500 pb-3 pl-4">Total</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="date in dates"
          :key="date.id"
          class="cursor-pointer transition-colors"
          :class="{
            'bg-primary-50': selectedDateId === date.id,
            'bg-green-50': (date.availableCount ?? 0) === maxAvailable && maxAvailable > 0 && selectedDateId !== date.id,
            'hover:bg-gray-50': selectedDateId !== date.id,
          }"
          @click="emit('select-date', date.id)"
        >
          <td class="py-3 pr-4">
            <div class="flex items-center gap-2">
              <input
                type="radio"
                :checked="selectedDateId === date.id"
                class="w-4 h-4 text-primary-500"
                @click.stop
                @change="emit('select-date', date.id)"
              />
              <div>
                <div class="font-medium text-sm">{{ formatDate(date.proposedDate) }}</div>
                <div v-if="date.startTime" class="text-xs text-gray-500">{{ date.startTime }}</div>
              </div>
            </div>
          </td>
          <td
            v-for="invitee in invitees"
            :key="invitee.id"
            class="text-center px-2 py-3"
          >
            <svg
              class="w-5 h-5 mx-auto"
              :class="getVoteColor(getVoteStatus(date, invitee))"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path :d="getVoteIcon(getVoteStatus(date, invitee))"/>
            </svg>
          </td>
          <td class="text-center pl-4 py-3">
            <span
              class="inline-flex items-center justify-center w-8 h-8 rounded-full text-sm font-semibold"
              :class="{
                'bg-green-100 text-green-700': (date.availableCount ?? 0) === maxAvailable && maxAvailable > 0,
                'bg-gray-100 text-gray-600': (date.availableCount ?? 0) < maxAvailable || maxAvailable === 0,
              }"
            >
              {{ date.availableCount ?? 0 }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Legend -->
    <div class="flex items-center gap-4 mt-4 pt-4 border-t border-gray-200 text-sm text-gray-500">
      <div class="flex items-center gap-1">
        <svg class="w-4 h-4 text-green-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
        </svg>
        Available
      </div>
      <div class="flex items-center gap-1">
        <svg class="w-4 h-4 text-red-400" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
        </svg>
        Unavailable
      </div>
      <div class="flex items-center gap-1">
        <svg class="w-4 h-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4Z"/>
        </svg>
        No response
      </div>
    </div>
  </div>
</template>
