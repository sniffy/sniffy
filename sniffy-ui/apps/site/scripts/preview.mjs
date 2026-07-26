import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import { createServer } from 'node:http';
import { dirname, extname, resolve, sep } from 'node:path';
import process from 'node:process';
import { fileURLToPath, URL } from 'node:url';

const mimeTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.gif', 'image/gif'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.jpeg', 'image/jpeg'],
  ['.jpg', 'image/jpeg'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.map', 'application/json; charset=utf-8'],
  ['.mjs', 'text/javascript; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.txt', 'text/plain; charset=utf-8'],
  ['.webp', 'image/webp'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2'],
  ['.xml', 'application/xml; charset=utf-8'],
]);

const options = {
  baseUrl: '/',
  host: '127.0.0.1',
  port: 4173,
  root: dirname(fileURLToPath(import.meta.url)),
};

for (let index = 2; index < process.argv.length; index += 1) {
  const argument = process.argv[index];
  const value = process.argv[index + 1];

  if (argument === '--base-url' && value) {
    options.baseUrl = value;
    index += 1;
  } else if (argument === '--host' && value) {
    options.host = value;
    index += 1;
  } else if (argument === '--port' && value) {
    options.port = Number(value);
    index += 1;
  } else if (argument === '--root' && value) {
    options.root = value;
    index += 1;
  } else {
    throw new Error(`Unknown or incomplete argument: ${argument}`);
  }
}

if (!Number.isInteger(options.port) || options.port < 0 || options.port > 65_535) {
  throw new Error(`Invalid port: ${options.port}`);
}
if (
  !options.baseUrl.startsWith('/') ||
  !options.baseUrl.endsWith('/') ||
  options.baseUrl.includes('//')
) {
  throw new Error(`Invalid base URL: ${options.baseUrl}`);
}

const root = resolve(options.root);

const sendText = (response, statusCode, message) => {
  response.writeHead(statusCode, { 'Content-Type': 'text/plain; charset=utf-8' });
  response.end(`${message}\n`);
};

const sendFile = (request, response, file, statusCode = 200) => {
  response.writeHead(statusCode, {
    'Cache-Control': 'no-store',
    'Content-Length': file.stat.size,
    'Content-Type': mimeTypes.get(extname(file.path).toLowerCase()) ?? 'application/octet-stream',
  });

  if (request.method === 'HEAD') {
    response.end();
    return;
  }

  createReadStream(file.path).pipe(response);
};

const sendNotFound = async (request, response) => {
  try {
    const path = resolve(root, '404.html');
    const fileStat = await stat(path);
    if (!fileStat.isFile()) {
      sendText(response, 404, 'Not Found');
      return;
    }
    sendFile(request, response, { path, stat: fileStat }, 404);
  } catch {
    sendText(response, 404, 'Not Found');
  }
};

const resolveRequestPath = async (requestUrl) => {
  const url = new URL(requestUrl, 'http://localhost');
  const decodedPath = decodeURIComponent(url.pathname).replaceAll('\\', '/');
  if (!decodedPath.startsWith(options.baseUrl)) {
    return undefined;
  }
  const relativePath =
    decodedPath.slice(options.baseUrl.length).replace(/^\/+/, '') || 'index.html';
  let target = resolve(root, relativePath);

  if (target !== root && !target.startsWith(`${root}${sep}`)) {
    return undefined;
  }

  const targetStat = await stat(target);
  if (targetStat.isDirectory()) {
    target = resolve(target, 'index.html');
  }

  return { path: target, stat: await stat(target) };
};

const server = createServer(async (request, response) => {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.setHeader('Allow', 'GET, HEAD');
    sendText(response, 405, 'Method Not Allowed');
    return;
  }

  try {
    const file = await resolveRequestPath(request.url ?? '/');
    if (!file?.stat.isFile()) {
      await sendNotFound(request, response);
      return;
    }
    sendFile(request, response, file);
  } catch (error) {
    if (error instanceof URIError) {
      sendText(response, 400, 'Bad Request');
      return;
    }
    if (error && typeof error === 'object' && 'code' in error && error.code === 'ENOENT') {
      await sendNotFound(request, response);
      return;
    }
    process.stderr.write(`${String(error)}\n`);
    sendText(response, 500, 'Internal Server Error');
  }
});

server.listen(options.port, options.host, () => {
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : options.port;
  process.stdout.write(`Sniffy website preview: http://${options.host}:${port}/\n`);
  process.stdout.write('Press Ctrl+C to stop.\n');
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
