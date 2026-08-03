# AI-assisted delivery in Sniffy

This directory defines the provider-neutral lifecycle, control plane, routing, supervision, review, and verification model used by
ChatGPT, Codex Cloud, Local Codex, IDE agents, automation, contributors, and humans. Repository engineering rules remain in the
nearest applicable `AGENTS.md`.

## Runtime versus reference policy

Dispatchers must not load this whole directory on every heartbeat.

- [`runtime-contract.md`](runtime-contract.md) is the compact policy loaded by every dispatcher tick.
- [`profile.yml`](profile.yml) is the Sniffy-specific declarative configuration.
- the GitHub status workflow and local snapshot helper perform deterministic queue filtering.
- detailed documents are loaded after candidate selection, only by the lifecycle worker that needs them.
- [`instruction-audit.md`](instruction-audit.md) records entry points, token-cost findings, model choices, and migration to generated
  dispatch projections.

The status artifact is a read-only materialized selection view. It never replaces live GitHub state or the guarded
`delivery-control/v1` mutation protocol.

## Core model

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification (when required) -> Approval -> Done
Execution: Ready | In progress | Blocked
```

`Implementer` and `Verifier` are planned future roles. `Executor` is the current product/runtime route. `Assignee` is the concrete
GitHub identity or human. `Worker reference` records the active claim, generation, worker/task/process, branch/PR/head,
`claimedAt`, and `leaseUntil`.

Issues and PRs are both first-class, but one item owns lifecycle state:

```text
one formal closing issue -> issue canonical; PR is implementation evidence
no formal closing issue  -> PR canonical
two or more issues       -> PR canonical in Planning
```

PR authorship is provenance, not an automatic Implementer. Draft PRs do not enter Review. A Review handoff requires an open,
ready-for-review/non-draft PR at the exact published head. Merge remains Dmitry's explicit decision.

## Control and execution planes

```text
programmatic snapshot/queue projection
            |
            v
compact dispatcher -> one selected canonical candidate
            |
            v
guarded delivery-control/v1 claim or reconciliation
            |
            v
selected lifecycle worker -> exact evidence -> guarded handoff
            |
            v
Dmitry accepts privileged actions and merge
```

All executors use the same rotating technical control issue. Autonomous commands use the human-readable Markdown wrapper, exact
markers, lowercase `json` fence, and authoritative marked JSON specified in [`control-plane.md`](control-plane.md). Target
issues/PRs contain human-useful plans, reviews, proof, blockers, and handoffs—not field commands, polling, or lease noise.

## Current adapters

### ChatGPT

Create recurring dispatcher tasks in **Chat**, not **Work**. Use one hourly budget task or four compact hourly tasks at `:00`,
`:15`, `:30`, and `:45` when 15-minute queue latency is worth the usage. The exact prompt is
[`.chatgpt/scheduled-task-prompt.md`](../../.chatgpt/scheduled-task-prompt.md).

Each tick reads the compact runtime contract and one fresh generated status artifact, selects at most one
`dispatch.orderedCandidates` entry, and live-reads only that target. Codex Cloud is supervised because it does not poll Project 2.

### Local Codex

The persistent dispatcher uses `gpt-5.6-luna` / low. Normal implementation/verification workers use `gpt-5.6-terra` / medium.
`gpt-5.6-sol` / high is an explicit difficult-task escalation; xhigh requires an exceptional recorded reason. The app prompt and
worker template live under `.codex/local/`; the headless helper defaults to Terra/medium.

Local Codex is preferred for complex/persistent work and exact same-repository existing-PR continuation. It never adopts a fork or
Dependabot branch for direct correction.

### Status and control workflows

- `.github/workflows/delivery-status.yml` publishes Project, issue, PR, formal-link, mergeability, and deterministic dispatch views.
- `.github/workflows/delivery-control.yml` validates and serializes guarded Project transitions.
- raw exports are diagnostic; normal dispatchers consume the normalized `dispatch` view.

## Lease-based supervision

A worker owns completion signalling. When its lifecycle turn finishes, it publishes evidence and performs the guarded handoff.
Normal scheduler ticks ignore `In progress` ownership while `leaseUntil` is valid; they do not poll the worker.

The worker may renew the same claim/generation before expiry. A stale-recovery candidate exists only when the worker reference is
missing, the lease is missing/invalid, or the lease has expired. Targeted recovery preserves the exact generation/branch/task,
finishes a lost handoff when possible, and releases to Ready only when no active or recoverable work remains. No duplicate worker or
separate monitoring scheduler is created.

CI waits, contributor updates, bot commands, rebases, and draft publication use bounded operation leases under the same rule.

## Telemetry

Every non-interactive tick/worker records start/end, duration, adapter, model/reasoning, snapshot identity, selected candidate,
outcome, and provider token counters when available. Missing counters are `null` with `usageSource: unavailable`; exact billing
usage is never fabricated.

## Documentation map

- [`runtime-contract.md`](runtime-contract.md) — compact recurring contract.
- [`instruction-audit.md`](instruction-audit.md) — entry-point/token/model/profile audit and baseline tag command.
- [`profile.yml`](profile.yml) — repository/Project, identities, routes, model profiles, leases, telemetry, and snapshot settings.
- [`lifecycle.md`](lifecycle.md) — statuses, execution states, canonical item, routes, and corrections.
- [`control-plane.md`](control-plane.md) — guarded mutation schema, readable wrapper, concurrency, reactions, and rotation.
- [`status-snapshot.md`](status-snapshot.md) — materialization, freshness, dispatch projections, and permissions.
- [`event-loop.md`](event-loop.md) — clock, generated selection, claims, leases, and stale recovery.
- [`pull-request-intake.md`](pull-request-intake.md) — universal PR discovery/canonicalization and fork/bot boundaries.
- [`routing.md`](routing.md) — executor/model/provenance separation, continuity, and convergence.
- [`supervision.md`](supervision.md) — dispatch proof, lease recovery, review convergence, and merge boundary.
- [`verification.md`](verification.md) — implementer, reviewer, verifier, CI, runtime/browser/system proof.
- executor runbooks under [`executors/`](executors/) — environment-specific adapters.

## Authority

Use, in order: Dmitry's explicit current decision; canonical issue/PR and superseding comments; exact implementation PR/head and
evidence; nearest `AGENTS.md`; root `AGENTS.md`; runtime contract/profile; detailed shared policy; executor runbook; concrete prompt.
Historical chats and Actions logs are evidence, not durable delivery state.
