/**
 * Warhammer 40,000 Types and Constants
 * Defines game types, army rules, and configuration interfaces for Warhammer 40k events
 */

// =============================================================================
// GAME TYPES
// =============================================================================

export type Warhammer40kGameType = 'matched' | 'narrative' | 'crusade' | 'open'

export const GAME_TYPE_LABELS: Record<Warhammer40kGameType, string> = {
  matched: 'Matched Play',
  narrative: 'Narrative Play',
  crusade: 'Crusade',
  open: 'Open Play',
}

export const GAME_TYPE_DESCRIPTIONS: Record<Warhammer40kGameType, string> = {
  matched: 'Balanced competitive play with equal points and matched missions',
  narrative: 'Story-driven games with asymmetric scenarios',
  crusade: 'Campaign play where your army grows and evolves over time',
  open: 'Casual play with no restrictions — bring whatever you want',
}

// =============================================================================
// EVENT TYPES
// =============================================================================

export type Warhammer40kEventType = 'casual' | 'tournament' | 'campaign' | 'league'

export const EVENT_TYPE_LABELS: Record<Warhammer40kEventType, string> = {
  casual: 'Casual Game',
  tournament: 'Tournament',
  campaign: 'Campaign',
  league: 'League',
}

export const EVENT_TYPE_DESCRIPTIONS: Record<Warhammer40kEventType, string> = {
  casual: 'Pick-up game or friendly match',
  tournament: 'Competitive event with rankings and prizes',
  campaign: 'Multi-session narrative campaign',
  league: 'Ongoing league with scheduled rounds',
}

// =============================================================================
// PLAYER MODES
// =============================================================================

export type Warhammer40kPlayerMode = '1v1' | '2v2' | 'group'

export const PLAYER_MODE_LABELS: Record<Warhammer40kPlayerMode, string> = {
  '1v1': '1 vs 1',
  '2v2': '2 vs 2',
  group: 'Group / Multiplayer',
}

// =============================================================================
// TERRAIN & TABLE
// =============================================================================

export type Warhammer40kTerrainType = 'tournament' | 'casual' | 'bring_your_own'

export const TERRAIN_TYPE_LABELS: Record<Warhammer40kTerrainType, string> = {
  tournament: 'Tournament Standard',
  casual: 'Casual / Store Terrain',
  bring_your_own: 'Bring Your Own',
}

export const TERRAIN_TYPE_DESCRIPTIONS: Record<Warhammer40kTerrainType, string> = {
  tournament: 'GW-recommended competitive terrain layout',
  casual: 'Whatever terrain is available at the venue',
  bring_your_own: 'Players bring their own terrain pieces',
}

export interface TableSize {
  id: string
  label: string
  description: string
}

export const TABLE_SIZES: TableSize[] = [
  { id: '44x30', label: '44" x 30"', description: 'Combat Patrol (500pts)' },
  { id: '44x60', label: '44" x 60"', description: 'Strike Force / Onslaught (1000-3000pts)' },
  { id: 'custom', label: 'Custom', description: 'Non-standard table size' },
]

// =============================================================================
// MISSION PACKS
// =============================================================================

export interface MissionPack {
  id: string
  name: string
  description: string
}

export const MISSION_PACKS: MissionPack[] = [
  { id: 'leviathan', name: 'Leviathan', description: '10th Edition core mission pack' },
  { id: 'pariah_nexus', name: 'Pariah Nexus', description: 'Competitive mission pack' },
  { id: 'chapter_approved', name: 'Chapter Approved', description: 'Seasonal competitive missions' },
  { id: 'crusade', name: 'Crusade Missions', description: 'Narrative campaign missions' },
  { id: 'custom', name: 'Custom Mission', description: 'Homebrew or modified missions' },
]

// =============================================================================
// POINTS PRESETS
// =============================================================================

export const POINTS_PRESETS: number[] = [500, 1000, 1500, 2000, 2500, 3000]

// =============================================================================
// TOURNAMENT STRUCTURE
// =============================================================================

export type Warhammer40kTournamentStyle = 'swiss' | 'single_elimination' | 'round_robin'

export const TOURNAMENT_STYLE_LABELS: Record<Warhammer40kTournamentStyle, string> = {
  swiss: 'Swiss',
  single_elimination: 'Single Elimination',
  round_robin: 'Round Robin',
}

// =============================================================================
// HELPER CONSTANTS
// =============================================================================

export const TOURNAMENT_EVENT_TYPES: Warhammer40kEventType[] = ['tournament', 'league']

export const DEFAULT_MAX_PLAYERS: Record<Warhammer40kEventType, number> = {
  casual: 2,
  tournament: 16,
  campaign: 8,
  league: 12,
}

export const DEFAULT_TIME_LIMIT: Record<Warhammer40kGameType, number> = {
  matched: 180,
  narrative: 180,
  crusade: 180,
  open: 120,
}

// =============================================================================
// EVENT CONFIG
// =============================================================================

export interface Warhammer40kEventConfig {
  // Game setup
  gameType: Warhammer40kGameType
  pointsLimit: number
  playerMode: Warhammer40kPlayerMode

  // Mission
  missionPack: string | null
  missionNotes: string | null

  // Army rules
  battleReadyRequired: boolean
  wysiwygRequired: boolean
  forgeWorldAllowed: boolean
  legendsAllowed: boolean
  armyRulesNotes: string | null

  // Terrain & table
  terrainType: Warhammer40kTerrainType
  tableSize: string

  // Game flow
  timeLimitMinutes: number | null

  // Tournament
  eventType: Warhammer40kEventType
  tournamentStyle: Warhammer40kTournamentStyle | null
  roundsCount: number | null

  // Prizes
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string

  // Settings
  allowSpectators: boolean
  allowProxies: boolean
  proxyNotes: string | null
}

export interface Warhammer40kEventConfigInput {
  gameType: Warhammer40kGameType
  pointsLimit: number
  playerMode: Warhammer40kPlayerMode
  missionPack: string | null
  missionNotes: string | null
  battleReadyRequired: boolean
  wysiwygRequired: boolean
  forgeWorldAllowed: boolean
  legendsAllowed: boolean
  armyRulesNotes: string | null
  terrainType: Warhammer40kTerrainType
  tableSize: string
  timeLimitMinutes: number | null
  eventType: Warhammer40kEventType
  tournamentStyle: Warhammer40kTournamentStyle | null
  roundsCount: number | null
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  allowSpectators: boolean
  allowProxies: boolean
  proxyNotes: string | null
}

export interface CreateWarhammer40kEventInput {
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
  // Warhammer 40k config
  warhammer40kConfig: Warhammer40kEventConfigInput
}

// =============================================================================
// DEFAULT CONFIG
// =============================================================================

export const DEFAULT_WARHAMMER40K_CONFIG: Warhammer40kEventConfigInput = {
  gameType: 'matched',
  pointsLimit: 2000,
  playerMode: '1v1',
  missionPack: 'leviathan',
  missionNotes: null,
  battleReadyRequired: false,
  wysiwygRequired: false,
  forgeWorldAllowed: true,
  legendsAllowed: false,
  armyRulesNotes: null,
  terrainType: 'casual',
  tableSize: '44x60',
  timeLimitMinutes: null,
  eventType: 'casual',
  tournamentStyle: null,
  roundsCount: null,
  hasPrizes: false,
  prizeStructure: null,
  entryFee: null,
  entryFeeCurrency: 'USD',
  allowSpectators: true,
  allowProxies: false,
  proxyNotes: null,
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

export function isTournamentEventType(eventType: Warhammer40kEventType): boolean {
  return TOURNAMENT_EVENT_TYPES.includes(eventType)
}

export function getGameTypeLabel(gameType: Warhammer40kGameType): string {
  return GAME_TYPE_LABELS[gameType] || gameType
}

export function getPointsLabel(points: number): string {
  return `${points}pts`
}

export function getMissionPack(packId: string | null): MissionPack | null {
  if (!packId) return null
  return MISSION_PACKS.find((p) => p.id === packId) || null
}

export function getTableSize(sizeId: string): TableSize | null {
  return TABLE_SIZES.find((s) => s.id === sizeId) || null
}
