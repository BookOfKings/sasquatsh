import { FullConfig } from '@playwright/test'

/**
 * Global setup function that runs before all tests
 * Use this for one-time setup tasks like:
 * - Database seeding
 * - Creating test users
 * - Setting up test data
 */
async function globalSetup(config: FullConfig) {
  console.log('Running global setup...')

  // Log test environment
  console.log('Base URL:', config.projects[0]?.use?.baseURL || 'http://localhost:5173')

  // Check for test credentials
  if (process.env.TEST_USER_EMAIL && process.env.TEST_USER_PASSWORD) {
    console.log('Test user credentials configured')
  } else {
    console.log('No test user credentials - authenticated tests will be skipped')
    console.log('Set TEST_USER_EMAIL and TEST_USER_PASSWORD to run authenticated tests')
  }

  console.log('Global setup complete')
}

export default globalSetup
