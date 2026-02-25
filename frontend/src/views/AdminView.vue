<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getPendingLocations,
  getAllLocations,
  approveLocation,
  rejectLocation,
  deleteLocation,
} from '@/services/adminApi'
import type { EventLocation } from '@/types/social'
import LocationCard from '@/components/admin/LocationCard.vue'

const auth = useAuthStore()

const activeTab = ref<'pending' | 'all'>('pending')
const loading = ref(true)
const pendingLocations = ref<EventLocation[]>([])
const allLocations = ref<EventLocation[]>([])
const errorMessage = ref('')
const successMessage = ref('')

const displayedLocations = computed(() => {
  return activeTab.value === 'pending' ? pendingLocations.value : allLocations.value
})

onMounted(async () => {
  await loadLocations()
})

async function loadLocations() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const [pending, all] = await Promise.all([
      getPendingLocations(token),
      getAllLocations(token),
    ])
    pendingLocations.value = pending
    allLocations.value = all
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load locations'
  } finally {
    loading.value = false
  }
}

async function handleApprove(id: string) {
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await approveLocation(token, id)

    // Move from pending to all and update status
    const location = pendingLocations.value.find(l => l.id === id)
    if (location) {
      location.status = 'approved'
      pendingLocations.value = pendingLocations.value.filter(l => l.id !== id)
      const existingIndex = allLocations.value.findIndex(l => l.id === id)
      if (existingIndex >= 0) {
        allLocations.value[existingIndex] = location
      }
    }

    successMessage.value = 'Location approved'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to approve location'
  }
}

async function handleReject(id: string) {
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await rejectLocation(token, id)

    // Move from pending and update status
    const location = pendingLocations.value.find(l => l.id === id)
    if (location) {
      location.status = 'rejected'
      pendingLocations.value = pendingLocations.value.filter(l => l.id !== id)
      const existingIndex = allLocations.value.findIndex(l => l.id === id)
      if (existingIndex >= 0) {
        allLocations.value[existingIndex] = location
      }
    }

    successMessage.value = 'Location rejected'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to reject location'
  }
}

async function handleDelete(id: string) {
  if (!confirm('Are you sure you want to delete this location?')) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteLocation(token, id)
    pendingLocations.value = pendingLocations.value.filter(l => l.id !== id)
    allLocations.value = allLocations.value.filter(l => l.id !== id)

    successMessage.value = 'Location deleted'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete location'
  }
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Site Administration</h1>
      <p class="text-gray-500">Manage event locations and site settings</p>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- Tabs -->
    <div class="flex gap-2 mb-6">
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'pending' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'pending'"
      >
        Pending Approval ({{ pendingLocations.length }})
      </button>
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'all' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'all'"
      >
        All Locations ({{ allLocations.length }})
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Location List -->
    <template v-else>
      <div v-if="displayedLocations.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
        </svg>
        <p class="text-lg font-medium">
          {{ activeTab === 'pending' ? 'No pending locations' : 'No locations yet' }}
        </p>
        <p v-if="activeTab === 'pending'">
          All location submissions have been reviewed.
        </p>
      </div>

      <div v-else class="space-y-4">
        <LocationCard
          v-for="location in displayedLocations"
          :key="location.id"
          :location="location"
          :show-actions="true"
          @approve="handleApprove"
          @reject="handleReject"
          @delete="handleDelete"
        />
      </div>
    </template>
  </div>
</template>
