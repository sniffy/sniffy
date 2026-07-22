import { expect, test } from '@playwright/test';

test('the mobile homepage renders and navigates to current documentation', async ({ page }) => {
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  expect(page.viewportSize()).toMatchObject({ width: 412, height: 839 });
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy' })).toBeVisible();

  await page.getByRole('link', { name: 'Open the documentation scaffold' }).click();

  await expect(page).toHaveURL(/\/docs\/$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy documentation' })).toBeVisible();
});
