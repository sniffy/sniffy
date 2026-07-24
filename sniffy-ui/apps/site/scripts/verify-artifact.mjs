import { spawn } from 'node:child_process';
import { resolve } from 'node:path';
import process from 'node:process';
import { clearTimeout, setTimeout } from 'node:timers';
import { setTimeout as delay } from 'node:timers/promises';
import { chromium } from '@playwright/test';

const build = resolve(import.meta.dirname, '../build');
const preview = spawn(
  process.execPath,
  [resolve(build, 'preview.mjs'), '--host', '127.0.0.1', '--port', '0'],
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

  await page.goto(previewUrl, { waitUntil: 'networkidle' });
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
    { route: 'missing-shell-route/', status: 404, headingText: 'This trail went cold.' },
  ]) {
    const url = `${previewUrl}${route}`;
    const response = await page.goto(url, { waitUntil: 'networkidle' });
    const heading = await page.locator('h1').first().textContent();
    if (response?.status() !== status || !heading?.includes(headingText)) {
      throw new Error(`Artifact preview failed for ${url}: HTTP ${response?.status()}`);
    }
    process.stdout.write(`Verified ${url} renders over HTTP.\n`);
  }

  await page.goto(`${previewUrl}use-cases/database-query-testing/`, {
    waitUntil: 'networkidle',
  });
  if (
    (await page.locator('link[rel="canonical"]').getAttribute('href')) !==
      'https://sniffy.io/use-cases/database-query-testing/' ||
    (await page.locator('meta[property="og:title"]').getAttribute('content')) !==
      'Database query testing with Sniffy'
  ) {
    throw new Error('Artifact preview did not preserve the use-case canonical or social metadata.');
  }
  await page.getByRole('navigation', { name: 'Related documentation' }).waitFor();
  process.stdout.write('Verified packaged use-case metadata and related documentation.\n');

  await page.goto(previewUrl, { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: /Search docs/ }).click();
  const searchInput = page.getByRole('combobox', {
    name: 'Search current Sniffy documentation',
  });
  await searchInput.fill('traffic capture');
  await page.getByRole('option').first().waitFor();
  await searchInput.press('Enter');
  await page.waitForURL(/\/docs\/network\/traffic-capture\/(?:#.*)?$/);
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
