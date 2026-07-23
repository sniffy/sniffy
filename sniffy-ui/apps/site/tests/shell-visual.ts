import type { Page } from '@playwright/test';

/** Recreate Docusaurus' former external-link glyph for visual-neutral shell captures. */
export async function prepareShellScreenshot(page: Page): Promise<void> {
  await page.locator('a:has(> [data-testid="github-star-count"])').evaluateAll((links) => {
    for (const link of links) {
      if (link.hasAttribute('data-shell-original-html')) continue;
      link.setAttribute('data-shell-original-html', link.innerHTML);
      link.setAttribute('data-shell-original-class', link.className);
      link.setAttribute(
        'class',
        link.classList.contains('navbar__link') ? 'navbar__item navbar__link' : 'menu__link',
      );
      link.replaceChildren('GitHub');
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      svg.setAttribute('width', '13.5');
      svg.setAttribute('height', '13.5');
      svg.style.marginLeft = '0.3rem';
      const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
      use.setAttribute('href', '#theme-svg-external-link');
      svg.append(use);
      link.append(svg);
    }
  });
}

export async function restoreShellScreenshot(page: Page): Promise<void> {
  await page.locator('a[data-shell-original-html]').evaluateAll((links) => {
    for (const link of links) {
      link.innerHTML = link.getAttribute('data-shell-original-html') ?? '';
      link.setAttribute('class', link.getAttribute('data-shell-original-class') ?? '');
      link.removeAttribute('data-shell-original-html');
      link.removeAttribute('data-shell-original-class');
    }
  });
}
