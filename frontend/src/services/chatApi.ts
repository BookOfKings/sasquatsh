import { supabase } from './supabase'
import type {
  ChatMessage,
  ChatContextType,
  ChatMessagesResponse,
  SendMessageResponse
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
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

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
