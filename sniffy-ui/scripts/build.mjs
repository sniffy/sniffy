import { build } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { cp, mkdir, mkdtemp, readFile, rename, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const workspace = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repository = resolve(workspace, '..');
const generatedRoot = process.env.SNIFFY_GENERATED_ROOT
  ? resolve(process.env.SNIFFY_GENERATED_ROOT)
  : repository;
const scratch = await mkdtemp(resolve(tmpdir(), 'sniffy-ui-build-'));
const profilerDestination = resolve(
  generatedRoot,
  'sniffy-web-common/src/main/resources/io/sniffy/ui',
);
const agentDestination = resolve(generatedRoot, 'sniffy/src/main/resources/web');
process.env.NODE_ENV = 'production';

async function normalizeSourceMap(mapPath) {
  const map = JSON.parse(await readFile(mapPath, 'utf8'));
  map.sources = map.sources.map((source) => {
    const sourcePath = resolve(dirname(mapPath), source);
    const workspacePath = relative(workspace, sourcePath).split(sep).join('/');
    if (workspacePath.startsWith('../'))
      throw new Error(`Source map path escapes the frontend workspace: ${source}`);
    return `sniffy-ui/${workspacePath}`;
  });
  await writeFile(mapPath, JSON.stringify(map));
}

async function buildProfiler(fileName, minify, sourcemap) {
  const outDir = resolve(scratch, fileName);
  await build({
    root: workspace,
    configFile: false,
    plugins: [react(), tailwindcss()],
    define: { 'process.env.NODE_ENV': JSON.stringify('production') },
    build: {
      outDir,
      emptyOutDir: true,
      minify,
      sourcemap,
      cssCodeSplit: false,
      target: ['es2020', 'chrome90', 'firefox88', 'safari14'],
      lib: {
        entry: resolve(workspace, 'apps/profiler/src/index.tsx'),
        name: 'SniffyProfiler',
        formats: ['iife'],
        fileName: () => fileName,
      },
      rollupOptions: { output: { assetFileNames: '[name][extname]' } },
    },
    logLevel: 'warn',
  });
  return outDir;
}

try {
  const readableDir = await buildProfiler('sniffy.js', false, false);
  const minifiedDir = await buildProfiler('sniffy.min.js', 'oxc', true);
  await mkdir(profilerDestination, { recursive: true });
  const readable = (await readFile(resolve(readableDir, 'sniffy.js'), 'utf8')).replace(
    /[ \t]+$/gm,
    '',
  );
  await writeFile(resolve(profilerDestination, 'sniffy.js'), readable);
  const minifiedPath = resolve(minifiedDir, 'sniffy.min.js');
  const mapSource = resolve(minifiedDir, 'sniffy.min.js.map');
  const minified = (await readFile(minifiedPath, 'utf8')).replace(
    'sourceMappingURL=sniffy.min.js.map',
    'sourceMappingURL=sniffy.map',
  );
  await writeFile(resolve(profilerDestination, 'sniffy.min.js'), minified);
  await rename(mapSource, resolve(minifiedDir, 'sniffy.map'));
  await normalizeSourceMap(resolve(minifiedDir, 'sniffy.map'));
  await cp(resolve(minifiedDir, 'sniffy.map'), resolve(profilerDestination, 'sniffy.map'));

  const agentOut = resolve(scratch, 'agent');
  await build({
    root: resolve(workspace, 'apps/agent'),
    configFile: false,
    base: './',
    plugins: [react(), tailwindcss()],
    define: { 'process.env.NODE_ENV': JSON.stringify('production') },
    build: {
      outDir: agentOut,
      emptyOutDir: true,
      minify: 'oxc',
      sourcemap: true,
      target: ['es2020', 'chrome90', 'firefox88', 'safari14'],
      rollupOptions: {
        output: {
          entryFileNames: 'sniffy-agent.js',
          chunkFileNames: 'sniffy-agent.js',
          assetFileNames: (asset) =>
            asset.names?.some((name) => name.endsWith('.css'))
              ? 'sniffy-agent.css'
              : '[name][extname]',
        },
      },
    },
    logLevel: 'warn',
  });
  await normalizeSourceMap(resolve(agentOut, 'sniffy-agent.js.map'));
  await mkdir(agentDestination, { recursive: true });
  for (const file of ['index.html', 'sniffy-agent.js', 'sniffy-agent.js.map', 'sniffy-agent.css'])
    await cp(resolve(agentOut, file), resolve(agentDestination, file));
} finally {
  await rm(scratch, { recursive: true, force: true });
}
