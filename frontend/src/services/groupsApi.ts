import type {
  Group,
  GroupSummary,
  CreateGroupInput,
  UpdateGroupInput,
  GroupSearchFilter,
  GroupMember,
  JoinRequest,
  GroupInvitation,
  CreateInvitationInput,
  InvitationPreview,
  MemberRole,
} from '@/types/groups'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests to Edge Functions
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

// Public endpoints (Supabase requires anon key)
export async function getPublicGroups(filter?: GroupSearchFilter): Promise<GroupSummary[]> {
  const params = new URLSearchParams()

  if (filter?.search) params.set('search', filter.search)
  if (filter?.groupType) params.set('type', filter.groupType)
  if (filter?.city) params.set('city', filter.city)
  if (filter?.state) params.set('state', filter.state)

  const queryString = params.toString()
  const url = `${FUNCTIONS_URL}/groups${queryString ? `?${queryString}` : ''}`

  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error('Failed to fetch groups')
  }

  return response.json() as Promise<GroupSummary[]>
}

export async function getGroup(idOrSlug: string): Promise<Group> {
  // Determine if it's a UUID or slug
  const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(idOrSlug)
  const param = isUuid ? 'id' : 'slug'

  const response = await fetch(`${FUNCTIONS_URL}/groups?${param}=${idOrSlug}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

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

export async function getMyGroups(token: string): Promise<(GroupSummary & { userRole: MemberRole })[]> {
  return authenticatedRequest<(GroupSummary & { userRole: MemberRole })[]>('/groups?mine=true', token)
}

// Member management
export async function getGroupMembers(
  token: string,
  groupId: string
): Promise<GroupMember[]> {
  return authenticatedRequest<GroupMember[]>(`/groups?id=${groupId}&include=members`, token)
}

export async function removeMember(
  token: string,
  groupId: string,
  userId: string
): Promise<void> {
  return authenticatedRequest<void>(`/groups?id=${groupId}&action=remove&userId=${userId}`, token, {
    method: 'DELETE',
  })
}

export async function changeMemberRole(
  token: string,
  groupId: string,
  userId: string,
  role: MemberRole
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=role&userId=${userId}`, token, {
    method: 'PUT',
    body: JSON.stringify({ role }),
  })
}

export async function transferOwnership(
  token: string,
  groupId: string,
  newOwnerId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=transfer&userId=${newOwnerId}`, token, {
    method: 'PUT',
  })
}

// Join requests
export async function requestToJoin(
  token: string,
  groupId: string,
  message?: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=request`, token, {
    method: 'POST',
    body: JSON.stringify({ message }),
  })
}

export async function getJoinRequests(
  token: string,
  groupId: string
): Promise<JoinRequest[]> {
  return authenticatedRequest<JoinRequest[]>(`/groups?id=${groupId}&include=requests`, token)
}

export async function approveRequest(
  token: string,
  groupId: string,
  userId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=approve&userId=${userId}`, token, {
    method: 'POST',
  })
}

export async function rejectRequest(
  token: string,
  groupId: string,
  userId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/groups?id=${groupId}&action=reject&userId=${userId}`, token, {
    method: 'POST',
  })
}

// Invitations
export async function createInvitation(
  token: string,
  groupId: string,
  options?: CreateInvitationInput
): Promise<GroupInvitation> {
  return authenticatedRequest<GroupInvitation>(`/groups?id=${groupId}&action=invite`, token, {
    method: 'POST',
    body: JSON.stringify(options || {}),
  })
}

export async function getInvitations(
  token: string,
  groupId: string
): Promise<GroupInvitation[]> {
  return authenticatedRequest<GroupInvitation[]>(`/groups?id=${groupId}&include=invitations`, token)
}

export async function revokeInvitation(
  token: string,
  groupId: string,
  invitationId: string
): Promise<void> {
  return authenticatedRequest<void>(`/groups?id=${groupId}&action=revoke-invite&inviteId=${invitationId}`, token, {
    method: 'DELETE',
  })
}

export async function getInvitationPreview(inviteCode: string): Promise<InvitationPreview> {
  const response = await fetch(`${FUNCTIONS_URL}/groups?action=preview-invite&code=${inviteCode}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
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

  return response.json() as Promise<InvitationPreview>
}

export async function acceptInvitation(
  token: string,
  inviteCode: string
): Promise<{ message: string; groupId: string; groupName: string }> {
  return authenticatedRequest<{ message: string; groupId: string; groupName: string }>(
    `/groups?action=accept-invite&code=${inviteCode}`,
    token,
    { method: 'POST' }
  )
}
