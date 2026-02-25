<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getAllLocations,
  createLocation,
  updateLocation,
  deleteLocation,
  mergeLocations,
  getBggCacheStats,
  importPopularGames,
  importHotGames,
  refreshStaleCache,
} from '@/services/adminApi'
import type { EventLocation } from '@/types/social'
import type { BggCacheStats } from '@/services/adminApi'

const auth = useAuthStore()

const activeTab = ref<'locations' | 'bggCache'>('locations')

const loading = ref(true)
const locations = ref<EventLocation[]>([])
const errorMessage = ref('')
const successMessage = ref('')

// BGG Cache state
const cacheStats = ref<BggCacheStats | null>(null)
const cacheLoading = ref(false)
const cacheImporting = ref(false)

// Create/Edit dialog
const showDialog = ref(false)
const editingLocation = ref<EventLocation | null>(null)
const saving = ref(false)
const form = reactive({
  name: '',
  city: '',
  state: '',
  venue: '',
  startDate: '',
  endDate: '',
})

// Merge mode
const mergeMode = ref(false)
const selectedForMerge = ref<string[]>([])

onMounted(async () => {
  await loadLocations()
  await loadCacheStats()
})

async function loadCacheStats() {
  cacheLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    cacheStats.value = await getBggCacheStats(token)
  } catch (err) {
    console.error('Failed to load cache stats:', err)
  } finally {
    cacheLoading.value = false
  }
}

async function handleImportPopular() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importPopularGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleImportHot() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importHotGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import hot games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleRefreshStale() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshStaleCache(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh cache'
  } finally {
    cacheImporting.value = false
  }
}

function formatCacheDate(dateStr: string | null): string {
  if (!dateStr) return 'Never'
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

async function loadLocations() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    locations.value = await getAllLocations(token)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load locations'
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingLocation.value = null
  form.name = ''
  form.city = ''
  form.state = ''
  form.venue = ''
  form.startDate = ''
  form.endDate = ''
  showDialog.value = true
}

function openEditDialog(location: EventLocation) {
  editingLocation.value = location
  form.name = location.name
  form.city = location.city
  form.state = location.state
  form.venue = location.venue || ''
  form.startDate = location.startDate
  form.endDate = location.endDate
  showDialog.value = true
}

async function handleSave() {
  if (!form.name.trim() || !form.city.trim() || !form.state.trim() || !form.startDate || !form.endDate) {
    errorMessage.value = 'Name, city, state, and dates are required'
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingLocation.value) {
      // Update existing
      const updated = await updateLocation(token, editingLocation.value.id, {
        name: form.name.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        venue: form.venue.trim() || undefined,
        startDate: form.startDate,
        endDate: form.endDate,
      })
      const index = locations.value.findIndex(l => l.id === updated.id)
      if (index >= 0) locations.value[index] = updated
      successMessage.value = 'Location updated'
    } else {
      // Create new
      const created = await createLocation(token, {
        name: form.name.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        venue: form.venue.trim() || undefined,
        startDate: form.startDate,
        endDate: form.endDate,
      })
      locations.value.unshift(created)
      successMessage.value = 'Location created'
    }

    showDialog.value = false
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save location'
  } finally {
    saving.value = false
  }
}

async function handleDelete(location: EventLocation) {
  if (!confirm(`Delete "${location.name}"? Any LFP posts using this location will have it removed.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteLocation(token, location.id)
    locations.value = locations.value.filter(l => l.id !== location.id)
    successMessage.value = 'Location deleted'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete location'
  }
}

function toggleMergeMode() {
  mergeMode.value = !mergeMode.value
  selectedForMerge.value = []
}

function toggleSelectForMerge(id: string) {
  if (selectedForMerge.value.includes(id)) {
    selectedForMerge.value = selectedForMerge.value.filter(i => i !== id)
  } else {
    selectedForMerge.value.push(id)
  }
}

async function handleMerge() {
  if (selectedForMerge.value.length < 2) {
    errorMessage.value = 'Select at least 2 locations to merge'
    return
  }

  const keepId = selectedForMerge.value[0]!
  const removeIds = selectedForMerge.value.slice(1)
  const keepLocation = locations.value.find(l => l.id === keepId)

  if (!confirm(`Keep "${keepLocation?.name ?? 'selected'}" and merge ${removeIds.length} other location(s) into it? This will update all LFP posts to use the kept location.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await mergeLocations(token, keepId, removeIds)
    locations.value = locations.value.filter(l => !removeIds.includes(l.id))
    selectedForMerge.value = []
    mergeMode.value = false
    successMessage.value = `Merged ${removeIds.length} location(s)`
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to merge locations'
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function isExpired(endDate: string): boolean {
  return new Date(endDate) < new Date()
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Site Administration</h1>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 mb-6 border-b border-gray-200">
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'locations'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'locations'"
      >
        Event Locations
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'bggCache'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'bggCache'"
      >
        BGG Cache
      </button>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- BGG Cache Tab -->
    <div v-if="activeTab === 'bggCache'">
      <div class="card p-6 mb-6">
        <h2 class="text-lg font-semibold mb-4">BoardGameGeek Cache</h2>
        <p class="text-gray-600 mb-6">
          The BGG cache stores board game data locally for fast searching. Games are fetched from BoardGameGeek and cached to avoid rate limits and slow API responses.
        </p>

        <!-- Stats -->
        <div v-if="cacheLoading" class="text-center py-4">
          <svg class="w-6 h-6 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <div v-else-if="cacheStats" class="grid grid-cols-3 gap-4 mb-6">
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.totalGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">Total Games</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.rankedGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">With Rankings</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-sm font-medium text-gray-900">{{ formatCacheDate(cacheStats.oldestCache) }}</div>
            <div class="text-sm text-gray-500">Oldest Entry</div>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <div class="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
            <div>
              <div class="font-medium text-blue-900">Import Popular Games</div>
              <div class="text-sm text-blue-700">Imports ~100 top-rated BGG games + current hot list</div>
            </div>
            <button
              class="btn-primary"
              :disabled="cacheImporting"
              @click="handleImportPopular"
            >
              <svg v-if="cacheImporting" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Import Popular
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Import Hot Games</div>
              <div class="text-sm text-gray-600">Imports BGG's current "hot" list (~50 games)</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleImportHot"
            >
              Import Hot
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Refresh Stale Entries</div>
              <div class="text-sm text-gray-600">Updates cache entries older than 7 days</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleRefreshStale"
            >
              Refresh Stale
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Locations Tab -->
    <div v-if="activeTab === 'locations'">
      <div class="flex items-center justify-between mb-6">
        <p class="text-gray-500">Manage event locations for Looking For Players</p>
        <div class="flex gap-2">
          <button
            v-if="!mergeMode"
            class="btn-outline"
            @click="toggleMergeMode"
          >
            Merge Duplicates
          </button>
          <button
            v-if="mergeMode"
            class="btn-ghost"
            @click="toggleMergeMode"
          >
            Cancel
          </button>
          <button
            v-if="mergeMode && selectedForMerge.length >= 2"
            class="btn-primary"
            @click="handleMerge"
          >
            Merge Selected ({{ selectedForMerge.length }})
          </button>
          <button
            v-if="!mergeMode"
            class="btn-primary"
            @click="openCreateDialog"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Location
          </button>
        </div>
      </div>

    <!-- Merge instructions -->
    <div v-if="mergeMode" class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
      <p class="text-blue-800">
        <strong>Merge Mode:</strong> Select locations to merge. The first selected location will be kept, and all others will be deleted. Any LFP posts using the deleted locations will be updated to use the kept one.
      </p>
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
      <div v-if="locations.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
        </svg>
        <p class="text-lg font-medium">No event locations yet</p>
        <p>Add your first event location to get started.</p>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="location in locations"
          :key="location.id"
          class="card p-4 flex items-center gap-4"
          :class="{
            'opacity-50': isExpired(location.endDate),
            'ring-2 ring-primary-500': selectedForMerge.includes(location.id),
          }"
        >
          <!-- Merge checkbox -->
          <div v-if="mergeMode" class="flex-shrink-0">
            <input
              type="checkbox"
              :checked="selectedForMerge.includes(location.id)"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              @change="toggleSelectForMerge(location.id)"
            />
          </div>

          <!-- Location info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-gray-900">{{ location.name }}</h3>
              <span v-if="isExpired(location.endDate)" class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">
                Expired
              </span>
              <span v-if="selectedForMerge[0] === location.id" class="text-xs px-2 py-0.5 rounded-full bg-primary-100 text-primary-700">
                Keep this one
              </span>
            </div>
            <p class="text-sm text-gray-500">
              {{ location.city }}, {{ location.state }}
              <span v-if="location.venue"> &bull; {{ location.venue }}</span>
            </p>
            <p class="text-sm text-gray-500">
              {{ formatDate(location.startDate) }} - {{ formatDate(location.endDate) }}
            </p>
          </div>

          <!-- Actions -->
          <div v-if="!mergeMode" class="flex gap-2 flex-shrink-0">
            <button
              class="btn-ghost text-gray-600 text-sm"
              @click="openEditDialog(location)"
            >
              Edit
            </button>
            <button
              class="btn-ghost text-red-600 text-sm"
              @click="handleDelete(location)"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Create/Edit Dialog -->
    <div v-if="showDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-4">
          {{ editingLocation ? 'Edit Location' : 'Add Event Location' }}
        </h3>

        <div class="space-y-4">
          <div>
            <label class="label">Event Name *</label>
            <input
              v-model="form.name"
              type="text"
              class="input"
              placeholder="e.g., Dice Tower West 2026"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">City *</label>
              <input
                v-model="form.city"
                type="text"
                class="input"
                placeholder="City"
              />
            </div>
            <div>
              <label class="label">State *</label>
              <input
                v-model="form.state"
                type="text"
                class="input"
                placeholder="State"
              />
            </div>
          </div>

          <div>
            <label class="label">Venue</label>
            <input
              v-model="form.venue"
              type="text"
              class="input"
              placeholder="e.g., Las Vegas Convention Center"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Start Date *</label>
              <input
                v-model="form.startDate"
                type="date"
                class="input"
              />
            </div>
            <div>
              <label class="label">End Date *</label>
              <input
                v-model="form.endDate"
                type="date"
              class="input"
              />
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showDialog = false" :disabled="saving">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSave" :disabled="saving">
            <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingLocation ? 'Save Changes' : 'Add Location' }}
          </button>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>
