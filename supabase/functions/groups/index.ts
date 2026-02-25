import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, jsonResponse, errorResponse, getCorsHeaders } from '../_shared/firebase.ts'

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
    isPublic: row.is_public as boolean,
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
    isPublic: row.is_public as boolean,
    createdByUserId: row.created_by_user_id as string,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    memberCount: (row.memberships as unknown[])?.length ?? 0,
    creator: row.creator
      ? {
          id: (row.creator as Record<string, unknown>).id as string,
          displayName: (row.creator as Record<string, unknown>).display_name as string | null,
          avatarUrl: (row.creator as Record<string, unknown>).avatar_url as string | null,
        }
      : null,
  }
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders() })
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const groupId = url.searchParams.get('id')
  const slug = url.searchParams.get('slug')
  const action = url.searchParams.get('action') // join, leave

  // GET - List groups or get single group
  if (req.method === 'GET') {
    // Get single group by ID or slug
    if (groupId || slug) {
      let query = supabase
        .from('groups')
        .select(`
          *,
          creator:users!created_by_user_id(id, display_name, avatar_url),
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

    // List public groups
    const search = url.searchParams.get('search')
    const groupType = url.searchParams.get('type')
    const city = url.searchParams.get('city')
    const state = url.searchParams.get('state')

    let query = supabase
      .from('groups')
      .select(`
        id, name, slug, description, logo_url, group_type,
        location_city, location_state, is_public,
        memberships:group_memberships(count)
      `)
      .eq('is_public', true)
      .order('name', { ascending: true })

    if (search) {
      query = query.or(`name.ilike.%${search}%,description.ilike.%${search}%`)
    }
    if (groupType) {
      query = query.eq('group_type', groupType)
    }
    if (city) {
      query = query.ilike('location_city', `%${city}%`)
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
  const authHeader = req.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) {
    return errorResponse('Unauthorized', 401)
  }

  const token = authHeader.slice(7)
  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid token', 401)
  }

  // Get the user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  // POST - Create group or join group
  if (req.method === 'POST') {
    // Join group
    if (action === 'join' && groupId) {
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

    // Create new group
    const body = await req.json()

    if (!body.name?.trim()) {
      return errorResponse('Group name is required', 400)
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
        is_public: body.isPublic !== false,
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

  // PUT - Update group
  if (req.method === 'PUT') {
    if (!groupId) {
      return errorResponse('Group ID required', 400)
    }

    // Verify user is owner or admin
    const { data: membership } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('group_id', groupId)
      .eq('user_id', user.id)
      .single()

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
    if (body.isPublic !== undefined) updates.is_public = body.isPublic

    const { data, error } = await supabase
      .from('groups')
      .update(updates)
      .eq('id', groupId)
      .select(`
        *,
        creator:users!created_by_user_id(id, display_name, avatar_url),
        memberships:group_memberships(id)
      `)
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse(toGroup(data))
  }

  // DELETE - Delete group or leave group
  if (req.method === 'DELETE') {
    if (!groupId) {
      return errorResponse('Group ID required', 400)
    }

    // Leave group
    if (action === 'leave') {
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', groupId)
        .eq('user_id', user.id)
        .single()

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

      return new Response(null, { status: 204, headers: getCorsHeaders() })
    }

    // Delete group - owner only
    const { data: membership } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('group_id', groupId)
      .eq('user_id', user.id)
      .single()

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

    return new Response(null, { status: 204, headers: getCorsHeaders() })
  }

  return errorResponse('Method not allowed', 405)
})
