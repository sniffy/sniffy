import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const workspace = resolve(site, '../..');
const repository = resolve(workspace, '..');
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
      'package:site-artifact': 'node apps/site/scripts/package-artifact.mjs',
      'test:site':
        'npm run build:site && npm run verify:site-search-index && vitest run --project unit apps/site/src/site.test.ts apps/site/src/homepage.test.tsx apps/site/src/use-case.test.tsx apps/site/src/sql-profiling.test.tsx apps/site/src/source-snippets.test.ts apps/site/src/product-version.test.ts apps/site/src/documentation.test.ts apps/site/src/deterministic-search-index.test.ts apps/site/src/theme/SearchBar/index.test.tsx apps/site/src/theme/SearchBar/search-index.test.ts apps/site/src/theme/NavbarItem/GitHubStarsNavbarItem.test.tsx && npm test --workspace @sniffy/site',
      'verify:site-search-index': 'node apps/site/scripts/verify-search-index.mjs',
      'verify:site-artifact': 'node apps/site/scripts/verify-artifact.mjs',
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
    expect(styles).toContain('--sniffy-site-header-height:');
    expect(styles).not.toMatch(/#[\da-f]{3,8}\b/i);

    for (const theme of ['base', 'dark', 'light']) {
      expect(
        readFileSync(resolve(workspace, `packages/theme/src/${theme}.css`), 'utf8'),
      ).not.toContain('--sniffy-site-');
    }
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
    expect(config).toContain("favicon: 'favicon.ico'");
    expect(config).toContain("image: 'img/brand/sniffy-social.svg'");
    expect(config).toContain("label: 'Documentation'");
    expect(config).toContain("label: 'Use cases'");
    expect(config).toContain("type: 'custom-githubStars'");
    expect(config).toContain('deterministicSearchPlugin');
    expect(config).toContain('indexDocs: true');
    expect(config).toContain('indexBlog: false');
    expect(config).toContain('indexPages: false');
    expect(config).not.toContain('sniffy-search-item');
    expect(config).not.toMatch(/navbar:\s*\{[\s\S]*?logo:\s*\{/);
    expect(config).toContain("routeBasePath: 'docs'");
    expect(config).toContain("lastVersion: 'current'");
    expect(config).toContain('blog: false');
    expect(config).not.toMatch(/versioned_(docs|sidebars)/);
  });

  it('keeps established and wordmark assets with the narrow 404 wrapper inside the site', () => {
    for (const path of [
      'static/img/brand/sniffy-social.svg',
      'src/theme/NotFound/Content/index.tsx',
    ]) {
      expect(readFileSync(resolve(site, path), 'utf8')).not.toHaveLength(0);
    }
    expect(readFileSync(resolve(site, 'static/favicon.ico')).byteLength).toBeGreaterThan(0);
    expect(existsSync(resolve(site, 'static/img/brand/sniffy-mark.svg'))).toBe(false);
    expect(existsSync(resolve(site, 'static/img/brand/sniffy-mark-dark.svg'))).toBe(false);

    const notFound = readFileSync(resolve(site, 'src/theme/NotFound/Content/index.tsx'), 'utf8');
    expect(notFound).toContain('This trail went cold.');
    expect(notFound).toContain('aria-label="Page recovery"');
  });

  it('keeps the production base URL and packages a dependency-free cross-platform preview', () => {
    const config = readFileSync(resolve(site, 'docusaurus.config.ts'), 'utf8');
    const readme = readFileSync(resolve(site, 'artifact/README.md'), 'utf8');
    const preview = readFileSync(resolve(site, 'scripts/preview.mjs'), 'utf8');
    const verification = readFileSync(resolve(site, 'scripts/verify-artifact.mjs'), 'utf8');

    expect(config).toContain("baseUrl: '/'");
    expect(readFileSync(resolve(site, 'artifact/.node-version'), 'utf8').trim()).toBe('24.18.0');
    expect(readme).toContain('node preview.mjs');
    expect(readme).toContain('macOS or Linux');
    expect(readme).toContain('Windows');
    expect(readme).toContain('No dependency installation or repository checkout is required.');
    expect(preview).toContain("options = { host: '127.0.0.1', port: 4173 }");
    expect(preview).toContain("resolve(root, '404.html')");
    expect(preview).not.toMatch(/from ['"][^n.]/);
    expect(verification).toContain("import { chromium } from '@playwright/test';");
    expect(verification).toContain("page.goto(url, { waitUntil: 'networkidle' })");
    expect(verification).toContain('Search current Sniffy documentation');
    expect(verification).toContain('traffic capture');
    expect(verification).toContain('use-cases/database-query-testing/');
    expect(verification).toContain('use-cases/sql-profiling/');
    expect(verification).toContain('See the database work behind each request.');
    expect(verification).toContain('Make database behavior part of the test contract.');
    expect(verification).toContain('This trail went cold.');
    expect(verification).toContain('return route.fulfill({ json: {} })');
    expect(verification).toContain("getByRole('link', { name: 'Sniffy GitHub repository' })");
    expect(verification).toContain("getByTestId('github-star-count')");
    expect(verification).toContain("message.type() === 'error'");
  });

  it('uses a pinned local search indexer with a Sniffy-owned resilient search dialog', () => {
    const packageJson = readJson(resolve(site, 'package.json'));
    const config = readFileSync(resolve(site, 'docusaurus.config.ts'), 'utf8');
    const search = readFileSync(resolve(site, 'src/theme/SearchBar/index.tsx'), 'utf8');
    const index = readFileSync(resolve(site, 'src/theme/SearchBar/search-index.ts'), 'utf8');
    const deterministicIndex = readFileSync(
      resolve(site, 'src/deterministic-search-index.ts'),
      'utf8',
    );
    const verifier = readFileSync(resolve(site, 'scripts/verify-search-index.mjs'), 'utf8');

    expect(packageJson.dependencies).toMatchObject({
      '@cmfcmf/docusaurus-search-local': '2.0.1',
      lunr: '2.3.9',
    });
    expect(packageJson.devDependencies).toMatchObject({ '@types/lunr': '2.3.7' });
    expect(config).toContain('maxSearchResults: 8');
    expect(search).toContain("currentDocsSearchTag = 'docs-default-current'");
    expect(search).toContain('aria-keyshortcuts="Meta+K Control+K"');
    expect(search).toContain('Search is temporarily unavailable.');
    expect(search).toContain('history.push(result.url)');
    expect(deterministicIndex).toContain('normalizeGeneratedSearchIndexes(props.outDir)');
    expect(index).toContain('Array.from(new Set(tags.filter(Boolean)))');
    expect(index).toContain('Array.from(bestByUrl.values())');
    expect(index).toContain("!document.sectionRoute.startsWith('/docs/')");
    expect(deterministicIndex).toContain('createDevelopmentSearchIndex');
    expect(deterministicIndex).toContain('async allContentLoaded(props)');
    expect(verifier).toContain("'SSL TLS traffic decryption'");
    expect(verifier).toContain('/docs/network/traffic-capture/#ssltls-traffic-decryption');
  });
});

describe('website validation workflow contract', () => {
  const workflow = readFileSync(resolve(repository, '.github/workflows/pr.yml'), 'utf8');
  const websiteJob = workflow.match(/\n {2}website:\n([\s\S]*?)\n {2}smoke-test:\n/)?.[1];

  it('runs as an always-on job in the existing pull-request workflow', () => {
    expect(websiteJob).toBeDefined();
    expect(workflow).toContain('workflow_dispatch:');
    expect(workflow).toMatch(/push:\n\s+branches:\n\s+- develop/);
    expect(workflow).toMatch(/pull_request:\n\s+branches-ignore:/);
    expect(workflow).not.toMatch(/^\s+paths:/m);
  });

  it('keeps validation read-only, explicit, and independent from release publication', () => {
    for (const step of [
      'Install locked dependencies',
      'Lint website sources',
      'Check website formatting',
      'Typecheck frontend workspace',
      'Test website contracts',
      'Build website with broken link and image validation',
      'Verify generated documentation search index',
      'Test desktop and mobile website navigation',
      'Package reviewable website artifact',
      'Verify downloaded website preview',
      'Upload complete reviewable website',
    ]) {
      expect(websiteJob).toContain(`- name: ${step}`);
    }
    const contentsPermission = ['content', 's: read'].join('');
    expect(websiteJob).toContain(`permissions:\n      ${contentsPermission}`);
    expect(websiteJob).not.toContain('secrets.');
    expect(websiteJob).not.toMatch(/\bmvn\b/);
    expect(websiteJob).not.toMatch(/npm run (?:build|check:generated|check:bundle)(?:\s|$)/);
    expect(websiteJob).not.toMatch(/deploy|pages/i);
  });

  it('uploads the complete static build while preserving generated-resource checks elsewhere', () => {
    expect(websiteJob).toContain('uses: actions/upload-artifact@v7');
    expect(websiteJob).toContain(
      'name: sniffy-website-${{ github.event.pull_request.head.sha || github.sha }}',
    );
    expect(websiteJob).toContain('path: sniffy-ui/apps/site/build/');
    expect(websiteJob).toContain('if-no-files-found: error');
    expect(websiteJob).toContain('include-hidden-files: true');
    expect(websiteJob).toContain('run: npm run package:site-artifact');
    expect(websiteJob).toContain('run: npm run verify:site-search-index');
    expect(websiteJob).toContain('run: npm run verify:site-artifact');
    expect(workflow).toContain('run: npm run build');
    expect(workflow).toContain('run: npm run check:generated');
    expect(workflow).toContain('run: npm run check:bundle');
  });

  it('runs the website browser suite at desktop and mobile viewports', () => {
    const playwright = readFileSync(resolve(site, 'playwright.config.ts'), 'utf8');

    expect(playwright).toContain("name: 'desktop-chromium'");
    expect(playwright).toContain("devices['Desktop Chrome']");
    expect(playwright).toContain("testMatch: '**/routes.spec.ts'");
    expect(playwright).toContain("name: 'sql-profiling-chromium'");
    expect(playwright).toContain("name: 'mobile-chromium'");
    expect(playwright).toContain("devices['Pixel 7']");
    expect(playwright).toContain("testMatch: '**/mobile.spec.ts'");
    expect(playwright).toContain("'**/dev-search.spec.ts'");
    expect(playwright).toContain("snapshotPathTemplate: '{testDir}/visual-baselines/{arg}{ext}'");
  });
});
