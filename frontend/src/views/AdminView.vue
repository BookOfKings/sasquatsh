<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getAllLocations,
  getPendingLocations,
  createLocation,
  updateLocation,
  deleteLocation,
  mergeLocations,
  approveLocation,
  rejectLocation,
  getBggCacheStats,
  importPopularGames,
  importHotGames,
  refreshStaleCache,
  refreshIncompleteCache,
  refreshBggThumbnails,
  getAdminDashboard,
  getAdminUsers,
  suspendUser,
  unsuspendUser,
  banUser,
  unbanUser,
  setUserTier,
  toggleFoundingMember,
  updateUser,
  deleteUser,
  sendPasswordReset,
  getAdminGroups,
  deleteGroup,
  getGroupMembers,
  addGroupMember,
  removeGroupMember,
  changeGroupMemberRole,
  getAdminNotes,
  createAdminNote,
  updateAdminNote,
  deleteAdminNote,
  getAdminBugs,
  createAdminBug,
  updateAdminBug,
  deleteAdminBug,
  getMtgCacheStats,
  warmMtgStaples,
  warmMtgCommanders,
  refreshMtgStaleCache,
  getAdminEvents,
  deleteAdminEvent,
} from '@/services/adminApi'
import type { EventLocation } from '@/types/social'
import type { BggCacheStats, BggCacheEntry, AdminStats, ServiceHealth, AdminUser, AdminGroup, AdminGroupMember, AdminNote, AdminBug, MtgCacheStats, MtgCacheWarmResult, AdminEvent } from '@/services/adminApi'
import { listBggCache, refreshBggGame, updateBggCacheEntry } from '@/services/adminApi'
import { getAllAds, getAdStats, createAd, updateAd, deleteAd, toggleAdActive } from '@/services/adsApi'
import { getAllBadges, getUserBadges, adminAwardBadge, adminRevokeBadge, type Badge } from '@/services/badgesApi'
import type { Ad, AdStats, CreateAdInput } from '@/services/adsApi'
import { getChatReports, reviewChatReport, issueModerationAction, getChatModerationHistory } from '@/services/chatApi'
import type { ChatReport, ChatModerationHistoryItem, ChatModerationAction } from '@/types/chat'
import { REPORT_REASON_LABELS, MODERATION_ACTION_LABELS } from '@/types/chat'
import { getAllRaffles, getRaffleEntries, createRaffle, updateRaffle, deleteRaffle, selectRaffleWinner, markWinnerNotified, markPrizeClaimed } from '@/services/raffleApi'
import type { RaffleWithDetails, RaffleEntryWithUser, CreateRaffleInput, RaffleStatus } from '@/types/raffle'
import { ENTRY_TYPE_LABELS } from '@/types/raffle'
import GameSearch from '@/components/common/GameSearch.vue'
import type { BggGame } from '@/types/bgg'
import D20Spinner from '@/components/common/D20Spinner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import StateSelect from '@/components/common/StateSelect.vue'

const auth = useAuthStore()

// Minimum time to show loading spinner (in ms) so users can admire the D20
const MIN_LOADING_TIME = 3000

// Helper to ensure minimum loading time
function withMinLoadingTime<T>(promise: Promise<T>, startTime: number): Promise<T> {
  return promise.then(async (result) => {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    return result
  })
}

const activeTab = ref<'dashboard' | 'users' | 'groups' | 'events' | 'notes' | 'bugs' | 'chatReports' | 'ads' | 'raffles' | 'locations' | 'caches'>('dashboard')
const activeCacheTab = ref<'bgg' | 'mtg'>('bgg')

// Events state
const deletingEventId = ref<string | null>(null)
const eventsLoading = ref(false)
const adminEvents = ref<AdminEvent[]>([])
const eventsTotal = ref(0)
const eventsPage = ref(1)
const eventsSearch = ref('')
const eventsShowPast = ref(false)

// Dashboard state
const dashboardLoading = ref(false)
const dashboardStats = ref<AdminStats | null>(null)
const serviceHealth = ref<ServiceHealth[]>([])
const lastRefresh = ref<Date | null>(null)

const loading = ref(true)
const locations = ref<EventLocation[]>([])
const errorMessage = ref('')
const successMessage = ref('')

// BGG Cache state
const cacheStats = ref<BggCacheStats | null>(null)
const cacheLoading = ref(false)
const cacheImporting = ref(false)

// MTG Cache state
const mtgCacheStats = ref<MtgCacheStats | null>(null)
const mtgCacheLoading = ref(false)
const mtgCacheWarming = ref(false)
const mtgWarmProgress = ref('')
const mtgWarmResult = ref<MtgCacheWarmResult | null>(null)
const cacheGames = ref<BggCacheEntry[]>([])
const cacheGamesTotal = ref(0)
const cacheGamesPage = ref(1)
const cacheGamesLoading = ref(false)
const cacheSearch = ref('')
const cacheFilter = ref<'all' | 'missing_thumbnail' | 'missing_players' | 'incomplete'>('all')
const refreshingGameId = ref<number | null>(null)
const showCacheEditDialog = ref(false)
const editingCacheGame = ref<BggCacheEntry | null>(null)
const cacheEditForm = reactive({
  thumbnailUrl: '',
  imageUrl: '',
  minPlayers: null as number | null,
  maxPlayers: null as number | null,
})
const savingCacheGame = ref(false)

// User management state
const users = ref<AdminUser[]>([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersLoading = ref(false)
const userSearch = ref('')
const showSuspendedOnly = ref(false)
const suspendingUserId = ref<string | null>(null)
const suspendReason = ref('')
const showSuspendDialog = ref(false)
const userToSuspend = ref<AdminUser | null>(null)
const showUserEditDialog = ref(false)
const editingUser = ref<AdminUser | null>(null)
const userEditForm = reactive({
  displayName: '',
  username: '',
  isAdmin: false,
  isFoundingMember: false,
  tier: 'free' as 'free' | 'basic' | 'pro' | 'premium',
  tierReason: '',
})
const savingUser = ref(false)
const deletingUserId = ref<string | null>(null)
const allBadgesList = ref<Badge[]>([])
const userBadgeIds = ref<Set<number>>(new Set())
const badgesLoading = ref(false)
const badgeUpdating = ref<number | null>(null)
const sendingPasswordReset = ref<string | null>(null)

// Ban management
const showBanDialog = ref(false)
const userToBan = ref<AdminUser | null>(null)
const banReason = ref('')
const banningUserId = ref<string | null>(null)
const showBannedOnly = ref(false)

// Tier management
const showTierDialog = ref(false)
const userToSetTier = ref<AdminUser | null>(null)
const tierForm = reactive({
  tier: 'free' as 'free' | 'basic' | 'pro' | 'premium',
  reason: '',
})
const settingTierId = ref<string | null>(null)
const togglingFoundingId = ref<string | null>(null)

// Group management state
const groups = ref<AdminGroup[]>([])
const groupsTotal = ref(0)
const groupsPage = ref(1)
const groupsLoading = ref(false)
const groupSearch = ref('')
const deletingGroupId = ref<string | null>(null)

// Group member management
const showMembersDialog = ref(false)
const selectedGroup = ref<AdminGroup | null>(null)
const groupMembers = ref<AdminGroupMember[]>([])
const membersLoading = ref(false)
const removingMemberId = ref<string | null>(null)
const showAddMemberDialog = ref(false)
const addMemberSearch = ref('')
const addMemberResults = ref<AdminUser[]>([])
const addMemberSearching = ref(false)
const addingMemberId = ref<string | null>(null)
const changingRoleId = ref<string | null>(null)

// Notes state
const notes = ref<AdminNote[]>([])
const expandedNotes = reactive(new Set<string>())
const notesLoading = ref(false)
const showNoteDialog = ref(false)
const editingNote = ref<AdminNote | null>(null)
const noteForm = reactive({
  title: '',
  content: '',
  category: 'general',
})
const savingNote = ref(false)
const deletingNoteId = ref<string | null>(null)

// Bugs state
const bugs = ref<AdminBug[]>([])
const bugsLoading = ref(false)
const bugStatusFilter = ref('open')
const bugPriorityFilter = ref('')
const showBugDialog = ref(false)
const editingBug = ref<AdminBug | null>(null)
const bugForm = reactive({
  title: '',
  description: '',
  stepsToReproduce: '',
  priority: 'medium' as 'low' | 'medium' | 'high' | 'critical',
  status: 'open' as 'open' | 'in_progress' | 'resolved' | 'closed' | 'wont_fix',
})
const savingBug = ref(false)
const deletingBugId = ref<string | null>(null)

// Chat Reports state
const chatReports = ref<ChatReport[]>([])
const chatReportsLoading = ref(false)
const chatReportStatusFilter = ref('pending')
const showModerationDialog = ref(false)
const selectedReport = ref<ChatReport | null>(null)
const moderationForm = reactive({
  action: 'warning' as ChatModerationAction,
  reason: '',
})
const issuingModeration = ref(false)
const dismissingReportId = ref<string | null>(null)
const moderationHistory = ref<ChatModerationHistoryItem[]>([])
const moderationHistoryLoading = ref(false)

// Ads state
const ads = ref<Ad[]>([])
const adStats = ref<AdStats[]>([])
const adsLoading = ref(false)
const showAdDialog = ref(false)
const editingAd = ref<Ad | null>(null)
const adForm = reactive({
  name: '',
  advertiserName: '',
  adType: 'banner' as 'banner' | 'sidebar' | 'featured',
  placement: 'general' as 'general' | 'dashboard' | 'events' | 'groups',
  imageUrl: '',
  title: '',
  description: '',
  linkUrl: '',
  targetCity: '',
  targetState: '',
  startDate: '',
  endDate: '',
  isActive: true,
  isHouseAd: false,
  priority: 0,
})
const savingAd = ref(false)
const deletingAdId = ref<string | null>(null)
const togglingAdId = ref<string | null>(null)
const showAdStats = ref(false)

// Raffles state
const raffles = ref<RaffleWithDetails[]>([])
const rafflesLoading = ref(false)
const showRaffleDialog = ref(false)
const editingRaffle = ref<RaffleWithDetails | null>(null)
const previewingRaffle = ref<RaffleWithDetails | null>(null)
const raffleForm = reactive({
  title: '',
  description: '',
  prizeName: '',
  prizeDescription: '',
  prizeImageUrl: '',
  prizeBggId: null as number | null,
  prizeValueCents: null as number | null,
  startDate: '',
  endDate: '',
  termsConditions: '',
  mailInInstructions: '',
  bannerImageUrl: '',
  status: 'draft' as RaffleStatus,
})
const savingRaffle = ref(false)
const deletingRaffleId = ref<string | null>(null)
const selectingWinnerId = ref<string | null>(null)
const showRaffleEntries = ref(false)
const selectedRaffleForEntries = ref<RaffleWithDetails | null>(null)
const raffleEntries = ref<RaffleEntryWithUser[]>([])
const raffleEntriesLoading = ref(false)
const selectedPrizeGame = ref<BggGame | null>(null)
const prizeGameSearch = ref('')

// Create/Edit dialog
const showDialog = ref(false)
const editingLocation = ref<EventLocation | null>(null)
const saving = ref(false)
type LocationType = 'temporary' | 'permanent' | 'recurring'
const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const form = reactive({
  name: '',
  addressLine1: '',
  city: '',
  state: '',
  venue: '',
  locationType: 'temporary' as LocationType,
  startDate: '',
  endDate: '',
  recurringDays: [] as number[],
})

// Merge mode
const mergeMode = ref(false)
const selectedForMerge = ref<string[]>([])

// Pending locations
const pendingLocations = ref<EventLocation[]>([])
const approvingId = ref<string | null>(null)
const rejectingId = ref<string | null>(null)

onMounted(async () => {
  await loadDashboard()
  await loadLocations()
  await loadCacheStats()
  await loadCacheGames()
  await loadMtgCacheStats()
})

async function loadDashboard() {
  dashboardLoading.value = true
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const data = await withMinLoadingTime(getAdminDashboard(token), startTime)
    dashboardStats.value = data.stats
    serviceHealth.value = data.services
    // Add cache health entries
    updateServiceHealthWithCaches()
    lastRefresh.value = new Date()
  } catch (err) {
    console.error('Failed to load dashboard:', err)
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    dashboardLoading.value = false
  }
}

function getHealthStatusColor(status: 'healthy' | 'degraded' | 'unhealthy'): string {
  switch (status) {
    case 'healthy': return 'bg-green-100 text-green-800'
    case 'degraded': return 'bg-yellow-100 text-yellow-800'
    case 'unhealthy': return 'bg-red-100 text-red-800'
  }
}

function getHealthDotColor(status: 'healthy' | 'degraded' | 'unhealthy'): string {
  switch (status) {
    case 'healthy': return 'bg-green-500'
    case 'degraded': return 'bg-yellow-500'
    case 'unhealthy': return 'bg-red-500'
  }
}

function getCacheHealthEntries(): ServiceHealth[] {
  const entries: ServiceHealth[] = []

  // BGG Cache health
  if (cacheStats.value) {
    const total = cacheStats.value.totalGames
    const status = total > 100 ? 'healthy' : total > 0 ? 'degraded' : 'unhealthy'
    entries.push({
      name: 'BGG Cache',
      status,
      message: `${total.toLocaleString()} games cached`
    })
  }

  // MTG/Scryfall Cache health
  if (mtgCacheStats.value) {
    const total = mtgCacheStats.value.totalCards
    const stale = mtgCacheStats.value.staleCount
    let status: 'healthy' | 'degraded' | 'unhealthy' = 'healthy'
    let message = `${total.toLocaleString()} cards cached`

    if (total === 0) {
      status = 'unhealthy'
      message = 'No cards cached'
    } else if (stale > 50) {
      status = 'degraded'
      message = `${total.toLocaleString()} cards (${stale} stale)`
    }

    entries.push({
      name: 'Scryfall Cache',
      status,
      message
    })
  }

  return entries
}

function updateServiceHealthWithCaches() {
  // Remove any existing cache entries and add fresh ones
  const baseSevices = serviceHealth.value.filter(s => !s.name.includes('Cache'))
  serviceHealth.value = [...baseSevices, ...getCacheHealthEntries()]
}

async function loadCacheStats() {
  cacheLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    cacheStats.value = await getBggCacheStats(token)
    updateServiceHealthWithCaches()
  } catch (err) {
    console.error('Failed to load cache stats:', err)
  } finally {
    cacheLoading.value = false
  }
}

async function handleImportPopular() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importPopularGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleImportHot() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importHotGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import hot games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleRefreshStale() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshStaleCache(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh cache'
  } finally {
    cacheImporting.value = false
  }
}

async function handleRefreshIncomplete() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshIncompleteCache(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh incomplete entries'
  } finally {
    cacheImporting.value = false
  }
}

async function handleRefreshThumbnails() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshBggThumbnails(token)
    successMessage.value = result.message
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh thumbnails'
  } finally {
    cacheImporting.value = false
  }
}

function formatCacheDate(dateStr: string | null): string {
  if (!dateStr) return 'Never'
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

// MTG Cache Functions
async function loadMtgCacheStats() {
  mtgCacheLoading.value = true
  try {
    mtgCacheStats.value = await getMtgCacheStats()
    updateServiceHealthWithCaches()
  } catch (err) {
    console.error('Failed to load MTG cache stats:', err)
  } finally {
    mtgCacheLoading.value = false
  }
}

async function handleWarmMtgStaples() {
  mtgCacheWarming.value = true
  mtgWarmResult.value = null
  mtgWarmProgress.value = 'Starting...'
  errorMessage.value = ''
  let totalCached = 0, totalSkipped = 0, totalFailed = 0
  try {
    const token = await auth.getIdToken()
    if (!token) return

    let offset = 0
    let hasMore = true
    let batchNum = 0
    while (hasMore) {
      batchNum++
      mtgWarmProgress.value = `Batch ${batchNum}: ${totalCached} cached, ${totalSkipped} skipped so far...`
      await new Promise(r => setTimeout(r, 0)) // let Vue re-render
      const result = await warmMtgStaples(token, offset)
      totalCached += result.cached
      totalSkipped += result.skipped
      totalFailed += result.failed
      hasMore = result.hasMore ?? false
      offset = result.nextOffset ?? 0
    }

    mtgWarmResult.value = { action: 'warm-staples', cached: totalCached, skipped: totalSkipped, failed: totalFailed, total: totalCached + totalSkipped + totalFailed }
    mtgWarmProgress.value = ''
    successMessage.value = `Cached ${totalCached} cards (${totalSkipped} skipped, ${totalFailed} failed)`
    await loadMtgCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to warm MTG cache'
    if (totalCached > 0) {
      errorMessage.value += ` (${totalCached} cached before error)`
    }
  } finally {
    mtgCacheWarming.value = false
    mtgWarmProgress.value = ''
  }
}

async function handleWarmMtgCommanders() {
  mtgCacheWarming.value = true
  mtgWarmResult.value = null
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await warmMtgCommanders(token, 5)
    mtgWarmResult.value = result
    successMessage.value = `Cached ${result.cached} commanders (${result.skipped} skipped, ${result.failed} failed)`
    await loadMtgCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to warm commanders'
  } finally {
    mtgCacheWarming.value = false
  }
}

async function handleRefreshMtgStale() {
  mtgCacheWarming.value = true
  mtgWarmResult.value = null
  mtgWarmProgress.value = 'Checking for stale entries...'
  errorMessage.value = ''
  let totalCached = 0, totalSkipped = 0, totalFailed = 0
  try {
    const token = await auth.getIdToken()
    if (!token) return

    let hasMore = true
    let batchNum = 0
    while (hasMore) {
      batchNum++
      mtgWarmProgress.value = `Batch ${batchNum}: ${totalCached} refreshed, ${totalFailed} failed so far...`
      await new Promise(r => setTimeout(r, 0)) // let Vue re-render
      const result = await refreshMtgStaleCache(token)
      totalCached += result.cached
      totalSkipped += result.skipped
      totalFailed += result.failed
      hasMore = result.hasMore ?? false
    }

    mtgWarmResult.value = { action: 'refresh-stale', cached: totalCached, skipped: totalSkipped, failed: totalFailed, total: totalCached + totalSkipped + totalFailed }
    mtgWarmProgress.value = ''
    successMessage.value = `Refreshed ${totalCached} stale entries (${totalSkipped} skipped, ${totalFailed} failed)`
    await loadMtgCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh stale MTG cache'
    if (totalCached > 0) {
      errorMessage.value += ` (${totalCached} refreshed before error)`
    }
  } finally {
    mtgCacheWarming.value = false
    mtgWarmProgress.value = ''
  }
}

// BGG Cache Browse Functions
async function loadCacheGames() {
  cacheGamesLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await listBggCache(token, {
      search: cacheSearch.value || undefined,
      page: cacheGamesPage.value,
      limit: 20,
      filter: cacheFilter.value === 'all' ? undefined : cacheFilter.value,
    })
    cacheGames.value = result.games
    cacheGamesTotal.value = result.total
  } catch (err) {
    console.error('Failed to load cache games:', err)
  } finally {
    cacheGamesLoading.value = false
  }
}

async function handleRefreshSingleGame(bggId: number) {
  refreshingGameId.value = bggId
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshBggGame(token, bggId)
    successMessage.value = result.message
    await loadCacheGames()
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh game'
  } finally {
    refreshingGameId.value = null
  }
}

function openCacheEditDialog(game: BggCacheEntry) {
  editingCacheGame.value = game
  cacheEditForm.thumbnailUrl = game.thumbnailUrl || ''
  cacheEditForm.imageUrl = game.imageUrl || ''
  cacheEditForm.minPlayers = game.minPlayers
  cacheEditForm.maxPlayers = game.maxPlayers
  showCacheEditDialog.value = true
}

function closeCacheEditDialog() {
  showCacheEditDialog.value = false
  editingCacheGame.value = null
}

async function handleSaveCacheGame() {
  if (!editingCacheGame.value) return
  savingCacheGame.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await updateBggCacheEntry(token, editingCacheGame.value.bggId, {
      thumbnailUrl: cacheEditForm.thumbnailUrl || null,
      imageUrl: cacheEditForm.imageUrl || null,
      minPlayers: cacheEditForm.minPlayers,
      maxPlayers: cacheEditForm.maxPlayers,
    })
    successMessage.value = 'Game updated successfully'
    closeCacheEditDialog()
    await loadCacheGames()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update game'
  } finally {
    savingCacheGame.value = false
  }
}

let cacheSearchTimeout: ReturnType<typeof setTimeout> | null = null
function handleCacheSearchInput() {
  if (cacheSearchTimeout) clearTimeout(cacheSearchTimeout)
  cacheSearchTimeout = setTimeout(() => {
    cacheGamesPage.value = 1
    loadCacheGames()
  }, 400)
}

function handleCacheFilterChange() {
  cacheGamesPage.value = 1
  loadCacheGames()
}

function handleCachePageChange(page: number) {
  cacheGamesPage.value = page
  loadCacheGames()
}

// User Management Functions
async function loadUsers() {
  usersLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminUsers(token, {
      search: userSearch.value || undefined,
      suspended: showSuspendedOnly.value || undefined,
      banned: showBannedOnly.value || undefined,
      page: usersPage.value,
      limit: 20,
    })
    users.value = result.users
    usersTotal.value = result.total
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load users'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    usersLoading.value = false
  }
}

function openSuspendDialog(user: AdminUser) {
  userToSuspend.value = user
  suspendReason.value = ''
  showSuspendDialog.value = true
}

async function handleSuspendUser() {
  if (!userToSuspend.value) return

  suspendingUserId.value = userToSuspend.value.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await suspendUser(token, userToSuspend.value.id, suspendReason.value || undefined)
    successMessage.value = `User @${userToSuspend.value.username} suspended`
    showSuspendDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to suspend user'
  } finally {
    suspendingUserId.value = null
  }
}

async function handleUnsuspendUser(user: AdminUser) {
  if (!confirm(`Unsuspend @${user.username}?`)) return

  suspendingUserId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await unsuspendUser(token, user.id)
    successMessage.value = `User @${user.username} unsuspended`
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to unsuspend user'
  } finally {
    suspendingUserId.value = null
  }
}

async function openUserEditDialog(user: AdminUser) {
  editingUser.value = user
  userEditForm.displayName = user.displayName || ''
  userEditForm.username = user.username
  userEditForm.isAdmin = user.isAdmin
  userEditForm.isFoundingMember = user.isFoundingMember || false
  userEditForm.tier = user.subscriptionOverrideTier || user.subscriptionTier || 'free'
  userEditForm.tierReason = user.subscriptionOverrideReason || ''
  showUserEditDialog.value = true

  // Load badges
  badgesLoading.value = true
  userBadgeIds.value = new Set()
  try {
    const [badges, earned] = await Promise.all([
      allBadgesList.value.length ? Promise.resolve(allBadgesList.value) : getAllBadges(),
      getUserBadges(user.id),
    ])
    allBadgesList.value = badges
    userBadgeIds.value = new Set(earned.map(b => b.badge_id))
  } catch (err) {
    console.error('Failed to load badges:', err)
  } finally {
    badgesLoading.value = false
  }
}

async function toggleUserBadge(badgeId: number) {
  if (!editingUser.value) return
  badgeUpdating.value = badgeId
  try {
    const token = await auth.getIdToken()
    if (!token) return
    if (userBadgeIds.value.has(badgeId)) {
      await adminRevokeBadge(token, editingUser.value.id, badgeId)
      userBadgeIds.value.delete(badgeId)
    } else {
      await adminAwardBadge(token, editingUser.value.id, badgeId)
      userBadgeIds.value.add(badgeId)
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update badge'
  } finally {
    badgeUpdating.value = null
  }
}

async function editSignupUser(username: string) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminUsers(token, { search: username, limit: 1 })
    const user = result.users[0]
    if (user) {
      openUserEditDialog(user)
    } else {
      errorMessage.value = 'User not found'
    }
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load user'
  }
}

async function handleSaveUser() {
  if (!editingUser.value) return
  if (!userEditForm.username.trim()) {
    errorMessage.value = 'Username is required'
    return
  }

  savingUser.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    // Update basic user info including founding member status
    await updateUser(token, editingUser.value.id, {
      displayName: userEditForm.displayName.trim() || undefined,
      username: userEditForm.username.trim(),
      isAdmin: userEditForm.isAdmin,
      isFoundingMember: userEditForm.isFoundingMember,
    })

    // Check if tier changed and update if needed
    const currentTier = editingUser.value.subscriptionOverrideTier || editingUser.value.subscriptionTier || 'free'
    if (userEditForm.tier !== currentTier) {
      // If setting to free, we're removing the override (pass null)
      const newTier = userEditForm.tier === 'free' ? null : userEditForm.tier
      await setUserTier(token, editingUser.value.id, newTier, userEditForm.tierReason || undefined)
    }

    successMessage.value = `User @${userEditForm.username} updated`
    showUserEditDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update user'
  } finally {
    savingUser.value = false
  }
}

async function handleDeleteUser(user: AdminUser) {
  if (!confirm(`DELETE user @${user.username}?\n\nThis will permanently delete:\n- All their group memberships\n- All their event registrations\n- All their LFP posts\n- All their planning session data\n\nThis action CANNOT be undone.`)) return
  if (!confirm(`Are you ABSOLUTELY SURE you want to delete @${user.username}? Type "delete" in the next prompt would be safer, but this is a second confirmation.`)) return

  deletingUserId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await deleteUser(token, user.id)
    successMessage.value = result.message
    await loadUsers()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete user'
  } finally {
    deletingUserId.value = null
  }
}

async function handleSendPasswordReset(user: AdminUser) {
  if (!confirm(`Send password reset email to ${user.email}?\n\nNote: This will only work if the user signed up with email/password. OAuth users (Google, etc.) cannot reset their password this way.`)) return

  sendingPasswordReset.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await sendPasswordReset(token, user.id)
    successMessage.value = result.message
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to send password reset'
  } finally {
    sendingPasswordReset.value = null
  }
}

// Ban management functions
function openBanDialog(user: AdminUser) {
  userToBan.value = user
  banReason.value = ''
  showBanDialog.value = true
}

async function handleBanUser() {
  if (!userToBan.value) return

  banningUserId.value = userToBan.value.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await banUser(token, userToBan.value.id, banReason.value || undefined)
    successMessage.value = result.message
    showBanDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to ban user'
  } finally {
    banningUserId.value = null
  }
}

async function handleUnbanUser(user: AdminUser) {
  if (!confirm(`Unban @${user.username}?\n\nThis will restore their account access.`)) return

  banningUserId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await unbanUser(token, user.id)
    successMessage.value = result.message
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to unban user'
  } finally {
    banningUserId.value = null
  }
}

// Tier management functions
function openTierDialog(user: AdminUser) {
  userToSetTier.value = user
  tierForm.tier = user.subscriptionOverrideTier || user.subscriptionTier || 'free'
  tierForm.reason = user.subscriptionOverrideReason || ''
  showTierDialog.value = true
}

async function handleSetTier() {
  if (!userToSetTier.value) return

  settingTierId.value = userToSetTier.value.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    // If setting to free, we're removing the override
    const tier = tierForm.tier === 'free' ? null : tierForm.tier
    const result = await setUserTier(token, userToSetTier.value.id, tier, tierForm.reason || undefined)
    successMessage.value = result.message
    showTierDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to set tier'
  } finally {
    settingTierId.value = null
  }
}

async function handleToggleFounding(user: AdminUser) {
  const newStatus = !user.isFoundingMember
  const action = newStatus ? 'grant Founding Member badge to' : 'remove Founding Member badge from'
  if (!confirm(`${action} @${user.username}?`)) return

  togglingFoundingId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await toggleFoundingMember(token, user.id, newStatus)
    successMessage.value = result.message
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update founding member status'
  } finally {
    togglingFoundingId.value = null
  }
}

function getTierBadgeClass(tier: string): string {
  switch (tier) {
    case 'premium':
      return 'bg-yellow-100 text-yellow-800'
    case 'pro':
      return 'bg-purple-100 text-purple-800'
    case 'basic':
      return 'bg-blue-100 text-blue-800'
    default:
      return 'bg-gray-100 text-gray-600'
  }
}

let userSearchTimeout: ReturnType<typeof setTimeout> | null = null
function handleUserSearch() {
  if (userSearchTimeout) clearTimeout(userSearchTimeout)
  userSearchTimeout = setTimeout(() => {
    usersPage.value = 1
    loadUsers()
  }, 300)
}

// Group Management Functions
async function loadEvents() {
  eventsLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminEvents(token, {
      search: eventsSearch.value || undefined,
      page: eventsPage.value,
      limit: 20,
      showPast: eventsShowPast.value,
    })
    adminEvents.value = result.events
    eventsTotal.value = result.total
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load events'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    eventsLoading.value = false
  }
}

async function handleDeleteEvent(event: AdminEvent) {
  if (!confirm(`Delete event "${event.title}"? This will remove all registrations and cannot be undone.`)) return

  deletingEventId.value = event.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAdminEvent(token, event.id)
    successMessage.value = `Event "${event.title}" deleted`
    await loadEvents()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete event'
  } finally {
    deletingEventId.value = null
  }
}

async function loadGroups() {
  groupsLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminGroups(token, {
      search: groupSearch.value || undefined,
      page: groupsPage.value,
      limit: 20,
    })
    groups.value = result.groups
    groupsTotal.value = result.total
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load groups'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    groupsLoading.value = false
  }
}

async function handleDeleteGroup(group: AdminGroup) {
  if (!confirm(`Delete group "${group.name}"? This will remove all members and cannot be undone.`)) return

  deletingGroupId.value = group.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteGroup(token, group.id)
    successMessage.value = `Group "${group.name}" deleted`
    await loadGroups()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete group'
  } finally {
    deletingGroupId.value = null
  }
}

let groupSearchTimeout: ReturnType<typeof setTimeout> | null = null
function handleGroupSearch() {
  if (groupSearchTimeout) clearTimeout(groupSearchTimeout)
  groupSearchTimeout = setTimeout(() => {
    groupsPage.value = 1
    loadGroups()
  }, 300)
}

// ============ Group Member Functions ============

async function openMembersDialog(group: AdminGroup) {
  selectedGroup.value = group
  showMembersDialog.value = true
  await loadGroupMembers(group.id)
}

async function loadGroupMembers(groupId: string) {
  membersLoading.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getGroupMembers(token, groupId)
    groupMembers.value = result.members
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load members'
  } finally {
    membersLoading.value = false
  }
}

async function handleRemoveMember(member: AdminGroupMember) {
  if (!selectedGroup.value) return
  if (!confirm(`Remove @${member.username || member.displayName || 'this user'} from "${selectedGroup.value.name}"?`)) return

  removingMemberId.value = member.userId
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await removeGroupMember(token, selectedGroup.value.id, member.userId)
    successMessage.value = `Member removed from group`
    await loadGroupMembers(selectedGroup.value.id)
    // Update member count in the groups list
    const groupIndex = groups.value.findIndex(g => g.id === selectedGroup.value?.id)
    if (groupIndex >= 0 && groups.value[groupIndex]) {
      groups.value[groupIndex]!.memberCount--
    }
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to remove member'
  } finally {
    removingMemberId.value = null
  }
}

async function handleChangeRole(member: AdminGroupMember, newRole: 'owner' | 'admin' | 'member') {
  if (!selectedGroup.value) return

  changingRoleId.value = member.userId
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await changeGroupMemberRole(token, selectedGroup.value.id, member.userId, newRole)
    successMessage.value = `Role changed to ${newRole}`
    await loadGroupMembers(selectedGroup.value.id)
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to change role'
  } finally {
    changingRoleId.value = null
  }
}

function openAddMemberDialog() {
  showAddMemberDialog.value = true
  addMemberSearch.value = ''
  addMemberResults.value = []
}

let addMemberSearchTimeout: ReturnType<typeof setTimeout> | null = null
async function handleAddMemberSearch() {
  if (addMemberSearchTimeout) clearTimeout(addMemberSearchTimeout)
  if (!addMemberSearch.value.trim()) {
    addMemberResults.value = []
    return
  }
  addMemberSearchTimeout = setTimeout(async () => {
    addMemberSearching.value = true
    try {
      const token = await auth.getIdToken()
      if (!token) return
      const result = await getAdminUsers(token, { search: addMemberSearch.value, limit: 10 })
      // Filter out users who are already members
      const memberIds = new Set(groupMembers.value.map(m => m.userId))
      addMemberResults.value = result.users.filter(u => !memberIds.has(u.id))
    } catch (err) {
      console.error('Failed to search users:', err)
    } finally {
      addMemberSearching.value = false
    }
  }, 300)
}

async function handleAddMember(user: AdminUser) {
  if (!selectedGroup.value) return

  addingMemberId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await addGroupMember(token, selectedGroup.value.id, user.id, 'member')
    successMessage.value = `@${user.username} added to group`
    showAddMemberDialog.value = false
    addMemberSearch.value = ''
    addMemberResults.value = []
    await loadGroupMembers(selectedGroup.value.id)
    // Update member count in the groups list
    const groupIndex = groups.value.findIndex(g => g.id === selectedGroup.value?.id)
    if (groupIndex >= 0 && groups.value[groupIndex]) {
      groups.value[groupIndex]!.memberCount++
    }
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to add member'
  } finally {
    addingMemberId.value = null
  }
}

function getRoleBadgeClass(role: string) {
  switch (role) {
    case 'owner': return 'bg-purple-100 text-purple-800'
    case 'admin': return 'bg-blue-100 text-blue-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

// ============ Notes Functions ============

async function loadNotes() {
  notesLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminNotes(token)
    notes.value = result.notes
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load notes'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    notesLoading.value = false
  }
}

function openNoteDialog(note?: AdminNote) {
  if (note) {
    editingNote.value = note
    noteForm.title = note.title
    noteForm.content = note.content
    noteForm.category = note.category
  } else {
    editingNote.value = null
    noteForm.title = ''
    noteForm.content = ''
    noteForm.category = 'general'
  }
  showNoteDialog.value = true
}

async function handleSaveNote() {
  if (!noteForm.title.trim() || !noteForm.content.trim()) {
    errorMessage.value = 'Title and content are required'
    return
  }

  savingNote.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingNote.value) {
      await updateAdminNote(token, editingNote.value.id, {
        title: noteForm.title.trim(),
        content: noteForm.content.trim(),
        category: noteForm.category.trim(),
      })
      successMessage.value = 'Note updated'
    } else {
      await createAdminNote(token, {
        title: noteForm.title.trim(),
        content: noteForm.content.trim(),
        category: noteForm.category.trim(),
      })
      successMessage.value = 'Note created'
    }

    showNoteDialog.value = false
    await loadNotes()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save note'
  } finally {
    savingNote.value = false
  }
}

async function handleTogglePin(note: AdminNote) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await updateAdminNote(token, note.id, { isPinned: !note.isPinned })
    await loadNotes()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update note'
  }
}

async function handleToggleImplemented(note: AdminNote) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await updateAdminNote(token, note.id, { isImplemented: !note.isImplemented })
    await loadNotes()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update note'
  }
}

async function handleDeleteNote(note: AdminNote) {
  if (!confirm(`Delete note "${note.title}"?`)) return

  deletingNoteId.value = note.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAdminNote(token, note.id)
    successMessage.value = 'Note deleted'
    await loadNotes()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete note'
  } finally {
    deletingNoteId.value = null
  }
}

// ============ Bugs Functions ============

async function loadBugs() {
  bugsLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminBugs(token, {
      status: bugStatusFilter.value || undefined,
      priority: bugPriorityFilter.value || undefined,
    })
    bugs.value = result.bugs
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load bugs'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    bugsLoading.value = false
  }
}

function openBugDialog(bug?: AdminBug) {
  if (bug) {
    editingBug.value = bug
    bugForm.title = bug.title
    bugForm.description = bug.description || ''
    bugForm.stepsToReproduce = bug.stepsToReproduce || ''
    bugForm.priority = bug.priority
    bugForm.status = bug.status
  } else {
    editingBug.value = null
    bugForm.title = ''
    bugForm.description = ''
    bugForm.stepsToReproduce = ''
    bugForm.priority = 'medium'
    bugForm.status = 'open'
  }
  showBugDialog.value = true
}

async function handleSaveBug() {
  if (!bugForm.title.trim()) {
    errorMessage.value = 'Title is required'
    return
  }

  savingBug.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingBug.value) {
      await updateAdminBug(token, editingBug.value.id, {
        title: bugForm.title.trim(),
        description: bugForm.description.trim() || undefined,
        stepsToReproduce: bugForm.stepsToReproduce.trim() || undefined,
        priority: bugForm.priority,
        status: bugForm.status,
      })
      successMessage.value = 'Bug updated'
    } else {
      await createAdminBug(token, {
        title: bugForm.title.trim(),
        description: bugForm.description.trim() || undefined,
        stepsToReproduce: bugForm.stepsToReproduce.trim() || undefined,
        priority: bugForm.priority,
      })
      successMessage.value = 'Bug reported'
    }

    showBugDialog.value = false
    await loadBugs()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save bug'
  } finally {
    savingBug.value = false
  }
}

async function handleDeleteBug(bug: AdminBug) {
  if (!confirm(`Delete bug "${bug.title}"?`)) return

  deletingBugId.value = bug.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAdminBug(token, bug.id)
    successMessage.value = 'Bug deleted'
    await loadBugs()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete bug'
  } finally {
    deletingBugId.value = null
  }
}

function getPriorityColor(priority: string): string {
  switch (priority) {
    case 'critical': return 'bg-red-100 text-red-800'
    case 'high': return 'bg-orange-100 text-orange-800'
    case 'medium': return 'bg-yellow-100 text-yellow-800'
    case 'low': return 'bg-gray-100 text-gray-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'open': return 'bg-blue-100 text-blue-800'
    case 'in_progress': return 'bg-purple-100 text-purple-800'
    case 'resolved': return 'bg-green-100 text-green-800'
    case 'closed': return 'bg-gray-100 text-gray-800'
    case 'wont_fix': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-800'
  }
}

// ============ Chat Reports Functions ============

async function loadChatReports() {
  chatReportsLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    chatReports.value = await getChatReports(token, {
      status: chatReportStatusFilter.value || undefined,
      limit: 50,
    })
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load chat reports'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    chatReportsLoading.value = false
  }
}

function openModerationDialog(report: ChatReport) {
  selectedReport.value = report
  moderationForm.action = 'warning'
  moderationForm.reason = ''
  moderationHistory.value = []
  showModerationDialog.value = true

  // Load moderation history for this user
  if (report.message?.user?.id) {
    loadModerationHistory(report.message.user.id)
  }
}

async function loadModerationHistory(userId: string) {
  moderationHistoryLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    moderationHistory.value = await getChatModerationHistory(token, userId)
  } catch (err) {
    console.error('Failed to load moderation history:', err)
  } finally {
    moderationHistoryLoading.value = false
  }
}

async function handleIssueModeration() {
  if (!selectedReport.value?.message?.user?.id) return
  if (!moderationForm.reason.trim()) {
    errorMessage.value = 'Reason is required'
    return
  }

  issuingModeration.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await issueModerationAction(
      token,
      selectedReport.value.message.user.id,
      moderationForm.action,
      moderationForm.reason.trim(),
      selectedReport.value.id
    )

    successMessage.value = `Moderation action issued: ${MODERATION_ACTION_LABELS[moderationForm.action]}`
    showModerationDialog.value = false
    await loadChatReports()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to issue moderation action'
  } finally {
    issuingModeration.value = false
  }
}

async function handleDismissReport(report: ChatReport) {
  if (!confirm('Dismiss this report? The message will remain visible.')) return

  dismissingReportId.value = report.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await reviewChatReport(token, report.id, 'dismissed')
    successMessage.value = 'Report dismissed'
    await loadChatReports()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to dismiss report'
  } finally {
    dismissingReportId.value = null
  }
}

function getContextLabel(contextType: string): string {
  switch (contextType) {
    case 'event': return 'Event Chat'
    case 'group': return 'Group Chat'
    case 'planning': return 'Planning Session'
    default: return contextType
  }
}

function getReportStatusColor(status: string): string {
  switch (status) {
    case 'pending': return 'bg-orange-100 text-orange-800'
    case 'reviewed': return 'bg-blue-100 text-blue-800'
    case 'action_taken': return 'bg-green-100 text-green-800'
    case 'dismissed': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-800'
  }
}

// ============ Ads Functions ============

async function loadAds() {
  adsLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const [adsResult, statsResult] = await Promise.all([
      getAllAds(token),
      getAdStats(token),
    ])
    ads.value = adsResult
    adStats.value = statsResult
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load ads'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    adsLoading.value = false
  }
}

function openAdDialog(ad?: Ad) {
  editingAd.value = ad || null
  if (ad) {
    adForm.name = ad.name
    adForm.advertiserName = ad.advertiserName || ''
    adForm.adType = ad.adType as 'banner' | 'sidebar' | 'featured'
    adForm.placement = ad.placement as 'general' | 'dashboard' | 'events' | 'groups'
    adForm.imageUrl = ad.imageUrl || ''
    adForm.title = ad.title || ''
    adForm.description = ad.description || ''
    adForm.linkUrl = ad.linkUrl
    adForm.targetCity = ad.targetCity || ''
    adForm.targetState = ad.targetState || ''
    adForm.startDate = ad.startDate?.split('T')[0] || ''
    adForm.endDate = ad.endDate?.split('T')[0] || ''
    adForm.isActive = ad.isActive
    adForm.isHouseAd = ad.isHouseAd
    adForm.priority = ad.priority
  } else {
    adForm.name = ''
    adForm.advertiserName = ''
    adForm.adType = 'banner'
    adForm.placement = 'general'
    adForm.imageUrl = ''
    adForm.title = ''
    adForm.description = ''
    adForm.linkUrl = ''
    adForm.targetCity = ''
    adForm.targetState = ''
    adForm.startDate = ''
    adForm.endDate = ''
    adForm.isActive = true
    adForm.isHouseAd = false
    adForm.priority = 0
  }
  showAdDialog.value = true
}

async function handleSaveAd() {
  if (!adForm.name.trim()) {
    errorMessage.value = 'Ad name is required'
    return
  }
  if (!adForm.linkUrl.trim()) {
    errorMessage.value = 'Link URL is required'
    return
  }

  savingAd.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const data: CreateAdInput = {
      name: adForm.name.trim(),
      advertiserName: adForm.advertiserName.trim() || undefined,
      adType: adForm.adType,
      placement: adForm.placement,
      imageUrl: adForm.imageUrl.trim() || undefined,
      title: adForm.title.trim() || undefined,
      description: adForm.description.trim() || undefined,
      linkUrl: adForm.linkUrl.trim(),
      targetCity: adForm.targetCity.trim() || undefined,
      targetState: adForm.targetState.trim() || undefined,
      startDate: adForm.startDate || undefined,
      endDate: adForm.endDate || undefined,
      isActive: adForm.isActive,
      isHouseAd: adForm.isHouseAd,
      priority: adForm.priority,
    }

    if (editingAd.value) {
      await updateAd(token, editingAd.value.id, data)
      successMessage.value = 'Ad updated'
    } else {
      await createAd(token, data)
      successMessage.value = 'Ad created'
    }

    showAdDialog.value = false
    await loadAds()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save ad'
  } finally {
    savingAd.value = false
  }
}

async function handleDeleteAd(ad: Ad) {
  if (!confirm(`Delete ad "${ad.name}"?`)) return

  deletingAdId.value = ad.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAd(token, ad.id)
    successMessage.value = 'Ad deleted'
    await loadAds()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete ad'
  } finally {
    deletingAdId.value = null
  }
}

async function handleToggleAdActive(ad: Ad) {
  togglingAdId.value = ad.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await toggleAdActive(token, ad.id, !ad.isActive)
    await loadAds()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to toggle ad status'
  } finally {
    togglingAdId.value = null
  }
}

function getAdStatsById(adId: string): AdStats | undefined {
  return adStats.value.find(s => s.id === adId)
}

// Raffle functions
async function loadRaffles() {
  rafflesLoading.value = true
  errorMessage.value = ''
  const startTime = Date.now()
  try {
    const token = await auth.getIdToken()
    if (!token) return
    raffles.value = await getAllRaffles(token)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load raffles'
  } finally {
    const elapsed = Date.now() - startTime
    if (elapsed < MIN_LOADING_TIME) {
      await new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME - elapsed))
    }
    rafflesLoading.value = false
  }
}

function openRaffleDialog(raffle?: RaffleWithDetails) {
  if (raffle) {
    editingRaffle.value = raffle
    raffleForm.title = raffle.title
    raffleForm.description = raffle.description || ''
    raffleForm.prizeName = raffle.prizeName
    raffleForm.prizeDescription = raffle.prizeDescription || ''
    raffleForm.prizeImageUrl = raffle.prizeImageUrl || ''
    raffleForm.prizeBggId = raffle.prizeBggId
    raffleForm.prizeValueCents = raffle.prizeValueCents
    raffleForm.startDate = raffle.startDate ? new Date(raffle.startDate).toISOString().slice(0, 16) : ''
    raffleForm.endDate = raffle.endDate ? new Date(raffle.endDate).toISOString().slice(0, 16) : ''
    raffleForm.termsConditions = raffle.termsConditions || ''
    raffleForm.mailInInstructions = raffle.mailInInstructions || ''
    raffleForm.bannerImageUrl = raffle.bannerImageUrl || ''
    raffleForm.status = raffle.status
    prizeGameSearch.value = raffle.prizeName
    // Create a partial game object for display if we have the data
    if (raffle.prizeBggId) {
      selectedPrizeGame.value = {
        bggId: raffle.prizeBggId,
        name: raffle.prizeName,
        thumbnailUrl: raffle.prizeImageUrl,
        imageUrl: raffle.prizeImageUrl,
        yearPublished: null,
        minPlayers: null,
        maxPlayers: null,
        minPlaytime: null,
        maxPlaytime: null,
        playingTime: null,
        weight: null,
        description: raffle.prizeDescription,
        categories: [],
        mechanics: [],
      }
    } else {
      selectedPrizeGame.value = null
    }
  } else {
    editingRaffle.value = null
    raffleForm.title = ''
    raffleForm.description = ''
    raffleForm.prizeName = ''
    raffleForm.prizeDescription = ''
    raffleForm.prizeImageUrl = ''
    raffleForm.prizeBggId = null
    raffleForm.prizeValueCents = null
    raffleForm.startDate = ''
    raffleForm.endDate = ''
    raffleForm.termsConditions = ''
    raffleForm.mailInInstructions = ''
    raffleForm.bannerImageUrl = ''
    raffleForm.status = 'draft'
    selectedPrizeGame.value = null
    prizeGameSearch.value = ''
  }
  showRaffleDialog.value = true
}

function handlePrizeGameSelect(game: BggGame) {
  selectedPrizeGame.value = game
  raffleForm.prizeName = game.name
  raffleForm.prizeBggId = game.bggId
  raffleForm.prizeImageUrl = game.imageUrl || game.thumbnailUrl || ''
  if (game.description) {
    // Strip HTML tags and truncate
    const plainText = game.description.replace(/<[^>]*>/g, '').trim()
    raffleForm.prizeDescription = plainText.length > 500 ? plainText.slice(0, 500) + '...' : plainText
  }
}

function clearPrizeGame() {
  selectedPrizeGame.value = null
  prizeGameSearch.value = ''
  raffleForm.prizeName = ''
  raffleForm.prizeBggId = null
  raffleForm.prizeImageUrl = ''
  raffleForm.prizeDescription = ''
}

async function handleSaveRaffle() {
  if (!raffleForm.title || !raffleForm.prizeName || !raffleForm.startDate || !raffleForm.endDate) {
    errorMessage.value = 'Title, prize name, start date, and end date are required'
    return
  }

  savingRaffle.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const input: CreateRaffleInput = {
      title: raffleForm.title,
      description: raffleForm.description || undefined,
      prizeName: raffleForm.prizeName,
      prizeDescription: raffleForm.prizeDescription || undefined,
      prizeImageUrl: raffleForm.prizeImageUrl || undefined,
      prizeBggId: raffleForm.prizeBggId || undefined,
      prizeValueCents: raffleForm.prizeValueCents || undefined,
      startDate: new Date(raffleForm.startDate).toISOString(),
      endDate: new Date(raffleForm.endDate).toISOString(),
      termsConditions: raffleForm.termsConditions || undefined,
      mailInInstructions: raffleForm.mailInInstructions || undefined,
      bannerImageUrl: raffleForm.bannerImageUrl || undefined,
      status: raffleForm.status,
    }

    if (editingRaffle.value) {
      await updateRaffle(token, editingRaffle.value.id, input)
      successMessage.value = 'Raffle updated'
    } else {
      await createRaffle(token, input)
      successMessage.value = 'Raffle created'
    }
    showRaffleDialog.value = false
    await loadRaffles()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save raffle'
  } finally {
    savingRaffle.value = false
  }
}

async function handleDeleteRaffle(raffle: RaffleWithDetails) {
  if (!confirm(`Delete raffle "${raffle.title}"? This cannot be undone.`)) return

  deletingRaffleId.value = raffle.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteRaffle(token, raffle.id)
    successMessage.value = 'Raffle deleted'
    await loadRaffles()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete raffle'
  } finally {
    deletingRaffleId.value = null
  }
}

async function handleSelectWinner(raffle: RaffleWithDetails) {
  if (!confirm(`Select a random winner for "${raffle.title}"? This will end the raffle.`)) return

  selectingWinnerId.value = raffle.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await selectRaffleWinner(token, raffle.id)
    successMessage.value = `Winner selected: ${result.winner.displayName || result.winner.email} (${result.winnerEntries}/${result.totalEntries} entries)`
    await loadRaffles()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to select winner'
  } finally {
    selectingWinnerId.value = null
  }
}

async function handleMarkNotified(raffle: RaffleWithDetails) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await markWinnerNotified(token, raffle.id)
    successMessage.value = 'Winner marked as notified'
    await loadRaffles()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update'
  }
}

async function handleMarkClaimed(raffle: RaffleWithDetails) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await markPrizeClaimed(token, raffle.id)
    successMessage.value = 'Prize marked as claimed'
    await loadRaffles()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update'
  }
}

async function openRaffleEntries(raffle: RaffleWithDetails) {
  selectedRaffleForEntries.value = raffle
  showRaffleEntries.value = true
  raffleEntriesLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    raffleEntries.value = await getRaffleEntries(token, raffle.id)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load entries'
  } finally {
    raffleEntriesLoading.value = false
  }
}

function getRaffleStatusColor(status: RaffleStatus): string {
  switch (status) {
    case 'draft': return 'bg-gray-100 text-gray-600'
    case 'active': return 'bg-green-100 text-green-800'
    case 'ended': return 'bg-blue-100 text-blue-800'
    case 'cancelled': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-600'
  }
}

function canSelectWinner(raffle: RaffleWithDetails): boolean {
  return raffle.status === 'active' && new Date(raffle.endDate) <= new Date()
}

async function loadLocations() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const [allLocations, pending] = await Promise.all([
      getAllLocations(token),
      getPendingLocations(token),
    ])
    locations.value = allLocations
    pendingLocations.value = pending
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load locations'
  } finally {
    loading.value = false
  }
}

async function handleApprove(location: EventLocation) {
  approvingId.value = location.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const approved = await approveLocation(token, location.id)
    // Move from pending to main list
    pendingLocations.value = pendingLocations.value.filter(l => l.id !== location.id)
    locations.value.unshift(approved)
    successMessage.value = `Approved "${location.name}"`
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to approve location'
  } finally {
    approvingId.value = null
  }
}

async function handleReject(location: EventLocation) {
  if (!confirm(`Reject "${location.name}"? This venue will not be available for users.`)) return

  rejectingId.value = location.id
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await rejectLocation(token, location.id)
    pendingLocations.value = pendingLocations.value.filter(l => l.id !== location.id)
    successMessage.value = `Rejected "${location.name}"`
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to reject location'
  } finally {
    rejectingId.value = null
  }
}

function openCreateDialog() {
  editingLocation.value = null
  form.name = ''
  form.addressLine1 = ''
  form.city = ''
  form.state = ''
  form.venue = ''
  form.locationType = 'temporary'
  form.startDate = ''
  form.endDate = ''
  form.recurringDays = []
  showDialog.value = true
}

function openEditDialog(location: EventLocation) {
  editingLocation.value = location
  form.name = location.name
  form.addressLine1 = location.addressLine1 || ''
  form.city = location.city
  form.state = location.state
  form.venue = location.venue || ''
  // Determine location type from data
  if (location.isPermanent) {
    form.locationType = 'permanent'
  } else if (location.recurringDays && location.recurringDays.length > 0) {
    form.locationType = 'recurring'
  } else {
    form.locationType = 'temporary'
  }
  form.startDate = location.startDate || ''
  form.endDate = location.endDate || ''
  form.recurringDays = location.recurringDays ? [...location.recurringDays] : []
  showDialog.value = true
}

function toggleRecurringDay(day: number) {
  const idx = form.recurringDays.indexOf(day)
  if (idx >= 0) {
    form.recurringDays.splice(idx, 1)
  } else {
    form.recurringDays.push(day)
    form.recurringDays.sort((a, b) => a - b)
  }
}

async function handleSave() {
  // Basic validation
  if (!form.name.trim() || !form.city.trim() || !form.state.trim()) {
    errorMessage.value = 'Name, city, and state are required'
    return
  }

  // Type-specific validation
  if (form.locationType === 'temporary' && (!form.startDate || !form.endDate)) {
    errorMessage.value = 'Start and end dates are required for temporary locations'
    return
  }
  if (form.locationType === 'recurring' && form.recurringDays.length === 0) {
    errorMessage.value = 'Select at least one recurring day'
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const payload = {
      name: form.name.trim(),
      addressLine1: form.addressLine1.trim() || undefined,
      city: form.city.trim(),
      state: form.state.trim(),
      venue: form.venue.trim() || undefined,
      isPermanent: form.locationType === 'permanent',
      recurringDays: form.locationType === 'recurring' ? form.recurringDays : null,
      startDate: form.locationType === 'temporary' ? form.startDate : null,
      endDate: form.locationType === 'temporary' ? form.endDate : null,
    }

    if (editingLocation.value) {
      // Update existing
      const updated = await updateLocation(token, editingLocation.value.id, payload)
      const index = locations.value.findIndex(l => l.id === updated.id)
      if (index >= 0) locations.value[index] = updated
      successMessage.value = 'Location updated'
    } else {
      // Create new
      const created = await createLocation(token, payload)
      locations.value.unshift(created)
      successMessage.value = 'Location created'
    }

    showDialog.value = false
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save location'
  } finally {
    saving.value = false
  }
}

async function handleDelete(location: EventLocation) {
  if (!confirm(`Delete "${location.name}"? Any LFP posts using this location will have it removed.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteLocation(token, location.id)
    locations.value = locations.value.filter(l => l.id !== location.id)
    successMessage.value = 'Location deleted'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete location'
  }
}

function toggleMergeMode() {
  mergeMode.value = !mergeMode.value
  selectedForMerge.value = []
}

function toggleSelectForMerge(id: string) {
  if (selectedForMerge.value.includes(id)) {
    selectedForMerge.value = selectedForMerge.value.filter(i => i !== id)
  } else {
    selectedForMerge.value.push(id)
  }
}

async function handleMerge() {
  if (selectedForMerge.value.length < 2) {
    errorMessage.value = 'Select at least 2 locations to merge'
    return
  }

  const keepId = selectedForMerge.value[0]!
  const removeIds = selectedForMerge.value.slice(1)
  const keepLocation = locations.value.find(l => l.id === keepId)

  if (!confirm(`Keep "${keepLocation?.name ?? 'selected'}" and merge ${removeIds.length} other location(s) into it? This will update all LFP posts to use the kept location.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await mergeLocations(token, keepId, removeIds)
    locations.value = locations.value.filter(l => !removeIds.includes(l.id))
    selectedForMerge.value = []
    mergeMode.value = false
    successMessage.value = `Merged ${removeIds.length} location(s)`
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to merge locations'
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function isLocationExpired(location: EventLocation): boolean {
  // Permanent locations never expire
  if (location.isPermanent) return false
  // Recurring locations never expire
  if (location.recurringDays && location.recurringDays.length > 0) return false
  // No end date means not expired
  if (!location.endDate) return false
  return new Date(location.endDate) < new Date()
}

function getLocationTypeLabel(location: EventLocation): string {
  if (location.isPermanent) return 'Permanent'
  if (location.recurringDays && location.recurringDays.length > 0) {
    const days = location.recurringDays.map(d => dayNames[d]).join(', ')
    return `Recurring: ${days}`
  }
  return 'Event'
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 py-8 overflow-x-hidden">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Site Administration</h1>
    </div>

    <!-- Tabs -->
    <div class="flex gap-0.5 mb-6 border-b border-gray-200 overflow-x-auto -mx-4 px-4 scrollbar-hide">
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'dashboard'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'dashboard'"
      >
        Dashboard
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'users'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'users'; loadUsers()"
      >
        Users
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'groups'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'groups'; loadGroups()"
      >
        Groups
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'events'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'events'; loadEvents()"
      >
        Events
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'notes'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'notes'; loadNotes()"
      >
        Notes
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'bugs'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'bugs'; loadBugs()"
      >
        Bugs
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'chatReports'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'chatReports'; loadChatReports()"
      >
        Chat Reports
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'ads'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'ads'; loadAds()"
      >
        Ads
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'raffles'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'raffles'; loadRaffles()"
      >
        Raffles
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'locations'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'locations'"
      >
        Locations
      </button>
      <button
        class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
        :class="activeTab === 'caches'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'caches'"
      >
        Caches
      </button>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- Dashboard Tab -->
    <div v-if="activeTab === 'dashboard'">
      <!-- Loading State -->
      <div v-if="dashboardLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <!-- Stats Section -->
        <div class="mb-8">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">Site Statistics</h2>
            <div class="flex items-center gap-3">
              <span v-if="lastRefresh" class="text-sm text-gray-500">
                Last updated: {{ lastRefresh.toLocaleTimeString() }}
              </span>
              <button
                class="btn-outline text-sm"
                :disabled="dashboardLoading"
                @click="loadDashboard"
              >
                Refresh
              </button>
            </div>
          </div>

          <div v-if="dashboardStats" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <!-- Users -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-blue-100">
                  <svg class="w-6 h-6 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.users.total }}</div>
                  <div class="text-sm text-gray-500">Total Users</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                <span class="text-green-600 font-medium">+{{ dashboardStats.users.last7Days }}</span> last 7 days
              </div>
            </div>

            <!-- Groups -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-purple-100">
                  <svg class="w-6 h-6 text-purple-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.groups.total }}</div>
                  <div class="text-sm text-gray-500">Groups</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.groups.public }} public, {{ dashboardStats.groups.private }} private
              </div>
            </div>

            <!-- Events -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-green-100">
                  <svg class="w-6 h-6 text-green-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.events.total }}</div>
                  <div class="text-sm text-gray-500">Events</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.events.upcoming }} upcoming
              </div>
            </div>

            <!-- Planning Sessions -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-orange-100">
                  <svg class="w-6 h-6 text-orange-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M17,12V3A1,1 0 0,0 16,2H3A1,1 0 0,0 2,3V17L6,13H16A1,1 0 0,0 17,12M21,6H19V15H6V17A1,1 0 0,0 7,18H18L22,22V7A1,1 0 0,0 21,6Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.planningSessions.total }}</div>
                  <div class="text-sm text-gray-500">Planning Sessions</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.planningSessions.open }} open
              </div>
            </div>

            <!-- LFP Requests -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-pink-100">
                  <svg class="w-6 h-6 text-pink-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M15.5,12C18,12 20,14 20,16.5C20,17.38 19.75,18.21 19.31,18.9L22.39,22L21,23.39L17.88,20.32C17.19,20.75 16.37,21 15.5,21C13,21 11,19 11,16.5C11,14 13,12 15.5,12M15.5,14A2.5,2.5 0 0,0 13,16.5A2.5,2.5 0 0,0 15.5,19A2.5,2.5 0 0,0 18,16.5A2.5,2.5 0 0,0 15.5,14M5,3H19C20.1,3 21,3.9 21,5V13.03C20.5,12.23 19.81,11.54 19,11V5H5V19H9.5C9.81,19.75 10.26,20.42 10.81,21H5C3.9,21 3,20.1 3,19V5C3,3.9 3.9,3 5,3M7,7H17V9H7V7M7,11H12.03C11.23,11.5 10.54,12.19 10,13H7V11M7,15H9.17C9.06,15.5 9,16 9,16.5V17H7V15Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.playerRequests.total }}</div>
                  <div class="text-sm text-gray-500">LFP Posts</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.playerRequests.active }} active
              </div>
            </div>

            <!-- BGG Cache -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-indigo-100">
                  <svg class="w-6 h-6 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.bggCache.total }}</div>
                  <div class="text-sm text-gray-500">Cached Games</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Subscription & Revenue Section -->
          <div v-if="dashboardStats" class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
            <!-- Subscription Tiers -->
            <div class="card p-4">
              <h3 class="font-semibold mb-4 flex items-center gap-2">
                <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                </svg>
                Subscription Tiers
              </h3>
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full bg-gray-400"></span>
                    <span class="text-sm">Free</span>
                  </div>
                  <span class="font-bold">{{ dashboardStats.users.byTier?.free ?? 0 }}</span>
                </div>
                <div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="w-3 h-3 rounded-full bg-blue-500"></span>
                      <span class="text-sm">Basic ($4.99/mo)</span>
                    </div>
                    <span class="font-bold text-blue-600">{{ dashboardStats.users.byTier?.basic ?? 0 }}</span>
                  </div>
                  <div v-if="dashboardStats.users.tierBreakdown" class="ml-5 mt-1 text-xs text-gray-500 space-y-0.5">
                    <div class="flex justify-between">
                      <span>Paid</span>
                      <span>{{ dashboardStats.users.tierBreakdown.basicPaid }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span>Upgraded (free)</span>
                      <span class="text-amber-600">{{ dashboardStats.users.tierBreakdown.basicUpgraded }}</span>
                    </div>
                  </div>
                </div>
                <div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="w-3 h-3 rounded-full bg-purple-500"></span>
                      <span class="text-sm">Pro ($7.99/mo)</span>
                    </div>
                    <span class="font-bold text-purple-600">{{ dashboardStats.users.byTier?.pro ?? 0 }}</span>
                  </div>
                  <div v-if="dashboardStats.users.tierBreakdown" class="ml-5 mt-1 text-xs text-gray-500 space-y-0.5">
                    <div class="flex justify-between">
                      <span>Paid</span>
                      <span>{{ dashboardStats.users.tierBreakdown.proPaid }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span>Upgraded (free)</span>
                      <span class="text-amber-600">{{ dashboardStats.users.tierBreakdown.proUpgraded }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <!-- Visual bar chart -->
              <div class="mt-4 pt-4 border-t border-gray-100">
                <div class="flex h-4 rounded-full overflow-hidden bg-gray-100">
                  <div
                    class="bg-gray-400 transition-all"
                    :style="{ width: `${(dashboardStats.users.byTier?.free ?? 0) / dashboardStats.users.total * 100}%` }"
                  ></div>
                  <div
                    class="bg-blue-500 transition-all"
                    :style="{ width: `${(dashboardStats.users.byTier?.basic ?? 0) / dashboardStats.users.total * 100}%` }"
                  ></div>
                  <div
                    class="bg-purple-500 transition-all"
                    :style="{ width: `${(dashboardStats.users.byTier?.pro ?? 0) / dashboardStats.users.total * 100}%` }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Projected Revenue -->
            <div class="card p-4">
              <h3 class="font-semibold mb-4 flex items-center gap-2">
                <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M5,6H23V18H5V6M14,9A3,3 0 0,1 17,12A3,3 0 0,1 14,15A3,3 0 0,1 11,12A3,3 0 0,1 14,9M9,8A2,2 0 0,1 7,10V14A2,2 0 0,1 9,16H19A2,2 0 0,1 21,14V10A2,2 0 0,1 19,8H9M1,10H3V20H19V22H1V10Z"/>
                </svg>
                Projected Monthly Revenue
              </h3>
              <div class="text-center py-4">
                <div class="text-4xl font-bold text-green-600">
                  ${{ dashboardStats.revenue?.projectedMonthly?.toFixed(2) ?? '0.00' }}
                </div>
                <div class="text-sm text-gray-500 mt-1">per month</div>
              </div>
              <div class="border-t border-gray-100 pt-4 space-y-3 text-sm">
                <!-- Basic Tier Breakdown -->
                <div>
                  <div class="flex justify-between">
                    <span class="text-gray-600 font-medium">Basic ({{ dashboardStats.revenue?.basicCount ?? 0 }} total)</span>
                    <span class="font-medium">${{ ((dashboardStats.revenue?.basicPaidCount ?? 0) * 4.99).toFixed(2) }}</span>
                  </div>
                  <div class="ml-4 mt-1 text-xs text-gray-500 space-y-0.5">
                    <div class="flex justify-between">
                      <span>Paid ({{ dashboardStats.revenue?.basicPaidCount ?? 0 }} x $4.99)</span>
                      <span class="text-green-600">${{ ((dashboardStats.revenue?.basicPaidCount ?? 0) * 4.99).toFixed(2) }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span>Promoted ({{ (dashboardStats.revenue?.basicCount ?? 0) - (dashboardStats.revenue?.basicPaidCount ?? 0) }} x $0.00)</span>
                      <span class="text-amber-600">$0.00</span>
                    </div>
                  </div>
                </div>
                <!-- Pro Tier Breakdown -->
                <div>
                  <div class="flex justify-between">
                    <span class="text-gray-600 font-medium">Pro ({{ dashboardStats.revenue?.proCount ?? 0 }} total)</span>
                    <span class="font-medium">${{ ((dashboardStats.revenue?.proPaidCount ?? 0) * 7.99).toFixed(2) }}</span>
                  </div>
                  <div class="ml-4 mt-1 text-xs text-gray-500 space-y-0.5">
                    <div class="flex justify-between">
                      <span>Paid ({{ dashboardStats.revenue?.proPaidCount ?? 0 }} x $7.99)</span>
                      <span class="text-green-600">${{ ((dashboardStats.revenue?.proPaidCount ?? 0) * 7.99).toFixed(2) }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span>Promoted ({{ (dashboardStats.revenue?.proCount ?? 0) - (dashboardStats.revenue?.proPaidCount ?? 0) }} x $0.00)</span>
                      <span class="text-amber-600">$0.00</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Today's Signups Section -->
          <div v-if="dashboardStats?.users?.todaySignups && dashboardStats.users.todaySignups.length > 0" class="mt-6">
            <h3 class="font-semibold mb-4 flex items-center gap-2">
              <svg class="w-5 h-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
              </svg>
              Today's Signups ({{ dashboardStats.users.todaySignups.length }})
            </h3>
            <div class="card divide-y divide-gray-100">
              <div
                v-for="signup in dashboardStats.users.todaySignups"
                :key="signup.id"
                class="p-3 flex items-center gap-3"
              >
                <UserAvatar
                  :avatar-url="signup.avatarUrl"
                  :display-name="signup.displayName"
                  size="sm"
                />
                <div class="flex-1 min-w-0">
                  <div class="font-medium truncate">{{ signup.displayName || signup.username }}</div>
                  <div class="text-xs text-gray-500">@{{ signup.username }}</div>
                </div>
                <div class="text-right">
                  <div class="text-sm text-gray-600">{{ signup.email }}</div>
                  <div class="text-xs text-gray-400">{{ new Date(signup.createdAt).toLocaleTimeString() }}</div>
                </div>
                <button
                  @click="editSignupUser(signup.username)"
                  class="p-2 text-gray-400 hover:text-primary-500 hover:bg-gray-100 rounded-lg transition-colors"
                  title="Edit user"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div v-else-if="dashboardStats" class="mt-6">
            <h3 class="font-semibold mb-4 flex items-center gap-2">
              <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
              </svg>
              Today's Signups (0)
            </h3>
            <div class="card p-4 text-center text-gray-500">
              No new signups today
            </div>
          </div>

          <!-- Popular Games Section -->
          <div v-if="dashboardStats?.popularGames && dashboardStats.popularGames.length > 0" class="mt-6">
            <h3 class="font-semibold mb-4 flex items-center gap-2">
              <svg class="w-5 h-5 text-orange-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
              Popular Games (Top 10)
            </h3>
            <div class="card divide-y divide-gray-100">
              <div
                v-for="(game, index) in dashboardStats?.popularGames"
                :key="game.name"
                class="p-3 flex items-center gap-3"
              >
                <span class="text-lg font-bold text-gray-400 w-6">{{ index + 1 }}</span>
                <img
                  v-if="game.thumbnailUrl"
                  :src="game.thumbnailUrl"
                  :alt="game.name"
                  class="w-10 h-10 rounded object-cover"
                />
                <div v-else class="w-10 h-10 rounded bg-gray-200 flex items-center justify-center">
                  <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5Z"/>
                  </svg>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="font-medium truncate">{{ game.name }}</div>
                  <div v-if="game.bggId" class="text-xs text-gray-500">BGG ID: {{ game.bggId }}</div>
                </div>
                <div class="text-right">
                  <div class="font-bold text-primary-600">{{ game.count }}</div>
                  <div class="text-xs text-gray-500">event{{ game.count !== 1 ? 's' : '' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Service Health Section -->
        <div>
          <h2 class="text-lg font-semibold mb-4">Service Health</h2>
          <div class="card divide-y divide-gray-100">
            <div
              v-for="service in serviceHealth"
              :key="service.name"
              class="p-4 flex items-center justify-between"
            >
              <div class="flex items-center gap-3">
                <div
                  class="w-3 h-3 rounded-full"
                  :class="getHealthDotColor(service.status)"
                ></div>
                <div>
                  <div class="font-medium">{{ service.name }}</div>
                  <div v-if="service.message" class="text-sm text-gray-500">{{ service.message }}</div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span
                  v-if="service.latencyMs"
                  class="text-sm text-gray-500"
                >
                  {{ service.latencyMs }}ms
                </span>
                <span
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="getHealthStatusColor(service.status)"
                >
                  {{ service.status }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Users Tab -->
    <div v-if="activeTab === 'users'">
      <div class="mb-6">
        <div class="flex gap-4 items-center">
          <div class="flex-1">
            <input
              v-model="userSearch"
              type="text"
              class="input"
              placeholder="Search by email, username, or display name..."
              @input="handleUserSearch"
            />
          </div>
          <label class="flex items-center gap-2 text-sm">
            <input
              v-model="showSuspendedOnly"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              @change="usersPage = 1; loadUsers()"
            />
            <span>Suspended only</span>
          </label>
          <label class="flex items-center gap-2 text-sm">
            <input
              v-model="showBannedOnly"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-red-500 focus:ring-red-500"
              @change="usersPage = 1; loadUsers()"
            />
            <span>Banned only</span>
          </label>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="usersLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <!-- User List -->
      <template v-else>
        <div class="text-sm text-gray-500 mb-4">
          {{ usersTotal }} user{{ usersTotal !== 1 ? 's' : '' }} found
        </div>

        <div v-if="users.length === 0" class="text-center py-12 text-gray-500">
          <p>No users found matching your search.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="user in users"
            :key="user.id"
            class="card p-4"
            :class="{
              'bg-red-50 border-red-200': user.accountStatus === 'banned',
              'bg-orange-50 border-orange-200': user.isSuspended && user.accountStatus !== 'banned'
            }"
          >
            <div class="flex items-start gap-3">
            <!-- Avatar -->
            <UserAvatar
              :avatar-url="user.avatarUrl"
              :display-name="user.displayName"
              :is-founding-member="user.isFoundingMember"
              :is-admin="user.isAdmin"
              size="md"
              class="flex-shrink-0"
            />

            <!-- User info -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-medium text-gray-900">
                  {{ user.displayName || user.username }}
                </span>
                <span class="text-gray-500">@{{ user.username }}</span>
                <span v-if="user.isAdmin" class="text-xs px-2 py-0.5 rounded-full bg-purple-100 text-purple-700">
                  Admin
                </span>
                <span v-if="user.isFoundingMember" class="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 flex items-center gap-1">
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                  </svg>
                  Founder
                </span>
                <!-- Account Status Badge -->
                <span
                  v-if="user.accountStatus === 'banned'"
                  class="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700"
                >
                  Banned
                </span>
                <span
                  v-else-if="user.isSuspended"
                  class="text-xs px-2 py-0.5 rounded-full bg-orange-100 text-orange-700"
                >
                  Suspended
                </span>
                <!-- Tier Badge -->
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="getTierBadgeClass(user.effectiveTier)"
                >
                  {{ user.effectiveTier }}
                  <span v-if="user.subscriptionOverrideTier" class="opacity-75">(override)</span>
                </span>
              </div>
              <p class="text-sm text-gray-500 truncate">
                {{ user.email }}
                <span v-if="user.authProvider && user.authProvider !== 'password'"
                      class="ml-1 px-1.5 py-0.5 text-xs rounded bg-blue-100 text-blue-700">
                  {{ user.authProvider === 'google.com' ? 'Google' : user.authProvider }}
                </span>
              </p>
              <p v-if="user.accountStatus === 'banned' && user.banReason" class="text-sm text-red-600 mt-1">
                Ban reason: {{ user.banReason }}
              </p>
              <p v-else-if="user.isSuspended && user.suspensionReason" class="text-sm text-orange-600 mt-1">
                Suspension reason: {{ user.suspensionReason }}
              </p>
              <p v-if="user.subscriptionOverrideReason" class="text-sm text-blue-600 mt-1">
                Tier override: {{ user.subscriptionOverrideReason }}
              </p>
            </div>
            </div>

            <!-- Created date & Actions -->
            <div class="flex items-center justify-between mt-2 pt-2 border-t border-gray-100">
            <div class="text-sm text-gray-500">
              Joined {{ new Date(user.createdAt).toLocaleDateString() }}
            </div>

            <div class="flex gap-1 flex-wrap">
              <!-- Edit button -->
              <button
                class="btn-ghost text-gray-600 text-sm"
                title="Edit user"
                @click="openUserEditDialog(user)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                </svg>
              </button>

              <!-- Set Tier button -->
              <button
                class="btn-ghost text-blue-600 text-sm"
                title="Set subscription tier"
                :disabled="settingTierId === user.id"
                @click="openTierDialog(user)"
              >
                <svg v-if="settingTierId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M16,6L18.29,8.29L13.41,13.17L9.41,9.17L2,16.59L3.41,18L9.41,12L13.41,16L19.71,9.71L22,12V6H16Z"/>
                </svg>
              </button>

              <!-- Toggle Founding Member button -->
              <button
                class="btn-ghost text-sm"
                :class="user.isFoundingMember ? 'text-amber-600' : 'text-gray-400'"
                :title="user.isFoundingMember ? 'Remove Founding Member badge' : 'Grant Founding Member badge'"
                :disabled="togglingFoundingId === user.id"
                @click="handleToggleFounding(user)"
              >
                <svg v-if="togglingFoundingId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
                </svg>
              </button>

              <!-- Password Reset button - only for email/password users -->
              <button
                v-if="user.authProvider === 'password'"
                class="btn-ghost text-gray-600 text-sm"
                title="Send password reset email"
                :disabled="sendingPasswordReset === user.id"
                @click="handleSendPasswordReset(user)"
              >
                <svg v-if="sendingPasswordReset === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12.63,2C18.16,2 22.64,6.5 22.64,12C22.64,17.5 18.16,22 12.63,22C9.12,22 6.05,20.18 4.26,17.43L5.84,16.18C7.25,18.47 9.76,20 12.64,20A8,8 0 0,0 20.64,12A8,8 0 0,0 12.64,4C8.56,4 5.2,7.06 4.71,11H7.47L3.73,14.73L0,11H2.69C3.19,5.95 7.45,2 12.63,2M15.59,10.24C16.09,10.25 16.5,10.65 16.5,11.16V15.77C16.5,16.27 16.09,16.69 15.58,16.69H10.05C9.54,16.69 9.13,16.27 9.13,15.77V11.16C9.13,10.65 9.54,10.25 10.04,10.24V9.23C10.04,7.7 11.29,6.46 12.81,6.46C14.34,6.46 15.59,7.7 15.59,9.23V10.24M14.54,9.23C14.54,8.28 13.77,7.5 12.81,7.5C11.86,7.5 11.09,8.28 11.09,9.23V10.24H14.54V9.23Z"/>
                </svg>
              </button>

              <!-- Suspend/Unsuspend button (only if not banned) -->
              <button
                v-if="user.isSuspended && user.accountStatus !== 'banned'"
                class="btn-ghost text-green-600 text-sm"
                title="Unsuspend user"
                :disabled="suspendingUserId === user.id"
                @click="handleUnsuspendUser(user)"
              >
                <svg v-if="suspendingUserId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10,17L5,12L6.41,10.58L10,14.17L17.59,6.58L19,8M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
                </svg>
              </button>
              <button
                v-else-if="!user.isAdmin && user.accountStatus !== 'banned'"
                class="btn-ghost text-yellow-600 text-sm"
                title="Suspend user (temporary)"
                :disabled="suspendingUserId === user.id"
                @click="openSuspendDialog(user)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M9,9H15V15H9"/>
                </svg>
              </button>

              <!-- Ban/Unban button -->
              <button
                v-if="user.accountStatus === 'banned'"
                class="btn-ghost text-green-600 text-sm"
                title="Unban user"
                :disabled="banningUserId === user.id"
                @click="handleUnbanUser(user)"
              >
                <svg v-if="banningUserId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2m0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8m3.59-13L12 10.59 8.41 7 7 8.41 10.59 12 7 15.59 8.41 17 12 13.41 15.59 17 17 15.59 13.41 12 17 8.41"/>
                </svg>
              </button>
              <button
                v-else-if="!user.isAdmin"
                class="btn-ghost text-red-600 text-sm"
                title="Ban user (permanent)"
                :disabled="banningUserId === user.id"
                @click="openBanDialog(user)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,2C17.5,2 22,6.5 22,12C22,17.5 17.5,22 12,22C6.5,22 2,17.5 2,12C2,6.5 6.5,2 12,2M12,4C7.58,4 4,7.58 4,12C4,13.85 4.63,15.55 5.68,16.91L16.91,5.68C15.55,4.63 13.85,4 12,4M12,20C16.42,20 20,16.42 20,12C20,10.15 19.37,8.45 18.32,7.09L7.09,18.32C8.45,19.37 10.15,20 12,20Z"/>
                </svg>
              </button>

              <!-- Delete button -->
              <button
                v-if="!user.isAdmin"
                class="btn-ghost text-red-800 text-sm"
                title="Delete user and all data"
                :disabled="deletingUserId === user.id"
                @click="handleDeleteUser(user)"
              >
                <svg v-if="deletingUserId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                </svg>
              </button>
            </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="usersTotal > 20" class="flex justify-center gap-2 mt-6">
          <button
            class="btn-outline text-sm"
            :disabled="usersPage === 1"
            @click="usersPage--; loadUsers()"
          >
            Previous
          </button>
          <span class="px-4 py-2 text-sm text-gray-500">
            Page {{ usersPage }} of {{ Math.ceil(usersTotal / 20) }}
          </span>
          <button
            class="btn-outline text-sm"
            :disabled="usersPage >= Math.ceil(usersTotal / 20)"
            @click="usersPage++; loadUsers()"
          >
            Next
          </button>
        </div>
      </template>
    </div>

    <!-- Groups Tab -->
    <div v-if="activeTab === 'groups'">
      <div class="mb-6">
        <input
          v-model="groupSearch"
          type="text"
          class="input"
          placeholder="Search by group name or slug..."
          @input="handleGroupSearch"
        />
      </div>

      <!-- Loading -->
      <div v-if="groupsLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <!-- Group List -->
      <template v-else>
        <div class="text-sm text-gray-500 mb-4">
          {{ groupsTotal }} group{{ groupsTotal !== 1 ? 's' : '' }} found
        </div>

        <div v-if="groups.length === 0" class="text-center py-12 text-gray-500">
          <p>No groups found matching your search.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="group in groups"
            :key="group.id"
            class="card p-4"
          >
            <div class="flex items-start gap-3">
              <!-- Logo -->
              <div class="flex-shrink-0">
                <img
                  v-if="group.logoUrl"
                  :src="group.logoUrl"
                  :alt="group.name"
                  class="w-10 h-10 rounded-lg object-cover"
                />
                <div
                  v-else
                  class="w-10 h-10 rounded-lg bg-gray-200 flex items-center justify-center text-gray-500 font-medium"
                >
                  {{ (group.name || 'G').charAt(0).toUpperCase() }}
                </div>
              </div>

              <!-- Group info -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-medium text-gray-900">{{ group.name }}</span>
                  <span v-if="group.isPublic" class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">
                    Public
                  </span>
                  <span v-else class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                    Private
                  </span>
                </div>
                <p class="text-sm text-gray-500">
                  /g/{{ group.slug }} &bull; {{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}
                </p>
                <p v-if="group.description" class="text-sm text-gray-500 truncate mt-1">
                  {{ group.description }}
                </p>
              </div>
            </div>

            <!-- Created date & Actions -->
            <div class="flex items-center justify-between mt-2 pt-2 border-t border-gray-100">
              <div class="text-sm text-gray-500">
                Created {{ new Date(group.createdAt).toLocaleDateString() }}
              </div>
              <div class="flex gap-2">
              <button
                class="btn-ghost text-blue-600 text-sm"
                @click="openMembersDialog(group)"
              >
                <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
                </svg>
                Members
              </button>
              <button
                class="btn-ghost text-red-600 text-sm"
                :disabled="deletingGroupId === group.id"
                @click="handleDeleteGroup(group)"
              >
                <svg v-if="deletingGroupId === group.id" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Delete
              </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="groupsTotal > 20" class="flex justify-center gap-2 mt-6">
          <button
            class="btn-outline text-sm"
            :disabled="groupsPage === 1"
            @click="groupsPage--; loadGroups()"
          >
            Previous
          </button>
          <span class="px-4 py-2 text-sm text-gray-500">
            Page {{ groupsPage }} of {{ Math.ceil(groupsTotal / 20) }}
          </span>
          <button
            class="btn-outline text-sm"
            :disabled="groupsPage >= Math.ceil(groupsTotal / 20)"
            @click="groupsPage++; loadGroups()"
          >
            Next
          </button>
        </div>
      </template>
    </div>

    <!-- Events Tab -->
    <div v-if="activeTab === 'events'">
      <div class="flex items-center justify-between mb-4">
        <p class="text-gray-500">All events in the system</p>
        <label class="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            v-model="eventsShowPast"
            @change="eventsPage = 1; loadEvents()"
            class="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          Show past events
        </label>
      </div>

      <div class="mb-4">
        <input
          v-model="eventsSearch"
          type="text"
          placeholder="Search by title, game, or city..."
          class="input w-full"
          @input="eventsPage = 1; loadEvents()"
        />
      </div>

      <div v-if="eventsLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <p class="text-sm text-gray-500 mb-3">{{ eventsTotal }} {{ eventsShowPast ? 'total' : 'upcoming' }} events</p>

        <div v-if="adminEvents.length === 0" class="text-center py-12 text-gray-500">
          No events found
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="event in adminEvents"
            :key="event.id"
            class="card p-4"
          >
            <div class="flex items-start gap-3">
              <!-- Game thumbnail -->
              <div class="w-12 h-12 rounded-lg bg-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  v-if="event.primaryGameThumbnail"
                  :src="event.primaryGameThumbnail"
                  :alt="event.gameTitle || event.title"
                  class="w-full h-full object-cover"
                />
                <img
                  v-else-if="event.gameSystem === 'mtg'"
                  src="/icons/mtg-logo.png"
                  alt="MTG"
                  class="w-8 h-8 object-contain"
                />
                <img
                  v-else-if="event.gameSystem === 'pokemon_tcg'"
                  src="/icons/pokemon-logo.png"
                  alt="Pokemon TCG"
                  class="w-8 h-8 object-contain"
                />
                <img
                  v-else-if="event.gameSystem === 'yugioh'"
                  src="/icons/yugioh-logo.png"
                  alt="Yu-Gi-Oh!"
                  class="w-8 h-8 object-contain"
                />
                <img
                  v-else-if="event.gameSystem === 'warhammer40k'"
                  src="/icons/warhammer40k-logo.png"
                  alt="Warhammer 40k"
                  class="w-8 h-8 object-contain"
                />
                <svg v-else class="w-6 h-6 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.27 6C19.28 8.17 19.05 10.73 17.94 12.81C17 14.5 15.65 15.93 14.5 17.5C14 18.2 13.5 18.95 13.13 19.8C12.92 20.34 12.74 20.9 12.6 21.5H11.5C11.35 20.9 11.17 20.34 10.96 19.8C10.59 18.95 10.09 18.2 9.58 17.5C8.43 15.93 7.08 14.5 6.15 12.81C5.04 10.73 4.81 8.17 5.82 6C6.71 4.05 8.63 2.5 11.04 2.5C13.45 2.5 15.37 4.05 16.27 6H18.27Z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
              <div class="flex flex-wrap items-center gap-2 mb-1">
                <h3 class="font-semibold text-lg">{{ event.title }}</h3>
                <span
                  v-if="event.gameSystem && event.gameSystem !== 'board_game'"
                  class="px-2 py-0.5 text-xs rounded-full bg-purple-100 text-purple-700 whitespace-nowrap"
                >
                  {{ event.gameSystem === 'mtg' ? 'MTG' : event.gameSystem === 'pokemon_tcg' ? 'Pokemon' : event.gameSystem === 'yugioh' ? 'Yu-Gi-Oh' : event.gameSystem === 'warhammer40k' ? 'Warhammer 40k' : event.gameSystem }}
                </span>
                <span
                  class="px-2 py-0.5 text-xs rounded-full whitespace-nowrap"
                  :class="event.status === 'published' ? 'bg-green-100 text-green-700' : event.status === 'draft' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-700'"
                >
                  {{ event.status }}
                </span>
                <span v-if="!event.isPublic" class="px-2 py-0.5 text-xs rounded-full bg-gray-100 text-gray-600">Private</span>
              </div>
              <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-600">
                <span>{{ new Date(event.eventDate + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }) }}</span>
                <span v-if="event.startTime">{{ event.startTime.slice(0, 5) }}</span>
                <span v-if="event.durationMinutes">{{ event.durationMinutes }} min</span>
                <span v-if="event.city">{{ event.city }}<span v-if="event.state">, {{ event.state }}</span></span>
              </div>
              <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-gray-500 mt-1">
                <span v-if="event.host">Hosted by {{ event.host.displayName || event.host.username || 'Unknown' }}</span>
                <span>{{ event.registrationCount }}{{ event.maxPlayers ? '/' + event.maxPlayers : '' }} registered</span>
                <span v-if="event.gameTitle" class="truncate max-w-[200px]">Game: {{ event.gameTitle }}</span>
              </div>
              <div class="flex items-center gap-2 mt-2">
                <a
                  :href="'/games/' + event.id"
                  target="_blank"
                  class="btn-ghost text-sm"
                >
                  View
                </a>
                <button
                  class="btn-ghost text-sm text-red-600 hover:text-red-700 hover:bg-red-50"
                  :disabled="deletingEventId === event.id"
                  @click="handleDeleteEvent(event)"
                >
                  <span v-if="deletingEventId === event.id">Deleting...</span>
                  <span v-else>Delete</span>
                </button>
              </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="eventsTotal > 20" class="flex items-center justify-between mt-4">
          <button
            class="btn-ghost text-sm"
            :disabled="eventsPage === 1"
            @click="eventsPage--; loadEvents()"
          >
            Previous
          </button>
          <span class="text-sm text-gray-500">Page {{ eventsPage }} of {{ Math.ceil(eventsTotal / 20) }}</span>
          <button
            class="btn-ghost text-sm"
            :disabled="eventsPage >= Math.ceil(eventsTotal / 20)"
            @click="eventsPage++; loadEvents()"
          >
            Next
          </button>
        </div>
      </template>
    </div>

    <!-- Notes Tab -->
    <div v-if="activeTab === 'notes'">
      <div class="flex items-center justify-between mb-6">
        <p class="text-gray-500">Project notes and documentation</p>
        <button class="btn-primary" @click="openNoteDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Add Note
        </button>
      </div>

      <!-- Loading -->
      <div v-if="notesLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <div v-if="notes.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No notes yet</p>
          <p>Add your first project note.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="note in notes"
            :key="note.id"
            class="card p-4"
            :class="{
              'ring-2 ring-yellow-400': note.isPinned,
              'bg-green-50': note.isImplemented
            }"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span v-if="note.isPinned" class="text-yellow-500">
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16,12V4H17V2H7V4H8V12L6,14V16H11.2V22H12.8V16H18V14L16,12Z"/>
                    </svg>
                  </span>
                  <h3 class="font-semibold" :class="note.isImplemented ? 'text-green-700' : 'text-gray-900'">{{ note.title }}</h3>
                  <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                    {{ note.category }}
                  </span>
                  <span v-if="note.isImplemented" class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">
                    Implemented
                  </span>
                </div>
                <div class="text-gray-600 whitespace-pre-wrap">
                  <template v-if="note.content && note.content.split('\n').length > 20 && !expandedNotes.has(note.id)">
                    {{ note.content.split('\n').slice(0, 20).join('\n') }}
                    <button class="text-primary-600 hover:text-primary-800 font-medium text-sm mt-1 block" @click="expandedNotes.add(note.id)">more...</button>
                  </template>
                  <template v-else>{{ note.content }}</template>
                  <button v-if="note.content && note.content.split('\n').length > 20 && expandedNotes.has(note.id)" class="text-primary-600 hover:text-primary-800 font-medium text-sm mt-1 block" @click="expandedNotes.delete(note.id)">show less</button>
                </div>
                <p class="text-xs text-gray-400 mt-2">
                  By {{ note.createdBy?.displayName || note.createdBy?.username || 'Unknown' }}
                  &bull; Updated {{ new Date(note.updatedAt).toLocaleDateString() }}
                </p>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button
                  class="btn-ghost text-sm"
                  :class="note.isImplemented ? 'text-green-600' : 'text-gray-400'"
                  @click="handleToggleImplemented(note)"
                  :title="note.isImplemented ? 'Mark as not implemented' : 'Mark as implemented'"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                  </svg>
                </button>
                <button
                  class="btn-ghost text-gray-500 text-sm"
                  :class="{ 'text-yellow-500': note.isPinned }"
                  @click="handleTogglePin(note)"
                  :title="note.isPinned ? 'Unpin' : 'Pin'"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16,12V4H17V2H7V4H8V12L6,14V16H11.2V22H12.8V16H18V14L16,12Z"/>
                  </svg>
                </button>
                <button class="btn-ghost text-gray-600 text-sm" @click="openNoteDialog(note)">
                  Edit
                </button>
                <button
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingNoteId === note.id"
                  @click="handleDeleteNote(note)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Bugs Tab -->
    <div v-if="activeTab === 'bugs'">
      <div class="flex items-center justify-between mb-6">
        <div class="flex gap-4 items-center">
          <select v-model="bugStatusFilter" class="input w-40" @change="loadBugs()">
            <option value="">All Statuses</option>
            <option value="open">Open</option>
            <option value="in_progress">In Progress</option>
            <option value="resolved">Resolved</option>
            <option value="closed">Closed</option>
            <option value="wont_fix">Won't Fix</option>
          </select>
          <select v-model="bugPriorityFilter" class="input w-40" @change="loadBugs()">
            <option value="">All Priorities</option>
            <option value="critical">Critical</option>
            <option value="high">High</option>
            <option value="medium">Medium</option>
            <option value="low">Low</option>
          </select>
        </div>
        <button class="btn-primary" @click="openBugDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Report Bug
        </button>
      </div>

      <!-- Loading -->
      <div v-if="bugsLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <div v-if="bugs.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No bugs reported</p>
          <p>That's a good thing!</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="bug in bugs"
            :key="bug.id"
            class="card p-4"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-2 flex-wrap">
                  <h3 class="font-semibold text-gray-900 break-words">{{ bug.title }}</h3>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="getPriorityColor(bug.priority)"
                  >
                    {{ bug.priority }}
                  </span>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="getStatusColor(bug.status)"
                  >
                    {{ bug.status.replace('_', ' ') }}
                  </span>
                </div>
                <p v-if="bug.description" class="text-gray-600 mb-2 break-words overflow-hidden">{{ bug.description }}</p>
                <div v-if="bug.stepsToReproduce" class="text-sm text-gray-500 bg-gray-50 rounded p-2 mb-2">
                  <strong>Steps to reproduce:</strong>
                  <p class="whitespace-pre-wrap">{{ bug.stepsToReproduce }}</p>
                </div>
                <p class="text-xs text-gray-400">
                  Reported by {{ bug.reportedBy?.displayName || bug.reportedBy?.username || 'Unknown' }}
                  &bull; {{ new Date(bug.createdAt).toLocaleDateString() }}
                  <span v-if="bug.assignedTo">
                    &bull; Assigned to {{ bug.assignedTo.displayName || bug.assignedTo.username }}
                  </span>
                </p>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button class="btn-ghost text-gray-600 text-sm" @click="openBugDialog(bug)">
                  Edit
                </button>
                <button
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingBugId === bug.id"
                  @click="handleDeleteBug(bug)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Chat Reports Tab -->
    <div v-if="activeTab === 'chatReports'">
      <div class="flex items-center justify-between mb-6">
        <div class="flex gap-4 items-center">
          <label class="text-sm text-gray-600">Status:</label>
          <select v-model="chatReportStatusFilter" class="form-select text-sm" @change="loadChatReports()">
            <option value="">All</option>
            <option value="pending">Pending</option>
            <option value="reviewed">Reviewed</option>
            <option value="action_taken">Action Taken</option>
            <option value="dismissed">Dismissed</option>
          </select>
        </div>
        <button class="btn-outline" @click="loadChatReports()">
          <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
          </svg>
          Refresh
        </button>
      </div>

      <!-- Loading -->
      <div v-if="chatReportsLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <div v-if="chatReports.length === 0" class="text-center py-12 text-gray-500">
          <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
          </svg>
          <p class="text-lg font-medium">No chat reports</p>
          <p>All clear! No reports to review.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="report in chatReports"
            :key="report.id"
            class="card p-4"
            :class="{
              'border-l-4 border-l-orange-500': report.status === 'pending',
              'border-l-4 border-l-green-500': report.status === 'action_taken',
            }"
          >
            <div class="flex items-start gap-4">
              <div class="flex-1 min-w-0">
                <!-- Report Header -->
                <div class="flex items-center gap-2 mb-2">
                  <span
                    class="text-xs font-medium px-2 py-0.5 rounded-full"
                    :class="getReportStatusColor(report.status)"
                  >
                    {{ report.status.replace('_', ' ') }}
                  </span>
                  <span class="text-xs px-2 py-0.5 rounded-full bg-red-50 text-red-700">
                    {{ REPORT_REASON_LABELS[report.reason] }}
                  </span>
                  <span class="text-xs text-gray-400">
                    {{ getContextLabel(report.message?.contextType || '') }}
                  </span>
                </div>

                <!-- Reported Message -->
                <div class="bg-gray-50 rounded-lg p-3 mb-3">
                  <div class="flex items-center gap-2 mb-1">
                    <UserAvatar
                      v-if="report.message?.user"
                      :avatar-url="report.message.user.avatarUrl"
                      :display-name="report.message.user.displayName"
                      size="xs"
                    />
                    <span class="text-sm font-medium text-gray-700">
                      {{ report.message?.user?.displayName || 'Unknown User' }}
                    </span>
                    <span v-if="report.message?.isDeleted" class="text-xs text-red-500">(deleted)</span>
                  </div>
                  <p class="text-sm text-gray-800">{{ report.message?.content }}</p>
                </div>

                <!-- Report Details -->
                <div v-if="report.details" class="text-sm text-gray-600 mb-2">
                  <span class="font-medium">Details:</span> {{ report.details }}
                </div>

                <!-- Reporter and Time -->
                <div class="flex items-center gap-4 text-xs text-gray-500">
                  <span>
                    Reported by {{ report.reporter?.displayName || 'Unknown' }}
                  </span>
                  <span>
                    {{ new Date(report.createdAt).toLocaleString() }}
                  </span>
                </div>
              </div>

              <!-- Actions -->
              <div v-if="report.status === 'pending'" class="flex flex-col gap-2 flex-shrink-0">
                <button
                  class="btn-primary text-sm py-1.5 px-3"
                  @click="openModerationDialog(report)"
                >
                  Take Action
                </button>
                <button
                  class="btn-ghost text-gray-600 text-sm py-1.5 px-3"
                  :disabled="dismissingReportId === report.id"
                  @click="handleDismissReport(report)"
                >
                  <svg v-if="dismissingReportId === report.id" class="animate-spin -ml-1 mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  Dismiss
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Moderation Dialog -->
    <div
      v-if="showModerationDialog"
      class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
      @click.self="showModerationDialog = false"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div class="px-4 py-3 border-b border-gray-200">
          <h3 class="font-semibold text-gray-900">Issue Moderation Action</h3>
        </div>

        <div class="p-4 space-y-4">
          <!-- User Info -->
          <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
            <UserAvatar
              v-if="selectedReport?.message?.user"
              :avatar-url="selectedReport.message.user.avatarUrl"
              :display-name="selectedReport.message.user.displayName"
              size="md"
            />
            <div>
              <div class="font-medium">{{ selectedReport?.message?.user?.displayName || 'Unknown User' }}</div>
              <div class="text-sm text-gray-500">User to moderate</div>
            </div>
          </div>

          <!-- Moderation History -->
          <div v-if="moderationHistory.length > 0" class="border rounded-lg p-3">
            <h4 class="text-sm font-medium text-gray-700 mb-2">Previous Actions</h4>
            <div class="space-y-2 max-h-32 overflow-y-auto">
              <div v-for="item in moderationHistory" :key="item.id" class="text-xs text-gray-600 flex justify-between">
                <span>{{ MODERATION_ACTION_LABELS[item.action] }} - {{ item.reason }}</span>
                <span class="text-gray-400">{{ new Date(item.createdAt).toLocaleDateString() }}</span>
              </div>
            </div>
          </div>
          <div v-else-if="moderationHistoryLoading" class="text-sm text-gray-500 text-center py-2">
            Loading history...
          </div>
          <div v-else class="text-sm text-gray-500 text-center py-2">
            No previous moderation actions
          </div>

          <!-- Action Select -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Action</label>
            <select v-model="moderationForm.action" class="form-select w-full">
              <option v-for="(label, value) in MODERATION_ACTION_LABELS" :key="value" :value="value">
                {{ label }}
              </option>
            </select>
          </div>

          <!-- Reason -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Reason</label>
            <textarea
              v-model="moderationForm.reason"
              placeholder="Explain the reason for this action..."
              class="form-textarea w-full"
              rows="3"
            ></textarea>
          </div>
        </div>

        <div class="px-4 py-3 border-t border-gray-200 flex justify-end gap-2">
          <button class="btn-ghost" @click="showModerationDialog = false">
            Cancel
          </button>
          <button
            class="btn-primary"
            :disabled="issuingModeration || !moderationForm.reason.trim()"
            @click="handleIssueModeration"
          >
            <svg v-if="issuingModeration" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Issue Action
          </button>
        </div>
      </div>
    </div>

    <!-- Ads Tab -->
    <div v-if="activeTab === 'ads'">
      <div class="flex items-center justify-between mb-6">
        <div class="flex gap-4 items-center">
          <label class="flex items-center gap-2 text-sm text-gray-600">
            <input type="checkbox" v-model="showAdStats" class="rounded border-gray-300">
            Show Stats
          </label>
        </div>
        <button class="btn-primary" @click="openAdDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Create Ad
        </button>
      </div>

      <!-- Loading -->
      <div v-if="adsLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <div v-if="ads.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No ads yet</p>
          <p>Create your first ad to get started.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="ad in ads"
            :key="ad.id"
            class="card p-4"
            :class="{ 'opacity-60': !ad.isActive }"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex gap-4 flex-1">
                <!-- Ad Image Preview -->
                <div v-if="ad.imageUrl" class="w-16 h-16 rounded overflow-hidden flex-shrink-0 bg-gray-100">
                  <img :src="ad.imageUrl" :alt="ad.name" class="w-full h-full object-cover" />
                </div>
                <div v-else class="w-16 h-16 rounded flex-shrink-0 bg-gray-100 flex items-center justify-center">
                  <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,19H5V5H19M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M13.96,12.29L11.21,15.83L9.25,13.47L6.5,17H17.5L13.96,12.29Z"/>
                  </svg>
                </div>

                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <h3 class="font-semibold text-gray-900">{{ ad.name }}</h3>
                    <span
                      class="text-xs px-2 py-0.5 rounded-full"
                      :class="ad.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'"
                    >
                      {{ ad.isActive ? 'Active' : 'Inactive' }}
                    </span>
                    <span
                      v-if="ad.isHouseAd"
                      class="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-800"
                    >
                      House Ad
                    </span>
                    <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                      {{ ad.placement }}
                    </span>
                  </div>
                  <p v-if="ad.title" class="text-gray-700 font-medium">{{ ad.title }}</p>
                  <p v-if="ad.description" class="text-sm text-gray-600 line-clamp-2">{{ ad.description }}</p>
                  <p v-if="ad.advertiserName" class="text-xs text-gray-500 mt-1">
                    Advertiser: {{ ad.advertiserName }}
                  </p>
                  <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
                    <span v-if="ad.startDate || ad.endDate">
                      {{ ad.startDate ? new Date(ad.startDate).toLocaleDateString() : 'Always' }}
                      -
                      {{ ad.endDate ? new Date(ad.endDate).toLocaleDateString() : 'Forever' }}
                    </span>
                    <span>Priority: {{ ad.priority }}</span>
                    <a :href="ad.linkUrl" target="_blank" class="text-primary-500 hover:underline truncate max-w-xs">
                      {{ ad.linkUrl }}
                    </a>
                  </div>
                  <!-- Stats -->
                  <div v-if="showAdStats" class="mt-2 flex items-center gap-4 text-sm">
                    <span class="text-gray-600">
                      <strong>{{ getAdStatsById(ad.id)?.impressionCount || 0 }}</strong> impressions
                    </span>
                    <span class="text-gray-600">
                      <strong>{{ getAdStatsById(ad.id)?.clickCount || 0 }}</strong> clicks
                    </span>
                    <span class="text-gray-600">
                      <strong>{{ getAdStatsById(ad.id)?.ctrPercent || 0 }}%</strong> CTR
                    </span>
                  </div>
                </div>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button
                  class="btn-ghost text-sm"
                  :class="ad.isActive ? 'text-yellow-600' : 'text-green-600'"
                  :disabled="togglingAdId === ad.id"
                  @click="handleToggleAdActive(ad)"
                >
                  {{ ad.isActive ? 'Deactivate' : 'Activate' }}
                </button>
                <button class="btn-ghost text-gray-600 text-sm" @click="openAdDialog(ad)">
                  Edit
                </button>
                <button
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingAdId === ad.id"
                  @click="handleDeleteAd(ad)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Ad Create/Edit Dialog -->
    <div v-if="showAdDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showAdDialog = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto mx-4">
        <h2 class="text-xl font-semibold mb-4">
          {{ editingAd ? 'Edit Ad' : 'Create Ad' }}
        </h2>

        <div class="grid grid-cols-2 gap-4">
          <!-- Name -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Internal Name *</label>
            <input v-model="adForm.name" type="text" class="input" placeholder="e.g., Summer Promo Banner">
          </div>

          <!-- Advertiser Name -->
          <div>
            <label class="block text-sm font-medium mb-1">Advertiser Name</label>
            <input v-model="adForm.advertiserName" type="text" class="input" placeholder="Leave empty for self-promo">
          </div>

          <!-- House Ad -->
          <div class="flex items-center gap-2 pt-6">
            <input v-model="adForm.isHouseAd" type="checkbox" id="isHouseAd" class="rounded border-gray-300">
            <label for="isHouseAd" class="text-sm">House Ad (self-promotion)</label>
          </div>

          <!-- Title -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Ad Title</label>
            <input v-model="adForm.title" type="text" class="input" placeholder="Catchy headline for the ad">
          </div>

          <!-- Description -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Description</label>
            <textarea v-model="adForm.description" class="input" rows="2" placeholder="Short description (max 255 chars)"></textarea>
          </div>

          <!-- Link URL -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Link URL *</label>
            <input v-model="adForm.linkUrl" type="url" class="input" placeholder="https://... or /pricing">
          </div>

          <!-- Image URL -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Image URL</label>
            <input v-model="adForm.imageUrl" type="url" class="input" placeholder="https://... (optional)">
          </div>

          <!-- Ad Type -->
          <div>
            <label class="block text-sm font-medium mb-1">Ad Type</label>
            <select v-model="adForm.adType" class="input">
              <option value="banner">Banner</option>
              <option value="sidebar">Sidebar</option>
              <option value="featured">Featured</option>
            </select>
          </div>

          <!-- Placement -->
          <div>
            <label class="block text-sm font-medium mb-1">Placement</label>
            <select v-model="adForm.placement" class="input">
              <option value="general">General (all pages)</option>
              <option value="dashboard">Dashboard</option>
              <option value="events">Events</option>
              <option value="groups">Groups</option>
            </select>
          </div>

          <!-- Targeting -->
          <div>
            <label class="block text-sm font-medium mb-1">Target City</label>
            <input v-model="adForm.targetCity" type="text" class="input" placeholder="Optional">
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">Target State</label>
            <input v-model="adForm.targetState" type="text" class="input" placeholder="Optional">
          </div>

          <!-- Scheduling -->
          <div>
            <label class="block text-sm font-medium mb-1">Start Date</label>
            <input v-model="adForm.startDate" type="date" class="input">
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">End Date</label>
            <input v-model="adForm.endDate" type="date" class="input">
          </div>

          <!-- Priority & Active -->
          <div>
            <label class="block text-sm font-medium mb-1">Priority</label>
            <input v-model.number="adForm.priority" type="number" class="input" min="0" max="100">
            <p class="text-xs text-gray-500 mt-1">Higher = shown more often</p>
          </div>
          <div class="flex items-center gap-2 pt-6">
            <input v-model="adForm.isActive" type="checkbox" id="isActive" class="rounded border-gray-300">
            <label for="isActive" class="text-sm">Active</label>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-outline" @click="showAdDialog = false">Cancel</button>
          <button class="btn-primary" :disabled="savingAd" @click="handleSaveAd">
            {{ savingAd ? 'Saving...' : (editingAd ? 'Save Changes' : 'Create Ad') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Raffles Tab -->
    <div v-if="activeTab === 'raffles'">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-lg font-semibold">Monthly Raffles</h2>
        <button class="btn-primary" @click="openRaffleDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Create Raffle
        </button>
      </div>

      <!-- Loading -->
      <div v-if="rafflesLoading" class="text-center py-12">
        <D20Spinner size="lg" class="mx-auto" />
      </div>

      <template v-else>
        <div v-if="raffles.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No raffles yet</p>
          <p>Create your first raffle to engage users.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="raffle in raffles"
            :key="raffle.id"
            class="card p-4"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex gap-4 flex-1">
                <!-- Prize Image -->
                <div v-if="raffle.prizeImageUrl" class="w-20 h-20 rounded overflow-hidden flex-shrink-0 bg-gray-100">
                  <img :src="raffle.prizeImageUrl" :alt="raffle.prizeName" class="w-full h-full object-cover" />
                </div>
                <div v-else class="w-20 h-20 rounded flex-shrink-0 bg-gray-100 flex items-center justify-center">
                  <svg class="w-10 h-10 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M9.06,1.93C7.17,1.92 5.33,3.74 6.17,6H3A2,2 0 0,0 1,8V10A1,1 0 0,0 2,11H11V8H13V11H22A1,1 0 0,0 23,10V8A2,2 0 0,0 21,6H17.83C18.67,3.74 16.83,1.92 14.94,1.93C13.5,1.93 12.71,2.71 12,3.5C11.29,2.71 10.5,1.93 9.06,1.93M9.06,3.93C9.57,3.93 10.13,4.26 10.74,4.87L12,6.13L13.26,4.87C13.87,4.26 14.43,3.93 14.94,3.93C15.46,3.93 16,4.5 16,5.19C16,5.9 15.5,6.5 15,6.87L14.5,7.2L13.4,8H10.6L9.5,7.2L9,6.87C8.5,6.5 8,5.9 8,5.19C8,4.5 8.54,3.93 9.06,3.93M2,12V20A2,2 0 0,0 4,22H20A2,2 0 0,0 22,20V12H13V20H11V12H2Z"/>
                  </svg>
                </div>

                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <h3 class="font-semibold text-gray-900">{{ raffle.title }}</h3>
                    <span
                      class="text-xs px-2 py-0.5 rounded-full"
                      :class="getRaffleStatusColor(raffle.status)"
                    >
                      {{ raffle.status.charAt(0).toUpperCase() + raffle.status.slice(1) }}
                    </span>
                  </div>
                  <p class="text-gray-700 font-medium">Prize: {{ raffle.prizeName }}</p>
                  <p v-if="raffle.description" class="text-sm text-gray-600 line-clamp-2">{{ raffle.description }}</p>

                  <div class="flex items-center gap-4 mt-2 text-xs text-gray-500">
                    <span>
                      {{ new Date(raffle.startDate).toLocaleDateString() }} -
                      {{ new Date(raffle.endDate).toLocaleDateString() }}
                    </span>
                    <span v-if="raffle.stats">
                      {{ raffle.stats.entries || raffle.stats.totalEntries || 0 }} entries from {{ raffle.stats.users || raffle.stats.uniqueParticipants || 0 }} users
                    </span>
                  </div>

                  <!-- Winner Info -->
                  <div v-if="raffle.winner" class="mt-2 p-2 bg-green-50 rounded-lg">
                    <div class="flex items-center gap-2">
                      <UserAvatar :user="{ avatarUrl: raffle.winner.avatarUrl, displayName: raffle.winner.displayName }" size="sm" :show-badge="false" />
                      <span class="font-medium text-green-800">Winner: {{ raffle.winner.displayName || 'Unknown' }}</span>
                      <span v-if="raffle.winnerNotifiedAt" class="text-xs text-green-600">Notified</span>
                      <span v-if="raffle.winnerClaimedAt" class="text-xs text-green-600">Claimed</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="flex gap-2 flex-shrink-0 flex-wrap">
                <button
                  v-if="canSelectWinner(raffle)"
                  class="btn-primary text-sm"
                  :disabled="selectingWinnerId === raffle.id"
                  @click="handleSelectWinner(raffle)"
                >
                  {{ selectingWinnerId === raffle.id ? 'Selecting...' : 'Select Winner' }}
                </button>
                <button
                  v-if="raffle.winner && !raffle.winnerNotifiedAt"
                  class="btn-outline text-sm text-blue-600"
                  @click="handleMarkNotified(raffle)"
                >
                  Mark Notified
                </button>
                <button
                  v-if="raffle.winner && raffle.winnerNotifiedAt && !raffle.winnerClaimedAt"
                  class="btn-outline text-sm text-green-600"
                  @click="handleMarkClaimed(raffle)"
                >
                  Mark Claimed
                </button>
                <button class="btn-ghost text-sm" @click="openRaffleEntries(raffle)">
                  View Entries
                </button>
                <button class="btn-ghost text-purple-600 text-sm" @click="previewingRaffle = raffle">
                  Preview
                </button>
                <button class="btn-ghost text-gray-600 text-sm" @click="openRaffleDialog(raffle)">
                  Edit
                </button>
                <button
                  v-if="raffle.status === 'draft' || raffle.status === 'cancelled'"
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingRaffleId === raffle.id"
                  @click="handleDeleteRaffle(raffle)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Raffle Create/Edit Dialog -->
    <div v-if="showRaffleDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @mousedown.self="showRaffleDialog = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto mx-4">
        <h2 class="text-xl font-semibold mb-4">
          {{ editingRaffle ? 'Edit Raffle' : 'Create Raffle' }}
        </h2>

        <div class="grid grid-cols-2 gap-4">
          <!-- Title -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Title *</label>
            <input v-model="raffleForm.title" type="text" class="input" placeholder="March 2026 Raffle">
          </div>

          <!-- Description -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Description</label>
            <textarea v-model="raffleForm.description" class="input" rows="2" placeholder="Enter to win this month's prize!"></textarea>
          </div>

          <!-- Prize Search -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Search for Prize (Board Game) *</label>
            <GameSearch
              v-model="prizeGameSearch"
              placeholder="Search BoardGameGeek..."
              @select="handlePrizeGameSelect"
            />
          </div>

          <!-- Selected Prize Preview -->
          <div v-if="selectedPrizeGame" class="col-span-2 bg-gray-50 rounded-lg p-4">
            <div class="flex items-start gap-4">
              <img
                v-if="selectedPrizeGame.imageUrl || selectedPrizeGame.thumbnailUrl"
                :src="selectedPrizeGame.imageUrl || selectedPrizeGame.thumbnailUrl || ''"
                :alt="selectedPrizeGame.name"
                class="w-20 h-20 object-cover rounded-lg bg-white"
              />
              <div v-else class="w-20 h-20 bg-gray-200 rounded-lg flex items-center justify-center">
                <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between">
                  <h4 class="font-semibold text-gray-900">{{ selectedPrizeGame.name }}</h4>
                  <button type="button" class="text-red-500 hover:text-red-700 text-sm" @click="clearPrizeGame">
                    Clear
                  </button>
                </div>
                <p v-if="selectedPrizeGame.yearPublished" class="text-sm text-gray-500">{{ selectedPrizeGame.yearPublished }}</p>
                <div v-if="selectedPrizeGame.minPlayers || selectedPrizeGame.maxPlayers" class="text-sm text-gray-600 mt-1">
                  <span v-if="selectedPrizeGame.minPlayers && selectedPrizeGame.maxPlayers">
                    {{ selectedPrizeGame.minPlayers }}-{{ selectedPrizeGame.maxPlayers }} players
                  </span>
                  <span v-if="selectedPrizeGame.playingTime" class="ml-2">
                    &bull; {{ selectedPrizeGame.playingTime }} min
                  </span>
                </div>
                <p class="text-xs text-gray-400 mt-1">BGG ID: {{ selectedPrizeGame.bggId }}</p>
              </div>
            </div>
          </div>

          <!-- Manual Prize Name (if no game selected) -->
          <div v-if="!selectedPrizeGame" class="col-span-2">
            <label class="block text-sm font-medium mb-1">Or Enter Prize Name Manually *</label>
            <input v-model="raffleForm.prizeName" type="text" class="input" placeholder="e.g., Gift Card, Custom Prize">
          </div>

          <!-- Prize Description -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Prize Description</label>
            <textarea v-model="raffleForm.prizeDescription" class="input" rows="2" placeholder="A beautiful bird-themed engine building game..."></textarea>
          </div>

          <!-- Prize Image URL (only show if no game selected or to override) -->
          <div>
            <label class="block text-sm font-medium mb-1">Prize Image URL</label>
            <input v-model="raffleForm.prizeImageUrl" type="url" class="input" placeholder="https://...">
            <p v-if="selectedPrizeGame" class="text-xs text-gray-400 mt-1">Auto-filled from BGG, edit to override</p>
          </div>

          <!-- Prize Value -->
          <div>
            <label class="block text-sm font-medium mb-1">Prize Value (cents)</label>
            <div class="flex items-center gap-2">
              <input v-model.number="raffleForm.prizeValueCents" type="number" class="input flex-1" placeholder="e.g., 5999">
              <span v-if="raffleForm.prizeValueCents" class="text-sm font-medium text-primary-600 whitespace-nowrap">
                = ${{ (raffleForm.prizeValueCents / 100).toFixed(2) }}
              </span>
            </div>
          </div>

          <!-- Start Date -->
          <div>
            <label class="block text-sm font-medium mb-1">Start Date *</label>
            <input v-model="raffleForm.startDate" type="datetime-local" class="input">
          </div>

          <!-- End Date -->
          <div>
            <label class="block text-sm font-medium mb-1">End Date *</label>
            <input v-model="raffleForm.endDate" type="datetime-local" class="input">
          </div>

          <!-- Banner Image URL -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Custom Banner Image URL <span class="text-gray-400 font-normal">(optional)</span></label>
            <input v-model="raffleForm.bannerImageUrl" type="url" class="input" placeholder="e.g., /BrassRaffleBanner.png or https://...">
            <p class="text-xs text-gray-500 mt-1">Full custom banner for home page. If empty, dynamic banner is shown.</p>
          </div>

          <!-- Terms & Conditions -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Terms & Conditions</label>
            <textarea v-model="raffleForm.termsConditions" class="input" rows="3" placeholder="Official rules and eligibility..."></textarea>
          </div>

          <!-- Mail-in Instructions -->
          <div class="col-span-2">
            <label class="block text-sm font-medium mb-1">Mail-in Instructions</label>
            <textarea v-model="raffleForm.mailInInstructions" class="input" rows="2" placeholder="No purchase necessary. To enter by mail..."></textarea>
            <p class="text-xs text-gray-500 mt-1">Required for legal compliance. Leave empty to disable mail-in entries.</p>
          </div>

          <!-- Status -->
          <div>
            <label class="block text-sm font-medium mb-1">Status</label>
            <select v-model="raffleForm.status" class="input">
              <option value="draft">Draft</option>
              <option value="active">Active</option>
              <option value="cancelled">Cancelled</option>
            </select>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-outline" @click="showRaffleDialog = false">Cancel</button>
          <button class="btn-primary" :disabled="savingRaffle" @click="handleSaveRaffle">
            {{ savingRaffle ? 'Saving...' : (editingRaffle ? 'Save Changes' : 'Create Raffle') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Raffle Entries Dialog -->
    <div v-if="showRaffleEntries" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @mousedown.self="showRaffleEntries = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-3xl max-h-[90vh] overflow-y-auto mx-4">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-semibold">
            Entries for {{ selectedRaffleForEntries?.title }}
          </h2>
          <button class="btn-ghost" @click="showRaffleEntries = false">
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <div v-if="raffleEntriesLoading" class="text-center py-8">
          <D20Spinner size="md" class="mx-auto" />
        </div>

        <template v-else>
          <div v-if="raffleEntries.length === 0" class="text-center py-8 text-gray-500">
            No entries yet.
          </div>

          <div v-else class="space-y-2">
            <div
              v-for="entry in raffleEntries"
              :key="entry.id"
              class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
            >
              <div class="flex items-center gap-3">
                <UserAvatar
                  v-if="entry.user"
                  :user="{ avatarUrl: entry.user.avatarUrl, displayName: entry.user.displayName }"
                  size="sm"
                  :show-badge="false"
                />
                <div>
                  <p class="font-medium">{{ entry.user?.displayName || entry.mailInName || 'Anonymous' }}</p>
                  <p class="text-xs text-gray-500">{{ ENTRY_TYPE_LABELS[entry.entryType] }}</p>
                </div>
              </div>
              <div class="flex items-center gap-4">
                <span class="text-sm font-semibold text-primary-600">{{ entry.entryCount }} {{ entry.entryCount === 1 ? 'entry' : 'entries' }}</span>
                <span class="text-xs text-gray-400">{{ new Date(entry.createdAt).toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Raffle Preview Modal -->
    <div v-if="previewingRaffle" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @mousedown.self="previewingRaffle = null">
      <div class="bg-gray-100 rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto mx-4">
        <!-- Header -->
        <div class="bg-white border-b px-6 py-4 flex items-center justify-between sticky top-0 z-10">
          <div>
            <h2 class="text-xl font-semibold">Customer Preview</h2>
            <p class="text-sm text-gray-500">What users will see</p>
          </div>
          <button class="btn-ghost" @click="previewingRaffle = null">
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <div class="p-6 space-y-6">
          <!-- Step 1: Home Page Banner -->
          <div>
            <div class="flex items-center gap-2 mb-3">
              <span class="w-6 h-6 rounded-full bg-primary-500 text-white text-sm font-bold flex items-center justify-center">1</span>
              <h3 class="font-semibold text-gray-900">Home Page Banner</h3>
              <span class="text-xs text-gray-400">sasquatsh.com</span>
            </div>
            <div class="bg-white rounded-xl shadow-sm p-4">
              <!-- Custom Banner -->
              <div v-if="previewingRaffle.bannerImageUrl" class="w-full max-w-lg mx-auto cursor-pointer">
                <div class="rounded-xl overflow-hidden shadow-lg hover:shadow-xl transition-shadow">
                  <img
                    :src="previewingRaffle.bannerImageUrl"
                    :alt="previewingRaffle.title"
                    class="w-full h-auto"
                  />
                </div>
              </div>

              <!-- Dynamic Banner (fallback) -->
              <div v-else class="w-full max-w-lg mx-auto">
                <div class="w-full bg-gradient-to-r from-yellow-400 via-orange-500 to-red-500 rounded-xl p-4 text-white shadow-lg hover:shadow-xl transition-shadow cursor-pointer text-left">
                  <div class="flex items-center gap-4">
                    <div class="relative flex-shrink-0">
                      <div v-if="previewingRaffle.prizeImageUrl" class="w-16 h-16 rounded-lg overflow-hidden bg-white/20 shadow-inner">
                        <img :src="previewingRaffle.prizeImageUrl" :alt="previewingRaffle.prizeName" class="w-full h-full object-cover" />
                      </div>
                      <div v-else class="w-16 h-16 rounded-lg bg-white/20 flex items-center justify-center">
                        <svg class="w-8 h-8 text-white/80" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M9.06,1.93C7.17,1.92 5.33,3.74 6.17,6H3A2,2 0 0,0 1,8V10A1,1 0 0,0 2,11H11V8H13V11H22A1,1 0 0,0 23,10V8A2,2 0 0,0 21,6H17.83C18.67,3.74 16.83,1.92 14.94,1.93C13.5,1.93 12.71,2.71 12,3.5C11.29,2.71 10.5,1.93 9.06,1.93M9.06,3.93C9.57,3.93 10.13,4.26 10.74,4.87L12,6.13L13.26,4.87C13.87,4.26 14.43,3.93 14.94,3.93C15.46,3.93 16,4.5 16,5.19C16,5.9 15.5,6.5 15,6.87L14.5,7.2L13.4,8H10.6L9.5,7.2L9,6.87C8.5,6.5 8,5.9 8,5.19C8,4.5 8.54,3.93 9.06,3.93M2,12V20A2,2 0 0,0 4,22H20A2,2 0 0,0 22,20V12H13V20H11V12H2Z"/>
                        </svg>
                      </div>
                      <div class="absolute -top-1 -right-1 w-4 h-4 bg-yellow-300 rounded-full animate-pulse"></div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="text-xs font-bold uppercase tracking-wider text-yellow-200">Monthly Raffle</span>
                        <span class="text-xs bg-white/20 px-2 py-0.5 rounded-full">30d left</span>
                      </div>
                      <p class="font-bold text-lg truncate">Win: {{ previewingRaffle.prizeName }}</p>
                      <p class="text-sm text-white/80">
                        Sign up to enter
                        <svg class="w-4 h-4 inline ml-1" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M4,11V13H16L10.5,18.5L11.92,19.92L19.84,12L11.92,4.08L10.5,5.5L16,11H4Z"/>
                        </svg>
                      </p>
                    </div>
                  </div>
                </div>
              </div>
              <p class="text-center text-xs text-gray-400 mt-2">User clicks to sign up or view dashboard</p>
            </div>
          </div>

          <!-- Arrow -->
          <div class="flex justify-center">
            <svg class="w-8 h-8 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z"/>
            </svg>
          </div>

          <!-- Step 2: Dashboard Card -->
          <div>
            <div class="flex items-center gap-2 mb-3">
              <span class="w-6 h-6 rounded-full bg-primary-500 text-white text-sm font-bold flex items-center justify-center">2</span>
              <h3 class="font-semibold text-gray-900">Dashboard View</h3>
              <span class="text-xs text-gray-400">After login/signup</span>
            </div>
            <div class="bg-white rounded-xl shadow-sm overflow-hidden">
              <!-- Dashboard Card Header -->
              <div
                class="relative bg-gradient-to-r from-primary-500 to-primary-600 text-white p-4"
                :style="previewingRaffle.bannerImageUrl ? { backgroundImage: `url(${previewingRaffle.bannerImageUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}"
              >
                <div :class="previewingRaffle.bannerImageUrl ? 'bg-black/40 -m-4 p-4' : ''">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <svg class="w-8 h-8 text-yellow-300" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9.06,1.93C7.17,1.92 5.33,3.74 6.17,6H3A2,2 0 0,0 1,8V10A1,1 0 0,0 2,11H11V8H13V11H22A1,1 0 0,0 23,10V8A2,2 0 0,0 21,6H17.83C18.67,3.74 16.83,1.92 14.94,1.93C13.5,1.93 12.71,2.71 12,3.5C11.29,2.71 10.5,1.93 9.06,1.93M9.06,3.93C9.57,3.93 10.13,4.26 10.74,4.87L12,6.13L13.26,4.87C13.87,4.26 14.43,3.93 14.94,3.93C15.46,3.93 16,4.5 16,5.19C16,5.9 15.5,6.5 15,6.87L14.5,7.2L13.4,8H10.6L9.5,7.2L9,6.87C8.5,6.5 8,5.9 8,5.19C8,4.5 8.54,3.93 9.06,3.93M2,12V20A2,2 0 0,0 4,22H20A2,2 0 0,0 22,20V12H13V20H11V12H2Z"/>
                      </svg>
                      <div>
                        <h3 class="font-bold text-lg">{{ previewingRaffle.title }}</h3>
                        <p class="text-white/90 text-sm">30d left</p>
                      </div>
                    </div>
                    <div class="text-right">
                      <p class="text-3xl font-bold">3</p>
                      <p class="text-sm text-white/80">entries</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Dashboard Card Content -->
              <div class="p-4">
                <div class="flex items-start gap-4 mb-4">
                  <div v-if="previewingRaffle.prizeImageUrl" class="w-16 h-16 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100">
                    <img :src="previewingRaffle.prizeImageUrl" :alt="previewingRaffle.prizeName" class="w-full h-full object-cover" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="font-semibold text-gray-900">Prize: {{ previewingRaffle.prizeName }}</p>
                    <p v-if="previewingRaffle.prizeDescription" class="text-sm text-gray-600 line-clamp-2">{{ previewingRaffle.prizeDescription }}</p>
                    <p v-if="previewingRaffle.prizeValueCents" class="text-sm text-primary-600 font-medium mt-1">
                      Value: ${{ (previewingRaffle.prizeValueCents / 100).toFixed(2) }}
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-4 text-sm text-gray-500 mb-4">
                  <span>47 total entries</span>
                  <span>&bull;</span>
                  <span>12 participants</span>
                </div>

                <div class="mb-4">
                  <p class="text-sm font-medium text-gray-700 mb-2">Your entries:</p>
                  <div class="flex flex-wrap gap-2">
                    <span class="text-xs bg-primary-50 text-primary-700 px-2 py-1 rounded-full">Hosted Event (+2)</span>
                    <span class="text-xs bg-primary-50 text-primary-700 px-2 py-1 rounded-full">Attended Event (+1)</span>
                  </div>
                </div>

                <div class="bg-gray-50 rounded-lg p-3">
                  <p class="text-sm font-medium text-gray-700 mb-2">Earn more entries:</p>
                  <ul class="text-sm text-gray-600 space-y-1">
                    <li class="flex items-center gap-2">
                      <svg class="w-4 h-4 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
                      </svg>
                      Host a game night (1-2 entries)
                    </li>
                    <li class="flex items-center gap-2">
                      <svg class="w-4 h-4 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
                      </svg>
                      Plan a group session (1-2 entries)
                    </li>
                    <li class="flex items-center gap-2">
                      <svg class="w-4 h-4 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                      </svg>
                      Attend a game (1 entry)
                    </li>
                  </ul>
                  <p class="text-xs text-gray-400 mt-2">Paid subscribers earn 2x entries for hosting and planning!</p>
                </div>

                <div v-if="previewingRaffle.termsConditions || previewingRaffle.mailInInstructions" class="mt-3 flex items-center justify-between text-xs text-gray-400">
                  <span v-if="previewingRaffle.termsConditions">View Terms & Conditions</span>
                  <span v-if="previewingRaffle.mailInInstructions">Mail-in entry available</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="bg-white border-t px-6 py-4 flex justify-end gap-2 sticky bottom-0">
          <button class="btn-outline" @click="previewingRaffle = null">Close</button>
          <button class="btn-primary" @click="openRaffleDialog(previewingRaffle); previewingRaffle = null">
            Edit Raffle
          </button>
        </div>
      </div>
    </div>

    <!-- Caches Tab -->
    <div v-if="activeTab === 'caches'">
      <!-- Cache Sub-tabs -->
      <div class="flex gap-4 mb-6 border-b border-gray-200">
        <button
          class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
          :class="activeCacheTab === 'bgg'
            ? 'text-primary-600 border-b-2 border-primary-500'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeCacheTab = 'bgg'"
        >
          BoardGameGeek
        </button>
        <button
          class="px-3 py-2 text-sm font-medium transition-colors -mb-px whitespace-nowrap"
          :class="activeCacheTab === 'mtg'
            ? 'text-primary-600 border-b-2 border-primary-500'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeCacheTab = 'mtg'"
        >
          Magic: The Gathering
        </button>
      </div>

      <!-- BGG Cache Sub-tab -->
      <div v-if="activeCacheTab === 'bgg'">
        <div class="card p-6 mb-6">
          <h2 class="text-lg font-semibold mb-4">BoardGameGeek Cache</h2>
        <p class="text-gray-600 mb-6">
          The BGG cache stores board game data locally for fast searching. Games are fetched from BoardGameGeek and cached to avoid rate limits and slow API responses.
        </p>

        <!-- Stats -->
        <div v-if="cacheLoading" class="text-center py-4">
          <D20Spinner size="md" class="mx-auto" />
        </div>
        <div v-else-if="cacheStats" class="grid grid-cols-3 gap-4 mb-6">
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.totalGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">Total Games</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.rankedGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">With Rankings</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-sm font-medium text-gray-900">{{ formatCacheDate(cacheStats.oldestCache) }}</div>
            <div class="text-sm text-gray-500">Oldest Entry</div>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <div class="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
            <div>
              <div class="font-medium text-blue-900">Import Popular Games</div>
              <div class="text-sm text-blue-700">Imports ~100 top-rated BGG games + current hot list</div>
            </div>
            <button
              class="btn-primary"
              :disabled="cacheImporting"
              @click="handleImportPopular"
            >
              <svg v-if="cacheImporting" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Import Popular
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Import Hot Games</div>
              <div class="text-sm text-gray-600">Imports BGG's current "hot" list (~50 games)</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleImportHot"
            >
              Import Hot
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Refresh Stale Entries</div>
              <div class="text-sm text-gray-600">Updates cache entries older than 7 days</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleRefreshStale"
            >
              Refresh Stale
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-yellow-50 rounded-lg border border-yellow-200">
            <div>
              <div class="font-medium text-gray-900">Refresh Incomplete Entries</div>
              <div class="text-sm text-gray-600">Fetches full data for games missing thumbnails/player counts</div>
            </div>
            <button
              class="btn-primary"
              :disabled="cacheImporting"
              @click="handleRefreshIncomplete"
            >
              Fix Incomplete
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Refresh Game Thumbnails</div>
              <div class="text-sm text-gray-600">Fetches missing thumbnails for event games from BGG</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleRefreshThumbnails"
            >
              Refresh Thumbnails
            </button>
          </div>
        </div>
      </div>

      <!-- Browse Cache Entries -->
      <div class="card p-6">
        <h2 class="text-lg font-semibold mb-4">Browse Cache Entries</h2>

        <!-- Search and Filter -->
        <div class="flex flex-wrap gap-4 mb-4">
          <div class="flex-1 min-w-[200px]">
            <input
              v-model="cacheSearch"
              type="text"
              class="input"
              placeholder="Search games by name..."
              @input="handleCacheSearchInput"
            />
          </div>
          <select
            v-model="cacheFilter"
            class="input w-auto"
            @change="handleCacheFilterChange"
          >
            <option value="all">All Games</option>
            <option value="missing_thumbnail">Missing Thumbnail</option>
            <option value="missing_players">Missing Players</option>
            <option value="incomplete">Any Incomplete</option>
          </select>
          <button class="btn-outline" @click="loadCacheGames">
            <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
            </svg>
            Refresh
          </button>
        </div>

        <!-- Results Count -->
        <div class="text-sm text-gray-500 mb-4">
          {{ cacheGamesTotal }} games found
        </div>

        <!-- Loading -->
        <div v-if="cacheGamesLoading" class="text-center py-8">
          <D20Spinner size="md" class="mx-auto" />
        </div>

        <!-- Games List -->
        <div v-else-if="cacheGames.length > 0" class="space-y-2">
          <div
            v-for="game in cacheGames"
            :key="game.bggId"
            class="flex items-center gap-4 p-3 bg-gray-50 rounded-lg hover:bg-gray-100"
          >
            <!-- Thumbnail -->
            <div class="w-12 h-12 rounded bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="game.thumbnailUrl"
                :src="game.thumbnailUrl"
                :alt="game.name"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-6 h-6 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,5V11.59L18,8.58L14,12.59L10,8.59L6,12.59L3,9.58V5A2,2 0 0,1 5,3H19A2,2 0 0,1 21,5M18,11.42L21,14.43V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V12.42L6,15.41L10,11.41L14,15.41"/>
              </svg>
            </div>

            <!-- Info -->
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900 truncate">{{ game.name }}</div>
              <div class="text-sm text-gray-500">
                BGG #{{ game.bggId }}
                <span v-if="game.yearPublished"> &bull; {{ game.yearPublished }}</span>
                <span v-if="game.minPlayers && game.maxPlayers"> &bull; {{ game.minPlayers }}-{{ game.maxPlayers }} players</span>
                <span v-if="game.bggRank" class="text-primary-600"> &bull; Rank #{{ game.bggRank }}</span>
              </div>
            </div>

            <!-- Status badges -->
            <div class="flex gap-2 flex-shrink-0">
              <span v-if="!game.thumbnailUrl" class="text-xs px-2 py-1 rounded bg-yellow-100 text-yellow-700">
                No Image
              </span>
              <span v-if="!game.minPlayers" class="text-xs px-2 py-1 rounded bg-orange-100 text-orange-700">
                No Players
              </span>
            </div>

            <!-- Actions -->
            <div class="flex gap-2 flex-shrink-0">
              <button
                class="btn-sm btn-outline"
                :disabled="refreshingGameId === game.bggId"
                @click="handleRefreshSingleGame(game.bggId)"
              >
                <svg v-if="refreshingGameId === game.bggId" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
                </svg>
              </button>
              <button
                class="btn-sm btn-outline"
                @click="openCacheEditDialog(game)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="text-center py-8 text-gray-500">
          No games found. Try a different search or filter.
        </div>

        <!-- Pagination -->
        <div v-if="cacheGamesTotal > 20" class="flex justify-center gap-2 mt-4">
          <button
            class="btn-outline btn-sm"
            :disabled="cacheGamesPage === 1"
            @click="handleCachePageChange(cacheGamesPage - 1)"
          >
            Previous
          </button>
          <span class="px-3 py-1 text-sm text-gray-600">
            Page {{ cacheGamesPage }} of {{ Math.ceil(cacheGamesTotal / 20) }}
          </span>
          <button
            class="btn-outline btn-sm"
            :disabled="cacheGamesPage >= Math.ceil(cacheGamesTotal / 20)"
            @click="handleCachePageChange(cacheGamesPage + 1)"
          >
            Next
          </button>
        </div>
      </div>

      <!-- Edit Cache Entry Dialog -->
      <div v-if="showCacheEditDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
        <div class="bg-white rounded-xl max-w-lg w-full p-6">
          <h3 class="text-lg font-semibold mb-4">Edit Game: {{ editingCacheGame?.name }}</h3>

          <div class="space-y-4">
            <div>
              <label class="label">Thumbnail URL</label>
              <input v-model="cacheEditForm.thumbnailUrl" type="url" class="input" placeholder="https://...">
            </div>
            <div>
              <label class="label">Image URL</label>
              <input v-model="cacheEditForm.imageUrl" type="url" class="input" placeholder="https://...">
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="label">Min Players</label>
                <input v-model.number="cacheEditForm.minPlayers" type="number" class="input" min="1">
              </div>
              <div>
                <label class="label">Max Players</label>
                <input v-model.number="cacheEditForm.maxPlayers" type="number" class="input" min="1">
              </div>
            </div>

            <!-- Preview -->
            <div v-if="cacheEditForm.thumbnailUrl" class="mt-4">
              <label class="label">Preview</label>
              <img :src="cacheEditForm.thumbnailUrl" class="w-24 h-24 rounded object-cover" />
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-6">
            <button class="btn-outline" @click="closeCacheEditDialog">Cancel</button>
            <button class="btn-primary" :disabled="savingCacheGame" @click="handleSaveCacheGame">
              {{ savingCacheGame ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </div>
      </div>
      </div>

      <!-- MTG/Scryfall Cache Sub-tab -->
      <div v-if="activeCacheTab === 'mtg'">
        <div class="card p-6">
          <h2 class="text-lg font-semibold mb-4">Magic: The Gathering Cache</h2>
        <p class="text-gray-600 mb-6">
          The Scryfall cache stores MTG card data locally for fast searching. Cards are fetched from Scryfall API and cached to avoid rate limits.
        </p>

        <!-- Stats -->
        <div v-if="mtgCacheLoading" class="text-center py-4">
          <D20Spinner size="md" class="mx-auto" />
        </div>
        <div v-else-if="mtgCacheStats" class="grid grid-cols-4 gap-4 mb-6">
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ mtgCacheStats.totalCards.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">Total Cards</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ mtgCacheStats.staplesListSize }}</div>
            <div class="text-sm text-gray-500">Staples List</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold" :class="mtgCacheStats.staleCount > 0 ? 'text-orange-600' : 'text-green-600'">{{ mtgCacheStats.staleCount }}</div>
            <div class="text-sm text-gray-500">Stale Entries</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-sm font-medium text-gray-900">{{ formatCacheDate(mtgCacheStats.newestEntry) }}</div>
            <div class="text-sm text-gray-500">Last Cached</div>
          </div>
        </div>

        <!-- Last warm result -->
        <div v-if="mtgWarmResult" class="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
          <div class="font-medium text-green-900">Last Warm Result: {{ mtgWarmResult.action }}</div>
          <div class="text-sm text-green-700">
            Cached: {{ mtgWarmResult.cached }} | Skipped: {{ mtgWarmResult.skipped }} | Failed: {{ mtgWarmResult.failed }}
          </div>
          <div v-if="mtgWarmResult.errors && mtgWarmResult.errors.length > 0" class="mt-2 text-xs text-red-600">
            Errors: {{ mtgWarmResult.errors.slice(0, 3).join(', ') }}
            <span v-if="mtgWarmResult.errors.length > 3">... and {{ mtgWarmResult.errors.length - 3 }} more</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <div class="flex items-center justify-between p-4 bg-purple-50 rounded-lg">
            <div>
              <div class="font-medium text-purple-900">Warm Commander Staples</div>
              <div class="text-sm text-purple-700">Caches ~170 most-played Commander cards (Sol Ring, Command Tower, etc.)</div>
            </div>
            <button
              class="btn-primary"
              :disabled="mtgCacheWarming"
              @click="handleWarmMtgStaples"
            >
              <svg v-if="mtgCacheWarming" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Warm Staples
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Warm Commanders</div>
              <div class="text-sm text-gray-600">Caches legendary creatures via Scryfall search (5 pages)</div>
            </div>
            <button
              class="btn-outline"
              :disabled="mtgCacheWarming"
              @click="handleWarmMtgCommanders"
            >
              Warm Commanders
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Refresh Stale Entries</div>
              <div class="text-sm text-gray-600">Re-fetches cards older than 24 hours</div>
            </div>
            <button
              class="btn-outline"
              :disabled="mtgCacheWarming || !mtgCacheStats || mtgCacheStats.staleCount === 0"
              @click="handleRefreshMtgStale"
            >
              Refresh Stale
            </button>
          </div>

          <!-- Progress indicator -->
          <div v-if="mtgWarmProgress" class="p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div class="flex items-center gap-3">
              <svg class="animate-spin h-5 w-5 text-blue-500" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              <span class="text-sm text-blue-700 font-medium">{{ mtgWarmProgress }}</span>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>

    <!-- Locations Tab -->
    <div v-if="activeTab === 'locations'">
      <div class="flex items-center justify-between mb-6">
        <div>
          <p class="text-gray-500">Manage event locations for Looking For Players</p>
          <p v-if="pendingLocations.length > 0" class="text-sm text-orange-600 font-medium mt-1">
            {{ pendingLocations.length }} pending submission(s) need review
          </p>
        </div>
        <div class="flex gap-2">
          <button
            v-if="!mergeMode"
            class="btn-outline"
            @click="toggleMergeMode"
          >
            Merge Duplicates
          </button>
          <button
            v-if="mergeMode"
            class="btn-ghost"
            @click="toggleMergeMode"
          >
            Cancel
          </button>
          <button
            v-if="mergeMode && selectedForMerge.length >= 2"
            class="btn-primary"
            @click="handleMerge"
          >
            Merge Selected ({{ selectedForMerge.length }})
          </button>
          <button
            v-if="!mergeMode"
            class="btn-primary"
            @click="openCreateDialog"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Location
          </button>
        </div>
      </div>

    <!-- Merge instructions -->
    <div v-if="mergeMode" class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
      <p class="text-blue-800">
        <strong>Merge Mode:</strong> Select locations to merge. The first selected location will be kept, and all others will be deleted. Any LFP posts using the deleted locations will be updated to use the kept one.
      </p>
    </div>

    <!-- Pending Venues -->
    <div v-if="pendingLocations.length > 0 && !mergeMode" class="mb-6">
      <h3 class="text-lg font-semibold text-orange-600 mb-3 flex items-center gap-2">
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,4A8,8 0 0,1 20,12A8,8 0 0,1 12,20A8,8 0 0,1 4,12A8,8 0 0,1 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        Pending Review ({{ pendingLocations.length }})
      </h3>
      <div class="space-y-3">
        <div
          v-for="location in pendingLocations"
          :key="location.id"
          class="card p-4 border-2 border-orange-200 bg-orange-50"
        >
          <div class="flex items-start gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h4 class="font-semibold text-gray-900">{{ location.name }}</h4>
                <span class="text-xs px-2 py-0.5 rounded-full bg-orange-100 text-orange-700">
                  Pending
                </span>
              </div>
              <p class="text-sm text-gray-600 mt-1">
                <span v-if="location.addressLine1">{{ location.addressLine1 }}, </span>{{ location.city }}, {{ location.state }}
                <span v-if="location.venue"> &bull; {{ location.venue }}</span>
              </p>
              <p class="text-sm text-gray-500">
                <template v-if="location.isPermanent">
                  <span class="text-emerald-600 font-medium">Permanent Location</span>
                </template>
                <template v-else-if="location.recurringDays && location.recurringDays.length > 0">
                  <span class="text-blue-600 font-medium">{{ getLocationTypeLabel(location) }}</span>
                </template>
                <template v-else>
                  {{ formatDate(location.startDate) }} - {{ formatDate(location.endDate) }}
                </template>
              </p>
              <p v-if="location.createdBy" class="text-xs text-gray-400 mt-1">
                Submitted by {{ location.createdBy.displayName || 'User' }}
              </p>
            </div>
            <div class="flex gap-2 flex-shrink-0">
              <button
                class="btn-primary text-sm py-1.5 px-3"
                :disabled="approvingId === location.id || rejectingId === location.id"
                @click="handleApprove(location)"
              >
                <svg v-if="approvingId === location.id" class="animate-spin -ml-1 mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Approve
              </button>
              <button
                class="btn-ghost text-red-600 text-sm py-1.5 px-3"
                :disabled="approvingId === location.id || rejectingId === location.id"
                @click="handleReject(location)"
              >
                <svg v-if="rejectingId === location.id" class="animate-spin -ml-1 mr-1 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Reject
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Location List -->
    <template v-else>
      <div v-if="locations.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
        </svg>
        <p class="text-lg font-medium">No event locations yet</p>
        <p>Add your first event location to get started.</p>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="location in locations"
          :key="location.id"
          class="card p-4 flex items-center gap-4"
          :class="{
            'opacity-50': isLocationExpired(location),
            'ring-2 ring-primary-500': selectedForMerge.includes(location.id),
          }"
        >
          <!-- Merge checkbox -->
          <div v-if="mergeMode" class="flex-shrink-0">
            <input
              type="checkbox"
              :checked="selectedForMerge.includes(location.id)"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              @change="toggleSelectForMerge(location.id)"
            />
          </div>

          <!-- Location info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-gray-900">{{ location.name }}</h3>
              <span
                class="text-xs px-2 py-0.5 rounded-full"
                :class="{
                  'bg-green-100 text-green-700': location.status === 'approved',
                  'bg-orange-100 text-orange-700': location.status === 'pending',
                  'bg-red-100 text-red-700': location.status === 'rejected',
                }"
              >
                {{ location.status }}
              </span>
              <span v-if="isLocationExpired(location)" class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">
                Expired
              </span>
              <span v-if="location.isPermanent" class="text-xs px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700">
                Permanent
              </span>
              <span v-else-if="location.recurringDays && location.recurringDays.length > 0" class="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                Recurring
              </span>
              <span v-if="selectedForMerge[0] === location.id" class="text-xs px-2 py-0.5 rounded-full bg-primary-100 text-primary-700">
                Keep this one
              </span>
            </div>
            <p class="text-sm text-gray-500">
              {{ location.city }}, {{ location.state }}
              <span v-if="location.venue"> &bull; {{ location.venue }}</span>
            </p>
            <p class="text-sm text-gray-500">
              <template v-if="location.isPermanent">
                Always available
              </template>
              <template v-else-if="location.recurringDays && location.recurringDays.length > 0">
                {{ getLocationTypeLabel(location) }}
              </template>
              <template v-else-if="location.startDate && location.endDate">
                {{ formatDate(location.startDate) }} - {{ formatDate(location.endDate) }}
              </template>
              <template v-else>
                No dates set
              </template>
              <span v-if="location.eventCount" class="ml-2 text-primary-600">
                ({{ location.eventCount }} event{{ location.eventCount === 1 ? '' : 's' }})
              </span>
              <span v-if="location.userCount" class="ml-2 text-secondary-600">
                ({{ location.userCount }} user{{ location.userCount === 1 ? '' : 's' }})
              </span>
            </p>
          </div>

          <!-- Actions -->
          <div v-if="!mergeMode" class="flex gap-2 flex-shrink-0">
            <button
              class="btn-ghost text-gray-600 text-sm"
              @click="openEditDialog(location)"
            >
              Edit
            </button>
            <button
              class="btn-ghost text-red-600 text-sm"
              @click="handleDelete(location)"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Create/Edit Dialog -->
    <div v-if="showDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-4">
          {{ editingLocation ? 'Edit Location' : 'Add Event Location' }}
        </h3>

        <div class="space-y-4">
          <!-- Location Type -->
          <div>
            <label class="label">Location Type *</label>
            <div class="flex gap-2">
              <button
                type="button"
                class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
                :class="form.locationType === 'temporary'
                  ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
                @click="form.locationType = 'temporary'"
              >
                Event/Convention
              </button>
              <button
                type="button"
                class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
                :class="form.locationType === 'permanent'
                  ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
                @click="form.locationType = 'permanent'"
              >
                Permanent
              </button>
              <button
                type="button"
                class="flex-1 px-3 py-2 text-sm rounded-lg border transition-colors"
                :class="form.locationType === 'recurring'
                  ? 'bg-emerald-100 border-emerald-500 text-emerald-700'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
                @click="form.locationType = 'recurring'"
              >
                Recurring
              </button>
            </div>
          </div>

          <div>
            <label class="label">{{ form.locationType === 'temporary' ? 'Event Name' : 'Location Name' }} *</label>
            <input
              v-model="form.name"
              type="text"
              class="input"
              :placeholder="form.locationType === 'temporary' ? 'e.g., Dice Tower West 2026' : 'e.g., Dragon\'s Lair Comics'"
            />
          </div>

          <div>
            <label class="label">Street Address</label>
            <input
              v-model="form.addressLine1"
              type="text"
              class="input"
              placeholder="e.g., 3000 Paradise Rd"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">City *</label>
              <input
                v-model="form.city"
                type="text"
                class="input"
                placeholder="City"
              />
            </div>
            <div>
              <label class="label">State *</label>
              <StateSelect v-model="form.state" />
            </div>
          </div>

          <div>
            <label class="label">Venue/Building</label>
            <input
              v-model="form.venue"
              type="text"
              class="input"
              placeholder="e.g., Las Vegas Convention Center"
            />
          </div>

          <!-- Dates (only for temporary) -->
          <div v-if="form.locationType === 'temporary'" class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Start Date *</label>
              <input
                v-model="form.startDate"
                type="date"
                class="input"
              />
            </div>
            <div>
              <label class="label">End Date *</label>
              <input
                v-model="form.endDate"
                type="date"
                class="input"
                :min="form.startDate"
              />
            </div>
          </div>

          <!-- Recurring Days (only for recurring) -->
          <div v-if="form.locationType === 'recurring'">
            <label class="label">Game Night Days *</label>
            <div class="flex gap-1">
              <button
                v-for="(name, idx) in dayNames"
                :key="idx"
                type="button"
                class="w-10 h-10 rounded-lg text-sm font-medium transition-colors"
                :class="form.recurringDays.includes(idx)
                  ? 'bg-emerald-500 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                @click="toggleRecurringDay(idx)"
              >
                {{ name }}
              </button>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showDialog = false" :disabled="saving">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSave" :disabled="saving">
            <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingLocation ? 'Save Changes' : 'Add Location' }}
          </button>
        </div>
      </div>
    </div>
    </div>

    <!-- Suspend User Dialog -->
    <div v-if="showSuspendDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showSuspendDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Suspend User</h3>

        <p class="text-gray-600 mb-4">
          Are you sure you want to suspend <strong>@{{ userToSuspend?.username }}</strong>?
          They will not be able to log in while suspended.
        </p>

        <div class="mb-4">
          <label class="label">Reason (optional)</label>
          <textarea
            v-model="suspendReason"
            class="input h-24 resize-none"
            placeholder="Enter a reason for the suspension..."
          ></textarea>
        </div>

        <div class="flex justify-end gap-3">
          <button class="btn-ghost" @click="showSuspendDialog = false" :disabled="suspendingUserId !== null">
            Cancel
          </button>
          <button class="btn-primary bg-red-600 hover:bg-red-700" @click="handleSuspendUser" :disabled="suspendingUserId !== null">
            <svg v-if="suspendingUserId !== null" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Suspend User
          </button>
        </div>
      </div>
    </div>

    <!-- Ban User Dialog -->
    <div v-if="showBanDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showBanDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4 text-red-600">Permanently Ban User</h3>

        <p class="text-gray-600 mb-4">
          Are you sure you want to <strong>permanently ban</strong> <strong>@{{ userToBan?.username }}</strong>?
          This will prevent them from accessing their account permanently.
        </p>

        <div class="bg-red-50 border border-red-200 rounded p-3 mb-4 text-sm text-red-700">
          <strong>Warning:</strong> This action is different from suspension. Bans are permanent and should be used for serious violations.
        </div>

        <div class="mb-4">
          <label class="label">Reason (recommended)</label>
          <textarea
            v-model="banReason"
            class="input h-24 resize-none"
            placeholder="Enter a reason for the ban..."
          ></textarea>
        </div>

        <div class="flex justify-end gap-3">
          <button class="btn-ghost" @click="showBanDialog = false" :disabled="banningUserId !== null">
            Cancel
          </button>
          <button class="btn-primary bg-red-600 hover:bg-red-700" @click="handleBanUser" :disabled="banningUserId !== null">
            <svg v-if="banningUserId !== null" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Ban User Permanently
          </button>
        </div>
      </div>
    </div>

    <!-- Set Tier Dialog -->
    <div v-if="showTierDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showTierDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Set Subscription Tier</h3>

        <p class="text-gray-600 mb-4">
          Set subscription tier for <strong>@{{ userToSetTier?.username }}</strong>.
          This creates an override that bypasses their actual subscription.
        </p>

        <div class="space-y-4 mb-6">
          <div>
            <label class="label">Tier</label>
            <select v-model="tierForm.tier" class="input">
              <option value="free">Free (remove override)</option>
              <option value="basic">Basic ($4.99 value)</option>
              <option value="pro">Pro ($7.99 value)</option>
              <option value="premium">Premium (all features)</option>
            </select>
            <p class="text-sm text-gray-500 mt-1">
              Current: {{ userToSetTier?.subscriptionTier || 'free' }}
              <span v-if="userToSetTier?.subscriptionOverrideTier">
                (override: {{ userToSetTier.subscriptionOverrideTier }})
              </span>
            </p>
          </div>

          <div>
            <label class="label">Reason</label>
            <textarea
              v-model="tierForm.reason"
              class="input h-20 resize-none"
              placeholder="e.g., Beta tester, Community contributor, Support case #123"
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3">
          <button class="btn-ghost" @click="showTierDialog = false" :disabled="settingTierId !== null">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSetTier" :disabled="settingTierId !== null">
            <svg v-if="settingTierId !== null" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ tierForm.tier === 'free' ? 'Remove Override' : 'Set Tier' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Note Dialog -->
    <div v-if="showNoteDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showNoteDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-4">{{ editingNote ? 'Edit Note' : 'Add Note' }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">Title *</label>
            <input
              v-model="noteForm.title"
              type="text"
              class="input"
              placeholder="Note title"
            />
          </div>

          <div>
            <label class="label">Category</label>
            <input
              v-model="noteForm.category"
              type="text"
              class="input"
              placeholder="e.g., general, feature, todo"
            />
          </div>

          <div>
            <label class="label">Content *</label>
            <textarea
              v-model="noteForm.content"
              class="input h-40 resize-none"
              placeholder="Note content..."
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showNoteDialog = false" :disabled="savingNote">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveNote" :disabled="savingNote">
            <svg v-if="savingNote" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingNote ? 'Save Changes' : 'Add Note' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Bug Dialog -->
    <div v-if="showBugDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showBugDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10 max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">{{ editingBug ? 'Edit Bug' : 'Report Bug' }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">Title *</label>
            <input
              v-model="bugForm.title"
              type="text"
              class="input"
              placeholder="Brief description of the bug"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Priority</label>
              <select v-model="bugForm.priority" class="input">
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
                <option value="critical">Critical</option>
              </select>
            </div>
            <div v-if="editingBug">
              <label class="label">Status</label>
              <select v-model="bugForm.status" class="input">
                <option value="open">Open</option>
                <option value="in_progress">In Progress</option>
                <option value="resolved">Resolved</option>
                <option value="closed">Closed</option>
                <option value="wont_fix">Won't Fix</option>
              </select>
            </div>
          </div>

          <div>
            <label class="label">Description</label>
            <textarea
              v-model="bugForm.description"
              class="input h-24 resize-none"
              placeholder="Detailed description of the bug..."
            ></textarea>
          </div>

          <div>
            <label class="label">Steps to Reproduce</label>
            <textarea
              v-model="bugForm.stepsToReproduce"
              class="input h-24 resize-none"
              placeholder="1. Go to...&#10;2. Click on...&#10;3. See error"
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showBugDialog = false" :disabled="savingBug">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveBug" :disabled="savingBug">
            <svg v-if="savingBug" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingBug ? 'Save Changes' : 'Report Bug' }}
          </button>
        </div>
      </div>
    </div>

    <!-- User Edit Dialog -->
    <div v-if="showUserEditDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showUserEditDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Edit User</h3>

        <div v-if="editingUser" class="space-y-4">
          <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
            <UserAvatar
              :avatar-url="editingUser.avatarUrl"
              :display-name="editingUser.displayName"
              :is-founding-member="editingUser.isFoundingMember"
              :is-admin="editingUser.isAdmin"
              size="md"
              class="flex-shrink-0"
            />
            <div>
              <div class="font-medium">{{ editingUser.email }}</div>
              <div class="text-sm text-gray-500">ID: {{ editingUser.id.slice(0, 8) }}...</div>
            </div>
          </div>

          <div>
            <label class="label">Display Name</label>
            <input
              v-model="userEditForm.displayName"
              type="text"
              class="input"
              placeholder="Display name (optional)"
            />
          </div>

          <div>
            <label class="label">Username *</label>
            <div class="flex items-center">
              <span class="text-gray-500 mr-1">@</span>
              <input
                v-model="userEditForm.username"
                type="text"
                class="input flex-1"
                placeholder="username"
              />
            </div>
            <p class="text-xs text-gray-500 mt-1">3-30 characters, starts with letter, alphanumeric + underscore only</p>
          </div>

          <div class="flex items-center gap-6">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="userEditForm.isAdmin"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              />
              <span>Admin privileges</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="userEditForm.isFoundingMember"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-amber-500 focus:ring-amber-500"
              />
              <span class="flex items-center gap-1">
                <svg class="w-4 h-4 text-amber-500" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M5,16L3,5L8.5,10L12,4L15.5,10L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z"/>
                </svg>
                Founding Member
              </span>
            </label>
          </div>

          <!-- Subscription Tier Section -->
          <div class="border-t border-gray-200 pt-4 mt-4">
            <h4 class="font-medium text-gray-900 mb-3">Subscription Tier</h4>
            <div class="space-y-3">
              <div>
                <label class="label">Tier (free from payment)</label>
                <select v-model="userEditForm.tier" class="input">
                  <option value="free">Free</option>
                  <option value="basic">Basic ($4.99 value)</option>
                  <option value="pro">Pro ($7.99 value)</option>
                  <option value="premium">Premium (all features)</option>
                </select>
                <p class="text-xs text-gray-500 mt-1">
                  Actual subscription: {{ editingUser?.subscriptionTier || 'free' }}
                  <span v-if="editingUser?.subscriptionOverrideTier" class="text-blue-600">
                    (current override: {{ editingUser.subscriptionOverrideTier }})
                  </span>
                </p>
              </div>
              <div v-if="userEditForm.tier !== 'free'">
                <label class="label">Override Reason</label>
                <input
                  v-model="userEditForm.tierReason"
                  type="text"
                  class="input"
                  placeholder="e.g., Beta tester, Support case, Promo"
                />
              </div>
            </div>
          </div>

          <!-- Badges Section -->
          <div class="border-t border-gray-200 pt-4 mt-4">
            <h4 class="font-medium text-gray-900 mb-3">Badges ({{ userBadgeIds.size }} earned)</h4>
            <div v-if="badgesLoading" class="flex justify-center py-4">
              <svg class="animate-spin h-5 w-5 text-primary-500" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
            </div>
            <div v-else class="max-h-48 overflow-y-auto space-y-1">
              <button
                v-for="badge in allBadgesList"
                :key="badge.id"
                @click="toggleUserBadge(badge.id)"
                :disabled="badgeUpdating === badge.id"
                class="w-full flex items-center gap-2 px-2 py-1.5 rounded text-left text-sm transition-colors"
                :class="userBadgeIds.has(badge.id) ? 'bg-primary-50 hover:bg-primary-100' : 'hover:bg-gray-50'"
              >
                <div class="w-5 h-5 flex-shrink-0">
                  <svg v-if="badgeUpdating === badge.id" class="w-5 h-5 animate-spin text-gray-400" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  <div
                    v-else
                    class="w-5 h-5 border-2 rounded flex items-center justify-center"
                    :class="userBadgeIds.has(badge.id) ? 'bg-primary-500 border-primary-500' : 'border-gray-300'"
                  >
                    <svg v-if="userBadgeIds.has(badge.id)" class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
                    </svg>
                  </div>
                </div>
                <div v-if="badge.icon_svg" v-html="badge.icon_svg" class="w-5 h-5 flex-shrink-0" :class="!userBadgeIds.has(badge.id) ? 'grayscale opacity-40' : ''"></div>
                <span class="flex-1 truncate" :class="userBadgeIds.has(badge.id) ? 'text-gray-900' : 'text-gray-400'">
                  {{ badge.name }}
                </span>
                <span class="text-[10px] px-1.5 py-0.5 rounded-full flex-shrink-0"
                  :class="{
                    'bg-amber-50 text-amber-700': badge.tier === 'bronze',
                    'bg-gray-100 text-gray-600': badge.tier === 'silver',
                    'bg-yellow-50 text-yellow-700': badge.tier === 'gold',
                    'bg-emerald-50 text-emerald-700': badge.tier === 'platinum',
                  }"
                >{{ badge.tier }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showUserEditDialog = false" :disabled="savingUser">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveUser" :disabled="savingUser">
            <svg v-if="savingUser" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Save Changes
          </button>
        </div>
      </div>
    </div>

    <!-- Group Members Dialog -->
    <div v-if="showMembersDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showMembersDialog = false"></div>
      <div class="card p-6 w-full max-w-2xl relative z-10 max-h-[80vh] flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold">
            Members of "{{ selectedGroup?.name }}"
          </h3>
          <button
            class="btn-primary text-sm"
            @click="openAddMemberDialog"
          >
            <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Add Member
          </button>
        </div>

        <div class="flex-1 overflow-y-auto">
          <div v-if="membersLoading" class="flex justify-center py-8">
            <D20Spinner size="lg" />
          </div>

          <div v-else-if="groupMembers.length === 0" class="text-center py-8 text-gray-500">
            No members in this group
          </div>

          <div v-else class="space-y-2">
            <div
              v-for="member in groupMembers"
              :key="member.id"
              class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
            >
              <!-- Avatar -->
              <UserAvatar
                :avatar-url="member.avatarUrl"
                :display-name="member.displayName"
                :is-founding-member="member.isFoundingMember"
                :is-admin="member.isAdmin"
                size="md"
                class="flex-shrink-0"
              />

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium">{{ member.displayName || member.username }}</span>
                  <span class="text-gray-500 text-sm">@{{ member.username }}</span>
                  <span :class="['text-xs px-2 py-0.5 rounded-full', getRoleBadgeClass(member.role)]">
                    {{ member.role }}
                  </span>
                </div>
                <p class="text-sm text-gray-500 truncate">{{ member.email }}</p>
              </div>

              <!-- Actions -->
              <div class="flex items-center gap-2 flex-shrink-0">
                <!-- Role dropdown -->
                <select
                  :value="member.role"
                  :disabled="changingRoleId === member.userId"
                  class="text-sm border border-gray-300 rounded px-2 py-1"
                  @change="handleChangeRole(member, ($event.target as HTMLSelectElement).value as 'owner' | 'admin' | 'member')"
                >
                  <option value="member">Member</option>
                  <option value="admin">Admin</option>
                  <option value="owner">Owner</option>
                </select>

                <!-- Remove button -->
                <button
                  class="btn-ghost text-red-600 text-sm p-1"
                  :disabled="removingMemberId === member.userId"
                  title="Remove from group"
                  @click="handleRemoveMember(member)"
                >
                  <svg v-if="removingMemberId === member.userId" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                  </svg>
                  <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end mt-4 pt-4 border-t">
          <button class="btn-ghost" @click="showMembersDialog = false">
            Close
          </button>
        </div>
      </div>
    </div>

    <!-- Add Member Dialog -->
    <div v-if="showAddMemberDialog" class="fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showAddMemberDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Add Member</h3>

        <div class="mb-4">
          <label class="label">Search Users</label>
          <input
            v-model="addMemberSearch"
            type="text"
            class="input"
            placeholder="Search by email, username, or name..."
            @input="handleAddMemberSearch"
          />
        </div>

        <div class="max-h-64 overflow-y-auto">
          <div v-if="addMemberSearching" class="text-center py-4">
            <D20Spinner size="sm" />
          </div>

          <div v-else-if="addMemberSearch && addMemberResults.length === 0" class="text-center py-4 text-gray-500">
            No users found
          </div>

          <div v-else class="space-y-2">
            <button
              v-for="user in addMemberResults"
              :key="user.id"
              class="w-full flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 text-left"
              :disabled="addingMemberId === user.id"
              @click="handleAddMember(user)"
            >
              <UserAvatar
                :avatar-url="user.avatarUrl"
                :display-name="user.displayName"
                :is-founding-member="user.isFoundingMember"
                :is-admin="user.isAdmin"
                size="sm"
                class="flex-shrink-0"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium">{{ user.displayName || user.username }}</div>
                <div class="text-sm text-gray-500">@{{ user.username }} &bull; {{ user.email }}</div>
              </div>
              <div v-if="addingMemberId === user.id">
                <svg class="animate-spin h-4 w-4 text-primary-500" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
              </div>
              <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="flex justify-end mt-4 pt-4 border-t">
          <button class="btn-ghost" @click="showAddMemberDialog = false">
            Cancel
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
