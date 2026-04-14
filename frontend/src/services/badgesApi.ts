const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface Badge {
  id: number
  slug: string
  name: string
  description: string
  icon_svg: string | null
  category: string
  tier: string
  requirement_type: string
  requirement_count: number
  sort_order: number
}

export interface UserBadge {
  id: string
  user_id: string
  badge_id: number
  earned_at: string
  is_pinned: boolean
  badge: Badge
}

async function authRequest<T>(path: string, token: string, options?: RequestInit): Promise<T> {
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
    try { const d = await response.json(); if (d?.error) message = d.error } catch {}
    throw new Error(message)
  }
  return response.json() as Promise<T>
}

export async function getAllBadges(): Promise<Badge[]> {
  const response = await fetch(`${FUNCTIONS_URL}/badges`, {
    headers: { 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` },
  })
  if (!response.ok) throw new Error('Failed to fetch badges')
  const data = await response.json()
  return data.badges
}

export async function getMyBadges(token: string): Promise<UserBadge[]> {
  const data = await authRequest<{ badges: UserBadge[] }>('/badges?action=my-badges', token)
  return data.badges
}

export async function getUserBadges(userId: string): Promise<UserBadge[]> {
  const response = await fetch(`${FUNCTIONS_URL}/badges?action=user&userId=${userId}`, {
    headers: { 'Authorization': `Bearer ${SUPABASE_ANON_KEY}` },
  })
  if (!response.ok) throw new Error('Failed to fetch user badges')
  const data = await response.json()
  return data.badges
}

export async function computeMyBadges(token: string): Promise<{ badges: UserBadge[]; newlyEarned: number }> {
  return authRequest('/badges?action=compute', token, { method: 'POST' })
}

export async function togglePinBadge(token: string, badgeId: number): Promise<{ pinned: boolean }> {
  return authRequest(`/badges?action=pin&badgeId=${badgeId}`, token, { method: 'PUT' })
}
