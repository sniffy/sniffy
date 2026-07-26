import { expect, test } from '@playwright/test';
import path from 'node:path';
import { expectNoAccessibilityViolations } from '../accessibility';
import { prepareShellScreenshot } from '../shell-visual';

const stylePath = path.join(__dirname, '../screenshot-stability.css');
test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) =>
    route.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

test('the network fault page is accurate, linked, accessible, responsive, and metadata-complete', async ({
  page,
  request,
}) => {
  await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
  const response = await page.goto('/use-cases/network-fault-testing/');
  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Java network fault and resilience testing with Sniffy \| Sniffy/);
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
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    'content',
    /blocked TCP connections and fixed Sniffy latency/,
  );
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute(
    'content',
    'https://sniffy.io/use-cases/network-fault-testing/',
  );
  await expect(page.locator('meta[property="og:title"]')).toHaveAttribute(
    'content',
    'Java network fault and resilience testing with Sniffy',
  );
  await expect(page.locator('meta[name="twitter:description"]')).toHaveAttribute(
    'content',
    /not arbitrary network chaos/,
  );
  for (const text of [
    /packet loss/,
    /not a constant end-to-end response time/,
    /JVM-global/,
    /UDP DatagramChannel/,
    /values below -1/,
  ])
    await expect(page.getByText(text)).toBeVisible();

  const docs = page.getByRole('navigation', { name: 'Related documentation' });
  for (const href of [
    '/docs/network/fault-emulation/',
    '/docs/installation/',
    '/docs/configuration/',
    '/docs/testing/junit/',
  ]) {
    await expect(docs.locator(`a[href="${href}"]`)).toBeVisible();
    expect((await request.get(href)).status()).toBe(200);
  }
  const code = page.getByRole('region', {
    name: 'Make an unavailable network an explicit test condition.',
  });
  await code.getByLabel('Block monitored connections for one test code example').focus();
  await expect(
    code.getByLabel('Block monitored connections for one test code example'),
  ).toBeFocused();
  const geometry = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(geometry.scrollWidth).toBe(geometry.clientWidth);
  await expect(page.getByRole('main')).toHaveCount(1);
  await expectNoAccessibilityViolations(page);
  const motion = await page.locator('.sniffy-use-case').evaluate((element) => {
    const style = getComputedStyle(element);
    const toMilliseconds = (duration: string) =>
      Number.parseFloat(duration) * (duration.endsWith('ms') ? 1 : 1000);
    return [toMilliseconds(style.animationDuration), toMilliseconds(style.transitionDuration)];
  });
  for (const duration of motion) expect(duration).toBeLessThanOrEqual(0.01);
});

for (const colorScheme of ['dark', 'light'] as const)
  test(`${colorScheme} desktop network fault page matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/use-cases/network-fault-testing/');
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await prepareShellScreenshot(page);
    await expect(page).toHaveScreenshot(`network-fault-testing-desktop-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
      stylePath,
    });
  });
