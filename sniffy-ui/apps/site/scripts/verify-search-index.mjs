import { createHash } from 'node:crypto';
import { readFile, readdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import process from 'node:process';

import lunr from 'lunr';

const build = resolve(import.meta.dirname, '../build');
const baseUrl = process.env.SNIFFY_SITE_BASE_URL ?? '/';
if (
  !baseUrl.startsWith('/') ||
  !baseUrl.endsWith('/') ||
  baseUrl.includes('//') ||
  baseUrl.split('/').some((segment) => segment === '.' || segment === '..')
) {
  throw new Error(`SNIFFY_SITE_BASE_URL must have one leading and trailing slash: ${baseUrl}`);
}
const qualifyRoute = (route) => (baseUrl === '/' ? route : `${baseUrl.slice(0, -1)}${route}`);
const currentDocsPrefix = qualifyRoute('/docs/');
const nextDocsPrefix = qualifyRoute('/docs/next/');
const archivedDocsPrefix = qualifyRoute('/docs/3.1/');
const indexFiles = (await readdir(build))
  .filter((file) => /^search-index-.+\.json$/.test(file))
  .sort();

const expectedIndexFiles = [
  'search-index-docs-default-3.1.json',
  'search-index-docs-default-current.json',
];
if (JSON.stringify(indexFiles) !== JSON.stringify(expectedIndexFiles)) {
  throw new Error(`Expected isolated current and 3.1 indexes, found ${indexFiles.join(', ')}.`);
}

const documents = new Map();
const indexes = new Map();
const digest = createHash('sha256');

for (const file of indexFiles) {
  const scope = file.includes('docs-default-3.1') ? 'archived' : 'current';
  const bytes = await readFile(resolve(build, file));
  digest.update(bytes);
  const parsed = JSON.parse(bytes.toString('utf8'));

  if (!Array.isArray(parsed.documents) || typeof parsed.index !== 'object') {
    throw new Error(`${file} has an invalid generated shape.`);
  }

  const canonicalDocuments = [...parsed.documents].sort(
    (left, right) =>
      left.sectionRoute.localeCompare(right.sectionRoute, 'en') ||
      left.pageTitle.localeCompare(right.pageTitle, 'en') ||
      left.sectionTitle.localeCompare(right.sectionTitle, 'en') ||
      left.type.localeCompare(right.type, 'en'),
  );
  if (
    parsed.documents.some(
      (document, index) => document.id !== index + 1 || document !== canonicalDocuments[index],
    )
  ) {
    throw new Error(`${file} does not use canonical document order and identifiers.`);
  }

  const ids = new Set();
  for (const document of parsed.documents) {
    const currentRoute =
      document.sectionRoute.startsWith(currentDocsPrefix) &&
      !document.sectionRoute.startsWith(nextDocsPrefix) &&
      !document.sectionRoute.startsWith(archivedDocsPrefix) &&
      !/^\d/.test(document.sectionRoute.slice(currentDocsPrefix.length));
    const archivedRoute = document.sectionRoute.startsWith(archivedDocsPrefix);
    if (
      !Number.isInteger(document.id) ||
      ids.has(document.id) ||
      typeof document.pageTitle !== 'string' ||
      typeof document.sectionTitle !== 'string' ||
      typeof document.sectionRoute !== 'string' ||
      (scope === 'current' ? !currentRoute : !archivedRoute) ||
      document.type !== 'docs'
    ) {
      throw new Error(`${file} contains an invalid or out-of-scope document.`);
    }
    ids.add(document.id);
  }

  documents.set(scope, parsed.documents);
  indexes.set(scope, lunr.Index.load(parsed.index));
}

function search(scope, input) {
  const terms = input.toLocaleLowerCase('en').split(/\s+/).filter(Boolean);
  const index = indexes.get(scope);
  const scopedDocuments = documents.get(scope);
  if (!index || !scopedDocuments) throw new Error(`Missing ${scope} search index.`);
  return index
    .query((query) => {
      for (const term of terms) {
        query.term(term, { boost: 6, fields: ['title'] });
        query.term(term, {
          boost: 3,
          fields: ['title'],
          wildcard: lunr.Query.wildcard.TRAILING,
        });
        query.term(term, { fields: ['content'] });
        query.term(term, {
          boost: 0.8,
          fields: ['content'],
          wildcard: lunr.Query.wildcard.TRAILING,
        });
      }
    })
    .flatMap((match) => scopedDocuments.filter(({ id }) => id.toString() === match.ref));
}

for (const [query, expectedRoute] of [
  ['installation', qualifyRoute('/docs/installation/')],
  ['configuration', qualifyRoute('/docs/configuration/')],
  ['SQL assertions', qualifyRoute('/docs/testing/api/')],
  ['network fault simulation', qualifyRoute('/docs/network/fault-emulation/')],
  ['traffic capture', qualifyRoute('/docs/network/traffic-capture/')],
]) {
  if (
    !search('current', query).some(({ sectionRoute }) => sectionRoute.startsWith(expectedRoute))
  ) {
    throw new Error(`Generated search index did not map "${query}" to ${expectedRoute}.`);
  }
}

if (
  !search('current', 'SSL TLS traffic decryption').some(
    ({ sectionRoute }) =>
      sectionRoute === qualifyRoute('/docs/network/traffic-capture/#ssltls-traffic-decryption'),
  )
) {
  throw new Error('Generated search index did not preserve the TLS section anchor.');
}

for (const [query, expectedRoute] of [
  ['Sniffy 3.1 documentation', qualifyRoute('/docs/3.1/')],
  ['JUnit Rule', qualifyRoute('/docs/3.1/testing/junit/')],
  ['shared connection', qualifyRoute('/docs/3.1/testing/shared-connection/')],
]) {
  if (
    !search('archived', query).some(({ sectionRoute }) => sectionRoute.startsWith(expectedRoute))
  ) {
    throw new Error(`Archived search index did not map "${query}" to ${expectedRoute}.`);
  }
}

if (
  search('current', 'Sniffy 3.1 documentation').some(({ sectionRoute }) =>
    sectionRoute.startsWith(archivedDocsPrefix),
  ) ||
  search('archived', '4.0.0-SNAPSHOT').some(
    ({ sectionRoute }) => !sectionRoute.startsWith(archivedDocsPrefix),
  )
) {
  throw new Error('Current and archived search results are not isolated.');
}

const sitemap = await readFile(resolve(build, 'sitemap.xml'), 'utf8');
for (const route of [
  qualifyRoute('/docs/'),
  qualifyRoute('/docs/3.1/'),
  qualifyRoute('/docs/3.1/testing/junit/'),
]) {
  if (!sitemap.includes(`${route}</loc>`)) {
    throw new Error(`Generated sitemap does not contain ${route}.`);
  }
}
if (sitemap.includes(qualifyRoute('/docs/3.0/'))) {
  throw new Error('Generated sitemap unexpectedly publishes an unavailable 3.0 archive.');
}

process.stdout.write(
  `Verified ${indexFiles.length} deterministic isolated indexes with ${[...documents.values()].reduce((total, entries) => total + entries.length, 0)} sections (sha256 ${digest.digest('hex')}).\n`,
);
