export interface BggSearchResult {
  bggId: number
  name: string
  yearPublished: number | null
  thumbnailUrl?: string | null
}

export interface BggGame {
  bggId: number
  name: string
  yearPublished: number | null
  thumbnailUrl: string | null
  imageUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  minPlaytime: number | null
  maxPlaytime: number | null
  playingTime: number | null
  weight: number | null
  description: string | null
  categories: string[]
  mechanics: string[]
}

export interface EventGame {
  id: string
  eventId: string
  bggId: number | null
  gameName: string
  thumbnailUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  playingTime: number | null
  isPrimary: boolean
  isAlternative: boolean
  addedByUserId: string | null
  createdAt: string
}

export interface AddEventGameInput {
  bggId?: number
  gameName: string
  thumbnailUrl?: string
  minPlayers?: number
  maxPlayers?: number
  playingTime?: number
  isPrimary?: boolean
  isAlternative?: boolean
}
