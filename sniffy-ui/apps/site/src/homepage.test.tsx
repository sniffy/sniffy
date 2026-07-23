import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import Home from './pages/index';

vi.mock('@docusaurus/useDocusaurusContext', () => ({
  default: () => ({
    siteConfig: {
      customFields: {
        productVersion: '4.0.0-SNAPSHOT',
      },
    },
  }),
}));

const site = resolve(import.meta.dirname, '..');
const workspace = resolve(site, '../..');
const homepage = readFileSync(resolve(site, 'src/pages/index.tsx'), 'utf8');
const config = readFileSync(resolve(site, 'docusaurus.config.ts'), 'utf8');

describe('Sniffy homepage contract', () => {
  it('renders the product story, current integrations, and versioned installation examples', () => {
    render(<Home />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Make invisible I/O observable—and testable.',
      }),
    ).toBeVisible();
    expect(
      screen.getByRole('img', { name: /Sniffy profiler showing executed SQL/ }),
    ).toHaveAttribute('src', '/img/home/sniffy-profiler.png');
    expect(screen.getByRole('link', { name: 'Start with Sniffy' })).toHaveAttribute(
      'href',
      '/docs/installation/',
    );

    const useCases = screen.getByRole('region', {
      name: 'See behavior. Set expectations. Break assumptions safely.',
    });
    for (const heading of [
      'SQL profiling and assertions',
      'Network fault testing',
      'Traffic and TLS capture',
    ]) {
      expect(within(useCases).getByRole('heading', { level: 3, name: heading })).toBeVisible();
    }

    expect(screen.getByText(/io\.sniffy:sniffy-spring:4\.0\.0-SNAPSHOT/)).toBeVisible();
    expect(screen.getByText(/<version>4\.0\.0-SNAPSHOT<\/version>/)).toBeVisible();
  });

  it('uses the root product version for both installation examples', () => {
    expect(config).toContain('productVersion: readProductVersion(repositoryRoot)');
    expect(homepage).toContain('siteConfig.customFields?.productVersion');
    expect(homepage).toContain('<version>${productVersion}</version>');
    expect(homepage).toContain('io.sniffy:sniffy-spring:${productVersion}');
    expect(homepage).not.toMatch(/io\.sniffy:sniffy-spring:\d/);
  });

  it('links every homepage use case to a current documentation route', () => {
    for (const route of [
      '/docs/testing/api/',
      '/docs/network/fault-emulation/',
      '/docs/network/traffic-capture/',
      '/docs/installation/',
      '/docs/testing/junit/',
      '/docs/configuration/nio-monitoring/',
    ]) {
      expect(homepage).toContain(route);
    }

    for (const futureRoute of [
      '/sql-query-count/',
      '/network-fault-testing/',
      '/traffic-capture/',
    ]) {
      expect(homepage).not.toContain(`to="${futureRoute}"`);
      expect(homepage).not.toContain(`href: '${futureRoute}'`);
    }
  });

  it('publishes complete page metadata and descriptive product media', () => {
    expect(homepage).toContain('<link rel="canonical" href="https://sniffy.io/" />');
    for (const property of ['og:title', 'og:description', 'og:url', 'og:image']) {
      expect(homepage).toContain(`property="${property}"`);
    }
    for (const name of ['twitter:title', 'twitter:description', 'twitter:image']) {
      expect(homepage).toContain(`name="${name}"`);
    }
    expect(homepage).toContain('alt="Sniffy profiler showing executed SQL');
  });

  it('keeps homepage product media identical to the real profiler visual fixture', () => {
    const published = readFileSync(resolve(site, 'static/img/home/sniffy-profiler.png'));
    const fixture = readFileSync(resolve(workspace, 'tests/e2e/visual-baselines/profiler.png'));
    const provenance = readFileSync(resolve(site, 'static/img/home/README.md'), 'utf8');

    expect(published.equals(fixture)).toBe(true);
    expect(provenance).toContain('/mock/mock.html');
    expect(provenance).toContain(
      'npm run test:e2e:update -- --grep "@visual final profiler and agent states"',
    );
  });
});
