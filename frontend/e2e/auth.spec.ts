import { test, expect } from '@playwright/test'
import { dismissCookieConsent } from './fixtures/test-helpers'

test.describe('Authentication', () => {
  // Dismiss cookie banner before each test (not using auth storage state)
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await dismissCookieConsent(page)
  })

  test.describe('Login Page', () => {
    test('should display login form', async ({ page }) => {
      await page.goto('/login')

      // Check for Sasquatsh branding
      await expect(page.getByRole('heading', { name: /sasquatsh/i })).toBeVisible()

      // Check for email input (using label for= and id=)
      const emailInput = page.locator('#email')
      await expect(emailInput).toBeVisible()

      // Check for password input
      const passwordInput = page.locator('#password')
      await expect(passwordInput).toBeVisible()

      // Check for submit button
      const submitButton = page.getByRole('button', { name: /sign in/i })
      await expect(submitButton).toBeVisible()
    })

    test('should show validation errors for empty form', async ({ page }) => {
      await page.goto('/login')

      // Click submit without filling form
      await page.getByRole('button', { name: /sign in/i }).click()

      // Should show validation error
      const errorMessage = page.getByText(/please fill in all fields/i)
      await expect(errorMessage).toBeVisible()
    })

    test('should have link to signup page', async ({ page }) => {
      await page.goto('/login')

      // Find and click signup button
      const signupButton = page.getByRole('button', { name: /sign up/i })
      await expect(signupButton).toBeVisible()
      await signupButton.click()

      await expect(page).toHaveURL(/\/signup/)
    })

    test('should have Google login option', async ({ page }) => {
      await page.goto('/login')

      // Check for Google login button
      const googleButton = page.getByRole('button', { name: /continue with google/i })
      await expect(googleButton).toBeVisible()
    })

    test('should show error for invalid credentials', async ({ page }) => {
      await page.goto('/login')

      // Fill in invalid credentials
      await page.locator('#email').fill('invalid@test.com')
      await page.locator('#password').fill('wrongpassword')

      // Submit
      await page.getByRole('button', { name: /sign in/i }).click()

      // Should show error (wait for API response)
      await page.waitForTimeout(2000)
      const errorAlert = page.locator('.alert-error')
      await expect(errorAlert).toBeVisible()
    })
  })

  test.describe('Signup Page', () => {
    test('should display signup form', async ({ page }) => {
      await page.goto('/signup')

      // Check for username input (signup has username field)
      const usernameInput = page.locator('#username')
      await expect(usernameInput).toBeVisible()

      // Check for email input
      const emailInput = page.locator('#email')
      await expect(emailInput).toBeVisible()

      // Check for password input
      const passwordInput = page.locator('#password')
      await expect(passwordInput).toBeVisible()

      // Check for Create Account submit button (exact match)
      const submitButton = page.getByRole('button', { name: 'Create Account' })
      await expect(submitButton).toBeVisible()
    })

    test('should have link to login page', async ({ page }) => {
      await page.goto('/signup')

      // The "Sign in" button at the bottom of the signup form
      const loginButton = page.locator('.text-center').last().getByRole('button', { name: /sign in/i })
      await expect(loginButton).toBeVisible()

      // Click using JavaScript to bypass overlay issues
      await loginButton.evaluate((el: HTMLElement) => el.click())

      // Wait for navigation
      await page.waitForURL(/\/login/, { timeout: 5000 })
    })

    test('should show validation errors for empty form', async ({ page }) => {
      await page.goto('/signup')

      // The Create Account button should be disabled when the form is empty
      const submitButton = page.getByRole('button', { name: 'Create Account' })
      await expect(submitButton).toBeDisabled()

      // Fill in just username to partially complete form
      await page.locator('#username').fill('testuser')

      // Button should still be disabled (missing email and password)
      await expect(submitButton).toBeDisabled()
    })
  })
})
