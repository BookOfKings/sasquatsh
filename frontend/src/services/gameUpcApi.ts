const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface BggInfoResult {
  id: number
  name: string
  published: string | null
  thumbnail_url: string | null
  image_url: string | null
  page_url: string | null
  data_url: string | null
  update_url: string | null
  version_status: 'verified' | 'none' | 'choose_from_versions'
  confidence: number
  versions: BggVersionResult[]
}

export interface BggVersionResult {
  version_id: number
  name: string
  published: string | null
  language: string | null
  thumbnail_url: string | null
  image_url: string | null
  update_url: string | null
  confidence: number
}

export interface UpcLookupResult {
  upc: string
  name: string | null
  searched_for: string | null
  bgg_info_status: 'verified' | 'choose_from_bgg_info_or_search' | 'not found'
  bgg_info: BggInfoResult[]
  status?: string
  message?: string
}

async function authRequest<T>(path: string, token: string, options?: RequestInit): Promise<T> {
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
    try { const d = await response.json(); if (d?.error) message = d.error } catch {}
    throw new Error(message)
  }
  return response.json() as Promise<T>
}

// Look up a game by UPC/barcode
export async function lookupUpc(token: string, upc: string, search?: string): Promise<UpcLookupResult> {
  let path = `/game-upc?upc=${encodeURIComponent(upc)}`
  if (search) path += `&search=${encodeURIComponent(search)}`
  return authRequest<UpcLookupResult>(path, token)
}

// Vote/confirm a BGG match for a UPC
export async function voteUpcMatch(
  token: string,
  upc: string,
  bggId: number,
  bggVersion?: number
): Promise<UpcLookupResult> {
  let path = `/game-upc?upc=${encodeURIComponent(upc)}&bggId=${bggId}`
  if (bggVersion) path += `&bggVersion=${bggVersion}`
  return authRequest<UpcLookupResult>(path, token, { method: 'POST' })
}

// Undo a vote
export async function undoUpcVote(
  token: string,
  upc: string,
  bggId: number,
  bggVersion?: number
): Promise<UpcLookupResult> {
  let path = `/game-upc?upc=${encodeURIComponent(upc)}&bggId=${bggId}`
  if (bggVersion) path += `&bggVersion=${bggVersion}`
  return authRequest<UpcLookupResult>(path, token, { method: 'DELETE' })
}
