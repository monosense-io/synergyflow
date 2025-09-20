import { test, expect } from '@playwright/test';

test('credentials login sets session cookie and accesses protected', async ({ page, context }) => {
  // Get CSRF token
  const csrfResp = await page.request.get('/api/auth/csrf');
  expect(csrfResp.ok()).toBeTruthy();
  const csrf = await csrfResp.json();
  const csrfToken = csrf.csrfToken as string;

  // Post to credentials callback to simulate login
  const form = new URLSearchParams();
  form.set('csrfToken', csrfToken);
  form.set('username', 'tester');
  form.set('password', 'irrelevant');
  form.set('callbackUrl', '/');

  const loginResp = await page.request.post('/api/auth/callback/credentials', {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: form.toString(),
  });
  expect(loginResp.ok()).toBeTruthy();

  // Navigate to protected page should be allowed now
  await page.goto('/protected');
  await expect(page.getByRole('heading', { name: 'Protected Page' })).toBeVisible();

  // Check session cookie exists
  const cookies = await context.cookies();
  const session = cookies.find(c => c.name.includes('next-auth.session'));
  expect(session).toBeTruthy();

  // Cookie security: httpOnly true always; secure only in prod
  expect(session!.httpOnly).toBe(true);
  const isProd = process.env.NODE_ENV === 'production';
  expect(session!.secure).toBe(isProd);
  expect(session!.sameSite).toBe('Lax');
});

