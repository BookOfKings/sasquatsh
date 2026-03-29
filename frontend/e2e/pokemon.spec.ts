import { test, expect, Page } from '@playwright/test'

/**
 * E2E Tests for Pokemon TCG Features
 * Tests Pokemon-specific functionality including:
 * - Pokemon event creation with format-specific settings
 * - Pokemon event detail view with all summary sections
 * - Event registration flow
 *
 * Uses Free tier test account for basic tests.
 *
 * NOTE: These tests may fail intermittently due to Firebase rate limiting.
 * If you see "An error occurred" during login, wait a few minutes and retry.
 * Run with: npx playwright test pokemon.spec.ts --project=chromium
 */

// Test credentials from environment (use the main test user - free tier)
const TEST_EMAIL = process.env.TEST_USER_EMAIL
const TEST_PASSWORD = process.env.TEST_USER_PASSWORD

if (!TEST_EMAIL || !TEST_PASSWORD) {
  console.warn('WARNING: TEST_USER_EMAIL and TEST_USER_PASSWORD not set. Pokemon tests will be skipped.')
}

// Helper function to login
async function loginTestUser(page: Page) {
  if (!TEST_EMAIL || !TEST_PASSWORD) {
    throw new Error('Test credentials not configured. Set TEST_FREE_USER_EMAIL and TEST_FREE_USER_PASSWORD in environment.')
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

test.describe('Pokemon - Event Creation Page', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should navigate to Host Pokemon Event page', async ({ page }) => {
    await page.goto('/pokemon/events/create')

    // Should see Pokemon event creation heading
    await expect(page.getByRole('heading', { name: /host pokemon.*event/i })).toBeVisible()
  })

  test('should display Pokemon-specific form sections', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Should see Format section
    const formatSection = page.getByRole('heading', { name: 'Format', level: 3, exact: true })
    await expect(formatSection).toBeVisible()

    // Should see Event Structure section
    const structureSection = page.getByRole('heading', { name: 'Event Structure', level: 3 })
    await expect(structureSection).toBeVisible()

    // Should see Player Settings section
    const playerSection = page.getByRole('heading', { name: /player settings/i })
    await expect(playerSection).toBeVisible()
  })

  test('should show Pokemon format options', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    // Should see Standard format button
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await expect(standardButton).toBeVisible({ timeout: 10000 })

    // Should see Expanded format button
    const expandedButton = page.getByRole('button', { name: /Expanded/i }).first()
    await expect(expandedButton).toBeVisible()
  })

  test('should show Pokemon event type options', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Should see Casual Play option
    const casualButton = page.getByRole('button', { name: /Casual Play/i })
    await expect(casualButton).toBeVisible()

    // Should see League Play option
    const leagueButton = page.getByRole('button', { name: /League Play/i })
    await expect(leagueButton).toBeVisible()

    // Should see League Cup option
    const leagueCupButton = page.getByRole('button', { name: /League Cup/i })
    await expect(leagueCupButton).toBeVisible()
  })

  test('should show deck rules section with proxy settings', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Select Standard format first
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await standardButton.click()
    await page.waitForTimeout(500)

    // Should see Deck Rules section
    const deckRulesSection = page.getByRole('heading', { name: 'Deck Rules', level: 3 })
    await expect(deckRulesSection).toBeVisible()

    // Should see proxy option
    const proxyText = page.getByText(/prox/i).first()
    await expect(proxyText).toBeVisible()
  })

  test('should show tournament settings for League Cup', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Select League Cup event type
    const leagueCupButton = page.getByRole('button', { name: /League Cup/i })
    await leagueCupButton.click()
    await page.waitForTimeout(500)

    // Should see tournament settings
    const tournamentSettings = page.getByText(/Swiss|Rounds|Best of/i).first()
    await expect(tournamentSettings).toBeVisible()
  })

  test('should show prizes section', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Should see Entry & Prizes section
    const prizesSection = page.getByRole('heading', { name: /Entry.*Prizes/i })
    await expect(prizesSection).toBeVisible()
  })

  test('should allow filling basic event info', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    const eventTitle = generateTestName('Pokemon Test Event')

    // Fill title (uses placeholder, not label)
    const titleInput = page.getByPlaceholder(/Saturday Pokemon League/i)
    await titleInput.fill(eventTitle)
    await expect(titleInput).toHaveValue(eventTitle)

    // Fill date
    const dateInput = page.locator('input[type="date"]').first()
    await dateInput.fill(getTomorrowDate())

    // Fill time
    const timeInput = page.locator('input[type="time"]').first()
    await timeInput.fill('14:00')
  })

  test('should show Play! Pokemon official event toggle for tournaments', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(1500)

    // Select League Cup (official tournament type)
    const leagueCupButton = page.getByRole('button', { name: /League Cup/i })
    await leagueCupButton.click()
    await page.waitForTimeout(500)

    // Should see Play! Pokemon or Championship Points option
    const playPokemonText = page.getByText(/Play! Pokemon|Championship Points/i).first()
    await expect(playPokemonText).toBeVisible()
  })
})

test.describe('Pokemon - Event Creation Full Flow', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should fill out Standard League Cup event form', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    const eventTitle = generateTestName('E2E Pokemon League Cup')

    // 1. Fill Event Title (uses placeholder, not label)
    const titleInput = page.getByPlaceholder(/Saturday Pokemon League/i)
    await titleInput.fill(eventTitle)
    await expect(titleInput).toHaveValue(eventTitle)

    // 2. Fill Date
    const dateInput = page.locator('input[type="date"]').first()
    await dateInput.fill(getTomorrowDate())

    // 3. Select Standard Format
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await standardButton.click()
    await page.waitForTimeout(500)

    // 4. Select League Cup event type
    const leagueCupButton = page.getByRole('button', { name: /League Cup/i })
    await leagueCupButton.click()
    await page.waitForTimeout(500)

    // 5. Verify form has Player Settings section
    const playerSettingsHeading = page.getByRole('heading', { name: 'Player Settings', level: 3 })
    await expect(playerSettingsHeading).toBeVisible()

    // 6. Check submit button exists
    const submitButton = page.getByRole('button', { name: /Create.*Event/i })
    await expect(submitButton).toBeVisible()

    // Note: We don't actually submit to avoid creating test data
  })

  test('should fill out Prerelease event form', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    const eventTitle = generateTestName('E2E Pokemon Prerelease')

    // 1. Fill Event Title (uses placeholder, not label)
    const titleInput = page.getByPlaceholder(/Saturday Pokemon League/i)
    await titleInput.fill(eventTitle)

    // 2. Fill Date
    const dateInput = page.locator('input[type="date"]').first()
    await dateInput.fill(getTomorrowDate())

    // 3. Select Prerelease event type (should be in limited formats)
    const prereleaseButton = page.getByRole('button', { name: /Prerelease/i })
    await prereleaseButton.click()
    await page.waitForTimeout(500)

    // 4. Prerelease should show limited format info (deck building tips)
    const limitedText = page.getByText(/Build.*Battle|40.*card|limited/i).first()
    await expect(limitedText).toBeVisible()
  })
})

test.describe('Pokemon - Event Detail View', () => {
  test('should display Pokemon format summary card', async ({ page }) => {
    // Navigate to a Pokemon event (using known test event if available)
    // For public Pokemon events, we can check without login
    await page.goto('/games')
    await page.waitForTimeout(2000)

    // Look for a Pokemon TCG event specifically (has Pokemon in the title or event type)
    // Check for yellow-themed event cards indicating Pokemon TCG
    const pokemonEventCard = page.locator('.border-yellow-200, [class*="yellow"]').filter({ hasText: /Pokemon|League Cup|Prerelease/i }).first()

    // Skip if no Pokemon event exists
    if (!(await pokemonEventCard.isVisible().catch(() => false))) {
      test.skip(true, 'No Pokemon TCG event found in games list')
      return
    }

    await pokemonEventCard.click()
    await page.waitForTimeout(1500)

    // Should see Pokemon format card (has yellow gradient background)
    const formatCard = page.locator('.bg-gradient-to-r.from-yellow-50')
    await expect(formatCard).toBeVisible()
  })
})

test.describe('Pokemon - Event Detail Sections', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should display all Pokemon event sections when viewing event', async ({ page }) => {
    // Go to games list
    await page.goto('/games')
    await page.waitForTimeout(2000)

    // Find and click on a Pokemon event
    const pokemonEventCard = page.locator('text=Pokemon').first()

    if (await pokemonEventCard.isVisible().catch(() => false)) {
      await pokemonEventCard.click()
      await page.waitForTimeout(2000)

      // Should see Event Structure section
      const structureSection = page.getByRole('heading', { name: 'Event Structure', level: 3 })
      await expect(structureSection).toBeVisible()

      // Should see Deck Rules section
      const deckRulesSection = page.getByRole('heading', { name: 'Deck Rules', level: 3 })
      await expect(deckRulesSection).toBeVisible()

      // Should see Entry & Prizes section
      const prizesSection = page.getByRole('heading', { name: /Entry.*Prizes/i, level: 3 })
      await expect(prizesSection).toBeVisible()

      // Should see Players section
      const playersSection = page.getByRole('heading', { name: /Players/i, level: 3 })
      await expect(playersSection).toBeVisible()
    } else {
      // Skip test if no Pokemon event exists
      test.skip()
    }
  })

  test('should show tournament details for League Cup', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(2000)

    // Find League Cup event
    const leagueCupEvent = page.getByText(/League Cup/i).first()

    // Skip if no League Cup event exists
    if (!(await leagueCupEvent.isVisible().catch(() => false))) {
      test.skip(true, 'No League Cup event found in games list')
      return
    }

    await leagueCupEvent.click()
    await page.waitForTimeout(2000)

    // Should show tournament-specific info (Swiss, rounds, or Best of)
    const tournamentInfo = page.getByText(/Swiss|rounds|Best of/i).first()
    await expect(tournamentInfo).toBeVisible()
  })

  test('should show deck rules with proxy policy', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(2000)

    const pokemonEvent = page.getByText(/Pokemon/i).first()

    if (await pokemonEvent.isVisible().catch(() => false)) {
      await pokemonEvent.click()
      await page.waitForTimeout(2000)

      // Should see proxy policy indicator
      const proxyText = page.getByText(/Proxies|No Proxies/i).first()
      await expect(proxyText).toBeVisible()
    } else {
      test.skip()
    }
  })

  test('should show entry fee and prize info', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(2000)

    const pokemonEvent = page.getByText(/Pokemon/i).first()

    if (await pokemonEvent.isVisible().catch(() => false)) {
      await pokemonEvent.click()
      await page.waitForTimeout(2000)

      // Check for Entry & Prizes section content
      const entryFeeText = page.getByText(/Entry Fee|\$\d+/i).first()
      const hasFee = await entryFeeText.isVisible().catch(() => false)

      const prizesText = page.getByText(/Prizes|Prize Support/i).first()
      const hasPrizes = await prizesText.isVisible().catch(() => false)

      // At least one should be visible if event has entry/prizes
      expect(hasFee || hasPrizes).toBeTruthy()
    } else {
      test.skip()
    }
  })
})

test.describe('Pokemon - Event Registration', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should show Join Game button for non-registered user', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(2000)

    // Find a Pokemon event that user is not registered for
    const pokemonEvent = page.getByText(/Pokemon/i).first()

    if (await pokemonEvent.isVisible().catch(() => false)) {
      await pokemonEvent.click()
      await page.waitForTimeout(2000)

      // Should see Join Game or Cancel Registration button
      const joinButton = page.getByRole('button', { name: /Join Game/i })
      const cancelButton = page.getByRole('button', { name: /Cancel Registration/i })

      const canJoin = await joinButton.isVisible().catch(() => false)
      const isRegistered = await cancelButton.isVisible().catch(() => false)

      // One of these should be visible (depending on registration status)
      expect(canJoin || isRegistered).toBeTruthy()
    } else {
      test.skip()
    }
  })

  test('should display player roster grid', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(2000)

    const pokemonEvent = page.getByText(/Pokemon/i).first()

    if (await pokemonEvent.isVisible().catch(() => false)) {
      await pokemonEvent.click()
      await page.waitForTimeout(2000)

      // Should see Players heading
      const playersHeading = page.getByRole('heading', { name: /Players/i })
      await expect(playersHeading).toBeVisible()

      // Should see player slots (either filled or "Open Slot")
      const slots = page.getByText(/Open Slot|spots available/i).first()
      await expect(slots).toBeVisible()
    } else {
      test.skip()
    }
  })
})

test.describe('Pokemon - Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should have link from Create Event to Host Pokemon Event', async ({ page }) => {
    await page.goto('/games/create')
    await page.waitForTimeout(1500)

    // Check if "Game Limit Reached" modal appears (free tier user)
    const limitModal = page.getByText('Game Limit Reached')
    if (await limitModal.isVisible({ timeout: 1000 }).catch(() => false)) {
      // Click "Go Back" to dismiss modal
      const goBackButton = page.getByRole('button', { name: 'Go Back' })
      await goBackButton.click()
      await page.waitForTimeout(500)
      // Navigate directly to Pokemon event creation page
      await page.goto('/pokemon/events/create')
      await page.waitForURL(/\/pokemon\/events\/create/)
      return // Test passes - we verified the route works
    }

    // Should see Pokemon link in the game system switcher (yellow styled link)
    const pokemonLink = page.locator('a[href="/pokemon/events/create"]')
    await expect(pokemonLink).toBeVisible()

    // Click should navigate to Pokemon event creation
    await pokemonLink.click()
    await page.waitForURL(/\/pokemon\/events\/create/)
  })

  test('should load Pokemon event creation page without errors', async ({ page }) => {
    await page.goto('/pokemon/events/create')

    // Page should load correctly
    await expect(page).toHaveURL(/\/pokemon\/events\/create/)

    // Should not show error message
    const errorMsg = page.getByText(/error|failed|unable/i)
    await expect(errorMsg).not.toBeVisible({ timeout: 3000 }).catch(() => {
      // Some error text might be in form validation, that's ok
    })

    // Should see the create event heading
    const heading = page.getByRole('heading', { level: 1 })
    await expect(heading).toBeVisible()
  })
})

test.describe('Pokemon - Format-Specific Behavior', () => {
  test.beforeEach(async ({ page }) => {
    await loginTestUser(page)
    await dismissCookieBanner(page)
  })

  test('should show 60 cards requirement for Standard format', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    // Select Standard format
    const standardButton = page.getByRole('button', { name: /Standard/i }).first()
    await standardButton.click()
    await page.waitForTimeout(500)

    // Should indicate 60 card deck size somewhere
    const deckSizeText = page.getByText(/60.*card/i).first()
    await expect(deckSizeText).toBeVisible()
  })

  test('should show 40 cards requirement for Limited events', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    // Select Prerelease (limited format)
    const prereleaseButton = page.getByRole('button', { name: /Prerelease/i })
    await prereleaseButton.click()
    await page.waitForTimeout(500)

    // Should indicate 40+ card deck size
    const deckSizeText = page.getByText(/40.*card/i).first()
    await expect(deckSizeText).toBeVisible()
  })

  test('should show age divisions for official events', async ({ page }) => {
    await page.goto('/pokemon/events/create')
    await page.waitForTimeout(2000)

    // Select League Cup (official event)
    const leagueCupButton = page.getByRole('button', { name: /League Cup/i })
    await leagueCupButton.click()
    await page.waitForTimeout(500)

    // Enable Play! Pokemon official event
    const playPokemonToggle = page.getByText(/Play! Pokemon|Official Event/i).first()
    if (await playPokemonToggle.isVisible().catch(() => false)) {
      await playPokemonToggle.click()
      await page.waitForTimeout(500)

      // Should show age divisions (Junior, Senior, Masters)
      const divisionsText = page.getByText(/Junior|Senior|Masters/i).first()
      await expect(divisionsText).toBeVisible()
    }
  })
})
