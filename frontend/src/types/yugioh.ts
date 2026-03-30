/**
 * Yu-Gi-Oh! TCG Types and Constants
 * Defines formats, event types, and configuration interfaces for Yu-Gi-Oh! events
 */

// =============================================================================
// FORMATS
// =============================================================================

export interface YugiohFormat {
  id: string
  name: string
  description: string
  mainDeckMin: number
  mainDeckMax: number
  extraDeckMax: number
  sideDeckMax: number
  maxCopies: number
  startingLP: number
  startingHand: number
  isOfficial: boolean
}

export const YUGIOH_FORMATS: YugiohFormat[] = [
  {
    id: 'advanced',
    name: 'Advanced',
    description: 'Standard competitive format with Forbidden/Limited list',
    mainDeckMin: 40,
    mainDeckMax: 60,
    extraDeckMax: 15,
    sideDeckMax: 15,
    maxCopies: 3,
    startingLP: 8000,
    startingHand: 5,
    isOfficial: true,
  },
  {
    id: 'traditional',
    name: 'Traditional',
    description: 'All Forbidden cards become Limited instead',
    mainDeckMin: 40,
    mainDeckMax: 60,
    extraDeckMax: 15,
    sideDeckMax: 15,
    maxCopies: 3,
    startingLP: 8000,
    startingHand: 5,
    isOfficial: true,
  },
  {
    id: 'speed_duel',
    name: 'Speed Duel',
    description: 'Fast-paced format with smaller decks and Skill Cards',
    mainDeckMin: 20,
    mainDeckMax: 30,
    extraDeckMax: 5,
    sideDeckMax: 5,
    maxCopies: 3,
    startingLP: 4000,
    startingHand: 4,
    isOfficial: true,
  },
  {
    id: 'time_wizard',
    name: 'Time Wizard',
    description: 'Retro format using older card pools (Goat, Edison, etc.)',
    mainDeckMin: 40,
    mainDeckMax: 60,
    extraDeckMax: 15,
    sideDeckMax: 15,
    maxCopies: 3,
    startingLP: 8000,
    startingHand: 5,
    isOfficial: false,
  },
  {
    id: 'casual',
    name: 'Casual',
    description: 'Kitchen table play with house rules',
    mainDeckMin: 40,
    mainDeckMax: 60,
    extraDeckMax: 15,
    sideDeckMax: 15,
    maxCopies: 3,
    startingLP: 8000,
    startingHand: 5,
    isOfficial: false,
  },
]

// =============================================================================
// EVENT TYPES
// =============================================================================

export type YugiohEventType =
  | 'casual'
  | 'locals'
  | 'ots'
  | 'regional'
  | 'ycs'
  | 'nationals'
  | 'worlds'

export const YUGIOH_EVENT_TYPE_DESCRIPTIONS: Record<YugiohEventType, string> = {
  casual: 'Friendly games with no stakes',
  locals: 'Local store tournament',
  ots: 'Official Tournament Store event with OTS packs',
  regional: 'Regional Championship qualifier',
  ycs: 'Yu-Gi-Oh! Championship Series premier event',
  nationals: 'National Championship',
  worlds: 'World Championship',
}

export const YUGIOH_EVENT_TYPE_LABELS: Record<YugiohEventType, string> = {
  casual: 'Casual Play',
  locals: 'Locals',
  ots: 'OTS Tournament',
  regional: 'Regional Championship',
  ycs: 'YCS',
  nationals: 'Nationals',
  worlds: 'Worlds',
}

// =============================================================================
// TOURNAMENT STRUCTURE
// =============================================================================

export type YugiohTournamentStyle = 'swiss' | 'single_elimination' | 'double_elimination'

export const TOURNAMENT_STYLE_LABELS: Record<YugiohTournamentStyle, string> = {
  swiss: 'Swiss',
  single_elimination: 'Single Elimination',
  double_elimination: 'Double Elimination',
}

// =============================================================================
// HELPER CONSTANTS
// =============================================================================

/** Event types that show tournament settings */
export const TOURNAMENT_EVENT_TYPES: YugiohEventType[] = [
  'locals',
  'ots',
  'regional',
  'ycs',
  'nationals',
  'worlds',
]

/** Event types that are officially sanctioned */
export const OFFICIAL_EVENT_TYPES: YugiohEventType[] = [
  'ots',
  'regional',
  'ycs',
  'nationals',
  'worlds',
]

/** Default max players per event type */
export const DEFAULT_MAX_PLAYERS: Record<YugiohEventType, number> = {
  casual: 8,
  locals: 16,
  ots: 32,
  regional: 256,
  ycs: 1000,
  nationals: 500,
  worlds: 128,
}

/** Default round time per event type (minutes) */
export const DEFAULT_ROUND_TIME: Record<YugiohEventType, number> = {
  casual: 40,
  locals: 40,
  ots: 40,
  regional: 40,
  ycs: 40,
  nationals: 40,
  worlds: 40,
}

/** Default top cut per event type */
export const DEFAULT_TOP_CUT: Record<YugiohEventType, number | null> = {
  casual: null,
  locals: null,
  ots: 8,
  regional: 32,
  ycs: 64,
  nationals: 32,
  worlds: 16,
}

// =============================================================================
// EVENT CONFIG
// =============================================================================

export interface YugiohEventConfig {
  // Format
  formatId: string | null
  customFormatName: string | null

  // Event structure
  eventType: YugiohEventType
  tournamentStyle: YugiohTournamentStyle | null
  roundsCount: number | null
  roundTimeMinutes: number
  bestOf: 1 | 3
  topCut: number | null

  // Deck rules
  allowProxies: boolean
  proxyLimit: number | null
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowSideDeck: boolean
  enforceFormatLegality: boolean
  houseRulesNotes: string | null

  // Prizes
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string

  // Official play
  isOfficialEvent: boolean
  awardsOtsPoints: boolean

  // Settings
  allowSpectators: boolean
}

/** Input type for creating/updating events */
export interface YugiohEventConfigInput {
  formatId: string | null
  customFormatName: string | null
  eventType: YugiohEventType
  tournamentStyle: YugiohTournamentStyle | null
  roundsCount: number | null
  roundTimeMinutes: number
  bestOf: 1 | 3
  topCut: number | null
  allowProxies: boolean
  proxyLimit: number | null
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowSideDeck: boolean
  enforceFormatLegality: boolean
  houseRulesNotes: string | null
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  isOfficialEvent: boolean
  awardsOtsPoints: boolean
  allowSpectators: boolean
}

/** Full event creation input including base event fields */
export interface CreateYugiohEventInput {
  title: string
  description?: string
  eventDate: string
  startTime: string
  timezone: string
  durationMinutes: number
  setupMinutes?: number
  maxPlayers: number
  hostIsPlaying: boolean
  isPublic: boolean
  isCharityEvent: boolean
  groupId?: string
  // Location
  eventLocationId?: string
  addressLine1?: string
  city?: string
  state?: string
  postalCode?: string
  locationDetails?: string
  venueHall?: string
  venueRoom?: string
  venueTable?: string
  // Yu-Gi-Oh! config
  yugiohConfig: YugiohEventConfigInput
}

// =============================================================================
// DEFAULT CONFIG
// =============================================================================

export const DEFAULT_YUGIOH_CONFIG: YugiohEventConfigInput = {
  formatId: 'advanced',
  customFormatName: null,
  eventType: 'casual',
  tournamentStyle: null,
  roundsCount: null,
  roundTimeMinutes: 40,
  bestOf: 3,
  topCut: null,
  allowProxies: false,
  proxyLimit: null,
  requireDeckRegistration: false,
  deckSubmissionDeadline: null,
  allowSideDeck: true,
  enforceFormatLegality: true,
  houseRulesNotes: null,
  hasPrizes: false,
  prizeStructure: null,
  entryFee: null,
  entryFeeCurrency: 'USD',
  isOfficialEvent: false,
  awardsOtsPoints: false,
  allowSpectators: true,
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Get format by ID
 */
export function getYugiohFormat(formatId: string | null): YugiohFormat | null {
  if (!formatId) return null
  return YUGIOH_FORMATS.find((f) => f.id === formatId) || null
}

/**
 * Check if event type is a tournament
 */
export function isTournamentEventType(eventType: YugiohEventType): boolean {
  return TOURNAMENT_EVENT_TYPES.includes(eventType)
}

/**
 * Check if event type is officially sanctioned
 */
export function isOfficialEventType(eventType: YugiohEventType): boolean {
  return OFFICIAL_EVENT_TYPES.includes(eventType)
}

/**
 * Get deck size display string for a format
 */
export function getDeckSizeDisplay(format: YugiohFormat): string {
  if (format.mainDeckMin === format.mainDeckMax) {
    return `${format.mainDeckMin} cards`
  }
  return `${format.mainDeckMin}-${format.mainDeckMax} cards`
}
