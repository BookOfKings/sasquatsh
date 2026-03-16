// Chat message types

export type ChatContextType = 'event' | 'group' | 'planning'

export interface ChatUser {
  id: string
  displayName: string | null
  avatarUrl: string | null
  isFoundingMember?: boolean
  isAdmin?: boolean
}

export interface ChatMessage {
  id: string
  contextType: ChatContextType
  contextId: string
  userId: string
  content: string
  createdAt: string
  user: ChatUser | null
}

export interface SendMessageInput {
  content: string
}

export interface ChatMessagesResponse {
  messages: ChatMessage[]
}

export interface SendMessageResponse {
  message: ChatMessage
}
