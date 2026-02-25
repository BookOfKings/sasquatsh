<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getPlayerRequests,
  getMyPlayerRequests,
  createPlayerRequest,
  updatePlayerRequest,
  deletePlayerRequest,
} from '@/services/socialApi'
import type { PlayerRequest, CreatePlayerRequestInput } from '@/types/social'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const requests = ref<PlayerRequest[]>([])
const myRequests = ref<PlayerRequest[]>([])
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref<'browse' | 'mine'>('browse')
const showCreateDialog = ref(false)
const creating = ref(false)

const filters = reactive({
  city: '',
  state: '',
})

const form = reactive<CreatePlayerRequestInput>({
  title: '',
  description: '',
  gamePreferences: '',
  city: '',
  state: '',
  availableDays: '',
  playerCountNeeded: 1,
})

const hasFilters = computed(() => filters.city || filters.state)

onMounted(async () => {
  await loadRequests()
  if (auth.isAuthenticated.value) {
    await loadMyRequests()
  }
})

async function loadRequests() {
  loading.value = true
  errorMessage.value = ''

  try {
    requests.value = await getPlayerRequests(
      hasFilters.value ? { city: filters.city, state: filters.state } : undefined
    )
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load requests'
  } finally {
    loading.value = false
  }
}

async function loadMyRequests() {
  try {
    const token = await auth.getIdToken()
    if (token) {
      myRequests.value = await getMyPlayerRequests(token)
    }
  } catch (err) {
    console.error('Failed to load my requests:', err)
  }
}

function applyFilters() {
  loadRequests()
}

function clearFilters() {
  filters.city = ''
  filters.state = ''
  loadRequests()
}

function openCreateDialog() {
  if (!auth.isAuthenticated.value) {
    router.push({ name: 'login', query: { redirect: '/looking-for-players' } })
    return
  }
  resetForm()
  showCreateDialog.value = true
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.gamePreferences = ''
  form.city = ''
  form.state = ''
  form.availableDays = ''
  form.playerCountNeeded = 1
}

async function handleCreate() {
  if (!form.title.trim()) {
    errorMessage.value = 'Title is required'
    return
  }

  creating.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    const newRequest = await createPlayerRequest(token, form)
    myRequests.value.unshift(newRequest)
    requests.value.unshift(newRequest)
    showCreateDialog.value = false
    successMessage.value = 'Request posted successfully!'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create request'
  } finally {
    creating.value = false
  }
}

async function handleDeactivate(request: PlayerRequest) {
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await updatePlayerRequest(token, request.id, { isActive: false })
    request.isActive = false
    requests.value = requests.value.filter(r => r.id !== request.id)
    successMessage.value = 'Request deactivated'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to deactivate'
  }
}

async function handleDelete(request: PlayerRequest) {
  if (!confirm('Are you sure you want to delete this request?')) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deletePlayerRequest(token, request.id)
    myRequests.value = myRequests.value.filter(r => r.id !== request.id)
    requests.value = requests.value.filter(r => r.id !== request.id)
    successMessage.value = 'Request deleted'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete'
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function getTimeAgo(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays} days ago`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`
  return formatDate(dateStr)
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold">Looking for Players</h1>
        <p class="text-gray-500">Find or post requests for game night companions</p>
      </div>
      <button class="btn-primary" @click="openCreateDialog">
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Post Request
      </button>
    </div>

    <!-- Success/Error Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- Tabs (if authenticated) -->
    <div v-if="auth.isAuthenticated.value" class="flex gap-2 mb-6">
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'browse' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'browse'"
      >
        Browse All
      </button>
      <button
        class="px-4 py-2 rounded-lg font-medium transition-colors"
        :class="activeTab === 'mine' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
        @click="activeTab = 'mine'"
      >
        My Requests ({{ myRequests.length }})
      </button>
    </div>

    <!-- Filters -->
    <div v-if="activeTab === 'browse'" class="card p-4 mb-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="flex-1 min-w-[150px]">
          <label class="label">City</label>
          <input
            v-model="filters.city"
            type="text"
            class="input"
            placeholder="Filter by city"
          />
        </div>
        <div class="w-32">
          <label class="label">State</label>
          <input
            v-model="filters.state"
            type="text"
            class="input"
            placeholder="State"
          />
        </div>
        <div class="flex gap-2">
          <button class="btn-primary" @click="applyFilters">
            Filter
          </button>
          <button v-if="hasFilters" class="btn-ghost" @click="clearFilters">
            Clear
          </button>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Browse Tab -->
    <template v-else-if="activeTab === 'browse'">
      <div v-if="requests.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
        </svg>
        <p class="text-lg font-medium">No requests found</p>
        <p>Be the first to post a request!</p>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="request in requests"
          :key="request.id"
          class="card p-4"
        >
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 rounded-full bg-secondary-500 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="request.user?.avatarUrl"
                :src="request.user.avatarUrl"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-2">
                <div>
                  <h3 class="font-semibold text-gray-900">{{ request.title }}</h3>
                  <p class="text-sm text-gray-500">
                    {{ request.user?.displayName || 'Anonymous' }}
                    <span v-if="request.city || request.state">
                      &bull; {{ [request.city, request.state].filter(Boolean).join(', ') }}
                    </span>
                  </p>
                </div>
                <span class="text-xs text-gray-400 whitespace-nowrap">{{ getTimeAgo(request.createdAt) }}</span>
              </div>

              <p v-if="request.description" class="mt-2 text-gray-600">
                {{ request.description }}
              </p>

              <div class="flex flex-wrap gap-2 mt-3">
                <span v-if="request.gamePreferences" class="chip bg-primary-100 text-primary-700 text-xs">
                  {{ request.gamePreferences }}
                </span>
                <span v-if="request.availableDays" class="chip bg-gray-100 text-gray-700 text-xs">
                  {{ request.availableDays }}
                </span>
                <span v-if="request.playerCountNeeded > 1" class="chip bg-secondary-100 text-secondary-700 text-xs">
                  Looking for {{ request.playerCountNeeded }} players
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- My Requests Tab -->
    <template v-else-if="activeTab === 'mine'">
      <div v-if="myRequests.length === 0" class="text-center py-12 text-gray-500">
        <p class="text-lg font-medium">You haven't posted any requests</p>
        <button class="btn-primary mt-4" @click="openCreateDialog">
          Post Your First Request
        </button>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="request in myRequests"
          :key="request.id"
          class="card p-4"
          :class="{ 'opacity-60': !request.isActive }"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <h3 class="font-semibold text-gray-900">{{ request.title }}</h3>
                <span v-if="!request.isActive" class="chip-warning text-xs">Inactive</span>
              </div>
              <p class="text-sm text-gray-500">
                Posted {{ getTimeAgo(request.createdAt) }}
                <span v-if="request.city || request.state">
                  &bull; {{ [request.city, request.state].filter(Boolean).join(', ') }}
                </span>
              </p>
              <p v-if="request.description" class="mt-2 text-gray-600">
                {{ request.description }}
              </p>
            </div>

            <div class="flex gap-2">
              <button
                v-if="request.isActive"
                class="btn-ghost text-gray-500 text-sm"
                @click="handleDeactivate(request)"
              >
                Deactivate
              </button>
              <button
                class="btn-ghost text-red-500 text-sm"
                @click="handleDelete(request)"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showCreateDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10 max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">Post a Request</h3>

        <div class="space-y-4">
          <div>
            <label class="label">Title *</label>
            <input
              v-model="form.title"
              type="text"
              class="input"
              placeholder="e.g., Looking for Catan players in Seattle"
            />
          </div>

          <div>
            <label class="label">Description</label>
            <textarea
              v-model="form.description"
              rows="3"
              class="input"
              placeholder="Tell others about what you're looking for..."
            ></textarea>
          </div>

          <div>
            <label class="label">Game Preferences</label>
            <input
              v-model="form.gamePreferences"
              type="text"
              class="input"
              placeholder="e.g., Strategy games, party games, heavy euros"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">City</label>
              <input
                v-model="form.city"
                type="text"
                class="input"
                placeholder="City"
              />
            </div>
            <div>
              <label class="label">State</label>
              <input
                v-model="form.state"
                type="text"
                class="input"
                placeholder="State"
              />
            </div>
          </div>

          <div>
            <label class="label">Availability</label>
            <input
              v-model="form.availableDays"
              type="text"
              class="input"
              placeholder="e.g., Weekends, Friday nights"
            />
          </div>

          <div>
            <label class="label">Players Needed</label>
            <input
              v-model.number="form.playerCountNeeded"
              type="number"
              class="input w-24"
              min="1"
            />
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showCreateDialog = false" :disabled="creating">
            Cancel
          </button>
          <button class="btn-primary" @click="handleCreate" :disabled="creating">
            <svg v-if="creating" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Post Request
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
