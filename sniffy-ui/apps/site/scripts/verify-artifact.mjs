import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import process from 'node:process';
import { clearTimeout, setTimeout } from 'node:timers';
import { setTimeout as delay } from 'node:timers/promises';
import { URL } from 'node:url';
import { chromium } from '@playwright/test';

const build = resolve(import.meta.dirname, '../build');
const baseUrl = process.env.SNIFFY_SITE_BASE_URL?.trim() || '/';
if (!baseUrl.startsWith('/') || !baseUrl.endsWith('/') || baseUrl.includes('//')) {
  throw new Error(`SNIFFY_SITE_BASE_URL must have one leading and trailing slash: ${baseUrl}`);
}
const packagedPreview = resolve(build, 'preview.mjs');
const previewScript = existsSync(packagedPreview)
  ? packagedPreview
  : resolve(import.meta.dirname, 'preview.mjs');
const preview = spawn(
  process.execPath,
  [previewScript, '--root', build, '--base-url', baseUrl, '--host', '127.0.0.1', '--port', '0'],
  { stdio: ['ignore', 'pipe', 'pipe'] },
);

let stderr = '';
preview.stderr.setEncoding('utf8');
preview.stderr.on('data', (chunk) => {
  stderr += chunk;
});

const previewUrl = await new Promise((resolveUrl, reject) => {
  const timeout = setTimeout(() => reject(new Error(`Preview did not start. ${stderr}`)), 10_000);
  let stdout = '';

  preview.stdout.setEncoding('utf8');
  preview.stdout.on('data', (chunk) => {
    stdout += chunk;
    const match = stdout.match(/Sniffy website preview: (http:\/\/127\.0\.0\.1:\d+\/)/);
    if (match) {
      clearTimeout(timeout);
      resolveUrl(match[1]);
    }
  });
  preview.once('exit', (code) => {
    clearTimeout(timeout);
    reject(new Error(`Preview exited before startup with code ${code}. ${stderr}`));
  });
});

const browser = await chromium.launch();

try {
  const previewSiteUrl = new URL(baseUrl, previewUrl).toString();
  const previewRoute = (route) => new URL(route, previewSiteUrl).toString();
  const canonicalRoute = (route) => new URL(route, 'https://sniffy.io/').toString();
  const page = await browser.newPage();
  const pageErrors = [];
  const consoleErrors = [];
  let blockedGitHubRequests = 0;
  page.on('pageerror', (error) => pageErrors.push(error));
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  await page.route('https://api.github.com/repos/sniffy/sniffy', (route) => {
    blockedGitHubRequests += 1;
    // Never let the packaged-preview check reach the live API. An unusable response
    // exercises the same graceful fallback without Chromium logging a network error.
    return route.fulfill({ json: {} });
  });

  await page.goto(previewSiteUrl, { waitUntil: 'networkidle' });
  const githubLink = page.getByRole('link', { name: 'Sniffy GitHub repository' }).first();
  await githubLink.waitFor();
  if ((await githubLink.getAttribute('href')) !== 'https://github.com/sniffy/sniffy') {
    throw new Error('Artifact preview fallback does not link to the Sniffy GitHub repository.');
  }
  const countSlot = githubLink.getByTestId('github-star-count');
  if ((await countSlot.textContent())?.trim() !== '') {
    throw new Error('Artifact preview fallback rendered a GitHub star count.');
  }
  if (blockedGitHubRequests === 0) {
    throw new Error('Artifact preview did not attempt the blocked GitHub repository API.');
  }
  if (pageErrors.length > 0 || consoleErrors.length > 0) {
    throw new Error('Artifact preview fallback raised a page or console error.');
  }
  process.stdout.write(
    'Verified packaged preview keeps the GitHub link usable without a fallback count when the repository API is unavailable.\n',
  );

  for (const { route, status, headingText } of [
    {
      route: '',
      status: 200,
      headingText: 'Make invisible I/O observable—and testable.',
    },
    { route: 'docs/', status: 200, headingText: 'Sniffy documentation' },
    {
      route: 'docs/network/traffic-capture/',
      status: 200,
      headingText: 'Traffic capture and TLS inspection',
    },
    {
      route: 'use-cases/database-query-testing/',
      status: 200,
      headingText: 'Make database behavior part of the test contract.',
    },
    {
      route: 'use-cases/sql-profiling/',
      status: 200,
      headingText: 'See the database work behind each request.',
    },
    {
      route: 'use-cases/network-fault-testing/',
      status: 200,
      headingText: 'Exercise failure paths without pretending to be the whole network.',
    },
    {
      route: 'use-cases/traffic-capture/',
      status: 200,
      headingText: 'Follow network bytes from the Java call that moved them.',
    },
    { route: 'missing-shell-route/', status: 404, headingText: 'This trail went cold.' },
  ]) {
    const url = previewRoute(route);
    const response = await page.goto(url, { waitUntil: 'networkidle' });
    const heading = await page.locator('h1').first().textContent();
    if (response?.status() !== status || !heading?.includes(headingText)) {
      throw new Error(`Artifact preview failed for ${url}: HTTP ${response?.status()}`);
    }
    process.stdout.write(`Verified ${url} renders over HTTP.\n`);
  }

  const staticAssetUrl = previewRoute('img/brand/sniffy-social.svg');
  const staticAssetResponse = await page.request.get(staticAssetUrl);
  if (
    staticAssetResponse.status() !== 200 ||
    !staticAssetResponse.headers()['content-type']?.startsWith('image/svg+xml')
  ) {
    throw new Error(`Artifact preview failed for static asset ${staticAssetUrl}.`);
  }
  process.stdout.write(`Verified ${staticAssetUrl} serves a static asset.\n`);

  await page.goto(previewRoute('docs/latest/#_configuration'));
  await page.waitForURL(new URL(`${baseUrl}docs/configuration/`, previewUrl).toString());
  process.stdout.write('Verified packaged legacy documentation redirect honors the base URL.\n');

  await page.goto(previewRoute('use-cases/database-query-testing/'), {
    waitUntil: 'networkidle',
  });
  if (
    (await page.locator('link[rel="canonical"]').getAttribute('href')) !==
      canonicalRoute('use-cases/database-query-testing/') ||
    (await page.locator('meta[property="og:title"]').getAttribute('content')) !==
      'Database query testing with Sniffy'
  ) {
    throw new Error('Artifact preview did not preserve the use-case canonical or social metadata.');
  }
  await page.getByRole('navigation', { name: 'Related documentation' }).waitFor();
  process.stdout.write('Verified packaged use-case metadata and related documentation.\n');

  await page.goto(previewRoute('use-cases/sql-profiling/'), { waitUntil: 'networkidle' });
  if (
    (await page.locator('link[rel="canonical"]').getAttribute('href')) !==
      canonicalRoute('use-cases/sql-profiling/') ||
    (await page.locator('meta[property="og:title"]').getAttribute('content')) !==
      'SQL profiling and N+1 diagnosis with Sniffy'
  ) {
    throw new Error('Artifact preview did not preserve SQL profiling metadata.');
  }
  await page.getByRole('navigation', { name: 'Related documentation' }).waitFor();
  process.stdout.write('Verified packaged SQL profiling metadata and related documentation.\n');

  await page.goto(previewRoute('use-cases/network-fault-testing/'), {
    waitUntil: 'networkidle',
  });
  if (
    (await page.locator('link[rel="canonical"]').getAttribute('href')) !==
      canonicalRoute('use-cases/network-fault-testing/') ||
    (await page.locator('meta[property="og:title"]').getAttribute('content')) !==
      'Java network fault and resilience testing with Sniffy'
  ) {
    throw new Error(
      'Artifact preview did not preserve the network-fault canonical or social metadata.',
    );
  }
  await page.getByRole('navigation', { name: 'Related documentation' }).waitFor();
  process.stdout.write('Verified packaged network-fault metadata and related documentation.\n');

  await page.goto(previewRoute('use-cases/traffic-capture/'), {
    waitUntil: 'networkidle',
  });
  if (
    (await page.locator('link[rel="canonical"]').getAttribute('href')) !==
      canonicalRoute('use-cases/traffic-capture/') ||
    (await page.locator('meta[property="og:title"]').getAttribute('content')) !==
      'Java traffic capture and TLS inspection with Sniffy'
  ) {
    throw new Error(
      'Artifact preview did not preserve the traffic-capture canonical or social metadata.',
    );
  }
  await page.getByRole('navigation', { name: 'Related documentation' }).waitFor();
  process.stdout.write('Verified packaged traffic-capture metadata and related documentation.\n');

  await page.goto(previewSiteUrl, { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: /Search docs/ }).click();
  const searchInput = page.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });
  await searchInput.fill('traffic capture');
  await page.getByRole('option').first().waitFor();
  await searchInput.press('Enter');
  const expectedSearchPath = `${baseUrl}docs/network/traffic-capture/`;
  await page.waitForURL(
    (url) => url.pathname === expectedSearchPath && (url.hash === '' || url.hash.startsWith('#')),
  );
  await page
    .getByRole('heading', { level: 1, name: 'Traffic capture and TLS inspection' })
    .waitFor();
  process.stdout.write('Verified packaged documentation search navigates over HTTP.\n');

  if (pageErrors.length > 0) {
    throw new AggregateError(pageErrors, 'Artifact preview raised browser page errors.');
  }
} finally {
  await browser.close();
  preview.kill('SIGTERM');
  await Promise.race([
    new Promise((resolveExit) => preview.once('exit', resolveExit)),
    delay(5_000),
  ]);
}
