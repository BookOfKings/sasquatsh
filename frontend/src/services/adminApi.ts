import type { EventLocation } from '@/types/social'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// ============ Admin Dashboard Types ============

export interface PopularGame {
  name: string
  bggId: number | null
  count: number
  thumbnailUrl: string | null
}

export interface TodaySignup {
  id: string
  username: string
  email: string
  displayName: string | null
  avatarUrl: string | null
  createdAt: string
}

export interface AdminStats {
  users: {
    total: number
    last7Days: number
    last30Days: number
    byTier: {
      free: number
      basic: number
      pro: number
    }
    tierBreakdown?: {
      basicPaid: number
      basicUpgraded: number
      proPaid: number
      proUpgraded: number
    }
    todaySignups?: TodaySignup[]
  }
  revenue: {
    projectedMonthly: number
    basicPaidCount: number
    proPaidCount: number
    basicCount: number
    proCount: number
  }
  groups: {
    total: number
    public: number
    private: number
  }
  events: {
    total: number
    upcoming: number
  }
  planningSessions: {
    total: number
    open: number
  }
  playerRequests: {
    total: number
    active: number
  }
  bggCache: {
    total: number
  }
  popularGames: PopularGame[]
}

export interface ServiceHealth {
  name: string
  status: 'healthy' | 'degraded' | 'unhealthy'
  latencyMs?: number
  message?: string
}

export interface AdminDashboardData {
  stats: AdminStats
  services: ServiceHealth[]
}

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// ============ Event Location Admin ============

// Get all locations including expired (admin only)
export async function getAllLocations(token: string): Promise<EventLocation[]> {
  return authenticatedRequest<EventLocation[]>('/event-locations?all=true', token)
}

// Create location (admin only)
export async function createLocation(
  token: string,
  data: {
    name: string
    city: string
    state: string
    venue?: string
    startDate?: string | null
    endDate?: string | null
    isPermanent?: boolean
    recurringDays?: number[] | null
  }
): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>('/event-locations', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Update location (admin only)
export async function updateLocation(
  token: string,
  id: string,
  data: Partial<EventLocation>
): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>(`/event-locations?id=${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

// Delete location (admin only)
export async function deleteLocation(token: string, id: string): Promise<void> {
  return authenticatedRequest<void>(`/event-locations?id=${id}`, token, {
    method: 'DELETE',
  })
}

// Get pending locations (admin only)
export async function getPendingLocations(token: string): Promise<EventLocation[]> {
  return authenticatedRequest<EventLocation[]>('/event-locations?status=pending', token)
}

// Approve location (admin only)
export async function approveLocation(token: string, id: string): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>(`/event-locations?id=${id}&action=approve`, token, {
    method: 'PUT',
  })
}

// Reject location (admin only)
export async function rejectLocation(token: string, id: string): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>(`/event-locations?id=${id}&action=reject`, token, {
    method: 'PUT',
  })
}

// Merge duplicate locations (admin only)
// Keeps the first location and updates all player_requests to point to it
export async function mergeLocations(
  token: string,
  keepId: string,
  removeIds: string[]
): Promise<{ merged: number; keptId: string }> {
  return authenticatedRequest<{ merged: number; keptId: string }>('/event-locations?action=merge', token, {
    method: 'POST',
    body: JSON.stringify({ keepId, removeIds }),
  })
}

// ============ BGG Cache Admin ============

export interface BggCacheStats {
  totalGames: number
  rankedGames: number
  oldestCache: string | null
}

export interface BggCacheImportResult {
  message: string
  imported: number
  refreshed?: number
}

// Get BGG cache statistics
export async function getBggCacheStats(token: string): Promise<BggCacheStats> {
  return authenticatedRequest<BggCacheStats>('/bgg-cache?action=stats', token)
}

// Import popular games (top BGG + hot list)
export async function importPopularGames(token: string): Promise<BggCacheImportResult> {
  return authenticatedRequest<BggCacheImportResult>('/bgg-cache?action=import-popular', token, {
    method: 'POST',
  })
}

// Import hot games from BGG
export async function importHotGames(token: string): Promise<BggCacheImportResult> {
  return authenticatedRequest<BggCacheImportResult>('/bgg-cache?action=import-hot', token, {
    method: 'POST',
  })
}

// Refresh stale cache entries
export async function refreshStaleCache(token: string): Promise<BggCacheImportResult> {
  return authenticatedRequest<BggCacheImportResult>('/bgg-cache?action=refresh-stale', token, {
    method: 'POST',
  })
}

// Refresh incomplete cache entries (missing thumbnail/player data)
export async function refreshIncompleteCache(token: string): Promise<BggCacheImportResult> {
  return authenticatedRequest<BggCacheImportResult>('/bgg-cache?action=refresh-incomplete', token, {
    method: 'POST',
  })
}

// Refresh BGG thumbnails for event games missing them
export async function refreshBggThumbnails(token: string): Promise<{ message: string; refreshed: number; total: number; errors?: string[] }> {
  return authenticatedRequest<{ message: string; refreshed: number; total: number; errors?: string[] }>('/admin-stats?action=refresh-bgg-cache', token, {
    method: 'POST',
  })
}

// Import games by ID range
export async function importGamesByRange(
  token: string,
  startId: number,
  endId: number,
  batchSize = 100
): Promise<BggCacheImportResult & { nextStartId: number }> {
  return authenticatedRequest<BggCacheImportResult & { nextStartId: number }>('/bgg-cache?action=import-range', token, {
    method: 'POST',
    body: JSON.stringify({ startId, endId, batchSize }),
  })
}

export interface BggCacheEntry {
  bggId: number
  name: string
  yearPublished: number | null
  thumbnailUrl: string | null
  imageUrl?: string | null
  minPlayers: number | null
  maxPlayers: number | null
  bggRank: number | null
  cachedAt: string
}

export interface BggCacheListResult {
  games: BggCacheEntry[]
  total: number
  page: number
  limit: number
  totalPages: number
}

// List BGG cache entries with search and pagination
export async function listBggCache(
  token: string,
  options: {
    search?: string
    page?: number
    limit?: number
    filter?: 'all' | 'missing_thumbnail' | 'missing_players' | 'incomplete'
  } = {}
): Promise<BggCacheListResult> {
  const params = new URLSearchParams({ action: 'list' })
  if (options.search) params.append('search', options.search)
  if (options.page) params.append('page', options.page.toString())
  if (options.limit) params.append('limit', options.limit.toString())
  if (options.filter && options.filter !== 'all') params.append('filter', options.filter)

  return authenticatedRequest<BggCacheListResult>(`/bgg-cache?${params}`, token)
}

// Refresh a single game from BGG
export async function refreshBggGame(token: string, bggId: number): Promise<{ message: string; game: BggCacheEntry; saved: boolean }> {
  return authenticatedRequest<{ message: string; game: BggCacheEntry; saved: boolean }>('/bgg-cache?action=refresh-game', token, {
    method: 'POST',
    body: JSON.stringify({ bggId }),
  })
}

// Manually update a BGG cache entry
export async function updateBggCacheEntry(
  token: string,
  bggId: number,
  updates: {
    thumbnailUrl?: string | null
    imageUrl?: string | null
    minPlayers?: number | null
    maxPlayers?: number | null
  }
): Promise<{ message: string; updated: boolean }> {
  return authenticatedRequest<{ message: string; updated: boolean }>('/bgg-cache?action=update-game', token, {
    method: 'POST',
    body: JSON.stringify({ bggId, ...updates }),
  })
}

// ============ Admin Dashboard ============

// Get full dashboard data (stats + health)
export async function getAdminDashboard(token: string): Promise<AdminDashboardData> {
  return authenticatedRequest<AdminDashboardData>('/admin-stats', token)
}

// Get stats only
export async function getAdminStats(token: string): Promise<{ stats: AdminStats }> {
  return authenticatedRequest<{ stats: AdminStats }>('/admin-stats?action=stats', token)
}

// Get service health only
export async function getServiceHealth(token: string): Promise<{ services: ServiceHealth[] }> {
  return authenticatedRequest<{ services: ServiceHealth[] }>('/admin-stats?action=health', token)
}

// ============ User Management ============

export interface AdminUser {
  id: string
  email: string
  username: string
  displayName: string | null
  avatarUrl: string | null
  isAdmin: boolean
  isFoundingMember: boolean
  isSuspended: boolean
  suspensionReason: string | null
  suspendedAt: string | null
  accountStatus: 'active' | 'suspended' | 'banned'
  bannedAt: string | null
  banReason: string | null
  subscriptionTier: 'free' | 'basic' | 'pro' | 'premium'
  subscriptionOverrideTier: 'free' | 'basic' | 'pro' | 'premium' | null
  subscriptionOverrideReason: string | null
  subscriptionStatus: 'active' | 'past_due' | 'canceled' | 'incomplete' | null
  subscriptionExpiresAt: string | null
  effectiveTier: 'free' | 'basic' | 'pro' | 'premium'
  authProvider: string  // 'password', 'google.com', 'facebook.com', etc.
  createdAt: string
}

export interface AdminUserListResponse {
  users: AdminUser[]
  total: number
  page: number
  limit: number
}

// Get list of users (admin only)
export async function getAdminUsers(
  token: string,
  options?: { search?: string; suspended?: boolean; banned?: boolean; page?: number; limit?: number }
): Promise<AdminUserListResponse> {
  const params = new URLSearchParams({ action: 'users' })
  if (options?.search) params.set('search', options.search)
  if (options?.suspended) params.set('suspended', 'true')
  if (options?.banned) params.set('banned', 'true')
  if (options?.page) params.set('page', options.page.toString())
  if (options?.limit) params.set('limit', options.limit.toString())

  return authenticatedRequest<AdminUserListResponse>(`/admin-stats?${params}`, token)
}

// Suspend a user (admin only)
export async function suspendUser(token: string, userId: string, reason?: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=suspend-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId, reason }),
  })
}

// Unsuspend a user (admin only)
export async function unsuspendUser(token: string, userId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=unsuspend-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

// Ban a user permanently (admin only)
export async function banUser(token: string, userId: string, reason?: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=ban-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId, reason }),
  })
}

// Unban a user (admin only)
export async function unbanUser(token: string, userId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=unban-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

// Set user subscription tier override (admin only)
export async function setUserTier(
  token: string,
  userId: string,
  tier: 'free' | 'basic' | 'pro' | 'premium' | null,
  reason?: string
): Promise<{ message: string; effectiveTier: string }> {
  return authenticatedRequest<{ message: string; effectiveTier: string }>('/admin-stats?action=set-tier', token, {
    method: 'POST',
    body: JSON.stringify({ userId, tier, reason }),
  })
}

// Toggle founding member status (admin only)
export async function toggleFoundingMember(
  token: string,
  userId: string,
  isFoundingMember: boolean
): Promise<{ message: string; isFoundingMember: boolean }> {
  return authenticatedRequest<{ message: string; isFoundingMember: boolean }>('/admin-stats?action=toggle-founding', token, {
    method: 'POST',
    body: JSON.stringify({ userId, isFoundingMember }),
  })
}

// Update a user (admin only)
export async function updateUser(
  token: string,
  userId: string,
  data: { displayName?: string; username?: string; isAdmin?: boolean; isFoundingMember?: boolean }
): Promise<{ user: AdminUser; message: string }> {
  return authenticatedRequest<{ user: AdminUser; message: string }>('/admin-stats?action=update-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId, ...data }),
  })
}

// Delete a user and all their data (admin only)
export async function deleteUser(token: string, userId: string): Promise<{ message: string; deletedUser: { id: string; email: string; username: string } }> {
  return authenticatedRequest<{ message: string; deletedUser: { id: string; email: string; username: string } }>('/admin-stats?action=delete-user', token, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

// Send password reset email to a user (admin only)
export async function sendPasswordReset(token: string, userId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=send-password-reset', token, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}

// ============ Group Management ============

export interface AdminGroup {
  id: string
  name: string
  slug: string
  description: string | null
  logoUrl: string | null
  isPublic: boolean
  groupType: string
  memberCount: number
  createdAt: string
}

export interface AdminGroupListResponse {
  groups: AdminGroup[]
  total: number
  page: number
  limit: number
}

// Admin event type
export interface AdminEvent {
  id: string
  title: string
  gameTitle: string | null
  gameSystem: string | null
  eventDate: string
  startTime: string | null
  durationMinutes: number | null
  city: string | null
  state: string | null
  status: string
  isPublic: boolean
  maxPlayers: number | null
  hostIsPlaying: boolean
  registrationCount: number
  primaryGameThumbnail: string | null
  host: {
    id: string
    displayName: string | null
    username: string | null
    avatarUrl: string | null
  } | null
  createdAt: string
}

export interface AdminEventListResponse {
  events: AdminEvent[]
  total: number
  page: number
  limit: number
}

// Get list of events (admin only)
export async function getAdminEvents(
  token: string,
  options?: { search?: string; page?: number; limit?: number; showPast?: boolean }
): Promise<AdminEventListResponse> {
  const params = new URLSearchParams({ action: 'events' })
  if (options?.search) params.set('search', options.search)
  if (options?.page) params.set('page', options.page.toString())
  if (options?.limit) params.set('limit', options.limit.toString())
  if (options?.showPast) params.set('showPast', 'true')

  return authenticatedRequest<AdminEventListResponse>(`/admin-stats?${params}`, token)
}

// Delete an event (admin only)
export async function deleteAdminEvent(token: string, eventId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=delete-event', token, {
    method: 'POST',
    body: JSON.stringify({ eventId }),
  })
}

// Get list of groups (admin only)
export async function getAdminGroups(
  token: string,
  options?: { search?: string; page?: number; limit?: number }
): Promise<AdminGroupListResponse> {
  const params = new URLSearchParams({ action: 'groups' })
  if (options?.search) params.set('search', options.search)
  if (options?.page) params.set('page', options.page.toString())
  if (options?.limit) params.set('limit', options.limit.toString())

  return authenticatedRequest<AdminGroupListResponse>(`/admin-stats?${params}`, token)
}

// Delete a group (admin only)
export async function deleteGroup(token: string, groupId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=delete-group', token, {
    method: 'POST',
    body: JSON.stringify({ groupId }),
  })
}

// Get group members (admin only)
export interface AdminGroupMember {
  id: string
  userId: string
  username: string
  displayName: string | null
  email: string
  avatarUrl: string | null
  isFoundingMember?: boolean
  isAdmin?: boolean
  role: 'owner' | 'admin' | 'member'
  joinedAt: string
}

export async function getGroupMembers(
  token: string,
  groupId: string
): Promise<{ members: AdminGroupMember[] }> {
  return authenticatedRequest<{ members: AdminGroupMember[] }>(
    `/admin-stats?action=group-members&groupId=${groupId}`,
    token
  )
}

// Add member to group (admin only)
export async function addGroupMember(
  token: string,
  groupId: string,
  userId: string,
  role: 'owner' | 'admin' | 'member' = 'member'
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=add-group-member', token, {
    method: 'POST',
    body: JSON.stringify({ groupId, userId, role }),
  })
}

// Remove member from group (admin only)
export async function removeGroupMember(
  token: string,
  groupId: string,
  userId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=remove-group-member', token, {
    method: 'POST',
    body: JSON.stringify({ groupId, userId }),
  })
}

// Change member role (admin only)
export async function changeGroupMemberRole(
  token: string,
  groupId: string,
  userId: string,
  role: 'owner' | 'admin' | 'member'
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=change-member-role', token, {
    method: 'POST',
    body: JSON.stringify({ groupId, userId, role }),
  })
}

// ============ Admin Notes ============

export interface AdminNote {
  id: string
  title: string
  content: string
  category: string
  isPinned: boolean
  isImplemented: boolean
  createdAt: string
  updatedAt: string
  createdBy: {
    id: string
    username: string
    displayName: string | null
  } | null
}

export interface AdminNotesResponse {
  notes: AdminNote[]
}

// Get all notes
export async function getAdminNotes(
  token: string,
  options?: { category?: string }
): Promise<AdminNotesResponse> {
  const params = new URLSearchParams({ action: 'notes' })
  if (options?.category) params.set('category', options.category)

  return authenticatedRequest<AdminNotesResponse>(`/admin-stats?${params}`, token)
}

// Create a note
export async function createAdminNote(
  token: string,
  data: { title: string; content: string; category?: string }
): Promise<{ note: AdminNote; message: string }> {
  return authenticatedRequest<{ note: AdminNote; message: string }>('/admin-stats?action=create-note', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Update a note
export async function updateAdminNote(
  token: string,
  noteId: string,
  data: { title?: string; content?: string; category?: string; isPinned?: boolean; isImplemented?: boolean }
): Promise<{ note: AdminNote; message: string }> {
  return authenticatedRequest<{ note: AdminNote; message: string }>('/admin-stats?action=update-note', token, {
    method: 'POST',
    body: JSON.stringify({ noteId, ...data }),
  })
}

// Delete a note
export async function deleteAdminNote(token: string, noteId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=delete-note', token, {
    method: 'POST',
    body: JSON.stringify({ noteId }),
  })
}

// ============ Admin Bugs ============

export interface AdminBug {
  id: string
  title: string
  description: string | null
  stepsToReproduce: string | null
  status: 'open' | 'in_progress' | 'resolved' | 'closed' | 'wont_fix'
  priority: 'low' | 'medium' | 'high' | 'critical'
  resolvedAt: string | null
  createdAt: string
  updatedAt: string
  reportedBy: {
    id: string
    username: string
    displayName: string | null
  } | null
  assignedTo: {
    id: string
    username: string
    displayName: string | null
  } | null
}

export interface AdminBugsResponse {
  bugs: AdminBug[]
}

// Get all bugs
export async function getAdminBugs(
  token: string,
  options?: { status?: string; priority?: string }
): Promise<AdminBugsResponse> {
  const params = new URLSearchParams({ action: 'bugs' })
  if (options?.status) params.set('status', options.status)
  if (options?.priority) params.set('priority', options.priority)

  return authenticatedRequest<AdminBugsResponse>(`/admin-stats?${params}`, token)
}

// Create a bug
export async function createAdminBug(
  token: string,
  data: { title: string; description?: string; stepsToReproduce?: string; priority?: string }
): Promise<{ bug: AdminBug; message: string }> {
  return authenticatedRequest<{ bug: AdminBug; message: string }>('/admin-stats?action=create-bug', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Update a bug
export async function updateAdminBug(
  token: string,
  bugId: string,
  data: { title?: string; description?: string; stepsToReproduce?: string; status?: string; priority?: string; assignedToUserId?: string | null }
): Promise<{ bug: AdminBug; message: string }> {
  return authenticatedRequest<{ bug: AdminBug; message: string }>('/admin-stats?action=update-bug', token, {
    method: 'POST',
    body: JSON.stringify({ bugId, ...data }),
  })
}

// Delete a bug
export async function deleteAdminBug(token: string, bugId: string): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/admin-stats?action=delete-bug', token, {
    method: 'POST',
    body: JSON.stringify({ bugId }),
  })
}

// ============ MTG/Scryfall Cache ============

export interface MtgCacheStats {
  totalCards: number
  oldestEntry: string | null
  newestEntry: string | null
  staleCount: number
  cacheTtlHours: number
  staplesListSize: number
}

export interface MtgCacheWarmResult {
  action: string
  cached: number
  skipped: number
  failed: number
  total: number
  errors?: string[]
}

// Get MTG cache statistics (no auth required)
export async function getMtgCacheStats(): Promise<MtgCacheStats> {
  const response = await fetch(`${FUNCTIONS_URL}/scryfall-cache-warmer?action=status`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(`Failed to get MTG cache stats: ${response.statusText}`)
  }

  return response.json()
}

// Warm Commander staples
export async function warmMtgStaples(token: string, offset = 0): Promise<MtgCacheWarmResult & { hasMore?: boolean; nextOffset?: number | null; totalStaples?: number }> {
  return authenticatedRequest<MtgCacheWarmResult & { hasMore?: boolean; nextOffset?: number | null; totalStaples?: number }>(`/scryfall-cache-warmer?action=warm-staples&offset=${offset}`, token, {
    method: 'POST',
  })
}

// Warm commanders via search
export async function warmMtgCommanders(token: string, pages = 5): Promise<MtgCacheWarmResult> {
  return authenticatedRequest<MtgCacheWarmResult>(`/scryfall-cache-warmer?action=warm-commanders&pages=${pages}`, token, {
    method: 'POST',
  })
}

// Warm by custom search query
export async function warmMtgBySearch(token: string, query: string, pages = 3): Promise<MtgCacheWarmResult> {
  return authenticatedRequest<MtgCacheWarmResult>(`/scryfall-cache-warmer?action=warm-search&q=${encodeURIComponent(query)}&pages=${pages}`, token, {
    method: 'POST',
  })
}

// Refresh stale MTG cache entries
export async function refreshMtgStaleCache(token: string): Promise<MtgCacheWarmResult & { hasMore?: boolean; remaining?: number }> {
  return authenticatedRequest<MtgCacheWarmResult & { hasMore?: boolean; remaining?: number }>('/scryfall-cache-warmer?action=refresh-stale', token, {
    method: 'POST',
  })
}
