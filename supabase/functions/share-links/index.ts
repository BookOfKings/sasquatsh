import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

function generateCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 16; i++) {
    code += chars[Math.floor(Math.random() * chars.length)]
  }
  return code
}

async function resolveGroupCurrentTarget(
  supabase: ReturnType<typeof createClient>,
  groupId: string
): Promise<{ type: 'planning_session' | 'event'; id: string; title: string } | null> {
  // 1. Open planning session (people are still voting)
  const { data: openSession } = await supabase
    .from('planning_sessions')
    .select('id, title')
    .eq('group_id', groupId)
    .eq('status', 'open')
    .gt('response_deadline', new Date().toISOString())
    .order('response_deadline', { ascending: true })
    .limit(1)
    .single()

  if (openSession) {
    return { type: 'planning_session', id: openSession.id, title: openSession.title }
  }

  // 2. Next upcoming event from this group
  const today = new Date().toISOString().split('T')[0]
  const { data: nextEvent } = await supabase
    .from('events')
    .select('id, title')
    .eq('group_id', groupId)
    .gte('event_date', today)
    .order('event_date', { ascending: true })
    .order('start_time', { ascending: true })
    .limit(1)
    .single()

  if (nextEvent) {
    return { type: 'event', id: nextEvent.id, title: nextEvent.title }
  }

  return null
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)
  const code = url.searchParams.get('code')
  const action = url.searchParams.get('action')

  // GET ?code=XYZ — Preview the link (no auth required)
  if (req.method === 'GET' && code) {
    const { data: link, error } = await supabase
      .from('shareable_invite_links')
      .select('*, group:groups(id, name, slug, logo_url, city, state)')
      .eq('invite_code', code)
      .eq('is_active', true)
      .single()

    if (error || !link) return errorResponse('Invalid or expired invite link', 404)

    // Check expiry
    if (link.expires_at && new Date(link.expires_at) < new Date()) {
      return errorResponse('This invite link has expired', 410)
    }

    // Check max uses
    if (link.max_uses && link.uses_count >= link.max_uses) {
      return errorResponse('This invite link has reached its maximum uses', 410)
    }

    // Get creator info
    const { data: creator } = await supabase
      .from('users')
      .select('display_name, avatar_url')
      .eq('id', link.created_by_user_id)
      .single()

    // Resolve target
    let target: { type: string; id: string; title: string } | null = null

    if (link.link_type === 'session') {
      if (link.planning_session_id) {
        const { data: session } = await supabase
          .from('planning_sessions')
          .select('id, title, status, response_deadline')
          .eq('id', link.planning_session_id)
          .single()
        if (session) {
          target = { type: 'planning_session', id: session.id, title: session.title }
        }
      } else if (link.event_id) {
        const { data: event } = await supabase
          .from('events')
          .select('id, title')
          .eq('id', link.event_id)
          .single()
        if (event) {
          target = { type: 'event', id: event.id, title: event.title }
        }
      }
    } else {
      // group_recurring — resolve dynamically
      target = await resolveGroupCurrentTarget(supabase, link.group_id)
    }

    return jsonResponse({
      linkType: link.link_type,
      group: link.group ? {
        id: (link.group as any).id,
        name: (link.group as any).name,
        slug: (link.group as any).slug,
        logoUrl: (link.group as any).logo_url,
        city: (link.group as any).city,
        state: (link.group as any).state,
      } : null,
      target,
      invitedBy: creator ? {
        displayName: creator.display_name,
        avatarUrl: creator.avatar_url,
      } : null,
    })
  }

  // POST ?code=XYZ&action=accept — Accept the link (auth required)
  if (req.method === 'POST' && code && action === 'accept') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!user) return errorResponse('User not found', 404)

    // Validate link
    const { data: link } = await supabase
      .from('shareable_invite_links')
      .select('*')
      .eq('invite_code', code)
      .eq('is_active', true)
      .single()

    if (!link) return errorResponse('Invalid or expired invite link', 404)
    if (link.expires_at && new Date(link.expires_at) < new Date()) {
      return errorResponse('This invite link has expired', 410)
    }
    if (link.max_uses && link.uses_count >= link.max_uses) {
      return errorResponse('This invite link has reached its maximum uses', 410)
    }

    // Check if already used by this user
    const { data: existingUse } = await supabase
      .from('shareable_invite_link_uses')
      .select('id')
      .eq('link_id', link.id)
      .eq('user_id', user.id)
      .single()

    if (existingUse) {
      // Already used — just resolve target and redirect
      let target = null
      if (link.link_type === 'session' && link.planning_session_id) {
        target = { type: 'planning_session', id: link.planning_session_id }
      } else if (link.link_type === 'session' && link.event_id) {
        target = { type: 'event', id: link.event_id }
      } else {
        target = await resolveGroupCurrentTarget(supabase, link.group_id)
      }

      const { data: group } = await supabase
        .from('groups')
        .select('slug')
        .eq('id', link.group_id)
        .single()

      return jsonResponse({
        alreadyUsed: true,
        groupSlug: group?.slug,
        target,
      })
    }

    // 1. Join group if not already a member
    const { data: existingMember } = await supabase
      .from('group_memberships')
      .select('id, status')
      .eq('group_id', link.group_id)
      .eq('user_id', user.id)
      .single()

    if (!existingMember) {
      await supabase.from('group_memberships').insert({
        group_id: link.group_id,
        user_id: user.id,
        role: 'member',
        status: 'active',
      })
    } else if (existingMember.status !== 'active') {
      await supabase
        .from('group_memberships')
        .update({ status: 'active' })
        .eq('id', existingMember.id)
    }

    // 2. Resolve target
    let target: { type: string; id: string } | null = null

    if (link.link_type === 'session') {
      if (link.planning_session_id) {
        target = { type: 'planning_session', id: link.planning_session_id }
      } else if (link.event_id) {
        target = { type: 'event', id: link.event_id }
      }
    } else {
      target = await resolveGroupCurrentTarget(supabase, link.group_id)
    }

    // 3. Add to target
    if (target?.type === 'planning_session') {
      const { data: existingInvitee } = await supabase
        .from('planning_invitees')
        .select('id')
        .eq('session_id', target.id)
        .eq('user_id', user.id)
        .single()

      if (!existingInvitee) {
        await supabase.from('planning_invitees').insert({
          session_id: target.id,
          user_id: user.id,
          has_slot: true,
        })
      }
    } else if (target?.type === 'event') {
      const { data: existingReg } = await supabase
        .from('event_registrations')
        .select('id')
        .eq('event_id', target.id)
        .eq('user_id', user.id)
        .single()

      if (!existingReg) {
        await supabase.from('event_registrations').insert({
          event_id: target.id,
          user_id: user.id,
          status: 'confirmed',
        })
      }
    }

    // 4. Record use
    await supabase.from('shareable_invite_link_uses').insert({
      link_id: link.id,
      user_id: user.id,
    })
    await supabase
      .from('shareable_invite_links')
      .update({ uses_count: (link.uses_count || 0) + 1 })
      .eq('id', link.id)

    // Get group slug for redirect
    const { data: group } = await supabase
      .from('groups')
      .select('slug')
      .eq('id', link.group_id)
      .single()

    return jsonResponse({
      alreadyUsed: false,
      groupSlug: group?.slug,
      target: target || { type: 'group' },
    })
  }

  // POST ?action=create — Create a shareable link (auth required)
  if (req.method === 'POST' && action === 'create') {
    const token = getFirebaseToken(req)
    if (!token) return errorResponse('Authentication required', 401)
    const decoded = await verifyFirebaseToken(token)
    if (!decoded) return errorResponse('Invalid token', 401)

    const { data: user } = await supabase
      .from('users')
      .select('id')
      .eq('firebase_uid', decoded.uid)
      .single()
    if (!user) return errorResponse('User not found', 404)

    const body = await req.json()
    const { groupId, linkType, planningSessionId, eventId, maxUses, expiresInDays } = body

    if (!groupId) return errorResponse('groupId is required', 400)
    if (!linkType || !['session', 'group_recurring'].includes(linkType)) {
      return errorResponse('linkType must be "session" or "group_recurring"', 400)
    }
    if (linkType === 'session' && !planningSessionId && !eventId) {
      return errorResponse('Session link requires planningSessionId or eventId', 400)
    }

    // Verify user is a group member
    const { data: membership } = await supabase
      .from('group_memberships')
      .select('role')
      .eq('group_id', groupId)
      .eq('user_id', user.id)
      .eq('status', 'active')
      .single()

    if (!membership) return errorResponse('You must be a group member', 403)

    const inviteCode = generateCode()
    const expiresAt = expiresInDays
      ? new Date(Date.now() + expiresInDays * 24 * 60 * 60 * 1000).toISOString()
      : null

    const { data: link, error } = await supabase
      .from('shareable_invite_links')
      .insert({
        group_id: groupId,
        created_by_user_id: user.id,
        invite_code: inviteCode,
        link_type: linkType,
        planning_session_id: planningSessionId || null,
        event_id: eventId || null,
        max_uses: maxUses || null,
        expires_at: expiresAt,
      })
      .select()
      .single()

    if (error) {
      console.error('Failed to create link:', error)
      return errorResponse('Failed to create invite link', 500)
    }

    return jsonResponse({
      ...link,
      url: `https://sasquatsh.com/join/${inviteCode}`,
    }, 201)
  }

  return errorResponse('Method not allowed', 405)
})
