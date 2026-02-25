import type { UserSummary } from './events'

export interface PlayerRequest {
  id: string
  userId: string
  title: string
  description: string | null
  gamePreferences: string | null
  city: string | null
  state: string | null
  availableDays: string | null
  playerCountNeeded: number
  isActive: boolean
  createdAt: string
  expiresAt: string | null
  user: UserSummary | null
}

export interface CreatePlayerRequestInput {
  title: string
  description?: string
  gamePreferences?: string
  city?: string
  state?: string
  availableDays?: string
  playerCountNeeded?: number
  expiresInDays?: number
}

export interface UpdatePlayerRequestInput {
  title?: string
  description?: string | null
  gamePreferences?: string | null
  city?: string | null
  state?: string | null
  availableDays?: string | null
  playerCountNeeded?: number
  isActive?: boolean
}

export interface GameInvitation {
  id: string
  eventId: string
  inviteCode: string
  invitedByUserId: string
  invitedEmail: string | null
  channel: string | null
  status: string
  acceptedByUserId: string | null
  createdAt: string
  acceptedAt: string | null
  expiresAt: string | null
  event: {
    id: string
    title: string
    eventDate: string
    startTime: string
    city: string | null
    state: string | null
    maxPlayers: number
    host: {
      id: string
      displayName: string | null
    } | null
  } | null
}

export interface CreateInvitationInput {
  eventId: string
  email?: string
  channel?: 'link' | 'email' | 'facebook' | 'twitter' | 'instagram'
  expiresInDays?: number
}
