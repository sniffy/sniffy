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
  assert(
    rootPackageJson.scripts?.['site:test'] === 'npm run test -w @sniffy/site',
    'root site:test must delegate to the @sniffy/site workspace test script',
  );
  assert(
    rootPackageJson.scripts?.build === 'node scripts/build.mjs',
    'root production build command must remain the generated-resource build',
  );
  assert(
    sitePackageJson.dependencies?.['@sniffy/theme'] === '0.0.0',
    '@sniffy/site must consume the workspace @sniffy/theme package',
  );
  assert(
    !('sockjs' in (rootPackageJson.overrides ?? {})),
    'root overrides must not force the sockjs transitive uuid dependency',
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
    response.writeHead(500, { 'content-type': 'text/plain; charset=utf-8' });
    response.end(String(error));
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
