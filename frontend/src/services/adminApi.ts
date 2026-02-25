import type { EventLocation } from '@/types/social'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

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
