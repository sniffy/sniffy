import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import NetworkFaultTestingPage from './pages/use-cases/network-fault-testing';
import { createNetworkFaultTestingUseCase } from './use-cases/network-fault-testing';

vi.mock('@docusaurus/useDocusaurusContext', () => ({
  default: () => ({ siteConfig: { customFields: { productVersion: '3.2.1-SNAPSHOT' } } }),
}));

const site = resolve(import.meta.dirname, '..');
const repository = resolve(site, '../../..');
const content = createNetworkFaultTestingUseCase('3.2.1-SNAPSHOT');

describe('network fault testing use-case page', () => {
  it('renders focused metadata, boundaries, repository-version examples, and real product evidence', () => {
    render(<NetworkFaultTestingPage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Exercise failure paths without pretending to be the whole network.',
      }),
    ).toBeVisible();
    expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
      'href',
      'https://sniffy.io/use-cases/network-fault-testing/',
    );
    expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
      'content',
      'Java network fault and resilience testing with Sniffy',
    );
    expect(document.querySelector('meta[name="twitter:description"]')).toHaveAttribute(
      'content',
      content.metadata.social.description,
    );
    expect(screen.getByText(/sniffy-junit-jupiter:3\.2\.1-SNAPSHOT/)).toBeVisible();
    expect(screen.getByText(/<version>3\.2\.1-SNAPSHOT<\/version>/)).toBeVisible();
    expect(screen.getByRole('img', { name: /allow, block, and delay controls/ })).toHaveAttribute(
      'src',
      '/img/use-cases/traffic-capture-network.png',
    );
    expect(screen.getAllByRole('link', { name: 'Review the delay model' })).toHaveLength(2);

    const capabilities = screen.getByRole('region', {
      name: 'Separate discovery, exploration, and test automation.',
    });
    for (const name of [
      'Discover before disrupting',
      'Explore faults in the browser',
      'Make isolation repeatable',
      'Use delay with its real scope',
      'Preserve the transport boundary',
    ]) {
      expect(within(capabilities).getByRole('heading', { level: 3, name })).toBeVisible();
    }
  });

  it('links only to existing current documentation', () => {
    for (const relatedDoc of content.metadata.relatedDocs) {
      const relative = relatedDoc.href.replace(/^\/docs\//, '').replace(/\/$/, '');
      const candidates = [
        resolve(site, `docs/${relative}.mdx`),
        resolve(site, `docs/${relative}/index.mdx`),
      ];
      expect(
        candidates.some((candidate) => {
          try {
            return readFileSync(candidate, 'utf8').length > 0;
          } catch {
            return false;
          }
        }),
      ).toBe(true);
    }
  });

  it('backs discovery, blocking, delay, lifecycle, and transport claims with current sources', () => {
    const registry = readFileSync(
      resolve(repository, 'sniffy-core/src/main/java/io/sniffy/registry/ConnectionsRegistry.java'),
      'utf8',
    );
    const socket = readFileSync(
      resolve(
        repository,
        'sniffy-core/src/main/java/io/sniffy/socket/CompatSnifferSocketImpl.java',
      ),
      'utf8',
    );
    const input = readFileSync(
      resolve(repository, 'sniffy-core/src/main/java/io/sniffy/socket/SnifferInputStream.java'),
      'utf8',
    );
    const extension = readFileSync(
      resolve(
        repository,
        'sniffy-test/sniffy-junit-jupiter/src/main/java/io/sniffy/test/junit/jupiter/SniffyExtension.java',
      ),
      'utf8',
    );
    const junitDocs = readFileSync(resolve(site, 'docs/testing/junit.mdx'), 'utf8');
    const faultDocs = readFileSync(resolve(site, 'docs/network/fault-emulation.mdx'), 'utf8');
    const nioDocs = readFileSync(resolve(site, 'docs/configuration/nio-monitoring.mdx'), 'utf8');
    const captureDocs = readFileSync(resolve(site, 'docs/network/traffic-capture.mdx'), 'utf8');
    const copy = JSON.stringify(content);

    expect(registry).toContain(
      'setSocketAddressStatus(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), 0)',
    );
    expect(socket).toContain('connectionStatus < 0');
    expect(socket).toContain('connectionStatus * numberOfSleepCycles');
    expect(input).toContain('DEFAULT_TCP_WINDOW_SIZE');
    expect(extension).toContain('setSocketAddressStatus(null, null, -1)');
    expect(junitDocs).toContain('JVM-global connectivity registry');
    expect(faultDocs).toContain('Establishing connection adds _D_ milliseconds delay');
    for (const boundary of ['UDP `DatagramChannel`', 'UNIX-domain', 'NIO2/AIO'])
      expect(nioDocs).toContain(boundary);
    expect(captureDocs).toContain('native transport');
    for (const limitation of [
      'packet loss',
      'bandwidth',
      'random chaos',
      'not a constant end-to-end response time',
    ])
      expect(copy).toContain(limitation);
  });

  it('keeps the route and shared layout free of page-specific contracts', () => {
    const route = readFileSync(
      resolve(site, 'src/pages/use-cases/network-fault-testing/index.tsx'),
      'utf8',
    );
    const layout = readFileSync(
      resolve(site, 'src/components/use-case/use-case-layout.tsx'),
      'utf8',
    );
    expect(route).toContain('<UseCaseLayout content={content} />');
    expect(layout).not.toContain('Discover before disrupting');
    expect(layout).not.toContain('NetworkFallbackTest');
  });

  it('uses the documented byte-identical profiler fixture instead of invented UI', () => {
    const published = readFileSync(
      resolve(site, 'static/img/use-cases/traffic-capture-network.png'),
    );
    const source = readFileSync(
      resolve(repository, 'sniffy-ui/tests/e2e/visual-baselines/profiler-registry-checked.png'),
    );
    const provenance = readFileSync(resolve(site, 'static/img/use-cases/README.md'), 'utf8');
    expect(published.equals(source)).toBe(true);
    expect(provenance).toContain('byte-for-byte copy');
    expect(content.productResult.provenance).toContain('deterministic Playwright fixture');
  });
});
