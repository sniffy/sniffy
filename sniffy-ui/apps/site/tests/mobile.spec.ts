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
  await expect(sidebar.getByText('Search docs')).toHaveCount(0);
  await expect(
    sidebar.getByRole('button', { name: /Switch between dark and light mode/ }),
  ).toBeVisible();

  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(dimensions.scrollWidth).toBe(dimensions.clientWidth);
});

test('the mobile configuration table scrolls to its final column without page overflow', async ({
  page,
}) => {
  await page.goto('/docs/configuration/');

  const tableRegion = page.getByRole('region', { name: 'Sniffy configuration properties' });
  await expect(tableRegion).toBeVisible();
  const geometry = await tableRegion.evaluate((region) => {
    const clientWidth = region.clientWidth;
    const scrollWidth = region.scrollWidth;
    region.scrollLeft = scrollWidth;
    const lastHeader = region.querySelector('th:last-child');
    const regionBox = region.getBoundingClientRect();
    const headerBox = lastHeader?.getBoundingClientRect();
    return {
      clientWidth,
      scrollWidth,
      scrollLeft: region.scrollLeft,
      documentClientWidth: document.documentElement.clientWidth,
      documentScrollWidth: document.documentElement.scrollWidth,
      regionRight: regionBox.right,
      headerLeft: headerBox?.left,
      headerRight: headerBox?.right,
      headerText: lastHeader?.textContent?.trim(),
    };
  });

  expect(geometry.scrollWidth).toBeGreaterThan(geometry.clientWidth);
  expect(geometry.scrollLeft).toBeGreaterThan(0);
  expect(geometry.headerText).toBe('Default Value');
  expect(geometry.headerLeft).toBeGreaterThanOrEqual(0);
  expect(geometry.headerRight).toBeLessThanOrEqual(geometry.regionRight + 1);
  expect(geometry.documentScrollWidth).toBe(geometry.documentClientWidth);
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

  test(`the ${colorScheme} mobile configuration table matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/docs/configuration/');

    const tableRegion = page.getByRole('region', { name: 'Sniffy configuration properties' });
    await tableRegion.evaluate((region) => {
      region.scrollLeft = region.scrollWidth;
    });
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`configuration-mobile-${colorScheme}.png`, {
      animations: 'disabled',
    });
  });
}
