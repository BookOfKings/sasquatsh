import { test as base } from '@playwright/test'
import { loadTestData, TestData } from './test-utils'

/**
 * Extended test fixture that provides access to test data
 */
export const test = base.extend<{
  testData: TestData | null
}>({
  testData: async ({}, use) => {
    const data = loadTestData()
    await use(data)
  },
})

export { expect } from '@playwright/test'

/**
 * Helper to get test event ID, throwing if not available
 */
export function getTestEventId(): string {
  const data = loadTestData()
  if (!data?.eventId) {
    throw new Error('Test event not available - setup may have failed')
  }
  return data.eventId
}

/**
 * Helper to get test event slug, throwing if not available
 */
export function getTestEventSlug(): string {
  const data = loadTestData()
  if (!data?.eventSlug) {
    throw new Error('Test event slug not available - setup may have failed')
  }
  return data.eventSlug
}

/**
 * Helper to get test group ID, throwing if not available
 */
export function getTestGroupId(): string {
  const data = loadTestData()
  if (!data?.groupId) {
    throw new Error('Test group not available - setup may have failed')
  }
  return data.groupId
}

/**
 * Helper to get test group slug, throwing if not available
 */
export function getTestGroupSlug(): string {
  const data = loadTestData()
  if (!data?.groupSlug) {
    throw new Error('Test group slug not available - setup may have failed')
  }
  return data.groupSlug
}

/**
 * Check if test data is available
 */
export function hasTestData(): boolean {
  const data = loadTestData()
  return !!(data?.eventId && data?.groupId)
}
