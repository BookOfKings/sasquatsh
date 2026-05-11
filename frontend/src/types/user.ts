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
  subscriptionOverrideExpiresAt?: string
  accountStatus?: 'active' | 'suspended' | 'banned'
  isAdmin: boolean
  isFoundingMember: boolean
  blockedUserIds: string[]
  createdAt: string
  authProvider?: 'password' | 'google.com' | 'facebook.com' | string
}

// Helper to get the effective tier (override takes precedence if not expired)
export function getEffectiveTier(user: User): 'free' | 'basic' | 'pro' | 'premium' {
  if (user.subscriptionOverrideTier) {
    if (!user.subscriptionOverrideExpiresAt || new Date(user.subscriptionOverrideExpiresAt) > new Date()) {
      return user.subscriptionOverrideTier
    }
  }
  return user.subscriptionTier
}

export interface UserSummary {
  id: string
  username: string
  displayName?: string
  avatarUrl?: string
  isFoundingMember?: boolean
  isAdmin?: boolean
}
