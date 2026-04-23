import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'
import { sendPushNotification } from '../_shared/push.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

type ContextType = 'event' | 'group' | 'planning'
type ReportReason = 'harassment' | 'spam' | 'hate_speech' | 'inappropriate' | 'threats' | 'other'
type ModerationAction = 'warning' | 'mute_1h' | 'mute_24h' | 'mute_7d' | 'ban_chat'

interface ChatMessage {
  id: string
  contextType: ContextType
  contextId: string
  userId: string
  content: string
  createdAt: string
  user: {
    id: string
    displayName: string | null
    avatarUrl: string | null
    isFoundingMember: boolean
    isAdmin: boolean
  } | null
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // All chat operations require authentication
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, blocked_user_ids, is_suspended, is_admin')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  if (user.is_suspended) {
    return errorResponse('Account suspended', 403)
  }

  const url = new URL(req.url)
  const adminMode = url.searchParams.get('admin')
  const adminAction = url.searchParams.get('action')

  // Admin endpoints - skip context validation for admin-only operations
  const isAdminEndpoint = adminMode === 'reports' || adminMode === 'moderation-history' ||
    adminAction === 'review-report' || adminAction === 'moderate'

  if (isAdminEndpoint) {
    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
    }
  }

  const contextType = url.searchParams.get('contextType') as ContextType | null
  const contextId = url.searchParams.get('contextId')

  // Validate context parameters (skip for admin endpoints)
  if (!isAdminEndpoint) {
    if (!contextType || !['event', 'group', 'planning'].includes(contextType)) {
      return errorResponse('Invalid or missing contextType (must be event, group, or planning)', 400)
    }

    if (!contextId) {
      return errorResponse('Missing contextId', 400)
    }

    // Verify user has access to this context
    const hasAccess = await verifyContextAccess(supabase, contextType, contextId, user.id)
    if (!hasAccess) {
      return errorResponse('Not authorized to access this chat', 403)
    }
  }

  const blockedUserIds: string[] = user.blocked_user_ids ?? []

  // GET - Fetch messages or admin reports
  if (req.method === 'GET') {
    // Admin: Fetch reports
    if (adminMode === 'reports') {

      const status = url.searchParams.get('status') // pending, reviewed, action_taken, dismissed
      const limit = Math.min(parseInt(url.searchParams.get('limit') || '50', 10), 100)

      let query = supabase
        .from('chat_reports')
        .select(`
          id, reason, details, status, admin_notes, created_at, reviewed_at,
          message:chat_messages!message_id(id, content, context_type, context_id, created_at, is_deleted,
            user:users!user_id(id, display_name, avatar_url)
          ),
          reporter:users!reporter_id(id, display_name, avatar_url),
          reviewer:users!reviewed_by(id, display_name)
        `)
        .order('created_at', { ascending: false })
        .limit(limit)

      if (status) {
        query = query.eq('status', status)
      }

      const { data, error } = await query

      if (error) {
        console.error('Error fetching reports:', error)
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ reports: data })
    }

    // Admin: Fetch moderation history for a user
    if (adminMode === 'moderation-history') {
      const targetUserId = url.searchParams.get('userId')
      if (!targetUserId) {
        return errorResponse('Missing userId', 400)
      }

      const { data, error } = await supabase
        .from('chat_moderation_actions')
        .select(`
          id, action, reason, expires_at, created_at,
          issuer:users!issued_by(id, display_name)
        `)
        .eq('user_id', targetUserId)
        .order('created_at', { ascending: false })
        .limit(20)

      if (error) {
        console.error('Error fetching moderation history:', error)
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ history: data })
    }

    const limit = Math.min(parseInt(url.searchParams.get('limit') || '50', 10), 100)
    const before = url.searchParams.get('before') // cursor for pagination (ISO timestamp)

    let query = supabase
      .from('chat_messages')
      .select(`
        id, context_type, context_id, user_id, content, created_at,
        user:users!user_id(id, display_name, avatar_url, is_founding_member, is_admin)
      `)
      .eq('context_type', contextType)
      .eq('context_id', contextId)
      .eq('is_deleted', false)
      .order('created_at', { ascending: false })
      .limit(limit)

    // Pagination cursor - get messages older than this timestamp
    if (before) {
      query = query.lt('created_at', before)
    }

    // Filter out messages from blocked users
    if (blockedUserIds.length > 0) {
      query = query.not('user_id', 'in', `(${blockedUserIds.join(',')})`)
    }

    const { data, error } = await query

    if (error) {
      console.error('Error fetching messages:', error)
      return errorResponse(error.message, 500)
    }

    // Transform and reverse to chronological order for display
    const messages = (data ?? []).map(transformMessage).reverse()
    return jsonResponse({ messages })
  }

  // POST - Send message or report
  if (req.method === 'POST') {
    const action = url.searchParams.get('action')

    // Handle report action
    if (action === 'report') {
      const messageId = url.searchParams.get('messageId')
      if (!messageId) {
        return errorResponse('Missing messageId', 400)
      }

      let body: { reason?: ReportReason; details?: string }
      try {
        body = await req.json()
      } catch {
        return errorResponse('Invalid JSON body', 400)
      }

      const validReasons: ReportReason[] = ['harassment', 'spam', 'hate_speech', 'inappropriate', 'threats', 'other']
      if (!body.reason || !validReasons.includes(body.reason)) {
        return errorResponse('Invalid or missing reason', 400)
      }

      // Verify message exists and is in this context
      const { data: message, error: msgError } = await supabase
        .from('chat_messages')
        .select('id, user_id')
        .eq('id', messageId)
        .eq('context_type', contextType)
        .eq('context_id', contextId)
        .eq('is_deleted', false)
        .single()

      if (msgError || !message) {
        return errorResponse('Message not found', 404)
      }

      // Cannot report own messages
      if (message.user_id === user.id) {
        return errorResponse('Cannot report your own message', 400)
      }

      // Check if already reported by this user
      const { data: existingReport } = await supabase
        .from('chat_reports')
        .select('id')
        .eq('message_id', messageId)
        .eq('reporter_id', user.id)
        .maybeSingle()

      if (existingReport) {
        return errorResponse('You have already reported this message', 400)
      }

      const { data: report, error: reportError } = await supabase
        .from('chat_reports')
        .insert({
          message_id: messageId,
          reporter_id: user.id,
          reason: body.reason,
          details: body.details?.trim() || null,
        })
        .select('id')
        .single()

      if (reportError) {
        console.error('Error creating report:', reportError)
        return errorResponse(reportError.message, 500)
      }

      return jsonResponse({ success: true, reportId: report.id })
    }

    // Regular message sending
    let body: { content?: string }
    try {
      body = await req.json()
    } catch {
      return errorResponse('Invalid JSON body', 400)
    }

    const content = body.content?.trim()

    if (!content || content.length === 0) {
      return errorResponse('Message content is required', 400)
    }

    if (content.length > 1000) {
      return errorResponse('Message too long (max 1000 characters)', 400)
    }

    // Check if user is muted
    const { data: muteCheck } = await supabase
      .from('chat_moderation_actions')
      .select('id, action, expires_at')
      .eq('user_id', user.id)
      .in('action', ['mute_1h', 'mute_24h', 'mute_7d', 'ban_chat'])
      .or('expires_at.is.null,expires_at.gt.now()')
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (muteCheck) {
      const isBanned = muteCheck.action === 'ban_chat'
      const expiresMsg = muteCheck.expires_at
        ? ` until ${new Date(muteCheck.expires_at).toLocaleString()}`
        : ''
      return errorResponse(
        isBanned
          ? 'You are banned from chat'
          : `You are muted from chat${expiresMsg}`,
        403
      )
    }

    const { data, error } = await supabase
      .from('chat_messages')
      .insert({
        context_type: contextType,
        context_id: contextId,
        user_id: user.id,
        content: content,
      })
      .select(`
        id, context_type, context_id, user_id, content, created_at,
        user:users!user_id(id, display_name, avatar_url, is_founding_member, is_admin)
      `)
      .single()

    if (error) {
      console.error('Error sending message:', error)
      return errorResponse(error.message, 500)
    }

    // Send push notifications to other participants (fire-and-forget)
    try {
      await sendChatPushNotifications(supabase, user.id, contextType, contextId, content)
    } catch (err) {
      console.error('Failed to send chat push notifications:', err)
    }

    return jsonResponse({ message: transformMessage(data) })
  }

  // DELETE - Soft delete own message
  if (req.method === 'DELETE') {
    const messageId = url.searchParams.get('messageId')
    if (!messageId) {
      return errorResponse('Missing messageId', 400)
    }

    // Verify ownership (or admin)
    const { data: existing, error: fetchError } = await supabase
      .from('chat_messages')
      .select('user_id')
      .eq('id', messageId)
      .eq('context_type', contextType)
      .eq('context_id', contextId)
      .single()

    if (fetchError || !existing) {
      return errorResponse('Message not found', 404)
    }

    if (existing.user_id !== user.id && !user.is_admin) {
      return errorResponse('Not authorized to delete this message', 403)
    }

    const { error } = await supabase
      .from('chat_messages')
      .update({ is_deleted: true, updated_at: new Date().toISOString() })
      .eq('id', messageId)

    if (error) {
      console.error('Error deleting message:', error)
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  // PATCH - Admin moderation actions
  if (req.method === 'PATCH') {
    if (!user.is_admin) {
      return errorResponse('Admin access required', 403)
    }

    const action = url.searchParams.get('action')

    // Update report status
    if (action === 'review-report') {
      const reportId = url.searchParams.get('reportId')
      if (!reportId) {
        return errorResponse('Missing reportId', 400)
      }

      let body: { status?: string; adminNotes?: string }
      try {
        body = await req.json()
      } catch {
        return errorResponse('Invalid JSON body', 400)
      }

      const validStatuses = ['reviewed', 'action_taken', 'dismissed']
      if (!body.status || !validStatuses.includes(body.status)) {
        return errorResponse('Invalid status', 400)
      }

      const { error } = await supabase
        .from('chat_reports')
        .update({
          status: body.status,
          reviewed_by: user.id,
          reviewed_at: new Date().toISOString(),
          admin_notes: body.adminNotes || null,
        })
        .eq('id', reportId)

      if (error) {
        console.error('Error updating report:', error)
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ success: true })
    }

    // Issue moderation action (warn/mute/ban)
    if (action === 'moderate') {
      let body: { userId?: string; action?: ModerationAction; reason?: string; reportId?: string }
      try {
        body = await req.json()
      } catch {
        return errorResponse('Invalid JSON body', 400)
      }

      if (!body.userId) {
        return errorResponse('Missing userId', 400)
      }

      const validActions: ModerationAction[] = ['warning', 'mute_1h', 'mute_24h', 'mute_7d', 'ban_chat']
      if (!body.action || !validActions.includes(body.action)) {
        return errorResponse('Invalid moderation action', 400)
      }

      if (!body.reason?.trim()) {
        return errorResponse('Reason is required', 400)
      }

      // Calculate expiration for temporary mutes
      let expiresAt: string | null = null
      const now = new Date()
      switch (body.action) {
        case 'mute_1h':
          expiresAt = new Date(now.getTime() + 60 * 60 * 1000).toISOString()
          break
        case 'mute_24h':
          expiresAt = new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString()
          break
        case 'mute_7d':
          expiresAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString()
          break
        // warning and ban_chat have no expiration
      }

      const { data: modAction, error } = await supabase
        .from('chat_moderation_actions')
        .insert({
          user_id: body.userId,
          action: body.action,
          reason: body.reason.trim(),
          report_id: body.reportId || null,
          issued_by: user.id,
          expires_at: expiresAt,
        })
        .select('id')
        .single()

      if (error) {
        console.error('Error creating moderation action:', error)
        return errorResponse(error.message, 500)
      }

      // If action came from a report, update report status
      if (body.reportId) {
        await supabase
          .from('chat_reports')
          .update({
            status: 'action_taken',
            reviewed_by: user.id,
            reviewed_at: new Date().toISOString(),
          })
          .eq('id', body.reportId)
      }

      return jsonResponse({ success: true, actionId: modAction.id })
    }

    return errorResponse('Invalid admin action', 400)
  }

  return errorResponse('Method not allowed', 405)
})

// Verify user has access to the chat context
async function verifyContextAccess(
  supabase: ReturnType<typeof createClient>,
  contextType: ContextType,
  contextId: string,
  userId: string
): Promise<boolean> {
  switch (contextType) {
    case 'event': {
      // Check if user is host and get host's subscription tier
      const { data: event } = await supabase
        .from('events')
        .select('host_user_id, host:users!host_user_id(subscription_tier, subscription_override_tier)')
        .eq('id', contextId)
        .single()

      if (!event) return false

      // Chat requires host to have Basic+ subscription (override takes precedence)
      const hostData = event.host as { subscription_tier: string; subscription_override_tier: string | null } | null
      const hostTier = hostData?.subscription_override_tier || hostData?.subscription_tier || 'free'
      const hasChatAccess = ['basic', 'pro', 'premium'].includes(hostTier)
      if (!hasChatAccess) return false

      // Host always has access if they have Basic+
      if (event.host_user_id === userId) return true

      // Check if user is registered
      const { data: registration } = await supabase
        .from('event_registrations')
        .select('id')
        .eq('event_id', contextId)
        .eq('user_id', userId)
        .in('status', ['pending', 'confirmed'])
        .maybeSingle()

      return !!registration
    }

    case 'group': {
      // Get group and owner's subscription tier
      const { data: group } = await supabase
        .from('groups')
        .select('created_by_user_id, owner:users!created_by_user_id(subscription_tier, subscription_override_tier)')
        .eq('id', contextId)
        .single()

      if (!group) return false

      // Chat requires owner to have Basic+ subscription
      const ownerData = group.owner as { subscription_tier: string; subscription_override_tier: string | null } | null
      const ownerTier = ownerData?.subscription_override_tier || ownerData?.subscription_tier || 'free'
      const hasChatAccess = ['basic', 'pro', 'premium'].includes(ownerTier)
      if (!hasChatAccess) return false

      // Check if user is a group member
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('id')
        .eq('group_id', contextId)
        .eq('user_id', userId)
        .maybeSingle()

      return !!membership
    }

    case 'planning': {
      // Get session and creator's subscription tier
      const { data: session } = await supabase
        .from('planning_sessions')
        .select('created_by_user_id, creator:users!created_by_user_id(subscription_tier, subscription_override_tier)')
        .eq('id', contextId)
        .single()

      if (!session) return false

      // Chat requires creator to have Basic+ subscription
      const creatorData = session.creator as { subscription_tier: string; subscription_override_tier: string | null } | null
      const creatorTier = creatorData?.subscription_override_tier || creatorData?.subscription_tier || 'free'
      const hasChatAccess = ['basic', 'pro', 'premium'].includes(creatorTier)
      if (!hasChatAccess) return false

      // Creator always has access
      if (session.created_by_user_id === userId) return true

      // Check if user is invitee
      const { data: invitee } = await supabase
        .from('planning_invitees')
        .select('id')
        .eq('session_id', contextId)
        .eq('user_id', userId)
        .maybeSingle()

      return !!invitee
    }

    default:
      return false
  }
}

async function sendChatPushNotifications(
  supabase: ReturnType<typeof createClient>,
  senderUserId: string,
  contextType: ContextType,
  contextId: string,
  messageContent: string
) {
  // Get sender's display name
  const { data: sender } = await supabase
    .from('users')
    .select('display_name')
    .eq('id', senderUserId)
    .single()

  const senderName = sender?.display_name || 'Someone'

  // Get participant user IDs based on context type
  let participantUserIds: string[] = []

  if (contextType === 'event') {
    // Event: host + registered users
    const { data: event } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', contextId)
      .single()

    const { data: regs } = await supabase
      .from('event_registrations')
      .select('user_id')
      .eq('event_id', contextId)
      .eq('status', 'confirmed')

    const ids = new Set<string>()
    if (event?.host_user_id) ids.add(event.host_user_id)
    if (regs) regs.forEach((r: { user_id: string }) => ids.add(r.user_id))
    participantUserIds = Array.from(ids)
  } else if (contextType === 'group') {
    // Group: all members
    const { data: members } = await supabase
      .from('group_memberships')
      .select('user_id')
      .eq('group_id', contextId)

    participantUserIds = members?.map((m: { user_id: string }) => m.user_id) || []
  } else if (contextType === 'planning') {
    // Planning: all invitees
    const { data: invitees } = await supabase
      .from('planning_invitees')
      .select('user_id')
      .eq('session_id', contextId)

    participantUserIds = invitees?.map((i: { user_id: string }) => i.user_id) || []
  }

  // Remove the sender
  const recipientIds = participantUserIds.filter(id => id !== senderUserId)
  if (recipientIds.length === 0) return

  // Get FCM tokens for all recipients (batch query)
  const { data: recipients } = await supabase
    .from('users')
    .select('fcm_token')
    .in('id', recipientIds)
    .not('fcm_token', 'is', null)

  if (!recipients || recipients.length === 0) return

  // Build notification
  const contextLabel = contextType === 'event' ? 'Event' : contextType === 'group' ? 'Group' : 'Planning'
  const title = `${senderName} in ${contextLabel} Chat`
  const body = messageContent.length > 100 ? messageContent.substring(0, 97) + '...' : messageContent
  const pushData = { contextType, contextId, type: 'chat_message' }

  // Send to all recipients in parallel
  const promises = recipients.map((r: { fcm_token: string }) =>
    sendPushNotification(r.fcm_token, title, body, pushData).catch(err => {
      console.error('Push failed for token:', err)
    })
  )
  await Promise.allSettled(promises)
}

function transformMessage(row: Record<string, unknown>): ChatMessage {
  const userData = row.user as Record<string, unknown> | null
  return {
    id: row.id as string,
    contextType: row.context_type as ContextType,
    contextId: row.context_id as string,
    userId: row.user_id as string,
    content: row.content as string,
    createdAt: row.created_at as string,
    user: userData
      ? {
          id: userData.id as string,
          displayName: userData.display_name as string | null,
          avatarUrl: userData.avatar_url as string | null,
          isFoundingMember: userData.is_founding_member as boolean,
          isAdmin: userData.is_admin as boolean,
        }
      : null,
  }
}
