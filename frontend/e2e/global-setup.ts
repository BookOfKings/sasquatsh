import { FullConfig } from '@playwright/test'
import {
  authenticateTestUser,
  createTestEvent,
  createTestGroup,
  saveTestData,
  clearTestData,
  createEmulatorUser,
  isUsingEmulator,
} from './test-utils'

// Test user credentials for emulator mode
const EMULATOR_TEST_USERS = {
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
 * Global setup function that runs before all tests
 * - If using emulator: Creates test users in emulator
 * - Authenticates test user
 * - Creates test event and group for tests to use
 * - Saves test data IDs for tests and teardown to access
 */
async function globalSetup(config: FullConfig) {
  console.log('Running global setup...')
  console.log('Base URL:', config.projects[0]?.use?.baseURL || 'http://localhost:5173')

  // Clear any stale test data
  clearTestData()

  const useEmulator = isUsingEmulator()
  let email: string | undefined
  let password: string | undefined

  if (useEmulator) {
    console.log('Using Firebase Auth Emulator')
    // Use emulator test credentials
    email = EMULATOR_TEST_USERS.main.email
    password = EMULATOR_TEST_USERS.main.password

    // Set env vars for auth.setup.ts to use
    process.env.TEST_USER_EMAIL = EMULATOR_TEST_USERS.main.email
    process.env.TEST_USER_PASSWORD = EMULATOR_TEST_USERS.main.password
    process.env.TEST_BASIC_USER_EMAIL = EMULATOR_TEST_USERS.basic.email
    process.env.TEST_BASIC_USER_PASSWORD = EMULATOR_TEST_USERS.basic.password
    process.env.TEST_PRO_USER_EMAIL = EMULATOR_TEST_USERS.pro.email
    process.env.TEST_PRO_USER_PASSWORD = EMULATOR_TEST_USERS.pro.password
  } else {
    // Use production credentials from env vars
    email = process.env.TEST_USER_EMAIL
    password = process.env.TEST_USER_PASSWORD
  }

  if (!email || !password) {
    console.log('No test user credentials - authenticated tests will be skipped')
    console.log('Set TEST_USER_EMAIL and TEST_USER_PASSWORD to run authenticated tests')
    console.log('Or use emulator mode: npm run test:emulator')
    console.log('Global setup complete (no test data created)')
    return
  }

  console.log('Test user credentials configured')

  try {
    // If using emulator, create the test users first
    if (useEmulator) {
      console.log('Creating emulator test users...')
      for (const [name, user] of Object.entries(EMULATOR_TEST_USERS)) {
        try {
          await createEmulatorUser(user.email, user.password, user.displayName)
        } catch (error) {
          // User might already exist, which is fine
          console.log(`  ${name} user: ${(error as Error).message}`)
        }
      }

      // NOTE: When using emulator, we skip backend API calls because
      // the Supabase backend validates against production Firebase.
      // Emulator mode is for UI-only testing (login flows, form validation, etc.)
      console.log('Emulator mode: Skipping backend test data creation')
      console.log('(Backend APIs require production Firebase tokens)')

      saveTestData({
        createdAt: new Date().toISOString(),
      })

      console.log('Global setup complete (emulator mode - UI tests only)')
      return
    }

    // Production mode: Authenticate and create test data via backend
    console.log('Authenticating test user...')
    const token = await authenticateTestUser(email, password)
    console.log('Authentication successful')

    // Create test event
    console.log('Creating test event...')
    const event = await createTestEvent(token, {
      title: `E2E Test Event - ${new Date().toISOString().slice(0, 10)}`,
      description: 'Automated test event for E2E testing. Will be deleted after tests.',
    })

    // Create test group
    console.log('Creating test group...')
    const group = await createTestGroup(token, {
      name: `E2E Test Group - ${new Date().toISOString().slice(0, 10)}`,
      description: 'Automated test group for E2E testing. Will be deleted after tests.',
      joinPolicy: 'open',
    })

    // Save test data for tests and teardown to access
    saveTestData({
      token,
      eventId: event.id,
      eventSlug: event.slug,
      groupId: group.id,
      groupSlug: group.slug,
      createdAt: new Date().toISOString(),
    })

    console.log('Test data created successfully:')
    console.log(`  Event: ${event.id} (${event.slug})`)
    console.log(`  Group: ${group.id} (${group.slug})`)
  } catch (error) {
    console.error('Failed to create test data:', error)
    console.log('Authenticated tests may fail due to missing test data')
  }

  console.log('Global setup complete')
}

export default globalSetup
