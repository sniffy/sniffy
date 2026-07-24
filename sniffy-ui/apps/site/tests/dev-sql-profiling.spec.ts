import { expect, test } from '@playwright/test';

test('the development server renders the SQL profiling route', async ({ page }) => {
  const response = await page.goto('/use-cases/sql-profiling/');
  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', { level: 1, name: 'See the database work behind each request.' }),
  ).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://sniffy.io/use-cases/sql-profiling/',
  );
});
