import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

type ContextType = 'event' | 'group' | 'planning'

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
  const contextType = url.searchParams.get('contextType') as ContextType | null
  const contextId = url.searchParams.get('contextId')

  // Validate context parameters
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

  const blockedUserIds: string[] = user.blocked_user_ids ?? []

  // GET - Fetch messages (with pagination)
  if (req.method === 'GET') {
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

  // POST - Send message
  if (req.method === 'POST') {
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
      // Check if user is host
      const { data: event } = await supabase
        .from('events')
        .select('host_user_id')
        .eq('id', contextId)
        .single()

      if (event?.host_user_id === userId) return true

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
      // Check if user is creator
      const { data: session } = await supabase
        .from('planning_sessions')
        .select('created_by_user_id')
        .eq('id', contextId)
        .single()

      if (session?.created_by_user_id === userId) return true

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
