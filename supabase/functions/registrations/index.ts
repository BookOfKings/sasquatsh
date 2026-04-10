import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'
import { sendPushNotification } from '../_shared/push.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // Get Firebase token from custom header
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)
  const eventId = url.searchParams.get('eventId')

  // POST - Register for event
  if (req.method === 'POST') {
    const body = await req.json()
    const targetEventId = body.eventId || eventId

    if (!targetEventId) {
      return errorResponse('Event ID required', 400)
    }

    // Check if event exists and has capacity
    const { data: event, error: eventError } = await supabase
      .from('events')
      .select('id, max_players, host_user_id')
      .eq('id', targetEventId)
      .single()

    if (eventError || !event) {
      return errorResponse('Event not found', 404)
    }

    // Can't register for own event
    if (event.host_user_id === user.id) {
      return errorResponse('Cannot register for your own event', 400)
    }

    // Check if already registered
    const { data: existing } = await supabase
      .from('event_registrations')
      .select('id')
      .eq('event_id', targetEventId)
      .eq('user_id', user.id)
      .single()

    if (existing) {
      return errorResponse('Already registered for this event', 400)
    }

    // Count current registrations
    const { count } = await supabase
      .from('event_registrations')
      .select('id', { count: 'exact', head: true })
      .eq('event_id', targetEventId)
      .in('status', ['pending', 'confirmed'])

    const currentCount = count ?? 0
    const status = currentCount >= event.max_players ? 'waitlist' : 'confirmed'

    // Create registration
    const { error } = await supabase
      .from('event_registrations')
      .insert({
        event_id: targetEventId,
        user_id: user.id,
        status,
      })

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Award raffle entry for attending an event (only if confirmed, not waitlisted)
    if (status === 'confirmed') {
      try {
        await supabase.rpc('award_raffle_entry', {
          p_user_id: user.id,
          p_entry_type: 'attend_event',
          p_source_id: targetEventId,
        })
      } catch (err) {
        // Don't fail registration if raffle entry fails
        console.error('Failed to award raffle entry:', err)
      }
    }

    // Send push notification to event host
    if (status === 'confirmed') {
      try {
        // Get host's FCM token and registering user's name
        const { data: hostData } = await supabase
          .from('users')
          .select('fcm_token, display_name')
          .eq('id', event.host_user_id)
          .single()

        const { data: playerData } = await supabase
          .from('users')
          .select('display_name')
          .eq('id', user.id)
          .single()

        if (hostData?.fcm_token) {
          const playerName = playerData?.display_name || 'Someone'
          await sendPushNotification(
            hostData.fcm_token,
            'New Player Joined!',
            `${playerName} registered for your game`,
            { eventId: targetEventId, type: 'event_registration' }
          )
        }
      } catch (err) {
        // Never block registration for push notification failure
        console.error('Failed to send push notification:', err)
      }
    }

    return jsonResponse({
      message: status === 'waitlist'
        ? 'Added to waitlist'
        : 'Successfully registered for event',
    })
  }

  // DELETE - Cancel registration
  if (req.method === 'DELETE') {
    if (!eventId) {
      return errorResponse('Event ID required', 400)
    }

    const { error } = await supabase
      .from('event_registrations')
      .delete()
      .eq('event_id', eventId)
      .eq('user_id', user.id)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  return errorResponse('Method not allowed', 405)
})
