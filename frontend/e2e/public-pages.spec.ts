import { test, expect } from '@playwright/test'

test.describe('Public Pages', () => {
  test.describe('Pricing Page', () => {
    test('should display pricing plans', async ({ page }) => {
      await page.goto('/pricing')

      // Check for Free tier
      await expect(page.getByText(/free/i).first()).toBeVisible()

      // Check for Basic tier
      await expect(page.getByText(/basic/i).first()).toBeVisible()

      // Check for Pro tier
      await expect(page.getByText(/pro/i).first()).toBeVisible()
    })

    test('should have action buttons for each plan', async ({ page }) => {
      await page.goto('/pricing')

      // Check for at least one CTA button (Get Started, Sign Up, Upgrade, etc.)
      const ctaButtons = page.getByRole('button', { name: /get started|sign up|upgrade|current plan/i })
      await expect(ctaButtons.first()).toBeVisible()
    })

    test('should display pricing amounts', async ({ page }) => {
      await page.goto('/pricing')

      // Check for price display ($0, $7.99, $14.99)
      await expect(page.getByText(/\$/).first()).toBeVisible()
    })

    test('should have feature lists', async ({ page }) => {
      await page.goto('/pricing')

      // Check for features (games, groups)
      await expect(page.getByText(/game/i).first()).toBeVisible()
      await expect(page.getByText(/group/i).first()).toBeVisible()
    })
  })

  test.describe('Terms of Service', () => {
    test('should display terms page', async ({ page }) => {
      await page.goto('/terms')

      // Check for terms content - h1 says "Terms of Service"
      await expect(page.getByRole('heading', { level: 1, name: /terms of service/i })).toBeVisible()
    })
  })

  test.describe('Privacy Policy', () => {
    test('should display privacy page', async ({ page }) => {
      await page.goto('/privacy')

      // Check for privacy content - h1 says "Privacy Policy"
      await expect(page.getByRole('heading', { level: 1, name: /privacy policy/i })).toBeVisible()
    })
  })

  test.describe('Cookie Policy', () => {
    test('should display cookies page', async ({ page }) => {
      await page.goto('/cookies')

      // Check for cookies content - h1 says "Cookie Policy"
      await expect(page.getByRole('heading', { level: 1, name: /cookie policy/i })).toBeVisible()
    })
  })

  test.describe('Contact Page', () => {
    test('should display contact page', async ({ page }) => {
      await page.goto('/contact')

      // Check for contact heading or content
      await expect(page.getByRole('heading', { name: /contact/i })).toBeVisible()
    })

    test('should have contact form elements', async ({ page }) => {
      await page.goto('/contact')

      // Check for form elements - name, email, message, submit
      const submitButton = page.getByRole('button', { name: /send|submit/i })
      await expect(submitButton).toBeVisible()
    })
  })
})
