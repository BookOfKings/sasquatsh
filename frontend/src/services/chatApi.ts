import { supabase } from './supabase'
import type {
  ChatMessage,
  ChatContextType,
  ChatMessagesResponse,
  SendMessageResponse,
  ChatReportReason,
  ChatReport,
  ChatModerationHistoryItem,
  ChatModerationAction,
} from '@/types/chat'
import type { RealtimeChannel } from '@supabase/supabase-js'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper for authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${FUNCTIONS_URL}${path}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'X-Firebase-Token': token,
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    })
  } catch {
    throw new Error('Unable to connect to the server. Please check your internet connection and try again.')
  }

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// Fetch chat messages with pagination
export async function getChatMessages(
  token: string,
  contextType: ChatContextType,
  contextId: string,
  options?: { limit?: number; before?: string }
): Promise<ChatMessage[]> {
  const params = new URLSearchParams({
    contextType,
    contextId,
  })

  if (options?.limit) params.append('limit', String(options.limit))
  if (options?.before) params.append('before', options.before)

  const response = await authenticatedRequest<ChatMessagesResponse>(
    `/chat?${params.toString()}`,
    token
  )

  return response.messages
}

// Send a new message
export async function sendChatMessage(
  token: string,
  contextType: ChatContextType,
  contextId: string,
  content: string
): Promise<ChatMessage> {
  const params = new URLSearchParams({ contextType, contextId })

  const response = await authenticatedRequest<SendMessageResponse>(
    `/chat?${params.toString()}`,
    token,
    {
      method: 'POST',
      body: JSON.stringify({ content }),
    }
  )

  return response.message
}

// Delete a message (soft delete)
export async function deleteChatMessage(
  token: string,
  contextType: ChatContextType,
  contextId: string,
  messageId: string
): Promise<void> {
  const params = new URLSearchParams({ contextType, contextId, messageId })

  await authenticatedRequest<void>(
    `/chat?${params.toString()}`,
    token,
    { method: 'DELETE' }
  )
}

// Subscribe to real-time chat updates
export function subscribeToChatMessages(
  contextType: ChatContextType,
  contextId: string,
  onNewMessage: (message: ChatMessage) => void,
  onMessageDeleted?: (messageId: string) => void
): RealtimeChannel {
  const channel = supabase
    .channel(`chat:${contextType}:${contextId}`)
    .on(
      'postgres_changes',
      {
        event: 'INSERT',
        schema: 'public',
        table: 'chat_messages',
        filter: `context_id=eq.${contextId}`,
      },
      async (payload) => {
        // Only process messages for our context type
        if (payload.new.context_type !== contextType) return

        // Fetch user details for the new message
        const { data: userData } = await supabase
          .from('users')
          .select('id, display_name, avatar_url, is_founding_member, is_admin')
          .eq('id', payload.new.user_id)
          .single()

        const message: ChatMessage = {
          id: payload.new.id,
          contextType: payload.new.context_type,
          contextId: payload.new.context_id,
          userId: payload.new.user_id,
          content: payload.new.content,
          createdAt: payload.new.created_at,
          user: userData
            ? {
                id: userData.id,
                displayName: userData.display_name,
                avatarUrl: userData.avatar_url,
                isFoundingMember: userData.is_founding_member,
                isAdmin: userData.is_admin,
              }
            : null,
        }

        onNewMessage(message)
      }
    )
    .on(
      'postgres_changes',
      {
        event: 'UPDATE',
        schema: 'public',
        table: 'chat_messages',
        filter: `context_id=eq.${contextId}`,
      },
      (payload) => {
        // Only process for our context type
        if (payload.new.context_type !== contextType) return

        // Handle soft delete (is_deleted = true)
        if (payload.new.is_deleted && onMessageDeleted) {
          onMessageDeleted(payload.new.id)
        }
      }
    )
    .subscribe()

  return channel
}

// Unsubscribe from chat updates
export function unsubscribeFromChat(channel: RealtimeChannel): void {
  supabase.removeChannel(channel)
}

// Get chat stats (message count and last message time)
export async function getChatStats(
  contextType: ChatContextType,
  contextId: string
): Promise<{ count: number; lastMessageAt: string | null }> {
  const { count, error: countError } = await supabase
    .from('chat_messages')
    .select('*', { count: 'exact', head: true })
    .eq('context_type', contextType)
    .eq('context_id', contextId)
    .eq('is_deleted', false)

  const { data: lastMessage } = await supabase
    .from('chat_messages')
    .select('created_at')
    .eq('context_type', contextType)
    .eq('context_id', contextId)
    .eq('is_deleted', false)
    .order('created_at', { ascending: false })
    .limit(1)
    .maybeSingle()

  return {
    count: countError ? 0 : (count ?? 0),
    lastMessageAt: lastMessage?.created_at ?? null,
  }
}

// Report a message
export async function reportChatMessage(
  token: string,
  contextType: ChatContextType,
  contextId: string,
  messageId: string,
  reason: ChatReportReason,
  details?: string
): Promise<{ reportId: string }> {
  const params = new URLSearchParams({ contextType, contextId, messageId, action: 'report' })

  return authenticatedRequest<{ success: boolean; reportId: string }>(
    `/chat?${params.toString()}`,
    token,
    {
      method: 'POST',
      body: JSON.stringify({ reason, details }),
    }
  )
}

// Admin: Get chat reports
export async function getChatReports(
  token: string,
  options?: { status?: string; limit?: number }
): Promise<ChatReport[]> {
  // Use dummy context params (admin endpoints don't actually use them for reports)
  const params = new URLSearchParams({
    contextType: 'event',
    contextId: '00000000-0000-0000-0000-000000000000',
    admin: 'reports',
  })

  if (options?.status) params.append('status', options.status)
  if (options?.limit) params.append('limit', String(options.limit))

  const response = await authenticatedRequest<{ reports: Record<string, unknown>[] }>(
    `/chat?${params.toString()}`,
    token
  )

  return response.reports.map(transformReport)
}

// Admin: Get moderation history for a user
export async function getChatModerationHistory(
  token: string,
  userId: string
): Promise<ChatModerationHistoryItem[]> {
  const params = new URLSearchParams({
    contextType: 'event',
    contextId: '00000000-0000-0000-0000-000000000000',
    admin: 'moderation-history',
    userId,
  })

  const response = await authenticatedRequest<{ history: Record<string, unknown>[] }>(
    `/chat?${params.toString()}`,
    token
  )

  return response.history.map((item) => ({
    id: item.id as string,
    action: item.action as ChatModerationAction,
    reason: item.reason as string,
    expiresAt: item.expires_at as string | null,
    createdAt: item.created_at as string,
    issuer: item.issuer as { id: string; displayName: string | null } | null,
  }))
}

// Admin: Review a report
export async function reviewChatReport(
  token: string,
  reportId: string,
  status: 'reviewed' | 'action_taken' | 'dismissed',
  adminNotes?: string
): Promise<void> {
  const params = new URLSearchParams({
    contextType: 'event',
    contextId: '00000000-0000-0000-0000-000000000000',
    action: 'review-report',
    reportId,
  })

  await authenticatedRequest<{ success: boolean }>(
    `/chat?${params.toString()}`,
    token,
    {
      method: 'PATCH',
      body: JSON.stringify({ status, adminNotes }),
    }
  )
}

// Admin: Issue moderation action
export async function issueModerationAction(
  token: string,
  userId: string,
  action: ChatModerationAction,
  reason: string,
  reportId?: string
): Promise<{ actionId: string }> {
  const params = new URLSearchParams({
    contextType: 'event',
    contextId: '00000000-0000-0000-0000-000000000000',
    action: 'moderate',
  })

  return authenticatedRequest<{ success: boolean; actionId: string }>(
    `/chat?${params.toString()}`,
    token,
    {
      method: 'PATCH',
      body: JSON.stringify({ userId, action, reason, reportId }),
    }
  )
}

// Transform report from API response
function transformReport(row: Record<string, unknown>): ChatReport {
  const message = row.message as Record<string, unknown> | null
  const messageUser = message?.user as Record<string, unknown> | null
  const reporter = row.reporter as Record<string, unknown> | null
  const reviewer = row.reviewer as Record<string, unknown> | null

  return {
    id: row.id as string,
    reason: row.reason as ChatReportReason,
    details: row.details as string | null,
    status: row.status as 'pending' | 'reviewed' | 'action_taken' | 'dismissed',
    adminNotes: row.admin_notes as string | null,
    createdAt: row.created_at as string,
    reviewedAt: row.reviewed_at as string | null,
    message: message
      ? {
          id: message.id as string,
          content: message.content as string,
          contextType: message.context_type as ChatContextType,
          contextId: message.context_id as string,
          createdAt: message.created_at as string,
          isDeleted: message.is_deleted as boolean,
          user: messageUser
            ? {
                id: messageUser.id as string,
                displayName: messageUser.display_name as string | null,
                avatarUrl: messageUser.avatar_url as string | null,
              }
            : null,
        }
      : null,
    reporter: reporter
      ? {
          id: reporter.id as string,
          displayName: reporter.display_name as string | null,
          avatarUrl: reporter.avatar_url as string | null,
        }
      : null,
    reviewer: reviewer
      ? {
          id: reviewer.id as string,
          displayName: reviewer.display_name as string | null,
        }
      : null,
  }
}
