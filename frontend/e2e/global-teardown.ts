import {
  authenticateTestUser,
  deleteTestEvent,
  deleteTestGroup,
  loadTestData,
  clearTestData,
} from './test-utils'

/**
 * Global teardown function that runs after all tests complete
 * - Loads test data IDs from setup
 * - Deletes test event and group
 * - Cleans up test data file
 */
async function globalTeardown() {
  console.log('Running global teardown...')

  // Load test data created during setup
  const testData = loadTestData()

  if (!testData || (!testData.eventId && !testData.groupId)) {
    console.log('No test data to clean up')
    clearTestData()
    console.log('Global teardown complete')
    return
  }

  // Check for test credentials
  const email = process.env.TEST_USER_EMAIL
  const password = process.env.TEST_USER_PASSWORD

  if (!email || !password) {
    console.log('No test credentials - cannot clean up test data')
    console.log('Manual cleanup may be required for:')
    if (testData.eventId) console.log(`  Event: ${testData.eventId}`)
    if (testData.groupId) console.log(`  Group: ${testData.groupId}`)
    clearTestData()
    console.log('Global teardown complete')
    return
  }

  try {
    // Re-authenticate (token may have expired)
    console.log('Authenticating for cleanup...')
    const token = await authenticateTestUser(email, password)

    // Delete test event
    if (testData.eventId) {
      console.log(`Deleting test event: ${testData.eventId}`)
      await deleteTestEvent(token, testData.eventId)
    }

    // Delete test group
    if (testData.groupId) {
      console.log(`Deleting test group: ${testData.groupId}`)
      await deleteTestGroup(token, testData.groupId)
    }

    console.log('Test data cleaned up successfully')
  } catch (error) {
    console.error('Error during cleanup:', error)
    console.log('Manual cleanup may be required')
  }

  // Always clear the test data file
  clearTestData()
  console.log('Global teardown complete')
}

export default globalTeardown
