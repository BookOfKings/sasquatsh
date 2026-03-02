import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

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
    bio: row.bio as string | null,
    favoriteGames: row.favorite_games as string[] | null,
    preferredGameTypes: row.preferred_game_types as string[] | null,
    isAdmin: row.is_admin as boolean ?? false,
    blockedUserIds: row.blocked_user_ids as string[] ?? [],
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
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
    createdAt: row.created_at as string,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

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
    const { data: registrations } = await supabase
      .from('event_registrations')
      .select(`
        status,
        event:events(id, title, event_date, start_time, city, state)
      `)
      .eq('user_id', user.id)
      .in('status', ['pending', 'confirmed'])
      .gte('events.event_date', new Date().toISOString().split('T')[0])
      .order('events(event_date)', { ascending: true })
      .limit(5)

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

  return errorResponse('Method not allowed', 405)
})
