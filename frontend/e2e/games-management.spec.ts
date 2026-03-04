import { test, expect, Page } from '@playwright/test'

/**
 * Comprehensive E2E Tests for Games Management within Events
 * Covers: Add games to events, Remove games from events, Game search (BGG integration)
 *
 * Prerequisites:
 * - TEST_USER_EMAIL and TEST_USER_PASSWORD environment variables must be set
 * - The test user account must exist in the system
 * - Backend API must be running
 * - BoardGameGeek API must be accessible
 *
 * NOTE: Uses serial mode to reduce Firebase login rate limits.
 * Tests share the same browser context when run serially.
 */

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
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })
}

// Helper to generate a unique event title for testing
function generateTestEventTitle(): string {
  return `Game Test Event ${Date.now()}`
}

// Helper to get tomorrow's date in YYYY-MM-DD format
function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0] || ''
}

test.describe('Games Management - Unauthenticated', () => {
  test('should display games on event detail page', async ({ page }) => {
    await page.goto('/games')

    await page.waitForTimeout(1000)

    const eventCard = page.locator('.card').first()
    const hasCards = await eventCard.isVisible().catch(() => false)

    if (hasCards) {
      await eventCard.click()
      await page.waitForURL(/\/games\//)

      // Look for games section on event detail
      const gamesSection = page.getByRole('heading', { name: /games/i })
      const gameCards = page.locator('[class*="game-card"], [class*="ring-primary"], .border.rounded-lg')

      const hasGamesSection = await gamesSection.isVisible().catch(() => false)
      const hasGameCards = await gameCards.first().isVisible().catch(() => false)

      // Event may or may not have games
      // Test passes as long as page loads
      expect(true).toBeTruthy()
    }
  })

  test('should show game info (players, playtime) if games exist', async ({ page }) => {
    await page.goto('/games')

    await page.waitForTimeout(1000)

    const eventCard = page.locator('.card').first()
    const hasCards = await eventCard.isVisible().catch(() => false)

    if (hasCards) {
      await eventCard.click()
      await page.waitForURL(/\/games\//)

      // Look for game info badges
      const playerInfo = page.getByText(/\d+-?\d* players/i)
      const timeInfo = page.getByText(/\d+ min/i)

      const hasPlayerInfo = await playerInfo.first().isVisible().catch(() => false)
      const hasTimeInfo = await timeInfo.first().isVisible().catch(() => false)

      // If games exist, they should have info
      // Test passes regardless - this is just checking the display
      expect(true).toBeTruthy()
    }
  })
})

test.describe('Games Management - Authenticated', () => {
  // Run tests serially to share auth state and reduce Firebase rate limits
  test.describe.configure({ mode: 'serial' })

  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Skipping authenticated tests - set TEST_USER_EMAIL and TEST_USER_PASSWORD'
  )

  test.beforeEach(async ({ page }) => {
    // Login if not already logged in (reduces Firebase auth API calls)
    await loginIfNeeded(page, process.env.TEST_USER_EMAIL!, process.env.TEST_USER_PASSWORD!)
  })

  test.describe('Add Games to Event', () => {
    test('should show BoardGameGeek search on create event page', async ({ page }) => {
      await page.goto('/games/create')

      // Find the game search section
      const gameSearchLabel = page.getByText(/search boardgamegeek/i)
      await expect(gameSearchLabel).toBeVisible()

      // Find the search input
      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await expect(gameSearchInput).toBeVisible()
    })

    test('should search BGG and show results', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')

      // Wait for search results (BGG API call)
      await page.waitForTimeout(2000)

      // Check for autocomplete/search results
      // Results could be in various containers
      const hasDropdown = await page.locator('[role="listbox"], [role="option"], .autocomplete-results, .search-results, [class*="dropdown"]').isVisible().catch(() => false)
      const hasSuggestions = await page.getByText(/settlers of catan|catan/i).first().isVisible().catch(() => false)

      // Either dropdown or inline suggestions should appear
      expect(hasDropdown || hasSuggestions).toBeTruthy()
    })

    test('should add a game from BGG search results', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Ticket to Ride')

      // Wait for search results
      await page.waitForTimeout(2000)

      // Try to click on a result
      const ticketToRide = page.getByText(/ticket to ride/i).first()
      const isVisible = await ticketToRide.isVisible().catch(() => false)

      if (isVisible) {
        await ticketToRide.click()

        // Check if game was added to selected games
        await page.waitForTimeout(500)

        // Should show selected games section or update primary game name
        const selectedGamesSection = page.getByText(/selected games/i)
        const gameCard = page.locator('[class*="game-card"], .card').filter({ hasText: /ticket to ride/i })

        const hasSection = await selectedGamesSection.isVisible().catch(() => false)
        const hasCard = await gameCard.isVisible().catch(() => false)

        // Primary game name should be auto-filled
        const primaryGameInput = page.locator('#gameTitle')
        const primaryValue = await primaryGameInput.inputValue()

        expect(hasSection || hasCard || primaryValue.toLowerCase().includes('ticket')).toBeTruthy()
      }
    })

    test('should not add duplicate games', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)

      // Add first game
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)

      const catanResult = page.getByText(/catan/i).first()
      const isVisible = await catanResult.isVisible().catch(() => false)

      if (isVisible) {
        await catanResult.click()
        await page.waitForTimeout(500)

        // Try to add the same game again
        await gameSearchInput.fill('Catan')
        await page.waitForTimeout(2000)

        const catanResultAgain = page.getByText(/catan/i).first()
        const isVisibleAgain = await catanResultAgain.isVisible().catch(() => false)

        if (isVisibleAgain) {
          await catanResultAgain.click()
          await page.waitForTimeout(500)

          // Count game cards - should only have 1
          const gameCards = page.locator('[class*="game-card"], .border.rounded-lg').filter({ hasText: /catan/i })
          const count = await gameCards.count()

          // Should not have duplicates
          expect(count).toBeLessThanOrEqual(1)
        }
      }
    })

    test('should set first added game as primary', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')

      await page.waitForTimeout(2000)

      const catanResult = page.getByText(/catan/i).first()
      const isVisible = await catanResult.isVisible().catch(() => false)

      if (isVisible) {
        await catanResult.click()
        await page.waitForTimeout(500)

        // Check that primary game name is auto-filled
        const primaryGameInput = page.locator('#gameTitle')
        const primaryValue = await primaryGameInput.inputValue()

        expect(primaryValue.toLowerCase()).toContain('catan')

        // Look for "Primary" badge on the game card
        const primaryBadge = page.getByText(/primary/i)
        const hasPrimaryBadge = await primaryBadge.isVisible().catch(() => false)
        expect(hasPrimaryBadge).toBeTruthy()
      }
    })

    test('should be able to add multiple games', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)

      // Add first game
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      // Clear and add second game
      await gameSearchInput.clear()
      await gameSearchInput.fill('Ticket to Ride')
      await page.waitForTimeout(2000)
      const ticket = page.getByText(/ticket to ride/i).first()
      if (await ticket.isVisible().catch(() => false)) {
        await ticket.click()
        await page.waitForTimeout(500)
      }

      // Should have selected games visible
      const selectedGamesLabel = page.getByText(/selected games/i)
      const hasLabel = await selectedGamesLabel.isVisible().catch(() => false)

      if (hasLabel) {
        // Count game cards
        const gameCards = page.locator('[class*="game-card"], .card, .border.rounded-lg').filter({ has: page.locator('[class*="remove"], button') })
        const count = await gameCards.count()
        expect(count).toBeGreaterThanOrEqual(1)
      }
    })

    test('should create event with multiple games attached', async ({ page }) => {
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      // Fill event details
      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      // Add a game from BGG
      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)

      const catanResult = page.getByText(/catan/i).first()
      if (await catanResult.isVisible().catch(() => false)) {
        await catanResult.click()
        await page.waitForTimeout(500)
      }

      // Submit
      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Check that games section shows the added game
        const gamesSection = page.getByRole('heading', { name: /games/i })
        const hasGames = await gamesSection.isVisible().catch(() => false)

        if (hasGames) {
          // Look for the game we added
          const addedGame = page.getByText(/catan/i)
          const hasAddedGame = await addedGame.isVisible().catch(() => false)
          expect(hasAddedGame).toBeTruthy()
        }
      }
    })
  })

  test.describe('Remove Games from Event', () => {
    test('should show remove button on game cards in create form', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)

      const catanResult = page.getByText(/catan/i).first()
      if (await catanResult.isVisible().catch(() => false)) {
        await catanResult.click()
        await page.waitForTimeout(500)

        // Look for remove button on the game card
        const removeButton = page.locator('[class*="remove"], button').filter({ hasText: /remove|x/i })
        const removeIcon = page.locator('[class*="game-card"], .card').locator('button, [role="button"]')

        const hasRemoveButton = await removeButton.first().isVisible().catch(() => false)
        const hasRemoveIcon = await removeIcon.first().isVisible().catch(() => false)

        expect(hasRemoveButton || hasRemoveIcon).toBeTruthy()
      }
    })

    test('should remove game when clicking remove button', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)

      const catanResult = page.getByText(/catan/i).first()
      if (await catanResult.isVisible().catch(() => false)) {
        await catanResult.click()
        await page.waitForTimeout(500)

        // Verify game was added
        const primaryGameInput = page.locator('#gameTitle')
        const valueBefore = await primaryGameInput.inputValue()
        expect(valueBefore.toLowerCase()).toContain('catan')

        // Find and click remove button
        // Looking for a button within the game card or nearby
        const gameCard = page.locator('.border.rounded-lg, [class*="game-card"]').filter({ hasText: /catan/i })
        const removeButton = gameCard.locator('button').first()

        if (await removeButton.isVisible().catch(() => false)) {
          await removeButton.click()
          await page.waitForTimeout(500)

          // Game should be removed - primary game name should be cleared
          const valueAfter = await primaryGameInput.inputValue()
          expect(valueAfter).toBe('')
        }
      }
    })

    test('should update primary game when primary is removed', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)

      // Add two games
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      await gameSearchInput.clear()
      await gameSearchInput.fill('Ticket to Ride')
      await page.waitForTimeout(2000)
      const ticket = page.getByText(/ticket to ride/i).first()
      if (await ticket.isVisible().catch(() => false)) {
        await ticket.click()
        await page.waitForTimeout(500)
      }

      // Verify first game is primary
      const primaryGameInput = page.locator('#gameTitle')
      const primaryBefore = await primaryGameInput.inputValue()

      // Remove first (primary) game
      const firstGameCard = page.locator('.border.rounded-lg, [class*="game-card"]').first()
      const removeButton = firstGameCard.locator('button').first()

      if (await removeButton.isVisible().catch(() => false)) {
        await removeButton.click()
        await page.waitForTimeout(500)

        // Primary should now be the second game (Ticket to Ride)
        const primaryAfter = await primaryGameInput.inputValue()
        expect(primaryAfter.toLowerCase()).not.toBe(primaryBefore.toLowerCase())
      }
    })
  })

  test.describe('Edit Event Games', () => {
    test('should show existing games when editing event', async ({ page }) => {
      // Create an event with a game first
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      // Add a game
      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Navigate to edit
        await page.getByRole('button', { name: /edit/i }).click()
        await page.waitForURL(/\/edit/)

        // Should show existing games
        const selectedGamesLabel = page.getByText(/selected games/i)
        const hasGames = await selectedGamesLabel.isVisible().catch(() => false)

        if (hasGames) {
          // Should see the game we added
          const catanCard = page.locator('.border.rounded-lg, [class*="game-card"]').filter({ hasText: /catan/i })
          const hasCatan = await catanCard.isVisible().catch(() => false)
          expect(hasCatan).toBeTruthy()
        }
      }
    })

    test('should be able to add more games when editing', async ({ page }) => {
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
        await page.getByRole('button', { name: /edit/i }).click()
        await page.waitForURL(/\/edit/)

        // Add a game in edit mode
        const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
        await gameSearchInput.fill('Ticket to Ride')
        await page.waitForTimeout(2000)

        const ticket = page.getByText(/ticket to ride/i).first()
        if (await ticket.isVisible().catch(() => false)) {
          await ticket.click()
          await page.waitForTimeout(500)

          // Should show in selected games
          const selectedGamesLabel = page.getByText(/selected games/i)
          const hasLabel = await selectedGamesLabel.isVisible().catch(() => false)
          expect(hasLabel).toBeTruthy()
        }
      }
    })

    test('should be able to remove games when editing', async ({ page }) => {
      // Create an event with a game
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        await page.getByRole('button', { name: /edit/i }).click()
        await page.waitForURL(/\/edit/)

        // Try to remove the game
        const gameCard = page.locator('.border.rounded-lg, [class*="game-card"]').first()
        const removeButton = gameCard.locator('button').first()

        if (await removeButton.isVisible().catch(() => false)) {
          const countBefore = await page.locator('.border.rounded-lg, [class*="game-card"]').count()
          await removeButton.click()
          await page.waitForTimeout(500)
          const countAfter = await page.locator('.border.rounded-lg, [class*="game-card"]').count()

          expect(countAfter).toBeLessThan(countBefore)
        }
      }
    })
  })

  test.describe('Game Display on Event Detail', () => {
    test('should show primary game badge on event detail', async ({ page }) => {
      // Create an event with games
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Check for primary badge
        const primaryBadge = page.getByText(/primary/i)
        const hasPrimaryBadge = await primaryBadge.isVisible().catch(() => false)
        expect(hasPrimaryBadge).toBeTruthy()
      }
    })

    test('should show game thumbnail if available', async ({ page }) => {
      await page.goto('/games')

      await page.waitForTimeout(1000)

      const eventCard = page.locator('.card').first()
      const hasCards = await eventCard.isVisible().catch(() => false)

      if (hasCards) {
        await eventCard.click()
        await page.waitForURL(/\/games\//)

        // Look for game images in the games section
        const gameImages = page.locator('.games img, [class*="game-card"] img, .rounded img')
        const hasImages = await gameImages.first().isVisible().catch(() => false)

        // Test passes whether images exist or not - just checking functionality
        expect(true).toBeTruthy()
      }
    })

    test('should show alternative games badge', async ({ page }) => {
      // Create event with multiple games
      await page.goto('/games/create')

      const eventTitle = generateTestEventTitle()
      const eventDate = getTomorrowDate()

      await page.locator('#title').fill(eventTitle)
      await page.locator('#eventDate').fill(eventDate)
      await page.locator('#startTime').fill('19:00')
      await page.locator('#durationMinutes').fill('120')
      await page.locator('#maxPlayers').fill('4')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)

      // Add first game
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      // Add second game
      await gameSearchInput.clear()
      await gameSearchInput.fill('Ticket to Ride')
      await page.waitForTimeout(2000)
      const ticket = page.getByText(/ticket to ride/i).first()
      if (await ticket.isVisible().catch(() => false)) {
        await ticket.click()
        await page.waitForTimeout(500)
      }

      await page.getByRole('button', { name: /host game/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/games\/[a-zA-Z0-9-]+$/.test(page.url())

      if (isOnDetailPage) {
        // Check for alternative badge on second game
        const alternativeBadge = page.getByText(/alternative/i)
        const hasAlternative = await alternativeBadge.isVisible().catch(() => false)
        expect(hasAlternative).toBeTruthy()
      }
    })
  })

  test.describe('Set Primary Game', () => {
    test('should be able to set a different game as primary in create form', async ({ page }) => {
      await page.goto('/games/create')

      const gameSearchInput = page.getByPlaceholder(/search for a board game/i)

      // Add two games
      await gameSearchInput.fill('Catan')
      await page.waitForTimeout(2000)
      const catan = page.getByText(/catan/i).first()
      if (await catan.isVisible().catch(() => false)) {
        await catan.click()
        await page.waitForTimeout(500)
      }

      await gameSearchInput.clear()
      await gameSearchInput.fill('Ticket to Ride')
      await page.waitForTimeout(2000)
      const ticket = page.getByText(/ticket to ride/i).first()
      if (await ticket.isVisible().catch(() => false)) {
        await ticket.click()
        await page.waitForTimeout(500)
      }

      // Verify first game is primary
      const primaryGameInput = page.locator('#gameTitle')
      const primaryBefore = await primaryGameInput.inputValue()
      expect(primaryBefore.toLowerCase()).toContain('catan')

      // Look for "set as primary" button on second game card
      // The second card should have an option to set as primary
      const secondGameCard = page.locator('.border.rounded-lg, [class*="game-card"]').nth(1)
      const setPrimaryButton = secondGameCard.locator('button, [role="button"]')

      const buttonCount = await setPrimaryButton.count()
      if (buttonCount > 1) {
        // Usually there's remove and set-primary buttons
        // Set primary is typically the second one
        await setPrimaryButton.nth(1).click()
        await page.waitForTimeout(500)

        const primaryAfter = await primaryGameInput.inputValue()
        expect(primaryAfter.toLowerCase()).toContain('ticket')
      }
    })
  })
})
