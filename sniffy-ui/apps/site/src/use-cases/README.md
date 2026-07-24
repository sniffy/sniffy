# Git-authored use-case pages

Each use-case page has three layers:

1. `components/use-case/use-case.ts` owns the typed, runtime-validated content and metadata contract.
2. `components/use-case/use-case-layout.tsx` owns shared presentation and the focused supporting components.
3. A file in this directory owns one page's product copy, examples, imagery, related documentation, and CTA. A thin
   route module under `pages/use-cases/` supplies repository-backed values such as the current product version.

Use `database-query-testing.ts` as the representative example. A new page should:

- call `defineUseCase` so missing metadata, invalid routes, missing documentation links, or invalid media dimensions
  fail deterministically during tests and the Docusaurus build;
- use a canonical `/use-cases/<slug>/` route and current trailing-slash `/docs/` links;
- keep page-specific wording and arrays here rather than adding topic conditionals to the layout;
- cite repository paths and tagged regions for technical code examples;
- use reviewed Sniffy media with written provenance instead of mocked or invented product UI;
- add focused render/metadata/source tests, direct route and artifact checks, accessibility coverage, and desktop/mobile
  light/dark visual baselines;
- add navigation only after the page exists, without enabling future use-case routes.

Extend the shared contract only when at least two pages need the same new semantic field. Layout-only styling belongs in
the shared component and site CSS; product facts remain in the individual content module.
