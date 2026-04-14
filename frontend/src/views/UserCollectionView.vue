<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicProfile } from '@/services/profileApi'
import { getUserCollection, type CollectionGame } from '@/services/collectionsApi'
import type { PublicProfile } from '@/types/profile'

const route = useRoute()
const router = useRouter()

const profile = ref<PublicProfile | null>(null)
const games = ref<CollectionGame[]>([])
const loading = ref(true)
const error = ref('')
const filter = ref('')

// Lightbox
const lightboxImage = ref<string | null>(null)
const lightboxName = ref('')

function openLightbox(imageUrl: string | null | undefined, thumbnailUrl: string | null | undefined, name: string) {
  const url = imageUrl || thumbnailUrl
  if (!url) return
  lightboxImage.value = url.replace('__small/', '__original/').replace('__medium/', '__original/')
  lightboxName.value = name
}

const filteredGames = ref<CollectionGame[]>([])

function updateFilter() {
  const q = filter.value.toLowerCase()
  filteredGames.value = q ? games.value.filter(g => g.game_name.toLowerCase().includes(q)) : games.value
}

onMounted(async () => {
  const userId = route.params.userId as string
  if (!userId) {
    error.value = 'Invalid user'
    loading.value = false
    return
  }

  try {
    const [p, g] = await Promise.all([
      getPublicProfile(userId),
      getUserCollection(userId),
    ])
    profile.value = p
    games.value = g
    filteredGames.value = g
  } catch (err: any) {
    if (err.message?.includes('private') || err.message?.includes('403')) {
      error.value = 'This collection is private'
    } else {
      error.value = 'Could not load this collection'
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-primary-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-center py-12">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M11.83,9L15,12.16C15,12.11 15,12.05 15,12A3,3 0 0,0 12,9C11.94,9 11.89,9 11.83,9M7.53,9.8L9.08,11.35C9.03,11.56 9,11.77 9,12A3,3 0 0,0 12,15C12.22,15 12.44,14.97 12.65,14.92L14.2,16.47C13.53,16.8 12.79,17 12,17A5,5 0 0,1 7,12C7,11.21 7.2,10.47 7.53,9.8M2,4.27L4.28,6.55L4.73,7C3.08,8.3 1.78,10 1,12C2.73,16.39 7,19.5 12,19.5C13.55,19.5 15.03,19.2 16.38,18.66L16.81,19.08L19.73,22L21,20.73L3.27,3M12,7A5,5 0 0,1 17,12C17,12.64 16.87,13.26 16.64,13.82L19.57,16.75C21.07,15.5 22.27,13.86 23,12C21.27,7.61 17,4.5 12,4.5C10.6,4.5 9.26,4.75 8,5.2L10.17,7.35C10.74,7.13 11.35,7 12,7Z"/>
      </svg>
      <p class="text-gray-500 text-lg">{{ error }}</p>
      <button @click="router.back()" class="mt-4 text-primary-600 hover:underline">Go back</button>
    </div>

    <!-- Collection -->
    <template v-else>
      <div class="flex items-center gap-3 mb-6">
        <button @click="router.back()" class="p-1 text-gray-400 hover:text-gray-600">
          <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
        </button>
        <div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ profile?.displayName || profile?.username }}'s Collection
          </h1>
          <p class="text-sm text-gray-500">{{ games.length }} game{{ games.length === 1 ? '' : 's' }}</p>
        </div>
      </div>

      <!-- Filter -->
      <div v-if="games.length > 5" class="relative mb-4">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
          <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
        </svg>
        <input
          v-model="filter"
          type="text"
          placeholder="Filter games..."
          class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
          @input="updateFilter"
        />
      </div>

      <div v-if="games.length === 0" class="text-center py-12 text-gray-500">
        This collection is empty
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
              @click="openLightbox(game.image_url, game.thumbnail_url, game.game_name)"
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
          </div>
        </div>
      </div>
    </template>

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
          <img :src="lightboxImage" :alt="lightboxName" class="w-full rounded-lg shadow-2xl" />
          <p class="text-white text-center mt-3 font-medium">{{ lightboxName }}</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>
