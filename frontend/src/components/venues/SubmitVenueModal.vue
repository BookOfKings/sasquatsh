<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { submitVenue } from '@/services/venuesApi'
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

const form = ref({
  name: '',
  city: '',
  state: '',
  venue: '',
  startDate: '',
  endDate: '',
})

const isFormValid = computed(() => {
  return (
    form.value.name.trim() &&
    form.value.city.trim() &&
    form.value.state.trim() &&
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
      city: '',
      state: '',
      venue: '',
      startDate: '',
      endDate: '',
    }
    errorMessage.value = ''
    successMessage.value = ''
  }
})

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
      city: form.value.city.trim(),
      state: form.value.state.trim(),
      venue: form.value.venue.trim() || undefined,
      startDate: form.value.startDate,
      endDate: form.value.endDate,
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

const usStates = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
]
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
        <!-- Venue Name -->
        <div>
          <label class="label">Event/Convention Name *</label>
          <input
            v-model="form.name"
            type="text"
            class="input"
            placeholder="e.g., Dice Tower West 2026"
            required
            maxlength="200"
          />
        </div>

        <!-- Venue (optional) -->
        <div>
          <label class="label">Venue/Location Name</label>
          <input
            v-model="form.venue"
            type="text"
            class="input"
            placeholder="e.g., Westgate Las Vegas Resort"
            maxlength="200"
          />
        </div>

        <!-- City and State -->
        <div class="grid grid-cols-2 gap-4">
          <div>
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
            <select v-model="form.state" class="input" required>
              <option value="">Select state</option>
              <option v-for="state in usStates" :key="state" :value="state">
                {{ state }}
              </option>
            </select>
          </div>
        </div>

        <!-- Dates -->
        <div class="grid grid-cols-2 gap-4">
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
