import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const bggApiToken = Deno.env.get('BGG_API_TOKEN')

const BGG_API_BASE = 'https://boardgamegeek.com/xmlapi2'

interface BggGame {
  bggId: number
  name: string
  yearPublished: number | null
  thumbnailUrl: string | null
  imageUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  minPlaytime: number | null
  maxPlaytime: number | null
  playingTime: number | null
  weight: number | null
  description: string | null
  categories: string[]
  mechanics: string[]
}

interface BggSearchResult {
  bggId: number
  name: string
  yearPublished: number | null
  thumbnailUrl?: string | null
}

// XML parsing helpers
function getElementText(xml: string, tag: string): string | null {
  const regex = new RegExp(`<${tag}[^>]*>([^<]*)</${tag}>`, 'i')
  const match = xml.match(regex)
  return match ? decodeXmlEntities(match[1].trim()) : null
}

function getAttribute(xml: string, tag: string, attr: string): string | null {
  const tagRegex = new RegExp(`<${tag}[^>]*${attr}="([^"]*)"[^>]*>`, 'i')
  const match = xml.match(tagRegex)
  return match ? decodeXmlEntities(match[1]) : null
}

function getAttributeFromElement(element: string, attr: string): string | null {
  const regex = new RegExp(`${attr}="([^"]*)"`, 'i')
  const match = element.match(regex)
  return match ? decodeXmlEntities(match[1]) : null
}

function getAllElements(xml: string, tag: string): string[] {
  const regex = new RegExp(`<${tag}[^>]*(?:/>|>[^]*?</${tag}>)`, 'gi')
  return xml.match(regex) || []
}

function decodeXmlEntities(text: string): string {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#10;/g, '\n')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(parseInt(code, 10)))
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// Fetch with retry logic
async function fetchWithRetry(url: string, maxRetries = 3): Promise<Response | null> {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 15000) // 15 second timeout

      const headers: Record<string, string> = {}
      if (bggApiToken) {
        headers['Authorization'] = `Bearer ${bggApiToken}`
      }

      const response = await fetch(url, { signal: controller.signal, headers })
      clearTimeout(timeoutId)

      if (response.ok) {
        return response
      }

      // BGG returns 202 when processing - need to wait and retry
      if (response.status === 202) {
        console.log(`BGG returned 202, waiting before retry ${attempt}/${maxRetries}`)
        await sleep(3000)
        continue
      }

      console.error(`BGG returned ${response.status}`)
    } catch (err) {
      console.error(`Fetch attempt ${attempt} failed:`, err)
    }

    if (attempt < maxRetries) {
      await sleep(2000 * attempt) // Exponential backoff
    }
  }

  return null
}

// Parse search results from BGG XML
function parseSearchResults(xml: string): BggSearchResult[] {
  const items = getAllElements(xml, 'item')
  return items.map(item => {
    const bggId = parseInt(getAttributeFromElement(item, 'id') || '0', 10)

    // Get primary name
    const nameElements = getAllElements(item, 'name')
    let name = ''
    for (const nameEl of nameElements) {
      if (getAttributeFromElement(nameEl, 'type') === 'primary') {
        name = getAttributeFromElement(nameEl, 'value') || ''
        break
      }
    }
    if (!name && nameElements.length > 0) {
      name = getAttributeFromElement(nameElements[0], 'value') || ''
    }

    const yearPublished = parseInt(getAttribute(item, 'yearpublished', 'value') || '0', 10) || null

    return { bggId, name, yearPublished }
  }).filter(r => r.bggId > 0 && r.name)
}

// Parse game details from BGG XML
function parseGameDetails(xml: string): BggGame | null {
  const items = getAllElements(xml, 'item')
  if (items.length === 0) return null

  const item = items[0]
  const bggId = parseInt(getAttributeFromElement(item, 'id') || '0', 10)
  if (!bggId) return null

  // Get primary name
  const nameElements = getAllElements(item, 'name')
  let name = ''
  for (const nameEl of nameElements) {
    if (getAttributeFromElement(nameEl, 'type') === 'primary') {
      name = getAttributeFromElement(nameEl, 'value') || ''
      break
    }
  }

  const yearPublished = parseInt(getAttribute(item, 'yearpublished', 'value') || '0', 10) || null
  const thumbnailUrl = getElementText(item, 'thumbnail')
  const imageUrl = getElementText(item, 'image')
  const minPlayers = parseInt(getAttribute(item, 'minplayers', 'value') || '0', 10) || null
  const maxPlayers = parseInt(getAttribute(item, 'maxplayers', 'value') || '0', 10) || null
  const minPlaytime = parseInt(getAttribute(item, 'minplaytime', 'value') || '0', 10) || null
  const maxPlaytime = parseInt(getAttribute(item, 'maxplaytime', 'value') || '0', 10) || null
  const playingTime = parseInt(getAttribute(item, 'playingtime', 'value') || '0', 10) || null

  // Get weight from statistics
  const statsRegex = /<statistics[^>]*>([\s\S]*?)<\/statistics>/i
  const statsMatch = item.match(statsRegex)
  let weight: number | null = null
  if (statsMatch) {
    const weightStr = getAttribute(statsMatch[1], 'averageweight', 'value')
    weight = weightStr ? parseFloat(weightStr) : null
    if (weight) weight = Math.round(weight * 100) / 100
  }

  // Get description
  let description = getElementText(item, 'description')
  if (description) {
    description = description
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<[^>]+>/g, '')
      .substring(0, 2000)
  }

  // Get categories
  const categoryElements = getAllElements(item, 'link').filter(
    el => getAttributeFromElement(el, 'type') === 'boardgamecategory'
  )
  const categories = categoryElements
    .map(el => getAttributeFromElement(el, 'value'))
    .filter((v): v is string => !!v)

  // Get mechanics
  const mechanicElements = getAllElements(item, 'link').filter(
    el => getAttributeFromElement(el, 'type') === 'boardgamemechanic'
  )
  const mechanics = mechanicElements
    .map(el => getAttributeFromElement(el, 'value'))
    .filter((v): v is string => !!v)

  return {
    bggId,
    name,
    yearPublished,
    thumbnailUrl,
    imageUrl,
    minPlayers,
    maxPlayers,
    minPlaytime,
    maxPlaytime,
    playingTime,
    weight,
    description,
    categories,
    mechanics,
  }
}

// Search local cache first
async function searchLocalCache(supabase: ReturnType<typeof createClient>, query: string): Promise<BggSearchResult[]> {
  // Try the full-text search function first
  const { data, error } = await supabase.rpc('search_bgg_cache', {
    search_query: query,
    result_limit: 20,
  })

  if (!error && data && data.length > 0) {
    return data.map((row: {
      bgg_id: number
      name: string
      year_published: number | null
      thumbnail_url: string | null
    }) => ({
      bggId: row.bgg_id,
      name: row.name,
      yearPublished: row.year_published,
      thumbnailUrl: row.thumbnail_url,
    }))
  }

  // Fallback to simple ILIKE search if function doesn't exist yet
  const { data: fallbackData } = await supabase
    .from('bgg_games_cache')
    .select('bgg_id, name, year_published, thumbnail_url')
    .ilike('name', `%${query}%`)
    .order('num_ratings', { ascending: false, nullsFirst: false })
    .limit(20)

  if (fallbackData && fallbackData.length > 0) {
    return fallbackData.map(row => ({
      bggId: row.bgg_id,
      name: row.name,
      yearPublished: row.year_published,
      thumbnailUrl: row.thumbnail_url,
    }))
  }

  return []
}

// Get game from cache
async function getFromCache(supabase: ReturnType<typeof createClient>, bggId: number): Promise<BggGame | null> {
  const { data: cached } = await supabase
    .from('bgg_games_cache')
    .select('*')
    .eq('bgg_id', bggId)
    .single()

  if (cached) {
    // Return cached data if less than 7 days old
    const cacheAge = Date.now() - new Date(cached.cached_at).getTime()
    if (cacheAge < 7 * 24 * 60 * 60 * 1000) {
      return {
        bggId: cached.bgg_id,
        name: cached.name,
        yearPublished: cached.year_published,
        thumbnailUrl: cached.thumbnail_url,
        imageUrl: cached.image_url,
        minPlayers: cached.min_players,
        maxPlayers: cached.max_players,
        minPlaytime: cached.min_playtime,
        maxPlaytime: cached.max_playtime,
        playingTime: cached.playing_time,
        weight: cached.weight ? parseFloat(cached.weight) : null,
        description: cached.description,
        categories: cached.categories || [],
        mechanics: cached.mechanics || [],
      }
    }
  }

  return null
}

// Save game to cache
async function saveToCache(supabase: ReturnType<typeof createClient>, game: BggGame): Promise<void> {
  await supabase
    .from('bgg_games_cache')
    .upsert({
      bgg_id: game.bggId,
      name: game.name,
      year_published: game.yearPublished,
      thumbnail_url: game.thumbnailUrl,
      image_url: game.imageUrl,
      min_players: game.minPlayers,
      max_players: game.maxPlayers,
      min_playtime: game.minPlaytime,
      max_playtime: game.maxPlaytime,
      playing_time: game.playingTime,
      weight: game.weight,
      description: game.description,
      categories: game.categories,
      mechanics: game.mechanics,
      cached_at: new Date().toISOString(),
    })
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  if (req.method !== 'GET') {
    return errorResponse('Method not allowed', 405)
  }

  const url = new URL(req.url)
  const searchQuery = url.searchParams.get('search')
  const bggId = url.searchParams.get('id')
  const skipCache = url.searchParams.get('skipCache') === 'true'

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Search for games
  if (searchQuery) {
    try {
      // 1. Try local cache first (fast!)
      if (!skipCache) {
        const cachedResults = await searchLocalCache(supabase, searchQuery)
        if (cachedResults.length > 0) {
          console.log(`Cache hit for search: "${searchQuery}" (${cachedResults.length} results)`)
          return jsonResponse(cachedResults)
        }
      }

      console.log(`Cache miss for search: "${searchQuery}", falling back to BGG API`)

      // 2. Fall back to BGG API
      const bggUrl = `${BGG_API_BASE}/search?query=${encodeURIComponent(searchQuery)}&type=boardgame`
      const response = await fetchWithRetry(bggUrl)

      if (!response) {
        // If BGG API fails, return empty results instead of error
        console.error('BGG API failed, returning empty results')
        return jsonResponse([])
      }

      const xml = await response.text()
      const results = parseSearchResults(xml)

      // Limit to 20 results
      return jsonResponse(results.slice(0, 20))
    } catch (err) {
      console.error('BGG search error:', err)
      // Return empty results instead of error to not break UI
      return jsonResponse([])
    }
  }

  // Get game details
  if (bggId) {
    const id = parseInt(bggId, 10)
    if (isNaN(id) || id <= 0) {
      return errorResponse('Invalid BGG ID', 400)
    }

    try {
      // 1. Check cache first
      if (!skipCache) {
        const cached = await getFromCache(supabase, id)
        if (cached) {
          console.log(`Cache hit for game ID: ${id}`)
          return jsonResponse(cached)
        }
      }

      console.log(`Cache miss for game ID: ${id}, fetching from BGG`)

      // 2. Fetch from BGG
      const bggUrl = `${BGG_API_BASE}/thing?id=${id}&stats=1`
      const response = await fetchWithRetry(bggUrl)

      if (!response) {
        return errorResponse('Failed to fetch from BGG API - please try again', 503)
      }

      const xml = await response.text()
      const game = parseGameDetails(xml)

      if (!game) {
        return errorResponse('Game not found', 404)
      }

      // 3. Cache the result (fire and forget)
      saveToCache(supabase, game).catch(err => {
        console.error('Failed to cache game:', err)
      })

      return jsonResponse(game)
    } catch (err) {
      console.error('BGG details error:', err)
      return errorResponse('Failed to fetch game details - please try again', 503)
    }
  }

  return errorResponse('Missing search or id parameter', 400)
})
