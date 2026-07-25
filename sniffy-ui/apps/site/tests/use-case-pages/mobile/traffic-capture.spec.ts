import { expect, test } from '@playwright/test';
import path from 'node:path';

import { expectNoAccessibilityViolations } from '../../accessibility';
import { prepareShellScreenshot } from '../../shell-visual';

const shellScreenshotStyle = path.join(__dirname, '../../screenshot-stability.css');

test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
    route.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

test('the mobile traffic capture page reflows without hiding its limits or documentation', async ({
  page,
}) => {
  const response = await page.goto('/use-cases/traffic-capture/');

  expect(response?.status()).toBe(200);
  expect(page.viewportSize()).toMatchObject({ width: 412, height: 839 });
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Follow network bytes from the Java call that moved them.',
    }),
  ).toBeVisible();
  await expect(page.getByText(/does not capture another process/)).toBeVisible();
  await expect(page.getByText(/NIO2\/AIO/)).toBeVisible();
  const geometry = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
    mainRight: document.querySelector('main')?.getBoundingClientRect().right,
  }));
  expect(geometry.scrollWidth).toBe(geometry.clientWidth);
  expect(geometry.mainRight).toBeLessThanOrEqual(geometry.clientWidth);
  await expect(page.getByRole('navigation', { name: 'Related documentation' })).toBeVisible();
  await expectNoAccessibilityViolations(page);
});

for (const colorScheme of ['dark', 'light'] as const) {
  test(`the ${colorScheme} mobile traffic capture page matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/use-cases/traffic-capture/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareShellScreenshot(page);
    await expect(page).toHaveScreenshot(`traffic-capture-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
      stylePath: shellScreenshotStyle,
    });
  });
}
