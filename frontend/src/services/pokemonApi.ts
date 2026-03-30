import type {
  PokemonCard,
  PokemonSet,
  PokemonCardSearchResult,
  PokemonSetSearchResult,
  PokemonType,
  PokemonSupertype,
  PokemonLegalities,
} from '@/types/pokemon'

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

const POKEMON_FUNCTION_URL = `${SUPABASE_URL}/functions/v1/pokemon-cache`

// For direct API access during development (bypasses cache)
const POKEMON_TCG_API_URL = 'https://api.pokemontcg.io/v2'

interface SearchResponse {
  cards: PokemonCard[]
  totalCards: number
  hasMore: boolean
  page: number
  pageSize: number
  source: 'cache' | 'api'
}

interface SetSearchResponse {
  sets: PokemonSet[]
  totalSets: number
  hasMore: boolean
  page: number
  pageSize: number
  source: 'cache' | 'api'
}

interface AutocompleteResponse {
  suggestions: string[]
  total: number
}

/**
 * Make a request to our Pokemon cache edge function
 */
async function pokemonRequest<T>(params: Record<string, string>): Promise<T> {
  const url = new URL(POKEMON_FUNCTION_URL)
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
 * Make a direct request to Pokemon TCG API (for development/fallback)
 */
async function directApiRequest<T>(endpoint: string, params?: Record<string, string>): Promise<T> {
  const url = new URL(`${POKEMON_TCG_API_URL}/${endpoint}`)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      url.searchParams.set(key, value)
    })
  }

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ error: 'Unknown error' }))
    throw new Error(error.error || error.message || `HTTP ${response.status}`)
  }

  return response.json()
}

// ==================== Card Search Functions ====================

/**
 * Search for Pokemon cards by name
 */
export async function searchCards(query: string, page = 1, pageSize = 20): Promise<SearchResponse> {
  if (!query || query.length < 2) {
    return { cards: [], totalCards: 0, hasMore: false, page: 1, pageSize, source: 'cache' }
  }

  try {
    return await pokemonRequest<SearchResponse>({
      search: query,
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
  } catch (error) {
    // Fallback to direct API if cache function isn't deployed yet
    console.warn('Pokemon cache function not available, using direct API:', error)
    return searchCardsDirect(query, page, pageSize)
  }
}

/**
 * Search cards directly from Pokemon TCG API (fallback)
 */
export async function searchCardsDirect(query: string, page = 1, pageSize = 20): Promise<SearchResponse> {
  if (!query || query.length < 2) {
    return { cards: [], totalCards: 0, hasMore: false, page: 1, pageSize, source: 'api' }
  }

  const response = await directApiRequest<PokemonCardSearchResult>('cards', {
    q: `name:"${query}*"`,
    page: page.toString(),
    pageSize: pageSize.toString(),
    orderBy: 'name',
  })

  return {
    cards: response.data,
    totalCards: response.totalCount,
    hasMore: response.page * response.pageSize < response.totalCount,
    page: response.page,
    pageSize: response.pageSize,
    source: 'api',
  }
}

/**
 * Advanced card search with filters
 */
export async function searchCardsAdvanced(options: {
  name?: string
  types?: PokemonType[]
  supertype?: PokemonSupertype
  subtypes?: string[]
  setId?: string
  rarity?: string
  legality?: keyof PokemonLegalities
  page?: number
  pageSize?: number
}): Promise<SearchResponse> {
  const queryParts: string[] = []

  if (options.name) {
    queryParts.push(`name:"${options.name}*"`)
  }
  if (options.types && options.types.length > 0) {
    queryParts.push(`types:${options.types.join(' OR types:')}`)
  }
  if (options.supertype) {
    queryParts.push(`supertype:"${options.supertype}"`)
  }
  if (options.subtypes && options.subtypes.length > 0) {
    queryParts.push(`subtypes:${options.subtypes.join(' OR subtypes:')}`)
  }
  if (options.setId) {
    queryParts.push(`set.id:${options.setId}`)
  }
  if (options.rarity) {
    queryParts.push(`rarity:"${options.rarity}"`)
  }
  if (options.legality) {
    queryParts.push(`legalities.${options.legality}:legal`)
  }

  if (queryParts.length === 0) {
    return { cards: [], totalCards: 0, hasMore: false, page: 1, pageSize: options.pageSize || 20, source: 'api' }
  }

  const page = options.page || 1
  const pageSize = options.pageSize || 20

  try {
    return await pokemonRequest<SearchResponse>({
      q: queryParts.join(' '),
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
  } catch {
    // Fallback to direct API
    const response = await directApiRequest<PokemonCardSearchResult>('cards', {
      q: queryParts.join(' '),
      page: page.toString(),
      pageSize: pageSize.toString(),
      orderBy: 'name',
    })

    return {
      cards: response.data,
      totalCards: response.totalCount,
      hasMore: response.page * response.pageSize < response.totalCount,
      page: response.page,
      pageSize: response.pageSize,
      source: 'api',
    }
  }
}

/**
 * Get autocomplete suggestions for card names
 */
export async function autocompleteCards(query: string): Promise<string[]> {
  if (!query || query.length < 2) {
    return []
  }

  try {
    const response = await pokemonRequest<AutocompleteResponse>({
      autocomplete: query,
    })
    return response.suggestions
  } catch {
    // Fallback: do a search and extract unique names
    const searchResult = await searchCardsDirect(query, 1, 10)
    const names = new Set(searchResult.cards.map(c => c.name))
    return Array.from(names).slice(0, 10)
  }
}

// ==================== Single Card Functions ====================

/**
 * Get a card by its Pokemon TCG API ID
 */
export async function getCardById(cardId: string): Promise<PokemonCard> {
  try {
    return await pokemonRequest<PokemonCard>({
      id: cardId,
    })
  } catch {
    // Fallback to direct API
    const response = await directApiRequest<{ data: PokemonCard }>(`cards/${cardId}`)
    return response.data
  }
}

/**
 * Get a card by exact name (returns first match)
 */
export async function getCardByName(name: string): Promise<PokemonCard | null> {
  const response = await directApiRequest<PokemonCardSearchResult>('cards', {
    q: `name:"${name}"`,
    pageSize: '1',
  })

  return response.data[0] || null
}

/**
 * Get a random card
 */
export async function getRandomCard(filters?: {
  types?: PokemonType[]
  supertype?: PokemonSupertype
}): Promise<PokemonCard> {
  const queryParts: string[] = []

  if (filters?.types && filters.types.length > 0) {
    queryParts.push(`types:${filters.types[0]}`)
  }
  if (filters?.supertype) {
    queryParts.push(`supertype:"${filters.supertype}"`)
  }

  // Get a random page, then a random card from that page
  const countResponse = await directApiRequest<PokemonCardSearchResult>('cards', {
    q: queryParts.length > 0 ? queryParts.join(' ') : 'supertype:pokemon',
    pageSize: '1',
  })

  const totalPages = Math.ceil(countResponse.totalCount / 50)
  const randomPage = Math.floor(Math.random() * Math.min(totalPages, 100)) + 1

  const response = await directApiRequest<PokemonCardSearchResult>('cards', {
    q: queryParts.length > 0 ? queryParts.join(' ') : 'supertype:pokemon',
    page: randomPage.toString(),
    pageSize: '50',
  })

  if (response.data.length === 0) {
    throw new Error('No cards found matching the criteria')
  }
  const randomIndex = Math.floor(Math.random() * response.data.length)
  return response.data[randomIndex]!
}

// ==================== Set Functions ====================

/**
 * Get all Pokemon TCG sets
 */
export async function getSets(): Promise<PokemonSet[]> {
  try {
    const response = await pokemonRequest<SetSearchResponse>({
      sets: '',
    })
    return response.sets
  } catch {
    // Fallback to direct API
    const response = await directApiRequest<PokemonSetSearchResult>('sets', {
      orderBy: '-releaseDate',
    })
    return response.data
  }
}

/**
 * Get a set by ID
 */
export async function getSetById(setId: string): Promise<PokemonSet> {
  try {
    return await pokemonRequest<PokemonSet>({
      setId: setId,
    })
  } catch {
    const response = await directApiRequest<{ data: PokemonSet }>(`sets/${setId}`)
    return response.data
  }
}

/**
 * Get all cards in a set
 */
export async function getSetCards(setId: string, page = 1, pageSize = 50): Promise<SearchResponse> {
  return searchCardsAdvanced({
    setId,
    page,
    pageSize,
  })
}

// ==================== Utility Functions ====================

/**
 * Get available Pokemon types
 */
export async function getTypes(): Promise<string[]> {
  const response = await directApiRequest<{ data: string[] }>('types')
  return response.data
}

/**
 * Get available subtypes (Stage 1, Stage 2, VMAX, etc.)
 */
export async function getSubtypes(): Promise<string[]> {
  const response = await directApiRequest<{ data: string[] }>('subtypes')
  return response.data
}

/**
 * Get available supertypes (Pokemon, Trainer, Energy)
 */
export async function getSupertypes(): Promise<string[]> {
  const response = await directApiRequest<{ data: string[] }>('supertypes')
  return response.data
}

/**
 * Get available rarities
 */
export async function getRarities(): Promise<string[]> {
  const response = await directApiRequest<{ data: string[] }>('rarities')
  return response.data
}

// ==================== Card Helper Functions ====================

/**
 * Get the best image URL for a card
 */
export function getCardImageUrl(card: PokemonCard, size: 'small' | 'large' = 'small'): string {
  return card.images[size]
}

/**
 * Get card price (TCGPlayer market price preferred)
 */
export function getCardPrice(card: PokemonCard): number | null {
  // Try TCGPlayer prices first
  const tcgPrices = card.tcgplayer?.prices
  if (tcgPrices) {
    // Prefer normal, then holofoil, then reverse
    return tcgPrices.normal?.market
      ?? tcgPrices.holofoil?.market
      ?? tcgPrices.reverseHolofoil?.market
      ?? null
  }

  // Fall back to Cardmarket
  return card.cardmarket?.prices?.averageSellPrice ?? null
}

/**
 * Format card price as currency string
 */
export function formatCardPrice(card: PokemonCard): string {
  const price = getCardPrice(card)
  if (price === null) return 'N/A'
  return `$${price.toFixed(2)}`
}

/**
 * Check if card is legal in a format
 */
export function isCardLegal(card: PokemonCard, format: keyof PokemonLegalities): boolean {
  return card.legalities[format] === 'Legal'
}

/**
 * Get card set info string
 */
export function getCardSetInfo(card: PokemonCard): string {
  return `${card.set.name} ${card.number}/${card.set.printedTotal}`
}

/**
 * Normalize card name for comparison
 */
function normalizeCardName(name: string): string {
  return name.toLowerCase().trim().replace(/[^\w\s]/g, '')
}

/**
 * Calculate search ranking score (lower is better)
 */
export function calculateSearchRank(cardName: string, query: string): number {
  const normalizedCard = normalizeCardName(cardName)
  const normalizedQuery = normalizeCardName(query)

  // Exact match
  if (normalizedCard === normalizedQuery) return 0

  // Starts with query
  if (normalizedCard.startsWith(normalizedQuery)) return 1

  // Word boundary match
  const words = normalizedCard.split(/\s+/)
  for (const word of words) {
    if (word.startsWith(normalizedQuery)) return 2
  }

  // Contains match
  if (normalizedCard.includes(normalizedQuery)) return 3

  // No match
  return 99
}

// ==================== Deck Parsing Functions ====================

/**
 * Parse a Pokemon TCG deck list string into card entries
 * Supports formats like:
 * - "4 Charizard ex SVI 125"
 * - "4 Charizard ex sv1-125"
 * - "4x Charizard ex"
 * - "Charizard ex x4"
 */
export function parseDeckList(deckList: string): Array<{
  quantity: number
  name: string
  setCode?: string
  number?: string
}> {
  const lines = deckList.split('\n').filter(line => line.trim())
  const entries: Array<{
    quantity: number
    name: string
    setCode?: string
    number?: string
  }> = []

  for (const line of lines) {
    const trimmed = line.trim()

    // Skip comments and section headers
    if (trimmed.startsWith('//') || trimmed.startsWith('#')) continue
    if (trimmed.toLowerCase().startsWith('pokemon:')) continue
    if (trimmed.toLowerCase().startsWith('trainer:')) continue
    if (trimmed.toLowerCase().startsWith('energy:')) continue
    if (trimmed.toLowerCase() === 'pokemon' || trimmed.toLowerCase() === 'trainer' || trimmed.toLowerCase() === 'energy') continue

    // Pattern: "4 Charizard ex SVI 125" or "4 Charizard ex sv1-125"
    let match = trimmed.match(/^(\d+)x?\s+(.+?)\s+([A-Z0-9]+)[\s-](\d+)$/i)
    if (match && match[1] && match[2] && match[3] && match[4]) {
      entries.push({
        quantity: parseInt(match[1], 10),
        name: match[2].trim(),
        setCode: match[3].toUpperCase(),
        number: match[4],
      })
      continue
    }

    // Pattern: "4 Charizard ex" or "4x Charizard ex"
    match = trimmed.match(/^(\d+)x?\s+(.+?)$/)
    if (match && match[1] && match[2]) {
      entries.push({
        quantity: parseInt(match[1], 10),
        name: match[2].trim(),
      })
      continue
    }

    // Pattern: "Charizard ex x4"
    const altMatch = trimmed.match(/^(.+?)\s+x?(\d+)$/)
    if (altMatch && altMatch[1] && altMatch[2]) {
      entries.push({
        quantity: parseInt(altMatch[2], 10),
        name: altMatch[1].trim(),
      })
      continue
    }

    // Single card with no quantity
    if (trimmed && !trimmed.match(/^\d+$/)) {
      entries.push({
        quantity: 1,
        name: trimmed,
      })
    }
  }

  return entries
}

/**
 * Validate deck card count (60 cards for standard formats)
 */
export function validateDeckCardCount(
  cards: Array<{ quantity: number }>,
  requiredCount = 60
): { valid: boolean; count: number; message?: string } {
  const count = cards.reduce((sum, card) => sum + card.quantity, 0)

  if (count < requiredCount) {
    return {
      valid: false,
      count,
      message: `Deck has ${count} cards, needs ${requiredCount}`,
    }
  }
  if (count > requiredCount) {
    return {
      valid: false,
      count,
      message: `Deck has ${count} cards, maximum is ${requiredCount}`,
    }
  }

  return { valid: true, count }
}

/**
 * Check for cards exceeding the 4-copy limit (except basic energy)
 */
export function findOverLimitCards(
  cards: Array<{ name: string; quantity: number; supertype?: PokemonSupertype }>
): Array<{ name: string; quantity: number }> {
  const cardCounts = new Map<string, number>()

  for (const card of cards) {
    // Basic energy cards have no limit
    if (card.supertype === 'Energy' && card.name.toLowerCase().includes('basic')) {
      continue
    }

    const currentCount = cardCounts.get(card.name) || 0
    cardCounts.set(card.name, currentCount + card.quantity)
  }

  const overLimit: Array<{ name: string; quantity: number }> = []
  for (const [name, quantity] of cardCounts) {
    if (quantity > 4) {
      overLimit.push({ name, quantity })
    }
  }

  return overLimit
}
