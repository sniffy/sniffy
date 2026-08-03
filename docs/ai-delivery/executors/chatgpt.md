# ChatGPT supervisor and executor

ChatGPT is the default delivery supervisor and may plan, implement, review, or verify when the active chat has the required GitHub,
sandbox, artifact, browser, identity, and publication capabilities.

## Recurring scheduler

Create recurring heartbeats in **Chat**, not **Work**. Work is reserved for a selected long-running investigation or lifecycle turn.
The scheduler reads only `.chatgpt/status-snapshot-instructions.md`, `runtime-contract.md`, and one current generated dispatch view.
It does not repeatedly preload the entire AI-delivery documentation set.

Use either:

- one hourly budget Chat task; or
- four compact hourly Chat tasks at `:00`, `:15`, `:30`, and `:45` for 15-minute queue latency.

The exact task text lives in [`.chatgpt/scheduled-task-prompt.md`](../../../.chatgpt/scheduled-task-prompt.md). Persistent task chats
are telemetry, not durable state. Repository changes do not rewrite embedded task prompts; replace and smoke-test them manually.

A tick chooses at most the first programmatically generated attention candidate, live-reads only that target, performs one guarded
reconciliation/lifecycle turn/external dispatch, and stops. Empty ticks return `NO_CHANGE` plus telemetry without work-item,
Project, source, worker, target-comment, or control mutation.

## Supervisor responsibilities

ChatGPT owns:

- universal base-branch PR intake and deterministic issue/PR canonicalization;
- duplicate representation suppression;
- stale exact-head and route/assignee reconciliation;
- Planning and deliberate Implementer/Verifier selection;
- supervised Codex Cloud dispatch and 15/15/hourly observation;
- exact-head Review and verification coordination;
- convergence checkpoints after repeated substantive blockers;
- control-log rotation;
- clear human handoff and no-merge enforcement.

It does not copy PR author/provider identity into Implementer. It preserves an explicitly routed same-repository PR and never adopts
a fork or Dependabot branch for direct correction.

## Capabilities and delegation

Treat GitHub connector, arbitrary network, command sandbox, installed runtimes/dependencies, artifact access, browser, credentials,
branch write permission, and formal review identity as separate capabilities. Run a targeted capability preflight after candidate
selection. Delegate missing obligations rather than claiming unsupported proof.

Focused connector-backed implementation is allowed when ChatGPT can honestly edit, test/inspect, publish, and verify. Bounded fresh
work may go to Codex Cloud. Complex/persistent/cross-version/service/browser or exact existing-PR work normally goes to Local Codex.
Privileged or unresolved product/risk decisions go to Dmitry.

## Common Project control path

ChatGPT uses `delivery-control/v1` on the newest active `ai-delivery-control` issue. Every new autonomous command uses the
human-readable Markdown wrapper from `control-plane.md`, exact markers, and lowercase `json` fence. The authoritative marked JSON,
not display prose, drives automation. Never post bare JSON, field commands, claim arbitration, leases, or polling messages on the
target item.

A claim guards current canonical type, Status, `Execution = Ready`, Executor, and exact PR head when applicable; it sets a unique
worker token/owner/lease/reference/next observation. A handoff is complete only after the terminal reaction and re-read Project,
assignment, and coupled PR state.

## PR intake and Review publication

Programmatic snapshot projections cover every open PR targeting `develop` regardless of author/label/provider. ChatGPT live-reads
only a selected attention candidate.

- one formal closing issue -> issue canonical;
- no formal closing issue -> PR canonical;
- multiple formal closing issues -> PR Planning;
- draft PR -> no Review.

Implementation publication is complete only when the intended PR is open, targets develop, is ready for review/non-draft, matches
the exact inspected remote head, and has synchronized evidence. A canonical issue transition includes
`reviewPullRequest.number/head`; a canonical PR guards target plus `expected.head`.

## Supervised Codex Cloud

The profile marks Codex Cloud `dispatchMode: supervised` with ChatGPT as dispatch owner. ChatGPT claims
`Implementation / Ready / Codex Cloud` while preserving Executor, submits exactly one supported trigger, requires durable
acknowledgement, records the generation and first 15-minute observation, then stops. A definitely rejected/non-submitted trigger is
released to Ready. An uncertain acknowledged generation remains provisional and is recovered without duplicate dispatch.

## Review identity and durable outcome

Review inspects the complete exact-head diff, canonical requirements, generated/unrelated files, tests actually run, unresolved
threads, and matching-head CI/artifacts. Prefer one comprehensive outcome.

If the reviewer is independent, blockers receive one formal `REQUEST_CHANGES`. If the active identity authored the PR, publish the
same complete blocking findings as an ordinary PR comment and state the identity limitation. The technical Review outcome is still
completed in the same tick:

- implementation/code/design defect -> `Implementation / Ready` after deliberately selecting an Implementer;
- requirements/architecture/canonicalization defect -> `Planning / Ready / ChatGPT`;
- exact Dmitry action -> `Blocked / Human`;
- technically acceptable -> required Verification or `Approval / Ready / Human`.

Never leave the reviewed exact head in `Review / Ready / ChatGPT` merely because formal self-review is unavailable.

## Monitoring and telemetry

After starting a worker/operation, observe after 15 minutes, another 15 minutes, then hourly. Store `nextObservationAt` durably.
Routine waiting remains In progress.

Every tick records start/end/duration, scheduler, model/reasoning when exposed, snapshot run, candidate, outcome, and exact provider
token counters when exposed. Otherwise use null counters and `usageSource: unavailable`; never invent billing precision.

## Boundaries

Use the strongest truthful evidence and say exactly what was not run. Never merge, enable auto-merge, bypass protection, rewrite
shared history, expose credentials, deploy, or change privileged settings without Dmitry's explicit instruction.
