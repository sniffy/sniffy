import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspace = resolve(site, '../..');
const readJson = (path: string) => JSON.parse(readFileSync(path, 'utf8')) as Record<string, any>;

describe('@sniffy/site workspace contract', () => {
  it('pins one compatible Docusaurus release across the application', () => {
    const packageJson = readJson(resolve(site, 'package.json'));

    expect(packageJson.dependencies).toMatchObject({
      '@docusaurus/core': '3.10.2',
      '@docusaurus/preset-classic': '3.10.2',
      '@sniffy/theme': '0.0.0',
    });
    expect(packageJson.devDependencies).toMatchObject({
      '@docusaurus/module-type-aliases': '3.10.2',
      '@docusaurus/tsconfig': '3.10.2',
      '@docusaurus/types': '3.10.2',
    });
  });

  it('keeps Java-resource production builds separate from site commands', () => {
    const packageJson = readJson(resolve(workspace, 'package.json'));

    expect(packageJson.scripts).toMatchObject({
      build: 'node scripts/build.mjs',
      'build:site': 'npm run build --workspace @sniffy/site',
      'dev:site': 'npm run start --workspace @sniffy/site',
      'test:site':
        'npm run build:site && vitest run --project unit apps/site/src/site.test.ts && npm test --workspace @sniffy/site',
    });
  });

  it('adapts Infima to shared theme layers without copying palette literals', () => {
    const styles = readFileSync(resolve(site, 'src/css/custom.css'), 'utf8');

    expect(styles).toContain("@import '@sniffy/theme/base.css';");
    expect(styles).toContain("@import '@sniffy/theme/dark.css';");
    expect(styles).toContain('--ifm-color-primary: var(--sniffy-accent);');
    expect(styles).not.toMatch(/#[\da-f]{3,8}\b/i);
  });

  it('enables current docs while leaving future routes disabled', () => {
    const config = readFileSync(resolve(site, 'docusaurus.config.ts'), 'utf8');

    expect(config).toContain("routeBasePath: 'docs'");
    expect(config).toContain("lastVersion: 'current'");
    expect(config).toContain('blog: false');
    expect(config).not.toMatch(/versioned_(docs|sidebars)/);
  });
});
