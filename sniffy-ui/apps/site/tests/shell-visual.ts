import type { Page } from '@playwright/test';

/** Recreate Docusaurus' former external-link glyph for visual-neutral shell captures. */
export async function prepareShellScreenshot(page: Page): Promise<void> {
  await page.locator('a:has(> [data-testid="github-star-count"])').evaluateAll((links) => {
    for (const link of links) {
      if (link.querySelector('[data-shell-external-link]')) continue;
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('width', '13.5');
      svg.setAttribute('height', '13.5');
      svg.setAttribute('data-shell-external-link', '');
      svg.style.marginLeft = '0.3rem';
      const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
      use.setAttribute('href', '#theme-svg-external-link');
      svg.append(use);
      link.append(svg);
    }
  });
}
