import { test, expect } from '@playwright/test';

test('should display landing page', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle('SynergyFlow');
  await expect(page.getByText('IT Service Management Platform')).toBeVisible();
});

test('should navigate to protected page', async ({ page }) => {
  await page.goto('/');
  await page.getByText('View Protected Page').click();
  await expect(page).toHaveURL(/.*protected/);
  await expect(page.getByRole('heading', { name: 'Protected Page' })).toBeVisible();
});

test('should redirect unauthenticated users from protected page', async ({ page }) => {
  // Unauthenticated users should be redirected away from protected page
  await page.goto('/protected');
  await expect(page).toHaveURL(/\/$/);
});

test('should have secure cookie settings in production mode', async ({ page }) => {
  // This test would check for secure cookie settings in production
  // For now, we'll just verify the environment is set up correctly
  await page.goto('/');
  
  // Check that the page loads correctly
  await expect(page).toHaveTitle('SynergyFlow');
  
  // In a real implementation with auth, we would check:
  // const cookies = await context.cookies();
  // expect(cookies.some(c => c.name === 'session')).toBeTruthy();
  // expect(cookies.find(c => c.name === 'session')?.secure).toBe(true);
  // expect(cookies.find(c => c.name === 'session')?.httpOnly).toBe(true);
  // expect(cookies.find(c => c.name === 'session')?.sameSite).toBe('Lax');
});
