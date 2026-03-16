import type { EventLocation, CreateEventLocationInput } from '@/types/social'

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

// Get hot locations (popular/active venues ranked by usage)
export async function getHotLocations(): Promise<EventLocation[]> {
  const response = await fetch(`${FUNCTIONS_URL}/event-locations?hot=true`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch hot locations')
  }

  return response.json() as Promise<EventLocation[]>
}

// Get all active approved locations (not just hot)
export async function getActiveLocations(): Promise<EventLocation[]> {
  const response = await fetch(`${FUNCTIONS_URL}/event-locations`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch locations')
  }

  return response.json() as Promise<EventLocation[]>
}

// Submit a new venue for approval (creates as pending)
export async function submitVenue(
  token: string,
  data: CreateEventLocationInput
): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>('/event-locations', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Get a single location by ID
export async function getLocationById(id: string): Promise<EventLocation> {
  const response = await fetch(`${FUNCTIONS_URL}/event-locations?id=${id}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch location')
  }

  return response.json() as Promise<EventLocation>
}

// Get ALL approved locations for event creation (includes recurring venues on any day)
export async function getLocationsForEventCreation(): Promise<EventLocation[]> {
  const response = await fetch(`${FUNCTIONS_URL}/event-locations?forEvent=true`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch locations')
  }

  return response.json() as Promise<EventLocation[]>
}
