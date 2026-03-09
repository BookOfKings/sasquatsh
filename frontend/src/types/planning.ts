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
  maxParticipants: number | null
  maxGames: number // Tier-based limit: Basic=5, Pro=10
  inviteeCount?: number
  userVotedGameIds?: string[] // Game IDs the current user has voted for
  group?: {
    id: string
    name: string
    slug: string
  } | null
  createdBy?: {
    id: string
    displayName: string | null
    username: string | null
    avatarUrl: string | null
    isFoundingMember?: boolean
  } | null
  invitees?: PlanningInvitee[]
  dates?: PlanningDate[]
  gameSuggestions?: GameSuggestion[]
  items?: PlanningItem[]
}

// Planning session invitation with user's response status
export interface PlanningInvitation extends PlanningSession {
  hasResponded: boolean
  cannotAttendAny: boolean
  acceptedAt: string | null
}

export type ItemCategory = 'food' | 'drinks' | 'supplies' | 'other'

export interface PlanningItem {
  id: string
  itemName: string
  itemCategory: ItemCategory
  quantityNeeded: number
  claimedByUserId: string | null
  claimedAt: string | null
  createdAt: string
  claimedBy?: {
    id: string
    displayName: string | null
    username: string
    avatarUrl: string | null
    isFoundingMember?: boolean
  } | null
}

export interface AddPlanningItemInput {
  itemName: string
  itemCategory?: ItemCategory
  quantityNeeded?: number
}

export interface PlanningInvitee {
  id: string
  userId: string
  hasResponded: boolean
  respondedAt: string | null
  cannotAttendAny: boolean
  hasSlot: boolean
  acceptedAt: string | null
  user?: {
    id: string
    displayName: string | null
    username: string | null
    avatarUrl: string | null
    isFoundingMember?: boolean
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
    isFoundingMember?: boolean
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
    isFoundingMember?: boolean
  } | null
}

export interface CreatePlanningSessionInput {
  groupId: string
  title: string
  description?: string
  responseDeadline: string
  inviteeUserIds: string[]
  proposedDates: { date: string; startTime?: string }[]
  sendEmailInvites?: boolean
  initialGameSuggestions?: SuggestGameInput[]
  maxParticipants?: number
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

// Game that qualified from planning (2+ interested players)
export interface PlannedGame {
  bggId: number | null
  name: string
  image: string | null
  interestedCount: number
}
