<script setup lang="ts">
import { computed } from 'vue'

withDefaults(defineProps<{
  eventDate?: string
  startTime?: string
  timezone?: string
  durationMinutes?: number
  setupMinutes?: number
  disabled?: boolean
  errors?: {
    eventDate?: string
    startTime?: string
    durationMinutes?: string
  }
}>(), {
  eventDate: '',
  startTime: '19:00',
  timezone: 'America/New_York',
  durationMinutes: 120,
  setupMinutes: 15,
})

const emit = defineEmits<{
  (e: 'update:eventDate', value: string): void
  (e: 'update:startTime', value: string): void
  (e: 'update:timezone', value: string): void
  (e: 'update:durationMinutes', value: number): void
  (e: 'update:setupMinutes', value: number): void
}>()

// Today's date for min attribute
const today = computed(() => new Date().toISOString().split('T')[0])

// Timezone options
const timezoneOptions = [
  { value: 'America/New_York', label: 'Eastern Time (ET)' },
  { value: 'America/Chicago', label: 'Central Time (CT)' },
  { value: 'America/Denver', label: 'Mountain Time (MT)' },
  { value: 'America/Phoenix', label: 'Arizona (no DST)' },
  { value: 'America/Los_Angeles', label: 'Pacific Time (PT)' },
  { value: 'America/Anchorage', label: 'Alaska Time (AKT)' },
  { value: 'Pacific/Honolulu', label: 'Hawaii Time (HT)' },
  { value: 'Europe/London', label: 'UK (GMT/BST)' },
  { value: 'Europe/Paris', label: 'Central Europe (CET)' },
  { value: 'Asia/Tokyo', label: 'Japan (JST)' },
  { value: 'Australia/Sydney', label: 'Sydney (AEST)' },
]

// Quick duration buttons
const durationPresets = [
  { label: '1h', minutes: 60 },
  { label: '2h', minutes: 120 },
  { label: '3h', minutes: 180 },
  { label: '4h', minutes: 240 },
]
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Date & Time</h3>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <!-- Date -->
      <div>
        <label for="eventDate" class="label">Date *</label>
        <input
          id="eventDate"
          :value="eventDate"
          type="date"
          :min="today"
          class="input"
          :class="{ 'input-error': errors?.eventDate }"
          :disabled="disabled"
          @input="$emit('update:eventDate', ($event.target as HTMLInputElement).value)"
        />
        <p v-if="errors?.eventDate" class="text-sm text-red-500 mt-1">{{ errors.eventDate }}</p>
      </div>

      <!-- Start Time -->
      <div>
        <label for="startTime" class="label">Start Time *</label>
        <input
          id="startTime"
          :value="startTime"
          type="time"
          class="input"
          :class="{ 'input-error': errors?.startTime }"
          :disabled="disabled"
          @input="$emit('update:startTime', ($event.target as HTMLInputElement).value)"
        />
        <p v-if="errors?.startTime" class="text-sm text-red-500 mt-1">{{ errors.startTime }}</p>
      </div>

      <!-- Timezone -->
      <div>
        <label for="timezone" class="label">Time Zone</label>
        <select
          id="timezone"
          :value="timezone"
          class="input"
          :disabled="disabled"
          @change="$emit('update:timezone', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="tz in timezoneOptions" :key="tz.value" :value="tz.value">
            {{ tz.label }}
          </option>
        </select>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <!-- Duration -->
      <div>
        <label for="durationMinutes" class="label">Duration (minutes) *</label>
        <div class="flex gap-2">
          <input
            id="durationMinutes"
            :value="durationMinutes"
            type="number"
            class="input flex-1"
            :class="{ 'input-error': errors?.durationMinutes }"
            min="1"
            :disabled="disabled"
            @input="$emit('update:durationMinutes', parseInt(($event.target as HTMLInputElement).value) || 0)"
          />
          <div class="flex gap-1">
            <button
              v-for="preset in durationPresets"
              :key="preset.minutes"
              type="button"
              class="px-2 py-1 text-xs rounded border transition-colors"
              :class="durationMinutes === preset.minutes
                ? 'bg-primary-100 border-primary-300 text-primary-700'
                : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
              :disabled="disabled"
              @click="$emit('update:durationMinutes', preset.minutes)"
            >
              {{ preset.label }}
            </button>
          </div>
        </div>
        <p v-if="errors?.durationMinutes" class="text-sm text-red-500 mt-1">{{ errors.durationMinutes }}</p>
      </div>

      <!-- Setup Time -->
      <div>
        <label for="setupMinutes" class="label">Setup Time (minutes)</label>
        <input
          id="setupMinutes"
          :value="setupMinutes"
          type="number"
          class="input"
          min="0"
          :disabled="disabled"
          @input="$emit('update:setupMinutes', parseInt(($event.target as HTMLInputElement).value) || 0)"
        />
        <p class="text-sm text-gray-500 mt-1">Time before event starts for setup</p>
      </div>
    </div>
  </div>
</template>
