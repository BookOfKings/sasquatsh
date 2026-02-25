export interface UserProfile {
  id: string
  firebaseUid: string
  email: string
  displayName: string | null
  avatarUrl: string | null
  maxTravelMiles: number | null
  homeCity: string | null
  homeState: string | null
  homePostalCode: string | null
  bio: string | null
  favoriteGames: string[] | null
  preferredGameTypes: string[] | null
  isAdmin: boolean
  blockedUserIds: string[]
  createdAt: string
  updatedAt: string
  groups?: UserGroupMembership[]
  upcomingEvents?: UserUpcomingEvent[]
  stats?: UserStats
}

export interface BlockedUser {
  id: string
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
  displayName: string | null
  avatarUrl: string | null
  homeCity: string | null
  homeState: string | null
  bio: string | null
  favoriteGames: string[] | null
  preferredGameTypes: string[] | null
  createdAt: string
}

export interface UpdateProfileInput {
  displayName?: string | null
  avatarUrl?: string | null
  maxTravelMiles?: number | null
  homeCity?: string | null
  homeState?: string | null
  homePostalCode?: string | null
  bio?: string | null
  favoriteGames?: string[] | null
  preferredGameTypes?: string[] | null
}
