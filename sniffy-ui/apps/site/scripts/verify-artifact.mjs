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
  page.on('pageerror', (error) => pageErrors.push(error));

  for (const { route, status, headingText } of [
    { route: '', status: 200, headingText: 'Sniffy' },
    { route: 'docs/', status: 200, headingText: 'Sniffy documentation' },
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
