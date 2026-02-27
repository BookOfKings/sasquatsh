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

// Player Request - Host needs players for their event (someone bailed)
export interface PlayerRequest {
  id: string
  userId: string
  eventId: string
  description: string | null
  playerCountNeeded: number
  status: 'open' | 'filled' | 'cancelled'
  isActive: boolean
  createdAt: string
  expiresAt: string
  event: {
    id: string
    title: string
    gameTitle: string | null
    eventDate: string
    startTime: string
    city: string | null
    state: string | null
    addressLine1: string | null
    locationDetails: string | null
  } | null
  host: {
    id: string
    displayName: string | null
    username: string | null
    avatarUrl: string | null
  } | null
}

export interface CreatePlayerRequestInput {
  eventId: string
  description?: string
  playerCountNeeded?: number
}

export interface UpdatePlayerRequestInput {
  description?: string | null
  playerCountNeeded?: number
}

export interface PlayerRequestFilters {
  eventId?: string
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
