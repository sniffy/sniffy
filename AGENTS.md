# Repository agent instructions

These rules apply to every change in this repository. More specific `AGENTS.md` or `AGENTS.override.md` files may add
constraints for their subtrees.

## Task ownership and autonomy

- Treat the linked GitHub issue and its acceptance criteria as the source of truth. Read the complete issue, relevant
  comments, existing pull requests, and current code before editing.
- For tasks explicitly marked ready for autonomous execution, do not stop for routine design choices or ask for
  confirmation. Inspect the repository, choose the smallest maintainable solution consistent with the issue, and record
  material decisions in the pull request. Stop only for a real blocker such as missing credentials, unavailable external
  infrastructure, destructive ambiguity, or mutually incompatible acceptance criteria.
- Complete the task end to end when permissions allow: implement, add or update tests, run applicable checks, review the
  diff, commit, push, and update or open a pull request. Use a draft while implementation or locally applicable
  verification is still in progress; unless the task explicitly requires a draft handoff, mark it ready for review after
  the implementation and those checks are complete. Never merge a pull request unless the task explicitly says to do so.
- Treat GitHub remote state as the publication source of truth. A local branch, local commit, `make_pr` metadata, or a
  final summary does not prove delivery. Before reporting publication, verify a resolvable remote branch, full commit SHA,
  pull-request URL, base/head branches, draft state, and matching head SHA through GitHub.
- Preserve unrelated user changes. Do not reset, clean, force-push, rebase shared branches, or rewrite history unless the
  issue explicitly requires it. Prefer a new `agent/<short-description>` branch from `develop` for new work.
- Before editing a task that combines two or more high-risk axes—new public APIs or artifacts, global mutable state,
  resource lifecycle/failure composition, multiple JDK/framework versions, or cross-reactor module placement—confirm
  that the issue contains a short design preflight: scope boundaries, module ownership, a failure/lifecycle matrix, and
  an acceptance-criterion-to-proof matrix. If a material product or public-API decision is unresolved or contradictory,
  stop before implementation and report that decision instead of inventing a broad compatibility layer.

## Compatibility and scope

- Preserve Java 8 source, bytecode, and runtime compatibility unless a task explicitly changes the supported baseline.
  Do not use post-Java-8 language features or APIs in shared production or test sources. JDK-specific integrations must
  stay behind capability probes, isolated modules, or Maven profiles.
- Keep changes inside the requested behavior. Preserve public contracts and existing user changes; do not turn a
  focused fix into an unrelated redesign.
- Prefer the smallest compatible dependency update. Do not perform broad dependency modernization as part of an
  unrelated task. Document security-motivated upgrades and compatibility trade-offs in the pull request.

## Concurrency and lifecycle

- Treat deterministic concurrency tests as executable contracts. Do not weaken assertions, add retries, retry a failed
  test run to green, replace latches/hooks with timing sleeps, or increase timeouts to hide an ordering defect.
- Instrumentation is secondary to application resource safety. Monitoring, proxy parsing, logging, registry, or
  callback failure must not prevent required physical close, shutdown, cancellation, wakeup, or cleanup. Preserve the
  first failure and suppress later cleanup failures when multiple steps fail.
- Keep application callbacks out of private JDK/provider locks and internal construction scopes. Preserve documented
  lock order and ownership rather than relying on a particular OS/JDK implementation.

## Build and verification

- The Codex Cloud environment is described in `docs/codex-workflow.md`. Use `source .codex/cloud/use-jdk.sh <version>`
  to switch among the installed JDK 8, 11, 17, 21, and 25 toolchains.
- Run focused tests for the changed behavior first.
- After switching JDKs in the same worktree, run `clean` before the first Maven build under the new JDK or use an
  isolated checkout/output directory. Never treat `target/` classes compiled by another JDK as evidence for the current
  runtime; Maven incremental compilation can otherwise reuse bytecode linked to APIs or covariant method descriptors
  that do not exist on Java 8.
- For a Java 8 artifact compiled on a newer JDK, `source`/`target` bytecode levels alone are insufficient. Run the
  configured Java 8 API-signature check during `verify` and, for compatibility-sensitive changes, execute the same
  modern-JDK-built artifact on a real Java 8 runtime without recompiling it there.
- Run independent validation obligations as separate commands with individually visible exit statuses. Do not combine
  multiple JDK/module checks into one opaque long-running chain. A manual interruption is not a result; if a command is
  intentionally time-bounded, use an explicit timeout, report that timeout, and leave the complete reactor result to CI.
- Treat every acceptance criterion as an evidence obligation. Verify that its focused test is discovered and executed
  in the test report; compiled tests, unused fixtures, skipped modules, and an overall green CI result are not proof.
- For NIO changes, run `mvn -pl sniffy-module-nio -am clean test` on Java 8 and the current development JDK.
- For TLS changes, run `mvn -pl sniffy-module-tls -am clean test` on Java 8 and the current development JDK.
- For cross-module or release-facing changes, run
  `mvn -T 1C -B clean verify --file pom.xml -U -P ci -Dgpg.skip=true -Dmaven.wagon.http.retryHandler.count=3`.
- Run `git diff --check` before committing. A failing or skipped required check must be reported with its exact reason;
  never describe a partial, retried, or ignored run as passing.

## Frontend work

- The private `sniffy-ui/` npm workspace is the source for both the injected profiler and `SniffyAgent` UI. Use Node 24+
  and the root workspace scripts; do not hand-edit generated Java resources.
- Keep profiler CSS and Base UI portals inside its open ShadowRoot. Do not add dynamic imports, runtime assets, host-page
  mutations, global CSS, or absolute backend assumptions.
- Run lint, typecheck, Vitest, Storybook build, production build, generated-resource comparison, bundle validation, npm
  audit, and Playwright for UI changes. Review `npm run dev` playground pages and visual diffs before updating baselines.
- Storybook's MCP addon is optional for local exploration. CI and tests must never require an external AI or MCP service.

### Visual evidence contract

- This contract applies to frontend and UI changes, not backend-only or documentation-only changes. For every intentionally
  changed visual surface, attach before and after screenshots covering representative desktop and mobile viewports where
  applicable, every affected light/dark theme, and important interaction states.
- For a visual-neutral refactor, attach identical before and after screenshots or link directly to downloadable visual-test
  artifacts that prove the baseline did not change.
- Identify the application, route or story, viewport, browser, theme, and state in each filename or caption so reviewers
  can map every image to the surface it proves.
- If a screenshot test fails only in a particular environment, reproduce it against both the exact base SHA and head SHA
  in that environment and attach or link the resulting diff. Never update a visual baseline unless the visual change is
  intentional and explicitly approved.
- If Codex Cloud cannot embed screenshots directly, run Playwright in the supported CI environment, upload the visual
  outputs as downloadable artifacts, and link those artifacts from the pull request. Lack of local GUI access alone does
  not satisfy the visual-evidence requirement.

## Pull requests

- Keep pull requests draft only while implementation or locally applicable verification is incomplete. Unless the task
  explicitly requires a draft handoff, mark the pull request ready for review after both are complete and remote
  publication has been verified.
- When credentials and the delivery contract permit it, create the pull request using the authenticated agent account
  rather than asking a human to create it manually. This preserves formal human review and Request Changes capability.
- Cloud checkouts may not have an `origin` remote. Use an explicit repository URL or configure the intended remote rather
  than treating a missing remote as successful local completion.
- The pull request description must explain the problem, design, compatibility impact, tests executed, checks not run,
  dependency changes, and remaining risks. Link the authoritative issue and include the exact published head SHA.
- Do not hide limitations. If the environment cannot run a platform-, JDK-, or credential-dependent check, say exactly
  what is missing and leave CI to perform that check.
- A `REQUEST_CHANGES` review does not dispatch or resume Codex Cloud. Whenever a maintainer expects an agent to continue
  after review, the maintainer must also post a separate, explicit `@codex` follow-up comment that summarizes every
  blocking review item and tells the agent not to merge.
- After that dispatch comment, schedule a status check for 15 minutes later and a second check 15 minutes after the first;
  if the work is still incomplete, continue monitoring hourly. Do not mark the task as actively being fixed merely
  because a review was submitted: verify that the explicit dispatch comment exists and that the agent has acknowledged
  it or pushed a new commit. This dispatch and monitoring contract applies to every agent-authored pull request.

## Review guidelines

- Prioritize correctness, compatibility, resource safety, concurrency, public API stability, and test integrity over
  formatting preferences.
- Review the complete acceptance-to-proof matrix on the first pass. Avoid serially discovering independent missing
  requirements across multiple fix cycles when they could be reported together.
- Flag tests that were weakened, made timing-dependent, skipped, or retried to conceal a failure.
- Flag accidental Java baseline increases, use of newer JDK APIs in Java 8 artifacts, and unintentional dependency or
  public API changes.
- Flag resource leaks or cleanup paths where Sniffy instrumentation can prevent application resources from closing.
