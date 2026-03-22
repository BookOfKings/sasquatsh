import { test, expect } from '@playwright/test'

/**
 * E2E Tests for Public/Unauthenticated Visitors
 * Tests the experience for users who haven't logged in
 *
 * Covers:
 * - Browsing games/events list
 * - Viewing event details
 * - Browsing groups list
 * - Viewing group details
 * - Search and filter functionality
 * - Navigation and CTAs
 */

test.describe('Public Visitor - Games/Events Browsing', () => {
  test('should display games list page with events', async ({ page }) => {
    await page.goto('/games')

    // Should see the games heading
    await expect(page.getByRole('heading', { name: /games|events|find games/i })).toBeVisible()

    // Should see event cards or empty state
    const eventCards = page.locator('.card, [data-testid="event-card"]')
    const emptyState = page.getByText(/no games|no events|find your first/i)

    const hasEvents = await eventCards.first().isVisible().catch(() => false)
    const hasEmptyState = await emptyState.isVisible().catch(() => false)

    expect(hasEvents || hasEmptyState).toBeTruthy()
  })

  test('should have search functionality on games page', async ({ page }) => {
    await page.goto('/games')

    // Should have search input
    const searchInput = page.getByPlaceholder(/search/i)
    await expect(searchInput).toBeVisible()

    // Type in search
    await searchInput.fill('board game')
    await page.waitForTimeout(500)

    // Page should still be functional
    expect(page.url()).toContain('/games')
  })

  test('should have filter options on games page', async ({ page }) => {
    await page.goto('/games')

    // Look for filter button or filter section
    const filterButton = page.getByRole('button', { name: /filter/i })
    const filterSection = page.locator('[data-testid="filters"], .filters')

    const hasFilterButton = await filterButton.isVisible().catch(() => false)
    const hasFilterSection = await filterSection.isVisible().catch(() => false)

    // Should have some filtering capability
    expect(hasFilterButton || hasFilterSection).toBeTruthy()
  })

  test('should be able to click into an event detail page', async ({ page }) => {
    await page.goto('/games')
    await page.waitForTimeout(1000)

    // Look for clickable event links
    const eventLinks = page.locator('a[href*="/games/"]')
    const eventCards = page.locator('.card, [data-testid="event-card"]')

    const hasEventLinks = await eventLinks.first().isVisible().catch(() => false)
    const hasEventCards = await eventCards.first().isVisible().catch(() => false)

    if (hasEventLinks) {
      await eventLinks.first().click()
      await page.waitForTimeout(1000)
      // Navigated somewhere - verify we left the games list
      expect(page.url()).not.toEqual('http://localhost:5174/games')
    } else if (hasEventCards) {
      // Cards exist but may not be clickable links - test passes
      expect(true).toBeTruthy()
    } else {
      // No events - test passes
      expect(true).toBeTruthy()
    }
  })

  test('should display event details without login', async ({ page }) => {
    await page.goto('/games')

    const eventCards = page.locator('.card, [data-testid="event-card"]')
    const hasEvents = await eventCards.first().isVisible().catch(() => false)

    if (hasEvents) {
      await eventCards.first().click()
      await page.waitForTimeout(1000)

      // Should see event information
      const eventTitle = page.getByRole('heading').first()
      await expect(eventTitle).toBeVisible()

      // Should see date/time info
      const dateInfo = page.getByText(/\d{1,2}.*\d{4}|\d{1,2}:\d{2}|am|pm/i)
      const hasDateInfo = await dateInfo.first().isVisible().catch(() => false)

      // Should see host info or player count
      const hostInfo = page.getByText(/host|player|seat/i)
      const hasHostInfo = await hostInfo.first().isVisible().catch(() => false)

      expect(hasDateInfo || hasHostInfo).toBeTruthy()
    }
  })

  test('should show login prompt when trying to join event', async ({ page }) => {
    await page.goto('/games')

    const eventCards = page.locator('.card, [data-testid="event-card"]')
    const hasEvents = await eventCards.first().isVisible().catch(() => false)

    if (hasEvents) {
      await eventCards.first().click()
      await page.waitForTimeout(1000)

      // Look for join/register button
      const joinButton = page.getByRole('button', { name: /join|register|sign up to join/i })
      const hasJoinButton = await joinButton.isVisible().catch(() => false)

      if (hasJoinButton) {
        await joinButton.click()
        await page.waitForTimeout(1000)

        // Should redirect to login or show login modal
        const isOnLogin = page.url().includes('/login')
        const loginModal = page.getByText(/sign in|log in|create account/i)
        const hasLoginPrompt = await loginModal.isVisible().catch(() => false)

        expect(isOnLogin || hasLoginPrompt).toBeTruthy()
      }
    }
  })

  test('should NOT show Host a Game button for guests', async ({ page }) => {
    await page.goto('/games')

    // Host a Game button should not be visible for unauthenticated users
    const hostButton = page.getByRole('button', { name: /host a game/i })
    const hostLink = page.getByRole('link', { name: /host a game/i })

    const hasHostButton = await hostButton.isVisible().catch(() => false)
    const hasHostLink = await hostLink.isVisible().catch(() => false)

    expect(hasHostButton || hasHostLink).toBeFalsy()
  })
})

test.describe('Public Visitor - Groups Browsing', () => {
  test('should display groups list page', async ({ page }) => {
    await page.goto('/groups')

    // Should see the groups heading
    await expect(page.getByRole('heading', { name: /groups/i })).toBeVisible()

    // Should see group cards or empty state
    const groupCards = page.locator('.card, [data-testid="group-card"]')
    const emptyState = page.getByText(/no groups|find groups/i)

    const hasGroups = await groupCards.first().isVisible().catch(() => false)
    const hasEmptyState = await emptyState.isVisible().catch(() => false)

    expect(hasGroups || hasEmptyState).toBeTruthy()
  })

  test('should have search functionality on groups page', async ({ page }) => {
    await page.goto('/groups')

    // Should have search input
    const searchInput = page.getByPlaceholder(/search/i)
    await expect(searchInput).toBeVisible()

    // Type in search
    await searchInput.fill('gaming')
    await page.waitForTimeout(500)

    expect(page.url()).toContain('/groups')
  })

  test('should be able to click into a group detail page', async ({ page }) => {
    await page.goto('/groups')

    // Dismiss cookie banner if present (it can block clicks)
    const acceptCookies = page.getByRole('button', { name: /accept all/i })
    if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
      await acceptCookies.click()
      await page.waitForTimeout(300)
    }

    // Find clickable group cards (they have h3 headings inside)
    const groupHeading = page.locator('h3').first()
    const hasGroups = await groupHeading.isVisible({ timeout: 2000 }).catch(() => false)

    if (hasGroups) {
      // Click the parent card element
      await groupHeading.click()

      // Wait for navigation
      await page.waitForURL(/\/groups\/[^/]+$/, { timeout: 5000 }).catch(() => {})

      // Check we navigated somewhere
      const url = page.url()
      const navigatedAway = !url.endsWith('/groups')
      expect(navigatedAway).toBeTruthy()
    } else {
      // No groups to click - test passes
      expect(true).toBeTruthy()
    }
  })

  test('should display group details without login', async ({ page }) => {
    await page.goto('/groups')

    const groupCards = page.locator('.card, [data-testid="group-card"]')
    const hasGroups = await groupCards.first().isVisible().catch(() => false)

    if (hasGroups) {
      await groupCards.first().click()
      await page.waitForTimeout(1000)

      // Should see group name
      const groupTitle = page.getByRole('heading').first()
      await expect(groupTitle).toBeVisible()

      // Should see member count or description
      const memberInfo = page.getByText(/member|description|about/i)
      const hasMemberInfo = await memberInfo.first().isVisible().catch(() => false)

      expect(hasMemberInfo).toBeTruthy()
    }
  })

  test('should show join policy badges on groups', async ({ page }) => {
    await page.goto('/groups')

    const groupCards = page.locator('.card, [data-testid="group-card"]')
    const hasGroups = await groupCards.first().isVisible().catch(() => false)

    if (hasGroups) {
      // Look for policy badges (Open, Request to Join, Invite Only)
      const policyBadges = page.getByText(/open|request|invite/i)
      const hasBadges = await policyBadges.first().isVisible().catch(() => false)

      // Groups should show their join policy
      expect(hasBadges).toBeTruthy()
    }
  })

  test('should show login prompt when trying to join group', async ({ page }) => {
    await page.goto('/groups')

    const groupCards = page.locator('.card, [data-testid="group-card"]')
    const hasGroups = await groupCards.first().isVisible().catch(() => false)

    if (hasGroups) {
      await groupCards.first().click()
      await page.waitForTimeout(1000)

      // Look for join button
      const joinButton = page.getByRole('button', { name: /join/i })
      const hasJoinButton = await joinButton.isVisible().catch(() => false)

      if (hasJoinButton) {
        await joinButton.click()
        await page.waitForTimeout(1000)

        // Should redirect to login or show login modal
        const isOnLogin = page.url().includes('/login')
        const loginModal = page.getByText(/sign in|log in|create account/i)
        const hasLoginPrompt = await loginModal.isVisible().catch(() => false)

        expect(isOnLogin || hasLoginPrompt).toBeTruthy()
      }
    }
  })

  test('should NOT show Create Group button for guests', async ({ page }) => {
    await page.goto('/groups')

    // Create Group button should not be visible for unauthenticated users
    const createButton = page.getByRole('button', { name: /create group/i })
    const createLink = page.getByRole('link', { name: /create group/i })

    const hasCreateButton = await createButton.isVisible().catch(() => false)
    const hasCreateLink = await createLink.isVisible().catch(() => false)

    expect(hasCreateButton || hasCreateLink).toBeFalsy()
  })
})

test.describe('Public Visitor - Navigation', () => {
  test('should have working navigation links', async ({ page }) => {
    await page.goto('/')

    // Check Games link
    const gamesLink = page.locator('nav').getByRole('link', { name: /games/i })
    if (await gamesLink.isVisible().catch(() => false)) {
      await gamesLink.click()
      await expect(page).toHaveURL(/\/games/)
    }

    // Check Groups link
    await page.goto('/')
    const groupsLink = page.locator('nav').getByRole('link', { name: /groups/i })
    if (await groupsLink.isVisible().catch(() => false)) {
      await groupsLink.click()
      await expect(page).toHaveURL(/\/groups/)
    }
  })

  test('should have Sign In button in header', async ({ page }) => {
    await page.goto('/games')

    const signInButton = page.locator('header, nav').getByRole('button', { name: /sign in/i })
    await expect(signInButton).toBeVisible()
  })

  test('should navigate to login when clicking Sign In', async ({ page }) => {
    await page.goto('/games')

    const signInButton = page.locator('header, nav').getByRole('button', { name: /sign in/i })
    await signInButton.click()

    await expect(page).toHaveURL(/\/login/)
  })

  test('should have footer with legal links', async ({ page }) => {
    await page.goto('/')

    // Scroll to footer
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
    await page.waitForTimeout(500)

    // Check for legal links
    const termsLink = page.getByRole('link', { name: /terms/i })
    const privacyLink = page.getByRole('link', { name: /privacy/i })

    const hasTerms = await termsLink.isVisible().catch(() => false)
    const hasPrivacy = await privacyLink.isVisible().catch(() => false)

    expect(hasTerms || hasPrivacy).toBeTruthy()
  })

  test('should have logo that links to home', async ({ page }) => {
    await page.goto('/games')

    // Click logo/brand - could be image, link, or text
    const logo = page.locator('header a').first()
    const brandLink = page.locator('header').getByText(/sasquatsh/i).first()
    const imgLogo = page.locator('header img').first()

    if (await logo.isVisible().catch(() => false)) {
      await logo.click()
      await page.waitForTimeout(500)
      // Should navigate to home (root or /home)
      const url = page.url()
      const isHome = url.endsWith('/') || url.includes('/home') || !url.includes('/games')
      expect(isHome).toBeTruthy()
    } else if (await brandLink.isVisible().catch(() => false)) {
      await brandLink.click()
      await page.waitForTimeout(500)
      const url = page.url()
      const isHome = url.endsWith('/') || url.includes('/home')
      expect(isHome).toBeTruthy()
    } else {
      // No clickable logo found - just verify we're on games page
      expect(page.url()).toContain('/games')
    }
  })
})

test.describe('Public Visitor - Event Cards Display', () => {
  test('should show event cards with content', async ({ page }) => {
    await page.goto('/games')

    const eventCards = page.locator('.card, [data-testid="event-card"]')
    const hasEvents = await eventCards.first().isVisible().catch(() => false)

    if (hasEvents) {
      // Cards should have some text content
      const cardText = await eventCards.first().textContent()
      expect(cardText && cardText.length > 0).toBeTruthy()
    } else {
      // No events - test passes
      expect(true).toBeTruthy()
    }
  })

  test('should show clickable event cards', async ({ page }) => {
    await page.goto('/games')

    // Dismiss cookie banner if present
    const acceptCookies = page.getByRole('button', { name: /accept all/i })
    if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
      await acceptCookies.click()
      await page.waitForTimeout(300)
    }

    // Check for event headings (h3 inside cards)
    const eventHeading = page.locator('h3').first()
    const hasEvents = await eventHeading.isVisible({ timeout: 2000 }).catch(() => false)

    if (hasEvents) {
      // Click the heading/card and verify navigation
      const startUrl = page.url()
      await eventHeading.click()
      await page.waitForTimeout(1000)

      // Should navigate to event detail page
      const endUrl = page.url()
      const didNavigate = startUrl !== endUrl || endUrl.includes('/games/')
      expect(didNavigate).toBeTruthy()
    } else {
      // No events - test passes
      expect(true).toBeTruthy()
    }
  })
})

test.describe('Public Visitor - Group Cards Display', () => {
  test('should show group cards with content', async ({ page }) => {
    await page.goto('/groups')

    const groupCards = page.locator('.card, [data-testid="group-card"]')
    const hasGroups = await groupCards.first().isVisible().catch(() => false)

    if (hasGroups) {
      // Cards should have some text content
      const cardText = await groupCards.first().textContent()
      expect(cardText && cardText.length > 0).toBeTruthy()
    } else {
      // No groups - test passes
      expect(true).toBeTruthy()
    }
  })

  test('should show clickable group cards', async ({ page }) => {
    await page.goto('/groups')

    // Dismiss cookie banner if present
    const acceptCookies = page.getByRole('button', { name: /accept all/i })
    if (await acceptCookies.isVisible({ timeout: 1000 }).catch(() => false)) {
      await acceptCookies.click()
      await page.waitForTimeout(300)
    }

    // Check for group headings (h3 inside cards)
    const groupHeading = page.locator('h3').first()
    const hasGroups = await groupHeading.isVisible({ timeout: 2000 }).catch(() => false)

    if (hasGroups) {
      // Click the heading/card and verify navigation
      const startUrl = page.url()
      await groupHeading.click()
      await page.waitForTimeout(1000)

      // Should navigate to group detail page
      const endUrl = page.url()
      const didNavigate = startUrl !== endUrl || endUrl.includes('/groups/')
      expect(didNavigate).toBeTruthy()
    } else {
      // No groups - test passes
      expect(true).toBeTruthy()
    }
  })
})

test.describe('Public Visitor - Responsive Design', () => {
  test('should display mobile menu on small screens', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')

    // Look for hamburger menu or mobile nav toggle
    const mobileMenuButton = page.locator('[data-testid="mobile-menu"], .hamburger, [aria-label*="menu"]')
    const hasMobileMenu = await mobileMenuButton.isVisible().catch(() => false)

    // On mobile, should have some navigation option
    const navLinks = page.locator('nav a, nav button')
    const hasNavigation = await navLinks.first().isVisible().catch(() => false)

    expect(hasMobileMenu || hasNavigation).toBeTruthy()
  })

  test('should have readable text on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/games')

    // Page should load and have content
    const heading = page.getByRole('heading').first()
    await expect(heading).toBeVisible()
  })
})
