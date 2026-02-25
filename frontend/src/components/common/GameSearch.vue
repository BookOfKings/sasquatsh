<script setup lang="ts">
import { ref, watch } from 'vue'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import type { BggSearchResult, BggGame } from '@/types/bgg'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'select', game: BggGame): void
}>()

const inputValue = ref(props.modelValue)
const showDropdown = ref(false)
const loading = ref(false)
const results = ref<BggSearchResult[]>([])
const loadingGame = ref<number | null>(null)

let searchTimeout: ReturnType<typeof setTimeout> | null = null

watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal
})

function handleInput(event: Event) {
  const value = (event.target as HTMLInputElement).value
  inputValue.value = value
  emit('update:modelValue', value)

  // Debounce search
  if (searchTimeout) clearTimeout(searchTimeout)

  if (value.trim().length < 2) {
    results.value = []
    showDropdown.value = false
    return
  }

  searchTimeout = setTimeout(async () => {
    loading.value = true
    try {
      results.value = await searchBggGames(value)
      showDropdown.value = results.value.length > 0
    } catch (err) {
      console.error('Search error:', err)
      results.value = []
    } finally {
      loading.value = false
    }
  }, 300)
}

async function selectGame(result: BggSearchResult) {
  loadingGame.value = result.bggId
  try {
    const game = await getBggGame(result.bggId)
    inputValue.value = game.name
    emit('update:modelValue', game.name)
    emit('select', game)
    showDropdown.value = false
  } catch (err) {
    console.error('Failed to get game details:', err)
  } finally {
    loadingGame.value = null
  }
}

function handleBlur() {
  // Delay to allow click on dropdown item
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}

function handleFocus() {
  if (results.value.length > 0) {
    showDropdown.value = true
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
        :placeholder="placeholder || 'Search for a game...'"
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

    <!-- Dropdown -->
    <div
      v-if="showDropdown"
      class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto"
    >
      <button
        v-for="result in results"
        :key="result.bggId"
        type="button"
        class="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
        @click="selectGame(result)"
      >
        <svg v-if="loadingGame === result.bggId" class="w-5 h-5 text-primary-500 animate-spin flex-shrink-0" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else class="w-5 h-5 text-gray-400 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
        </svg>
        <div class="flex-1 min-w-0">
          <div class="font-medium text-gray-900 truncate">{{ result.name }}</div>
          <div v-if="result.yearPublished" class="text-sm text-gray-500">{{ result.yearPublished }}</div>
        </div>
      </button>

      <div v-if="results.length === 0 && !loading" class="px-4 py-3 text-gray-500 text-sm">
        No games found
      </div>
    </div>
  </div>
</template>
