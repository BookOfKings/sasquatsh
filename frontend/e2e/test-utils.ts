/**
 * Test utilities for E2E tests
 * Provides functions to authenticate and manage test data via API
 *
 * Required environment variables (set in .env or CI/CD):
 * - VITE_FIREBASE_API_KEY
 * - VITE_SUPABASE_FUNCTIONS_URL
 * - VITE_SUPABASE_ANON_KEY
 * - TEST_USER_EMAIL / TEST_USER_PASSWORD (for global setup)
 * - TEST_BASIC_USER_EMAIL / TEST_BASIC_USER_PASSWORD (for basic tier tests)
 */

function getRequiredEnv(name: string): string {
  const value = process.env[name]
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}. Ensure .env file exists or CI/CD secrets are configured.`)
  }
  return value
}

const FIREBASE_API_KEY = getRequiredEnv('VITE_FIREBASE_API_KEY')
const SUPABASE_FUNCTIONS_URL = getRequiredEnv('VITE_SUPABASE_FUNCTIONS_URL')
const SUPABASE_ANON_KEY = getRequiredEnv('VITE_SUPABASE_ANON_KEY')

interface FirebaseAuthResponse {
  idToken: string
  email: string
  refreshToken: string
  expiresIn: string
  localId: string
}

interface TestEvent {
  id: string
  title: string
  slug: string
}

interface TestGroup {
  id: string
  name: string
  slug: string
}

/**
 * Authenticate with Firebase using email/password
 */
export async function authenticateTestUser(
  email: string,
  password: string
): Promise<string> {
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FIREBASE_API_KEY}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        password,
        returnSecureToken: true,
      }),
    }
  )

  if (!response.ok) {
    const error = await response.json()
    throw new Error(`Firebase auth failed: ${error.error?.message || response.statusText}`)
  }

  const data: FirebaseAuthResponse = await response.json()
  return data.idToken
}

/**
 * Make an authenticated API request
 * Uses both Supabase anon key for Authorization and Firebase token for X-Firebase-Token
 */
async function apiRequest<T>(
  endpoint: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${SUPABASE_FUNCTIONS_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      ...options.headers,
    },
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(`API request failed: ${response.status} - ${errorText}`)
  }

  // Handle empty responses (like DELETE)
  const text = await response.text()
  if (!text) return {} as T
  return JSON.parse(text) as T
}

/**
 * Create a test event
 */
export async function createTestEvent(
  token: string,
  overrides: Partial<{
    title: string
    description: string
    eventDate: string
    startTime: string
    city: string
    state: string
  }> = {}
): Promise<TestEvent> {
  // Create event date for tomorrow
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const eventDate = tomorrow.toISOString().split('T')[0]

  const eventData = {
    title: `E2E Test Event - ${Date.now()}`,
    description: 'This event was created by automated E2E tests and will be deleted after tests complete.',
    eventDate,
    startTime: '19:00',
    durationMinutes: 180,
    city: 'Seattle',
    state: 'WA',
    postalCode: '98101',
    ...overrides,
  }

  const result = await apiRequest<TestEvent>('/events', token, {
    method: 'POST',
    body: JSON.stringify(eventData),
  })

  console.log(`Created test event: ${result.title} (${result.id})`)
  return result
}

/**
 * Delete a test event
 */
export async function deleteTestEvent(token: string, eventId: string): Promise<void> {
  try {
    await apiRequest(`/events?id=${eventId}`, token, {
      method: 'DELETE',
    })
    console.log(`Deleted test event: ${eventId}`)
  } catch (error) {
    console.error(`Failed to delete test event ${eventId}:`, error)
  }
}

/**
 * Create a test group
 */
export async function createTestGroup(
  token: string,
  overrides: Partial<{
    name: string
    description: string
    groupType: string
    joinPolicy: string
  }> = {}
): Promise<TestGroup> {
  const groupData = {
    name: `E2E Test Group - ${Date.now()}`,
    description: 'This group was created by automated E2E tests and will be deleted after tests complete.',
    groupType: 'local',
    joinPolicy: 'open',
    locationCity: 'Seattle',
    locationState: 'WA',
    ...overrides,
  }

  const result = await apiRequest<TestGroup>('/groups', token, {
    method: 'POST',
    body: JSON.stringify(groupData),
  })

  console.log(`Created test group: ${result.name} (${result.id})`)
  return result
}

/**
 * Delete a test group
 */
export async function deleteTestGroup(token: string, groupId: string): Promise<void> {
  try {
    await apiRequest(`/groups?id=${groupId}`, token, {
      method: 'DELETE',
    })
    console.log(`Deleted test group: ${groupId}`)
  } catch (error) {
    console.error(`Failed to delete test group ${groupId}:`, error)
  }
}

/**
 * Store for test data IDs - persisted to file for cross-process access
 */
import * as fs from 'fs'
import * as path from 'path'
import { fileURLToPath } from 'url'

// ES module compatible __dirname
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const TEST_DATA_FILE = path.join(__dirname, '.test-data.json')

export interface TestData {
  token?: string
  eventId?: string
  eventSlug?: string
  groupId?: string
  groupSlug?: string
  createdAt?: string
}

export function saveTestData(data: TestData): void {
  fs.writeFileSync(TEST_DATA_FILE, JSON.stringify(data, null, 2))
}

export function loadTestData(): TestData | null {
  try {
    if (fs.existsSync(TEST_DATA_FILE)) {
      return JSON.parse(fs.readFileSync(TEST_DATA_FILE, 'utf-8'))
    }
  } catch {
    // Ignore errors
  }
  return null
}

export function clearTestData(): void {
  try {
    if (fs.existsSync(TEST_DATA_FILE)) {
      fs.unlinkSync(TEST_DATA_FILE)
    }
  } catch {
    // Ignore errors
  }
}
