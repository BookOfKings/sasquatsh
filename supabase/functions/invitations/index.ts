import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'
import { sendEmail, invitationEmail } from '../_shared/email.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const APP_URL = Deno.env.get('APP_URL') || 'https://sasquatsh.web.app'

// Generate a random invite code
function generateInviteCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

// Transform database row to Invitation
function toInvitation(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    eventId: row.event_id as string,
    inviteCode: row.invite_code as string,
    invitedByUserId: row.invited_by_user_id as string,
    invitedEmail: row.invited_email as string | null,
    channel: row.channel as string | null,
    status: row.status as string,
    acceptedByUserId: row.accepted_by_user_id as string | null,
    createdAt: row.created_at as string,
    acceptedAt: row.accepted_at as string | null,
    expiresAt: row.expires_at as string | null,
    event: row.event ? {
      id: (row.event as Record<string, unknown>).id as string,
      title: (row.event as Record<string, unknown>).title as string,
      eventDate: (row.event as Record<string, unknown>).event_date as string,
      startTime: (row.event as Record<string, unknown>).start_time as string,
      city: (row.event as Record<string, unknown>).city as string | null,
      state: (row.event as Record<string, unknown>).state as string | null,
      maxPlayers: (row.event as Record<string, unknown>).max_players as number,
      host: (row.event as Record<string, unknown>).host ? {
        id: ((row.event as Record<string, unknown>).host as Record<string, unknown>).id as string,
        displayName: ((row.event as Record<string, unknown>).host as Record<string, unknown>).display_name as string | null,
      } : null,
    } : null,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const inviteCode = url.searchParams.get('code')
  const action = url.searchParams.get('action')

  // GET invitation by code (public, for accepting invites)
  if (req.method === 'GET' && inviteCode) {
    const { data, error } = await supabase
      .from('game_invitations')
      .select(`
        *,
        event:events(
          id, title, event_date, start_time, city, state, max_players,
          host:users!host_user_id(id, display_name)
        )
      `)
      .eq('invite_code', inviteCode)
      .single()

    if (error || !data) {
      return errorResponse('Invitation not found', 404)
    }

    // Check if expired
    if (data.expires_at && new Date(data.expires_at) < new Date()) {
      return errorResponse('Invitation has expired', 410)
    }

    if (data.status === 'accepted') {
      return errorResponse('Invitation has already been used', 410)
    }

    return jsonResponse(toInvitation(data))
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
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // POST with action=accept - Accept an invitation
  if (req.method === 'POST' && action === 'accept' && inviteCode) {
    // Get the invitation
    const { data: invitation, error: invError } = await supabase
      .from('game_invitations')
      .select('*, event:events(id, max_players)')
      .eq('invite_code', inviteCode)
      .single()

    if (invError || !invitation) {
      return errorResponse('Invitation not found', 404)
    }

    if (invitation.status === 'accepted') {
      return errorResponse('Invitation has already been used', 410)
    }

    if (invitation.expires_at && new Date(invitation.expires_at) < new Date()) {
      return errorResponse('Invitation has expired', 410)
    }

    const eventId = invitation.event_id

    // Check if already registered
    const { data: existingReg } = await supabase
      .from('event_registrations')
      .select('id')
      .eq('event_id', eventId)
      .eq('user_id', user.id)
      .single()

    if (existingReg) {
      return errorResponse('You are already registered for this event', 400)
    }

    // Check capacity
    const { count } = await supabase
      .from('event_registrations')
      .select('*', { count: 'exact', head: true })
      .eq('event_id', eventId)
      .eq('status', 'confirmed')

    const event = invitation.event as Record<string, unknown>
    if (count !== null && count >= (event.max_players as number)) {
      return errorResponse('Event is full', 400)
    }

    // Register user for the event
    const { error: regError } = await supabase
      .from('event_registrations')
      .insert({
        event_id: eventId,
        user_id: user.id,
        status: 'confirmed',
      })

    if (regError) {
      return errorResponse(regError.message, 500)
    }

    // Mark invitation as accepted
    await supabase
      .from('game_invitations')
      .update({
        status: 'accepted',
        accepted_by_user_id: user.id,
        accepted_at: new Date().toISOString(),
      })
      .eq('id', invitation.id)

    return jsonResponse({ message: 'Successfully joined the game!', eventId })
  }

  // POST with action=invite-group-member - Invite group members to a planned event
  if (req.method === 'POST' && action === 'invite-group-member') {
    const body = await req.json()
    const { eventId, userIds } = body

    if (!eventId || !userIds?.length) {
      return errorResponse('eventId and userIds required', 400)
    }

    // Get event and verify it's from planning
    const { data: event } = await supabase
      .from('events')
      .select('id, host_user_id, group_id, from_planning_session_id, max_players')
      .eq('id', eventId)
      .single()

    if (!event) {
      return errorResponse('Event not found', 404)
    }

    if (!event.from_planning_session_id || !event.group_id) {
      return errorResponse('This action is only for planned group events', 400)
    }

    // Check if user is host or group admin
    const isHost = event.host_user_id === user.id
    if (!isHost) {
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', event.group_id)
        .eq('user_id', user.id)
        .single()

      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Only host or group admins can invite members', 403)
      }
    }

    // Verify all userIds are group members
    const { data: validMembers } = await supabase
      .from('group_memberships')
      .select('user_id')
      .eq('group_id', event.group_id)
      .in('user_id', userIds)

    const validUserIds = new Set(validMembers?.map(m => m.user_id) || [])
    const invalidIds = userIds.filter((id: string) => !validUserIds.has(id))

    if (invalidIds.length > 0) {
      return errorResponse('Some users are not members of this group', 400)
    }

    // Get already registered users
    const { data: existingRegs } = await supabase
      .from('event_registrations')
      .select('user_id')
      .eq('event_id', eventId)

    const registeredIds = new Set(existingRegs?.map(r => r.user_id) || [])
    const newUserIds = userIds.filter((id: string) => !registeredIds.has(id))

    if (newUserIds.length === 0) {
      return jsonResponse({ message: 'All users are already registered', addedCount: 0 })
    }

    // Check capacity
    const currentCount = existingRegs?.length || 0
    const spotsLeft = event.max_players - currentCount
    if (spotsLeft <= 0) {
      return errorResponse('Event is full', 400)
    }

    // Only add up to available spots
    const usersToAdd = newUserIds.slice(0, spotsLeft)

    // Create registrations directly (auto-confirm since they're invited)
    const registrations = usersToAdd.map((userId: string) => ({
      event_id: eventId,
      user_id: userId,
      status: 'confirmed',
    }))

    const { error: regError } = await supabase
      .from('event_registrations')
      .insert(registrations)

    if (regError) {
      return errorResponse(regError.message, 500)
    }

    return jsonResponse({
      message: `Added ${usersToAdd.length} members to the event`,
      addedCount: usersToAdd.length,
    })
  }

  // POST - Create invitation
  if (req.method === 'POST') {
    const body = await req.json()
    const eventId = body.eventId

    if (!eventId) {
      return errorResponse('Event ID required', 400)
    }

    // Verify user is host or registered
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!event) {
      return errorResponse('Event not found', 404)
    }

    const isHost = event.host_user_id === user.id

    if (!isHost) {
      const { data: registration } = await supabase
        .from('event_registrations')
        .select('id')
        .eq('event_id', eventId)
        .eq('user_id', user.id)
        .single()

      if (!registration) {
        return errorResponse('Only host or registered users can create invitations', 403)
      }
    }

    // Generate unique invite code
    let inviteCode = generateInviteCode()
    let attempts = 0
    while (attempts < 5) {
      const { data: existing } = await supabase
        .from('game_invitations')
        .select('id')
        .eq('invite_code', inviteCode)
        .single()

      if (!existing) break
      inviteCode = generateInviteCode()
      attempts++
    }

    // Calculate expiry (default 7 days)
    const expiresAt = new Date()
    expiresAt.setDate(expiresAt.getDate() + (body.expiresInDays || 7))

    const { data, error } = await supabase
      .from('game_invitations')
      .insert({
        event_id: eventId,
        invite_code: inviteCode,
        invited_by_user_id: user.id,
        invited_email: body.email?.trim() || null,
        channel: body.channel || 'link',
        expires_at: expiresAt.toISOString(),
      })
      .select(`
        *,
        event:events(
          id, title, event_date, start_time, city, state, max_players,
          host:users!host_user_id(id, display_name)
        )
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Send email if an email address was provided
    if (body.email?.trim()) {
      const event = data.event as Record<string, unknown>
      const host = event?.host as Record<string, unknown>
      const inviteUrl = `${APP_URL}/invite/${inviteCode}`

      // Format date and time
      const eventDate = new Date(event.event_date as string + 'T00:00:00')
      const formattedDate = eventDate.toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric',
      })

      const startTime = event.start_time as string
      let formattedTime = ''
      if (startTime) {
        const [hours, minutes] = startTime.split(':')
        const hour = parseInt(hours)
        const ampm = hour >= 12 ? 'PM' : 'AM'
        const hour12 = hour % 12 || 12
        formattedTime = `${hour12}:${minutes} ${ampm}`
      }

      const location = [event.city, event.state].filter(Boolean).join(', ') || 'Location TBD'

      const emailContent = invitationEmail({
        eventTitle: event.title as string,
        hostName: (host?.display_name as string) || 'A friend',
        eventDate: formattedDate,
        eventTime: formattedTime || 'Time TBD',
        location,
        inviteUrl,
      })

      // Send async - don't block the response
      sendEmail({
        to: body.email.trim(),
        ...emailContent,
      }).then(result => {
        if (!result.success) {
          console.error('Failed to send invitation email:', result.error)
        }
      })
    }

    return jsonResponse(toInvitation(data), 201)
  }

  // GET - List invitations for an event (host only)
  if (req.method === 'GET') {
    const eventId = url.searchParams.get('eventId')

    if (!eventId) {
      return errorResponse('Event ID required', 400)
    }

    // Verify user is host
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!event || event.host_user_id !== user.id) {
      return errorResponse('Not authorized', 403)
    }

    const { data, error } = await supabase
      .from('game_invitations')
      .select(`
        *,
        event:events(
          id, title, event_date, start_time, city, state, max_players,
          host:users!host_user_id(id, display_name)
        )
      `)
      .eq('event_id', eventId)
      .order('created_at', { ascending: false })

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(data.map(toInvitation))
  }

  // DELETE - Revoke invitation
  if (req.method === 'DELETE') {
    const invitationId = url.searchParams.get('id')

    if (!invitationId) {
      return errorResponse('Invitation ID required', 400)
    }

    // Verify ownership
    const { data: invitation } = await supabase
      .from('game_invitations')
      .select('invited_by_user_id, event:events(host_user_id)')
      .eq('id', invitationId)
      .single()

    if (!invitation) {
      return errorResponse('Invitation not found', 404)
    }

    const isInviter = invitation.invited_by_user_id === user.id
    const isHost = (invitation.event as Record<string, unknown>)?.host_user_id === user.id

    if (!isInviter && !isHost) {
      return errorResponse('Not authorized', 403)
    }

    const { error } = await supabase
      .from('game_invitations')
      .delete()
      .eq('id', invitationId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  return errorResponse('Method not allowed', 405)
})
