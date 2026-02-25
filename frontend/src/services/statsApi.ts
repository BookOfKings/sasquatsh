const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface Stats {
  gamesToday: number
  gamesEver: number
}

export async function getStats(): Promise<Stats> {
  try {
    const response = await fetch(`${FUNCTIONS_URL}/stats`, {
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
    })

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
