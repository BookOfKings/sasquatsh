import { test as base, expect } from '@playwright/test'

/**
 * Extended test with authentication support
 *
 * To use authenticated tests:
 * 1. Set environment variables for test user credentials:
 *    - TEST_USER_EMAIL
 *    - TEST_USER_PASSWORD
 *
 * 2. Use the `authenticatedPage` fixture in your tests:
 *    test('my test', async ({ authenticatedPage }) => {
 *      await authenticatedPage.goto('/dashboard')
 *    })
 */

// Extend the base test to include an authenticated page
export const test = base.extend<{
  authenticatedPage: typeof base.prototype.page
}>({
  authenticatedPage: async ({ page }, use) => {
    const email = process.env.TEST_USER_EMAIL
    const password = process.env.TEST_USER_PASSWORD

    if (!email || !password) {
      console.warn('TEST_USER_EMAIL and TEST_USER_PASSWORD not set. Skipping authentication.')
      await use(page)
      return
    }

    // Navigate to login page
    await page.goto('/login')

    // Fill in credentials
    await page.getByLabel(/email/i).fill(email)
    await page.getByLabel(/password/i).fill(password)

    // Submit login form
    await page.getByRole('button', { name: /sign in|login|log in/i }).click()

    // Wait for redirect to dashboard or home
    await page.waitForURL(/\/(dashboard|home|events|groups)?$/, { timeout: 10000 })

    // Use the authenticated page
    await use(page)
  },
})

export { expect }
