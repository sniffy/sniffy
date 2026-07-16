# Codex Cloud and autonomous delivery workflow

This document describes the one-time Codex Cloud setup for Sniffy and the operating model for turning product ideas into reviewed pull requests with minimal interactive intervention.

## 1. What is stored in the repository

- `AGENTS.md` contains repository-wide engineering, autonomy, verification, and review rules.
- `.codex/cloud/setup.sh` installs GitHub CLI and Temurin JDK 8, writes Maven toolchains for JDK 8, 11, 17, 21, and 25, persists the environment, configures GitHub authentication when the `GH_TOKEN` secret is present, and primes the Maven dependency cache.
- `.codex/cloud/maintenance.sh` refreshes dependencies when a cached cloud environment is resumed on another branch.
- `.codex/cloud/use-jdk.sh` switches the current shell between installed JDKs.
- `.codex/local/run-issue.sh` prepares an issue branch and launches unattended local Codex in an isolated machine.
- `.github/ISSUE_TEMPLATE/codex-task.yml` provides a Definition-of-Ready template for autonomous tasks.

Codex loads `AGENTS.md` automatically. More specific instructions can be added later in subdirectories when a module needs different build or review rules.

## 2. One-time Codex Cloud setup

The repository files cannot create an environment in another user's OpenAI account, so the following UI setup is required once.

1. Open Codex Cloud and connect the GitHub account that has access to `sniffy/sniffy`.
2. Allow Codex access to the repository.
3. Create an environment named `sniffy` for repository `sniffy/sniffy`.
4. Use `develop` as the default branch.
5. Set the setup script to:

   ```bash
   bash .codex/cloud/setup.sh
   ```

6. Set the maintenance script to:

   ```bash
   bash .codex/cloud/maintenance.sh
   ```

7. Codex Cloud setup always has internet access, but the agent phase is offline by default and follows a separate environment policy. For tasks that must publish branches or pull requests, set **Agent internet access** to **On**, allow only `github.com` and `api.github.com`, and enable `GET`, `HEAD`, `OPTIONS`, `POST`, `PUT`, `PATCH`, and `DELETE`. A read-only method policy can let `git push --dry-run` succeed while the real `POST /git-receive-pack` and GitHub API writes fail with HTTP 403. Keep broader internet access disabled unless the task explicitly needs it.
8. Add a Codex environment secret named `GH_TOKEN` when Cloud tasks should push branches and create or update pull requests autonomously. Prefer a dedicated, revocable, fine-grained PAT whose owner is a member of the `sniffy` organization. Set the resource owner to `sniffy`, select only `sniffy/sniffy`, and grant Metadata read, Contents read/write, Pull requests read/write, and Issues read/write. Grant **Workflows read/write** whenever an authorized task may add, modify, rename, or delete files under `.github/workflows/`; Contents read/write alone cannot publish those changes. GitHub may describe the rejected push as requiring the classic-PAT `workflow` scope, while the equivalent fine-grained permission is **Workflows read/write**. The separate **Actions** permission controls workflow runs and Actions resources (for example dispatch, rerun, cancel, artifacts, and caches); it does not authorize workflow-file changes, so grant it only when a task must perform those operations. If organization policy requires approval, approve the newly created or modified token before resetting the Codex environment cache. GitHub does not support fine-grained PAT contributions from outside or repository collaborators; for such an account, either make it an organization member before issuing the token or, if organization policy permits, use a short-lived classic PAT with the `public_repo` and `workflow` scopes when workflow files are in scope.
9. The setup script installs the pinned GitHub CLI release into `~/.local`, verifies its published SHA-256 checksum, then consumes `GH_TOKEN` while secrets are available, stores it in GitHub CLI's host configuration, and configures Git to use GitHub CLI as its credential helper. Its non-mutating `git push --dry-run` verifies setup-phase endpoint and credential-helper connectivity only; it does not prove token write permission or agent-phase `POST` access. The repository API's `.permissions.push` field is also insufficient because it reports the account role without proving that the token can write. This intentionally makes the credential available to the agent phase even though the `GH_TOKEN` environment variable itself is removed.
10. Codex invalidates the environment cache when the setup script or secrets change. Use **Reset cache** if the next task still uses stale credentials or an inconsistent cached toolchain.
11. In Codex settings, enable code review for this repository. Automatic review may be enabled after the review rules in `AGENTS.md` have produced useful results on several test pull requests.

The setup phase has internet access and the agent phase follows the environment's configured internet policy. Cached environments may be reused, so the maintenance script must remain safe and idempotent.

### Credential safety, identity, and rollback

Persisting GitHub authentication for the agent phase intentionally makes the credential available to repository code,
build plugins, dependencies, and other processes running as the container user. Treat that as an explicit security
trade-off, not as a hidden setup detail.

- Use a dedicated agent or machine account when possible, a short expiration, and repository-only least privilege.
  Treat Workflows read/write as an additional high-impact capability and omit it from environments whose tasks never
  edit `.github/workflows/`.
- Never place a token in a remote URL, command line, commit, log, task summary, or test fixture.
- Protect `develop` with server-side rules that require pull requests and block force pushes and deletion. The token
  owner and administrators used by automation must not bypass those rules. Local hooks are defense in depth only.
- A fine-grained PAT cannot be limited to one branch or to fast-forward updates, so branch/ruleset enforcement belongs
  on GitHub rather than in agent instructions alone.
- Git commit author, GitHub push/API actor, and Codex task identity are different. Use a dedicated authenticated agent
  account to make publication provenance clear and to preserve the human maintainer's ability to submit formal reviews.
- Revoke the token immediately if it appears in output, an unexpected branch is updated, or required rules fail. Remove
  the secret, reset the Cloud environment cache, and verify that the old credential no longer authenticates.

The earlier `SNIFFY_GITHUB_PAT` bootstrap proposed in PR #635 is superseded by the repository's current `GH_TOKEN` and
GitHub CLI setup. Do not add a second credential store or parallel authentication path.

## 3. Validating the environment

Start a small cloud task on `develop` with this prompt:

```text
Validate the Sniffy development environment only. Read AGENTS.md, report the active Java and Maven versions, report `gh --version`, and verify GitHub authentication with `gh auth status --hostname github.com`. Prove effective token and agent-network write access with a reversible remote-ref round trip: set `probe_branch="agent/codex-auth-check-$(git rev-parse --short=12 HEAD)"`, run `git push --porcelain https://github.com/sniffy/sniffy.git "HEAD:refs/heads/${probe_branch}"`, delete it with `gh api --method DELETE "repos/sniffy/sniffy/git/refs/heads/${probe_branch}"`, and confirm `git ls-remote --heads https://github.com/sniffy/sniffy.git "${probe_branch}"` returns no ref. Stop and report the exact output if creation or cleanup fails. Switch to JDK 8 and JDK 25 using .codex/cloud/use-jdk.sh, run git diff --check, and run one small focused Maven test without changing tracked files. Report every command and result. Do not leave a remote branch or open a pull request.
```

For manual validation inside a shell:

```bash
source .codex/cloud/use-jdk.sh 8
mvn -version

source .codex/cloud/use-jdk.sh 25
mvn -version
```

### Cross-JDK and time-bounded validation

Switching `JAVA_HOME` does not invalidate Maven outputs. When two JDKs are used in one worktree, the first Maven command
under each newly selected JDK must include `clean`, or the runs must use isolated checkouts/output directories. Otherwise
a Java 8 run can reuse `target/` classes compiled by a newer JDK and fail with misleading linkage errors such as a
covariant `ByteBuffer.position(int)` or `limit(int)` descriptor that Java 8 does not provide.

For Java 8 artifacts, distinguish three separate proofs:

1. a clean compile and focused test on real JDK 8;
2. a modern-JDK `verify` run that executes the configured Java 8 API-signature check;
3. for compatibility-sensitive code, execution on real JDK 8 of the same artifact built on the modern JDK, without
   recompiling that artifact under JDK 8.

Run independent focused, cross-version, regression, and diff checks as separate commands. Give an intentionally bounded
command its own explicit timeout and report the timeout exit status. Do not manually interrupt a combined command and
present the earlier parts as one completed validation result. If the full reactor cannot finish within the Cloud task
window, report the focused results separately and make the corresponding GitHub Actions run the authoritative
full-reactor proof.

## 4. Definition of Ready for autonomous work

An issue is ready when it states:

- the desired observable outcome;
- relevant current behavior, modules, and links;
- product, compatibility, and migration decisions;
- explicit non-goals;
- testable acceptance criteria;
- required JDKs, platforms, tests, and documentation;
- branch/history constraints and external-service requirements;
- whether the agent may commit/push and whether it should create or update a pull request.

The issue must not delegate unresolved product decisions to Codex. Implementation choices may remain open when a conservative, maintainable answer can be derived from the repository.

### 4.1. Complex-task design and proof preflight

A task needs a preflight before implementation when it combines two or more of these risk axes:

- a new published artifact or public API;
- global mutable state, concurrency, resource ownership, or cleanup;
- failure composition where primary and suppressed exceptions matter;
- multiple JDK, framework, engine, or platform versions;
- cross-reactor module placement or a test-only compatibility consumer;
- migration or compatibility requirements that can conflict with existing behavior.

The preflight is a short addition to the authoritative issue, not a speculative design document. It must resolve:

1. **Scope boundary:** which modules and APIs may change, and which tempting adjacent redesigns are excluded.
2. **Architecture ownership:** where production code, test-only consumers, compatibility checks, and documentation belong.
3. **Lifecycle/failure matrix:** success, user failure, framework failure, partial setup, cleanup failure, and combined failure.
4. **Compatibility matrix:** the exact built artifact, runtime/JDK/framework combinations, and whether sources are rebuilt or the same artifact is consumed.
5. **Acceptance-to-proof matrix:** every criterion mapped to a focused test, CI job, static inspection, or explicitly documented manual check.
6. **Delivery topology:** base/head branch, existing or new PR, PR author identity, draft/ready transition, and publication verification.

If the preflight exposes a product decision—especially whether to add a public/core API or preserve unrelated global
state—the delivery lead resolves it before dispatch. Codex should not infer such a decision from a cleanup requirement.

A green build is evidence only for tests that were actually discovered and executed. The proof matrix must name the
test class or CI job and later be checked against Surefire/CI output.

### 4.2. Slicing complex work without creating micro-PR overhead

Split work into separate issues or pull requests only when the slices are independently useful, reviewable, and
mergeable. Do not create a PR per test or per callback merely to make the task look smaller.

For a cohesive cross-cutting feature, prefer one draft PR with explicit implementation checkpoints:

1. module/artifact skeleton and compatibility boundary;
2. production lifecycle and happy-path tests;
3. failure/cleanup matrix and regression tests;
4. cross-version consumer and reactor/CI wiring;
5. documentation, final proof-matrix audit, and ready-for-review handoff.

A short read-only design/refinement task before implementation is often cheaper than multiple review/fix loops. For a
feature that combines global state, failure composition, and multiple runtime versions, use that refinement step even
when the final implementation remains one PR.

## 5. Routing tasks

### Use Codex Cloud by default when

- work starts from a clean branch based on `develop`;
- the task is well specified and can be validated in the cloud container;
- dependencies are public or can be installed in the setup phase;
- no unusual Git history surgery is required;
- no private local service, hardware, proprietary IDE, or operating-system-specific environment is required;
- the task can finish as a reviewable diff or draft pull request.

Examples: focused fixes, tests, documentation, ordinary refactoring, dependency updates, adding a new isolated module, and most issue-driven implementation.

### Use unattended local Codex in an isolated VM or dev container when

- the existing branch and commit ancestry must be manipulated precisely;
- the task needs unrestricted Git/GitHub CLI operations against an existing pull request;
- several local services, Docker, special networking, hardware, or corporate resources are needed;
- debugging requires an environment not available in Codex Cloud;
- the task is broad enough that persistent local artifacts and manual inspection are valuable.

The local run should still be non-interactive: use `--ask-for-approval never`, run inside a disposable VM/container, put the complete specification in the issue, and instruct the agent to implement, test, commit, push, and update the draft pull request without asking routine questions.

Recommended launcher:

```bash
bash .codex/local/run-issue.sh NUMBER [BRANCH_NAME]
```

The script requires authenticated `gh`, a clean working tree, and an installed Codex CLI. It fetches the issue text, switches to an existing issue branch or creates `agent/issue-NUMBER` from the default branch, and starts Codex with `gpt-5.6-sol`, Extra High reasoning, no approvals, search enabled, and `danger-full-access`. Override the defaults with `CODEX_MODEL` and `CODEX_REASONING_EFFORT`.

`danger-full-access` is appropriate only inside the isolated environment because it grants the agent the same filesystem and network authority as the executing user.

## 6. Starting and following up cloud work

1. Refine the issue with the autonomous task template. For a complex task, complete the design/proof preflight and make
   the issue thread the authoritative dispatch record.
2. Start implementation from a top-level issue comment containing `@codex`. Record the comment URL and do not describe
   the task as started until the Codex connector reacts or posts a task link. A mention inside a submitted GitHub review
   is not a reliable task trigger. Pull-request comment triggers additionally depend on Codex code review being enabled;
   use the authoritative issue thread as the fallback for implementation and correction tasks.
3. Select the `sniffy` environment and current `develop`. For complex architectural work, select the strongest available
   coding model and High or Extra High reasoning.
4. Ask for implementation, validation, actual GitHub publication, and a ready-for-review handoff—not merely a plan,
   local commit, `make_pr` call, or draft that remains draft after the work is complete.
5. Require the agent to push the branch, create or update the intended pull request, and verify the remote branch, full
   commit SHA, pull-request URL, base/head branches, draft state, and head SHA through `git ls-remote`, `gh api`, and
   `gh pr view` before reporting success. A dry-run push is not publication.
6. Treat Codex Cloud UI metadata as informative rather than authoritative. A pull request created directly with `gh`
   may not appear as attached in the Cloud UI; the resolvable GitHub branch, commit, and pull-request URLs are the source
   of truth. A `GitHub Mention` card is task history, not evidence of an unpublished branch or PR. Avoid duplicate
   mentions merely to republish the same work.
7. Review the complete remote diff, issue acceptance criteria and proof matrix, `AGENTS.md`, comments, review threads,
   and every relevant CI job. Confirm named tests were discovered and executed; do not approve from an agent summary.
8. For bounded corrections, leave precise review feedback and dispatch a new top-level `@codex` comment on the
   authoritative issue that links the existing pull request, branch, review, and required checks. If review changes
   architecture or product scope, first update the issue and mark conflicting older guidance as superseded.
9. Request a high-signal review with `@codex review`, or rely on automatic reviews after they have been enabled.

A standard dispatch prompt is:

```text
@codex Implement the complete linked issue autonomously from current develop. Read AGENTS.md, docs/codex-workflow.md,
and the entire issue thread first; the issue is authoritative. Make ordinary conservative engineering decisions without
asking for routine clarification. Before editing, turn every acceptance criterion into a proof matrix and identify any
unresolved scope, public-API, global-state, lifecycle, module-placement, or compatibility decision. Do not implement
through a material ambiguity. Implement the change, add or update tests and documentation, run every applicable focused
and repository check as separately reported commands, and review the final diff.

Use a new agent/<short-description> branch unless the issue names an existing pull-request branch. Publish real GitHub
state: push the branch, create or update the intended pull request using the authenticated agent account when authorized,
and verify the remote branch, full commit SHA, pull-request URL, base/head branches, draft state, and head SHA with
git ls-remote, gh api, and gh pr view. Do not treat a local commit, make_pr metadata, or a Cloud summary as publication.
Use a draft while work is incomplete, then mark the pull request ready for review after implementation and locally
applicable verification are complete unless this issue explicitly requires it to remain draft. Do not merge or enable
auto-merge.

During handoff, map each criterion to the focused test or CI job that proves it, confirm those tests actually executed,
and reply on the issue and pull request with exact resolvable URLs, the full SHA, and exact check results. If a genuine
blocker remains, stop without repeatedly rebuilding the same change and report the exact command/output plus the
smallest permission or decision needed.
```

### Review convergence and escalation

The first review should be comprehensive: inspect the full diff, all acceptance criteria, test discovery/execution,
module boundaries, compatibility evidence, documentation, and CI. Report independent findings together.

After each follow-up, review the new delta and re-run the full proof matrix. Do not assume an older green run covers a
new head.

Use a **two-substantive-round rule**: after two review/fix rounds, or immediately after an architectural/product-scope
reversal, stop issuing another narrow patch prompt. Re-baseline the authoritative issue, collapse or mark superseded
guidance, and choose one of:

- one fresh comprehensive Cloud task against the current remote head;
- unattended local Codex on the same branch when persistent artifacts, long builds, or repeated debugging matter;
- a human decision/design pass before further coding.

Switching to local Codex can reduce cold-start and task-window friction, but it does not repair an ambiguous
specification. The preflight and proof matrix remain mandatory for both routes.

## 7. Operating model

### Product Owner — Dmitry

- defines desired outcomes, user value, priorities, and release constraints;
- makes product and compatibility decisions when trade-offs affect users;
- accepts or rejects the result after review.

### Delivery lead — ChatGPT

- owns the book of work in GitHub Projects and issues;
- converts discussions into issues with explicit requirements, non-goals, acceptance criteria, and validation;
- selects Cloud, unattended local Codex, or human-led execution;
- dispatches ready tasks and tracks the trigger reaction/task link, remote branch, pull request, blockers, CI, reviews,
  and follow-ups;
- distinguishes dispatched, working, locally complete, published, ready for review, approved, and merged states; never
  infers one state from another or from an agent summary;
- reviews implementation and architecture against the issue and `AGENTS.md`;
- keeps project status and issue descriptions current;
- prepares a weekly retrospective.

### Codex Cloud

- executes well-defined, isolated tasks in parallel;
- follows `AGENTS.md`, implements and validates the change, and returns a reviewable diff or pull request;
- does not make unresolved product decisions;
- reports precise blockers instead of waiting for an interactive answer.

### Unattended local Codex

- handles tasks requiring a privileged or specialized disposable environment, unusual branch operations, existing PR surgery, or services unavailable in Cloud;
- receives the same issue-driven specification and Definition of Done;
- commits, pushes, and updates the draft pull request when explicitly authorized.

## 8. Book-of-work states

Recommended GitHub Project states:

1. `Idea` — captured but not refined.
2. `Needs product decision` — blocked on outcome, compatibility, or scope.
3. `Ready for agent` — satisfies Definition of Ready.
4. `In progress: cloud` or `In progress: local` — dispatched with a task/branch link.
5. `Agent blocked` — exact blocker and requested decision recorded in the issue.
6. `Review` — draft PR exists and checks/review are in progress.
7. `Ready to merge` — acceptance criteria and required checks pass.
8. `Done` — merged or intentionally closed with rationale.

Useful labels include `agent:cloud`, `agent:local`, `agent:blocked`, `needs:product`, `needs:review`, and `security`. Labels describe routing and attention; the GitHub Project status remains the workflow source of truth.

## 9. Review and Definition of Done

The delivery lead reviews:

- whether the implementation matches every acceptance criterion;
- compatibility and public API impact;
- module boundaries and unnecessary complexity;
- test quality, discovery, execution, and whether tests were weakened or made flaky;
- exact commands and JDKs used;
- dependency/security changes;
- documentation and migration impact;
- accidental generated files or unrelated edits;
- remote branch, PR head, draft/ready state, review threads, and relevant CI logs.

A task is done only when the implementation is published and remotely verified, the pull request has reached the
requested review state, required checks pass, known limitations are documented, review findings are resolved or accepted
explicitly, and the issue/project state is updated.

## 10. Incident retrospectives

- [Issue #637 / PR #643: Codex Cloud delivery and publication](retrospectives/2026-07-16-codex-cloud-issue-637.md)
- [Issue #238 / PR #633: JUnit Jupiter delivery](retrospectives/2026-07-issue-238-junit-jupiter.md)

## 11. Weekly retrospective

The weekly retrospective should cover:

- completed and merged work;
- work started, still running, or blocked;
- lead time from `Ready for agent` to draft PR and merge;
- cloud vs local routing decisions and whether they were correct;
- CI failures, escaped defects, and repeated review findings;
- tasks that required human clarification and how to improve their issue specification;
- changes needed in `AGENTS.md`, setup scripts, templates, or the test matrix;
- number of substantive review/fix rounds, whether the two-round re-baseline rule was triggered, and why;
- the next week's top priorities and capacity risks.

The retrospective should result in concrete issue/template/instruction changes, not only a narrative summary.
