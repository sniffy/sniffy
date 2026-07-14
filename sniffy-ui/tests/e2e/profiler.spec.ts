import { expect, test } from '@playwright/test';

const browserErrors = new WeakMap<object, string[]>();

test.beforeEach(async ({ page }) => {
  const errors: string[] = [];
  browserErrors.set(page, errors);
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(message.text());
  });
  page.on('pageerror', (error) => errors.push(error.message));
});

test.afterEach(async ({ page }) => {
  expect(browserErrors.get(page)).toEqual([]);
});

test('mounts one isolated profiler and supports core interactions', async ({ page }) => {
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  await expect(profiler).toHaveCount(1);
  await expect(profiler.locator('#sniffy-root')).toBeVisible();
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText('Sniffy profiler')).toBeVisible();
  await profiler.getByRole('tab', { name: 'Top SQL' }).click();
  await expect(profiler.getByRole('table', { name: 'Top SQL' })).toBeVisible();
  await profiler.getByRole('button', { name: /^Count/ }).click();
  await expect(profiler.getByRole('button', { name: /^Count/ })).toContainText('↓');
  await profiler.getByRole('button', { name: /^Count/ }).click();
  await expect(profiler.getByRole('button', { name: /^Count/ })).toContainText('↑');
  await profiler.getByRole('button', { name: 'Reset gathered queries' }).click();
  await expect(profiler.getByText('No SQL queries gathered yet.')).toBeVisible();
  await profiler.getByRole('button', { name: 'Maximize panel' }).click();
  await expect(profiler.locator('.sniffy-panel')).toHaveAttribute('data-maximized', 'true');
  await profiler.getByRole('button', { name: 'Close profiler' }).click();
  await expect(profiler.getByRole('button', { name: 'Open Sniffy profiler' })).toBeVisible();
  await profiler.getByRole('button', { name: 'Minimize Sniffy counters' }).click();
  await expect(profiler.getByRole('button', { name: 'Expand Sniffy counters' })).toBeVisible();
});

test('keeps existing XHR handlers and resolves relative detail URLs', async ({ page }) => {
  await page.goto('/mock/path/subpath/page');
  await page.getByRole('button', { name: 'Run application XHR' }).click();
  await expect(page.locator('#host-status')).toHaveAttribute('data-response', '200');
  await expect.poll(() => page.evaluate(() => (window as any).hostHandlerCalls)).toBe(1);
  const profiler = page.locator('sniffy-profiler');
  await expect(profiler.locator('[title="SQL queries"]')).toContainText('4');
  await expect(profiler.locator('[title="Server time"]')).toContainText('105 ms');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText(/GET .*ajax\.json - 200/)).toBeVisible();
});

test('resolves no-trailing-slash details and preserves the Sniffy request header', async ({
  page,
}) => {
  let guardHeader = '';
  page.on('request', (request) => {
    if (request.url().endsWith('/mock/notrailingslash'))
      guardHeader = request.headers()['sniffy-inject-html-enabled'] ?? '';
  });
  await page.goto('/mock/mock.html');
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        const xhr = new XMLHttpRequest();
        xhr.addEventListener('loadend', () => resolve());
        xhr.open('GET', '/mock/notrailingslash');
        xhr.send();
      }),
  );
  const profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText(/GET \/mock\/notrailingslash - 200/)).toBeVisible();
  expect(guardHeader).toBe('false');
});

test('supports collapsibles, copy, clear, registry mutations and keyboard tabs', async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: async (value: string) => {
          (window as typeof window & { copiedSniffyReport?: string }).copiedSniffyReport = value;
        },
      },
    });
  });
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  const open = profiler.getByRole('button', { name: 'Open Sniffy profiler' });
  await open.focus();
  await page.keyboard.press('Enter');
  await expect(profiler.getByText('Sniffy profiler')).toBeVisible();
  await profiler.getByRole('button', { name: 'Stack trace', exact: true }).click();
  await expect(profiler.getByText(/Service\.call/).first()).toBeVisible();
  await profiler.getByRole('button', { name: 'Copy report' }).click();
  await expect
    .poll(() =>
      page.evaluate(
        () => (window as typeof window & { copiedSniffyReport?: string }).copiedSniffyReport,
      ),
    )
    .toContain('executedQueries');

  await profiler.getByRole('tab', { name: 'Executed Queries' }).focus();
  await page.keyboard.press('ArrowRight');
  const networkTab = profiler.getByRole('tab', { name: 'Network Connections' });
  await expect(networkTab).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(networkTab).toHaveAttribute('aria-selected', 'true');
  await expect(profiler.getByRole('table', { name: 'Socket connections' })).toBeVisible();
  const socketSwitch = profiler.getByRole('switch', {
    name: 'Enable en.wikipedia.org:443 socket',
  });
  await socketSwitch.click();
  await expect(socketSwitch).not.toBeChecked();
  await profiler
    .getByRole('button', { name: 'Increase en.wikipedia.org:443 socket delay' })
    .click();
  const persistentSwitch = profiler.getByRole('switch', { name: 'Keep settings after restart' });
  await persistentSwitch.click();

  await profiler.getByRole('button', { name: 'Clear captured data' }).click();
  await profiler.getByRole('tab', { name: 'Executed Queries' }).click();
  await expect(profiler.getByText('No requests captured yet.')).toBeVisible();
  await profiler.getByRole('button', { name: 'Close profiler' }).click();
  await expect(profiler.locator('[title="SQL queries"]')).toContainText('0');
  await expect(profiler.locator('[title="Server time"]')).toContainText('0 ms');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();

  await profiler.getByRole('tab', { name: 'Network Connections' }).click();
  await profiler
    .getByRole('button', { name: 'Decrease en.wikipedia.org:443 socket delay' })
    .click();
  await socketSwitch.click();
  const resetPersistent = page.waitForResponse((response) =>
    response.url().endsWith('/connectionregistry/persistent/'),
  );
  await persistentSwitch.click();
  await resetPersistent;
});

test('preserves POST and DELETE registry mutation routes', async ({ request }) => {
  const encode = (value: string | number) => encodeURIComponent(encodeURIComponent(String(value)));
  const socketPath = `/mock/connectionregistry/socket/${encode('en.wikipedia.org')}/${encode(443)}`;
  const dataSourcePath = `/mock/connectionregistry/datasource/${encode('jdbc:h2:mem:/something:')}/${encode('sa')}`;
  const textBody = { headers: { 'Content-Type': 'text/plain' } };
  const original = await request
    .get('/mock/connectionregistry/')
    .then((response) => response.json());
  const socketStatus = original.sockets.find(
    (socket: { host: string; port: number }) =>
      socket.host === 'en.wikipedia.org' && socket.port === 443,
  ).status;
  const dataSourceStatus = original.dataSources.find(
    (dataSource: { url: string; userName: string }) =>
      dataSource.url === 'jdbc:h2:mem:/something:' && dataSource.userName === 'sa',
  ).status;

  try {
    expect((await request.delete(socketPath)).status()).toBe(201);
    expect((await request.post(socketPath, { ...textBody, data: '100' })).status()).toBe(201);
    expect((await request.delete(dataSourcePath)).status()).toBe(201);
    expect((await request.post(dataSourcePath, { ...textBody, data: '0' })).status()).toBe(201);
  } finally {
    await request.post(socketPath, { ...textBody, data: String(socketStatus) });
    await request.post(dataSourcePath, { ...textBody, data: String(dataSourceStatus) });
  }
});

test('supports dual-origin CORS without runtime assets', async ({ page }) => {
  const requests: string[] = [];
  page.on('request', (request) => requests.push(request.url()));
  await page.goto('/mock/cors/page');
  await expect(page.locator('sniffy-profiler')).toHaveCount(1);
  expect(
    requests.filter(
      (url) => !url.startsWith('http://127.0.0.1:3000') && !url.startsWith('http://127.0.0.1:3001'),
    ),
  ).toEqual([]);
  expect(requests.filter((url) => /\.(css|woff2?|png|svg)$/.test(url))).toEqual([]);
});

test('serves real empty, loading, error and many-row playground states', async ({ page }) => {
  await page.goto('/mock/scenarios/loading');
  let profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText('Loading request details…')).toBeVisible();

  await page.goto('/mock/scenarios/empty');
  profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText('No SQL queries.')).toBeVisible();

  await page.goto('/mock/scenarios/error');
  profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByRole('alert')).toContainText('failed with 503');
  const expectedErrors = browserErrors.get(page) ?? [];
  expect(expectedErrors.every((message) => message.includes('503'))).toBe(true);
  expectedErrors.splice(0);

  await page.goto('/mock/scenarios/many');
  profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.locator('code.hljs')).toHaveCount(36);
});

test('isolates hostile host CSS in both directions', async ({ page }) => {
  const before = await page.goto('/mock/hostile/nested/page').then(() =>
    page.evaluate(() =>
      [...document.querySelectorAll('body *:not(sniffy-profiler)')].map((element) => ({
        id: element.id,
        zIndex: getComputedStyle(element).zIndex,
      })),
    ),
  );
  const profiler = page.locator('sniffy-profiler');
  await expect(profiler.getByRole('button', { name: 'Open Sniffy profiler' })).toBeVisible();
  const hostColor = await page
    .locator('#host-status')
    .evaluate((element) => getComputedStyle(element).color);
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  const profilerColor = await profiler
    .locator('#sniffy-root')
    .evaluate((element) => getComputedStyle(element).color);
  expect(hostColor).not.toBe(profilerColor);
  await profiler.getByRole('button', { name: 'Clear captured data' }).click();
  const after = await page.evaluate(() =>
    [...document.querySelectorAll('body *:not(sniffy-profiler)')].map((element) => ({
      id: element.id,
      zIndex: getComputedStyle(element).zIndex,
    })),
  );
  expect(after).toEqual(before);
});

test('@visual profiler and agent baselines', async ({ page }) => {
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler).toHaveScreenshot('profiler.png', { animations: 'disabled' });
  await page.goto('/agent/');
  await expect(page).toHaveScreenshot('agent.png', { fullPage: true, animations: 'disabled' });
});
