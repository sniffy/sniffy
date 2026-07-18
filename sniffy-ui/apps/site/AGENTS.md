# Docusaurus site agent instructions

These rules apply to every change under `sniffy-ui/apps/site` in addition to the repository instructions.

## Scope and ownership

- Keep the Docusaurus application, its configuration, MDX, static assets, tests, and site-only helpers in this directory.
- Import product identity and semantic tokens from `@sniffy/theme`. Site-local layout or component tokens must use a
  site-specific name and must not be presented as a shared product contract.
- Do not change profiler or agent behavior, generated Java resources, Maven modules, deployment workflows, or public
  infrastructure from this application.

## Build and routes

- Run site commands from `sniffy-ui`: `npm run dev:site`, `npm run build:site`, and `npm run test:site`.
- Keep the root `npm run build` command dedicated to profiler and agent Java-resource generation.
- `/` is the website homepage and `/docs/` is current stable documentation. Use canonical trailing-slash links.
- `/docs/next/` and `/docs/<version>/` are reserved for separately authorized documentation work. Do not create versioned
  content or enable Docusaurus versioning without that issue's provenance and compatibility requirements.
- `/blog/` is reserved and must remain disabled until the separately authorized Git-backed blog work. Do not add sample
  posts, feeds, placeholder publication, or blog configuration.

## Content

- Keep scaffolding copy minimal until the branded shell, homepage, documentation migration, and landing-page issues are
  separately authorized.
- Do not copy AsciiDoc or repository source snippets into MDX. Later migration work must follow the architecture contract
  and tagged-snippet pipeline.
- Do not add historical documentation, use-case pages, redirects, deployment files, or public-domain changes as incidental
  work.

## Accessibility

- Use semantic HTML, a logical heading order, descriptive link text, keyboard-operable controls, and visible focus states.
- Preserve WCAG AA text contrast by mapping Infima through tested Sniffy semantic tokens. Do not solve contrast issues by
  copying palette values into site CSS.
- Respect reduced-motion and zoom/reflow behavior when later work introduces motion or responsive components.
- Do not suppress accessibility findings without a documented, issue-scoped reason.

## Verification

- Run `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test`, `npm run build:site`, and `npm run test:site`.
- For any frontend change, also run the repository-required Storybook build, existing production build,
  `check:generated`, `check:bundle`, audit, and applicable Playwright checks.
- Verify `/`, `/docs/`, and the deliberate 404 behavior for reserved routes against the production site build.
- Review desktop and meaningful mobile states and attach review-only screenshots to the pull request, never to this
  directory or repository history.
