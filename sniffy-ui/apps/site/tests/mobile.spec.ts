import { expect, test } from '@playwright/test';
import path from 'node:path';

import { githubScreenshotStyle, prepareGitHubStarsScreenshot } from './github-stars-visual';

const shellScreenshotStyle = path.join(__dirname, 'screenshot-stability.css');

test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
    route.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

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
  await expect(
    sidebar.getByRole('link', { name: 'Sniffy GitHub repository, 12,345 stars' }),
  ).toBeVisible();
  await expect(page.getByRole('button', { name: /Search docs/ })).toBeVisible();
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

test('the mobile search dialog coexists with navigation without clipping or page overflow', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Search docs/ }).click();

  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  const input = dialog.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });
  await expect(dialog).toBeVisible();
  await input.fill('configuration');
  await expect(dialog.getByRole('option').first()).toBeVisible();

  const geometry = await page.evaluate(() => {
    const dialog = document.querySelector('dialog');
    return {
      clientWidth: document.documentElement.clientWidth,
      dialogLeft: dialog?.getBoundingClientRect().left,
      dialogRight: dialog?.getBoundingClientRect().right,
      isModal: dialog?.matches(':modal'),
      scrollWidth: document.documentElement.scrollWidth,
    };
  });
  expect(geometry.isModal).toBe(true);
  expect(geometry.dialogLeft).toBeGreaterThanOrEqual(0);
  expect(geometry.dialogRight).toBeLessThanOrEqual(geometry.clientWidth);
  expect(geometry.scrollWidth).toBe(geometry.clientWidth);

  await input.press('Escape');
  await expect(page.getByRole('button', { name: /Search docs/ })).toBeFocused();
  await page.getByRole('button', { name: 'Toggle navigation bar' }).click();
  await expect(page.locator('.navbar-sidebar')).toBeVisible();
});

for (const colorScheme of ['dark', 'light'] as const) {
  test(`the ${colorScheme} mobile shell matches its reviewed baseline`, async ({ page }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`home-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
      stylePath: shellScreenshotStyle,
    });
  });

  test(`the ${colorScheme} mobile GitHub control matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');
    await page.getByRole('button', { name: 'Toggle navigation bar' }).click();
    const githubLink = page.locator('.navbar-sidebar').getByRole('link', {
      name: 'Sniffy GitHub repository, 12,345 stars',
    });
    await expect(githubLink).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareGitHubStarsScreenshot(page);
    await expect(githubLink).toHaveScreenshot(`github-stars-populated-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      stylePath: githubScreenshotStyle,
    });
  });

  test(`the ${colorScheme} mobile GitHub fallback matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.unroute('https://api.github.com/repos/sniffy/sniffy');
    await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
      route.abort('failed'),
    );
    await page.emulateMedia({ colorScheme });
    await page.goto('/');
    await page.getByRole('button', { name: 'Toggle navigation bar' }).click();
    const githubLink = page
      .locator('.navbar-sidebar')
      .getByRole('link', { name: 'Sniffy GitHub repository' });
    await expect(githubLink).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareGitHubStarsScreenshot(page);
    await expect(githubLink).toHaveScreenshot(`github-stars-fallback-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      stylePath: githubScreenshotStyle,
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
      stylePath: shellScreenshotStyle,
    });
  });

  test(`the ${colorScheme} mobile search dialog matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');
    await page.getByRole('button', { name: /Search docs/ }).click();
    await page
      .getByRole('combobox', { name: 'Search current Sniffy documentation' })
      .fill('traffic capture');

    await expect(page.getByRole('option').first()).toBeVisible();
    await page
      .getByRole('combobox', { name: 'Search current Sniffy documentation' })
      .evaluate((input) => {
        input.style.caretColor = 'transparent';
      });
    await expect(page).toHaveScreenshot(`search-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      stylePath: shellScreenshotStyle,
    });
  });
}
