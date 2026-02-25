import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

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
  const itemId = url.searchParams.get('id')
  const action = url.searchParams.get('action')

  // POST - Add item to event
  if (req.method === 'POST') {
    const body = await req.json()
    const eventId = body.eventId

    if (!eventId) {
      return errorResponse('Event ID required', 400)
    }

    // Verify user is host of the event
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!event || event.host_user_id !== user.id) {
      return errorResponse('Only the host can add items', 403)
    }

    const { data, error } = await supabase
      .from('event_items')
      .insert({
        event_id: eventId,
        item_name: body.itemName,
        item_category: body.itemCategory ?? 'other',
        quantity_needed: body.quantityNeeded ?? 1,
      })
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse({
      id: data.id,
      itemName: data.item_name,
      itemCategory: data.item_category,
      quantityNeeded: data.quantity_needed,
      claimedByUserId: null,
      claimedByName: null,
      claimedAt: null,
    })
  }

  // PUT - Claim or unclaim item
  if (req.method === 'PUT') {
    if (!itemId) {
      return errorResponse('Item ID required', 400)
    }

    if (action === 'claim') {
      // Check if already claimed
      const { data: item } = await supabase
        .from('event_items')
        .select('claimed_by_user_id')
        .eq('id', itemId)
        .single()

      if (item?.claimed_by_user_id) {
        return errorResponse('Item already claimed', 400)
      }

      const { error } = await supabase
        .from('event_items')
        .update({
          claimed_by_user_id: user.id,
          claimed_at: new Date().toISOString(),
        })
        .eq('id', itemId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Item claimed successfully' })
    }

    if (action === 'unclaim') {
      // Verify user owns the claim
      const { data: item } = await supabase
        .from('event_items')
        .select('claimed_by_user_id')
        .eq('id', itemId)
        .single()

      if (item?.claimed_by_user_id !== user.id) {
        return errorResponse('You did not claim this item', 403)
      }

      const { error } = await supabase
        .from('event_items')
        .update({
          claimed_by_user_id: null,
          claimed_at: null,
        })
        .eq('id', itemId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return new Response(null, { status: 204, headers: getCorsHeaders() })
    }

    return errorResponse('Invalid action', 400)
  }

  // DELETE - Remove item (host only)
  if (req.method === 'DELETE') {
    if (!itemId) {
      return errorResponse('Item ID required', 400)
    }

    // Verify user is host of the event
    const { data: item } = await supabase
      .from('event_items')
      .select('event_id')
      .eq('id', itemId)
      .single()

    if (!item) {
      return errorResponse('Item not found', 404)
    }

    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', item.event_id)
      .single()

    if (!event || event.host_user_id !== user.id) {
      return errorResponse('Only the host can remove items', 403)
    }

    const { error } = await supabase
      .from('event_items')
      .delete()
      .eq('id', itemId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})
