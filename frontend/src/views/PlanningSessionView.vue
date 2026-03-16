<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getEffectiveTier } from '@/types/user'
import { hasFeature } from '@/config/subscriptionLimits'
import {
  getPlanningSession,
  respondToPlanningSession,
  suggestGame,
  voteForGame,
  unvoteGame,
  removeSuggestion,
  finalizePlanningSession,
  cancelPlanningSession,
  addPlanningItem,
  claimPlanningItem,
  unclaimPlanningItem,
  removePlanningItem,
  addPlanningInvitees,
  scheduleGameSessions,
  setHostSessionPreferences,
} from '@/services/planningApi'
import { getGroupMembers } from '@/services/groupsApi'
import { supabase } from '@/services/supabase'
import type { GroupMember } from '@/types/groups'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import type { PlanningSession, GameSuggestion, PlanningItem, ItemCategory } from '@/types/planning'
import type { BggSearchResult } from '@/types/bgg'
import type { ScheduleEntry, HostPreference } from '@/types/sessions'
import DateAvailabilityMatrix from '@/components/planning/DateAvailabilityMatrix.vue'
import DateAvailabilitySummary from '@/components/planning/DateAvailabilitySummary.vue'
import GameSuggestionCard from '@/components/planning/GameSuggestionCard.vue'
import SessionScheduler from '@/components/planning/SessionScheduler.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const session = ref<PlanningSession | null>(null)
const errorMessage = ref('')
const showChat = ref(false)

// Responsive detection for availability view
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768 // md breakpoint
}
const successMessage = ref('')

// Response form state
const responding = ref(false)
const responseForm = reactive({
  cannotAttendAny: false,
  dateAvailability: {} as Record<string, boolean>,
})

// Game suggestion state
const gameSearchQuery = ref('')
const gameSearchResults = ref<BggSearchResult[]>([])
const searchingGames = ref(false)
const addingGame = ref(false)
const showGameSearch = ref(false)

// Finalization state
const finalizing = ref(false)
const selectedDateId = ref<string | null>(null)
const selectedGameId = ref<string | null>(null)

// Multi-table scheduling state
const savingSchedule = ref(false)
const scheduleEntries = ref<ScheduleEntry[]>([])
const hostPreferences = ref<HostPreference[]>([])

// Computed: is this a multi-table session?
const isMultiTable = computed(() => {
  return (session.value?.tableCount ?? 0) >= 2
})

// Items to bring state
const showAddItemForm = ref(false)
const addingItem = ref(false)
const claimingItemId = ref<string | null>(null)
const newItem = reactive({
  name: '',
  category: 'food' as ItemCategory,
  quantity: 1,
})

// Invite more members state
const showInviteModal = ref(false)
const loadingMembers = ref(false)
const groupMembers = ref<GroupMember[]>([])
const selectedMemberIds = ref<string[]>([])
const invitingMembers = ref(false)
const sendInviteEmails = ref(true)

const sessionId = computed(() => route.params.id as string)

const isCreator = computed(() => {
  if (!session.value || !auth.user.value) return false
  return session.value.createdByUserId === auth.user.value.id
})

// Check if user can invite members (creator OR group admin)
const canInviteMembers = ref(false)

async function checkCanInviteMembers() {
  if (!session.value?.groupId || !auth.user.value) {
    canInviteMembers.value = isCreator.value
    return
  }

  // Creator can always invite
  if (isCreator.value) {
    canInviteMembers.value = true
    return
  }

  // Check if user is group admin
  const { data: membership } = await supabase
    .from('group_memberships')
    .select('role')
    .eq('group_id', session.value.groupId)
    .eq('user_id', auth.user.value.id)
    .single()

  canInviteMembers.value = membership?.role === 'owner' || membership?.role === 'admin'
}

// Check if user can use items feature (Pro+ only)
const canUseItems = computed(() => {
  if (!auth.user.value) return false
  const tier = getEffectiveTier(auth.user.value)
  return hasFeature(tier, 'items')
})

const currentUserInvitee = computed(() => {
  if (!session.value?.invitees || !auth.user.value) return null
  return session.value.invitees.find(i => i.userId === auth.user.value!.id)
})

const hasResponded = computed(() => currentUserInvitee.value?.hasResponded ?? false)

// Check if user has accepted the invitation (or is the creator who is auto-accepted)
const hasAccepted = computed(() => {
  if (isCreator.value) return true
  return !!currentUserInvitee.value?.acceptedAt
})

const isOpen = computed(() => session.value?.status === 'open')

const deadlinePassed = computed(() => {
  if (!session.value) return false
  return new Date(session.value.responseDeadline) < new Date()
})

const respondedCount = computed(() => {
  return session.value?.invitees?.filter(i => i.hasResponded).length ?? 0
})

const totalInvitees = computed(() => session.value?.invitees?.length ?? 0)

// Slot-related computed properties
const slotsFilled = computed(() => {
  return session.value?.invitees?.filter(i => i.hasSlot).length ?? 0
})

const currentUserHasSlot = computed(() => {
  return currentUserInvitee.value?.hasSlot ?? false
})

const slotsAvailable = computed(() => {
  if (!session.value?.maxParticipants) return true
  return slotsFilled.value < session.value.maxParticipants
})

const bestDate = computed(() => {
  if (!session.value?.dates) return null
  const sorted = [...session.value.dates].sort((a, b) => (b.availableCount ?? 0) - (a.availableCount ?? 0))
  return sorted[0]
})

const topGame = computed(() => {
  if (!session.value?.gameSuggestions) return null
  const sorted = [...session.value.gameSuggestions].sort((a, b) => b.voteCount - a.voteCount)
  return sorted[0]
})

// Use summary view for mobile or when there are more than 10 invitees
const useSummaryView = computed(() => {
  if (isMobile.value) return true
  return (session.value?.invitees?.length ?? 0) > 10
})

onMounted(async () => {
  // Set up responsive detection
  checkMobile()
  window.addEventListener('resize', checkMobile)

  await loadSession()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

async function loadSession(preserveFormState = false) {
  loading.value = true
  errorMessage.value = ''

  // Save current form state if we need to preserve it
  const savedDateAvailability = preserveFormState ? { ...responseForm.dateAvailability } : null
  const savedCannotAttendAny = preserveFormState ? responseForm.cannotAttendAny : null

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    session.value = await getPlanningSession(token, sessionId.value)

    // Check if user can invite members (creator or group admin)
    checkCanInviteMembers()

    // Restore or initialize date availability
    if (session.value.dates) {
      if (preserveFormState && savedDateAvailability) {
        // Restore saved state
        responseForm.dateAvailability = savedDateAvailability
        responseForm.cannotAttendAny = savedCannotAttendAny ?? false
      } else {
        // Initialize from existing votes only - don't default to false
        // This allows the matrix to show "No response" for unvoted dates
        responseForm.dateAvailability = {}
        for (const date of session.value.dates) {
          const userVote = date.votes?.find(v => v.userId === auth.user.value?.id)
          if (userVote !== undefined) {
            responseForm.dateAvailability[date.id] = userVote.isAvailable
          }
        }
      }
    }

    // Pre-select best date/game for finalization
    if (bestDate.value) {
      selectedDateId.value = bestDate.value.id
    }
    if (topGame.value) {
      selectedGameId.value = topGame.value.id
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load session'
  } finally {
    loading.value = false
  }
}

async function submitResponse() {
  if (!session.value) return

  responding.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await respondToPlanningSession(token, session.value.id, {
      cannotAttendAny: responseForm.cannotAttendAny,
      dateAvailability: responseForm.cannotAttendAny
        ? []
        : Object.entries(responseForm.dateAvailability).map(([dateId, isAvailable]) => ({
            dateId,
            isAvailable,
          })),
    })

    successMessage.value = 'Your response has been recorded!'
    await loadSession()
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to submit response'
  } finally {
    responding.value = false
  }
}

let searchTimeout: ReturnType<typeof setTimeout> | null = null

function handleGameSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)

  if (gameSearchQuery.value.trim().length < 2) {
    gameSearchResults.value = []
    return
  }

  searchTimeout = setTimeout(async () => {
    searchingGames.value = true
    try {
      gameSearchResults.value = await searchBggGames(gameSearchQuery.value)
    } catch (err) {
      console.error('Search error:', err)
    } finally {
      searchingGames.value = false
    }
  }, 300)
}

async function selectGameToSuggest(result: BggSearchResult) {
  if (!session.value) return

  addingGame.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const game = await getBggGame(result.bggId)

    await suggestGame(token, session.value.id, {
      gameName: game.name,
      bggId: game.bggId,
      thumbnailUrl: game.thumbnailUrl ?? undefined,
      minPlayers: game.minPlayers ?? undefined,
      maxPlayers: game.maxPlayers ?? undefined,
      playingTime: game.playingTime ?? undefined,
    })

    gameSearchQuery.value = ''
    gameSearchResults.value = []
    showGameSearch.value = false
    // Preserve form state when reloading after game suggestion
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to add game suggestion'
  } finally {
    addingGame.value = false
  }
}

async function handleVoteGame(suggestion: GameSuggestion) {
  if (!session.value) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (suggestion.hasVoted) {
      await unvoteGame(token, session.value.id, suggestion.id)
    } else {
      await voteForGame(token, session.value.id, suggestion.id)
    }

    // Preserve form state when reloading after game vote
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to vote'
  }
}

function canRemoveSuggestion(suggestion: GameSuggestion): boolean {
  if (!session.value || !auth.user.value) return false
  if (session.value.status !== 'open') return false
  // Allow suggester, session creator, or site admin to remove
  return suggestion.suggestedByUserId === auth.user.value.id || isCreator.value || auth.isAdmin.value
}

async function handleRemoveSuggestion(suggestion: GameSuggestion) {
  if (!session.value) return
  if (!confirm(`Remove "${suggestion.gameName}" from suggestions?`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await removeSuggestion(token, session.value.id, suggestion.id)
    // Preserve form state when reloading after removing suggestion
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to remove suggestion'
  }
}

async function handleSaveSchedule(schedule: ScheduleEntry[], preferences: HostPreference[]) {
  if (!session.value) return

  savingSchedule.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    // Save both schedule and preferences
    await scheduleGameSessions(token, session.value.id, schedule)
    await setHostSessionPreferences(token, session.value.id, preferences)

    // Update local state
    scheduleEntries.value = schedule
    hostPreferences.value = preferences

    successMessage.value = 'Schedule saved successfully'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save schedule'
  } finally {
    savingSchedule.value = false
  }
}

async function handleFinalize() {
  if (!session.value || !selectedDateId.value) return

  // For multi-table sessions, ensure schedule is saved
  if (isMultiTable.value && scheduleEntries.value.length === 0) {
    errorMessage.value = 'Please schedule games to tables before finalizing'
    return
  }

  finalizing.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const result = await finalizePlanningSession(
      token,
      session.value.id,
      selectedDateId.value,
      selectedGameId.value || undefined
    )

    router.push(`/games/${result.eventId}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to finalize session'
  } finally {
    finalizing.value = false
  }
}

async function handleCancel() {
  if (!session.value) return
  if (!confirm('Are you sure you want to cancel this planning session?')) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await cancelPlanningSession(token, session.value.id)
    await loadSession()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to cancel session'
  }
}

// ============ Items to Bring ============

async function handleAddItem() {
  if (!session.value || !newItem.name.trim()) return

  addingItem.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await addPlanningItem(token, session.value.id, {
      itemName: newItem.name.trim(),
      itemCategory: newItem.category,
      quantityNeeded: newItem.quantity,
    })

    // Reset form
    newItem.name = ''
    newItem.category = 'food'
    newItem.quantity = 1
    showAddItemForm.value = false

    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to add item'
  } finally {
    addingItem.value = false
  }
}

async function handleClaimItem(item: PlanningItem) {
  if (!session.value) return

  claimingItemId.value = item.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await claimPlanningItem(token, session.value.id, item.id)
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to claim item'
  } finally {
    claimingItemId.value = null
  }
}

async function handleUnclaimItem(item: PlanningItem) {
  if (!session.value) return

  claimingItemId.value = item.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await unclaimPlanningItem(token, session.value.id, item.id)
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to unclaim item'
  } finally {
    claimingItemId.value = null
  }
}

async function handleRemoveItem(item: PlanningItem) {
  if (!session.value) return
  if (!confirm(`Remove "${item.itemName}" from the list?`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await removePlanningItem(token, session.value.id, item.id)
    await loadSession(true)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to remove item'
  }
}

function getCategoryIcon(category: ItemCategory): string {
  switch (category) {
    case 'food': return 'M12,6A2,2 0 0,0 14,4A2,2 0 0,0 12,2A2,2 0 0,0 10,4A2,2 0 0,0 12,6M15.5,11.5C15.5,12.34 14.83,13 14,13V22H12V16H10V22H8V13C7.17,13 6.5,12.34 6.5,11.5V7C6.5,5.62 7.62,4.5 9,4.5H15C16.38,4.5 17.5,5.62 17.5,7V11.5'
    case 'drinks': return 'M3,14C3,15.31 3.84,16.41 5,16.83V20H2V22H22V20H19V16.83C20.16,16.41 21,15.31 21,14V8H3V14M7,10H17V14C17,14.55 16.55,15 16,15H8C7.45,15 7,14.55 7,14V10M8,2H10V4H8V2M14,2H16V4H14V2M11,2H13V5H11V2Z'
    case 'supplies': return 'M20,8H17V5C17,3.89 16.11,3 15,3H9C7.89,3 7,3.89 7,5V8H4C2.89,8 2,8.89 2,10V20C2,21.11 2.89,22 4,22H20C21.11,22 22,21.11 22,20V10C22,8.89 21.11,8 20,8M9,5H15V8H9V5M20,20H4V10H20V20Z'
    default: return 'M20,6H16V4C16,2.89 15.11,2 14,2H10C8.89,2 8,2.89 8,4V6H4C2.89,6 2,6.89 2,8V19C2,20.11 2.89,21 4,21H20C21.11,21 22,20.11 22,19V8C22,6.89 21.11,6 20,6M10,4H14V6H10V4Z'
  }
}

// ============ Invite More Members ============

async function openInviteModal() {
  if (!session.value?.groupId) return

  showInviteModal.value = true
  loadingMembers.value = true
  selectedMemberIds.value = []

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const members = await getGroupMembers(token, session.value.groupId)

    // Filter out already invited members
    const invitedUserIds = new Set(session.value.invitees?.map(i => i.userId) || [])
    groupMembers.value = members.filter(m => !invitedUserIds.has(m.userId))
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load members'
    showInviteModal.value = false
  } finally {
    loadingMembers.value = false
  }
}

function toggleMemberSelection(userId: string) {
  const index = selectedMemberIds.value.indexOf(userId)
  if (index === -1) {
    selectedMemberIds.value.push(userId)
  } else {
    selectedMemberIds.value.splice(index, 1)
  }
}

async function handleInviteMembers() {
  if (!session.value || selectedMemberIds.value.length === 0) return

  invitingMembers.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) throw new Error('Not authenticated')

    const result = await addPlanningInvitees(
      token,
      session.value.id,
      selectedMemberIds.value,
      sendInviteEmails.value
    )

    successMessage.value = result.message
    showInviteModal.value = false
    selectedMemberIds.value = []

    // Reload session to show new invitees
    await loadSession(true)

    // Clear success message after a few seconds
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to invite members'
  } finally {
    invitingMembers.value = false
  }
}

function getCategoryColor(category: ItemCategory): string {
  switch (category) {
    case 'food': return 'text-orange-500 bg-orange-100'
    case 'drinks': return 'text-blue-500 bg-blue-100'
    case 'supplies': return 'text-purple-500 bg-purple-100'
    default: return 'text-gray-500 bg-gray-100'
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

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function formatTime(timeStr: string): string {
  // Convert "HH:MM" or "HH:MM:SS" to 12-hour format
  const parts = timeStr.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const period = hours >= 12 ? 'PM' : 'AM'
  const hour12 = hours % 12 || 12
  return `${hour12}:${minutes.toString().padStart(2, '0')} ${period}`
}

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'open':
      return 'chip-success'
    case 'finalized':
      return 'chip bg-blue-100 text-blue-700'
    case 'cancelled':
      return 'chip-error'
    default:
      return 'chip bg-gray-100 text-gray-700'
  }
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Loading -->
    <div v-if="loading" class="card p-8 text-center">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading planning session...</p>
    </div>

    <!-- Error - Not Invited or Other Error -->
    <div v-else-if="errorMessage && !session" class="card p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-red-400 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <h2 class="text-xl font-semibold text-gray-900 mb-2">
        {{ errorMessage.includes('invited') ? 'Not Invited' : 'Unable to Load Session' }}
      </h2>
      <p class="text-gray-500 mb-6">
        {{ errorMessage }}
      </p>
      <div class="flex justify-center gap-3">
        <button class="btn-outline" @click="router.push('/dashboard')">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z"/>
          </svg>
          Dashboard
        </button>
        <button class="btn-primary" @click="loadSession()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
          </svg>
          Try Again
        </button>
      </div>
    </div>

    <!-- Not Accepted - User must accept invitation first -->
    <div v-else-if="session && !hasAccepted" class="card p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-orange-400 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
      </svg>
      <h2 class="text-xl font-semibold text-gray-900 mb-2">Invitation Not Accepted</h2>
      <p class="text-gray-500 mb-6">
        You need to accept this planning invitation before you can view the session details and respond with your availability.
      </p>
      <button class="btn-primary" @click="router.push('/dashboard')">
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
        </svg>
        Go to Dashboard
      </button>
    </div>

    <!-- Session Content (only if accepted) -->
    <template v-else-if="session && hasAccepted">
      <!-- Back link -->
      <div class="mb-6">
        <button
          v-if="session.group"
          class="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1"
          @click="router.push(`/groups/${session.group.slug}`)"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
          Back to {{ session.group.name }}
        </button>
      </div>

      <!-- Messages -->
      <div v-if="successMessage" class="alert-success mb-6">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="alert-error mb-6">
        {{ errorMessage }}
      </div>

      <!-- Header -->
      <div class="card mb-6">
        <div class="p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <div class="flex items-center gap-3 mb-2">
                <h1 class="text-2xl font-bold">{{ session.title }}</h1>
                <span :class="getStatusBadgeClass(session.status)">{{ session.status }}</span>
              </div>
              <p v-if="session.description" class="text-gray-600 mb-3">{{ session.description }}</p>
              <div class="flex items-center gap-4 text-sm text-gray-500">
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                  </svg>
                  Created by {{ session.createdBy?.displayName || session.createdBy?.username || 'Unknown' }}
                </span>
                <span class="flex items-center gap-1">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
                  </svg>
                  Deadline: {{ formatDateTime(session.responseDeadline) }}
                  <span v-if="deadlinePassed" class="text-red-500 font-medium">(Passed)</span>
                </span>
              </div>
            </div>
            <div v-if="isCreator && isOpen" class="flex gap-2">
              <button class="btn-ghost text-red-500" @click="handleCancel">
                Cancel
              </button>
            </div>
          </div>

          <!-- Response Progress -->
          <div class="mt-6 pt-6 border-t border-gray-100">
            <div class="flex items-center justify-between text-sm mb-2">
              <span class="font-medium">Responses</span>
              <span class="text-gray-500">{{ respondedCount }} of {{ totalInvitees }}</span>
            </div>
            <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full bg-primary-500 rounded-full transition-all"
                :style="{ width: `${totalInvitees > 0 ? (respondedCount / totalInvitees) * 100 : 0}%` }"
              />
            </div>
          </div>

          <!-- Participant Slots (when limited) -->
          <div v-if="session.maxParticipants" class="mt-4 pt-4 border-t border-gray-100">
            <div class="flex items-center justify-between text-sm mb-2">
              <span class="font-medium">Participation Slots</span>
              <span class="text-gray-500">{{ slotsFilled }} of {{ session.maxParticipants }}</span>
            </div>
            <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :class="slotsFilled >= session.maxParticipants ? 'bg-red-500' : 'bg-green-500'"
                :style="{ width: `${(slotsFilled / session.maxParticipants) * 100}%` }"
              />
            </div>
            <p v-if="currentUserHasSlot" class="text-sm text-green-600 mt-2 flex items-center gap-1">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              You have a participation slot
            </p>
            <p v-else-if="!slotsAvailable" class="text-sm text-red-500 mt-2">
              All participation slots are filled. You can still respond but won't be able to vote on games.
            </p>
            <p v-else class="text-sm text-gray-500 mt-2">
              Respond to secure your participation slot (first-come-first-served)
            </p>
          </div>
        </div>
      </div>

      <!-- Finalized State -->
      <div v-if="session.status === 'finalized'" class="card mb-6 bg-blue-50 border-blue-200">
        <div class="p-6">
          <h2 class="font-semibold text-blue-800 mb-2">Session Finalized</h2>
          <p class="text-blue-700">
            The game night has been scheduled for {{ session.finalizedDate ? formatDate(session.finalizedDate) : 'TBD' }}.
          </p>
          <button
            v-if="session.createdEventId"
            class="btn-primary mt-4"
            @click="router.push(`/games/${session.createdEventId}`)"
          >
            View Event
          </button>
        </div>
      </div>

      <!-- Cancelled State -->
      <div v-else-if="session.status === 'cancelled'" class="card mb-6 bg-red-50 border-red-200">
        <div class="p-6">
          <h2 class="font-semibold text-red-800">Session Cancelled</h2>
          <p class="text-red-700">This planning session has been cancelled.</p>
        </div>
      </div>

      <!-- Active Session -->
      <template v-else>
        <!-- Your Preferences (for all participants) -->
        <div v-if="currentUserInvitee || isCreator" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Your Preferences</h2>
            <p class="text-sm text-gray-500 mt-1">Select your available dates and suggest/vote on games</p>
          </div>
          <div class="p-6">
            <div v-if="hasResponded" class="text-green-600 flex items-center gap-2 mb-4">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              You have already responded. You can update your preferences below.
            </div>

            <!-- Date Availability Section -->
            <div class="mb-6">
              <h3 class="font-medium text-gray-700 mb-3 flex items-center gap-2">
                <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
                </svg>
                Date Availability
              </h3>

              <!-- Cannot Attend Checkbox -->
              <label class="flex items-center gap-3 mb-4 p-4 rounded-lg border border-gray-200 cursor-pointer hover:bg-gray-50">
                <input
                  v-model="responseForm.cannotAttendAny"
                  type="checkbox"
                  class="w-5 h-5 text-red-500 rounded border-gray-300"
                />
                <div>
                  <span class="font-medium">I can't make any of these dates</span>
                  <p class="text-sm text-gray-500">Check this if none of the proposed dates work for you</p>
                </div>
              </label>

              <!-- Date Selection -->
              <div v-if="!responseForm.cannotAttendAny" class="space-y-3">
                <p class="text-sm text-gray-600 mb-2">Select the dates you're available:</p>
                <label
                  v-for="date in session.dates"
                  :key="date.id"
                  class="flex items-center gap-3 p-4 rounded-lg border cursor-pointer hover:bg-gray-50 transition-colors"
                  :class="responseForm.dateAvailability[date.id] ? 'border-green-500 bg-green-50' : 'border-gray-200'"
                >
                  <input
                    v-model="responseForm.dateAvailability[date.id]"
                    type="checkbox"
                    class="w-5 h-5 text-green-500 rounded border-gray-300"
                  />
                  <div class="flex-1">
                    <span class="font-medium">{{ formatDate(date.proposedDate) }}</span>
                    <span v-if="date.startTime" class="text-gray-500 ml-2">at {{ formatTime(date.startTime) }}</span>
                  </div>
                  <span class="text-sm text-gray-500">{{ date.availableCount ?? 0 }} available</span>
                </label>
              </div>
            </div>

            <!-- Game Suggestions Section (inline) -->
            <div class="mb-6 pt-6 border-t border-gray-200">
              <div class="flex items-center justify-between mb-3">
                <h3 class="font-medium text-gray-700 flex items-center gap-2">
                  <svg class="w-5 h-5 text-secondary-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                  Game Suggestions
                  <span class="text-sm font-normal text-gray-500">(vote for games you want to play)</span>
                </h3>
                <!-- Game count indicator -->
                <span v-if="session.maxGames" class="text-sm text-gray-500">
                  {{ session.gameSuggestions?.length ?? 0 }} / {{ session.maxGames }} games
                </span>
              </div>

              <!-- Game Search -->
              <div v-if="!showGameSearch" class="mb-4">
                <button
                  class="btn-outline text-sm"
                  :disabled="!!(session.maxParticipants && !currentUserHasSlot) || !!(session.maxGames && (session.gameSuggestions?.length ?? 0) >= session.maxGames)"
                  @click="showGameSearch = true"
                >
                  <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
                  </svg>
                  Suggest a Game
                </button>
                <p v-if="session.maxParticipants && !currentUserHasSlot" class="text-sm text-red-500 mt-2">
                  You need a participation slot to suggest games
                </p>
                <p v-else-if="session.maxGames && (session.gameSuggestions?.length ?? 0) >= session.maxGames" class="text-sm text-orange-600 mt-2">
                  Game suggestion limit reached ({{ session.maxGames }} max)
                </p>
              </div>
              <div v-else class="mb-4">
                <div class="relative">
                  <input
                    v-model="gameSearchQuery"
                    type="text"
                    class="input pr-10"
                    placeholder="Search BoardGameGeek..."
                    @input="handleGameSearch"
                  />
                  <button
                    class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
                    @click="showGameSearch = false; gameSearchQuery = ''; gameSearchResults = []"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                  </button>
                </div>
                <!-- Powered by BGG -->
                <div class="flex items-center justify-end mt-1">
                  <a href="https://boardgamegeek.com" target="_blank" rel="noopener noreferrer" class="hover:opacity-80 transition-opacity">
                    <img src="/powered-by-bgg.svg" alt="Powered by BoardGameGeek" class="h-5" />
                  </a>
                </div>
                <div v-if="searchingGames" class="mt-2 text-gray-500 text-sm">Searching...</div>
                <div v-else-if="gameSearchResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
                  <button
                    v-for="result in gameSearchResults"
                    :key="result.bggId"
                    class="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left border-b border-gray-100 last:border-0"
                    :disabled="addingGame"
                    @click="selectGameToSuggest(result)"
                  >
                    <!-- Game thumbnail -->
                    <img
                      v-if="result.thumbnailUrl"
                      :src="result.thumbnailUrl"
                      :alt="result.name"
                      class="w-10 h-10 object-cover rounded flex-shrink-0 bg-gray-100"
                    />
                    <div v-else class="w-10 h-10 flex items-center justify-center bg-gray-100 rounded flex-shrink-0">
                      <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                      </svg>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="font-medium truncate">{{ result.name }}</div>
                      <div v-if="result.yearPublished" class="text-sm text-gray-500">{{ result.yearPublished }}</div>
                    </div>
                  </button>
                </div>
              </div>

              <!-- Multi-vote explanation -->
              <p class="text-sm text-gray-500 mb-3">
                Vote for all games you'd like to play. Games with 2+ votes will be included in the event!
              </p>

              <!-- Suggested Games List (compact) -->
              <div v-if="session.gameSuggestions && session.gameSuggestions.length > 0" class="space-y-2">
                <GameSuggestionCard
                  v-for="suggestion in [...session.gameSuggestions].sort((a, b) => b.voteCount - a.voteCount)"
                  :key="suggestion.id"
                  :suggestion="suggestion"
                  :selectable="false"
                  :selected="false"
                  :removable="canRemoveSuggestion(suggestion)"
                  :voting-disabled="!!(session.maxParticipants && !currentUserHasSlot)"
                  @vote="handleVoteGame(suggestion)"
                  @remove="handleRemoveSuggestion(suggestion)"
                />
              </div>
              <div v-else class="text-gray-500 text-sm py-2">
                No games suggested yet. Be the first to suggest a game!
              </div>
            </div>

            <!-- Items to Bring Section (Pro+ feature) -->
            <div v-if="canUseItems" class="mb-6 pt-6 border-t border-gray-200">
              <h3 class="font-medium text-gray-700 mb-3 flex items-center gap-2">
                <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20,6H16V4C16,2.89 15.11,2 14,2H10C8.89,2 8,2.89 8,4V6H4C2.89,6 2,6.89 2,8V19C2,20.11 2.89,21 4,21H20C21.11,21 22,20.11 22,19V8C22,6.89 21.11,6 20,6M10,4H14V6H10V4Z"/>
                </svg>
                Items to Bring
                <span class="text-sm font-normal text-gray-500">(claim items you'll bring)</span>
              </h3>

              <!-- Add Item Form (Creator only) -->
              <div v-if="isCreator && isOpen" class="mb-4">
                <button v-if="!showAddItemForm" class="btn-outline text-sm" @click="showAddItemForm = true">
                  <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
                  </svg>
                  Add Item
                </button>
                <div v-else class="bg-gray-50 rounded-lg p-4">
                  <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-3">
                    <input
                      v-model="newItem.name"
                      type="text"
                      class="input sm:col-span-2"
                      placeholder="Item name (e.g., Chips, Soda, Napkins)"
                    />
                    <select v-model="newItem.category" class="input">
                      <option value="food">Food</option>
                      <option value="drinks">Drinks</option>
                      <option value="supplies">Supplies</option>
                      <option value="other">Other</option>
                    </select>
                  </div>
                  <div class="flex items-center gap-3">
                    <label class="flex items-center gap-2 text-sm text-gray-600">
                      Qty:
                      <input
                        v-model.number="newItem.quantity"
                        type="number"
                        min="1"
                        max="99"
                        class="input w-16 text-center"
                      />
                    </label>
                    <div class="flex-1"></div>
                    <button class="btn-ghost text-sm" @click="showAddItemForm = false" :disabled="addingItem">
                      Cancel
                    </button>
                    <button
                      class="btn-primary text-sm"
                      :disabled="!newItem.name.trim() || addingItem"
                      @click="handleAddItem"
                    >
                      <svg v-if="addingItem" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                      </svg>
                      Add
                    </button>
                  </div>
                </div>
              </div>

              <!-- Items List -->
              <div v-if="session.items && session.items.length > 0" class="space-y-2">
                <div
                  v-for="item in session.items"
                  :key="item.id"
                  class="flex items-center gap-3 p-3 rounded-lg border"
                  :class="item.claimedByUserId ? 'bg-green-50 border-green-200' : 'bg-white border-gray-200'"
                >
                  <!-- Category Icon -->
                  <div
                    class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                    :class="getCategoryColor(item.itemCategory)"
                  >
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path :d="getCategoryIcon(item.itemCategory)"/>
                    </svg>
                  </div>

                  <!-- Item Info -->
                  <div class="flex-1 min-w-0">
                    <div class="font-medium">
                      {{ item.itemName }}
                      <span v-if="item.quantityNeeded > 1" class="text-gray-500 text-sm">(x{{ item.quantityNeeded }})</span>
                    </div>
                    <div v-if="item.claimedBy" class="text-sm text-green-600">
                      Claimed by {{ item.claimedBy.displayName || '@' + item.claimedBy.username }}
                    </div>
                    <div v-else class="text-sm text-gray-400">Unclaimed</div>
                  </div>

                  <!-- Actions -->
                  <div class="flex items-center gap-2">
                    <!-- Claim/Unclaim Button -->
                    <template v-if="item.claimedByUserId === auth.user.value?.id">
                      <button
                        class="btn-ghost text-sm text-red-600"
                        :disabled="claimingItemId === item.id"
                        @click="handleUnclaimItem(item)"
                      >
                        <svg v-if="claimingItemId === item.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                        </svg>
                        <span v-else>Unclaim</span>
                      </button>
                    </template>
                    <template v-else-if="!item.claimedByUserId">
                      <button
                        class="btn-outline text-sm"
                        :disabled="claimingItemId === item.id"
                        @click="handleClaimItem(item)"
                      >
                        <svg v-if="claimingItemId === item.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                        </svg>
                        <span v-else>I'll bring this</span>
                      </button>
                    </template>

                    <!-- Remove Button (Creator only) -->
                    <button
                      v-if="isCreator && isOpen"
                      class="btn-ghost text-gray-400 hover:text-red-500 p-1"
                      title="Remove item"
                      @click="handleRemoveItem(item)"
                    >
                      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              <div v-else class="text-gray-500 text-sm py-2">
                No items added yet.
                <span v-if="isCreator && isOpen">Add items that attendees can volunteer to bring!</span>
              </div>
            </div>

            <!-- Save Button -->
            <div class="pt-4 border-t border-gray-200">
              <button
                class="btn-primary"
                :disabled="responding"
                @click="submitResponse"
              >
                <svg v-if="responding" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                {{ hasResponded ? 'Update My Availability' : 'Save My Availability' }}
              </button>
              <p class="text-xs text-gray-500 mt-2">Game votes are saved automatically when you click them</p>
            </div>
          </div>
        </div>

        <!-- Date Availability (for creator overview) -->
        <div v-if="isCreator && session.dates && session.invitees" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Availability Overview</h2>
            <p class="text-sm text-gray-500">
              {{ useSummaryView ? 'Click a date to see who\'s available' : 'Overview of everyone\'s availability' }}
            </p>
          </div>
          <div class="p-6" :class="{ 'overflow-x-auto': !useSummaryView }">
            <!-- Summary View (mobile or >10 invitees) -->
            <DateAvailabilitySummary
              v-if="useSummaryView"
              :dates="session.dates"
              :invitees="session.invitees"
              :selected-date-id="selectedDateId"
              :current-user-id="auth.user.value?.id"
              :pending-availability="responseForm.dateAvailability"
              @select-date="selectedDateId = $event"
            />
            <!-- Matrix View (desktop with <=10 invitees) -->
            <DateAvailabilityMatrix
              v-else
              :dates="session.dates"
              :invitees="session.invitees"
              :selected-date-id="selectedDateId"
              :current-user-id="auth.user.value?.id"
              :pending-availability="responseForm.dateAvailability"
              @select-date="selectedDateId = $event"
            />
          </div>
        </div>

        <!-- Game Suggestions Overview (for creator to select final game) -->
        <div v-if="isCreator && session.gameSuggestions && session.gameSuggestions.length > 0" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Game Selection</h2>
            <p class="text-sm text-gray-500">Select a game to include in the final event</p>
          </div>
          <div class="p-6">
            <div class="space-y-3">
              <GameSuggestionCard
                v-for="suggestion in [...session.gameSuggestions].sort((a, b) => b.voteCount - a.voteCount)"
                :key="suggestion.id"
                :suggestion="suggestion"
                :selectable="true"
                :selected="selectedGameId === suggestion.id"
                :removable="canRemoveSuggestion(suggestion)"
                @vote="handleVoteGame(suggestion)"
                @select="selectedGameId = suggestion.id"
                @remove="handleRemoveSuggestion(suggestion)"
              />
            </div>
          </div>
        </div>

        <!-- Invitees List -->
        <div class="card mb-6">
          <div class="p-6 border-b border-gray-100 flex items-start justify-between">
            <div>
              <h2 class="font-semibold">Invited Members</h2>
              <p v-if="session.maxParticipants" class="text-sm text-gray-500 mt-1">
                Limited to {{ session.maxParticipants }} participants (first-come-first-served)
              </p>
            </div>
            <button
              v-if="canInviteMembers && isOpen"
              class="btn btn-sm btn-outline flex items-center gap-1"
              @click="openInviteModal"
            >
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
              </svg>
              Invite More
            </button>
          </div>
          <div class="p-6">
            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              <div
                v-for="invitee in session.invitees"
                :key="invitee.id"
                class="flex flex-col items-center text-center"
              >
                <div class="relative">
                  <div :class="session.maxParticipants && invitee.hasSlot ? 'ring-2 ring-green-500 rounded-full' : ''">
                    <UserAvatar
                      :avatar-url="invitee.user?.avatarUrl"
                      :display-name="invitee.user?.displayName"
                      :is-founding-member="invitee.user?.isFoundingMember"
                      :is-admin="invitee.user?.isAdmin"
                      size="lg"
                    />
                  </div>
                  <div
                    v-if="invitee.hasResponded"
                    class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center"
                    :class="invitee.cannotAttendAny ? 'bg-red-500' : 'bg-green-500'"
                  >
                    <svg v-if="invitee.cannotAttendAny" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                    <svg v-else class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                </div>
                <span class="text-sm mt-2 truncate w-full">{{ invitee.user?.displayName || invitee.user?.username || 'Unknown' }}</span>
                <span class="text-xs text-gray-400">
                  <template v-if="session.maxParticipants && invitee.hasSlot">
                    <span class="text-green-600">Has Slot</span>
                  </template>
                  <template v-else>
                    {{ invitee.hasResponded ? (invitee.cannotAttendAny ? 'Unavailable' : 'Responded') : 'Pending' }}
                  </template>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Multi-Table Session Scheduler (for creator) -->
        <div v-if="isCreator && isMultiTable && session.gameSuggestions && session.gameSuggestions.length > 0" class="card">
          <div class="p-6 border-b border-gray-100">
            <div class="flex items-center gap-2">
              <h2 class="font-semibold">Schedule Games to Tables</h2>
              <span class="px-2 py-0.5 bg-primary-100 text-primary-700 text-xs rounded-full">
                {{ session.tableCount }} tables
              </span>
            </div>
          </div>
          <div class="p-6">
            <SessionScheduler
              :table-count="session.tableCount || 2"
              :game-suggestions="session.gameSuggestions"
              :initial-schedule="session.scheduledSessions || []"
              :initial-preferences="session.hostSessionPreferences || []"
              :saving="savingSchedule"
              @save="handleSaveSchedule"
            />
          </div>
        </div>

        <!-- Finalize Section (for creator) -->
        <div v-if="isCreator" class="card">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Finalize Game</h2>
          </div>
          <div class="p-6">
            <p class="text-gray-600 mb-4">
              When you're ready, finalize this session to create a draft event with the selected date and game.
            </p>

            <!-- Multi-table note -->
            <div v-if="isMultiTable" class="bg-blue-50 text-blue-700 text-sm p-3 rounded-lg mb-4">
              This is a multi-table session. Make sure you've scheduled games to tables above before finalizing.
              Members will sign up for specific game sessions after finalization.
            </div>

            <div class="grid grid-cols-2 gap-4 mb-6">
              <div>
                <label class="label">Selected Date</label>
                <select v-model="selectedDateId" class="input">
                  <option v-for="date in session.dates" :key="date.id" :value="date.id">
                    {{ formatDate(date.proposedDate) }} ({{ date.availableCount ?? 0 }} available)
                  </option>
                </select>
              </div>
              <div v-if="!isMultiTable">
                <label class="label">Selected Game</label>
                <select v-model="selectedGameId" class="input">
                  <option :value="null">No game selected</option>
                  <option v-for="game in session.gameSuggestions" :key="game.id" :value="game.id">
                    {{ game.gameName }} ({{ game.voteCount }} votes)
                  </option>
                </select>
              </div>
            </div>

            <button
              class="btn-primary"
              :disabled="!selectedDateId || finalizing || (isMultiTable && scheduleEntries.length === 0)"
              @click="handleFinalize"
            >
              <svg v-if="finalizing" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Create Game Event
            </button>
          </div>
        </div>
      </template>

      <!-- Planning Chat (for coordination) -->
      <div v-if="session" class="card">
        <div class="p-4 border-b border-gray-100">
          <button
            class="w-full flex items-center justify-between text-left"
            @click="showChat = !showChat"
          >
            <h2 class="font-semibold flex items-center gap-2">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H6L4,18V4H20"/>
              </svg>
              Planning Chat
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
            context-type="planning"
            :context-id="session.id"
            title="Planning Discussion"
          />
        </div>
      </div>
    </template>

    <!-- Invite More Members Modal -->
    <div
      v-if="showInviteModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="showInviteModal = false"
    >
      <div class="bg-white rounded-lg max-w-md w-full max-h-[80vh] flex flex-col">
        <div class="p-4 border-b border-gray-200 flex items-center justify-between">
          <h3 class="font-semibold text-lg">Invite More Members</h3>
          <button
            class="text-gray-400 hover:text-gray-600"
            @click="showInviteModal = false"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <div class="p-4 overflow-y-auto flex-1">
          <!-- Loading -->
          <div v-if="loadingMembers" class="flex justify-center py-8">
            <svg class="animate-spin h-8 w-8 text-primary-500" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
          </div>

          <!-- No uninvited members -->
          <div v-else-if="groupMembers.length === 0" class="text-center py-8 text-gray-500">
            All group members have already been invited.
          </div>

          <!-- Member list -->
          <div v-else class="space-y-2">
            <p class="text-sm text-gray-500 mb-3">
              Select members to invite to this planning session.
            </p>
            <label
              v-for="member in groupMembers"
              :key="member.userId"
              class="flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors"
              :class="selectedMemberIds.includes(member.userId) ? 'bg-primary-50 border border-primary-300' : 'bg-gray-50 border border-transparent hover:bg-gray-100'"
            >
              <input
                type="checkbox"
                :checked="selectedMemberIds.includes(member.userId)"
                class="w-5 h-5 text-primary-500 rounded"
                @change="toggleMemberSelection(member.userId)"
              />
              <UserAvatar
                :avatar-url="member.avatarUrl"
                :display-name="member.displayName"
                :is-founding-member="member.isFoundingMember"
                :is-admin="member.isAdmin"
                size="sm"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium truncate">{{ member.displayName || member.username }}</div>
                <div v-if="member.displayName && member.username" class="text-sm text-gray-500 truncate">@{{ member.username }}</div>
              </div>
            </label>
          </div>
        </div>

        <div class="p-4 border-t border-gray-200 space-y-3">
          <!-- Email option -->
          <label class="flex items-center gap-2 text-sm">
            <input
              v-model="sendInviteEmails"
              type="checkbox"
              class="w-4 h-4 text-primary-500 rounded"
            />
            Send email invitations
          </label>

          <!-- Actions -->
          <div class="flex gap-3">
            <button
              class="btn btn-outline flex-1"
              @click="showInviteModal = false"
            >
              Cancel
            </button>
            <button
              class="btn btn-primary flex-1"
              :disabled="selectedMemberIds.length === 0 || invitingMembers"
              @click="handleInviteMembers"
            >
              <svg v-if="invitingMembers" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              {{ invitingMembers ? 'Inviting...' : `Invite (${selectedMemberIds.length})` }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
