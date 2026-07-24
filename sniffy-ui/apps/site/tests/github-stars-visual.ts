import type { Page } from '@playwright/test';
import path from 'node:path';

const githubScreenshotStyle = path.join(__dirname, 'github-stars-screenshot.css');

/** Load the embedded screenshot font before Playwright starts comparing pixels. */
export async function prepareGitHubStarsScreenshot(page: Page): Promise<void> {
  await page.addStyleTag({ path: githubScreenshotStyle });
  await page.evaluate(async () => {
    await document.fonts.load("400 16px 'Sniffy Visual Test'");
    await document.fonts.load("700 16px 'Sniffy Visual Test'");
    await document.fonts.ready;
  });
}
