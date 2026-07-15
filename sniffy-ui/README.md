# Sniffy frontend workspace

Private npm workspace for the injected profiler and `SniffyAgent` connectivity page. It is built into committed Maven resources and is not published independently. Requires Node 24 or newer; start with `npm ci`.

Development commands:

- `npm run storybook` — fast isolated component and product-state development with Storybook HMR.
- `npm run dev` — the full real-backend environment: agent Vite HMR plus automatic profiler IIFE rebuild and host-page reload.
- `npm run dev:agent` — the real backend and agent Vite HMR, with the committed profiler.
- `npm run dev:profiler` — the real backend and watched profiler, with the committed agent.
- `npm run preview:generated` — production-like review of committed Maven resources, without source watching or HMR.
- `npm run build` — regenerate the committed production resources.

The stable pages are `http://127.0.0.1:3000/mock/mock.html`, `http://127.0.0.1:3000/mock/hostile/nested/page`, and `http://127.0.0.1:3000/agent/`; the second playground origin remains on port 3001. Profiler development output lives under ignored `.tmp/dev/`. It uses full-page reload, rather than HMR, so every edit gets a fresh injected script, global XHR interceptor, React root, and custom element. One Ctrl-C closes both playground listeners, the Vite server and HMR sockets, the profiler watcher, and reload streams.

Development and preview responses use `Cache-Control: no-store` to avoid stale local review. Development commands never update Maven resources. `npm run check:generated` rebuilds in a temporary directory and byte-compares every committed output; `npm run check:bundle` enforces the one-file/eval-free profiler contract, the 200 KiB gzip budget, stable agent assets, and machine-neutral source maps.

Production browser baseline: Chromium 90+, Firefox 88+, and Safari 14+. The profiler is a classic IIFE with an open Shadow DOM and no runtime frontend asset requests. The agent UI is an ES2020 module served by the embedded agent server.
