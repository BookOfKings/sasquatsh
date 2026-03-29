<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getDeck,
  createDeck,
  updateDeck,
  addCardsToDeck,
  updateCardQuantities,
  removeCardsFromDeck,
  setCommander,
} from '@/services/mtgDeckApi'
import { getFormats, getCardById } from '@/services/scryfallApi'
import type { MtgDeck, MtgDeckCard, MtgFormat, ScryfallCard, CreateDeckInput } from '@/types/mtg'
import FormatSelector from '@/components/mtg/FormatSelector.vue'
import PowerLevelSelector from '@/components/mtg/PowerLevelSelector.vue'
import CardSearch from '@/components/mtg/CardSearch.vue'
import DeckBuilder from '@/components/mtg/DeckBuilder.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isEditing = computed(() => route.params.id && route.params.id !== 'new')
const deckId = computed(() => isEditing.value ? route.params.id as string : null)

const deck = ref<MtgDeck | null>(null)
const cards = ref<MtgDeckCard[]>([])
const formats = ref<MtgFormat[]>([])
const selectedFormat = ref<MtgFormat | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

// Form fields
const deckName = ref('')
const deckDescription = ref('')
const formatId = ref<string | null>(null)
const powerLevel = ref<number | null>(null)
const isPublic = ref(false)
const commanderCard = ref<ScryfallCard | null>(null)
const partnerCommanderCard = ref<ScryfallCard | null>(null)

// Track unsaved changes
const hasChanges = ref(false)

async function loadDeck() {
  if (!deckId.value) {
    loading.value = false
    return
  }

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    deck.value = await getDeck(token, deckId.value)
    cards.value = deck.value.cards || []

    // Populate form
    deckName.value = deck.value.name
    deckDescription.value = deck.value.description || ''
    formatId.value = deck.value.formatId
    powerLevel.value = deck.value.powerLevel
    isPublic.value = deck.value.isPublic
    commanderCard.value = deck.value.commander || null
    partnerCommanderCard.value = deck.value.partnerCommander || null

    // Find the format
    if (formatId.value) {
      selectedFormat.value = formats.value.find(f => f.id === formatId.value) || null
    }
  } catch (err) {
    console.error('Failed to load deck:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load deck'
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

function handleFormatChange(format: MtgFormat | null) {
  selectedFormat.value = format
  formatId.value = format?.id || null
  hasChanges.value = true
}

async function saveDeck() {
  if (!deckName.value.trim()) {
    alert('Please enter a deck name')
    return
  }

  saving.value = true
  error.value = null

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    if (isEditing.value && deck.value) {
      // Update existing deck
      await updateDeck(token, deck.value.id, {
        name: deckName.value,
        description: deckDescription.value || undefined,
        formatId: formatId.value || undefined,
        powerLevel: powerLevel.value || undefined,
        isPublic: isPublic.value,
        commanderScryfallId: commanderCard.value?.scryfallId,
        partnerCommanderScryfallId: partnerCommanderCard.value?.scryfallId,
      })

      hasChanges.value = false
    } else {
      // Create new deck
      const input: CreateDeckInput = {
        name: deckName.value,
        description: deckDescription.value || undefined,
        formatId: formatId.value || undefined,
        powerLevel: powerLevel.value || undefined,
        isPublic: isPublic.value,
        commanderScryfallId: commanderCard.value?.scryfallId,
        partnerCommanderScryfallId: partnerCommanderCard.value?.scryfallId,
      }

      const newDeck = await createDeck(token, input)
      deck.value = newDeck

      // Navigate to edit mode
      router.replace(`/mtg/decks/${newDeck.id}/edit`)
    }
  } catch (err) {
    console.error('Failed to save deck:', err)
    error.value = err instanceof Error ? err.message : 'Failed to save deck'
  } finally {
    saving.value = false
  }
}

async function handleAddCard(card: ScryfallCard, board: string, quantity: number) {
  if (!deck.value) {
    // Need to create deck first
    await saveDeck()
    if (!deck.value) return
  }

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    // Check if card already exists in this board
    const existingIndex = cards.value.findIndex(
      c => c.scryfallId === card.scryfallId && c.board === board
    )

    if (existingIndex >= 0) {
      // Update quantity
      const existing = cards.value[existingIndex]!
      const newQuantity = existing.quantity + quantity
      await updateCardQuantities(token, deck.value.id, [
        { scryfallId: card.scryfallId, quantity: newQuantity, board },
      ])
      existing.quantity = newQuantity
    } else {
      // Add new card
      const typedBoard = board as 'main' | 'sideboard' | 'maybeboard' | 'commander'
      await addCardsToDeck(token, deck.value.id, [
        { scryfallId: card.scryfallId, quantity, board: typedBoard },
      ])
      cards.value.push({
        id: `temp-${Date.now()}`,
        deckId: deck.value.id,
        scryfallId: card.scryfallId,
        quantity,
        board: typedBoard,
        card,
      })
    }
  } catch (err) {
    console.error('Failed to add card:', err)
    alert(err instanceof Error ? err.message : 'Failed to add card')
  }
}

async function handleUpdateQuantity(scryfallId: string, board: string, quantity: number) {
  if (!deck.value) return

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await updateCardQuantities(token, deck.value.id, [
      { scryfallId, quantity, board },
    ])

    const index = cards.value.findIndex(c => c.scryfallId === scryfallId && c.board === board)
    if (index >= 0) {
      const existing = cards.value[index]!
      existing.quantity = quantity
    }
  } catch (err) {
    console.error('Failed to update quantity:', err)
    alert(err instanceof Error ? err.message : 'Failed to update quantity')
  }
}

async function handleRemoveCard(scryfallId: string, board: string) {
  if (!deck.value) return

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await removeCardsFromDeck(token, deck.value.id, [{ scryfallId, board }])
    cards.value = cards.value.filter(c => !(c.scryfallId === scryfallId && c.board === board))
  } catch (err) {
    console.error('Failed to remove card:', err)
    alert(err instanceof Error ? err.message : 'Failed to remove card')
  }
}

async function handleSetCommander(card: ScryfallCard, isPartner: boolean) {
  if (!deck.value) {
    // Just store locally until deck is created
    if (isPartner) {
      partnerCommanderCard.value = card
    } else {
      commanderCard.value = card
    }
    hasChanges.value = true
    return
  }

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    await setCommander(token, deck.value.id, card.scryfallId, isPartner)

    if (isPartner) {
      partnerCommanderCard.value = card
    } else {
      commanderCard.value = card
    }
  } catch (err) {
    console.error('Failed to set commander:', err)
    alert(err instanceof Error ? err.message : 'Failed to set commander')
  }
}

async function handleCommanderSelect(card: ScryfallCard) {
  // Load full card data
  try {
    const fullCard = await getCardById(card.scryfallId)
    commanderCard.value = fullCard
    hasChanges.value = true

    if (deck.value) {
      const token = await auth.getIdToken()
      if (token) {
        await setCommander(token, deck.value.id, fullCard.scryfallId, false)
      }
    }
  } catch (err) {
    console.error('Failed to load commander:', err)
  }
}

function goBack() {
  if (hasChanges.value) {
    if (!confirm('You have unsaved changes. Are you sure you want to leave?')) {
      return
    }
  }
  router.push('/mtg/decks')
}

// Watch for format changes
watch(formatId, (newId) => {
  selectedFormat.value = formats.value.find(f => f.id === newId) || null
})

// Mark changes
watch([deckName, deckDescription, powerLevel, isPublic], () => {
  hasChanges.value = true
})

onMounted(async () => {
  await loadFormats()
  await loadDeck()
})
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-7xl">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-4">
        <button
          class="text-gray-500 hover:text-gray-700"
          @click="goBack"
        >
          <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
        </button>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ isEditing ? 'Edit Deck' : 'Create New Deck' }}
          </h1>
          <p v-if="deck?.name" class="text-gray-500">{{ deck.name }}</p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <span v-if="hasChanges" class="text-sm text-amber-600">Unsaved changes</span>
        <button
          class="btn btn-primary"
          :disabled="saving || !deckName.trim()"
          @click="saveDeck"
        >
          <template v-if="saving">
            <svg class="w-4 h-4 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Saving...
          </template>
          <template v-else>
            Save Deck
          </template>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 animate-spin mx-auto text-primary-500 mb-4" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="text-gray-500">Loading deck...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center py-12">
      <p class="text-red-500 mb-4">{{ error }}</p>
      <button class="btn btn-primary" @click="loadDeck">Try Again</button>
    </div>

    <!-- Deck Editor -->
    <div v-else class="space-y-6">
      <!-- Deck Settings -->
      <div class="bg-white rounded-lg border p-6">
        <h2 class="text-lg font-semibold mb-4">Deck Settings</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Deck Name *
            </label>
            <input
              v-model="deckName"
              type="text"
              class="input"
              placeholder="My Awesome Deck"
            />
          </div>

          <!-- Format -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Format
            </label>
            <FormatSelector
              v-model="formatId"
              @change="handleFormatChange"
            />
          </div>

          <!-- Description -->
          <div class="md:col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              v-model="deckDescription"
              class="input h-24"
              placeholder="Describe your deck strategy..."
            ></textarea>
          </div>

          <!-- Commander (if Commander format) -->
          <div v-if="selectedFormat?.hasCommander">
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Commander
            </label>
            <CardSearch
              model-value=""
              placeholder="Search for your commander..."
              filter-commander
              @select="handleCommanderSelect"
            />
            <div v-if="commanderCard" class="mt-2 p-2 bg-amber-50 rounded flex items-center gap-2">
              <img
                v-if="commanderCard.imageUris.small"
                :src="commanderCard.imageUris.small"
                :alt="commanderCard.name"
                class="w-10 h-14 rounded"
              />
              <span class="font-medium">{{ commanderCard.name }}</span>
              <button
                class="ml-auto text-gray-400 hover:text-gray-600"
                @click="commanderCard = null; hasChanges = true"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Power Level -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Power Level
            </label>
            <PowerLevelSelector
              v-model="powerLevel"
              show-description
            />
          </div>

          <!-- Visibility -->
          <div class="md:col-span-2">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="isPublic"
                type="checkbox"
                class="w-4 h-4 text-primary-600 rounded"
              />
              <span class="text-sm text-gray-700">Make this deck public</span>
            </label>
            <p class="text-xs text-gray-500 mt-1">
              Public decks can be viewed by anyone browsing decks
            </p>
          </div>
        </div>
      </div>

      <!-- Deck Builder (only show after deck is created) -->
      <div v-if="deck">
        <DeckBuilder
          :cards="cards"
          :format="selectedFormat"
          :has-commander="!!commanderCard"
          @add-card="handleAddCard"
          @update-quantity="handleUpdateQuantity"
          @remove-card="handleRemoveCard"
          @set-commander="handleSetCommander"
        />
      </div>

      <!-- Prompt to save first for new decks -->
      <div v-else class="bg-gray-50 rounded-lg border-2 border-dashed p-8 text-center">
        <svg class="w-12 h-12 mx-auto text-gray-400 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21,16.5C21,16.88 20.79,17.21 20.47,17.38L12.57,21.82C12.41,21.94 12.21,22 12,22C11.79,22 11.59,21.94 11.43,21.82L3.53,17.38C3.21,17.21 3,16.88 3,16.5V7.5C3,7.12 3.21,6.79 3.53,6.62L11.43,2.18C11.59,2.06 11.79,2 12,2C12.21,2 12.41,2.06 12.57,2.18L20.47,6.62C20.79,6.79 21,7.12 21,7.5V16.5M12,4.15L5,8.09V15.91L12,19.85L19,15.91V8.09L12,4.15Z"/>
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">Save to start building</h3>
        <p class="text-gray-500 mb-4">
          Enter a deck name and save to start adding cards
        </p>
        <button
          class="btn btn-primary"
          :disabled="!deckName.trim() || saving"
          @click="saveDeck"
        >
          Save & Start Building
        </button>
      </div>
    </div>
  </div>
</template>
