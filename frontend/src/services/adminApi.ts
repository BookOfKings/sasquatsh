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

// Get pending locations (admin only)
export async function getPendingLocations(token: string): Promise<EventLocation[]> {
  return authenticatedRequest<EventLocation[]>('/event-locations?status=pending', token)
}

// Get all locations (admin only)
export async function getAllLocations(token: string): Promise<EventLocation[]> {
  return authenticatedRequest<EventLocation[]>('/event-locations?status=all', token)
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

// Delete location (admin only)
export async function deleteLocation(token: string, id: string): Promise<void> {
  return authenticatedRequest<void>(`/event-locations?id=${id}`, token, {
    method: 'DELETE',
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
