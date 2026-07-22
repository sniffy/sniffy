import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

import type { Plugin } from '@docusaurus/types';

export const legacyDocsPath = '/docs/latest/';
export const currentDocsPath = '/docs/';

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

export function renderLegacyDocsPage(routes: Record<string, string>): string {
  const serializedRoutes = JSON.stringify(routes).replace(/</g, '\\u003c');

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sniffy documentation</title>
    <link rel="canonical" href="https://sniffy.io${currentDocsPath}">
    <script>
      (function () {
        var routes = ${serializedRoutes};
        var fallback = ${JSON.stringify(currentDocsPath)};
        var rawAnchor = window.location.hash.slice(1);
        var anchor;
        try {
          anchor = decodeURIComponent(rawAnchor);
        } catch (error) {
          anchor = '';
        }
        window.location.replace(Object.prototype.hasOwnProperty.call(routes, anchor) ? routes[anchor] : fallback);
      })();
    </script>
  </head>
  <body>
    <main>
      <h1>Sniffy documentation has moved</h1>
      <p><a href="${currentDocsPath}">Continue to the current Sniffy documentation.</a></p>
    </main>
  </body>
</html>
`;
}

export default function legacyDocsPlugin(): Plugin {
  return {
    name: 'sniffy-legacy-docs-compatibility',
    async postBuild({ outDir, siteDir }) {
      const mapping = await readFile(
        resolve(siteDir, 'docs/migration/asciidoc-route-map.mdx'),
        'utf8',
      );
      const outputDirectory = resolve(outDir, 'docs/latest');
      await mkdir(outputDirectory, { recursive: true });
      await writeFile(
        resolve(outputDirectory, 'index.html'),
        renderLegacyDocsPage(parseLegacyRouteMap(mapping)),
      );
    },
  };
}
