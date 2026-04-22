const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface CollectionGame {
  id: string
  user_id: string
  bgg_id: number
  game_name: string
  thumbnail_url: string | null
  image_url: string | null
  min_players: number | null
  max_players: number | null
  playing_time: number | null
  year_published: number | null
  bgg_rank: number | null
  average_rating: number | null
  created_at: string
}

export interface TopGame {
  bgg_id: number
  name: string
  thumbnail_url: string | null
  image_url: string | null
  min_players: number | null
  max_players: number | null
  playing_time: number | null
  year_published: number | null
  bgg_rank: number | null
  average_rating: number | null
}

async function authRequest<T>(path: string, token: string, options?: RequestInit): Promise<T> {
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
    } catch { /* no body */ }
    throw new Error(message)
  }

  return response.json() as Promise<T>
}

export async function getMyCollection(token: string): Promise<CollectionGame[]> {
  const data = await authRequest<{ games: CollectionGame[] }>('/collections', token)
  return data.games
}

export async function getUserCollection(userId: string): Promise<CollectionGame[]> {
  const response = await fetch(`${FUNCTIONS_URL}/collections?userId=${userId}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })
  if (!response.ok) throw new Error('Failed to fetch collection')
  const data = await response.json()
  return data.games
}

export async function getTopGames(): Promise<TopGame[]> {
  const response = await fetch(`${FUNCTIONS_URL}/collections?action=top-games`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })
  if (!response.ok) throw new Error('Failed to fetch top games')
  const data = await response.json()
  return data.games
}

export async function addGamesToCollection(
  token: string,
  games: Array<{
    bgg_id: number
    name: string
    thumbnail_url?: string | null
    image_url?: string | null
    min_players?: number | null
    max_players?: number | null
    playing_time?: number | null
    year_published?: number | null
    bgg_rank?: number | null
    average_rating?: number | null
  }>
): Promise<CollectionGame[]> {
  const data = await authRequest<{ games: CollectionGame[] }>('/collections', token, {
    method: 'POST',
    body: JSON.stringify({ games }),
  })
  return data.games
}

export async function removeGameFromCollection(token: string, bggId: number): Promise<void> {
  await authRequest<{ removed: boolean }>(`/collections?bggId=${bggId}`, token, {
    method: 'DELETE',
  })
}
