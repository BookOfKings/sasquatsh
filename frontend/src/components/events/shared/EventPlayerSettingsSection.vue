<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  maxPlayers?: number
  hostIsPlaying?: boolean
  isPublic?: boolean
  isCharityEvent?: boolean
  disabled?: boolean
  defaultMaxPlayers?: number
  errors?: {
    maxPlayers?: string
  }
}>(), {
  maxPlayers: 4,
  hostIsPlaying: true,
  isPublic: true,
  isCharityEvent: false,
  defaultMaxPlayers: 4,
})

const emit = defineEmits<{
  (e: 'update:maxPlayers', value: number): void
  (e: 'update:hostIsPlaying', value: boolean): void
  (e: 'update:isPublic', value: boolean): void
  (e: 'update:isCharityEvent', value: boolean): void
}>()

// Computed message for spots available
const spotsMessage = computed(() => {
  const max = props.maxPlayers || props.defaultMaxPlayers || 4
  if (props.hostIsPlaying) {
    return `${max - 1} spots for others`
  }
  return `${max} spots (you're not playing)`
})
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Player Settings</h3>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- Max Players -->
      <div>
        <label for="maxPlayers" class="label">Max Players *</label>
        <input
          id="maxPlayers"
          :value="maxPlayers"
          type="number"
          class="input"
          :class="{ 'input-error': errors?.maxPlayers }"
          min="1"
          :disabled="disabled"
          @input="$emit('update:maxPlayers', parseInt(($event.target as HTMLInputElement).value) || 0)"
        />
        <p v-if="errors?.maxPlayers" class="text-sm text-red-500 mt-1">{{ errors.maxPlayers }}</p>
        <p v-else class="text-sm text-gray-500 mt-1">{{ spotsMessage }}</p>
      </div>

      <!-- Host Is Playing -->
      <div class="flex items-center">
        <label class="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            :checked="hostIsPlaying"
            class="w-5 h-5 rounded text-primary-500 border-gray-300 focus:ring-primary-500"
            :disabled="disabled"
            @change="$emit('update:hostIsPlaying', ($event.target as HTMLInputElement).checked)"
          />
          <div>
            <span class="label">I am playing</span>
            <p class="text-sm text-gray-500">Include yourself as a player</p>
          </div>
        </label>
      </div>

      <!-- Slot for game-specific settings (Allow Spectators, etc.) -->
      <slot name="extra-settings" />
    </div>

    <!-- Slot for game-specific row (minAge, difficulty, status, etc.) -->
    <slot name="extra-row" />

    <!-- Public and Charity checkboxes -->
    <div class="flex flex-col sm:flex-row gap-4">
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          :checked="isPublic"
          type="checkbox"
          class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
          :disabled="disabled"
          @change="$emit('update:isPublic', ($event.target as HTMLInputElement).checked)"
        />
        <span class="text-sm text-gray-700">Public event (visible to everyone)</span>
      </label>
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          :checked="isCharityEvent"
          type="checkbox"
          class="w-4 h-4 rounded border-gray-300 text-secondary-500 focus:ring-secondary-500"
          :disabled="disabled"
          @change="$emit('update:isCharityEvent', ($event.target as HTMLInputElement).checked)"
        />
        <span class="text-sm text-gray-700">Charity event</span>
      </label>
    </div>
  </div>
</template>
