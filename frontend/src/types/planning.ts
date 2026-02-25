export interface PlanningSession {
  id: string
  groupId: string
  createdByUserId: string
  title: string
  description: string | null
  responseDeadline: string
  status: 'open' | 'finalized' | 'cancelled'
  finalizedDate: string | null
  finalizedGameId: string | null
  createdEventId: string | null
  createdAt: string
  inviteeCount?: number
  group?: {
    id: string
    name: string
    slug: string
  } | null
  createdBy?: {
    id: string
    displayName: string | null
    avatarUrl: string | null
  } | null
  invitees?: PlanningInvitee[]
  dates?: PlanningDate[]
  gameSuggestions?: GameSuggestion[]
}

export interface PlanningInvitee {
  id: string
  userId: string
  hasResponded: boolean
  respondedAt: string | null
  cannotAttendAny: boolean
  user?: {
    id: string
    displayName: string | null
    avatarUrl: string | null
  } | null
}

export interface PlanningDate {
  id: string
  proposedDate: string
  startTime: string | null
  availableCount?: number
  votes?: DateVote[]
}

export interface DateVote {
  userId: string
  isAvailable: boolean
  user?: {
    displayName: string | null
    avatarUrl: string | null
  } | null
}

export interface GameSuggestion {
  id: string
  suggestedByUserId: string
  bggId: number | null
  gameName: string
  thumbnailUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  playingTime: number | null
  createdAt: string
  voteCount: number
  hasVoted: boolean
  suggestedBy?: {
    displayName: string | null
    avatarUrl: string | null
  } | null
}

export interface CreatePlanningSessionInput {
  groupId: string
  title: string
  description?: string
  responseDeadline: string
  inviteeUserIds: string[]
  proposedDates: { date: string; startTime?: string }[]
}

export interface PlanningResponseInput {
  cannotAttendAny: boolean
  dateAvailability: { dateId: string; isAvailable: boolean }[]
}

export interface SuggestGameInput {
  gameName: string
  bggId?: number
  thumbnailUrl?: string
  minPlayers?: number
  maxPlayers?: number
  playingTime?: number
}
