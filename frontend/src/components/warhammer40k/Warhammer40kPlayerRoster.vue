<script setup lang="ts">
import { computed } from 'vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

interface Registration {
  id: string
  userId: string
  status: string
  user?: {
    displayName: string | null
    avatarUrl: string | null
    isFoundingMember?: boolean
    isAdmin?: boolean
  } | null
}

const props = defineProps<{
  registrations: Registration[]
  hostUserId: string
  maxPlayers: number
  confirmedCount: number
}>()

// Sort registrations: host first, then alphabetical
const sortedRegistrations = computed(() => {
  return [...props.registrations].sort((a, b) => {
    if (a.userId === props.hostUserId) return -1
    if (b.userId === props.hostUserId) return 1
    const nameA = a.user?.displayName || 'Unknown'
    const nameB = b.user?.displayName || 'Unknown'
    return nameA.localeCompare(nameB)
  })
})

// Empty slots
const emptySlots = computed(() => {
  return Math.max(0, props.maxPlayers - props.confirmedCount)
})
</script>

<template>
  <div class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2 text-red-800">
        <svg class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
        </svg>
        Commanders
        <span class="text-sm font-normal text-gray-500">
          ({{ confirmedCount }} / {{ maxPlayers }})
        </span>
      </h3>
    </div>

    <div class="p-4">
      <!-- Player Grid -->
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        <!-- Registered Players -->
        <div
          v-for="reg in sortedRegistrations"
          :key="reg.id"
          class="flex flex-col items-center p-3 rounded-lg"
          :class="reg.userId === hostUserId ? 'bg-red-50 border border-red-200' : 'bg-gray-50'"
        >
          <div class="relative">
            <UserAvatar
              :avatar-url="reg.user?.avatarUrl"
              :display-name="reg.user?.displayName"
              :is-founding-member="reg.user?.isFoundingMember"
              :is-admin="reg.user?.isAdmin"
              size="lg"
            />
            <!-- Host badge -->
            <div
              v-if="reg.userId === hostUserId"
              class="absolute -bottom-1 -right-1 w-6 h-6 bg-red-600 rounded-full flex items-center justify-center"
              title="Commander"
            >
              <svg class="w-3.5 h-3.5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,16L3,5L8.5,10L12,4L15.5,10L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z"/>
              </svg>
            </div>
          </div>
          <span class="mt-2 text-sm font-medium text-gray-900 text-center truncate max-w-full">
            {{ reg.user?.displayName || 'Unknown' }}
          </span>
          <span
            v-if="reg.userId === hostUserId"
            class="text-xs text-red-600 font-medium"
          >
            Commander
          </span>
        </div>

        <!-- Empty Slots -->
        <div
          v-for="i in emptySlots"
          :key="`empty-${i}`"
          class="flex flex-col items-center p-3 rounded-lg border-2 border-dashed border-gray-200"
        >
          <div class="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
          </div>
          <span class="mt-2 text-sm text-gray-400">Open Slot</span>
        </div>
      </div>

      <!-- Status message -->
      <p v-if="emptySlots > 0" class="text-center text-sm text-gray-500 mt-4">
        {{ emptySlots }} {{ emptySlots === 1 ? 'spot' : 'spots' }} available
      </p>
      <p v-else class="text-center text-sm text-red-600 mt-4">
        Event is full
      </p>
    </div>
  </div>
</template>
