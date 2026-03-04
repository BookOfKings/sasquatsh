import { FullConfig } from '@playwright/test'
import {
  authenticateTestUser,
  createTestEvent,
  createTestGroup,
  saveTestData,
  clearTestData,
} from './test-utils'

/**
 * Global setup function that runs before all tests
 * - Authenticates test user
 * - Creates test event and group for tests to use
 * - Saves test data IDs for tests and teardown to access
 */
async function globalSetup(config: FullConfig) {
  console.log('Running global setup...')
  console.log('Base URL:', config.projects[0]?.use?.baseURL || 'http://localhost:5173')

  // Clear any stale test data
  clearTestData()

  // Check for test credentials
  const email = process.env.TEST_USER_EMAIL
  const password = process.env.TEST_USER_PASSWORD

  if (!email || !password) {
    console.log('No test user credentials - authenticated tests will be skipped')
    console.log('Set TEST_USER_EMAIL and TEST_USER_PASSWORD to run authenticated tests')
    console.log('Global setup complete (no test data created)')
    return
  }

  console.log('Test user credentials configured')

  try {
    // Authenticate
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
