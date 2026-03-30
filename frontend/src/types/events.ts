import type { PlannedGame } from './planning'
import type { EventTable, GameSession } from './sessions'
import type { MtgEventConfig } from './mtg'
import type { PokemonEventConfig } from './pokemon'
import type { YugiohEventConfig } from './yugioh'

export type GameSystem = 'board_game' | 'mtg' | 'pokemon_tcg' | 'yugioh'

export interface UserSummary {
  id: string
  displayName: string | null
  avatarUrl: string | null
  isFoundingMember?: boolean
  isAdmin?: boolean
  subscriptionTier?: 'free' | 'basic' | 'pro' | 'premium'
  subscriptionOverrideTier?: 'free' | 'basic' | 'pro' | 'premium'
}

export interface EventSummary {
  id: string
  title: string
  gameTitle: string | null
  gameCategory: string | null
  eventDate: string
  startTime: string
  timezone: string | null
  durationMinutes: number
  city: string | null
  state: string | null
  difficultyLevel: string | null
  maxPlayers: number
  hostIsPlaying: boolean
  confirmedCount: number
  isPublic: boolean
  isCharityEvent: boolean
  minAge: number | null
  status: string
  primaryGameThumbnail: string | null
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

export interface EventGameSummary {
  id: string
  bggId: number | null
  gameName: string
  thumbnailUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  playingTime: number | null
  isPrimary: boolean
  isAlternative: boolean
}

export interface VenueSummary {
  id: string
  name: string
  city: string
  state: string
  postalCode: string | null
}

export interface Event {
  id: string
  hostUserId: string
  title: string
  description: string | null
  gameTitle: string | null
  gameCategory: string | null
  gameSystem: GameSystem
  eventDate: string
  startTime: string
  timezone: string | null
  durationMinutes: number
  setupMinutes: number
  addressLine1: string | null
  city: string | null
  state: string | null
  postalCode: string | null
  locationDetails: string | null
  eventLocationId: string | null
  venueHall: string | null
  venueRoom: string | null
  venueTable: string | null
  difficultyLevel: string | null
  maxPlayers: number
  hostIsPlaying: boolean
  confirmedCount: number
  isPublic: boolean
  isCharityEvent: boolean
  isMultiTable: boolean
  minAge: number | null
  status: string
  host: UserSummary | null
  venue: VenueSummary | null
  registrations: EventRegistration[] | null
  items: EventItem[] | null
  games: EventGameSummary[] | null
  plannedGames: PlannedGame[] | null // Games from multi-game planning (2+ interested)
  groupId: string | null
  fromPlanningSessionId: string | null // Set if event was created from planning session
  createdAt: string
  // Multi-table session data
  tables: EventTable[] | null
  sessions: GameSession[] | null
  // MTG event configuration
  mtgConfig: MtgEventConfig | null
  // Pokemon TCG event configuration
  pokemonConfig: PokemonEventConfig | null
  // Yu-Gi-Oh! TCG event configuration
  yugiohConfig: YugiohEventConfig | null
}

export interface CreateEventInput {
  title: string
  description?: string
  gameTitle?: string
  gameCategory?: string
  gameSystem?: GameSystem
  eventDate: string
  startTime: string
  timezone?: string
  durationMinutes?: number
  setupMinutes?: number
  addressLine1?: string
  city?: string
  state?: string
  postalCode?: string
  locationDetails?: string
  eventLocationId?: string
  venueHall?: string
  venueRoom?: string
  venueTable?: string
  difficultyLevel?: string
  maxPlayers?: number
  hostIsPlaying?: boolean
  isPublic?: boolean
  isCharityEvent?: boolean
  minAge?: number
  status?: string
  groupId?: string  // Link to a group
  mtgConfig?: Partial<MtgEventConfig>  // MTG-specific configuration
  pokemonConfig?: Partial<PokemonEventConfig>  // Pokemon TCG-specific configuration
  yugiohConfig?: Partial<YugiohEventConfig>  // Yu-Gi-Oh! TCG-specific configuration
}

export interface UpdateEventInput {
  title: string
  description: string | null
  gameTitle: string | null
  gameCategory: string | null
  gameSystem?: GameSystem
  eventDate: string
  startTime: string
  timezone: string | null
  durationMinutes: number
  setupMinutes: number
  addressLine1: string | null
  city: string | null
  state: string | null
  postalCode: string | null
  locationDetails: string | null
  eventLocationId: string | null
  venueHall: string | null
  venueRoom: string | null
  venueTable: string | null
  difficultyLevel: string | null
  maxPlayers: number
  hostIsPlaying: boolean
  isPublic: boolean
  isCharityEvent: boolean
  minAge: number | null
  status: string
  plannedGames?: PlannedGame[] | null
  mtgConfig?: Partial<MtgEventConfig>
  pokemonConfig?: Partial<PokemonEventConfig>
  yugiohConfig?: Partial<YugiohEventConfig>
}

export interface CreateEventItemInput {
  itemName: string
  itemCategory?: string
  quantityNeeded?: number
  bringingItem?: boolean // Auto-claim when adding (for "I'm bringing this" flow)
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
  nearbyZip?: string
  radiusMiles?: number
  venueId?: string
}
