import { createHash } from 'node:crypto';
import { readFile, readdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import process from 'node:process';

const site = resolve(import.meta.dirname, '..');
const archive = resolve(site, 'versioned_docs/version-3.1');
const manifest = JSON.parse(await readFile(resolve(archive, '_archive-manifest.json'), 'utf8'));
const externalLinks = JSON.parse(await readFile(resolve(archive, '_external-links.json'), 'utf8'));
const expectedSource = {
  tag: 'v3.1.14',
  commit: '55f744f6a32dd68ee8d5d31c54796e9b5d75aad1',
};
const expectedPages = [
  'index.mdx',
  'installation/index.mdx',
  'setup/spring.mdx',
  'setup/datasource.mdx',
  'setup/filter.mdx',
  'setup/containers.mdx',
  'configuration/index.mdx',
  'testing/api.mdx',
  'testing/shared-connection.mdx',
  'testing/junit.mdx',
  'testing/kotest.mdx',
  'testing/spring.mdx',
  'testing/testng.mdx',
  'testing/spock.mdx',
  'network/fault-emulation.mdx',
  'network/traffic-capture.mdx',
  'migration/to-3.1.mdx',
].sort();

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function sha256(content) {
  return createHash('sha256').update(content).digest('hex');
}

async function allFiles(directory, prefix = '') {
  const entries = await readdir(directory, { withFileTypes: true });
  return (
    await Promise.all(
      entries.map(async (entry) => {
        const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
        return entry.isDirectory()
          ? allFiles(resolve(directory, entry.name), relative)
          : [relative];
      }),
    )
  ).flat();
}

assert(manifest.schemaVersion === 1, 'The archive manifest schema is unsupported.');
assert(manifest.sourceTag === expectedSource.tag, 'The archive source tag changed.');
assert(manifest.sourceCommit === expectedSource.commit, 'The archive source commit changed.');
assert(
  manifest.snippets.length === 14,
  'The complete historical include set must contain 14 snippets.',
);
assert(
  manifest.assets.length === 3,
  'The complete historical asset set must contain three assets.',
);

const archivePages = (await allFiles(archive)).filter((file) => file.endsWith('.mdx')).sort();
assert(
  JSON.stringify(archivePages) === JSON.stringify(expectedPages),
  `Archived page inventory differs: ${archivePages.join(', ')}`,
);

const renderedExternalLinks = new Set();
for (const page of archivePages) {
  const source = await readFile(resolve(archive, page), 'utf8');
  assert(
    source.includes("import ArchiveBanner from '@site/src/components/ArchiveBanner';") &&
      source.includes('<ArchiveBanner />'),
    `${page} does not render the archive notice.`,
  );
  assert(!source.includes('<SourceSnippet'), `${page} resolves a live current-source snippet.`);
  assert(!source.includes('4.0.0-SNAPSHOT'), `${page} leaked the current product version.`);
  assert(
    !source.includes('{sniffy-version}'),
    `${page} contains an unresolved version placeholder.`,
  );
  const prose = source.replace(/```[\s\S]*?```/g, '');
  for (const match of prose.matchAll(/\[[^\]]+\]\((https?:\/\/[^)]+)\)/g)) {
    renderedExternalLinks.add(match[1]);
  }
}

assert(externalLinks.auditDate === '2026-07-27', 'The historical link audit date changed.');
assert(externalLinks.sourceTag === expectedSource.tag, 'The link audit source tag changed.');
assert(
  externalLinks.sourceCommit === expectedSource.commit,
  'The link audit source commit changed.',
);
const classifiedRenderedLinks = externalLinks.links
  .flatMap((link) => (link.renderedUrl ? [link.renderedUrl] : []))
  .sort();
assert(
  JSON.stringify([...renderedExternalLinks].sort()) === JSON.stringify(classifiedRenderedLinks),
  'Every rendered historical external link must have exactly one audit classification.',
);
assert(
  externalLinks.links.some(
    (link) =>
      link.sourceUrl === 'http://groovy.codehaus.org/' &&
      link.action === 'removed-link-preserved-text' &&
      link.renderedUrl === null,
  ),
  'The dead Groovy link classification is missing.',
);

for (const snippet of manifest.snippets) {
  assert(snippet.sourceTag === expectedSource.tag, `${snippet.id} has the wrong source tag.`);
  assert(
    snippet.sourceCommit === expectedSource.commit,
    `${snippet.id} has the wrong source commit.`,
  );
  assert(/^[0-9a-f]{40}$/.test(snippet.sourceBlobSha), `${snippet.id} has an invalid blob SHA.`);
  assert(
    snippet.region === 'whole-file' || snippet.region.length > 0,
    `${snippet.id} has no region marker.`,
  );
  const excerpt = await readFile(resolve(archive, snippet.excerptPath), 'utf8');
  assert(sha256(excerpt) === snippet.excerptSha256, `${snippet.id} excerpt bytes changed.`);
  for (const owner of snippet.owningArchivedPages) {
    assert(expectedPages.includes(owner), `${snippet.id} names an unknown owner ${owner}.`);
    const page = await readFile(resolve(archive, owner), 'utf8');
    const renderedSnippet = `<!-- archived-snippet:${snippet.id} -->\n\n\`\`\`${snippet.language}\n${excerpt.replace(/\n$/, '')}\n\`\`\``;
    assert(
      page.includes(renderedSnippet),
      `${owner} does not render frozen snippet ${snippet.id}.`,
    );
  }
}

for (const asset of manifest.assets) {
  assert(asset.sourceTag === expectedSource.tag, `${asset.sourcePath} has the wrong source tag.`);
  assert(
    asset.sourceCommit === expectedSource.commit,
    `${asset.sourcePath} has the wrong source commit.`,
  );
  assert(
    /^[0-9a-f]{40}$/.test(asset.sourceBlobSha),
    `${asset.sourcePath} has an invalid blob SHA.`,
  );
  assert(
    asset.committedPath.startsWith('/img/docs/3.1/'),
    `${asset.sourcePath} is outside the archive asset namespace.`,
  );
  const committed = await readFile(resolve(site, 'static', asset.committedPath.slice(1)));
  assert(
    sha256(committed) === asset.committedSha256,
    `${asset.committedPath} no longer matches the archive manifest.`,
  );
}

const versions = JSON.parse(await readFile(resolve(site, 'versions.json'), 'utf8'));
assert(
  JSON.stringify(versions) === JSON.stringify(['3.1']),
  'Only the normalized 3.1 archive may be published.',
);
const sidebars = JSON.parse(
  await readFile(resolve(site, 'versioned_sidebars/version-3.1-sidebars.json'), 'utf8'),
);
const sidebarIds = [];
function collectSidebarIds(items) {
  for (const item of items) {
    if (typeof item === 'string') sidebarIds.push(`${item}.mdx`);
    else if (Array.isArray(item.items)) collectSidebarIds(item.items);
  }
}
collectSidebarIds(sidebars.docsSidebar);
assert(
  JSON.stringify(sidebarIds.sort()) === JSON.stringify(expectedPages),
  'The archived sidebar must include every archived page exactly once.',
);

const installation = await readFile(resolve(archive, 'installation/index.mdx'), 'utf8');
assert(
  (installation.match(/3\.1\.14/g) ?? []).length >= 7,
  'Historical installation coordinates no longer preserve version 3.1.14.',
);

process.stdout.write(
  `Verified ${expectedPages.length} archived pages, ${manifest.snippets.length} frozen snippets, and ${manifest.assets.length} historical assets from ${expectedSource.tag}@${expectedSource.commit}.\n`,
);
