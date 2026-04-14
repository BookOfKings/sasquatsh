import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// Username validation regex: 3-30 chars, starts with letter, alphanumeric + underscores
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/
const RESERVED_USERNAMES = ['admin', 'administrator', 'root', 'system', 'support', 'help', 'info', 'sasquatsh', 'moderator', 'mod']

// Transform database row to UserProfile
function toUserProfile(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    firebaseUid: row.firebase_uid as string,
    email: row.email as string,
    username: row.username as string,
    displayName: row.display_name as string | null,
    avatarUrl: row.avatar_url as string | null,
    birthYear: row.birth_year as number | null,
    maxTravelMiles: row.max_travel_miles as number | null,
    homeCity: row.home_city as string | null,
    homeState: row.home_state as string | null,
    homePostalCode: row.home_postal_code as string | null,
    activeCity: row.active_city as string | null,
    activeState: row.active_state as string | null,
    activeLocationExpiresAt: row.active_location_expires_at as string | null,
    activeEventLocationId: row.active_event_location_id as string | null,
    activeLocationHall: row.active_location_hall as string | null,
    activeLocationRoom: row.active_location_room as string | null,
    activeLocationTable: row.active_location_table as string | null,
    timezone: row.timezone as string | null,
    bio: row.bio as string | null,
    favoriteGames: row.favorite_games as string[] | null,
    preferredGameTypes: row.preferred_game_types as string[] | null,
    isAdmin: row.is_admin as boolean ?? false,
    isFoundingMember: row.is_founding_member as boolean ?? false,
    blockedUserIds: row.blocked_user_ids as string[] ?? [],
    collectionVisibility: (row.collection_visibility as string) ?? 'private',
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    authProvider: (row.auth_provider as string) ?? 'password',
    passwordChangedAt: row.password_changed_at as string | null,
  }
}

// Transform for public profile (limited info - shows @username, not real name)
function toPublicProfile(row: Record<string, unknown>) {
  return {
    id: row.id as string,
    username: row.username as string,
    displayName: row.display_name as string | null, // Only shown to authorized viewers
    avatarUrl: row.avatar_url as string | null,
    homeCity: row.home_city as string | null,
    homeState: row.home_state as string | null,
    bio: row.bio as string | null,
    favoriteGames: row.favorite_games as string[] | null,
    preferredGameTypes: row.preferred_game_types as string[] | null,
    isFoundingMember: row.is_founding_member as boolean ?? false,
    collectionVisibility: (row.collection_visibility as string) ?? 'private',
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
  const userId = url.searchParams.get('id')
  const action = url.searchParams.get('action')
  const include = url.searchParams.get('include')

  // GET public profile by ID (no auth required)
  if (req.method === 'GET' && userId) {
    const { data, error } = await supabase
      .from('users')
      .select('*')
      .eq('id', userId)
      .single()

    if (error || !data) {
      return errorResponse('User not found', 404)
    }

    return jsonResponse(toPublicProfile(data))
  }

  // All other operations require authentication
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('*')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // GET - Get blocked users list
  if (req.method === 'GET' && include === 'blocked') {
    const blockedIds: string[] = user.blocked_user_ids ?? []

    if (blockedIds.length === 0) {
      return jsonResponse([])
    }

    const { data: blockedUsers, error } = await supabase
      .from('users')
      .select('id, username, display_name, avatar_url')
      .in('id', blockedIds)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(blockedUsers.map(u => ({
      id: u.id,
      username: u.username,
      displayName: u.display_name,
      avatarUrl: u.avatar_url,
    })))
  }

  // GET - Search users by username
  if (req.method === 'GET' && action === 'search') {
    const query = url.searchParams.get('q')?.trim()

    if (!query || query.length < 2) {
      return jsonResponse([])
    }

    // Search for users by username (case-insensitive prefix match)
    // Exclude current user and blocked users
    const blockedIds: string[] = user.blocked_user_ids ?? []

    const { data: users, error } = await supabase
      .from('users')
      .select('id, username, display_name, avatar_url, is_founding_member')
      .ilike('username', `${query}%`)
      .neq('id', user.id)
      .limit(10)

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Filter out blocked users
    const filteredUsers = users.filter(u => !blockedIds.includes(u.id))

    return jsonResponse(filteredUsers.map(u => ({
      id: u.id,
      username: u.username,
      displayName: u.display_name,
      avatarUrl: u.avatar_url,
      isFoundingMember: u.is_founding_member ?? false,
    })))
  }

  // GET - Get current user's full profile
  if (req.method === 'GET') {
    // Get user's group memberships
    const { data: memberships } = await supabase
      .from('group_memberships')
      .select(`
        role, joined_at,
        group:groups(id, name, slug, logo_url, group_type)
      `)
      .eq('user_id', user.id)

    // Get user's upcoming events (registered)
    const today = new Date().toISOString().split('T')[0]
    const { data: allRegistrations } = await supabase
      .from('event_registrations')
      .select(`
        status,
        event:events(id, title, event_date, start_time, city, state)
      `)
      .eq('user_id', user.id)
      .in('status', ['pending', 'confirmed'])

    // Filter to upcoming events and sort by date (embedded table filters not supported via query builder)
    const registrations = (allRegistrations ?? [])
      .filter(r => r.event && (r.event as Record<string, unknown>).event_date >= today)
      .sort((a, b) => {
        const dateA = (a.event as Record<string, unknown>).event_date as string
        const dateB = (b.event as Record<string, unknown>).event_date as string
        return dateA.localeCompare(dateB)
      })
      .slice(0, 5)

    // Get user's hosted events count
    const { count: hostedCount } = await supabase
      .from('events')
      .select('*', { count: 'exact', head: true })
      .eq('host_user_id', user.id)

    // Get user's past events count
    const { count: attendedCount } = await supabase
      .from('event_registrations')
      .select('*', { count: 'exact', head: true })
      .eq('user_id', user.id)
      .eq('status', 'confirmed')

    const profile = toUserProfile(user)

    return jsonResponse({
      ...profile,
      groups: memberships?.map(m => ({
        role: m.role,
        joinedAt: m.joined_at,
        group: m.group ? {
          id: (m.group as Record<string, unknown>).id,
          name: (m.group as Record<string, unknown>).name,
          slug: (m.group as Record<string, unknown>).slug,
          logoUrl: (m.group as Record<string, unknown>).logo_url,
          groupType: (m.group as Record<string, unknown>).group_type,
        } : null,
      })) ?? [],
      upcomingEvents: registrations?.map(r => ({
        status: r.status,
        event: r.event ? {
          id: (r.event as Record<string, unknown>).id,
          title: (r.event as Record<string, unknown>).title,
          eventDate: (r.event as Record<string, unknown>).event_date,
          startTime: (r.event as Record<string, unknown>).start_time,
          city: (r.event as Record<string, unknown>).city,
          state: (r.event as Record<string, unknown>).state,
        } : null,
      })).filter(r => r.event !== null) ?? [],
      stats: {
        hostedCount: hostedCount ?? 0,
        attendedCount: attendedCount ?? 0,
        groupCount: memberships?.length ?? 0,
      },
    })
  }

  // PUT - Update current user's profile
  if (req.method === 'PUT') {
    const body = await req.json()

    const updates: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    // Validate and update username if provided
    if (body.username !== undefined) {
      const username = body.username?.trim()

      if (!username) {
        return errorResponse('Username cannot be empty', 400)
      }

      if (!USERNAME_REGEX.test(username)) {
        return errorResponse('Username must be 3-30 characters, start with a letter, and contain only letters, numbers, and underscores', 400)
      }

      if (RESERVED_USERNAMES.includes(username.toLowerCase())) {
        return errorResponse('This username is reserved', 400)
      }

      // Check uniqueness (case-insensitive), excluding current user
      const { data: existingUser } = await supabase
        .from('users')
        .select('id')
        .ilike('username', username)
        .neq('id', user.id)
        .limit(1)

      if (existingUser && existingUser.length > 0) {
        return errorResponse('Username is already taken', 400)
      }

      updates.username = username
    }

    // Only update fields that are provided
    if (body.displayName !== undefined) updates.display_name = body.displayName?.trim() || null
    if (body.avatarUrl !== undefined) updates.avatar_url = body.avatarUrl || null
    if (body.birthYear !== undefined) updates.birth_year = body.birthYear || null
    if (body.maxTravelMiles !== undefined) updates.max_travel_miles = body.maxTravelMiles
    if (body.homeCity !== undefined) updates.home_city = body.homeCity?.trim() || null
    if (body.homeState !== undefined) updates.home_state = body.homeState?.trim() || null
    if (body.homePostalCode !== undefined) updates.home_postal_code = body.homePostalCode?.trim() || null
    if (body.bio !== undefined) updates.bio = body.bio?.trim() || null
    if (body.favoriteGames !== undefined) updates.favorite_games = body.favoriteGames || null
    if (body.preferredGameTypes !== undefined) updates.preferred_game_types = body.preferredGameTypes || null
    if (body.activeCity !== undefined) updates.active_city = body.activeCity?.trim() || null
    if (body.activeState !== undefined) updates.active_state = body.activeState?.trim() || null
    if (body.activeLocationExpiresAt !== undefined) updates.active_location_expires_at = body.activeLocationExpiresAt || null
    if (body.activeEventLocationId !== undefined) updates.active_event_location_id = body.activeEventLocationId || null
    if (body.activeLocationHall !== undefined) updates.active_location_hall = body.activeLocationHall?.trim() || null
    if (body.activeLocationRoom !== undefined) updates.active_location_room = body.activeLocationRoom?.trim() || null
    if (body.activeLocationTable !== undefined) updates.active_location_table = body.activeLocationTable?.trim() || null
    if (body.timezone !== undefined) updates.timezone = body.timezone?.trim() || null
    if (body.collectionVisibility !== undefined) updates.collection_visibility = body.collectionVisibility

    const { data, error } = await supabase
      .from('users')
      .update(updates)
      .eq('id', user.id)
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toUserProfile(data))
  }

  // POST - Avatar upload or Block/Unblock user actions
  if (req.method === 'POST') {
    // Handle avatar upload
    if (action === 'upload-avatar') {
      const contentType = req.headers.get('content-type') || ''

      if (!contentType.includes('multipart/form-data')) {
        return errorResponse('Content-Type must be multipart/form-data', 400)
      }

      try {
        const formData = await req.formData()
        const file = formData.get('avatar') as File | null

        if (!file) {
          return errorResponse('No avatar file provided', 400)
        }

        // Validate file type
        const allowedTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']
        if (!allowedTypes.includes(file.type)) {
          return errorResponse('Invalid file type. Allowed: PNG, JPEG, WebP, GIF', 400)
        }

        // Validate file size (2MB max)
        if (file.size > 2 * 1024 * 1024) {
          return errorResponse('File too large. Maximum size is 2MB', 400)
        }

        // Generate file path: {user_id}/avatar.{extension}
        const extension = file.type.split('/')[1]
        const filePath = `${user.id}/avatar.${extension}`

        // Delete existing avatars for this user (any extension)
        const { data: existingFiles } = await supabase.storage
          .from('avatars')
          .list(user.id)

        if (existingFiles && existingFiles.length > 0) {
          const filesToDelete = existingFiles.map(f => `${user.id}/${f.name}`)
          await supabase.storage.from('avatars').remove(filesToDelete)
        }

        // Upload new avatar
        const arrayBuffer = await file.arrayBuffer()
        const { error: uploadError } = await supabase.storage
          .from('avatars')
          .upload(filePath, arrayBuffer, {
            contentType: file.type,
            upsert: true,
          })

        if (uploadError) {
          console.error('Avatar upload error:', uploadError)
          return errorResponse('Failed to upload avatar', 500)
        }

        // Get public URL with cache-busting timestamp
        const { data: urlData } = supabase.storage
          .from('avatars')
          .getPublicUrl(filePath)

        const avatarUrl = `${urlData.publicUrl}?t=${Date.now()}`

        // Update user's avatar_url in database
        const { data, error: updateError } = await supabase
          .from('users')
          .update({
            avatar_url: avatarUrl,
            updated_at: new Date().toISOString(),
          })
          .eq('id', user.id)
          .select()
          .single()

        if (updateError) {
          return errorResponse(updateError.message, 500)
        }

        return jsonResponse({
          message: 'Avatar uploaded successfully',
          avatarUrl,
          user: toUserProfile(data),
        })
      } catch (err) {
        console.error('Avatar upload error:', err)
        return errorResponse('Failed to process avatar upload', 500)
      }
    }

    // Handle avatar deletion
    if (action === 'delete-avatar') {
      // Delete existing avatars for this user
      const { data: existingFiles } = await supabase.storage
        .from('avatars')
        .list(user.id)

      if (existingFiles && existingFiles.length > 0) {
        const filesToDelete = existingFiles.map(f => `${user.id}/${f.name}`)
        await supabase.storage.from('avatars').remove(filesToDelete)
      }

      // Update user's avatar_url to null
      const { data, error } = await supabase
        .from('users')
        .update({
          avatar_url: null,
          updated_at: new Date().toISOString(),
        })
        .eq('id', user.id)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        message: 'Avatar deleted',
        user: toUserProfile(data),
      })
    }

    // Handle password changed notification
    if (action === 'password-changed') {
      const { data, error } = await supabase
        .from('users')
        .update({
          password_changed_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        })
        .eq('id', user.id)
        .select()
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        message: 'Password change recorded',
        user: toUserProfile(data),
      })
    }

    const targetUserId = url.searchParams.get('userId')

    if (!targetUserId) {
      return errorResponse('userId parameter required for block/unblock', 400)
    }

    if (targetUserId === user.id) {
      return errorResponse('Cannot block yourself', 400)
    }

    // Verify target user exists
    const { data: targetUser } = await supabase
      .from('users')
      .select('id')
      .eq('id', targetUserId)
      .single()

    if (!targetUser) {
      return errorResponse('Target user not found', 404)
    }

    const currentBlockedIds: string[] = user.blocked_user_ids ?? []

    if (action === 'block') {
      // Add to blocked list if not already blocked
      if (currentBlockedIds.includes(targetUserId)) {
        return jsonResponse({ message: 'User already blocked', blockedUserIds: currentBlockedIds })
      }

      const newBlockedIds = [...currentBlockedIds, targetUserId]

      const { data, error } = await supabase
        .from('users')
        .update({
          blocked_user_ids: newBlockedIds,
          updated_at: new Date().toISOString(),
        })
        .eq('id', user.id)
        .select('blocked_user_ids')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'User blocked', blockedUserIds: data.blocked_user_ids })
    }

    if (action === 'unblock') {
      // Remove from blocked list
      const newBlockedIds = currentBlockedIds.filter(id => id !== targetUserId)

      const { data, error } = await supabase
        .from('users')
        .update({
          blocked_user_ids: newBlockedIds,
          updated_at: new Date().toISOString(),
        })
        .eq('id', user.id)
        .select('blocked_user_ids')
        .single()

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({ message: 'User unblocked', blockedUserIds: data.blocked_user_ids })
    }

    return errorResponse('Invalid action. Use block or unblock', 400)
  }

  // DELETE - Delete account
  if (req.method === 'DELETE' && action === 'delete-account') {
    // Prevent admin users from self-deleting (must be done by another admin)
    if (user.is_admin) {
      return errorResponse('Admin accounts cannot be self-deleted. Contact another admin.', 400)
    }

    try {
      // Delete in order to handle foreign key constraints
      // Note: Many tables have ON DELETE CASCADE, but we explicitly delete to ensure clean removal

      // 1. Chat messages will cascade delete with user (ON DELETE CASCADE)
      // 2. Chat reports - delete reports they made or are about them
      await supabase.from('chat_reports').delete().eq('reporter_user_id', user.id)
      await supabase.from('chat_reports').delete().eq('reported_user_id', user.id)

      // 3. Planning votes and suggestions
      await supabase.from('planning_game_votes').delete().eq('user_id', user.id)
      await supabase.from('planning_date_votes').delete().eq('user_id', user.id)
      await supabase.from('planning_game_suggestions').delete().eq('suggested_by_user_id', user.id)

      // 4. Planning session invitations
      await supabase.from('planning_session_invitations').delete().eq('user_id', user.id)

      // 5. Event registrations
      await supabase.from('event_registrations').delete().eq('user_id', user.id)

      // 6. Event items - unclaim items they claimed
      await supabase.from('event_items').update({ claimed_by_user_id: null, claimed_at: null }).eq('claimed_by_user_id', user.id)

      // 7. Player requests
      await supabase.from('player_requests').delete().eq('user_id', user.id)

      // 8. Groups they own - handle before memberships
      const { data: ownedGroups } = await supabase
        .from('group_memberships')
        .select('group_id')
        .eq('user_id', user.id)
        .eq('role', 'owner')

      if (ownedGroups) {
        for (const membership of ownedGroups) {
          // Check if there are other owners
          const { data: otherOwners } = await supabase
            .from('group_memberships')
            .select('user_id')
            .eq('group_id', membership.group_id)
            .eq('role', 'owner')
            .neq('user_id', user.id)

          if (!otherOwners || otherOwners.length === 0) {
            // No other owners - delete the group and all related data
            await supabase.from('group_invitations').delete().eq('group_id', membership.group_id)
            await supabase.from('group_join_requests').delete().eq('group_id', membership.group_id)
            await supabase.from('group_memberships').delete().eq('group_id', membership.group_id)
            await supabase.from('groups').delete().eq('id', membership.group_id)
          }
        }
      }

      // 9. Group memberships (remaining ones where they're not sole owner)
      await supabase.from('group_memberships').delete().eq('user_id', user.id)

      // 10. Group join requests
      await supabase.from('group_join_requests').delete().eq('user_id', user.id)

      // 11. Group invitations
      await supabase.from('group_invitations').delete().eq('invited_user_id', user.id)

      // 12. Events they host - mark as cancelled
      await supabase.from('events').update({ status: 'cancelled' }).eq('created_by_user_id', user.id)

      // 13. Planning sessions they created - cancel
      await supabase.from('planning_sessions').update({ status: 'cancelled' }).eq('created_by_user_id', user.id)

      // 14. Delete avatar from storage
      try {
        const { data: avatarFiles } = await supabase.storage
          .from('avatars')
          .list(user.id)

        if (avatarFiles && avatarFiles.length > 0) {
          const filesToDelete = avatarFiles.map(f => `${user.id}/${f.name}`)
          await supabase.storage.from('avatars').remove(filesToDelete)
        }
      } catch {
        // Avatar deletion is non-critical, continue
      }

      // Finally delete the user (this will cascade delete chat_messages)
      const { error } = await supabase
        .from('users')
        .delete()
        .eq('id', user.id)

      if (error) {
        return errorResponse(`Failed to delete account: ${error.message}`, 500)
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error during account deletion'
      return errorResponse(`Failed to delete account: ${message}`, 500)
    }

    return jsonResponse({ message: 'Account deleted successfully' })
  }

  return errorResponse('Method not allowed', 405)
})
