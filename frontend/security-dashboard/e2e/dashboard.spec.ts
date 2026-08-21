import { expect, test } from '@playwright/test'

test('dashboard shell renders', async ({ page }) => {
  await page.goto('/dashboard')
  await expect(page.getByRole('heading', { name: 'Operations dashboard' })).toBeVisible()
})
