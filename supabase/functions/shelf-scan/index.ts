import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const geminiApiKey = Deno.env.get('GEMINI_API_KEY')!

const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${geminiApiKey}`

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
    let mimeType = 'image/jpeg'

    if (contentType.includes('multipart/form-data')) {
      const formData = await req.formData()
      const file = formData.get('image') as File
      if (!file) return errorResponse('No image file provided', 400)

      if (!file.type.startsWith('image/')) {
        return errorResponse('File must be an image', 400)
      }
      if (file.size > 10 * 1024 * 1024) {
        return errorResponse('Image must be under 10MB', 400)
      }

      mimeType = file.type
      const buffer = await file.arrayBuffer()
      imageBase64 = btoa(String.fromCharCode(...new Uint8Array(buffer)))
    } else {
      const body = await req.json()
      if (!body.image) return errorResponse('image field required (base64)', 400)
      imageBase64 = body.image.replace(/^data:image\/[^;]+;base64,/, '')
      if (body.mimeType) mimeType = body.mimeType
    }

    // Call Gemini Flash to identify board games
    let detectedTitles: string[]
    try {
      const geminiResponse = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [
              {
                text: 'Look at this image of a board game shelf or collection. List every board game title you can identify. Include games you can identify from their box art, logos, or spine text. Return ONLY the game names, one per line, with no numbering, bullets, descriptions, or other text. If you cannot identify any board games, respond with "NONE".'
              },
              {
                inlineData: {
                  mimeType,
                  data: imageBase64,
                },
              },
            ],
          }],
          generationConfig: {
            temperature: 0.1,
            maxOutputTokens: 2048,
          },
        }),
      })

      if (!geminiResponse.ok) {
        const errText = await geminiResponse.text()
        console.error('Gemini API error:', geminiResponse.status, errText)
        return errorResponse('Failed to analyze image', 502)
      }

      const geminiData = await geminiResponse.json()
      const responseText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || ''

      if (!responseText || responseText.trim() === 'NONE') {
        await supabase.from('shelf_scans').insert({
          user_id: user.id,
          games_detected: 0,
          games_added: 0,
        })
        return jsonResponse({ games: [], rawText: '', message: 'No board games detected in image' })
      }

      // Parse response — one game per line
      detectedTitles = responseText
        .split('\n')
        .map((l: string) => l.trim())
        .filter((l: string) => l.length >= 2 && l !== 'NONE')
        // Remove any numbering or bullets the model might add despite instructions
        .map((l: string) => l.replace(/^[\d]+[.\)]\s*/, '').replace(/^[-*•]\s*/, '').trim())
        .filter((l: string) => l.length >= 2)

    } catch (err) {
      console.error('Gemini fetch error:', err)
      return errorResponse('Failed to connect to image analysis service', 502)
    }

    // Search BGG for each detected title
    const gameMatches: Array<{
      detectedTitle: string
      bggId: number | null
      name: string | null
      yearPublished: number | null
      thumbnailUrl: string | null
      imageUrl: string | null
      minPlayers: number | null
      maxPlayers: number | null
      playingTime: number | null
      confidence: string
    }> = []

    for (const title of detectedTitles.slice(0, 50)) {
      try {
        // Search local BGG cache — try exact match first, then fuzzy
        const { data: exactMatch } = await supabase
          .from('bgg_games_cache')
          .select('bgg_id, name, year_published, thumbnail_url, image_url, min_players, max_players, playing_time')
          .ilike('name', title)
          .limit(1)

        if (exactMatch && exactMatch.length > 0) {
          const game = exactMatch[0]
          gameMatches.push({
            detectedTitle: title,
            bggId: game.bgg_id,
            name: game.name,
            yearPublished: game.year_published,
            thumbnailUrl: game.thumbnail_url,
            imageUrl: game.image_url,
            minPlayers: game.min_players,
            maxPlayers: game.max_players,
            playingTime: game.playing_time,
            confidence: 'high',
          })
          continue
        }

        // Fuzzy match
        const { data: fuzzyMatch } = await supabase
          .from('bgg_games_cache')
          .select('bgg_id, name, year_published, thumbnail_url, image_url, min_players, max_players, playing_time')
          .ilike('name', `%${title}%`)
          .order('bgg_rank', { ascending: true, nullsFirst: false })
          .limit(1)

        if (fuzzyMatch && fuzzyMatch.length > 0) {
          const game = fuzzyMatch[0]
          gameMatches.push({
            detectedTitle: title,
            bggId: game.bgg_id,
            name: game.name,
            yearPublished: game.year_published,
            thumbnailUrl: game.thumbnail_url,
            imageUrl: game.image_url,
            minPlayers: game.min_players,
            maxPlayers: game.max_players,
            playingTime: game.playing_time,
            confidence: 'medium',
          })
        } else {
          gameMatches.push({
            detectedTitle: title,
            bggId: null,
            name: null,
            yearPublished: null,
            thumbnailUrl: null,
            imageUrl: null,
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
      rawText: detectedTitles.join('\n'),
      totalDetected: detectedTitles.length,
      matched: gameMatches.filter(g => g.bggId).length,
    })
  }

  return errorResponse('Method not allowed', 405)
})
