// Pokemon TCG types and interfaces
// Based on Pokemon TCG API v2: https://docs.pokemontcg.io/

// ==================== Card Types ====================

export interface PokemonCard {
  id: string                    // Unique identifier (e.g., "base1-4")
  name: string                  // Card name (e.g., "Charizard")
  supertype: PokemonSupertype   // Pokemon, Trainer, or Energy
  subtypes?: string[]           // e.g., ["Stage 2"], ["Item"], ["Basic"]
  level?: string                // Pokemon level (older cards)
  hp?: string                   // Hit points (e.g., "120")
  types?: PokemonType[]         // Energy types (e.g., ["Fire"])
  evolvesFrom?: string          // What this Pokemon evolves from
  evolvesTo?: string[]          // What this Pokemon can evolve to

  // Rules and abilities
  rules?: string[]              // Special rules text
  ancientTrait?: AncientTrait
  abilities?: Ability[]
  attacks?: Attack[]

  // Weaknesses, resistances, retreat
  weaknesses?: WeaknessResistance[]
  resistances?: WeaknessResistance[]
  retreatCost?: string[]        // Energy symbols for retreat
  convertedRetreatCost?: number

  // Set info
  set: PokemonSet
  number: string                // Card number in set

  // Artist and rarity
  artist?: string
  rarity?: string
  flavorText?: string
  nationalPokedexNumbers?: number[]

  // Legalities
  legalities: PokemonLegalities

  // Regulation mark (for Standard rotation)
  regulationMark?: string

  // Images
  images: CardImages

  // Pricing
  tcgplayer?: TcgPlayerPricing
  cardmarket?: CardMarketPricing
}

export type PokemonSupertype = 'Pokémon' | 'Trainer' | 'Energy'

export type PokemonType =
  | 'Colorless'
  | 'Darkness'
  | 'Dragon'
  | 'Fairy'
  | 'Fighting'
  | 'Fire'
  | 'Grass'
  | 'Lightning'
  | 'Metal'
  | 'Psychic'
  | 'Water'

export interface AncientTrait {
  name: string
  text: string
}

export interface Ability {
  name: string
  text: string
  type: string  // Usually "Ability" or "Poké-Power", "Poké-Body"
}

export interface Attack {
  name: string
  cost: string[]         // Energy cost (e.g., ["Fire", "Fire", "Colorless"])
  convertedEnergyCost: number
  damage: string         // Damage amount (e.g., "100", "30+", "")
  text: string           // Attack effect text
}

export interface WeaknessResistance {
  type: PokemonType
  value: string          // e.g., "×2", "-30"
}

export interface CardImages {
  small: string          // Small card image URL
  large: string          // Large card image URL
}

// ==================== Set Types ====================

export interface PokemonSet {
  id: string              // Unique identifier (e.g., "base1")
  name: string            // Set name (e.g., "Base")
  series: string          // Series name (e.g., "Base")
  printedTotal: number    // Number of cards printed in set
  total: number           // Total cards including secret rares
  legalities: PokemonLegalities
  ptcgoCode?: string      // Pokemon TCG Online code
  releaseDate: string     // Release date (YYYY/MM/DD)
  updatedAt: string       // Last updated timestamp
  images: SetImages
}

export interface SetImages {
  symbol: string          // Set symbol image URL
  logo: string            // Set logo image URL
}

// ==================== Legality Types ====================

export interface PokemonLegalities {
  standard?: LegalityStatus
  expanded?: LegalityStatus
  unlimited?: LegalityStatus
}

export type LegalityStatus = 'Legal' | 'Banned' | 'Not Legal'

// Format definitions for deck building
export interface PokemonFormat {
  id: string
  name: string
  description: string
  minDeckSize: number
  maxDeckSize: number
  maxCopies: number       // Max copies of non-basic energy cards (usually 4)
  isRotating: boolean     // Whether the format rotates
}

export const POKEMON_FORMATS: PokemonFormat[] = [
  {
    id: 'standard',
    name: 'Standard',
    description: 'The main competitive format with the most recent sets',
    minDeckSize: 60,
    maxDeckSize: 60,
    maxCopies: 4,
    isRotating: true,
  },
  {
    id: 'expanded',
    name: 'Expanded',
    description: 'Includes cards from Black & White era onwards',
    minDeckSize: 60,
    maxDeckSize: 60,
    maxCopies: 4,
    isRotating: false,
  },
  {
    id: 'unlimited',
    name: 'Unlimited',
    description: 'All cards ever printed are legal',
    minDeckSize: 60,
    maxDeckSize: 60,
    maxCopies: 4,
    isRotating: false,
  },
  {
    id: 'theme',
    name: 'Theme Deck',
    description: 'Pre-constructed theme decks only',
    minDeckSize: 60,
    maxDeckSize: 60,
    maxCopies: 4,
    isRotating: false,
  },
]

// ==================== Pricing Types ====================

export interface TcgPlayerPricing {
  url: string
  updatedAt: string
  prices?: {
    normal?: PriceData
    holofoil?: PriceData
    reverseHolofoil?: PriceData
    firstEditionHolofoil?: PriceData
    firstEditionNormal?: PriceData
  }
}

export interface CardMarketPricing {
  url: string
  updatedAt: string
  prices?: {
    averageSellPrice?: number
    lowPrice?: number
    trendPrice?: number
    reverseHoloSell?: number
    reverseHoloLow?: number
    reverseHoloTrend?: number
  }
}

export interface PriceData {
  low?: number
  mid?: number
  high?: number
  market?: number
  directLow?: number
}

// ==================== Deck Types ====================

export interface PokemonDeck {
  id: string
  ownerUserId: string
  name: string
  description?: string
  formatId: string

  // Cards in the deck
  cards: PokemonDeckCard[]

  // Metadata
  cardCount: number
  pokemonCount: number
  trainerCount: number
  energyCount: number

  // External imports
  pokemonTcgLiveCode?: string
  limitlessTcgId?: string
  importUrl?: string

  // Stats
  estimatedPriceUsd?: number

  isPublic: boolean
  createdAt: string
  updatedAt: string
}

export interface PokemonDeckCard {
  id: string
  deckId: string
  pokemonTcgId: string   // Pokemon TCG API card ID
  quantity: number
  cardName: string       // Cached for display

  // Card type for deck composition stats
  supertype: PokemonSupertype
}

// ==================== Event Types ====================

export type PokemonEventType =
  | 'casual'           // Casual play
  | 'league'           // Pokemon League play
  | 'league_cup'       // League Cup tournament
  | 'league_challenge' // League Challenge
  | 'regional'         // Regional Championship
  | 'international'    // International Championship
  | 'worlds'           // World Championship
  | 'prerelease'       // Prerelease event
  | 'draft'            // Booster draft

export type PokemonTournamentStyle =
  | 'swiss'
  | 'single_elimination'
  | 'double_elimination'

export interface PokemonEventConfig {
  formatId: string
  eventType: PokemonEventType

  // Tournament settings
  tournamentStyle?: PokemonTournamentStyle
  roundsCount?: number
  roundTimeMinutes?: number
  bestOf?: 1 | 3
  topCut?: number

  // Registration
  requireDeckRegistration: boolean
  deckSubmissionDeadline?: string
  allowDeckChanges: boolean
  enforceFormatLegality: boolean

  // Entry and prizes
  entryFee?: number
  entryFeeCurrency?: string
  hasPrizes: boolean
  prizeStructure?: string

  // Play settings
  allowProxies: boolean
  proxyLimit?: number
  usePlayPoints: boolean  // Official Play! Pokemon points
  houseRulesNotes?: string  // Custom house rules

  // Event materials
  providesBasicEnergy: boolean
  providesDamageCounters: boolean
  sleevesRecommended: boolean
  providesBuildBattleKits: boolean  // For prerelease events

  // Age divisions (official events)
  ageDivisions?: ('junior' | 'senior' | 'masters')[]
}

// ==================== Registration Types ====================

export interface PokemonEventRegistration {
  id: string
  registrationId: string
  deckId?: string

  // Deck snapshot at submission
  deckSnapshot?: PokemonDeckSnapshot

  // Quick display fields
  featuredPokemonName?: string
  featuredPokemonImage?: string
  deckArchetype?: string          // e.g., "Charizard ex", "Lugia VSTAR"

  // Age division for official events
  ageDivision?: 'junior' | 'senior' | 'masters'

  submittedAt?: string
  isConfirmed: boolean

  createdAt: string
  updatedAt: string
}

export interface PokemonDeckSnapshot {
  name: string
  formatId: string
  cards: {
    pokemonTcgId: string
    cardName: string
    quantity: number
    supertype: PokemonSupertype
  }[]
  cardCount: number
  pokemonCount: number
  trainerCount: number
  energyCount: number
}

// ==================== API Response Types ====================

export interface PokemonCardSearchResult {
  data: PokemonCard[]
  page: number
  pageSize: number
  count: number
  totalCount: number
}

export interface PokemonSetSearchResult {
  data: PokemonSet[]
  page: number
  pageSize: number
  count: number
  totalCount: number
}

// ==================== Cache Types ====================

export interface PokemonCardCache {
  pokemonTcgId: string
  name: string
  supertype: PokemonSupertype
  subtypes?: string[]
  hp?: string
  types?: PokemonType[]
  evolvesFrom?: string
  setId: string
  setName: string
  number: string
  rarity?: string
  imageSmall: string
  imageLarge: string
  legalities: PokemonLegalities
  priceUsd?: number
  cachedAt: string
  staleAt: string
}

export interface PokemonSetCache {
  setId: string
  name: string
  series: string
  printedTotal: number
  total: number
  releaseDate: string
  symbolUrl: string
  logoUrl: string
  legalities: PokemonLegalities
  cachedAt: string
  staleAt: string
}

// ==================== Helper Functions ====================

export function getCardDisplayName(card: PokemonCard): string {
  return card.name
}

export function getCardSetInfo(card: PokemonCard): string {
  return `${card.set.name} ${card.number}/${card.set.printedTotal}`
}

export function isCardLegalInFormat(card: PokemonCard, formatId: string): boolean {
  const legality = card.legalities[formatId as keyof PokemonLegalities]
  return legality === 'Legal'
}

export function getCardPrice(card: PokemonCard): number | undefined {
  // Try to get market price from TCGPlayer
  const tcgPrices = card.tcgplayer?.prices
  if (tcgPrices) {
    // Prefer normal, then holofoil, then reverse
    return tcgPrices.normal?.market
      ?? tcgPrices.holofoil?.market
      ?? tcgPrices.reverseHolofoil?.market
  }

  // Fall back to Cardmarket
  return card.cardmarket?.prices?.averageSellPrice
}

export function getDeckTypeBreakdown(deck: PokemonDeck): {
  pokemon: number
  trainer: number
  energy: number
} {
  return {
    pokemon: deck.pokemonCount,
    trainer: deck.trainerCount,
    energy: deck.energyCount,
  }
}

export function validateDeckSize(deck: PokemonDeck, format: PokemonFormat): {
  valid: boolean
  message?: string
} {
  if (deck.cardCount < format.minDeckSize) {
    return {
      valid: false,
      message: `Deck has ${deck.cardCount} cards, minimum is ${format.minDeckSize}`,
    }
  }
  if (deck.cardCount > format.maxDeckSize) {
    return {
      valid: false,
      message: `Deck has ${deck.cardCount} cards, maximum is ${format.maxDeckSize}`,
    }
  }
  return { valid: true }
}

// Energy type colors for UI
export const POKEMON_TYPE_COLORS: Record<PokemonType, string> = {
  Colorless: '#A8A878',
  Darkness: '#705848',
  Dragon: '#7038F8',
  Fairy: '#EE99AC',
  Fighting: '#C03028',
  Fire: '#F08030',
  Grass: '#78C850',
  Lightning: '#F8D030',
  Metal: '#B8B8D0',
  Psychic: '#F85888',
  Water: '#6890F0',
}

// Supertype icons/labels
export const POKEMON_SUPERTYPE_LABELS: Record<PokemonSupertype, string> = {
  'Pokémon': 'Pokemon',
  'Trainer': 'Trainer',
  'Energy': 'Energy',
}

// ==================== Event Type Metadata ====================

export const POKEMON_EVENT_TYPE_DESCRIPTIONS: Record<PokemonEventType, string> = {
  casual: 'Free play, no structured rounds',
  league: 'Casual recurring league-style play',
  league_challenge: 'Small competitive local event',
  league_cup: 'Higher-level local competitive event',
  regional: 'Large competitive tournament',
  international: 'International Championship event',
  worlds: 'World Championship event',
  prerelease: 'Build from sealed product and play on-site',
  draft: 'Limited event using drafted packs',
}

// Event types that show tournament settings
export const TOURNAMENT_EVENT_TYPES: PokemonEventType[] = [
  'league_challenge',
  'league_cup',
  'regional',
  'international',
  'worlds',
]

// Event types that are limited formats (build on-site)
export const LIMITED_EVENT_TYPES: PokemonEventType[] = [
  'prerelease',
  'draft',
]

// Default max players by event type
export const DEFAULT_MAX_PLAYERS: Record<PokemonEventType, number> = {
  casual: 16,
  league: 24,
  league_challenge: 24,
  league_cup: 32,
  regional: 64,
  international: 128,
  worlds: 128,
  prerelease: 32,
  draft: 8,
}

// Format-specific deck descriptions
export const FORMAT_DECK_DESCRIPTIONS: Record<string, string> = {
  standard: '60-card decks using the current Standard format',
  expanded: '60-card decks using Expanded legality',
  unlimited: '60-card decks with all cards legal',
  theme: 'Preconstructed theme deck event',
  casual: 'Casual play with house rules',
  gym_leader_challenge: 'Single-type singleton format (Gym Leader Challenge)',
  retro: 'Classic format using older card pools',
}

// Limited event helper text
export const LIMITED_EVENT_GUIDANCE = {
  prerelease: {
    title: 'Prerelease Event',
    description: 'Players build 40-card decks on-site from Build & Battle Kits',
    tips: [
      'Build & Battle Kits should be provided',
      'Basic Energy should be available for players',
      '40-card minimum deck size',
      '4-card copy limit still applies',
    ],
  },
  draft: {
    title: 'Draft Event',
    description: 'Players draft cards from booster packs and build decks on-site',
    tips: [
      'Typically 3 packs per player',
      'Basic Energy should be available',
      '40-card minimum deck size',
      'Players keep what they draft (or rare redraft)',
    ],
  },
}

// ==================== Event Creation Types ====================

export interface CreatePokemonEventInput {
  // Basic event info
  title: string
  description?: string
  eventDate: string
  startTime: string
  timezone: string
  durationMinutes: number
  setupMinutes: number
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

  // Pokemon TCG config
  pokemonConfig: PokemonEventConfigInput
}

export interface PokemonEventConfigInput {
  formatId: string | null
  customFormatName: string | null

  // Event structure
  eventType: PokemonEventType
  tournamentStyle: PokemonTournamentStyle | null
  roundsCount: number | null
  roundTimeMinutes: number
  bestOf: 1 | 3
  topCut: number | null

  // Deck rules
  allowProxies: boolean
  proxyLimit: number | null
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowDeckChanges: boolean
  enforceFormatLegality: boolean
  houseRulesNotes: string | null

  // Prizes and entry
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  usePlayPoints: boolean  // Awards Championship Points (organizer intent)
  organizerConfirmedOfficialLocation: boolean  // Self-attestation for Play! Pokemon location

  // Event materials
  providesBasicEnergy: boolean
  providesDamageCounters: boolean
  sleevesRecommended: boolean
  providesBuildBattleKits: boolean

  // Age divisions
  hasJuniorDivision: boolean
  hasSeniorDivision: boolean
  hasMastersDivision: boolean

  // Spectators
  allowSpectators: boolean
}
