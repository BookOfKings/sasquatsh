<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ScryfallCard, MtgDeckCard, MtgFormat } from '@/types/mtg'
import { parseManaCost, getManaSymbolUrl } from '@/types/mtg'
import CardSearch from './CardSearch.vue'
import CardImage from './CardImage.vue'
import DeckList from './DeckList.vue'

const props = defineProps<{
  cards: MtgDeckCard[]
  format?: MtgFormat | null
  hasCommander?: boolean
}>()

const emit = defineEmits<{
  (e: 'add-card', card: ScryfallCard, board: string, quantity: number): void
  (e: 'update-quantity', scryfallId: string, board: string, quantity: number): void
  (e: 'remove-card', scryfallId: string, board: string): void
  (e: 'set-commander', card: ScryfallCard, isPartner: boolean): void
}>()

const searchQuery = ref('')
const selectedBoard = ref<'main' | 'sideboard' | 'maybeboard'>('main')
const addQuantity = ref(1)
const previewCard = ref<ScryfallCard | null>(null)

// Calculate deck statistics
const deckStats = computed(() => {
  const stats = {
    total: 0,
    mainDeck: 0,
    sideboard: 0,
    creatures: 0,
    spells: 0,
    lands: 0,
    averageCmc: 0,
  }

  let totalCmc = 0
  let cardsWithCmc = 0

  for (const entry of props.cards) {
    if (entry.board === 'main' || entry.board === 'commander') {
      stats.mainDeck += entry.quantity
      stats.total += entry.quantity

      const typeLine = entry.card?.typeLine?.toLowerCase() || ''
      if (typeLine.includes('creature')) {
        stats.creatures += entry.quantity
      } else if (typeLine.includes('land')) {
        stats.lands += entry.quantity
      } else {
        stats.spells += entry.quantity
      }

      if (entry.card && !typeLine.includes('land') && entry.card.cmc > 0) {
        totalCmc += entry.card.cmc * entry.quantity
        cardsWithCmc += entry.quantity
      }
    } else if (entry.board === 'sideboard') {
      stats.sideboard += entry.quantity
    }
  }

  stats.averageCmc = cardsWithCmc > 0 ? Math.round((totalCmc / cardsWithCmc) * 100) / 100 : 0

  return stats
})

// Deck validation
const validation = computed(() => {
  const issues: string[] = []
  const warnings: string[] = []

  if (!props.format) {
    return { issues, warnings, isValid: true }
  }

  // Check deck size
  if (props.format.minDeckSize && deckStats.value.mainDeck < props.format.minDeckSize) {
    issues.push(`Deck needs ${props.format.minDeckSize - deckStats.value.mainDeck} more cards (minimum ${props.format.minDeckSize})`)
  }

  if (props.format.maxDeckSize && deckStats.value.mainDeck > props.format.maxDeckSize) {
    issues.push(`Deck has ${deckStats.value.mainDeck - props.format.maxDeckSize} too many cards (maximum ${props.format.maxDeckSize})`)
  }

  // Check sideboard size
  if (props.format.hasSideboard && deckStats.value.sideboard > props.format.sideboardSize) {
    issues.push(`Sideboard has ${deckStats.value.sideboard - props.format.sideboardSize} too many cards (maximum ${props.format.sideboardSize})`)
  }

  // Check copy limits
  if (props.format.maxCopies) {
    const copyCounts: Record<string, number> = {}
    for (const entry of props.cards) {
      if (entry.board === 'main' || entry.board === 'sideboard') {
        const name = entry.card?.name || entry.scryfallId
        // Skip basic lands
        const isBasicLand = entry.card?.typeLine?.toLowerCase().includes('basic land')
        if (!isBasicLand) {
          copyCounts[name] = (copyCounts[name] || 0) + entry.quantity
        }
      }
    }

    for (const [name, count] of Object.entries(copyCounts)) {
      if (count > props.format.maxCopies) {
        issues.push(`${name}: ${count} copies (maximum ${props.format.maxCopies})`)
      }
    }
  }

  // Check commander requirement
  if (props.format.hasCommander && props.hasCommander === false) {
    warnings.push('No commander selected')
  }

  return {
    issues,
    warnings,
    isValid: issues.length === 0,
  }
})

function handleCardSelect(card: ScryfallCard) {
  emit('add-card', card, selectedBoard.value, addQuantity.value)
  searchQuery.value = ''
  addQuantity.value = 1
}

function handleUpdateQuantity(scryfallId: string, board: string, quantity: number) {
  emit('update-quantity', scryfallId, board, quantity)
}

function handleRemoveCard(scryfallId: string, board: string) {
  emit('remove-card', scryfallId, board)
}

function handleCardClick(card: ScryfallCard) {
  previewCard.value = card
}

function setAsCommander(card: ScryfallCard, isPartner = false) {
  emit('set-commander', card, isPartner)
}

function closePreview() {
  previewCard.value = null
}
</script>

<template>
  <div class="deck-builder grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- Left: Card Search & Add -->
    <div class="lg:col-span-2 space-y-4">
      <!-- Search Bar -->
      <div class="bg-white rounded-lg border p-4">
        <h3 class="font-semibold mb-3">Add Cards</h3>
        <CardSearch
          v-model="searchQuery"
          placeholder="Search for cards..."
          @select="handleCardSelect"
        />

        <!-- Add options -->
        <div class="flex items-center gap-4 mt-3">
          <div class="flex items-center gap-2">
            <label class="text-sm text-gray-600">Quantity:</label>
            <input
              v-model.number="addQuantity"
              type="number"
              min="1"
              max="99"
              class="w-16 input text-center"
            />
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-gray-600">Add to:</label>
            <select v-model="selectedBoard" class="input">
              <option value="main">Main Deck</option>
              <option v-if="format?.hasSideboard" value="sideboard">Sideboard</option>
              <option value="maybeboard">Maybeboard</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Deck Statistics -->
      <div class="bg-white rounded-lg border p-4">
        <h3 class="font-semibold mb-3">Deck Statistics</h3>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="text-center">
            <div class="text-2xl font-bold" :class="validation.isValid ? 'text-green-600' : 'text-red-600'">
              {{ deckStats.mainDeck }}
            </div>
            <div class="text-sm text-gray-500">
              Main Deck
              <span v-if="format?.minDeckSize">/ {{ format.minDeckSize }}</span>
            </div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-700">{{ deckStats.creatures }}</div>
            <div class="text-sm text-gray-500">Creatures</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-700">{{ deckStats.spells }}</div>
            <div class="text-sm text-gray-500">Spells</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-gray-700">{{ deckStats.lands }}</div>
            <div class="text-sm text-gray-500">Lands</div>
          </div>
        </div>
        <div class="mt-3 text-center text-sm text-gray-500">
          Average CMC: <span class="font-medium">{{ deckStats.averageCmc }}</span>
        </div>

        <!-- Validation Messages -->
        <div v-if="validation.issues.length > 0" class="mt-4 p-3 bg-red-50 rounded-lg">
          <h4 class="text-sm font-medium text-red-700 mb-1">Issues:</h4>
          <ul class="text-sm text-red-600 list-disc list-inside">
            <li v-for="(issue, idx) in validation.issues" :key="idx">{{ issue }}</li>
          </ul>
        </div>
        <div v-if="validation.warnings.length > 0" class="mt-4 p-3 bg-amber-50 rounded-lg">
          <h4 class="text-sm font-medium text-amber-700 mb-1">Warnings:</h4>
          <ul class="text-sm text-amber-600 list-disc list-inside">
            <li v-for="(warning, idx) in validation.warnings" :key="idx">{{ warning }}</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Right: Deck List -->
    <div class="lg:col-span-1">
      <div class="bg-white rounded-lg border p-4 sticky top-4 max-h-[calc(100vh-120px)] overflow-y-auto">
        <h3 class="font-semibold mb-3">Deck List</h3>
        <DeckList
          :cards="cards"
          :show-quantity-controls="true"
          :show-remove="true"
          :group-by-type="true"
          @update:quantity="handleUpdateQuantity"
          @remove="handleRemoveCard"
          @card-click="handleCardClick"
        />
      </div>
    </div>

    <!-- Card Preview Modal -->
    <Teleport to="body">
      <div
        v-if="previewCard"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
        @click="closePreview"
      >
        <div
          class="bg-white rounded-xl p-6 max-w-md w-full mx-4 shadow-2xl"
          @click.stop
        >
          <div class="flex justify-between items-start mb-4">
            <div>
              <h3 class="text-xl font-bold">{{ previewCard.name }}</h3>
              <p class="text-gray-500">{{ previewCard.typeLine }}</p>
            </div>
            <button
              class="text-gray-400 hover:text-gray-600"
              @click="closePreview"
            >
              <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
            </button>
          </div>

          <div class="flex gap-4">
            <div class="w-40 flex-shrink-0">
              <CardImage :card="previewCard" size="normal" />
            </div>
            <div class="flex-1 space-y-3">
              <!-- Mana Cost -->
              <div v-if="previewCard.manaCost" class="flex items-center gap-1">
                <img
                  v-for="(symbol, idx) in parseManaCost(previewCard.manaCost)"
                  :key="idx"
                  :src="getManaSymbolUrl(symbol)"
                  :alt="symbol"
                  class="w-5 h-5"
                />
                <span class="text-gray-500 ml-2">({{ previewCard.cmc }})</span>
              </div>

              <!-- Oracle Text -->
              <p v-if="previewCard.oracleText" class="text-sm whitespace-pre-line">
                {{ previewCard.oracleText }}
              </p>

              <!-- Power/Toughness or Loyalty -->
              <p v-if="previewCard.power && previewCard.toughness" class="font-bold">
                {{ previewCard.power }}/{{ previewCard.toughness }}
              </p>
              <p v-else-if="previewCard.loyalty" class="font-bold">
                Loyalty: {{ previewCard.loyalty }}
              </p>

              <!-- Legalities -->
              <div class="text-xs text-gray-500">
                <span>{{ previewCard.setName }} ({{ previewCard.rarity }})</span>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="mt-4 flex gap-2">
            <button
              v-if="format?.hasCommander"
              class="btn btn-secondary flex-1"
              @click="setAsCommander(previewCard!); closePreview()"
            >
              Set as Commander
            </button>
            <button
              class="btn btn-primary flex-1"
              @click="handleCardSelect(previewCard!); closePreview()"
            >
              Add to Deck
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
