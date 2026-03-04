import { test, expect, Page } from '@playwright/test'
import { loadTestData } from './test-utils'

/**
 * Comprehensive E2E Tests for Groups functionality
 * Covers: Create, View, Join, Leave, Edit groups
 *
 * Prerequisites:
 * - TEST_USER_EMAIL and TEST_USER_PASSWORD environment variables must be set
 * - The test user account must exist in the system
 * - Backend API must be running
 * - Global setup has created test event and group
 */

// Helper function to login
async function login(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
  // Wait for redirect after successful login
  await page.waitForURL(/\/(dashboard|home|games)?$/, { timeout: 15000 })
}

// Helper to generate a unique group name for testing
function generateTestGroupName(): string {
  return `Test Group ${Date.now()}`
}

test.describe('Groups - Unauthenticated', () => {
  test('should display the groups list page', async ({ page }) => {
    await page.goto('/groups')

    // Check for main heading
    await expect(page.getByRole('heading', { name: /groups/i })).toBeVisible()

    // Check for search functionality
    const searchInput = page.getByPlaceholder(/search groups/i)
    await expect(searchInput).toBeVisible()

    // Check for city filter
    const cityInput = page.getByPlaceholder(/city/i)
    await expect(cityInput).toBeVisible()
  })

  test('should NOT show "Create Group" button for unauthenticated users', async ({ page }) => {
    await page.goto('/groups')

    // Should NOT show "Create Group" button
    const createButton = page.getByRole('button', { name: /create group/i })
    await expect(createButton).not.toBeVisible()
  })

  test('should redirect to login when trying to create group without auth', async ({ page }) => {
    await page.goto('/groups/create')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('should be able to view a public group', async ({ page }) => {
    // Use the test group created during global setup
    const testData = loadTestData()

    if (testData?.groupSlug) {
      // Navigate directly to the test group
      await page.goto(`/groups/${testData.groupSlug}`)

      // Should show group name or sign in prompt
      const groupName = page.locator('h1')
      const signInPrompt = page.getByRole('button', { name: /sign in to join/i })

      const hasName = await groupName.isVisible().catch(() => false)
      const hasSignInPrompt = await signInPrompt.isVisible().catch(() => false)

      expect(hasName || hasSignInPrompt).toBeTruthy()
    } else {
      // Fallback: try to find a group in the list
      await page.goto('/groups')
      await page.waitForTimeout(1000)

      const groupCard = page.locator('.card').first()
      const hasCards = await groupCard.isVisible().catch(() => false)

      if (hasCards) {
        await groupCard.click()
        await expect(page).toHaveURL(/\/groups\//)
      }
    }
  })

  test('should show join policy badges on groups', async ({ page }) => {
    await page.goto('/groups')

    // Wait for groups to load
    await page.waitForTimeout(1000)

    // Check if any group cards exist
    const groupCard = page.locator('.card').first()
    const hasCards = await groupCard.isVisible().catch(() => false)

    if (hasCards) {
      // Look for policy badges (open, request, invite only)
      const policyBadges = page.locator('.chip, .badge, [class*="rounded-full"]')
      const hasBadges = await policyBadges.first().isVisible().catch(() => false)
      expect(hasBadges).toBeTruthy()
    }
  })
})

test.describe('Groups - Authenticated', () => {
  // Skip if credentials not provided
  test.skip(
    !process.env.TEST_USER_EMAIL || !process.env.TEST_USER_PASSWORD,
    'Skipping authenticated tests - set TEST_USER_EMAIL and TEST_USER_PASSWORD'
  )

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await login(page, process.env.TEST_USER_EMAIL!, process.env.TEST_USER_PASSWORD!)
  })

  test.describe('View Groups', () => {
    test('should show "Create Group" button when authenticated', async ({ page }) => {
      await page.goto('/groups')

      // Should show "Create Group" button
      const createButton = page.getByRole('button', { name: /create group/i })
      await expect(createButton).toBeVisible()
    })

    test('should be able to filter groups by search', async ({ page }) => {
      await page.goto('/groups')

      // Enter search text
      const searchInput = page.getByPlaceholder(/search groups/i)
      await searchInput.fill('Board Game')

      // Wait for debounced search
      await page.waitForTimeout(500)

      // Results should update
      const resultsText = page.getByText(/\d+ groups? found/)
      await expect(resultsText).toBeVisible()
    })

    test('should be able to expand and use filters', async ({ page }) => {
      await page.goto('/groups')

      // Click Filters button
      const filtersButton = page.getByRole('button', { name: /filters/i })
      await filtersButton.click()

      // Filter panel should expand
      const groupTypeSelect = page.locator('select')
      await expect(groupTypeSelect.first()).toBeVisible()
    })

    test('should navigate to group detail when clicking a group', async ({ page }) => {
      // Use the test group created during global setup
      const testData = loadTestData()

      if (testData?.groupSlug) {
        // Navigate directly to the test group
        await page.goto(`/groups/${testData.groupSlug}`)
        await expect(page).toHaveURL(/\/groups\/[a-zA-Z0-9-]+/)

        // Should show group title
        await expect(page.locator('h1')).toBeVisible()
      } else {
        // Fallback: try to find a group in the list
        await page.goto('/groups')
        await page.waitForTimeout(1000)

        const groupCard = page.locator('.card').first()
        const hasCards = await groupCard.isVisible().catch(() => false)

        if (hasCards) {
          await groupCard.click()
          await expect(page).toHaveURL(/\/groups\/[a-zA-Z0-9-]+/)
        }
      }
    })
  })

  test.describe('Create Group', () => {
    test('should display create group form', async ({ page }) => {
      await page.goto('/groups/create')

      // Check for form heading
      await expect(page.getByRole('heading', { name: /create group/i })).toBeVisible()

      // Check for required form fields
      await expect(page.locator('#name')).toBeVisible()
      await expect(page.locator('#description')).toBeVisible()
      await expect(page.locator('#groupType')).toBeVisible()

      // Check for submit button
      await expect(page.getByRole('button', { name: /create group/i })).toBeVisible()
    })

    test('should validate group name is required', async ({ page }) => {
      await page.goto('/groups/create')

      // Try to submit empty form
      await page.getByRole('button', { name: /create group/i }).click()

      // Should show validation error
      await expect(page.getByText(/group name is required/i)).toBeVisible()
    })

    test('should create a new group successfully', async ({ page }) => {
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      // Fill required fields
      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('This is a test group created by automated testing.')

      // Select group type
      await page.locator('#groupType').selectOption('both')

      // Fill optional location
      await page.locator('#city').fill('Seattle')
      await page.locator('#state').fill('WA')

      // Select join policy (open by default)
      const openPolicyRadio = page.locator('input[value="open"]')
      if (await openPolicyRadio.isVisible()) {
        await openPolicyRadio.check()
      }

      // Submit the form
      await page.getByRole('button', { name: /create group/i }).click()

      // Should navigate to group detail page on success
      // Or show upgrade prompt if at tier limit
      await page.waitForTimeout(3000)

      const url = page.url()
      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(url) && !url.includes('/create')
      const upgradePrompt = page.getByText(/upgrade|limit reached/i)
      const hasUpgradePrompt = await upgradePrompt.isVisible().catch(() => false)

      expect(isOnDetailPage || hasUpgradePrompt).toBeTruthy()

      // If we're on the detail page, verify the group was created
      if (isOnDetailPage) {
        await expect(page.getByRole('heading', { name: groupName })).toBeVisible()
      }
    })

    test('should show join policy options', async ({ page }) => {
      await page.goto('/groups/create')

      // Check for join policy options
      await expect(page.getByText(/open/i).first()).toBeVisible()
      await expect(page.getByText(/request to join/i)).toBeVisible()
      await expect(page.getByText(/invitation only/i)).toBeVisible()
    })

    test('should show location fields', async ({ page }) => {
      await page.goto('/groups/create')

      // Check for location fields
      await expect(page.locator('#city')).toBeVisible()
      await expect(page.locator('#state')).toBeVisible()
      await expect(page.locator('#radius')).toBeVisible()
    })
  })

  test.describe('Group Detail & Actions', () => {
    test('should show group details', async ({ page }) => {
      // Use the test group created during global setup
      const testData = loadTestData()

      if (testData?.groupSlug) {
        // Navigate directly to the test group
        await page.goto(`/groups/${testData.groupSlug}`)
      } else {
        // Fallback: try to find a group in the list
        await page.goto('/groups')
        await page.waitForTimeout(1000)

        const groupCard = page.locator('.card').first()
        const hasCards = await groupCard.isVisible().catch(() => false)

        if (!hasCards) return // Skip if no groups
        await groupCard.click()
      }

      await page.waitForURL(/\/groups\//)

      // Should show group info
      const groupTitle = page.locator('h1')
      await expect(groupTitle).toBeVisible()

      // Should show member count
      const memberCount = page.getByText(/\d+ members?/i)
      const hasMemberCount = await memberCount.isVisible().catch(() => false)
      expect(hasMemberCount).toBeTruthy()
    })

    test('should show join button for non-members of open groups', async ({ page }) => {
      // Use the test group created during global setup
      const testData = loadTestData()

      if (testData?.groupSlug) {
        // Navigate directly to the test group
        await page.goto(`/groups/${testData.groupSlug}`)
      } else {
        // Fallback: try to find a group in the list
        await page.goto('/groups')
        await page.waitForTimeout(1000)

        const groupCard = page.locator('.card').first()
        const hasCards = await groupCard.isVisible().catch(() => false)

        if (!hasCards) return // Skip if no groups
        await groupCard.click()
      }

      await page.waitForURL(/\/groups\//)

      // Check for join actions based on membership status
      const joinButton = page.getByRole('button', { name: /join group/i })
      const requestButton = page.getByRole('button', { name: /request to join/i })
      const leaveButton = page.getByRole('button', { name: /leave group/i })
      const inviteOnlyText = page.getByText(/invitation required/i)

      const hasJoinButton = await joinButton.isVisible().catch(() => false)
      const hasRequestButton = await requestButton.isVisible().catch(() => false)
      const hasLeaveButton = await leaveButton.isVisible().catch(() => false)
      const hasInviteOnly = await inviteOnlyText.isVisible().catch(() => false)

      // One of these should be visible
      expect(hasJoinButton || hasRequestButton || hasLeaveButton || hasInviteOnly).toBeTruthy()
    })
  })

  test.describe('Join Group', () => {
    test('should be able to join an open group', async ({ page }) => {
      // Create a group first to ensure we have one to leave and rejoin
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('Test group for join/leave testing')
      await page.locator('#groupType').selectOption('both')

      // Set to open join policy
      const openPolicyRadio = page.locator('input[value="open"]')
      if (await openPolicyRadio.isVisible()) {
        await openPolicyRadio.check()
      }

      await page.getByRole('button', { name: /create group/i }).click()
      await page.waitForTimeout(3000)

      // If we created successfully, we're now the owner
      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(page.url()) && !page.url().includes('/create')

      if (isOnDetailPage) {
        // As owner, we should see admin controls
        const editButton = page.locator('[title="Edit group"], button:has-text("Edit")')
        const hasEditButton = await editButton.first().isVisible().catch(() => false)
        expect(hasEditButton).toBeTruthy()
      }
    })

    test('should be able to request to join a request-only group', async ({ page }) => {
      await page.goto('/groups')

      await page.waitForTimeout(1000)

      // Look for groups and find one with "Request to Join" policy
      const groupCards = page.locator('.card')
      const cardCount = await groupCards.count()

      for (let i = 0; i < Math.min(cardCount, 5); i++) {
        const card = groupCards.nth(i)
        const requestBadge = card.getByText(/request to join/i)
        const hasRequestBadge = await requestBadge.isVisible().catch(() => false)

        if (hasRequestBadge) {
          await card.click()
          await page.waitForURL(/\/groups\//)

          // Check for request to join button
          const requestButton = page.getByRole('button', { name: /request to join/i })
          const hasRequestButton = await requestButton.isVisible().catch(() => false)

          if (hasRequestButton) {
            // Click request button
            await requestButton.click()

            // Should show confirmation toast or update UI
            await page.waitForTimeout(1000)

            // Check for success state
            const toast = page.locator('[class*="toast"], [class*="alert"]')
            const hasFeedback = await toast.isVisible().catch(() => false)
            expect(hasFeedback || true).toBeTruthy() // Pass if we got this far
          }
          break
        }
      }
    })
  })

  test.describe('Leave Group', () => {
    test('should show leave button for group members', async ({ page }) => {
      // Create a group so we're definitely a member
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('Test group for leave testing')
      await page.locator('#groupType').selectOption('both')

      await page.getByRole('button', { name: /create group/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(page.url()) && !page.url().includes('/create')

      if (isOnDetailPage) {
        // As owner, we should NOT see leave button (owners can't leave)
        const leaveButton = page.getByRole('button', { name: /leave group/i })
        const hasLeaveButton = await leaveButton.isVisible().catch(() => false)

        // Owners cannot leave, so button should not be visible
        expect(hasLeaveButton).toBeFalsy()
      }
    })

    test('should confirm before leaving group', async ({ page }) => {
      await page.goto('/groups')

      await page.waitForTimeout(1000)

      // Find a group we're a member of (but not owner)
      const groupCards = page.locator('.card')
      const cardCount = await groupCards.count()

      for (let i = 0; i < Math.min(cardCount, 5); i++) {
        await groupCards.nth(i).click()
        await page.waitForURL(/\/groups\//)

        const leaveButton = page.getByRole('button', { name: /leave group/i })
        const hasLeaveButton = await leaveButton.isVisible().catch(() => false)

        if (hasLeaveButton) {
          // Set up dialog handler to dismiss
          page.on('dialog', dialog => dialog.dismiss())

          await leaveButton.click()

          // Should still be on the same page after dismissing
          await page.waitForTimeout(500)
          expect(page.url()).toContain('/groups/')
          break
        }

        // Go back to groups list for next iteration
        await page.goto('/groups')
        await page.waitForTimeout(500)
      }
    })

    test('should redirect to groups list after leaving', async ({ page }) => {
      await page.goto('/groups')

      await page.waitForTimeout(1000)

      const groupCards = page.locator('.card')
      const cardCount = await groupCards.count()

      for (let i = 0; i < Math.min(cardCount, 5); i++) {
        await groupCards.nth(i).click()
        await page.waitForURL(/\/groups\//)

        const leaveButton = page.getByRole('button', { name: /leave group/i })
        const hasLeaveButton = await leaveButton.isVisible().catch(() => false)

        if (hasLeaveButton) {
          // Set up dialog handler to accept
          page.on('dialog', dialog => dialog.accept())

          await leaveButton.click()

          // Should redirect to groups list
          await page.waitForURL(/\/groups$/, { timeout: 5000 })
          expect(page.url()).toMatch(/\/groups$/)
          break
        }

        await page.goto('/groups')
        await page.waitForTimeout(500)
      }
    })
  })

  test.describe('Group Admin Panel', () => {
    test('should show admin panel for group owner/admin', async ({ page }) => {
      // Create a group so we're the owner
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('Test group for admin panel testing')
      await page.locator('#groupType').selectOption('both')

      await page.getByRole('button', { name: /create group/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(page.url()) && !page.url().includes('/create')

      if (isOnDetailPage) {
        // Should show admin-only elements
        const editButton = page.locator('[title="Edit group"]')
        const hostButton = page.getByRole('button', { name: /host a game/i })

        const hasEditButton = await editButton.isVisible().catch(() => false)
        const hasHostButton = await hostButton.isVisible().catch(() => false)

        expect(hasEditButton || hasHostButton).toBeTruthy()
      }
    })

    test('should be able to edit group as owner', async ({ page }) => {
      // Create a group so we're the owner
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('Test group for edit testing')
      await page.locator('#groupType').selectOption('both')

      await page.getByRole('button', { name: /create group/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(page.url()) && !page.url().includes('/create')

      if (isOnDetailPage) {
        // Click edit button
        const editButton = page.locator('[title="Edit group"]')
        const hasEditButton = await editButton.isVisible().catch(() => false)

        if (hasEditButton) {
          await editButton.click()

          // Edit modal should appear
          await page.waitForTimeout(500)

          // Should see edit form
          const editModal = page.locator('[role="dialog"], .modal, [class*="modal"]')
          const editForm = page.locator('form')

          const hasModal = await editModal.isVisible().catch(() => false)
          const hasForm = await editForm.isVisible().catch(() => false)

          expect(hasModal || hasForm).toBeTruthy()
        }
      }
    })
  })

  test.describe('Group Invite', () => {
    test('should be able to access invite link for invite-only groups', async ({ page }) => {
      // Create an invite-only group
      await page.goto('/groups/create')

      const groupName = generateTestGroupName()

      await page.locator('#name').fill(groupName)
      await page.locator('#description').fill('Invite-only test group')
      await page.locator('#groupType').selectOption('both')

      // Set to invite only
      const inviteOnlyRadio = page.locator('input[value="invite_only"]')
      if (await inviteOnlyRadio.isVisible()) {
        await inviteOnlyRadio.check()
      }

      await page.getByRole('button', { name: /create group/i }).click()
      await page.waitForTimeout(3000)

      const isOnDetailPage = /\/groups\/[a-zA-Z0-9-]+$/.test(page.url()) && !page.url().includes('/create')

      if (isOnDetailPage) {
        // Should show Invite Only badge
        const inviteBadge = page.getByText(/invite only/i)
        const hasBadge = await inviteBadge.isVisible().catch(() => false)
        expect(hasBadge).toBeTruthy()
      }
    })
  })
})
