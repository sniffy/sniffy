# Supervision and delivery control

This policy applies whenever ChatGPT supervises work performed by Codex Cloud, Local Codex, an IDE agent, ChatGPT itself,
automation such as Dependabot, an external contributor, or a human.

## Lifecycle authority

Use [`lifecycle.md`](lifecycle.md) as the canonical Status/Execution model:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification? -> Approval -> Done
Execution: Ready | In progress | Blocked
```

A local commit is not publication; publication is not lifecycle handoff; handoff is not Review; Review is not Verification;
Verification is not merge. `Blocked / Human` is reserved for one exact Dmitry decision or action.

## Canonical issue and PR

Apply [`pull-request-intake.md`](pull-request-intake.md) before supervising a PR:

- one formal closing issue -> the issue is canonical and the PR is exact-head implementation evidence;
- no closing issue -> the PR is canonical;
- several closing issues -> the PR is canonical in Planning;
- draft PR -> do not start Review;
- issue and PR both in Project -> keep one canonical active item and make the duplicate non-claimable.

Every report names the canonical item and the exact implementation PR head when one exists.

## Lifecycle handoff

A worker owns one lifecycle turn and must publish its own completion signal through the common guarded protocol:

- Planning records deliberate future Implementer/Verifier routes;
- Implementation publishes one intended PR, marks it ready for review, verifies open/develop/non-draft/exact-head state, and hands
  the canonical item to `Review / Ready / ChatGPT`;
- Review routes to Verification, Approval, a deliberate correction route, Planning, or Human;
- Verification routes to Approval or a precisely classified correction;
- Approval becomes Done only after actual merge or deliberate closure.

A command setting Review identifies the exact PR structurally: target plus `expected.head` for a canonical PR, or
`reviewPullRequest.number/head` for a canonical issue. Every handoff clears stale claim/lease ownership. Assignment and PR state are
updated and verified separately.

Every autonomous control command uses the human-readable Markdown wrapper from [`control-plane.md`](control-plane.md), its exact
start/end markers, and a lowercase `json` fence. The marked JSON is authoritative. Never use target-item polling or lease comments
as a substitute for the central control protocol.

## Dispatch proof

`In progress` is valid only with a successful guarded claim and a concrete worker or external-operation reference:

- Codex Cloud: exact trigger plus durable acknowledgement or one preserved provisional generation;
- Local Codex app: concrete task/chat/worktree;
- Local Codex CLI: concrete process/worktree/branch;
- adopted continuation: exact same-repository PR/branch/head selected by the canonical route;
- contributor/bot operation: exact PR/head plus the concrete requested operation.

For supervised Cloud dispatch, claim while preserving Executor and record the exact trigger generation plus lease.
When a trigger was not submitted or was definitively rejected, release the provisional claim to Ready. When submission succeeded
but acknowledgement is uncertain, preserve one provisional generation under a short lease; never redispatch it blindly.

## Lease-based stale recovery

The scheduler checks the queue every 15 minutes, but it does **not** poll healthy workers every 15 minutes. A worker is responsible
for updating the canonical issue/PR and performing the guarded lifecycle handoff when it finishes.

Every claim records at least:

```text
claimToken
generation
claimedAt
leaseUntil
worker/task/process/branch/PR reference
```

A valid future `leaseUntil` means ownership is active. Normal dispatcher ticks:

- count it against executor capacity;
- do not open the worker conversation/task;
- do not query provider-wide inventory;
- do not post “still running” comments;
- do not schedule a second monitor.

A worker that legitimately needs more time renews the same claim/generation before expiry through a guarded update. Completion is
signalled by its normal evidence plus guarded handoff, which clears claim and lease data.

An item becomes a stale-recovery candidate only when the Worker reference is missing, the lease is missing/invalid, or
`leaseUntil` has expired. The next ordinary scheduler tick performs one targeted recovery:

1. re-read the canonical item, exact worker reference, branch/PR/head, and relevant GitHub evidence;
2. if the lifecycle handoff already completed, do nothing;
3. if the same worker is demonstrably active, preserve its generation and extend the lease;
4. if completion evidence exists but the handoff was lost, finish the deterministic handoff;
5. if the worker failed or disappeared, recover the same workspace/branch/generation where possible;
6. release to Ready only when no active work or recoverable publication remains;
7. never create a duplicate generation, branch, or PR.

CI waits, contributor updates, Dependabot operations, rebases, and draft publication use bounded operation leases too. They are
revisited after lease expiry, or earlier only when a GitHub event has already produced a new actionable lifecycle state. There is no
staged periodic per-worker observation cycle and no separate monitoring scheduler.

## Branch and PR continuity

Fresh work uses one branch and one intended PR. Continuation reuses the exact open same-repository branch and PR regardless of who
created it. Changing executor preserves useful commits and evidence; never reset to develop, rebase, force-push, or open a duplicate
PR merely because ownership changed.

Fork PRs remain contributor-owned. Dependabot and other managed automation branches remain bot-owned. Use contributor feedback,
documented bot operations, or a deliberate internal replacement task instead of autonomous branch adoption.

## Review and correction

Review inspects the complete exact-head diff, authoritative requirements, tests actually run, generated/unrelated files, unresolved
threads, and matching-head CI/artifacts. Publish one comprehensive result.

Formal review requires an identity independent from the PR author. When blockers remain:

- independent reviewer -> comprehensive `REQUEST_CHANGES`;
- PR author identity -> the same complete findings as an ordinary PR comment, explicitly stating the identity limitation.

The technical Review outcome and durable route complete in the same tick. Route code/design defects to `Implementation / Ready`
after choosing an eligible Implementer; requirements/architecture/canonicalization defects to `Planning / Ready / ChatGPT`; and
one exact maintainer action to `Blocked / Human`. A technically acceptable self-authored PR proceeds to Verification or
`Approval / Ready / Human` rather than remaining in Review.

If corrected work returns to Review and still has substantive blockers, perform the convergence checkpoint in [`routing.md`](routing.md)
before another implementation dispatch. Continue coherently, preserve and escalate the existing PR, return to Planning, or request
one exact Dmitry decision. Infrastructure noise is not a substantive correction round.

## Target-comment noise and merge boundary

The canonical issue/PR contains human-useful plans, reviews, proof, blockers, and handoffs. Technical commands, claims, leases, and
recovery details stay in the rotating control log and scheduler telemetry.

No agent may merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged repository/hosting
operations without Dmitry's explicit instruction.
