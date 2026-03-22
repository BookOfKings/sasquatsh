import { test, expect, Page } from '@playwright/test'

// Helper to dismiss cookie consent modal
async function dismissCookieConsent(page: Page) {
  const acceptCookies = page.getByRole('button', { name: /accept all/i })
  if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
    await acceptCookies.click()
    await page.waitForTimeout(500)
  }
}

test.describe('Groups', () => {
  test.describe('Groups List Page', () => {
    test('should display groups page', async ({ page }) => {
      await page.goto('/groups')

      // Check for groups heading
      await expect(page.getByRole('heading', { name: /groups/i })).toBeVisible()
    })

    test('should have search functionality', async ({ page }) => {
      await page.goto('/groups')

      // Check for search input
      const searchInput = page.getByPlaceholder(/search/i)
      await expect(searchInput).toBeVisible()
    })

    test('should show groups list or empty state', async ({ page }) => {
      await page.goto('/groups')

      // The page should show either group cards or an empty state message
      const groupCards = page.locator('.card')
      const emptyState = page.getByText(/no groups|find groups|join a group/i)

      const hasCards = await groupCards.first().isVisible().catch(() => false)
      const hasEmptyState = await emptyState.isVisible().catch(() => false)

      // Should have either content or empty state
      expect(hasCards || hasEmptyState).toBeTruthy()
    })
  })

  test.describe('Create Group Page', () => {
    test('should redirect to login or show form', async ({ page }) => {
      await page.goto('/groups/create')

      // Should either redirect to login or show the form
      const url = page.url()
      const hasLoginRedirect = url.includes('/login')
      const hasForm = await page.locator('#name').isVisible().catch(() => false)

      expect(hasLoginRedirect || hasForm).toBeTruthy()
    })
  })
})

// Authenticated tests - these require TEST_USER_EMAIL and TEST_USER_PASSWORD env vars
test.describe('Groups (Authenticated)', () => {
  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Skipping authenticated tests - set TEST_USER_EMAIL and TEST_USER_PASSWORD'
  )

  test('should display create group form when authenticated', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await dismissCookieConsent(page)
    await page.locator('#email').fill(process.env.TEST_USER_EMAIL!)
    await page.locator('#password').fill(process.env.TEST_USER_PASSWORD!)
    await page.getByRole('button', { name: /sign in/i }).click()

    // Wait for login to complete (may redirect to games, dashboard, or home)
    await page.waitForURL(/\/(dashboard|home|games|groups)?$/, { timeout: 15000 })
    // Delay to avoid Firebase rate limits
    await page.waitForTimeout(2000)

    // Navigate to create group
    await page.goto('/groups/create')
    await dismissCookieConsent(page)

    // Check for form elements
    await expect(page.locator('#name')).toBeVisible()
    await expect(page.getByRole('button', { name: /create/i })).toBeVisible()
  })

  test('should validate group name is required', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await dismissCookieConsent(page)
    await page.locator('#email').fill(process.env.TEST_USER_EMAIL!)
    await page.locator('#password').fill(process.env.TEST_USER_PASSWORD!)
    await page.getByRole('button', { name: /sign in/i }).click()
    await page.waitForURL(/\/(dashboard|home|games|groups)?$/, { timeout: 15000 })
    // Delay to avoid Firebase rate limits
    await page.waitForTimeout(2000)

    // Navigate to create group
    await page.goto('/groups/create')
    await dismissCookieConsent(page)
    await page.waitForTimeout(500)

    // Try to submit empty form using force to bypass any remaining overlays
    await page.getByRole('button', { name: /create/i }).click({ force: true })

    // Should show validation error
    await expect(page.getByText(/required/i)).toBeVisible()
  })
})
