<script setup lang="ts">
import { ref, watch } from 'vue'
import { searchCards, searchBannedCards, getCardById } from '@/services/scryfallApi'
import { parseManaCost, getManaSymbolUrl } from '@/types/mtg'
import type { ScryfallCard, BannedCard } from '@/types/mtg'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  disabled?: boolean
  filterCommander?: boolean  // Only show valid commanders
  forBannedCards?: boolean   // Use banned card search mode
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'select', card: ScryfallCard): void
  (e: 'selectBanned', card: BannedCard): void
}>()

const inputValue = ref(props.modelValue)
const showDropdown = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const results = ref<ScryfallCard[]>([])
const bannedResults = ref<BannedCard[]>([])
const loadingCard = ref<string | null>(null)

let searchTimeout: ReturnType<typeof setTimeout> | null = null

watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

function handleInput(event: Event) {
  const value = (event.target as HTMLInputElement).value
  inputValue.value = value
  emit('update:modelValue', value)
  error.value = null

  // Debounce search
  if (searchTimeout) clearTimeout(searchTimeout)

  if (value.trim().length < 2) {
    results.value = []
    bannedResults.value = []
    showDropdown.value = false
    return
  }

  searchTimeout = setTimeout(async () => {
    loading.value = true
    error.value = null
    try {
      if (props.forBannedCards) {
        // Use banned card search with ranking and filtering
        bannedResults.value = await searchBannedCards(value)
        results.value = []
        showDropdown.value = bannedResults.value.length > 0
      } else {
        // Standard card search
        let query = value
        if (props.filterCommander) {
          // Filter for legendary creatures or cards that can be commander
          query = `${value} (type:legendary type:creature OR oracle:"can be your commander")`
        }

        const response = await searchCards(query)
        results.value = response.cards.slice(0, 15) // Limit to 15 results
        bannedResults.value = []
        showDropdown.value = results.value.length > 0
      }
    } catch (err) {
      console.error('Search error:', err)
      error.value = 'Search failed. Please try again.'
      results.value = []
      bannedResults.value = []
    } finally {
      loading.value = false
    }
  }, 300)
}

async function selectCard(card: ScryfallCard) {
  loadingCard.value = card.scryfallId
  try {
    // Get full card details if needed
    const fullCard = await getCardById(card.scryfallId)
    inputValue.value = fullCard.name
    emit('update:modelValue', fullCard.name)
    emit('select', fullCard)
    showDropdown.value = false
  } catch (err) {
    console.error('Failed to get card details:', err)
    // Still emit with the card we have
    inputValue.value = card.name
    emit('update:modelValue', card.name)
    emit('select', card)
    showDropdown.value = false
  } finally {
    loadingCard.value = null
  }
}

function selectBannedCard(card: BannedCard) {
  inputValue.value = card.name
  emit('update:modelValue', card.name)
  emit('selectBanned', card)
  showDropdown.value = false
}

function handleBlur() {
  // Delay to allow click on dropdown item
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

function handleFocus() {
  if (results.value.length > 0 || bannedResults.value.length > 0) {
    showDropdown.value = true
  }
}

function getCardImage(card: ScryfallCard): string | null {
  if (card.isDoubleFaced && card.cardFaces?.[0]?.imageUris) {
    return card.cardFaces[0].imageUris.small
  }
  return card.imageUris.small
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
  <div class="relative">
    <div class="relative">
      <input
        :value="inputValue"
        type="text"
        class="input pr-10"
        :placeholder="placeholder || 'Search for a card...'"
        :disabled="disabled"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      <div class="absolute right-3 top-1/2 -translate-y-1/2">
        <svg v-if="loading" class="w-5 h-5 text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
          <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
        </svg>
      </div>
    </div>

    <!-- Powered by Scryfall attribution -->
    <div class="flex items-center justify-end mt-1">
      <a
        href="https://scryfall.com"
        target="_blank"
        rel="noopener noreferrer"
        class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors"
      >
        <span>Powered by</span>
        <span class="font-medium">Scryfall</span>
      </a>
    </div>

    <!-- Dropdown -->
    <div
      v-if="showDropdown"
      class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-80 overflow-y-auto"
    >
      <!-- Banned card mode results -->
      <template v-if="forBannedCards">
        <button
          v-for="card in bannedResults"
          :key="card.oracleId"
          type="button"
          class="w-full flex items-center gap-3 px-4 py-2 text-left hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
          @click="selectBannedCard(card)"
        >
          <!-- Card thumbnail (smaller for banned card mode) -->
          <img
            v-if="card.imageUrl"
            :src="card.imageUrl"
            :alt="card.name"
            class="w-10 h-14 object-cover rounded flex-shrink-0 bg-gray-100"
          />
          <div v-else class="w-10 h-14 flex items-center justify-center bg-gradient-to-br from-purple-800 to-blue-900 rounded flex-shrink-0">
            <span class="text-white text-sm font-bold">M</span>
          </div>

          <div class="flex-1 min-w-0">
            <span class="font-medium text-gray-900 block truncate">{{ card.name }}</span>
            <span v-if="card.typeLine" class="text-sm text-gray-500 block truncate">{{ card.typeLine }}</span>
          </div>
        </button>

        <div v-if="bannedResults.length === 0 && !loading && !error" class="px-4 py-3 text-gray-500 text-sm">
          No cards found
        </div>
      </template>

      <!-- Standard card search results -->
      <template v-else>
        <button
          v-for="card in results"
          :key="card.scryfallId"
          type="button"
          class="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
          @click="selectCard(card)"
        >
          <!-- Loading spinner -->
          <div v-if="loadingCard === card.scryfallId" class="w-14 h-20 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
          </div>
          <!-- Card image -->
          <img
            v-else-if="getCardImage(card)"
            :src="getCardImage(card)!"
            :alt="card.name"
            class="w-14 h-20 object-cover rounded flex-shrink-0 bg-gray-100"
          />
          <!-- Fallback card back -->
          <div v-else class="w-14 h-20 flex items-center justify-center bg-gradient-to-br from-purple-800 to-blue-900 rounded flex-shrink-0">
            <span class="text-white text-2xl font-bold">M</span>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-900 truncate">{{ card.name }}</span>
              <!-- Mana cost -->
              <div v-if="card.manaCost" class="flex items-center gap-0.5 flex-shrink-0">
                <img
                  v-for="(symbol, idx) in parseManaCost(card.manaCost)"
                  :key="idx"
                  :src="getManaSymbolUrl(symbol)"
                  :alt="symbol"
                  class="w-4 h-4"
                />
              </div>
            </div>
            <div class="text-sm text-gray-600 truncate">{{ card.typeLine }}</div>
            <div class="flex items-center gap-2 text-xs mt-0.5">
              <span :class="getRarityColor(card.rarity)" class="capitalize">{{ card.rarity }}</span>
              <span class="text-gray-400">{{ card.setName }}</span>
            </div>
          </div>
        </button>

        <div v-if="results.length === 0 && !loading && !error" class="px-4 py-3 text-gray-500 text-sm">
          No cards found
        </div>
      </template>

      <!-- Error state -->
      <div v-if="error" class="px-4 py-3 text-red-500 text-sm">
        {{ error }}
      </div>
    </div>
  </div>
</template>
