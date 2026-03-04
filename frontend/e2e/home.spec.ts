import { test, expect } from '@playwright/test'

test.describe('Home Page', () => {
  test('should display the landing page', async ({ page }) => {
    await page.goto('/')

    // Check for Sasquatsh branding
    await expect(page.getByRole('heading', { name: /sasquatsh/i })).toBeVisible()
  })

  test('should show stats cards', async ({ page }) => {
    await page.goto('/')

    // Check for stats sections
    await expect(page.getByText(/today/i)).toBeVisible()
    await expect(page.getByText(/all time/i)).toBeVisible()
  })

  test('should have sign in button in header for guests', async ({ page }) => {
    await page.goto('/')

    // Check for Sign In button in header navigation
    const signInButton = page.locator('header').getByRole('button', { name: /sign in/i })
    await expect(signInButton).toBeVisible()
  })

  test('should navigate to login page from header', async ({ page }) => {
    await page.goto('/')

    // Click Sign In button in header
    await page.locator('header').getByRole('button', { name: /sign in/i }).click()

    // Should be on login page
    await expect(page).toHaveURL(/\/login/)
  })

  test('should navigate to signup page', async ({ page }) => {
    await page.goto('/')

    // Click Get Started button
    await page.getByRole('button', { name: /get started/i }).click()

    // Should be on signup page
    await expect(page).toHaveURL(/\/signup/)
  })

  test('should have browse public games link', async ({ page }) => {
    await page.goto('/')

    // Check for browse link
    const browseButton = page.getByRole('button', { name: /browse public games/i })
    await expect(browseButton).toBeVisible()
  })
})
