const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface ShareLinkPreview {
  linkType: 'session' | 'group_recurring'
  group: {
    id: string
    name: string
    slug: string
    logoUrl: string | null
    city: string | null
    state: string | null
  } | null
  target: {
    type: 'planning_session' | 'event'
    id: string
    title: string
  } | null
  invitedBy: {
    displayName: string | null
    avatarUrl: string | null
  } | null
}

export interface ShareLinkAcceptResult {
  alreadyUsed: boolean
  groupSlug: string | null
  target: {
    type: 'planning_session' | 'event' | 'group'
    id?: string
  } | null
}

export interface ShareLink {
  id: string
  invite_code: string
  link_type: string
  group_id: string
  uses_count: number
  max_uses: number | null
  is_active: boolean
  expires_at: string | null
  created_at: string
  url: string
}

export async function getShareLinkPreview(code: string): Promise<ShareLinkPreview> {
  const response = await fetch(`${FUNCTIONS_URL}/share-links?code=${code}`, {
    headers: { 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` },
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.error || 'Invalid invite link')
  }
  return response.json()
}

export async function acceptShareLink(token: string, code: string): Promise<ShareLinkAcceptResult> {
  const response = await fetch(`${FUNCTIONS_URL}/share-links?code=${code}&action=accept`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
    },
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.error || 'Failed to accept invite')
  }
  return response.json()
}

export async function createShareLink(
  token: string,
  params: {
    groupId: string
    linkType: 'session' | 'group_recurring'
    planningSessionId?: string
    eventId?: string
    maxUses?: number
    expiresInDays?: number
  }
): Promise<ShareLink> {
  const response = await fetch(`${FUNCTIONS_URL}/share-links?action=create`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(params),
  })
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.error || 'Failed to create link')
  }
  return response.json()
}
