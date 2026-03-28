<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getMyDecks, deleteDeck, importDeck } from '@/services/mtgDeckApi'
import { getFormats } from '@/services/scryfallApi'
import type { MtgDeck, MtgFormat, ImportDeckInput } from '@/types/mtg'
import { getManaSymbolUrl } from '@/types/mtg'
import DeckImportModal from '@/components/mtg/DeckImportModal.vue'

const router = useRouter()
const auth = useAuthStore()

const decks = ref<MtgDeck[]>([])
const formats = ref<MtgFormat[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const showImportModal = ref(false)
const deletingDeck = ref<string | null>(null)

// Filters
const selectedFormat = ref<string | null>(null)
const searchQuery = ref('')

const filteredDecks = computed(() => {
  let result = decks.value

  if (selectedFormat.value) {
    result = result.filter(d => d.formatId === selectedFormat.value)
  }

  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(d =>
      d.name.toLowerCase().includes(query) ||
      d.description?.toLowerCase().includes(query)
    )
  }

  return result
})

// Group decks by format
const decksByFormat = computed(() => {
  const groups: Record<string, MtgDeck[]> = {}

  for (const deck of filteredDecks.value) {
    const formatName = deck.format?.name || 'No Format'
    if (!groups[formatName]) {
      groups[formatName] = []
    }
    groups[formatName].push(deck)
  }

  return groups
})

async function loadDecks() {
  loading.value = true
  error.value = null

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    decks.value = await getMyDecks(token, selectedFormat.value || undefined)
  } catch (err) {
    console.error('Failed to load decks:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load decks'
  } finally {
    loading.value = false
  }
}

async function loadFormats() {
  try {
    formats.value = await getFormats()
  } catch (err) {
    console.error('Failed to load formats:', err)
  }
}

async function handleDeleteDeck(deckId: string) {
  if (!confirm('Are you sure you want to delete this deck?')) return

  deletingDeck.value = deckId
  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await deleteDeck(token, deckId)
    decks.value = decks.value.filter(d => d.id !== deckId)
  } catch (err) {
    console.error('Failed to delete deck:', err)
    alert(err instanceof Error ? err.message : 'Failed to delete deck')
  } finally {
    deletingDeck.value = null
  }
}

async function handleImport(input: ImportDeckInput) {
  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const result = await importDeck(token, input)
    decks.value.unshift(result.deck)
    showImportModal.value = false

    // Navigate to the new deck
    router.push(`/mtg/decks/${result.deck.id}`)
  } catch (err) {
    console.error('Failed to import deck:', err)
    alert(err instanceof Error ? err.message : 'Failed to import deck')
  }
}

function createNewDeck() {
  router.push('/mtg/decks/new')
}

function viewDeck(deckId: string) {
  router.push(`/mtg/decks/${deckId}`)
}

function editDeck(deckId: string) {
  router.push(`/mtg/decks/${deckId}/edit`)
}

function getColorSymbols(deck: MtgDeck): string[] {
  // Get colors from commander or deck colors
  const colors = deck.commander?.colorIdentity || []
  return colors.map(c => `{${c}}`)
}

onMounted(() => {
  loadDecks()
  loadFormats()
})
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-6xl">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">My Decks</h1>
        <p class="text-gray-500">Manage your Magic: The Gathering deck collection</p>
      </div>
      <div class="flex gap-2">
        <button
          class="btn btn-secondary"
          @click="showImportModal = true"
        >
          <svg class="w-5 h-5 mr-1" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,20H19V18H5M19,9H15V3H9V9H5L12,16L19,9Z"/>
          </svg>
          Import
        </button>
        <button
          class="btn btn-primary"
          @click="createNewDeck"
        >
          <svg class="w-5 h-5 mr-1" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          New Deck
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="bg-white rounded-lg border p-4 mb-6">
      <div class="flex flex-wrap gap-4">
        <div class="flex-1 min-w-[200px]">
          <input
            v-model="searchQuery"
            type="text"
            class="input"
            placeholder="Search decks..."
          />
        </div>
        <div class="w-48">
          <select v-model="selectedFormat" class="input" @change="loadDecks">
            <option :value="null">All Formats</option>
            <option v-for="format in formats" :key="format.id" :value="format.id">
              {{ format.name }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 animate-spin mx-auto text-primary-500 mb-4" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="text-gray-500">Loading your decks...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-12">
      <p class="text-red-500 mb-4">{{ error }}</p>
      <button class="btn btn-primary" @click="loadDecks">Try Again</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="decks.length === 0" class="text-center py-12">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M21,16.5C21,16.88 20.79,17.21 20.47,17.38L12.57,21.82C12.41,21.94 12.21,22 12,22C11.79,22 11.59,21.94 11.43,21.82L3.53,17.38C3.21,17.21 3,16.88 3,16.5V7.5C3,7.12 3.21,6.79 3.53,6.62L11.43,2.18C11.59,2.06 11.79,2 12,2C12.21,2 12.41,2.06 12.57,2.18L20.47,6.62C20.79,6.79 21,7.12 21,7.5V16.5M12,4.15L5,8.09V15.91L12,19.85L19,15.91V8.09L12,4.15Z"/>
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">No decks yet</h3>
      <p class="text-gray-500 mb-4">Create your first deck or import one from Moxfield/Archidekt</p>
      <div class="flex justify-center gap-3">
        <button class="btn btn-secondary" @click="showImportModal = true">
          Import Deck
        </button>
        <button class="btn btn-primary" @click="createNewDeck">
          Create Deck
        </button>
      </div>
    </div>

    <!-- Deck Grid -->
    <div v-else class="space-y-8">
      <div v-for="(formatDecks, formatName) in decksByFormat" :key="formatName">
        <h2 class="text-lg font-semibold mb-3">{{ formatName }}</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="deck in formatDecks"
            :key="deck.id"
            class="bg-white rounded-lg border hover:shadow-md transition-shadow cursor-pointer"
            @click="viewDeck(deck.id)"
          >
            <!-- Deck Card -->
            <div class="p-4">
              <div class="flex items-start gap-3">
                <!-- Commander Image -->
                <div
                  v-if="deck.commander"
                  class="w-16 h-22 flex-shrink-0 rounded overflow-hidden bg-gray-100"
                >
                  <img
                    v-if="deck.commander.imageUris.small"
                    :src="deck.commander.imageUris.small"
                    :alt="deck.commander.name"
                    class="w-full h-full object-cover"
                  />
                </div>
                <div
                  v-else
                  class="w-16 h-22 flex-shrink-0 rounded bg-gradient-to-br from-purple-800 to-blue-900 flex items-center justify-center"
                >
                  <span class="text-white text-2xl font-bold">{{ deck.name.charAt(0) }}</span>
                </div>

                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-gray-900 truncate">{{ deck.name }}</h3>
                  <p v-if="deck.commander" class="text-sm text-gray-500 truncate">
                    {{ deck.commander.name }}
                  </p>
                  <div class="flex items-center gap-2 mt-1">
                    <span class="text-sm text-gray-400">{{ deck.cardCount || 0 }} cards</span>
                    <div v-if="deck.powerLevel" class="flex items-center gap-1">
                      <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100">
                        PL {{ deck.powerLevel }}
                      </span>
                    </div>
                  </div>
                  <!-- Color Identity -->
                  <div v-if="getColorSymbols(deck).length > 0" class="flex items-center gap-0.5 mt-2">
                    <img
                      v-for="(symbol, idx) in getColorSymbols(deck)"
                      :key="idx"
                      :src="getManaSymbolUrl(symbol)"
                      :alt="symbol"
                      class="w-4 h-4"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex border-t">
              <button
                class="flex-1 py-2 text-sm text-gray-600 hover:bg-gray-50 transition-colors"
                @click.stop="editDeck(deck.id)"
              >
                Edit
              </button>
              <button
                class="flex-1 py-2 text-sm text-red-500 hover:bg-red-50 transition-colors border-l"
                :disabled="deletingDeck === deck.id"
                @click.stop="handleDeleteDeck(deck.id)"
              >
                <template v-if="deletingDeck === deck.id">
                  Deleting...
                </template>
                <template v-else>
                  Delete
                </template>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Import Modal -->
    <DeckImportModal
      :open="showImportModal"
      @close="showImportModal = false"
      @import="handleImport"
    />
  </div>
</template>
