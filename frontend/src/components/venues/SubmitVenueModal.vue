<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { submitVenue } from '@/services/venuesApi'
import StateSelect from '@/components/common/StateSelect.vue'
import type { EventLocation } from '@/types/social'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submitted', location: EventLocation): void
}>()

const auth = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

type LocationType = 'temporary' | 'permanent' | 'recurring'

const form = ref({
  name: '',
  addressLine1: '',
  city: '',
  state: '',
  postalCode: '',
  venue: '',
  locationType: 'temporary' as LocationType,
  startDate: '',
  endDate: '',
  recurringDays: [] as number[],
})

const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const isFormValid = computed(() => {
  const hasBasicInfo = form.value.name.trim() && form.value.city.trim() && form.value.state.trim()

  if (form.value.locationType === 'permanent') {
    return hasBasicInfo
  }

  if (form.value.locationType === 'recurring') {
    return hasBasicInfo && form.value.recurringDays.length > 0
  }

  // Temporary - requires dates
  return (
    hasBasicInfo &&
    form.value.startDate &&
    form.value.endDate &&
    new Date(form.value.endDate) >= new Date(form.value.startDate)
  )
})

watch(() => props.visible, (visible) => {
  if (visible) {
    // Reset form when opened
    form.value = {
      name: '',
      addressLine1: '',
      city: '',
      state: '',
      postalCode: '',
      venue: '',
      locationType: 'temporary',
      startDate: '',
      endDate: '',
      recurringDays: [],
    }
    errorMessage.value = ''
    successMessage.value = ''
  }
})

function toggleDay(day: number) {
  const idx = form.value.recurringDays.indexOf(day)
  if (idx >= 0) {
    form.value.recurringDays.splice(idx, 1)
  } else {
    form.value.recurringDays.push(day)
    form.value.recurringDays.sort((a, b) => a - b)
  }
}

async function handleSubmit() {
  if (!isFormValid.value) return

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      errorMessage.value = 'Please sign in to submit a venue'
      return
    }

    const location = await submitVenue(token, {
      name: form.value.name.trim(),
      addressLine1: form.value.addressLine1.trim() || undefined,
      city: form.value.city.trim(),
      state: form.value.state.trim(),
      postalCode: form.value.postalCode.trim() || undefined,
      venue: form.value.venue.trim() || undefined,
      isPermanent: form.value.locationType === 'permanent',
      recurringDays: form.value.locationType === 'recurring' ? form.value.recurringDays : undefined,
      startDate: form.value.locationType === 'temporary' ? form.value.startDate : undefined,
      endDate: form.value.locationType === 'temporary' ? form.value.endDate : undefined,
    })

    successMessage.value = 'Venue submitted for approval!'
    emit('submitted', location)

    // Close after a short delay
    setTimeout(() => {
      emit('close')
    }, 1500)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to submit venue'
  } finally {
    loading.value = false
  }
}

function close() {
  emit('close')
}

</script>

<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/50" @click="close"></div>
    <div class="card p-6 w-full max-w-lg relative z-10">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold">Submit a Venue</h3>
        <button class="p-1 text-gray-400 hover:text-gray-600" @click="close">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </button>
      </div>

      <p class="text-sm text-gray-600 mb-4">
        Submit a gaming convention, expo, or event venue. An admin will review and approve your submission.
      </p>

      <!-- Error -->
      <div v-if="errorMessage" class="alert-error mb-4">
        {{ errorMessage }}
      </div>

      <!-- Success -->
      <div v-if="successMessage" class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-4">
        {{ successMessage }}
      </div>

      <form v-if="!successMessage" @submit.prevent="handleSubmit" class="space-y-4">
        <!-- Location Type -->
        <div>
          <label class="label">Location Type *</label>
          <div class="flex gap-2">
            <button
              type="button"
              class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
              :class="form.locationType === 'temporary'
                ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
              @click="form.locationType = 'temporary'"
            >
              Event/Convention
            </button>
            <button
              type="button"
              class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
              :class="form.locationType === 'permanent'
                ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
              @click="form.locationType = 'permanent'"
            >
              Permanent
            </button>
            <button
              type="button"
              class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
              :class="form.locationType === 'recurring'
                ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
              @click="form.locationType = 'recurring'"
            >
              Recurring
            </button>
          </div>
          <p class="text-xs text-gray-500 mt-1">
            <template v-if="form.locationType === 'temporary'">
              One-time events like conventions or expos with specific dates
            </template>
            <template v-else-if="form.locationType === 'permanent'">
              Game stores or venues that are always available
            </template>
            <template v-else>
              Venues with regular game nights on specific days of the week
            </template>
          </p>
        </div>

        <!-- Venue Name -->
        <div>
          <label class="label">
            {{ form.locationType === 'temporary' ? 'Event/Convention Name' : 'Location Name' }} *
          </label>
          <input
            v-model="form.name"
            type="text"
            class="input"
            :placeholder="form.locationType === 'temporary' ? 'e.g., Dice Tower West 2026' : 'e.g., Dragon\'s Lair Comics & Games'"
            required
            maxlength="200"
          />
        </div>

        <!-- Venue (optional) -->
        <div>
          <label class="label">Venue/Building Name</label>
          <input
            v-model="form.venue"
            type="text"
            class="input"
            :placeholder="form.locationType === 'temporary' ? 'e.g., Westgate Las Vegas Resort' : 'e.g., Northgate Mall'"
            maxlength="200"
          />
        </div>

        <!-- Street Address -->
        <div>
          <label class="label">Street Address</label>
          <input
            v-model="form.addressLine1"
            type="text"
            class="input"
            placeholder="e.g., 3000 Paradise Rd"
            maxlength="200"
          />
        </div>

        <!-- City, State, Zip -->
        <div class="grid grid-cols-4 gap-4">
          <div class="col-span-2">
            <label class="label">City *</label>
            <input
              v-model="form.city"
              type="text"
              class="input"
              placeholder="Las Vegas"
              required
              maxlength="100"
            />
          </div>
          <div>
            <label class="label">State *</label>
            <StateSelect v-model="form.state" required />
          </div>
          <div>
            <label class="label">Zip</label>
            <input
              v-model="form.postalCode"
              type="text"
              class="input"
              placeholder="89109"
              maxlength="10"
            />
          </div>
        </div>

        <!-- Dates (only for temporary) -->
        <div v-if="form.locationType === 'temporary'" class="grid grid-cols-2 gap-4">
          <div>
            <label class="label">Start Date *</label>
            <input
              v-model="form.startDate"
              type="date"
              class="input"
              required
            />
          </div>
          <div>
            <label class="label">End Date *</label>
            <input
              v-model="form.endDate"
              type="date"
              class="input"
              required
              :min="form.startDate"
            />
          </div>
        </div>

        <!-- Recurring Days (only for recurring) -->
        <div v-if="form.locationType === 'recurring'">
          <label class="label">Game Night Days *</label>
          <div class="flex gap-1">
            <button
              v-for="(name, idx) in dayNames"
              :key="idx"
              type="button"
              class="w-10 h-10 rounded-lg text-sm font-medium transition-colors"
              :class="form.recurringDays.includes(idx)
                ? 'bg-emerald-500 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
              @click="toggleDay(idx)"
            >
              {{ name }}
            </button>
          </div>
          <p v-if="form.recurringDays.length === 0" class="text-xs text-red-500 mt-1">
            Select at least one day
          </p>
        </div>

        <div class="flex justify-end gap-3 pt-4 border-t border-gray-200">
          <button type="button" class="btn-ghost" @click="close">
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="!isFormValid || loading"
          >
            <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Submit for Review
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
