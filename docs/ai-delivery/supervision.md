# Supervision and delivery control

This policy applies whenever ChatGPT supervises work performed by Codex Cloud, Local Codex, an IDE agent, ChatGPT itself,
automation such as Dependabot, or a human contributor.

## Lifecycle authority

Use [`lifecycle.md`](lifecycle.md) as the canonical Status/Execution model:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification? -> Approval -> Done
Execution: Ready | In progress | Blocked
```

`Ready` applies to the next action in the current lifecycle status. `Review` is a lifecycle status. `Blocked` preserves where
work should resume, stops autonomous processing, and routes the next action to Dmitry.

Do not replace lifecycle fields with optimistic prose. Track supporting facts independently:

- guarded claim/transition result;
- concrete dispatch acknowledgement;
- local completion and local proof;
- remote publication at an exact branch/SHA/PR;
- code review outcome;
- verification outcome;
- actual merge or deliberate closure.

A local commit is not publication; publication is not review; review is not verification; verification is not merge.

## Lifecycle handoff

A worker completes one lifecycle turn and writes the next route through the common guarded protocol in
[`control-plane.md`](control-plane.md):

- Planning records `Implementer` when a future Implementation turn is needed and records `Verifier`; handoff to Dmitry stays
  `Planning / Ready / Human`.
- Planning approval may write `Implementation / Ready / Executor := Implementer` only when Implementer is non-empty and
  deliberately selected.
- Implementation publication writes `Review / Ready / Executor := ChatGPT` by default.
- External PR intake writes `Review / Ready / Executor := ChatGPT`, the selected Verifier, and evidence fields while leaving
  Implementer unchanged/empty.
- Successful Review writes `Verification / Ready / Executor := Verifier` or `Approval / Ready / Human` when evidence is already
  sufficient.
- Successful Verification writes `Approval / Ready / Human`.
- Approval becomes `Done` only after actual merge or deliberate closure.

A blank Implementer is valid outside a routed Implementation turn. It must not prevent intake, Review, rebase monitoring,
Verification, Approval, blocking, supersession, or closure. When Review or Verification discovers that new code is needed and
Implementer is empty, deliberately choose a supported executor through Planning or create a linked replacement task before
entering Implementation. Do not infer that route from the external PR author.

Every handoff clears stale worker ownership and lease data. GitHub assignment is updated separately and verified; a Project field
transition does not prove assignment succeeded.

## Dispatch proof

- A Cloud implementation is dispatched only after the proven trigger has a connector reaction, task link, or equivalent durable
  acknowledgement. A review mention alone is not implementation dispatch.
- An app-native Local Codex item is dispatched only after a verified claim plus a concrete worker task/chat/worktree.
- A headless Local Codex item is dispatched only after a verified claim plus process/worktree/branch record.
- An IDE agent is working only while the human explicitly owns the interactive session or remote evidence proves activity.
- A ChatGPT scheduled tick is working only after its guarded claim succeeded and it began the exact lifecycle operation.
- A ChatGPT direct-execution task is working only after the exact GitHub or sandbox operation began.

`In progress` with no verified claim/current worker reference is invalid. When execution or external dispatch cannot start,
release to `Ready`. Set `Blocked` only when Dmitry must decide or act; set `Executor = Human`, assign `bedrin`, and record the
exact requested action.

## Queue polling and worker monitoring

Queue polling and worker follow-up are responsibilities of the same event loop:

- **Queue polling** finds `Ready` work and claims it. Desired logical cadence is every 15 minutes.
- **External PR intake** reconciles eligible PRs before ordinary queue work without inventing an Implementer.
- **Worker monitoring** observes an existing claimed implementation/correction after 15 minutes, again 15 minutes later, then
  hourly while incomplete.
- **Control-log maintenance** rotates the active technical issue when a needed command would exceed the documented threshold.

Do not create an additional monitoring Scheduled Task. A normal dispatcher tick first checks whether an existing owned worker is
due for observation; only then may it claim unrelated work.

Every new delegated task, retry, continuation, executor change, or Request Changes return starts the normal observation cycle:

1. observe after 15 minutes;
2. if incomplete, observe 15 minutes later;
3. if still incomplete, observe hourly.

Each observation re-reads lifecycle fields, intended executor and assignee, worker record, expected branch/PR, acknowledgement or
new head, review threads, and current CI. `Implementer` is read when relevant but may be empty; current ownership comes from
`Executor`, `Assignee`, and Worker reference. Notify the target conversation only on meaningful progress, completion, or a real
blocker. Routine “still running” telemetry remains in the scheduler/worker transcript.

A dispatch comment does not prove work started. Verify acknowledgement, process/task reference, or new remote evidence. Stop
monitoring when lifecycle handoff completes or Dmitry owns a blocked next action.

## Branch and PR continuity

- Fresh work uses one new branch from current `develop` and one intended PR unless independently useful slices were approved.
- Continuation work reuses the exact open branch and PR.
- Changing executor preserves useful published work; do not reset to `develop`, rebase, force-push, or create a duplicate PR.
- When `develop` moves materially before handoff, normally merge current `origin/develop` into the feature branch and rerun
  exact-head proof. Do not use a stale green run as final evidence.
- Preserve completed work when publication fails. Recover the existing commit/workspace rather than rebuilding by default.

For Dependabot and other automation-owned PRs:

- review the bot's exact head directly when the update is self-contained;
- request the bot's documented rebase rather than rewriting its branch for a stale base or conflict;
- keep `Executor = ChatGPT` while waiting for that observable rebase and record the old head plus next observation point;
- do not require or set `Implementer` merely to monitor the external operation;
- do not normally push compatibility fixes onto the bot branch;
- create a linked issue and agent-owned replacement PR when implementation changes are required, selecting an Implementer for
  that linked work;
- keep the source PR blocked/superseded with explicit links and rationale.

## Review and correction

Review audits the whole acceptance-to-proof matrix, not only the visible delta. Inspect:

- complete exact-head diff and scope;
- architecture, module/API ownership, compatibility, lifecycle, and resource safety;
- test design, discovery, and implementer proof;
- dependencies, workflows, permissions, and standard-tooling rationale;
- generated/unrelated files, documentation, migration, rollback, and PR-body accuracy;
- unresolved comments and threads;
- exact-head CI statuses and relevant logs.

Report independent findings together. One comprehensive Request Changes is better than serial discovery of unrelated blockers.

A Request Changes result does not justify blindly assigning the external author as Implementer. For a Sniffy-owned implementation
PR, return to its already selected implementation route. For an external PR with empty Implementer, either keep the PR in Review
while the external author updates it voluntarily, or route deliberate replacement/correction work through Planning with a
supported Implementer.

### Convergence checkpoint

The checkpoint is not a timer and does not create another process. It is a required Review decision inside the normal lifecycle.

Trigger it when a substantive Request Changes returned work to Implementation, the corrected head came back to Review, and the
next complete Review still finds substantive blockers. Before another correction dispatch, ChatGPT performs the critical
analysis in [`routing.md`](routing.md): verify the issue, architecture, acceptance criteria, proof strategy, executor capability,
and whether another narrow patch would converge.

The checkpoint must explicitly choose one route:

- continue the same executor with coherent superseding guidance;
- preserve the branch/PR and escalate continuation to Local Codex;
- return to Planning and re-baseline the architecture/requirements/proof matrix;
- block for one exact Dmitry decision or action.

Do not automatically send a third list of local edits. Do not automatically escalate for infrastructure noise or an ordinary
failing check. Record substantive correction rounds separately from CI/provider noise.

After an executor change, start the normal 15-minute observation cycle only when the replacement dispatch is concretely
acknowledged. The convergence checkpoint itself needs no schedule.

## Review identities

Formal review must use an identity independent from the PR author.

- When independent review is available and proof passes, submit `APPROVE`.
- When blockers remain, submit one precise `REQUEST_CHANGES`, then explicitly return the task to an already selected internal
  Implementation route or create the linked replacement task required by PR intake policy.
- When the active identity cannot review its own PR, leave exact ready-for-Dmitry-review or blocking feedback and state the
  identity limitation.

A Dependabot-authored PR is independent from `bedrin-gpt`, so ChatGPT may formally approve or request changes after a complete
exact-head review. That identity rule comes from the PR author, not the optional Project Implementer field.

## Target-comment noise policy

The central control issue stores technical Project transition commands. Do not post claim intents, field commands, lease
arbitration, polling notices, or winner/loser messages on the target item.

The target issue/PR should contain only human-useful plans, review decisions, publication/verification evidence, real blockers,
and meaningful handoffs. Preserve technical control commands for troubleshooting in the rotating log rather than deleting them.

## Merge and privileged boundaries

No agent or supervising workflow may merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged
repository/hosting operations without Dmitry's explicit instruction. `Approval / Ready` is the human acceptance queue. This
boundary also applies to Dependabot even though GitHub supports bot commands and auto-merge.
