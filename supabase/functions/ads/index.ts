import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Simple hash function for IP anonymization
async function hashIP(ip: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(ip + 'sasquatsh-salt')
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('').substring(0, 16)
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const placement = url.searchParams.get('placement') || 'general'
  const action = url.searchParams.get('action')
  const adId = url.searchParams.get('id')

  // Get client IP for tracking
  const clientIP = req.headers.get('x-forwarded-for')?.split(',')[0] ||
                   req.headers.get('cf-connecting-ip') ||
                   'unknown'
  const ipHash = await hashIP(clientIP)

  // Try to get user ID if authenticated
  let userId: string | null = null
  const token = getFirebaseToken(req)
  if (token) {
    const firebaseUser = await verifyFirebaseToken(token)
    if (firebaseUser) {
      const { data: user } = await supabase
        .from('users')
        .select('id')
        .eq('firebase_uid', firebaseUser.uid)
        .single()
      if (user) userId = user.id
    }
  }

  // GET - Fetch active ads for placement
  if (req.method === 'GET' && !action) {
    const today = new Date().toISOString().split('T')[0]

    const { data: ads, error } = await supabase
      .from('ads')
      .select('id, title, description, image_url, link_url, ad_type')
      .eq('is_active', true)
      .eq('placement', placement)
      .or(`start_date.is.null,start_date.lte.${today}`)
      .or(`end_date.is.null,end_date.gte.${today}`)
      .order('priority', { ascending: false })
      .limit(5)

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Pick one randomly (weighted by order/priority)
    if (ads.length === 0) {
      return jsonResponse(null)
    }

    // Weighted random selection (higher priority = more likely)
    const weights = ads.map((_, i) => ads.length - i)
    const totalWeight = weights.reduce((a, b) => a + b, 0)
    let random = Math.random() * totalWeight
    let selectedAd = ads[0]

    for (let i = 0; i < ads.length; i++) {
      random -= weights[i]
      if (random <= 0) {
        selectedAd = ads[i]
        break
      }
    }

    return jsonResponse({
      id: selectedAd.id,
      title: selectedAd.title,
      description: selectedAd.description,
      imageUrl: selectedAd.image_url,
      linkUrl: selectedAd.link_url,
      adType: selectedAd.ad_type,
    })
  }

  // POST - Track impression or click
  if (req.method === 'POST') {
    if (!adId) {
      return errorResponse('Ad ID required', 400)
    }

    const pageUrl = url.searchParams.get('page') || null

    if (action === 'impression') {
      // Check if we already tracked this impression recently (within 1 hour)
      const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000).toISOString()

      const { data: existing } = await supabase
        .from('ad_impressions')
        .select('id')
        .eq('ad_id', adId)
        .eq('ip_hash', ipHash)
        .gte('created_at', oneHourAgo)
        .limit(1)

      if (existing && existing.length > 0) {
        // Already tracked recently, skip
        return jsonResponse({ tracked: false, reason: 'duplicate' })
      }

      const { error } = await supabase
        .from('ad_impressions')
        .insert({
          ad_id: adId,
          user_id: userId,
          page_url: pageUrl,
          ip_hash: ipHash,
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ tracked: true })
    }

    if (action === 'click') {
      const { error } = await supabase
        .from('ad_clicks')
        .insert({
          ad_id: adId,
          user_id: userId,
          page_url: pageUrl,
          ip_hash: ipHash,
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ tracked: true })
    }

    return errorResponse('Invalid action', 400)
  }

  return errorResponse('Method not allowed', 405)
})
