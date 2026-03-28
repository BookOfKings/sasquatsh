import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const SCRYFALL_API_BASE = 'https://api.scryfall.com'

// Scryfall rate limit: 10 requests per second - be conservative
const DELAY_BETWEEN_REQUESTS = 100 // 100ms = 10 req/sec

// Cache TTL: 24 hours for card data (rarely changes)
const CACHE_TTL_HOURS = 24

interface ScryfallCard {
  id: string
  oracle_id: string
  name: string
  mana_cost?: string
  cmc: number
  type_line: string
  oracle_text?: string
  power?: string
  toughness?: string
  loyalty?: string
  colors?: string[]
  color_identity: string[]
  keywords: string[]
  legalities: Record<string, string>
  set: string
  set_name: string
  collector_number: string
  rarity: string
  image_uris?: {
    small?: string
    normal?: string
    large?: string
    art_crop?: string
    png?: string
  }
  prices: Record<string, string | null>
  layout: string
  card_faces?: Array<{
    name: string
    mana_cost?: string
    type_line?: string
    oracle_text?: string
    image_uris?: {
      small?: string
      normal?: string
      large?: string
      art_crop?: string
      png?: string
    }
  }>
}

interface ScryfallSearchResponse {
  object: string
  total_cards: number
  has_more: boolean
  data: ScryfallCard[]
}

interface ScryfallAutocompleteResponse {
  object: string
  total_values: number
  data: string[]
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// Fetch with retry logic
async function fetchWithRetry(url: string, maxRetries = 3): Promise<Response> {
  let lastError: Error | null = null

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 15000)

      const response = await fetch(url, {
        signal: controller.signal,
        headers: {
          'User-Agent': 'Sasquatsh/1.0 (Game Night Planning App)',
          'Accept': 'application/json',
        },
      })
      clearTimeout(timeoutId)

      if (response.ok) {
        return response
      }

      // Scryfall returns 429 for rate limiting
      if (response.status === 429) {
        console.log(`Scryfall rate limit hit, waiting before retry ${attempt}/${maxRetries}`)
        await sleep(1000 * attempt)
        continue
      }

      lastError = new Error(`HTTP ${response.status}`)
    } catch (err) {
      lastError = err as Error
      console.error(`Fetch attempt ${attempt} failed:`, err)
    }

    if (attempt < maxRetries) {
      await sleep(500 * attempt)
    }
  }

  throw lastError || new Error('Fetch failed')
}

// Save card to cache
async function saveCardToCache(supabase: ReturnType<typeof createClient>, card: ScryfallCard): Promise<void> {
  const imageUris = card.image_uris || card.card_faces?.[0]?.image_uris

  const row = {
    scryfall_id: card.id,
    oracle_id: card.oracle_id,
    name: card.name,
    mana_cost: card.mana_cost || card.card_faces?.[0]?.mana_cost,
    cmc: card.cmc,
    type_line: card.type_line,
    oracle_text: card.oracle_text || card.card_faces?.[0]?.oracle_text,
    power: card.power,
    toughness: card.toughness,
    loyalty: card.loyalty,
    colors: card.colors || [],
    color_identity: card.color_identity,
    keywords: card.keywords,
    legalities: card.legalities,
    set_code: card.set,
    set_name: card.set_name,
    collector_number: card.collector_number,
    rarity: card.rarity,
    image_uri_small: imageUris?.small,
    image_uri_normal: imageUris?.normal,
    image_uri_large: imageUris?.large,
    image_uri_art_crop: imageUris?.art_crop,
    image_uri_png: imageUris?.png,
    prices: card.prices,
    is_double_faced: !!card.card_faces && card.card_faces.length > 1,
    card_faces: card.card_faces || null,
    layout: card.layout,
    cached_at: new Date().toISOString(),
  }

  const { error } = await supabase
    .from('scryfall_cards_cache')
    .upsert(row, { onConflict: 'scryfall_id' })

  if (error) {
    console.error('Failed to cache card:', error)
  }
}

// Save multiple cards to cache
async function saveCardsToCache(supabase: ReturnType<typeof createClient>, cards: ScryfallCard[]): Promise<void> {
  for (const card of cards) {
    await saveCardToCache(supabase, card)
  }
}

// Transform cached card to response format
function transformCachedCard(cached: Record<string, unknown>) {
  return {
    scryfallId: cached.scryfall_id,
    oracleId: cached.oracle_id,
    name: cached.name,
    manaCost: cached.mana_cost,
    cmc: cached.cmc,
    typeLine: cached.type_line,
    oracleText: cached.oracle_text,
    power: cached.power,
    toughness: cached.toughness,
    loyalty: cached.loyalty,
    colors: cached.colors,
    colorIdentity: cached.color_identity,
    keywords: cached.keywords,
    legalities: cached.legalities,
    setCode: cached.set_code,
    setName: cached.set_name,
    collectorNumber: cached.collector_number,
    rarity: cached.rarity,
    imageUris: {
      small: cached.image_uri_small,
      normal: cached.image_uri_normal,
      large: cached.image_uri_large,
      artCrop: cached.image_uri_art_crop,
      png: cached.image_uri_png,
    },
    prices: cached.prices,
    isDoubleFaced: cached.is_double_faced,
    cardFaces: cached.card_faces,
    layout: cached.layout,
  }
}

// Transform Scryfall card to response format
function transformScryfallCard(card: ScryfallCard) {
  const imageUris = card.image_uris || card.card_faces?.[0]?.image_uris

  return {
    scryfallId: card.id,
    oracleId: card.oracle_id,
    name: card.name,
    manaCost: card.mana_cost || card.card_faces?.[0]?.mana_cost,
    cmc: card.cmc,
    typeLine: card.type_line,
    oracleText: card.oracle_text || card.card_faces?.[0]?.oracle_text,
    power: card.power,
    toughness: card.toughness,
    loyalty: card.loyalty,
    colors: card.colors || [],
    colorIdentity: card.color_identity,
    keywords: card.keywords,
    legalities: card.legalities,
    setCode: card.set,
    setName: card.set_name,
    collectorNumber: card.collector_number,
    rarity: card.rarity,
    imageUris: {
      small: imageUris?.small || null,
      normal: imageUris?.normal || null,
      large: imageUris?.large || null,
      artCrop: imageUris?.art_crop || null,
      png: imageUris?.png || null,
    },
    prices: card.prices,
    isDoubleFaced: !!card.card_faces && card.card_faces.length > 1,
    cardFaces: card.card_faces?.map(face => ({
      name: face.name,
      manaCost: face.mana_cost,
      typeLine: face.type_line,
      oracleText: face.oracle_text,
      imageUris: face.image_uris ? {
        small: face.image_uris.small,
        normal: face.image_uris.normal,
        large: face.image_uris.large,
        artCrop: face.image_uris.art_crop,
        png: face.image_uris.png,
      } : null,
    })),
    layout: card.layout,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)

  // GET: Search cards
  const searchQuery = url.searchParams.get('search')
  if (searchQuery) {
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const unique = url.searchParams.get('unique') || 'cards' // cards, art, prints

    // Try local cache first with fuzzy search
    const { data: cachedResults } = await supabase
      .from('scryfall_cards_cache')
      .select('*')
      .ilike('name', `%${searchQuery}%`)
      .order('name')
      .limit(20)

    // If we have good cached results, return them
    if (cachedResults && cachedResults.length >= 5) {
      return jsonResponse({
        cards: cachedResults.map(transformCachedCard),
        totalCards: cachedResults.length,
        hasMore: false,
        source: 'cache',
      })
    }

    // Fetch from Scryfall API
    try {
      const scryfallUrl = `${SCRYFALL_API_BASE}/cards/search?q=${encodeURIComponent(searchQuery)}&unique=${unique}&order=name&page=${page}`
      const response = await fetchWithRetry(scryfallUrl)
      const data: ScryfallSearchResponse = await response.json()

      // Cache the results in background
      if (data.data && data.data.length > 0) {
        saveCardsToCache(supabase, data.data).catch(err => {
          console.error('Failed to cache search results:', err)
        })
      }

      return jsonResponse({
        cards: data.data.map(transformScryfallCard),
        totalCards: data.total_cards,
        hasMore: data.has_more,
        source: 'scryfall',
      })
    } catch (err) {
      console.error('Scryfall search failed:', err)

      // Return cached results even if incomplete
      if (cachedResults && cachedResults.length > 0) {
        return jsonResponse({
          cards: cachedResults.map(transformCachedCard),
          totalCards: cachedResults.length,
          hasMore: false,
          source: 'cache_fallback',
        })
      }

      return errorResponse('Search failed', 502)
    }
  }

  // GET: Autocomplete (fast name suggestions)
  const autocompleteQuery = url.searchParams.get('autocomplete')
  if (autocompleteQuery) {
    try {
      const scryfallUrl = `${SCRYFALL_API_BASE}/cards/autocomplete?q=${encodeURIComponent(autocompleteQuery)}`
      const response = await fetchWithRetry(scryfallUrl)
      const data: ScryfallAutocompleteResponse = await response.json()

      return jsonResponse({
        suggestions: data.data,
        total: data.total_values,
      })
    } catch (err) {
      console.error('Scryfall autocomplete failed:', err)

      // Fallback to local cache
      const { data: cachedNames } = await supabase
        .from('scryfall_cards_cache')
        .select('name')
        .ilike('name', `${autocompleteQuery}%`)
        .order('name')
        .limit(20)

      return jsonResponse({
        suggestions: cachedNames?.map(c => c.name) || [],
        total: cachedNames?.length || 0,
        source: 'cache_fallback',
      })
    }
  }

  // GET: Get card by Scryfall ID
  const cardId = url.searchParams.get('id')
  if (cardId) {
    // Check cache first
    const { data: cached } = await supabase
      .from('scryfall_cards_cache')
      .select('*')
      .eq('scryfall_id', cardId)
      .single()

    if (cached) {
      const cacheAge = Date.now() - new Date(cached.cached_at as string).getTime()
      const maxAge = CACHE_TTL_HOURS * 60 * 60 * 1000

      if (cacheAge < maxAge) {
        return jsonResponse(transformCachedCard(cached))
      }
    }

    // Fetch from Scryfall
    try {
      const scryfallUrl = `${SCRYFALL_API_BASE}/cards/${cardId}`
      const response = await fetchWithRetry(scryfallUrl)
      const card: ScryfallCard = await response.json()

      // Cache the result
      await saveCardToCache(supabase, card)

      return jsonResponse(transformScryfallCard(card))
    } catch (err) {
      console.error('Scryfall card fetch failed:', err)

      // Return stale cache if available
      if (cached) {
        return jsonResponse(transformCachedCard(cached))
      }

      return errorResponse('Card not found', 404)
    }
  }

  // GET: Get card by exact name
  const exactName = url.searchParams.get('exact')
  if (exactName) {
    // Check cache first
    const { data: cached } = await supabase
      .from('scryfall_cards_cache')
      .select('*')
      .eq('name', exactName)
      .order('cached_at', { ascending: false })
      .limit(1)
      .single()

    if (cached) {
      const cacheAge = Date.now() - new Date(cached.cached_at as string).getTime()
      const maxAge = CACHE_TTL_HOURS * 60 * 60 * 1000

      if (cacheAge < maxAge) {
        return jsonResponse(transformCachedCard(cached))
      }
    }

    // Fetch from Scryfall
    try {
      const scryfallUrl = `${SCRYFALL_API_BASE}/cards/named?exact=${encodeURIComponent(exactName)}`
      const response = await fetchWithRetry(scryfallUrl)
      const card: ScryfallCard = await response.json()

      await saveCardToCache(supabase, card)

      return jsonResponse(transformScryfallCard(card))
    } catch (err) {
      console.error('Scryfall exact name fetch failed:', err)

      if (cached) {
        return jsonResponse(transformCachedCard(cached))
      }

      return errorResponse('Card not found', 404)
    }
  }

  // GET: Get random card (optionally filtered)
  const randomQuery = url.searchParams.get('random')
  if (randomQuery !== null) {
    try {
      const q = url.searchParams.get('q') || ''
      const scryfallUrl = q
        ? `${SCRYFALL_API_BASE}/cards/random?q=${encodeURIComponent(q)}`
        : `${SCRYFALL_API_BASE}/cards/random`

      const response = await fetchWithRetry(scryfallUrl)
      const card: ScryfallCard = await response.json()

      // Cache the random card
      await saveCardToCache(supabase, card)

      return jsonResponse(transformScryfallCard(card))
    } catch (err) {
      console.error('Scryfall random card failed:', err)
      return errorResponse('Failed to get random card', 502)
    }
  }

  // GET: Get MTG formats
  const formats = url.searchParams.get('formats')
  if (formats !== null) {
    const { data: formatsData, error } = await supabase
      .from('mtg_formats')
      .select('*')
      .eq('is_active', true)
      .order('sort_order')

    if (error) {
      return errorResponse('Failed to fetch formats', 500)
    }

    return jsonResponse({
      formats: formatsData?.map(f => ({
        id: f.id,
        name: f.name,
        description: f.description,
        minDeckSize: f.min_deck_size,
        maxDeckSize: f.max_deck_size,
        maxCopies: f.max_copies,
        hasCommander: f.has_commander,
        hasSideboard: f.has_sideboard,
        sideboardSize: f.sideboard_size,
        isConstructed: f.is_constructed,
      })) || [],
    })
  }

  return errorResponse('Invalid request', 400)
})
