# Repository agent instructions

These rules apply to every change in this repository. More specific `AGENTS.md` or `AGENTS.override.md` files add
constraints for their subtrees.

## Authority and task ownership

- Follow, in order: explicit maintainer decisions; the current GitHub issue or pull request; the nearest applicable
  `AGENTS.md`; this file; executor runbooks and task templates. A lower level must not silently override a higher one.
- Read the complete issue, relevant comments, existing pull request, review threads, current code, and remote state before
  editing. Product, compatibility, public-API, and privileged-operator decisions belong to the maintainer.
- For autonomous work, implement the smallest coherent solution without routine clarification. Stop for missing authority,
  credentials, external infrastructure, destructive ambiguity, or contradictory requirements.
- The roles, executors, routing rules, delivery state machine, and verification ownership model live under
  [`docs/ai-delivery/`](docs/ai-delivery/README.md). They are operational guidance, not a replacement for repository policy.
- Before executor-specific work, read the applicable runbook under `docs/ai-delivery/executors/`. Codex Cloud work must read
  [`docs/ai-delivery/executors/codex-cloud.md`](docs/ai-delivery/executors/codex-cloud.md) before editing or publication.

## Delivery and Git safety

- Complete authorized work end to end when permissions allow: implement, test, inspect the final diff, commit, push, and
  create or update the intended pull request. Keep it draft only while implementation or locally applicable validation is
  incomplete.
- Before handing implementation to `Review`, ensure the intended pull request is open, targets `develop`, points at the exact
  published head, and is **not draft**. Mark it ready for review and re-read that state before submitting the guarded Project
  transition. When an issue is the canonical Project item, include the linked PR number and exact head required by
  `delivery-control/v1`; do not rely on prose in `Worker reference` to identify the PR.
- GitHub remote state is the publication source of truth. Before reporting delivery, verify the remote branch, full SHA,
  pull-request URL, base/head refs, draft state, and matching pull-request head.
- An absent `origin` in an isolated Sniffy checkout is recoverable configuration, not by itself a publication blocker.
  Configure its push URL as `https://github.com/sniffy/sniffy.git` without embedded credentials, verify authentication, and
  attempt the authorized push before reporting an access blocker.
- Preserve unrelated work. Do not reset, clean, rebase shared branches, force-push, rewrite history, create a replacement PR
  for a continuation, merge, or enable auto-merge unless Dmitry explicitly authorizes that exact action.
- Prefer a fresh `agent/<short-description>` branch from current `develop` for new work. Continue review fixes on the exact
  existing branch and pull request.

## Scope, architecture, and compatibility

- Preserve Java 8 source, bytecode, API, and runtime compatibility unless the issue explicitly changes the supported
  baseline. Keep JDK-specific behavior behind capability probes, isolated modules, or Maven profiles.
- Keep changes inside the requested behavior. Preserve public contracts and user changes; do not turn a focused task into
  an adjacent redesign or broad dependency modernization.
- A task combining two or more high-risk axes—public APIs/artifacts, global state, concurrency or cleanup, failure
  composition, several JDK/framework versions, cross-reactor placement, or migration compatibility—requires an issue-level
  design/proof preflight before implementation.
- The preflight must resolve scope boundaries, ownership, lifecycle/failure behavior, compatibility matrix, delivery
  topology, and an acceptance-to-proof matrix. Green CI is not a substitute for unresolved design.

## Standard tooling and infrastructure

- Prefer maintained tools, official actions, platform features, and ecosystem-standard declarative configuration for
  build, CI, release, dependency, security, formatting, linting, coverage, packaging, reporting, and scaffolding work.
- Bespoke infrastructure requires a maintainer-approved issue rationale covering the observable gap, alternatives,
  smallest custom surface, owner, permissions/security, deterministic tests, compatibility/upgrade path, operational
  failure semantics, and removal condition.
- A new workflow or materially new job additionally requires documented triggers, least privilege, blocking semantics,
  expected signal/noise, maintainer failure response, lifecycle, and overlap with existing platform signals.
- If shared infrastructure is discovered inside unrelated work, stop and refine a separate issue instead of expanding the
  current pull request. Additional workflow-specific rules live in [`.github/AGENTS.md`](.github/AGENTS.md).

## Verification

- Convert every acceptance criterion into executable or inspectable proof. Confirm named tests were discovered and ran;
  compiled fixtures, skipped modules, and an overall green build do not prove unused coverage.
- Run focused checks first. Run independent obligations as separate commands with visible exit statuses. Never describe an
  interrupted, timed-out, retried, skipped, or ignored command as passing.
- After switching JDKs in one worktree, run `clean` before the first Maven build under the new JDK or use isolated outputs.
  Do not execute stale classes compiled against a different JDK.
- For Java 8 compatibility-sensitive artifacts, distinguish clean Java 8 tests, modern-JDK API-signature verification, and
  execution on real Java 8 of the same modern-JDK-built artifact when required.
- For NIO changes, follow [`sniffy-module-nio/AGENTS.md`](sniffy-module-nio/AGENTS.md). For TLS changes, run
  `mvn -pl sniffy-module-tls -am clean test` on Java 8 and the current development JDK.
- For cross-module or release-facing changes, run
  `mvn -T 1C -B clean verify --file pom.xml -U -P ci -Dgpg.skip=true -Dmaven.wagon.http.retryHandler.count=3`.
- Run `git diff --check` before committing. Report every required check not run and its exact reason.
- Frontend and visual proof rules live in [`sniffy-ui/AGENTS.md`](sniffy-ui/AGENTS.md); the site adds narrower rules in
  [`sniffy-ui/apps/site/AGENTS.md`](sniffy-ui/apps/site/AGENTS.md).

## Concurrency and resource safety

- Deterministic concurrency tests are executable contracts. Do not add retries, timing sleeps, weakened assertions, or
  increased timeouts to hide ordering defects.
- Instrumentation failure must not prevent physical close, shutdown, cancellation, wakeup, or cleanup. Preserve the first
  failure and suppress later cleanup failures.
- Keep application callbacks outside private JDK/provider locks and internal construction scopes. Preserve documented lock
  order and ownership.

## Pull requests and review

- Pull-request descriptions must explain the problem, design, compatibility impact, dependency changes, tests and checks,
  limitations, remaining risks, and exact published head SHA.
- Review the complete exact-head diff, issue/proof matrix, unresolved threads, generated and unrelated files, actual test
  execution, artifacts or functional behavior when relevant, and all required CI. Prefer one comprehensive first review
  over serial discovery of independent findings.
- Formal approval or Request Changes must use an identity independent from the PR author. When that is impossible, state
  the limitation and leave precise blocking or ready-for-human-review feedback without pretending a formal review occurred.
- Returning work to an executor requires an explicit continuation dispatch and the monitoring cadence in
  [`docs/ai-delivery/supervision.md`](docs/ai-delivery/supervision.md). A review comment alone does not prove work started.
- Approval is not merge. Never merge or enable auto-merge without Dmitry's explicit instruction.