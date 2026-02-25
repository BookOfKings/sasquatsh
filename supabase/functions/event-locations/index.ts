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
    createdByUserId: row.created_by_user_id as string | null,
    approvedByUserId: row.approved_by_user_id as string | null,
    approvedAt: row.approved_at as string | null,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    createdBy: row.created_by ? {
      id: (row.created_by as Record<string, unknown>).id as string,
      displayName: (row.created_by as Record<string, unknown>).display_name as string | null,
    } : null,
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
  const status = url.searchParams.get('status')

  // GET - List event locations (public for approved, admin for pending/all)
  if (req.method === 'GET' && !locationId) {
    // Check if admin is requesting pending locations
    if (status === 'pending' || status === 'all') {
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

      let query = supabase
        .from('event_locations')
        .select(`
          *,
          created_by:users!event_locations_created_by_user_id_fkey(id, display_name)
        `)
        .order('created_at', { ascending: false })

      if (status === 'pending') {
        query = query.eq('status', 'pending')
      }

      const { data, error } = await query

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(data.map(toEventLocation))
    }

    // Public: List approved, active (not expired) locations
    const today = new Date().toISOString().split('T')[0]
    const { data, error } = await supabase
      .from('event_locations')
      .select('*')
      .eq('status', 'approved')
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
      .select(`
        *,
        created_by:users!event_locations_created_by_user_id_fkey(id, display_name)
      `)
      .eq('id', locationId)
      .single()

    if (error) {
      return errorResponse('Location not found', 404)
    }

    return jsonResponse(toEventLocation(data))
  }

  // All other operations require authentication
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

  // POST - Create new event location
  if (req.method === 'POST') {
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

    // Check for duplicates (same normalized name + overlapping dates)
    const { data: duplicates } = await supabase
      .from('event_locations')
      .select('id, name, start_date, end_date')
      .eq('name_normalized', normalizedName)
      .neq('status', 'rejected')
      .lte('start_date', body.endDate)
      .gte('end_date', body.startDate)

    if (duplicates && duplicates.length > 0) {
      return errorResponse(
        `A similar event location "${duplicates[0].name}" already exists for overlapping dates`,
        400
      )
    }

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
        status: 'pending',
        created_by_user_id: user.id,
      })
      .select('*')
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventLocation(data), 201)
  }

  // PUT - Update/Approve/Reject location (admin only)
  if (req.method === 'PUT') {
    if (!locationId) {
      return errorResponse('Location ID required', 400)
    }

    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
    }

    // Approve action
    if (action === 'approve') {
      const { data, error } = await supabase
        .from('event_locations')
        .update({
          status: 'approved',
          approved_by_user_id: user.id,
          approved_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        })
        .eq('id', locationId)
        .select('*')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(toEventLocation(data))
    }

    // Reject action
    if (action === 'reject') {
      const { data, error } = await supabase
        .from('event_locations')
        .update({
          status: 'rejected',
          updated_at: new Date().toISOString(),
        })
        .eq('id', locationId)
        .select('*')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(toEventLocation(data))
    }

    // General update
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
    if (body.status !== undefined) updates.status = body.status

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

    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
    }

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
