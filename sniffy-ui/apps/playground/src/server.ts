import cors from 'cors';
import express, { type Request, type Response } from 'express';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { longStack, registryFixture, requestFixture, topSqlFixture } from '@sniffy/fixtures';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../../..');
const profilerFile = resolve(
  root,
  'sniffy-web-common/src/main/resources/io/sniffy/ui/sniffy.min.js',
);
const agentDirectory = resolve(root, 'sniffy/src/main/resources/web');
const app = express();
app.use(
  cors({
    origin: true,
    credentials: true,
    exposedHeaders: ['Sniffy-Sql-Queries', 'Sniffy-Request-Details', 'Sniffy-Time-To-First-Byte'],
  }),
);
app.use(express.text({ type: '*/*' }));

let persistent = false;
let topSqlResetPending = false;
const registry = structuredClone(registryFixture);

function sendAjax(details: string, sqlQueries = 2) {
  return (_request: Request, response: Response) =>
    response
      .set({
        'Sniffy-Sql-Queries': String(sqlQueries),
        'Sniffy-Time-To-First-Byte': '21',
        'Sniffy-Request-Details': details,
      })
      .json({ foo: 'bar', baz: 42 });
}

app.get('/mock/ajax.json', sendAjax('request/a54b32e7-b94b-450b-b145-0cf62270d32a'));
app.get(
  '/mock/path/subpath/ajax.json',
  sendAjax('../../request/a54b32e7-b94b-450b-b145-0cf62270d32a'),
);
const noTrailingSlashRequestId = 'notrailingslash-a54b32e7-b94b-450b-b145-0cf62270d32a';
app.get('/mock/notrailingslash', sendAjax(`./request/${noTrailingSlashRequestId}`));
app.get('/mock/204.json', sendAjax('request/b43a32e7-b94b-450b-b145-0cf62270d32a', 1));

const requestIds = new Set([
  'a54b32e7-b94b-450b-b145-0cf62270d32a',
  'b43a32e7-b94b-450b-b145-0cf62270d32a',
  'ID',
  'empty',
  'loading',
  'error',
  'many',
  noTrailingSlashRequestId,
]);
app.get(/\/request\/([^/]+)$/, async (request, response) => {
  const id = String(request.params[0]);
  const exactPath = request.path;
  const validPath =
    id === noTrailingSlashRequestId
      ? exactPath === `/mock/notrailingslash/request/${noTrailingSlashRequestId}`
      : requestIds.has(id);
  if (!validPath) {
    console.error(`Unexpected request fixture: ${id}`);
    response.status(500).json({ error: 'Unexpected request fixture', id, path: exactPath });
    return;
  }
  if (id === 'loading') await new Promise((resolvePromise) => setTimeout(resolvePromise, 1_500));
  if (id === 'error') {
    response.status(503).json({ error: 'Intentional playground error' });
    return;
  }
  if (id === 'empty' || id === 'b43a32e7-b94b-450b-b145-0cf62270d32a') {
    response.status(200).send();
    return;
  }
  if (id === 'many') {
    response.json({
      ...requestFixture,
      executedQueries: Array.from({ length: 36 }, (_, index) => ({
        ...requestFixture.executedQueries?.[0],
        query: `select * from playground_rows where scenario_id = ${index}`,
      })),
    });
    return;
  }
  response.json(requestFixture);
});

app.get(/\/connectionregistry\/$/, (_request, response) =>
  response.json({ ...registry, persistent }),
);
app.get(/\/topsql\/$/, (_request, response) => {
  response.json(topSqlResetPending ? [] : topSqlFixture);
  topSqlResetPending = false;
});
app.delete(/\/topsql\/$/, (_request, response) => {
  topSqlResetPending = true;
  response.sendStatus(201);
});
app.all(/\/connectionregistry\/persistent\/$/, (request, response) => {
  persistent = request.method === 'POST';
  response.sendStatus(201);
});
app.post(/\/connectionregistry\/socket\/([^/]+)\/([^/]+)$/, (request, response) => {
  const host = decodeURIComponent(decodeURIComponent(String(request.params[0])));
  const port = decodeURIComponent(decodeURIComponent(String(request.params[1])));
  const item = registry.sockets.find(
    (candidate) => String(candidate.host) === host && String(candidate.port) === port,
  );
  if (item) item.status = Number(request.body);
  response.sendStatus(201);
});
app.delete(/\/connectionregistry\/socket\/([^/]+)\/([^/]+)$/, (request, response) => {
  const host = decodeURIComponent(decodeURIComponent(String(request.params[0])));
  const port = decodeURIComponent(decodeURIComponent(String(request.params[1])));
  const item = registry.sockets.find(
    (candidate) => String(candidate.host) === host && String(candidate.port) === port,
  );
  if (item) item.status = -1;
  response.sendStatus(201);
});
app.post(/\/connectionregistry\/datasource\/([^/]+)\/([^/]+)$/, (request, response) => {
  const url = decodeURIComponent(decodeURIComponent(String(request.params[0])));
  const user = decodeURIComponent(decodeURIComponent(String(request.params[1])));
  const item = registry.dataSources.find(
    (candidate) => candidate.url === url && candidate.userName === user,
  );
  if (item) item.status = Number(request.body);
  response.sendStatus(201);
});
app.delete(/\/connectionregistry\/datasource\/([^/]+)\/([^/]+)$/, (request, response) => {
  const url = decodeURIComponent(decodeURIComponent(String(request.params[0])));
  const user = decodeURIComponent(decodeURIComponent(String(request.params[1])));
  const item = registry.dataSources.find(
    (candidate) => candidate.url === url && candidate.userName === user,
  );
  if (item) item.status = -1;
  response.sendStatus(201);
});

app.get(/\/sniffy\.min\.js$/, async (_request, response) =>
  response
    .type('application/javascript')
    .set('Cache-Control', 'public, max-age=31536000')
    .send(await readFile(profilerFile, 'utf8')),
);
app.use('/agent', express.static(agentDirectory, { index: 'index.html' }));

function hostPage({
  scriptOrigin = '',
  hostile = false,
  nested = false,
  requestId = 'a54b32e7-b94b-450b-b145-0cf62270d32a',
  rootFontSize = 48,
}: {
  scriptOrigin?: string;
  hostile?: boolean;
  nested?: boolean;
  requestId?: string;
  rootFontSize?: number;
}) {
  const prefix = nested ? '../../../' : './';
  const script = `${scriptOrigin}${prefix}sniffy/4.0.0/sniffy.min.js`;
  return `<!doctype html><html><head><meta charset="utf-8"><title>Sniffy playground</title>${hostile ? `<style>html{font-size:${rootFontSize}px!important} *{all:unset!important;color:fuchsia!important} sniffy-profiler{position:static!important;z-index:-999!important} body{background:lime!important} #host-card{position:fixed;z-index:2147483647}</style>` : '<style>body{font:16px system-ui;background:#f5f7fa;color:#172033;padding:2rem}</style>'}</head><body><main id="host-card"><h1>Host application</h1><p id="host-status">Host styles and behavior stay independent.</p><button id="host-xhr">Run application XHR</button></main><script>window.hostHandlerCalls=0;document.getElementById('host-xhr').onclick=function(){var xhr=new XMLHttpRequest();xhr.onreadystatechange=function(){if(xhr.readyState===4){window.hostHandlerCalls++;document.getElementById('host-status').dataset.response=xhr.status}};xhr.open('GET','${nested ? 'ajax.json' : '/mock/ajax.json'}');xhr.send()};</script><script id="sniffy-header" type="application/javascript" data-request-id="${requestId}" data-request-method="GET" data-response-code="200" src="${script}"></script><data id="sniffy" data-sql-queries="2" data-server-time="84"></data><pre id="long-content">${longStack}</pre></body></html>`;
}

app.get('/mock/mock.html', (_request, response) => response.type('html').send(hostPage({})));
app.get('/mock/path/subpath/page', (_request, response) =>
  response.type('html').send(hostPage({ nested: true })),
);
app.get('/mock/hostile/nested/page', (_request, response) =>
  response.type('html').send(hostPage({ nested: true, hostile: true })),
);
for (const rootFontSize of [10, 16, 32, 48])
  app.get(`/mock/hostile/root-${rootFontSize}/page`, (_request, response) =>
    response.type('html').send(hostPage({ nested: true, hostile: true, rootFontSize })),
  );
app.get('/mock/cors/page', (_request, response) =>
  response.type('html').send(hostPage({ scriptOrigin: 'http://127.0.0.1:3001/' })),
);
for (const scenario of ['empty', 'loading', 'error', 'many'])
  app.get(`/mock/scenarios/${scenario}`, (_request, response) =>
    response.type('html').send(hostPage({ requestId: scenario })),
  );
app.get('/favicon.ico', (_request, response) => response.sendStatus(204));
app.use((request, response) => {
  console.error(`Unexpected request: ${request.method} ${request.originalUrl}`);
  response
    .status(500)
    .json({ error: 'Unexpected request', method: request.method, url: request.originalUrl });
});

const servers = [3000, 3001].map((port) =>
  app.listen(port, '127.0.0.1', () => {
    if (port === 3000) {
      console.log('Sniffy playground:');
      console.log('  legacy:  http://127.0.0.1:3000/mock/mock.html');
      console.log('  hostile: http://127.0.0.1:3000/mock/hostile/nested/page');
      console.log('  agent:   http://127.0.0.1:3000/agent/');
    }
  }),
);
process.on('SIGTERM', () => servers.forEach((server) => server.close()));
