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
    postalCode: row.postal_code as string | null,
    venue: row.venue as string | null,
    timezone: row.timezone as string | null,
    startDate: row.start_date as string | null,
    endDate: row.end_date as string | null,
    isPermanent: row.is_permanent as boolean,
    recurringDays: row.recurring_days as number[] | null,
    status: row.status as string,
    eventCount: (row.event_count as number) ?? 0,
    userCount: (row.user_count as number) ?? 0,
    createdByUserId: row.created_by_user_id as string | null,
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
    const includeAll = url.searchParams.get('all') === 'true'
    const hotOnly = url.searchParams.get('hot') === 'true'
    const statusFilter = url.searchParams.get('status')

    // Hot locations endpoint - returns popular active venues
    if (hotOnly) {
      const { data, error } = await supabase
        .from('hot_locations')
        .select('*')
        .limit(20) // Fetch more to allow for filtering

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Filter to only show venues that are currently active
      const today = new Date().toISOString().split('T')[0]
      const currentDayOfWeek = new Date().getDay() // 0 = Sunday, 6 = Saturday

      const activeVenues = data.filter((venue: Record<string, unknown>) => {
        // Permanent venues are always active
        if (venue.is_permanent) return true

        // Check recurring days - venue is active if today is one of the recurring days
        const recurringDays = venue.recurring_days as number[] | null
        if (recurringDays && recurringDays.length > 0) {
          return recurringDays.includes(currentDayOfWeek)
        }

        // For dated venues, check if today is within the date range
        const startDate = venue.start_date as string | null
        const endDate = venue.end_date as string | null

        if (startDate && endDate) {
          return startDate <= today && today <= endDate
        }

        // If no date constraints, show it
        return true
      })

      return jsonResponse(activeVenues.slice(0, 10).map(toEventLocation))
    }

    // Admin endpoint - all locations including expired/pending
    if (includeAll || statusFilter) {
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
        .select('*')
        .order('start_date', { ascending: false })

      if (statusFilter) {
        query = query.eq('status', statusFilter)
      }

      const { data, error } = await query

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(data.map(toEventLocation))
    }

    // Public: List active, approved locations
    // Includes permanent locations, recurring locations, and dated locations that are currently active
    const today = new Date().toISOString().split('T')[0]
    const currentDayOfWeek = new Date().getDay() // 0 = Sunday, 6 = Saturday

    const { data, error } = await supabase
      .from('event_locations')
      .select('*')
      .eq('status', 'approved')
      .order('is_permanent', { ascending: false })
      .order('start_date', { ascending: true, nullsFirst: false })

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Filter to only show venues that are currently active
    const activeVenues = data.filter((venue: Record<string, unknown>) => {
      // Permanent venues are always active
      if (venue.is_permanent) return true

      // Check recurring days - venue is active if today is one of the recurring days
      const recurringDays = venue.recurring_days as number[] | null
      if (recurringDays && recurringDays.length > 0) {
        return recurringDays.includes(currentDayOfWeek)
      }

      // For dated venues, check if today is within the date range
      const startDate = venue.start_date as string | null
      const endDate = venue.end_date as string | null

      if (startDate && endDate) {
        return startDate <= today && today <= endDate
      }

      // If only end_date is set, check it hasn't passed
      if (endDate) {
        return today <= endDate
      }

      // If no date constraints, show it
      return true
    })

    return jsonResponse(activeVenues.map(toEventLocation))
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

  // POST - Create new event location (any authenticated user, creates as pending)
  if (req.method === 'POST') {
    // Admin-only: merge action
    if (action === 'merge') {
      if (!user.is_admin) {
        return errorResponse('Admin access required', 403)
      }

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

      // Also update events using these locations
      for (const removeId of removeIds) {
        await supabase
          .from('events')
          .update({ event_location_id: keepId })
          .eq('event_location_id', removeId)
      }

      // Also update users using these locations
      for (const removeId of removeIds) {
        await supabase
          .from('users')
          .update({ active_event_location_id: keepId })
          .eq('active_event_location_id', removeId)
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

    // Any authenticated user can submit a venue (pending approval)
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

    const isPermanent = body.isPermanent === true
    const recurringDays = Array.isArray(body.recurringDays) ? body.recurringDays : null

    // Validate recurring days if provided
    if (recurringDays && recurringDays.length > 0) {
      const validDays = recurringDays.every((d: number) => Number.isInteger(d) && d >= 0 && d <= 6)
      if (!validDays) {
        return errorResponse('Recurring days must be integers from 0 (Sunday) to 6 (Saturday)', 400)
      }
    }

    // For non-permanent locations, dates are required
    if (!isPermanent && !recurringDays?.length) {
      if (!body.startDate) {
        return errorResponse('Start date is required for temporary locations', 400)
      }
      if (!body.endDate) {
        return errorResponse('End date is required for temporary locations', 400)
      }
      // Check end date is on or after start date (same day is valid)
      if (new Date(body.endDate) < new Date(body.startDate)) {
        return errorResponse('End date must be on or after start date', 400)
      }
    }

    const normalizedName = normalizeName(body.name)

    // Admins can create as approved, regular users create as pending
    const status = user.is_admin ? 'approved' : 'pending'

    const { data, error } = await supabase
      .from('event_locations')
      .insert({
        name: body.name.trim(),
        name_normalized: normalizedName,
        city: body.city.trim(),
        state: body.state.trim(),
        postal_code: body.postalCode?.trim() || null,
        venue: body.venue?.trim() || null,
        start_date: body.startDate || null,
        end_date: body.endDate || null,
        is_permanent: isPermanent,
        recurring_days: recurringDays,
        status,
        created_by_user_id: user.id,
      })
      .select('*')
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toEventLocation(data), 201)
  }

  // PUT - Update location (admin only for most actions)
  if (req.method === 'PUT') {
    if (!locationId) {
      return errorResponse('Location ID required', 400)
    }

    // Admin-only: Approve action
    if (action === 'approve') {
      if (!user.is_admin) {
        return errorResponse('Admin access required', 403)
      }

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

    // Admin-only: Reject action
    if (action === 'reject') {
      if (!user.is_admin) {
        return errorResponse('Admin access required', 403)
      }

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

    // Admin-only: General update
    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
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
    if (body.postalCode !== undefined) updates.postal_code = body.postalCode?.trim() || null
    if (body.venue !== undefined) updates.venue = body.venue?.trim() || null
    if (body.startDate !== undefined) updates.start_date = body.startDate || null
    if (body.endDate !== undefined) updates.end_date = body.endDate || null
    if (body.isPermanent !== undefined) updates.is_permanent = body.isPermanent
    if (body.recurringDays !== undefined) {
      // Validate recurring days
      if (body.recurringDays && body.recurringDays.length > 0) {
        const validDays = body.recurringDays.every((d: number) => Number.isInteger(d) && d >= 0 && d <= 6)
        if (!validDays) {
          return errorResponse('Recurring days must be integers from 0 (Sunday) to 6 (Saturday)', 400)
        }
      }
      updates.recurring_days = body.recurringDays || null
    }
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
    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
    }

    if (!locationId) {
      return errorResponse('Location ID required', 400)
    }

    // Clear the event_location_id from any player_requests using this location
    await supabase
      .from('player_requests')
      .update({ event_location_id: null })
      .eq('event_location_id', locationId)

    // Clear from events
    await supabase
      .from('events')
      .update({ event_location_id: null })
      .eq('event_location_id', locationId)

    // Clear from users
    await supabase
      .from('users')
      .update({ active_event_location_id: null })
      .eq('active_event_location_id', locationId)

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
