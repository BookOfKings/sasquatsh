import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  const url = new URL(req.url)
  const path = url.pathname.split('/').pop()

  // GET /first-player/random - Get a random statement (authenticated)
  if (req.method === 'GET' && path === 'random') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    // Use Postgres random ordering to pick one
    const { data, error } = await supabase
      .from('first_player_statements')
      .select('id, statement')
      .limit(1)
      .order('id', { ascending: true })

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to fetch statement', 500)
    }

    if (!data || data.length === 0) {
      return errorResponse('No statements found', 404)
    }

    // Get total count and pick a random one
    const { count } = await supabase
      .from('first_player_statements')
      .select('id', { count: 'exact', head: true })

    const randomOffset = Math.floor(Math.random() * (count || 1))
    const { data: randomData, error: randomError } = await supabase
      .from('first_player_statements')
      .select('id, statement')
      .order('id', { ascending: true })
      .range(randomOffset, randomOffset)
      .limit(1)

    if (randomError || !randomData || randomData.length === 0) {
      console.error('Random fetch error:', randomError)
      return errorResponse('Failed to fetch random statement', 500)
    }

    return jsonResponse({ statement: randomData[0] })
  }

  // GET /first-player - Get all statements (authenticated)
  if (req.method === 'GET') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data, error } = await supabase
      .from('first_player_statements')
      .select('id, statement, created_at')
      .order('id', { ascending: true })

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to fetch statements', 500)
    }

    return jsonResponse({ statements: data })
  }

  // POST /first-player - Add a new statement (authenticated)
  if (req.method === 'POST') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)

    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    // Look up user
    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()

    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { statement } = body

    if (!statement || typeof statement !== 'string' || statement.trim().length === 0) {
      return errorResponse('Statement text is required', 400)
    }

    if (statement.length > 200) {
      return errorResponse('Statement must be 200 characters or less', 400)
    }

    const { data, error } = await supabase
      .from('first_player_statements')
      .insert({ statement: statement.trim(), created_by: user.id })
      .select('id, statement, created_at')
      .single()

    if (error) {
      console.error('Database error:', error)
      return errorResponse('Failed to add statement', 500)
    }

    return jsonResponse({ statement: data }, 201)
  }

  return errorResponse('Method not allowed', 405)
})
