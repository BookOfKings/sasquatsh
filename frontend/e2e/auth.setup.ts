import { test as setup, expect } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

// ES module compatible __dirname
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const STORAGE_STATE_DIR = path.join(__dirname, '../playwright/.auth')

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

  // Save auth state
  await page.context().storageState({ path: path.join(STORAGE_STATE_DIR, 'main-user.json') })
  console.log('Main user auth state saved')
})

// Basic tier test user auth setup
setup('authenticate basic test user', async ({ page }) => {
  const email = process.env.TEST_BASIC_USER_EMAIL || 'testbasicaccount@sasquatsh.com'
  const password = process.env.TEST_BASIC_USER_PASSWORD || 'password1'

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()

  // Wait for successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 30000 })

  // Verify we're logged in
  await expect(page.getByRole('button', { name: /dashboard/i })).toBeVisible({ timeout: 5000 })

  // Save auth state
  await page.context().storageState({ path: path.join(STORAGE_STATE_DIR, 'basic-user.json') })
  console.log('Basic user auth state saved')
})
