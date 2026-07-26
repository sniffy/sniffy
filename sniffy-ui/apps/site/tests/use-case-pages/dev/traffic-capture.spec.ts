import { expect, test } from '@playwright/test';

test('the real development server renders the traffic capture route', async ({ page }) => {
  const response = await page.goto('/use-cases/traffic-capture/');

  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Follow network bytes from the Java call that moved them.',
    }),
  ).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://sniffy.io/use-cases/traffic-capture/',
  );
  await expect(page.getByRole('navigation', { name: 'Related documentation' })).toBeVisible();
});
