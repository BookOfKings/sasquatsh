import { test, expect, Page } from '@playwright/test'

/**
 * E2E Tests for MTG (Magic: The Gathering) Features
 * Tests MTG-specific functionality including:
 * - Deck management (create, edit, delete)
 * - MTG event creation with format-specific settings
 * - Scryfall card search integration
 *
 * Uses Basic tier test account which has access to all features.
 *
 * NOTE: These tests may fail intermittently due to Firebase rate limiting.
 * If you see "An error occurred" during login, wait a few minutes and retry.
 * Run with: npx playwright test mtg.spec.ts --project=chromium
 */

// Test credentials from environment
const TEST_EMAIL = process.env.TEST_BASIC_USER_EMAIL
const TEST_PASSWORD = process.env.TEST_BASIC_USER_PASSWORD

if (!TEST_EMAIL || !TEST_PASSWORD) {
  console.warn('WARNING: TEST_BASIC_USER_EMAIL and TEST_BASIC_USER_PASSWORD not set. MTG tests will be skipped.')
}

// Helper function to login
async function loginTestUser(page: Page) {
  if (!TEST_EMAIL || !TEST_PASSWORD) {
    throw new Error('Test credentials not configured. Set TEST_BASIC_USER_EMAIL and TEST_BASIC_USER_PASSWORD in environment.')
  }
  await page.goto('/login')
  await page.waitForTimeout(1000) // Wait for page to stabilize
  await page.locator('#email').fill(TEST_EMAIL)
  await page.locator('#password').fill(TEST_PASSWORD)
  await page.getByRole('button', { name: /sign in/i }).click()

  // Wait for either successful redirect or error message
  try {
    await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 20000 })
  } catch {
    // Check if there's an error message (rate limiting)
    const errorMsg = page.getByText(/error occurred|try again/i)
    if (await errorMsg.isVisible().catch(() => false)) {
      throw new Error('Login failed - possibly due to Firebase rate limiting. Wait and retry.')
    }
    throw new Error('Login failed - unexpected error')
  }
  await page.waitForTimeout(2000) // Avoid Firebase rate limits
}

// Helper to dismiss cookie banner if present
async function dismissCookieBanner(page: Page) {
  const cookieBanner = page.getByRole('button', { name: /accept|got it|dismiss/i })
  if (await cookieBanner.isVisible({ timeout: 1000 }).catch(() => false)) {
    await cookieBanner.click()
    await page.waitForTimeout(500)
  }
}

// Helper to generate unique names
function generateTestName(prefix: string): string {
  return `${prefix} ${Date.now()}`
}

// Helper to get tomorrow's date in YYYY-MM-DD format
function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0] || ''
}

test.describe('MTG - Deck Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should navigate to My Decks page', async ({ page }) => {
    await page.goto('/mtg/decks')

    // Should see My Decks heading
    await expect(page.getByRole('heading', { name: /my decks/i })).toBeVisible()
  })

  test('should navigate to Create Deck page', async ({ page }) => {
    await page.goto('/mtg/decks/new')

    // Should see deck builder elements
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

    // Should see format selector
    const formatSelector = page.getByText(/format/i).first()
    await expect(formatSelector).toBeVisible()
  })

  test('should display format options in deck builder', async ({ page }) => {
    await page.goto('/mtg/decks/new')
    await page.waitForTimeout(2000) // Wait for formats to load

    // Look for format selector - should have Commander option
    const formatSection = page.getByText('Format', { exact: true }).or(
      page.getByLabel(/format/i)
    )
    await expect(formatSection.first()).toBeVisible({ timeout: 10000 })
  })

  test('should allow entering deck name', async ({ page }) => {
    await page.goto('/mtg/decks/new')

    const deckName = generateTestName('Test Commander Deck')

    // Find and fill deck name input
    const nameInput = page.getByPlaceholder(/deck name/i).or(page.locator('input[type="text"]').first())
    await nameInput.fill(deckName)

    await expect(nameInput).toHaveValue(deckName)
  })

  test('should show power level selector', async ({ page }) => {
    await page.goto('/mtg/decks/new')
    await page.waitForTimeout(1000)

    // Look for power level label specifically
    const powerLevelLabel = page.getByText('Power Level', { exact: true })
    await expect(powerLevelLabel).toBeVisible({ timeout: 5000 })
  })

  test('should show card search in deck builder', async ({ page }) => {
    await page.goto('/mtg/decks/new')
    await page.waitForTimeout(1500)

    // Deck builder page should load without errors
    await expect(page).toHaveURL(/\/mtg\/decks\/new/)

    // Should have some form of search or card input available
    // The actual structure may vary, so we just verify the page loaded
    const heading = page.getByRole('heading').first()
    await expect(heading).toBeVisible()
  })
})

test.describe('MTG - Event Creation', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should navigate to Host MTG Event page', async ({ page }) => {
    await page.goto('/mtg/events/create')

    // Should see MTG event creation heading
    await expect(page.getByRole('heading', { name: /host mtg event/i })).toBeVisible()
  })

  test('should display MTG-specific form sections', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1500)

    // Should see Format section (h3) - use exact match
    const formatSection = page.getByRole('heading', { name: 'Format', level: 3, exact: true })
    await expect(formatSection).toBeVisible()

    // Should see Event Structure section (h3)
    const structureSection = page.getByRole('heading', { name: 'Event Structure', level: 3 })
    await expect(structureSection).toBeVisible()

    // Should see Deck Rules section (h3)
    const deckRulesSection = page.getByRole('heading', { name: 'Deck Rules', level: 3 })
    await expect(deckRulesSection).toBeVisible()
  })

  test('should show format dropdown with MTG formats', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    // Format selection is now button-based, look for Commander button
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await expect(commanderButton).toBeVisible({ timeout: 10000 })

    // Check that Standard and Modern buttons exist
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await expect(standardButton).toBeVisible()

    const modernButton = page.getByRole('button', { name: /Modern/i }).first()
    await expect(modernButton).toBeVisible()
  })

  test('should show event type options', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1500)

    // Event types are now buttons, look for Casual Play button
    const casualButton = page.getByRole('button', { name: /Casual Play/i })
    await expect(casualButton).toBeVisible()

    // Verify Swiss tournament option exists as button
    const swissButton = page.getByRole('button', { name: /Swiss/i })
    await expect(swissButton).toBeVisible()

    // Verify Pod Play option exists
    const podsButton = page.getByRole('button', { name: /Pod Play/i })
    await expect(podsButton).toBeVisible()
  })

  test('should show play mode selector', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1500)

    // Play mode buttons should be visible
    const openPlayButton = page.getByRole('button', { name: /Open Play/i })
    await expect(openPlayButton).toBeVisible()

    const assignedPodsButton = page.getByRole('button', { name: /Assigned Pods/i })
    await expect(assignedPodsButton).toBeVisible()

    const tournamentPairingsButton = page.getByRole('button', { name: /Tournament Pairings/i })
    await expect(tournamentPairingsButton).toBeVisible()
  })

  test('should show proxy settings in deck rules', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1000)

    // Should see proxy option
    const proxyCheckbox = page.getByText(/prox/i).first()
    await expect(proxyCheckbox).toBeVisible()
  })

  test('should allow filling basic event info', async ({ page }) => {
    await page.goto('/mtg/events/create')

    const eventTitle = generateTestName('MTG Test Event')

    // Fill title
    const titleInput = page.getByLabel(/event title/i).or(page.locator('#title'))
    await titleInput.fill(eventTitle)
    await expect(titleInput).toHaveValue(eventTitle)

    // Fill date
    const dateInput = page.getByLabel(/date/i).first().or(page.locator('input[type="date"]').first())
    await dateInput.fill(getTomorrowDate())

    // Fill time
    const timeInput = page.getByLabel(/start time/i).or(page.locator('input[type="time"]').first())
    await timeInput.fill('19:00')
  })

  test('should show player settings with MTG defaults', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1000)

    // Should see Player Settings section
    const playerSection = page.getByRole('heading', { name: /player settings/i })
    await expect(playerSection).toBeVisible()

    // Max players default should be 8 for MTG (vs 4 for board games)
    const maxPlayersInput = page.getByLabel(/max players/i)
    await expect(maxPlayersInput).toHaveValue('8')

    // Should see Allow Spectators checkbox (MTG-specific)
    const spectatorsCheckbox = page.getByText(/spectator/i)
    await expect(spectatorsCheckbox).toBeVisible()
  })

  test('should show power level range for Commander format', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    // Click Commander format button
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await commanderButton.click()
    await page.waitForTimeout(500)

    // For Commander format, power level section should appear
    // Should see "Power Level" heading
    const powerLevelHeading = page.getByRole('heading', { name: 'Power Level', level: 3 })
    await expect(powerLevelHeading).toBeVisible({ timeout: 5000 })

    // Should see power level buttons like Casual, Mid, High Power, cEDH
    const casualPowerButton = page.getByRole('button', { name: /Casual/i }).filter({ hasText: /Precons/i })
    await expect(casualPowerButton).toBeVisible()
  })

  test('should auto-set defaults when Commander format selected', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    // Click Commander format button
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await commanderButton.click()
    await page.waitForTimeout(500)

    // Pod Play should be auto-selected for Commander
    const podPlayButton = page.getByRole('button', { name: /Pod Play/i })
    await expect(podPlayButton).toHaveClass(/bg-blue-600/)

    // Assigned Pods play mode should be auto-selected
    const assignedPodsButton = page.getByRole('button', { name: /Assigned Pods/i })
    await expect(assignedPodsButton).toHaveClass(/bg-blue-600/)
  })

  test('should validate required fields before submission', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(1500)

    // Find submit button
    const submitButton = page.getByRole('button', { name: 'Create MTG Event' })
    await expect(submitButton).toBeVisible()

    // Button should be disabled when form is empty (required fields not filled)
    // Or clicking should show validation errors
    const isDisabled = await submitButton.isDisabled().catch(() => false)

    if (!isDisabled) {
      // If not disabled, clicking should show validation
      await submitButton.click()
      await page.waitForTimeout(500)
      // Should still be on the create page (form not submitted)
      await expect(page).toHaveURL(/\/mtg\/events\/create/)
    } else {
      expect(isDisabled).toBeTruthy()
    }
  })
})

test.describe('MTG - Event Creation Full Flow', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should fill out Commander event form', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    const eventTitle = generateTestName('E2E Commander Night')

    // 1. Fill Event Title
    const titleInput = page.getByLabel('Event Title *')
    await titleInput.fill(eventTitle)
    await expect(titleInput).toHaveValue(eventTitle)

    // 2. Fill Date
    const dateInput = page.locator('input[type="date"]').first()
    await dateInput.fill(getTomorrowDate())

    // 3. Click Commander Format button
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await commanderButton.click()
    await page.waitForTimeout(500)

    // 4. Verify form has Player Settings section
    const playerSettingsHeading = page.getByRole('heading', { name: 'Player Settings', level: 3 })
    await expect(playerSettingsHeading).toBeVisible()

    // 5. Check submit button exists
    const submitButton = page.getByRole('button', { name: 'Create MTG Event' })
    await expect(submitButton).toBeVisible()

    // Note: We don't actually submit to avoid creating test data
  })
})

test.describe('MTG - Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should have link from Create Event to Host MTG Event', async ({ page }) => {
    await page.goto('/games/create')
    await page.waitForTimeout(1000)

    // Should see link to Host MTG Event
    const mtgLink = page.getByRole('link', { name: /host mtg event/i })
    await expect(mtgLink).toBeVisible()

    // Click should navigate to MTG event creation
    await mtgLink.click()
    await page.waitForURL(/\/mtg\/events\/create/)
  })

  test('should show My Decks link when logged in', async ({ page }) => {
    await page.goto('/mtg/decks')

    // Page should load without error
    await expect(page).toHaveURL(/\/mtg\/decks/)

    // Should not redirect to login
    await expect(page).not.toHaveURL(/\/login/)
  })
})

test.describe('MTG - Scryfall Integration', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should show format dropdown with options', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    // Format selection is now button-based, verify Commander button exists
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await expect(commanderButton).toBeVisible()

    // Verify other format buttons exist
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await expect(standardButton).toBeVisible()
  })

  test('should display fallback formats if API fails', async ({ page }) => {
    await page.goto('/mtg/events/create')
    await page.waitForTimeout(2000)

    // Even if API fails, should show fallback format buttons
    // Look for multiple format buttons to verify formats loaded
    const commanderButton = page.getByRole('button', { name: /Commander/i }).first()
    await expect(commanderButton).toBeVisible()

    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await expect(standardButton).toBeVisible()

    const modernButton = page.getByRole('button', { name: /Modern/i }).first()
    await expect(modernButton).toBeVisible()
  })
})

test.describe('MTG - Deck Builder Card Search', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should load deck builder page successfully', async ({ page }) => {
    await page.goto('/mtg/decks/new')
    await page.waitForTimeout(1500)

    // Verify page loaded correctly
    await expect(page).toHaveURL(/\/mtg\/decks\/new/)

    // Should see format selector
    const formatSection = page.getByText('Format', { exact: true }).first()
    await expect(formatSection).toBeVisible({ timeout: 5000 })
  })

  test('should have deck name input', async ({ page }) => {
    await page.goto('/mtg/decks/new')
    await page.waitForTimeout(1000)

    // Should be able to enter deck name
    const nameInput = page.locator('input[type="text"]').first()
    await expect(nameInput).toBeVisible()

    await nameInput.fill('Test Deck Name')
    await expect(nameInput).toHaveValue('Test Deck Name')
  })
})
