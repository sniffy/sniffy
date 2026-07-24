import { expect, test } from '@playwright/test';
import path from 'node:path';

import { expectNoAccessibilityViolations } from './accessibility';
import { prepareShellScreenshot } from './shell-visual';

const screenshotStyle = path.join(__dirname, 'screenshot-stability.css');
const route = '/use-cases/sql-profiling/';

test.beforeEach(async ({ page }) => {
  await page.route('https://api.github.com/repos/sniffy/sniffy', (request) =>
    request.fulfill({ json: { stargazers_count: 12_345 } }),
  );
});

test('SQL profiling output, links, accessibility, keyboard focus, landmarks, and reduced motion are complete', async ({
  page,
  request,
}) => {
  await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
  const response = await page.goto(route);
  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle('SQL profiling and N+1 diagnosis with Sniffy | Sniffy');
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    'content',
    /Profile JDBC queries in the browser/,
  );
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://sniffy.io/use-cases/sql-profiling/',
  );
  await expect(page.locator('meta[property="og:url"]')).toHaveAttribute(
    'content',
    'https://sniffy.io/use-cases/sql-profiling/',
  );
  await expect(page.locator('meta[property="og:title"]')).toHaveAttribute(
    'content',
    'SQL profiling and N+1 diagnosis with Sniffy',
  );
  await expect(page.locator('meta[name="twitter:title"]')).toHaveAttribute(
    'content',
    'SQL profiling and N+1 diagnosis with Sniffy',
  );
  await expect(page.locator('meta[name="twitter:image"]')).toHaveAttribute(
    'content',
    'https://sniffy.io/img/brand/sniffy-social.svg',
  );
  await expect(page.getByRole('main')).toHaveCount(1);
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible();
  await expect(page.getByRole('contentinfo')).toBeVisible();
  await expect(page.getByText(/sniffy-web supports Jakarta Servlet 5\.0 and newer/)).toBeVisible();
  await expect(
    page.getByText(/sniffy-web-javax supports Javax Servlet 3\.1 and 4\.0/),
  ).toBeVisible();
  await expect(page.getByLabel('Jakarta Servlet 5.0+: add sniffy-web code example')).toContainText(
    '<artifactId>sniffy-web</artifactId>',
  );
  await expect(
    page.getByLabel('Javax Servlet 3.1/4.0: add sniffy-web-javax code example'),
  ).toContainText('<artifactId>sniffy-web-javax</artifactId>');
  const related = page.getByRole('navigation', { name: 'Related documentation' });
  for (const href of [
    '/docs/installation/',
    '/docs/setup/filter/',
    '/docs/setup/datasource/',
    '/docs/testing/api/',
  ]) {
    await expect(related.locator(`a[href="${href}"]`)).toBeVisible();
    expect((await request.get(href)).status()).toBe(200);
  }
  const code = page.getByLabel('Protect the observed query boundary code example');
  await code.focus();
  await expect(code).toBeFocused();
  const motion = await page.locator('.sniffy-use-case').evaluate((element) => {
    const style = getComputedStyle(element);
    const milliseconds = (value: string) =>
      Number.parseFloat(value) * (value.endsWith('ms') ? 1 : 1000);
    return [milliseconds(style.animationDuration), milliseconds(style.transitionDuration)];
  });
  expect(motion.every((duration) => duration <= 0.01)).toBe(true);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth === document.documentElement.clientWidth,
    ),
  ).toBe(true);
  await expectNoAccessibilityViolations(page);
});

for (const colorScheme of ['light', 'dark'] as const) {
  for (const viewport of [
    { label: 'desktop', width: 1280, height: 720 },
    { label: 'mobile', width: 412, height: 915 },
  ]) {
    test(`the ${viewport.label} ${colorScheme} SQL profiling page matches its reviewed baseline`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      await page.emulateMedia({ colorScheme });
      await page.goto(route);
      await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
      await prepareShellScreenshot(page);
      await expect(page).toHaveScreenshot(`sql-profiling-${viewport.label}-${colorScheme}.png`, {
        animations: 'disabled',
        fullPage: true,
        stylePath: screenshotStyle,
      });
    });
  }
}
