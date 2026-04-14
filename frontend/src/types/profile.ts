export interface UserProfile {
  id: string
  firebaseUid: string
  email: string
  username: string
  displayName: string | null
  avatarUrl: string | null
  birthYear: number | null
  maxTravelMiles: number | null
  homeCity: string | null
  homeState: string | null
  homePostalCode: string | null
  activeCity: string | null
  activeState: string | null
  activeLocationExpiresAt: string | null
  activeEventLocationId: string | null
  activeLocationHall: string | null
  activeLocationRoom: string | null
  activeLocationTable: string | null
  timezone: string | null
  bio: string | null
  favoriteGames: string[] | null
  preferredGameTypes: string[] | null
  isAdmin: boolean
  isFoundingMember: boolean
  blockedUserIds: string[]
  collectionVisibility: 'public' | 'private'
  createdAt: string
  updatedAt: string
  authProvider?: 'password' | 'google.com' | 'facebook.com' | string
  passwordChangedAt?: string | null
  groups?: UserGroupMembership[]
  upcomingEvents?: UserUpcomingEvent[]
  stats?: UserStats
}

export interface BlockedUser {
  id: string
  username: string
  displayName: string | null
  avatarUrl: string | null
}

export interface UserGroupMembership {
  role: string
  joinedAt: string
  group: {
    id: string
    name: string
    slug: string
    logoUrl: string | null
    groupType: string
  } | null
}

export interface UserUpcomingEvent {
  status: string
  event: {
    id: string
    title: string
    eventDate: string
    startTime: string
    city: string | null
    state: string | null
  } | null
}

export interface UserStats {
  hostedCount: number
  attendedCount: number
  groupCount: number
}

export interface PublicProfile {
  id: string
  username: string
  displayName: string | null
  avatarUrl: string | null
  homeCity: string | null
  homeState: string | null
  bio: string | null
  favoriteGames: string[] | null
  preferredGameTypes: string[] | null
  isFoundingMember: boolean
  collectionVisibility: 'public' | 'private'
  createdAt: string
}

export interface UpdateProfileInput {
  username?: string
  displayName?: string | null
  avatarUrl?: string | null
  birthYear?: number | null
  maxTravelMiles?: number | null
  homeCity?: string | null
  homeState?: string | null
  homePostalCode?: string | null
  activeCity?: string | null
  activeState?: string | null
  activeLocationExpiresAt?: string | null
  activeEventLocationId?: string | null
  activeLocationHall?: string | null
  activeLocationRoom?: string | null
  activeLocationTable?: string | null
  timezone?: string | null
  bio?: string | null
  favoriteGames?: string[] | null
  preferredGameTypes?: string[] | null
  collectionVisibility?: 'public' | 'private'
}
