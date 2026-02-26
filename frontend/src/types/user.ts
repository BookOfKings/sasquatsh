export interface User {
  id: string
  email: string
  username: string
  displayName?: string
  avatarUrl?: string
  subscriptionTier: 'free' | 'pro' | 'premium'
  subscriptionExpiresAt?: string
  isAdmin: boolean
  blockedUserIds: string[]
  createdAt: string
}

export interface UserSummary {
  id: string
  username: string
  displayName?: string
  avatarUrl?: string
}
