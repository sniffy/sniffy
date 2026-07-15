# Codex Cloud and autonomous delivery workflow

This document describes the one-time Codex Cloud setup for Sniffy and the operating model for turning product ideas into reviewed pull requests with minimal interactive intervention.

## 1. What is stored in the repository

- `AGENTS.md` contains repository-wide engineering, autonomy, verification, and review rules.
- `.codex/cloud/setup.sh` installs Maven and Temurin JDK 8, 11, 17, 21, and 25, writes Maven toolchains, persists the environment, configures GitHub authentication when the `GH_TOKEN` secret is present, and primes the Maven dependency cache.
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

7. Keep agent-phase internet disabled for ordinary implementation tasks. Enable limited internet only when a task explicitly requires current external documentation, vulnerability/advisory lookup, or dependency metadata that was not available during setup.
8. Add a Codex environment secret named `GH_TOKEN` when Cloud tasks should push branches and create or update pull requests autonomously. Use a dedicated, revocable, fine-grained PAT scoped only to `sniffy/sniffy`, with the minimum required repository permissions: Metadata read, Contents read/write, Pull requests read/write, and Issues read/write. Add Actions permissions only when a task must operate workflow runs.
9. The setup script first installs the pinned GitHub CLI release into `~/.local`, verifies its published SHA-256 checksum, then consumes `GH_TOKEN` while secrets are available, stores it in GitHub CLI's host configuration, and configures Git to use GitHub CLI as its credential helper. This intentionally makes the PAT's authority available to the agent phase even though the `GH_TOKEN` environment variable itself is removed. Never use a broad personal or organization-wide token for this environment.
10. Codex invalidates the environment cache when the setup script or secrets change. Use **Reset cache** if the next task still uses stale credentials or an inconsistent cached toolchain.
11. In Codex settings, enable code review for this repository. Automatic review may be enabled after the review rules in `AGENTS.md` have produced useful results on several test pull requests.

The setup phase has internet access and the agent phase follows the environment's configured internet policy. Cached environments may be reused, so the maintenance script must remain safe and idempotent.

## 3. Validating the environment

Start a small cloud task on `develop` with this prompt:

```text
Validate the Sniffy development environment only. Read AGENTS.md, report the active Java and Maven versions, report `gh --version`, verify GitHub authentication with `gh auth status --hostname github.com` and confirm push permission using `gh api repos/sniffy/sniffy --jq '.permissions.push'`, switch to JDK 8 and JDK 25 using .codex/cloud/use-jdk.sh, run git diff --check, and run one small focused Maven test without changing tracked files. Report every command and result. Do not create a branch or pull request.
```

For manual validation inside a shell:

```bash
source .codex/cloud/use-jdk.sh 8
mvn -version

source .codex/cloud/use-jdk.sh 25
mvn -version
```

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

1. Refine the issue with the autonomous task template.
2. Select the `sniffy` environment and the intended base branch in Codex Cloud, then reference the issue as the authoritative specification.
3. For complex architectural work, select the strongest available coding model and Extra High reasoning. For small mechanical tasks, use the default reasoning level unless deeper analysis is needed.
4. Ask for implementation, validation, and a draft pull request—not merely a plan.
5. Review the cloud task summary, commands, tests, and diff.
6. Use a follow-up task for bounded corrections.
7. On a Codex-created pull request, a comment such as `@codex fix the CI failures` starts another cloud task with that pull request as context when the GitHub integration has permission.
8. Request a high-signal review with `@codex review`, or rely on automatic reviews after they have been enabled.

A standard dispatch prompt is:

```text
Implement the complete linked issue autonomously. Read AGENTS.md and the entire issue thread first; the issue is authoritative. Make ordinary engineering decisions yourself using the most conservative maintainable option. Implement the change, add or update tests and documentation, run all checks available in the environment, review the final diff, and open a draft pull request linked to the issue. Do not stop for a plan or ask for routine clarification. If a genuine blocker remains, leave the repository clean and report the exact blocker and the smallest decision or permission needed.
```

## 7. Operating model

### Product Owner — Dmitry

- defines desired outcomes, user value, priorities, and release constraints;
- makes product and compatibility decisions when trade-offs affect users;
- accepts or rejects the result after review.

### Delivery lead — ChatGPT

- owns the book of work in GitHub Projects and issues;
- converts discussions into issues with explicit requirements, non-goals, acceptance criteria, and validation;
- selects Cloud, unattended local Codex, or human-led execution;
- dispatches ready tasks and tracks blockers, CI, reviews, and follow-ups;
- reviews implementation and architecture against the issue and `AGENTS.md`;
- keeps project status and issue descriptions current;
- prepares a weekly retrospective.

### Codex Cloud

- executes well-defined, isolated tasks in parallel;
- follows `AGENTS.md`, implements and validates the change, and returns a reviewable diff or draft pull request;
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
- test quality, including whether tests were weakened or made flaky;
- exact commands and JDKs used;
- dependency/security changes;
- documentation and migration impact;
- accidental generated files or unrelated edits.

A task is done only when required checks pass, known limitations are documented, review findings are resolved or accepted explicitly, and the issue/project state is updated.

## 10. Weekly retrospective

The weekly retrospective should cover:

- completed and merged work;
- work started, still running, or blocked;
- lead time from `Ready for agent` to draft PR and merge;
- cloud vs local routing decisions and whether they were correct;
- CI failures, escaped defects, and repeated review findings;
- tasks that required human clarification and how to improve their issue specification;
- changes needed in `AGENTS.md`, setup scripts, templates, or the test matrix;
- the next week's top priorities and capacity risks.

The retrospective should result in concrete issue/template/instruction changes, not only a narrative summary.
