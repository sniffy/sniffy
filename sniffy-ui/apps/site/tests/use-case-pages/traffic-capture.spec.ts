import { expect, test } from '@playwright/test';
import path from 'node:path';

import { expectNoAccessibilityViolations } from '../accessibility';
import { prepareShellScreenshot } from '../shell-visual';

const shellScreenshotStyle = path.join(__dirname, '../screenshot-stability.css');

test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
    route.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

test('the traffic capture page is precise, linked, keyboard-accessible, and metadata-complete', async ({
  page,
  request,
}) => {
  await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
  const response = await page.goto('/use-cases/traffic-capture/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Java traffic capture and TLS inspection with Sniffy \| Sniffy/);
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
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    'content',
    /inside a Java application/,
  );
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute(
    'content',
    'https://sniffy.io/use-cases/traffic-capture/',
  );
  await expect(page.locator('meta[property="og:title"]')).toHaveAttribute(
    'content',
    'Java traffic capture and TLS inspection with Sniffy',
  );
  await expect(page.locator('meta[name="twitter:description"]')).toHaveAttribute(
    'content',
    /inside the instrumented JVM/,
  );

  await expect(page.getByText(/not from a host-wide packet sniffer/)).toBeVisible();
  await expect(page.getByText(/does not capture another process/)).toBeVisible();
  await expect(page.getByText(/UDP DatagramChannel/)).toBeVisible();
  await expect(page.getByText(/native transports/)).toBeVisible();

  const relatedDocumentation = page.getByRole('navigation', {
    name: 'Related documentation',
  });
  for (const href of [
    '/docs/network/traffic-capture/',
    '/docs/configuration/nio-monitoring/',
    '/docs/configuration/',
    '/docs/installation/',
  ]) {
    await expect(relatedDocumentation.locator(`a[href="${href}"]`)).toBeVisible();
    expect((await request.get(href)).status()).toBe(200);
  }

  const productImage = page.getByRole('img', {
    name: /Current Sniffy profiler Network Connections tab/,
  });
  await expect(productImage).toHaveAttribute('src', '/img/use-cases/traffic-capture-network.png');
  await expect(productImage).toHaveJSProperty('naturalWidth', 760);
  const codeExample = page.getByRole('region', {
    name: 'Capture raw traffic, then opt into supported TLS plaintext.',
  });
  await codeExample.getByLabel('Read the supported TLS plaintext view code example').focus();
  await expect(
    codeExample.getByLabel('Read the supported TLS plaintext view code example'),
  ).toBeFocused();

  const dimensions = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(dimensions.scrollWidth).toBe(dimensions.clientWidth);
  await expect(page.getByRole('main')).toHaveCount(1);
  await expectNoAccessibilityViolations(page);

  const motion = await page.locator('.sniffy-use-case').evaluate((element) => {
    const style = getComputedStyle(element);
    const toMilliseconds = (duration: string) =>
      Number.parseFloat(duration) * (duration.endsWith('ms') ? 1 : 1000);
    return {
      animationDuration: toMilliseconds(style.animationDuration),
      transitionDuration: toMilliseconds(style.transitionDuration),
    };
  });
  expect(motion.animationDuration).toBeLessThanOrEqual(0.01);
  expect(motion.transitionDuration).toBeLessThanOrEqual(0.01);
});

for (const colorScheme of ['dark', 'light'] as const) {
  test(`the ${colorScheme} desktop traffic capture page matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/use-cases/traffic-capture/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareShellScreenshot(page);
    await expect(page).toHaveScreenshot(`traffic-capture-desktop-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
      stylePath: shellScreenshotStyle,
    });
  });
}
