import { expect, test } from '@playwright/test';

test('the real development server renders the network fault route', async ({ page }) => {
  const response = await page.goto('/use-cases/network-fault-testing/');
  expect(response?.status()).toBe(200);
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Exercise failure paths without pretending to be the whole network.',
    }),
  ).toBeVisible();
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://sniffy.io/use-cases/network-fault-testing/',
  );
  await expect(page.getByRole('navigation', { name: 'Related documentation' })).toBeVisible();
});
