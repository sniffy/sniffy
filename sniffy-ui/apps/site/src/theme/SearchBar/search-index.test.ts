import lunr from 'lunr';

import {
  loadSearchIndexes,
  parseSearchIndex,
  resetSearchIndexCacheForTests,
  searchIndexes,
} from './search-index';

function createPayload() {
  const source = [
    {
      id: 1,
      pageTitle: 'Installation',
      sectionRoute: '/docs/installation/',
      sectionTitle: 'Installation',
      type: 'docs' as const,
      content: 'Install Sniffy from Maven Central.',
    },
    {
      id: 2,
      pageTitle: 'Network fault emulation',
      sectionRoute: '/docs/network/fault-emulation/#connection-delay',
      sectionTitle: 'Connection delay',
      type: 'docs' as const,
      content: 'Simulate network faults and connection latency.',
    },
  ];
  const index = lunr(function buildIndex() {
    this.ref('id');
    this.field('title');
    this.field('content');
    this.field('tags');
    for (const document of source) {
      this.add({
        content: document.content,
        id: document.id.toString(),
        tags: '',
        title: document.sectionTitle,
      });
    }
  });

  return {
    documents: source.map((document) => ({
      id: document.id,
      pageTitle: document.pageTitle,
      sectionRoute: document.sectionRoute,
      sectionTitle: document.sectionTitle,
      type: document.type,
    })),
    index: index.toJSON(),
  };
}

describe('documentation search index', () => {
  beforeEach(() => resetSearchIndexCacheForTests());

  it('loads, validates, and caches a same-origin generated index', async () => {
    const fetcher = vi.fn(async () => new Response(JSON.stringify(createPayload())));

    const first = await loadSearchIndexes('/', ['docs-default-current'], { fetcher });
    const second = await loadSearchIndexes('/', ['docs-default-current'], { fetcher });

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(first).toHaveLength(1);
    expect(second).toEqual(first);
    expect(fetcher).toHaveBeenCalledWith('/search-index-docs-default-current.json', {
      credentials: 'same-origin',
    });
  });

  it('uses an available contextual index when another tag is absent', async () => {
    const fetcher = vi.fn(async (input: string | URL | Request) =>
      String(input).endsWith('search-index-default.json')
        ? new Response('missing', { status: 404 })
        : new Response(JSON.stringify(createPayload())),
    );

    await expect(
      loadSearchIndexes('/', ['default', 'docs-default-current'], { fetcher }),
    ).resolves.toHaveLength(1);
  });

  it('uses the current development index when production JSON is absent', async () => {
    const fetcher = vi.fn(async () => new Response('development shell'));

    const indexes = await loadSearchIndexes('/', ['docs-default-current'], {
      developmentIndex: createPayload(),
      fetcher,
    });

    expect(searchIndexes(indexes, 'network fault')).toMatchObject([
      {
        pageTitle: 'Network fault emulation',
        sectionTitle: 'Connection delay',
        url: '/docs/network/fault-emulation/#connection-delay',
      },
    ]);
    expect(fetcher).toHaveBeenCalledTimes(1);
    await expect(
      loadSearchIndexes('/', ['docs-default-current'], {
        developmentIndex: { documents: [], index: null },
        fetcher,
      }),
    ).rejects.toThrow('invalid shape');
  });

  it('returns section-aware results and safely handles empty input', () => {
    const index = parseSearchIndex(createPayload());

    expect(searchIndexes([index], 'network fault')).toMatchObject([
      {
        pageTitle: 'Network fault emulation',
        sectionTitle: 'Connection delay',
        url: '/docs/network/fault-emulation/#connection-delay',
      },
    ]);
    expect(searchIndexes([index], '   ')).toEqual([]);
  });

  it('rejects malformed, duplicate, and out-of-scope documents', () => {
    expect(() => parseSearchIndex({ documents: [], index: null })).toThrow('invalid shape');

    const duplicate = createPayload();
    duplicate.documents[1] = { ...duplicate.documents[0] };
    expect(() => parseSearchIndex(duplicate)).toThrow('invalid route or identifier');

    const outOfScope = createPayload();
    outOfScope.documents[0].sectionRoute = '/blog/release/';
    expect(() => parseSearchIndex(outOfScope)).toThrow('invalid route or identifier');
  });

  it('reports HTTP and serialized-index failures without returning partial data', async () => {
    await expect(
      loadSearchIndexes('/', ['docs-default-current'], {
        fetcher: vi.fn(async () => new Response('missing', { status: 404 })),
      }),
    ).rejects.toThrow('HTTP 404');

    resetSearchIndexCacheForTests();
    await expect(
      loadSearchIndexes('/', ['docs-default-current'], {
        fetcher: vi.fn(async () => new Response('{"documents":[],"index":{"version":"broken"}}')),
      }),
    ).rejects.toThrow();
  });
});
