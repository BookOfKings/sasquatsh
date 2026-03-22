import { test, expect, Page } from '@playwright/test'
import { loadTestData } from './test-utils'

/**
 * Comprehensive E2E Tests for Events (Games) functionality
 * Covers: Create, View, Edit, Delete events
 *
 * Prerequisites:
 * - TEST_USER_EMAIL and TEST_USER_PASSWORD environment variables must be set
 * - The test user account must exist in the system
 * - Backend API must be running
 * - Global setup has created test event and group
 *
 * NOTE: Uses serial mode to reduce Firebase login rate limits.
 * Tests share the same browser context when run serially.
 */

// Helper to dismiss cookie consent modal
async function dismissCookieConsent(page: Page) {
  const acceptCookies = page.getByRole('button', { name: /accept all/i })
  if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
    await acceptCookies.click()
    await page.waitForTimeout(500)
  }
}

// Helper function to login (checks if already logged in first)
async function loginIfNeeded(page: Page, email: string, password: string) {
  // Navigate to a page to check auth status
  await page.goto('/games')

  // Check if already logged in (dashboard button visible in nav)
  const dashboardButton = page.getByRole('button', { name: /dashboard/i })
  const isLoggedIn = await dashboardButton.isVisible({ timeout: 2000 }).catch(() => false)

  if (isLoggedIn) {
    return // Already logged in
  }

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
  // Wait for redirect after successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })
  // Delay to avoid Firebase rate limits
  await page.waitForTimeout(2000)
}

// Helper to generate a unique event title for testing
function generateTestEventTitle(): string {
  return `Test Event ${Date.now()}`
}

// Helper to get tomorrow's date in YYYY-MM-DD format
function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0] || ''
}

test.describe('Events - Unauthenticated', () => {
  test('should display the events/games list page', async ({ page }) => {
    await page.goto('/games')

    // Check for main heading
    await expect(page.getByRole('heading', { name: /find games/i })).toBeVisible()

    // Check for search functionality
    const searchInput = page.getByPlaceholder(/search for games/i)
    await expect(searchInput).toBeVisible()

    // Check for city filter
    const cityInput = page.getByPlaceholder(/city/i)
    await expect(cityInput).toBeVisible()
  })

  test('should redirect to login when trying to create event without auth', async ({ page }) => {
    await page.goto('/games/create')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('should be able to view a public event detail page', async ({ page }) => {
    // Use the test event created during global setup
    const testData = loadTestData()

    if (testData?.eventSlug) {
      // Navigate directly to the test event
      await page.goto(`/games/${testData.eventSlug}`)

      // Should show event details or login prompt
      const eventTitle = page.locator('h1')
      const loginPrompt = page.getByRole('button', { name: /sign in to join/i })
      const hasTitle = await eventTitle.isVisible().catch(() => false)
      const hasLoginPrompt = await loginPrompt.isVisible().catch(() => false)

      expect(hasTitle || hasLoginPrompt).toBeTruthy()
    } else {
      // Fallback: try to find an event in the list
      await page.goto('/games')
      await page.waitForTimeout(1000)

      // Click on event heading (h3) which links to detail page
      const eventHeading = page.locator('h3').first()
      const hasEvents = await eventHeading.isVisible().catch(() => false)

      if (hasEvents) {
        await eventHeading.click()
        await expect(page).toHaveURL(/\/games\//)
      }
    }
  })

  test('should show "Host a Game" button only when authenticated', async ({ page }) => {
    await page.goto('/games')

    // Should NOT show "Host a Game" button for unauthenticated users
    const hostButton = page.getByRole('button', { name: /host a game/i })
    await expect(hostButton).not.toBeVisible()
  })
})

test.describe('Events - Authenticated', () => {
  // Run tests serially to share auth state and reduce Firebase rate limits
  test.describe.configure({ mode: 'serial' })

  // Skip if credentials not provided
  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Skipping authenticated tests - set TEST_USER_EMAIL and TEST_USER_PASSWORD'
  )

  test.beforeEach(async ({ page }) => {
    // Login if not already logged in (reduces Firebase auth API calls)
    await loginIfNeeded(page, process.env.TEST_USER_EMAIL!, process.env.TEST_USER_PASSWORD!)
    // Dismiss cookie consent if visible
    await dismissCookieConsent(page)
  })

  test.describe('View Events', () => {
    test('should show "Host a Game" button when authenticated', async ({ page }) => {
      await page.goto('/games')

      // Should show "Host a Game" button for authenticated users
      const hostButton = page.getByRole('button', { name: /host a game/i })
      await expect(hostButton).toBeVisible()
    })

    test('should be able to filter events by search', async ({ page }) => {
      await page.goto('/games')

      // Enter search text
      const searchInput = page.getByPlaceholder(/search for games/i)
      await searchInput.fill('Catan')

      // Wait for debounced search to apply
      await page.waitForTimeout(500)

      // Results should update (either show filtered results or empty state)
      const resultsText = page.getByText(/\d+ games? found/)
      await expect(resultsText).toBeVisible()
    })

    test('should be able to expand and use filters', async ({ page }) => {
      await page.goto('/games')

      // Click Filters button
      const filtersButton = page.getByRole('button', { name: /filters/i })
      await filtersButton.click()

      // Filter panel should expand
      const categorySelect = page.getByRole('combobox').filter({ hasText: /all categories/i })
      await expect(categorySelect.or(page.locator('select').first())).toBeVisible()
    })

    test('should navigate to event detail when clicking an event', async ({ page }) => {
      // Use the test event created during global setup
      const testData = loadTestData()

      if (testData?.eventSlug) {
        // Navigate directly to the test event
        await page.goto(`/games/${testData.eventSlug}`)
        await expect(page).toHaveURL(/\/games\/[a-zA-Z0-9-]+/)

        // Should show event title
        await expect(page.locator('h1')).toBeVisible()
      } else {
        // Fallback: try to find an event in the list
        await page.goto('/games')
        await page.waitForTimeout(1000)

        // Click on event heading (h3) which links to detail page
        const eventHeading = page.locator('h3').first()
        const hasEvents = await eventHeading.isVisible().catch(() => false)

        if (hasEvents) {
          await eventHeading.click()
          await expect(page).toHaveURL(/\/games\/[a-zA-Z0-9-]+/)
        }
      }
    })
  })

  test.describe('Create Event', () => {
    test('should display create event form', async ({ page }) => {
      await page.goto('/games/create')

      // Check for form heading
      await expect(page.getByRole('heading', { name: /host a game/i })).toBeVisible()

      // Check for required form fields
      await expect(page.locator('#title')).toBeVisible()
      await expect(page.locator('#eventDate')).toBeVisible()
      await expect(page.locator('#startTime')).toBeVisible()
      await expect(page.locator('#maxPlayers')).toBeVisible()

      // Check for submit button
      await expect(page.getByRole('button', { name: /host game/i })).toBeVisible()
    })

    test('should validate required fields', async ({ page }) => {
      await page.goto('/games/create')
      await dismissCookieConsent(page)

      // Try to submit empty form
      await page.getByRole('button', { name: /host game/i }).click({ force: true })

      // Should show validation errors (check for any validation message)
      const hasValidationError = await page.getByText(/required|please|fill|enter/i).isVisible({ timeout: 3000 }).catch(() => false)
      const stillOnCreatePage = page.url().includes('/games/create')

      // Validation succeeded if error is shown or form submission was blocked
      expect(hasValidationError || stillOnCreatePage).toBeTruthy()
    })

    test('should create a new event successfully', async ({ page }) => {
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      // Fill required fields
      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      // Optionally fill description
      await page.locator('#description').fill('This is a test event created by automated testing.')

      // Submit the form
      await page.getByRole('button', { name: /host game/i }).click()

      // Should navigate to event detail page on success
      // Or show upgrade prompt if at tier limit
      await page.waitForTimeout(3000)

      const url = page.url()
      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(url)
      const upgradePrompt = page.getByText(/upgrade|limit reached/i)
      const hasUpgradePrompt = await upgradePrompt.isVisible().catch(() => false)

      // Either successfully created or hit tier limit
      expect(isOnDetailPage || hasUpgradePrompt).toBeTruthy()

      // If we're on the detail page, verify the event was created
      if (isOnDetailPage) {
        await expect(page.getByRole('heading', { name: eventTitle })).toBeVisible()
      }
    })

    test('should be able to search and add BGG games', async ({ page }) => {
      await page.goto('/games/create')

      // Find the game search input
      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')

      // Wait for search results
      await page.waitForTimeout(1000)

      // Check if search results appear
      const searchResults = page.locator('[role="listbox"], [role="option"], .autocomplete-results, .search-results')
      const hasResults = await searchResults.isVisible().catch(() => false)

      if (hasResults) {
        // Click first result
        await searchResults.locator('text=/catan/i').first().click()

        // Game should be added to selected games
        const selectedGames = page.locator('.selected-games, [data-testid="selected-games"]')
        await expect(selectedGames.or(page.getByText(/selected games/i))).toBeVisible()
      }
    })

    test('should be able to select group for event', async ({ page }) => {
      await page.goto('/games/create')

      // Check if group selector exists (only shows if user is admin/owner of groups)
      const groupSelector = page.locator('#groupId')
      const hasGroupSelector = await groupSelector.isVisible().catch(() => false)

      if (hasGroupSelector) {
        // Should have "No group - personal event" option
        await expect(groupSelector).toContainText(/no group|personal/i)
      }
    })
  })

  test.describe('Event Detail & Actions', () => {
    test('should show edit and delete buttons for event host', async ({ page }) => {
      // First create an event or navigate to one user owns
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      await page.getByRole('button', { name: /host game/i }).click()

      // Wait for navigation or upgrade prompt
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Should see Edit button
        await expect(page.getByRole('button', { name: /edit/i })).toBeVisible()
        // Should see Delete button
        await expect(page.getByRole('button', { name: /delete/i })).toBeVisible()
      }
    })

    test('should be able to register for an event', async ({ page }) => {
      // Use the test event created during global setup
      const testData = loadTestData()

      if (testData?.eventSlug) {
        // Navigate directly to the test event
        await page.goto(`/games/${testData.eventSlug}`)
        await page.waitForURL(/\/games\//)
      } else {
        // Fallback: try to find an event in the list
        await page.goto('/games')
        await page.waitForTimeout(1000)

        const eventCard = page.locator('.card').first()
        const hasCards = await eventCard.isVisible().catch(() => false)

        if (!hasCards) return // Skip if no events
        await eventCard.click()
        await page.waitForURL(/\/games\//)
      }

      // Check for Join Game or Cancel Registration button
      const joinButton = page.getByRole('button', { name: /join game/i })
      const cancelButton = page.getByRole('button', { name: /cancel registration/i })
      const gameFullText = page.getByText(/game is full/i)

      const hasJoinButton = await joinButton.isVisible().catch(() => false)
      const hasCancelButton = await cancelButton.isVisible().catch(() => false)
      const isGameFull = await gameFullText.isVisible().catch(() => false)

      // One of these states should be true (unless viewing own event)
      const isHostedByUser = await page.getByRole('button', { name: /edit/i }).isVisible().catch(() => false)

      if (!isHostedByUser) {
        expect(hasJoinButton || hasCancelButton || isGameFull).toBeTruthy()
      }
    })

    test('should be able to share an event', async ({ page }) => {
      // Use the test event created during global setup
      const testData = loadTestData()

      if (testData?.eventSlug) {
        // Navigate directly to the test event
        await page.goto(`/games/${testData.eventSlug}`)
        await page.waitForURL(/\/games\//)
      } else {
        // Fallback: try to find an event in the list
        await page.goto('/games')
        await page.waitForTimeout(1000)

        const eventCard = page.locator('.card').first()
        const hasCards = await eventCard.isVisible().catch(() => false)

        if (!hasCards) return // Skip if no events
        await eventCard.click()
        await page.waitForURL(/\/games\//)
      }

      // Find and click Share button
      const shareButton = page.getByRole('button', { name: /share/i })
      const hasShareButton = await shareButton.isVisible().catch(() => false)

      if (hasShareButton) {
        await shareButton.click()

        // Share modal should appear
        await expect(page.getByText(/share|copy link/i)).toBeVisible()
      }
    })
  })

  test.describe('Edit Event', () => {
    test('should navigate to edit page and show form', async ({ page }) => {
      // Create an event first
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Click Edit button
        await page.getByRole('button', { name: /edit/i }).click()

        // Should be on edit page
        await expect(page).toHaveURL(/\/games\/[a-zA-Z0-9-]+\/edit/)

        // Form should be pre-filled
        await expect(page.locator('#title')).toHaveValue(eventTitle)
      }
    })

    test('should be able to update event details', async ({ page }) => {
      // Create an event first
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Navigate to edit
        await page.getByRole('button', { name: /edit/i }).click()
        await page.waitForURL(/\/edit/)

        // Update the title
        const updatedTitle = `${eventTitle} - Updated`
        await page.locator('#title').clear()
        await page.locator('#title').fill(updatedTitle)

        // Save changes
        await page.getByRole('button', { name: /save changes/i }).click()

        // Should navigate back to detail page
        await page.waitForURL(/\/games\/[a-zA-Z0-9-]+$/, { timeout: 5000 })

        // Updated title should be visible
        await expect(page.getByRole('heading', { name: updatedTitle })).toBeVisible()
      }
    })
  })

  test.describe('Delete Event', () => {
    test('should show confirmation dialog before deleting', async ({ page }) => {
      // Create an event first
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Set up dialog handler to dismiss confirmation
        page.on('dialog', dialog => dialog.dismiss())

        // Click Delete button
        await page.getByRole('button', { name: /delete/i }).click()

        // User dismissed, should still be on detail page
        await expect(page).toHaveURL(/\/games\/[a-zA-Z0-9-]+$/)
      }
    })

    test('should delete event when confirmed', async ({ page }) => {
      // Create an event first
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Set up dialog handler to accept confirmation
        page.on('dialog', dialog => dialog.accept())

        // Click Delete button
        await page.getByRole('button', { name: /delete/i }).click()

        // Should redirect to games list
        await page.waitForURL(/\/games$/, { timeout: 5000 })

        // Toast message should appear
        const toast = page.getByText(/event deleted/i)
        const hasToast = await toast.isVisible().catch(() => false)
        expect(hasToast || page.url().includes('/games')).toBeTruthy()
      }
    })
  })
})
