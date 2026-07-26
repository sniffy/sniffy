import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { resolve } from 'node:path';

import searchLocalPlugin from '@cmfcmf/docusaurus-search-local';
import type { LoadContext, Plugin } from '@docusaurus/types';
import lunr from 'lunr';

import deterministicSearchPlugin, {
  createDevelopmentSearchIndex,
  normalizeSearchIndex,
  qualifySearchRoutesForBaseUrl,
} from './deterministic-search-index';

vi.mock('@cmfcmf/docusaurus-search-local', () => ({
  default: vi.fn(),
}));

function createPayload(order: number[]) {
  const source = [
    {
      id: 41,
      pageTitle: 'Configuration',
      sectionRoute: '/docs/configuration/',
      sectionTitle: 'Configuration',
      type: 'docs',
      content: 'Configure Sniffy system properties.',
    },
    {
      id: 7,
      pageTitle: 'Installation',
      sectionRoute: '/docs/installation/',
      sectionTitle: 'Installation',
      type: 'docs',
      content: 'Install Sniffy from Maven Central.',
    },
  ];
  const documents = order.map((index) => source[index]);
  const index = lunr(function buildIndex() {
    this.ref('id');
    this.field('title');
    this.field('content');
    this.field('tags');
    for (const document of documents) {
      this.add({
        content: document.content,
        id: document.id.toString(),
        tags: '',
        title: document.sectionTitle,
      });
    }
  });

  return {
    documents: documents.map((document) => ({
      id: document.id,
      pageTitle: document.pageTitle,
      sectionRoute: document.sectionRoute,
      sectionTitle: document.sectionTitle,
      type: document.type,
    })),
    index: index.toJSON() as Parameters<typeof normalizeSearchIndex>[0]['index'],
  } satisfies Parameters<typeof normalizeSearchIndex>[0];
}

describe('deterministic documentation search index', () => {
  it('qualifies Docusaurus routes for root and project Pages base URLs', () => {
    expect(qualifySearchRoutesForBaseUrl(['docs/', '/blog/', '/404.html'], '/')).toEqual([
      '/docs/',
      '/blog/',
      '/404.html',
    ]);
    expect(qualifySearchRoutesForBaseUrl(['docs/', '/blog/', '404.html'], '/sniffy/')).toEqual([
      '/sniffy/docs/',
      '/sniffy/blog/',
      '/sniffy/404.html',
    ]);
  });

  it('does not double-prefix a route already qualified for the Pages base URL', () => {
    expect(qualifySearchRoutesForBaseUrl(['/sniffy/docs/already-qualified/'], '/sniffy/')).toEqual([
      '/sniffy/docs/already-qualified/',
    ]);
  });

  it('qualifies routes before forwarding the upstream plugin post-build lifecycle', async () => {
    const outDir = await mkdtemp(resolve(tmpdir(), 'sniffy-search-plugin-'));
    const upstreamPostBuild = vi.fn<NonNullable<Plugin['postBuild']>>(async (props) => {
      await writeFile(
        resolve(props.outDir, 'search-index-test.json'),
        JSON.stringify(createPayload([0, 1])),
      );
    });
    vi.mocked(searchLocalPlugin).mockReturnValue({
      name: 'test-search-plugin',
      postBuild: upstreamPostBuild,
    });
    const plugin = deterministicSearchPlugin({ siteDir: '/site' } as LoadContext, {});
    const props = {
      baseUrl: '/sniffy/',
      marker: { preserved: true },
      outDir,
      routesPaths: ['docs/', '/sniffy/already-qualified/'],
    } as unknown as Parameters<NonNullable<Plugin['postBuild']>>[0];

    try {
      if (!plugin.postBuild) throw new Error('Expected the wrapped plugin to define postBuild.');
      await plugin.postBuild(props);

      expect(upstreamPostBuild).toHaveBeenCalledOnce();
      expect(upstreamPostBuild).toHaveBeenCalledWith({
        ...props,
        routesPaths: ['/sniffy/docs/', '/sniffy/already-qualified/'],
      });
    } finally {
      await rm(outDir, { force: true, recursive: true });
    }
  });

  it('canonicalizes document identifiers and serialized Lunr references', () => {
    const first = normalizeSearchIndex(createPayload([0, 1]));
    const second = normalizeSearchIndex(createPayload([1, 0]));

    expect(JSON.stringify(first)).toBe(JSON.stringify(second));
    expect(first.documents).toMatchObject([
      { id: 1, sectionRoute: '/docs/configuration/' },
      { id: 2, sectionRoute: '/docs/installation/' },
    ]);
    expect(lunr.Index.load(first.index).search('installation')).toMatchObject([{ ref: '2' }]);
  });

  it('builds deterministic current-doc development sections from MDX sources', () => {
    const sources = [
      {
        pageTitle: 'Using the Sniffy API',
        route: '/docs/testing/api/',
        source: [
          '---',
          'title: Using the Sniffy API',
          '---',
          'Write SQL query assertions against recorded traffic.',
          '### Functional approach',
          'Use `Sniffy.execute()` to validate SQL queries.',
        ].join('\n'),
      },
      {
        pageTitle: 'Traffic capture',
        route: '/docs/network/traffic-capture/',
        source: '### SSL/TLS Traffic Decryption\nDecrypt captured network traffic.',
      },
    ];
    const first = createDevelopmentSearchIndex(sources);
    const second = createDevelopmentSearchIndex([...sources].reverse());
    const index = lunr.Index.load(first.index);

    expect(JSON.stringify(first)).toBe(JSON.stringify(second));
    expect(first.documents).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          sectionRoute: '/docs/network/traffic-capture/#ssltls-traffic-decryption',
          sectionTitle: 'SSL/TLS Traffic Decryption',
        }),
        expect.objectContaining({
          sectionRoute: '/docs/testing/api/',
          sectionTitle: 'Using the Sniffy API',
        }),
      ]),
    );
    expect(index.search('sql')).not.toHaveLength(0);
  });

  it('matches Docusaurus heading slugs without indexing fenced-code headings', () => {
    const result = createDevelopmentSearchIndex([
      {
        pageTitle: 'Repeated sections',
        route: '/docs/repeated/',
        source: [
          '```md',
          '## Not a heading',
          '```',
          '## Repeated *heading*',
          'First section with a [searchable link](https://example.com).',
          '## Repeated *heading*',
          'Second section.',
        ].join('\n'),
      },
    ]);

    expect(result.documents.map(({ sectionRoute }) => sectionRoute)).toEqual([
      '/docs/repeated/',
      '/docs/repeated/#repeated-heading',
      '/docs/repeated/#repeated-heading-1',
    ]);
    expect(lunr.Index.load(result.index).search('searchable')).not.toHaveLength(0);
  });

  it('rejects an empty development source set', () => {
    expect(() => createDevelopmentSearchIndex([])).toThrow(
      'The current documentation sources produced no development search index.',
    );
  });
});
