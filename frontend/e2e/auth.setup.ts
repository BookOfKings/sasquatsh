import { test as setup, expect, Page } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

// ES module compatible __dirname
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const STORAGE_STATE_DIR = path.join(__dirname, '../playwright/.auth')

/**
 * Dismiss cookie consent banner if visible
 * This gets saved in storage state so it only needs to happen once
 */
async function dismissCookieConsent(page: Page) {
  const acceptCookies = page.getByRole('button', { name: /accept all/i })
  if (await acceptCookies.isVisible({ timeout: 2000 }).catch(() => false)) {
    await acceptCookies.click()
    // Wait for banner to disappear
    await page.waitForTimeout(500)
  }
}

/**
 * Auth setup - runs once before all tests to create authenticated sessions
 * Saves storage state (cookies, localStorage) for reuse by tests
 */

// Main test user auth setup
setup('authenticate main test user', async ({ page }) => {
  const email = process.env.TEST_USER_EMAIL
  const password = process.env.TEST_USER_PASSWORD

  if (!email || !password) {
    console.log('Skipping main user auth setup - no credentials')
    return
  }

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  // Wait for successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 30000 })

  // Verify we're logged in
  await expect(page.getByRole('button', { name: /dashboard/i })).toBeVisible({ timeout: 5000 })

  // Dismiss cookie consent banner (saves in storage state)
  await dismissCookieConsent(page)

  // Save auth state
  await page.context().storageState({ path: path.join(STORAGE_STATE_DIR, 'main-user.json') })
  console.log('Main user auth state saved')
})

// Basic tier test user auth setup
setup('authenticate basic test user', async ({ page }) => {
  const email = process.env.TEST_BASIC_USER_EMAIL
  const password = process.env.TEST_BASIC_USER_PASSWORD

  if (!email || !password) {
    console.log('Skipping basic user auth setup - no credentials (set TEST_BASIC_USER_EMAIL and TEST_BASIC_USER_PASSWORD)')
    return
  }

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  // Wait for successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 30000 })

  // Verify we're logged in
  await expect(page.getByRole('button', { name: /dashboard/i })).toBeVisible({ timeout: 5000 })

  // Dismiss cookie consent banner (saves in storage state)
  await dismissCookieConsent(page)

  // Save auth state
  await page.context().storageState({ path: path.join(STORAGE_STATE_DIR, 'basic-user.json') })
  console.log('Basic user auth state saved')
})

// Pro tier test user auth setup
setup('authenticate pro test user', async ({ page }) => {
  const email = process.env.TEST_PRO_USER_EMAIL
  const password = process.env.TEST_PRO_USER_PASSWORD

  if (!email || !password) {
    console.log('Skipping pro user auth setup - no credentials (set TEST_PRO_USER_EMAIL and TEST_PRO_USER_PASSWORD)')
    return
  }

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  // Wait for successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 30000 })

  // Verify we're logged in
  await expect(page.getByRole('button', { name: /dashboard/i })).toBeVisible({ timeout: 5000 })

  // Dismiss cookie consent banner (saves in storage state)
  await dismissCookieConsent(page)

  // Save auth state
  await page.context().storageState({ path: path.join(STORAGE_STATE_DIR, 'pro-user.json') })
  console.log('Pro user auth state saved')
})
