import { expect, test } from '@playwright/test';

test('theme comparison story has a reviewed light and dark baseline @visual', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(
    'http://127.0.0.1:6006/iframe.html?id=design-system-primitives--themes&viewMode=story',
  );

  await expect(page.getByRole('heading', { name: 'Light theme' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Dark theme' })).toBeVisible();
  await expect(page.locator('#storybook-root')).toHaveScreenshot('theme-comparison.png', {
    animations: 'disabled',
  });
});

test('theme comparison story stacks both themes at a narrow viewport @visual', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(
    'http://127.0.0.1:6006/iframe.html?id=design-system-primitives--themes&viewMode=story',
  );

  await expect(page.getByRole('heading', { name: 'Light theme' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Dark theme' })).toBeVisible();
  await expect(page.locator('#storybook-root')).toHaveScreenshot('theme-comparison-mobile.png', {
    animations: 'disabled',
  });
});
