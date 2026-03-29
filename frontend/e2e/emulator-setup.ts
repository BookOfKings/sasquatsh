/**
 * Firebase Auth Emulator Setup
 *
 * This script sets up test users in the Firebase Auth Emulator.
 * Run this before E2E tests when using the emulator.
 */

const EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || 'localhost:9099'
const FIREBASE_API_KEY = process.env.VITE_FIREBASE_API_KEY || 'fake-api-key'
const FIREBASE_PROJECT_ID = process.env.VITE_FIREBASE_PROJECT_ID || 'sasquatsh'

interface EmulatorUser {
  email: string
  password: string
  displayName: string
}

// Test users to create in the emulator
export const TEST_USERS = {
  main: {
    email: 'e2e-test@sasquatsh.com',
    password: 'TestPassword123!',
    displayName: 'E2E Test User',
  },
  basic: {
    email: 'e2e-basic@sasquatsh.com',
    password: 'TestPassword123!',
    displayName: 'E2E Basic User',
  },
  pro: {
    email: 'e2e-pro@sasquatsh.com',
    password: 'TestPassword123!',
    displayName: 'E2E Pro User',
  },
}

/**
 * Check if the Firebase Auth Emulator is running
 */
export async function isEmulatorRunning(): Promise<boolean> {
  try {
    const response = await fetch(`http://${EMULATOR_HOST}/`, {
      method: 'GET',
    })
    return response.ok || response.status === 400 // Emulator returns 400 for root
  } catch {
    return false
  }
}

/**
 * Clear all users from the emulator
 */
export async function clearEmulatorUsers(): Promise<void> {
  const response = await fetch(
    `http://${EMULATOR_HOST}/emulator/v1/projects/${FIREBASE_PROJECT_ID}/accounts`,
    { method: 'DELETE' }
  )

  if (!response.ok && response.status !== 404) {
    console.warn('Warning: Could not clear emulator users:', response.status)
  }
}

/**
 * Create a user in the emulator
 */
export async function createEmulatorUser(user: EmulatorUser): Promise<string> {
  const response = await fetch(
    `http://${EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FIREBASE_API_KEY}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: user.email,
        password: user.password,
        displayName: user.displayName,
        returnSecureToken: true,
      }),
    }
  )

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    // If user already exists, that's fine
    if (error.error?.message === 'EMAIL_EXISTS') {
      console.log(`  User ${user.email} already exists`)
      return ''
    }
    throw new Error(`Failed to create user ${user.email}: ${JSON.stringify(error)}`)
  }

  const data = await response.json()
  console.log(`  Created user: ${user.email} (${data.localId})`)
  return data.localId
}

/**
 * Sign in to the emulator and get an ID token
 */
export async function signInEmulatorUser(email: string, password: string): Promise<string> {
  const response = await fetch(
    `http://${EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${FIREBASE_API_KEY}`,
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
    const error = await response.json().catch(() => ({}))
    throw new Error(`Failed to sign in ${email}: ${JSON.stringify(error)}`)
  }

  const data = await response.json()
  return data.idToken
}

/**
 * Setup all test users in the emulator
 */
export async function setupEmulatorUsers(): Promise<void> {
  console.log('Setting up Firebase Auth Emulator users...')

  // Check if emulator is running
  const running = await isEmulatorRunning()
  if (!running) {
    throw new Error(
      `Firebase Auth Emulator is not running at ${EMULATOR_HOST}.\n` +
      'Start it with: npm run emulator:start'
    )
  }

  console.log(`  Emulator running at ${EMULATOR_HOST}`)

  // Clear existing users (optional - helps with clean state)
  // await clearEmulatorUsers()
  // console.log('  Cleared existing users')

  // Create test users
  for (const [name, user] of Object.entries(TEST_USERS)) {
    try {
      await createEmulatorUser(user)
    } catch (error) {
      console.warn(`  Warning creating ${name} user:`, error)
    }
  }

  console.log('Emulator users setup complete')
}

// Run if executed directly
if (process.argv[1]?.endsWith('emulator-setup.ts') || process.argv[1]?.endsWith('emulator-setup.js')) {
  setupEmulatorUsers()
    .then(() => process.exit(0))
    .catch((error) => {
      console.error('Emulator setup failed:', error)
      process.exit(1)
    })
}
