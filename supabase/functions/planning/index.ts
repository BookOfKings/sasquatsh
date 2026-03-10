import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'
import { sendEmail, planningInviteEmail } from '../_shared/email.ts'

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
    .select('id, subscription_tier, subscription_override_tier')
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
          has_responded,
          cannot_attend_any,
          accepted_at,
          session:planning_sessions(
            id, group_id, created_by_user_id, title, description,
            response_deadline, status, finalized_date, created_event_id, created_at,
            group:groups(id, name, slug),
            created_by:users!created_by_user_id(id, display_name, username, avatar_url, is_founding_member, is_admin)
          )
        `)
        .eq('user_id', user.id)

      if (error) return errorResponse(error.message, 500)

      const sessions = data
        .filter(d => d.session !== null)
        .map(d => ({
          ...transformSessionSummary(d.session as Record<string, unknown>),
          hasResponded: d.has_responded ?? false,
          cannotAttendAny: d.cannot_attend_any ?? false,
          acceptedAt: d.accepted_at ?? null,
        }))

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
          max_participants,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, username, avatar_url, is_founding_member, is_admin),
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
          max_participants, max_games,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, username, avatar_url, is_founding_member, is_admin)
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
        return errorResponse('You need to be invited to view this planning session', 403)
      }

      // Fetch invitees
      const { data: invitees } = await supabase
        .from('planning_invitees')
        .select(`
          id, user_id, has_responded, responded_at, cannot_attend_any, has_slot, accepted_at,
          user:users(id, display_name, username, avatar_url, is_founding_member, is_admin)
        `)
        .eq('session_id', sessionId)

      // Fetch dates with votes
      const { data: dates } = await supabase
        .from('planning_dates')
        .select(`
          id, proposed_date, start_time,
          votes:planning_date_votes(
            user_id, is_available,
            user:users(display_name, username, avatar_url, is_founding_member, is_admin)
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
          suggested_by:users!suggested_by_user_id(display_name, avatar_url, is_founding_member, is_admin),
          votes:planning_game_votes(user_id)
        `)
        .eq('session_id', sessionId)
        .order('created_at', { ascending: true })

      // Fetch items to bring
      const { data: items } = await supabase
        .from('planning_session_items')
        .select(`
          id, item_name, item_category, quantity_needed, claimed_by_user_id, claimed_at, created_at,
          claimed_by:users!claimed_by_user_id(id, display_name, username, avatar_url, is_founding_member, is_admin)
        `)
        .eq('session_id', sessionId)
        .order('created_at', { ascending: true })

      // Fetch user's voted game IDs (for multi-vote checkbox state)
      const suggestionIds = (suggestions ?? []).map(s => s.id)
      let userVotedGameIds: string[] = []
      if (suggestionIds.length > 0) {
        const { data: userVotes } = await supabase
          .from('planning_game_votes')
          .select('suggestion_id')
          .eq('user_id', user.id)
          .in('suggestion_id', suggestionIds)

        userVotedGameIds = (userVotes ?? []).map(v => v.suggestion_id)
      }

      return jsonResponse({
        ...transformSessionFull(session, invitees ?? [], dates ?? [], suggestions ?? [], items ?? [], user.id),
        userVotedGameIds,
      })
    }

    return errorResponse('groupId, id, or mine parameter required', 400)
  }

  // POST - Create session or perform actions
  if (req.method === 'POST') {
    // Create new planning session
    if (!sessionId && !action) {
      const body = await req.json()

      // Check subscription tier - planning requires Basic+
      const effectiveTier = user.subscription_override_tier || user.subscription_tier || 'free'
      const planningTiers = ['basic', 'pro', 'premium']
      if (!planningTiers.includes(effectiveTier)) {
        return errorResponse(
          JSON.stringify({
            code: 'FEATURE_NOT_AVAILABLE',
            message: 'Game night planning requires a Basic plan or higher. Upgrade to unlock this feature.',
            feature: 'planning',
            tier: effectiveTier,
          }),
          403
        )
      }

      if (!body.groupId || !body.title || !body.responseDeadline || !body.inviteeUserIds?.length || !body.proposedDates?.length) {
        return errorResponse('groupId, title, responseDeadline, inviteeUserIds, and proposedDates required', 400)
      }

      if (body.proposedDates.length < 1) {
        return errorResponse('At least 1 proposed date required', 400)
      }

      // Validate maxParticipants if provided
      if (body.maxParticipants !== undefined && body.maxParticipants !== null) {
        const maxP = Number(body.maxParticipants)
        if (isNaN(maxP) || maxP < 2 || maxP > 100) {
          return errorResponse('maxParticipants must be between 2 and 100', 400)
        }
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

      // Calculate max_games based on host's tier
      const tierGameLimits: Record<string, number> = { free: 0, basic: 5, pro: 10, premium: 10 }
      const maxGames = tierGameLimits[effectiveTier] || 5

      // Create session
      const { data: session, error: sessionError } = await supabase
        .from('planning_sessions')
        .insert({
          group_id: body.groupId,
          created_by_user_id: user.id,
          title: body.title,
          description: body.description || null,
          response_deadline: body.responseDeadline,
          max_participants: body.maxParticipants || null,
          max_games: maxGames,
        })
        .select()
        .single()

      if (sessionError) return errorResponse(sessionError.message, 500)

      // Add invitees (always include the creator so they can participate too)
      const allInviteeIds = new Set<string>(body.inviteeUserIds)
      allInviteeIds.add(user.id) // Ensure creator is included

      // Creator always gets a slot (first participant) and is auto-accepted
      const now = new Date().toISOString()
      const inviteeRows = Array.from(allInviteeIds).map((userId: string) => ({
        session_id: session.id,
        user_id: userId,
        has_slot: userId === user.id, // Creator auto-gets a slot
        accepted_at: userId === user.id ? now : null, // Creator is auto-accepted
      }))

      await supabase.from('planning_invitees').insert(inviteeRows)

      // Add proposed dates
      const dateRows = body.proposedDates.map((d: { date: string; startTime?: string }) => ({
        session_id: session.id,
        proposed_date: d.date,
        start_time: d.startTime || null,
      }))

      await supabase.from('planning_dates').insert(dateRows)

      // Add initial game suggestions if provided
      if (body.initialGameSuggestions && body.initialGameSuggestions.length > 0) {
        const suggestionRows = body.initialGameSuggestions.map((g: {
          gameName: string
          bggId?: number
          thumbnailUrl?: string
          minPlayers?: number
          maxPlayers?: number
          playingTime?: number
        }) => ({
          session_id: session.id,
          suggested_by_user_id: user.id,
          bgg_id: g.bggId || null,
          game_name: g.gameName,
          thumbnail_url: g.thumbnailUrl || null,
          min_players: g.minPlayers || null,
          max_players: g.maxPlayers || null,
          playing_time: g.playingTime || null,
        }))

        await supabase.from('planning_game_suggestions').insert(suggestionRows)
      }

      // Send email invites if requested
      if (body.sendEmailInvites) {
        // Get group name and creator name
        const { data: groupData } = await supabase
          .from('groups')
          .select('name, slug')
          .eq('id', body.groupId)
          .single()

        const { data: creatorData } = await supabase
          .from('users')
          .select('display_name, username')
          .eq('id', user.id)
          .single()

        // Get invitee emails (exclude creator)
        const inviteeIdsWithoutCreator = body.inviteeUserIds.filter((id: string) => id !== user.id)
        if (inviteeIdsWithoutCreator.length > 0) {
          const { data: inviteeUsers } = await supabase
            .from('users')
            .select('id, email')
            .in('id', inviteeIdsWithoutCreator)

          if (inviteeUsers && inviteeUsers.length > 0) {
            // Format dates for email
            const formattedDates = body.proposedDates.map((d: { date: string; startTime?: string }) => {
              const dateObj = new Date(d.date + 'T12:00:00')
              const dateStr = dateObj.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
              return d.startTime ? `${dateStr} at ${d.startTime}` : dateStr
            })

            // Format deadline
            const deadlineDate = new Date(body.responseDeadline)
            const deadlineStr = deadlineDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })

            const hostName = creatorData?.display_name || creatorData?.username || 'Someone'
            const planningUrl = `https://sasquatsh.com/groups/${groupData?.slug}/planning/${session.id}`

            // Send emails in parallel
            const emailPromises = inviteeUsers.map(invitee => {
              const emailContent = planningInviteEmail({
                sessionTitle: body.title,
                groupName: groupData?.name || 'your group',
                hostName,
                proposedDates: formattedDates,
                responseDeadline: deadlineStr,
                planningUrl,
              })
              return sendEmail({
                to: invitee.email,
                subject: emailContent.subject,
                text: emailContent.text,
                html: emailContent.html,
              })
            })

            // Don't wait for all emails - fire and forget
            Promise.all(emailPromises).catch(err => {
              console.error('Failed to send some planning invite emails:', err)
            })
          }
        }
      }

      // Fetch full session to return
      const { data: fullSession } = await supabase
        .from('planning_sessions')
        .select(`
          id, group_id, created_by_user_id, title, description,
          response_deadline, status, finalized_date, created_event_id, created_at,
          max_participants,
          group:groups(id, name, slug),
          created_by:users!created_by_user_id(id, display_name, username, avatar_url, is_founding_member, is_admin)
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
      .select('id, has_responded, has_slot')
      .eq('session_id', sessionId)
      .eq('user_id', user.id)
      .single()

    if (!isCreator && !invitee) {
      return errorResponse('You need to be invited to this planning session', 403)
    }

    // Accept invitation (marks user as interested, but they still need to respond with availability)
    if (action === 'accept') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      if (!invitee) {
        return errorResponse('Only invitees can accept', 403)
      }

      // Check if already accepted
      const { data: currentInvitee } = await supabase
        .from('planning_invitees')
        .select('accepted_at')
        .eq('session_id', sessionId)
        .eq('user_id', user.id)
        .single()

      if (currentInvitee?.accepted_at) {
        return jsonResponse({ message: 'Already accepted' })
      }

      await supabase
        .from('planning_invitees')
        .update({ accepted_at: new Date().toISOString() })
        .eq('session_id', sessionId)
        .eq('user_id', user.id)

      return jsonResponse({ message: 'Invitation accepted' })
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

      // Check if user should get a slot (first-come-first-served)
      let grantSlot = invitee.has_slot // Keep existing slot if they have one

      // Fetch session's max_participants
      const { data: sessionData, error: sessionError } = await supabase
        .from('planning_sessions')
        .select('max_participants')
        .eq('id', sessionId)
        .single()

      if (sessionError) {
        return errorResponse('Failed to fetch session data', 500)
      }

      if (!invitee.has_slot && !body.cannotAttendAny) {
        if (sessionData?.max_participants) {
          // Optimistically grant slot, then verify
          grantSlot = true
        } else {
          // No limit - everyone gets a slot
          grantSlot = true
        }
      }

      // Get current invitee state to check accepted_at
      const { data: currentInvitee } = await supabase
        .from('planning_invitees')
        .select('accepted_at')
        .eq('session_id', sessionId)
        .eq('user_id', user.id)
        .single()

      // Update invitee record (also set accepted_at if not already set)
      await supabase
        .from('planning_invitees')
        .update({
          has_responded: true,
          responded_at: new Date().toISOString(),
          cannot_attend_any: body.cannotAttendAny || false,
          has_slot: grantSlot,
          accepted_at: currentInvitee?.accepted_at || new Date().toISOString(),
        })
        .eq('session_id', sessionId)
        .eq('user_id', user.id)

      // If we granted a slot and there's a limit, verify we didn't exceed it (race condition protection)
      if (grantSlot && !invitee.has_slot && sessionData?.max_participants) {
        const { count } = await supabase
          .from('planning_invitees')
          .select('id', { count: 'exact', head: true })
          .eq('session_id', sessionId)
          .eq('has_slot', true)

        if ((count ?? 0) > sessionData.max_participants) {
          // We exceeded the limit - revoke our slot (last one in loses)
          await supabase
            .from('planning_invitees')
            .update({ has_slot: false })
            .eq('session_id', sessionId)
            .eq('user_id', user.id)
          grantSlot = false
        }
      }

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

      return jsonResponse({ message: 'Response recorded', hasSlot: grantSlot })
    }

    // Suggest a game
    if (action === 'suggest-game') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      // Check if session has participant limit and user has a slot
      const { data: sessionData, error: sessionError } = await supabase
        .from('planning_sessions')
        .select('max_participants, max_games, created_by_user_id')
        .eq('id', sessionId)
        .single()

      if (sessionError) {
        return errorResponse('Failed to fetch session data', 500)
      }

      if (sessionData?.max_participants && !invitee?.has_slot) {
        return errorResponse('You must have a participation slot to suggest games', 403)
      }

      // Check tier-based game limit
      const maxGames = sessionData?.max_games || 5
      const { count: currentCount } = await supabase
        .from('planning_game_suggestions')
        .select('*', { count: 'exact', head: true })
        .eq('session_id', sessionId)

      if ((currentCount ?? 0) >= maxGames) {
        return errorResponse(`Maximum ${maxGames} games allowed for this session`, 400)
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
          suggested_by:users!suggested_by_user_id(display_name, avatar_url, is_founding_member, is_admin)
        `)
        .single()

      if (error) return errorResponse(error.message, 500)

      return jsonResponse({
        ...transformGameSuggestion(suggestion, [], user.id),
      })
    }

    // Vote for a game (toggle: adds vote if not exists, removes if exists)
    if (action === 'vote-game') {
      // Check if session has participant limit and user has a slot
      const { data: sessionData, error: sessionError } = await supabase
        .from('planning_sessions')
        .select('max_participants')
        .eq('id', sessionId)
        .single()

      if (sessionError) {
        return errorResponse('Failed to fetch session data', 500)
      }

      if (sessionData?.max_participants && !invitee?.has_slot) {
        return errorResponse('You must have a participation slot to vote on games', 403)
      }

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

      // Check if vote already exists (toggle behavior for multi-vote)
      const { data: existingVote } = await supabase
        .from('planning_game_votes')
        .select('id')
        .eq('suggestion_id', suggestionId)
        .eq('user_id', user.id)
        .single()

      if (existingVote) {
        // Remove vote (toggle off)
        const { error } = await supabase
          .from('planning_game_votes')
          .delete()
          .eq('id', existingVote.id)

        if (error) return errorResponse(error.message, 500)
        return jsonResponse({ message: 'Vote removed', voted: false })
      } else {
        // Add vote (toggle on)
        const { error } = await supabase
          .from('planning_game_votes')
          .insert({
            suggestion_id: suggestionId,
            user_id: user.id,
          })

        if (error) return errorResponse(error.message, 500)
        return jsonResponse({ message: 'Vote added', voted: true })
      }
    }

    // Remove vote for a game
    if (action === 'unvote-game') {
      // Check if session has participant limit and user has a slot
      const { data: sessionData, error: sessionError } = await supabase
        .from('planning_sessions')
        .select('max_participants')
        .eq('id', sessionId)
        .single()

      if (sessionError) {
        return errorResponse('Failed to fetch session data', 500)
      }

      if (sessionData?.max_participants && !invitee?.has_slot) {
        return errorResponse('You must have a participation slot to vote on games', 403)
      }

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

    // ============ Invitee Management ============

    // Add more invitees to an existing session
    if (action === 'add-invitees') {
      if (session.status !== 'open') {
        return errorResponse('Session is no longer open', 400)
      }

      // Only creator can add invitees
      if (!isCreator) {
        return errorResponse('Only the session creator can invite more people', 403)
      }

      const body = await req.json()

      if (!body.userIds || !Array.isArray(body.userIds) || body.userIds.length === 0) {
        return errorResponse('userIds array required', 400)
      }

      // Verify all new invitees are group members
      const { data: validMembers } = await supabase
        .from('group_memberships')
        .select('user_id')
        .eq('group_id', session.group_id)
        .in('user_id', body.userIds)

      const validUserIds = new Set(validMembers?.map(m => m.user_id) || [])
      const invalidIds = body.userIds.filter((id: string) => !validUserIds.has(id))

      if (invalidIds.length > 0) {
        return errorResponse('Some users are not members of this group', 400)
      }

      // Get existing invitees to avoid duplicates
      const { data: existingInvitees } = await supabase
        .from('planning_invitees')
        .select('user_id')
        .eq('session_id', sessionId)

      const existingUserIds = new Set(existingInvitees?.map(i => i.user_id) || [])
      const newUserIds = body.userIds.filter((id: string) => !existingUserIds.has(id))

      if (newUserIds.length === 0) {
        return jsonResponse({ message: 'All users are already invited', addedCount: 0 })
      }

      // Insert new invitees
      const inviteeRows = newUserIds.map((userId: string) => ({
        session_id: sessionId,
        user_id: userId,
        has_slot: false,
        accepted_at: null,
      }))

      const { error: insertError } = await supabase
        .from('planning_invitees')
        .insert(inviteeRows)

      if (insertError) {
        return errorResponse(insertError.message, 500)
      }

      // Optionally send email invites
      if (body.sendEmailInvites) {
        // Get group and session info
        const { data: groupData } = await supabase
          .from('groups')
          .select('name, slug')
          .eq('id', session.group_id)
          .single()

        const { data: sessionData } = await supabase
          .from('planning_sessions')
          .select('title, response_deadline')
          .eq('id', sessionId)
          .single()

        const { data: creatorData } = await supabase
          .from('users')
          .select('display_name, username')
          .eq('id', user.id)
          .single()

        // Get dates for the email
        const { data: dates } = await supabase
          .from('planning_dates')
          .select('proposed_date, start_time')
          .eq('session_id', sessionId)
          .order('proposed_date')

        // Get new invitee emails
        const { data: inviteeUsers } = await supabase
          .from('users')
          .select('id, email')
          .in('id', newUserIds)

        if (inviteeUsers && inviteeUsers.length > 0 && sessionData && groupData) {
          // Format dates for email
          const formattedDates = (dates || []).map((d: { proposed_date: string; start_time?: string }) => {
            const dateObj = new Date(d.proposed_date + 'T12:00:00')
            const dateStr = dateObj.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })
            return d.start_time ? `${dateStr} at ${d.start_time}` : dateStr
          })

          // Format deadline
          const deadlineDate = new Date(sessionData.response_deadline)
          const deadlineStr = deadlineDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })

          const hostName = creatorData?.display_name || creatorData?.username || 'Someone'
          const planningUrl = `https://sasquatsh.com/groups/${groupData?.slug}/planning/${sessionId}`

          // Send emails in parallel
          const emailPromises = inviteeUsers.map(inviteeUser => {
            const emailContent = planningInviteEmail({
              sessionTitle: sessionData.title,
              groupName: groupData?.name || 'your group',
              hostName,
              proposedDates: formattedDates,
              responseDeadline: deadlineStr,
              planningUrl,
            })
            return sendEmail({
              to: inviteeUser.email,
              subject: emailContent.subject,
              text: emailContent.text,
              html: emailContent.html,
            })
          })

          // Don't wait for all emails - fire and forget
          Promise.all(emailPromises).catch(err => {
            console.error('Failed to send some planning invite emails:', err)
          })
        }
      }

      return jsonResponse({ message: `Invited ${newUserIds.length} new people`, addedCount: newUserIds.length })
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

      // Get all game suggestions with their vote counts
      const { data: allSuggestions } = await supabase
        .from('planning_game_suggestions')
        .select(`
          id, game_name, bgg_id, thumbnail_url,
          votes:planning_game_votes(id)
        `)
        .eq('session_id', sessionId)

      // Calculate qualifying games (2+ votes) for multi-game events
      const suggestionsWithCounts = (allSuggestions ?? []).map(s => ({
        ...s,
        voteCount: (s.votes as { id: string }[])?.length ?? 0,
      }))

      // Get games with 2+ interested players (will "fire")
      const qualifyingGames = suggestionsWithCounts
        .filter(s => s.voteCount >= 2)
        .sort((a, b) => b.voteCount - a.voteCount)
        .map(s => ({
          bggId: s.bgg_id,
          name: s.game_name,
          image: s.thumbnail_url,
          interestedCount: s.voteCount,
        }))

      // Get primary game (top-voted or selected)
      let finalGame: { id: string; game_name: string; bgg_id: number | null } | null = null

      if (body.selectedGameId) {
        const { data } = await supabase
          .from('planning_game_suggestions')
          .select('id, game_name, bgg_id')
          .eq('id', body.selectedGameId)
          .eq('session_id', sessionId)
          .single()
        finalGame = data
      } else if (suggestionsWithCounts.length) {
        // Get top-voted game as primary
        suggestionsWithCounts.sort((a, b) => b.voteCount - a.voteCount)
        finalGame = suggestionsWithCounts[0]
      }

      // Create draft event with planned_games for multi-game support
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
          planned_games: qualifyingGames.length > 0 ? qualifyingGames : null,
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
    maxParticipants: row.max_participants ?? null,
    maxGames: row.max_games ?? 5,
    inviteeCount: (row.invitees as { count: number }[])?.[0]?.count ?? 0,
    group: row.group ? {
      id: (row.group as Record<string, unknown>).id,
      name: (row.group as Record<string, unknown>).name,
      slug: (row.group as Record<string, unknown>).slug,
    } : null,
    createdBy: row.created_by ? {
      id: (row.created_by as Record<string, unknown>).id,
      displayName: (row.created_by as Record<string, unknown>).display_name,
      username: (row.created_by as Record<string, unknown>).username,
      avatarUrl: (row.created_by as Record<string, unknown>).avatar_url,
      isFoundingMember: (row.created_by as Record<string, unknown>).is_founding_member,
      isAdmin: (row.created_by as Record<string, unknown>).is_admin,
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
      hasSlot: i.has_slot ?? false,
      acceptedAt: i.accepted_at ?? null,
      user: i.user ? {
        id: (i.user as Record<string, unknown>).id,
        displayName: (i.user as Record<string, unknown>).display_name,
        username: (i.user as Record<string, unknown>).username,
        avatarUrl: (i.user as Record<string, unknown>).avatar_url,
        isFoundingMember: (i.user as Record<string, unknown>).is_founding_member,
        isAdmin: (i.user as Record<string, unknown>).is_admin,
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
            username: (v.user as Record<string, unknown>).username,
            avatarUrl: (v.user as Record<string, unknown>).avatar_url,
            isFoundingMember: (v.user as Record<string, unknown>).is_founding_member,
            isAdmin: (v.user as Record<string, unknown>).is_admin,
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
      isFoundingMember: claimedBy.is_founding_member,
      isAdmin: claimedBy.is_admin,
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
      isFoundingMember: (row.suggested_by as Record<string, unknown>).is_founding_member,
      isAdmin: (row.suggested_by as Record<string, unknown>).is_admin,
    } : null,
  }
}
