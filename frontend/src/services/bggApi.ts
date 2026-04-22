import type { BggSearchResult, BggGame, EventGame, AddEventGameInput } from '@/types/bgg'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${FUNCTIONS_URL}${path}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'X-Firebase-Token': token,
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    })
  } catch {
    throw new Error('Unable to connect to the server. Please check your internet connection and try again.')
  }

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

// Search BGG for games (public - but Supabase requires anon key)
export async function searchBggGames(query: string): Promise<BggSearchResult[]> {
  if (!query.trim()) return []

  const response = await fetch(
    `${FUNCTIONS_URL}/bgg?search=${encodeURIComponent(query)}`,
    {
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
    }
  )

  if (!response.ok) {
    throw new Error('Failed to search BGG')
  }

  return response.json() as Promise<BggSearchResult[]>
}

// Get BGG game details (public - but Supabase requires anon key)
export async function getBggGame(bggId: number): Promise<BggGame> {
  const response = await fetch(`${FUNCTIONS_URL}/bgg?id=${bggId}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch game details')
  }

  return response.json() as Promise<BggGame>
}

// Add game to event
export async function addEventGame(
  token: string,
  eventId: string,
  data: AddEventGameInput
): Promise<EventGame> {
  return authenticatedRequest<EventGame>('/event-games', token, {
    method: 'POST',
    body: JSON.stringify({ eventId, ...data }),
  })
}

// Remove game from event
export async function removeEventGame(
  token: string,
  gameId: string
): Promise<void> {
  return authenticatedRequest<void>(`/event-games?id=${gameId}`, token, {
    method: 'DELETE',
  })
}

// Update game in event (change primary/alternative status)
export async function updateEventGame(
  token: string,
  gameId: string,
  data: Partial<AddEventGameInput>
): Promise<EventGame> {
  return authenticatedRequest<EventGame>(`/event-games?id=${gameId}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}
