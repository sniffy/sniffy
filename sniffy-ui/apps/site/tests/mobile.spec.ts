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

test('the mobile menu exposes the shell navigation without horizontal overflow', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'Toggle navigation bar' }).click();

  const sidebar = page.locator('.navbar-sidebar');
  await expect(sidebar).toBeVisible();
  await expect(sidebar.getByRole('link', { name: 'Documentation' })).toBeVisible();
  await expect(sidebar.getByText('Use cases', { exact: true })).toBeVisible();
  await expect(sidebar.getByRole('link', { name: 'GitHub' })).toBeVisible();
  await expect(sidebar.getByText('Search docs')).toBeVisible();
  await expect(
    sidebar.getByRole('button', { name: /Switch between dark and light mode/ }),
  ).toBeVisible();

  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(dimensions.scrollWidth).toBe(dimensions.clientWidth);
});

for (const colorScheme of ['dark', 'light'] as const) {
  test(`the ${colorScheme} mobile shell matches its reviewed baseline`, async ({ page }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`home-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
    });
  });
}
