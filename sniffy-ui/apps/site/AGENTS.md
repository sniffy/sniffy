# Site application instructions

These rules apply to `sniffy-ui/apps/site`.

## Scope

- Keep this app focused on the Docusaurus website foundation, documentation, and future website content.
- Do not change profiler, agent, generated Java resources, Maven release behavior, deployment workflows, blog publishing, or historical documentation unless an issue explicitly requests it.
- Blog and historical documentation routes are reserved but must remain disabled until their follow-up issues authorize content and route behavior.

## Content

- Keep placeholder copy minimal until a reviewed content issue supplies final homepage, shell, documentation, or landing-page text.
- Do not migrate AsciiDoc or copy legacy generated documentation in site-foundation tasks.
- Internal links must use canonical trailing-slash routes such as `/docs/`, not legacy aliases.

## Theme and accessibility

- Consume shared product tokens from `@sniffy/theme` entry points instead of duplicating product palette values.
- Site-local Infima or Docusaurus adapter variables may alias shared tokens, but must not redefine their product meaning.
- Preserve semantic headings, named landmarks, accessible link text, visible focus, and sufficient contrast for all pages.

## Build and verification

- Use Node 24+ from the `sniffy-ui` workspace.
- For site changes, run `npm ci`, `npm run site:build`, `npm run site:test`, `npm run lint`, `npm run typecheck`, `npm run test`, `npm run storybook:build`, `npm run build`, `npm run check:generated`, `npm run check:bundle`, `npm audit`, and applicable Playwright coverage unless an environment limitation is reported exactly.
- Verify ordinary Maven builds still do not require Node when changing site workspace wiring.
- Do not commit review-only screenshots, Docusaurus build output, or local preview artifacts.
