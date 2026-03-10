export interface User {
  id: string
  email: string
  username: string
  displayName?: string
  avatarUrl?: string
  subscriptionTier: 'free' | 'basic' | 'pro' | 'premium'
  subscriptionExpiresAt?: string
  subscriptionStatus?: 'active' | 'past_due' | 'canceled' | 'incomplete'
  subscriptionOverrideTier?: 'free' | 'basic' | 'pro' | 'premium'
  accountStatus?: 'active' | 'suspended' | 'banned'
  isAdmin: boolean
  isFoundingMember: boolean
  blockedUserIds: string[]
  createdAt: string
}

// Helper to get the effective tier (override takes precedence)
export function getEffectiveTier(user: User): 'free' | 'basic' | 'pro' | 'premium' {
  return user.subscriptionOverrideTier || user.subscriptionTier
}

export interface UserSummary {
  id: string
  username: string
  displayName?: string
  avatarUrl?: string
  isFoundingMember?: boolean
  isAdmin?: boolean
}
