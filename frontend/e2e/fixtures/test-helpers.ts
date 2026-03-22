import { Page, expect } from '@playwright/test'

/**
 * Test Helper Functions and Fixtures
 * Reusable utilities for E2E tests
 */

export interface TestCredentials {
  email: string
  password: string
}

/**
 * Get test credentials from environment variables
 */
export function getTestCredentials(): TestCredentials | null {
  const email = process.env.TEST_USER_EMAIL
  const password = process.env.TEST_USER_PASSWORD

  if (!email || !password) {
    return null
  }

  return { email, password }
}

/**
 * Check if authenticated tests should run
 */
export function shouldRunAuthenticatedTests(): boolean {
  return !!process.env.TEST_USER_EMAIL && !!process.env.TEST_USER_PASSWORD
}

/**
 * Login helper function
 */
export async function login(page: Page, email: string, password: string): Promise<boolean> {
  try {
    await page.goto('/login')
    await page.locator('#email').fill(email)
    await page.locator('#password').fill(password)
    await page.getByRole('button', { name: /sign in/i }).click()

    // Wait for redirect after successful login
    await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })

    return true
  } catch (error) {
    console.error('Login failed:', error)
    return false
  }
}

/**
 * Logout helper function
 */
export async function logout(page: Page): Promise<void> {
  // Look for user menu or logout button
  const userMenu = page.locator('[data-testid="user-menu"], [aria-label="User menu"], .user-menu')
  const hasUserMenu = await userMenu.isVisible().catch(() => false)

  if (hasUserMenu) {
    await userMenu.click()
    const logoutButton = page.getByRole('button', { name: /logout|sign out/i })
    await logoutButton.click()
  } else {
    // Direct logout button
    const logoutButton = page.getByRole('button', { name: /logout|sign out/i })
    if (await logoutButton.isVisible().catch(() => false)) {
      await logoutButton.click()
    }
  }

  // Wait for redirect to login or home
  await page.waitForURL(/\/(login|home)?$/, { timeout: 5000 }).catch(() => {})
}

/**
 * Generate unique test identifiers
 */
export function generateTestId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(7)}`
}

/**
 * Generate unique event title
 */
export function generateTestEventTitle(): string {
  return `Test Event ${generateTestId()}`
}

/**
 * Generate unique group name
 */
export function generateTestGroupName(): string {
  return `Test Group ${generateTestId()}`
}

/**
 * Get tomorrow's date in YYYY-MM-DD format
 */
export function getTomorrowDate(): string {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0] || ''
}

/**
 * Get a date N days from now in YYYY-MM-DD format
 */
export function getFutureDate(daysFromNow: number): string {
  const future = new Date()
  future.setDate(future.getDate() + daysFromNow)
  return future.toISOString().split('T')[0] || ''
}

/**
 * Dismiss cookie consent banner if visible
 * Note: This is also called during auth setup, so storage state should have it dismissed
 * Use this for unauthenticated tests or when storage state is not loaded
 */
export async function dismissCookieConsent(page: Page): Promise<void> {
  const acceptCookies = page.getByRole('button', { name: /accept all/i })
  if (await acceptCookies.isVisible({ timeout: 2000 }).catch(() => false)) {
    await acceptCookies.click()
    await page.waitForTimeout(500)
  }
}

/**
 * Wait for network idle
 */
export async function waitForNetworkIdle(page: Page, timeout = 5000): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => {})
}

/**
 * Check if element is visible with graceful handling
 */
export async function isVisible(page: Page, selector: string): Promise<boolean> {
  return page.locator(selector).isVisible().catch(() => false)
}

/**
 * Wait for toast/notification to appear
 */
export async function waitForToast(page: Page, textPattern?: RegExp): Promise<boolean> {
  const toastSelectors = [
    '[role="alert"]',
    '.toast',
    '[class*="toast"]',
    '[class*="notification"]',
    '.alert',
  ]

  for (const selector of toastSelectors) {
    const toast = page.locator(selector)
    const isVisible = await toast.isVisible().catch(() => false)

    if (isVisible) {
      if (textPattern) {
        const text = await toast.textContent()
        if (text && textPattern.test(text)) {
          return true
        }
      } else {
        return true
      }
    }
  }

  return false
}

/**
 * Create a test event and return to detail page
 */
export async function createTestEvent(
  page: Page,
  options: {
    title?: string
    date?: string
    time?: string
    duration?: number
    maxPlayers?: number
    description?: string
  } = {}
): Promise<string | null> {
  const {
    title = generateTestEventTitle(),
    date = getTomorrowDate(),
    time = '19:00',
    duration = 120,
    maxPlayers = 4,
    description = 'Test event created by automated testing',
  } = options

  await page.goto('/games/create')

  await page.locator('#title').fill(title)
  await page.locator('#eventDate').fill(date)
  await page.locator('#startTime').fill(time)
  await page.locator('#durationMinutes').fill(String(duration))
  await page.locator('#maxPlayers').fill(String(maxPlayers))
  await page.locator('#description').fill(description)

  await page.getByRole('button', { name: /host game/i }).click()

  // Wait for navigation
  await page.waitForTimeout(3000)

  const url = page.url()
  const match = url.match(/\/games\/([a-zA-Z0-9-]+)$/)

  if (match && match[1]) {
    return match[1]
  }

  return null
}

/**
 * Create a test group and return to detail page
 */
export async function createTestGroup(
  page: Page,
  options: {
    name?: string
    description?: string
    groupType?: 'geographic' | 'interest' | 'both'
    joinPolicy?: 'open' | 'request' | 'invite_only'
    city?: string
    state?: string
  } = {}
): Promise<string | null> {
  const {
    name = generateTestGroupName(),
    description = 'Test group created by automated testing',
    groupType = 'both',
    joinPolicy = 'open',
    city = 'Test City',
    state = 'TC',
  } = options

  await page.goto('/groups/create')

  await page.locator('#name').fill(name)
  await page.locator('#description').fill(description)
  await page.locator('#groupType').selectOption(groupType)
  await page.locator('#city').fill(city)
  await page.locator('#state').fill(state)

  // Select join policy
  const policyRadio = page.locator(`input[value="${joinPolicy}"]`)
  if (await policyRadio.isVisible().catch(() => false)) {
    await policyRadio.check()
  }

  await page.getByRole('button', { name: /create group/i }).click()

  // Wait for navigation
  await page.waitForTimeout(3000)

  const url = page.url()
  const match = url.match(/\/groups\/([a-zA-Z0-9-]+)$/)

  if (match && match[1] && !url.includes('/create')) {
    return match[1]
  }

  return null
}

/**
 * Delete a test event (requires being on detail page)
 */
export async function deleteTestEvent(page: Page): Promise<boolean> {
  const deleteButton = page.getByRole('button', { name: /delete/i })
  const hasDeleteButton = await deleteButton.isVisible().catch(() => false)

  if (!hasDeleteButton) {
    return false
  }

  // Set up dialog handler to accept
  page.on('dialog', dialog => dialog.accept())

  await deleteButton.click()

  // Wait for redirect
  await page.waitForURL(/\/games$/, { timeout: 5000 }).catch(() => {})

  return page.url().includes('/games') && !page.url().includes('/games/')
}

/**
 * Clean up test data (delete events/groups created during tests)
 * This should be called in afterEach or afterAll hooks
 */
export async function cleanupTestData(
  page: Page,
  testIds: { events?: string[]; groups?: string[] }
): Promise<void> {
  const { events = [], groups = [] } = testIds

  // Delete test events
  for (const eventId of events) {
    try {
      await page.goto(`/games/${eventId}`)
      await deleteTestEvent(page)
    } catch (error) {
      console.log(`Failed to delete event ${eventId}:`, error)
    }
  }

  // Note: Groups typically can't be deleted by owners, only admins
  // This is a placeholder for potential cleanup
  for (const groupSlug of groups) {
    console.log(`Note: Group ${groupSlug} may need manual cleanup`)
  }
}
