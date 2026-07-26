import { expect, test } from '@playwright/test';
import path from 'node:path';
import { expectNoAccessibilityViolations } from '../../accessibility';
import { prepareShellScreenshot } from '../../shell-visual';

const stylePath = path.join(__dirname, '../../screenshot-stability.css');
test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
    route.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

test('the mobile network fault page reflows without hiding limits or documentation', async ({
  page,
}) => {
  const response = await page.goto('/use-cases/network-fault-testing/');
  expect(response?.status()).toBe(200);
  expect(page.viewportSize()).toMatchObject({ width: 412, height: 839 });
  await expect(page.getByText(/not a host proxy/)).toBeVisible();
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

for (const colorScheme of ['dark', 'light'] as const)
  test(`${colorScheme} mobile network fault page matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/use-cases/network-fault-testing/');
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareShellScreenshot(page);
    await expect(page).toHaveScreenshot(`network-fault-testing-mobile-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
      stylePath,
    });
  });
