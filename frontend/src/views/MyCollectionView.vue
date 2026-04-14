<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { getMyProfile, updateProfile } from '@/services/profileApi'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import {
  getMyCollection,
  getTopGames,
  addGamesToCollection,
  removeGameFromCollection,
  type CollectionGame,
  type TopGame,
} from '@/services/collectionsApi'

const auth = useAuthStore()

// State
const myGames = ref<CollectionGame[]>([])
const topGames = ref<TopGame[]>([])
const searchResults = ref<any[]>([])
const searchQuery = ref('')
const loading = ref(true)
const collectionVisibility = ref<'public' | 'private'>('private')
const savingVisibility = ref(false)
const searching = ref(false)
const activeTab = ref<'collection' | 'browse' | 'search'>('collection')
const pendingAdds = ref<Set<number>>(new Set())
const pendingRemoves = ref<Set<number>>(new Set())

// Which BGG IDs the user owns
const ownedBggIds = computed(() => new Set(myGames.value.map(g => g.bgg_id)))

// Load data
onMounted(async () => {
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const [collection, top, profile] = await Promise.all([
      getMyCollection(token),
      getTopGames(),
      getMyProfile(token),
    ])
    myGames.value = collection
    topGames.value = top
    collectionVisibility.value = profile.collectionVisibility ?? 'private'
  } catch (err) {
    console.error('Failed to load collection:', err)
  } finally {
    loading.value = false
  }
})

// Debounced search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, (query) => {
  if (searchTimeout) clearTimeout(searchTimeout)
  if (!query.trim()) {
    searchResults.value = []
    searching.value = false
    return
  }
  searching.value = true
  searchTimeout = setTimeout(async () => {
    try {
      const results = await searchBggGames(query)
      searchResults.value = results
    } catch (err) {
      console.error('Search failed:', err)
    } finally {
      searching.value = false
    }
  }, 400)
})

async function toggleGame(game: { bgg_id?: number; bggId?: number; name?: string; game_name?: string; thumbnail_url?: string | null; thumbnailUrl?: string | null; image_url?: string | null; imageUrl?: string | null; min_players?: number | null; minPlayers?: number | null; max_players?: number | null; maxPlayers?: number | null; playing_time?: number | null; playingTime?: number | null; year_published?: number | null; yearPublished?: number | null; bgg_rank?: number | null; bggRank?: number | null; average_rating?: number | null; averageRating?: number | null }) {
  const bggId = game.bgg_id ?? game.bggId
  if (!bggId) return

  const token = await auth.getIdToken()
  if (!token) return

  const isOwned = ownedBggIds.value.has(bggId)

  try {
    if (isOwned) {
      pendingRemoves.value.add(bggId)
      await removeGameFromCollection(token, bggId)
      myGames.value = myGames.value.filter(g => g.bgg_id !== bggId)
    } else {
      pendingAdds.value.add(bggId)
      const gameName = game.name ?? game.game_name ?? 'Unknown'
      const added = await addGamesToCollection(token, [{
        bgg_id: bggId,
        name: gameName,
        thumbnail_url: game.thumbnail_url ?? game.thumbnailUrl,
        image_url: game.image_url ?? game.imageUrl,
        min_players: game.min_players ?? game.minPlayers,
        max_players: game.max_players ?? game.maxPlayers,
        playing_time: game.playing_time ?? game.playingTime,
        year_published: game.year_published ?? game.yearPublished,
        bgg_rank: game.bgg_rank ?? game.bggRank,
        average_rating: game.average_rating ?? game.averageRating,
      }])
      myGames.value.push(...added)
      myGames.value.sort((a, b) => a.game_name.localeCompare(b.game_name))
    }
  } catch (err) {
    console.error('Failed to update collection:', err)
  } finally {
    pendingAdds.value.delete(bggId)
    pendingRemoves.value.delete(bggId)
  }
}

function isPending(bggId: number) {
  return pendingAdds.value.has(bggId) || pendingRemoves.value.has(bggId)
}

// BGG ID lookup
const bggIdInput = ref('')
const bggIdLoading = ref(false)
const bggIdError = ref('')
const bggIdResult = ref<any>(null)

async function lookupBggId() {
  const id = Number(bggIdInput.value.trim())
  if (!id || isNaN(id)) {
    bggIdError.value = 'Enter a valid BGG ID number'
    return
  }
  bggIdError.value = ''
  bggIdResult.value = null
  bggIdLoading.value = true
  try {
    const game = await getBggGame(id)
    bggIdResult.value = game
  } catch {
    bggIdError.value = `Could not find game with BGG ID ${id}`
  } finally {
    bggIdLoading.value = false
  }
}

// Visibility toggle
async function toggleVisibility() {
  const token = await auth.getIdToken()
  if (!token) return
  const newValue = collectionVisibility.value === 'public' ? 'private' : 'public'
  savingVisibility.value = true
  try {
    await updateProfile(token, { collectionVisibility: newValue })
    collectionVisibility.value = newValue
  } catch (err) {
    console.error('Failed to update visibility:', err)
  } finally {
    savingVisibility.value = false
  }
}

// Image lightbox
const lightboxImage = ref<string | null>(null)
const lightboxName = ref('')

function openLightbox(imageUrl: string | null | undefined, thumbnailUrl: string | null | undefined, name: string) {
  const url = imageUrl || thumbnailUrl
  if (!url) return
  // BGG thumbnail URLs use __small, swap to __original for full size
  lightboxImage.value = url.replace('__small/', '__original/').replace('__medium/', '__original/')
  lightboxName.value = name
}

// Collection filter
const collectionFilter = ref('')
const filteredGames = computed(() => {
  if (!collectionFilter.value.trim()) return myGames.value
  const q = collectionFilter.value.toLowerCase()
  return myGames.value.filter(g => g.game_name.toLowerCase().includes(q))
})
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-2">
      <h1 class="text-2xl font-bold text-gray-900">My Game Collection</h1>
      <button
        @click="toggleVisibility"
        :disabled="savingVisibility"
        class="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-full border transition-colors"
        :class="collectionVisibility === 'public'
          ? 'bg-primary-50 border-primary-300 text-primary-700'
          : 'bg-gray-50 border-gray-300 text-gray-600'"
      >
        <svg v-if="savingVisibility" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <svg v-else-if="collectionVisibility === 'public'" class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4.5C7,4.5 2.73,7.61 1,12C2.73,16.39 7,19.5 12,19.5C17,19.5 21.27,16.39 23,12C21.27,7.61 17,4.5 12,4.5Z"/>
        </svg>
        <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M11.83,9L15,12.16C15,12.11 15,12.05 15,12A3,3 0 0,0 12,9C11.94,9 11.89,9 11.83,9M7.53,9.8L9.08,11.35C9.03,11.56 9,11.77 9,12A3,3 0 0,0 12,15C12.22,15 12.44,14.97 12.65,14.92L14.2,16.47C13.53,16.8 12.79,17 12,17A5,5 0 0,1 7,12C7,11.21 7.2,10.47 7.53,9.8M2,4.27L4.28,6.55L4.73,7C3.08,8.3 1.78,10 1,12C2.73,16.39 7,19.5 12,19.5C13.55,19.5 15.03,19.2 16.38,18.66L16.81,19.08L19.73,22L21,20.73L3.27,3M12,7A5,5 0 0,1 17,12C17,12.64 16.87,13.26 16.64,13.82L19.57,16.75C21.07,15.5 22.27,13.86 23,12C21.27,7.61 17,4.5 12,4.5C10.6,4.5 9.26,4.75 8,5.2L10.17,7.35C10.74,7.13 11.35,7 12,7Z"/>
        </svg>
        {{ collectionVisibility === 'public' ? 'Public' : 'Private' }}
      </button>
    </div>
    <p class="text-gray-600 mb-6">Track the board games you own. Browse top games or search to add to your collection.</p>

    <!-- Tabs -->
    <div class="flex border-b border-gray-200 mb-6">
      <button
        @click="activeTab = 'collection'"
        class="px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px"
        :class="activeTab === 'collection' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'"
      >
        My Games ({{ myGames.length }})
      </button>
      <button
        @click="activeTab = 'browse'"
        class="px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px"
        :class="activeTab === 'browse' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'"
      >
        Top 50
      </button>
      <button
        @click="activeTab = 'search'"
        class="px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px"
        :class="activeTab === 'search' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'"
      >
        Search BGG
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-primary-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- My Collection Tab -->
    <div v-else-if="activeTab === 'collection'">
      <div v-if="myGames.length === 0" class="text-center py-12">
        <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
        </svg>
        <p class="text-gray-500 mb-4">Your collection is empty</p>
        <button @click="activeTab = 'browse'" class="btn-primary">Browse Top 50 Games</button>
      </div>

      <template v-else>
        <!-- Collection search -->
        <div class="relative mb-4">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
          </svg>
          <input
            v-model="collectionFilter"
            type="text"
            placeholder="Filter your games..."
            class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
          />
        </div>

        <div v-if="filteredGames.length === 0" class="text-center py-8 text-gray-500">
          No games match "{{ collectionFilter }}"
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="game in filteredGames"
          :key="game.bgg_id"
          class="bg-white border border-gray-200 rounded-lg overflow-hidden hover:shadow-md transition-shadow"
        >
          <div class="flex items-start gap-3 p-3">
            <img
              v-if="game.thumbnail_url"
              :src="game.thumbnail_url"
              :alt="game.game_name"
              class="w-16 h-16 object-cover rounded flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
              @click.stop="openLightbox(game.image_url, game.thumbnail_url, game.game_name)"
            />
            <div v-else class="w-16 h-16 bg-gray-100 rounded flex items-center justify-center flex-shrink-0">
              <svg class="w-8 h-8 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-medium text-sm text-gray-900 truncate">{{ game.game_name }}</h3>
              <div class="text-xs text-gray-500 mt-1 space-y-0.5">
                <div v-if="game.year_published">({{ game.year_published }})</div>
                <div v-if="game.min_players && game.max_players">
                  {{ game.min_players }}-{{ game.max_players }} players
                </div>
                <div v-if="game.playing_time">{{ game.playing_time }} min</div>
              </div>
            </div>
            <button
              @click="toggleGame(game)"
              :disabled="isPending(game.bgg_id)"
              class="p-1.5 text-red-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors flex-shrink-0"
              title="Remove from collection"
            >
              <svg v-if="isPending(game.bgg_id)" class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
      </template>
    </div>

    <!-- Browse Top 50 Tab -->
    <div v-else-if="activeTab === 'browse'">
      <p class="text-sm text-gray-500 mb-4">Top ranked games on BoardGameGeek. Check the ones you own.</p>
      <div class="space-y-2">
        <div
          v-for="game in topGames"
          :key="game.bgg_id"
          class="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-lg hover:border-primary-200 transition-colors cursor-pointer"
          @click="toggleGame(game)"
        >
          <div class="flex-shrink-0 w-6 flex justify-center">
            <svg v-if="isPending(game.bgg_id)" class="w-5 h-5 animate-spin text-primary-500" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            <div
              v-else
              class="w-5 h-5 border-2 rounded flex items-center justify-center transition-colors"
              :class="ownedBggIds.has(game.bgg_id) ? 'bg-primary-500 border-primary-500' : 'border-gray-300'"
            >
              <svg v-if="ownedBggIds.has(game.bgg_id)" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
            </div>
          </div>
          <span class="text-xs text-gray-400 w-8 text-right flex-shrink-0">#{{ game.bgg_rank }}</span>
          <img
            v-if="game.thumbnail_url"
            :src="game.thumbnail_url"
            :alt="game.name"
            class="w-10 h-10 object-cover rounded flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
            @click.stop="openLightbox(game.image_url, game.thumbnail_url, game.name)"
          />
          <div v-else class="w-10 h-10 bg-gray-100 rounded flex-shrink-0"></div>
          <div class="flex-1 min-w-0">
            <div class="font-medium text-sm truncate">{{ game.name }}</div>
            <div class="text-xs text-gray-500">
              <span v-if="game.year_published">({{ game.year_published }}) </span>
              <span v-if="game.min_players && game.max_players">{{ game.min_players }}-{{ game.max_players }}p</span>
              <span v-if="game.playing_time"> · {{ game.playing_time }}min</span>
              <span v-if="game.average_rating"> · {{ Number(game.average_rating).toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Search Tab -->
    <div v-else-if="activeTab === 'search'">
      <div class="relative mb-4">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
          <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search BoardGameGeek..."
          class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        />
      </div>

      <!-- BGG ID Lookup -->
      <div class="mb-4 p-3 bg-gray-50 rounded-lg">
        <label class="text-xs font-medium text-gray-500 mb-1 block">Add by BGG ID</label>
        <div class="flex gap-2">
          <input
            v-model="bggIdInput"
            type="text"
            inputmode="numeric"
            placeholder="e.g. 174430"
            class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
            @keydown.enter="lookupBggId"
          />
          <button
            @click="lookupBggId"
            :disabled="bggIdLoading"
            class="btn-primary px-4 py-2 text-sm"
          >
            <svg v-if="bggIdLoading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            <span v-else>Lookup</span>
          </button>
        </div>
        <p v-if="bggIdError" class="text-xs text-red-500 mt-1">{{ bggIdError }}</p>
        <div
          v-if="bggIdResult"
          class="flex items-center gap-3 mt-2 p-3 bg-white border border-gray-200 rounded-lg hover:border-primary-200 transition-colors cursor-pointer"
          @click="toggleGame(bggIdResult); bggIdResult = null; bggIdInput = ''"
        >
          <div class="flex-shrink-0 w-6 flex justify-center">
            <div
              class="w-5 h-5 border-2 rounded flex items-center justify-center transition-colors"
              :class="ownedBggIds.has(bggIdResult.bggId) ? 'bg-primary-500 border-primary-500' : 'border-gray-300'"
            >
              <svg v-if="ownedBggIds.has(bggIdResult.bggId)" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
            </div>
          </div>
          <img
            v-if="bggIdResult.thumbnailUrl"
            :src="bggIdResult.thumbnailUrl"
            :alt="bggIdResult.name"
            class="w-10 h-10 object-cover rounded flex-shrink-0"
          />
          <div v-else class="w-10 h-10 bg-gray-100 rounded flex-shrink-0"></div>
          <div class="flex-1 min-w-0">
            <div class="font-medium text-sm truncate">{{ bggIdResult.name }}</div>
            <div class="text-xs text-gray-500">
              <span v-if="bggIdResult.yearPublished">({{ bggIdResult.yearPublished }}) </span>
              <span v-if="bggIdResult.minPlayers && bggIdResult.maxPlayers">{{ bggIdResult.minPlayers }}-{{ bggIdResult.maxPlayers }}p</span>
              <span v-if="bggIdResult.playingTime"> · {{ bggIdResult.playingTime }}min</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="searching" class="flex justify-center py-8">
        <svg class="animate-spin h-6 w-6 text-primary-500" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <div v-else-if="searchQuery && searchResults.length === 0" class="text-center py-8 text-gray-500">
        No games found for "{{ searchQuery }}"
      </div>

      <div v-else class="space-y-2">
        <div
          v-for="game in searchResults"
          :key="game.bggId"
          class="flex items-center gap-3 p-3 bg-white border border-gray-200 rounded-lg hover:border-primary-200 transition-colors cursor-pointer"
          @click="toggleGame(game)"
        >
          <div class="flex-shrink-0 w-6 flex justify-center">
            <svg v-if="isPending(game.bggId)" class="w-5 h-5 animate-spin text-primary-500" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            <div
              v-else
              class="w-5 h-5 border-2 rounded flex items-center justify-center transition-colors"
              :class="ownedBggIds.has(game.bggId) ? 'bg-primary-500 border-primary-500' : 'border-gray-300'"
            >
              <svg v-if="ownedBggIds.has(game.bggId)" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
            </div>
          </div>
          <img
            v-if="game.thumbnailUrl"
            :src="game.thumbnailUrl"
            :alt="game.name"
            class="w-10 h-10 object-cover rounded flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
            @click.stop="openLightbox(game.imageUrl, game.thumbnailUrl, game.name)"
          />
          <div v-else class="w-10 h-10 bg-gray-100 rounded flex-shrink-0"></div>
          <div class="flex-1 min-w-0">
            <div class="font-medium text-sm truncate">{{ game.name }}</div>
            <div class="text-xs text-gray-500">
              <span v-if="game.yearPublished">({{ game.yearPublished }}) </span>
              <span v-if="game.minPlayers && game.maxPlayers">{{ game.minPlayers }}-{{ game.maxPlayers }}p</span>
              <span v-if="game.playingTime"> · {{ game.playingTime }}min</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!searchQuery && !searching" class="text-center py-8 text-gray-400">
        Type a game name to search BoardGameGeek
      </div>
    </div>

    <!-- Image Lightbox -->
    <Teleport to="body">
      <div
        v-if="lightboxImage"
        class="fixed inset-0 z-[10000] flex items-center justify-center bg-black/70 p-4"
        @click="lightboxImage = null"
      >
        <div class="relative max-w-lg w-full" @click.stop>
          <button
            @click="lightboxImage = null"
            class="absolute -top-10 right-0 text-white hover:text-gray-300 transition-colors"
          >
            <svg class="w-8 h-8" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
          <img
            :src="lightboxImage"
            :alt="lightboxName"
            class="w-full rounded-lg shadow-2xl"
          />
          <p class="text-white text-center mt-3 font-medium">{{ lightboxName }}</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>
