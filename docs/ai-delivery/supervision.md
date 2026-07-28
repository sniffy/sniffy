# Supervision and delivery state

This policy applies whenever ChatGPT supervises work delegated to Codex Cloud, Local Codex, an IDE agent, another
implementation agent, or a human contributor using the autonomous delivery workflow.

## Delivery states

Track these states independently:

1. **Ready:** the authoritative issue contains outcome, decisions, non-goals, proof matrix, routing, and required access.
2. **Dispatched:** a concrete executor task exists and the trigger is observable.
3. **Working:** the intended executor acknowledged or produced new remote evidence.
4. **Locally complete:** implementation and locally available checks finished; nothing is implied about GitHub publication.
5. **Published:** a resolvable remote branch, full SHA, and intended PR exist and the PR head matches.
6. **Review:** the exact-head diff, comments, threads, evidence, and CI are being assessed.
7. **Ready to merge:** acceptance criteria, review, and required checks pass. This is not merged.
8. **Done:** Dmitry-authorized merge or deliberate closure is recorded and follow-up monitoring is stopped.
9. **Blocked:** the exact missing decision, access, infrastructure, or proof is recorded with the smallest next action.

Do not collapse these into optimistic status language. A local commit is not published; publication is not approval; approval
is not merge.

## Dispatch proof

- A Cloud implementation is dispatched only after the proven top-level trigger has a connector reaction, task link, or
  equivalent durable acknowledgement. A review mention alone is not implementation dispatch.
- A Local Codex item is dispatched only after the issue claim and app-owned worker task/chat are confirmed. A Project status
  or label alone does not prove a worker exists.
- An IDE agent is working only while the human explicitly owns the interactive session or remote commits prove activity.
- A ChatGPT direct-execution task is working when the intended branch or exact file operation has begun; ChatGPT must not
  claim a local checkout, command, browser session, or publication that did not actually happen.

## Required monitoring cadence

Every new task, retry, continuation, or Request Changes return starts a fresh supervision cycle:

1. check after 15 minutes;
2. if incomplete, check 15 minutes later;
3. if still incomplete, switch to hourly monitoring.

Do not postpone the first check to the hourly monitor. Each check must inspect the authoritative issue/PR, intended executor,
expected branch and PR, acknowledgement or new head, review threads, and current CI. Notify only on meaningful progress,
completion, or a real blocker.

A review or dispatch comment does not prove work started. Verify acknowledgement or a new commit. Stop monitoring when the
review cycle is complete or a human decision is required.

## Branch and PR continuity

- Fresh work uses one new branch from current `develop` and one intended PR unless the issue explicitly approves independent
  slices.
- Continuation work reuses the exact open branch and PR. Do not reset to `develop`, rebase, force-push, create a replacement
  branch, or open a duplicate PR.
- When `develop` moves materially before handoff, normally merge current `origin/develop` into the feature branch and rerun
  exact-head proof. Do not use a stale green run as final evidence.
- Preserve completed work when publication fails. Diagnose access and recover the existing commit instead of rebuilding the
  implementation from scratch.

## Comprehensive review and correction

The first review should audit the whole acceptance-to-proof matrix, not only the most visible delta. Inspect:

- complete exact-head diff and scope;
- module/API ownership and forbidden adjacent work;
- lifecycle, failure composition, compatibility, and resource safety;
- test quality, discovery, and execution;
- dependencies, workflows, permissions, and standard-tooling rationale;
- generated or unrelated files;
- documentation, migration, rollback, and PR-body accuracy;
- unresolved comments and review threads;
- exact-head CI, artifacts, logs, screenshots, or functional behavior.

Report independent findings together. After corrections, review the new delta and rerun the affected proof matrix. Two
substantive review/fix rounds, or one architecture/product reversal, trigger re-baselining: update the authoritative issue,
mark superseded guidance, and deliberately choose the next executor rather than stacking narrow prompts.

## Review identities

The GitHub assignee or PR author is a technical identity, not the executor product. Formal review must use an identity that
is allowed to review and is independent from the PR author.

- When independent review is available and all proof passes, submit `APPROVE`.
- When blockers remain, submit one precise `REQUEST_CHANGES` covering all current findings, then explicitly dispatch the
  continuation.
- When the active identity cannot review its own PR, leave exact ready-for-human-review or blocking feedback and state the
  identity limitation. Do not pretend a formal review occurred.

## Merge and privileged boundaries

No agent or supervising ChatGPT workflow may merge, enable auto-merge, bypass protection, rewrite shared history, or perform
privileged repository/hosting operations without Dmitry's explicit instruction. A task may prepare a decision packet and
verification checklist; the privileged operator remains human unless the issue explicitly authorizes otherwise.

## Local app integration

The app-native Local Codex dispatcher and worker prompts live under `.codex/local/`. They implement this policy for one
persistent dispatcher, one issue worker chat/worktree, and in-chat follow-up. Their filenames are preserved because
configured Scheduled tasks may reference them. The detailed environment runbook remains in
[`../local-codex-worker.md`](../local-codex-worker.md).
