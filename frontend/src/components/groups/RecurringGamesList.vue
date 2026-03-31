<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { RecurringGame, CreateRecurringGameInput, UpdateRecurringGameInput } from '@/types/groups'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getRecurringGames,
  createRecurringGame,
  updateRecurringGame,
  deleteRecurringGame,
} from '@/services/recurringGamesApi'
import { canCreateRecurringGame } from '@/config/subscriptionLimits'
import RecurringGameCard from './RecurringGameCard.vue'
import RecurringGameForm from './RecurringGameForm.vue'

const props = defineProps<{
  groupId: string
  isAdmin: boolean
}>()

const authStore = useAuthStore()

const games = ref<RecurringGame[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// Modal state
const showForm = ref(false)
const editingGame = ref<RecurringGame | undefined>(undefined)

// Delete confirmation state
const showDeleteConfirm = ref(false)
const gameToDelete = ref<RecurringGame | null>(null)
const deleteFutureEvents = ref(false)

// Tier check (default to free if unknown)
const currentTier = ref<'free' | 'basic' | 'pro' | 'premium'>('free')

async function loadGames() {
  loading.value = true
  error.value = null
  try {
    const token = await authStore.getIdToken()
    if (!token) return
    games.value = await getRecurringGames(token, props.groupId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load recurring games'
  } finally {
    loading.value = false
  }
}

function openCreateForm() {
  editingGame.value = undefined
  showForm.value = true
}

function openEditForm(game: RecurringGame) {
  editingGame.value = game
  showForm.value = true
}

async function handleSave(data: CreateRecurringGameInput) {
  try {
    const token = await authStore.getIdToken()
    if (!token) return

    if (editingGame.value) {
      await updateRecurringGame(token, editingGame.value.id, data as UpdateRecurringGameInput)
    } else {
      await createRecurringGame(token, props.groupId, data)
    }

    showForm.value = false
    editingGame.value = undefined
    await loadGames()
  } catch (err: any) {
    error.value = err.message || 'Failed to save recurring game'
  }
}

function confirmDelete(game: RecurringGame) {
  gameToDelete.value = game
  deleteFutureEvents.value = false
  showDeleteConfirm.value = true
}

async function handleDelete() {
  if (!gameToDelete.value) return
  try {
    const token = await authStore.getIdToken()
    if (!token) return
    await deleteRecurringGame(token, gameToDelete.value.id, deleteFutureEvents.value)
    showDeleteConfirm.value = false
    gameToDelete.value = null
    await loadGames()
  } catch (err: any) {
    error.value = err.message || 'Failed to delete recurring game'
  }
}

async function handleToggleActive(game: RecurringGame) {
  try {
    const token = await authStore.getIdToken()
    if (!token) return
    await updateRecurringGame(token, game.id, { isActive: !game.isActive })
    await loadGames()
  } catch (err: any) {
    error.value = err.message || 'Failed to update recurring game'
  }
}

onMounted(() => {
  // Set tier from auth store user if available
  if (authStore.user.value?.subscriptionTier) {
    currentTier.value = authStore.user.value.subscriptionTier as typeof currentTier.value
  }
  loadGames()
})
</script>

<template>
  <!-- Hide entire section if no games and not admin -->
  <div v-if="games.length > 0 || isAdmin">
    <!-- Section header -->
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
        <svg class="w-5 h-5 text-gray-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
        </svg>
        Recurring Games
      </h2>
      <button
        v-if="isAdmin && canCreateRecurringGame(currentTier, games.length)"
        type="button"
        class="btn-primary btn-sm"
        @click="openCreateForm"
      >
        <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Add Recurring Game
      </button>
    </div>

    <!-- Error -->
    <div v-if="error" class="rounded-lg bg-red-50 border border-red-200 p-3 mb-4">
      <p class="text-sm text-red-700">{{ error }}</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
    </div>

    <!-- Empty state for admin -->
    <div
      v-else-if="games.length === 0 && isAdmin"
      class="rounded-lg border-2 border-dashed border-gray-300 p-6 text-center"
    >
      <svg class="w-10 h-10 mx-auto text-gray-300 mb-3" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
      </svg>
      <p class="text-gray-500 mb-3">Set up a recurring game night</p>
      <button
        type="button"
        class="btn-primary"
        @click="openCreateForm"
      >
        Create Recurring Game
      </button>
    </div>

    <!-- Games list -->
    <div v-else class="space-y-3">
      <RecurringGameCard
        v-for="game in games"
        :key="game.id"
        :game="game"
        :is-admin="isAdmin"
        @edit="openEditForm"
        @delete="confirmDelete"
        @toggle-active="handleToggleActive"
      />
    </div>

    <!-- Create/Edit modal -->
    <RecurringGameForm
      v-if="showForm"
      :game="editingGame"
      :group-id="groupId"
      @save="handleSave"
      @cancel="showForm = false"
    />

    <!-- Delete confirmation dialog -->
    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      @click.self="showDeleteConfirm = false"
    >
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Delete Recurring Game</h3>
        <p class="text-sm text-gray-600 mb-4">
          Are you sure you want to delete "{{ gameToDelete?.title }}"?
          This cannot be undone.
        </p>
        <label class="flex items-center gap-2 mb-4 cursor-pointer">
          <input
            type="checkbox"
            v-model="deleteFutureEvents"
            class="w-4 h-4 rounded text-red-500 focus:ring-red-500"
          />
          <span class="text-sm text-gray-700">Also delete future generated events</span>
        </label>
        <div class="flex justify-end gap-3">
          <button
            type="button"
            class="btn-secondary"
            @click="showDeleteConfirm = false"
          >
            Cancel
          </button>
          <button
            type="button"
            class="btn-primary bg-red-600 hover:bg-red-700"
            @click="handleDelete"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
