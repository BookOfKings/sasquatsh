// Multi-table game session types

export interface EventTable {
  id: string
  tableNumber: number
  tableName: string | null
}

export interface SessionRegistration {
  userId: string
  displayName: string | null
  avatarUrl: string | null
  isHostReserved: boolean
}

export interface GameSession {
  id: string
  tableId: string
  tableNumber: number
  bggId: number | null
  gameName: string
  thumbnailUrl: string | null
  minPlayers: number | null
  maxPlayers: number | null
  slotIndex: number
  startTime: string | null
  durationMinutes: number
  status: string
  registeredCount: number
  isFull: boolean
  isUserRegistered: boolean
  registrations: SessionRegistration[]
}

export interface EventSessionsResponse {
  tables: EventTable[]
  sessions: GameSession[]
}

// Planning session scheduling types
export interface ScheduleEntry {
  suggestionId: string
  tableNumber: number
  slotIndex: number
  durationOverride?: number
}

export interface HostPreference {
  tableNumber: number
  slotIndex: number
}
