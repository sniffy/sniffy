import { readdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

import searchLocalPlugin from '@cmfcmf/docusaurus-search-local';
import type { AllContent, LoadContext, Plugin } from '@docusaurus/types';
import lunr from 'lunr';

type SearchPluginOptions = {
  indexBlog?: boolean;
  indexDocSidebarParentCategories?: number;
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

export type DevelopmentSearchSource = {
  pageTitle: string;
  route: string;
  source: string;
};

export function qualifySearchRoutesForBaseUrl(routesPaths: string[], baseUrl: string) {
  return routesPaths.map((route) =>
    route.startsWith(baseUrl) ? route : `${baseUrl}${route.replace(/^\/+/, '')}`,
  );
}

type DocsLoadedContent = {
  loadedVersions?: {
    docs?: {
      permalink?: string;
      source?: string;
      title?: string;
    }[];
    versionName?: string;
  }[];
};

type DevelopmentSearchSection = SearchDocument & {
  content: string;
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

function normalizeMarkdownText(value: string) {
  return value
    .replace(/<SourceSnippet\b[\s\S]*?\/>/g, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/[`*_~>|{}]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function headingId(value: string) {
  return normalizeMarkdownText(value)
    .normalize('NFKD')
    .toLocaleLowerCase('en')
    .replace(/[^\p{Letter}\p{Number}\s-]/gu, '')
    .trim()
    .replace(/\s+/g, '-');
}

function parseDevelopmentSections(source: DevelopmentSearchSource) {
  const body = source.source.replace(/^---\r?\n[\s\S]*?\r?\n---\r?\n/, '');
  const lines = body.split(/\r?\n/);
  const headings: { line: number; title: string; hash: string }[] = [];
  const slugCounts = new Map<string, number>();
  let fenced = false;

  lines.forEach((line, index) => {
    if (/^\s*```/.test(line)) {
      fenced = !fenced;
      return;
    }
    if (fenced) return;
    const match = /^(#{1,3})\s+(.+?)\s*#*\s*$/.exec(line);
    if (!match) return;
    const title = normalizeMarkdownText(match[2]);
    if (!title) return;
    const baseSlug = headingId(title);
    const count = slugCounts.get(baseSlug) ?? 0;
    slugCounts.set(baseSlug, count + 1);
    headings.push({
      hash: baseSlug ? `#${baseSlug}${count === 0 ? '' : `-${count}`}` : '',
      line: index,
      title,
    });
  });

  const pageSection: Omit<DevelopmentSearchSection, 'id'> = {
    content: normalizeMarkdownText(lines.slice(0, headings[0]?.line ?? lines.length).join('\n')),
    pageTitle: source.pageTitle,
    sectionRoute: source.route,
    sectionTitle: source.pageTitle,
    type: 'docs',
  };
  const headingSections = headings.map((heading, index) => ({
    content: normalizeMarkdownText(
      lines.slice(heading.line + 1, headings[index + 1]?.line ?? lines.length).join('\n'),
    ),
    pageTitle: source.pageTitle,
    sectionRoute: `${source.route}${heading.hash}`,
    sectionTitle: heading.title,
    type: 'docs' as const,
  }));

  return [pageSection, ...headingSections];
}

export function createDevelopmentSearchIndex(
  sources: DevelopmentSearchSource[],
): SearchIndexPayload {
  const sections = [...sources]
    .sort((left, right) => left.route.localeCompare(right.route, 'en'))
    .flatMap(parseDevelopmentSections)
    .map((section, index) => ({ ...section, id: index + 1 }));
  if (sections.length === 0) {
    throw new Error('The current documentation sources produced no development search index.');
  }

  const index = lunr(function buildIndex() {
    this.k1(1.2);
    this.b(0.75);
    this.ref('id');
    this.field('title');
    this.field('content');
    this.field('tags');
    for (const section of sections) {
      this.add({
        content: section.content,
        id: section.id.toString(),
        tags: '',
        title: section.sectionTitle,
      });
    }
  });

  return normalizeSearchIndex({
    documents: sections.map(({ id, pageTitle, sectionRoute, sectionTitle, type }) => ({
      id,
      pageTitle,
      sectionRoute,
      sectionTitle,
      type,
    })),
    index: index.toJSON() as SerializedIndex,
  });
}

async function createLoadedDevelopmentSearchIndex(
  siteDir: string,
  allContent: AllContent,
): Promise<SearchIndexPayload> {
  const docsContent = allContent['docusaurus-plugin-content-docs']?.default as
    DocsLoadedContent | undefined;
  const currentDocs = docsContent?.loadedVersions?.find(
    ({ versionName }) => versionName === 'current',
  )?.docs;
  if (!currentDocs || currentDocs.length === 0) {
    throw new Error(
      'The current documentation plugin content is unavailable for development search.',
    );
  }

  const sources = await Promise.all(
    currentDocs.map(async (document) => {
      if (!document.permalink?.startsWith('/docs/') || !document.source || !document.title) {
        throw new Error('The current documentation metadata is incomplete for development search.');
      }
      const sourcePath = document.source.startsWith('@site/')
        ? resolve(siteDir, document.source.slice('@site/'.length))
        : resolve(siteDir, document.source);
      return {
        pageTitle: document.title,
        route: document.permalink.endsWith('/') ? document.permalink : `${document.permalink}/`,
        source: await readFile(sourcePath, 'utf8'),
      };
    }),
  );
  return createDevelopmentSearchIndex(sources);
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
  const upstreamAllContentLoaded = upstream.allContentLoaded;

  return {
    ...upstream,
    async allContentLoaded(props) {
      await upstreamAllContentLoaded?.(props);
      if (process.env.NODE_ENV !== 'production') {
        props.actions.setGlobalData({
          contentBoost: searchOptions.lunr?.contentBoost ?? 1,
          developmentIndex: await createLoadedDevelopmentSearchIndex(
            context.siteDir,
            props.allContent,
          ),
          indexDocSidebarParentCategories: searchOptions.indexDocSidebarParentCategories ?? 0,
          maxSearchResults: searchOptions.maxSearchResults ?? 8,
          parentCategoriesBoost: searchOptions.lunr?.parentCategoriesBoost ?? 2,
          tagsBoost: searchOptions.lunr?.tagsBoost ?? 3,
          titleBoost: searchOptions.lunr?.titleBoost ?? 5,
        });
      }
    },
    async postBuild(props) {
      await upstreamPostBuild?.({
        ...props,
        routesPaths: qualifySearchRoutesForBaseUrl(props.routesPaths, props.baseUrl),
      });
      await normalizeGeneratedSearchIndexes(props.outDir);
    },
  };
}
