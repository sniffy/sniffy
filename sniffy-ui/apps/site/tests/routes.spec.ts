import { expect, test } from '@playwright/test';

test('the minimal homepage renders at /', async ({ page }) => {
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Website foundation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Open the documentation scaffold' })).toHaveAttribute(
    'href',
    '/docs/',
  );
});

test('the minimal current documentation renders at /docs/', async ({ page }) => {
  const response = await page.goto('/docs/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Sniffy documentation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy documentation' })).toBeVisible();
});

for (const route of ['/blog/', '/docs/next/', '/docs/3.1/']) {
  test(`${route} remains reserved and disabled`, async ({ page }) => {
    const response = await page.goto(route);

    expect(response?.status()).toBe(404);
  });
}
