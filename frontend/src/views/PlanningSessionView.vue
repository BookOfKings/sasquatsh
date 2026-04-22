<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
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
  updateSessionSettings,
} from '@/services/planningApi'
import { getGroupMembers } from '@/services/groupsApi'
import { supabase } from '@/services/supabase'
import type { GroupMember } from '@/types/groups'
import { searchBggGames, getBggGame } from '@/services/bggApi'
import { getMyCollection, getUserCollection, type CollectionGame } from '@/services/collectionsApi'
import type { PlanningSession, GameSuggestion, PlanningItem, ItemCategory } from '@/types/planning'
import type { BggSearchResult } from '@/types/bgg'
import type { ScheduleEntry, HostPreference } from '@/types/sessions'
import DateAvailabilityMatrix from '@/components/planning/DateAvailabilityMatrix.vue'
import DateAvailabilitySummary from '@/components/planning/DateAvailabilitySummary.vue'
import GameSuggestionCard from '@/components/planning/GameSuggestionCard.vue'
import SessionScheduler from '@/components/planning/SessionScheduler.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import { getChatStats } from '@/services/chatApi'
import { createShareLink } from '@/services/shareLinksApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const session = ref<PlanningSession | null>(null)
const errorMessage = ref('')
const showChat = ref(false)
const chatStats = ref<{ count: number; lastMessageAt: string | null }>({ count: 0, lastMessageAt: null })

// Responsive detection for availability view
const isMobile = ref(false)
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768 // md breakpoint
}
const successMessage = ref('')
const shareLink = ref('')
const shareLinkLoading = ref(false)

async function generateShareLink() {
  if (!session.value) return
  shareLinkLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const link = await createShareLink(token, {
      groupId: session.value.groupId,
      linkType: 'session',
      planningSessionId: session.value.id,
    })
    shareLink.value = link.url
    await navigator.clipboard.writeText(link.url)
    successMessage.value = 'Invite link copied to clipboard!'
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create link'
  } finally {
    shareLinkLoading.value = false
  }
}

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
const gameSearchSource = ref<'my_collection' | 'host_collection' | 'bgg'>('my_collection')
const myCollectionGames = ref<CollectionGame[]>([])
const hostCollectionGames = ref<CollectionGame[]>([])
const collectionsLoaded = ref(false)

// Finalization state
const finalizing = ref(false)
const selectedDateId = ref<string | null>(null)
const selectedGameId = ref<string | null>(null)
const finalizeMode = ref<'single' | 'multi-table'>('single') // Mode override for finalization

// Multi-table scheduling state
const savingSchedule = ref(false)
const scheduleEntries = ref<ScheduleEntry[]>([])
const hostPreferences = ref<HostPreference[]>([])
const localScheduleCount = ref(0) // Track live schedule count from SessionScheduler

// Multi-table settings state
const updatingSettings = ref(false)
const desiredTableCount = ref<number>(2)

// Computed: is this a multi-table session?
const isMultiTable = computed(() => {
  return (session.value?.tableCount ?? 0) >= 2
})

// Computed: max tables based on creator's subscription tier
const maxTables = computed(() => {
  const tier = session.value?.createdBy?.subscriptionOverrideTier || session.value?.createdBy?.subscriptionTier || 'free'
  return tier === 'pro' || tier === 'premium' ? 10 : 5
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

// Wizard step state
const currentStep = ref(1)
const steps = computed(() => {
  const baseSteps = [
    { id: 1, name: 'Dates', icon: 'M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1' },
    { id: 2, name: 'Games', icon: 'M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z' },
    { id: 3, name: 'Items', icon: 'M20,6H16V4C16,2.89 15.11,2 14,2H10C8.89,2 8,2.89 8,4V6H4C2.89,6 2,6.89 2,8V19C2,20.11 2.89,21 4,21H20C21.11,21 22,20.11 22,19V8C22,6.89 21.11,6 20,6M10,4H14V6H10V4Z' },
    { id: 4, name: 'People', icon: 'M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z' },
  ]
  // Add finalize step for creator
  if (isCreator.value) {
    baseSteps.push({ id: 5, name: 'Finalize', icon: 'M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z' })
  }
  return baseSteps
})

// Step completion status
const stepCompletion = computed((): Record<number, boolean> => ({
  1: hasResponded.value, // Dates - completed if user responded
  2: (session.value?.gameSuggestions?.length ?? 0) > 0, // Games - completed if any games suggested
  3: (session.value?.items?.length ?? 0) > 0, // Items - completed if any items exist
  4: true, // People - always "complete" (just viewing)
  5: session.value?.status === 'finalized', // Finalize - completed if finalized
}))
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

// Check if session creator has paid tier (Basic+) - grants all participants access to items feature
const creatorHasPaidTier = computed(() => {
  if (!session.value?.createdBy) return false
  const creatorTier = session.value.createdBy.subscriptionOverrideTier
    || session.value.createdBy.subscriptionTier
    || 'free'
  return hasFeature(creatorTier, 'items')
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

    // Initialize finalize mode and table count based on session type
    finalizeMode.value = isMultiTable.value ? 'multi-table' : 'single'
    desiredTableCount.value = session.value.tableCount ?? 2

    // Load chat stats
    chatStats.value = await getChatStats('planning', sessionId.value)
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

async function loadCollections() {
  if (collectionsLoaded.value) return
  try {
    const token = await auth.getIdToken()
    if (!token || !session.value) return

    const promises: Promise<void>[] = []

    // Load my collection
    promises.push(
      getMyCollection(token).then(g => { myCollectionGames.value = g }).catch(() => {})
    )

    // Load host's collection (if I'm not the host)
    if (session.value.createdByUserId && session.value.createdByUserId !== auth.user.value?.id) {
      promises.push(
        getUserCollection(session.value.createdByUserId).then(g => { hostCollectionGames.value = g }).catch(() => {})
      )
    }

    await Promise.all(promises)
    collectionsLoaded.value = true
  } catch {
    // Silent fail — BGG search is always available
  }
}

const filteredCollectionGames = computed(() => {
  const source = gameSearchSource.value === 'my_collection' ? myCollectionGames.value : hostCollectionGames.value
  const q = gameSearchQuery.value.trim().toLowerCase()
  if (!q) return source
  return source.filter(g => g.game_name.toLowerCase().includes(q))
})

function selectCollectionGame(game: CollectionGame) {
  if (!session.value) return

  addingGame.value = true
  const token = auth.getIdToken()
  token.then(async (t) => {
    if (!t) return
    try {
      await suggestGame(t, session.value!.id, {
        gameName: game.game_name,
        bggId: game.bgg_id,
        thumbnailUrl: game.thumbnail_url ?? undefined,
        minPlayers: game.min_players ?? undefined,
        maxPlayers: game.max_players ?? undefined,
        playingTime: game.playing_time ?? undefined,
      })
      gameSearchQuery.value = ''
      showGameSearch.value = false
      await loadSession(true)
    } catch (err) {
      errorMessage.value = err instanceof Error ? err.message : 'Failed to add game suggestion'
    } finally {
      addingGame.value = false
    }
  })
}

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

    // Reload session to get updated scheduledSessions from server
    await loadSession(true)

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

  // For multi-table mode, ensure schedule is saved
  if (isMultiTable.value && scheduleEntries.value.length === 0) {
    errorMessage.value = 'Please schedule games to tables before finalizing'
    return
  }

  finalizing.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const mode = isMultiTable.value ? 'multi-table' : 'single'
    const result = await finalizePlanningSession(
      token,
      session.value.id,
      selectedDateId.value,
      !isMultiTable.value ? (selectedGameId.value || undefined) : undefined,
      mode // Pass the mode to backend
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

async function handleToggleMultiTable(enable: boolean) {
  if (!session.value) return

  updatingSettings.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const tableCount = enable ? desiredTableCount.value : null
    await updateSessionSettings(token, session.value.id, tableCount)

    // Update finalize mode to match
    finalizeMode.value = enable ? 'multi-table' : 'single'

    successMessage.value = enable ? 'Multi-table mode enabled!' : 'Multi-table mode disabled'
    await loadSession(true)
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update session settings'
  } finally {
    updatingSettings.value = false
  }
}

async function handleUpdateTableCount() {
  if (!session.value || !isMultiTable.value) return

  updatingSettings.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await updateSessionSettings(token, session.value.id, desiredTableCount.value)

    successMessage.value = 'Table count updated!'
    await loadSession(true)
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update table count'
  } finally {
    updatingSettings.value = false
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

function formatRelativeTime(isoString: string | null): string {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffMins < 1) return 'just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays === 1) return 'yesterday'
  if (diffDays < 7) return `${diffDays}d ago`
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
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
          <!-- Title row with cancel button -->
          <div class="flex items-start justify-between gap-4 mb-3">
            <h1 class="text-2xl font-bold text-gray-900">{{ session.title }}</h1>
            <div class="flex items-center gap-2 flex-shrink-0">
              <button
                @click="generateShareLink"
                :disabled="shareLinkLoading"
                class="text-sm text-primary-500 hover:text-primary-700 transition-colors flex items-center gap-1"
                title="Copy invite link"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10.59,13.41C11,13.8 11,14.44 10.59,14.83C10.2,15.22 9.56,15.22 9.17,14.83C7.22,12.88 7.22,9.71 9.17,7.76L12.71,4.22C14.66,2.27 17.83,2.27 19.78,4.22C21.73,6.17 21.73,9.34 19.78,11.29L18.29,12.78C18.3,11.96 18.17,11.14 17.89,10.36L18.36,9.88C19.54,8.71 19.54,6.81 18.36,5.64C17.19,4.46 15.29,4.46 14.12,5.64L10.59,9.17C9.41,10.34 9.41,12.24 10.59,13.41M13.41,9.17C13.8,8.78 14.44,8.78 14.83,9.17C16.78,11.12 16.78,14.29 14.83,16.24L11.29,19.78C9.34,21.73 6.17,21.73 4.22,19.78C2.27,17.83 2.27,14.66 4.22,12.71L5.71,11.22C5.7,12.04 5.83,12.86 6.11,13.64L5.64,14.12C4.46,15.29 4.46,17.19 5.64,18.36C6.81,19.54 8.71,19.54 9.88,18.36L13.41,14.83C14.59,13.66 14.59,11.76 13.41,10.59C13,10.2 13,9.56 13.41,9.17Z"/>
                </svg>
                {{ shareLink ? 'Copied!' : 'Share' }}
              </button>
              <button
                v-if="isCreator && isOpen"
                class="text-sm text-gray-400 hover:text-red-500 transition-colors"
                @click="handleCancel"
              >
                Cancel Session
              </button>
            </div>
          </div>

          <!-- Description -->
          <p v-if="session.description" class="text-gray-600 mb-4">{{ session.description }}</p>

          <!-- Metadata row -->
          <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-500">
            <!-- Status indicator -->
            <span class="flex items-center gap-1.5">
              <span
                class="w-2 h-2 rounded-full"
                :class="{
                  'bg-green-500': session.status === 'open',
                  'bg-blue-500': session.status === 'finalized',
                  'bg-red-500': session.status === 'cancelled',
                  'bg-gray-400': !['open', 'finalized', 'cancelled'].includes(session.status)
                }"
              />
              <span class="capitalize">{{ session.status }}</span>
            </span>

            <!-- Multi-table indicator -->
            <span v-if="isMultiTable" class="flex items-center gap-1.5 text-purple-600">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M4,4H20A2,2 0 0,1 22,6V18A2,2 0 0,1 20,20H4A2,2 0 0,1 2,18V6A2,2 0 0,1 4,4M4,6V18H11V6H4M20,18V6H18V18H20M13,6V18H16V6H13Z"/>
              </svg>
              <span>Multi-Table</span>
            </span>

            <!-- Separator dot -->
            <span class="text-gray-300 hidden sm:inline">·</span>

            <!-- Creator -->
            <span class="flex items-center gap-1.5">
              <UserAvatar
                :avatar-url="session.createdBy?.avatarUrl ?? undefined"
                :display-name="session.createdBy?.displayName ?? undefined"
                :username="session.createdBy?.username ?? undefined"
                :user-id="session.createdByUserId"
                size="xs"
                :is-founding-member="session.createdBy?.isFoundingMember"
                :is-admin="session.createdBy?.isAdmin"
              />
              <span>{{ session.createdBy?.displayName || session.createdBy?.username || 'Unknown' }}</span>
            </span>

            <!-- Separator dot -->
            <span class="text-gray-300 hidden sm:inline">·</span>

            <!-- Deadline -->
            <span
              class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium"
              :class="deadlinePassed
                ? 'bg-red-100 text-red-700'
                : 'bg-amber-50 text-amber-700 border border-amber-200'"
            >
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22C6.47,22 2,17.5 2,12A10,10 0 0,1 12,2M12.5,7V12.25L17,14.92L16.25,16.15L11,13V7H12.5Z"/>
              </svg>
              <span v-if="deadlinePassed">Deadline passed</span>
              <span v-else>Respond by {{ formatDateTime(session.responseDeadline) }}</span>
            </span>
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

          <!-- Open to Group Badge -->
          <div v-if="session.openToGroup" class="mt-4 pt-4 border-t border-gray-100">
            <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-green-100 text-green-700 text-sm font-medium">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
              </svg>
              Open to all group members
            </span>
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
        <!-- Stepper Navigation -->
        <div class="card mb-6">
          <div class="p-4">
            <div class="flex items-center justify-between">
              <button
                v-for="(step, index) in steps"
                :key="step.id"
                class="flex-1 flex flex-col items-center gap-1 py-2 px-1 rounded-lg transition-colors relative"
                :class="[
                  currentStep === step.id ? 'bg-primary-50' : 'hover:bg-gray-50',
                  index < steps.length - 1 ? '' : ''
                ]"
                @click="currentStep = step.id"
              >
                <!-- Step circle with icon -->
                <div
                  class="w-10 h-10 rounded-full flex items-center justify-center transition-colors"
                  :class="[
                    currentStep === step.id ? 'bg-primary-500 text-white' :
                    stepCompletion[step.id] ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'
                  ]"
                >
                  <svg v-if="stepCompletion[step.id] && currentStep !== step.id" class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                  </svg>
                  <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                    <path :d="step.icon"/>
                  </svg>
                </div>
                <!-- Step label -->
                <span
                  class="text-xs font-medium"
                  :class="currentStep === step.id ? 'text-primary-700' : 'text-gray-500'"
                >
                  {{ step.name }}
                </span>
                <!-- Connector line -->
                <div
                  v-if="index < steps.length - 1"
                  class="absolute top-6 left-[60%] w-[80%] h-0.5"
                  :class="stepCompletion[step.id] ? 'bg-green-200' : 'bg-gray-200'"
                />
              </button>
            </div>
          </div>
        </div>

        <!-- Step 1: Dates -->
        <div v-show="currentStep === 1" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Date Availability</h2>
            <p class="text-sm text-gray-500 mt-1">Select which dates work for you</p>
          </div>
          <div class="p-6">
            <div v-if="hasResponded" class="text-green-600 flex items-center gap-2 mb-4">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              You have already responded. You can update your availability below.
            </div>

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

            <!-- Save Availability Button -->
            <div class="mt-4 flex items-center gap-4">
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
              <button class="btn-outline" @click="currentStep = 2">
                Next: Games
                <svg class="w-4 h-4 ml-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
                </svg>
              </button>
            </div>

            <!-- Creator: Availability Overview -->
            <div v-if="isCreator && session.dates && session.invitees" class="mt-6 pt-6 border-t border-gray-200">
              <h3 class="font-medium text-gray-700 mb-3">Everyone's Availability</h3>
              <div :class="{ 'overflow-x-auto': !useSummaryView }">
                <DateAvailabilitySummary
                  v-if="useSummaryView"
                  :dates="session.dates"
                  :invitees="session.invitees"
                  :selected-date-id="selectedDateId"
                  :current-user-id="auth.user.value?.id"
                  :pending-availability="responseForm.dateAvailability"
                  @select-date="selectedDateId = $event"
                />
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
          </div>
        </div>

        <!-- Step 2: Games -->
        <div v-show="currentStep === 2" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Game Suggestions</h2>
            <p class="text-sm text-gray-500 mt-1">Suggest and vote on games you want to play</p>
          </div>
          <div class="p-6">
            <!-- Game count header -->
            <div class="flex items-center justify-between mb-4">
              <span class="text-sm text-gray-500">Vote for all games you'd like to play. Games with 2+ votes will be included!</span>
              <span v-if="session.gameSuggestions?.length" class="text-sm text-gray-500">
                {{ session.gameSuggestions.length }} game{{ session.gameSuggestions.length === 1 ? '' : 's' }}
              </span>
            </div>

            <!-- Game Search -->
            <div v-if="!showGameSearch" class="mb-4">
                <button
                  class="btn-outline text-sm"
                  :disabled="!!(session.maxParticipants && !currentUserHasSlot)"
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
              </div>
              <div v-else class="mb-4">
                <!-- Source tabs -->
                <div class="flex gap-1 mb-2">
                  <button
                    type="button"
                    class="px-3 py-1 text-xs font-medium rounded-full transition-colors"
                    :class="gameSearchSource === 'my_collection' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                    @click="gameSearchSource = 'my_collection'; loadCollections()"
                  >My Collection</button>
                  <button
                    v-if="session.createdByUserId && session.createdByUserId !== auth.user.value?.id"
                    type="button"
                    class="px-3 py-1 text-xs font-medium rounded-full transition-colors"
                    :class="gameSearchSource === 'host_collection' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                    @click="gameSearchSource = 'host_collection'; loadCollections()"
                  >Host's Collection</button>
                  <button
                    type="button"
                    class="px-3 py-1 text-xs font-medium rounded-full transition-colors"
                    :class="gameSearchSource === 'bgg' ? 'bg-primary-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                    @click="gameSearchSource = 'bgg'"
                  >Search BGG</button>
                  <div class="flex-1"></div>
                  <button
                    class="p-1 text-gray-400 hover:text-gray-600"
                    @click="showGameSearch = false; gameSearchQuery = ''; gameSearchResults = []"
                  >
                    <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                  </button>
                </div>

                <!-- Search input -->
                <div class="relative">
                  <input
                    v-model="gameSearchQuery"
                    type="text"
                    class="input pr-10"
                    :placeholder="gameSearchSource === 'bgg' ? 'Search BoardGameGeek...' : 'Filter collection...'"
                    @input="gameSearchSource === 'bgg' ? handleGameSearch() : undefined"
                  />
                  <svg class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
                  </svg>
                </div>

                <!-- BGG attribution -->
                <div v-if="gameSearchSource === 'bgg'" class="flex items-center justify-end mt-1">
                  <a href="https://boardgamegeek.com" target="_blank" rel="noopener noreferrer" class="hover:opacity-80 transition-opacity">
                    <img src="/powered-by-bgg.svg" alt="Powered by BoardGameGeek" class="h-5" />
                  </a>
                </div>

                <!-- Collection results -->
                <template v-if="gameSearchSource !== 'bgg'">
                  <div v-if="filteredCollectionGames.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
                    <button
                      v-for="game in filteredCollectionGames"
                      :key="game.bgg_id"
                      class="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left border-b border-gray-100 last:border-0"
                      :disabled="addingGame"
                      @click="selectCollectionGame(game)"
                    >
                      <img
                        v-if="game.thumbnail_url"
                        :src="game.thumbnail_url"
                        :alt="game.game_name"
                        class="w-10 h-10 object-cover rounded flex-shrink-0 bg-gray-100"
                      />
                      <div v-else class="w-10 h-10 flex items-center justify-center bg-gray-100 rounded flex-shrink-0">
                        <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
                        </svg>
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="font-medium truncate">{{ game.game_name }}</div>
                        <div class="text-sm text-gray-500">
                          <span v-if="game.year_published">{{ game.year_published }}</span>
                          <span v-if="game.min_players && game.max_players"> · {{ game.min_players }}-{{ game.max_players }}p</span>
                          <span v-if="game.playing_time"> · {{ game.playing_time }}min</span>
                        </div>
                      </div>
                    </button>
                  </div>
                  <div v-else-if="collectionsLoaded" class="mt-2 text-sm text-gray-500 text-center py-4">
                    {{ gameSearchSource === 'my_collection'
                      ? (myCollectionGames.length === 0 ? 'Your collection is empty — add games in My Collection' : 'No matching games')
                      : (hostCollectionGames.length === 0 ? "Host's collection is private or empty" : 'No matching games')
                    }}
                  </div>
                  <div v-else class="mt-2 text-sm text-gray-500 text-center py-4">Loading collection...</div>
                </template>

                <!-- BGG search results -->
                <template v-else>
                  <div v-if="searchingGames" class="mt-2 text-gray-500 text-sm">Searching...</div>
                  <div v-else-if="gameSearchResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
                    <button
                      v-for="result in gameSearchResults"
                      :key="result.bggId"
                      class="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50 text-left border-b border-gray-100 last:border-0"
                      :disabled="addingGame"
                      @click="selectGameToSuggest(result)"
                    >
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
                </template>
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
                :selectable="isCreator"
                :selected="selectedGameId === suggestion.id"
                :removable="canRemoveSuggestion(suggestion)"
                :voting-disabled="!!(session.maxParticipants && !currentUserHasSlot)"
                @vote="handleVoteGame(suggestion)"
                @select="selectedGameId = suggestion.id"
                @remove="handleRemoveSuggestion(suggestion)"
              />
            </div>
            <div v-else class="text-gray-500 text-sm py-2">
              No games suggested yet. Be the first to suggest a game!
            </div>

            <!-- Navigation buttons -->
            <div class="mt-6 pt-4 border-t border-gray-200 flex items-center justify-between">
              <button class="btn-ghost" @click="currentStep = 1">
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z"/>
                </svg>
                Back: Dates
              </button>
              <button class="btn-outline" @click="currentStep = 3">
                Next: Items
                <svg class="w-4 h-4 ml-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Step 3: Items -->
        <div v-show="currentStep === 3" class="card mb-6">
          <div class="p-6 border-b border-gray-100">
            <h2 class="font-semibold">Items to Bring</h2>
            <p class="text-sm text-gray-500 mt-1">Claim items you'll bring to the event</p>
          </div>
          <div class="p-6">
            <!-- Items to Bring Section (available when creator has Basic+ tier) -->
            <div v-if="creatorHasPaidTier" class="mb-6 pt-6 border-t border-gray-200">
              <h3 class="font-medium text-gray-700 mb-3 flex items-center gap-2">
                <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20,6H16V4C16,2.89 15.11,2 14,2H10C8.89,2 8,2.89 8,4V6H4C2.89,6 2,6.89 2,8V19C2,20.11 2.89,21 4,21H20C21.11,21 22,20.11 22,19V8C22,6.89 21.11,6 20,6M10,4H14V6H10V4Z"/>
                </svg>
                Items to Bring
                <span class="text-sm font-normal text-gray-500">(claim items you'll bring)</span>
              </h3>

              <!-- Add Item Form (available to all participants when creator has Basic+ tier) -->
              <div v-if="creatorHasPaidTier && isOpen" class="mb-4">
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

                    <!-- Remove Button (Creator or item adder) -->
                    <button
                      v-if="(isCreator || item.addedByUserId === auth.user.value?.id) && isOpen"
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
                <span v-if="creatorHasPaidTier && isOpen">Add items that attendees can volunteer to bring!</span>
              </div>
            </div>

            <!-- Items feature not available message -->
            <div v-if="!creatorHasPaidTier" class="bg-gray-50 rounded-lg p-4 text-center">
              <p class="text-gray-600 text-sm">Items feature requires the session host to have a Basic subscription or higher.</p>
            </div>

            <!-- Navigation buttons -->
            <div class="mt-6 pt-4 border-t border-gray-200 flex items-center justify-between">
              <button class="btn-ghost" @click="currentStep = 2">
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z"/>
                </svg>
                Back: Games
              </button>
              <button class="btn-outline" @click="currentStep = 4">
                Next: People
                <svg class="w-4 h-4 ml-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Step 4: People/Invitees -->
        <div v-show="currentStep === 4" class="card mb-6">
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
                  <!-- Grayscale filter for non-responders (except creator who is always attending) -->
                  <div
                    :class="[
                      session.maxParticipants && invitee.hasSlot ? 'ring-2 ring-green-500 rounded-full' : '',
                      !(invitee.hasResponded || invitee.userId === session.createdByUserId) ? 'grayscale opacity-50' : ''
                    ]"
                  >
                    <UserAvatar
                      :avatar-url="invitee.user?.avatarUrl"
                      :display-name="invitee.user?.displayName"
                      :is-founding-member="invitee.user?.isFoundingMember"
                      :is-admin="invitee.user?.isAdmin"
                      :user-id="invitee.userId"
                      size="lg"
                    />
                  </div>
                  <!-- Status badge: creator always shows as attending, others show their response status -->
                  <div
                    v-if="invitee.hasResponded || invitee.userId === session.createdByUserId"
                    class="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center"
                    :class="invitee.cannotAttendAny && invitee.userId !== session.createdByUserId ? 'bg-red-500' : 'bg-green-500'"
                  >
                    <svg v-if="invitee.cannotAttendAny && invitee.userId !== session.createdByUserId" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                    <svg v-else class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                </div>
                <span class="text-sm mt-2 truncate w-full">{{ invitee.user?.displayName || invitee.user?.username || 'Unknown' }}</span>
                <span class="text-xs text-gray-400">
                  <template v-if="invitee.userId === session.createdByUserId">
                    <span class="text-primary-600">Organizer</span>
                  </template>
                  <template v-else-if="session.maxParticipants && invitee.hasSlot">
                    <span class="text-green-600">Has Slot</span>
                  </template>
                  <template v-else>
                    {{ invitee.hasResponded ? (invitee.cannotAttendAny ? 'Unavailable' : 'Responded') : 'Pending' }}
                  </template>
                </span>
              </div>
            </div>

            <!-- Navigation buttons -->
            <div class="mt-6 pt-4 border-t border-gray-200 flex items-center justify-between">
              <button class="btn-ghost" @click="currentStep = 3">
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z"/>
                </svg>
                Back: Items
              </button>
              <button v-if="isCreator" class="btn-primary" @click="currentStep = 5">
                Next: Finalize
                <svg class="w-4 h-4 ml-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Step 5: Finalize (Creator only) -->
        <div v-show="currentStep === 5 && isCreator" class="space-y-6">
          <!-- Event Mode Toggle -->
          <div class="card">
            <div class="p-6 border-b border-gray-100">
              <h2 class="font-semibold">Event Mode</h2>
              <p class="text-sm text-gray-500 mt-1">Choose how you want to organize this game night</p>
            </div>
            <div class="p-6">
              <div class="flex gap-3">
                <button
                  type="button"
                  class="flex-1 px-4 py-3 rounded-lg border-2 transition-colors text-left"
                  :class="!isMultiTable ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'"
                  :disabled="updatingSettings"
                  @click="isMultiTable && handleToggleMultiTable(false)"
                >
                  <div class="flex items-center gap-2">
                    <svg class="w-5 h-5" :class="!isMultiTable ? 'text-primary-600' : 'text-gray-400'" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
                    </svg>
                    <span class="font-medium" :class="!isMultiTable ? 'text-primary-700' : 'text-gray-700'">Single Event</span>
                    <svg v-if="!isMultiTable" class="w-4 h-4 text-primary-600" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                  <p class="text-sm text-gray-500 mt-1">Everyone attends the same game session</p>
                </button>
                <button
                  type="button"
                  class="flex-1 px-4 py-3 rounded-lg border-2 transition-colors text-left"
                  :class="isMultiTable ? 'border-purple-500 bg-purple-50' : 'border-gray-200 hover:border-gray-300'"
                  :disabled="updatingSettings"
                  @click="!isMultiTable && handleToggleMultiTable(true)"
                >
                  <div class="flex items-center gap-2">
                    <svg class="w-5 h-5" :class="isMultiTable ? 'text-purple-600' : 'text-gray-400'" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M4,4H20A2,2 0 0,1 22,6V18A2,2 0 0,1 20,20H4A2,2 0 0,1 2,18V6A2,2 0 0,1 4,4M4,6V18H11V6H4M20,18V6H18V18H20M13,6V18H16V6H13Z"/>
                    </svg>
                    <span class="font-medium" :class="isMultiTable ? 'text-purple-700' : 'text-gray-700'">Multi-Table</span>
                    <svg v-if="isMultiTable" class="w-4 h-4 text-purple-600" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                  <p class="text-sm text-gray-500 mt-1">Multiple games run simultaneously on different tables</p>
                </button>
              </div>

              <!-- Table count selector (when multi-table is enabled) -->
              <div v-if="isMultiTable" class="mt-4 pt-4 border-t border-gray-200">
                <div class="flex items-center gap-4">
                  <label class="text-sm font-medium text-gray-700">Number of Tables:</label>
                  <select
                    v-model.number="desiredTableCount"
                    class="input w-24"
                    :disabled="updatingSettings"
                  >
                    <option v-for="n in (maxTables - 1)" :key="n + 1" :value="n + 1">{{ n + 1 }}</option>
                  </select>
                  <button
                    v-if="desiredTableCount !== session.tableCount"
                    class="btn-sm btn-outline"
                    :disabled="updatingSettings"
                    @click="handleUpdateTableCount"
                  >
                    <svg v-if="updatingSettings" class="animate-spin w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                    </svg>
                    Update
                  </button>
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
              @update:schedule-count="localScheduleCount = $event"
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
              This will create a multi-table event. Make sure you've scheduled games to tables above before finalizing.
              Members will sign up for specific game sessions after the event is created.
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

            <!-- Validation Messages -->
            <div v-if="!selectedDateId || (isMultiTable && localScheduleCount === 0)" class="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <p class="text-sm font-medium text-amber-800 mb-2">Before you can create the event:</p>
              <ul class="text-sm text-amber-700 space-y-1">
                <li v-if="!selectedDateId" class="flex items-center gap-2">
                  <svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
                  </svg>
                  Select a date from the dropdown above
                </li>
                <li v-if="isMultiTable && localScheduleCount === 0" class="flex items-center gap-2">
                  <svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
                  </svg>
                  Schedule at least one game to a table (use the scheduler above)
                </li>
              </ul>
            </div>

            <div class="flex items-center gap-4">
              <button
                class="btn-primary"
                :disabled="!selectedDateId || finalizing || (isMultiTable && localScheduleCount === 0)"
                @click="handleFinalize"
              >
                <svg v-if="finalizing" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Create Game Event
              </button>
              <button class="btn-ghost" @click="currentStep = 4">
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15.41,16.58L10.83,12L15.41,7.41L14,6L8,12L14,18L15.41,16.58Z"/>
                </svg>
                Back to People
              </button>
            </div>
          </div>
        </div>
        </div>
      </template>

      <!-- Planning Chat (for coordination) -->
      <div v-if="session" class="card mt-6">
        <div class="p-4 border-b border-gray-100">
          <button
            class="w-full flex items-center justify-between text-left"
            @click="showChat = !showChat"
          >
            <div>
              <h2 class="font-semibold flex items-center gap-2">
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H6L4,18V4H20"/>
                </svg>
                Planning Chat
                <span v-if="chatStats.count > 0" class="text-xs font-normal text-gray-500">
                  ({{ chatStats.count }} message{{ chatStats.count !== 1 ? 's' : '' }})
                </span>
              </h2>
              <p v-if="chatStats.lastMessageAt" class="text-xs text-gray-500 mt-0.5 ml-7">
                Last message {{ formatRelativeTime(chatStats.lastMessageAt) }}
              </p>
            </div>
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
                :user-id="member.userId"
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
