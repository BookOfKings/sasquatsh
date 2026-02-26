import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  // All planning operations require authentication
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
  const sessionId = url.searchParams.get('id')
  const groupId = url.searchParams.get('groupId')
  const action = url.searchParams.get('action')
  const mine = url.searchParams.get('mine')

  // GET - List/fetch planning sessions
  if (req.method === 'GET') {
    // Get sessions user is invited to
    if (mine === 'true') {
      const { data, error } = await supabase
        .from('planning_invitees')
        .select(`
          session:planning_sessions(
            id, group_id, created_by_user_id, title, description,
            response_deadline, status, finalized_date, created_event_id, created_at,
            group:groups(id, name, slug),
            created_by:users!created_by_user_id(id, display_name, avatar_url)
          )
        `)
        .eq('user_id', user.id)

      if (error) return errorResponse(error.message, 500)

      const sessions = data
        .map(d => d.session)
        .filter((s): s is NonNullable<typeof s> => s !== null)
        .map(transformSessionSummary)

      return jsonResponse(sessions)
    }

    // Get sessions for a group
    if (groupId) {
      // Verify user is member of group
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', groupId)
        .eq('user_id', user.id)
        .single()

      if (!membership) {
        return errorResponse('Not a member of this group', 403)
      }

      const { data, error } = await supabase
        .from('planning_sessions')
        .select(`
          id, group_id, created_by_user_id, title, description,
          response_deadline, status, finalized_date, created_event_id, created_at,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, avatar_url),
          invitees:planning_invitees(count)
        `)
        .eq('group_id', groupId)
        .order('created_at', { ascending: false })

      if (error) return errorResponse(error.message, 500)
      return jsonResponse(data.map(transformSessionSummary))
    }

    // Get single session with full details
    if (sessionId) {
      const { data: session, error } = await supabase
        .from('planning_sessions')
        .select(`
          id, group_id, created_by_user_id, title, description,
          response_deadline, status, finalized_date, finalized_game_id, created_event_id, created_at,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, avatar_url)
        `)
        .eq('id', sessionId)
        .single()

      if (error || !session) {
        return errorResponse('Session not found', 404)
      }

      // Verify user is creator or invitee
      const { data: invitee } = await supabase
        .from('planning_invitees')
        .select('id')
        .eq('session_id', sessionId)
        .eq('user_id', user.id)
        .single()

      const isCreator = session.created_by_user_id === user.id
      if (!isCreator && !invitee) {
        return errorResponse('Not authorized to view this session', 403)
      }

      // Fetch invitees
      const { data: invitees } = await supabase
        .from('planning_invitees')
        .select(`
          id, user_id, has_responded, responded_at, cannot_attend_any,
          user:users(id, display_name, avatar_url)
        `)
        .eq('session_id', sessionId)

      // Fetch dates with votes
      const { data: dates } = await supabase
        .from('planning_dates')
        .select(`
          id, proposed_date, start_time,
          votes:planning_date_votes(
            user_id, is_available,
            user:users(display_name, avatar_url)
          )
        `)
        .eq('session_id', sessionId)
        .order('proposed_date', { ascending: true })

      // Fetch game suggestions with votes
      const { data: suggestions } = await supabase
        .from('planning_game_suggestions')
        .select(`
          id, suggested_by_user_id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time, created_at,
          suggested_by:users!suggested_by_user_id(display_name, avatar_url),
          votes:planning_game_votes(user_id)
        `)
        .eq('session_id', sessionId)
        .order('created_at', { ascending: true })

      // Fetch items to bring
      const { data: items } = await supabase
        .from('planning_session_items')
        .select(`
          id, item_name, item_category, quantity_needed, claimed_by_user_id, claimed_at, created_at,
          claimed_by:users!claimed_by_user_id(id, display_name, username, avatar_url)
        `)
        .eq('session_id', sessionId)
        .order('created_at', { ascending: true })

      return jsonResponse(transformSessionFull(session, invitees ?? [], dates ?? [], suggestions ?? [], items ?? [], user.id))
    }

    return errorResponse('groupId, id, or mine parameter required', 400)
  }

  // POST - Create session or perform actions
  if (req.method === 'POST') {
    // Create new planning session
    if (!sessionId && !action) {
      const body = await req.json()

      if (!body.groupId || !body.title || !body.responseDeadline || !body.inviteeUserIds?.length || !body.proposedDates?.length) {
        return errorResponse('groupId, title, responseDeadline, inviteeUserIds, and proposedDates required', 400)
      }

      if (body.proposedDates.length < 2) {
        return errorResponse('At least 2 proposed dates required', 400)
      }

      // Verify user is owner/admin of group
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', body.groupId)
        .eq('user_id', user.id)
        .single()

      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Must be group owner or admin to create planning sessions', 403)
      }

      // Verify all invitees are group members
      const { data: validMembers } = await supabase
        .from('group_memberships')
        .select('user_id')
        .eq('group_id', body.groupId)
        .in('user_id', body.inviteeUserIds)

      const validUserIds = new Set(validMembers?.map(m => m.user_id) ?? [])
      const invalidIds = body.inviteeUserIds.filter((id: string) => !validUserIds.has(id))
      if (invalidIds.length > 0) {
        return errorResponse('Some invitees are not members of this group', 400)
      }

      // Create session
      const { data: session, error: sessionError } = await supabase
        .from('planning_sessions')
        .insert({
          group_id: body.groupId,
          created_by_user_id: user.id,
          title: body.title,
          description: body.description || null,
          response_deadline: body.responseDeadline,
        })
        .select()
        .single()

      if (sessionError) return errorResponse(sessionError.message, 500)

      // Add invitees (always include the creator so they can participate too)
      const allInviteeIds = new Set<string>(body.inviteeUserIds)
      allInviteeIds.add(user.id) // Ensure creator is included

      const inviteeRows = Array.from(allInviteeIds).map((userId: string) => ({
        session_id: session.id,
        user_id: userId,
      }))

      await supabase.from('planning_invitees').insert(inviteeRows)

      // Add proposed dates
      const dateRows = body.proposedDates.map((d: { date: string; startTime?: string }) => ({
        session_id: session.id,
        proposed_date: d.date,
        start_time: d.startTime || null,
      }))

      await supabase.from('planning_dates').insert(dateRows)

      // Fetch full session to return
      const { data: fullSession } = await supabase
        .from('planning_sessions')
        .select(`
          id, group_id, created_by_user_id, title, description,
          response_deadline, status, finalized_date, created_event_id, created_at,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, avatar_url)
        `)
        .eq('id', session.id)
        .single()

      return jsonResponse(transformSessionSummary(fullSession!))
    }

    if (!sessionId) {
      return errorResponse('Session ID required for actions', 400)
    }

    // Verify session exists and user has access
    const { data: session } = await supabase
      .from('planning_sessions')
      .select('id, status, created_by_user_id, group_id')
      .eq('id', sessionId)
      .single()

    if (!session) {
      return errorResponse('Session not found', 404)
    }

    const isCreator = session.created_by_user_id === user.id

    // Verify user is invitee
    const { data: invitee } = await supabase
      .from('planning_invitees')
      .select('id, has_responded')
      .eq('session_id', sessionId)
      .eq('user_id', user.id)
      .single()

    if (!isCreator && !invitee) {
      return errorResponse('Not authorized for this session', 403)
    }

    // Submit availability response
    if (action === 'respond') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open for responses', 400)
      }

      if (!invitee) {
        return errorResponse('Only invitees can respond', 403)
      }

      const body = await req.json()

      // Update invitee record
      await supabase
        .from('planning_invitees')
        .update({
          has_responded: true,
          responded_at: new Date().toISOString(),
          cannot_attend_any: body.cannotAttendAny || false,
        })
        .eq('session_id', sessionId)
        .eq('user_id', user.id)

      // If not "cannot attend any", record date votes
      if (!body.cannotAttendAny && body.dateAvailability?.length) {
        // Delete existing votes
        const { data: dates } = await supabase
          .from('planning_dates')
          .select('id')
          .eq('session_id', sessionId)

        const dateIds = dates?.map(d => d.id) ?? []

        await supabase
          .from('planning_date_votes')
          .delete()
          .eq('user_id', user.id)
          .in('date_id', dateIds)

        // Insert new votes
        const voteRows = body.dateAvailability.map((v: { dateId: string; isAvailable: boolean }) => ({
          date_id: v.dateId,
          user_id: user.id,
          is_available: v.isAvailable,
        }))

        await supabase.from('planning_date_votes').insert(voteRows)
      }

      return jsonResponse({ message: 'Response recorded' })
    }

    // Suggest a game
    if (action === 'suggest-game') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      const body = await req.json()

      if (!body.gameName) {
        return errorResponse('gameName required', 400)
      }

      // Check for duplicate BGG ID
      if (body.bggId) {
        const { data: existing } = await supabase
          .from('planning_game_suggestions')
          .select('id')
          .eq('session_id', sessionId)
          .eq('bgg_id', body.bggId)
          .single()

        if (existing) {
          return errorResponse('This game has already been suggested', 400)
        }
      }

      const { data: suggestion, error } = await supabase
        .from('planning_game_suggestions')
        .insert({
          session_id: sessionId,
          suggested_by_user_id: user.id,
          bgg_id: body.bggId || null,
          game_name: body.gameName,
          thumbnail_url: body.thumbnailUrl || null,
          min_players: body.minPlayers || null,
          max_players: body.maxPlayers || null,
          playing_time: body.playingTime || null,
        })
        .select(`
          id, suggested_by_user_id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time, created_at,
          suggested_by:users!suggested_by_user_id(display_name, avatar_url)
        `)
        .single()

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({
        ...transformGameSuggestion(suggestion, [], user.id),
      })
    }

    // Vote for a game
    if (action === 'vote-game') {
      const suggestionId = url.searchParams.get('suggestionId')
      if (!suggestionId) {
        return errorResponse('suggestionId required', 400)
      }

      // Verify suggestion belongs to this session
      const { data: suggestion } = await supabase
        .from('planning_game_suggestions')
        .select('id, session_id')
        .eq('id', suggestionId)
        .single()

      if (!suggestion || suggestion.session_id !== sessionId) {
        return errorResponse('Invalid suggestion', 400)
      }

      // Add vote (upsert)
      const { error } = await supabase
        .from('planning_game_votes')
        .upsert({
          suggestion_id: suggestionId,
          user_id: user.id,
        }, { onConflict: 'suggestion_id,user_id' })

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({ message: 'Vote recorded' })
    }

    // Remove vote for a game
    if (action === 'unvote-game') {
      const suggestionId = url.searchParams.get('suggestionId')
      if (!suggestionId) {
        return errorResponse('suggestionId required', 400)
      }

      await supabase
        .from('planning_game_votes')
        .delete()
        .eq('suggestion_id', suggestionId)
        .eq('user_id', user.id)

      return jsonResponse({ message: 'Vote removed' })
    }

    // Remove a game suggestion
    if (action === 'remove-suggestion') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      const suggestionId = url.searchParams.get('suggestionId')
      if (!suggestionId) {
        return errorResponse('suggestionId required', 400)
      }

      // Get the suggestion
      const { data: suggestion } = await supabase
        .from('planning_game_suggestions')
        .select('id, session_id, suggested_by_user_id')
        .eq('id', suggestionId)
        .single()

      if (!suggestion || suggestion.session_id !== sessionId) {
        return errorResponse('Suggestion not found', 404)
      }

      // Only allow the person who suggested it, session creator, or site admin to remove
      if (suggestion.suggested_by_user_id !== user.id && session.created_by_user_id !== user.id && !user.is_admin) {
        return errorResponse('Only the suggester, session creator, or admin can remove this', 403)
      }

      // Delete votes first (foreign key constraint)
      await supabase
        .from('planning_game_votes')
        .delete()
        .eq('suggestion_id', suggestionId)

      // Delete the suggestion
      const { error } = await supabase
        .from('planning_game_suggestions')
        .delete()
        .eq('id', suggestionId)

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({ message: 'Suggestion removed' })
    }

    // ============ Item Management ============

    // Add item to bring
    if (action === 'add-item') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      // Only creator can add items
      if (!isCreator) {
        return errorResponse('Only the session creator can add items', 403)
      }

      const body = await req.json()

      if (!body.itemName?.trim()) {
        return errorResponse('itemName required', 400)
      }

      const validCategories = ['food', 'drinks', 'supplies', 'other']
      const category = validCategories.includes(body.itemCategory) ? body.itemCategory : 'other'

      const { data: item, error } = await supabase
        .from('planning_session_items')
        .insert({
          session_id: sessionId,
          item_name: body.itemName.trim(),
          item_category: category,
          quantity_needed: body.quantityNeeded || 1,
        })
        .select(`
          id, item_name, item_category, quantity_needed, claimed_by_user_id, claimed_at, created_at
        `)
        .single()

      if (error) return errorResponse(error.message, 500)

      return jsonResponse(transformItem(item, null))
    }

    // Claim an item
    if (action === 'claim-item') {
      const itemId = url.searchParams.get('itemId')
      if (!itemId) {
        return errorResponse('itemId required', 400)
      }

      // Verify item belongs to this session and is unclaimed
      const { data: item } = await supabase
        .from('planning_session_items')
        .select('id, session_id, claimed_by_user_id')
        .eq('id', itemId)
        .single()

      if (!item || item.session_id !== sessionId) {
        return errorResponse('Item not found', 404)
      }

      if (item.claimed_by_user_id) {
        return errorResponse('Item already claimed', 400)
      }

      const { error } = await supabase
        .from('planning_session_items')
        .update({
          claimed_by_user_id: user.id,
          claimed_at: new Date().toISOString(),
        })
        .eq('id', itemId)

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({ message: 'Item claimed' })
    }

    // Unclaim an item
    if (action === 'unclaim-item') {
      const itemId = url.searchParams.get('itemId')
      if (!itemId) {
        return errorResponse('itemId required', 400)
      }

      // Verify item belongs to this session and is claimed by this user
      const { data: item } = await supabase
        .from('planning_session_items')
        .select('id, session_id, claimed_by_user_id')
        .eq('id', itemId)
        .single()

      if (!item || item.session_id !== sessionId) {
        return errorResponse('Item not found', 404)
      }

      // Only the claimer or session creator can unclaim
      if (item.claimed_by_user_id !== user.id && !isCreator) {
        return errorResponse('Only the claimer or session creator can unclaim', 403)
      }

      const { error } = await supabase
        .from('planning_session_items')
        .update({
          claimed_by_user_id: null,
          claimed_at: null,
        })
        .eq('id', itemId)

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({ message: 'Item unclaimed' })
    }

    // Remove an item
    if (action === 'remove-item') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      // Only creator can remove items
      if (!isCreator) {
        return errorResponse('Only the session creator can remove items', 403)
      }

      const itemId = url.searchParams.get('itemId')
      if (!itemId) {
        return errorResponse('itemId required', 400)
      }

      // Verify item belongs to this session
      const { data: item } = await supabase
        .from('planning_session_items')
        .select('id, session_id')
        .eq('id', itemId)
        .single()

      if (!item || item.session_id !== sessionId) {
        return errorResponse('Item not found', 404)
      }

      const { error } = await supabase
        .from('planning_session_items')
        .delete()
        .eq('id', itemId)

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({ message: 'Item removed' })
    }

    return errorResponse('Invalid action', 400)
  }

  // PUT - Finalize or cancel session
  if (req.method === 'PUT') {
    if (!sessionId) {
      return errorResponse('Session ID required', 400)
    }

    const { data: session } = await supabase
      .from('planning_sessions')
      .select('id, status, created_by_user_id, group_id, title, description')
      .eq('id', sessionId)
      .single()

    if (!session) {
      return errorResponse('Session not found', 404)
    }

    if (session.created_by_user_id !== user.id) {
      return errorResponse('Only the creator can modify this session', 403)
    }

    // Cancel session
    if (action === 'cancel') {
      if (session.status !== 'open') {
        return errorResponse('Session is not open', 400)
      }

      await supabase
        .from('planning_sessions')
        .update({ status: 'cancelled', updated_at: new Date().toISOString() })
        .eq('id', sessionId)

      return jsonResponse({ message: 'Session cancelled' })
    }

    // Finalize session
    if (action === 'finalize') {
      if (session.status !== 'open') {
        return errorResponse('Session is not open', 400)
      }

      const body = await req.json()

      // Get selected date (or best date)
      let finalDate: { id: string; proposed_date: string; start_time: string | null } | null = null

      if (body.selectedDateId) {
        const { data } = await supabase
          .from('planning_dates')
          .select('id, proposed_date, start_time')
          .eq('id', body.selectedDateId)
          .eq('session_id', sessionId)
          .single()
        finalDate = data
      } else {
        // Calculate best date (most available votes)
        const { data: dates } = await supabase
          .from('planning_dates')
          .select(`
            id, proposed_date, start_time,
            votes:planning_date_votes(is_available)
          `)
          .eq('session_id', sessionId)

        if (dates?.length) {
          const datesWithCounts = dates.map(d => ({
            ...d,
            availableCount: (d.votes as { is_available: boolean }[])?.filter(v => v.is_available).length ?? 0,
          }))
          datesWithCounts.sort((a, b) => b.availableCount - a.availableCount)
          finalDate = datesWithCounts[0]
        }
      }

      if (!finalDate) {
        return errorResponse('No valid date found', 400)
      }

      // Get selected game (or top-voted game)
      let finalGame: { id: string; game_name: string; bgg_id: number | null } | null = null

      if (body.selectedGameId) {
        const { data } = await supabase
          .from('planning_game_suggestions')
          .select('id, game_name, bgg_id')
          .eq('id', body.selectedGameId)
          .eq('session_id', sessionId)
          .single()
        finalGame = data
      } else {
        // Get top-voted game
        const { data: suggestions } = await supabase
          .from('planning_game_suggestions')
          .select(`
            id, game_name, bgg_id,
            votes:planning_game_votes(id)
          `)
          .eq('session_id', sessionId)

        if (suggestions?.length) {
          const suggestionsWithCounts = suggestions.map(s => ({
            ...s,
            voteCount: (s.votes as { id: string }[])?.length ?? 0,
          }))
          suggestionsWithCounts.sort((a, b) => b.voteCount - a.voteCount)
          finalGame = suggestionsWithCounts[0]
        }
      }

      // Create draft event
      const { data: event, error: eventError } = await supabase
        .from('events')
        .insert({
          host_user_id: user.id,
          group_id: session.group_id,
          title: session.title,
          description: session.description,
          game_title: finalGame?.game_name || null,
          event_date: finalDate.proposed_date,
          start_time: finalDate.start_time || '19:00',
          duration_minutes: 180,
          status: 'draft',
          is_public: false,
        })
        .select('id')
        .single()

      if (eventError) return errorResponse(eventError.message, 500)

      // Get users who are available for the final date
      const { data: availableVotes } = await supabase
        .from('planning_date_votes')
        .select('user_id')
        .eq('date_id', finalDate.id)
        .eq('is_available', true)

      // Auto-register available users
      if (availableVotes?.length) {
        const registrations = availableVotes.map(v => ({
          event_id: event.id,
          user_id: v.user_id,
          status: 'confirmed',
        }))

        await supabase.from('event_registrations').insert(registrations)
      }

      // Copy items to bring from planning session to event
      const { data: planningItems } = await supabase
        .from('planning_session_items')
        .select('item_name, item_category, quantity_needed, claimed_by_user_id, claimed_at')
        .eq('session_id', sessionId)

      if (planningItems?.length) {
        const eventItems = planningItems.map(item => ({
          event_id: event.id,
          item_name: item.item_name,
          item_category: item.item_category,
          quantity_needed: item.quantity_needed,
          claimed_by_user_id: item.claimed_by_user_id,
          claimed_at: item.claimed_at,
        }))

        await supabase.from('event_items').insert(eventItems)
      }

      // Update session
      await supabase
        .from('planning_sessions')
        .update({
          status: 'finalized',
          finalized_date: finalDate.proposed_date,
          finalized_game_id: finalGame?.id || null,
          created_event_id: event.id,
          updated_at: new Date().toISOString(),
        })
        .eq('id', sessionId)

      return jsonResponse({ eventId: event.id, message: 'Session finalized and event created' })
    }

    return errorResponse('Invalid action', 400)
  }

  // DELETE - Delete session
  if (req.method === 'DELETE') {
    if (!sessionId) {
      return errorResponse('Session ID required', 400)
    }

    const { data: session } = await supabase
      .from('planning_sessions')
      .select('created_by_user_id')
      .eq('id', sessionId)
      .single()

    if (!session) {
      return errorResponse('Session not found', 404)
    }

    if (session.created_by_user_id !== user.id) {
      return errorResponse('Only the creator can delete this session', 403)
    }

    await supabase.from('planning_sessions').delete().eq('id', sessionId)

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})

// Transform functions
function transformSessionSummary(row: Record<string, unknown>) {
  return {
    id: row.id,
    groupId: row.group_id,
    createdByUserId: row.created_by_user_id,
    title: row.title,
    description: row.description,
    responseDeadline: row.response_deadline,
    status: row.status,
    finalizedDate: row.finalized_date,
    createdEventId: row.created_event_id,
    createdAt: row.created_at,
    inviteeCount: (row.invitees as { count: number }[])?.[0]?.count ?? 0,
    group: row.group ? {
      id: (row.group as Record<string, unknown>).id,
      name: (row.group as Record<string, unknown>).name,
      slug: (row.group as Record<string, unknown>).slug,
    } : null,
    createdBy: row.created_by ? {
      id: (row.created_by as Record<string, unknown>).id,
      displayName: (row.created_by as Record<string, unknown>).display_name,
      avatarUrl: (row.created_by as Record<string, unknown>).avatar_url,
    } : null,
  }
}

function transformSessionFull(
  session: Record<string, unknown>,
  invitees: Record<string, unknown>[],
  dates: Record<string, unknown>[],
  suggestions: Record<string, unknown>[],
  items: Record<string, unknown>[],
  currentUserId: string
) {
  return {
    ...transformSessionSummary(session),
    finalizedGameId: session.finalized_game_id,
    invitees: invitees.map(i => ({
      id: i.id,
      userId: i.user_id,
      hasResponded: i.has_responded,
      respondedAt: i.responded_at,
      cannotAttendAny: i.cannot_attend_any,
      user: i.user ? {
        id: (i.user as Record<string, unknown>).id,
        displayName: (i.user as Record<string, unknown>).display_name,
        avatarUrl: (i.user as Record<string, unknown>).avatar_url,
      } : null,
    })),
    dates: dates.map(d => {
      const votes = (d.votes as Record<string, unknown>[]) ?? []
      return {
        id: d.id,
        proposedDate: d.proposed_date,
        startTime: d.start_time,
        availableCount: votes.filter(v => v.is_available).length,
        votes: votes.map(v => ({
          userId: v.user_id,
          isAvailable: v.is_available,
          user: v.user ? {
            displayName: (v.user as Record<string, unknown>).display_name,
            avatarUrl: (v.user as Record<string, unknown>).avatar_url,
          } : null,
        })),
      }
    }),
    gameSuggestions: suggestions.map(s => transformGameSuggestion(s, (s.votes as Record<string, unknown>[]) ?? [], currentUserId)),
    items: items.map(i => transformItem(i, i.claimed_by as Record<string, unknown> | null)),
  }
}

function transformItem(
  row: Record<string, unknown>,
  claimedBy: Record<string, unknown> | null
) {
  return {
    id: row.id,
    itemName: row.item_name,
    itemCategory: row.item_category,
    quantityNeeded: row.quantity_needed,
    claimedByUserId: row.claimed_by_user_id,
    claimedAt: row.claimed_at,
    createdAt: row.created_at,
    claimedBy: claimedBy ? {
      id: claimedBy.id,
      displayName: claimedBy.display_name,
      username: claimedBy.username,
      avatarUrl: claimedBy.avatar_url,
    } : null,
  }
}

function transformGameSuggestion(
  row: Record<string, unknown>,
  votes: Record<string, unknown>[],
  currentUserId: string
) {
  return {
    id: row.id,
    suggestedByUserId: row.suggested_by_user_id,
    bggId: row.bgg_id,
    gameName: row.game_name,
    thumbnailUrl: row.thumbnail_url,
    minPlayers: row.min_players,
    maxPlayers: row.max_players,
    playingTime: row.playing_time,
    createdAt: row.created_at,
    voteCount: votes.length,
    hasVoted: votes.some(v => v.user_id === currentUserId),
    suggestedBy: row.suggested_by ? {
      displayName: (row.suggested_by as Record<string, unknown>).display_name,
      avatarUrl: (row.suggested_by as Record<string, unknown>).avatar_url,
    } : null,
  }
}
