<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventType } from '@/types/mtg'

const props = defineProps<{
  eventType: MtgEventType
  roundsCount: number | null
  roundTimeMinutes: number
  podsSize: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:eventType', value: MtgEventType): void
  (e: 'update:roundsCount', value: number | null): void
  (e: 'update:roundTimeMinutes', value: number): void
  (e: 'update:podsSize', value: number | null): void
}>()

const eventTypes: { value: MtgEventType; label: string; description: string }[] = [
  { value: 'casual', label: 'Casual Play', description: 'Free play, no structured rounds' },
  { value: 'swiss', label: 'Swiss Tournament', description: 'Everyone plays all rounds, paired by record' },
  { value: 'single_elim', label: 'Single Elimination', description: 'Lose once and you\'re out' },
  { value: 'double_elim', label: 'Double Elimination', description: 'Two losses and you\'re out' },
  { value: 'round_robin', label: 'Round Robin', description: 'Everyone plays everyone' },
  { value: 'pods', label: 'Pod Play', description: 'Small groups play together (great for Commander)' },
]

const isTournament = computed(() =>
  ['swiss', 'single_elim', 'double_elim', 'round_robin'].includes(props.eventType)
)

const isPods = computed(() => props.eventType === 'pods')

const selectedTypeDescription = computed(() =>
  eventTypes.find(t => t.value === props.eventType)?.description || ''
)
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Event Structure</h3>

    <!-- Event Type -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Event Type <span class="text-red-500">*</span>
      </label>
      <select
        :value="eventType"
        :disabled="disabled"
        class="input"
        @change="$emit('update:eventType', ($event.target as HTMLSelectElement).value as MtgEventType)"
      >
        <option v-for="type in eventTypes" :key="type.value" :value="type.value">
          {{ type.label }}
        </option>
      </select>
      <p class="text-sm text-gray-500 mt-1">{{ selectedTypeDescription }}</p>
    </div>

    <!-- Rounds Count (for tournaments) -->
    <div v-if="isTournament">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Number of Rounds
      </label>
      <input
        type="number"
        :value="roundsCount ?? ''"
        :disabled="disabled"
        class="input w-32"
        min="1"
        max="20"
        placeholder="Auto"
        @input="$emit('update:roundsCount', ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) : null)"
      />
      <p class="text-sm text-gray-500 mt-1">
        Leave blank to auto-calculate based on player count.
      </p>
    </div>

    <!-- Round Time -->
    <div v-if="isTournament || isPods">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Round Time (minutes)
      </label>
      <div class="flex items-center gap-3">
        <input
          type="number"
          :value="roundTimeMinutes"
          :disabled="disabled"
          class="input w-32"
          min="10"
          max="180"
          @input="$emit('update:roundTimeMinutes', parseInt(($event.target as HTMLInputElement).value) || 50)"
        />
        <div class="flex gap-2">
          <button
            type="button"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50"
            :class="roundTimeMinutes === 50 ? 'border-blue-500 text-blue-600' : 'border-gray-300'"
            @click="$emit('update:roundTimeMinutes', 50)"
          >
            50 min
          </button>
          <button
            type="button"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50"
            :class="roundTimeMinutes === 60 ? 'border-blue-500 text-blue-600' : 'border-gray-300'"
            @click="$emit('update:roundTimeMinutes', 60)"
          >
            60 min
          </button>
          <button
            type="button"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50"
            :class="roundTimeMinutes === 90 ? 'border-blue-500 text-blue-600' : 'border-gray-300'"
            @click="$emit('update:roundTimeMinutes', 90)"
          >
            90 min
          </button>
        </div>
      </div>
    </div>

    <!-- Pod Size (for pod play) -->
    <div v-if="isPods">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Pod Size
      </label>
      <select
        :value="podsSize ?? 4"
        :disabled="disabled"
        class="input w-32"
        @change="$emit('update:podsSize', parseInt(($event.target as HTMLSelectElement).value))"
      >
        <option :value="3">3 players</option>
        <option :value="4">4 players</option>
        <option :value="5">5 players</option>
        <option :value="6">6 players</option>
      </select>
      <p class="text-sm text-gray-500 mt-1">
        Standard Commander pods are 4 players.
      </p>
    </div>
  </div>
</template>
