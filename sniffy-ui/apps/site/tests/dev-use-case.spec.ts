import { expect, test } from '@playwright/test';

test('the real development server renders the database query testing route', async ({ page }) => {
  const response = await page.goto('/use-cases/database-query-testing/');

  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Make database behavior part of the test contract.',
    }),
  ).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://sniffy.io/use-cases/database-query-testing/',
  );
  await expect(page.getByRole('navigation', { name: 'Related documentation' })).toBeVisible();
});
