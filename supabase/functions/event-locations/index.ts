import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Transform database row to EventLocation
function toEventLocation(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    name: row.name as string,
    city: row.city as string,
    state: row.state as string,
    venue: row.venue as string | null,
    startDate: row.start_date as string,
    endDate: row.end_date as string,
    status: row.status as string,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  }
}

// Normalize name for duplicate detection
function normalizeName(name: string): string {
  return name.toLowerCase().trim().replace(/\s+/g, ' ')
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const locationId = url.searchParams.get('id')
  const action = url.searchParams.get('action')

  // GET - List event locations
  if (req.method === 'GET' && !locationId) {
    // Check if admin is requesting all locations (including expired)
    const includeAll = url.searchParams.get('all') === 'true'

    if (includeAll) {
      const token = getFirebaseToken(req)
      if (!token) {
        return errorResponse('Admin authentication required', 401)
      }

      const firebaseUser = await verifyFirebaseToken(token)
      if (!firebaseUser) {
        return errorResponse('Invalid Firebase token', 401)
      }

      const { data: user } = await supabase
        .from('users')
        .select('id, is_admin')
        .eq('firebase_uid', firebaseUser.uid)
        .single()

      if (!user?.is_admin) {
        return errorResponse('Admin access required', 403)
      }

      const { data, error } = await supabase
        .from('event_locations')
        .select('*')
        .order('start_date', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(data.map(toEventLocation))
    }

    // Public: List active (not expired) locations
    const today = new Date().toISOString().split('T')[0]
    const { data, error } = await supabase
      .from('event_locations')
      .select('*')
      .gte('end_date', today)
      .order('start_date', { ascending: true })

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toEventLocation))
  }

  // GET - Get single location by ID
  if (req.method === 'GET' && locationId) {
    const { data, error } = await supabase
      .from('event_locations')
      .select('*')
      .eq('id', locationId)
      .single()

    if (error) {
      return errorResponse('Location not found', 404)
    }

    return jsonResponse(toEventLocation(data))
  }

  // All other operations require admin authentication
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, is_admin')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  if (!user.is_admin) {
    return errorResponse('Admin access required', 403)
  }

  // POST - Create new event location (admin only)
  if (req.method === 'POST') {
    // Check for merge action
    if (action === 'merge') {
      const body = await req.json()
      const keepId = body.keepId as string
      const removeIds = body.removeIds as string[]

      if (!keepId || !removeIds || removeIds.length === 0) {
        return errorResponse('keepId and removeIds are required', 400)
      }

      // Update all player_requests to point to the kept location
      for (const removeId of removeIds) {
        await supabase
          .from('player_requests')
          .update({ event_location_id: keepId })
          .eq('event_location_id', removeId)
      }

      // Delete the duplicate locations
      const { error: deleteError } = await supabase
        .from('event_locations')
        .delete()
        .in('id', removeIds)

      if (deleteError) {
        return errorResponse(deleteError.message, 500)
      }

      return jsonResponse({ merged: removeIds.length, keptId: keepId })
    }

    const body = await req.json()

    if (!body.name?.trim()) {
      return errorResponse('Name is required', 400)
    }
    if (!body.city?.trim()) {
      return errorResponse('City is required', 400)
    }
    if (!body.state?.trim()) {
      return errorResponse('State is required', 400)
    }
    if (!body.startDate) {
      return errorResponse('Start date is required', 400)
    }
    if (!body.endDate) {
      return errorResponse('End date is required', 400)
    }

    // Check end date is after start date
    if (new Date(body.endDate) < new Date(body.startDate)) {
      return errorResponse('End date must be after start date', 400)
    }

    const normalizedName = normalizeName(body.name)

    const { data, error } = await supabase
      .from('event_locations')
      .insert({
        name: body.name.trim(),
        name_normalized: normalizedName,
        city: body.city.trim(),
        state: body.state.trim(),
        venue: body.venue?.trim() || null,
        start_date: body.startDate,
        end_date: body.endDate,
        status: 'approved',
        created_by_user_id: user.id,
      })
      .select('*')
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventLocation(data), 201)
  }

  // PUT - Update location (admin only)
  if (req.method === 'PUT') {
    if (!locationId) {
      return errorResponse('Location ID required', 400)
    }

    const body = await req.json()
    const updates: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    if (body.name !== undefined) {
      updates.name = body.name.trim()
      updates.name_normalized = normalizeName(body.name)
    }
    if (body.city !== undefined) updates.city = body.city.trim()
    if (body.state !== undefined) updates.state = body.state.trim()
    if (body.venue !== undefined) updates.venue = body.venue?.trim() || null
    if (body.startDate !== undefined) updates.start_date = body.startDate
    if (body.endDate !== undefined) updates.end_date = body.endDate

    const { data, error } = await supabase
      .from('event_locations')
      .update(updates)
      .eq('id', locationId)
      .select('*')
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventLocation(data))
  }

  // DELETE - Delete location (admin only)
  if (req.method === 'DELETE') {
    if (!locationId) {
      return errorResponse('Location ID required', 400)
    }

    // First, clear the event_location_id from any player_requests using this location
    await supabase
      .from('player_requests')
      .update({ event_location_id: null })
      .eq('event_location_id', locationId)

    const { error } = await supabase
      .from('event_locations')
      .delete()
      .eq('id', locationId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})
