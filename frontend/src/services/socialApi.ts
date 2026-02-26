import type {
  PlayerRequest,
  CreatePlayerRequestInput,
  UpdatePlayerRequestInput,
  PlayerRequestFilters,
  GameInvitation,
  CreateInvitationInput,
  EventLocation,
  CreateEventLocationInput,
} from '@/types/social'

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

// ============ Event Locations ============

// Get all approved, active event locations (public)
export async function getEventLocations(): Promise<EventLocation[]> {
  const response = await fetch(`${FUNCTIONS_URL}/event-locations`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch event locations')
  }

  return response.json() as Promise<EventLocation[]>
}

// Create event location (authenticated - creates as pending)
export async function createEventLocation(
  token: string,
  data: CreateEventLocationInput
): Promise<EventLocation> {
  return authenticatedRequest<EventLocation>('/event-locations', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// ============ Player Requests (Host needs players - someone bailed) ============

// Get all active player requests (public, but can pass token for blocked user filtering)
export async function getPlayerRequests(
  filters?: PlayerRequestFilters,
  token?: string
): Promise<PlayerRequest[]> {
  const params = new URLSearchParams()
  if (filters?.eventId) params.set('eventId', filters.eventId)

  const url = `${FUNCTIONS_URL}/player-requests${params.toString() ? '?' + params.toString() : ''}`

  const headers: Record<string, string> = {
    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    'Content-Type': 'application/json',
  }

  // Pass token for blocked user filtering if authenticated
  if (token) {
    headers['X-Firebase-Token'] = token
  }

  const response = await fetch(url, { headers })

  if (!response.ok) {
    throw new Error('Failed to fetch player requests')
  }

  return response.json() as Promise<PlayerRequest[]>
}

// Get my player requests (requests I've made as host)
export async function getMyPlayerRequests(token: string): Promise<PlayerRequest[]> {
  return authenticatedRequest<PlayerRequest[]>('/player-requests?id=mine', token)
}

// Create player request (I'm hosting an event and need players)
export async function createPlayerRequest(
  token: string,
  data: CreatePlayerRequestInput
): Promise<PlayerRequest> {
  return authenticatedRequest<PlayerRequest>('/player-requests', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Update player request
export async function updatePlayerRequest(
  token: string,
  requestId: string,
  data: UpdatePlayerRequestInput
): Promise<PlayerRequest> {
  return authenticatedRequest<PlayerRequest>(`/player-requests?id=${requestId}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

// Mark request as filled (found players)
export async function fillPlayerRequest(
  token: string,
  requestId: string
): Promise<PlayerRequest> {
  return authenticatedRequest<PlayerRequest>(`/player-requests?id=${requestId}&action=fill`, token, {
    method: 'POST',
  })
}

// Cancel player request
export async function cancelPlayerRequest(
  token: string,
  requestId: string
): Promise<PlayerRequest> {
  return authenticatedRequest<PlayerRequest>(`/player-requests?id=${requestId}&action=cancel`, token, {
    method: 'POST',
  })
}

// Delete player request
export async function deletePlayerRequest(
  token: string,
  requestId: string
): Promise<void> {
  return authenticatedRequest<void>(`/player-requests?id=${requestId}`, token, {
    method: 'DELETE',
  })
}

// ============ Invitations ============

// Get invitation by code (public - but Supabase requires anon key)
export async function getInvitationByCode(code: string): Promise<GameInvitation> {
  const response = await fetch(`${FUNCTIONS_URL}/invitations?code=${code}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    let message = 'Invitation not found'
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  return response.json() as Promise<GameInvitation>
}

// Accept invitation
export async function acceptInvitation(
  token: string,
  code: string
): Promise<{ message: string; eventId: string }> {
  return authenticatedRequest<{ message: string; eventId: string }>(
    `/invitations?code=${code}&action=accept`,
    token,
    { method: 'POST' }
  )
}

// Create invitation
export async function createInvitation(
  token: string,
  data: CreateInvitationInput
): Promise<GameInvitation> {
  return authenticatedRequest<GameInvitation>('/invitations', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Get invitations for an event
export async function getEventInvitations(
  token: string,
  eventId: string
): Promise<GameInvitation[]> {
  return authenticatedRequest<GameInvitation[]>(`/invitations?eventId=${eventId}`, token)
}

// Delete/revoke invitation
export async function revokeInvitation(
  token: string,
  invitationId: string
): Promise<void> {
  return authenticatedRequest<void>(`/invitations?id=${invitationId}`, token, {
    method: 'DELETE',
  })
}

// Generate share URL
export function getShareUrl(inviteCode: string): string {
  return `${window.location.origin}/invite/${inviteCode}`
}

// Generate social share URLs
export function getSocialShareUrls(inviteCode: string, eventTitle: string) {
  const shareUrl = getShareUrl(inviteCode)
  const encodedUrl = encodeURIComponent(shareUrl)
  const encodedText = encodeURIComponent(`Join me for ${eventTitle}!`)

  return {
    facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`,
    twitter: `https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedText}`,
    email: `mailto:?subject=${encodeURIComponent(`You're invited to ${eventTitle}`)}&body=${encodedText}%0A%0A${encodedUrl}`,
  }
}
