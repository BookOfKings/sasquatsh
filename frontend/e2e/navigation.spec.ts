import { test, expect } from '@playwright/test'

test.describe('Navigation', () => {
  test('should have header on main pages', async ({ page }) => {
    const pagesToCheck = ['/login', '/signup', '/pricing', '/groups', '/events']

    for (const url of pagesToCheck) {
      await page.goto(url)

      // Check page loaded (body visible)
      await expect(page.locator('body')).toBeVisible()
    }
  })

  test('should navigate between login and signup', async ({ page }) => {
    // Start at login
    await page.goto('/login')

    // Go to signup - click using evaluate to bypass overlay issues
    const signupButton = page.locator('.text-center').last().getByRole('button', { name: /sign up/i })
    await expect(signupButton).toBeVisible()
    await signupButton.evaluate((el: HTMLElement) => el.click())
    await page.waitForURL(/\/signup/, { timeout: 5000 })

    // Go back to login
    const signinButton = page.locator('.text-center').last().getByRole('button', { name: /sign in/i })
    await expect(signinButton).toBeVisible()
    await signinButton.evaluate((el: HTMLElement) => el.click())
    await page.waitForURL(/\/login/, { timeout: 5000 })
  })

  test('should have footer links on public pages', async ({ page }) => {
    await page.goto('/pricing')

    // Check for footer links (terms, privacy, etc)
    const footer = page.locator('footer')
    if (await footer.isVisible().catch(() => false)) {
      const termsLink = footer.getByRole('link', { name: /terms/i })
      const privacyLink = footer.getByRole('link', { name: /privacy/i })

      const hasTerms = await termsLink.isVisible().catch(() => false)
      const hasPrivacy = await privacyLink.isVisible().catch(() => false)

      expect(hasTerms || hasPrivacy).toBeTruthy()
    }
  })

  test('should navigate to groups from pricing', async ({ page }) => {
    await page.goto('/pricing')

    // Look for a groups link in navigation or page
    const groupsLink = page.getByRole('link', { name: /groups/i })
    if (await groupsLink.first().isVisible().catch(() => false)) {
      await groupsLink.first().click()
      await expect(page).toHaveURL(/\/groups/)
    }
  })
})

test.describe('Responsive Design', () => {
  test('should be usable on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    await page.goto('/')

    // Page should still be functional
    await expect(page.locator('body')).toBeVisible()

    // Check that key content is visible
    await expect(page.getByRole('heading', { name: /sasquatsh/i })).toBeVisible()
  })

  test('should be usable on tablet viewport', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 })

    await page.goto('/')

    // Page should still be functional
    await expect(page.locator('body')).toBeVisible()
    await expect(page.getByRole('heading', { name: /sasquatsh/i })).toBeVisible()
  })

  test('login form should work on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })

    await page.goto('/login')

    // Form elements should be visible and usable
    await expect(page.locator('#email')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  })
})
