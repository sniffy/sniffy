# Codex Cloud and autonomous delivery workflow

This document describes the operating model for turning Sniffy product ideas into reviewed pull requests with minimal
interactive intervention. It covers Codex Cloud, the native Windows desktop app with a WSL local worker, GitHub delivery,
review convergence, and the project states that make those systems cooperate safely.

## 1. What is stored in the repository

- `AGENTS.md` contains repository-wide engineering, autonomy, verification, publication, and review rules.
- `.codex/cloud/setup.sh` installs GitHub CLI and Temurin JDK 8, writes Maven toolchains for JDK 8, 11, 17, 21, and 25,
  persists the environment, configures GitHub authentication when `GH_TOKEN` is present, and primes Maven dependencies.
- `.codex/cloud/maintenance.sh` refreshes dependencies when a cached cloud environment resumes on another branch.
- `.codex/cloud/use-jdk.sh` switches the current shell between installed JDKs.
- `.codex/local/run-issue.sh` is the unattended CLI fallback for an isolated local machine.
- `.codex/local/scheduled-task-prompt.md` is the canonical prompt for the persistent app-native dispatcher chat.
- `.codex/local/worker-task-prompt.md` is the canonical template for the one-time standalone worker created per issue.
- `docs/local-codex-worker.md` is the Hyper-V, Windows app, WSL2, Docker, GitHub Project, Scheduled task, and Remote runbook.
- `.github/ISSUE_TEMPLATE/codex-task.yml` provides the Definition-of-Ready template for autonomous work.

Codex loads `AGENTS.md` automatically. More specific instructions may be added in subdirectories when a module needs a
different build or review contract.

## 2. One-time Codex Cloud setup

Repository files cannot create an environment in another user's OpenAI account. Complete this UI setup once:

1. Connect the GitHub account that can access `sniffy/sniffy`.
2. Create a Codex environment named `sniffy` for `sniffy/sniffy` with `develop` as the default branch.
3. Configure setup:

   ```bash
   bash .codex/cloud/setup.sh
   ```

4. Configure maintenance:

   ```bash
   bash .codex/cloud/maintenance.sh
   ```

5. Enable agent internet access only when a task must publish or access GitHub. Allow `github.com` and `api.github.com`
   and the HTTP methods required by the task. A policy that allows only read methods may let a dry-run appear healthy while
   the real `git-receive-pack` or GitHub API write fails.
6. Add `GH_TOKEN` when Cloud tasks should publish autonomously. Prefer a dedicated, revocable, fine-grained PAT owned by a
   `sniffy` organization member and limited to `sniffy/sniffy`.
7. Grant Metadata read, Contents read/write, Pull requests read/write, and Issues read/write. Grant Workflows read/write
   only when authorized tasks may change files under `.github/workflows/`. The GitHub Actions permission controls workflow
   runs and artifacts; it does not authorize edits to workflow files.
8. Approve a new or changed token when organization policy requires approval, then reset the Codex environment cache.
9. The setup script installs a pinned GitHub CLI release, verifies its checksum, persists the credential through `gh`, and
   configures Git's credential helper. Its dry-run push proves endpoint and credential-helper connectivity, not effective
   write permission or agent-phase network policy.
10. Use **Reset cache** after setup scripts, secrets, token approval, or environment policy changes.
11. Enable Codex code review for this repository after the rules in `AGENTS.md` have been smoke-tested.

### Credential safety and rollback

Persisting GitHub authentication exposes the credential to repository code, build plugins, dependencies, and processes
running as the environment user. Treat this as an explicit security trade-off.

- Prefer a dedicated agent account, short expiration, and least privilege.
- Never place tokens in remote URLs, command lines, commits, logs, summaries, or fixtures.
- Protect `develop` with required pull requests and block force pushes and deletion. Automation identities must not bypass
  the ruleset.
- A fine-grained PAT cannot be restricted to one branch, so branch safety belongs in GitHub rules.
- Keep commit author, GitHub actor, Codex task, and human reviewer identities distinct when formal review matters.
- Revoke immediately after credential exposure or an unexpected write. Reset cached environments and verify the revoked
  credential no longer authenticates.

The historical `SNIFFY_GITHUB_PAT` bootstrap is superseded by `GH_TOKEN` plus GitHub CLI. Do not add a parallel credential
store.

## 3. Validate the environment

Run a small Cloud task that reads `AGENTS.md`, reports Java/Maven/`gh` versions, checks `gh auth status`, performs a
reversible remote-ref round trip, switches between JDK 8 and JDK 25, runs `git diff --check`, and executes one focused test
without leaving a branch or PR.

For manual toolchain validation:

```bash
source .codex/cloud/use-jdk.sh 8
mvn -version
source .codex/cloud/use-jdk.sh 25
mvn -version
```

### Cross-JDK and time-bounded validation

Switching `JAVA_HOME` does not invalidate Maven output. The first Maven command under each newly selected JDK must use
`clean`, or the runs must use isolated output/checkouts. Otherwise Java 8 may execute classes compiled against a newer JDK
and fail with misleading linkage errors such as covariant `ByteBuffer.position(int)` or `limit(int)` descriptors.

For Java 8 artifacts distinguish:

1. clean compile and focused tests on real JDK 8;
2. a modern-JDK `verify` that executes the configured Java 8 API-signature check;
3. for compatibility-sensitive code, execution on real JDK 8 of the same artifact built on the modern JDK without
   recompiling that artifact under Java 8.

Run independent obligations as separate commands with visible exit statuses. Use explicit timeouts for intentionally
bounded commands and report timeout status. Do not manually interrupt one combined command and describe its earlier steps
as a completed validation result.

## 4. Definition of Ready

An autonomous issue is ready only when it states:

- desired observable outcome;
- relevant current behavior, modules, links, and existing PR/branch constraints;
- product, compatibility, migration, and public-API decisions;
- explicit non-goals;
- testable acceptance criteria;
- required JDKs, platforms, tests, documentation, and external services;
- whether the agent may commit/push and whether it should create or update a pull request.

The issue must not delegate unresolved product decisions to Codex. Conservative implementation details may remain open
when they can be derived from repository conventions.

### 4.1. Complex-task design and proof preflight

A preflight is required when a task combines two or more high-risk axes:

- a new published artifact or public API;
- global mutable state, concurrency, resource ownership, or cleanup;
- failure composition involving primary and suppressed exceptions;
- several JDK, framework, engine, or platform versions;
- cross-reactor module placement or a test-only compatibility consumer;
- migration or compatibility requirements that may conflict with existing behavior.

The authoritative issue must resolve:

1. scope boundaries and excluded adjacent redesigns;
2. ownership of production code, test-only consumers, compatibility checks, and documentation;
3. lifecycle/failure matrix including setup, user failure, framework failure, cleanup, and combined failure;
4. exact artifact/runtime/framework compatibility matrix and whether the same artifact or rebuilt sources are exercised;
5. acceptance-to-proof matrix mapping every criterion to named tests, CI jobs, static checks, or documented manual proof;
6. delivery topology: base/head branch, new or existing PR, author identity, draft/ready behavior, and publication proof.

A green build proves only the tests that were actually discovered and executed. Confirm named tests in Surefire/CI output.

### 4.2. Slice by independently useful delivery, not by file count

Create separate issues or PRs only when slices are independently useful, reviewable, and mergeable. For a cohesive
cross-cutting feature, prefer one draft PR with explicit checkpoints for skeleton/boundary, lifecycle behavior, failure
matrix, cross-version consumer, and documentation/proof audit.

A short read-only design/proof task is often cheaper than several review/fix rounds.

### 4.3. Standard tooling and infrastructure gate

Before custom build, CI, release, dependency, security, formatting, linting, reporting, packaging, or scaffolding code,
check maintained tools, official actions, platform features, and declarative configuration. Missing maintainer-approved
rationale required by `AGENTS.md` is a Definition-of-Ready failure, not permission to invent a framework.

If an unrelated task discovers a need for shared infrastructure, stop and refine a separate issue with alternatives,
ownership, permissions, failure/noise behavior, deterministic testing, upgrade path, and removal condition.

## 5. Route work to the right executor

### Use Codex Cloud by default when

- work starts from a clean branch based on `develop`;
- the task is isolated, well specified, and provable in the cloud environment;
- dependencies are public or installed during setup;
- no unusual Git history surgery, local hardware, private service, or special networking is required;
- the task can finish as a reviewable diff or published pull request within the task window.

Examples include focused fixes, tests, docs, ordinary refactoring, dependency updates, and isolated modules.

### Use the app-native local worker when

- an existing branch/PR must be updated precisely;
- persistent artifacts or long builds improve iteration;
- Docker, browsers, local services, special networking, hardware, or privileged tools are needed;
- Remote visibility from mobile and a dedicated issue chat are valuable;
- repeated Cloud cold starts or time windows are material.

Follow [`docs/local-codex-worker.md`](local-codex-worker.md). The app-native topology is:

```text
one persistent in-chat dispatcher
  -> no eligible issue: NO_CHANGE, no new chat/worktree
  -> eligible issue: one-time standalone task
       -> one dedicated worker chat
       -> one isolated worktree
       -> in-chat PR continuation at 15 min, 15 min, then hourly
```

The dispatcher uses [`.codex/local/scheduled-task-prompt.md`](../.codex/local/scheduled-task-prompt.md), never implements
source changes, and claims at most one issue. It renders
[`.codex/local/worker-task-prompt.md`](../.codex/local/worker-task-prompt.md) into the one-time child task.

The worker implements, tests, publishes, and then supervises its PR in the same worker chat. Every follow-up inspects the
latest complete diff, review comments/threads, and CI. It approves only through an independent permitted reviewer identity;
otherwise it reports the identity limitation. Blocking defects receive one precise Request Changes review or equivalent
blocking comment. No task merges or enables auto-merge without explicit instruction.

Before enabling the recurring dispatcher, smoke-test that the installed desktop app allows an in-chat scheduled run to
create one one-time standalone child task. If that app-native composition is unavailable, pause the dispatcher and create
the child manually in the app. Do not add an external observer, App Server integration, or UI automation without a
separately approved design.

### Use the unattended local CLI only as a fallback

```bash
bash .codex/local/run-issue.sh NUMBER [BRANCH_NAME]
```

The script requires authenticated `gh`, a clean tree, and Codex CLI. It prepares or reuses an issue branch and starts an
unattended local agent. A CLI run is not an app-owned task and does not provide the same desktop/Remote chat lifecycle. Do
not launch it from an app Scheduled task because that creates a nested agent with split context.

`danger-full-access` is appropriate only inside the disposable environment because it grants the agent the same authority
as its operating-system user.

## 6. Start and follow up Cloud work

1. Refine the issue with the autonomous task template and complete any required preflight.
2. Start implementation with a top-level issue comment containing `@codex`. Record the comment URL and do not call work
   started until the connector reacts or posts a task link. A review mention alone is not a reliable implementation trigger.
3. Select the `sniffy` environment and current `develop`; use stronger reasoning for complex architectural work.
4. Ask for implementation, validation, real GitHub publication, and a ready-for-review handoff, not merely a plan or local
   commit.
5. Require the agent to push, create/update the intended PR, and verify remote branch, full SHA, PR URL, base/head, draft
   state, and PR head through GitHub. A dry-run push is not publication.
6. Treat Codex UI metadata as informative. GitHub branch, commit, PR, reviews, and checks are authoritative.
7. Review the complete remote diff, issue acceptance/proof matrix, `AGENTS.md`, comments, threads, and CI. Confirm named
   tests executed; do not approve from the agent summary.
8. For corrections, leave precise feedback and a separate explicit `@codex` dispatch comment that links the existing PR,
   branch, review, and required checks. Update the issue first when scope or architecture changes.
9. Check after 15 minutes, again 15 minutes later, then hourly while incomplete. Verify acknowledgement or a new commit;
   do not infer active work from a review alone.
10. Request `@codex review` or rely on automatic review only after it has produced useful results on test PRs.

A standard dispatch prompt must require reading the authoritative issue and `AGENTS.md`, a proof matrix, conservative
implementation, focused and repository checks, final-diff review, real publication and remote verification, draft-to-ready
handoff when appropriate, and no merge or auto-merge.

### Review convergence and escalation

The first review should be comprehensive: full diff, all criteria, test discovery, module boundaries, compatibility,
documentation, and CI. Report independent findings together.

After every correction, review the new delta and rerun the affected full proof matrix. An old green run does not cover a
new head.

After two substantive review/fix rounds, or immediately after an architecture/product reversal, stop stacking narrow
prompts. Re-baseline the issue and choose one fresh comprehensive Cloud task, app-native local continuation on the same
branch, or a human design decision.

Changing executor does not repair an ambiguous specification.

## 7. Operating model

### Product owner — Dmitry

- defines outcomes, value, priorities, and release constraints;
- resolves product and compatibility trade-offs;
- accepts or rejects the reviewed result;
- is the only authority that may explicitly request merge from this automation workflow.

### Delivery lead — ChatGPT

- owns the book of work and turns discussions into authoritative issues;
- selects Cloud, app-native local, CLI fallback, or human execution;
- tracks trigger acknowledgement, worker chat/task, branch, PR, SHA, blockers, CI, reviews, and follow-ups;
- reviews implementation and architecture against the issue and `AGENTS.md`;
- distinguishes dispatched, working, locally complete, published, ready for review, approved, and merged;
- never infers remote state from a summary and never merges without explicit instruction;
- prepares the weekly retrospective.

### App-native dispatcher

- lives in one persistent local-project chat with an in-chat 15-minute schedule;
- reads and claims at most one eligible local issue;
- creates no task/chat/worktree for an empty queue;
- creates one one-time standalone worker task for an eligible issue;
- rolls back a claim when child creation fails;
- never edits source code.

### App-native worker

- owns one dedicated issue chat and isolated worktree;
- handles fresh implementation or an explicitly re-queued existing PR branch;
- implements, tests, publishes, and verifies GitHub state;
- creates one in-chat follow-up schedule at 15 minutes, another 15 minutes, then hourly;
- checks full diff, reviews, and CI before formal review;
- pauses follow-up when complete or blocked;
- never merges.

### Codex Cloud

- executes isolated, fully specified tasks in parallel;
- follows `AGENTS.md`, publishes when authorized, and reports precise blockers;
- does not make unresolved product decisions.

### Unattended local CLI

- remains a fallback for specialized isolated environments or when app-native task composition is unavailable;
- follows the same issue, proof, publication, and no-merge contracts;
- does not produce an app-owned Remote chat lifecycle.

## 8. Book-of-work states

Recommended GitHub Project states:

1. `Idea` — captured but not refined.
2. `Needs product decision` — blocked on outcome, compatibility, or scope.
3. `Ready for agent` — satisfies Definition of Ready.
4. `In progress: cloud` or `In progress: local` — dispatched with durable task/worker/branch evidence.
5. `Agent blocked` — exact blocker and smallest decision recorded.
6. `Review` — published PR exists and checks/review are in progress.
7. `Ready to merge` — acceptance criteria, review, and required checks pass.
8. `Done` — merged or intentionally closed with rationale.

Useful labels include `agent:cloud`, `agent:local`, `agent:blocked`, `needs:product`, `needs:review`, `security`, and optional
model-routing labels. Project status remains authoritative.

A local claim comment should record dispatcher/worker identity, child task title, fresh/continuation mode, branch, existing
PR when applicable, model/reasoning, and timestamp. This evidence helps detect stale `In progress` claims without silently
creating duplicate workers.

## 9. Review and Definition of Done

The delivery lead reviews:

- every acceptance criterion and its proof;
- standard tooling versus bespoke infrastructure rationale;
- compatibility, public API, lifecycle, and resource safety;
- module boundaries and unnecessary complexity;
- test quality, discovery, execution, and attempts to weaken or retry failures;
- exact commands and JDKs;
- dependency/security changes;
- documentation and migration impact;
- generated or unrelated files;
- remote branch, PR head, draft/ready state, review threads, and CI logs;
- whether the formal reviewer identity is independent from the PR author.

A task is done only when implementation is published and remotely verified, the requested PR state is reached, required
checks pass, known limitations are documented, review findings are resolved or explicitly accepted, project state is
updated, and the continuation schedule is paused. Approval is not merge.

## 10. Incident retrospectives

- [Issue #637 / PR #643: Codex Cloud delivery and publication](retrospectives/2026-07-16-codex-cloud-issue-637.md)
- [Issue #238 / PR #633: JUnit Jupiter delivery](retrospectives/2026-07-issue-238-junit-jupiter.md)
- [Docusaurus: Cloud versus local routing](retrospectives/2026-07-18-docusaurus-cloud-vs-local.md)

Historical retrospectives describe the system at the time of the incident. Current operational rules live in `AGENTS.md`,
this document, `docs/local-codex-worker.md`, and the canonical local prompt files.

## 11. Weekly retrospective

Cover:

- completed and merged work;
- work dispatched, running, in review, or blocked;
- lead time from `Ready for agent` to published PR and merge;
- Cloud versus local routing quality;
- dispatcher no-op cycles, spawned worker chats, stale claims, and duplicate/failed child creation;
- first/second 15-minute checks and hourly continuations that exceeded expectations;
- CI failures, escaped defects, and repeated review findings;
- tasks needing human clarification and how the issue template should improve;
- changes needed in `AGENTS.md`, setup, prompts, templates, or test matrix;
- substantive review/fix rounds and re-baseline decisions;
- next-week priorities and capacity risks.

The retrospective must produce concrete issue/template/instruction changes, not only narrative.
