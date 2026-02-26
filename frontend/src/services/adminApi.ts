import type { EventLocation } from '@/types/social'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// ============ Admin Dashboard Types ============

export interface AdminStats {
  users: {
    total: number
    last7Days: number
    last30Days: number
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
    startDate: string
    endDate: string
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
  isSuspended: boolean
  suspensionReason: string | null
  suspendedAt: string | null
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
  options?: { search?: string; suspended?: boolean; page?: number; limit?: number }
): Promise<AdminUserListResponse> {
  const params = new URLSearchParams({ action: 'users' })
  if (options?.search) params.set('search', options.search)
  if (options?.suspended) params.set('suspended', 'true')
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
