import { gzipSync } from 'node:zlib';
import { readFile, readdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const workspace = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const resourceDirectory = resolve(
  workspace,
  '../sniffy-web-common/src/main/resources/io/sniffy/ui',
);
const agentDirectory = resolve(workspace, '../sniffy/src/main/resources/web');
const bundle = await readFile(resolve(resourceDirectory, 'sniffy.min.js'));
const source = bundle.toString('utf8');
const gzip = gzipSync(bundle, { level: 9 });
const budget = 200 * 1024;
const files = (await readdir(resourceDirectory)).sort();
if (files.join(',') !== 'sniffy.js,sniffy.map,sniffy.min.js')
  throw new Error(`Unexpected profiler resources: ${files.join(', ')}`);
for (const [label, pattern] of [
  ['dynamic import', /\bimport\s*\(/],
  ['eval', /\beval\s*\(/],
  ['new Function', /new\s+Function\s*\(/],
  ['external stylesheet', /<link[^>]+stylesheet/i],
  ['unresolved build placeholder', /@@include|__VITE_/],
])
  if (pattern.test(source)) throw new Error(`Profiler bundle contains ${label}`);
if (!/\/\/# sourceMappingURL=sniffy\.map\n?$/.test(source))
  throw new Error('Profiler source map reference is missing or unstable');
if (gzip.length > budget) throw new Error(`Profiler gzip size ${gzip.length} exceeds ${budget}`);

const agentFiles = (await readdir(agentDirectory)).sort();
if (
  agentFiles.join(',') !==
  'favicon.ico,icon32.png,index.html,sniffy-agent.css,sniffy-agent.js,sniffy-agent.js.map'
)
  throw new Error(`Unexpected agent resources: ${agentFiles.join(', ')}`);
const [agentHtml, agentSource, agentCss] = await Promise.all([
  readFile(resolve(agentDirectory, 'index.html'), 'utf8'),
  readFile(resolve(agentDirectory, 'sniffy-agent.js'), 'utf8'),
  readFile(resolve(agentDirectory, 'sniffy-agent.css'), 'utf8'),
]);
for (const mapFile of [
  resolve(resourceDirectory, 'sniffy.map'),
  resolve(agentDirectory, 'sniffy-agent.js.map'),
]) {
  const sourceMap = JSON.parse(await readFile(mapFile, 'utf8'));
  if (
    !sourceMap.sources.every(
      (sourcePath) =>
        sourcePath.startsWith('sniffy-ui/') &&
        !sourcePath.includes('..') &&
        !sourcePath.includes('\\'),
    )
  )
    throw new Error(`${mapFile} contains a machine-specific source path`);
}
if (!agentHtml.includes('./sniffy-agent.js') || !agentHtml.includes('./sniffy-agent.css'))
  throw new Error('Agent HTML does not reference the stable generated assets');
if (/webjars|<script[^>]+https?:|<link[^>]+https?:/i.test(agentHtml))
  throw new Error('Agent HTML contains a legacy or external runtime asset');
for (const [label, pattern] of [
  ['dynamic import', /\bimport\s*\(/],
  ['eval', /\beval\s*\(/],
  ['new Function', /new\s+Function\s*\(/],
])
  if (pattern.test(agentSource)) throw new Error(`Agent bundle contains ${label}`);
if (/@import|url\s*\(/i.test(agentCss))
  throw new Error('Agent CSS contains a runtime import or URL');
console.log(
  `Profiler bundle: ${bundle.length} bytes raw, ${gzip.length} bytes gzip (budget ${budget}).`,
);
console.log('Agent bundle: one stable JS file, one CSS file, and no runtime assets.');
