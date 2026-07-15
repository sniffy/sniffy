import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';
import { createConnection } from 'node:net';
import { createProfilerWatcher, startDevelopmentEnvironment } from './dev';

async function waitFor(predicate: () => boolean | Promise<boolean>, message: string) {
  const deadline = Date.now() + 10_000;
  while (!(await predicate())) {
    if (Date.now() > deadline) throw new Error(`Timed out waiting for ${message}`);
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 25));
  }
}

function canConnect(port: number) {
  return new Promise<boolean>((resolvePromise) => {
    const socket = createConnection({ host: '127.0.0.1', port });
    socket.once('connect', () => {
      socket.destroy();
      resolvePromise(true);
    });
    socket.once('error', () => resolvePromise(false));
  });
}

describe('frontend development infrastructure', () => {
  test('serves Vite agent, watched profiler, reload routes, and generated preview', async () => {
    const developmentRoot = await mkdtemp(resolve(tmpdir(), 'sniffy-development-test-'));
    try {
      const environment = await startDevelopmentEnvironment({
        ports: [0, 0],
        developmentRoot,
      });
      const [port] = environment.ports;
      const origin = `http://127.0.0.1:${port}`;
      try {
        const [playground, hostile, agent, profiler, reloadClient] = await Promise.all([
          fetch(`${origin}/mock/mock.html`),
          fetch(`${origin}/mock/hostile/nested/page`),
          fetch(`${origin}/agent/`),
          fetch(`${origin}/mock/sniffy/4.0.0/sniffy.min.js`),
          fetch(`${origin}/__sniffy_dev/reload-client.js`),
        ]);
        expect(playground.status).toBe(200);
        expect(await playground.text()).toContain('/__sniffy_dev/reload-client.js');
        expect(hostile.status).toBe(200);
        expect(await agent.text()).toContain('/agent/@vite/client');
        expect(profiler.headers.get('cache-control')).toBe('no-store');
        const profilerSource = await profiler.text();
        expect(profilerSource).toContain('sniffy-profiler');
        expect(profilerSource).toMatch(/sourceMappingURL=data:[^\s]+/);
        expect(profilerSource).not.toMatch(/sourceMappingURL=(?!data:)[^\s]+/);
        expect(reloadClient.status).toBe(200);
        const reloadClientSource = await reloadClient.text();
        expect(reloadClientSource).toContain("fetch('/__sniffy_dev/reload'");
        expect(reloadClientSource).toContain('class ReloadConnectionError');
        expect(reloadClientSource).toContain("error.name === 'AbortError'");
        expect(reloadClientSource).not.toContain('error instanceof TypeError');

        const controller = new AbortController();
        const reload = await fetch(`${origin}/__sniffy_dev/reload`, {
          signal: controller.signal,
        });
        expect(reload.status).toBe(200);
        expect(reload.headers.get('content-type')).toContain('text/event-stream');
        controller.abort();
      } finally {
        await environment.close();
      }
      expect(await canConnect(port)).toBe(false);
    } finally {
      await rm(developmentRoot, { recursive: true, force: true });
    }

    const preview = await startDevelopmentEnvironment({ mode: 'generated', ports: [0, 0] });
    const previewOrigin = `http://127.0.0.1:${preview.ports[0]}`;
    try {
      const [agent, agentSource, agentStyles, profiler, reloadClient] = await Promise.all([
        fetch(`${previewOrigin}/agent/`),
        fetch(`${previewOrigin}/agent/sniffy-agent.js`),
        fetch(`${previewOrigin}/agent/sniffy-agent.css`),
        fetch(`${previewOrigin}/mock/sniffy/4.0.0/sniffy.min.js`),
        fetch(`${previewOrigin}/__sniffy_dev/reload-client.js`),
      ]);
      const agentHtml = await agent.text();
      expect(agentHtml).toContain('./sniffy-agent.js');
      expect(agentHtml).not.toContain('@vite/client');
      expect(agent.headers.get('cache-control')).toBe('no-store');
      expect(agentSource.headers.get('cache-control')).toBe('no-store');
      expect(agentStyles.headers.get('cache-control')).toBe('no-store');
      expect(profiler.status).toBe(200);
      expect(reloadClient.status).toBe(500);
      expect(await reloadClient.json()).toMatchObject({ error: 'Unexpected request' });
    } finally {
      await preview.close();
    }
    expect(await canConnect(preview.ports[0])).toBe(false);
  }, 30_000);

  test('publishes only successful profiler rebuilds and recovers after an error', async () => {
    const directory = await mkdtemp(resolve(tmpdir(), 'sniffy-profiler-watcher-test-'));
    const output = await mkdtemp(resolve(tmpdir(), 'sniffy-profiler-watcher-output-'));
    const entry = resolve(directory, 'entry.js');
    const dependency = resolve(directory, 'message.js');
    await writeFile(
      entry,
      "import { message } from './message.js'; window.testMessage = message;\n",
    );
    await writeFile(dependency, "export const message = 'initial';\n");
    let successfulRebuilds = 0;
    let buildErrors = 0;
    const watcher = await createProfilerWatcher({
      entry,
      outDir: output,
      plugins: [],
      onSuccessfulRebuild: () => successfulRebuilds++,
      onBuildError: () => buildErrors++,
    });
    try {
      await writeFile(dependency, "export const message = 'valid-change';\n");
      await waitFor(
        async () =>
          successfulRebuilds === 1 &&
          (await readFile(resolve(output, 'sniffy.min.js'), 'utf8')).includes('valid-change'),
        'one successful rebuild',
      );

      await writeFile(dependency, 'export const message = ;\n');
      await waitFor(() => buildErrors === 1, 'the build error');
      expect(successfulRebuilds).toBe(1);

      await writeFile(dependency, "export const message = 'recovered';\n");
      await waitFor(
        async () =>
          successfulRebuilds === 2 &&
          (await readFile(resolve(output, 'sniffy.min.js'), 'utf8')).includes('recovered'),
        'recovery rebuild',
      );
    } finally {
      await watcher.close();
      await rm(directory, { recursive: true, force: true });
      await rm(output, { recursive: true, force: true });
    }
  }, 30_000);
});
