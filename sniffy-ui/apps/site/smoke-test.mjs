/* global URL, fetch, console */
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';

const root = new URL('./build-test/', import.meta.url).pathname;
const requiredRoutes = ['/', '/docs/'];
const reservedRoutes = ['/blog/', '/docs/next/', '/docs/3.1.13/'];
const contentTypes = new Map([
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.css', 'text/css; charset=utf-8'],
]);

function routeFile(pathname) {
  const normalized = normalize(pathname).replace(/^([/\\])+/, '');
  const candidate = pathname.endsWith('/')
    ? join(root, normalized, 'index.html')
    : join(root, normalized);
  return candidate.startsWith(root) ? candidate : join(root, '404.html');
}

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
