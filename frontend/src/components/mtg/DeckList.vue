<script setup lang="ts">
import { computed } from 'vue'
import type { MtgDeckCard, ScryfallCard } from '@/types/mtg'
import { parseManaCost, getManaSymbolUrl } from '@/types/mtg'
import CardImage from './CardImage.vue'

const props = defineProps<{
  cards: MtgDeckCard[]
  showQuantityControls?: boolean
  showRemove?: boolean
  groupByType?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:quantity', scryfallId: string, board: string, quantity: number): void
  (e: 'remove', scryfallId: string, board: string): void
  (e: 'card-click', card: ScryfallCard): void
}>()

// Group cards by board
const cardsByBoard = computed(() => {
  const groups = {
    commander: [] as MtgDeckCard[],
    main: [] as MtgDeckCard[],
    sideboard: [] as MtgDeckCard[],
    maybeboard: [] as MtgDeckCard[],
  }

  for (const card of props.cards) {
    const board = card.board as keyof typeof groups
    if (groups[board]) {
      groups[board].push(card)
    }
  }

  return groups
})

// Group cards by type within main deck
const cardsByType = computed(() => {
  if (!props.groupByType) return null

  const mainCards = cardsByBoard.value.main || []
  const groups = {
    Creatures: [] as MtgDeckCard[],
    Planeswalkers: [] as MtgDeckCard[],
    Instants: [] as MtgDeckCard[],
    Sorceries: [] as MtgDeckCard[],
    Enchantments: [] as MtgDeckCard[],
    Artifacts: [] as MtgDeckCard[],
    Lands: [] as MtgDeckCard[],
    Other: [] as MtgDeckCard[],
  }

  for (const card of mainCards) {
    const typeLine = card.card?.typeLine?.toLowerCase() || ''

    if (typeLine.includes('creature')) {
      groups.Creatures.push(card)
    } else if (typeLine.includes('planeswalker')) {
      groups.Planeswalkers.push(card)
    } else if (typeLine.includes('instant')) {
      groups.Instants.push(card)
    } else if (typeLine.includes('sorcery')) {
      groups.Sorceries.push(card)
    } else if (typeLine.includes('enchantment')) {
      groups.Enchantments.push(card)
    } else if (typeLine.includes('artifact')) {
      groups.Artifacts.push(card)
    } else if (typeLine.includes('land')) {
      groups.Lands.push(card)
    } else {
      groups.Other.push(card)
    }
  }

  // Sort each group by CMC then name
  for (const key of Object.keys(groups) as (keyof typeof groups)[]) {
    groups[key].sort((a, b) => {
      const cmcDiff = (a.card?.cmc || 0) - (b.card?.cmc || 0)
      if (cmcDiff !== 0) return cmcDiff
      return (a.card?.name || '').localeCompare(b.card?.name || '')
    })
  }

  return groups
})

// Calculate totals
const totalMainDeck = computed(() => {
  return props.cards
    .filter(c => c.board === 'main' || c.board === 'commander')
    .reduce((sum, c) => sum + c.quantity, 0)
})

const totalSideboard = computed(() => {
  return props.cards
    .filter(c => c.board === 'sideboard')
    .reduce((sum, c) => sum + c.quantity, 0)
})

function incrementQuantity(card: MtgDeckCard) {
  emit('update:quantity', card.scryfallId, card.board, card.quantity + 1)
}

function decrementQuantity(card: MtgDeckCard) {
  if (card.quantity > 1) {
    emit('update:quantity', card.scryfallId, card.board, card.quantity - 1)
  } else {
    emit('remove', card.scryfallId, card.board)
  }
}

function handleRemove(card: MtgDeckCard) {
  emit('remove', card.scryfallId, card.board)
}

function handleCardClick(card: MtgDeckCard) {
  if (card.card) {
    emit('card-click', card.card)
  }
}

function getRarityColor(rarity: string): string {
  switch (rarity) {
    case 'mythic': return 'text-orange-500'
    case 'rare': return 'text-yellow-500'
    case 'uncommon': return 'text-gray-400'
    default: return 'text-gray-600'
  }
}
</script>

<template>
  <div class="deck-list space-y-6">
    <!-- Commander Section -->
    <div v-if="cardsByBoard.commander.length > 0" class="space-y-2">
      <h3 class="font-semibold text-lg flex items-center gap-2">
        <span class="w-6 h-6 rounded bg-amber-100 text-amber-700 flex items-center justify-center text-sm">C</span>
        Commander
      </h3>
      <div class="space-y-1">
        <div
          v-for="card in cardsByBoard.commander"
          :key="card.id"
          class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer group"
          @click="handleCardClick(card)"
        >
          <div v-if="card.card" class="w-10 h-14 flex-shrink-0">
            <CardImage :card="card.card" size="small" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium truncate">{{ card.card?.name || 'Unknown Card' }}</span>
              <div v-if="card.card?.manaCost" class="flex items-center gap-0.5">
                <img
                  v-for="(symbol, idx) in parseManaCost(card.card.manaCost)"
                  :key="idx"
                  :src="getManaSymbolUrl(symbol)"
                  :alt="symbol"
                  class="w-4 h-4"
                />
              </div>
            </div>
            <div class="text-sm text-gray-500 truncate">{{ card.card?.typeLine }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Deck Section -->
    <div v-if="cardsByBoard.main.length > 0" class="space-y-2">
      <h3 class="font-semibold text-lg">
        Main Deck
        <span class="text-gray-400 font-normal">({{ totalMainDeck }} cards)</span>
      </h3>

      <!-- Grouped by type -->
      <template v-if="groupByType && cardsByType">
        <div v-for="(cards, typeName) in cardsByType" :key="typeName">
          <template v-if="cards.length > 0">
            <h4 class="text-sm font-medium text-gray-600 mt-4 mb-2">
              {{ typeName }} ({{ cards.reduce((sum, c) => sum + c.quantity, 0) }})
            </h4>
            <div class="space-y-1">
              <div
                v-for="card in cards"
                :key="card.id"
                class="flex items-center gap-2 py-1 px-2 rounded hover:bg-gray-50 group"
              >
                <span class="w-6 text-center text-gray-600">{{ card.quantity }}x</span>
                <button
                  class="flex-1 text-left truncate hover:text-primary-600"
                  @click="handleCardClick(card)"
                >
                  {{ card.card?.name || 'Unknown' }}
                </button>
                <div v-if="card.card?.manaCost" class="flex items-center gap-0.5">
                  <img
                    v-for="(symbol, idx) in parseManaCost(card.card.manaCost)"
                    :key="idx"
                    :src="getManaSymbolUrl(symbol)"
                    :alt="symbol"
                    class="w-3 h-3"
                  />
                </div>
                <template v-if="showQuantityControls">
                  <button
                    class="w-6 h-6 rounded bg-gray-100 hover:bg-gray-200 text-gray-600 opacity-0 group-hover:opacity-100 transition-opacity"
                    @click.stop="decrementQuantity(card)"
                  >
                    -
                  </button>
                  <button
                    class="w-6 h-6 rounded bg-gray-100 hover:bg-gray-200 text-gray-600 opacity-0 group-hover:opacity-100 transition-opacity"
                    @click.stop="incrementQuantity(card)"
                  >
                    +
                  </button>
                </template>
                <button
                  v-if="showRemove"
                  class="text-red-400 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-opacity"
                  @click.stop="handleRemove(card)"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- Flat list -->
      <template v-else>
        <div class="space-y-1">
          <div
            v-for="card in cardsByBoard.main"
            :key="card.id"
            class="flex items-center gap-2 py-1 px-2 rounded hover:bg-gray-50 group"
          >
            <span class="w-6 text-center text-gray-600">{{ card.quantity }}x</span>
            <button
              class="flex-1 text-left truncate hover:text-primary-600"
              @click="handleCardClick(card)"
            >
              {{ card.card?.name || 'Unknown' }}
            </button>
            <span v-if="card.card?.rarity" :class="getRarityColor(card.card.rarity)" class="text-xs capitalize">
              {{ card.card.rarity.charAt(0) }}
            </span>
            <div v-if="card.card?.manaCost" class="flex items-center gap-0.5">
              <img
                v-for="(symbol, idx) in parseManaCost(card.card.manaCost)"
                :key="idx"
                :src="getManaSymbolUrl(symbol)"
                :alt="symbol"
                class="w-3 h-3"
              />
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Sideboard Section -->
    <div v-if="cardsByBoard.sideboard.length > 0" class="space-y-2">
      <h3 class="font-semibold text-lg">
        Sideboard
        <span class="text-gray-400 font-normal">({{ totalSideboard }} cards)</span>
      </h3>
      <div class="space-y-1">
        <div
          v-for="card in cardsByBoard.sideboard"
          :key="card.id"
          class="flex items-center gap-2 py-1 px-2 rounded hover:bg-gray-50 group"
        >
          <span class="w-6 text-center text-gray-600">{{ card.quantity }}x</span>
          <button
            class="flex-1 text-left truncate hover:text-primary-600"
            @click="handleCardClick(card)"
          >
            {{ card.card?.name || 'Unknown' }}
          </button>
          <div v-if="card.card?.manaCost" class="flex items-center gap-0.5">
            <img
              v-for="(symbol, idx) in parseManaCost(card.card.manaCost)"
              :key="idx"
              :src="getManaSymbolUrl(symbol)"
              :alt="symbol"
              class="w-3 h-3"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Maybeboard Section -->
    <div v-if="cardsByBoard.maybeboard.length > 0" class="space-y-2">
      <h3 class="font-semibold text-lg text-gray-500">
        Maybeboard
        <span class="text-gray-400 font-normal">({{ cardsByBoard.maybeboard.reduce((s, c) => s + c.quantity, 0) }})</span>
      </h3>
      <div class="space-y-1 opacity-75">
        <div
          v-for="card in cardsByBoard.maybeboard"
          :key="card.id"
          class="flex items-center gap-2 py-1 px-2 rounded hover:bg-gray-50"
        >
          <span class="w-6 text-center text-gray-400">{{ card.quantity }}x</span>
          <button
            class="flex-1 text-left truncate text-gray-600 hover:text-primary-600"
            @click="handleCardClick(card)"
          >
            {{ card.card?.name || 'Unknown' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="cards.length === 0" class="text-center py-8 text-gray-500">
      <svg class="w-12 h-12 mx-auto mb-3 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
        <path d="M21,16.5C21,16.88 20.79,17.21 20.47,17.38L12.57,21.82C12.41,21.94 12.21,22 12,22C11.79,22 11.59,21.94 11.43,21.82L3.53,17.38C3.21,17.21 3,16.88 3,16.5V7.5C3,7.12 3.21,6.79 3.53,6.62L11.43,2.18C11.59,2.06 11.79,2 12,2C12.21,2 12.41,2.06 12.57,2.18L20.47,6.62C20.79,6.79 21,7.12 21,7.5V16.5M12,4.15L5,8.09V15.91L12,19.85L19,15.91V8.09L12,4.15Z"/>
      </svg>
      <p>No cards in this deck yet</p>
      <p class="text-sm">Search for cards to add them</p>
    </div>
  </div>
</template>
