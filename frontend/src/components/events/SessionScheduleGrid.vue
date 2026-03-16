<script setup lang="ts">
import { ref, computed } from 'vue'
import type { EventTable, GameSession } from '@/types/sessions'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps<{
  tables: EventTable[]
  sessions: GameSession[]
  isHost?: boolean
  registering?: boolean
}>()

const emit = defineEmits<{
  (e: 'register', sessionId: string): void
  (e: 'cancel', sessionId: string): void
}>()

// Group sessions by slot index
const slotIndexes = computed(() => {
  const indexes = new Set<number>()
  for (const session of props.sessions) {
    indexes.add(session.slotIndex)
  }
  return Array.from(indexes).sort((a, b) => a - b)
})

// Get session for a specific table and slot
function getSession(tableNumber: number, slotIndex: number): GameSession | undefined {
  return props.sessions.find(
    s => s.tableNumber === tableNumber && s.slotIndex === slotIndex
  )
}

// Selected session for detail modal
const selectedSession = ref<GameSession | null>(null)

function openSessionDetail(session: GameSession) {
  selectedSession.value = session
}

function closeSessionDetail() {
  selectedSession.value = null
}

function handleRegister(sessionId: string) {
  emit('register', sessionId)
}

function handleCancel(sessionId: string) {
  emit('cancel', sessionId)
}

// Check if user has conflict at this slot
function hasConflict(slotIndex: number): boolean {
  return props.sessions.some(
    s => s.slotIndex === slotIndex && s.isUserRegistered
  )
}
</script>

<template>
  <div class="space-y-4">
    <!-- Helper text -->
    <p class="text-sm text-gray-600 bg-blue-50 border border-blue-100 rounded-lg px-3 py-2">
      Registration is optional — sign up to help plan, or just show up on game night!
    </p>

    <!-- Grid Header -->
    <div class="overflow-x-auto">
      <table class="w-full min-w-max">
        <thead>
          <tr>
            <th class="w-20 px-2 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Slot
            </th>
            <th
              v-for="table in tables"
              :key="table.id"
              class="px-2 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider min-w-[200px]"
            >
              {{ table.tableName || `Table ${table.tableNumber}` }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="slotIdx in slotIndexes" :key="slotIdx">
            <td class="px-2 py-3 text-sm text-gray-500 font-medium">
              #{{ slotIdx + 1 }}
            </td>
            <td
              v-for="table in tables"
              :key="table.id"
              class="px-2 py-3"
            >
              <div
                v-if="getSession(table.tableNumber, slotIdx)"
                class="border rounded-lg p-3 cursor-pointer transition-all hover:shadow-md"
                :class="[
                  getSession(table.tableNumber, slotIdx)?.isUserRegistered
                    ? 'border-primary-300 bg-primary-50'
                    : getSession(table.tableNumber, slotIdx)?.isFull
                      ? 'border-gray-200 bg-gray-50'
                      : 'border-gray-200 bg-white hover:border-primary-200'
                ]"
                @click="openSessionDetail(getSession(table.tableNumber, slotIdx)!)"
              >
                <!-- Game thumbnail and name -->
                <div class="flex items-start gap-2 mb-2">
                  <img
                    v-if="getSession(table.tableNumber, slotIdx)?.thumbnailUrl"
                    :src="getSession(table.tableNumber, slotIdx)?.thumbnailUrl ?? undefined"
                    :alt="getSession(table.tableNumber, slotIdx)?.gameName ?? undefined"
                    class="w-12 h-12 object-cover rounded flex-shrink-0"
                  />
                  <div v-else class="w-12 h-12 bg-gray-100 rounded flex items-center justify-center flex-shrink-0">
                    <svg class="w-6 h-6 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3Z"/>
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-sm truncate">
                      {{ getSession(table.tableNumber, slotIdx)?.gameName }}
                    </div>
                    <div class="text-xs text-gray-500">
                      {{ getSession(table.tableNumber, slotIdx)?.durationMinutes }}min
                    </div>
                  </div>
                </div>

                <!-- Player count -->
                <div class="flex items-center justify-between">
                  <div class="text-sm">
                    <span
                      :class="[
                        getSession(table.tableNumber, slotIdx)?.isFull
                          ? 'text-red-600 font-medium'
                          : 'text-gray-600'
                      ]"
                    >
                      {{ getSession(table.tableNumber, slotIdx)?.registeredCount }}/{{ getSession(table.tableNumber, slotIdx)?.maxPlayers || '?' }}
                    </span>
                    <span class="text-gray-400 text-xs ml-1">players</span>
                  </div>

                  <!-- Status badge -->
                  <span
                    v-if="getSession(table.tableNumber, slotIdx)?.isUserRegistered"
                    class="px-2 py-0.5 text-xs font-medium bg-primary-100 text-primary-700 rounded-full"
                  >
                    Joined
                  </span>
                  <span
                    v-else-if="getSession(table.tableNumber, slotIdx)?.isFull"
                    class="px-2 py-0.5 text-xs font-medium bg-gray-100 text-gray-600 rounded-full"
                  >
                    Full
                  </span>
                </div>
              </div>

              <!-- Empty cell -->
              <div v-else class="border border-dashed border-gray-200 rounded-lg p-3 text-center text-gray-400 text-sm">
                No game
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Session Detail Modal -->
    <div
      v-if="selectedSession"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closeSessionDetail"
    >
      <div class="bg-white rounded-lg max-w-md w-full">
        <div class="p-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="font-semibold text-lg">{{ selectedSession.gameName }}</h3>
          <button
            class="text-gray-400 hover:text-gray-600"
            @click="closeSessionDetail"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <div class="p-4">
          <!-- Game image -->
          <div class="flex items-center gap-4 mb-4">
            <img
              v-if="selectedSession.thumbnailUrl"
              :src="selectedSession.thumbnailUrl"
              :alt="selectedSession.gameName"
              class="w-20 h-20 object-cover rounded"
            />
            <div>
              <div class="text-sm text-gray-500">
                Table {{ selectedSession.tableNumber }} · Slot #{{ selectedSession.slotIndex + 1 }}
              </div>
              <div class="text-sm text-gray-500">
                {{ selectedSession.durationMinutes }} minutes
              </div>
              <div class="text-sm text-gray-500">
                {{ selectedSession.minPlayers || '?' }}-{{ selectedSession.maxPlayers || '?' }} players
              </div>
            </div>
          </div>

          <!-- Player count -->
          <div class="mb-4">
            <div class="text-sm font-medium text-gray-700 mb-2">
              Players ({{ selectedSession.registeredCount }}/{{ selectedSession.maxPlayers || '?' }})
            </div>
            <div v-if="selectedSession.registrations.length > 0" class="flex flex-wrap gap-2">
              <div
                v-for="reg in selectedSession.registrations"
                :key="reg.userId"
                class="flex items-center gap-2 px-2 py-1 bg-gray-100 rounded-full text-sm"
              >
                <UserAvatar
                  :avatar-url="reg.avatarUrl"
                  :display-name="reg.displayName"
                  size="sm"
                />
                <span>{{ reg.displayName || 'Unknown' }}</span>
                <span
                  v-if="reg.isHostReserved"
                  class="text-xs text-primary-600"
                  title="Host"
                >
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                  </svg>
                </span>
              </div>
            </div>
            <div v-else class="text-sm text-gray-500">
              No players yet
            </div>
          </div>

          <!-- Action buttons -->
          <div class="flex gap-3">
            <template v-if="selectedSession.isUserRegistered">
              <button
                class="btn-outline flex-1"
                :disabled="registering"
                @click="handleCancel(selectedSession.id); closeSessionDetail()"
              >
                Leave Session
              </button>
            </template>
            <template v-else-if="selectedSession.isFull">
              <button class="btn-ghost flex-1" disabled>
                Session Full
              </button>
            </template>
            <template v-else-if="hasConflict(selectedSession.slotIndex)">
              <button class="btn-ghost flex-1" disabled>
                Conflict - Already in another session
              </button>
            </template>
            <template v-else>
              <button
                class="btn-primary flex-1"
                :disabled="registering"
                @click="handleRegister(selectedSession.id); closeSessionDetail()"
              >
                <svg v-if="registering" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Join Session
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
