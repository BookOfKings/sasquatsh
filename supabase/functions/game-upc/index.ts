import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const gameUpcApiKey = Deno.env.get('GAMEUPC_API_KEY') || 'test_test_test_test_test'

// Use test tier for now, switch to v1 with real API key
const GAMEUPC_BASE = gameUpcApiKey === 'test_test_test_test_test'
  ? 'https://api.gameupc.com/test/'
  : 'https://api.gameupc.com/v1/'

const GAMEUPC_HEADERS = {
  'x-api-key': gameUpcApiKey,
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const url = new URL(req.url)

  // All endpoints require auth
  const token = getFirebaseToken(req)
  if (!token) return errorResponse('Authentication required', 401)
  const decoded = await verifyFirebaseToken(token)
  if (!decoded) return errorResponse('Invalid token', 401)

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const { data: user } = await supabase
    .from('users')
    .select('id')
    .eq('firebase_uid', decoded.uid)
    .single()
  if (!user) return errorResponse('User not found', 404)

  // GET /game-upc?upc=XXXXX — Look up a game by UPC/barcode
  if (req.method === 'GET') {
    const upc = url.searchParams.get('upc')
    const search = url.searchParams.get('search')

    if (!upc) return errorResponse('upc parameter is required', 400)

    // Clean UPC - strip non-numeric
    const cleanUpc = upc.replace(/[^0-9]/g, '')
    if (cleanUpc.length < 8 || cleanUpc.length > 13) {
      return errorResponse('UPC must be 8-13 digits', 400)
    }

    try {
      let apiUrl = `${GAMEUPC_BASE}upc/${cleanUpc}?search_mode=quality`
      if (search) {
        apiUrl += `&search=${encodeURIComponent(search)}`
      }

      const response = await fetch(apiUrl, { headers: GAMEUPC_HEADERS })

      if (response.status === 429) {
        return errorResponse('Rate limited — try again later', 429)
      }

      if (!response.ok) {
        const text = await response.text()
        console.error('GameUPC API error:', response.status, text)
        return errorResponse('Failed to look up barcode', 502)
      }

      const data = await response.json()
      return jsonResponse(data)
    } catch (err) {
      console.error('GameUPC fetch error:', err)
      return errorResponse('Failed to connect to barcode service', 502)
    }
  }

  // POST /game-upc?upc=XXX&bggId=YYY — Vote/confirm a BGG match for a UPC
  if (req.method === 'POST') {
    const upc = url.searchParams.get('upc')
    const bggId = url.searchParams.get('bggId')
    const bggVersion = url.searchParams.get('bggVersion')

    if (!upc || !bggId) {
      return errorResponse('upc and bggId parameters are required', 400)
    }

    const cleanUpc = upc.replace(/[^0-9]/g, '')
    const userId = `sasquatsh_${user.id.substring(0, 8)}`

    try {
      let apiUrl = `${GAMEUPC_BASE}upc/${cleanUpc}/bgg_id/${bggId}`
      if (bggVersion) {
        apiUrl += `/version/${bggVersion}`
      }

      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: {
          ...GAMEUPC_HEADERS,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ user_id: userId }),
      })

      if (!response.ok) {
        const text = await response.text()
        console.error('GameUPC vote error:', response.status, text)
        return errorResponse('Failed to submit vote', 502)
      }

      const data = await response.json()
      return jsonResponse(data)
    } catch (err) {
      console.error('GameUPC vote fetch error:', err)
      return errorResponse('Failed to connect to barcode service', 502)
    }
  }

  // DELETE /game-upc?upc=XXX&bggId=YYY — Undo a vote
  if (req.method === 'DELETE') {
    const upc = url.searchParams.get('upc')
    const bggId = url.searchParams.get('bggId')
    const bggVersion = url.searchParams.get('bggVersion')

    if (!upc || !bggId) {
      return errorResponse('upc and bggId parameters are required', 400)
    }

    const cleanUpc = upc.replace(/[^0-9]/g, '')
    const userId = `sasquatsh_${user.id.substring(0, 8)}`

    try {
      let apiUrl = `${GAMEUPC_BASE}upc/${cleanUpc}/bgg_id/${bggId}`
      if (bggVersion) {
        apiUrl += `/version/${bggVersion}`
      }

      const response = await fetch(apiUrl, {
        method: 'DELETE',
        headers: {
          ...GAMEUPC_HEADERS,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ user_id: userId }),
      })

      if (!response.ok) {
        const text = await response.text()
        console.error('GameUPC undo error:', response.status, text)
        return errorResponse('Failed to undo vote', 502)
      }

      const data = await response.json()
      return jsonResponse(data)
    } catch (err) {
      console.error('GameUPC undo fetch error:', err)
      return errorResponse('Failed to connect to barcode service', 502)
    }
  }

  return errorResponse('Method not allowed', 405)
})
