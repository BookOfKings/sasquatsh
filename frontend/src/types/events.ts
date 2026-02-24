export interface UserSummary {
  id: string
  displayName: string | null
  avatarUrl: string | null
}

export interface EventSummary {
  id: string
  title: string
  gameTitle: string | null
  gameCategory: string | null
  eventDate: string
  startTime: string
  durationMinutes: number
  city: string | null
  state: string | null
  difficultyLevel: string | null
  maxPlayers: number
  confirmedCount: number
  isPublic: boolean
  isCharityEvent: boolean
  status: string
  host: UserSummary | null
}

export interface EventRegistration {
  id: string
  userId: string
  status: string
  user: UserSummary | null
  registeredAt: string
}

export interface EventItem {
  id: string
  itemName: string
  itemCategory: string
  quantityNeeded: number
  claimedByUserId: string | null
  claimedByName: string | null
  claimedAt: string | null
}

export interface Event {
  id: string
  hostUserId: string
  title: string
  description: string | null
  gameTitle: string | null
  gameCategory: string | null
  eventDate: string
  startTime: string
  durationMinutes: number
  setupMinutes: number
  addressLine1: string | null
  city: string | null
  state: string | null
  postalCode: string | null
  locationDetails: string | null
  difficultyLevel: string | null
  maxPlayers: number
  confirmedCount: number
  isPublic: boolean
  isCharityEvent: boolean
  status: string
  host: UserSummary | null
  registrations: EventRegistration[] | null
  items: EventItem[] | null
  createdAt: string
}

export interface CreateEventInput {
  title: string
  description?: string
  gameTitle?: string
  gameCategory?: string
  eventDate: string
  startTime: string
  durationMinutes?: number
  setupMinutes?: number
  addressLine1?: string
  city?: string
  state?: string
  postalCode?: string
  locationDetails?: string
  difficultyLevel?: string
  maxPlayers?: number
  isPublic?: boolean
  isCharityEvent?: boolean
  status?: string
}

export interface UpdateEventInput {
  title: string
  description: string | null
  gameTitle: string | null
  gameCategory: string | null
  eventDate: string
  startTime: string
  durationMinutes: number
  setupMinutes: number
  addressLine1: string | null
  city: string | null
  state: string | null
  postalCode: string | null
  locationDetails: string | null
  difficultyLevel: string | null
  maxPlayers: number
  isPublic: boolean
  isCharityEvent: boolean
  status: string
}

export interface CreateEventItemInput {
  itemName: string
  itemCategory?: string
  quantityNeeded?: number
}

export type DifficultyLevel = 'beginner' | 'intermediate' | 'advanced'
export type EventStatus = 'draft' | 'published' | 'cancelled' | 'completed'
export type ItemCategory = 'food' | 'drinks' | 'supplies' | 'other'
export type GameCategory =
  | 'strategy'
  | 'party'
  | 'cooperative'
  | 'deckbuilding'
  | 'workerplacement'
  | 'areacontrol'
  | 'dice'
  | 'trivia'
  | 'roleplaying'
  | 'miniatures'
  | 'card'
  | 'family'
  | 'abstract'
  | 'other'

export interface EventSearchFilter {
  city?: string
  state?: string
  search?: string
  gameCategory?: GameCategory
  difficulty?: DifficultyLevel
  dateFrom?: string
  dateTo?: string
}
