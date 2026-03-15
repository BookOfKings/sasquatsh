import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders, verifyFirebaseToken, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const bggApiToken = Deno.env.get('BGG_API_TOKEN')

const BGG_API_BASE = 'https://boardgamegeek.com/xmlapi2'

// Rate limiting - BGG is sensitive to rapid requests
const DELAY_BETWEEN_REQUESTS = 1500 // 1.5 seconds

interface BggGameData {
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
  bggRank: number | null
  numRatings: number | null
  averageRating: number | null
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
async function fetchWithRetry(url: string, maxRetries = 3): Promise<Response> {
  let lastError: Error | null = null

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

      lastError = new Error(`HTTP ${response.status}`)
    } catch (err) {
      lastError = err as Error
      console.error(`Fetch attempt ${attempt} failed:`, err)
    }

    if (attempt < maxRetries) {
      await sleep(2000 * attempt) // Exponential backoff
    }
  }

  throw lastError || new Error('Fetch failed')
}

// Parse game details from BGG XML
function parseGameDetails(xml: string): BggGameData | null {
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

  // Get statistics
  const statsRegex = /<statistics[^>]*>([\s\S]*?)<\/statistics>/i
  const statsMatch = item.match(statsRegex)
  let weight: number | null = null
  let bggRank: number | null = null
  let numRatings: number | null = null
  let averageRating: number | null = null

  if (statsMatch) {
    const stats = statsMatch[1]

    const weightStr = getAttribute(stats, 'averageweight', 'value')
    weight = weightStr ? parseFloat(weightStr) : null
    if (weight) weight = Math.round(weight * 100) / 100

    const avgStr = getAttribute(stats, 'average', 'value')
    averageRating = avgStr ? parseFloat(avgStr) : null
    if (averageRating) averageRating = Math.round(averageRating * 100) / 100

    const usersRatedStr = getAttribute(stats, 'usersrated', 'value')
    numRatings = usersRatedStr ? parseInt(usersRatedStr, 10) : null

    // Get BGG rank from ranks
    const rankElements = getAllElements(stats, 'rank')
    for (const rankEl of rankElements) {
      if (getAttributeFromElement(rankEl, 'name') === 'boardgame') {
        const rankValue = getAttributeFromElement(rankEl, 'value')
        if (rankValue && rankValue !== 'Not Ranked') {
          bggRank = parseInt(rankValue, 10)
        }
        break
      }
    }
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
    bggRank,
    numRatings,
    averageRating,
  }
}

// Fetch multiple games by ID (BGG supports comma-separated IDs)
async function fetchGamesById(ids: number[]): Promise<BggGameData[]> {
  if (ids.length === 0) return []

  // BGG allows up to ~20 IDs per request
  const batchSize = 20
  const results: BggGameData[] = []

  for (let i = 0; i < ids.length; i += batchSize) {
    const batch = ids.slice(i, i + batchSize)
    const url = `${BGG_API_BASE}/thing?id=${batch.join(',')}&stats=1`

    try {
      const response = await fetchWithRetry(url)
      const xml = await response.text()

      // Parse all items in response
      const items = getAllElements(xml, 'item')
      for (const itemXml of items) {
        // Wrap in items tag for parser
        const game = parseGameDetails(`<items>${itemXml}</items>`)
        if (game) {
          results.push(game)
        }
      }
    } catch (err) {
      console.error(`Failed to fetch batch starting at ${i}:`, err)
    }

    if (i + batchSize < ids.length) {
      await sleep(DELAY_BETWEEN_REQUESTS)
    }
  }

  return results
}

// Fetch BGG "hot" list
async function fetchHotGames(): Promise<number[]> {
  const url = `${BGG_API_BASE}/hot?type=boardgame`

  try {
    const response = await fetchWithRetry(url)
    const xml = await response.text()

    const items = getAllElements(xml, 'item')
    return items
      .map(item => parseInt(getAttributeFromElement(item, 'id') || '0', 10))
      .filter(id => id > 0)
  } catch (err) {
    console.error('Failed to fetch hot games:', err)
    return []
  }
}

// Save games to cache
async function saveGamesToCache(supabase: ReturnType<typeof createClient>, games: BggGameData[]): Promise<number> {
  if (games.length === 0) return 0

  const rows = games.map(game => ({
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
    bgg_rank: game.bggRank,
    num_ratings: game.numRatings,
    average_rating: game.averageRating,
    cached_at: new Date().toISOString(),
  }))

  const { error } = await supabase
    .from('bgg_games_cache')
    .upsert(rows, { onConflict: 'bgg_id' })

  if (error) {
    console.error('Failed to save games to cache:', error)
    return 0
  }

  return games.length
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)

  // Verify admin access for write operations
  if (req.method === 'POST') {
    const token = getFirebaseToken(req)
    if (!token) {
      return errorResponse('Authentication required', 401)
    }

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) {
      return errorResponse('Invalid token', 401)
    }

    // Check if user is admin
    const { data: user } = await supabase
      .from('users')
      .select('is_admin')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user?.is_admin) {
      return errorResponse('Admin access required', 403)
    }
  }

  const action = url.searchParams.get('action')

  // GET: Cache stats
  if (req.method === 'GET' && action === 'stats') {
    const { count: totalCount } = await supabase
      .from('bgg_games_cache')
      .select('*', { count: 'exact', head: true })

    const { count: rankedCount } = await supabase
      .from('bgg_games_cache')
      .select('*', { count: 'exact', head: true })
      .not('bgg_rank', 'is', null)

    const { data: oldestGame } = await supabase
      .from('bgg_games_cache')
      .select('cached_at')
      .order('cached_at', { ascending: true })
      .limit(1)
      .single()

    return jsonResponse({
      totalGames: totalCount ?? 0,
      rankedGames: rankedCount ?? 0,
      oldestCache: oldestGame?.cached_at ?? null,
    })
  }

  // POST: Import games
  if (req.method === 'POST') {
    // Import hot games
    if (action === 'import-hot') {
      console.log('Fetching hot games from BGG...')
      const hotIds = await fetchHotGames()

      if (hotIds.length === 0) {
        return errorResponse('Failed to fetch hot games list', 502)
      }

      console.log(`Fetching details for ${hotIds.length} hot games...`)
      const games = await fetchGamesById(hotIds)

      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Imported ${saved} hot games`,
        imported: saved,
      })
    }

    // Import by ID range (for bulk population)
    if (action === 'import-range') {
      const body = await req.json()
      const { startId, endId, batchSize = 100 } = body

      if (!startId || !endId || startId > endId) {
        return errorResponse('Invalid startId/endId', 400)
      }

      const ids = []
      for (let i = startId; i <= endId && ids.length < batchSize; i++) {
        ids.push(i)
      }

      console.log(`Fetching games ${startId} to ${startId + ids.length - 1}...`)
      const games = await fetchGamesById(ids)

      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Imported ${saved} games from range ${startId}-${startId + ids.length - 1}`,
        imported: saved,
        nextStartId: startId + ids.length,
      })
    }

    // Import specific IDs
    if (action === 'import-ids') {
      const body = await req.json()
      const { ids } = body

      if (!Array.isArray(ids) || ids.length === 0) {
        return errorResponse('ids array required', 400)
      }

      console.log(`Fetching ${ids.length} specific games...`)
      const games = await fetchGamesById(ids)

      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Imported ${saved} games`,
        imported: saved,
      })
    }

    // Import top-rated games (fetch hot + known popular IDs)
    if (action === 'import-popular') {
      // Known popular game IDs (top BGG games of all time)
      const popularIds = [
        174430, // Gloomhaven
        224517, // Brass: Birmingham
        167791, // Terraforming Mars
        233078, // Twilight Imperium 4th
        316554, // Dune Imperium
        291457, // Gloomhaven: Jaws of the Lion
        220308, // Gaia Project
        342942, // Ark Nova
        187645, // Star Wars: Rebellion
        312484, // Lost Ruins of Arnak
        266192, // Wingspan
        169786, // Scythe
        28720,  // Brass: Lancashire
        161936, // Pandemic Legacy S1
        182028, // Through the Ages: A New Story
        12333,  // Twilight Struggle
        120677, // Terra Mystica
        84876,  // Castles of Burgundy
        173346, // 7 Wonders Duel
        31260,  // Agricola
        102794, // Caverna
        3076,   // Puerto Rico
        68448,  // 7 Wonders
        25613,  // Carcassonne
        13,     // Catan
        36218,  // Dominion
        39856,  // Dixit
        178900, // Codenames
        2651,   // Power Grid
        9209,   // Ticket to Ride
        230802, // Azul
        148228, // Splendor
        70323,  // King of Tokyo
        62219,  // Dominant Species
        205637, // Arkham Horror LCG
        164928, // Orléans
        110327, // Lords of Waterdeep
        157354, // Five Tribes
        126163, // Tzolk'in
        192135, // Inis
        199792, // Everdell
        295947, // Cascadia
        246900, // Eclipse Second Dawn
        314040, // Wandering Towers
        284083, // The Crew
        324856, // The Crew Mission Deep Sea
        276025, // Maracaibo
        285774, // Marvel Champions
      ]

      // Also fetch current hot games
      const hotIds = await fetchHotGames()

      // Combine and dedupe
      const allIds = [...new Set([...popularIds, ...hotIds])]

      console.log(`Fetching ${allIds.length} popular/hot games...`)
      const games = await fetchGamesById(allIds)

      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Imported ${saved} popular games`,
        imported: saved,
      })
    }

    // Refresh stale cache entries
    if (action === 'refresh-stale') {
      const { data: staleGames } = await supabase
        .from('bgg_games_cache')
        .select('bgg_id')
        .lt('cached_at', new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString())
        .order('num_ratings', { ascending: false })
        .limit(50)

      if (!staleGames || staleGames.length === 0) {
        return jsonResponse({ message: 'No stale entries to refresh', refreshed: 0 })
      }

      const ids = staleGames.map(g => g.bgg_id)
      console.log(`Refreshing ${ids.length} stale cache entries...`)

      const games = await fetchGamesById(ids)
      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Refreshed ${saved} stale entries`,
        refreshed: saved,
      })
    }

    // Refresh incomplete cache entries (missing thumbnail or player counts)
    if (action === 'refresh-incomplete') {
      const { data: incompleteGames } = await supabase
        .from('bgg_games_cache')
        .select('bgg_id')
        .is('thumbnail_url', null)
        .is('min_players', null)
        .limit(100)

      if (!incompleteGames || incompleteGames.length === 0) {
        return jsonResponse({ message: 'No incomplete entries to refresh', refreshed: 0 })
      }

      const ids = incompleteGames.map(g => g.bgg_id)
      console.log(`Refreshing ${ids.length} incomplete cache entries...`)

      const games = await fetchGamesById(ids)
      const saved = await saveGamesToCache(supabase, games)

      return jsonResponse({
        message: `Refreshed ${saved} incomplete entries`,
        refreshed: saved,
      })
    }
  }

  return errorResponse('Invalid action', 400)
})
