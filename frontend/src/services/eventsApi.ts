import type {
  Event,
  EventSummary,
  CreateEventInput,
  UpdateEventInput,
  EventItem,
  CreateEventItemInput,
  EventSearchFilter,
} from '@/types/events'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:5191'

interface ApiError {
  message?: string
}

async function request<T>(
  path: string,
  options?: RequestInit & { token?: string }
): Promise<T> {
  const headers = new Headers(options?.headers ?? undefined)

  if (options?.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (options?.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = (await response.json()) as ApiError
      if (data?.message) {
        message = data.message
      }
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

// Public endpoints
export async function getPublicEvents(
  filter?: EventSearchFilter
): Promise<EventSummary[]> {
  const params = new URLSearchParams()
  if (filter?.city) params.set('city', filter.city)
  if (filter?.state) params.set('state', filter.state)
  if (filter?.search) params.set('search', filter.search)
  if (filter?.gameCategory) params.set('gameCategory', filter.gameCategory)
  if (filter?.difficulty) params.set('difficulty', filter.difficulty)
  if (filter?.dateFrom) params.set('dateFrom', filter.dateFrom)
  if (filter?.dateTo) params.set('dateTo', filter.dateTo)

  const queryString = params.toString()
  const url = queryString ? `/api/events?${queryString}` : '/api/events'
  return request<EventSummary[]>(url)
}

export async function getEvent(id: string): Promise<Event> {
  return request<Event>(`/api/events/${id}`)
}

// Authenticated endpoints
export async function getMyEvents(token: string): Promise<EventSummary[]> {
  return request<EventSummary[]>('/api/events/my-events', { token })
}

export async function getHostedEvents(token: string): Promise<EventSummary[]> {
  return request<EventSummary[]>('/api/events/hosted', { token })
}

export async function createEvent(
  token: string,
  data: CreateEventInput
): Promise<Event> {
  return request<Event>('/api/events', {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  })
}

export async function updateEvent(
  token: string,
  id: string,
  data: UpdateEventInput
): Promise<Event> {
  return request<Event>(`/api/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
    token,
  })
}

export async function deleteEvent(token: string, id: string): Promise<void> {
  return request<void>(`/api/events/${id}`, {
    method: 'DELETE',
    token,
  })
}

export async function registerForEvent(
  token: string,
  eventId: string
): Promise<{ message: string }> {
  return request<{ message: string }>(`/api/events/${eventId}/register`, {
    method: 'POST',
    token,
  })
}

export async function cancelRegistration(
  token: string,
  eventId: string
): Promise<void> {
  return request<void>(`/api/events/${eventId}/register`, {
    method: 'DELETE',
    token,
  })
}

export async function addEventItem(
  token: string,
  eventId: string,
  data: CreateEventItemInput
): Promise<EventItem> {
  return request<EventItem>(`/api/events/${eventId}/items`, {
    method: 'POST',
    body: JSON.stringify(data),
    token,
  })
}

export async function claimItem(
  token: string,
  eventId: string,
  itemId: string
): Promise<{ message: string }> {
  return request<{ message: string }>(
    `/api/events/${eventId}/items/${itemId}/claim`,
    {
      method: 'POST',
      token,
    }
  )
}

export async function unclaimItem(
  token: string,
  eventId: string,
  itemId: string
): Promise<void> {
  return request<void>(`/api/events/${eventId}/items/${itemId}/claim`, {
    method: 'DELETE',
    token,
  })
}
