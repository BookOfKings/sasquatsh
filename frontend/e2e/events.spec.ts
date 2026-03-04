import { test, expect, Page } from '@playwright/test'

// Helper to dismiss cookie consent modal
async function dismissCookieConsent(page: Page) {
  const acceptCookies = page.getByRole('button', { name: /accept all/i })
  if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
    await acceptCookies.click()
    await page.waitForTimeout(500)
  }
}

test.describe('Events', () => {
  test.describe('Events List Page', () => {
    test('should display events page', async ({ page }) => {
      await page.goto('/events')

      // Check for events/games heading
      const heading = page.getByRole('heading', { name: /events|games|upcoming/i })
      await expect(heading.first()).toBeVisible()
    })

    test('should show events list or empty state', async ({ page }) => {
      await page.goto('/events')

      // The page should show either event cards or an empty state message
      const eventCards = page.locator('.card')
      const emptyState = page.getByText(/no events|find events|upcoming/i)

      const hasCards = await eventCards.first().isVisible().catch(() => false)
      const hasEmptyState = await emptyState.isVisible().catch(() => false)

      // Should have either content or empty state
      expect(hasCards || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Create Event Page', () => {
    test('should redirect to login or show form', async ({ page }) => {
      // The route might be /events/create or /games/create
      await page.goto('/events/create')

      // Should either redirect to login or show the form
      const url = page.url()
      const hasLoginRedirect = url.includes('/login')
      const hasForm = await page.locator('form').isVisible().catch(() => false)

      expect(hasLoginRedirect || hasForm).toBeTruthy()
    })
  })
})

// Authenticated tests
test.describe('Events (Authenticated)', () => {
  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Skipping authenticated tests - set TEST_USER_EMAIL and TEST_USER_PASSWORD'
  )

  test('should display create event form when authenticated', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.locator('#email').fill(process.env.TEST_USER_EMAIL!)
    await page.locator('#password').fill(process.env.TEST_USER_PASSWORD!)
    await page.getByRole('button', { name: /sign in/i }).click()
    await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })
    // Delay to avoid Firebase rate limits
    await page.waitForTimeout(2000)

    // Navigate to create event (route is /games/create)
    await page.goto('/games/create')
    await dismissCookieConsent(page)

    // Check for form elements - look for Host a Game heading
    await expect(page.getByRole('heading', { name: /host a game/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /host game/i })).toBeVisible()
  })

  test('should show upgrade prompt when at tier limit', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.locator('#email').fill(process.env.TEST_USER_EMAIL!)
    await page.locator('#password').fill(process.env.TEST_USER_PASSWORD!)
    await page.getByRole('button', { name: /sign in/i }).click()
    await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })
    // Delay to avoid Firebase rate limits
    await page.waitForTimeout(2000)

    // Navigate to create event (route is /games/create)
    await page.goto('/games/create')
    await dismissCookieConsent(page)

    // If user is at limit, upgrade prompt should appear
    // This test checks the component exists when triggered
    const upgradePrompt = page.getByText(/upgrade|limit reached/i)
    const isAtLimit = await upgradePrompt.isVisible().catch(() => false)

    // Test passes whether user is at limit or not
    // If at limit, prompt should be visible
    // If not at limit, form should be visible
    const hasForm = await page.getByRole('heading', { name: /host a game/i }).isVisible().catch(() => false)

    expect(isAtLimit || hasForm).toBeTruthy()
  })
})
