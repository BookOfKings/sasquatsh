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
