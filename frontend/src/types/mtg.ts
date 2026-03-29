// Magic: The Gathering Types

// Scryfall Card Data
export interface ScryfallCard {
  scryfallId: string
  oracleId: string
  name: string
  manaCost: string | null
  cmc: number
  typeLine: string
  oracleText: string | null
  power: string | null
  toughness: string | null
  loyalty: string | null
  colors: string[]
  colorIdentity: string[]
  keywords: string[]
  legalities: Record<string, 'legal' | 'not_legal' | 'restricted' | 'banned'>
  setCode: string
  setName: string
  collectorNumber: string
  rarity: string
  imageUris: {
    small: string | null
    normal: string | null
    large: string | null
    artCrop: string | null
    png: string | null
  }
  prices: Record<string, string | null>
  isDoubleFaced: boolean
  cardFaces: CardFace[] | null
  layout: string
}

export interface CardFace {
  name: string
  manaCost: string | null
  typeLine: string | null
  oracleText: string | null
  imageUris: {
    small: string | null
    normal: string | null
    large: string | null
    artCrop: string | null
    png: string | null
  } | null
}

// Search result (lighter weight)
export interface ScryfallSearchResult {
  scryfallId: string
  name: string
  manaCost: string | null
  typeLine: string
  imageUrl: string | null
  setCode: string
  rarity: string
}

// Banned card with Oracle identity as canonical reference
export interface BannedCard {
  oracleId: string           // Canonical identity - the Oracle ID
  name: string               // Display name
  scryfallId?: string        // Optional: specific printing reference
  typeLine?: string          // Display: type line for context
  imageUrl?: string          // Display: card image for UI
  setCode?: string           // Display: set code if needed
}

// Helper to create a BannedCard from a ScryfallCard
export function toBannedCard(card: ScryfallCard): BannedCard {
  return {
    oracleId: card.oracleId,
    name: card.name,
    scryfallId: card.scryfallId,
    typeLine: card.typeLine,
    imageUrl: card.imageUris.small || card.imageUris.normal || undefined,
    setCode: card.setCode,
  }
}

// MTG Format
export interface MtgFormat {
  id: string
  name: string
  description: string | null
  minDeckSize: number | null
  maxDeckSize: number | null
  maxCopies: number | null
  hasCommander: boolean
  hasSideboard: boolean
  sideboardSize: number
  isConstructed: boolean
}

// Deck Types
export interface MtgDeck {
  id: string
  ownerUserId: string
  name: string
  formatId: string | null
  commanderScryfallId: string | null
  partnerCommanderScryfallId: string | null
  description: string | null
  powerLevel: number | null
  isPublic: boolean
  moxfieldId: string | null
  archidektId: string | null
  importUrl: string | null
  createdAt: string
  updatedAt: string
  // Populated fields
  commander?: ScryfallCard | null
  partnerCommander?: ScryfallCard | null
  cards?: MtgDeckCard[]
  cardCount?: number
  format?: MtgFormat | null
}

export interface MtgDeckCard {
  id: string
  deckId: string
  scryfallId: string
  quantity: number
  board: 'main' | 'sideboard' | 'maybeboard' | 'commander'
  card?: ScryfallCard
}

export interface CreateDeckInput {
  name: string
  formatId?: string
  description?: string
  powerLevel?: number
  isPublic?: boolean
  commanderScryfallId?: string
  partnerCommanderScryfallId?: string
}

export interface UpdateDeckInput {
  name?: string
  formatId?: string
  description?: string
  powerLevel?: number
  isPublic?: boolean
  commanderScryfallId?: string
  partnerCommanderScryfallId?: string
}

export interface AddDeckCardInput {
  scryfallId: string
  quantity: number
  board: 'main' | 'sideboard' | 'maybeboard' | 'commander'
}

export interface ImportDeckInput {
  source: 'moxfield' | 'archidekt' | 'text'
  url?: string
  deckList?: string
  name?: string
  formatId?: string
}

// MTG Event Configuration
export interface MtgEventConfig {
  eventId: string
  formatId: string | null
  customFormatName: string | null
  eventType: MtgEventType
  roundsCount: number | null
  roundTimeMinutes: number
  podsSize: number | null
  allowProxies: boolean
  proxyLimit: number | null
  powerLevelMin: number | null
  powerLevelMax: number | null
  powerLevelRange: PowerLevelRange | null  // Simplified power level selection
  bannedCards: string[]
  packsPerPlayer: number | null
  draftStyle: DraftStyle | null
  cubeId: string | null
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowSpectators: boolean
  // New fields for enhanced event structure
  matchStyle: MatchStyle | null           // Best of 1 or Best of 3
  topCut: number | null                   // Top cut size (4, 8, etc.)
  playMode: PlayMode | null               // Open play, assigned pods, tournament pairings
  houseRulesNotes: string | null          // Custom house rules text
}

export type MtgEventType =
  | 'casual'
  | 'swiss'
  | 'single_elim'
  | 'double_elim'
  | 'round_robin'
  | 'pods'

export type DraftStyle =
  | 'standard'
  | 'rochester'
  | 'winston'
  | 'grid'

// Match style for tournament play
export type MatchStyle = 'bo1' | 'bo3'

// Play/seating mode
export type PlayMode = 'open_play' | 'assigned_pods' | 'tournament_pairings'

// Simplified power level ranges
export type PowerLevelRange = 'casual' | 'mid' | 'high' | 'cedh' | 'custom'

// Power level range definitions
export const POWER_LEVEL_RANGES: Record<PowerLevelRange, { min: number; max: number; label: string; description: string }> = {
  casual: { min: 1, max: 4, label: 'Casual', description: 'Precons, upgraded precons, and jank decks' },
  mid: { min: 5, max: 6, label: 'Mid', description: 'Balanced play, typical casual groups' },
  high: { min: 7, max: 8, label: 'High Power', description: 'Optimized decks, strong strategies' },
  cedh: { min: 9, max: 10, label: 'cEDH', description: 'Fully competitive, no restrictions' },
  custom: { min: 1, max: 10, label: 'Custom Range', description: 'Set your own power level range' },
}

// Format descriptions for UI display
export const FORMAT_DESCRIPTIONS: Record<string, string> = {
  commander: '100 cards, singleton, multiplayer-friendly',
  standard: '60-card constructed, rotating format',
  modern: '60-card constructed, cards from 8th Edition forward',
  pioneer: '60-card constructed, cards from Return to Ravnica forward',
  legacy: '60-card constructed, most cards legal',
  vintage: '60-card constructed, Power Nine legal (restricted)',
  pauper: '60-card constructed, commons only',
  draft: 'Limited format, deck built on site from packs',
  sealed: 'Limited format, 40-card deck from 6 packs',
  cube: 'Draft from a curated card pool',
  cube_draft: 'Draft from a curated card pool',
  oathbreaker: '60-card singleton, Planeswalker as commander',
  brawl: 'Standard-legal Commander variant, 60 cards',
  casual: 'Kitchen table magic, house rules welcome',
  custom: 'Custom format with your own rules',
}

// Format categories for grouping in UI
export const FORMAT_CATEGORIES: Record<string, string[]> = {
  'Constructed': ['standard', 'modern', 'pioneer', 'legacy', 'vintage', 'pauper'],
  'Commander': ['commander', 'oathbreaker', 'brawl'],
  'Limited': ['draft', 'sealed', 'cube', 'cube_draft'],
  'Casual': ['casual', 'custom'],
}

// Formats that support power level selection
export const POWER_LEVEL_FORMATS = ['commander', 'oathbreaker', 'brawl', 'casual', 'custom']

// Event creation input
export interface CreateMtgEventInput {
  // Base event fields
  title: string
  description?: string
  eventDate: string
  startTime: string
  timezone?: string
  durationMinutes?: number
  setupMinutes?: number
  maxPlayers?: number
  hostIsPlaying?: boolean
  isPublic?: boolean
  isCharityEvent?: boolean
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
  // MTG config
  mtgConfig: Partial<MtgEventConfig>
}

// MTG Registration with deck
export interface MtgEventRegistration {
  registrationId: string
  deckId: string | null
  deckSnapshot: MtgDeck | null
  commanderName: string | null
  commanderImageUrl: string | null
  submittedAt: string | null
}

// Event tags for MTG events
export type MtgEventTag =
  | 'cedh'           // Competitive EDH
  | 'budget'         // Budget restrictions
  | 'beginner'       // Beginner friendly
  | 'competitive'    // Competitive play
  | 'proxy_friendly' // Proxies allowed
  | 'custom_rules'   // House rules apply

// Power level descriptions
export const POWER_LEVEL_DESCRIPTIONS: Record<number, string> = {
  1: 'Precon - Unmodified preconstructed deck',
  2: 'Upgraded Precon - Light modifications to precon',
  3: 'Casual - Fun, theme, or jank focused',
  4: 'Low Power - Intentionally weaker builds',
  5: 'Mid Power - Balanced, typical casual play',
  6: 'Focused - Clear strategy, some optimization',
  7: 'Optimized - Tuned deck, efficient cards',
  8: 'High Power - Strong strategy, expensive cards',
  9: 'Fringe cEDH - Near-competitive, few restrictions',
  10: 'cEDH - Fully competitive, no restrictions',
}

// Helper to get mana symbol URLs (using Scryfall's SVGs)
export function getManaSymbolUrl(symbol: string): string {
  // Clean the symbol (remove { and })
  const clean = symbol.replace(/[{}]/g, '').toLowerCase()
  return `https://svgs.scryfall.io/card-symbols/${clean}.svg`
}

// Parse mana cost string into individual symbols
export function parseManaCost(manaCost: string | null): string[] {
  if (!manaCost) return []
  const matches = manaCost.match(/\{[^}]+\}/g)
  return matches || []
}

// Get color name from color code
export const COLOR_NAMES: Record<string, string> = {
  W: 'White',
  U: 'Blue',
  B: 'Black',
  R: 'Red',
  G: 'Green',
  C: 'Colorless',
}

// Get rarity display name
export const RARITY_NAMES: Record<string, string> = {
  common: 'Common',
  uncommon: 'Uncommon',
  rare: 'Rare',
  mythic: 'Mythic Rare',
  special: 'Special',
  bonus: 'Bonus',
}
