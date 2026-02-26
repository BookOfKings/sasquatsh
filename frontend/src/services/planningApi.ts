import type {
  PlanningSession,
  CreatePlanningSessionInput,
  PlanningResponseInput,
  SuggestGameInput,
  GameSuggestion,
  PlanningItem,
  AddPlanningItemInput,
} from '@/types/planning'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

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
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// List planning sessions for a group
export async function getGroupPlanningSessions(
  token: string,
  groupId: string
): Promise<PlanningSession[]> {
  return authenticatedRequest<PlanningSession[]>(
    `/planning?groupId=${groupId}`,
    token
  )
}

// Get planning sessions user is invited to
export async function getMyPlanningInvitations(
  token: string
): Promise<PlanningSession[]> {
  return authenticatedRequest<PlanningSession[]>('/planning?mine=true', token)
}

// Get single planning session with full details
export async function getPlanningSession(
  token: string,
  sessionId: string
): Promise<PlanningSession> {
  return authenticatedRequest<PlanningSession>(
    `/planning?id=${sessionId}`,
    token
  )
}

// Create new planning session
export async function createPlanningSession(
  token: string,
  data: CreatePlanningSessionInput
): Promise<PlanningSession> {
  return authenticatedRequest<PlanningSession>('/planning', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

// Submit availability response
export async function respondToPlanningSession(
  token: string,
  sessionId: string,
  data: PlanningResponseInput
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=respond`,
    token,
    {
      method: 'POST',
      body: JSON.stringify(data),
    }
  )
}

// Suggest a game
export async function suggestGame(
  token: string,
  sessionId: string,
  data: SuggestGameInput
): Promise<GameSuggestion> {
  return authenticatedRequest<GameSuggestion>(
    `/planning?id=${sessionId}&action=suggest-game`,
    token,
    {
      method: 'POST',
      body: JSON.stringify(data),
    }
  )
}

// Vote for a game
export async function voteForGame(
  token: string,
  sessionId: string,
  suggestionId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=vote-game&suggestionId=${suggestionId}`,
    token,
    {
      method: 'POST',
    }
  )
}

// Remove vote for a game
export async function unvoteGame(
  token: string,
  sessionId: string,
  suggestionId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=unvote-game&suggestionId=${suggestionId}`,
    token,
    {
      method: 'POST',
    }
  )
}

// Remove a game suggestion
export async function removeSuggestion(
  token: string,
  sessionId: string,
  suggestionId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=remove-suggestion&suggestionId=${suggestionId}`,
    token,
    {
      method: 'POST',
    }
  )
}

// Finalize planning session and create event
export async function finalizePlanningSession(
  token: string,
  sessionId: string,
  selectedDateId?: string,
  selectedGameId?: string
): Promise<{ eventId: string; message: string }> {
  return authenticatedRequest<{ eventId: string; message: string }>(
    `/planning?id=${sessionId}&action=finalize`,
    token,
    {
      method: 'PUT',
      body: JSON.stringify({ selectedDateId, selectedGameId }),
    }
  )
}

// Cancel planning session
export async function cancelPlanningSession(
  token: string,
  sessionId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=cancel`,
    token,
    {
      method: 'PUT',
    }
  )
}

// Delete planning session
export async function deletePlanningSession(
  token: string,
  sessionId: string
): Promise<void> {
  return authenticatedRequest<void>(`/planning?id=${sessionId}`, token, {
    method: 'DELETE',
  })
}

// ============ Item Management ============

// Add an item to bring
export async function addPlanningItem(
  token: string,
  sessionId: string,
  data: AddPlanningItemInput
): Promise<PlanningItem> {
  return authenticatedRequest<PlanningItem>(
    `/planning?id=${sessionId}&action=add-item`,
    token,
    {
      method: 'POST',
      body: JSON.stringify(data),
    }
  )
}

// Claim an item
export async function claimPlanningItem(
  token: string,
  sessionId: string,
  itemId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=claim-item&itemId=${itemId}`,
    token,
    {
      method: 'POST',
    }
  )
}

// Unclaim an item
export async function unclaimPlanningItem(
  token: string,
  sessionId: string,
  itemId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=unclaim-item&itemId=${itemId}`,
    token,
    {
      method: 'POST',
    }
  )
}

// Remove an item
export async function removePlanningItem(
  token: string,
  sessionId: string,
  itemId: string
): Promise<void> {
  return authenticatedRequest<void>(
    `/planning?id=${sessionId}&action=remove-item&itemId=${itemId}`,
    token,
    {
      method: 'POST',
    }
  )
}
