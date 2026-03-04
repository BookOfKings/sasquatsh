<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useEventStore } from '@/stores/useEventStore'
import { getMyProfile, updateProfile, getBlockedUsers, unblockUser, uploadAvatar, deleteAvatar } from '@/services/profileApi'
import { checkUsernameAvailable } from '@/services/authApi'
import { getLocationById } from '@/services/venuesApi'
import type { UserProfile, UpdateProfileInput, BlockedUser } from '@/types/profile'
import type { EventLocation } from '@/types/social'
import D20Spinner from '@/components/common/D20Spinner.vue'
import GameSearch from '@/components/common/GameSearch.vue'
import HotLocationsBar from '@/components/venues/HotLocationsBar.vue'
import VenueSelector from '@/components/venues/VenueSelector.vue'
import VenueDetailsFields from '@/components/venues/VenueDetailsFields.vue'
import SubmitVenueModal from '@/components/venues/SubmitVenueModal.vue'
import type { BggGame } from '@/types/bgg'
import { TIER_NAMES, type SubscriptionTier } from '@/config/subscriptionLimits'
import { getEffectiveTier } from '@/types/user'

const router = useRouter()
const auth = useAuthStore()
const eventStore = useEventStore()

const loading = ref(true)
const loadingHistory = ref(true)
const saving = ref(false)
const profile = ref<UserProfile | null>(null)
const blockedUsers = ref<BlockedUser[]>([])
const loadingBlocked = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const isEditing = ref(false)

// Username validation state
const usernameChecking = ref(false)
const usernameAvailable = ref<boolean | null>(null)
const usernameError = ref('')
let usernameCheckTimer: ReturnType<typeof setTimeout> | null = null
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/

// Venue modal state
const showVenueModal = ref(false)
const selectedVenue = ref<EventLocation | null>(null)

// Avatar upload state
const avatarUploading = ref(false)
const avatarDeleting = ref(false)
const avatarError = ref('')
const avatarFileInput = ref<HTMLInputElement | null>(null)

const form = reactive<UpdateProfileInput>({
  username: '',
  displayName: '',
  avatarUrl: '',
  birthYear: undefined,
  maxTravelMiles: 25,
  homeCity: '',
  homeState: '',
  homePostalCode: '',
  activeCity: '',
  activeState: '',
  activeLocationExpiresAt: '',
  activeEventLocationId: undefined,
  activeLocationHall: undefined,
  activeLocationRoom: undefined,
  activeLocationTable: undefined,
  timezone: 'America/New_York',
  bio: '',
  favoriteGames: [],
  preferredGameTypes: [],
})

// Common US timezone options
const timezoneOptions = [
  { value: 'America/New_York', label: 'Eastern Time (ET)' },
  { value: 'America/Chicago', label: 'Central Time (CT)' },
  { value: 'America/Denver', label: 'Mountain Time (MT)' },
  { value: 'America/Phoenix', label: 'Arizona (no DST)' },
  { value: 'America/Los_Angeles', label: 'Pacific Time (PT)' },
  { value: 'America/Anchorage', label: 'Alaska Time (AKT)' },
  { value: 'Pacific/Honolulu', label: 'Hawaii Time (HT)' },
  { value: 'Europe/London', label: 'UK (GMT/BST)' },
  { value: 'Europe/Paris', label: 'Central Europe (CET)' },
  { value: 'Asia/Tokyo', label: 'Japan (JST)' },
  { value: 'Australia/Sydney', label: 'Sydney (AEST)' },
]

// Watch username changes for availability checking
watch(() => form.username, (newUsername) => {
  // Reset state
  usernameAvailable.value = null
  usernameError.value = ''

  // Clear previous timer
  if (usernameCheckTimer) {
    clearTimeout(usernameCheckTimer)
  }

  // If username hasn't changed from profile, mark as available
  if (newUsername === profile.value?.username) {
    usernameAvailable.value = true
    return
  }

  if (!newUsername) {
    return
  }

  // Validate format first
  if (!USERNAME_REGEX.test(newUsername)) {
    if (newUsername.length < 3) {
      usernameError.value = 'Username must be at least 3 characters'
    } else if (newUsername.length > 30) {
      usernameError.value = 'Username must be at most 30 characters'
    } else if (!/^[a-zA-Z]/.test(newUsername)) {
      usernameError.value = 'Username must start with a letter'
    } else {
      usernameError.value = 'Only letters, numbers, and underscores allowed'
    }
    return
  }

  // Debounce the availability check
  usernameCheckTimer = setTimeout(async () => {
    usernameChecking.value = true
    try {
      const result = await checkUsernameAvailable(newUsername)
      usernameAvailable.value = result.available
      if (!result.available && result.reason) {
        usernameError.value = result.reason
      }
    } catch (err) {
      console.error('Failed to check username:', err)
    } finally {
      usernameChecking.value = false
    }
  }, 500)
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

const currentTier = computed((): SubscriptionTier => {
  if (!auth.user.value) return 'free'
  return getEffectiveTier(auth.user.value)
})

function getTierColor(tier: SubscriptionTier): string {
  switch (tier) {
    case 'pro':
      return 'bg-purple-100 text-purple-800 border-purple-200'
    case 'basic':
      return 'bg-blue-100 text-blue-800 border-blue-200'
    case 'premium':
      return 'bg-yellow-100 text-yellow-800 border-yellow-200'
    default:
      return 'bg-gray-100 text-gray-800 border-gray-200'
  }
}

// Get today's date for filtering
const today = new Date().toISOString().split('T')[0] ?? ''

// Past games (before today)
const pastGames = computed(() => {
  const hosted = eventStore.hostedEvents.value.filter(g => g.eventDate < today)
  const registered = eventStore.myEvents.value.filter(g => g.eventDate < today)

  // Combine and deduplicate (in case user hosts and also registered)
  const allPast = [...hosted]
  for (const event of registered) {
    if (!allPast.find(e => e.id === event.id)) {
      allPast.push(event)
    }
  }

  // Sort by date descending (most recent first)
  return allPast.sort((a, b) => b.eventDate.localeCompare(a.eventDate))
})

onMounted(async () => {
  await loadProfile()
  await Promise.all([
    loadBlockedUsers(),
    loadGameHistory(),
  ])
})

async function loadGameHistory() {
  loadingHistory.value = true
  try {
    await Promise.all([
      eventStore.loadMyEvents(),
      eventStore.loadHostedEvents(),
    ])
  } finally {
    loadingHistory.value = false
  }
}

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

function triggerAvatarUpload() {
  avatarFileInput.value?.click()
}

async function handleAvatarUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // Validate file type
  const allowedTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    avatarError.value = 'Please select a PNG, JPEG, WebP, or GIF image'
    return
  }

  // Validate file size (2MB)
  if (file.size > 2 * 1024 * 1024) {
    avatarError.value = 'Image must be smaller than 2MB'
    return
  }

  avatarUploading.value = true
  avatarError.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const result = await uploadAvatar(token, file)
    if (profile.value) {
      profile.value.avatarUrl = result.avatarUrl
    }
    // Update auth store so header shows new avatar
    auth.updateUserData({ avatarUrl: result.avatarUrl })
    successMessage.value = 'Avatar updated!'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    avatarError.value = err instanceof Error ? err.message : 'Failed to upload avatar'
  } finally {
    avatarUploading.value = false
    // Reset input so same file can be selected again
    input.value = ''
  }
}

async function handleAvatarDelete() {
  if (!confirm('Remove your avatar?')) return

  avatarDeleting.value = true
  avatarError.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteAvatar(token)
    if (profile.value) {
      profile.value.avatarUrl = null
    }
    // Update auth store so header removes avatar
    auth.updateUserData({ avatarUrl: undefined })
    successMessage.value = 'Avatar removed'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    avatarError.value = err instanceof Error ? err.message : 'Failed to remove avatar'
  } finally {
    avatarDeleting.value = false
  }
}

async function populateForm() {
  if (!profile.value) return
  form.username = profile.value.username ?? ''
  form.displayName = profile.value.displayName ?? ''
  form.avatarUrl = profile.value.avatarUrl ?? ''
  form.birthYear = profile.value.birthYear ?? undefined
  form.maxTravelMiles = profile.value.maxTravelMiles ?? 25
  form.homeCity = profile.value.homeCity ?? ''
  form.homeState = profile.value.homeState ?? ''
  form.homePostalCode = profile.value.homePostalCode ?? ''
  form.activeCity = profile.value.activeCity ?? ''
  form.activeState = profile.value.activeState ?? ''
  form.activeLocationExpiresAt = profile.value.activeLocationExpiresAt?.split('T')[0] ?? ''
  form.activeEventLocationId = profile.value.activeEventLocationId ?? undefined
  form.activeLocationHall = profile.value.activeLocationHall ?? undefined
  form.activeLocationRoom = profile.value.activeLocationRoom ?? undefined
  form.activeLocationTable = profile.value.activeLocationTable ?? undefined
  form.timezone = profile.value.timezone ?? 'America/New_York'
  form.bio = profile.value.bio ?? ''
  form.favoriteGames = profile.value.favoriteGames ?? []
  form.preferredGameTypes = profile.value.preferredGameTypes ?? []
  // Reset username availability state
  usernameAvailable.value = true
  usernameError.value = ''

  // Load venue details if set
  if (profile.value.activeEventLocationId) {
    try {
      selectedVenue.value = await getLocationById(profile.value.activeEventLocationId)
    } catch (err) {
      console.error('Failed to load venue details:', err)
      selectedVenue.value = null
    }
  } else {
    selectedVenue.value = null
  }
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
  // Validate username if changed
  if (form.username && form.username !== profile.value?.username) {
    if (!USERNAME_REGEX.test(form.username)) {
      errorMessage.value = 'Please enter a valid username'
      return
    }
    if (usernameAvailable.value === false) {
      errorMessage.value = 'Please choose a different username'
      return
    }
  }

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

function handleBggGameSelect(game: BggGame) {
  if (game.name && !form.favoriteGames?.includes(game.name)) {
    form.favoriteGames = [...(form.favoriteGames ?? []), game.name]
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

function handleVenueSelect(venue: EventLocation) {
  selectedVenue.value = venue
  form.activeEventLocationId = venue.id
  form.activeCity = venue.city
  form.activeState = venue.state
  // Set expiration to venue end date
  form.activeLocationExpiresAt = venue.endDate
}

function handleVenueSelectorSelect(venue: EventLocation | null) {
  if (venue) {
    handleVenueSelect(venue)
  } else {
    clearVenueSelection()
  }
}

function clearVenueSelection() {
  selectedVenue.value = null
  form.activeEventLocationId = undefined
  form.activeLocationHall = undefined
  form.activeLocationRoom = undefined
  form.activeLocationTable = undefined
}

function clearActiveLocation() {
  form.activeCity = ''
  form.activeState = ''
  form.activeLocationExpiresAt = ''
  clearVenueSelection()
}

function handleVenueSubmitted(venue: EventLocation) {
  // Could auto-select the submitted venue if approved
  console.log('Venue submitted:', venue.name)
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
      <D20Spinner size="lg" class="mx-auto" />
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
            <!-- Avatar with upload button -->
            <div class="relative flex-shrink-0">
              <div
                class="w-20 h-20 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden cursor-pointer group"
                @click="triggerAvatarUpload"
              >
                <img
                  v-if="profile.avatarUrl"
                  :src="profile.avatarUrl"
                  class="w-full h-full object-cover"
                />
                <svg v-else class="w-10 h-10 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                </svg>
                <!-- Upload overlay -->
                <div class="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded-full">
                  <svg v-if="avatarUploading" class="w-6 h-6 text-white animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  <svg v-else class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M9,16V10H5L12,3L19,10H15V16H9M5,20V18H19V20H5Z"/>
                  </svg>
                </div>
              </div>
              <!-- Delete button (shows when avatar exists) -->
              <button
                v-if="profile.avatarUrl && !avatarUploading && !avatarDeleting"
                class="absolute -bottom-1 -right-1 w-6 h-6 bg-red-500 rounded-full flex items-center justify-center text-white hover:bg-red-600 transition-colors"
                title="Remove avatar"
                @click.stop="handleAvatarDelete"
              >
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
              <!-- Hidden file input -->
              <input
                ref="avatarFileInput"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                class="hidden"
                @change="handleAvatarUpload"
              />
            </div>

            <!-- Info -->
            <div class="flex-1">
              <h2 class="text-xl font-bold">{{ profile.displayName || 'No name set' }}</h2>
              <p class="text-primary-600 font-medium">@{{ profile.username }}</p>
              <p class="text-gray-500 text-sm">{{ profile.email }}</p>
              <p class="text-sm text-gray-400 mt-1">Member since {{ memberSince }}</p>

              <div v-if="profile.homeCity || profile.homeState" class="flex items-center gap-1 mt-2 text-gray-600">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
                </svg>
                {{ [profile.homeCity, profile.homeState].filter(Boolean).join(', ') }}
              </div>

              <!-- Active Location Badge -->
              <div v-if="profile.activeCity || profile.activeState || selectedVenue" class="flex items-center gap-1 mt-2 text-secondary-600">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.27,6C19.28,8.17 19.05,10.73 17.94,12.81C17,14.5 15.65,15.93 14.5,17.5C14,18.14 13.5,18.81 13,19.5L12,20.75L11,19.5C10.5,18.81 10,18.14 9.5,17.5C8.35,15.93 7,14.5 6.06,12.81C4.95,10.73 4.72,8.17 5.73,6C7.21,2.89 11.33,2.03 13.86,4.11C14.45,4.57 14.96,5.13 15.35,5.76C15.57,5.36 15.82,5 16.11,4.63C17.56,2.95 20.1,2.88 21.47,4.63C22.68,6.17 22.88,8.46 22.13,10.3C21.55,11.72 20.61,12.96 19.55,14.04C19.33,14.26 19.1,14.5 18.86,14.71C18.38,15.17 17.39,15.4 16.95,14.95C16.5,14.5 16.72,13.55 17.2,13.06C17.44,12.83 17.67,12.59 17.88,12.35C18.75,11.5 19.5,10.53 19.88,9.4C20.23,8.34 20.25,7.14 19.66,6.16C19.24,5.45 18.44,5.13 17.65,5.3C17,5.44 16.47,5.89 16.11,6.44C16.03,6.56 15.96,6.69 15.89,6.82L14.47,9.73L13.05,6.83C12.97,6.68 12.89,6.53 12.79,6.39C12.07,5.38 10.7,5 9.57,5.5C8.59,5.94 8,6.85 8,7.84C8,8.35 8.09,8.87 8.26,9.36C8.54,10.17 9,10.9 9.54,11.56C10,12.11 10.5,12.64 11,13.16L11.23,13.39C11.74,13.9 12.73,14.15 13.2,13.65C13.67,13.15 13.47,12.2 12.97,11.7L12.79,11.5C12.39,11.1 12,10.68 11.66,10.24C11.3,9.77 11,9.26 10.83,8.72C10.71,8.36 10.72,8 10.89,7.72C11.08,7.4 11.45,7.23 11.82,7.25C12.23,7.27 12.59,7.5 12.82,7.85L14.5,10.62L16.18,7.86C16.41,7.53 16.75,7.28 17.15,7.26H17.2Z"/>
                </svg>
                <span class="font-medium">Traveling:</span>
                <template v-if="selectedVenue">
                  {{ selectedVenue.name }}
                  <span v-if="profile.activeLocationHall || profile.activeLocationRoom || profile.activeLocationTable" class="text-xs">
                    ({{ [profile.activeLocationHall, profile.activeLocationRoom, profile.activeLocationTable].filter(Boolean).join(' / ') }})
                  </span>
                </template>
                <template v-else>
                  {{ [profile.activeCity, profile.activeState].filter(Boolean).join(', ') }}
                </template>
                <span v-if="profile.activeLocationExpiresAt" class="text-xs text-gray-500">(until {{ new Date(profile.activeLocationExpiresAt).toLocaleDateString() }})</span>
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
          <!-- Username -->
          <div>
            <label class="label">Username</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">@</span>
              <input
                v-model="form.username"
                type="text"
                class="input pl-7"
                :class="{
                  'border-green-500 focus:ring-green-500': usernameAvailable === true && form.username !== profile?.username,
                  'border-red-500 focus:ring-red-500': usernameAvailable === false || usernameError
                }"
                placeholder="your_username"
              />
              <!-- Loading indicator -->
              <div v-if="usernameChecking" class="absolute right-3 top-1/2 -translate-y-1/2">
                <svg class="animate-spin h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
              </div>
              <!-- Available checkmark (only when changed) -->
              <div v-else-if="usernameAvailable === true && form.username !== profile?.username" class="absolute right-3 top-1/2 -translate-y-1/2">
                <svg class="h-5 w-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                </svg>
              </div>
              <!-- Unavailable X -->
              <div v-else-if="usernameAvailable === false || usernameError" class="absolute right-3 top-1/2 -translate-y-1/2">
                <svg class="h-5 w-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </div>
            </div>
            <p v-if="usernameError" class="text-xs text-red-500 mt-1">{{ usernameError }}</p>
            <p v-else-if="usernameAvailable === true && form.username !== profile?.username" class="text-xs text-green-600 mt-1">Username is available!</p>
            <p v-else class="text-xs text-gray-500 mt-1">3-30 characters, letters, numbers, and underscores only</p>
          </div>

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
              <p class="text-xs text-gray-500 mt-1">Visible to group members and event hosts</p>
            </div>
            <div>
              <label class="label">Avatar</label>
              <p class="text-sm text-gray-500">Click your avatar above to upload a new picture (PNG, JPEG, WebP, or GIF, max 2MB)</p>
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
            <div>
              <label class="label">Time Zone</label>
              <select v-model="form.timezone" class="input">
                <option v-for="tz in timezoneOptions" :key="tz.value" :value="tz.value">
                  {{ tz.label }}
                </option>
              </select>
              <p class="text-sm text-gray-500 mt-1">Used to display event times correctly</p>
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

          <!-- Home Location -->
          <div>
            <h4 class="font-medium mb-3">Home Location</h4>
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

          <!-- Active Location (Traveling) -->
          <div>
            <h4 class="font-medium mb-1">Active Location</h4>
            <p class="text-sm text-gray-500 mb-3">Set a temporary location when traveling (e.g., attending a convention)</p>

            <!-- Hot Locations Quick Select -->
            <HotLocationsBar
              :selected-id="form.activeEventLocationId"
              @select="handleVenueSelect"
            />

            <!-- Venue Selector -->
            <div class="mb-4">
              <label class="label">Select a Venue</label>
              <VenueSelector
                :model-value="form.activeEventLocationId ?? null"
                @update:model-value="(v) => form.activeEventLocationId = v ?? undefined"
                @select="handleVenueSelectorSelect"
              />
              <div class="flex items-center gap-2 mt-2">
                <button
                  type="button"
                  class="text-sm text-primary-500 hover:text-primary-600"
                  @click="showVenueModal = true"
                >
                  + Submit a new venue
                </button>
              </div>
            </div>

            <!-- Venue Details (Hall/Room/Table) when venue selected -->
            <div v-if="selectedVenue" class="mb-4">
              <label class="label">Location Details (optional)</label>
              <VenueDetailsFields
                :hall="form.activeLocationHall"
                :room="form.activeLocationRoom"
                :table="form.activeLocationTable"
                @update:hall="(v) => form.activeLocationHall = v ?? undefined"
                @update:room="(v) => form.activeLocationRoom = v ?? undefined"
                @update:table="(v) => form.activeLocationTable = v ?? undefined"
              />
            </div>

            <!-- Custom Location (City/State/Date) -->
            <div v-if="!selectedVenue" class="border-t border-gray-200 pt-4 mt-4">
              <p class="text-sm text-gray-500 mb-3">Or enter a custom location:</p>
              <div class="grid grid-cols-12 gap-4">
                <div class="col-span-5">
                  <label class="label">City</label>
                  <input
                    v-model="form.activeCity"
                    type="text"
                    class="input"
                    placeholder="Convention city"
                  />
                </div>
                <div class="col-span-4">
                  <label class="label">State</label>
                  <input
                    v-model="form.activeState"
                    type="text"
                    class="input"
                    placeholder="State"
                  />
                </div>
                <div class="col-span-3">
                  <label class="label">Until</label>
                  <input
                    v-model="form.activeLocationExpiresAt"
                    type="date"
                    class="input"
                    :min="today"
                  />
                </div>
              </div>
            </div>

            <button
              v-if="form.activeCity || form.activeState || selectedVenue"
              type="button"
              class="text-sm text-red-500 hover:text-red-600 mt-2"
              @click="clearActiveLocation"
            >
              Clear active location
            </button>
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
            <p class="text-sm text-gray-500 mb-2">Search BoardGameGeek to add your favorite games</p>
            <div class="flex gap-2 mb-2">
              <div class="flex-1">
                <GameSearch
                  v-model="newFavoriteGame"
                  placeholder="Search BGG for a game..."
                  @select="handleBggGameSelect"
                />
              </div>
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
              <div class="font-medium">@{{ user.username }}</div>
              <div v-if="user.displayName" class="text-sm text-gray-500">{{ user.displayName }}</div>
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

      <!-- Billing & Subscription -->
      <div class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20,8H4V6H20M20,18H4V12H20M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
            </svg>
            Subscription
          </h3>
        </div>
        <div class="p-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <span
                class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border"
                :class="getTierColor(currentTier)"
              >
                {{ TIER_NAMES[currentTier] }}
              </span>
              <span v-if="auth.user.value?.subscriptionOverrideTier" class="text-sm text-gray-500">
                (Complimentary)
              </span>
            </div>
            <div class="flex items-center gap-2">
              <router-link
                v-if="currentTier === 'free'"
                to="/pricing"
                class="btn-primary text-sm"
              >
                Upgrade
              </router-link>
              <router-link
                to="/billing"
                class="btn-outline text-sm"
              >
                {{ currentTier === 'free' ? 'View Plans' : 'Manage' }}
              </router-link>
            </div>
          </div>
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

      <!-- Game History -->
      <div class="card">
        <div class="p-4 border-b border-gray-100">
          <h3 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-gray-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M13.5,8H12V13L16.28,15.54L17,14.33L13.5,12.25V8M13,3A9,9 0 0,0 4,12H1L4.96,16.03L9,12H6A7,7 0 0,1 13,5A7,7 0 0,1 20,12A7,7 0 0,1 13,19C11.07,19 9.32,18.21 8.06,16.94L6.64,18.36C8.27,20 10.5,21 13,21A9,9 0 0,0 22,12A9,9 0 0,0 13,3"/>
            </svg>
            Game History
          </h3>
        </div>
        <div v-if="loadingHistory" class="p-8 text-center">
          <svg class="w-6 h-6 mx-auto text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <div v-else-if="pastGames.length > 0" class="divide-y divide-gray-100">
          <button
            v-for="game in pastGames.slice(0, 10)"
            :key="game.id"
            class="w-full flex items-center gap-3 p-4 hover:bg-gray-50 transition-colors text-left"
            @click="goToEvent(game.id)"
          >
            <div class="w-12 h-12 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-700">{{ game.title }}</div>
              <div class="text-sm text-gray-500">
                {{ formatDate(game.eventDate) }}
                <span v-if="game.city"> &bull; {{ game.city }}<span v-if="game.state">, {{ game.state }}</span></span>
              </div>
            </div>
            <span class="text-xs text-gray-400">{{ game.gameTitle || '' }}</span>
          </button>
          <div v-if="pastGames.length > 10" class="p-4 text-center text-sm text-gray-500">
            Showing most recent 10 of {{ pastGames.length }} games
          </div>
        </div>
        <div v-else class="p-8 text-center">
          <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
          </svg>
          <p class="text-gray-500">No game history yet.</p>
          <p class="text-sm text-gray-400 mt-1">Your completed games will appear here.</p>
        </div>
      </div>
    </template>

    <!-- Submit Venue Modal -->
    <SubmitVenueModal
      :visible="showVenueModal"
      @close="showVenueModal = false"
      @submitted="handleVenueSubmitted"
    />
  </div>
</template>
