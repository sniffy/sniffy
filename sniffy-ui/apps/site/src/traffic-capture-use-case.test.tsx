import { render, screen, within } from '@testing-library/react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import TrafficCapturePage from './pages/use-cases/traffic-capture';
import { trafficCaptureUseCase } from './use-cases/traffic-capture';

const site = resolve(import.meta.dirname, '..');
const repository = resolve(site, '../../..');

describe('traffic capture use-case page', () => {
  it('renders precise metadata, supported paths, code, product evidence, and calls to action', () => {
    render(<TrafficCapturePage />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Follow network bytes from the Java call that moved them.',
      }),
    ).toBeVisible();
    expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
      'href',
      'https://sniffy.io/use-cases/traffic-capture/',
    );
    expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
      'content',
      'Java traffic capture and TLS inspection with Sniffy',
    );
    expect(document.querySelector('meta[name="twitter:description"]')).toHaveAttribute(
      'content',
      trafficCaptureUseCase.metadata.social.description,
    );

    const capabilities = screen.getByRole('region', {
      name: 'Know which bytes Sniffy can put in context.',
    });
    for (const heading of [
      'Preserve application-level evidence',
      'Narrow the investigation',
      'Choose the supported transport paths',
      'Separate raw bytes from TLS plaintext',
      'Make the limits visible',
    ]) {
      expect(within(capabilities).getByRole('heading', { level: 3, name: heading })).toBeVisible();
    }

    expect(
      screen.getByRole('img', { name: /Current Sniffy profiler Network Connections tab/ }),
    ).toHaveAttribute('src', '/img/use-cases/traffic-capture-network.png');
    expect(screen.getByText(/captureNetworkTraffic\(true\)/)).toBeVisible();
    expect(screen.getAllByText(/getDecryptedNetworkTraffic/)).toHaveLength(2);
    expect(screen.getAllByRole('link', { name: 'Read the traffic capture guide' })).toHaveLength(2);
  });

  it('keeps related documentation on existing current-docs routes', () => {
    for (const relatedDoc of trafficCaptureUseCase.metadata.relatedDocs) {
      const relative = relatedDoc.href.replace(/^\/docs\//, '').replace(/\/$/, '');
      const documentPath =
        relative.length === 0
          ? resolve(site, 'docs/index.mdx')
          : resolve(site, `docs/${relative}.mdx`);
      const indexPath = resolve(site, `docs/${relative}/index.mdx`);

      expect(
        [documentPath, indexPath].some((candidate) => {
          try {
            return readFileSync(candidate, 'utf8').length > 0;
          } catch {
            return false;
          }
        }),
      ).toBe(true);
    }
  });

  it('backs the transport, NIO, TLS, and configuration claims with current repository sources', () => {
    const socketCapture = readFileSync(
      resolve(
        repository,
        'sniffy-core/src/test/java/io/sniffy/socket/CaptureTrafficOverviewTest.java',
      ),
      'utf8',
    );
    const tlsCapture = readFileSync(
      resolve(
        repository,
        'sniffy-module-tls/src/test/java/io/sniffy/tls/CaptureSslTrafficTest.java',
      ),
      'utf8',
    );
    const captureDocs = readFileSync(resolve(site, 'docs/network/traffic-capture.mdx'), 'utf8');
    const nioDocs = readFileSync(resolve(site, 'docs/configuration/nio-monitoring.mdx'), 'utf8');
    const configuration = readFileSync(resolve(site, 'docs/configuration/index.mdx'), 'utf8');
    const pageCopy = JSON.stringify(trafficCaptureUseCase);

    for (const token of [
      'captureNetworkTraffic(true)',
      'getNetworkTraffic(',
      'Threads.ANY',
      'AddressMatchers.anyAddressMatcher()',
    ]) {
      expect(socketCapture).toContain(token);
    }
    expect(tlsCapture).toContain('getDecryptedNetworkTraffic(');
    expect(captureDocs).toContain('Classic sockets and monitored TCP NIO');
    expect(captureDocs).toContain('Apache APR');
    expect(captureDocs).toContain('native transport');
    expect(configuration).toContain('io.sniffy.decryptTls');
    expect(configuration).toContain('io.sniffy.monitorNio');
    for (const limitation of ['UDP `DatagramChannel`', 'UNIX-domain', 'NIO2/AIO', 'fail open']) {
      expect(nioDocs).toContain(limitation);
    }
    expect(pageCopy).toContain('instrumented JVM');
    expect(pageCopy).toContain('does not capture another process');
    expect(pageCopy).toContain('supported JSSE');
    expect(pageCopy).not.toMatch(/host-wide visibility|every TLS stack|universal packet capture/i);
  });

  it('publishes the classic Socket monitoring prerequisite hidden by the source test fixture', () => {
    const configurationSource = readFileSync(
      resolve(
        repository,
        'sniffy-core/src/main/java/io/sniffy/configuration/SniffyConfiguration.java',
      ),
      'utf8',
    );
    const filterSource = readFileSync(
      resolve(repository, 'sniffy-web/src/main/java/io/sniffy/servlet/SniffyFilter.java'),
      'utf8',
    );
    const agentSource = readFileSync(
      resolve(repository, 'sniffy/src/main/java/io/sniffy/SniffyAgent.java'),
      'utf8',
    );
    const publishedSetup = trafficCaptureUseCase.codeExamples
      .map((example) => example.code)
      .join('\n');
    const publishedCopy = trafficCaptureUseCase.solution.body.join(' ');

    expect(configurationSource).toContain(
      '"io.sniffy.monitorSocket", "IO_SNIFFY_MONITOR_SOCKET", "false"',
    );
    expect(filterSource).toContain('setMonitorSocket(true)');
    expect(agentSource).toContain('SniffyConfiguration.INSTANCE.setMonitorSocket(true)');
    expect(publishedSetup).toContain('-Dio.sniffy.monitorSocket=true');
    expect(publishedCopy).toContain('defaults to false');
    expect(publishedCopy).toContain(
      'SniffyFilter and the standalone javaagent enable classic Socket monitoring implicitly',
    );
  });

  it('keeps traffic-specific content out of the route and shared layout', () => {
    const route = readFileSync(
      resolve(site, 'src/pages/use-cases/traffic-capture/index.tsx'),
      'utf8',
    );
    const layout = readFileSync(
      resolve(site, 'src/components/use-case/use-case-layout.tsx'),
      'utf8',
    );
    const pageContent = readFileSync(resolve(site, 'src/use-cases/traffic-capture.ts'), 'utf8');

    expect(route).toContain('trafficCaptureUseCase');
    expect(route).toContain('<UseCaseLayout content={trafficCaptureUseCase} />');
    expect(route).not.toContain('host-wide packet sniffer');
    for (const pageSpecificCopy of [
      'The visibility gap',
      'The Sniffy boundary',
      'Supported evidence',
      'Repository-backed setup',
      'Real product evidence',
      'Configuration and limits',
      'Start inside the JVM',
    ]) {
      expect(layout).not.toContain(pageSpecificCopy);
      expect(pageContent).toContain(pageSpecificCopy);
    }
    expect(pageContent).not.toContain('<section');
  });

  it('publishes a byte-identical current profiler fixture with written provenance', () => {
    const source = readFileSync(
      resolve(repository, 'sniffy-ui/tests/e2e/visual-baselines/profiler-registry-checked.png'),
    );
    const published = readFileSync(
      resolve(site, 'static/img/use-cases/traffic-capture-network.png'),
    );
    const provenance = readFileSync(resolve(site, 'static/img/use-cases/README.md'), 'utf8');

    expect(published.equals(source)).toBe(true);
    expect(provenance).toContain('byte-for-byte copy');
    expect(provenance).toContain('profiler-registry-checked.png');
    expect(provenance).toContain('/mock/mock.html');
    expect(trafficCaptureUseCase.productResult.provenance).toContain(
      'deterministic Playwright fixture',
    );
  });
});
