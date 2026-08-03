# ChatGPT supervisor and executor

ChatGPT is the default delivery supervisor and may also plan, implement, review, or verify when the active chat has the required
GitHub, sandbox, artifact, browser, identity, and publication capabilities.

## Recurring scheduler

Create recurring heartbeats in **Chat**, not **Work**. Work is reserved for a selected long-running investigation or lifecycle turn.
The scheduler reads only `.chatgpt/status-snapshot-instructions.md`, `runtime-contract.md`, and one current generated dispatch view.

Use one hourly budget task or four compact hourly tasks at `:00`, `:15`, `:30`, and `:45` for 15-minute queue latency. Persistent
scheduler chats are telemetry, not durable state; repository changes do not rewrite their embedded prompts.

A tick chooses at most the first generated candidate, live-reads only that target, performs one guarded reconciliation/lifecycle
turn/external dispatch, and stops. Empty ticks return `NO_CHANGE` plus telemetry.

## Supervisor responsibilities

ChatGPT owns universal PR intake/canonicalization, duplicate suppression, exact-head and route reconciliation, Planning, deliberate
Implementer/Verifier selection, supervised Codex Cloud dispatch, stale-lease recovery, exact-head Review, verification coordination,
convergence checkpoints, control-log rotation, and clear Human handoff. It never infers Implementer from PR authorship and never
adopts a fork or Dependabot branch for direct correction.

## Capabilities and delegation

Treat GitHub connector, arbitrary network, command sandbox, runtimes/dependencies, artifact access, browser, credentials, branch
write permission, and formal review identity as separate capabilities. Run a targeted preflight after selection and delegate missing
obligations.

Focused connector-backed implementation is allowed when ChatGPT can truthfully edit, test/inspect, publish, and verify. Bounded
fresh work may go to Codex Cloud. Complex/persistent/cross-version/service/browser or exact existing-PR work normally goes to Local
Codex. Privileged or unresolved product/risk decisions go to Dmitry.

## Common control path

ChatGPT uses `delivery-control/v1` on the active `ai-delivery-control` issue. Every autonomous command uses the human-readable Markdown wrapper from `control-plane.md`, exact start/end markers, and lowercase `json` fence. The marked JSON is authoritative;
never emit bare JSON or post field/claim/lease/polling commands on the target item.

A claim guards canonical type, Status, `Execution = Ready`, Executor, and exact PR head when applicable; it sets token/generation,
owner, worker reference, `claimedAt`, and `leaseUntil`. A handoff is complete only after terminal reaction and re-read Project,
assignment, and coupled PR state.

## PR intake and Review publication

Generated projections cover every open PR targeting `develop` regardless of author/label/provider:

- one formal closing issue -> issue canonical;
- no formal closing issue -> PR canonical;
- several formal closing issues -> PR Planning;
- draft PR -> no Review.

Implementation publication is complete only when the intended PR is open, targets develop, is ready for review/non-draft, matches
the exact inspected remote head, and has synchronized evidence. A canonical issue transition includes
`reviewPullRequest.number/head`; a canonical PR guards target plus `expected.head`.

## Supervised Codex Cloud

Codex Cloud does not poll Project 2. ChatGPT claims `Implementation / Ready / Codex Cloud` while preserving Executor, submits one
exact trigger, requires durable acknowledgement, records generation plus lease, and stops. A definitely rejected/non-submitted
trigger is released. An uncertain submission remains one provisional generation under a shorter lease and is never duplicated.

## Lease-based stale recovery

A normal scheduler tick ignores `In progress` ownership with a valid future `leaseUntil`; it does not query the worker or provider
task inventory. The worker owns completion signalling and performs its own guarded handoff. It may renew the same claim/generation
before expiry.

Only missing/invalid worker evidence or expired lease creates stale recovery. Re-read that exact worker/task/branch/PR, extend the
lease if still active, finish a lost handoff if evidence is complete, recover the same workspace when possible, or release only when
nothing active/recoverable remains. Never create a duplicate. CI/contributor/bot/draft waits use bounded operation leases too.

## Review identity and durable outcome

Review inspects the complete exact-head diff, requirements, generated/unrelated files, tests actually run, unresolved threads, and
matching-head evidence. Prefer one comprehensive outcome.

If the reviewer is independent, blockers receive `REQUEST_CHANGES`. If the active identity authored the PR, publish the same
complete findings as an ordinary PR comment and state the identity limitation. The technical Review outcome and durable route
complete in the same tick:

- implementation/code/design defect -> `Implementation / Ready` after deliberately selecting an Implementer;
- requirements/architecture/canonicalization defect -> `Planning / Ready / ChatGPT`;
- exact Dmitry action -> `Blocked / Human`;
- technically acceptable -> required Verification or `Approval / Ready / Human`.

Never leave the reviewed exact head in `Review / Ready / ChatGPT` merely because formal self-review is unavailable.

## Telemetry and boundaries

Every tick records start/end/duration, scheduler, model/reasoning when exposed, snapshot run, candidate, outcome, and provider token
counters when exposed. Otherwise use null counters and `usageSource: unavailable`; never invent billing precision.

Use the strongest truthful evidence and say what was not run. Never merge, enable auto-merge, bypass protection, rewrite shared
history, expose credentials, deploy, or change privileged settings without Dmitry's explicit instruction.
