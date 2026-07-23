import lunr from 'lunr';

import { normalizeSearchIndex } from './deterministic-search-index';

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
});
