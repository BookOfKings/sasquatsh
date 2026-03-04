import { defineConfig, devices } from '@playwright/test'
import { config } from 'dotenv'
import path from 'path'
import { fileURLToPath } from 'url'

// Load .env file for test credentials
config()

// ES module compatible __dirname
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

// Auth storage state paths
const STORAGE_STATE_DIR = path.join(__dirname, 'playwright/.auth')
const MAIN_USER_AUTH = path.join(STORAGE_STATE_DIR, 'main-user.json')
const BASIC_USER_AUTH = path.join(STORAGE_STATE_DIR, 'basic-user.json')

/**
 * Playwright E2E Test Configuration
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  // Test directory
  testDir: './e2e',

  // Global setup and teardown
  globalSetup: './e2e/global-setup.ts',
  globalTeardown: './e2e/global-teardown.ts',

  // Run tests in parallel
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,

  // Retry on CI only
  retries: process.env.CI ? 2 : 0,

  // Limit workers to avoid Firebase auth rate limits
  workers: process.env.CI ? 1 : 4,

  // Reporter to use
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],

  // Shared settings for all the projects below
  use: {
    // Base URL to use in actions like `await page.goto('/')`
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5174',

    // Collect trace when retrying the failed test
    trace: 'on-first-retry',

    // Take screenshot on failure
    screenshot: 'only-on-failure',

    // Record video on failure
    video: 'on-first-retry',
  },

  // Configure projects
  projects: [
    // === AUTH SETUP (runs first, once) ===
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },

    // === CHROMIUM PROJECTS ===
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup'],
      testIgnore: /basic-tier\.spec\.ts/,
    },
    {
      name: 'chromium-basic-tier',
      use: {
        ...devices['Desktop Chrome'],
        storageState: BASIC_USER_AUTH,
      },
      dependencies: ['setup'],
      testMatch: /basic-tier\.spec\.ts/,
    },

    // === FIREFOX PROJECTS ===
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
      dependencies: ['setup'],
      testIgnore: /basic-tier\.spec\.ts/,
    },

    // === WEBKIT PROJECTS ===
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
      dependencies: ['setup'],
      testIgnore: /basic-tier\.spec\.ts/,
    },

    // === MOBILE PROJECTS ===
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
      dependencies: ['setup'],
      testIgnore: /basic-tier\.spec\.ts/,
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 12'] },
      dependencies: ['setup'],
      testIgnore: /basic-tier\.spec\.ts/,
    },
  ],

  // Run local dev server before starting the tests
  webServer: {
    command: 'npm run dev -- --port 5174',
    url: 'http://localhost:5174',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
})
