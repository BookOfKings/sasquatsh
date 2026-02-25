import type { UserSummary } from './events'

// Event Locations (shared gaming convention/event locations)
export interface EventLocation {
  id: string
  name: string
  city: string
  state: string
  venue: string | null
  startDate: string
  endDate: string
  status: 'pending' | 'approved' | 'rejected'
  createdByUserId: string | null
  createdAt: string
  createdBy?: {
    id: string
    displayName: string | null
  } | null
}

export interface CreateEventLocationInput {
  name: string
  city: string
  state: string
  venue?: string
  startDate: string
  endDate: string
}

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
  // Event location fields
  eventLocationId: string | null
  hallArea: string | null
  tableNumber: string | null
  booth: string | null
  eventLocation: EventLocation | null
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
  // Event location fields
  eventLocationId?: string
  hallArea?: string
  tableNumber?: string
  booth?: string
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
  // Event location fields
  eventLocationId?: string | null
  hallArea?: string | null
  tableNumber?: string | null
  booth?: string | null
}

export interface PlayerRequestFilters {
  city?: string
  state?: string
  gameName?: string
  playerCount?: number
  eventLocationId?: string
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
