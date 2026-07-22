import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, extname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  currentDocsPath,
  legacyDocsPath,
  parseLegacyRouteMap,
  renderLegacyDocsPage,
} from './legacy-docs';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repositoryRoot = resolve(site, '../../..');
const legacyRoot = resolve(repositoryRoot, 'sniffy-documentation/src/main/asciidoc');
const docsRoot = resolve(site, 'docs');

describe('legacy documentation compatibility', () => {
  const mapping = readFileSync(resolve(docsRoot, 'migration/asciidoc-route-map.mdx'), 'utf8');
  const routes = parseLegacyRouteMap(mapping);

  it('maps the real IDs emitted by the assembled legacy Asciidoctor page', () => {
    const captureTrafficSource = readFileSync(resolve(legacyRoot, 'capture-traffic.adoc'), 'utf8');

    expect(Object.keys(routes).length).toBeGreaterThan(150);
    expect(routes).toMatchObject({
      _install: '/docs/installation/',
      _standalone_setup: '/docs/installation/',
      _configuration: '/docs/configuration/',
      _integration_with_junit: '/docs/testing/junit/',
      _emulating_network_issues: '/docs/network/fault-emulation/',
      _capture_traffic: '/docs/network/traffic-capture/',
      _ssltls_traffic_decryption: '/docs/network/traffic-capture/#ssltls-traffic-decryption',
    });
    expect(mapping).toContain(
      'input for the separately scoped legacy URL and anchor compatibility',
    );
    expect(captureTrafficSource).toContain(
      'https://sniffy.io/docs/latest/#_standalone_setup[Standalone Setup]',
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

const sourceRegionOverrides: Record<string, string> = {
  'sniffy/src/jboss-module/module.xml': 'WildFlyModule',
  'sniffy-test/sniffy-junit/src/test/java/io/sniffy/test/junit/usage/JUnitUsageTest.java':
    'JUnitUsage',
  'sniffy-test/sniffy-kotest/src/test/kotlin/io/sniffy/test/kotest/usage/KotestUsageTests.kt':
    'KotestUsage',
  'sniffy-test/sniffy-spring-test/src/test/java/io/sniffy/test/spring/usage/SpringUsageTest.java':
    'SpringUsage',
  'sniffy-test/sniffy-spring-test/src/test/java/io/sniffy/test/spring/usage/SpringSharedConnectionUsageTest.java':
    'SpringSharedConnectionUsage',
  'sniffy-test/sniffy-testng/src/test/java/io/sniffy/test/testng/usage/UsageTestNg.java':
    'TestNgUsage',
  'sniffy-integration-tests/sniffy-integration-tests-spock/src/test/groovy/io/sniffy/test/spock/usage/SpockUsageSpec.groovy':
    'SpockUsage',
};

function allFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = resolve(directory, entry.name);
    return entry.isDirectory() ? allFiles(path) : [path];
  });
}

function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/`/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

describe('migrated documentation contract', () => {
  const legacyFiles = readdirSync(legacyRoot)
    .filter((file) => file.endsWith('.adoc'))
    .sort();
  const mdxFiles = allFiles(docsRoot).filter((file) => extname(file) === '.mdx');
  const allMdx = mdxFiles.map((file) => readFileSync(file, 'utf8')).join('\n');
  const mapping = readFileSync(resolve(docsRoot, 'migration/asciidoc-route-map.mdx'), 'utf8');

  it('accounts for every legacy file and heading in the route map', () => {
    expect(legacyFiles).toHaveLength(19);
    for (const file of legacyFiles) {
      const source = readFileSync(resolve(legacyRoot, file), 'utf8');
      expect(mapping).toContain(`\`${file}#`);
      for (const match of source.matchAll(/^={1,4} (.+)$/gm)) {
        expect(mapping).toContain(`\`${file}#${slugify(match[1])}\``);
      }
    }
    expect(mapping).not.toContain('undefined');
    expect(mapping).not.toContain('.mdx');
  });

  it('replaces every repository source include with a named build-time snippet', () => {
    for (const file of legacyFiles) {
      const source = readFileSync(resolve(legacyRoot, file), 'utf8');
      for (const match of source.matchAll(/^include::(.+?)\[(?:tags=([^]]+))?\]$/gm)) {
        if (match[1].endsWith('.adoc')) {
          continue;
        }
        const repositoryPath = match[1].replace(/^(?:\.\.\/){4}/, '');
        const region = match[2] ?? sourceRegionOverrides[repositoryPath];
        expect(region).toBeTruthy();
        expect(allMdx).toContain(`file="${repositoryPath}"`);
        expect(allMdx).toContain(`region="${region}"`);
      }
    }
    expect(allMdx).not.toContain('include::');
  });

  it('contains no unresolved AsciiDoc-style placeholders', () => {
    expect(allMdx).not.toMatch(/(?<!\$)\{[A-Za-z][A-Za-z0-9_-]*\}/);
  });

  it('keeps all legacy images in the site without changing the legacy module', () => {
    for (const image of ['agent-ui.png', 'demo.gif', 'network-connections.png']) {
      const legacy = resolve(legacyRoot, 'images', image);
      const migrated = resolve(site, 'static/img/docs', image);
      expect(existsSync(migrated)).toBe(true);
      expect(statSync(migrated).size).toBe(statSync(legacy).size);
      expect(allMdx).toContain(`/img/docs/${image}`);
    }
    expect(existsSync(resolve(repositoryRoot, 'sniffy-documentation/pom.xml'))).toBe(true);
  });

  it('organizes the current documentation into explicit sidebar groups', () => {
    const sidebars = readFileSync(resolve(site, 'sidebars.ts'), 'utf8');
    for (const group of [
      'Installation',
      'Application setup',
      'Configuration',
      'Testing integrations',
      'Network behavior',
      'Migration',
    ]) {
      expect(sidebars).toContain(`label: '${group}'`);
    }
    for (const doc of [
      'installation/index',
      'setup/filter',
      'configuration/nio-monitoring',
      'testing/junit',
      'network/fault-emulation',
      'network/traffic-capture',
      'migration/to-4',
    ]) {
      expect(sidebars).toContain(`'${doc}'`);
    }
  });
});
