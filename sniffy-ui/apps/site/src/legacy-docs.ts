import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

import type { LoadContext, Plugin } from '@docusaurus/types';

import { legacyDocsPath } from './legacy-docs-routes';

export { currentDocsPath, legacyDocsPath } from './legacy-docs-routes';

export function qualifyLegacyDocsPath(baseUrl: string): string {
  return legacyDocsPath.startsWith(baseUrl)
    ? legacyDocsPath
    : `${baseUrl}${legacyDocsPath.replace(/^\/+/, '')}`;
}

function asciidoctorFragmentId(heading: string): string {
  return `_${heading
    .toLowerCase()
    .replace(/[`*@]/g, '')
    .replace(/\//g, '')
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')}`;
}

export function parseLegacyRouteMap(markdown: string): Record<string, string> {
  const routes: Record<string, string> = {};
  const row = /^\|\s*`[^`]+#([^`]+)`\s*\|\s*(.*?)\s*\|\s*\[`([^`]+)`\]\([^)]*\)\s*\|$/gm;
  const generatedIdOccurrences: Record<string, number> = {};

  for (const match of markdown.matchAll(row)) {
    const anchor = match[1];
    const heading = match[2];
    const destination = match[3];
    const generatedId = asciidoctorFragmentId(heading);
    const occurrence = (generatedIdOccurrences[generatedId] ?? 0) + 1;
    generatedIdOccurrences[generatedId] = occurrence;
    const actualLegacyId = occurrence === 1 ? generatedId : `${generatedId}_${occurrence}`;

    // Preserve inventory spellings used by old inbound links, but derive the
    // generated ID from the legacy heading. Asciidoctor 1.6 used underscores,
    // removed punctuation such as '/', and suffixed duplicate IDs.
    for (const legacyAnchor of [anchor, `_${anchor}`, actualLegacyId]) {
      routes[legacyAnchor] ??= destination;
    }
  }

  return routes;
}

export default function legacyDocsPlugin({ baseUrl, siteDir }: LoadContext): Plugin {
  return {
    name: 'sniffy-legacy-docs-compatibility',
    async loadContent() {
      const mapping = await readFile(
        resolve(siteDir, 'docs/migration/asciidoc-route-map.mdx'),
        'utf8',
      );
      return parseLegacyRouteMap(mapping);
    },
    async contentLoaded({ content, actions }) {
      const routesData = await actions.createData(
        'legacy-docs-routes.json',
        JSON.stringify(content),
      );
      const component = resolve(siteDir, 'src/legacy-docs-page.tsx');

      // The non-exact route also accepts the historical explicit index.html URL
      // in the development server. Static generation writes the directory route
      // to that same file, so both URLs share one implementation and artifact.
      actions.addRoute({
        path: qualifyLegacyDocsPath(baseUrl),
        exact: false,
        component,
        modules: { routes: routesData },
      });
    },
  };
}
