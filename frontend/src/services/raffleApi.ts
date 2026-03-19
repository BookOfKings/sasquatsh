// Raffle API Service

import type {
  Raffle,
  RaffleWithDetails,
  RaffleEntryWithUser,
  CreateRaffleInput,
  UpdateRaffleInput,
  MailInEntryInput,
  RaffleUser,
} from '@/types/raffle'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper for authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string | null,
  options?: RequestInit
): Promise<T> {
  const headers: Record<string, string> = {
    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    'Content-Type': 'application/json',
  }

  if (token) {
    headers['X-Firebase-Token'] = token
  }

  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      ...headers,
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  return response.json() as Promise<T>
}

// ============================================
// Public API
// ============================================

// Get the currently active raffle
export async function getActiveRaffle(
  token?: string | null
): Promise<RaffleWithDetails | null> {
  const response = await authenticatedRequest<{ raffle: RaffleWithDetails | null }>(
    '/raffle',
    token || null
  )
  return response.raffle
}

// Get a specific raffle by ID
export async function getRaffle(
  raffleId: string,
  token?: string | null
): Promise<RaffleWithDetails | null> {
  const response = await authenticatedRequest<{ raffle: RaffleWithDetails }>(
    `/raffle?id=${raffleId}`,
    token || null
  )
  return response.raffle
}

// Submit a mail-in entry
export async function submitMailInEntry(
  input: MailInEntryInput,
  token?: string | null
): Promise<{ entryId: string; message: string }> {
  const response = await authenticatedRequest<{
    success: boolean
    entryId: string
    message: string
  }>(
    '/raffle?action=mail-in',
    token || null,
    {
      method: 'POST',
      body: JSON.stringify(input),
    }
  )
  return { entryId: response.entryId, message: response.message }
}

// ============================================
// Admin API
// ============================================

// List all raffles (admin)
export async function getAllRaffles(
  token: string
): Promise<RaffleWithDetails[]> {
  const response = await authenticatedRequest<{ raffles: RaffleWithDetails[] }>(
    '/raffle?admin=list',
    token
  )
  return response.raffles
}

// Get raffle entries (admin)
export async function getRaffleEntries(
  token: string,
  raffleId: string
): Promise<RaffleEntryWithUser[]> {
  const response = await authenticatedRequest<{ entries: RaffleEntryWithUser[] }>(
    `/raffle?admin=entries&id=${raffleId}`,
    token
  )
  return response.entries
}

// Create a new raffle (admin)
export async function createRaffle(
  token: string,
  input: CreateRaffleInput
): Promise<Raffle> {
  const response = await authenticatedRequest<{ success: boolean; raffle: Raffle }>(
    '/raffle',
    token,
    {
      method: 'POST',
      body: JSON.stringify(input),
    }
  )
  return response.raffle
}

// Update a raffle (admin)
export async function updateRaffle(
  token: string,
  raffleId: string,
  input: UpdateRaffleInput
): Promise<Raffle> {
  const response = await authenticatedRequest<{ success: boolean; raffle: Raffle }>(
    `/raffle?id=${raffleId}`,
    token,
    {
      method: 'PUT',
      body: JSON.stringify(input),
    }
  )
  return response.raffle
}

// Delete a raffle (admin)
export async function deleteRaffle(
  token: string,
  raffleId: string
): Promise<void> {
  await authenticatedRequest<{ success: boolean }>(
    `/raffle?id=${raffleId}`,
    token,
    { method: 'DELETE' }
  )
}

// Select winner for a raffle (admin)
export async function selectRaffleWinner(
  token: string,
  raffleId: string
): Promise<{
  winner: RaffleUser & { email?: string }
  totalEntries: number
  winnerEntries: number
}> {
  const response = await authenticatedRequest<{
    success: boolean
    winner: RaffleUser & { email?: string }
    totalEntries: number
    winnerEntries: number
  }>(
    `/raffle?action=select-winner&id=${raffleId}`,
    token,
    { method: 'POST' }
  )
  return {
    winner: response.winner,
    totalEntries: response.totalEntries,
    winnerEntries: response.winnerEntries,
  }
}

// Mark winner as notified (admin)
export async function markWinnerNotified(
  token: string,
  raffleId: string
): Promise<Raffle> {
  return updateRaffle(token, raffleId, {
    winnerNotifiedAt: new Date().toISOString(),
  })
}

// Mark prize as claimed (admin)
export async function markPrizeClaimed(
  token: string,
  raffleId: string
): Promise<Raffle> {
  return updateRaffle(token, raffleId, {
    winnerClaimedAt: new Date().toISOString(),
  })
}
