<script setup lang="ts">
import { onMounted, computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEventStore } from '@/stores/useEventStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { hasFeature } from '@/config/subscriptionLimits'
import { inviteGroupMembersToEvent } from '@/services/eventsApi'
import { registerForSession, cancelSessionRegistration } from '@/services/sessionsApi'
import { supabase } from '@/services/supabase'
import ShareModal from '@/components/common/ShareModal.vue'
import D20Spinner from '@/components/common/D20Spinner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import SessionScheduleGrid from '@/components/events/SessionScheduleGrid.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const route = useRoute()
const router = useRouter()
const eventStore = useEventStore()
const auth = useAuthStore()

const toast = reactive({
  visible: false,
  message: '',
  type: 'success' as 'success' | 'error',
})

const addItemDialog = ref(false)
const newItemName = ref('')
const newItemCategory = ref('games')
const newItemBringing = ref(false) // Toggle for "I'm bringing this"
const showShareModal = ref(false)

// Invite group members state
const inviteMembersDialog = ref(false)
const groupMembers = ref<{ id: string; displayName: string; avatarUrl: string | null; selected: boolean }[]>([])
const loadingGroupMembers = ref(false)
const invitingMembers = ref(false)

// Session registration state (multi-table mode)
const registeringSession = ref(false)

// Chat state
const showChat = ref(false)

const eventId = computed(() => route.params.id as string)
const event = computed(() => eventStore.currentEvent.value)
const isHost = computed(() => event.value?.hostUserId === auth.user.value?.id)
const canDelete = computed(() => isHost.value || auth.isAdmin.value)
const canEdit = computed(() => isHost.value || auth.isAdmin.value)
const deleting = ref(false)
const isRegistered = computed(() => {
  if (!auth.user.value || !event.value?.registrations) return false
  return event.value.registrations.some(
    (r) => r.userId === auth.user.value?.id && r.status === 'confirmed'
  )
})
const spotsLeft = computed(() => {
  if (!event.value) return 0
  return event.value.maxPlayers - event.value.confirmedCount
})

// Check if user can see full address details
// For private/invite-only events, only show full address to host, registered users, or admins
const canSeeFullAddress = computed(() => {
  if (!event.value) return false
  // Public events - everyone can see address
  if (event.value.isPublic) return true
  // Private events - only host, registered users, or admins
  return isHost.value || isRegistered.value || auth.isAdmin.value
})

// Check if user can see/use items feature
// Items are available if the HOST has Pro+ (not the viewing user)
// This allows free users to participate in paid hosts' events
const canUseItems = computed(() => {
  if (!event.value?.host) return false
  // Get host's effective tier (override takes precedence)
  const hostTier = event.value.host.subscriptionOverrideTier || event.value.host.subscriptionTier || 'free'
  return hasFeature(hostTier, 'items')
})

// Check if chat is available (host has Basic+ subscription)
const canUseChat = computed(() => {
  if (!event.value?.host) return false
  const hostTier = event.value.host.subscriptionOverrideTier || event.value.host.subscriptionTier || 'free'
  return hasFeature(hostTier, 'chat')
})

// Check if user can add items (host or registered player)
const canAddItems = computed(() => {
  if (!auth.isAuthenticated.value) return false
  return isHost.value || isRegistered.value
})

// Check if this is a planned event that allows inviting group members
const isPlannedGroupEvent = computed(() => {
  return event.value?.fromPlanningSessionId && event.value?.groupId
})

// Whether user can invite group members (set via async check on mount)
const canInviteMembers = ref(false)

// Items grouped by status
const neededItems = computed(() => {
  return event.value?.items?.filter(item => !item.claimedByUserId) ?? []
})

const claimedItems = computed(() => {
  // Exclude current user's items (they appear in "What You're Bringing" section)
  const currentUserId = auth.user.value?.id
  return event.value?.items?.filter(item => item.claimedByUserId && item.claimedByUserId !== currentUserId) ?? []
})

// Get items a specific user is bringing
function getItemsForUser(userId: string) {
  return event.value?.items?.filter(item => item.claimedByUserId === userId) ?? []
}

// Get current user's items
const myItems = computed(() => {
  if (!auth.user.value) return []
  return event.value?.items?.filter(item => item.claimedByUserId === auth.user.value?.id) ?? []
})

// Edit item state
const editItemDialog = ref(false)
const editingItem = ref<{ id: string; itemName: string; itemCategory: string } | null>(null)

function openEditItem(item: { id: string; itemName: string; itemCategory: string }) {
  editingItem.value = { ...item }
  editItemDialog.value = true
}

async function handleUpdateItem() {
  if (!editingItem.value || !editingItem.value.itemName.trim()) return

  const result = await eventStore.updateItem(eventId.value, editingItem.value.id, {
    itemName: editingItem.value.itemName.trim(),
    itemCategory: editingItem.value.itemCategory,
  })
  showMessage(result.ok, result.message)

  if (result.ok) {
    editItemDialog.value = false
    editingItem.value = null
  }
}

async function handleRemoveMyItem(itemId: string) {
  // Unclaim the item (removes it from "my items")
  const result = await eventStore.unclaimItem(eventId.value, itemId)
  showMessage(result.ok, result.ok ? 'Item removed from your list' : result.message)
}

// Check if user can invite members (host or group admin)
async function checkCanInviteMembers() {
  if (!event.value?.fromPlanningSessionId || !event.value?.groupId || !auth.user.value) {
    canInviteMembers.value = false
    return
  }

  // Host can always invite
  if (isHost.value) {
    canInviteMembers.value = true
    return
  }

  // Check if user is group admin
  const { data: membership } = await supabase
    .from('group_memberships')
    .select('role')
    .eq('group_id', event.value.groupId)
    .eq('user_id', auth.user.value.id)
    .single()

  canInviteMembers.value = membership?.role === 'owner' || membership?.role === 'admin'
}

// Load group members for invite modal
async function loadGroupMembers() {
  if (!event.value?.groupId) return

  loadingGroupMembers.value = true
  try {
    // Get all group members
    const { data: memberships } = await supabase
      .from('group_memberships')
      .select(`
        user_id,
        user:users!user_id(id, display_name, avatar_url)
      `)
      .eq('group_id', event.value.groupId)

    if (!memberships) {
      groupMembers.value = []
      return
    }

    // Get already registered users
    const registeredIds = new Set(event.value.registrations?.map(r => r.userId) || [])
    // Also exclude the host
    registeredIds.add(event.value.hostUserId)

    // Filter out already registered and map to our format
    type UserData = { id: string; display_name: string | null; avatar_url: string | null }
    groupMembers.value = memberships
      .filter(m => !registeredIds.has(m.user_id))
      .map(m => {
        const user = m.user as unknown as UserData
        return {
          id: user.id,
          displayName: user.display_name || 'Anonymous',
          avatarUrl: user.avatar_url,
          selected: false,
        }
      })
  } finally {
    loadingGroupMembers.value = false
  }
}

// Open invite members modal
async function openInviteMembersModal() {
  inviteMembersDialog.value = true
  await loadGroupMembers()
}

// Handle inviting selected members
async function handleInviteMembers() {
  const selectedIds = groupMembers.value.filter(m => m.selected).map(m => m.id)
  if (selectedIds.length === 0) {
    showMessage(false, 'Please select at least one member to invite')
    return
  }

  invitingMembers.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) {
      showMessage(false, 'Authentication error')
      return
    }

    const result = await inviteGroupMembersToEvent(token, eventId.value, selectedIds)
    showMessage(true, result.message)
    inviteMembersDialog.value = false

    // Reload event to show new registrations
    await eventStore.loadEvent(eventId.value)
  } catch (error) {
    showMessage(false, error instanceof Error ? error.message : 'Failed to invite members')
  } finally {
    invitingMembers.value = false
  }
}

// Toggle all members selection
function toggleAllMembers(selectAll: boolean) {
  groupMembers.value.forEach(m => {
    m.selected = selectAll
  })
}

const selectedMembersCount = computed(() => groupMembers.value.filter(m => m.selected).length)

onMounted(async () => {
  await eventStore.loadEvent(eventId.value)
  // Check invite permissions after event loads
  checkCanInviteMembers()
})

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

function formatTime(timeStr: string): string {
  const parts = timeStr.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const date = new Date()
  date.setHours(hours, minutes)
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  })
}

function getDifficultyClasses(level: string | null): string {
  switch (level) {
    case 'beginner': return 'chip-success'
    case 'intermediate': return 'chip-warning'
    case 'advanced': return 'chip-error'
    default: return 'chip bg-gray-100 text-gray-700'
  }
}

function showMessage(ok: boolean, message: string) {
  toast.message = message
  toast.type = ok ? 'success' : 'error'
  toast.visible = true
  setTimeout(() => {
    toast.visible = false
  }, 3500)
}

async function handleRegister() {
  const result = await eventStore.registerForEvent(eventId.value)
  showMessage(result.ok, result.message)
}

async function handleCancelRegistration() {
  const result = await eventStore.cancelRegistration(eventId.value)
  showMessage(result.ok, result.message)
}

// Multi-table session registration handlers
async function handleSessionRegister(sessionId: string) {
  registeringSession.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await registerForSession(token, sessionId)
    showMessage(true, 'Successfully joined session')
    // Reload event to get updated session data
    await eventStore.loadEvent(eventId.value)
  } catch (err) {
    showMessage(false, err instanceof Error ? err.message : 'Failed to join session')
  } finally {
    registeringSession.value = false
  }
}

async function handleSessionCancel(sessionId: string) {
  registeringSession.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await cancelSessionRegistration(token, sessionId)
    showMessage(true, 'Left session successfully')
    // Reload event to get updated session data
    await eventStore.loadEvent(eventId.value)
  } catch (err) {
    showMessage(false, err instanceof Error ? err.message : 'Failed to leave session')
  } finally {
    registeringSession.value = false
  }
}

async function handleAddItem() {
  if (!newItemName.value.trim()) return

  const result = await eventStore.addItem(eventId.value, {
    itemName: newItemName.value.trim(),
    itemCategory: newItemCategory.value,
    bringingItem: newItemBringing.value,
  })
  showMessage(result.ok, result.message)

  if (result.ok) {
    addItemDialog.value = false
    newItemName.value = ''
    newItemCategory.value = 'other'
    newItemBringing.value = false
  }
}

async function handleClaimItem(itemId: string) {
  const result = await eventStore.claimItem(eventId.value, itemId)
  showMessage(result.ok, result.message)
}

async function handleUnclaimItem(itemId: string) {
  const result = await eventStore.unclaimItem(eventId.value, itemId)
  showMessage(result.ok, result.message)
}

async function handleDeleteItem(itemId: string) {
  if (!confirm('Are you sure you want to delete this item?')) return
  const result = await eventStore.deleteItem(eventId.value, itemId)
  showMessage(result.ok, result.ok ? 'Item deleted' : result.message)
}

function goToEdit() {
  router.push(`/games/${eventId.value}/edit`)
}

async function handleDelete() {
  if (!event.value) return
  if (!confirm(`Are you sure you want to delete "${event.value.title}"? This cannot be undone.`)) {
    return
  }

  deleting.value = true
  const result = await eventStore.deleteEvent(eventId.value)

  if (result.ok) {
    showMessage(true, 'Event deleted')
    router.push('/games')
  } else {
    showMessage(false, result.message)
  }
  deleting.value = false
}

function goBack() {
  router.push('/games')
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Back Button -->
    <button class="btn-ghost mb-4" @click="goBack">
      <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
      </svg>
      Back to Games
    </button>

    <!-- Loading -->
    <div v-if="eventStore.loading.value" class="text-center py-12">
      <D20Spinner size="lg" class="mx-auto" />
      <p class="mt-4 text-gray-500">Loading event...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="eventStore.error.value && !event" class="card p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-red-400 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <h2 class="text-xl font-semibold text-gray-900 mb-2">Unable to load event</h2>
      <p class="text-gray-500 mb-6">{{ eventStore.error.value }}</p>
      <button class="btn-primary" @click="eventStore.loadEvent(eventId)">
        Try Again
      </button>
    </div>

    <!-- Event Not Found -->
    <div v-else-if="!eventStore.loading.value && !event" class="card p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
      </svg>
      <h2 class="text-xl font-semibold text-gray-900 mb-2">Event not found</h2>
      <p class="text-gray-500 mb-6">This event may have been deleted or you don't have access to view it.</p>
      <button class="btn-primary" @click="goBack">
        Back to Games
      </button>
    </div>

    <template v-if="event">
      <!-- Main Event Card -->
      <div class="card p-6 mb-6">
        <!-- Header -->
        <div class="flex items-start gap-4 mb-4">
          <UserAvatar
            :avatar-url="event.host?.avatarUrl"
            :display-name="event.host?.displayName"
            :is-founding-member="event.host?.isFoundingMember"
            :is-admin="event.host?.isAdmin"
            size="lg"
            class="flex-shrink-0"
          />
          <div class="flex-1 min-w-0">
            <h1 class="text-2xl font-bold text-gray-900">{{ event.title }}</h1>
            <p class="text-gray-500">
              <span v-if="event.gameTitle">{{ event.gameTitle }} &bull; </span>
              Hosted by {{ event.host?.displayName || 'Unknown' }}
            </p>
          </div>
          <div class="flex gap-2">
            <!-- Invite Group Members (for planned events) -->
            <button
              v-if="isPlannedGroupEvent && canInviteMembers && spotsLeft > 0"
              class="btn-outline text-purple-600 hover:bg-purple-50"
              @click="openInviteMembersModal"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
              </svg>
              Invite Members
            </button>
            <button
              v-if="auth.isAuthenticated.value"
              class="btn-outline"
              @click="showShareModal = true"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M18,16.08C17.24,16.08 16.56,16.38 16.04,16.85L8.91,12.7C8.96,12.47 9,12.24 9,12C9,11.76 8.96,11.53 8.91,11.3L15.96,7.19C16.5,7.69 17.21,8 18,8A3,3 0 0,0 21,5A3,3 0 0,0 18,2A3,3 0 0,0 15,5C15,5.24 15.04,5.47 15.09,5.7L8.04,9.81C7.5,9.31 6.79,9 6,9A3,3 0 0,0 3,12A3,3 0 0,0 6,15C6.79,15 7.5,14.69 8.04,14.19L15.16,18.34C15.11,18.55 15.08,18.77 15.08,19C15.08,20.61 16.39,21.91 18,21.91C19.61,21.91 20.92,20.61 20.92,19A2.92,2.92 0 0,0 18,16.08Z"/>
              </svg>
              Share
            </button>
            <button
              v-if="canEdit"
              class="btn-outline"
              @click="goToEdit"
            >
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
              </svg>
              Edit
            </button>
            <button
              v-if="canDelete"
              class="btn-outline text-red-600 hover:bg-red-50"
              :disabled="deleting"
              @click="handleDelete"
            >
              <svg v-if="deleting" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <svg v-else class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
              </svg>
              Delete
            </button>
          </div>
        </div>

        <!-- Tags -->
        <div class="flex flex-wrap gap-2 mb-6">
          <span
            v-if="event.difficultyLevel"
            :class="getDifficultyClasses(event.difficultyLevel)"
          >
            {{ event.difficultyLevel }}
          </span>
          <span
            v-if="event.isCharityEvent"
            class="chip-secondary"
          >
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>
            </svg>
            Charity Event
          </span>
          <span
            v-if="!event.isPublic"
            class="chip-warning"
          >
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
            </svg>
            Private
          </span>
        </div>

        <!-- Info Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <div class="space-y-4">
            <!-- Date & Time -->
            <div class="flex items-start gap-3">
              <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <div>
                <div class="font-medium">{{ formatDate(event.eventDate) }}</div>
                <div class="text-sm text-gray-500">{{ formatTime(event.startTime) }} ({{ event.durationMinutes }} min)</div>
              </div>
            </div>

            <!-- Location -->
            <div v-if="event.venue || event.addressLine1 || event.city" class="flex items-start gap-3">
              <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
              </svg>
              <div>
                <!-- Full address details (only for public events or authorized users) -->
                <template v-if="canSeeFullAddress">
                  <!-- Venue name if at a venue -->
                  <div v-if="event.venue" class="font-medium text-primary-700">{{ event.venue.name }}</div>
                  <!-- Street address if available -->
                  <div v-if="event.addressLine1" class="font-medium">{{ event.addressLine1 }}</div>
                  <!-- City, State, Postal Code -->
                  <div class="text-sm text-gray-500">
                    {{ [event.city, event.state, event.postalCode].filter(Boolean).join(', ') }}
                  </div>
                  <!-- Venue hall/room/table details -->
                  <div v-if="event.venueHall || event.venueRoom || event.venueTable" class="text-sm text-gray-600 mt-1">
                    <span v-if="event.venueHall">{{ event.venueHall }}</span>
                    <span v-if="event.venueHall && (event.venueRoom || event.venueTable)"> &bull; </span>
                    <span v-if="event.venueRoom">{{ event.venueRoom }}</span>
                    <span v-if="event.venueRoom && event.venueTable"> &bull; </span>
                    <span v-if="event.venueTable">Table {{ event.venueTable }}</span>
                  </div>
                  <!-- Additional location details -->
                  <div v-if="event.locationDetails" class="text-sm text-gray-500 mt-1">{{ event.locationDetails }}</div>
                </template>
                <!-- Limited address for private events (city/state only) -->
                <template v-else>
                  <div class="font-medium text-gray-700">
                    {{ [event.city, event.state].filter(Boolean).join(', ') }}
                  </div>
                  <div class="text-sm text-gray-500 mt-1 italic">
                    Full address visible after joining
                  </div>
                </template>
              </div>
            </div>
          </div>

          <!-- Players -->
          <div class="flex items-start gap-3">
            <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
            </svg>
            <div>
              <div class="font-medium">{{ event.confirmedCount }} / {{ event.maxPlayers }} players</div>
              <div class="text-sm text-gray-500">
                {{ spotsLeft > 0 ? `${spotsLeft} spots left` : 'Game is full' }}
              </div>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div v-if="event.description" class="mb-6">
          <h3 class="font-semibold mb-2">About this game</h3>
          <p class="text-gray-600">{{ event.description }}</p>
        </div>

        <!-- Planned Games (from multi-game planning sessions) -->
        <div v-if="event.plannedGames && event.plannedGames.length > 0" class="mb-6">
          <h3 class="font-semibold mb-3 flex items-center gap-2">
            <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.66,11.2C17.43,10.9 17.15,10.64 16.89,10.38C16.22,9.78 15.46,9.35 14.82,8.72C13.33,7.26 13,4.85 13.95,3C13,3.23 12.17,3.75 11.46,4.32C8.87,6.4 7.85,10.07 9.07,13.22C9.11,13.32 9.15,13.42 9.15,13.55C9.15,13.77 9,13.97 8.8,14.05C8.57,14.15 8.33,14.09 8.14,13.93C8.08,13.88 8.04,13.83 8,13.76C6.87,12.33 6.69,10.28 7.45,8.64C5.78,10 4.87,12.3 5,14.47C5.06,14.97 5.12,15.47 5.29,15.97C5.43,16.57 5.7,17.17 6,17.7C7.08,19.43 8.95,20.67 10.96,20.92C13.1,21.19 15.39,20.8 17.03,19.32C18.86,17.66 19.5,15 18.56,12.72L18.43,12.46C18.22,12 17.66,11.2 17.66,11.2M14.5,17.5C14.22,17.74 13.76,18 13.4,18.1C12.28,18.5 11.16,17.94 10.5,17.28C11.69,17 12.4,16.12 12.61,15.23C12.78,14.43 12.46,13.77 12.33,13C12.21,12.26 12.23,11.63 12.5,10.94C12.69,11.32 12.89,11.7 13.13,12C13.9,13 15.11,13.44 15.37,14.8C15.41,14.94 15.43,15.08 15.43,15.23C15.46,16.05 15.1,16.95 14.5,17.5H14.5Z"/>
            </svg>
            Games for Tonight
            <span class="text-sm font-normal text-gray-500">({{ event.plannedGames.length }} games fired)</span>
          </h3>
          <p class="text-sm text-gray-500 mb-3">These games had 2+ interested players during planning!</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div
              v-for="(game, index) in event.plannedGames"
              :key="game.bggId || index"
              class="flex items-center gap-3 p-3 border border-green-200 rounded-lg bg-green-50"
            >
              <div class="w-12 h-12 rounded bg-white flex-shrink-0 overflow-hidden">
                <img
                  v-if="game.image"
                  :src="game.image"
                  :alt="game.name"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium text-gray-900">{{ game.name }}</div>
                <div class="flex flex-wrap gap-x-3 gap-y-1 text-sm">
                  <span class="text-green-600">{{ game.interestedCount }} interested</span>
                  <span v-if="game.minPlayers || game.maxPlayers" class="text-gray-500">
                    {{ game.minPlayers === game.maxPlayers ? `${game.minPlayers}p` : `${game.minPlayers}-${game.maxPlayers}p` }}
                  </span>
                  <span v-if="game.playingTime" class="text-gray-500">
                    {{ game.playingTime }}min
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Games -->
        <div v-if="event.games && event.games.length > 0" class="mb-6">
          <h3 class="font-semibold mb-3">Games</h3>
          <div class="space-y-3">
            <div
              v-for="game in event.games"
              :key="game.id"
              class="flex items-center gap-3 p-3 border border-gray-200 rounded-lg"
              :class="{ 'ring-2 ring-primary-500 bg-primary-50': game.isPrimary }"
            >
              <div class="w-12 h-12 rounded bg-gray-100 flex-shrink-0 overflow-hidden">
                <img
                  v-if="game.thumbnailUrl"
                  :src="game.thumbnailUrl"
                  :alt="game.gameName"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900">{{ game.gameName }}</span>
                  <span v-if="game.isPrimary" class="chip-primary text-xs">Primary</span>
                  <span v-else-if="game.isAlternative" class="chip text-xs bg-gray-100 text-gray-600">Alternative</span>
                </div>
                <div class="flex flex-wrap gap-2 mt-1 text-xs text-gray-500">
                  <span v-if="game.minPlayers || game.maxPlayers">
                    <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
                    </svg>
                    {{ game.minPlayers === game.maxPlayers ? `${game.minPlayers}` : `${game.minPlayers}-${game.maxPlayers}` }} players
                  </span>
                  <span v-if="game.playingTime">
                    <svg class="w-3 h-3 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
                    </svg>
                    {{ game.playingTime }} min
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Registration Actions (simple mode) -->
        <div v-if="auth.isAuthenticated.value && !isHost && !event.isMultiTable">
          <button
            v-if="!isRegistered && spotsLeft > 0"
            class="btn-primary"
            @click="handleRegister"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Join Game
          </button>
          <button
            v-else-if="isRegistered"
            class="btn border-2 border-red-500 text-red-500 hover:bg-red-50"
            @click="handleCancelRegistration"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C17.67,14 23,15.33 23,18V20H7V18C7,15.33 12.33,14 15,14M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12M5,9.59L7.12,7.46L8.54,8.88L6.41,11L8.54,13.12L7.12,14.54L5,12.41L2.88,14.54L1.46,13.12L3.59,11L1.46,8.88L2.88,7.46L5,9.59Z"/>
            </svg>
            Cancel Registration
          </button>
          <span v-else-if="spotsLeft <= 0" class="chip-error">
            Game is Full
          </span>
        </div>

        <div v-else-if="!auth.isAuthenticated.value && !event.isMultiTable">
          <button class="btn-outline" @click="goToLogin">
            Sign in to join
          </button>
        </div>
      </div>

      <!-- Multi-Table Session Schedule -->
      <div v-if="event.isMultiTable && event.tables && event.sessions" class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M19,3H18V1H16V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3Z"/>
            </svg>
            Game Sessions
            <span class="text-sm font-normal text-gray-500">({{ event.tables.length }} tables)</span>
          </h2>
        </div>
        <div class="p-4">
          <p v-if="!auth.isAuthenticated.value" class="text-gray-600 mb-4">
            <button class="text-primary-500 hover:underline" @click="goToLogin">Sign in</button>
            to register for game sessions.
          </p>
          <SessionScheduleGrid
            :tables="event.tables"
            :sessions="event.sessions"
            :is-host="isHost"
            :registering="registeringSession"
            @register="handleSessionRegister"
            @cancel="handleSessionCancel"
          />
        </div>
      </div>

      <!-- Registered Players -->
      <div v-if="event.registrations && event.registrations.length > 0" class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
            </svg>
            Players ({{ event.registrations.length }})
          </h2>
        </div>
        <div class="divide-y divide-gray-100">
          <div
            v-for="reg in event.registrations"
            :key="reg.id"
            class="flex items-start gap-3 p-4"
          >
            <UserAvatar
              :avatar-url="reg.user?.avatarUrl"
              :display-name="reg.user?.displayName"
              :is-founding-member="reg.user?.isFoundingMember"
              :is-admin="reg.user?.isAdmin"
              size="md"
              class="flex-shrink-0"
            />
            <div class="flex-1 min-w-0">
              <div class="font-medium">{{ reg.user?.displayName || 'Anonymous' }}</div>
              <div class="text-sm text-gray-500">{{ reg.status }}</div>
              <!-- Items this player is bringing -->
              <div v-if="canUseItems && getItemsForUser(reg.userId).length > 0" class="mt-2 flex flex-wrap gap-1">
                <span
                  v-for="item in getItemsForUser(reg.userId)"
                  :key="item.id"
                  class="inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-700"
                >
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,21H19V19H5M19,10H15V3H9V10H5L12,17L19,10Z"/>
                  </svg>
                  {{ item.itemName }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Items to Bring (Pro+ feature) -->
      <div v-if="canUseItems" class="card">
        <div class="p-4 border-b border-gray-100 flex items-center justify-between">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,21H19V19H5M19,10H15V3H9V10H5L12,17L19,10Z"/>
            </svg>
            Items to Bring
          </h2>
          <button
            v-if="canAddItems"
            class="btn-ghost text-primary-500"
            @click="addItemDialog = true"
          >
            <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Item
          </button>
        </div>

        <!-- What You're Bringing Section -->
        <div v-if="myItems.length > 0">
          <div class="px-4 py-2 bg-primary-50 border-b border-primary-100">
            <h3 class="text-sm font-medium text-primary-800 flex items-center gap-2">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
              What You're Bringing ({{ myItems.length }})
            </h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div
              v-for="item in myItems"
              :key="item.id"
              class="flex items-center gap-3 p-4 bg-primary-50/30"
            >
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,21H19V19H5M19,10H15V3H9V10H5L12,17L19,10Z"/>
              </svg>
              <div class="flex-1">
                <div class="font-medium text-primary-900">{{ item.itemName }}</div>
                <span class="chip bg-primary-100 text-primary-700 text-xs">{{ item.itemCategory }}</span>
              </div>
              <div class="flex items-center gap-1">
                <button
                  class="btn-sm btn-ghost text-primary-600 hover:bg-primary-100"
                  @click="openEditItem(item)"
                  title="Edit item"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                  </svg>
                </button>
                <button
                  class="btn-sm btn-ghost text-red-500 hover:bg-red-50"
                  @click="handleRemoveMyItem(item.id)"
                  title="Remove from your list"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Still Needed Section -->
        <div v-if="neededItems.length > 0">
          <div class="px-4 py-2 bg-amber-50 border-b border-amber-100">
            <h3 class="text-sm font-medium text-amber-800 flex items-center gap-2">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,6A4,4 0 0,0 8,10H10A2,2 0 0,1 12,8A2,2 0 0,1 14,10C14,12 11,11.75 11,15H13C13,12.75 16,12.5 16,10A4,4 0 0,0 12,6M11,16V18H13V16H11Z"/>
              </svg>
              Still Needed ({{ neededItems.length }})
            </h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div
              v-for="item in neededItems"
              :key="item.id"
              class="flex items-center gap-3 p-4"
            >
              <svg class="w-5 h-5 text-amber-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
              </svg>
              <div class="flex-1">
                <div class="font-medium">{{ item.itemName }}</div>
                <span class="chip bg-gray-100 text-gray-600 text-xs">{{ item.itemCategory }}</span>
              </div>
              <div class="flex items-center gap-2">
                <button
                  v-if="auth.isAuthenticated.value"
                  class="btn-sm btn-primary"
                  @click="handleClaimItem(item.id)"
                >
                  I'll Bring It
                </button>
                <button
                  v-if="isHost"
                  class="btn-sm btn-ghost text-red-500"
                  @click="handleDeleteItem(item.id)"
                  title="Delete item"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Being Brought Section -->
        <div v-if="claimedItems.length > 0">
          <div class="px-4 py-2 bg-green-50 border-b border-green-100" :class="{ 'border-t': neededItems.length > 0 }">
            <h3 class="text-sm font-medium text-green-800 flex items-center gap-2">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              Being Brought ({{ claimedItems.length }})
            </h3>
          </div>
          <div class="divide-y divide-gray-100">
            <div
              v-for="item in claimedItems"
              :key="item.id"
              class="flex items-center gap-3 p-4"
            >
              <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              <div class="flex-1">
                <div class="font-medium">{{ item.itemName }}</div>
                <div class="flex items-center gap-2 text-sm text-gray-500">
                  <span class="chip bg-gray-100 text-gray-600 text-xs">{{ item.itemCategory }}</span>
                  <span class="text-green-600">{{ item.claimedByName }} is bringing this</span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button
                  v-if="item.claimedByUserId === auth.user.value?.id"
                  class="btn-sm btn-ghost text-red-500"
                  @click="handleUnclaimItem(item.id)"
                >
                  Cancel
                </button>
                <button
                  v-if="isHost"
                  class="btn-sm btn-ghost text-red-500"
                  @click="handleDeleteItem(item.id)"
                  title="Delete item"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty state -->
        <p v-if="!event.items || event.items.length === 0" class="text-gray-500 text-center py-8">
          No items added yet. {{ canAddItems ? 'Add items the group should bring!' : '' }}
        </p>
      </div>
    </template>

    <!-- Event Chat (visible to host and registered users if host has Basic+) -->
    <div v-if="canUseChat && (isHost || isRegistered)" class="card mb-6">
      <div class="p-4 border-b border-gray-100">
        <button
          class="w-full flex items-center justify-between text-left"
          @click="showChat = !showChat"
        >
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H6L4,18V4H20"/>
            </svg>
            Event Chat
          </h2>
          <svg
            class="w-5 h-5 text-gray-400 transition-transform"
            :class="{ 'rotate-180': showChat }"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z"/>
          </svg>
        </button>
      </div>
      <div v-if="showChat" class="h-96">
        <ChatPanel
          context-type="event"
          :context-id="eventId"
        />
      </div>
    </div>

    <!-- Add Item Dialog -->
    <div v-if="addItemDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="addItemDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Add Item</h3>
        <div class="space-y-4">
          <div>
            <label class="label">Item name</label>
            <input
              v-model="newItemName"
              type="text"
              class="input"
              placeholder="What should someone bring?"
            />
          </div>
          <div>
            <label class="label">Category</label>
            <select v-model="newItemCategory" class="input">
              <option value="games">Games</option>
              <option value="food">Food</option>
              <option value="drinks">Drinks</option>
              <option value="supplies">Supplies</option>
              <option value="other">Other</option>
            </select>
          </div>
          <div class="flex items-center gap-3 p-3 bg-green-50 rounded-lg border border-green-200">
            <input
              id="bringing-item"
              v-model="newItemBringing"
              type="checkbox"
              class="w-5 h-5 rounded border-green-300 text-green-600 focus:ring-green-500"
            />
            <label for="bringing-item" class="flex-1 cursor-pointer">
              <span class="font-medium text-green-800">I'm bringing this</span>
              <span class="block text-sm text-green-600">Check this if you'll bring this item yourself</span>
            </label>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="addItemDialog = false">Cancel</button>
          <button class="btn-primary" @click="handleAddItem">
            {{ newItemBringing ? "Add & Claim" : "Add Item" }}
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Item Dialog -->
    <div v-if="editItemDialog && editingItem" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="editItemDialog = false; editingItem = null"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Edit Item</h3>
        <div class="space-y-4">
          <div>
            <label class="label">Item name</label>
            <input
              v-model="editingItem.itemName"
              type="text"
              class="input"
              placeholder="What are you bringing?"
            />
          </div>
          <div>
            <label class="label">Category</label>
            <select v-model="editingItem.itemCategory" class="input">
              <option value="games">Games</option>
              <option value="food">Food</option>
              <option value="drinks">Drinks</option>
              <option value="supplies">Supplies</option>
              <option value="other">Other</option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="editItemDialog = false; editingItem = null">Cancel</button>
          <button class="btn-primary" @click="handleUpdateItem">
            Save Changes
          </button>
        </div>
      </div>
    </div>

    <!-- Invite Group Members Dialog -->
    <div v-if="inviteMembersDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="inviteMembersDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10 max-h-[80vh] flex flex-col">
        <h3 class="text-lg font-semibold mb-2 flex items-center gap-2">
          <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
          </svg>
          Invite Group Members
        </h3>
        <p class="text-sm text-gray-500 mb-4">
          Select group members to invite to this game. They will be added directly as confirmed players.
        </p>

        <!-- Loading state -->
        <div v-if="loadingGroupMembers" class="flex items-center justify-center py-8">
          <D20Spinner size="md" />
        </div>

        <!-- Empty state -->
        <div v-else-if="groupMembers.length === 0" class="text-center py-8 text-gray-500">
          <svg class="w-12 h-12 mx-auto text-gray-300 mb-3" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
          </svg>
          <p>All group members are already registered for this event.</p>
        </div>

        <!-- Members list -->
        <div v-else class="flex-1 overflow-y-auto">
          <!-- Select all toggle -->
          <div class="flex items-center justify-between px-3 py-2 bg-gray-50 rounded-lg mb-3">
            <span class="text-sm font-medium text-gray-700">
              {{ selectedMembersCount }} of {{ groupMembers.length }} selected
            </span>
            <button
              class="text-sm text-purple-600 hover:text-purple-800 font-medium"
              @click="toggleAllMembers(selectedMembersCount !== groupMembers.length)"
            >
              {{ selectedMembersCount === groupMembers.length ? 'Deselect All' : 'Select All' }}
            </button>
          </div>

          <div class="space-y-2">
            <label
              v-for="member in groupMembers"
              :key="member.id"
              class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors"
              :class="member.selected ? 'border-purple-300 bg-purple-50' : 'border-gray-200 hover:border-purple-200 hover:bg-purple-50/50'"
            >
              <input
                v-model="member.selected"
                type="checkbox"
                class="w-5 h-5 rounded border-purple-300 text-purple-600 focus:ring-purple-500"
              />
              <UserAvatar
                :avatar-url="member.avatarUrl"
                :display-name="member.displayName"
                size="sm"
              />
              <span class="font-medium">{{ member.displayName }}</span>
            </label>
          </div>
        </div>

        <!-- Spots left warning -->
        <div v-if="spotsLeft < groupMembers.length && groupMembers.length > 0" class="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
          <p class="text-sm text-amber-800">
            <svg class="w-4 h-4 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
            </svg>
            Only {{ spotsLeft }} spot{{ spotsLeft === 1 ? '' : 's' }} remaining. Only the first {{ spotsLeft }} selected member{{ spotsLeft === 1 ? '' : 's' }} will be added.
          </p>
        </div>

        <div class="flex justify-end gap-3 mt-6 pt-4 border-t">
          <button class="btn-ghost" @click="inviteMembersDialog = false">Cancel</button>
          <button
            class="btn-primary bg-purple-600 hover:bg-purple-700"
            :disabled="selectedMembersCount === 0 || invitingMembers"
            @click="handleInviteMembers"
          >
            <svg v-if="invitingMembers" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ invitingMembers ? 'Inviting...' : `Invite ${selectedMembersCount} Member${selectedMembersCount === 1 ? '' : 's'}` }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div
      v-if="toast.visible"
      class="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 px-4 py-3 rounded-lg shadow-lg"
      :class="toast.type === 'success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- Share Modal -->
    <ShareModal
      v-if="event"
      :event-id="event.id"
      :event-title="event.title"
      :visible="showShareModal"
      @close="showShareModal = false"
    />
  </div>
</template>
