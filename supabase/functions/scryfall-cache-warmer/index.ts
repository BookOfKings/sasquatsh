import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, getFirebaseToken, createResponders, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

const SCRYFALL_API_BASE = 'https://api.scryfall.com'
const DELAY_BETWEEN_REQUESTS = 100 // 100ms = 10 req/sec (Scryfall limit)
const CACHE_TTL_HOURS = 24

// Top Commander staples - most commonly played cards
const COMMANDER_STAPLES = [
  // Mana rocks
  'Sol Ring', 'Arcane Signet', 'Mana Crypt', 'Chrome Mox', 'Mox Diamond',
  'Fellwar Stone', 'Mind Stone', 'Thought Vessel', 'Commander\'s Sphere',
  'Talisman of Progress', 'Talisman of Dominance', 'Talisman of Indulgence',
  'Talisman of Impulse', 'Talisman of Unity', 'Talisman of Hierarchy',
  'Talisman of Creativity', 'Talisman of Conviction', 'Talisman of Curiosity',
  'Talisman of Resilience', 'Signets', 'Dimir Signet', 'Azorius Signet',
  'Orzhov Signet', 'Boros Signet', 'Selesnya Signet', 'Golgari Signet',
  'Simic Signet', 'Izzet Signet', 'Rakdos Signet', 'Gruul Signet',

  // Lands
  'Command Tower', 'Exotic Orchard', 'City of Brass', 'Mana Confluence',
  'Reflecting Pool', 'Cavern of Souls', 'Ancient Tomb', 'Strip Mine',
  'Wasteland', 'Ghost Quarter', 'Field of the Dead', 'Reliquary Tower',
  'Urborg, Tomb of Yawgmoth', 'Cabal Coffers', 'Nykthos, Shrine to Nyx',
  'Gaea\'s Cradle', 'Serra\'s Sanctum', 'Boseiju, Who Shelters All',

  // White staples
  'Swords to Plowshares', 'Path to Exile', 'Teferi\'s Protection',
  'Smothering Tithe', 'Land Tax', 'Enlightened Tutor', 'Generous Gift',
  'Wrath of God', 'Farewell', 'Anointed Procession', 'Esper Sentinel',
  'Drannith Magistrate', 'Grand Abolisher', 'Silence', 'Flawless Maneuver',

  // Blue staples
  'Cyclonic Rift', 'Rhystic Study', 'Mystic Remora', 'Counterspell',
  'Swan Song', 'Force of Will', 'Force of Negation', 'Fierce Guardianship',
  'Mana Drain', 'Pact of Negation', 'Narset\'s Reversal', 'Mystical Tutor',
  'Windfall', 'Timetwister', 'Brainstorm', 'Ponder', 'Preordain',
  'Consecrated Sphinx', 'Thassa\'s Oracle', 'Laboratory Maniac',

  // Black staples
  'Demonic Tutor', 'Vampiric Tutor', 'Imperial Seal', 'Diabolic Intent',
  'Toxic Deluge', 'Damnation', 'Deadly Rollick', 'Feed the Swarm',
  'Necropotence', 'Ad Nauseam', 'Dark Ritual', 'Cabal Ritual',
  'Animate Dead', 'Reanimate', 'Entomb', 'Buried Alive', 'Living Death',
  'Gray Merchant of Asphodel', 'Grave Pact', 'Dictate of Erebos',

  // Red staples
  'Deflecting Swat', 'Dockside Extortionist', 'Jeska\'s Will', 'Wheel of Fortune',
  'Blasphemous Act', 'Chaos Warp', 'Gamble', 'Faithless Looting',
  'Goblin Engineer', 'Imperial Recruiter', 'Underworld Breach',
  'Pyroblast', 'Red Elemental Blast', 'Ragavan, Nimble Pilferer',

  // Green staples
  'Birds of Paradise', 'Llanowar Elves', 'Elvish Mystic', 'Fyndhorn Elves',
  'Arbor Elf', 'Wild Growth', 'Utopia Sprawl', 'Carpet of Flowers',
  'Sylvan Library', 'Worldly Tutor', 'Green Sun\'s Zenith', 'Finale of Devastation',
  'Craterhoof Behemoth', 'Beast Within', 'Nature\'s Claim', 'Heroic Intervention',
  'Collector Ouphe', 'Veil of Summer', 'Destiny Spinner',

  // Multicolor staples
  'Aura Shards', 'Mirari\'s Wake', 'Deathrite Shaman', 'Assassin\'s Trophy',
  'Abrupt Decay', 'Anguished Unmaking', 'Vindicate', 'Wear // Tear',
  'Notion Thief', 'Narset, Parter of Veils', 'Teferi, Time Raveler',

  // Artifacts
  'Lightning Greaves', 'Swiftfoot Boots', 'Skullclamp', 'Sensei\'s Divining Top',
  'Scroll Rack', 'Isochron Scepter', 'Helm of the Host', 'Cloudstone Curio',
  'Panharmonicon', 'The Great Henge', 'Bolas\'s Citadel', 'Aetherflux Reservoir',

  // Popular commanders
  'Kenrith, the Returned King', 'Korvold, Fae-Cursed King', 'Yuriko, the Tiger\'s Shadow',
  'Atraxa, Praetors\' Voice', 'Edgar Markov', 'The Ur-Dragon', 'Muldrotha, the Gravetide',
  'Kaalia of the Vast', 'Prossh, Skyraider of Kher', 'Teysa Karlov',
  'Krenko, Mob Boss', 'Talrand, Sky Summoner', 'Meren of Clan Nel Toth',
]

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
  next_page?: string
  data: ScryfallCard[]
}

interface WarmResult {
  cached: number
  skipped: number
  failed: number
  total: number
  errors?: string[]
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

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

      if (response.status === 429) {
        console.log(`Scryfall rate limit hit, waiting before retry ${attempt}/${maxRetries}`)
        await sleep(1000 * attempt)
        continue
      }

      if (response.status === 404) {
        throw new Error('Card not found')
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
    console.error('Failed to cache card:', card.name, error)
    throw error
  }
}

function isStale(cachedAt: string): boolean {
  const cacheAge = Date.now() - new Date(cachedAt).getTime()
  const maxAge = CACHE_TTL_HOURS * 60 * 60 * 1000
  return cacheAge >= maxAge
}

async function warmCardsByName(
  supabase: ReturnType<typeof createClient>,
  cardNames: string[]
): Promise<WarmResult> {
  let cached = 0
  let skipped = 0
  let failed = 0
  const errors: string[] = []

  for (const name of cardNames) {
    try {
      // Check if already cached and fresh
      const { data: existing } = await supabase
        .from('scryfall_cards_cache')
        .select('cached_at')
        .eq('name', name)
        .single()

      if (existing && !isStale(existing.cached_at)) {
        skipped++
        continue
      }

      // Rate limit
      await sleep(DELAY_BETWEEN_REQUESTS)

      // Fetch from Scryfall by exact name
      const url = `${SCRYFALL_API_BASE}/cards/named?exact=${encodeURIComponent(name)}`
      const response = await fetchWithRetry(url)
      const card: ScryfallCard = await response.json()

      await saveCardToCache(supabase, card)
      cached++
      console.log(`Cached: ${name}`)
    } catch (err) {
      failed++
      const msg = `${name}: ${(err as Error).message}`
      errors.push(msg)
      console.error(`Failed to cache: ${msg}`)
    }
  }

  return { cached, skipped, failed, total: cardNames.length, errors: errors.length > 0 ? errors : undefined }
}

async function warmBySearch(
  supabase: ReturnType<typeof createClient>,
  query: string,
  maxPages = 5
): Promise<WarmResult> {
  let cached = 0
  let skipped = 0
  let failed = 0
  let page = 1
  const errors: string[] = []

  let nextPageUrl: string | null = `${SCRYFALL_API_BASE}/cards/search?q=${encodeURIComponent(query)}&unique=cards&order=edhrec`

  while (nextPageUrl && page <= maxPages) {
    try {
      await sleep(DELAY_BETWEEN_REQUESTS)

      const response = await fetchWithRetry(nextPageUrl)
      const data: ScryfallSearchResponse = await response.json()

      for (const card of data.data) {
        try {
          // Check if already cached and fresh
          const { data: existing } = await supabase
            .from('scryfall_cards_cache')
            .select('cached_at')
            .eq('scryfall_id', card.id)
            .single()

          if (existing && !isStale(existing.cached_at)) {
            skipped++
            continue
          }

          await saveCardToCache(supabase, card)
          cached++
        } catch (err) {
          failed++
          errors.push(`${card.name}: ${(err as Error).message}`)
        }
      }

      console.log(`Page ${page}: cached ${data.data.length} cards`)

      if (data.has_more && data.next_page) {
        nextPageUrl = data.next_page
        page++
      } else {
        nextPageUrl = null
      }
    } catch (err) {
      console.error(`Search page ${page} failed:`, err)
      errors.push(`Page ${page}: ${(err as Error).message}`)
      break
    }
  }

  return { cached, skipped, failed, total: cached + skipped + failed, errors: errors.length > 0 ? errors : undefined }
}

async function getCacheStatus(supabase: ReturnType<typeof createClient>): Promise<{
  totalCards: number
  oldestEntry: string | null
  newestEntry: string | null
  staleCount: number
}> {
  const { count } = await supabase
    .from('scryfall_cards_cache')
    .select('*', { count: 'exact', head: true })

  const { data: oldest } = await supabase
    .from('scryfall_cards_cache')
    .select('cached_at')
    .order('cached_at', { ascending: true })
    .limit(1)
    .single()

  const { data: newest } = await supabase
    .from('scryfall_cards_cache')
    .select('cached_at')
    .order('cached_at', { ascending: false })
    .limit(1)
    .single()

  const staleThreshold = new Date(Date.now() - CACHE_TTL_HOURS * 60 * 60 * 1000).toISOString()
  const { count: staleCount } = await supabase
    .from('scryfall_cards_cache')
    .select('*', { count: 'exact', head: true })
    .lt('cached_at', staleThreshold)

  return {
    totalCards: count || 0,
    oldestEntry: oldest?.cached_at || null,
    newestEntry: newest?.cached_at || null,
    staleCount: staleCount || 0,
  }
}

async function verifyAdmin(
  supabase: ReturnType<typeof createClient>,
  req: Request
): Promise<{ isAdmin: boolean; error?: string }> {
  const token = getFirebaseToken(req)
  if (!token) {
    return { isAdmin: false, error: 'No authentication token' }
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return { isAdmin: false, error: 'Invalid token' }
  }

  const { data: user } = await supabase
    .from('users')
    .select('is_admin')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (!user?.is_admin) {
    return { isAdmin: false, error: 'Admin access required' }
  }

  return { isAdmin: true }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const action = url.searchParams.get('action')

  // GET: Cache status (no auth required)
  if (req.method === 'GET' && action === 'status') {
    const status = await getCacheStatus(supabase)
    return jsonResponse({
      ...status,
      cacheTtlHours: CACHE_TTL_HOURS,
      staplesListSize: COMMANDER_STAPLES.length,
    })
  }

  // All POST actions require admin auth
  if (req.method !== 'POST') {
    return errorResponse('Method not allowed', 405)
  }

  const { isAdmin, error: authError } = await verifyAdmin(supabase, req)
  if (!isAdmin) {
    return errorResponse(authError || 'Unauthorized', 403)
  }

  // POST: Warm staples
  if (action === 'warm-staples') {
    console.log(`Starting warm-staples: ${COMMANDER_STAPLES.length} cards`)
    const result = await warmCardsByName(supabase, COMMANDER_STAPLES)
    return jsonResponse({
      action: 'warm-staples',
      ...result,
    })
  }

  // POST: Warm commanders (search-based)
  if (action === 'warm-commanders') {
    const maxPages = parseInt(url.searchParams.get('pages') || '5', 10)
    console.log(`Starting warm-commanders: max ${maxPages} pages`)
    const result = await warmBySearch(supabase, 'is:commander', maxPages)
    return jsonResponse({
      action: 'warm-commanders',
      ...result,
    })
  }

  // POST: Warm by custom search query
  if (action === 'warm-search') {
    const query = url.searchParams.get('q')
    if (!query) {
      return errorResponse('Missing query parameter "q"', 400)
    }
    const maxPages = parseInt(url.searchParams.get('pages') || '3', 10)
    console.log(`Starting warm-search: "${query}" max ${maxPages} pages`)
    const result = await warmBySearch(supabase, query, maxPages)
    return jsonResponse({
      action: 'warm-search',
      query,
      ...result,
    })
  }

  // POST: Refresh stale entries
  if (action === 'refresh-stale') {
    const limit = parseInt(url.searchParams.get('limit') || '100', 10)
    const staleThreshold = new Date(Date.now() - CACHE_TTL_HOURS * 60 * 60 * 1000).toISOString()

    const { data: staleCards } = await supabase
      .from('scryfall_cards_cache')
      .select('name')
      .lt('cached_at', staleThreshold)
      .order('cached_at', { ascending: true })
      .limit(limit)

    if (!staleCards || staleCards.length === 0) {
      return jsonResponse({ action: 'refresh-stale', message: 'No stale entries found', cached: 0 })
    }

    console.log(`Refreshing ${staleCards.length} stale entries`)
    const result = await warmCardsByName(supabase, staleCards.map(c => c.name))
    return jsonResponse({
      action: 'refresh-stale',
      ...result,
    })
  }

  return errorResponse('Invalid action. Use: status, warm-staples, warm-commanders, warm-search, refresh-stale', 400)
})
