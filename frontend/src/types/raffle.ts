// Raffle System Types

export type RaffleStatus = 'draft' | 'active' | 'ended' | 'cancelled'
export type RaffleEntryType = 'host_event' | 'plan_session' | 'attend_event' | 'mail_in'

export interface RaffleUser {
  id: string
  displayName: string | null
  avatarUrl: string | null
  subscriptionTier?: string
}

export interface Raffle {
  id: string
  title: string
  description: string | null
  prizeName: string
  prizeDescription: string | null
  prizeImageUrl: string | null
  prizeBggId: number | null
  prizeValueCents: number | null
  startDate: string
  endDate: string
  termsConditions: string | null
  mailInInstructions: string | null
  status: RaffleStatus
  winnerUserId: string | null
  winnerSelectedAt: string | null
  winnerNotifiedAt: string | null
  winnerClaimedAt: string | null
  bannerImageUrl: string | null
  createdAt: string
  updatedAt: string
}

export interface RaffleWithDetails extends Raffle {
  winner?: RaffleUser | null
  createdBy?: RaffleUser | null
  stats?: {
    totalEntries: number
    uniqueParticipants: number
    entries?: number
    users?: number
  }
  userEntries?: RaffleEntry[]
  userTotalEntries?: number
}

export interface RaffleEntry {
  id: string
  raffleId: string
  userId: string
  entryType: RaffleEntryType
  sourceId: string | null
  entryCount: number
  createdAt: string
}

export interface RaffleEntryWithUser extends RaffleEntry {
  mailInName?: string | null
  mailInAddress?: string | null
  mailInVerified?: boolean
  user?: RaffleUser | null
}

export interface CreateRaffleInput {
  title: string
  description?: string
  prizeName: string
  prizeDescription?: string
  prizeImageUrl?: string
  prizeBggId?: number
  prizeValueCents?: number
  startDate: string
  endDate: string
  termsConditions?: string
  mailInInstructions?: string
  bannerImageUrl?: string
  status?: RaffleStatus
}

export interface UpdateRaffleInput {
  title?: string
  description?: string | null
  prizeName?: string
  prizeDescription?: string | null
  prizeImageUrl?: string | null
  prizeBggId?: number | null
  prizeValueCents?: number | null
  startDate?: string
  endDate?: string
  termsConditions?: string | null
  mailInInstructions?: string | null
  bannerImageUrl?: string | null
  status?: RaffleStatus
  winnerNotifiedAt?: string | null
  winnerClaimedAt?: string | null
}

export interface MailInEntryInput {
  name: string
  address: string
  raffleId?: string
}

// Labels for entry types
export const ENTRY_TYPE_LABELS: Record<RaffleEntryType, string> = {
  host_event: 'Hosted Event',
  plan_session: 'Planned Session',
  attend_event: 'Attended Event',
  mail_in: 'Mail-in Entry',
}

// Icons for entry types (using emoji for simplicity)
export const ENTRY_TYPE_ICONS: Record<RaffleEntryType, string> = {
  host_event: 'calendar-plus',
  plan_session: 'clipboard-list',
  attend_event: 'user-check',
  mail_in: 'envelope',
}
