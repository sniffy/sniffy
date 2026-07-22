# Frontend development

The private [`sniffy-ui`](../sniffy-ui/) npm workspace owns both browser surfaces. `apps/profiler` builds the classic, self-contained injected script; `apps/agent` builds stable files for the embedded Java agent server. `packages/theme`, `packages/ui`, `packages/api`, and `packages/fixtures` are shared. The workspace is not published to npm or Maven.

Use Node 24 or newer:

```bash
cd sniffy-ui
npm ci
npm run dev
npm run storybook
```

Choose the workflow that matches the work:

- `npm run storybook` provides fast isolated component and product-state development with Storybook HMR.
- `npm run dev` starts the full frontend environment: the real playground backend, agent Vite HMR, and profiler automatic rebuild/full-page reload.
- `npm run dev:agent` focuses on the agent and serves the committed profiler.
- `npm run dev:profiler` focuses on the injected profiler and serves the committed agent.
- `npm run preview:generated` serves both committed Maven resources without HMR or source watching.
- `npm run build` is the only normal command that regenerates committed production resources.

The playground prints direct URLs for `http://127.0.0.1:3000/mock/mock.html`, `http://127.0.0.1:3000/mock/hostile/nested/page`, and `http://127.0.0.1:3000/agent/`. It runs on ports 3000 and 3001 so path rewriting and real CORS behavior are testable without MSW. Unexpected requests fail with HTTP 500.

The development orchestrator owns both HTTP listeners, the agent Vite middleware/HMR server, the profiler watcher, and the profiler reload stream. Agent source updates use React Fast Refresh at the stable `/agent/` route while API calls continue through the same real Express backend. The profiler remains a classic injected IIFE; Vite watches its complete imported module graph and writes only to ignored `sniffy-ui/.tmp/dev/profiler/`. After each successful rebuild, an SSE notification reloads the complete host page. A full reload intentionally creates a fresh global XHR interceptor, React root, and custom element instead of repeatedly injecting scripts into one document. A failed build is logged and sends no reload; a later valid edit rebuilds normally.

Development and generated-resource preview responses use `Cache-Control: no-store`, which avoids stale assets during local review rather than modeling long-lived production caching. SIGINT and SIGTERM close reload streams, HTTP listeners, Vite/HMR sockets, and the watcher; one Ctrl-C should return the terminal without orphan listeners. Startup removes only the orchestrator-owned `.tmp/dev` tree. No development or preview command writes committed Maven resources.

`npm run build` writes the profiler to `sniffy-web-common/src/main/resources/io/sniffy/ui/` and the agent files to `sniffy/src/main/resources/web/`. Maven consumes committed outputs and never requires Node. Always run `npm run check:generated` and `npm run check:bundle`; the latter requires one classic profiler IIFE, rejects dynamic imports/eval/runtime stylesheets/placeholders, and enforces 200 KiB gzip.

The profiler derives its backend from the resolved `#sniffy-header` script URL, retains relative request-detail resolution and XHR interception, and mounts one `<sniffy-profiler>` with an open ShadowRoot. Styles and portals stay inside that root, and the host page is never restyled. The production target is ES2020 with Chromium 90+, Firefox 88+, and Safari 14+ as the documented baseline.

`npm run test` runs unit tests plus Storybook interaction/accessibility tests in Chromium. Install all browser engines with `npx playwright install chromium firefox webkit`, then use `npm run test:e2e` for behavior coverage in Chromium, Firefox, and WebKit. Visual baselines live in `tests/e2e/visual-baselines/` and are updated only with `npm run test:e2e:update` in the pinned Playwright Linux/Chromium environment used by CI; inspect both profiler and agent images before committing them.

The existing `Check Pull Request` workflow includes an always-on `Website` job for every pull request. It reports locked dependency installation, lint, formatting, typecheck, site contract tests, the Docusaurus production build, and desktop/mobile Playwright smoke coverage as separate steps, then uploads the complete `apps/site/build/` directory as a downloadable artifact. The artifact includes a README, the CI Node version pin, and a dependency-free `preview.mjs` launcher. After extraction, `node preview.mjs` serves the unchanged production build on macOS, Linux, or Windows; opening `index.html` through `file://` is intentionally unsupported because the production site uses `baseUrl: /`. CI starts that packaged launcher and verifies `/`, `/docs/`, and the branded 404 response over HTTP before upload. The job also runs with the workflow's existing manual and `develop`-push triggers; it stays independent from Maven publication and never deploys the site.

The branded site shell aliases shared semantic tokens in `apps/site/src/css/custom.css`. Variables prefixed
`--sniffy-site-` are private Docusaurus layout decisions; product identity, semantic colors, typography, spacing,
and radii remain owned by `@sniffy/theme`. Review shell changes with `npm run test:site`, including the committed
desktop/mobile light/dark baselines and accessibility audit. The review artifact remains the easiest way to inspect
the unchanged production build on another machine: download it, extract it, run `node preview.mjs`, and open the
printed HTTP URL.
