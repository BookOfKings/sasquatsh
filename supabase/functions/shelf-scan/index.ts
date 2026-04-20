import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const gcpApiKey = Deno.env.get('GCP_VISION_API_KEY')!

const VISION_API_URL = `https://vision.googleapis.com/v1/images:annotate?key=${gcpApiKey}`

// Tier limits for shelf scans per month
const SCAN_LIMITS: Record<string, number> = {
  free: 5,
  basic: 20,
  pro: Infinity,
  premium: Infinity,
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  const token = getFirebaseToken(req)
  if (!token) return errorResponse('Authentication required', 401)
  const decoded = await verifyFirebaseToken(token)
  if (!decoded) return errorResponse('Invalid token', 401)

  const { data: user } = await supabase
    .from('users')
    .select('id, subscription_tier, subscription_override_tier')
    .eq('firebase_uid', decoded.uid)
    .single()
  if (!user) return errorResponse('User not found', 404)

  const effectiveTier = user.subscription_override_tier || user.subscription_tier || 'free'

  // GET /shelf-scan — Get remaining scans this month
  if (req.method === 'GET') {
    const limit = SCAN_LIMITS[effectiveTier] ?? 5
    const monthStart = new Date()
    monthStart.setDate(1)
    monthStart.setHours(0, 0, 0, 0)

    const { count } = await supabase
      .from('shelf_scans')
      .select('id', { count: 'exact', head: true })
      .eq('user_id', user.id)
      .gte('created_at', monthStart.toISOString())

    const used = count ?? 0
    const remaining = limit === Infinity ? -1 : Math.max(0, limit - used)

    return jsonResponse({ used, limit: limit === Infinity ? 'unlimited' : limit, remaining })
  }

  // POST /shelf-scan — Scan a shelf image
  if (req.method === 'POST') {
    // Check tier limit
    const limit = SCAN_LIMITS[effectiveTier] ?? 5
    if (limit !== Infinity) {
      const monthStart = new Date()
      monthStart.setDate(1)
      monthStart.setHours(0, 0, 0, 0)

      const { count } = await supabase
        .from('shelf_scans')
        .select('id', { count: 'exact', head: true })
        .eq('user_id', user.id)
        .gte('created_at', monthStart.toISOString())

      if ((count ?? 0) >= limit) {
        return errorResponse(`You've used all ${limit} shelf scans this month. Upgrade your plan for more.`, 429)
      }
    }

    // Get image from request
    const contentType = req.headers.get('content-type') || ''
    let imageBase64: string

    if (contentType.includes('multipart/form-data')) {
      const formData = await req.formData()
      const file = formData.get('image') as File
      if (!file) return errorResponse('No image file provided', 400)

      // Validate file type
      if (!file.type.startsWith('image/')) {
        return errorResponse('File must be an image', 400)
      }
      // Max 10MB
      if (file.size > 10 * 1024 * 1024) {
        return errorResponse('Image must be under 10MB', 400)
      }

      const buffer = await file.arrayBuffer()
      imageBase64 = btoa(String.fromCharCode(...new Uint8Array(buffer)))
    } else {
      // JSON body with base64 image
      const body = await req.json()
      if (!body.image) return errorResponse('image field required (base64)', 400)
      // Strip data URL prefix if present
      imageBase64 = body.image.replace(/^data:image\/[^;]+;base64,/, '')
    }

    // Call Google Cloud Vision API
    let detectedTexts: string[]
    try {
      const visionResponse = await fetch(VISION_API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          requests: [{
            image: { content: imageBase64 },
            features: [
              { type: 'TEXT_DETECTION', maxResults: 50 },
            ],
          }],
        }),
      })

      if (!visionResponse.ok) {
        const errText = await visionResponse.text()
        console.error('Vision API error:', visionResponse.status, errText)
        return errorResponse('Failed to analyze image', 502)
      }

      const visionData = await visionResponse.json()
      const annotations = visionData.responses?.[0]?.textAnnotations

      if (!annotations || annotations.length === 0) {
        // Record scan even if no text found
        await supabase.from('shelf_scans').insert({
          user_id: user.id,
          games_detected: 0,
          games_added: 0,
        })
        return jsonResponse({ games: [], rawText: '', message: 'No text detected in image' })
      }

      // Full text from the image
      const fullText = annotations[0].description as string

      // Split into lines and extract potential game titles
      detectedTexts = extractGameTitles(fullText)
    } catch (err) {
      console.error('Vision API fetch error:', err)
      return errorResponse('Failed to connect to image analysis service', 502)
    }

    // Search BGG for each detected title
    const gameMatches: Array<{
      detectedTitle: string
      bggId: number | null
      name: string | null
      yearPublished: number | null
      thumbnailUrl: string | null
      minPlayers: number | null
      maxPlayers: number | null
      playingTime: number | null
      confidence: string
    }> = []

    for (const title of detectedTexts.slice(0, 30)) { // Cap at 30 titles
      try {
        // Search local BGG cache first
        const { data: cached } = await supabase
          .from('bgg_games_cache')
          .select('bgg_id, name, year_published, thumbnail_url, min_players, max_players, playing_time')
          .ilike('name', `%${title}%`)
          .order('bgg_rank', { ascending: true, nullsFirst: false })
          .limit(1)

        if (cached && cached.length > 0) {
          const game = cached[0]
          gameMatches.push({
            detectedTitle: title,
            bggId: game.bgg_id,
            name: game.name,
            yearPublished: game.year_published,
            thumbnailUrl: game.thumbnail_url,
            minPlayers: game.min_players,
            maxPlayers: game.max_players,
            playingTime: game.playing_time,
            confidence: 'high',
          })
        } else {
          // No cache match — return as unmatched
          gameMatches.push({
            detectedTitle: title,
            bggId: null,
            name: null,
            yearPublished: null,
            thumbnailUrl: null,
            minPlayers: null,
            maxPlayers: null,
            playingTime: null,
            confidence: 'none',
          })
        }
      } catch {
        // Skip failed searches
      }
    }

    // Record the scan
    await supabase.from('shelf_scans').insert({
      user_id: user.id,
      games_detected: gameMatches.filter(g => g.bggId).length,
      games_added: 0,
    })

    return jsonResponse({
      games: gameMatches,
      rawText: detectedTexts.join('\n'),
      totalDetected: detectedTexts.length,
      matched: gameMatches.filter(g => g.bggId).length,
    })
  }

  return errorResponse('Method not allowed', 405)
})

// Extract potential board game titles from OCR text
function extractGameTitles(text: string): string[] {
  const lines = text.split('\n').map(l => l.trim()).filter(Boolean)
  const titles: string[] = []
  const seen = new Set<string>()

  for (const line of lines) {
    // Skip very short lines (likely noise) or very long lines (paragraphs)
    if (line.length < 3 || line.length > 60) continue

    // Skip lines that are just numbers, dates, or common non-title text
    if (/^\d+$/.test(line)) continue
    if (/^\d{1,2}[\/\-]\d{1,2}/.test(line)) continue
    if (/^(ages?\s*\d|players?|min|hrs?|edition|\d+\s*-\s*\d+\s*players?)$/i.test(line)) continue

    // Clean up the line
    const cleaned = line
      .replace(/[™®©]/g, '')
      .replace(/\s+/g, ' ')
      .trim()

    if (cleaned.length < 3) continue

    const key = cleaned.toLowerCase()
    if (!seen.has(key)) {
      seen.add(key)
      titles.push(cleaned)
    }
  }

  return titles
}
