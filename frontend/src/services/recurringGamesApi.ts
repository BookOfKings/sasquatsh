import type { RecurringGame, CreateRecurringGameInput, UpdateRecurringGameInput } from '@/types/groups'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    let code: string | undefined
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
      if (data?.code) code = data.code
      if (code) {
        const err = new Error(message) as Error & { code?: string; data?: unknown }
        err.code = code
        err.data = data
        throw err
      }
    } catch (e) {
      if (e instanceof Error && (e as Error & { code?: string }).code) {
        throw e
      }
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// Get all recurring games for a group
export async function getRecurringGames(
  token: string,
  groupId: string
): Promise<RecurringGame[]> {
  return authenticatedRequest<RecurringGame[]>(
    `/recurring-games?groupId=${encodeURIComponent(groupId)}`,
    token,
    { method: 'GET' }
  )
}

// Create a new recurring game
export async function createRecurringGame(
  token: string,
  groupId: string,
  data: CreateRecurringGameInput
): Promise<RecurringGame> {
  return authenticatedRequest<RecurringGame>('/recurring-games', token, {
    method: 'POST',
    body: JSON.stringify({ ...data, groupId }),
  })
}

// Update an existing recurring game
export async function updateRecurringGame(
  token: string,
  id: string,
  data: UpdateRecurringGameInput
): Promise<RecurringGame> {
  return authenticatedRequest<RecurringGame>(
    `/recurring-games?id=${encodeURIComponent(id)}`,
    token,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    }
  )
}

// Delete a recurring game
export async function deleteRecurringGame(
  token: string,
  id: string,
  deleteFutureEvents?: boolean
): Promise<void> {
  const params = new URLSearchParams({ id })
  if (deleteFutureEvents) {
    params.set('deleteFutureEvents', 'true')
  }
  return authenticatedRequest<void>(
    `/recurring-games?${params.toString()}`,
    token,
    { method: 'DELETE' }
  )
}
