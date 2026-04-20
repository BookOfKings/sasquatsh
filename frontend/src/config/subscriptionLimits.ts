// Subscription tier limits and features configuration

export type SubscriptionTier = 'free' | 'basic' | 'pro' | 'premium'

export interface TierLimits {
  gamesPerEvent: number
  maxGroups: number
  maxRecurringGamesPerGroup: number
  shelfScansPerMonth: number // AI shelf recognition scans per month
  features: {
    tableInfo: boolean      // Can specify hall/room/table per game
    planning: boolean       // Access to game night planning feature
    items: boolean          // Access to items to bring feature
    chat: boolean           // Access to chat feature
    recurringGames: boolean // Access to recurring games feature
    showAds: boolean        // Show upgrade advertisements
  }
}

export const TIER_LIMITS: Record<SubscriptionTier, TierLimits> = {
  free: {
    gamesPerEvent: 1,
    maxGroups: 1,
    maxRecurringGamesPerGroup: 0,
    shelfScansPerMonth: 5,
    features: {
      tableInfo: false,
      planning: false,
      items: false,
      chat: false,
      recurringGames: false,
      showAds: true,
    },
  },
  basic: {
    gamesPerEvent: 5,
    maxGroups: 5,
    maxRecurringGamesPerGroup: 1,
    shelfScansPerMonth: 20,
    features: {
      tableInfo: true,
      planning: true,
      items: true,
      chat: true,
      recurringGames: true,
      showAds: false,
    },
  },
  pro: {
    gamesPerEvent: 10,
    maxGroups: 10,
    maxRecurringGamesPerGroup: Infinity,
    shelfScansPerMonth: Infinity,
    features: {
      tableInfo: true,
      planning: true,
      items: true,
      chat: true,
      recurringGames: true,
      showAds: false,
    },
  },
  premium: {
    // Premium is essentially unlimited (legacy tier or special cases)
    gamesPerEvent: Infinity,
    maxGroups: Infinity,
    maxRecurringGamesPerGroup: Infinity,
    shelfScansPerMonth: Infinity,
    features: {
      tableInfo: true,
      planning: true,
      items: true,
      chat: true,
      recurringGames: true,
      showAds: false,
    },
  },
}

export const TIER_PRICES = {
  free: 0,
  basic: 4.99,
  pro: 7.99,
  premium: 0, // Custom pricing
}

export const TIER_NAMES: Record<SubscriptionTier, string> = {
  free: 'Free',
  basic: 'Basic',
  pro: 'Pro',
  premium: 'Premium',
}

// Feature descriptions for pricing page
export const TIER_FEATURES: Record<SubscriptionTier, string[]> = {
  free: [
    'Host 1 game per event',
    'Create 1 group',
    'Basic event management',
    'Join unlimited events',
  ],
  basic: [
    'Host up to 5 games per event',
    'Create up to 5 groups',
    '1 recurring game per group',
    'Specify table locations per game',
    'Game night planning feature',
    'Items to bring lists',
    'Event chat',
    'No ads',
  ],
  pro: [
    'Host up to 10 games per event',
    'Create up to 10 groups',
    'Unlimited recurring games',
    'Specify table locations per game',
    'Game night planning feature',
    'Items to bring lists',
    'Event chat',
    'No ads',
  ],
  premium: [
    'Unlimited games per event',
    'Unlimited groups',
    'All Pro features',
    'Custom branding (coming soon)',
    'API access (coming soon)',
  ],
}

// Helper function to get limits for a tier
export function getLimits(tier: SubscriptionTier): TierLimits {
  return TIER_LIMITS[tier] || TIER_LIMITS.free
}

// Helper to check if a feature is available
export function hasFeature(tier: SubscriptionTier, feature: keyof TierLimits['features']): boolean {
  return getLimits(tier).features[feature]
}

// Helper to check if user can create more groups
export function canCreateGroup(tier: SubscriptionTier, currentCount: number): boolean {
  const limit = getLimits(tier).maxGroups
  return currentCount < limit
}

// Helper to check if user can add more games to event
export function canAddGame(tier: SubscriptionTier, currentCount: number): boolean {
  const limit = getLimits(tier).gamesPerEvent
  return currentCount < limit
}

// Helper to check if user can create more recurring games in a group
export function canCreateRecurringGame(tier: SubscriptionTier, currentCount: number): boolean {
  const limit = getLimits(tier).maxRecurringGamesPerGroup
  return currentCount < limit
}
