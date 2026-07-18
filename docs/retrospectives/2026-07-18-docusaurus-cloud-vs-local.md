# Docusaurus foundation: Cloud versus local Codex retrospective

Date: 2026-07-18

Compared work:

- issue [#658](https://github.com/sniffy/sniffy/issues/658);
- local Codex pull request [#681](https://github.com/sniffy/sniffy/pull/681);
- independent Codex Cloud pull request [#682](https://github.com/sniffy/sniffy/pull/682).

## Outcome

Both final implementations satisfied #658 and passed their complete pull-request workflows. The local implementation is the preferred base because it reached a correct, narrowly scoped design in one commit, uses real Chromium production-route tests, has clearer site-specific agent instructions, and explicitly encodes canonical current-doc routing. The Cloud implementation became acceptable after three substantive review/fix rounds and contributed a useful dependency-remediation improvement: narrower `serialize-javascript` overrides produced a clean audit.

Routing conclusion: the feature was not too large for Cloud by file count, but it was at the upper edge of a good Cloud task because it combined many independent risk axes. It was suitable for Cloud only with an explicit acceptance-to-proof matrix and negative scope checklist. Without those, review became the place where missing requirements were discovered.

## Measured delivery

Cloud pull request #682:

- initial implementation commit: `d41c4bc114ed52061010df19de698f8ccb069c38`;
- first fix: `3701a024f25059f86970d710adee08d2ac3003d2`;
- second fix: `08dcc819d6843cdae45d7db7b1e5949ed569ccda`;
- final fix: `f03e82aea28a5d9a8f8779657473da978cdf4d7f`;
- three `REQUEST_CHANGES -> fix` rounds before approval;
- PR creation to approval: 2 hours 36 minutes 19 seconds;
- approximate dispatch to approval: about 3 hours 4 minutes.

The wall-clock duration includes CI, Maven matrix execution, and scheduled review intervals; it is not continuous coding time.

Local pull request #681 reached the reviewed implementation in one commit and required no review/fix round before approval.

## What Cloud missed initially

### Acceptance criteria were not converted into durable proof

The first `site:test` only built Docusaurus a second time. It did not prove reserved-route 404 behavior, workspace command isolation, unchanged Java-resource build semantics, shared-theme imports, or absence of copied palette literals.

Future dispatches must map each acceptance criterion to a named test, CI job, static assertion, or explicitly documented manual check before editing begins.

### Non-goals were insufficiently concrete

The first pass added navbar/footer configuration, copyright, a favicon path without an asset, hero presentation, an eyebrow, CTA styling, and extra product copy. These were reasonable guesses for a “minimal site” but belonged to later shell/homepage work.

For high-boundary tasks, write negative scope as concrete forbidden outputs, not only broad prose. Example:

```text
No navbar or footer configuration.
No favicon, logo, copyright, hero layout, CTA styling, or final product copy.
The homepage may contain only Layout, main, h1, one short paragraph, and one docs link.
```

### Repository alignment was not treated as a hard invariant

Cloud selected React 18 independently even though the workspace used React 19 and Docusaurus accepted it. New frontend workspaces should reuse exact existing React and React DOM versions unless an issue explicitly authorizes a split.

### Verification commands were underspecified

`mvn -DskipTests validate` was reported as no-Node proof even though the acceptance criterion required the normal reactor build. When a command is part of acceptance, provide the exact command and precondition in the issue, for example a full `clean verify` after proving `command -v node` is empty.

### Dependency remediation optimized the report before compatibility

A temporary `sockjs -> uuid 11.1.1` override crossed the dependency's declared major range to remove moderate findings, while the affected development path was not exercised by CI. Security overrides must stay inside declared compatibility ranges unless a dedicated runtime test proves the major override.

### Cross-platform filesystem behavior was not considered in the first helper

The first smoke server used `URL.pathname` as a filesystem path and string-prefix containment, which was unsafe or incorrect on Windows. Node filesystem helpers in a multi-platform repository must use `fileURLToPath`, `path.relative`, and explicit drive/separator reasoning.

### New verification code introduced a security finding

The initial smoke server returned `String(error)` to HTTP clients, exposing filesystem details. Diagnostics should be logged internally while clients receive a generic error response. Full CI and CodeQL remain necessary even for test-only helpers.

### PR metadata was treated as a one-time handoff

After fixes, the PR description remained stale: old head SHA, obsolete audit state, incomplete Maven proof, and inaccessible `/tmp` screenshot references. Before reporting completion, the agent must re-read the remote PR body and reconcile every SHA, command result, limitation, and CI statement with the current head.

## Cloud versus local routing policy

### Prefer Codex Cloud when

- the task starts from a clean branch;
- all required context is committed or available through GitHub;
- the scope is isolated and product decisions are already resolved;
- verification is deterministic and available in the Cloud environment;
- no local service, device, private network, unusual Git history, or persistent artifact inspection is required;
- the task has few independent risk axes, or a complete preflight/proof matrix is already written;
- an asynchronous 20–90 minute implementation plus CI loop is acceptable.

Typical examples: focused fixes, regression tests, ordinary refactors, dependency updates, documentation changes, and later site routes that follow an established pattern.

### Prefer unattended local Codex when

- the task defines a new architectural or testing pattern for later agents;
- browser inspection, interactive UI work, screenshots, or local visual comparison are central;
- local services, Docker, hardware, credentials, special networking, or OS-specific behavior are required;
- existing PR/history surgery or unrestricted Git operations matter;
- the task is likely to change direction after human inspection;
- many independent risk axes interact and persistent local artifacts or long-running debugging reduce iteration cost.

Typical examples: first foundations for a subsystem, design-system and shell work, profiler interaction changes, new public APIs, complex concurrency/lifecycle behavior, and cross-platform tooling whose behavior must be inspected locally.

### Risk-axis rule

Do not classify size by changed-line count or lockfile size. Count independent decision/proof axes instead. The Docusaurus foundation combined:

1. npm workspace architecture;
2. Docusaurus route/version policy;
3. theme-token ownership;
4. React compatibility;
5. dependency security;
6. browser/runtime proof;
7. cross-platform Node filesystem behavior;
8. Maven/no-Node isolation;
9. visual evidence publication;
10. PR handoff accuracy.

A task with several such axes may still go to Cloud, but only after a read-only preflight and a complete proof matrix. After two substantive review/fix rounds, re-baseline the issue and consider moving the same branch to local Codex rather than continuing narrow Cloud patches.

## Required acceptance-to-proof template

For Cloud tasks that introduce a new module, workflow, site, public contract, or compatibility boundary, add a table like this before dispatch:

| Requirement | Allowed implementation boundary | Required proof |
| --- | --- | --- |
| Workspace/module placement | Exact directory and package/module ownership | Committed contract assertion |
| Runtime routes or behavior | Named routes/states | Real runtime or browser tests |
| Reserved/disabled behavior | Exact forbidden routes/features | Negative assertions with expected status/result |
| Shared ownership | Named shared package/API | Static import/config assertion |
| Existing build semantics | Exact command or artifact invariant | Static assertion plus generated-output check |
| Platform support | Named supported platforms | Portable APIs and platform-specific CI where needed |
| Dependency/security change | Allowed version/range policy | Audit plus compatibility/runtime verification |
| External isolation | Exact environment precondition | Exact command proving absence/presence |
| Delivery | Branch, PR, draft/ready state | Remote SHA/body/check reconciliation |

Also include a negative scope checklist naming tempting adjacent features that must not appear.

## Improvements to retain from Cloud #682

When finishing local PR #681, evaluate and selectively port:

- targeted `serialize-javascript` overrides rather than one global override;
- the newest compatible patched version that remains green;
- a clean `npm audit --audit-level=high`, and ideally a fully clean audit, without unsupported major transitive overrides;
- explicit assertions that React versions remain aligned with the existing workspace;
- explicit test coverage that site dev/build/test scripts delegate only to `@sniffy/site`;
- generic external error responses for any test HTTP server while preserving diagnostics in stderr.

Do not replace the local real-browser production-route tests with the Cloud static-server smoke test. Do not add the Cloud sidebar or extra direct dependencies unless a concrete requirement needs them.
