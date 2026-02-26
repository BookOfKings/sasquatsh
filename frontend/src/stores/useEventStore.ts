import { computed, ref } from 'vue'
import {
  getPublicEvents,
  getEvent as getEventApi,
  getMyEvents,
  getHostedEvents,
  createEvent as createEventApi,
  updateEvent as updateEventApi,
  deleteEvent as deleteEventApi,
  registerForEvent as registerApi,
  cancelRegistration as cancelRegistrationApi,
  addEventItem as addItemApi,
  claimItem as claimItemApi,
  unclaimItem as unclaimItemApi,
} from '@/services/eventsApi'
import { useAuthStore } from '@/stores/useAuthStore'
import type {
  Event,
  EventSummary,
  CreateEventInput,
  UpdateEventInput,
  CreateEventItemInput,
  EventSearchFilter,
} from '@/types/events'

interface ActionResult {
  ok: boolean
  message: string
}

const publicEvents = ref<EventSummary[]>([])
const myEvents = ref<EventSummary[]>([])
const hostedEvents = ref<EventSummary[]>([])
const currentEvent = ref<Event | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function loadPublicEvents(filter?: EventSearchFilter): Promise<void> {
  loading.value = true
  error.value = null
  try {
    publicEvents.value = await getPublicEvents(filter)
  } catch (err) {
    console.error('Unable to load events', err)
    error.value = err instanceof Error ? err.message : 'Unable to load events'
  } finally {
    loading.value = false
  }
}

async function loadMyEvents(): Promise<void> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) return

  loading.value = true
  error.value = null
  try {
    myEvents.value = await getMyEvents(token)
  } catch (err) {
    console.error('Unable to load my events', err)
    error.value = err instanceof Error ? err.message : 'Unable to load events'
  } finally {
    loading.value = false
  }
}

async function loadHostedEvents(): Promise<void> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) return

  loading.value = true
  error.value = null
  try {
    hostedEvents.value = await getHostedEvents(token)
  } catch (err) {
    console.error('Unable to load hosted events', err)
    error.value = err instanceof Error ? err.message : 'Unable to load events'
  } finally {
    loading.value = false
  }
}

async function loadEvent(id: string): Promise<Event | null> {
  loading.value = true
  error.value = null
  try {
    const auth = useAuthStore()
    const token = await auth.getIdToken()
    currentEvent.value = await getEventApi(id, token ?? undefined)
    return currentEvent.value
  } catch (err) {
    console.error('Unable to load event', err)
    error.value = err instanceof Error ? err.message : 'Unable to load event'
    return null
  } finally {
    loading.value = false
  }
}

async function createEvent(data: CreateEventInput): Promise<ActionResult & { event?: Event }> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in to create an event' }
  }

  if (!data.title?.trim()) {
    return { ok: false, message: 'A title is required' }
  }

  if (!data.eventDate) {
    return { ok: false, message: 'An event date is required' }
  }

  if (!data.startTime) {
    return { ok: false, message: 'A start time is required' }
  }

  try {
    const event = await createEventApi(token, {
      ...data,
      title: data.title.trim(),
      description: data.description?.trim() || undefined,
      gameTitle: data.gameTitle?.trim() || undefined,
    })
    return { ok: true, message: 'Event created!', event }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to create event',
    }
  }
}

async function updateEvent(id: string, data: UpdateEventInput): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in to update an event' }
  }

  try {
    const updated = await updateEventApi(token, id, data)
    currentEvent.value = updated
    return { ok: true, message: 'Event updated!' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to update event',
    }
  }
}

async function deleteEvent(id: string): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in to delete an event' }
  }

  try {
    await deleteEventApi(token, id)
    publicEvents.value = publicEvents.value.filter((e) => e.id !== id)
    hostedEvents.value = hostedEvents.value.filter((e) => e.id !== id)
    return { ok: true, message: 'Event deleted' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to delete event',
    }
  }
}

async function registerForEvent(eventId: string): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in to register' }
  }

  try {
    await registerApi(token, eventId)
    // Reload the event to get updated registrations
    await loadEvent(eventId)
    return { ok: true, message: 'Successfully registered!' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to register',
    }
  }
}

async function cancelRegistration(eventId: string): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in' }
  }

  try {
    await cancelRegistrationApi(token, eventId)
    // Reload the event to get updated registrations
    await loadEvent(eventId)
    return { ok: true, message: 'Registration cancelled' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to cancel registration',
    }
  }
}

async function addItem(eventId: string, data: CreateEventItemInput): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in' }
  }

  if (!data.itemName?.trim()) {
    return { ok: false, message: 'Item name is required' }
  }

  try {
    const item = await addItemApi(token, eventId, {
      ...data,
      itemName: data.itemName.trim(),
    })
    if (currentEvent.value && currentEvent.value.id === eventId) {
      currentEvent.value.items = [...(currentEvent.value.items || []), item]
    }
    return { ok: true, message: 'Item added!' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to add item',
    }
  }
}

async function claimItem(eventId: string, itemId: string): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in' }
  }

  try {
    await claimItemApi(token, eventId, itemId)
    // Reload the event to get updated items
    await loadEvent(eventId)
    return { ok: true, message: 'Item claimed!' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to claim item',
    }
  }
}

async function unclaimItem(eventId: string, itemId: string): Promise<ActionResult> {
  const auth = useAuthStore()
  const token = await auth.getIdToken()
  if (!token) {
    return { ok: false, message: 'You must be logged in' }
  }

  try {
    await unclaimItemApi(token, eventId, itemId)
    // Reload the event to get updated items
    await loadEvent(eventId)
    return { ok: true, message: 'Item unclaimed' }
  } catch (err) {
    return {
      ok: false,
      message: err instanceof Error ? err.message : 'Unable to unclaim item',
    }
  }
}

export function useEventStore() {
  return {
    publicEvents: computed(() => publicEvents.value),
    myEvents: computed(() => myEvents.value),
    hostedEvents: computed(() => hostedEvents.value),
    currentEvent: computed(() => currentEvent.value),
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    loadPublicEvents,
    loadMyEvents,
    loadHostedEvents,
    loadEvent,
    createEvent,
    updateEvent,
    deleteEvent,
    registerForEvent,
    cancelRegistration,
    addItem,
    claimItem,
    unclaimItem,
  }
}
