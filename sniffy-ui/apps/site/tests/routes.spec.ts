import { expect, test } from '@playwright/test';

import { expectNoAccessibilityViolations } from './accessibility';

test('the minimal homepage renders at /', async ({ page }) => {
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Website foundation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Open the documentation scaffold' })).toHaveAttribute(
    'href',
    '/docs/',
  );
  await page.getByRole('link', { name: 'Open the documentation scaffold' }).click();
  await expect(page).toHaveURL(/\/docs\/$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy documentation' })).toBeVisible();
});

test('the branded shell exposes primary navigation and footer landmarks', async ({ page }) => {
  await page.goto('/');

  const navigation = page.getByRole('navigation', { name: 'Main' });
  await expect(navigation).toBeVisible();
  await expect(navigation.getByRole('link', { name: 'Documentation' })).toHaveAttribute(
    'href',
    '/docs/',
  );
  await expect(navigation.getByText('Use cases', { exact: true })).toBeVisible();
  await expect(navigation.getByRole('link', { name: 'GitHub' })).toHaveAttribute(
    'href',
    'https://github.com/sniffy/sniffy',
  );
  await expect(navigation.getByRole('link', { name: 'Sniffy' }).locator('img')).toHaveCount(0);
  await expect(page.getByRole('button', { name: /Search docs/ })).toBeVisible();

  const footer = page.getByRole('contentinfo');
  await expect(footer).toBeVisible();
  await expect(footer.getByText('Product', { exact: true })).toBeVisible();
  await expect(footer.getByText('Use cases', { exact: true })).toBeVisible();
  await expect(footer.getByText('Project', { exact: true })).toBeVisible();
});

test('the current documentation renders at /docs/', async ({ page }) => {
  const response = await page.goto('/docs/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Sniffy documentation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy documentation' })).toBeVisible();
});

test('the configuration table keeps every column reachable without page overflow', async ({
  page,
}) => {
  await page.goto('/docs/configuration/');

  const tableRegion = page.getByRole('region', { name: 'Sniffy configuration properties' });
  await expect(tableRegion).toBeVisible();
  await expect(tableRegion).toHaveAttribute('tabindex', '0');

  const initialGeometry = await tableRegion.evaluate((region) => ({
    clientWidth: region.clientWidth,
    scrollWidth: region.scrollWidth,
  }));
  expect(initialGeometry.scrollWidth).toBeGreaterThan(initialGeometry.clientWidth);

  const finalGeometry = await tableRegion.evaluate((region) => {
    region.scrollLeft = region.scrollWidth;
    const lastHeader = region.querySelector('th:last-child');
    const regionBox = region.getBoundingClientRect();
    const headerBox = lastHeader?.getBoundingClientRect();
    return {
      documentClientWidth: document.documentElement.clientWidth,
      documentScrollWidth: document.documentElement.scrollWidth,
      scrollLeft: region.scrollLeft,
      regionRight: regionBox.right,
      headerLeft: headerBox?.left,
      headerRight: headerBox?.right,
      headerText: lastHeader?.textContent?.trim(),
    };
  });

  expect(finalGeometry.scrollLeft).toBeGreaterThan(0);
  expect(finalGeometry.headerText).toBe('Default Value');
  expect(finalGeometry.headerLeft).toBeGreaterThanOrEqual(0);
  expect(finalGeometry.headerRight).toBeLessThanOrEqual(finalGeometry.regionRight + 1);
  expect(finalGeometry.documentScrollWidth).toBe(finalGeometry.documentClientWidth);
});

test('the site follows light preference and allows an explicit dark selection', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'light' });
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  await expect
    .poll(() =>
      page
        .locator('html')
        .evaluate((element) =>
          getComputedStyle(element).getPropertyValue('--sniffy-canvas').trim(),
        ),
    )
    .toBe('#f7f9fc');

  const toggle = page.getByRole('button', {
    name: 'Switch between dark and light mode (currently system mode)',
  });
  await toggle.click();
  const lightToggle = page.getByRole('button', {
    name: 'Switch between dark and light mode (currently light mode)',
  });
  await lightToggle.click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  await expect
    .poll(() =>
      page
        .locator('html')
        .evaluate((element) =>
          getComputedStyle(element).getPropertyValue('--sniffy-canvas').trim(),
        ),
    )
    .toBe('#090e17');
});

test('keyboard navigation exposes a visible focus indicator and activates documentation', async ({
  page,
}) => {
  await page.goto('/');

  let reachedDocumentation = false;
  for (let index = 0; index < 8; index += 1) {
    await page.keyboard.press('Tab');
    reachedDocumentation = await page.evaluate(
      () => (document.activeElement as HTMLAnchorElement | null)?.pathname === '/docs/',
    );
    if (reachedDocumentation) break;
  }

  expect(reachedDocumentation).toBe(true);
  const focused = page.locator(':focus');
  const focusStyle = await focused.evaluate((element) => {
    const style = getComputedStyle(element);
    return { style: style.outlineStyle, width: Number.parseFloat(style.outlineWidth) };
  });
  expect(focusStyle.style).not.toBe('none');
  expect(focusStyle.width).toBeGreaterThanOrEqual(2);

  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/docs\/$/);
});

test('the shell respects reduced motion and passes a representative accessibility audit', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'dark', reducedMotion: 'reduce' });
  await page.goto('/docs/installation/');

  const transitionSeconds = await page.locator('.navbar').evaluate((element) =>
    Math.max(
      ...getComputedStyle(element)
        .transitionDuration.split(',')
        .map((duration) => Number.parseFloat(duration) * (duration.includes('ms') ? 0.001 : 1)),
    ),
  );
  expect(transitionSeconds).toBeLessThanOrEqual(0.001);
  await expect(page.getByRole('main')).toHaveCount(1);
  await expect(page.getByRole('navigation', { name: 'Main' })).toHaveCount(1);
  await expect(page.getByRole('contentinfo')).toHaveCount(1);
  await expectNoAccessibilityViolations(page);
});

test('local documentation search covers representative pages and section anchors', async ({
  page,
}) => {
  await page.goto('/');
  await page.keyboard.press('Control+K');

  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  const input = dialog.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });
  await expect(dialog).toBeVisible();
  await expect(input).toBeFocused();
  await expect(dialog.getByText('Suggested searches')).toBeVisible();

  for (const [query, href] of [
    ['installation', '/docs/installation/'],
    ['configuration', '/docs/configuration/'],
    ['SQL assertions', '/docs/testing/api/'],
    ['network fault simulation', '/docs/network/fault-emulation/'],
    ['traffic capture', '/docs/network/traffic-capture/'],
    ['SSL TLS traffic decryption', '/docs/network/traffic-capture/#ssltls-traffic-decryption'],
  ] as const) {
    await input.fill(query);
    await expect(dialog.locator(`a[href="${href}"]`).first()).toBeVisible();
  }

  await input.fill('no-such-sniffy-documentation-result');
  await expect(dialog.getByText(/No documentation results/)).toBeVisible();
  await input.fill('');
  await expect(dialog.getByRole('option')).toHaveCount(0);

  await input.press('Escape');
  await expect(dialog).not.toBeVisible();
  await expect(page.getByRole('button', { name: /Search docs/ })).toBeFocused();

  await page.getByRole('button', { name: /Search docs/ }).click();
  await input.fill('traffic capture');
  const firstResult = dialog.getByRole('option').first();
  await expect(firstResult).toHaveAttribute('aria-selected', 'true');
  await input.press('ArrowDown');
  await expect(firstResult).toHaveAttribute('aria-selected', 'false');
  await input.press('ArrowUp');
  await input.press('Enter');
  await expect(page).toHaveURL(/\/docs\/network\/traffic-capture\/(?:#.*)?$/);
});

test('local documentation search fails closed for malformed index data', async ({ page }) => {
  const pageErrors: Error[] = [];
  page.on('pageerror', (error) => pageErrors.push(error));
  await page.route('**/search-index-*.json', async (route) => {
    await route.fulfill({
      body: '{"documents":[{"id":1,"sectionRoute":"/blog/not-docs/"}],"index":{}}',
      contentType: 'application/json',
      status: 200,
    });
  });
  await page.goto('/');
  await page.getByRole('button', { name: /Search docs/ }).click();

  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  await expect(dialog.getByText('Search is temporarily unavailable.')).toBeVisible();
  await expect(dialog.getByRole('link', { name: 'Browse all documentation' })).toHaveAttribute(
    'href',
    '/docs/',
  );
  expect(pageErrors).toEqual([]);
});

test('the open search dialog is keyboard-contained and accessible', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Search docs/ }).click();
  const dialog = page.getByRole('dialog', { name: 'Search Sniffy docs' });
  await dialog
    .getByRole('combobox', { name: 'Search current Sniffy documentation' })
    .fill('configuration');
  await expect(dialog.getByRole('option').first()).toBeVisible();
  await expectNoAccessibilityViolations(page);
});

test('the branded 404 offers useful recovery links and remains accessible', async ({ page }) => {
  const response = await page.goto('/missing-shell-route/');

  expect(response?.status()).toBe(404);
  await expect(
    page.getByRole('heading', { level: 1, name: 'This trail went cold.' }),
  ).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Page recovery' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Browse documentation' })).toHaveAttribute(
    'href',
    '/docs/',
  );
  await expectNoAccessibilityViolations(page);
});

for (const colorScheme of ['dark', 'light'] as const) {
  test(`the ${colorScheme} desktop homepage shell matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`home-desktop-${colorScheme}.png`, {
      animations: 'disabled',
      fullPage: true,
    });
  });

  test(`the ${colorScheme} desktop documentation chrome matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/docs/installation/');

    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`docs-desktop-${colorScheme}.png`, {
      animations: 'disabled',
    });
  });

  test(`the ${colorScheme} desktop search dialog matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/');
    await page.getByRole('button', { name: /Search docs/ }).click();
    await page
      .getByRole('combobox', { name: 'Search current Sniffy documentation' })
      .fill('traffic capture');

    await expect(page.getByRole('option').first()).toBeVisible();
    await expect(page).toHaveScreenshot(`search-desktop-${colorScheme}.png`, {
      animations: 'disabled',
    });
  });

  test(`the ${colorScheme} desktop configuration table matches its reviewed baseline`, async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme });
    await page.goto('/docs/configuration/');

    const tableRegion = page.getByRole('region', { name: 'Sniffy configuration properties' });
    await tableRegion.evaluate((region) => {
      region.scrollLeft = region.scrollWidth;
    });
    await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme);
    await expect(page).toHaveScreenshot(`configuration-desktop-${colorScheme}.png`, {
      animations: 'disabled',
    });
  });
}

for (const [route, heading] of [
  ['/docs/installation/', 'Installation'],
  ['/docs/setup/filter/', 'Servlet filter'],
  ['/docs/configuration/', 'Configuration'],
  ['/docs/testing/junit/', 'JUnit'],
  ['/docs/network/fault-emulation/', 'Network fault emulation'],
  ['/docs/network/traffic-capture/', 'Traffic capture and TLS inspection'],
  ['/docs/migration/to-4/', 'Migration to Sniffy 4.0'],
] as const) {
  test(`${route} exposes a navigable migrated section`, async ({ page }) => {
    const response = await page.goto(route);

    expect(response?.status()).toBe(200);
    await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
  });
}

test('migrated documentation images are published', async ({ request }) => {
  for (const image of ['agent-ui.png', 'demo.gif', 'network-connections.png']) {
    const response = await request.get(`/img/docs/${image}`);

    expect(response.status()).toBe(200);
    expect((await response.body()).length).toBeGreaterThan(0);
  }
});

test('a tagged Java region renders as a syntax-highlighted listing', async ({ page }) => {
  const response = await page.goto('/docs/source-snippets/');

  expect(response?.status()).toBe(200);
  const snippet = page.locator('.prism-code.language-java').filter({ hasText: 'Sniffy.spy()' });
  await expect(snippet).toBeVisible();
  await expect(snippet).toContainText('SqlQueries.atMostOneQuery()');
  await expect(snippet).not.toContainText('tag::testVerifyApi');
});

for (const route of ['/blog/', '/docs/next/', '/docs/3.1/']) {
  test(`${route} remains reserved and disabled`, async ({ page }) => {
    const response = await page.goto(route);

    expect(response?.status()).toBe(404);
  });
}
