const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface ShelfScanGame {
  detectedTitle: string
  bggId: number | null
  name: string | null
  yearPublished: number | null
  thumbnailUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  playingTime: number | null
  confidence: 'high' | 'none'
}

export interface ShelfScanResult {
  games: ShelfScanGame[]
  rawText: string
  totalDetected: number
  matched: number
  message?: string
}

export interface ShelfScanUsage {
  used: number
  limit: number | 'unlimited'
  remaining: number // -1 means unlimited
}

export async function getShelfScanUsage(token: string): Promise<ShelfScanUsage> {
  const response = await fetch(`${FUNCTIONS_URL}/shelf-scan`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
    },
  })
  if (!response.ok) throw new Error('Failed to get scan usage')
  return response.json()
}

export async function scanShelfImage(token: string, imageFile: File): Promise<ShelfScanResult> {
  const formData = new FormData()
  formData.append('image', imageFile)

  const response = await fetch(`${FUNCTIONS_URL}/shelf-scan`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
    },
    body: formData,
  })

  if (!response.ok) {
    let message = response.statusText
    try { const d = await response.json(); if (d?.error) message = d.error } catch {}
    throw new Error(message)
  }

  return response.json()
}
