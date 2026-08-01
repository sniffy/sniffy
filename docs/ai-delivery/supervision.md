# Supervision and delivery control

This policy applies whenever ChatGPT supervises work performed by Codex Cloud, Local Codex, an IDE agent, ChatGPT itself,
automation such as Dependabot, an external contributor, or a human.

## Lifecycle authority

Use [`lifecycle.md`](lifecycle.md) as the canonical Status/Execution model:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification? -> Approval -> Done
Execution: Ready | In progress | Blocked
```

`Ready` applies to the next action in the current lifecycle status. `Review` is a lifecycle status. `Blocked` preserves where
work should resume, stops autonomous processing, and routes the next action to Dmitry.

Do not replace lifecycle fields with optimistic prose. Track supporting facts independently:

- canonical issue/PR decision;
- guarded claim/transition result;
- concrete dispatch acknowledgement;
- local completion and local proof;
- remote publication at an exact branch/SHA/PR;
- PR open/base/draft state at handoff;
- code review outcome;
- verification outcome;
- actual merge or deliberate closure.

A local commit is not publication; publication is not lifecycle handoff; handoff is not review; review is not verification;
verification is not merge.

## Canonical issue and PR supervision

Before supervising a PR, apply [`pull-request-intake.md`](pull-request-intake.md):

- one formal closing issue -> supervise lifecycle on that issue and keep the PR exact head in Worker reference;
- no closing issue -> supervise lifecycle on the PR itself;
- several closing issues -> supervise Planning on the PR and suppress duplicate linked-issue work until scope is resolved.

If both an issue and its PR appear in Project 2, do not start two workers or Reviews. Preserve one canonical active item and make
the duplicate non-claimable through a guarded reconciliation. Every status report names both the canonical item and exact PR head
when implementation exists.

## Lifecycle handoff

A worker completes one lifecycle turn and writes the next route through the common guarded protocol in
[`control-plane.md`](control-plane.md):

- Planning records `Implementer` when a future Implementation turn is needed and records `Verifier`; handoff to Dmitry stays
  `Planning / Ready / Human`.
- Planning approval may write `Implementation / Ready / Executor := Implementer` only when Implementer is non-empty and
  deliberately selected.
- Implementation publication marks the PR ready when local proof is complete, re-reads open/develop/non-draft/exact-head state,
  then writes the canonical item to `Review / Ready / Executor = ChatGPT` with PR URL and exact head in Worker reference.
- The guarded Review command identifies that PR structurally: target plus `expected.head` for a canonical PR, or
  `reviewPullRequest.number/head` for a canonical issue.
- Universal PR intake performs the same Review handoff only for a non-draft PR that appeared outside the delivery system, leaving
  Implementer unchanged/empty.
- A multi-issue PR enters `Planning / Ready / ChatGPT` before Review.
- Successful Review writes `Verification / Ready / Executor := Verifier` or `Approval / Ready / Human` when evidence is already
  sufficient.
- Successful Verification writes `Approval / Ready / Human`.
- Approval becomes `Done` only after actual merge or deliberate closure.

The control plane re-checks the review PR after all expected-state guards pass and marks it ready if an executor left it draft. That
is a final invariant and recovery mechanism, not permission for executors to skip publication. It verifies the same exact head
before Project Review is written.

A blank Implementer is valid outside a routed Implementation turn. It must not prevent canonicalization, intake, Review, rebase or
contributor monitoring, Verification, Approval, blocking, supersession, or closure. When Review or Verification discovers that new
code is needed and Implementer is empty, deliberately choose a supported executor before entering Implementation.

Every handoff clears stale worker ownership and lease data. GitHub assignment is updated separately and verified; a Project field
transition does not prove assignment succeeded. A Review handoff additionally re-reads PR draft/head state after the terminal
control reaction.

## Dispatch proof

- A Cloud implementation is dispatched only after the proven trigger has a connector reaction, task link, or equivalent durable
  acknowledgement. A review mention alone is not implementation dispatch.
- Codex Cloud does not poll Project 2. ChatGPT owns dispatch for a valid `Implementation / Ready / Codex Cloud` route: claim while
  preserving Executor, issue exactly one supported trigger, record durable acknowledgement and the first observation point, or
  release the provisional claim to Ready when the trigger was not submitted or was definitively rejected. A submitted trigger
  with uncertain acknowledgement remains one provisional generation for targeted recovery; never redispatch it blindly.
- An app-native Local Codex item is dispatched only after a verified claim plus a concrete worker task/chat/worktree.
- A headless Local Codex item is dispatched only after a verified claim plus process/worktree/branch record.
- An adopted existing PR is dispatched only after the canonical Project item explicitly routes the exact same-repository PR,
  branch, and head to the selected executor.
- An IDE agent is working only while the human explicitly owns the interactive session or remote evidence proves activity.
- A ChatGPT scheduled tick is working only after its guarded claim succeeded and it began the exact lifecycle operation.
- A ChatGPT direct-execution task is working only after the exact GitHub or sandbox operation began.

`In progress` with no verified claim/current worker or observable external-operation reference is invalid. When execution or
external dispatch cannot start, release to `Ready`. Set `Blocked` only when Dmitry must decide or act; set `Executor = Human`,
assign `bedrin`, and record the exact requested action.

## Queue polling, PR intake, and worker monitoring

Queue polling and worker follow-up are responsibilities of the same event loop:

- **Universal PR intake** scans every open PR targeting `develop`, canonicalizes issue versus PR, and materializes reviewable work
  before ordinary queue selection.
- **Queue polling** finds canonical `Ready` work and claims it. Desired logical cadence is every 15 minutes.
- **Supervised external dispatch** lets the ChatGPT loop select configured `Ready / Codex Cloud` work even though ChatGPT is not
  the Implementer. The acknowledged dispatch, not the Ready route alone, starts worker monitoring.
- **Ready-route reconciliation** repairs a non-Human Ready item whose Assignee belongs to a different executor pool; such an item
  must not disappear from every dispatcher's eligibility filter.
- **Worker/operation monitoring** observes an existing implementation, correction, CI wait, contributor update, bot rebase, or
  draft publication after 15 minutes, again 15 minutes later, then hourly while incomplete. It includes ChatGPT-supervised Codex
  Cloud `In progress` routes even though their Executor field remains Codex Cloud.
- **Control-log maintenance** rotates the active technical issue when a needed command would exceed the documented threshold.

Do not create an additional monitoring Scheduled Task. A normal dispatcher tick first checks whether an existing owned worker or
PR operation is due for observation; only then may it claim unrelated work.

Every new delegated task, retry, continuation, adopted PR, executor change, or Request Changes return starts the normal
observation cycle:

1. observe after 15 minutes;
2. if incomplete, observe 15 minutes later;
3. if still incomplete, observe hourly.

Each observation re-reads canonical fields, linked issues/PR, intended executor and assignee, worker record, exact branch/head,
acknowledgement or new head, draft state, review threads, and current CI. `Implementer` may be empty; current ownership comes from
`Executor`, `Assignee`, and Worker reference. Notify the target conversation only on meaningful progress, completion, or a real
blocker. Routine “still running” telemetry remains in the scheduler/worker transcript.

A dispatch comment does not prove work started. Verify acknowledgement, process/task reference, or new remote evidence. Stop
monitoring when lifecycle handoff completes or Dmitry owns a blocked next action.

## Branch and PR continuity

- Fresh work uses one new branch from current `develop` and one intended PR unless independently useful slices were approved.
- Continuation/adopted-continuation work reuses the exact open same-repository branch and PR, regardless of whether Dmitry,
  ChatGPT, Codex, or an IDE agent originally created it.
- The canonical route, effective push permission, and absence of competing ownership authorize adoption; author identity alone does
  not.
- Changing executor preserves useful published work; do not reset to `develop`, rebase, force-push, or create a duplicate PR.
- When `develop` moves materially before handoff, normally merge current `origin/develop` into the feature branch and rerun
  exact-head proof. Do not use a stale green run as final evidence.
- Preserve completed work when publication fails. Recover the existing commit/workspace rather than rebuilding by default.
- Implementation publication is incomplete until the PR is open, targets `develop`, is ready for review (`draft = false`) at the
  exact published head, and the canonical item has a verified guarded `Review / Ready / ChatGPT` handoff.

For fork PRs:

- review the contributor's exact head without privileged execution of untrusted code;
- submit contributor-facing Request Changes and monitor for a new head;
- do not autonomously push to the fork or adopt the branch;
- create a linked internal replacement task only after an explicit routing decision.

For Dependabot and other managed automation PRs:

- review the bot's exact head directly when the update is self-contained;
- request the documented rebase rather than rewriting its branch for a stale base or conflict;
- keep `Executor = ChatGPT` while waiting and record old head plus next observation point;
- do not require or set Implementer merely to monitor the external operation;
- do not normally push compatibility fixes onto the bot branch;
- create a linked issue and agent-owned replacement PR when implementation changes are required;
- keep the source PR blocked/superseded with explicit links and rationale.

## Review and correction

Review audits the whole acceptance-to-proof matrix, not only the visible delta. Inspect:

- complete exact-head diff and scope;
- every authoritative/closing issue and later decision;
- architecture, module/API ownership, compatibility, lifecycle, and resource safety;
- test design, discovery, and implementer proof;
- dependencies, workflows, permissions, and standard-tooling rationale;
- generated/unrelated files, documentation, migration, rollback, and PR-body accuracy;
- unresolved comments and threads;
- exact-head CI statuses and relevant logs.

Report independent findings together. One comprehensive Request Changes is better than serial discovery of unrelated blockers.

A Request Changes result does not justify assigning the PR author as Implementer. Choose among:

- same-repository focused correction by ChatGPT on the exact branch;
- same-repository complex/persistent correction by Local Codex adopted continuation;
- contributor-owned correction for a fork PR;
- documented bot operation for Dependabot/managed automation;
- linked internal replacement task when direct branch correction is unsafe or inappropriate;
- return to Planning when scope, canonicalization, architecture, or proof is unresolved.

After correction, the implementation worker publishes a new exact head, marks the PR ready, verifies non-draft exact-head state,
and hands the same canonical item back to Review. Do not create a second issue or PR solely because ownership changed.

### Convergence checkpoint

The checkpoint is not a timer and does not create another process. It is a required Review decision inside the normal lifecycle.

Trigger it when a substantive Request Changes returned work to Implementation, the corrected head came back to Review, and the
next complete Review still finds substantive blockers. Before another correction dispatch, ChatGPT performs the critical analysis
in [`routing.md`](routing.md): verify the canonical item, architecture, acceptance criteria, proof strategy, executor capability,
branch continuity, and whether another narrow patch would converge.

The checkpoint must explicitly choose one route:

- continue the same executor with coherent superseding guidance;
- preserve/adopt the branch and PR and escalate continuation to Local Codex;
- return to Planning and re-baseline architecture/requirements/canonicalization/proof;
- block for one exact Dmitry decision or action.

Do not automatically send a third list of local edits. Do not automatically escalate for infrastructure noise or an ordinary
failing check. Record substantive correction rounds separately from CI/provider noise.

After an executor change, start the normal 15-minute observation cycle only when the replacement dispatch is concretely
acknowledged. The convergence checkpoint itself needs no schedule.

## Review identities

Formal review must use an identity independent from the PR author.

- When independent review is available and proof passes, submit `APPROVE`.
- When blockers remain, submit one precise `REQUEST_CHANGES`, then explicitly route the canonical item to an implementation or
  external-author correction path.
- When the active identity cannot review its own PR, leave exact ready-for-Dmitry-review or blocking feedback and state the
  identity limitation.

A Dmitry-, Dependabot-, external-contributor-, or other independently authored PR may receive a formal `bedrin-gpt` review.
A `bedrin-gpt`-authored PR cannot. That identity rule comes from the PR author, not the optional Project Implementer field.

## Target-comment noise policy

The central control issue stores technical Project transition commands. Do not post claim intents, field commands, lease
arbitration, polling notices, or winner/loser messages on the target item.

The canonical issue/PR should contain only human-useful plans, review decisions, publication/verification evidence, real blockers,
and meaningful handoffs. Linked PR review conversations remain the right place for formal reviews and inline findings. Preserve
technical control commands for troubleshooting in the rotating log rather than deleting them.

## Merge and privileged boundaries

No agent or supervising workflow may merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged
repository/hosting operations without Dmitry's explicit instruction. `Approval / Ready` is the human acceptance queue. This
boundary applies to all PR sources.
