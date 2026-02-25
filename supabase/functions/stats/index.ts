import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Cache stats for 5 minutes
let cachedStats: { gamesToday: number; gamesEver: number } | null = null
let cacheExpiry = 0

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  // Only allow GET requests
  if (req.method !== 'GET') {
    return errorResponse('Method not allowed', 405)
  }

  // Check cache
  const now = Date.now()
  if (cachedStats && now < cacheExpiry) {
    return jsonResponse(cachedStats)
  }

  try {
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Get today's date in YYYY-MM-DD format
    const today = new Date().toISOString().split('T')[0]

    // Query games today (published events happening today)
    const { count: gamesToday, error: todayError } = await supabase
      .from('events')
      .select('*', { count: 'exact', head: true })
      .eq('event_date', today)
      .eq('status', 'published')

    if (todayError) {
      console.error('Error fetching games today:', todayError)
      return errorResponse('Failed to fetch stats', 500)
    }

    // Query total games ever (published or completed)
    const { count: gamesEver, error: everError } = await supabase
      .from('events')
      .select('*', { count: 'exact', head: true })
      .in('status', ['published', 'completed'])

    if (everError) {
      console.error('Error fetching games ever:', everError)
      return errorResponse('Failed to fetch stats', 500)
    }

    // Update cache
    cachedStats = {
      gamesToday: gamesToday ?? 0,
      gamesEver: gamesEver ?? 0,
    }
    cacheExpiry = now + 5 * 60 * 1000 // 5 minutes

    return jsonResponse(cachedStats)
  } catch (err) {
    console.error('Stats error:', err)
    return errorResponse('Internal server error', 500)
  }
})
