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
const indexFiles = (await readdir(build))
  .filter((file) => /^search-index-.+\.json$/.test(file))
  .sort();

if (indexFiles.length !== 1) {
  throw new Error(`Expected one current-documentation search index, found ${indexFiles.length}.`);
}

const documents = [];
const indexes = [];
const digest = createHash('sha256');

for (const file of indexFiles) {
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
    if (
      !Number.isInteger(document.id) ||
      ids.has(document.id) ||
      typeof document.pageTitle !== 'string' ||
      typeof document.sectionTitle !== 'string' ||
      typeof document.sectionRoute !== 'string' ||
      !document.sectionRoute.startsWith(currentDocsPrefix) ||
      document.sectionRoute.startsWith(nextDocsPrefix) ||
      /^\d/.test(document.sectionRoute.slice(currentDocsPrefix.length)) ||
      document.type !== 'docs'
    ) {
      throw new Error(`${file} contains an invalid or out-of-scope document.`);
    }
    ids.add(document.id);
  }

  documents.push(...parsed.documents);
  indexes.push(lunr.Index.load(parsed.index));
}

function search(input) {
  const terms = input.toLocaleLowerCase('en').split(/\s+/).filter(Boolean);
  return indexes
    .flatMap((index) =>
      index.query((query) => {
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
      }),
    )
    .flatMap((match) => documents.filter(({ id }) => id.toString() === match.ref));
}

for (const [query, expectedRoute] of [
  ['installation', qualifyRoute('/docs/installation/')],
  ['configuration', qualifyRoute('/docs/configuration/')],
  ['SQL assertions', qualifyRoute('/docs/testing/api/')],
  ['network fault simulation', qualifyRoute('/docs/network/fault-emulation/')],
  ['traffic capture', qualifyRoute('/docs/network/traffic-capture/')],
]) {
  if (!search(query).some(({ sectionRoute }) => sectionRoute.startsWith(expectedRoute))) {
    throw new Error(`Generated search index did not map "${query}" to ${expectedRoute}.`);
  }
}

if (
  !search('SSL TLS traffic decryption').some(
    ({ sectionRoute }) =>
      sectionRoute === qualifyRoute('/docs/network/traffic-capture/#ssltls-traffic-decryption'),
  )
) {
  throw new Error('Generated search index did not preserve the TLS section anchor.');
}

process.stdout.write(
  `Verified ${indexFiles.length} deterministic current-docs index with ${documents.length} sections (sha256 ${digest.digest('hex')}).\n`,
);
