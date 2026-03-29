import type { Event } from '@/types/events'
import type { CreatePokemonEventInput, PokemonEventConfig } from '@/types/pokemon'

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

// Create a Pokemon TCG event
export async function createPokemonEvent(
  token: string,
  input: CreatePokemonEventInput
): Promise<Event> {
  return authenticatedRequest<Event>('/events', token, {
    method: 'POST',
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      gameSystem: 'pokemon_tcg',
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
      // Pokemon Config
      pokemonConfig: input.pokemonConfig,
    }),
  })
}

// Update a Pokemon TCG event
export async function updatePokemonEvent(
  token: string,
  eventId: string,
  input: Partial<CreatePokemonEventInput>
): Promise<Event> {
  return authenticatedRequest<Event>(`/events?id=${eventId}`, token, {
    method: 'PUT',
    body: JSON.stringify({
      title: input.title,
      description: input.description,
      gameSystem: 'pokemon_tcg',
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
      // Pokemon Config
      pokemonConfig: input.pokemonConfig,
    }),
  })
}

// Get Pokemon event (uses same endpoint as regular events)
export async function getPokemonEvent(
  token: string,
  eventId: string
): Promise<Event> {
  return authenticatedRequest<Event>(`/events?id=${eventId}`, token, {
    method: 'GET',
  })
}

// Type guard to check if an event is a Pokemon event
export function isPokemonEvent(event: Event): event is Event & { pokemonConfig: PokemonEventConfig } {
  return event.gameSystem === 'pokemon_tcg' && event.pokemonConfig !== null
}
