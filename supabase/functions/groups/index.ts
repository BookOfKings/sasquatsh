import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken, escapeFilterValue } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Helper to generate URL-friendly slug
function generateSlug(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
}

// Generate random invite code
function generateInviteCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 12; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

// Transform database row to GroupSummary
function toGroupSummary(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    name: row.name as string,
    slug: row.slug as string,
    description: row.description as string | null,
    logoUrl: row.logo_url as string | null,
    groupType: row.group_type as string,
    locationCity: row.location_city as string | null,
    locationState: row.location_state as string | null,
    joinPolicy: row.join_policy as string,
    memberCount: (row.memberships as { count: number }[])?.[0]?.count ?? 0,
  }
}

// Transform database row to full Group
function toGroup(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    name: row.name as string,
    slug: row.slug as string,
    description: row.description as string | null,
    logoUrl: row.logo_url as string | null,
    coverImageUrl: row.cover_image_url as string | null,
    groupType: row.group_type as string,
    locationCity: row.location_city as string | null,
    locationState: row.location_state as string | null,
    locationRadiusMiles: row.location_radius_miles as number | null,
    joinPolicy: row.join_policy as string,
    createdByUserId: row.created_by_user_id as string,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    memberCount: (row.memberships as unknown[])?.length ?? 0,
    creator: row.creator
      ? {
          id: (row.creator as Record<string, unknown>).id as string,
          displayName: (row.creator as Record<string, unknown>).display_name as string | null,
          avatarUrl: (row.creator as Record<string, unknown>).avatar_url as string | null,
          isFoundingMember: (row.creator as Record<string, unknown>).is_founding_member as boolean | undefined,
          isAdmin: (row.creator as Record<string, unknown>).is_admin as boolean | undefined,
          subscriptionTier: (row.creator as Record<string, unknown>).subscription_tier as string | undefined,
          subscriptionOverrideTier: (row.creator as Record<string, unknown>).subscription_override_tier as string | undefined,
        }
      : null,
  }
}

// Transform member row
function toGroupMember(row: Record<string, unknown>) {
  const user = row.user as Record<string, unknown>
  return {
    id: row.id as string,
    userId: row.user_id as string,
    displayName: user?.display_name as string | null,
    username: user?.username as string | null,
    email: user?.email as string | null,
    avatarUrl: user?.avatar_url as string | null,
    isFoundingMember: user?.is_founding_member as boolean | undefined,
    isAdmin: user?.is_admin as boolean | undefined,
    role: row.role as string,
    joinedAt: row.joined_at as string,
  }
}

// Transform join request row
function toJoinRequest(row: Record<string, unknown>) {
  const user = row.user as Record<string, unknown>
  return {
    id: row.id as string,
    userId: row.user_id as string,
    displayName: user?.display_name as string | null,
    username: user?.username as string | null,
    email: user?.email as string | null,
    avatarUrl: user?.avatar_url as string | null,
    isFoundingMember: user?.is_founding_member as boolean | undefined,
    isAdmin: user?.is_admin as boolean | undefined,
    message: row.message as string | null,
    status: row.status as string,
    createdAt: row.created_at as string,
  }
}

// Transform invitation row
function toInvitation(row: Record<string, unknown>) {
  const invitedBy = row.invited_by as Record<string, unknown>
  return {
    id: row.id as string,
    inviteCode: row.invite_code as string,
    invitedByDisplayName: invitedBy?.display_name as string | null,
    invitedEmail: row.invited_email as string | null,
    maxUses: row.max_uses as number | null,
    usesCount: row.uses_count as number,
    expiresAt: row.expires_at as string | null,
    createdAt: row.created_at as string,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const groupId = url.searchParams.get('id')
  const slug = url.searchParams.get('slug')
  const action = url.searchParams.get('action')
  const include = url.searchParams.get('include')
  const targetUserId = url.searchParams.get('userId')
  const inviteCode = url.searchParams.get('code')
  const inviteId = url.searchParams.get('inviteId')

  // GET - List groups or get single group
  if (req.method === 'GET') {
    // Preview invitation (no auth required)
    if (action === 'preview-invite' && inviteCode) {
      const { data: invitation } = await supabase
        .from('group_invitations')
        .select(`
          id, invite_code, invited_email, max_uses, uses_count, expires_at, created_at,
          invited_by:users!invited_by_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
          group:groups!group_id(id, name, slug, description, logo_url, group_type, location_city, location_state, join_policy)
        `)
        .eq('invite_code', inviteCode)
        .single()

      if (!invitation) {
        return errorResponse('Invalid invitation code', 404)
      }

      // Check expiry
      if (invitation.expires_at && new Date(invitation.expires_at) < new Date()) {
        return errorResponse('This invitation has expired', 400)
      }

      // Check max uses
      if (invitation.max_uses && invitation.uses_count >= invitation.max_uses) {
        return errorResponse('This invitation has reached its maximum uses', 400)
      }

      const group = invitation.group as Record<string, unknown>
      const invitedBy = invitation.invited_by as Record<string, unknown>

      return jsonResponse({
        inviteCode: invitation.invite_code,
        invitedEmail: invitation.invited_email,
        group: {
          id: group.id,
          name: group.name,
          slug: group.slug,
          description: group.description,
          logoUrl: group.logo_url,
          groupType: group.group_type,
          locationCity: group.location_city,
          locationState: group.location_state,
          joinPolicy: group.join_policy,
        },
        invitedBy: {
          id: invitedBy.id,
          displayName: invitedBy.display_name,
          avatarUrl: invitedBy.avatar_url,
          isFoundingMember: invitedBy.is_founding_member,
          isAdmin: invitedBy.is_admin,
        },
        expiresAt: invitation.expires_at,
      })
    }

    // Get user's pending invitations (requires auth)
    if (action === 'my-invitations') {
      const token = getFirebaseToken(req)
      if (!token) {
        return errorResponse('Authentication required', 401)
      }

      const firebaseUser = await verifyFirebaseToken(token)
      if (!firebaseUser) {
        return errorResponse('Invalid Firebase token', 401)
      }

      const { data: dbUser } = await supabase
        .from('users')
        .select('id')
        .eq('firebase_uid', firebaseUser.uid)
        .single()

      if (!dbUser) {
        return errorResponse('User not found', 404)
      }

      // Get pending invitations for this user
      const { data: invitations, error } = await supabase
        .from('group_invitations')
        .select(`
          id, invite_code, created_at, expires_at, status,
          invited_by:users!invited_by_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
          group:groups!group_id(id, name, slug, description, logo_url, group_type, location_city, location_state, memberships:group_memberships(count))
        `)
        .eq('invited_user_id', dbUser.id)
        .eq('status', 'pending')
        .order('created_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(invitations.map(inv => {
        const group = inv.group as Record<string, unknown>
        const invitedBy = inv.invited_by as Record<string, unknown>
        const memberships = group?.memberships as { count: number }[] | undefined
        return {
          id: inv.id,
          inviteCode: inv.invite_code,
          status: inv.status,
          createdAt: inv.created_at,
          expiresAt: inv.expires_at,
          invitedBy: invitedBy ? {
            id: invitedBy.id,
            displayName: invitedBy.display_name,
            avatarUrl: invitedBy.avatar_url,
            isFoundingMember: invitedBy.is_founding_member,
            isAdmin: invitedBy.is_admin,
          } : null,
          group: group ? {
            id: group.id,
            name: group.name,
            slug: group.slug,
            description: group.description,
            logoUrl: group.logo_url,
            groupType: group.group_type,
            locationCity: group.location_city,
            locationState: group.location_state,
            memberCount: memberships?.[0]?.count ?? 0,
          } : null,
        }
      }))
    }

    // Get single group by ID or slug
    if (groupId || slug) {
      // Check for special include queries that need auth
      if (include === 'members' || include === 'requests' || include === 'invitations') {
        const token = getFirebaseToken(req)
        let userId: string | null = null

        if (token) {
          const firebaseUser = await verifyFirebaseToken(token)
          if (firebaseUser) {
            const { data: dbUser } = await supabase
              .from('users')
              .select('id')
              .eq('firebase_uid', firebaseUser.uid)
              .single()
            userId = dbUser?.id ?? null
          }
        }

        // Get group info first
        const gid = groupId || slug
        let groupQuery = supabase.from('groups').select('id, join_policy')
        if (groupId) {
          groupQuery = groupQuery.eq('id', groupId)
        } else {
          groupQuery = groupQuery.eq('slug', slug)
        }
        const { data: group } = await groupQuery.single()
        if (!group) {
          return errorResponse('Group not found', 404)
        }

        // Check membership for authorization
        let membership: { role: string } | null = null
        if (userId) {
          const { data } = await supabase
            .from('group_memberships')
            .select('role')
            .eq('group_id', group.id)
            .eq('user_id', userId)
            .single()
          membership = data
        }

        const isOwnerOrAdmin = membership && ['owner', 'admin'].includes(membership.role)

        // Get members list - all groups are viewable, members list is public
        if (include === 'members') {

          const { data, error } = await supabase
            .from('group_memberships')
            .select(`
              id, user_id, role, joined_at,
              user:users(id, display_name, username, email, avatar_url, is_founding_member, is_admin)
            `)
            .eq('group_id', group.id)
            .order('joined_at', { ascending: true })

          if (error) {
            console.error('Members query error:', error)
            return errorResponse(error.message, 500)
          }

          const members = (data ?? []).map(toGroupMember)
          return jsonResponse(members)
        }

        // Get join requests (owner/admin only)
        if (include === 'requests') {
          if (!isOwnerOrAdmin) {
            return errorResponse('Not authorized to view join requests', 403)
          }

          const { data, error } = await supabase
            .from('group_join_requests')
            .select(`
              id, user_id, message, status, created_at,
              user:users!group_join_requests_user_id_fkey(id, display_name, username, email, avatar_url, is_founding_member, is_admin)
            `)
            .eq('group_id', group.id)
            .eq('status', 'pending')
            .order('created_at', { ascending: true })

          if (error) {
            return errorResponse(error.message, 500)
          }

          return jsonResponse((data ?? []).map(toJoinRequest))
        }

        // Get invitations (owner/admin only)
        if (include === 'invitations') {
          if (!isOwnerOrAdmin) {
            return errorResponse('Not authorized to view invitations', 403)
          }

          const { data, error } = await supabase
            .from('group_invitations')
            .select(`
              id, invite_code, invited_email, max_uses, uses_count, expires_at, created_at,
              invited_by:users!invited_by_user_id(id, display_name)
            `)
            .eq('group_id', group.id)
            .or('expires_at.is.null,expires_at.gt.now()')
            .order('created_at', { ascending: false })

          if (error) {
            return errorResponse(error.message, 500)
          }

          return jsonResponse((data ?? []).map(toInvitation))
        }
      }

      // Regular group fetch
      let query = supabase
        .from('groups')
        .select(`
          *,
          creator:users!created_by_user_id(id, display_name, avatar_url, is_founding_member, is_admin, subscription_tier, subscription_override_tier),
          memberships:group_memberships(id)
        `)

      if (groupId) {
        query = query.eq('id', groupId)
      } else if (slug) {
        query = query.eq('slug', slug)
      }

      const { data, error } = await query.single()

      if (error) {
        return errorResponse(error.code === 'PGRST116' ? 'Group not found' : error.message, error.code === 'PGRST116' ? 404 : 500)
      }

      return jsonResponse(toGroup(data))
    }

    // Get user's groups (requires auth)
    const mine = url.searchParams.get('mine')
    if (mine === 'true') {
      const token = getFirebaseToken(req)
      if (!token) {
        return errorResponse('Missing Firebase token', 401)
      }

      const firebaseUser = await verifyFirebaseToken(token)
      if (!firebaseUser) {
        return errorResponse('Invalid Firebase token', 401)
      }

      const { data: dbUser } = await supabase
        .from('users')
        .select('id')
        .eq('firebase_uid', firebaseUser.uid)
        .single()

      if (!dbUser) {
        return errorResponse('User not found', 404)
      }

      // Get groups where user is a member
      const { data, error } = await supabase
        .from('group_memberships')
        .select(`
          role,
          group:groups(
            id, name, slug, description, logo_url, group_type,
            location_city, location_state, join_policy,
            memberships:group_memberships(count)
          )
        `)
        .eq('user_id', dbUser.id)
        .order('joined_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Transform and include user's role
      const groups = (data ?? [])
        .filter(row => row.group)
        .map(row => ({
          ...toGroupSummary(row.group as Record<string, unknown>),
          userRole: row.role,
        }))

      return jsonResponse(groups)
    }

    // List all groups (all are searchable)
    const search = url.searchParams.get('search')
    const groupType = url.searchParams.get('type')
    const city = url.searchParams.get('city')
    const state = url.searchParams.get('state')

    let query = supabase
      .from('groups')
      .select(`
        id, name, slug, description, logo_url, group_type,
        location_city, location_state, join_policy,
        memberships:group_memberships(count)
      `)
      .order('name', { ascending: true })

    if (search) {
      const safeSearch = escapeFilterValue(search)
      query = query.or(`name.ilike.%${safeSearch}%,description.ilike.%${safeSearch}%`)
    }
    if (groupType) {
      query = query.eq('group_type', groupType)
    }
    if (city) {
      const safeCity = escapeFilterValue(city)
      query = query.ilike('location_city', `%${safeCity}%`)
    }
    if (state) {
      query = query.eq('location_state', state)
    }

    const { data, error } = await query

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse((data ?? []).map(toGroupSummary))
  }

  // All other methods require authentication
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  // Get the user from database including subscription info
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, subscription_tier, subscription_override_tier')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // Helper to get user's membership in a group
  async function getUserMembership(gid: string) {
    const { data } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('group_id', gid)
      .eq('user_id', user.id)
      .single()
    return data
  }

  // POST - Create group, join, request, approve, reject, invite, accept-invite
  if (req.method === 'POST') {
    // Join group (open groups only)
    if (action === 'join' && groupId) {
      // Check group join policy
      const { data: group } = await supabase
        .from('groups')
        .select('join_policy')
        .eq('id', groupId)
        .single()

      if (!group) {
        return errorResponse('Group not found', 404)
      }

      if (group.join_policy !== 'open') {
        return errorResponse('This group requires a request or invitation to join', 400)
      }

      // Check if already a member
      const { data: existing } = await supabase
        .from('group_memberships')
        .select('id')
        .eq('group_id', groupId)
        .eq('user_id', user.id)
        .single()

      if (existing) {
        return errorResponse('Already a member of this group', 400)
      }

      const { error } = await supabase
        .from('group_memberships')
        .insert({
          group_id: groupId,
          user_id: user.id,
          role: 'member',
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Joined group successfully' })
    }

    // Request to join (private groups)
    if (action === 'request' && groupId) {
      // Check if already a member
      const existing = await getUserMembership(groupId)
      if (existing) {
        return errorResponse('Already a member of this group', 400)
      }

      // Check for existing pending request
      const { data: existingRequest } = await supabase
        .from('group_join_requests')
        .select('id, status')
        .eq('group_id', groupId)
        .eq('user_id', user.id)
        .single()

      if (existingRequest) {
        if (existingRequest.status === 'pending') {
          return errorResponse('You already have a pending request', 400)
        }
        // If rejected, allow new request by updating
        const body = await req.json().catch(() => ({}))
        const { error } = await supabase
          .from('group_join_requests')
          .update({
            message: body.message?.trim() || null,
            status: 'pending',
            reviewed_by_user_id: null,
            reviewed_at: null,
          })
          .eq('id', existingRequest.id)

        if (error) {
          return errorResponse(error.message, 500)
        }
        return jsonResponse({ message: 'Join request submitted' })
      }

      const body = await req.json().catch(() => ({}))
      const { error } = await supabase
        .from('group_join_requests')
        .insert({
          group_id: groupId,
          user_id: user.id,
          message: body.message?.trim() || null,
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Join request submitted' })
    }

    // Approve join request
    if (action === 'approve' && groupId && targetUserId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Not authorized', 403)
      }

      // Find the pending request
      const { data: request } = await supabase
        .from('group_join_requests')
        .select('id')
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)
        .eq('status', 'pending')
        .single()

      if (!request) {
        return errorResponse('No pending request found', 404)
      }

      // Update request status
      await supabase
        .from('group_join_requests')
        .update({
          status: 'approved',
          reviewed_by_user_id: user.id,
          reviewed_at: new Date().toISOString(),
        })
        .eq('id', request.id)

      // Add as member
      const { error } = await supabase
        .from('group_memberships')
        .insert({
          group_id: groupId,
          user_id: targetUserId,
          role: 'member',
        })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Request approved' })
    }

    // Reject join request
    if (action === 'reject' && groupId && targetUserId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Not authorized', 403)
      }

      const { error } = await supabase
        .from('group_join_requests')
        .update({
          status: 'rejected',
          reviewed_by_user_id: user.id,
          reviewed_at: new Date().toISOString(),
        })
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)
        .eq('status', 'pending')

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'Request rejected' })
    }

    // Create invitation
    if (action === 'invite' && groupId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Not authorized', 403)
      }

      const body = await req.json().catch(() => ({}))
      const code = generateInviteCode()

      let expiresAt = null
      if (body.expiresInDays) {
        const d = new Date()
        d.setDate(d.getDate() + parseInt(body.expiresInDays))
        expiresAt = d.toISOString()
      }

      // If inviting a specific user, validate and check existing membership
      if (body.userId) {
        // Check if user exists
        const { data: invitedUser } = await supabase
          .from('users')
          .select('id')
          .eq('id', body.userId)
          .single()

        if (!invitedUser) {
          return errorResponse('User not found', 404)
        }

        // Check if already a member
        const { data: existingMembership } = await supabase
          .from('group_memberships')
          .select('id')
          .eq('group_id', groupId)
          .eq('user_id', body.userId)
          .single()

        if (existingMembership) {
          return errorResponse('User is already a member of this group', 400)
        }

        // Check if there's already a pending invitation for this user
        const { data: existingInvite } = await supabase
          .from('group_invitations')
          .select('id')
          .eq('group_id', groupId)
          .eq('invited_user_id', body.userId)
          .eq('status', 'pending')
          .single()

        if (existingInvite) {
          return errorResponse('User already has a pending invitation to this group', 400)
        }
      }

      const { data, error } = await supabase
        .from('group_invitations')
        .insert({
          group_id: groupId,
          invited_by_user_id: user.id,
          invite_code: code,
          invited_email: body.email?.trim() || null,
          invited_user_id: body.userId || null,
          max_uses: body.userId ? 1 : (body.maxUses || null), // Direct invites are single-use; link invites are unlimited by default
          expires_at: expiresAt,
          status: 'pending',
        })
        .select(`
          id, invite_code, invited_email, max_uses, uses_count, expires_at, created_at, status, invited_user_id,
          invited_by:users!invited_by_user_id(id, display_name)
        `)
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse(toInvitation(data), 201)
    }

    // Accept invitation
    if (action === 'accept-invite' && inviteCode) {
      // Find the invitation
      const { data: invitation } = await supabase
        .from('group_invitations')
        .select('id, group_id, max_uses, uses_count, expires_at')
        .eq('invite_code', inviteCode)
        .single()

      if (!invitation) {
        return errorResponse('Invalid invitation code', 404)
      }

      // Check expiry
      if (invitation.expires_at && new Date(invitation.expires_at) < new Date()) {
        return errorResponse('This invitation has expired', 400)
      }

      // Check max uses (initial check - will verify atomically later)
      if (invitation.max_uses && invitation.uses_count >= invitation.max_uses) {
        return errorResponse('This invitation has reached its maximum uses', 400)
      }

      // Check if already a member
      const existing = await getUserMembership(invitation.group_id)
      if (existing) {
        return errorResponse('Already a member of this group', 400)
      }

      // Check if already used this invite
      const { data: usedBefore } = await supabase
        .from('group_invitation_uses')
        .select('id')
        .eq('invitation_id', invitation.id)
        .eq('user_id', user.id)
        .single()

      if (usedBefore) {
        return errorResponse('You have already used this invitation', 400)
      }

      // Atomically increment uses_count and get new value
      // This prevents race conditions where multiple users bypass the limit
      const { data: updatedInvite, error: updateError } = await supabase
        .from('group_invitations')
        .update({ uses_count: invitation.uses_count + 1 })
        .eq('id', invitation.id)
        .select('uses_count, max_uses')
        .single()

      if (updateError) {
        return errorResponse('Failed to process invitation', 500)
      }

      // Re-check max_uses with the atomically incremented count
      // If we exceeded due to race condition, reject and decrement
      if (updatedInvite.max_uses && updatedInvite.uses_count > updatedInvite.max_uses) {
        // Decrement the count since we can't use this invite
        await supabase
          .from('group_invitations')
          .update({ uses_count: updatedInvite.uses_count - 1 })
          .eq('id', invitation.id)
        return errorResponse('This invitation has reached its maximum uses', 400)
      }

      // Add as member
      const { error: memberError } = await supabase
        .from('group_memberships')
        .insert({
          group_id: invitation.group_id,
          user_id: user.id,
          role: 'member',
        })

      if (memberError) {
        // Rollback the uses_count increment
        await supabase
          .from('group_invitations')
          .update({ uses_count: updatedInvite.uses_count - 1 })
          .eq('id', invitation.id)
        return errorResponse(memberError.message, 500)
      }

      // Record invitation use
      const { error: useError } = await supabase
        .from('group_invitation_uses')
        .insert({
          invitation_id: invitation.id,
          user_id: user.id,
        })

      // If recording use fails (likely duplicate), rollback
      if (useError) {
        await supabase
          .from('group_memberships')
          .delete()
          .eq('group_id', invitation.group_id)
          .eq('user_id', user.id)
        await supabase
          .from('group_invitations')
          .update({ uses_count: updatedInvite.uses_count - 1 })
          .eq('id', invitation.id)
        return errorResponse('You have already used this invitation', 400)
      }

      // Get group name for response
      const { data: group } = await supabase
        .from('groups')
        .select('id, name')
        .eq('id', invitation.group_id)
        .single()

      return jsonResponse({
        message: 'Joined group successfully',
        groupId: invitation.group_id,
        groupName: group?.name,
      })
    }

    // Respond to direct invitation (accept or decline)
    if (action === 'respond-invite' && inviteId) {
      const body = await req.json().catch(() => ({}))
      const response = body.response as 'accept' | 'decline'

      if (!response || !['accept', 'decline'].includes(response)) {
        return errorResponse('Response must be "accept" or "decline"', 400)
      }

      // Find the invitation
      const { data: invitation } = await supabase
        .from('group_invitations')
        .select('id, group_id, expires_at, status, invited_user_id')
        .eq('id', inviteId)
        .single()

      if (!invitation) {
        return errorResponse('Invitation not found', 404)
      }

      // Verify this invitation is for the current user
      if (invitation.invited_user_id !== user.id) {
        return errorResponse('This invitation is not for you', 403)
      }

      // Check status
      if (invitation.status !== 'pending') {
        return errorResponse('This invitation has already been responded to', 400)
      }

      // Check expiry
      if (invitation.expires_at && new Date(invitation.expires_at) < new Date()) {
        // Mark as expired
        await supabase
          .from('group_invitations')
          .update({ status: 'expired' })
          .eq('id', inviteId)
        return errorResponse('This invitation has expired', 400)
      }

      if (response === 'accept') {
        // Check if already a member
        const existing = await getUserMembership(invitation.group_id)
        if (existing) {
          // Mark invitation as accepted anyway
          await supabase
            .from('group_invitations')
            .update({ status: 'accepted' })
            .eq('id', inviteId)
          return errorResponse('Already a member of this group', 400)
        }

        // Add as member
        const { error: memberError } = await supabase
          .from('group_memberships')
          .insert({
            group_id: invitation.group_id,
            user_id: user.id,
            role: 'member',
          })

        if (memberError) {
          return errorResponse(memberError.message, 500)
        }

        // Update invitation status
        await supabase
          .from('group_invitations')
          .update({ status: 'accepted', uses_count: 1 })
          .eq('id', inviteId)

        // Get group name for response
        const { data: group } = await supabase
          .from('groups')
          .select('id, name')
          .eq('id', invitation.group_id)
          .single()

        return jsonResponse({
          message: 'Joined group successfully',
          groupId: invitation.group_id,
          groupName: group?.name,
        })
      } else {
        // Decline the invitation
        await supabase
          .from('group_invitations')
          .update({ status: 'declined' })
          .eq('id', inviteId)

        return jsonResponse({ message: 'Invitation declined' })
      }
    }

    // Create new group (no action specified)
    if (!action) {
      const body = await req.json()

      if (!body.name?.trim()) {
        return errorResponse('Group name is required', 400)
      }

      // Check subscription tier limits for group creation
      const effectiveTier = user.subscription_override_tier || user.subscription_tier || 'free'
      const tierLimits: Record<string, number> = {
        free: 1,
        basic: 5,
        pro: 10,
        premium: Infinity,
      }
      const maxGroups = tierLimits[effectiveTier] ?? 1

      // Count user's groups where they are the owner
      const { count: groupCount, error: countError } = await supabase
        .from('group_memberships')
        .select('id', { count: 'exact', head: true })
        .eq('user_id', user.id)
        .eq('role', 'owner')

      if (countError) {
        return errorResponse('Failed to check group limits', 500)
      }

      if ((groupCount ?? 0) >= maxGroups) {
        return errorResponse(
          JSON.stringify({
            code: 'TIER_LIMIT_REACHED',
            message: `You have reached your limit of ${maxGroups} group${maxGroups === 1 ? '' : 's'}. Upgrade your plan to create more groups.`,
            currentCount: groupCount,
            limit: maxGroups,
            tier: effectiveTier,
          }),
          403
        )
      }

      // Check if group name already exists (case-insensitive)
      const { data: existingGroup } = await supabase
        .from('groups')
        .select('id, name')
        .ilike('name', body.name.trim())
        .single()

      if (existingGroup) {
        return errorResponse(`A group with the name "${existingGroup.name}" already exists`, 400)
      }

      // Generate unique slug
      let baseSlug = generateSlug(body.name)
      let finalSlug = baseSlug
      let counter = 0

      while (true) {
        const { data: existingSlug } = await supabase
          .from('groups')
          .select('id')
          .eq('slug', finalSlug)
          .single()

        if (!existingSlug) break

        counter++
        finalSlug = `${baseSlug}-${counter}`
      }

      const { data, error } = await supabase
        .from('groups')
        .insert({
          name: body.name.trim(),
          slug: finalSlug,
          description: body.description?.trim() || null,
          group_type: body.groupType || 'both',
          location_city: body.locationCity?.trim() || null,
          location_state: body.locationState?.trim() || null,
          location_radius_miles: body.locationRadiusMiles || null,
          join_policy: body.joinPolicy || 'open',
          created_by_user_id: user.id,
        })
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Add creator as owner
      await supabase
        .from('group_memberships')
        .insert({
          group_id: data.id,
          user_id: user.id,
          role: 'owner',
        })

      return jsonResponse(toGroup(data), 201)
    }

    // Upload group logo image
    if (action === 'upload-logo' && groupId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Only admins can update the group logo', 403)
      }

      const formData = await req.formData()
      const file = formData.get('logo')
      if (!file || !(file instanceof File)) {
        return errorResponse('No logo file provided', 400)
      }

      const allowedTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
      if (!allowedTypes.includes(file.type)) {
        return errorResponse('Invalid file type. Allowed: PNG, JPEG, WebP, GIF', 400)
      }

      if (file.size > 5 * 1024 * 1024) {
        return errorResponse('File too large. Maximum 5MB', 400)
      }

      const ext = file.name.split('.').pop() || 'png'
      const path = `${groupId}/logo.${ext}`

      // Delete old logo if exists
      await supabase.storage.from('group-logos').remove([`${groupId}/logo.png`, `${groupId}/logo.jpg`, `${groupId}/logo.jpeg`, `${groupId}/logo.webp`, `${groupId}/logo.gif`])

      const { error: uploadError } = await supabase.storage
        .from('group-logos')
        .upload(path, file, { contentType: file.type, upsert: true })

      if (uploadError) {
        console.error('Logo upload error:', uploadError)
        return errorResponse('Failed to upload logo', 500)
      }

      const { data: urlData } = supabase.storage.from('group-logos').getPublicUrl(path)
      const logoUrl = `${urlData.publicUrl}?t=${Date.now()}`

      // Update group with new logo URL
      await supabase.from('groups').update({ logo_url: logoUrl }).eq('id', groupId)

      return jsonResponse({ logoUrl })
    }

    return errorResponse('Invalid action', 400)
  }

  // PUT - Update group or change member role
  if (req.method === 'PUT') {
    if (!groupId) {
      return errorResponse('Group ID required', 400)
    }

    // Change member role
    if (action === 'role' && targetUserId) {
      const membership = await getUserMembership(groupId)
      if (!membership || membership.role !== 'owner') {
        return errorResponse('Only the owner can change roles', 403)
      }

      const body = await req.json()
      const newRole = body.role

      if (!['admin', 'member'].includes(newRole)) {
        return errorResponse('Invalid role. Must be admin or member', 400)
      }

      // Can't change own role
      if (targetUserId === user.id) {
        return errorResponse('Cannot change your own role', 400)
      }

      const { error } = await supabase
        .from('group_memberships')
        .update({ role: newRole })
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: `Role changed to ${newRole}` })
    }

    // Transfer ownership
    if (action === 'transfer' && targetUserId) {
      const membership = await getUserMembership(groupId)
      if (!membership || membership.role !== 'owner') {
        return errorResponse('Only the owner can transfer ownership', 403)
      }

      // Can't transfer to yourself
      if (targetUserId === user.id) {
        return errorResponse('Cannot transfer ownership to yourself', 400)
      }

      // Verify target is a member
      const { data: targetMembership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)
        .single()

      if (!targetMembership) {
        return errorResponse('Target user is not a member of this group', 400)
      }

      // Transfer: make target the owner, demote current owner to admin
      const { error: transferError } = await supabase
        .from('group_memberships')
        .update({ role: 'owner' })
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)

      if (transferError) {
        return errorResponse(transferError.message, 500)
      }

      const { error: demoteError } = await supabase
        .from('group_memberships')
        .update({ role: 'admin' })
        .eq('group_id', groupId)
        .eq('user_id', user.id)

      if (demoteError) {
        return errorResponse(demoteError.message, 500)
      }

      return jsonResponse({ message: 'Ownership transferred successfully' })
    }

    // Update group
    const membership = await getUserMembership(groupId)
    if (!membership || !['owner', 'admin'].includes(membership.role)) {
      return errorResponse('Not authorized to update this group', 403)
    }

    const body = await req.json()
    const updates: Record<string, unknown> = {}

    if (body.name !== undefined) updates.name = body.name.trim()
    if (body.description !== undefined) updates.description = body.description?.trim() || null
    if (body.groupType !== undefined) updates.group_type = body.groupType
    if (body.locationCity !== undefined) updates.location_city = body.locationCity?.trim() || null
    if (body.locationState !== undefined) updates.location_state = body.locationState?.trim() || null
    if (body.locationRadiusMiles !== undefined) updates.location_radius_miles = body.locationRadiusMiles
    if (body.joinPolicy !== undefined) updates.join_policy = body.joinPolicy
    if (body.logoUrl !== undefined) updates.logo_url = body.logoUrl || null

    const { data, error } = await supabase
      .from('groups')
      .update(updates)
      .eq('id', groupId)
      .select(`
        *,
        creator:users!created_by_user_id(id, display_name, avatar_url, is_founding_member, is_admin, subscription_tier, subscription_override_tier),
        memberships:group_memberships(id)
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toGroup(data))
  }

  // DELETE - Delete group, leave, remove member, revoke invite
  if (req.method === 'DELETE') {
    if (!groupId) {
      return errorResponse('Group ID required', 400)
    }

    // Leave group
    if (action === 'leave') {
      const membership = await getUserMembership(groupId)

      if (!membership) {
        return errorResponse('Not a member of this group', 400)
      }

      if (membership.role === 'owner') {
        // Check if there are other members
        const { count } = await supabase
          .from('group_memberships')
          .select('*', { count: 'exact', head: true })
          .eq('group_id', groupId)

        if (count && count > 1) {
          return errorResponse('Owner must transfer ownership before leaving', 400)
        }
      }

      const { error } = await supabase
        .from('group_memberships')
        .delete()
        .eq('group_id', groupId)
        .eq('user_id', user.id)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return new Response(null, { status: 204, headers: getCorsHeaders(req) })
    }

    // Remove member
    if (action === 'remove' && targetUserId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Not authorized', 403)
      }

      // Can't remove yourself
      if (targetUserId === user.id) {
        return errorResponse('Use leave action to remove yourself', 400)
      }

      // Get target's membership
      const { data: targetMembership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)
        .single()

      if (!targetMembership) {
        return errorResponse('User is not a member', 404)
      }

      // Can't remove owner
      if (targetMembership.role === 'owner') {
        return errorResponse('Cannot remove the group owner', 400)
      }

      // Admin can't remove other admins
      if (membership.role === 'admin' && targetMembership.role === 'admin') {
        return errorResponse('Admins cannot remove other admins', 403)
      }

      const { error } = await supabase
        .from('group_memberships')
        .delete()
        .eq('group_id', groupId)
        .eq('user_id', targetUserId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return new Response(null, { status: 204, headers: getCorsHeaders(req) })
    }

    // Revoke invitation
    if (action === 'revoke-invite' && inviteId) {
      const membership = await getUserMembership(groupId)
      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('Not authorized', 403)
      }

      const { error } = await supabase
        .from('group_invitations')
        .delete()
        .eq('id', inviteId)
        .eq('group_id', groupId)

      if (error) {
        return errorResponse(error.message, 500)
      }

      return new Response(null, { status: 204, headers: getCorsHeaders(req) })
    }

    // Delete group - owner only
    const membership = await getUserMembership(groupId)
    if (!membership || membership.role !== 'owner') {
      return errorResponse('Only the owner can delete a group', 403)
    }

    const { error } = await supabase
      .from('groups')
      .delete()
      .eq('id', groupId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  return errorResponse('Method not allowed', 405)
})
