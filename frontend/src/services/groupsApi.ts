import type {
  Group,
  GroupSummary,
  CreateGroupInput,
  UpdateGroupInput,
  GroupSearchFilter,
} from '@/types/groups'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL

// Helper to make authenticated requests to Edge Functions
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
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

// Public endpoints
export async function getPublicGroups(filter?: GroupSearchFilter): Promise<GroupSummary[]> {
  const params = new URLSearchParams()

  if (filter?.search) params.set('search', filter.search)
  if (filter?.groupType) params.set('type', filter.groupType)
  if (filter?.city) params.set('city', filter.city)
  if (filter?.state) params.set('state', filter.state)

  const queryString = params.toString()
  const url = `${FUNCTIONS_URL}/groups${queryString ? `?${queryString}` : ''}`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error('Failed to fetch groups')
  }

  return response.json() as Promise<GroupSummary[]>
}

export async function getGroup(idOrSlug: string): Promise<Group> {
  // Determine if it's a UUID or slug
  const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(idOrSlug)
  const param = isUuid ? 'id' : 'slug'

  const response = await fetch(`${FUNCTIONS_URL}/groups?${param}=${idOrSlug}`)

  if (!response.ok) {
    throw new Error('Group not found')
  }

  return response.json() as Promise<Group>
}

// Authenticated endpoints
export async function createGroup(
  token: string,
  data: CreateGroupInput
): Promise<Group> {
  return authenticatedRequest<Group>('/groups', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateGroup(
  token: string,
  id: string,
  data: UpdateGroupInput
): Promise<Group> {
  return authenticatedRequest<Group>(`/groups?id=${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteGroup(token: string, id: string): Promise<void> {
  return authenticatedRequest<void>(`/groups?id=${id}`, token, {
    method: 'DELETE',
  })
}

export async function joinGroup(
  token: string,
  groupId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=join`, token, {
    method: 'POST',
  })
}

export async function leaveGroup(token: string, groupId: string): Promise<void> {
  return authenticatedRequest<void>(`/groups?id=${groupId}&action=leave`, token, {
    method: 'DELETE',
  })
}

export async function getMyGroups(token: string): Promise<GroupSummary[]> {
  return authenticatedRequest<GroupSummary[]>('/groups?type=mine', token)
}
