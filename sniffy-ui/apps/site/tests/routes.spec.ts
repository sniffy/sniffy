import { expect, test } from '@playwright/test';

test('the minimal homepage renders at /', async ({ page }) => {
  const response = await page.goto('/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Website foundation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Open the documentation scaffold' })).toHaveAttribute(
    'href',
    '/docs/',
  );
});

test('the current documentation renders at /docs/', async ({ page }) => {
  const response = await page.goto('/docs/');

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle(/Sniffy documentation \| Sniffy/);
  await expect(page.getByRole('heading', { level: 1, name: 'Sniffy documentation' })).toBeVisible();
});

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
