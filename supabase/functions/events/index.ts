import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken, escapeFilterValue } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  // Create request-bound response functions for proper CORS
  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  // Get Firebase token from custom header
  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get the user from database including blocked users and subscription info
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id, blocked_user_ids, subscription_tier, subscription_override_tier, is_admin')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const blockedUserIds: string[] = user.blocked_user_ids ?? []

  const url = new URL(req.url)
  const eventId = url.searchParams.get('id')
  const type = url.searchParams.get('type')

  // GET - List events or single event
  if (req.method === 'GET') {
    // Get single event by ID
    if (eventId) {
      const { data, error } = await supabase
        .from('events')
        .select(`
          *,
          host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin, subscription_tier, subscription_override_tier),
          venue:event_locations!event_location_id(id, name, city, state, postal_code),
          registrations:event_registrations(
            id, user_id, status, registered_at,
            user:users(id, display_name, avatar_url, is_founding_member, is_admin)
          ),
          items:event_items(
            id, item_name, item_category, quantity_needed,
            claimed_by_user_id, claimed_at,
            claimed_by:users!claimed_by_user_id(display_name)
          ),
          games:event_games(
            id, bgg_id, game_name, thumbnail_url,
            min_players, max_players, playing_time,
            is_primary, is_alternative
          ),
          mtg_config:mtg_event_config(
            format_id, custom_format_name, event_type, rounds_count, round_time_minutes,
            pods_size, allow_proxies, proxy_limit, power_level_min, power_level_max,
            banned_cards, packs_per_player, draft_style, cube_id, has_prizes,
            prize_structure, entry_fee, entry_fee_currency, require_deck_registration,
            deck_submission_deadline, allow_spectators
          ),
          pokemon_config:pokemon_event_config(
            format_id, custom_format_name, event_type, tournament_style, rounds_count,
            round_time_minutes, best_of, top_cut, allow_proxies, proxy_limit,
            require_deck_registration, deck_submission_deadline, allow_deck_changes,
            has_prizes, prize_structure, entry_fee, entry_fee_currency, use_play_points,
            has_junior_division, has_senior_division, has_masters_division, allow_spectators
          )
        `)
        .eq('id', eventId)
        .single()

      if (error) {
        if (error.code === 'PGRST116') {
          return errorResponse('Event not found', 404)
        }
        return errorResponse(error.message, 500)
      }

      // Check access: public+published, or user is host, or user is registered, or user is admin
      // Also allow group members to see planned events from their group
      const isHost = data.host_user_id === user.id
      const isAdmin = user.is_admin === true
      const isPublicAndPublished = data.is_public && data.status === 'published'
      const isRegistered = Array.isArray(data.registrations) &&
        data.registrations.some((r: { user_id: string }) => r.user_id === user.id)

      // Check if user is a group member for planned events
      let isGroupMember = false
      if (data.group_id && data.from_planning_session_id) {
        const { data: membership } = await supabase
          .from('group_memberships')
          .select('id')
          .eq('group_id', data.group_id)
          .eq('user_id', user.id)
          .single()
        isGroupMember = !!membership
      }

      if (!isHost && !isAdmin && !isPublicAndPublished && !isRegistered && !isGroupMember) {
        return errorResponse('Event not found', 404)
      }

      // If multi-table event, fetch tables and sessions
      let tables = null
      let sessions = null
      if (data.is_multi_table) {
        // Fetch tables
        const { data: tablesData } = await supabase
          .from('event_tables')
          .select('id, table_number, table_name')
          .eq('event_id', eventId)
          .order('table_number')

        tables = tablesData

        // Fetch sessions with registrations
        const { data: sessionsData } = await supabase
          .from('event_game_sessions')
          .select(`
            id, table_id, bgg_id, game_name, thumbnail_url,
            min_players, max_players, slot_index, start_time,
            duration_minutes, status,
            registrations:game_session_registrations(
              id, user_id, is_host_reserved,
              user:users(id, display_name, avatar_url)
            )
          `)
          .eq('event_id', eventId)
          .order('slot_index')

        // Get user's registered sessions
        const { data: userRegs } = await supabase
          .from('game_session_registrations')
          .select('session_id')
          .eq('user_id', user.id)

        const userSessionIds = new Set(userRegs?.map(r => r.session_id) || [])

        sessions = (sessionsData ?? []).map(s => {
          const regs = (s.registrations as Array<{
            id: string
            user_id: string
            is_host_reserved: boolean
            user: { id: string; display_name: string | null; avatar_url: string | null } | null
          }>) ?? []

          const table = (tablesData ?? []).find(t => t.id === s.table_id)

          return {
            id: s.id,
            tableId: s.table_id,
            tableNumber: table?.table_number ?? 0,
            bggId: s.bgg_id,
            gameName: s.game_name,
            thumbnailUrl: s.thumbnail_url,
            minPlayers: s.min_players,
            maxPlayers: s.max_players,
            slotIndex: s.slot_index,
            startTime: s.start_time,
            durationMinutes: s.duration_minutes,
            status: s.status,
            registeredCount: regs.length,
            isFull: s.max_players ? regs.length >= s.max_players : false,
            isUserRegistered: userSessionIds.has(s.id),
            registrations: regs.map(r => ({
              userId: r.user_id,
              displayName: r.user?.display_name ?? null,
              avatarUrl: r.user?.avatar_url ?? null,
              isHostReserved: r.is_host_reserved,
            })),
          }
        })
      }

      return jsonResponse(transformEvent(data, tables, sessions))
    }

    // Browse public events (with blocked user filtering)
    if (type === 'browse') {
      const city = url.searchParams.get('city')
      const state = url.searchParams.get('state')
      const gameCategory = url.searchParams.get('gameCategory')
      const difficulty = url.searchParams.get('difficulty')
      const dateFrom = url.searchParams.get('dateFrom')
      const dateTo = url.searchParams.get('dateTo')
      const search = url.searchParams.get('search')
      const nearbyZip = url.searchParams.get('nearbyZip')
      const radiusMiles = url.searchParams.get('radiusMiles')
      const venueId = url.searchParams.get('venueId')

      // If radius search is requested, get list of zip codes within radius
      let nearbyZipCodes: string[] | null = null
      if (nearbyZip && radiusMiles) {
        const radius = parseInt(radiusMiles, 10) || 25
        const { data: zipsData } = await supabase
          .rpc('get_zips_within_radius', { center_zip: nearbyZip, radius_miles: radius })

        if (zipsData && zipsData.length > 0) {
          nearbyZipCodes = zipsData.map((z: { zip: string }) => z.zip)
        }
      }

      let query = supabase
        .from('events')
        .select(`
          id, title, game_title, game_category, event_date, start_time,
          duration_minutes, city, state, postal_code, difficulty_level, max_players, host_is_playing,
          is_public, is_charity_event, min_age, status, host_user_id,
          host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
          registrations:event_registrations(count),
          games:event_games(thumbnail_url, is_primary)
        `)
        .eq('is_public', true)
        .eq('status', 'published')
        .gte('event_date', new Date().toISOString().split('T')[0])
        .order('event_date', { ascending: true })

      // Filter out events hosted by blocked users
      if (blockedUserIds.length > 0) {
        query = query.not('host_user_id', 'in', `(${blockedUserIds.join(',')})`)
      }

      // Apply radius filter if we have nearby zip codes
      if (nearbyZipCodes && nearbyZipCodes.length > 0) {
        query = query.in('postal_code', nearbyZipCodes)
      }

      // Apply other filters (city/state only if not using radius search)
      if (!nearbyZipCodes) {
        if (city) query = query.ilike('city', `%${city}%`)
        if (state) query = query.eq('state', state)
      }
      if (gameCategory) query = query.eq('game_category', gameCategory)
      if (difficulty) query = query.eq('difficulty_level', difficulty)
      if (dateFrom) query = query.gte('event_date', dateFrom)
      if (dateTo) query = query.lte('event_date', dateTo)
      if (search) {
        const safeSearch = escapeFilterValue(search)
        query = query.or(`title.ilike.%${safeSearch}%,game_title.ilike.%${safeSearch}%`)
      }
      if (venueId) query = query.eq('event_location_id', venueId)

      const { data, error } = await query

      if (error) return errorResponse(error.message, 500)

      // Also filter out events where blocked users are participants
      let filteredData = data
      if (blockedUserIds.length > 0) {
        // Get event IDs with blocked participants
        const { data: blockedParticipantEvents } = await supabase
          .from('event_registrations')
          .select('event_id')
          .in('user_id', blockedUserIds)
          .in('status', ['pending', 'confirmed'])

        if (blockedParticipantEvents && blockedParticipantEvents.length > 0) {
          const blockedEventIds = new Set(blockedParticipantEvents.map(e => e.event_id))
          filteredData = data.filter(e => !blockedEventIds.has(e.id as string))
        }
      }

      return jsonResponse(filteredData.map(transformEventSummary))
    }

    if (type === 'hosted') {
      // Get events hosted by user
      const { data, error } = await supabase
        .from('events')
        .select(`
          id, title, game_title, game_category, event_date, start_time,
          duration_minutes, city, state, difficulty_level, max_players, host_is_playing,
          is_public, is_charity_event, min_age, status,
          host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
          registrations:event_registrations(count),
          games:event_games(thumbnail_url, is_primary)
        `)
        .eq('host_user_id', user.id)
        .order('event_date', { ascending: true })

      if (error) return errorResponse(error.message, 500)
      return jsonResponse(data.map(transformEventSummary))
    }

    if (type === 'registered') {
      // Get events user is registered for
      const { data, error } = await supabase
        .from('event_registrations')
        .select(`
          event:events(
            id, title, game_title, game_category, event_date, start_time,
            duration_minutes, city, state, difficulty_level, max_players, host_is_playing,
            is_public, is_charity_event, min_age, status,
            host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
            registrations:event_registrations(count),
            games:event_games(thumbnail_url, is_primary)
          )
        `)
        .eq('user_id', user.id)
        .in('status', ['pending', 'confirmed'])

      if (error) return errorResponse(error.message, 500)
      return jsonResponse(
        data
          .map((r) => r.event)
          .filter((e): e is NonNullable<typeof e> => e !== null)
          .map(transformEventSummary)
      )
    }

    return errorResponse('Invalid type parameter', 400)
  }

  // POST - Create event
  if (req.method === 'POST') {
    const body = await req.json()

    // Check subscription tier limits for event creation
    const effectiveTier = user.subscription_override_tier || user.subscription_tier || 'free'
    const tierLimits: Record<string, number> = {
      free: 1,
      basic: 5,
      pro: 10,
      premium: Infinity,
    }
    const maxActiveEvents = tierLimits[effectiveTier] ?? 1

    // Count user's active events (upcoming, not past)
    const today = new Date().toISOString().split('T')[0]
    const { count: activeEventCount, error: countError } = await supabase
      .from('events')
      .select('id', { count: 'exact', head: true })
      .eq('host_user_id', user.id)
      .gte('event_date', today)

    if (countError) {
      return errorResponse('Failed to check event limits', 500)
    }

    if ((activeEventCount ?? 0) >= maxActiveEvents) {
      return errorResponse(
        JSON.stringify({
          code: 'TIER_LIMIT_REACHED',
          message: `You have reached your limit of ${maxActiveEvents} active game${maxActiveEvents === 1 ? '' : 's'}. Upgrade your plan to host more games.`,
          currentCount: activeEventCount,
          limit: maxActiveEvents,
          tier: effectiveTier,
        }),
        403
      )
    }

    // If groupId is provided, verify the user is owner/admin of that group
    if (body.groupId) {
      const { data: membership } = await supabase
        .from('group_memberships')
        .select('role')
        .eq('group_id', body.groupId)
        .eq('user_id', user.id)
        .single()

      if (!membership || !['owner', 'admin'].includes(membership.role)) {
        return errorResponse('You must be an owner or admin of the group to create events for it', 403)
      }
    }

    // Require either a venue or a city+postal code for location (postal code needed for radius search)
    const hasVenue = !!body.eventLocationId
    const hasCustomLocation = !!body.city?.trim() && !!body.postalCode?.trim()
    if (!hasVenue && !hasCustomLocation) {
      if (body.city?.trim() && !body.postalCode?.trim()) {
        return errorResponse('Postal/zip code is required for radius-based search to work.', 400)
      }
      return errorResponse('A location is required. Please select a venue or enter a city and zip code.', 400)
    }

    const { data, error } = await supabase
      .from('events')
      .insert({
        host_user_id: user.id,
        group_id: body.groupId || null,
        title: body.title,
        description: body.description,
        game_title: body.gameTitle,
        game_category: body.gameCategory,
        event_date: body.eventDate,
        start_time: body.startTime,
        timezone: body.timezone || 'America/New_York',
        duration_minutes: body.durationMinutes ?? 120,
        setup_minutes: body.setupMinutes ?? 15,
        address_line1: body.addressLine1,
        city: body.city,
        state: body.state,
        postal_code: body.postalCode,
        location_details: body.locationDetails,
        event_location_id: body.eventLocationId || null,
        venue_hall: body.venueHall || null,
        venue_room: body.venueRoom || null,
        venue_table: body.venueTable || null,
        difficulty_level: body.difficultyLevel,
        max_players: body.maxPlayers ?? 4,
        host_is_playing: body.hostIsPlaying ?? true,
        is_public: body.isPublic ?? true,
        is_charity_event: body.isCharityEvent ?? false,
        min_age: body.minAge ?? null,
        status: body.status ?? 'draft',
      })
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin)
      `)
      .single()

    if (error) return errorResponse(error.message, 500)

    // If MTG config provided, insert into mtg_event_config table
    if (body.mtgConfig && body.gameSystem === 'mtg') {
      const mtgConfig = body.mtgConfig
      await supabase.from('mtg_event_config').insert({
        event_id: data.id,
        format_id: mtgConfig.formatId,
        custom_format_name: mtgConfig.customFormatName,
        event_type: mtgConfig.eventType || 'casual',
        rounds_count: mtgConfig.roundsCount,
        round_time_minutes: mtgConfig.roundTimeMinutes,
        pods_size: mtgConfig.podsSize,
        allow_proxies: mtgConfig.allowProxies ?? false,
        proxy_limit: mtgConfig.proxyLimit,
        power_level_min: mtgConfig.powerLevelMin,
        power_level_max: mtgConfig.powerLevelMax,
        banned_cards: mtgConfig.bannedCards || [],
        packs_per_player: mtgConfig.packsPerPlayer,
        draft_style: mtgConfig.draftStyle,
        cube_id: mtgConfig.cubeId,
        has_prizes: mtgConfig.hasPrizes ?? false,
        prize_structure: mtgConfig.prizeStructure,
        entry_fee: mtgConfig.entryFee,
        entry_fee_currency: mtgConfig.entryFeeCurrency || 'USD',
        require_deck_registration: mtgConfig.requireDeckRegistration ?? false,
        deck_submission_deadline: mtgConfig.deckSubmissionDeadline,
        allow_spectators: mtgConfig.allowSpectators ?? true,
      })
    }

    // If Pokemon config provided, insert into pokemon_event_config table
    if (body.pokemonConfig && body.gameSystem === 'pokemon_tcg') {
      const pokemonConfig = body.pokemonConfig
      await supabase.from('pokemon_event_config').insert({
        event_id: data.id,
        format_id: pokemonConfig.formatId,
        custom_format_name: pokemonConfig.customFormatName,
        event_type: pokemonConfig.eventType || 'casual',
        tournament_style: pokemonConfig.tournamentStyle,
        rounds_count: pokemonConfig.roundsCount,
        round_time_minutes: pokemonConfig.roundTimeMinutes,
        best_of: pokemonConfig.bestOf,
        top_cut: pokemonConfig.topCut,
        allow_proxies: pokemonConfig.allowProxies ?? false,
        proxy_limit: pokemonConfig.proxyLimit,
        require_deck_registration: pokemonConfig.requireDeckRegistration ?? false,
        deck_submission_deadline: pokemonConfig.deckSubmissionDeadline,
        allow_deck_changes: pokemonConfig.allowDeckChanges ?? false,
        has_prizes: pokemonConfig.hasPrizes ?? false,
        prize_structure: pokemonConfig.prizeStructure,
        entry_fee: pokemonConfig.entryFee,
        entry_fee_currency: pokemonConfig.entryFeeCurrency || 'USD',
        use_play_points: pokemonConfig.usePlayPoints ?? false,
        has_junior_division: pokemonConfig.hasJuniorDivision ?? false,
        has_senior_division: pokemonConfig.hasSeniorDivision ?? false,
        has_masters_division: pokemonConfig.hasMastersDivision ?? true,
        allow_spectators: pokemonConfig.allowSpectators ?? true,
      })
    }

    // Award raffle entry for hosting an event (only if published)
    if (data.status === 'published') {
      try {
        await supabase.rpc('award_raffle_entry', {
          p_user_id: user.id,
          p_entry_type: 'host_event',
          p_source_id: data.id,
        })
      } catch (err) {
        // Don't fail event creation if raffle entry fails
        console.error('Failed to award raffle entry:', err)
      }
    }

    // Re-fetch event with config joins to return complete data
    const { data: fullEvent } = await supabase
      .from('events')
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
        mtg_config:mtg_event_config(
          format_id, custom_format_name, event_type, rounds_count, round_time_minutes,
          pods_size, allow_proxies, proxy_limit, power_level_min, power_level_max,
          banned_cards, packs_per_player, draft_style, cube_id, has_prizes,
          prize_structure, entry_fee, entry_fee_currency, require_deck_registration,
          deck_submission_deadline, allow_spectators
        ),
        pokemon_config:pokemon_event_config(
          format_id, custom_format_name, event_type, tournament_style, rounds_count,
          round_time_minutes, best_of, top_cut, allow_proxies, proxy_limit,
          require_deck_registration, deck_submission_deadline, allow_deck_changes,
          has_prizes, prize_structure, entry_fee, entry_fee_currency, use_play_points,
          has_junior_division, has_senior_division, has_masters_division, allow_spectators
        )
      `)
      .eq('id', data.id)
      .single()

    return jsonResponse(transformEvent(fullEvent || data))
  }

  // PUT - Update event
  if (req.method === 'PUT') {
    if (!eventId) return errorResponse('Event ID required', 400)

    // Verify ownership or admin status
    const { data: existing } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!existing) {
      return errorResponse('Event not found', 404)
    }

    // Allow host or site admin to edit
    if (existing.host_user_id !== user.id && !user.is_admin) {
      return errorResponse('Not authorized', 403)
    }

    const body = await req.json()

    // Require either a venue or a city+postal code for location (postal code needed for radius search)
    const hasVenue = !!body.eventLocationId
    const hasCustomLocation = !!body.city?.trim() && !!body.postalCode?.trim()
    if (!hasVenue && !hasCustomLocation) {
      if (body.city?.trim() && !body.postalCode?.trim()) {
        return errorResponse('Postal/zip code is required for radius-based search to work.', 400)
      }
      return errorResponse('A location is required. Please select a venue or enter a city and zip code.', 400)
    }

    const { data, error } = await supabase
      .from('events')
      .update({
        title: body.title,
        description: body.description,
        game_title: body.gameTitle,
        game_category: body.gameCategory,
        event_date: body.eventDate,
        start_time: body.startTime,
        timezone: body.timezone,
        duration_minutes: body.durationMinutes,
        setup_minutes: body.setupMinutes,
        address_line1: body.addressLine1,
        city: body.city,
        state: body.state,
        postal_code: body.postalCode,
        location_details: body.locationDetails,
        event_location_id: body.eventLocationId || null,
        venue_hall: body.venueHall || null,
        venue_room: body.venueRoom || null,
        venue_table: body.venueTable || null,
        difficulty_level: body.difficultyLevel,
        max_players: body.maxPlayers,
        host_is_playing: body.hostIsPlaying,
        is_public: body.isPublic,
        is_charity_event: body.isCharityEvent,
        min_age: body.minAge,
        status: body.status,
        planned_games: body.plannedGames !== undefined ? body.plannedGames : undefined,
        updated_at: new Date().toISOString(),
      })
      .eq('id', eventId)
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
        registrations:event_registrations(
          id, user_id, status, registered_at,
          user:users(id, display_name, avatar_url, is_founding_member, is_admin)
        ),
        items:event_items(
          id, item_name, item_category, quantity_needed,
          claimed_by_user_id, claimed_at,
          claimed_by:users!claimed_by_user_id(display_name)
        ),
        games:event_games(
          id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time,
          is_primary, is_alternative
        )
      `)
      .single()

    if (error) return errorResponse(error.message, 500)

    // If MTG config provided, upsert into mtg_event_config table
    if (body.mtgConfig && body.gameSystem === 'mtg') {
      const mtgConfig = body.mtgConfig
      await supabase.from('mtg_event_config').upsert({
        event_id: eventId,
        format_id: mtgConfig.formatId,
        custom_format_name: mtgConfig.customFormatName,
        event_type: mtgConfig.eventType || 'casual',
        rounds_count: mtgConfig.roundsCount,
        round_time_minutes: mtgConfig.roundTimeMinutes,
        pods_size: mtgConfig.podsSize,
        allow_proxies: mtgConfig.allowProxies ?? false,
        proxy_limit: mtgConfig.proxyLimit,
        power_level_min: mtgConfig.powerLevelMin,
        power_level_max: mtgConfig.powerLevelMax,
        banned_cards: mtgConfig.bannedCards || [],
        packs_per_player: mtgConfig.packsPerPlayer,
        draft_style: mtgConfig.draftStyle,
        cube_id: mtgConfig.cubeId,
        has_prizes: mtgConfig.hasPrizes ?? false,
        prize_structure: mtgConfig.prizeStructure,
        entry_fee: mtgConfig.entryFee,
        entry_fee_currency: mtgConfig.entryFeeCurrency || 'USD',
        require_deck_registration: mtgConfig.requireDeckRegistration ?? false,
        deck_submission_deadline: mtgConfig.deckSubmissionDeadline,
        allow_spectators: mtgConfig.allowSpectators ?? true,
      }, { onConflict: 'event_id' })
    }

    // If Pokemon config provided, upsert into pokemon_event_config table
    if (body.pokemonConfig && body.gameSystem === 'pokemon_tcg') {
      const pokemonConfig = body.pokemonConfig
      await supabase.from('pokemon_event_config').upsert({
        event_id: eventId,
        format_id: pokemonConfig.formatId,
        custom_format_name: pokemonConfig.customFormatName,
        event_type: pokemonConfig.eventType || 'casual',
        tournament_style: pokemonConfig.tournamentStyle,
        rounds_count: pokemonConfig.roundsCount,
        round_time_minutes: pokemonConfig.roundTimeMinutes,
        best_of: pokemonConfig.bestOf,
        top_cut: pokemonConfig.topCut,
        allow_proxies: pokemonConfig.allowProxies ?? false,
        proxy_limit: pokemonConfig.proxyLimit,
        require_deck_registration: pokemonConfig.requireDeckRegistration ?? false,
        deck_submission_deadline: pokemonConfig.deckSubmissionDeadline,
        allow_deck_changes: pokemonConfig.allowDeckChanges ?? false,
        has_prizes: pokemonConfig.hasPrizes ?? false,
        prize_structure: pokemonConfig.prizeStructure,
        entry_fee: pokemonConfig.entryFee,
        entry_fee_currency: pokemonConfig.entryFeeCurrency || 'USD',
        use_play_points: pokemonConfig.usePlayPoints ?? false,
        has_junior_division: pokemonConfig.hasJuniorDivision ?? false,
        has_senior_division: pokemonConfig.hasSeniorDivision ?? false,
        has_masters_division: pokemonConfig.hasMastersDivision ?? true,
        allow_spectators: pokemonConfig.allowSpectators ?? true,
      }, { onConflict: 'event_id' })
    }

    // Award raffle entry if event just became published
    if (body.status === 'published') {
      try {
        await supabase.rpc('award_raffle_entry', {
          p_user_id: existing.host_user_id,
          p_entry_type: 'host_event',
          p_source_id: eventId,
        })
      } catch (err) {
        // Don't fail event update if raffle entry fails
        console.error('Failed to award raffle entry:', err)
      }
    }

    // Re-fetch event with config joins to return complete data
    const { data: fullEvent } = await supabase
      .from('events')
      .select(`
        *,
        host:users!host_user_id(id, display_name, avatar_url, is_founding_member, is_admin),
        registrations:event_registrations(
          id, user_id, status, registered_at,
          user:users(id, display_name, avatar_url, is_founding_member, is_admin)
        ),
        items:event_items(
          id, item_name, item_category, quantity_needed,
          claimed_by_user_id, claimed_at,
          claimed_by:users!claimed_by_user_id(display_name)
        ),
        games:event_games(
          id, bgg_id, game_name, thumbnail_url,
          min_players, max_players, playing_time,
          is_primary, is_alternative
        ),
        mtg_config:mtg_event_config(
          format_id, custom_format_name, event_type, rounds_count, round_time_minutes,
          pods_size, allow_proxies, proxy_limit, power_level_min, power_level_max,
          banned_cards, packs_per_player, draft_style, cube_id, has_prizes,
          prize_structure, entry_fee, entry_fee_currency, require_deck_registration,
          deck_submission_deadline, allow_spectators
        ),
        pokemon_config:pokemon_event_config(
          format_id, custom_format_name, event_type, tournament_style, rounds_count,
          round_time_minutes, best_of, top_cut, allow_proxies, proxy_limit,
          require_deck_registration, deck_submission_deadline, allow_deck_changes,
          has_prizes, prize_structure, entry_fee, entry_fee_currency, use_play_points,
          has_junior_division, has_senior_division, has_masters_division, allow_spectators
        )
      `)
      .eq('id', eventId)
      .single()

    return jsonResponse(transformEvent(fullEvent || data))
  }

  // DELETE - Delete event
  if (req.method === 'DELETE') {
    if (!eventId) return errorResponse('Event ID required', 400)

    // Verify ownership or admin status
    const { data: existing } = await supabase
      .from('events')
      .select('host_user_id')
      .eq('id', eventId)
      .single()

    if (!existing) {
      return errorResponse('Event not found', 404)
    }

    // Allow host or site admin to delete
    if (existing.host_user_id !== user.id && !user.is_admin) {
      return errorResponse('Not authorized', 403)
    }

    const { error } = await supabase
      .from('events')
      .delete()
      .eq('id', eventId)

    if (error) return errorResponse(error.message, 500)
    return new Response(null, { status: 204, headers: getCorsHeaders(req) })
  }

  return errorResponse('Method not allowed', 405)
})

function transformEventSummary(row: Record<string, unknown>) {
  // Find the primary game's thumbnail, or fall back to any game's thumbnail
  const games = row.games as { thumbnail_url: string | null; is_primary: boolean }[] | null
  const primaryGame = games?.find(g => g.is_primary)
  const anyGameWithThumbnail = games?.find(g => g.thumbnail_url)
  const primaryGameThumbnail = primaryGame?.thumbnail_url || anyGameWithThumbnail?.thumbnail_url || null

  return {
    id: row.id,
    title: row.title,
    gameTitle: row.game_title,
    gameCategory: row.game_category,
    eventDate: row.event_date,
    startTime: row.start_time,
    timezone: row.timezone,
    durationMinutes: row.duration_minutes,
    city: row.city,
    state: row.state,
    difficultyLevel: row.difficulty_level,
    maxPlayers: row.max_players,
    hostIsPlaying: row.host_is_playing ?? true,
    // confirmedCount includes host if they're playing, plus registrations
    confirmedCount: ((row.registrations as { count: number }[])?.[0]?.count ?? 0) + (row.host_is_playing !== false ? 1 : 0),
    isPublic: row.is_public,
    isCharityEvent: row.is_charity_event,
    minAge: row.min_age,
    status: row.status,
    primaryGameThumbnail,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id,
          displayName: (row.host as Record<string, unknown>).display_name,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url,
          isFoundingMember: (row.host as Record<string, unknown>).is_founding_member,
          isAdmin: (row.host as Record<string, unknown>).is_admin,
        }
      : null,
  }
}

function transformEvent(
  row: Record<string, unknown>,
  tables?: { id: string; table_number: number; table_name: string | null }[] | null,
  sessions?: Record<string, unknown>[] | null
) {
  return {
    id: row.id,
    hostUserId: row.host_user_id,
    title: row.title,
    description: row.description,
    gameTitle: row.game_title,
    gameCategory: row.game_category,
    gameSystem: row.game_system ?? 'board_game',
    eventDate: row.event_date,
    startTime: row.start_time,
    timezone: row.timezone,
    durationMinutes: row.duration_minutes,
    setupMinutes: row.setup_minutes,
    addressLine1: row.address_line1,
    city: row.city,
    state: row.state,
    postalCode: row.postal_code,
    locationDetails: row.location_details,
    eventLocationId: row.event_location_id,
    venueHall: row.venue_hall,
    venueRoom: row.venue_room,
    venueTable: row.venue_table,
    difficultyLevel: row.difficulty_level,
    maxPlayers: row.max_players,
    hostIsPlaying: row.host_is_playing ?? true,
    // confirmedCount includes host if they're playing, plus confirmed registrations only
    confirmedCount: (Array.isArray(row.registrations)
      ? row.registrations.filter((r: { status: string }) => r.status === 'confirmed').length
      : 0) + (row.host_is_playing !== false ? 1 : 0),
    isPublic: row.is_public,
    isCharityEvent: row.is_charity_event,
    isMultiTable: row.is_multi_table ?? false,
    minAge: row.min_age,
    status: row.status,
    groupId: row.group_id,
    fromPlanningSessionId: row.from_planning_session_id,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id,
          displayName: (row.host as Record<string, unknown>).display_name,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url,
          isFoundingMember: (row.host as Record<string, unknown>).is_founding_member,
          isAdmin: (row.host as Record<string, unknown>).is_admin,
          subscriptionTier: (row.host as Record<string, unknown>).subscription_tier,
          subscriptionOverrideTier: (row.host as Record<string, unknown>).subscription_override_tier,
        }
      : null,
    venue: row.venue
      ? {
          id: (row.venue as Record<string, unknown>).id,
          name: (row.venue as Record<string, unknown>).name,
          city: (row.venue as Record<string, unknown>).city,
          state: (row.venue as Record<string, unknown>).state,
          postalCode: (row.venue as Record<string, unknown>).postal_code,
        }
      : null,
    registrations: Array.isArray(row.registrations)
      ? (row.registrations as Record<string, unknown>[]).map((r) => ({
          id: r.id,
          userId: r.user_id,
          status: r.status,
          registeredAt: r.registered_at,
          user: r.user
            ? {
                id: (r.user as Record<string, unknown>).id,
                displayName: (r.user as Record<string, unknown>).display_name,
                avatarUrl: (r.user as Record<string, unknown>).avatar_url,
                isFoundingMember: (r.user as Record<string, unknown>).is_founding_member,
                isAdmin: (r.user as Record<string, unknown>).is_admin,
              }
            : null,
        }))
      : null,
    items: Array.isArray(row.items)
      ? (row.items as Record<string, unknown>[]).map((i) => ({
          id: i.id,
          itemName: i.item_name,
          itemCategory: i.item_category,
          quantityNeeded: i.quantity_needed,
          claimedByUserId: i.claimed_by_user_id,
          claimedByName: i.claimed_by
            ? ((i.claimed_by as Record<string, unknown>).display_name as string)
            : null,
          claimedAt: i.claimed_at,
        }))
      : null,
    games: Array.isArray(row.games)
      ? (row.games as Record<string, unknown>[]).map((g) => ({
          id: g.id,
          bggId: g.bgg_id,
          gameName: g.game_name,
          thumbnailUrl: g.thumbnail_url,
          minPlayers: g.min_players,
          maxPlayers: g.max_players,
          playingTime: g.playing_time,
          isPrimary: g.is_primary,
          isAlternative: g.is_alternative,
        }))
      : null,
    plannedGames: row.planned_games ?? null,
    createdAt: row.created_at,
    // Multi-table session data
    tables: tables
      ? tables.map(t => ({
          id: t.id,
          tableNumber: t.table_number,
          tableName: t.table_name,
        }))
      : null,
    sessions: sessions ?? null,
    // MTG event configuration (from joined mtg_event_config table)
    mtgConfig: transformMtgConfig(row.mtg_config),
    // Pokemon TCG event configuration (from joined pokemon_event_config table)
    pokemonConfig: transformPokemonConfig(row.pokemon_config),
  }
}

// Transform MTG config from snake_case to camelCase
function transformMtgConfig(config: unknown): Record<string, unknown> | null {
  // Supabase returns an array for 1-to-1 relations via select, take first element
  const data = Array.isArray(config) ? config[0] : config
  if (!data) return null

  const c = data as Record<string, unknown>
  return {
    formatId: c.format_id,
    customFormatName: c.custom_format_name,
    eventType: c.event_type,
    roundsCount: c.rounds_count,
    roundTimeMinutes: c.round_time_minutes,
    podsSize: c.pods_size,
    allowProxies: c.allow_proxies,
    proxyLimit: c.proxy_limit,
    powerLevelMin: c.power_level_min,
    powerLevelMax: c.power_level_max,
    bannedCards: c.banned_cards,
    packsPerPlayer: c.packs_per_player,
    draftStyle: c.draft_style,
    cubeId: c.cube_id,
    hasPrizes: c.has_prizes,
    prizeStructure: c.prize_structure,
    entryFee: c.entry_fee,
    entryFeeCurrency: c.entry_fee_currency,
    requireDeckRegistration: c.require_deck_registration,
    deckSubmissionDeadline: c.deck_submission_deadline,
    allowSpectators: c.allow_spectators,
  }
}

// Transform Pokemon config from snake_case to camelCase
function transformPokemonConfig(config: unknown): Record<string, unknown> | null {
  // Supabase returns an array for 1-to-1 relations via select, take first element
  const data = Array.isArray(config) ? config[0] : config
  if (!data) return null

  const c = data as Record<string, unknown>
  return {
    formatId: c.format_id,
    customFormatName: c.custom_format_name,
    eventType: c.event_type,
    tournamentStyle: c.tournament_style,
    roundsCount: c.rounds_count,
    roundTimeMinutes: c.round_time_minutes,
    bestOf: c.best_of,
    topCut: c.top_cut,
    allowProxies: c.allow_proxies,
    proxyLimit: c.proxy_limit,
    requireDeckRegistration: c.require_deck_registration,
    deckSubmissionDeadline: c.deck_submission_deadline,
    allowDeckChanges: c.allow_deck_changes,
    hasPrizes: c.has_prizes,
    prizeStructure: c.prize_structure,
    entryFee: c.entry_fee,
    entryFeeCurrency: c.entry_fee_currency,
    usePlayPoints: c.use_play_points,
    hasJuniorDivision: c.has_junior_division,
    hasSeniorDivision: c.has_senior_division,
    hasMastersDivision: c.has_masters_division,
    allowSpectators: c.allow_spectators,
  }
}
