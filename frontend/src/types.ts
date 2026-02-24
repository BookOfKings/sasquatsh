export interface Player {
  id: string
  name: string
  email: string
  reservedAt: string
}

export interface BoardGame {
  id: string
  title: string
  description?: string
  host?: string
  location?: string
  startTime: string
  maxSeats: number
  players: Player[]
}

export interface NewBoardGameInput {
  title: string
  maxSeats: number
  description?: string
  host?: string
  location?: string
  startTime: string
}

export interface UpdateBoardGameInput {
  maxSeats: number
  location?: string
  startTime: string
}

export interface ReservationPayload {
  name: string
  email: string
}

export interface GameActionResult {
  ok: boolean
  message: string
}

export interface ReservationDto {
  id: string
  name: string
  email: string
  reservedAt: string
}

export interface BoardGameDto {
  id: string
  title: string
  description?: string | null
  host?: string | null
  location?: string | null
  maxSeats: number
  startTime: string
  createdAt?: string
  reservations: ReservationDto[]
}

export type ApiBoardGame = Pick<
  BoardGameDto,
  'title' | 'description' | 'host' | 'location' | 'maxSeats' | 'startTime'
>

export type ApiUpdateBoardGame = Pick<BoardGameDto, 'location' | 'maxSeats' | 'startTime'>

export type ApiCreateGameResponse = BoardGameDto

export interface ApiErrorResponse {
  message?: string
}
