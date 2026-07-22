import { access, copyFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const site = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const build = resolve(site, 'build');

await access(resolve(build, 'index.html'));
await access(resolve(build, 'docs/index.html'));

for (const [source, destination] of [
  [resolve(site, 'scripts/preview.mjs'), resolve(build, 'preview.mjs')],
  [resolve(site, 'artifact/README.md'), resolve(build, 'README.md')],
  [resolve(site, 'artifact/.node-version'), resolve(build, '.node-version')],
]) {
  await copyFile(source, destination);
}

process.stdout.write(
  'Packaged README.md, preview.mjs, and .node-version with the static website.\n',
);
