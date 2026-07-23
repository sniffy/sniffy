import lunr from 'lunr';

export type SearchIndexDocument = {
  id: number;
  pageTitle: string;
  sectionRoute: string;
  sectionTitle: string;
  type: 'blog' | 'docs' | 'page';
};

type SearchIndexPayload = {
  documents: SearchIndexDocument[];
  index: lunr.Index;
};

export type SearchResult = {
  pageTitle: string;
  score: number;
  sectionTitle: string;
  url: string;
};

const searchIndexCache = new Map<string, Promise<SearchIndexPayload>>();

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isSearchIndexDocument(value: unknown): value is SearchIndexDocument {
  if (!isRecord(value)) return false;

  return (
    Number.isInteger(value.id) &&
    typeof value.pageTitle === 'string' &&
    typeof value.sectionRoute === 'string' &&
    typeof value.sectionTitle === 'string' &&
    value.type === 'docs'
  );
}

export function parseSearchIndex(payload: unknown): SearchIndexPayload {
  if (!isRecord(payload) || !Array.isArray(payload.documents) || !isRecord(payload.index)) {
    throw new Error('The documentation search index has an invalid shape.');
  }

  if (!payload.documents.every(isSearchIndexDocument)) {
    throw new Error('The documentation search index contains an invalid document.');
  }

  const ids = new Set<number>();
  for (const document of payload.documents) {
    if (
      ids.has(document.id) ||
      !document.sectionRoute.startsWith('/docs/') ||
      document.pageTitle.trim().length === 0
    ) {
      throw new Error('The documentation search index contains an invalid route or identifier.');
    }
    ids.add(document.id);
  }

  return {
    documents: payload.documents,
    index: lunr.Index.load(payload.index as Parameters<typeof lunr.Index.load>[0]),
  };
}

async function fetchSearchIndex(url: string, fetcher: typeof fetch): Promise<SearchIndexPayload> {
  const response = await fetcher(url, { credentials: 'same-origin' });
  if (!response.ok) {
    throw new Error(`The documentation search index returned HTTP ${response.status}.`);
  }

  return parseSearchIndex(await response.json());
}

export async function loadSearchIndexes(
  baseUrl: string,
  tags: string[],
  fetcher: typeof fetch = fetch,
): Promise<SearchIndexPayload[]> {
  const uniqueTags = Array.from(new Set(tags.filter(Boolean)));
  if (uniqueTags.length === 0) {
    throw new Error('No documentation search context is available.');
  }

  const settled = await Promise.allSettled(
    uniqueTags.map((tag) => {
      const url = `${baseUrl}search-index-${tag}.json`;
      let cached = searchIndexCache.get(url);
      if (!cached) {
        cached = fetchSearchIndex(url, fetcher);
        searchIndexCache.set(url, cached);
      }
      return cached;
    }),
  );
  const indexes = settled.flatMap((result) =>
    result.status === 'fulfilled' ? [result.value] : [],
  );

  if (indexes.length === 0) {
    const failures = settled.flatMap((result) =>
      result.status === 'rejected' ? [result.reason] : [],
    );
    if (failures.length === 1 && failures[0] instanceof Error) {
      throw failures[0];
    }
    throw new AggregateError(failures, 'No documentation search index could be loaded.');
  }

  return indexes;
}

function queryIndex(index: lunr.Index, terms: string[]) {
  return index.query((query) => {
    for (const term of terms) {
      query.term(term, { boost: 6, fields: ['title'] });
      query.term(term, {
        boost: 3,
        fields: ['title'],
        wildcard: lunr.Query.wildcard.TRAILING,
      });
      query.term(term, { boost: 1, fields: ['content'] });
      query.term(term, {
        boost: 0.8,
        fields: ['content'],
        wildcard: lunr.Query.wildcard.TRAILING,
      });
      query.term(term, { boost: 2, fields: ['tags'] });
    }
  });
}

export function searchIndexes(
  indexes: SearchIndexPayload[],
  input: string,
  limit = 8,
): SearchResult[] {
  const terms = input.trim().toLocaleLowerCase('en').split(/\s+/).filter(Boolean);
  if (terms.length === 0) return [];

  const matches = indexes.flatMap(({ documents, index }) =>
    queryIndex(index, terms).flatMap((match) => {
      const document = documents.find(({ id }) => id.toString() === match.ref);
      if (!document) return [];

      return [
        {
          pageTitle: document.pageTitle,
          score: match.score,
          sectionTitle: document.sectionTitle || document.pageTitle,
          url: document.sectionRoute,
        },
      ];
    }),
  );

  const bestByUrl = new Map<string, SearchResult>();
  for (const match of matches.sort((left, right) => right.score - left.score)) {
    if (!bestByUrl.has(match.url)) bestByUrl.set(match.url, match);
  }

  return Array.from(bestByUrl.values()).slice(0, limit);
}

export function resetSearchIndexCacheForTests() {
  searchIndexCache.clear();
}
