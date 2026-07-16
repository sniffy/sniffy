import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import type { Response } from 'express';
import { readFile, rm, mkdir } from 'node:fs/promises';
import { createServer as createHttpServer, type Server } from 'node:http';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import {
  build,
  createServer as createViteServer,
  type PluginOption,
  type ViteDevServer,
} from 'vite';
import { createPlaygroundApp } from '../apps/playground/src/server';

const workspace = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repository = resolve(workspace, '..');
const temporaryRoot = resolve(workspace, '.tmp/dev');
const profilerOutput = resolve(temporaryRoot, 'profiler');
const generatedProfiler = resolve(
  repository,
  'sniffy-web-common/src/main/resources/io/sniffy/ui/sniffy.min.js',
);
const generatedAgent = resolve(repository, 'sniffy/src/main/resources/web');

export type DevelopmentMode = 'all' | 'agent' | 'profiler' | 'generated';

export const developmentApplications = {
  profiler: {
    kind: 'injected-iife',
    entry: resolve(workspace, 'apps/profiler/src/index.tsx'),
    output: resolve(profilerOutput, 'sniffy.min.js'),
    update: 'host-page-reload',
  },
  agent: {
    kind: 'vite',
    root: resolve(workspace, 'apps/agent'),
    base: '/agent/',
    update: 'hmr',
  },
} as const;

class ReloadChannel {
  private readonly responses = new Set<Response>();

  subscribe(response: Response) {
    this.responses.add(response);
    return () => this.responses.delete(response);
  }

  publish() {
    for (const response of this.responses) response.write('event: profiler\ndata: reload\n\n');
  }

  close() {
    for (const response of this.responses) response.end();
    this.responses.clear();
  }
}

interface ProfilerWatcher {
  close(): Promise<void>;
}

export async function createProfilerWatcher({
  entry = developmentApplications.profiler.entry,
  outDir = profilerOutput,
  plugins = [react(), tailwindcss()],
  onSuccessfulRebuild,
  onBuildError,
}: {
  entry?: string;
  outDir?: string;
  plugins?: PluginOption[];
  onSuccessfulRebuild?: () => void;
  onBuildError?: (error: Error) => void;
} = {}): Promise<ProfilerWatcher> {
  let initialBuild = true;
  let cycleFailed = false;
  let buildFailed = false;
  let lastSuccessfulOutput: Buffer | undefined;
  let eventQueue = Promise.resolve();
  let resolveReady: (() => void) | undefined;
  let rejectReady: ((error: Error) => void) | undefined;
  const ready = new Promise<void>((resolvePromise, rejectPromise) => {
    resolveReady = resolvePromise;
    rejectReady = rejectPromise;
  });
  // Vite's agent server sets NODE_ENV=development for Fast Refresh. Resolve this separate
  // watched build under production semantics so the injected IIFE keeps the same JSX runtime,
  // effect timing, and geometry as the generated artifact.
  const previousNodeEnvironment = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';
  let result;
  try {
    result = await build({
      root: workspace,
      configFile: false,
      plugins,
      define: { 'process.env.NODE_ENV': JSON.stringify('production') },
      build: {
        outDir,
        emptyOutDir: true,
        minify: false,
        sourcemap: 'inline',
        cssCodeSplit: false,
        target: ['es2020', 'chrome90', 'firefox88', 'safari14'],
        lib: {
          entry,
          name: 'SniffyProfiler',
          formats: ['iife'],
          fileName: () => 'sniffy.min.js',
        },
        rollupOptions: { output: { assetFileNames: '[name][extname]' } },
        watch: {
          clearScreen: false,
          exclude: [
            '**/.tmp/**',
            '**/apps/agent/**',
            '**/apps/playground/**',
            '**/node_modules/**',
            '**/playwright-report/**',
            '**/scripts/**',
            '**/storybook-static/**',
            '**/test-results/**',
            '**/tests/**',
          ],
        },
      },
      logLevel: 'warn',
    });
  } finally {
    if (previousNodeEnvironment === undefined) delete process.env.NODE_ENV;
    else process.env.NODE_ENV = previousNodeEnvironment;
  }
  if (!('on' in result)) throw new Error('Profiler development build did not create a watcher');
  const reportBuildError = (message: string, error: Error) => {
    if (!buildFailed) {
      buildFailed = true;
      console.error(message, error);
      onBuildError?.(error);
    }
    if (initialBuild) rejectReady?.(error);
  };
  result.on('event', (event) => {
    if (event.code === 'START') cycleFailed = false;
    else if (event.code === 'END' && !cycleFailed) {
      eventQueue = eventQueue.then(async () => {
        try {
          const output = await readFile(resolve(outDir, 'sniffy.min.js'));
          buildFailed = false;
          const changed = !lastSuccessfulOutput?.equals(output);
          lastSuccessfulOutput = output;
          if (initialBuild) {
            initialBuild = false;
            resolveReady?.();
            console.log('[profiler] development IIFE ready');
          } else if (changed) {
            console.log('[profiler] rebuilt; reloading playground pages');
            onSuccessfulRebuild?.();
          }
        } catch (error) {
          const buildError = error instanceof Error ? error : new Error(String(error));
          reportBuildError('[profiler] unable to publish build:', buildError);
        }
      });
    } else if (event.code === 'ERROR') {
      cycleFailed = true;
      eventQueue = eventQueue.then(() => reportBuildError('[profiler] build failed:', event.error));
    }
  });
  try {
    await ready;
  } catch (error) {
    await result.close();
    throw error;
  }
  return {
    async close() {
      await result.close();
      await eventQueue;
    },
  };
}

function listen(server: Server, port: number) {
  return new Promise<number>((resolvePromise, rejectPromise) => {
    const onError = (error: Error) => rejectPromise(error);
    server.once('error', onError);
    server.listen(port, '127.0.0.1', () => {
      server.off('error', onError);
      const address = server.address();
      if (!address || typeof address === 'string') rejectPromise(new Error('Missing server port'));
      else resolvePromise(address.port);
    });
  });
}

function closeServer(server: Server) {
  return new Promise<void>((resolvePromise, rejectPromise) => {
    server.close((error) => (error ? rejectPromise(error) : resolvePromise()));
    server.closeAllConnections();
  });
}

export async function startDevelopmentEnvironment({
  mode = 'all',
  ports = [3000, 3001],
  developmentRoot = temporaryRoot,
}: {
  mode?: DevelopmentMode;
  ports?: [number, number];
  developmentRoot?: string;
} = {}) {
  const usesProfilerWatcher = mode === 'all' || mode === 'profiler';
  const usesAgentVite = mode === 'all' || mode === 'agent';
  const reloadChannel = new ReloadChannel();
  const developmentProfilerOutput = resolve(developmentRoot, 'profiler');
  const developmentProfilerFile = resolve(developmentProfilerOutput, 'sniffy.min.js');
  const primaryServer = createHttpServer();
  let agentVite: ViteDevServer | undefined;
  let profilerWatcher: ProfilerWatcher | undefined;
  const listeningServers: Server[] = [];
  let stopped = false;

  try {
    if (usesAgentVite) {
      agentVite = await createViteServer({
        root: developmentApplications.agent.root,
        base: developmentApplications.agent.base,
        configFile: false,
        appType: 'spa',
        plugins: [react(), tailwindcss()],
        server: {
          middlewareMode: true,
          headers: { 'Cache-Control': 'no-store' },
          hmr: { server: primaryServer },
        },
        logLevel: 'warn',
      });
    }
    if (usesProfilerWatcher) {
      await rm(developmentRoot, { recursive: true, force: true });
      await mkdir(developmentProfilerOutput, { recursive: true });
      profilerWatcher = await createProfilerWatcher({
        outDir: developmentProfilerOutput,
        onSuccessfulRebuild: () => reloadChannel.publish(),
      });
    }
    const app = createPlaygroundApp({
      profilerFile: usesProfilerWatcher ? developmentProfilerFile : generatedProfiler,
      agentMiddleware: agentVite?.middlewares,
      agentDirectory: usesAgentVite ? undefined : generatedAgent,
      reloadClient: usesProfilerWatcher,
      subscribeToReload: (response) => reloadChannel.subscribe(response),
    });
    primaryServer.on('request', app);
    const secondaryServer = createHttpServer(app);
    const primaryPort = await listen(primaryServer, ports[0]);
    listeningServers.push(primaryServer);
    const secondaryPort = await listen(secondaryServer, ports[1]);
    listeningServers.push(secondaryServer);

    const modeLabel = mode === 'generated' ? 'generated-resource preview' : `${mode} development`;
    console.log(`Sniffy playground (${modeLabel}):`);
    console.log(`  legacy:  http://127.0.0.1:${primaryPort}/mock/mock.html`);
    console.log(`  hostile: http://127.0.0.1:${primaryPort}/mock/hostile/nested/page`);
    console.log(
      `  agent:   http://127.0.0.1:${primaryPort}/agent/ (${usesAgentVite ? 'Vite HMR' : 'generated resources'})`,
    );
    console.log(`  CORS:    http://127.0.0.1:${secondaryPort}/`);
    if (usesProfilerWatcher)
      console.log(`  profiler: ${developmentProfilerFile} (watch + full-page reload)`);

    return {
      ports: [primaryPort, secondaryPort] as const,
      async close() {
        if (stopped) return;
        stopped = true;
        reloadChannel.close();
        await Promise.allSettled(listeningServers.map(closeServer));
        await Promise.allSettled([agentVite?.close(), profilerWatcher?.close()]);
      },
    };
  } catch (error) {
    reloadChannel.close();
    await Promise.allSettled(listeningServers.map(closeServer));
    await Promise.allSettled([agentVite?.close(), profilerWatcher?.close()]);
    throw error;
  }
}

function requestedMode(): DevelopmentMode {
  const argument = process.argv.find((value) => value.startsWith('--mode='));
  const mode = argument?.slice('--mode='.length) ?? 'all';
  if (mode === 'all' || mode === 'agent' || mode === 'profiler' || mode === 'generated')
    return mode;
  throw new Error(`Unknown development mode: ${mode}`);
}

async function main() {
  const environment = await startDevelopmentEnvironment({ mode: requestedMode() });
  let stopping = false;
  const stop = async () => {
    if (stopping) return;
    stopping = true;
    console.log('\nStopping Sniffy frontend development services...');
    await environment.close();
  };
  process.once('SIGINT', () => void stop());
  process.once('SIGTERM', () => void stop());
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href)
  main().catch((error) => {
    console.error('Unable to start Sniffy frontend development environment:', error);
    process.exitCode = 1;
  });
