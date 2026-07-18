/* global URL, fetch, console */
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, extname, join, normalize, relative, sep } from 'node:path';

const appRoot = dirname(fileURLToPath(import.meta.url));
const root = join(appRoot, 'build-test');
const workspaceRoot = dirname(dirname(appRoot));
const rootPackageJson = JSON.parse(await readFile(join(workspaceRoot, 'package.json'), 'utf8'));
const sitePackageJson = JSON.parse(await readFile(join(appRoot, 'package.json'), 'utf8'));
const uiPackageJson = JSON.parse(
  await readFile(join(workspaceRoot, 'packages/ui/package.json'), 'utf8'),
);
const docusaurusConfig = await readFile(join(appRoot, 'docusaurus.config.ts'), 'utf8');
const customCss = await readFile(join(appRoot, 'src/css/custom.css'), 'utf8');
const requiredRoutes = ['/', '/docs/'];
const reservedRoutes = ['/blog/', '/docs/next/', '/docs/3.1.13/'];
const contentTypes = new Map([
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.css', 'text/css; charset=utf-8'],
]);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function assertWorkspaceContract() {
  assert(
    Array.isArray(rootPackageJson.workspaces) && rootPackageJson.workspaces.includes('apps/*'),
    'root workspace must include apps/* so @sniffy/site stays workspace-bound',
  );
  for (const scriptName of ['site:dev', 'site:build', 'site:test']) {
    const commandName = scriptName.slice('site:'.length);
    assert(
      rootPackageJson.scripts?.[scriptName] === `npm run ${commandName} -w @sniffy/site`,
      `root ${scriptName} must delegate to the @sniffy/site workspace ${commandName} script`,
    );
  }
  assert(
    rootPackageJson.scripts?.build === 'node scripts/build.mjs',
    'root production build command must remain the generated-resource build',
  );
  assert(
    sitePackageJson.scripts?.dev === 'docusaurus start --host 127.0.0.1',
    '@sniffy/site dev script must stay local to the site workspace',
  );
  assert(
    sitePackageJson.scripts?.build === 'docusaurus build',
    '@sniffy/site build script must stay local to the site workspace',
  );
  assert(
    sitePackageJson.scripts?.test ===
      'docusaurus build --out-dir build-test && node smoke-test.mjs',
    '@sniffy/site test script must build the site and run committed smoke assertions',
  );
  assert(
    sitePackageJson.dependencies?.['@sniffy/theme'] === '0.0.0',
    '@sniffy/site must consume the workspace @sniffy/theme package',
  );
  assert(
    sitePackageJson.dependencies?.react === uiPackageJson.dependencies?.react &&
      sitePackageJson.dependencies?.['react-dom'] === uiPackageJson.dependencies?.['react-dom'],
    '@sniffy/site React dependencies must stay aligned with the @sniffy/ui workspace React versions',
  );
  assert(
    !('sockjs' in (rootPackageJson.overrides ?? {})),
    'root overrides must not force the sockjs transitive uuid dependency',
  );
  assert(
    customCss.includes("@import '@sniffy/theme/base.css';") &&
      customCss.includes("@import '@sniffy/theme/dark.css';"),
    'site CSS must import shared @sniffy/theme base and dark entry points',
  );
  assert(
    !/(^|[^-\w])(?:#[0-9a-fA-F]{3,8}|black|white)(?![-\w])/u.test(customCss),
    'site CSS must not duplicate product palette literals',
  );
  assert(docusaurusConfig.includes('blog: false'), 'Docusaurus blog routes must remain disabled');
  assert(
    !/versions\s*:/u.test(docusaurusConfig),
    'Docusaurus historical/versioned docs must remain disabled',
  );
  assert(
    docusaurusConfig.includes('disableSwitch: true'),
    'Docusaurus theme switch must remain disabled until the light theme is implemented',
  );
  console.log('workspace contract assertions passed');
}

function isInsideRoot(candidate) {
  const rel = relative(root, candidate);
  return rel === '' || (!rel.startsWith('..') && !rel.startsWith(sep) && !/^[A-Za-z]:/.test(rel));
}

function routeFile(pathname) {
  const normalized = normalize(decodeURIComponent(pathname)).replace(/^([/\\])+/, '');
  const candidate = pathname.endsWith('/')
    ? join(root, normalized, 'index.html')
    : join(root, normalized);
  return isInsideRoot(candidate) ? candidate : join(root, '404.html');
}

assertWorkspaceContract();

const server = createServer(async (request, response) => {
  const url = new URL(request.url ?? '/', 'http://127.0.0.1');
  let file = routeFile(url.pathname);
  let status = 200;
  if (!existsSync(file)) {
    file = join(root, '404.html');
    status = 404;
  }
  try {
    const body = await readFile(file);
    response.writeHead(status, {
      'content-type': contentTypes.get(extname(file)) ?? 'application/octet-stream',
    });
    response.end(body);
  } catch (error) {
    console.error('site smoke static server failed to read a route fixture', error);
    response.writeHead(500, { 'content-type': 'text/plain; charset=utf-8' });
    response.end('Internal Server Error');
  }
});

await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const { port } = server.address();
try {
  for (const route of requiredRoutes) {
    const response = await fetch(`http://127.0.0.1:${port}${route}`);
    if (response.status !== 200)
      throw new Error(`${route} returned ${response.status}, expected 200`);
    const html = await response.text();
    if (!html.includes('Sniffy')) throw new Error(`${route} did not render Sniffy content`);
    console.log(`${route} rendered with HTTP 200`);
  }
  for (const route of reservedRoutes) {
    const response = await fetch(`http://127.0.0.1:${port}${route}`);
    if (response.status !== 404)
      throw new Error(`${route} returned ${response.status}, expected 404`);
    console.log(`${route} remains reserved with HTTP 404`);
  }
} finally {
  await new Promise((resolve, reject) =>
    server.close((error) => (error ? reject(error) : resolve())),
  );
}
