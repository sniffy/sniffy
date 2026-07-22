import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  currentDocsPath,
  legacyDocsPath,
  parseLegacyRouteMap,
  renderLegacyDocsPage,
} from './legacy-docs';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const mapping = readFileSync(resolve(site, 'docs/migration/asciidoc-route-map.mdx'), 'utf8');
const routes = parseLegacyRouteMap(mapping);

describe('legacy documentation compatibility', () => {
  it('derives every known legacy anchor from the migration inventory', () => {
    expect(Object.keys(routes).length).toBeGreaterThan(100);
    expect(routes).toMatchObject({
      _install: '/docs/installation/',
      configuration: '/docs/configuration/',
      '_unit-and-component-tests': '/docs/testing/api/',
      '_integration-with-junit': '/docs/testing/junit/',
      'emulating-network-issues': '/docs/network/fault-emulation/',
      '_capture-traffic': '/docs/network/traffic-capture/',
      'ssl-tls-traffic-decryption': '/docs/network/traffic-capture/#ssltls-traffic-decryption',
    });
    expect(mapping).toContain(
      'input for the separately scoped legacy URL and anchor compatibility',
    );
  });

  it('renders a static entry point with a current-docs canonical and safe fallback', () => {
    const html = renderLegacyDocsPage(routes);

    expect(legacyDocsPath).toBe('/docs/latest/');
    expect(html).toContain('<link rel="canonical" href="https://sniffy.io/docs/">');
    expect(html).toContain('decodeURIComponent(rawAnchor)');
    expect(html).toContain('Object.prototype.hasOwnProperty.call(routes, anchor)');
    expect(html).toContain(`var fallback = ${JSON.stringify(currentDocsPath)}`);
    expect(html).toContain('<a href="/docs/">');
  });
});
