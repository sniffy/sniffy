# Frontend development

The private [`sniffy-ui`](../sniffy-ui/) npm workspace owns both browser surfaces. `apps/profiler` builds the classic, self-contained injected script; `apps/agent` builds stable files for the embedded Java agent server. `packages/theme`, `packages/ui`, `packages/api`, and `packages/fixtures` are shared. The workspace is not published to npm or Maven.

Use Node 24 or newer:

```bash
cd sniffy-ui
npm ci
npm run dev
npm run storybook
```

The playground prints direct URLs for the legacy-equivalent page, a hostile nested-path page, and the agent UI. It runs on ports 3000 and 3001 so path rewriting and real CORS behavior are testable without MSW. Unexpected requests fail with HTTP 500.

`npm run build` writes the profiler to `sniffy-web-common/src/main/resources/io/sniffy/ui/` and the agent files to `sniffy/src/main/resources/web/`. Maven consumes committed outputs and never requires Node. Always run `npm run check:generated` and `npm run check:bundle`; the latter requires one classic profiler IIFE, rejects dynamic imports/eval/runtime stylesheets/placeholders, and enforces 200 KiB gzip.

The profiler derives its backend from the resolved `#sniffy-header` script URL, retains relative request-detail resolution and XHR interception, and mounts one `<sniffy-profiler>` with an open ShadowRoot. Styles and portals stay inside that root, and the host page is never restyled. The production target is ES2020 with Chromium 90+, Firefox 88+, and Safari 14+ as the documented baseline.

`npm run test` runs unit tests plus Storybook interaction/accessibility tests in Chromium. Install all browser engines with `npx playwright install chromium firefox webkit`, then use `npm run test:e2e` for behavior coverage in Chromium, Firefox, and WebKit. Visual baselines live in `tests/e2e/visual-baselines/` and are updated only with `npm run test:e2e:update` in the pinned Playwright Linux/Chromium environment used by CI; inspect both profiler and agent images before committing them.
