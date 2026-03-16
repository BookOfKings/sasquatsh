import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Username validation regex: 3-30 chars, starts with letter, alphanumeric + underscores
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  if (req.method !== 'GET') {
    return errorResponse('Method not allowed', 405)
  }

  const url = new URL(req.url)
  const username = url.searchParams.get('username')

  if (!username) {
    return errorResponse('Username parameter required', 400)
  }

  // Validate format
  if (!USERNAME_REGEX.test(username)) {
    return jsonResponse({
      available: false,
      reason: 'Username must be 3-30 characters, start with a letter, and contain only letters, numbers, and underscores',
    })
  }

  // Check reserved usernames
  const reserved = ['admin', 'administrator', 'root', 'system', 'support', 'help', 'info', 'sasquatsh', 'moderator', 'mod']
  if (reserved.includes(username.toLowerCase())) {
    return jsonResponse({
      available: false,
      reason: 'This username is reserved',
    })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Check if username exists (case-insensitive)
  const { data, error } = await supabase
    .from('users')
    .select('id')
    .ilike('username', username)
    .limit(1)

  if (error) {
    console.error('Database error:', error)
    return errorResponse('Failed to check username', 500)
  }

  const available = !data || data.length === 0

  return jsonResponse({
    available,
    reason: available ? undefined : 'Username is already taken',
  })
})
