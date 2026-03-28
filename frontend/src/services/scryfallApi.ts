import type { ScryfallCard, MtgFormat } from '@/types/mtg'

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

const SCRYFALL_FUNCTION_URL = `${SUPABASE_URL}/functions/v1/scryfall`

interface SearchResponse {
  cards: ScryfallCard[]
  totalCards: number
  hasMore: boolean
  source: string
}

interface AutocompleteResponse {
  suggestions: string[]
  total: number
}

interface FormatsResponse {
  formats: MtgFormat[]
}

async function scryfallRequest<T>(params: Record<string, string>): Promise<T> {
  const url = new URL(SCRYFALL_FUNCTION_URL)
  Object.entries(params).forEach(([key, value]) => {
    url.searchParams.set(key, value)
  })

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Unknown error' }))
    throw new Error(error.error || `HTTP ${response.status}`)
  }

  return response.json()
}

/**
 * Search for MTG cards by name or query
 */
export async function searchCards(query: string, page = 1): Promise<SearchResponse> {
  if (!query || query.length < 2) {
    return { cards: [], totalCards: 0, hasMore: false, source: 'empty' }
  }

  return scryfallRequest<SearchResponse>({
    search: query,
    page: page.toString(),
  })
}

/**
 * Get card name autocomplete suggestions (fast)
 */
export async function autocompleteCards(query: string): Promise<string[]> {
  if (!query || query.length < 2) {
    return []
  }

  const response = await scryfallRequest<AutocompleteResponse>({
    autocomplete: query,
  })

  return response.suggestions
}

/**
 * Get a specific card by Scryfall ID
 */
export async function getCardById(scryfallId: string): Promise<ScryfallCard> {
  return scryfallRequest<ScryfallCard>({
    id: scryfallId,
  })
}

/**
 * Get a card by exact name
 */
export async function getCardByName(name: string): Promise<ScryfallCard> {
  return scryfallRequest<ScryfallCard>({
    exact: name,
  })
}

/**
 * Get a random card, optionally filtered by query
 */
export async function getRandomCard(query?: string): Promise<ScryfallCard> {
  const params: Record<string, string> = { random: '1' }
  if (query) {
    params.q = query
  }

  return scryfallRequest<ScryfallCard>(params)
}

/**
 * Get list of available MTG formats
 */
export async function getFormats(): Promise<MtgFormat[]> {
  const response = await scryfallRequest<FormatsResponse>({
    formats: '',
  })

  return response.formats
}

/**
 * Check if a card is legal in a format
 */
export function isCardLegalInFormat(
  card: ScryfallCard,
  formatId: string
): 'legal' | 'not_legal' | 'restricted' | 'banned' {
  return card.legalities[formatId] || 'not_legal'
}

/**
 * Get the best image URL for a card
 */
export function getCardImageUrl(
  card: ScryfallCard,
  size: 'small' | 'normal' | 'large' | 'artCrop' | 'png' = 'normal'
): string | null {
  // For double-faced cards, use the first face
  if (card.isDoubleFaced && card.cardFaces?.[0]?.imageUris) {
    return card.cardFaces[0].imageUris[size]
  }

  return card.imageUris[size]
}

/**
 * Get both face images for a double-faced card
 */
export function getDoubleFacedCardImages(card: ScryfallCard): {
  front: string | null
  back: string | null
} {
  if (!card.isDoubleFaced || !card.cardFaces || card.cardFaces.length < 2) {
    return {
      front: card.imageUris.normal,
      back: null,
    }
  }

  return {
    front: card.cardFaces[0]?.imageUris?.normal || null,
    back: card.cardFaces[1]?.imageUris?.normal || null,
  }
}

/**
 * Parse a deck list string into card entries
 * Supports formats like:
 * - "4 Lightning Bolt"
 * - "4x Lightning Bolt"
 * - "Lightning Bolt x4"
 * - "1 Lightning Bolt (M10)"
 */
export function parseDeckList(deckList: string): Array<{ quantity: number; name: string }> {
  const lines = deckList.split('\n').filter(line => line.trim())
  const entries: Array<{ quantity: number; name: string }> = []

  for (const line of lines) {
    const trimmed = line.trim()

    // Skip comments and section headers
    if (trimmed.startsWith('//') || trimmed.startsWith('#')) continue
    if (trimmed.toLowerCase().startsWith('sideboard')) continue
    if (trimmed.toLowerCase().startsWith('commander')) continue
    if (trimmed.toLowerCase().startsWith('maybeboard')) continue

    // Try different patterns
    let match = trimmed.match(/^(\d+)x?\s+(.+?)(?:\s+\([^)]+\))?$/)
    if (!match) {
      const altMatch = trimmed.match(/^(.+?)\s+x?(\d+)$/)
      if (altMatch && altMatch[1] && altMatch[2]) {
        // Swap groups for "Card Name x4" format
        match = [altMatch[0], altMatch[2], altMatch[1]]
      }
    }

    if (match && match[1] && match[2]) {
      const quantity = parseInt(match[1], 10)
      const name = match[2].trim()
      if (quantity > 0 && name) {
        entries.push({ quantity, name })
      }
    } else if (trimmed && !trimmed.match(/^\d+$/)) {
      // Single card with no quantity
      entries.push({ quantity: 1, name: trimmed })
    }
  }

  return entries
}

/**
 * Get color identity as a string (e.g., "WUB" for Esper)
 */
export function getColorIdentityString(card: ScryfallCard): string {
  return card.colorIdentity.join('')
}

/**
 * Check if a card is a commander-legal commander
 */
export function isValidCommander(card: ScryfallCard): boolean {
  const typeLine = card.typeLine.toLowerCase()

  // Legendary creatures
  if (typeLine.includes('legendary') && typeLine.includes('creature')) {
    return true
  }

  // Cards that say they can be your commander
  if (card.oracleText?.toLowerCase().includes('can be your commander')) {
    return true
  }

  return false
}

/**
 * Calculate total deck price
 */
export function calculateDeckPrice(
  cards: Array<{ card: ScryfallCard; quantity: number }>,
  priceType: 'usd' | 'usd_foil' = 'usd'
): number {
  let total = 0

  for (const { card, quantity } of cards) {
    const price = card.prices[priceType]
    if (price) {
      total += parseFloat(price) * quantity
    }
  }

  return Math.round(total * 100) / 100
}
