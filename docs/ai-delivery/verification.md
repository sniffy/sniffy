# Implementation, review, and verification evidence

Implementation, Review, and Verification are different lifecycle statuses with different questions. The issue chooses both an
`Implementer` and a `Verifier` during Planning. Skipping duplicate work is allowed; collapsing the responsibilities into one
unexamined summary is not.

## Responsibility split

### Implementer: prove the change works locally

The Implementer owns the code/configuration change and the proof available in its implementation environment. It must:

- convert acceptance criteria into focused test obligations before editing;
- run the smallest relevant unit/module/integration checks first;
- run broader repository checks required by root or nested `AGENTS.md` when practical;
- start and inspect the changed UI/service in a browser or runtime when the task requires it and the environment supports it;
- confirm named tests were discovered and executed;
- inspect the complete final diff, generated output, dependencies, and unrelated-file scope;
- run `git diff --check`;
- publish the exact branch/SHA/PR and report commands, results, skipped obligations, and capability limitations honestly.

The Implementer must not use a later Verification lifecycle status as permission to skip ordinary local tests or a directly
available browser check. Conversely, it must not claim a source build, integration run, package download, or browser session it
could not perform.

### Reviewer: decide whether the implementation is acceptable code

The Reviewer is primarily code- and design-oriented. It must inspect:

- the complete exact-head diff relative to the current base;
- issue decisions, non-goals, thread updates, and acceptance-to-proof matrix;
- architecture, module/API ownership, compatibility, lifecycle, failure composition, resource safety, and security;
- test design and whether the tests would fail for the original defect;
- implementer command evidence and actual test discovery;
- dependencies, workflows, permissions, generated files, documentation, migration, and rollback;
- unresolved review comments and threads;
- exact-head CI statuses and relevant job logs when they materially support or contradict the implementation claim.

The Reviewer may run a focused reproduction when useful, but is not required to build a complete representative deployment
or repeat every user journey. Its outputs are:

- return to `Implementation / Ready` with one comprehensive Request Changes package;
- advance to `Verification / Ready` when outcome proof is still required;
- advance to `Approval / Ready` when the issue is low-risk and existing implementer/CI evidence already provides sufficient
  independent proof.

CI is evidence for Review, not a substitute for reading the code.

### Verifier: decide whether the delivered outcome matches the task

The Verifier is primarily outcome-oriented. It re-reads the authoritative issue and later discussion, then exercises the
published exact head or exact artifact in the most representative available environment. It should:

- identify the observable user/developer journey and negative cases promised by the issue;
- run the real integration, service, browser, platform, compatibility, or deployment path rather than only re-running unit
  tests;
- use the same published artifact when compatibility or packaging is part of the requirement;
- inspect runtime logs, browser errors, failed requests, screenshots, persisted state, cleanup, and externally visible output;
- compare actual behavior with both the issue and explicit thread decisions;
- record exact source/artifact identity, environment, commands/actions, and result;
- classify failure as implementation defect, verification-harness defect, requirement/architecture defect, missing capability,
  decision/action required from Dmitry, or infrastructure noise.

Examples include:

- start a minimal Tomcat application with Sniffy, execute a Hello World request, open the Sniffy UI, click the relevant
  controls, and confirm the captured request/SQL/network behavior and browser console state;
- run the modern-JDK-built artifact on a real Java 8 runtime without recompilation;
- deploy an exact website artifact to a local HTTP server, navigate representative desktop/mobile routes, exercise search or
  version selection, and inspect errors and screenshots;
- execute a rollback or failure-injection path and verify cleanup and externally visible failure composition.

Verification is not merely “CI is green” and not a second full code review. The Verifier may inspect code or logs to diagnose
a failure, but its acceptance decision is based on observable behavior against the task.

### Approval: human acceptance

Dmitry performs final acceptance in `Approval / Ready`. He may rely on the Review and Verification packets, inspect the result
subjectively, request more Planning/Implementation/Verification work, or merge/close. No automated lifecycle status may infer
merge from technical approval.

## Proof matrix

Before Implementation, map every requirement or risk to:

- the allowed module/API/configuration boundary;
- implementer-owned local proof;
- reviewer evidence to inspect;
- verifier-owned outcome proof when required;
- the exact environment and artifact;
- negative proof for forbidden or disabled behavior.

Example:

| Requirement | Implementer proof | Reviewer check | Verifier proof |
| --- | --- | --- | --- |
| Sniffy UI shows a captured servlet request | focused UI/integration tests and local browser smoke when available | diff, test quality, CI job and artifact identity | run minimal Tomcat + Sniffy, make request, click UI, inspect browser/logs |
| Java 8 runtime compatibility | clean Java 8 tests plus API-signature verification | bytecode/API changes and exact matrix logs | run the same modern-JDK-built artifact on real Java 8 |
| Website archive search isolation | Playwright contract and build | route/index code and exact-head Website job | serve exact artifact, search current/archive contexts, inspect links/errors |

For complex tasks, distinguish setup, body/user failure, framework failure, cleanup failure, and combined failure. State whether
compatibility tests execute the same artifact or rebuild sources.

## Capability-aware routing

The proof owner must be able to perform the proof in its actual environment. Do not route based only on model intelligence.
Confirm source, dependencies, network, runtimes, containers, browser, credentials, publication, and identity.

Examples:

- Current ChatGPT may use the GitHub connector, run installed Java, inspect/download CI artifacts through supported tools,
  serve local files, and drive installed Chromium, while its command sandbox may have no outbound internet and cannot fetch
  Maven/npm dependencies. Use an existing exact-head artifact or route the source build to Codex/local CI.
- Codex Cloud depends on committed setup, cache, allowed network policy, and task-window limits. A missing dependency or local
  service must be routed rather than hand-waved.
- Headless Local Codex can use persistent caches, Docker, Tomcat, browsers, and configured network on its Linux host, but the
  host security and credentials policy must explicitly allow them.
- A human remains required for protected settings, secrets, subjective product acceptance, and destructive operations.

When capability is absent, record the exact missing obligation and change `Verifier`/`Executor` while keeping
`Execution = Ready`. Set `Execution = Blocked` only when Dmitry must decide or act; then set `Executor = Human`,
`Assignee = bedrin`, and record the exact request. Do not reinterpret an easier check as equivalent evidence.

## Exact-head rule

Evidence belongs to one immutable commit:

1. record the current PR head SHA;
2. inspect the complete diff relative to the current base;
3. verify comments and unresolved threads;
4. select workflow runs whose `head_sha` matches exactly;
5. verify artifacts, screenshots, logs, and summaries belong to that run/head;
6. rerun affected proof after every code change or meaningful base merge.

An earlier green run does not prove a newer head. Codecov, bot comments, local summaries, and PR descriptions are supporting
evidence only; reconcile them with current GitHub state.

## Honest execution claims

Never claim an action occurred unless it completed and its output was inspected:

- a compile is not a test;
- `curl` is not a browser test;
- a downloaded artifact is not a local source build;
- a screenshot shown in ChatGPT is not attached to GitHub;
- a dry-run push or worker summary is not publication;
- a static workflow diff is not a production deployment;
- a retry is a separate attempt and must not hide the original result.

## Test integrity

- Run independent obligations as separate commands with visible exit status.
- Use clean outputs or isolated worktrees when switching JDKs.
- Confirm named tests in Surefire, Vitest, Playwright, or CI output.
- Do not weaken assertions, add timing sleeps/retries, skip tests, ignore failures, or update baselines/generated output merely
  to manufacture green evidence.
- A time-bounded command must use an explicit timeout and be reported as timed out when it does.

Repository-specific commands remain in root and nested `AGENTS.md`; executor runbooks describe capabilities and setup rather
than a competing build policy.

## Browser, artifact, and visual verification

Prefer exact-head CI artifacts for independent website review when ChatGPT cannot build from source. Serve unmodified output
over real HTTP, load it with a supported browser, record JavaScript errors and same-origin failures, exercise relevant
interactions, and inspect every generated screenshot.

Use [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) for the ChatGPT sandbox procedure. Frontend visual evidence
requirements live in [`../../sniffy-ui/AGENTS.md`](../../sniffy-ui/AGENTS.md). Screenshot publication is optional tooling and
does not replace inspection.

## Completion packet

Before `Approval / Ready`, record:

- exact head and base;
- complete diff reviewed and review identity limitation, if any;
- implementer tests actually run;
- exact-head CI jobs/logs inspected;
- verifier environment and observable actions/results, or rationale for safely skipping a separate Verification status;
- comments/threads resolved;
- limitations and residual risk;
- no pending automated worker or stale claim.
