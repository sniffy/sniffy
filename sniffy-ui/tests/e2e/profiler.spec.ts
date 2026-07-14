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
  await expect(
    profiler.getByRole('button', { name: 'Keep Sniffy counters pinned' }),
  ).toHaveAttribute('aria-pressed', 'true');
});

test('keeps existing XHR handlers and resolves relative detail URLs', async ({ page }) => {
  await page.goto('/mock/path/subpath/page');
  await page.getByRole('button', { name: 'Run application XHR' }).click();
  await expect(page.locator('#host-status')).toHaveAttribute('data-response', '200');
  await expect.poll(() => page.evaluate(() => (window as any).hostHandlerCalls)).toBe(1);
  const profiler = page.locator('sniffy-profiler');
  await expect(profiler.locator('[title="SQL queries"]')).toContainText('4');
  await expect(profiler.locator('[title="Server time"]')).toContainText('168 ms');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText(/GET .*ajax\.json - 200/)).toBeVisible();
});

test('resolves no-trailing-slash details and preserves the Sniffy request header', async ({
  page,
}) => {
  let guardHeader = '';
  let exactDetailsUrl = '';
  page.on('request', (request) => {
    if (request.url().endsWith('/mock/notrailingslash'))
      guardHeader = request.headers()['sniffy-inject-html-enabled'] ?? '';
    if (request.url().includes('notrailingslash-a54b32e7')) exactDetailsUrl = request.url();
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
  expect(exactDetailsUrl).toBe(
    'http://127.0.0.1:3000/mock/notrailingslash/request/notrailingslash-a54b32e7-b94b-450b-b145-0cf62270d32a',
  );
});

test('preserves same-origin query/fragment labels and distinguishes cross-origin labels', async ({
  page,
}) => {
  await page.goto('/mock/mock.html');
  await page.evaluate(async () => {
    const request = (url: string) =>
      new Promise<void>((resolve) => {
        const xhr = new XMLHttpRequest();
        xhr.addEventListener('loadend', () => resolve());
        xhr.open('GET', url);
        xhr.send();
      });
    await request('/mock/ajax.json?view=full#results');
    await request('http://127.0.0.1:3001/mock/ajax.json?origin=secondary#details');
  });
  const profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler.getByText('GET /mock/ajax.json?view=full#results - 200')).toBeVisible();
  await expect(
    profiler.getByText('GET http://127.0.0.1:3001/mock/ajax.json?origin=secondary#details - 200'),
  ).toBeVisible();
});

test('supports pinning, delayed hover/focus reveal, keyboard use and click fallback', async ({
  page,
}) => {
  await page.goto('/mock/mock.html');
  await page.clock.install();
  const profiler = page.locator('sniffy-profiler');
  const tray = profiler.locator('.sniffy-counter-tray');
  const pin = profiler.getByRole('button', { name: 'Keep Sniffy counters pinned' });
  await expect(pin).toHaveAttribute('aria-pressed', 'true');
  await expect(tray).toHaveAttribute('data-expanded', 'true');
  await pin.click();
  await expect(pin).toHaveAttribute('aria-pressed', 'false');
  await page.locator('#host-xhr').focus();
  await page.mouse.move(0, 0);
  await page.clock.fastForward(500);
  await expect(tray).toHaveAttribute('data-expanded', 'true');
  await page.clock.fastForward(300);
  await expect(tray).toHaveAttribute('data-expanded', 'false');

  const trigger = profiler.getByRole('button', { name: 'Open Sniffy profiler' });
  await trigger.hover();
  await expect(tray).toHaveAttribute('data-expanded', 'true');
  await page.mouse.move(0, 0);
  await page.clock.fastForward(800);
  await expect(tray).toHaveAttribute('data-expanded', 'false');

  await trigger.focus();
  await expect(tray).toHaveAttribute('data-expanded', 'true');
  await page.keyboard.press('Enter');
  await expect(trigger).toHaveAttribute('aria-expanded', 'true');
  await expect(profiler.getByText('Sniffy profiler')).toBeVisible();
  await expect(tray).toHaveAttribute('data-expanded', 'true');
  expect(await tray.evaluate((element) => getComputedStyle(element).transitionDuration)).toContain(
    '0.18s',
  );
});

test('compact trigger works in a touch context and reduced motion removes transitions', async ({
  browser,
}) => {
  const context = await browser.newContext({ hasTouch: true, reducedMotion: 'reduce' });
  const page = await context.newPage();
  await page.goto('http://127.0.0.1:3000/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  const trigger = profiler.getByRole('button', { name: 'Open Sniffy profiler' });
  await trigger.tap();
  await expect(profiler.getByText('Sniffy profiler')).toBeVisible();
  expect(
    await profiler
      .locator('.sniffy-counter-tray')
      .evaluate((element) => parseFloat(getComputedStyle(element).transitionDuration)),
  ).toBeLessThanOrEqual(0.001);
  await context.close();
});

test('pulses only counters changed by an intercepted request and clears the state', async ({
  page,
}) => {
  await page.goto('/mock/mock.html');
  await page.clock.install();
  const profiler = page.locator('sniffy-profiler');
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        const xhr = new XMLHttpRequest();
        xhr.addEventListener('loadend', () => resolve());
        xhr.open('GET', '/mock/204.json');
        xhr.send();
      }),
  );
  const sql = profiler.locator('[title="SQL queries"]');
  const time = profiler.locator('[title="Server time"]');
  await expect(sql).toHaveAttribute('data-updating', 'true');
  await expect(time).toHaveAttribute('data-updating', 'true');
  await expect(profiler.locator('[title="Network bytes"]')).toHaveAttribute(
    'data-updating',
    'false',
  );
  await expect(profiler.locator('[title="Exceptions"]')).toHaveAttribute('data-updating', 'false');
  await page.clock.fastForward(800);
  await expect(sql).toHaveAttribute('data-updating', 'false');
  await expect(time).toHaveAttribute('data-updating', 'false');
});

test('supports collapsibles, copy, clear and keyboard tabs', async ({ page }) => {
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
  await expect(networkTab).toHaveAttribute('data-active', '');
  await expect
    .poll(() => networkTab.evaluate((element) => getComputedStyle(element).borderBottomColor))
    .not.toBe('rgba(0, 0, 0, 0)');
  await expect(profiler.getByRole('table', { name: 'Socket connections' })).toBeVisible();

  await profiler.getByRole('button', { name: 'Clear captured data' }).click();
  await profiler.getByRole('tab', { name: 'Executed Queries' }).click();
  await expect(profiler.getByText('No requests captured yet.')).toBeVisible();
  await profiler.getByRole('button', { name: 'Close profiler' }).click();
  await expect(profiler.locator('[title="SQL queries"]')).toContainText('0');
  await expect(profiler.locator('[title="Server time"]')).toContainText('0 ms');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
});

test('copies with the fallback when Clipboard API is unavailable', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: undefined });
    Object.defineProperty(Document.prototype, 'execCommand', {
      configurable: true,
      value(command: string) {
        const host = document.querySelector('sniffy-profiler');
        const textarea = host?.shadowRoot?.activeElement as HTMLTextAreaElement | null;
        (
          window as typeof window & { fallbackCopy?: { command: string; value: string } }
        ).fallbackCopy = { command, value: textarea?.value ?? '' };
        return true;
      },
    });
  });
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await profiler.getByRole('button', { name: 'Copy report' }).click();

  await expect(profiler.getByText('Report copied.', { exact: true })).toBeVisible();
  await expect
    .poll(() =>
      page.evaluate(
        () =>
          (window as typeof window & { fallbackCopy?: { command: string; value: string } })
            .fallbackCopy,
      ),
    )
    .toMatchObject({ command: 'copy', value: expect.stringContaining('executedQueries') });
});

test('shows a useful error when Clipboard API and fallback both reject', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: async () => Promise.reject(new Error('denied')) },
    });
    Object.defineProperty(Document.prototype, 'execCommand', {
      configurable: true,
      value: () => false,
    });
  });
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await profiler.getByRole('button', { name: 'Copy report' }).click();
  await expect(profiler.getByRole('alert')).toContainText('Copy is not supported by this browser');
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

test('registry controls render and mutate equivalently in profiler and agent', async ({
  page,
  request,
}) => {
  const encode = (value: string | number) => encodeURIComponent(encodeURIComponent(String(value)));
  const socketSuffix = `/connectionregistry/socket/${encode('en.wikipedia.org')}/${encode(443)}`;
  const resetPath = `/mock${socketSuffix}`;
  const textBody = { headers: { 'Content-Type': 'text/plain' } };
  const exercise = async (surfaceName: 'profiler' | 'agent') => {
    await request.post(resetPath, { ...textBody, data: '100' });
    await page.goto(surfaceName === 'profiler' ? '/mock/mock.html' : '/agent/');
    const surface = surfaceName === 'profiler' ? page.locator('sniffy-profiler') : page;
    if (surfaceName === 'profiler') {
      await surface.getByRole('button', { name: 'Open Sniffy profiler' }).click();
      await surface.getByRole('tab', { name: 'Network Connections' }).click();
    }
    const socketSwitch = surface.getByRole('switch', {
      name: 'Enable en.wikipedia.org:443 socket',
    });
    await expect(socketSwitch).toBeChecked();
    const visual = () =>
      socketSwitch.evaluate((element) => ({
        background: getComputedStyle(element).backgroundColor,
        thumb: getComputedStyle(element.firstElementChild as Element).transform,
      }));
    const checkedVisual = await visual();
    const mutations: string[] = [];
    const recordMutation = (captured: import('@playwright/test').Request) => {
      if (captured.url().includes(socketSuffix) && captured.method() === 'POST')
        mutations.push(captured.postData() ?? '');
    };
    page.on('request', recordMutation);

    const disableResponse = page.waitForResponse(
      (response) => response.url().includes(socketSuffix) && response.request().method() === 'POST',
    );
    await socketSwitch.click();
    await disableResponse;
    await expect(socketSwitch).not.toBeChecked();
    await expect.poll(visual).not.toEqual(checkedVisual);
    const uncheckedVisual = await visual();
    expect(uncheckedVisual.background).not.toBe(checkedVisual.background);
    expect(uncheckedVisual.thumb).not.toBe(checkedVisual.thumb);

    await surface.getByRole('button', { name: 'Refresh' }).click();
    await expect(socketSwitch).not.toBeChecked();
    const enableResponse = page.waitForResponse(
      (response) => response.url().includes(socketSuffix) && response.request().method() === 'POST',
    );
    await socketSwitch.focus();
    await page.keyboard.press('Space');
    await enableResponse;
    await expect(socketSwitch).toBeChecked();
    await expect.poll(visual).toEqual(checkedVisual);

    const increment = surface.getByRole('button', {
      name: 'Increase en.wikipedia.org:443 socket delay',
    });
    const delayResponse = page.waitForResponse(
      (response) => response.url().includes(socketSuffix) && response.request().method() === 'POST',
    );
    await increment.click({ clickCount: 3 });
    await expect(surface.getByText('Saving…').first()).toBeVisible();
    await delayResponse;
    await expect.poll(() => mutations.length).toBe(3);
    expect(mutations).toEqual(['-100', '100', '103']);

    await surface.getByRole('button', { name: 'Refresh' }).click();
    await expect(
      surface.locator('input[aria-label="en.wikipedia.org:443 socket delay"]'),
    ).toHaveValue('103');
    page.off('request', recordMutation);
  };

  try {
    await exercise('profiler');
    await exercise('agent');
  } finally {
    await request.post(resetPath, { ...textBody, data: '100' });
  }
});

test('registry refresh preserves data and reports failure without flicker', async ({ page }) => {
  await page.goto('/agent/');
  const table = page.getByRole('table', { name: 'Socket connections' });
  await expect(table).toBeVisible();
  let releaseRefresh!: () => void;
  const heldRefresh = new Promise<void>((resolve) => {
    releaseRefresh = resolve;
  });
  await page.route('**/connectionregistry/', async (route) => {
    await heldRefresh;
    await route.continue();
  });
  const refreshResponse = page.waitForResponse((response) =>
    response.url().endsWith('/connectionregistry/'),
  );
  await page.getByRole('button', { name: 'Refresh' }).click();
  await expect(table).toBeVisible();
  await expect(page.getByRole('button', { name: 'Refreshing…' })).toBeDisabled();
  releaseRefresh();
  await refreshResponse;
  await page.unroute('**/connectionregistry/');

  await page.route('**/connectionregistry/', (route) =>
    route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":"failed"}' }),
  );
  await page.getByRole('button', { name: 'Refresh' }).click();
  await expect(page.getByRole('alert')).toContainText('failed with 503');
  await expect(table).toBeVisible();
  const expectedErrors = browserErrors.get(page) ?? [];
  expect(expectedErrors.every((message) => message.includes('503'))).toBe(true);
  expectedErrors.splice(0);
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

test('keeps collapsed and open geometry independent of host root font size', async ({ page }) => {
  const measure = async (url: string) => {
    await page.goto(url);
    const profiler = page.locator('sniffy-profiler');
    const collapsed = await profiler.locator('.sniffy-widget').boundingBox();
    await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
    const panel = await profiler.locator('.sniffy-panel').boundingBox();
    expect(collapsed).not.toBeNull();
    expect(panel).not.toBeNull();
    return { collapsed: collapsed!, panel: panel! };
  };
  const normal = await measure('/mock/mock.html');
  for (const rootFontSize of [10, 16, 32, 48]) {
    const hostile = await measure(`/mock/hostile/root-${rootFontSize}/page`);
    for (const key of ['width', 'height'] as const) {
      expect(Math.abs(hostile.collapsed[key] - normal.collapsed[key])).toBeLessThanOrEqual(1);
      expect(Math.abs(hostile.panel[key] - normal.panel[key])).toBeLessThanOrEqual(1);
    }
  }
});

test('@visual final profiler and agent states', async ({ page, request }) => {
  const socketPath = '/mock/connectionregistry/socket/en.wikipedia.org/443';
  const textBody = { headers: { 'Content-Type': 'text/plain' } };
  await request.post(socketPath, { ...textBody, data: '100' });
  await page.goto('/mock/mock.html');
  const profiler = page.locator('sniffy-profiler');
  const widget = profiler.locator('.sniffy-widget');
  await expect(widget).toHaveScreenshot('pinned-widget.png', { animations: 'disabled' });
  await profiler.getByRole('button', { name: 'Keep Sniffy counters pinned' }).click();
  await page.locator('#host-xhr').focus();
  await page.mouse.move(0, 0);
  await expect(profiler.locator('.sniffy-counter-tray')).toHaveAttribute('data-expanded', 'false', {
    timeout: 1_000,
  });
  await expect(widget).toHaveScreenshot('unpinned-widget.png', { animations: 'disabled' });
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).hover();
  await page.evaluate(
    () =>
      new Promise<void>((resolve) => {
        const xhr = new XMLHttpRequest();
        xhr.addEventListener('loadend', () => resolve());
        xhr.open('GET', '/mock/204.json');
        xhr.send();
      }),
  );
  await expect(widget).toHaveScreenshot('counter-update.png', { animations: 'disabled' });
  await profiler.getByRole('button', { name: 'Open Sniffy profiler' }).click();
  await expect(profiler).toHaveScreenshot('profiler.png', { animations: 'disabled' });
  await profiler.getByRole('tab', { name: 'Network Connections' }).click();
  await expect(profiler.getByRole('table', { name: 'Socket connections' })).toBeVisible();
  let releaseRefresh!: () => void;
  const refreshPending = new Promise<void>((resolve) => {
    releaseRefresh = resolve;
  });
  await page.route('**/connectionregistry/', async (route) => {
    await refreshPending;
    await route.continue();
  });
  const refreshResponse = page.waitForResponse((response) =>
    response.url().endsWith('/connectionregistry/'),
  );
  await profiler.getByRole('button', { name: 'Refresh' }).click();
  await expect(profiler.locator('.sniffy-panel')).toHaveScreenshot('registry-refresh.png', {
    animations: 'disabled',
  });
  releaseRefresh();
  await refreshResponse;
  await page.unroute('**/connectionregistry/');
  const profilerSwitch = profiler.getByRole('switch', {
    name: 'Enable en.wikipedia.org:443 socket',
  });
  const profilerMutation = page.waitForResponse((response) =>
    response.url().includes('/connectionregistry/socket/en.wikipedia.org/443'),
  );
  await profilerSwitch.click();
  await profilerMutation;
  await expect(profiler.locator('.sniffy-panel')).toHaveScreenshot('profiler-registry.png', {
    animations: 'disabled',
  });

  await page.goto('/agent/');
  const agentSwitch = page.getByRole('switch', { name: 'Enable en.wikipedia.org:443 socket' });
  await expect(agentSwitch).not.toBeChecked();
  const agentMutation = page.waitForResponse((response) =>
    response.url().includes('/connectionregistry/socket/en.wikipedia.org/443'),
  );
  await agentSwitch.click();
  await agentMutation;
  await expect(page).toHaveScreenshot('agent.png', { fullPage: true, animations: 'disabled' });

  await page.goto('/mock/hostile/root-48/page');
  await expect(page.locator('sniffy-profiler').locator('.sniffy-widget')).toHaveScreenshot(
    'hostile-widget.png',
    { animations: 'disabled' },
  );
});
