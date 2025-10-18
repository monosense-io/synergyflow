import { test, expect } from '@playwright/test'

test.describe('Time Tray E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Setup mock API responses
    await page.route('/api/v1/time-entries', async (route) => {
      const request = route.request()
      if (request.method() === 'POST') {
        // Mock successful time entry creation
        await route.fulfill({
          status: 202,
          contentType: 'application/json',
          body: JSON.stringify({
            trackingId: 'tracking-123',
            message: 'Time entry created and mirroring initiated',
            timeEntryIds: ['entry-123']
          })
        })
      }
    })

    // Navigate to the application
    await page.goto('/')
  })

  test('should open Time Tray and create time entry', async ({ page }) => {
    // Open Time Tray
    await page.click('[data-testid="time-tray-toggle"]')

    // Verify Time Tray is open
    await expect(page.locator('[data-testid="time-tray-panel"]')).toBeVisible()
    await expect(page.locator('h2:has-text("Time Tray")')).toBeVisible()

    // Fill out the form
    await page.fill('[data-testid="duration-input"]', '30')
    await page.fill('[data-testid="description-input"]', 'Test work on incident resolution')

    // Submit the form
    await page.click('[data-testid="create-time-entry-button"]')

    // Verify optimistic UI update
    await expect(page.locator('[data-testid="time-entry-status"]')).toHaveText('OPTIMISTIC')

    // Wait for API response and status update
    await page.waitForTimeout(1000) // Simulate network delay
    await expect(page.locator('[data-testid="time-entry-status"]')).toHaveText('CONFIRMED')

    // Verify Time Tray closes after successful submission
    await expect(page.locator('[data-testid="time-tray-panel"]')).not.toBeVisible()
  })

  test('should validate form inputs and show errors', async ({ page }) => {
    // Open Time Tray
    await page.click('[data-testid="time-tray-toggle"]')

    // Try to submit empty form
    await page.click('[data-testid="create-time-entry-button"]')

    // Verify validation errors
    await expect(page.locator('[data-testid="duration-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="description-error"]')).toBeVisible()

    // Fill invalid duration
    await page.fill('[data-testid="duration-input"]', '-5')
    await page.click('[data-testid="create-time-entry-button"]')

    // Verify duration error
    await expect(page.locator('[data-testid="duration-error"]')).toHaveText(
      'Duration must be at least 1 minute'
    )
  })

  test('should handle API errors gracefully', async ({ page }) => {
    // Mock API error
    await page.route('/api/v1/time-entries', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/problem+json',
        body: JSON.stringify({
          title: 'Validation Error',
          detail: 'Invalid request data',
          status: 400,
          fields: [
            {
              name: 'targetEntities',
              message: 'At least one target entity is required',
              code: 'REQUIRED'
            }
          ]
        })
      })
    })

    // Open Time Tray and fill form
    await page.click('[data-testid="time-tray-toggle"]')
    await page.fill('[data-testid="duration-input"]', '30')
    await page.fill('[data-testid="description-input"]', 'Test work')

    // Submit form
    await page.click('[data-testid="create-time-entry-button"]')

    // Verify error handling
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
    await expect(page.locator('[data-testid="error-message"]')).toHaveText(
      'Failed to create time entry. Please try again.'
    )

    // Verify optimistic entry is removed
    await expect(page.locator('[data-testid="time-entry-status"]')).not.toBeVisible()
  })

  test('should show freshness badges during mirroring', async ({ page }) => {
    // Mock slow API response to simulate mirroring
    await page.route('/api/v1/time-entries', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 2000)) // 2 second delay
      await route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({
          trackingId: 'tracking-123',
          message: 'Time entry created and mirroring initiated',
          timeEntryIds: ['entry-123']
        })
      })
    })

    // Open Time Tray and create entry
    await page.click('[data-testid="time-tray-toggle"]')
    await page.fill('[data-testid="duration-input"]', '45')
    await page.fill('[data-testid="description-input"]', 'Work on incident #123')
    await page.click('[data-testid="create-time-entry-button"]')

    // Verify initial freshness status
    await expect(page.locator('[data-testid="freshness-badge-incident-123"]')).toBeVisible()
    await expect(page.locator('[data-testid="freshness-badge-incident-123"]')).toHaveText('PENDING')

    // Simulate mirroring completion (this would come from WebSocket or polling in real app)
    await page.waitForTimeout(1000)
    await page.evaluate(() => {
      // Simulate receiving mirroring completion update
      window.dispatchEvent(new CustomEvent('mirroring-complete', {
        detail: { entityId: 'incident-123', status: 'COMPLETED' }
      }))
    })

    // Verify freshness badge updates
    await expect(page.locator('[data-testid="freshness-badge-incident-123"]')).toHaveText('COMPLETED')
  })

  test('should support bulk time entry creation', async ({ page }) => {
    // Mock bulk API endpoint
    await page.route('/api/v1/time-entries/bulk', async (route) => {
      await route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Bulk time entries created and mirroring initiated',
          trackingIds: ['tracking-1', 'tracking-2']
        })
      })
    })

    // Open bulk creation mode
    await page.click('[data-testid="time-tray-toggle"]')
    await page.click('[data-testid="bulk-mode-toggle"]')

    // Add multiple time entries
    await page.click('[data-testid="add-time-entry-button"]')
    await page.fill('[data-testid="duration-input-0"]', '30')
    await page.fill('[data-testid="description-input-0"]', 'First task')

    await page.click('[data-testid="add-time-entry-button"]')
    await page.fill('[data-testid="duration-input-1"]', '60')
    await page.fill('[data-testid="description-input-1"]', 'Second task')

    // Submit bulk entries
    await page.click('[data-testid="create-bulk-entries-button"]')

    // Verify multiple tracking IDs
    await expect(page.locator('[data-testid="bulk-success-message"]')).toBeVisible()
    await expect(page.locator('[data-testid="tracking-ids"]')).toContainText('tracking-1')
    await expect(page.locator('[data-testid="tracking-ids"]')).toContainText('tracking-2')
  })

  test('should close Time Tray when clicking outside or escape key', async ({ page }) => {
    // Open Time Tray
    await page.click('[data-testid="time-tray-toggle"]')
    await expect(page.locator('[data-testid="time-tray-panel"]')).toBeVisible()

    // Click outside to close
    await page.click('[data-testid="time-tray-backdrop"]')
    await expect(page.locator('[data-testid="time-tray-panel"]')).not.toBeVisible()

    // Reopen and test escape key
    await page.click('[data-testid="time-tray-toggle"]')
    await expect(page.locator('[data-testid="time-tray-panel"]')).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(page.locator('[data-testid="time-tray-panel"]')).not.toBeVisible()
  })
})