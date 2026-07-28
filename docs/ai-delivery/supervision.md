# Supervision and delivery control

This policy applies whenever ChatGPT supervises work performed by Codex Cloud, Local Codex, an IDE agent, ChatGPT itself,
automation such as Dependabot, or a human contributor using the AI-assisted delivery workflow.

## Lifecycle authority

Use [`lifecycle.md`](lifecycle.md) as the canonical Phase/Status model:

```text
Phase:  Draft -> Planning -> Implementation -> Review -> Verification? -> Approval -> Done
Status: Ready | In progress | Blocked
```

`Ready` applies to the next action in the current phase. It may be pool-ready for a worker or directed-ready for a specific
assignee such as Dmitry. `Review` is a phase. `Blocked` preserves the phase where work should resume.

Do not replace these fields with optimistic prose. Track supporting delivery facts independently:

- concrete dispatch/claim acknowledgement;
- local completion and local proof;
- remote publication at an exact branch/SHA/PR;
- code review outcome;
- verification outcome;
- actual merge or deliberate closure.

A local commit is not publication; publication is not review; review is not verification; verification is not merge.

## Phase handoff

A worker completes one phase turn and writes the next route:

- Planning records `Implementer` and `Verifier`; handoff to Dmitry stays `Planning / Ready / Human`.
- Planning approval writes `Implementation / Ready / Executor := Implementer`.
- Implementation publication writes `Review / Ready / Executor := ChatGPT` by default.
- External PR intake may write `Review / Ready / Implementer := PR author / Executor := ChatGPT` directly.
- Successful Review either writes `Verification / Ready / Executor := Verifier` or `Approval / Ready / Human` when existing
  evidence already satisfies the issue.
- Successful Verification writes `Approval / Ready / Human`.
- Approval becomes `Done` only after actual merge or deliberate closure.

Every handoff clears stale worker ownership and sets the intended Assignee or leaves it empty for a pool claim.

## Dispatch proof

- A Cloud implementation is dispatched only after the proven trigger has a connector reaction, task link, or equivalent
  durable acknowledgement. A review mention alone is not implementation dispatch.
- An app-native Local Codex item is dispatched only after claim plus confirmed worker task/chat/worktree.
- A headless Local Codex item is dispatched only after claim plus confirmed process/worktree/branch record.
- An IDE agent is working only while the human explicitly owns the interactive session or remote evidence proves activity.
- A ChatGPT scheduled tick is working only after its fresh chat has won a claim and begun the exact intended phase operation.
- A ChatGPT direct-execution task is working only after the exact intended GitHub or sandbox operation has begun.

`In progress` with no verified claim/current tick/worker reference is invalid. When execution or external dispatch cannot start,
release to `Ready` or record an exact `Blocked` reason.

## Queue polling and worker monitoring

The reusable queue event loop is described in [`event-loop.md`](event-loop.md). Queue polling and per-worker follow-up are
different concerns:

- **Queue polling** finds `Ready` work and claims it. The desired logical cadence is every 15 minutes.
- **External PR intake** uses GitHub Project Auto-add for new matching PRs and an idempotent tick reconciliation for existing or
  missed automation/contributor PRs. It materializes the PR itself in Review without a shadow issue when possible. See
  [`pull-request-intake.md`](pull-request-intake.md).
- **Worker monitoring** checks a claimed implementation or correction after 15 minutes, again 15 minutes later, then hourly
  while incomplete.

The current ChatGPT adapter uses four hourly Scheduled Tasks offset by 15 minutes. Every occurrence opens a fresh chat, reads
all durable state from GitHub/repository Markdown, and performs at most one phase turn directly. It cannot create a child
Scheduled Task and must not rely on previous tick chats. A no-op still creates a chat transcript but creates no GitHub/source
mutation. Keep the four task-definition chats stable; archive completed tick transcripts according to
[`chat-retention.md`](chat-retention.md). Chat cleanup never changes lifecycle state.

Every new delegated task, retry, continuation, or Request Changes return starts a fresh worker-monitoring cycle:

1. check after 15 minutes;
2. if incomplete, check 15 minutes later;
3. if still incomplete, switch to hourly monitoring.

Do not postpone the first check to the hourly monitor. Each check re-reads lifecycle fields, intended executor and assignee,
claim/worker record, expected branch and PR, acknowledgement or new head, review threads, and current CI. Notify only on
meaningful progress, completion, or a real blocker.

A review or dispatch comment does not prove work started. Verify acknowledgement or new remote evidence. Stop worker
monitoring when the phase handoff completes or a human/external blocker owns the next action.

## Branch and PR continuity

- Fresh work uses one new branch from current `develop` and one intended PR unless independently useful slices were approved.
- Continuation work reuses the exact open branch and PR. Do not reset to `develop`, rebase, force-push, create a replacement
  branch, or open a duplicate PR.
- When `develop` moves materially before handoff, normally merge current `origin/develop` into the feature branch and rerun
  exact-head proof. Do not use a stale green run as final evidence.
- Preserve completed work when publication fails. Diagnose access and recover the existing commit rather than rebuilding.

For Dependabot and other automation-owned PRs:

- review the bot's exact head directly when the proposed update is self-contained;
- request the bot's documented rebase rather than rewriting its branch for a stale base or conflict;
- do not normally push compatibility fixes onto the bot branch because extra commits complicate automated rebasing;
- create a linked issue and agent-owned replacement PR when implementation changes are required, and keep the source PR
  blocked/superseded with explicit links and rationale.

## Review and correction

The Review phase audits the whole acceptance-to-proof matrix, not only the visible delta. Inspect:

- complete exact-head diff and scope;
- architecture, module/API ownership, compatibility, lifecycle, and resource safety;
- test design, discovery, and implementer proof;
- dependencies, workflows, permissions, and standard-tooling rationale;
- generated/unrelated files, documentation, migration, rollback, and PR-body accuracy;
- unresolved comments and threads;
- exact-head CI statuses and relevant logs as supporting evidence.

Report independent findings together. After corrections, inspect the new delta and affected proof. Two substantive unattended
correction rounds, or one product/architecture reversal, trigger re-baselining: update the authoritative issue, mark
superseded guidance, and deliberately choose the next Implementer/Verifier. Infrastructure noise does not increment the
substantive counter.

## Review identities

The Assignee or PR author is a technical identity, not the executor product. Formal review must use an identity independent
from the PR author.

- When independent review is available and proof passes, submit `APPROVE`.
- When blockers remain, submit one precise `REQUEST_CHANGES`, then explicitly return the task to `Implementation / Ready` or
  create the linked replacement task required by the PR intake policy.
- When the active identity cannot review its own PR, leave exact ready-for-Dmitry-review or blocking feedback and state the
  identity limitation. Do not pretend a formal review occurred.

A Dependabot-authored PR is independent from `bedrin-gpt`, so ChatGPT may formally approve or request changes after a complete
exact-head review.

## Merge and privileged boundaries

No agent or supervising ChatGPT workflow may merge, enable auto-merge, bypass protection, rewrite shared history, or perform
privileged repository/hosting operations without Dmitry's explicit instruction. `Approval / Ready` is the human acceptance
queue; there is no separate Ready-to-merge phase unless an approved-but-unmerged backlog becomes a real need. This boundary
also applies to Dependabot even though GitHub supports Dependabot commands and auto-merge.
