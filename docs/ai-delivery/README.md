# AI-assisted delivery in Sniffy

This directory defines the provider-neutral lifecycle, control plane, routing, supervision, review, and verification model used by
ChatGPT, Codex Cloud, Local Codex, IDE agents, automation, contributors, and humans. Repository engineering rules remain in the
nearest applicable `AGENTS.md`.

## Runtime versus reference policy

Dispatchers must not load this whole directory on every heartbeat.

- [`runtime-contract.md`](runtime-contract.md) is the compact policy loaded by every dispatcher tick.
- [`profile.yml`](profile.yml) is the Sniffy-specific declarative configuration.
- the GitHub status workflow and local snapshot helper perform deterministic queue filtering.
- detailed documents are loaded after candidate selection, and only by the lifecycle worker that needs them.
- [`instruction-audit.md`](instruction-audit.md) records the effective entry points, token-cost findings, model choices, and
  migration from prompt-driven selection to generated dispatch projections.

The status artifact is a read-only materialized selection view. It never replaces live GitHub state or the guarded
`delivery-control/v1` mutation protocol.

## Core model

`Status` describes the kind of work; `Execution` describes whether its current action can run:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification (when required) -> Approval -> Done
Execution: Ready | In progress | Blocked
```

`Implementer` and `Verifier` are planned future roles. `Executor` is the current product/runtime route. `Assignee` is the concrete
GitHub identity or human. `Worker reference` records the active task/process/branch/PR/head/lease/next observation.

Issues and PRs are both first-class, but one item owns lifecycle state for one published change:

```text
one formal closing issue -> issue canonical; PR is implementation evidence
no formal closing issue  -> PR canonical
several closing issues   -> PR canonical in Planning
```

PR authorship is provenance, not an automatic Implementer choice. Draft PRs do not enter Review. A Review handoff requires the
open, non-draft PR at the exact published head. Merge remains Dmitry's explicit decision.

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

All executors use the same rotating technical control issue. New autonomous commands use the human-readable wrapper and marked
JSON specified in [`control-plane.md`](control-plane.md). Target issues/PRs contain human-useful plans, reviews, proof, blockers, and
handoffs—not field commands, leases, polling noise, or claim elections.

## Current adapters

### ChatGPT

Create recurring dispatcher tasks in **Chat**, not **Work**. Use either one hourly budget task or four compact hourly tasks at
`:00`, `:15`, `:30`, and `:45` when 15-minute queue latency is worth the usage. The exact setup prompt is
[`.chatgpt/scheduled-task-prompt.md`](../../.chatgpt/scheduled-task-prompt.md).

Each tick reads the compact runtime contract and one fresh generated status artifact. It chooses at most one
`dispatch.orderedCandidates` entry and live-reads only that target. Codex Cloud is `dispatchMode: supervised`; ChatGPT claims and
starts one routed Cloud generation because Cloud does not poll Project 2.

### Local Codex

The app dispatcher uses `gpt-5.6-luna` with low reasoning. Normal implementation/verification workers use `gpt-5.6-terra` with
medium reasoning. `gpt-5.6-sol` with high reasoning is an explicit difficult-task escalation rather than the heartbeat/default.
The app prompt is [`.codex/local/scheduled-task-prompt.md`](../../.codex/local/scheduled-task-prompt.md); the worker template is
[`.codex/local/worker-task-prompt.md`](../../.codex/local/worker-task-prompt.md). Headless workers use
[`.codex/local/run-issue.sh`](../../.codex/local/run-issue.sh) and may override model/reasoning explicitly.

Local Codex is preferred for complex/persistent work and exact same-repository existing-PR continuation. It never adopts a fork or
Dependabot branch for direct correction.

### Status and control workflows

- `.github/workflows/delivery-status.yml` publishes Project, issue, PR, formal-closing-link, mergeability, and deterministic dispatch
  projections.
- `.github/workflows/delivery-control.yml` validates and serializes guarded Project transitions.
- raw status exports are diagnostic; normal dispatchers consume the normalized `dispatch` view.

## Supervision and telemetry

A newly started worker or operation is observed after 15 minutes, again after 15 minutes, then hourly while incomplete. The next
observation belongs in durable worker evidence. Routine waiting remains `In progress`; `Blocked` means Dmitry must decide or act.

Every non-interactive tick/worker records start/end, duration, adapter, model/reasoning, snapshot identity, selected candidate,
outcome, and provider token counters when available. Missing counters are `null` with `usageSource: unavailable`; exact billing
usage is never fabricated.

## Documentation map

- [`runtime-contract.md`](runtime-contract.md) — compact recurring dispatcher/worker contract.
- [`instruction-audit.md`](instruction-audit.md) — entry-point analysis, token budget, scheduler/model setup, and baseline tag.
- [`profile.yml`](profile.yml) — Sniffy repository/Project, identities, routes, model profiles, supervision, telemetry, and snapshot
  configuration.
- [`lifecycle.md`](lifecycle.md) — statuses, execution states, canonical item, planned/current routing, and corrections.
- [`control-plane.md`](control-plane.md) — guarded mutation schema, readable command wrapper, concurrency, reactions, and rotation.
- [`status-snapshot.md`](status-snapshot.md) — read-only materialization, freshness, artifact pointer, and permissions.
- [`event-loop.md`](event-loop.md) — reusable clock, generated selection, claims, adapters, and leases.
- [`pull-request-intake.md`](pull-request-intake.md) — universal PR discovery/canonicalization and fork/bot boundaries.
- [`routing.md`](routing.md) — executor/model/provenance separation, capability/risk routing, existing PR continuity, convergence.
- [`supervision.md`](supervision.md) — dispatch proof, 15/15/hourly observation, correction convergence, and merge boundary.
- [`verification.md`](verification.md) — implementer, reviewer, verifier, CI, runtime/browser/system proof.
- [`executors/chatgpt.md`](executors/chatgpt.md), [`executors/codex-cloud.md`](executors/codex-cloud.md),
  [`executors/codex-local.md`](executors/codex-local.md), [`executors/ide-agent.md`](executors/ide-agent.md) — environment adapters.
- [`chat-retention.md`](chat-retention.md) — bounded replacement of persistent scheduler chats.
- [`../retrospectives/`](../retrospectives/README.md) — historical incidents and durable lessons, never recurring prompt input.

## Authority

Use, in order: Dmitry's explicit current decision; canonical issue/PR and superseding comments; exact implementation PR/head and
its evidence; nearest `AGENTS.md`; root `AGENTS.md`; runtime contract/profile; detailed shared policy; executor runbook; concrete
prompt. Historical chats and Actions logs are evidence, not durable delivery state.
