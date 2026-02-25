const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL

export interface Stats {
  gamesToday: number
  gamesEver: number
}

export async function getStats(): Promise<Stats> {
  try {
    const response = await fetch(`${FUNCTIONS_URL}/stats`)

    if (!response.ok) {
      throw new Error('Failed to fetch stats')
    }

    return response.json() as Promise<Stats>
  } catch (err) {
    console.error('Error fetching stats:', err)
    // Return default values on error
    return { gamesToday: 0, gamesEver: 0 }
  }
}
