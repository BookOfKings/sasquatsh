<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { GameSuggestion } from '@/types/planning'
import type { ScheduleEntry, HostPreference } from '@/types/sessions'

const props = defineProps<{
  tableCount: number
  gameSuggestions: GameSuggestion[]
  initialSchedule?: ScheduleEntry[]
  initialPreferences?: HostPreference[]
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'save', schedule: ScheduleEntry[], preferences: HostPreference[]): void
}>()

// Schedule grid state: key = "tableNumber-slotIndex", value = suggestionId
const scheduleMap = ref<Map<string, string>>(new Map())
// Host preferences: set of "tableNumber-slotIndex" keys
const hostPreferenceSet = ref<Set<string>>(new Set())
// Duration overrides: key = "tableNumber-slotIndex", value = minutes
const durationOverrides = ref<Map<string, number>>(new Map())

// Track slot count per table for UI
const slotCounts = ref<number[]>([])

// Initialize from props
watch(
  () => [props.initialSchedule, props.initialPreferences, props.tableCount],
  () => {
    // Reset
    scheduleMap.value = new Map()
    hostPreferenceSet.value = new Set()
    durationOverrides.value = new Map()
    slotCounts.value = Array(props.tableCount).fill(1)

    // Load initial schedule
    if (props.initialSchedule) {
      for (const entry of props.initialSchedule) {
        const key = `${entry.tableNumber}-${entry.slotIndex}`
        scheduleMap.value.set(key, entry.suggestionId)
        if (entry.durationOverride) {
          durationOverrides.value.set(key, entry.durationOverride)
        }
        // Update slot count
        const tableIdx = entry.tableNumber - 1
        if (tableIdx >= 0 && tableIdx < slotCounts.value.length) {
          if (entry.slotIndex >= (slotCounts.value[tableIdx] ?? 0)) {
            slotCounts.value[tableIdx] = entry.slotIndex + 1
          }
        }
      }
    }

    // Load initial preferences
    if (props.initialPreferences) {
      for (const pref of props.initialPreferences) {
        hostPreferenceSet.value.add(`${pref.tableNumber}-${pref.slotIndex}`)
      }
    }
  },
  { immediate: true }
)

// Get game by suggestion ID
function getGame(suggestionId: string): GameSuggestion | undefined {
  return props.gameSuggestions.find(g => g.id === suggestionId)
}

// Get scheduled game for a cell
function getScheduledGame(tableNumber: number, slotIndex: number): GameSuggestion | undefined {
  const key = `${tableNumber}-${slotIndex}`
  const suggestionId = scheduleMap.value.get(key)
  if (!suggestionId) return undefined
  return getGame(suggestionId)
}

// Get games not yet scheduled
const unscheduledGames = computed(() => {
  const scheduledIds = new Set(scheduleMap.value.values())
  return props.gameSuggestions.filter(g => !scheduledIds.has(g.id))
})

// Assign a game to a cell
function assignGame(tableNumber: number, slotIndex: number, suggestionId: string) {
  const key = `${tableNumber}-${slotIndex}`
  // Remove from any other cell first
  for (const [k, v] of scheduleMap.value.entries()) {
    if (v === suggestionId) {
      scheduleMap.value.delete(k)
    }
  }
  scheduleMap.value.set(key, suggestionId)
}

// Clear a cell
function clearCell(tableNumber: number, slotIndex: number) {
  const key = `${tableNumber}-${slotIndex}`
  scheduleMap.value.delete(key)
  hostPreferenceSet.value.delete(key)
  durationOverrides.value.delete(key)
}

// Toggle host preference for a cell
function toggleHostPreference(tableNumber: number, slotIndex: number) {
  const key = `${tableNumber}-${slotIndex}`
  // Can only prefer cells with games
  if (!scheduleMap.value.has(key)) return

  // Check for conflicts (same slot index on different table)
  for (const prefKey of hostPreferenceSet.value) {
    const [, existingSlot] = prefKey.split('-').map(Number)
    if (existingSlot === slotIndex && prefKey !== key) {
      // Remove the conflicting preference
      hostPreferenceSet.value.delete(prefKey)
    }
  }

  if (hostPreferenceSet.value.has(key)) {
    hostPreferenceSet.value.delete(key)
  } else {
    hostPreferenceSet.value.add(key)
  }
}

// Add a slot to a table
function addSlot(tableIndex: number) {
  if (tableIndex >= 0 && tableIndex < slotCounts.value.length) {
    slotCounts.value[tableIndex] = (slotCounts.value[tableIndex] ?? 0) + 1
  }
}

// Remove last slot from a table
function removeSlot(tableIndex: number) {
  const currentCount = slotCounts.value[tableIndex] ?? 0
  if (currentCount > 1) {
    const tableNumber = tableIndex + 1
    const slotIndex = currentCount - 1
    // Clear cell if it has content
    clearCell(tableNumber, slotIndex)
    slotCounts.value[tableIndex] = currentCount - 1
  }
}

// Set duration override
function setDuration(tableNumber: number, slotIndex: number, minutes: number) {
  const key = `${tableNumber}-${slotIndex}`
  if (minutes > 0) {
    durationOverrides.value.set(key, minutes)
  } else {
    durationOverrides.value.delete(key)
  }
}

// Get duration for a cell (override or game default)
function getDuration(tableNumber: number, slotIndex: number): number {
  const key = `${tableNumber}-${slotIndex}`
  const override = durationOverrides.value.get(key)
  if (override) return override

  const game = getScheduledGame(tableNumber, slotIndex)
  return game?.playingTime ?? 60
}

// Save handler
function handleSave() {
  const schedule: ScheduleEntry[] = []
  for (const [key, suggestionId] of scheduleMap.value.entries()) {
    const parts = key.split('-').map(Number)
    const tableNumber = parts[0] ?? 0
    const slotIndex = parts[1] ?? 0
    const override = durationOverrides.value.get(key)
    schedule.push({
      suggestionId,
      tableNumber,
      slotIndex,
      durationOverride: override,
    })
  }

  const preferences: HostPreference[] = []
  for (const key of hostPreferenceSet.value) {
    const parts = key.split('-').map(Number)
    const tableNumber = parts[0] ?? 0
    const slotIndex = parts[1] ?? 0
    preferences.push({ tableNumber, slotIndex })
  }

  emit('save', schedule, preferences)
}

// Drag and drop handlers
let draggedGameId: string | null = null

function onDragStart(e: DragEvent, suggestionId: string) {
  draggedGameId = suggestionId
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
  }
}

function onDragOver(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'move'
  }
}

function onDrop(e: DragEvent, tableNumber: number, slotIndex: number) {
  e.preventDefault()
  if (draggedGameId) {
    assignGame(tableNumber, slotIndex, draggedGameId)
    draggedGameId = null
  }
}

// Count scheduled games
const scheduledCount = computed(() => scheduleMap.value.size)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h3 class="font-semibold text-lg">Schedule Games to Tables</h3>
      <button
        class="btn-primary"
        :disabled="saving || scheduledCount === 0"
        @click="handleSave"
      >
        <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        Save Schedule
      </button>
    </div>

    <p class="text-sm text-gray-600">
      Drag games from the sidebar into table slots. Click the star to mark sessions you want to play.
    </p>

    <div class="flex gap-6">
      <!-- Unscheduled Games Sidebar -->
      <div class="w-64 flex-shrink-0">
        <h4 class="font-medium mb-3 text-gray-700">Available Games</h4>
        <div class="space-y-2 max-h-96 overflow-y-auto">
          <div
            v-for="game in unscheduledGames"
            :key="game.id"
            class="flex items-center gap-2 p-2 bg-white border border-gray-200 rounded-lg cursor-grab hover:border-primary-300 hover:shadow-sm transition-all"
            draggable="true"
            @dragstart="onDragStart($event, game.id)"
          >
            <img
              v-if="game.thumbnailUrl"
              :src="game.thumbnailUrl"
              :alt="game.gameName"
              class="w-10 h-10 object-cover rounded flex-shrink-0"
            />
            <div v-else class="w-10 h-10 bg-gray-100 rounded flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium text-sm truncate">{{ game.gameName }}</div>
              <div class="text-xs text-gray-500">
                {{ game.playingTime || '?' }}min · {{ game.voteCount }} votes
              </div>
            </div>
          </div>
          <div v-if="unscheduledGames.length === 0" class="text-sm text-gray-500 text-center py-4">
            All games scheduled
          </div>
        </div>
      </div>

      <!-- Schedule Grid -->
      <div class="flex-1 overflow-x-auto">
        <div class="inline-flex gap-4 min-w-max">
          <div v-for="tableIdx in tableCount" :key="tableIdx" class="w-56">
            <div class="flex items-center justify-between mb-2">
              <h4 class="font-medium text-gray-700">Table {{ tableIdx }}</h4>
              <div class="flex items-center gap-1">
                <button
                  class="p-1 text-gray-400 hover:text-gray-600"
                  title="Remove slot"
                  @click="removeSlot(tableIdx - 1)"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,13H5V11H19V13Z"/>
                  </svg>
                </button>
                <button
                  class="p-1 text-gray-400 hover:text-gray-600"
                  title="Add slot"
                  @click="addSlot(tableIdx - 1)"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="space-y-2">
              <div
                v-for="slotIdx in slotCounts[tableIdx - 1]"
                :key="slotIdx"
                class="relative"
              >
                <!-- Drop zone -->
                <div
                  class="border-2 border-dashed rounded-lg p-2 min-h-[100px] transition-colors"
                  :class="[
                    getScheduledGame(tableIdx, slotIdx - 1)
                      ? 'border-primary-200 bg-primary-50'
                      : 'border-gray-200 bg-gray-50 hover:border-gray-300'
                  ]"
                  @dragover="onDragOver"
                  @drop="onDrop($event, tableIdx, slotIdx - 1)"
                >
                  <div class="text-xs text-gray-400 mb-1">Slot {{ slotIdx }}</div>

                  <!-- Scheduled game -->
                  <div v-if="getScheduledGame(tableIdx, slotIdx - 1)" class="space-y-2">
                    <div class="flex items-start gap-2">
                      <img
                        v-if="getScheduledGame(tableIdx, slotIdx - 1)?.thumbnailUrl"
                        :src="getScheduledGame(tableIdx, slotIdx - 1)?.thumbnailUrl ?? undefined"
                        :alt="getScheduledGame(tableIdx, slotIdx - 1)?.gameName"
                        class="w-12 h-12 object-cover rounded flex-shrink-0"
                      />
                      <div class="flex-1 min-w-0">
                        <div class="font-medium text-sm truncate">
                          {{ getScheduledGame(tableIdx, slotIdx - 1)?.gameName }}
                        </div>
                        <div class="flex items-center gap-2 mt-1">
                          <input
                            type="number"
                            :value="getDuration(tableIdx, slotIdx - 1)"
                            min="15"
                            step="15"
                            class="w-16 text-xs px-1 py-0.5 border border-gray-200 rounded"
                            title="Duration in minutes"
                            @input="setDuration(tableIdx, slotIdx - 1, Number(($event.target as HTMLInputElement).value))"
                          />
                          <span class="text-xs text-gray-500">min</span>
                        </div>
                      </div>
                    </div>

                    <!-- Action buttons -->
                    <div class="flex items-center justify-between">
                      <button
                        class="p-1 rounded transition-colors"
                        :class="[
                          hostPreferenceSet.has(`${tableIdx}-${slotIdx - 1}`)
                            ? 'text-yellow-500 hover:text-yellow-600'
                            : 'text-gray-300 hover:text-yellow-500'
                        ]"
                        title="I want to play this"
                        @click="toggleHostPreference(tableIdx, slotIdx - 1)"
                      >
                        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                        </svg>
                      </button>
                      <button
                        class="p-1 text-gray-400 hover:text-red-500 transition-colors"
                        title="Remove"
                        @click="clearCell(tableIdx, slotIdx - 1)"
                      >
                        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                        </svg>
                      </button>
                    </div>
                  </div>

                  <!-- Empty state -->
                  <div v-else class="text-center py-4">
                    <div class="text-xs text-gray-400">Drop game here</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Summary -->
    <div class="bg-gray-50 rounded-lg p-4">
      <div class="flex items-center gap-6 text-sm">
        <div>
          <span class="text-gray-500">Scheduled:</span>
          <span class="font-medium ml-1">{{ scheduledCount }} game{{ scheduledCount === 1 ? '' : 's' }}</span>
        </div>
        <div>
          <span class="text-gray-500">Host playing:</span>
          <span class="font-medium ml-1">{{ hostPreferenceSet.size }} session{{ hostPreferenceSet.size === 1 ? '' : 's' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
