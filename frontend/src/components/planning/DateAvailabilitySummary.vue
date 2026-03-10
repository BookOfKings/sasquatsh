<script setup lang="ts">
import { ref, computed } from 'vue'
import type { PlanningDate, PlanningInvitee } from '@/types/planning'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps<{
  dates: PlanningDate[]
  invitees: PlanningInvitee[]
  selectedDateId?: string | null
  currentUserId?: string | null
  pendingAvailability?: Record<string, boolean>
}>()

const emit = defineEmits<{
  (e: 'select-date', dateId: string): void
}>()

const expandedDateId = ref<string | null>(null)

// Get effective available count including pending changes
function getEffectiveAvailableCount(date: PlanningDate): number {
  const baseCount = date.availableCount ?? 0

  if (!props.currentUserId || !props.pendingAvailability) {
    return baseCount
  }

  const pendingStatus = props.pendingAvailability[date.id]
  if (pendingStatus === undefined) {
    return baseCount
  }

  const currentInvitee = props.invitees.find(i => i.userId === props.currentUserId)
  if (!currentInvitee) {
    return baseCount
  }

  if (!currentInvitee.hasResponded) {
    return pendingStatus ? baseCount + 1 : baseCount
  }

  const existingVote = date.votes?.find(v => v.userId === props.currentUserId)
  const wasAvailable = existingVote?.isAvailable ?? false

  if (pendingStatus === wasAvailable) {
    return baseCount
  }

  return pendingStatus ? baseCount + 1 : baseCount - 1
}

const maxAvailable = computed(() => {
  return Math.max(...props.dates.map(d => getEffectiveAvailableCount(d)))
})

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
  const period = hours >= 12 ? 'PM' : 'AM'
  const hour12 = hours % 12 || 12
  return `${hour12}:${minutes.toString().padStart(2, '0')} ${period}`
}

function getAvailableInvitees(date: PlanningDate) {
  return props.invitees.filter(invitee => {
    // Check pending status for current user
    if (props.currentUserId && invitee.userId === props.currentUserId && props.pendingAvailability) {
      const pendingStatus = props.pendingAvailability[date.id]
      if (pendingStatus !== undefined) {
        return pendingStatus
      }
    }

    if (!invitee.hasResponded || invitee.cannotAttendAny) return false
    const vote = date.votes?.find(v => v.userId === invitee.userId)
    return vote?.isAvailable ?? false
  })
}

function getUnavailableInvitees(date: PlanningDate) {
  return props.invitees.filter(invitee => {
    // Check pending status for current user
    if (props.currentUserId && invitee.userId === props.currentUserId && props.pendingAvailability) {
      const pendingStatus = props.pendingAvailability[date.id]
      if (pendingStatus !== undefined) {
        return !pendingStatus
      }
    }

    if (invitee.cannotAttendAny) return true
    if (!invitee.hasResponded) return false
    const vote = date.votes?.find(v => v.userId === invitee.userId)
    return vote ? !vote.isAvailable : false
  })
}

function getPendingInvitees(date: PlanningDate) {
  return props.invitees.filter(invitee => {
    // Current user with pending availability is not "pending"
    if (props.currentUserId && invitee.userId === props.currentUserId && props.pendingAvailability) {
      const pendingStatus = props.pendingAvailability[date.id]
      if (pendingStatus !== undefined) {
        return false
      }
    }

    return !invitee.hasResponded && !invitee.cannotAttendAny
  })
}

function toggleExpanded(dateId: string) {
  expandedDateId.value = expandedDateId.value === dateId ? null : dateId
}
</script>

<template>
  <div class="space-y-3">
    <div
      v-for="date in dates"
      :key="date.id"
      class="border rounded-lg overflow-hidden transition-colors"
      :class="{
        'border-primary-500 bg-primary-50': selectedDateId === date.id,
        'border-green-300 bg-green-50': getEffectiveAvailableCount(date) === maxAvailable && maxAvailable > 0 && selectedDateId !== date.id,
        'border-gray-200': selectedDateId !== date.id && (getEffectiveAvailableCount(date) < maxAvailable || maxAvailable === 0),
      }"
    >
      <!-- Main Row -->
      <div
        class="flex items-center gap-3 p-4 cursor-pointer"
        @click="emit('select-date', date.id)"
      >
        <!-- Radio Button -->
        <input
          type="radio"
          :checked="selectedDateId === date.id"
          class="w-5 h-5 text-primary-500 flex-shrink-0"
          @click.stop
          @change="emit('select-date', date.id)"
        />

        <!-- Date Info -->
        <div class="flex-1 min-w-0">
          <div class="font-medium">{{ formatDate(date.proposedDate) }}</div>
          <div v-if="date.startTime" class="text-sm text-gray-500">{{ formatTime(date.startTime) }}</div>
        </div>

        <!-- Available Count & Avatars -->
        <div class="flex items-center gap-3">
          <!-- Stacked Avatars -->
          <div class="flex -space-x-2">
            <UserAvatar
              v-for="(invitee, idx) in getAvailableInvitees(date).slice(0, 5)"
              :key="invitee.id"
              :avatar-url="invitee.user?.avatarUrl"
              :display-name="invitee.user?.displayName"
              :is-founding-member="invitee.user?.isFoundingMember"
              :is-admin="invitee.user?.isAdmin"
              size="sm"
              class="ring-2 ring-white"
              :style="{ zIndex: 5 - idx }"
            />
            <div
              v-if="getAvailableInvitees(date).length > 5"
              class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-medium text-gray-600 ring-2 ring-white"
            >
              +{{ getAvailableInvitees(date).length - 5 }}
            </div>
          </div>

          <!-- Count Badge -->
          <span
            class="inline-flex items-center justify-center px-2.5 py-1 rounded-full text-sm font-semibold min-w-[3rem]"
            :class="{
              'bg-green-100 text-green-700': getEffectiveAvailableCount(date) === maxAvailable && maxAvailable > 0,
              'bg-gray-100 text-gray-600': getEffectiveAvailableCount(date) < maxAvailable || maxAvailable === 0,
            }"
          >
            {{ getEffectiveAvailableCount(date) }} / {{ invitees.length }}
          </span>

          <!-- Expand Button -->
          <button
            class="p-1 rounded hover:bg-gray-100 transition-colors"
            @click.stop="toggleExpanded(date.id)"
          >
            <svg
              class="w-5 h-5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': expandedDateId === date.id }"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Expanded Details -->
      <div v-if="expandedDateId === date.id" class="border-t border-gray-200 p-4 bg-white">
        <!-- Available -->
        <div v-if="getAvailableInvitees(date).length > 0" class="mb-4">
          <div class="text-sm font-medium text-green-700 mb-2 flex items-center gap-1">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
            </svg>
            Available ({{ getAvailableInvitees(date).length }})
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              v-for="invitee in getAvailableInvitees(date)"
              :key="invitee.id"
              class="flex items-center gap-2 px-2 py-1 bg-green-50 rounded-full"
            >
              <UserAvatar
                :avatar-url="invitee.user?.avatarUrl"
                :display-name="invitee.user?.displayName"
                :is-founding-member="invitee.user?.isFoundingMember"
                :is-admin="invitee.user?.isAdmin"
                size="xs"
              />
              <span class="text-sm text-green-700">{{ invitee.user?.displayName || invitee.user?.username || '?' }}</span>
            </div>
          </div>
        </div>

        <!-- Unavailable -->
        <div v-if="getUnavailableInvitees(date).length > 0" class="mb-4">
          <div class="text-sm font-medium text-red-600 mb-2 flex items-center gap-1">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
            Unavailable ({{ getUnavailableInvitees(date).length }})
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              v-for="invitee in getUnavailableInvitees(date)"
              :key="invitee.id"
              class="flex items-center gap-2 px-2 py-1 bg-red-50 rounded-full"
            >
              <UserAvatar
                :avatar-url="invitee.user?.avatarUrl"
                :display-name="invitee.user?.displayName"
                :is-founding-member="invitee.user?.isFoundingMember"
                :is-admin="invitee.user?.isAdmin"
                size="xs"
              />
              <span class="text-sm text-red-600">{{ invitee.user?.displayName || invitee.user?.username || '?' }}</span>
            </div>
          </div>
        </div>

        <!-- Pending -->
        <div v-if="getPendingInvitees(date).length > 0">
          <div class="text-sm font-medium text-gray-500 mb-2 flex items-center gap-1">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4Z"/>
            </svg>
            No response ({{ getPendingInvitees(date).length }})
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              v-for="invitee in getPendingInvitees(date)"
              :key="invitee.id"
              class="flex items-center gap-2 px-2 py-1 bg-gray-100 rounded-full"
            >
              <UserAvatar
                :avatar-url="invitee.user?.avatarUrl"
                :display-name="invitee.user?.displayName"
                :is-founding-member="invitee.user?.isFoundingMember"
                :is-admin="invitee.user?.isAdmin"
                size="xs"
              />
              <span class="text-sm text-gray-500">{{ invitee.user?.displayName || invitee.user?.username || '?' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="flex items-center gap-4 pt-2 text-sm text-gray-500">
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
