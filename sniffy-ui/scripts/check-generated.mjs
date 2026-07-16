import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const workspace = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repository = resolve(workspace, '..');
const generated = await mkdtemp(resolve(tmpdir(), 'sniffy-ui-generated-'));
const files = [
  'sniffy-web-common/src/main/resources/io/sniffy/ui/sniffy.js',
  'sniffy-web-common/src/main/resources/io/sniffy/ui/sniffy.min.js',
  'sniffy-web-common/src/main/resources/io/sniffy/ui/sniffy.map',
  'sniffy/src/main/resources/web/index.html',
  'sniffy/src/main/resources/web/sniffy-agent.js',
  'sniffy/src/main/resources/web/sniffy-agent.js.map',
  'sniffy/src/main/resources/web/sniffy-agent.css',
];
try {
  const result = spawnSync(process.execPath, [resolve(workspace, 'scripts/build.mjs')], {
    cwd: workspace,
    stdio: 'inherit',
    env: { ...process.env, SNIFFY_GENERATED_ROOT: generated },
  });
  if (result.status !== 0) process.exit(result.status ?? 1);
  const changed = [];
  for (const file of files) {
    const [expected, actual] = await Promise.all([
      readFile(resolve(repository, file)),
      readFile(resolve(generated, file)),
    ]);
    if (!expected.equals(actual)) changed.push(file);
  }
  if (changed.length) throw new Error(`Generated resources are stale:\n${changed.join('\n')}`);
  console.log(`Generated resources are reproducible (${files.length} files).`);
} finally {
  await rm(generated, { recursive: true, force: true });
}
