import type {
  MtgDeck,
  MtgDeckCard,
  CreateDeckInput,
  UpdateDeckInput,
  AddDeckCardInput,
  ImportDeckInput,
  ScryfallCard,
} from '@/types/mtg'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests to Edge Functions
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

// Transform database row to MtgDeck
function toDeck(row: Record<string, unknown>): MtgDeck {
  return {
    id: row.id as string,
    ownerUserId: row.owner_user_id as string,
    name: row.name as string,
    formatId: row.format_id as string | null,
    commanderScryfallId: row.commander_scryfall_id as string | null,
    partnerCommanderScryfallId: row.partner_commander_scryfall_id as string | null,
    description: row.description as string | null,
    powerLevel: row.power_level as number | null,
    isPublic: row.is_public as boolean,
    moxfieldId: row.moxfield_id as string | null,
    archidektId: row.archidekt_id as string | null,
    importUrl: row.import_url as string | null,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    cardCount: row.card_count as number | undefined,
    commander: row.commander ? transformCachedCard(row.commander as Record<string, unknown>) : undefined,
    partnerCommander: row.partnerCommander ? transformCachedCard(row.partnerCommander as Record<string, unknown>) : undefined,
    cards: Array.isArray(row.cards)
      ? row.cards.map(c => toDeckCard(c as Record<string, unknown>))
      : undefined,
    format: row.format as MtgDeck['format'],
  }
}

// Transform database row to MtgDeckCard
function toDeckCard(row: Record<string, unknown>): MtgDeckCard {
  return {
    id: row.id as string,
    deckId: row.deck_id as string,
    scryfallId: row.scryfall_id as string,
    quantity: row.quantity as number,
    board: row.board as MtgDeckCard['board'],
    card: row.card ? transformCachedCard(row.card as Record<string, unknown>) : undefined,
  }
}

// Transform cached card data to ScryfallCard
function transformCachedCard(row: Record<string, unknown>): ScryfallCard {
  return {
    scryfallId: row.scryfall_id as string,
    oracleId: row.oracle_id as string || '',
    name: row.name as string,
    manaCost: row.mana_cost as string | null,
    cmc: row.cmc as number || 0,
    typeLine: row.type_line as string || '',
    oracleText: row.oracle_text as string | null,
    power: row.power as string | null,
    toughness: row.toughness as string | null,
    loyalty: row.loyalty as string | null,
    colors: (row.colors as string[]) || [],
    colorIdentity: (row.color_identity as string[]) || [],
    keywords: (row.keywords as string[]) || [],
    legalities: (row.legalities as Record<string, 'legal' | 'not_legal' | 'restricted' | 'banned'>) || {},
    setCode: row.set_code as string || '',
    setName: row.set_name as string || '',
    collectorNumber: row.collector_number as string || '',
    rarity: row.rarity as string || 'common',
    imageUris: {
      small: row.image_small as string | null,
      normal: row.image_normal as string | null,
      large: row.image_large as string | null,
      artCrop: row.image_art_crop as string | null,
      png: row.image_png as string | null,
    },
    prices: (row.prices as Record<string, string | null>) || {},
    isDoubleFaced: row.is_double_faced as boolean || false,
    cardFaces: row.card_faces as ScryfallCard['cardFaces'] || null,
    layout: row.layout as string || 'normal',
  }
}

interface ListDecksResponse {
  decks: MtgDeck[]
}

interface DeckResponse {
  deck: MtgDeck
}

interface ImportResponse {
  deck: MtgDeck
  imported: number
}

/**
 * Get all decks for the current user
 */
export async function getMyDecks(token: string, formatId?: string): Promise<MtgDeck[]> {
  let path = '/mtg-decks'
  if (formatId) {
    path += `?format=${encodeURIComponent(formatId)}`
  }

  const response = await authenticatedRequest<ListDecksResponse>(path, token)
  return response.decks.map(d => toDeck(d as unknown as Record<string, unknown>))
}

/**
 * Get public decks (for browsing)
 */
export async function getPublicDecks(token: string, formatId?: string): Promise<MtgDeck[]> {
  let path = '/mtg-decks?public=true'
  if (formatId) {
    path += `&format=${encodeURIComponent(formatId)}`
  }

  const response = await authenticatedRequest<ListDecksResponse>(path, token)
  return response.decks.map(d => toDeck(d as unknown as Record<string, unknown>))
}

/**
 * Get a single deck by ID
 */
export async function getDeck(token: string, deckId: string): Promise<MtgDeck> {
  const response = await authenticatedRequest<Record<string, unknown>>(
    `/mtg-decks?id=${deckId}`,
    token
  )
  return toDeck(response)
}

/**
 * Create a new deck
 */
export async function createDeck(token: string, input: CreateDeckInput): Promise<MtgDeck> {
  const response = await authenticatedRequest<DeckResponse>('/mtg-decks', token, {
    method: 'POST',
    body: JSON.stringify(input),
  })
  return toDeck(response.deck as unknown as Record<string, unknown>)
}

/**
 * Update a deck
 */
export async function updateDeck(
  token: string,
  deckId: string,
  input: UpdateDeckInput
): Promise<MtgDeck> {
  const response = await authenticatedRequest<DeckResponse>(
    `/mtg-decks?id=${deckId}`,
    token,
    {
      method: 'PUT',
      body: JSON.stringify(input),
    }
  )
  return toDeck(response.deck as unknown as Record<string, unknown>)
}

/**
 * Delete a deck
 */
export async function deleteDeck(token: string, deckId: string): Promise<void> {
  await authenticatedRequest<{ success: boolean }>(
    `/mtg-decks?id=${deckId}`,
    token,
    { method: 'DELETE' }
  )
}

/**
 * Add cards to a deck
 */
export async function addCardsToDeck(
  token: string,
  deckId: string,
  cards: AddDeckCardInput[]
): Promise<MtgDeck> {
  const response = await authenticatedRequest<DeckResponse>(
    `/mtg-decks?id=${deckId}`,
    token,
    {
      method: 'PATCH',
      body: JSON.stringify({
        add: cards.map(c => ({
          scryfallId: c.scryfallId,
          quantity: c.quantity,
          board: c.board,
        })),
      }),
    }
  )
  return toDeck(response.deck as unknown as Record<string, unknown>)
}

/**
 * Update card quantities in a deck
 */
export async function updateCardQuantities(
  token: string,
  deckId: string,
  updates: Array<{ scryfallId: string; quantity: number; board?: string }>
): Promise<MtgDeck> {
  const response = await authenticatedRequest<DeckResponse>(
    `/mtg-decks?id=${deckId}`,
    token,
    {
      method: 'PATCH',
      body: JSON.stringify({ update: updates }),
    }
  )
  return toDeck(response.deck as unknown as Record<string, unknown>)
}

/**
 * Remove cards from a deck
 */
export async function removeCardsFromDeck(
  token: string,
  deckId: string,
  cards: Array<{ scryfallId: string; board?: string }>
): Promise<MtgDeck> {
  const response = await authenticatedRequest<DeckResponse>(
    `/mtg-decks?id=${deckId}`,
    token,
    {
      method: 'PATCH',
      body: JSON.stringify({ remove: cards }),
    }
  )
  return toDeck(response.deck as unknown as Record<string, unknown>)
}

/**
 * Import a deck from text or URL
 */
export async function importDeck(
  token: string,
  input: ImportDeckInput
): Promise<{ deck: MtgDeck; imported: number }> {
  const response = await authenticatedRequest<ImportResponse>(
    '/mtg-decks?action=import',
    token,
    {
      method: 'POST',
      body: JSON.stringify(input),
    }
  )
  return {
    deck: toDeck(response.deck as unknown as Record<string, unknown>),
    imported: response.imported,
  }
}

/**
 * Set the commander for a deck
 */
export async function setCommander(
  token: string,
  deckId: string,
  commanderScryfallId: string,
  isPartner = false
): Promise<MtgDeck> {
  const update: UpdateDeckInput = isPartner
    ? { partnerCommanderScryfallId: commanderScryfallId }
    : { commanderScryfallId }

  return updateDeck(token, deckId, update)
}

/**
 * Calculate deck statistics
 */
export function calculateDeckStats(deck: MtgDeck): {
  totalCards: number
  mainDeckCards: number
  sideboardCards: number
  averageCmc: number
  colorDistribution: Record<string, number>
} {
  if (!deck.cards) {
    return {
      totalCards: 0,
      mainDeckCards: 0,
      sideboardCards: 0,
      averageCmc: 0,
      colorDistribution: {},
    }
  }

  let totalCmc = 0
  let cardsWithCmc = 0
  const colorDistribution: Record<string, number> = {}

  let mainDeckCards = 0
  let sideboardCards = 0

  for (const entry of deck.cards) {
    if (entry.board === 'main' || entry.board === 'commander') {
      mainDeckCards += entry.quantity
    } else if (entry.board === 'sideboard') {
      sideboardCards += entry.quantity
    }

    if (entry.card) {
      // Count CMC (excluding lands)
      if (!entry.card.typeLine.toLowerCase().includes('land') && entry.card.cmc > 0) {
        totalCmc += entry.card.cmc * entry.quantity
        cardsWithCmc += entry.quantity
      }

      // Count colors
      for (const color of entry.card.colors) {
        colorDistribution[color] = (colorDistribution[color] || 0) + entry.quantity
      }
    }
  }

  return {
    totalCards: mainDeckCards + sideboardCards,
    mainDeckCards,
    sideboardCards,
    averageCmc: cardsWithCmc > 0 ? Math.round((totalCmc / cardsWithCmc) * 100) / 100 : 0,
    colorDistribution,
  }
}
