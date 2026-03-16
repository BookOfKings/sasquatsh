import type { EventSessionsResponse } from '@/types/sessions'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests to Edge Functions
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

// Get all sessions for an event
export async function getEventSessions(
  token: string,
  eventId: string
): Promise<EventSessionsResponse> {
  return authenticatedRequest<EventSessionsResponse>(
    `/sessions?eventId=${eventId}`,
    token
  )
}

// Register for a session
export async function registerForSession(
  token: string,
  sessionId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(
    `/sessions?action=register&sessionId=${sessionId}`,
    token,
    { method: 'POST' }
  )
}

// Cancel session registration
export async function cancelSessionRegistration(
  token: string,
  sessionId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(
    `/sessions?sessionId=${sessionId}`,
    token,
    { method: 'DELETE' }
  )
}

// Create a new session (host only)
export async function createSession(
  token: string,
  data: {
    eventId: string
    tableId: string
    gameName: string
    bggId?: number
    thumbnailUrl?: string
    minPlayers?: number
    maxPlayers?: number
    startTime?: string
    durationMinutes?: number
  }
): Promise<{ message: string; session: { id: string; tableId: string; slotIndex: number; gameName: string } }> {
  return authenticatedRequest(
    `/sessions?action=create`,
    token,
    {
      method: 'POST',
      body: JSON.stringify(data),
    }
  )
}

// Update a session (host only)
export async function updateSession(
  token: string,
  sessionId: string,
  data: {
    gameName?: string
    bggId?: number
    thumbnailUrl?: string
    minPlayers?: number
    maxPlayers?: number
    startTime?: string
    durationMinutes?: number
    status?: string
  }
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(
    `/sessions?sessionId=${sessionId}`,
    token,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    }
  )
}

// Delete a session (host only)
export async function deleteSession(
  token: string,
  sessionId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(
    `/sessions?sessionId=${sessionId}&action=delete-session`,
    token,
    { method: 'DELETE' }
  )
}
