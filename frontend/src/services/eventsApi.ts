import { supabase } from './supabase'
import type {
  Event,
  EventSummary,
  EventGameSummary,
  CreateEventInput,
  UpdateEventInput,
  EventItem,
  CreateEventItemInput,
  EventSearchFilter,
} from '@/types/events'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests to Edge Functions
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// Transform database row to EventSummary
function toEventSummary(row: Record<string, unknown>): EventSummary {
  // Find the primary game's thumbnail
  const games = row.games as { thumbnail_url: string | null; is_primary: boolean }[] | null
  const primaryGame = games?.find(g => g.is_primary)
  const anyGameWithThumbnail = games?.find(g => g.thumbnail_url)
  const primaryGameThumbnail = primaryGame?.thumbnail_url || anyGameWithThumbnail?.thumbnail_url || null

  return {
    id: row.id as string,
    title: row.title as string,
    gameTitle: row.game_title as string | null,
    gameCategory: row.game_category as string | null,
    eventDate: row.event_date as string,
    startTime: row.start_time as string,
    timezone: row.timezone as string | null,
    durationMinutes: row.duration_minutes as number,
    city: row.city as string | null,
    state: row.state as string | null,
    difficultyLevel: row.difficulty_level as string | null,
    maxPlayers: row.max_players as number,
    hostIsPlaying: (row.host_is_playing as boolean) ?? true,
    confirmedCount: ((row.registrations as { count: number }[])?.[0]?.count ?? 0) + (row.host_is_playing !== false ? 1 : 0),
    isPublic: row.is_public as boolean,
    isCharityEvent: row.is_charity_event as boolean,
    minAge: row.min_age as number | null,
    status: row.status as string,
    primaryGameThumbnail,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id as string,
          displayName: (row.host as Record<string, unknown>).display_name as string | null,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url as string | null,
        }
      : null,
  }
}

// Transform database row to full Event
function toEvent(row: Record<string, unknown>): Event {
  return {
    id: row.id as string,
    hostUserId: row.host_user_id as string,
    title: row.title as string,
    description: row.description as string | null,
    gameTitle: row.game_title as string | null,
    gameCategory: row.game_category as string | null,
    gameSystem: (row.game_system as Event['gameSystem']) ?? 'board_game',
    eventDate: row.event_date as string,
    startTime: row.start_time as string,
    timezone: row.timezone as string | null,
    durationMinutes: row.duration_minutes as number,
    setupMinutes: row.setup_minutes as number,
    addressLine1: row.address_line1 as string | null,
    city: row.city as string | null,
    state: row.state as string | null,
    postalCode: row.postal_code as string | null,
    locationDetails: row.location_details as string | null,
    eventLocationId: row.event_location_id as string | null,
    venueHall: row.venue_hall as string | null,
    venueRoom: row.venue_room as string | null,
    venueTable: row.venue_table as string | null,
    difficultyLevel: row.difficulty_level as string | null,
    maxPlayers: row.max_players as number,
    hostIsPlaying: (row.host_is_playing as boolean) ?? true,
    confirmedCount: ((row.registrations as unknown[])?.length ?? 0) + (row.host_is_playing !== false ? 1 : 0),
    isPublic: row.is_public as boolean,
    isCharityEvent: row.is_charity_event as boolean,
    isMultiTable: (row.is_multi_table as boolean) ?? false,
    minAge: row.min_age as number | null,
    status: row.status as string,
    host: row.host
      ? {
          id: (row.host as Record<string, unknown>).id as string,
          displayName: (row.host as Record<string, unknown>).display_name as string | null,
          avatarUrl: (row.host as Record<string, unknown>).avatar_url as string | null,
          subscriptionTier: (row.host as Record<string, unknown>).subscription_tier as 'free' | 'basic' | 'pro' | 'premium' | undefined,
          subscriptionOverrideTier: (row.host as Record<string, unknown>).subscription_override_tier as 'free' | 'basic' | 'pro' | 'premium' | undefined,
        }
      : null,
    venue: row.venue
      ? {
          id: (row.venue as Record<string, unknown>).id as string,
          name: (row.venue as Record<string, unknown>).name as string,
          city: (row.venue as Record<string, unknown>).city as string,
          state: (row.venue as Record<string, unknown>).state as string,
          postalCode: (row.venue as Record<string, unknown>).postal_code as string | null,
        }
      : null,
    registrations: (row.registrations as Record<string, unknown>[])?.map((r) => ({
      id: r.id as string,
      userId: r.user_id as string,
      status: r.status as string,
      registeredAt: r.registered_at as string,
      user: r.user
        ? {
            id: (r.user as Record<string, unknown>).id as string,
            displayName: (r.user as Record<string, unknown>).display_name as string | null,
            avatarUrl: (r.user as Record<string, unknown>).avatar_url as string | null,
          }
        : null,
    })) ?? null,
    items: (row.items as Record<string, unknown>[])?.map((i) => ({
      id: i.id as string,
      itemName: i.item_name as string,
      itemCategory: i.item_category as string,
      quantityNeeded: i.quantity_needed as number,
      claimedByUserId: i.claimed_by_user_id as string | null,
      claimedByName: i.claimed_by
        ? ((i.claimed_by as Record<string, unknown>).display_name as string | null)
        : null,
      claimedAt: i.claimed_at as string | null,
    })) ?? null,
    games: (row.games as Record<string, unknown>[])?.map((g): EventGameSummary => ({
      id: g.id as string,
      bggId: g.bgg_id as number | null,
      gameName: g.game_name as string,
      thumbnailUrl: g.thumbnail_url as string | null,
      minPlayers: g.min_players as number | null,
      maxPlayers: g.max_players as number | null,
      playingTime: g.playing_time as number | null,
      isPrimary: g.is_primary as boolean,
      isAlternative: g.is_alternative as boolean,
    })) ?? null,
    plannedGames: row.plannedGames as Event['plannedGames'] ?? null,
    groupId: row.groupId as string | null ?? null,
    fromPlanningSessionId: row.fromPlanningSessionId as string | null ?? null,
    createdAt: row.created_at as string,
    // Multi-table session data (populated by edge function when is_multi_table is true)
    tables: row.tables as Event['tables'] ?? null,
    sessions: row.sessions as Event['sessions'] ?? null,
    // MTG event configuration
    mtgConfig: row.mtgConfig as Event['mtgConfig'] ?? null,
  }
}

// Public endpoints - direct Supabase queries
export async function getPublicEvents(
  filter?: EventSearchFilter
): Promise<EventSummary[]> {
  let query = supabase
    .from('events')
    .select(`
      id, title, game_title, game_category, event_date, start_time,
      duration_minutes, city, state, difficulty_level, max_players, host_is_playing,
      is_public, is_charity_event, min_age, status,
      host:users!host_user_id(id, display_name, avatar_url),
      registrations:event_registrations(count),
      games:event_games(thumbnail_url, is_primary)
    `)
    .eq('is_public', true)
    .eq('status', 'published')
    .gte('event_date', new Date().toISOString().split('T')[0])
    .order('event_date', { ascending: true })

  if (filter?.city) {
    query = query.ilike('city', `%${filter.city}%`)
  }
  if (filter?.state) {
    query = query.eq('state', filter.state)
  }
  if (filter?.gameCategory) {
    query = query.eq('game_category', filter.gameCategory)
  }
  if (filter?.difficulty) {
    query = query.eq('difficulty_level', filter.difficulty)
  }
  if (filter?.dateFrom) {
    query = query.gte('event_date', filter.dateFrom)
  }
  if (filter?.dateTo) {
    query = query.lte('event_date', filter.dateTo)
  }
  if (filter?.search) {
    query = query.or(`title.ilike.%${filter.search}%,game_title.ilike.%${filter.search}%`)
  }
  if (filter?.venueId) {
    query = query.eq('event_location_id', filter.venueId)
  }

  const { data, error } = await query

  if (error) throw new Error(error.message)
  return (data ?? []).map(toEventSummary)
}

// Browse events with authentication (supports radius search and blocked user filtering)
export async function browseEvents(
  token: string,
  filter?: EventSearchFilter
): Promise<EventSummary[]> {
  const params = new URLSearchParams({ type: 'browse' })

  if (filter?.city) params.append('city', filter.city)
  if (filter?.state) params.append('state', filter.state)
  if (filter?.gameCategory) params.append('gameCategory', filter.gameCategory)
  if (filter?.difficulty) params.append('difficulty', filter.difficulty)
  if (filter?.dateFrom) params.append('dateFrom', filter.dateFrom)
  if (filter?.dateTo) params.append('dateTo', filter.dateTo)
  if (filter?.search) params.append('search', filter.search)
  if (filter?.nearbyZip) params.append('nearbyZip', filter.nearbyZip)
  if (filter?.radiusMiles) params.append('radiusMiles', String(filter.radiusMiles))
  if (filter?.venueId) params.append('venueId', filter.venueId)

  return authenticatedRequest<EventSummary[]>(`/events?${params.toString()}`, token)
}

// Get event - authenticated (can see drafts if host/registered)
export async function getEvent(id: string, token?: string): Promise<Event> {
  // Use authenticated endpoint if token provided
  if (token) {
    return authenticatedRequest<Event>(`/events?id=${id}`, token)
  }

  // Fallback to direct query for public events
  const { data, error } = await supabase
    .from('events')
    .select(`
      *,
      host:users!host_user_id(id, display_name, avatar_url),
      registrations:event_registrations(
        id, user_id, status, registered_at,
        user:users(id, display_name, avatar_url)
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
    .eq('id', id)
    .single()

  if (error) throw new Error(error.message)
  return toEvent(data)
}

// Authenticated endpoints - via Edge Functions
export async function getMyEvents(token: string): Promise<EventSummary[]> {
  return authenticatedRequest<EventSummary[]>('/events?type=registered', token)
}

export async function getHostedEvents(token: string): Promise<EventSummary[]> {
  return authenticatedRequest<EventSummary[]>('/events?type=hosted', token)
}

export async function createEvent(
  token: string,
  data: CreateEventInput
): Promise<Event> {
  return authenticatedRequest<Event>('/events', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function updateEvent(
  token: string,
  id: string,
  data: UpdateEventInput
): Promise<Event> {
  return authenticatedRequest<Event>(`/events?id=${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteEvent(token: string, id: string): Promise<void> {
  return authenticatedRequest<void>(`/events?id=${id}`, token, {
    method: 'DELETE',
  })
}

export async function registerForEvent(
  token: string,
  eventId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>('/registrations', token, {
    method: 'POST',
    body: JSON.stringify({ eventId }),
  })
}

export async function cancelRegistration(
  token: string,
  eventId: string
): Promise<void> {
  return authenticatedRequest<void>(`/registrations?eventId=${eventId}`, token, {
    method: 'DELETE',
  })
}

export async function addEventItem(
  token: string,
  eventId: string,
  data: CreateEventItemInput
): Promise<EventItem> {
  return authenticatedRequest<EventItem>('/items', token, {
    method: 'POST',
    body: JSON.stringify({ eventId, ...data }),
  })
}

export async function claimItem(
  token: string,
  _eventId: string,
  itemId: string
): Promise<{ message: string }> {
  return authenticatedRequest<{ message: string }>(`/items?id=${itemId}&action=claim`, token, {
    method: 'PUT',
  })
}

export async function unclaimItem(
  token: string,
  _eventId: string,
  itemId: string
): Promise<void> {
  return authenticatedRequest<void>(`/items?id=${itemId}&action=unclaim`, token, {
    method: 'PUT',
  })
}

export async function updateItem(
  token: string,
  itemId: string,
  data: { itemName?: string; itemCategory?: string }
): Promise<EventItem> {
  return authenticatedRequest<EventItem>(`/items?id=${itemId}&action=update`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export async function deleteItem(
  token: string,
  itemId: string
): Promise<void> {
  return authenticatedRequest<void>(`/items?id=${itemId}`, token, {
    method: 'DELETE',
  })
}

export async function inviteGroupMembersToEvent(
  token: string,
  eventId: string,
  userIds: string[]
): Promise<{ message: string; addedCount: number }> {
  return authenticatedRequest<{ message: string; addedCount: number }>(
    `/invitations?action=invite-group-member`,
    token,
    {
      method: 'POST',
      body: JSON.stringify({ eventId, userIds }),
    }
  )
}
