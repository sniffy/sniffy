import { readFileSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspace = resolve(site, '../..');
const repository = resolve(workspace, '..');
const readJson = (path: string) => JSON.parse(readFileSync(path, 'utf8')) as Record<string, any>;

function allFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = resolve(directory, entry.name);
    return entry.isDirectory() ? allFiles(path) : [path];
  });
}

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

  it('aligns React exactly with the existing UI workspace', () => {
    const sitePackageJson = readJson(resolve(site, 'package.json'));
    const uiPackageJson = readJson(resolve(workspace, 'packages/ui/package.json'));

    expect(sitePackageJson.dependencies).toMatchObject({
      react: uiPackageJson.dependencies.react,
      'react-dom': uiPackageJson.dependencies['react-dom'],
    });
  });

  it('keeps Java-resource production builds separate from delegated site commands', () => {
    const workspacePackageJson = readJson(resolve(workspace, 'package.json'));
    const sitePackageJson = readJson(resolve(site, 'package.json'));

    expect(workspacePackageJson.workspaces).toContain('apps/*');
    expect(workspacePackageJson.scripts).toMatchObject({
      build: 'node scripts/build.mjs',
      'build:site': 'npm run build --workspace @sniffy/site',
      'dev:site': 'npm run start --workspace @sniffy/site',
      'test:site':
        'npm run build:site && vitest run --project unit apps/site/src/site.test.ts apps/site/src/source-snippets.test.ts apps/site/src/product-version.test.ts apps/site/src/documentation.test.ts && npm test --workspace @sniffy/site',
    });
    expect(sitePackageJson.scripts).toMatchObject({
      start: 'docusaurus start',
      build: 'docusaurus build',
      test: 'playwright test --config playwright.config.ts',
    });
  });

  it('scopes the serialize-javascript security exception to affected build plugins', () => {
    const packageJson = readJson(resolve(workspace, 'package.json'));

    expect(packageJson.overrides).toEqual({
      'copy-webpack-plugin': {
        'serialize-javascript': '7.0.7',
      },
      'css-minimizer-webpack-plugin': {
        'serialize-javascript': '7.0.7',
      },
    });
    expect(packageJson.overrides).not.toHaveProperty('sockjs');
  });

  it('adapts Infima to shared theme layers without copying palette literals', () => {
    const styles = readFileSync(resolve(site, 'src/css/custom.css'), 'utf8');

    expect(styles).toContain("@import '@sniffy/theme/base.css';");
    expect(styles).toContain("@import '@sniffy/theme/dark.css';");
    expect(styles).toContain("@import '@sniffy/theme/light.css';");
    expect(styles).toContain('--ifm-color-primary: var(--sniffy-accent);');
    expect(styles).not.toMatch(/#[\da-f]{3,8}\b/i);
  });

  it('enables both shared themes and current docs while leaving future routes disabled', () => {
    const config = readFileSync(resolve(site, 'docusaurus.config.ts'), 'utf8');

    expect(config).toContain("onBrokenLinks: 'throw'");
    expect(config).toContain("onBrokenAnchors: 'throw'");
    expect(config).toContain("onBrokenMarkdownLinks: 'throw'");
    expect(config).toContain("onBrokenMarkdownImages: 'throw'");
    expect(config).toContain("defaultMode: 'dark'");
    expect(config).toContain('disableSwitch: false');
    expect(config).toContain('respectPrefersColorScheme: true');
    expect(config).toContain("routeBasePath: 'docs'");
    expect(config).toContain("lastVersion: 'current'");
    expect(config).toContain('blog: false');
    expect(config).not.toMatch(/versioned_(docs|sidebars)/);
  });
});

describe('website validation workflow contract', () => {
  const workflow = readFileSync(resolve(repository, '.github/workflows/website.yml'), 'utf8');

  it('supports manual, develop-push, and path-filtered pull-request validation', () => {
    expect(workflow).toContain('workflow_dispatch:');
    expect(workflow).toMatch(/push:\n[\s\S]*?branches:\n\s+- develop\n[\s\S]*?paths:/);
    expect(workflow).toMatch(/pull_request:\n\s+paths:/);
    expect(workflow).toContain('sniffy-ui/apps/site/**');
    expect(workflow).toContain('sniffy-ui/packages/theme/**');
    expect(workflow).not.toMatch(/^\s+- ['"]?\*\*\/\*\.java/m);
  });

  it('tracks every repository source consumed by a tagged site snippet', () => {
    const mdx = allFiles(resolve(site, 'docs'))
      .filter((file) => file.endsWith('.mdx'))
      .map((file) => readFileSync(file, 'utf8'))
      .join('\n');
    const sources = [...mdx.matchAll(/<SourceSnippet\s+file="([^"]+)"/g)].map(([, file]) => file);

    expect(new Set(sources).size).toBeGreaterThan(0);
    for (const source of new Set(sources)) {
      expect(workflow).toContain(source);
    }
  });

  it('keeps validation read-only, explicit, and independent from release publication', () => {
    for (const step of [
      'Install locked dependencies',
      'Lint website sources',
      'Check website formatting',
      'Typecheck frontend workspace',
      'Test website contracts',
      'Build website with broken link and image validation',
      'Test desktop and mobile website navigation',
      'Upload complete static website',
    ]) {
      expect(workflow).toContain(`- name: ${step}`);
    }
    const contentsPermission = ['content', 's: read'].join('');
    expect(workflow).toContain(`permissions:\n  ${contentsPermission}`);
    expect(workflow).not.toContain('secrets.');
    expect(workflow).not.toMatch(/\bmvn\b/);
    expect(workflow).not.toMatch(/npm run (?:build|check:generated|check:bundle)(?:\s|$)/);
    expect(workflow).not.toMatch(/deploy|pages/i);
  });

  it('uploads the complete static build while preserving generated-resource checks elsewhere', () => {
    const pullRequestWorkflow = readFileSync(
      resolve(repository, '.github/workflows/pr.yml'),
      'utf8',
    );

    expect(workflow).toContain('uses: actions/upload-artifact@v7');
    expect(workflow).toContain('path: sniffy-ui/apps/site/build/');
    expect(workflow).toContain('if-no-files-found: error');
    expect(pullRequestWorkflow).toContain('run: npm run build');
    expect(pullRequestWorkflow).toContain('run: npm run check:generated');
    expect(pullRequestWorkflow).toContain('run: npm run check:bundle');
  });

  it('runs the website browser suite at desktop and mobile viewports', () => {
    const playwright = readFileSync(resolve(site, 'playwright.config.ts'), 'utf8');

    expect(playwright).toContain("name: 'desktop-chromium'");
    expect(playwright).toContain("devices['Desktop Chrome']");
    expect(playwright).toContain("testMatch: '**/routes.spec.ts'");
    expect(playwright).toContain("name: 'mobile-chromium'");
    expect(playwright).toContain("devices['Pixel 7']");
    expect(playwright).toContain("testMatch: '**/mobile.spec.ts'");
  });
});
