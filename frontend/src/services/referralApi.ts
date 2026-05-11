const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface ReferrerCheck {
  valid: boolean
  reason?: string
  displayName?: string | null
  avatarUrl?: string | null
  username?: string
}

export interface ReferralUser {
  id: string
  username: string
  display_name: string | null
  avatar_url: string | null
}

export interface ReferralEntry {
  id: string
  status: string
  createdAt: string
  referred: ReferralUser | null
}

export interface ReferralReward {
  referralCountAtReward: number
  proExpiresAt: string
  createdAt: string
}

export interface ReferralStats {
  totalReferrals: number
  progressToNext: number
  nextMilestone: number
  referrals: ReferralEntry[]
  rewards: ReferralReward[]
  activeProReward: { expiresAt: string } | null
}

export async function checkReferrer(username: string): Promise<ReferrerCheck> {
  const response = await fetch(
    `${FUNCTIONS_URL}/referrals?action=check-referrer&username=${encodeURIComponent(username)}`,
    {
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
    }
  )

  if (!response.ok) throw new Error('Failed to check referrer')
  return response.json()
}

export async function getReferralStats(token: string): Promise<ReferralStats> {
  const response = await fetch(`${FUNCTIONS_URL}/referrals?action=stats`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) throw new Error('Failed to fetch referral stats')
  return response.json()
}
