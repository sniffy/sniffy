# Frontend workspace agent instructions

These rules apply under `sniffy-ui/` in addition to the repository root instructions. More specific application instructions,
such as `apps/site/AGENTS.md`, add narrower constraints.

## Workspace and generated resources

- Use Node 24+ and the root npm workspace commands. The workspace is the source for the profiler UI, SniffyAgent UI,
  playgrounds, shared packages, Storybook, and the Docusaurus site.
- Do not hand-edit generated Java resources. Change frontend sources, run the production build, and verify
  `npm run check:generated` reproduces committed outputs.
- Keep profiler CSS and Base UI portals inside the open ShadowRoot. Do not add dynamic imports, runtime assets, host-page
  mutations, global CSS leakage, or absolute backend assumptions without an issue-scoped design.
- Reuse exact workspace React/React DOM versions and existing shared packages unless a task explicitly authorizes a split.

## Dependencies and tooling

- Prefer compatible direct parent upgrades and maintained tooling. Do not add audit suppression, blanket overrides,
  unsupported transitive major overrides, or a second dependency-security framework.
- GitHub Dependency Review is the causal PR gate for newly introduced vulnerabilities. Use `npm audit` for dependency
  diagnosis and remediation evidence, not to broaden unrelated feature work.
- Storybook, browser tests, and CI must not require an external AI or MCP service.

## Verification

For frontend changes, run the applicable independent obligations:

- `npm ci`;
- lint and formatting;
- TypeScript validation;
- focused Vitest or Storybook interaction tests with coverage;
- Storybook build;
- production build;
- `npm run check:generated`;
- `npm run check:bundle`;
- applicable Playwright projects;
- site build, search, artifact, and browser checks when site surfaces are affected;
- `git diff --check`.

Report any obligation not run and why. Do not update generated files or snapshots merely to make a failing check green.

## Visual evidence

- For every intentional visual change, review representative desktop and mobile states, affected light/dark themes, and
  important interaction states. Identify application or route/story, viewport, browser, theme, and state.
- For a visual-neutral refactor, prove the baseline did not change or provide equivalent exact-head artifact evidence.
- Environment-specific screenshot failures require exact base/head reproduction before baseline changes. Never update a
  baseline unless the visual change is intentional and approved.
- When the executor cannot publish screenshots directly, preserve Playwright outputs as GitHub artifacts or use the
  repository screenshot-publishing skill after inspecting every selected image.
