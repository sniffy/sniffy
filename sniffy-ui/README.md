# Sniffy frontend workspace

Private npm workspace for the injected profiler and `SniffyAgent` connectivity page. It is built into committed Maven resources and is not published independently.

Requires Node 24 or newer. Run `npm ci`, then `npm run dev` for the real two-origin playground, `npm run storybook` for shared components and product states, and `npm run build` to regenerate Java resources. `npm run check:generated` rebuilds in a temporary directory and byte-compares every committed output; `npm run check:bundle` enforces the one-file/eval-free profiler contract, the 200 KiB gzip budget, stable agent assets, and machine-neutral source maps.

Production browser baseline: Chromium 90+, Firefox 88+, and Safari 14+. The profiler is a classic IIFE with an open Shadow DOM and no runtime frontend asset requests. The agent UI is an ES2020 module served by the embedded agent server.
