import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

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
}

// Simple XML parser helper functions
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
    // Clean up HTML entities and tags
    description = description
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<[^>]+>/g, '')
      .substring(0, 2000) // Limit length
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

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Search for games
  if (searchQuery) {
    try {
      const bggUrl = `${BGG_API_BASE}/search?query=${encodeURIComponent(searchQuery)}&type=boardgame`
      const response = await fetch(bggUrl)

      if (!response.ok) {
        return errorResponse('BGG API error', 502)
      }

      const xml = await response.text()
      const results = parseSearchResults(xml)

      // Limit to 20 results
      return jsonResponse(results.slice(0, 20))
    } catch (err) {
      console.error('BGG search error:', err)
      return errorResponse('Failed to search BGG', 500)
    }
  }

  // Get game details
  if (bggId) {
    const id = parseInt(bggId, 10)
    if (isNaN(id) || id <= 0) {
      return errorResponse('Invalid BGG ID', 400)
    }

    // Check cache first
    const { data: cached } = await supabase
      .from('bgg_games_cache')
      .select('*')
      .eq('bgg_id', id)
      .single()

    if (cached) {
      // Return cached data if less than 7 days old
      const cacheAge = Date.now() - new Date(cached.cached_at).getTime()
      if (cacheAge < 7 * 24 * 60 * 60 * 1000) {
        return jsonResponse({
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
        })
      }
    }

    // Fetch from BGG
    try {
      const bggUrl = `${BGG_API_BASE}/thing?id=${id}&stats=1`
      const response = await fetch(bggUrl)

      if (!response.ok) {
        return errorResponse('BGG API error', 502)
      }

      const xml = await response.text()
      const game = parseGameDetails(xml)

      if (!game) {
        return errorResponse('Game not found', 404)
      }

      // Cache the result
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

      return jsonResponse(game)
    } catch (err) {
      console.error('BGG details error:', err)
      return errorResponse('Failed to fetch game details', 500)
    }
  }

  return errorResponse('Missing search or id parameter', 400)
})
