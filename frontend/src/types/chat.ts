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

// Report types
export type ChatReportReason = 'harassment' | 'spam' | 'hate_speech' | 'inappropriate' | 'threats' | 'other'
export type ChatReportStatus = 'pending' | 'reviewed' | 'action_taken' | 'dismissed'
export type ChatModerationAction = 'warning' | 'mute_1h' | 'mute_24h' | 'mute_7d' | 'ban_chat'

export interface ChatReport {
  id: string
  reason: ChatReportReason
  details: string | null
  status: ChatReportStatus
  adminNotes: string | null
  createdAt: string
  reviewedAt: string | null
  message: {
    id: string
    content: string
    contextType: ChatContextType
    contextId: string
    createdAt: string
    isDeleted: boolean
    user: ChatUser | null
  } | null
  reporter: ChatUser | null
  reviewer: { id: string; displayName: string | null } | null
}

export interface ChatModerationHistoryItem {
  id: string
  action: ChatModerationAction
  reason: string
  expiresAt: string | null
  createdAt: string
  issuer: { id: string; displayName: string | null } | null
}

export const REPORT_REASON_LABELS: Record<ChatReportReason, string> = {
  harassment: 'Harassment',
  spam: 'Spam',
  hate_speech: 'Hate Speech',
  inappropriate: 'Inappropriate Content',
  threats: 'Threats',
  other: 'Other',
}

export const MODERATION_ACTION_LABELS: Record<ChatModerationAction, string> = {
  warning: 'Warning',
  mute_1h: 'Mute 1 Hour',
  mute_24h: 'Mute 24 Hours',
  mute_7d: 'Mute 7 Days',
  ban_chat: 'Ban from Chat',
}
