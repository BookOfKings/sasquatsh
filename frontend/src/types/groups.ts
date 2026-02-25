export type GroupType = 'geographic' | 'interest' | 'both'
export type MemberRole = 'owner' | 'admin' | 'member'
export type JoinPolicy = 'open' | 'request' | 'invite_only'

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
  joinPolicy: JoinPolicy
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
  joinPolicy: JoinPolicy
  memberCount: number
  userRole?: MemberRole  // Present when fetching user's groups
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
  joinPolicy?: JoinPolicy
}

export interface UpdateGroupInput {
  name?: string
  description?: string
  groupType?: GroupType
  locationCity?: string
  locationState?: string
  locationRadiusMiles?: number
  joinPolicy?: JoinPolicy
  logoUrl?: string | null
}

export interface GroupSearchFilter {
  search?: string
  groupType?: GroupType
  city?: string
  state?: string
}

// Member management types
export interface GroupMember {
  id: string
  userId: string
  displayName: string | null
  email: string | null
  avatarUrl: string | null
  role: MemberRole
  joinedAt: string
}

export interface JoinRequest {
  id: string
  userId: string
  displayName: string | null
  email: string | null
  avatarUrl: string | null
  message: string | null
  status: 'pending' | 'approved' | 'rejected'
  createdAt: string
}

export interface GroupInvitation {
  id: string
  inviteCode: string
  invitedByDisplayName: string | null
  invitedEmail: string | null
  maxUses: number | null
  usesCount: number
  expiresAt: string | null
  createdAt: string
}

export interface CreateInvitationInput {
  email?: string
  phone?: string
  maxUses?: number
  expiresInDays?: number
}

export interface InvitationPreview {
  inviteCode: string
  invitedEmail: string | null
  group: {
    id: string
    name: string
    slug: string
    description: string | null
    logoUrl: string | null
    groupType: GroupType
    locationCity: string | null
    locationState: string | null
    joinPolicy: JoinPolicy
  }
  invitedBy: {
    id: string
    displayName: string | null
    avatarUrl: string | null
  }
  expiresAt: string | null
}
