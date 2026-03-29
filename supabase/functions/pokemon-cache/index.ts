import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const POKEMON_TCG_API_BASE = 'https://api.pokemontcg.io/v2'

// Optional API key for higher rate limits (250 requests/day without, 20,000 with)
const POKEMON_TCG_API_KEY = Deno.env.get('POKEMON_TCG_API_KEY')

// Cache TTL: 24 hours for card data
const CACHE_TTL_HOURS = 24

// Pokemon TCG API types
interface PokemonCard {
  id: string
  name: string
  supertype: string
  subtypes?: string[]
  level?: string
  hp?: string
  types?: string[]
  evolvesFrom?: string
  evolvesTo?: string[]
  rules?: string[]
  ancientTrait?: {
    name: string
    text: string
  }
  abilities?: Array<{
    name: string
    text: string
    type: string
  }>
  attacks?: Array<{
    name: string
    cost: string[]
    convertedEnergyCost: number
    damage: string
    text: string
  }>
  weaknesses?: Array<{
    type: string
    value: string
  }>
  resistances?: Array<{
    type: string
    value: string
  }>
  retreatCost?: string[]
  convertedRetreatCost?: number
  set: {
    id: string
    name: string
    series: string
    printedTotal: number
    total: number
    legalities: Record<string, string>
    ptcgoCode?: string
    releaseDate: string
    updatedAt: string
    images: {
      symbol: string
      logo: string
    }
  }
  number: string
  artist?: string
  rarity?: string
  flavorText?: string
  nationalPokedexNumbers?: number[]
  legalities: Record<string, string>
  regulationMark?: string
  images: {
    small: string
    large: string
  }
  tcgplayer?: {
    url: string
    updatedAt: string
    prices?: Record<string, {
      low?: number
      mid?: number
      high?: number
      market?: number
      directLow?: number
    }>
  }
  cardmarket?: {
    url: string
    updatedAt: string
    prices?: Record<string, number>
  }
}

interface PokemonSet {
  id: string
  name: string
  series: string
  printedTotal: number
  total: number
  legalities: Record<string, string>
  ptcgoCode?: string
  releaseDate: string
  updatedAt: string
  images: {
    symbol: string
    logo: string
  }
}

interface PokemonSearchResponse {
  data: PokemonCard[]
  page: number
  pageSize: number
  count: number
  totalCount: number
}

interface PokemonSetResponse {
  data: PokemonSet[]
  page: number
  pageSize: number
  count: number
  totalCount: number
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// Get API headers (with optional API key)
function getApiHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Accept': 'application/json',
    'User-Agent': 'Sasquatsh/1.0 (Game Night Planning App)',
  }

  if (POKEMON_TCG_API_KEY) {
    headers['X-Api-Key'] = POKEMON_TCG_API_KEY
  }

  return headers
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
        headers: getApiHeaders(),
      })
      clearTimeout(timeoutId)

      if (response.ok) {
        return response
      }

      // Rate limiting
      if (response.status === 429) {
        console.log(`Pokemon TCG API rate limit hit, waiting before retry ${attempt}/${maxRetries}`)
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
async function saveCardToCache(supabase: ReturnType<typeof createClient>, card: PokemonCard): Promise<void> {
  const row = {
    pokemon_tcg_id: card.id,
    name: card.name,
    supertype: card.supertype,
    subtypes: card.subtypes || [],
    hp: card.hp,
    types: card.types || [],
    evolves_from: card.evolvesFrom,
    evolves_to: card.evolvesTo || [],
    abilities: card.abilities || [],
    attacks: card.attacks || [],
    weaknesses: card.weaknesses || [],
    resistances: card.resistances || [],
    retreat_cost: card.retreatCost || [],
    set_id: card.set.id,
    set_name: card.set.name,
    set_series: card.set.series,
    card_number: card.number,
    artist: card.artist,
    rarity: card.rarity,
    flavor_text: card.flavorText,
    national_pokedex_numbers: card.nationalPokedexNumbers || [],
    legalities: card.legalities,
    regulation_mark: card.regulationMark,
    image_small: card.images.small,
    image_large: card.images.large,
    tcgplayer_url: card.tcgplayer?.url,
    tcgplayer_prices: card.tcgplayer?.prices || null,
    cardmarket_url: card.cardmarket?.url,
    cardmarket_prices: card.cardmarket?.prices || null,
    cached_at: new Date().toISOString(),
    stale_at: new Date(Date.now() + CACHE_TTL_HOURS * 60 * 60 * 1000).toISOString(),
  }

  const { error } = await supabase
    .from('pokemon_cards_cache')
    .upsert(row, { onConflict: 'pokemon_tcg_id' })

  if (error) {
    console.error('Failed to cache Pokemon card:', error)
  }
}

// Save multiple cards to cache
async function saveCardsToCache(supabase: ReturnType<typeof createClient>, cards: PokemonCard[]): Promise<void> {
  for (const card of cards) {
    await saveCardToCache(supabase, card)
  }
}

// Save set to cache
async function saveSetToCache(supabase: ReturnType<typeof createClient>, set: PokemonSet): Promise<void> {
  const row = {
    set_id: set.id,
    name: set.name,
    series: set.series,
    printed_total: set.printedTotal,
    total: set.total,
    ptcgo_code: set.ptcgoCode,
    release_date: set.releaseDate,
    legalities: set.legalities,
    symbol_url: set.images.symbol,
    logo_url: set.images.logo,
    cached_at: new Date().toISOString(),
    stale_at: new Date(Date.now() + CACHE_TTL_HOURS * 60 * 60 * 1000).toISOString(),
  }

  const { error } = await supabase
    .from('pokemon_sets_cache')
    .upsert(row, { onConflict: 'set_id' })

  if (error) {
    console.error('Failed to cache Pokemon set:', error)
  }
}

// Transform cached card to response format
function transformCachedCard(cached: Record<string, unknown>): PokemonCard {
  return {
    id: cached.pokemon_tcg_id as string,
    name: cached.name as string,
    supertype: cached.supertype as string,
    subtypes: cached.subtypes as string[],
    hp: cached.hp as string,
    types: cached.types as string[],
    evolvesFrom: cached.evolves_from as string,
    evolvesTo: cached.evolves_to as string[],
    abilities: cached.abilities as PokemonCard['abilities'],
    attacks: cached.attacks as PokemonCard['attacks'],
    weaknesses: cached.weaknesses as PokemonCard['weaknesses'],
    resistances: cached.resistances as PokemonCard['resistances'],
    retreatCost: cached.retreat_cost as string[],
    set: {
      id: cached.set_id as string,
      name: cached.set_name as string,
      series: cached.set_series as string,
      printedTotal: 0, // Not stored individually
      total: 0,
      legalities: {},
      releaseDate: '',
      updatedAt: '',
      images: {
        symbol: '',
        logo: '',
      },
    },
    number: cached.card_number as string,
    artist: cached.artist as string,
    rarity: cached.rarity as string,
    flavorText: cached.flavor_text as string,
    nationalPokedexNumbers: cached.national_pokedex_numbers as number[],
    legalities: cached.legalities as Record<string, string>,
    regulationMark: cached.regulation_mark as string,
    images: {
      small: cached.image_small as string,
      large: cached.image_large as string,
    },
    tcgplayer: cached.tcgplayer_url ? {
      url: cached.tcgplayer_url as string,
      updatedAt: '',
      prices: cached.tcgplayer_prices as PokemonCard['tcgplayer']['prices'],
    } : undefined,
    cardmarket: cached.cardmarket_url ? {
      url: cached.cardmarket_url as string,
      updatedAt: '',
      prices: cached.cardmarket_prices as Record<string, number>,
    } : undefined,
  }
}

// Transform cached set to response format
function transformCachedSet(cached: Record<string, unknown>): PokemonSet {
  return {
    id: cached.set_id as string,
    name: cached.name as string,
    series: cached.series as string,
    printedTotal: cached.printed_total as number,
    total: cached.total as number,
    legalities: cached.legalities as Record<string, string>,
    ptcgoCode: cached.ptcgo_code as string,
    releaseDate: cached.release_date as string,
    updatedAt: '',
    images: {
      symbol: cached.symbol_url as string,
      logo: cached.logo_url as string,
    },
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

  // GET: Search cards by name
  const searchQuery = url.searchParams.get('search')
  if (searchQuery) {
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20', 10)

    // Try local cache first
    const { data: cachedResults } = await supabase
      .from('pokemon_cards_cache')
      .select('*')
      .ilike('name', `%${searchQuery}%`)
      .order('name')
      .range((page - 1) * pageSize, page * pageSize - 1)

    // Count total matches in cache
    const { count: cacheCount } = await supabase
      .from('pokemon_cards_cache')
      .select('*', { count: 'exact', head: true })
      .ilike('name', `%${searchQuery}%`)

    // If we have cached results, return them
    if (cachedResults && cachedResults.length >= 5) {
      return jsonResponse({
        cards: cachedResults.map(transformCachedCard),
        totalCards: cacheCount || cachedResults.length,
        hasMore: (cacheCount || 0) > page * pageSize,
        page,
        pageSize,
        source: 'cache',
      })
    }

    // Fetch from Pokemon TCG API
    try {
      const apiUrl = `${POKEMON_TCG_API_BASE}/cards?q=name:"${encodeURIComponent(searchQuery)}*"&page=${page}&pageSize=${pageSize}&orderBy=name`
      const response = await fetchWithRetry(apiUrl)
      const data: PokemonSearchResponse = await response.json()

      // Cache the results in background
      if (data.data && data.data.length > 0) {
        saveCardsToCache(supabase, data.data).catch(err => {
          console.error('Failed to cache search results:', err)
        })
      }

      return jsonResponse({
        cards: data.data,
        totalCards: data.totalCount,
        hasMore: data.page * data.pageSize < data.totalCount,
        page: data.page,
        pageSize: data.pageSize,
        source: 'api',
      })
    } catch (err) {
      console.error('Pokemon TCG API search failed:', err)

      // Return cached results even if incomplete
      if (cachedResults && cachedResults.length > 0) {
        return jsonResponse({
          cards: cachedResults.map(transformCachedCard),
          totalCards: cacheCount || cachedResults.length,
          hasMore: false,
          page,
          pageSize,
          source: 'cache_fallback',
        })
      }

      return errorResponse('Search failed', 502)
    }
  }

  // GET: Advanced query search
  const queryParam = url.searchParams.get('q')
  if (queryParam) {
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const pageSize = parseInt(url.searchParams.get('pageSize') || '20', 10)

    try {
      const apiUrl = `${POKEMON_TCG_API_BASE}/cards?q=${encodeURIComponent(queryParam)}&page=${page}&pageSize=${pageSize}&orderBy=name`
      const response = await fetchWithRetry(apiUrl)
      const data: PokemonSearchResponse = await response.json()

      // Cache the results
      if (data.data && data.data.length > 0) {
        saveCardsToCache(supabase, data.data).catch(err => {
          console.error('Failed to cache query results:', err)
        })
      }

      return jsonResponse({
        cards: data.data,
        totalCards: data.totalCount,
        hasMore: data.page * data.pageSize < data.totalCount,
        page: data.page,
        pageSize: data.pageSize,
        source: 'api',
      })
    } catch (err) {
      console.error('Pokemon TCG API query failed:', err)
      return errorResponse('Query failed', 502)
    }
  }

  // GET: Autocomplete card names
  const autocompleteQuery = url.searchParams.get('autocomplete')
  if (autocompleteQuery) {
    // Pokemon TCG API doesn't have autocomplete, use cache
    const { data: cachedNames } = await supabase
      .from('pokemon_cards_cache')
      .select('name')
      .ilike('name', `${autocompleteQuery}%`)
      .order('name')
      .limit(20)

    // Get unique names
    const uniqueNames = [...new Set(cachedNames?.map(c => c.name) || [])]

    return jsonResponse({
      suggestions: uniqueNames,
      total: uniqueNames.length,
    })
  }

  // GET: Get card by ID
  const cardId = url.searchParams.get('id')
  if (cardId) {
    // Check cache first
    const { data: cached } = await supabase
      .from('pokemon_cards_cache')
      .select('*')
      .eq('pokemon_tcg_id', cardId)
      .single()

    if (cached) {
      const cacheAge = Date.now() - new Date(cached.cached_at as string).getTime()
      const maxAge = CACHE_TTL_HOURS * 60 * 60 * 1000

      if (cacheAge < maxAge) {
        return jsonResponse(transformCachedCard(cached))
      }
    }

    // Fetch from API
    try {
      const apiUrl = `${POKEMON_TCG_API_BASE}/cards/${cardId}`
      const response = await fetchWithRetry(apiUrl)
      const data: { data: PokemonCard } = await response.json()

      // Cache the result
      await saveCardToCache(supabase, data.data)

      return jsonResponse(data.data)
    } catch (err) {
      console.error('Pokemon TCG API card fetch failed:', err)

      // Return stale cache if available
      if (cached) {
        return jsonResponse(transformCachedCard(cached))
      }

      return errorResponse('Card not found', 404)
    }
  }

  // GET: Get all sets
  const setsParam = url.searchParams.get('sets')
  if (setsParam !== null) {
    // Check cache first
    const { data: cachedSets } = await supabase
      .from('pokemon_sets_cache')
      .select('*')
      .order('release_date', { ascending: false })

    if (cachedSets && cachedSets.length > 50) {
      return jsonResponse({
        sets: cachedSets.map(transformCachedSet),
        totalSets: cachedSets.length,
        hasMore: false,
        page: 1,
        pageSize: cachedSets.length,
        source: 'cache',
      })
    }

    // Fetch from API
    try {
      const apiUrl = `${POKEMON_TCG_API_BASE}/sets?orderBy=-releaseDate`
      const response = await fetchWithRetry(apiUrl)
      const data: PokemonSetResponse = await response.json()

      // Cache all sets
      for (const set of data.data) {
        saveSetToCache(supabase, set).catch(err => {
          console.error('Failed to cache set:', err)
        })
      }

      return jsonResponse({
        sets: data.data,
        totalSets: data.totalCount,
        hasMore: false,
        page: 1,
        pageSize: data.data.length,
        source: 'api',
      })
    } catch (err) {
      console.error('Pokemon TCG API sets fetch failed:', err)

      if (cachedSets && cachedSets.length > 0) {
        return jsonResponse({
          sets: cachedSets.map(transformCachedSet),
          totalSets: cachedSets.length,
          hasMore: false,
          page: 1,
          pageSize: cachedSets.length,
          source: 'cache_fallback',
        })
      }

      return errorResponse('Failed to fetch sets', 502)
    }
  }

  // GET: Get set by ID
  const setId = url.searchParams.get('setId')
  if (setId) {
    // Check cache first
    const { data: cached } = await supabase
      .from('pokemon_sets_cache')
      .select('*')
      .eq('set_id', setId)
      .single()

    if (cached) {
      const cacheAge = Date.now() - new Date(cached.cached_at as string).getTime()
      const maxAge = CACHE_TTL_HOURS * 60 * 60 * 1000

      if (cacheAge < maxAge) {
        return jsonResponse(transformCachedSet(cached))
      }
    }

    // Fetch from API
    try {
      const apiUrl = `${POKEMON_TCG_API_BASE}/sets/${setId}`
      const response = await fetchWithRetry(apiUrl)
      const data: { data: PokemonSet } = await response.json()

      // Cache the result
      await saveSetToCache(supabase, data.data)

      return jsonResponse(data.data)
    } catch (err) {
      console.error('Pokemon TCG API set fetch failed:', err)

      if (cached) {
        return jsonResponse(transformCachedSet(cached))
      }

      return errorResponse('Set not found', 404)
    }
  }

  // GET: Cache stats (for admin)
  const statsParam = url.searchParams.get('stats')
  if (statsParam !== null) {
    const { count: totalCards } = await supabase
      .from('pokemon_cards_cache')
      .select('*', { count: 'exact', head: true })

    const { count: totalSets } = await supabase
      .from('pokemon_sets_cache')
      .select('*', { count: 'exact', head: true })

    const { count: staleCards } = await supabase
      .from('pokemon_cards_cache')
      .select('*', { count: 'exact', head: true })
      .lt('stale_at', new Date().toISOString())

    const { count: staleSets } = await supabase
      .from('pokemon_sets_cache')
      .select('*', { count: 'exact', head: true })
      .lt('stale_at', new Date().toISOString())

    return jsonResponse({
      totalCards: totalCards || 0,
      totalSets: totalSets || 0,
      staleCards: staleCards || 0,
      staleSets: staleSets || 0,
    })
  }

  return errorResponse('Invalid request', 400)
})
