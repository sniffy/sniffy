import { readdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

import searchLocalPlugin from '@cmfcmf/docusaurus-search-local';
import type { LoadContext, Plugin } from '@docusaurus/types';

type SearchPluginOptions = {
  indexBlog?: boolean;
  indexDocs?: boolean;
  indexPages?: boolean;
  language?: string;
  lunr?: {
    b?: number;
    contentBoost?: number;
    k1?: number;
    parentCategoriesBoost?: number;
    tagsBoost?: number;
    titleBoost?: number;
  };
  maxSearchResults?: number;
};

type SearchDocument = {
  id: number;
  pageTitle: string;
  sectionRoute: string;
  sectionTitle: string;
  type: string;
};

type SerializedIndex = {
  fieldVectors: [string, number[]][];
  invertedIndex: [string, Record<string, unknown>][];
  [key: string]: unknown;
};

type SearchIndexPayload = {
  documents: SearchDocument[];
  index: SerializedIndex;
};

function compareDocuments(left: SearchDocument, right: SearchDocument) {
  return (
    left.sectionRoute.localeCompare(right.sectionRoute, 'en') ||
    left.pageTitle.localeCompare(right.pageTitle, 'en') ||
    left.sectionTitle.localeCompare(right.sectionTitle, 'en') ||
    left.type.localeCompare(right.type, 'en')
  );
}

function remapDocumentRecord(record: Record<string, unknown>, idByPreviousId: Map<number, number>) {
  return Object.fromEntries(
    Object.entries(record)
      .map(([key, value]) => {
        const previousId = Number(key);
        return [String(idByPreviousId.get(previousId) ?? previousId), value] as const;
      })
      .sort(([left], [right]) => Number(left) - Number(right)),
  );
}

export function normalizeSearchIndex(payload: SearchIndexPayload): SearchIndexPayload {
  const sortedDocuments = [...payload.documents].sort(compareDocuments);
  const idByPreviousId = new Map(
    sortedDocuments.map((document, index) => [document.id, index + 1]),
  );
  const documents = sortedDocuments.map((document, index) => ({
    ...document,
    id: index + 1,
  }));
  const sortedTerms = payload.index.invertedIndex
    .map(([term, posting]) => {
      const previousIndex = posting._index;
      if (!Number.isInteger(previousIndex)) {
        throw new Error(`Search index term ${term} has an invalid identifier.`);
      }
      return { posting, previousIndex: previousIndex as number, term };
    })
    .sort((left, right) => (left.term < right.term ? -1 : left.term > right.term ? 1 : 0));
  const termIndexByPreviousIndex = new Map(
    sortedTerms.map(({ previousIndex }, index) => [previousIndex, index]),
  );
  const fieldVectors = payload.index.fieldVectors
    .map(([fieldAndId, vector]) => {
      const separator = fieldAndId.lastIndexOf('/');
      const field = fieldAndId.slice(0, separator);
      const previousId = Number(fieldAndId.slice(separator + 1));
      const id = idByPreviousId.get(previousId);
      if (!id) throw new Error(`Search index references unknown document ${previousId}.`);
      const remappedVector = [];
      for (let index = 0; index < vector.length; index += 2) {
        const termIndex = termIndexByPreviousIndex.get(vector[index]);
        if (termIndex === undefined) {
          throw new Error(`Search vector references unknown term ${vector[index]}.`);
        }
        remappedVector.push([termIndex, vector[index + 1]]);
      }
      return [
        `${field}/${id}`,
        remappedVector
          .sort(([left], [right]) => left - right)
          .flatMap(([termIndex, weight]) => [termIndex, weight]),
      ] as [string, number[]];
    })
    .sort(([left], [right]) => left.localeCompare(right, 'en', { numeric: true }));
  const invertedIndex = sortedTerms.map(({ term, posting }, index) => [
    term,
    Object.fromEntries(
      Object.entries(posting).map(([field, value]) => [
        field,
        field === '_index'
          ? index
          : typeof value !== 'object' || value === null
            ? value
            : remapDocumentRecord(value as Record<string, unknown>, idByPreviousId),
      ]),
    ),
  ]) as [string, Record<string, unknown>][];

  return {
    documents,
    index: {
      ...payload.index,
      fieldVectors,
      invertedIndex,
    },
  };
}

export async function normalizeGeneratedSearchIndexes(outDir: string) {
  const files = (await readdir(outDir))
    .filter((file) => /^search-index-.*\.json$/.test(file))
    .sort();
  if (files.length === 0) {
    throw new Error('The local search plugin did not generate an index.');
  }

  for (const file of files) {
    const path = resolve(outDir, file);
    const payload = JSON.parse(await readFile(path, 'utf8')) as SearchIndexPayload;
    await writeFile(path, JSON.stringify(normalizeSearchIndex(payload)), 'utf8');
  }
}

export default function deterministicSearchPlugin(context: LoadContext, options: unknown): Plugin {
  const searchOptions = (options ?? {}) as SearchPluginOptions;
  const upstream = searchLocalPlugin(context, {
    indexDocSidebarParentCategories: 0,
    includeParentCategoriesInPageTitle: false,
    indexBlog: true,
    indexDocs: true,
    indexPages: false,
    language: 'en',
    maxSearchResults: 8,
    ...searchOptions,
    lunr: {
      b: 0.75,
      contentBoost: 1,
      k1: 1.2,
      parentCategoriesBoost: 2,
      tagsBoost: 3,
      titleBoost: 5,
      ...searchOptions.lunr,
    },
  }) as Plugin;
  const upstreamPostBuild = upstream.postBuild;

  return {
    ...upstream,
    async postBuild(props) {
      await upstreamPostBuild?.(props);
      await normalizeGeneratedSearchIndexes(props.outDir);
    },
  };
}
