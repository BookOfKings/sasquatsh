import type {
  PlayerRequest,
  CreatePlayerRequestInput,
  UpdatePlayerRequestInput,
  GameInvitation,
  CreateInvitationInput,
} from '@/types/social'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
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

// ============ Player Requests ============

// Get all active player requests (public)
export async function getPlayerRequests(filters?: {
  city?: string
  state?: string
}): Promise<PlayerRequest[]> {
  const params = new URLSearchParams()
  if (filters?.city) params.set('city', filters.city)
  if (filters?.state) params.set('state', filters.state)

  const url = `${FUNCTIONS_URL}/player-requests${params.toString() ? '?' + params.toString() : ''}`
  const response = await fetch(url)

  if (!response.ok) {
    throw new Error('Failed to fetch player requests')
  }

  return response.json() as Promise<PlayerRequest[]>
}

// Get my player requests
export async function getMyPlayerRequests(token: string): Promise<PlayerRequest[]> {
  return authenticatedRequest<PlayerRequest[]>('/player-requests?id=mine', token)
}

// Create player request
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

// Get invitation by code (public)
export async function getInvitationByCode(code: string): Promise<GameInvitation> {
  const response = await fetch(`${FUNCTIONS_URL}/invitations?code=${code}`)

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
