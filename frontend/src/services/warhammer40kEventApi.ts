import type { Event } from '@/types/events'
import type { CreateWarhammer40kEventInput, Warhammer40kEventConfig } from '@/types/warhammer40k'

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
    let code: string | undefined
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
      if (data?.code) code = data.code
      if (code) {
        const err = new Error(message) as Error & { code?: string; data?: unknown }
        err.code = code
        err.data = data
        throw err
      }
    } catch (e) {
      if (e instanceof Error && (e as Error & { code?: string }).code) {
        throw e
      }
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// Create a Warhammer 40k event
export async function createWarhammer40kEvent(
  token: string,
  input: CreateWarhammer40kEventInput
): Promise<Event> {
  return authenticatedRequest<Event>('/events', token, {
    method: 'POST',
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      gameSystem: 'warhammer40k',
      eventDate: input.eventDate,
      startTime: input.startTime,
      timezone: input.timezone,
      durationMinutes: input.durationMinutes,
      setupMinutes: input.setupMinutes,
      maxPlayers: input.maxPlayers,
      hostIsPlaying: input.hostIsPlaying,
      isPublic: input.isPublic,
      isCharityEvent: input.isCharityEvent,
      groupId: input.groupId,
      // Location
      eventLocationId: input.eventLocationId,
      addressLine1: input.addressLine1,
      city: input.city,
      state: input.state,
      postalCode: input.postalCode,
      locationDetails: input.locationDetails,
      venueHall: input.venueHall,
      venueRoom: input.venueRoom,
      venueTable: input.venueTable,
      // Status
      status: 'published',
      // Warhammer 40k Config
      warhammer40kConfig: input.warhammer40kConfig,
    }),
  })
}

// Update a Warhammer 40k event
export async function updateWarhammer40kEvent(
  token: string,
  eventId: string,
  input: Partial<CreateWarhammer40kEventInput>
): Promise<Event> {
  return authenticatedRequest<Event>(`/events?id=${eventId}`, token, {
    method: 'PUT',
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      gameSystem: 'warhammer40k',
      eventDate: input.eventDate,
      startTime: input.startTime,
      timezone: input.timezone,
      durationMinutes: input.durationMinutes,
      setupMinutes: input.setupMinutes,
      maxPlayers: input.maxPlayers,
      hostIsPlaying: input.hostIsPlaying,
      isPublic: input.isPublic,
      isCharityEvent: input.isCharityEvent,
      groupId: input.groupId,
      // Location
      eventLocationId: input.eventLocationId,
      addressLine1: input.addressLine1,
      city: input.city,
      state: input.state,
      postalCode: input.postalCode,
      locationDetails: input.locationDetails,
      venueHall: input.venueHall,
      venueRoom: input.venueRoom,
      venueTable: input.venueTable,
      // Warhammer 40k Config
      warhammer40kConfig: input.warhammer40kConfig,
    }),
  })
}

// Get Warhammer 40k event (uses same endpoint as regular events)
export async function getWarhammer40kEvent(
  token: string,
  eventId: string
): Promise<Event> {
  return authenticatedRequest<Event>(`/events?id=${eventId}`, token, {
    method: 'GET',
  })
}

// Type guard to check if an event is a Warhammer 40k event
export function isWarhammer40kEvent(event: Event): event is Event & { warhammer40kConfig: Warhammer40kEventConfig } {
  return event.gameSystem === 'warhammer40k' && event.warhammer40kConfig !== null
}
