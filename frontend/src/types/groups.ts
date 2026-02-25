export type GroupType = 'geographic' | 'interest' | 'both'
export type MemberRole = 'owner' | 'admin' | 'member'

export interface Group {
  id: string
  name: string
  slug: string
  description: string | null
  logoUrl: string | null
  coverImageUrl: string | null
  groupType: GroupType
  locationCity: string | null
  locationState: string | null
  locationRadiusMiles: number | null
  isPublic: boolean
  createdByUserId: string
  createdAt: string
  updatedAt: string
  memberCount?: number
  creator?: {
    id: string
    displayName: string | null
    avatarUrl: string | null
  }
}

export interface GroupSummary {
  id: string
  name: string
  slug: string
  description: string | null
  logoUrl: string | null
  groupType: GroupType
  locationCity: string | null
  locationState: string | null
  isPublic: boolean
  memberCount: number
}

export interface GroupMembership {
  id: string
  groupId: string
  userId: string
  role: MemberRole
  joinedAt: string
  user?: {
    id: string
    displayName: string | null
    avatarUrl: string | null
  }
}

export interface RecurringGame {
  id: string
  groupId: string
  title: string
  description: string | null
  dayOfWeek: number // 0-6, Sunday = 0
  startTime: string
  durationMinutes: number
  maxPlayers: number
  locationDetails: string | null
  isActive: boolean
  createdAt: string
}

export interface CreateGroupInput {
  name: string
  description?: string
  groupType: GroupType
  locationCity?: string
  locationState?: string
  locationRadiusMiles?: number
  isPublic?: boolean
}

export interface UpdateGroupInput {
  name?: string
  description?: string
  groupType?: GroupType
  locationCity?: string
  locationState?: string
  locationRadiusMiles?: number
  isPublic?: boolean
}

export interface GroupSearchFilter {
  search?: string
  groupType?: GroupType
  city?: string
  state?: string
}
