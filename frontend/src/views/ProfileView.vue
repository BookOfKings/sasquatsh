<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getMyProfile, updateProfile, getBlockedUsers, unblockUser } from '@/services/profileApi'
import type { UserProfile, UpdateProfileInput, BlockedUser } from '@/types/profile'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const profile = ref<UserProfile | null>(null)
const blockedUsers = ref<BlockedUser[]>([])
const loadingBlocked = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const isEditing = ref(false)

const form = reactive<UpdateProfileInput>({
  displayName: '',
  avatarUrl: '',
  birthYear: undefined,
  maxTravelMiles: 25,
  homeCity: '',
  homeState: '',
  homePostalCode: '',
  bio: '',
  favoriteGames: [],
  preferredGameTypes: [],
})

const newFavoriteGame = ref('')

const gameTypeOptions = [
  { value: 'strategy', label: 'Strategy' },
  { value: 'party', label: 'Party' },
  { value: 'cooperative', label: 'Cooperative' },
  { value: 'deckbuilding', label: 'Deck Building' },
  { value: 'workerplacement', label: 'Worker Placement' },
  { value: 'areacontrol', label: 'Area Control' },
  { value: 'dice', label: 'Dice' },
  { value: 'trivia', label: 'Trivia' },
  { value: 'roleplaying', label: 'Role Playing' },
  { value: 'miniatures', label: 'Miniatures' },
  { value: 'card', label: 'Card' },
  { value: 'family', label: 'Family' },
  { value: 'abstract', label: 'Abstract' },
]

const travelOptions = [
  { value: 5, label: '5 miles' },
  { value: 10, label: '10 miles' },
  { value: 15, label: '15 miles' },
  { value: 25, label: '25 miles' },
  { value: 50, label: '50 miles' },
  { value: 100, label: '100 miles' },
  { value: 0, label: 'Any distance' },
]

const memberSince = computed(() => {
  if (!profile.value) return ''
  const date = new Date(profile.value.createdAt)
  return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
})

onMounted(async () => {
  await loadProfile()
  await loadBlockedUsers()
})

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    profile.value = await getMyProfile(token)
    populateForm()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load profile'
  } finally {
    loading.value = false
  }
}

async function loadBlockedUsers() {
  loadingBlocked.value = true
  try {
    const token = await auth.getIdToken()
    if (token) {
      blockedUsers.value = await getBlockedUsers(token)
    }
  } catch (err) {
    console.error('Failed to load blocked users:', err)
  } finally {
    loadingBlocked.value = false
  }
}

async function handleUnblock(userId: string) {
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await unblockUser(token, userId)
    blockedUsers.value = blockedUsers.value.filter(u => u.id !== userId)
    successMessage.value = 'User unblocked successfully'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to unblock user'
  }
}

function populateForm() {
  if (!profile.value) return
  form.displayName = profile.value.displayName ?? ''
  form.avatarUrl = profile.value.avatarUrl ?? ''
  form.birthYear = profile.value.birthYear ?? undefined
  form.maxTravelMiles = profile.value.maxTravelMiles ?? 25
  form.homeCity = profile.value.homeCity ?? ''
  form.homeState = profile.value.homeState ?? ''
  form.homePostalCode = profile.value.homePostalCode ?? ''
  form.bio = profile.value.bio ?? ''
  form.favoriteGames = profile.value.favoriteGames ?? []
  form.preferredGameTypes = profile.value.preferredGameTypes ?? []
}

function startEditing() {
  populateForm()
  isEditing.value = true
}

function cancelEditing() {
  populateForm()
  isEditing.value = false
  errorMessage.value = ''
}

async function saveProfile() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    profile.value = await updateProfile(token, form)
    isEditing.value = false
    successMessage.value = 'Profile updated successfully!'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update profile'
  } finally {
    saving.value = false
  }
}

function addFavoriteGame() {
  const game = newFavoriteGame.value.trim()
  if (game && !form.favoriteGames?.includes(game)) {
    form.favoriteGames = [...(form.favoriteGames ?? []), game]
    newFavoriteGame.value = ''
  }
}

function removeFavoriteGame(index: number) {
  form.favoriteGames = form.favoriteGames?.filter((_, i) => i !== index) ?? []
}

function toggleGameType(type: string) {
  const current = form.preferredGameTypes ?? []
  if (current.includes(type)) {
    form.preferredGameTypes = current.filter(t => t !== type)
  } else {
    form.preferredGameTypes = [...current, type]
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}

function goToEvent(eventId: string) {
  router.push(`/games/${eventId}`)
}

function goToGroup(slug: string) {
  router.push(`/groups/${slug}`)
}
</script>

<template>
  <div class="container-narrow py-8">
    <h1 class="text-2xl font-bold mb-6">My Profile</h1>

    <!-- Loading -->
    <div v-if="loading" class="card p-8 text-center">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading profile...</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage && !profile" class="alert-error">
      {{ errorMessage }}
    </div>

    <!-- Profile Content -->
    <template v-else-if="profile">
      <!-- Success Message -->
      <div v-if="successMessage" class="alert-success mb-6">
        <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        {{ successMessage }}
      </div>

      <!-- Error Message -->
      <div v-if="errorMessage" class="alert-error mb-6">
        {{ errorMessage }}
      </div>

      <!-- Profile Header Card -->
      <div class="card mb-6">
        <div class="p-6">
          <div class="flex items-start gap-4">
            <!-- Avatar -->
            <div class="w-20 h-20 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="profile.avatarUrl"
                :src="profile.avatarUrl"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-10 h-10 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>

            <!-- Info -->
            <div class="flex-1">
              <h2 class="text-xl font-bold">{{ profile.displayName || 'No name set' }}</h2>
              <p class="text-gray-500">{{ profile.email }}</p>
              <p class="text-sm text-gray-400 mt-1">Member since {{ memberSince }}</p>

              <div v-if="profile.homeCity || profile.homeState" class="flex items-center gap-1 mt-2 text-gray-600">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
                </svg>
                {{ [profile.homeCity, profile.homeState].filter(Boolean).join(', ') }}
              </div>
            </div>

            <!-- Edit Button -->
            <button
              v-if="!isEditing"
              class="btn-outline"
              @click="startEditing"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
              </svg>
              Edit Profile
            </button>
          </div>

          <!-- Bio -->
          <div v-if="profile.bio && !isEditing" class="mt-4 text-gray-600">
            {{ profile.bio }}
          </div>

          <!-- Stats -->
          <div class="grid grid-cols-3 gap-4 mt-6 pt-6 border-t border-gray-100">
            <div class="text-center">
              <div class="text-2xl font-bold text-primary-500">{{ profile.stats?.hostedCount ?? 0 }}</div>
              <div class="text-sm text-gray-500">Games Hosted</div>
            </div>
            <div class="text-center">
              <div class="text-2xl font-bold text-secondary-500">{{ profile.stats?.attendedCount ?? 0 }}</div>
              <div class="text-sm text-gray-500">Games Attended</div>
            </div>
            <div class="text-center">
              <div class="text-2xl font-bold text-accent-500">{{ profile.stats?.groupCount ?? 0 }}</div>
              <div class="text-sm text-gray-500">Groups</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Form -->
      <div v-if="isEditing" class="card mb-6">
        <div class="p-6 border-b border-gray-100">
          <h3 class="font-semibold">Edit Profile</h3>
        </div>
        <div class="p-6 space-y-6">
          <!-- Basic Info -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="label">Display Name</label>
              <input
                v-model="form.displayName"
                type="text"
                class="input"
                placeholder="Your display name"
              />
            </div>
            <div>
              <label class="label">Avatar URL</label>
              <input
                v-model="form.avatarUrl"
                type="url"
                class="input"
                placeholder="https://..."
              />
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="label">Birth Year</label>
              <input
                v-model.number="form.birthYear"
                type="number"
                class="input"
                :min="1900"
                :max="new Date().getFullYear()"
                placeholder="e.g. 1990"
              />
              <p class="text-sm text-gray-500 mt-1">Used to check age requirements for game nights</p>
            </div>
          </div>

          <div>
            <label class="label">Bio</label>
            <textarea
              v-model="form.bio"
              rows="3"
              class="input"
              placeholder="Tell others about yourself..."
            ></textarea>
          </div>

          <!-- Location -->
          <div>
            <h4 class="font-medium mb-3">Location</h4>
            <div class="grid grid-cols-12 gap-4">
              <div class="col-span-5">
                <label class="label">City</label>
                <input
                  v-model="form.homeCity"
                  type="text"
                  class="input"
                  placeholder="City"
                />
              </div>
              <div class="col-span-4">
                <label class="label">State</label>
                <input
                  v-model="form.homeState"
                  type="text"
                  class="input"
                  placeholder="State"
                />
              </div>
              <div class="col-span-3">
                <label class="label">Zip</label>
                <input
                  v-model="form.homePostalCode"
                  type="text"
                  class="input"
                  placeholder="Zip"
                />
              </div>
            </div>
          </div>

          <!-- Travel Distance -->
          <div>
            <label class="label">Maximum Travel Distance</label>
            <select v-model="form.maxTravelMiles" class="input w-auto">
              <option
                v-for="opt in travelOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
            <p class="text-sm text-gray-500 mt-1">How far are you willing to travel for game nights?</p>
          </div>

          <!-- Favorite Games -->
          <div>
            <label class="label">Favorite Games</label>
            <div class="flex gap-2 mb-2">
              <input
                v-model="newFavoriteGame"
                type="text"
                class="input flex-1"
                placeholder="Add a favorite game..."
                @keyup.enter="addFavoriteGame"
              />
              <button
                type="button"
                class="btn-outline"
                @click="addFavoriteGame"
              >
                Add
              </button>
            </div>
            <div v-if="form.favoriteGames && form.favoriteGames.length > 0" class="flex flex-wrap gap-2">
              <span
                v-for="(game, index) in form.favoriteGames"
                :key="index"
                class="chip bg-primary-100 text-primary-700 pr-1"
              >
                {{ game }}
                <button
                  type="button"
                  class="ml-1 p-0.5 hover:bg-primary-200 rounded"
                  @click="removeFavoriteGame(index)"
                >
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                  </svg>
                </button>
              </span>
            </div>
          </div>

          <!-- Preferred Game Types -->
          <div>
            <label class="label">Preferred Game Types</label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="opt in gameTypeOptions"
                :key="opt.value"
                type="button"
                class="chip transition-colors"
                :class="form.preferredGameTypes?.includes(opt.value)
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                @click="toggleGameType(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex justify-end gap-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              class="btn-ghost"
              :disabled="saving"
              @click="cancelEditing"
            >
              Cancel
            </button>
            <button
              type="button"
              class="btn-primary"
              :disabled="saving"
              @click="saveProfile"
            >
              <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Save Changes
            </button>
          </div>
        </div>
      </div>

      <!-- Favorite Games (View Mode) -->
      <div v-if="!isEditing && profile.favoriteGames && profile.favoriteGames.length > 0" class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
            </svg>
            Favorite Games
          </h3>
        </div>
        <div class="p-4">
          <div class="flex flex-wrap gap-2">
            <span
              v-for="game in profile.favoriteGames"
              :key="game"
              class="chip bg-primary-100 text-primary-700"
            >
              {{ game }}
            </span>
          </div>
        </div>
      </div>

      <!-- Preferred Game Types (View Mode) -->
      <div v-if="!isEditing && profile.preferredGameTypes && profile.preferredGameTypes.length > 0" class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
            </svg>
            Preferred Game Types
          </h3>
        </div>
        <div class="p-4">
          <div class="flex flex-wrap gap-2">
            <span
              v-for="type in profile.preferredGameTypes"
              :key="type"
              class="chip bg-secondary-100 text-secondary-700"
            >
              {{ gameTypeOptions.find(o => o.value === type)?.label || type }}
            </span>
          </div>
        </div>
      </div>

      <!-- Groups -->
      <div class="card mb-6">
        <div class="p-4 border-b border-gray-100 flex items-center justify-between">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-accent-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
            </svg>
            My Groups
          </h3>
          <button @click="router.push('/groups')" class="text-sm text-primary-500 hover:text-primary-600 font-medium">
            Browse Groups
          </button>
        </div>
        <div v-if="profile.groups && profile.groups.length > 0" class="divide-y divide-gray-100">
          <div
            v-for="membership in profile.groups"
            :key="membership.group?.id"
            class="flex items-center gap-3 p-4 hover:bg-gray-50 transition-colors"
          >
            <button
              class="flex items-center gap-3 flex-1 min-w-0 text-left"
              @click="membership.group && goToGroup(membership.group.slug)"
            >
              <div class="w-10 h-10 rounded-lg bg-accent-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  v-if="membership.group?.logoUrl"
                  :src="membership.group.logoUrl"
                  class="w-full h-full object-cover"
                />
                <svg v-else class="w-5 h-5 text-accent-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25Z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium">{{ membership.group?.name }}</div>
                <div class="text-sm text-gray-500 capitalize">
                  {{ membership.group?.groupType === 'geographic' ? 'Local Community' : membership.group?.groupType === 'interest' ? 'Interest Group' : 'Community' }}
                </div>
              </div>
            </button>
            <span :class="[
              'text-xs px-2 py-1 rounded-full',
              membership.role === 'owner' ? 'bg-purple-100 text-purple-700' :
              membership.role === 'admin' ? 'bg-blue-100 text-blue-700' :
              'bg-gray-100 text-gray-600'
            ]">
              {{ membership.role }}
            </span>
            <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
            </svg>
          </div>
        </div>
        <div v-else class="p-8 text-center">
          <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
          </svg>
          <p class="text-gray-500 mb-3">You haven't joined any groups yet.</p>
          <button @click="router.push('/groups')" class="btn-primary">
            Find Groups
          </button>
        </div>
      </div>

      <!-- Blocked Users -->
      <div class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2C17.5,2 22,6.5 22,12C22,17.5 17.5,22 12,22C6.5,22 2,17.5 2,12C2,6.5 6.5,2 12,2M12,4C7.58,4 4,7.58 4,12C4,16.42 7.58,20 12,20C16.42,20 20,16.42 20,12C20,7.58 16.42,4 12,4M16.59,6L18,7.41L13.41,12L18,16.59L16.59,18L12,13.41L7.41,18L6,16.59L10.59,12L6,7.41L7.41,6L12,10.59L16.59,6Z"/>
            </svg>
            Blocked Users
          </h3>
        </div>
        <div v-if="loadingBlocked" class="p-8 text-center">
          <svg class="w-6 h-6 mx-auto text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <div v-else-if="blockedUsers.length > 0" class="divide-y divide-gray-100">
          <div
            v-for="user in blockedUsers"
            :key="user.id"
            class="flex items-center gap-3 p-4"
          >
            <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="user.avatarUrl"
                :src="user.avatarUrl"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium">{{ user.displayName || 'Unknown User' }}</div>
            </div>
            <button
              class="btn-ghost text-red-500 text-sm"
              @click="handleUnblock(user.id)"
            >
              Unblock
            </button>
          </div>
        </div>
        <div v-else class="p-8 text-center">
          <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M12,6A6,6 0 0,1 18,12A6,6 0 0,1 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6M12,8A4,4 0 0,0 8,12A4,4 0 0,0 12,16A4,4 0 0,0 16,12A4,4 0 0,0 12,8Z"/>
          </svg>
          <p class="text-gray-500">You haven't blocked anyone.</p>
          <p class="text-sm text-gray-400 mt-1">Blocked users won't appear in your LFP or Games feeds.</p>
        </div>
      </div>

      <!-- Upcoming Events -->
      <div v-if="profile.upcomingEvents && profile.upcomingEvents.length > 0" class="card">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
            </svg>
            Upcoming Games
          </h3>
        </div>
        <div class="divide-y divide-gray-100">
          <button
            v-for="reg in profile.upcomingEvents"
            :key="reg.event?.id"
            class="w-full flex items-center gap-3 p-4 hover:bg-gray-50 transition-colors text-left"
            @click="reg.event && goToEvent(reg.event.id)"
          >
            <div class="w-12 h-12 rounded-lg bg-primary-100 flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium">{{ reg.event?.title }}</div>
              <div class="text-sm text-gray-500">
                {{ reg.event?.eventDate ? formatDate(reg.event.eventDate) : '' }}
                <span v-if="reg.event?.city"> &bull; {{ reg.event.city }}<span v-if="reg.event?.state">, {{ reg.event.state }}</span></span>
              </div>
            </div>
            <span class="chip-success text-xs">{{ reg.status }}</span>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>
