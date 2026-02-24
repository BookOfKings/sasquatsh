export interface User {
  id: string
  email: string
  displayName?: string
  avatarUrl?: string
  subscriptionTier: 'free' | 'pro' | 'premium'
  subscriptionExpiresAt?: string
  createdAt: string
}

export interface UserSummary {
  id: string
  displayName?: string
  avatarUrl?: string
}
