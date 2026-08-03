# Reusable event loop and claim protocol

The event loop separates a provider-neutral queue engine from clocks, Project adapters, ChatGPT, Codex, and project-specific
profiles. Sniffy is the first profile, not the framework itself.

```text
clock
  -> programmatic status/queue projection
      -> compact dispatcher selects one attention candidate
          -> guarded claim/reconciliation
              -> one lifecycle worker
                  -> exact evidence and guarded handoff
```

## Clock

A clock knows cadence and scheduler identity only. It must not embed lifecycle policy.

- ChatGPT budget clock: one hourly Chat Scheduled Task.
- ChatGPT full-latency clock: four hourly Chat tasks at `:00`, `:15`, `:30`, and `:45`.
- Local/headless clock: cron or systemd timer with host-local overlap protection.

Create ChatGPT heartbeats in **Chat**, not **Work**. Work and strong coding models are reserved for selected work rather than empty
queue checks.

## Programmatic selection

The GitHub status workflow publishes a normalized artifact with:

- active Project items and every explicit field value;
- every open base-branch PR, formal closing issues, exact head, draft/ownership, and mergeability hints;
- `dispatch.readyByExecutor` and `dispatch.inProgressByExecutor`;
- due observations, route mismatches, PR intake/conflict candidates, stale exact-head candidates, and one ordered attention list.

Local Codex uses `.codex/local/project-queue-snapshot.sh` once per tick for equivalent ready/in-progress selection. A future generic
framework may compile `profile.yml` into versioned runtime JSON; dispatchers should consume generated JSON, not parse policy prose.

Snapshot selection never authorizes mutation. After choosing one candidate, the dispatcher live-reads only that target and uses the
guarded control workflow as the authoritative compare-and-set.

## Candidate order

A dispatcher handles at most one outcome per tick:

1. conflicting same-repository PR requiring deliberate routing;
2. universal PR intake/canonicalization or duplicate representation;
3. stale exact-head downstream state;
4. Ready route/assignee mismatch;
5. due owned `In progress` observation;
6. Ready work for a supervised executor;
7. Ready work for the dispatcher itself.

If no candidate remains actionable after the targeted re-read, return `NO_CHANGE`. Do not read whole discussions, diffs, review
threads, CI logs, artifacts, or detailed runbooks merely to prove the queue is empty.

## Universal pull-request intake

Every open PR targeting the configured base branch is represented in the generated snapshot regardless of author, label,
bot/provider, branch creator, or whether an issue existed first. The programmatic projection identifies candidates; the dispatcher
live-reads only the selected one.

Canonicalization remains:

```text
one formal closing issue -> issue canonical
no formal closing issue  -> PR canonical
two or more closing issues -> PR canonical in Planning
```

Draft PRs are telemetry and possible implementation evidence but do not enter Review. If both an issue and PR are materialized,
keep one canonical active lifecycle item and make the duplicate non-claimable. Same-repository corrections preserve the exact
branch/PR. Fork and Dependabot branches remain contributor/bot-owned or are superseded by a deliberate internal task.

## Exact-head and route reconciliation

Review, Verification, Approval, or a derived human handoff is valid only for the exact PR head named in durable evidence. When the
current head differs, the generated projection flags it. A supervising tick live-verifies the new head, then uses one guarded
transition to `Review / Ready / ChatGPT` with the new exact PR identity and cleared stale ownership.

A configured Ready route whose Assignee belongs to another executor pool is a supervisory reconciliation obligation. Re-read the
route and either correct assignment or deliberately route the lifecycle status/executor. Never silently filter out an otherwise
Ready item and never infer a human blocker from stale assignment alone.

## Supervised executors

The current profile marks Codex Cloud `dispatchMode: supervised`, with ChatGPT as its dispatch owner. Cloud does not poll Project
2. A ChatGPT tick may therefore select `Implementation / Ready / Codex Cloud`, claim while preserving Executor, submit exactly one
Cloud trigger, require durable acknowledgement, record the generation and first observation, then stop.

A trigger never submitted or definitively rejected is released to Ready. A submitted trigger with uncertain acknowledgement stays
one provisional `In progress` generation for targeted recovery; never dispatch a duplicate. Arbitrary existing-PR continuation is
routed to Local Codex unless it is an explicitly recoverable Cloud branch.

## Polling executors

A polling dispatcher selects:

```text
Execution = Ready
Executor = its executor pool
Assignee = empty or the pool identity
```

Eligibility must not require optional Implementer/Verifier values outside the lifecycle status that uses them. A blank Implementer
means no implementation route has yet been deliberately selected.

Capacity is derived from owned `In progress` items in the retained snapshot and the profile's configured maximum. Provider-wide
conversation/task inventory is not a normal capacity or duplicate-generation check; use it only for targeted recovery of one
already-claimed ambiguous generation. Per-target guarded claims prevent duplicate ownership.

## Guarded claim

A claim:

1. live-reads current canonical type, Status, Execution, Executor, assignment, worker reference, and exact PR head when applicable;
2. creates a unique token, lease, owner, concrete/provisional worker reference, and next observation time;
3. posts one `delivery-control/v1` command on the active control issue using the readable wrapper from `control-plane.md`;
4. guards current type, Status, `Execution = Ready`, Executor, and exact PR head;
5. sets `Execution = In progress` and Worker reference in the same command;
6. inspects the terminal reaction and re-reads current state;
7. starts work only for the verified winner.

A guarded conflict is normal coordination. Make no source or work mutation from the stale view. If execution cannot start, release
to Ready. Set `Blocked / Human` only for one exact Dmitry decision/action.

## Worker contract

One worker owns one lifecycle turn. After claim it reads the complete canonical item and relevant comments, formal links, exact PR,
nearest `AGENTS.md`, current reviews/threads/CI/artifacts, and only the detailed lifecycle/runbook sections it needs.

- Planning resolves outcome, canonical scope, decisions, non-goals, risk, routes, and proof.
- Implementation changes the repository, proves the change, publishes one intended non-draft exact-head PR, and hands to Review.
- Review inspects the complete exact-head diff and publishes one comprehensive outcome with honest identity handling.
- Verification proves observable behavior in a representative environment rather than trusting unit tests or summaries.
- Approval and merge remain human-owned.

A transition to Review identifies the exact PR structurally. A canonical PR uses target plus `expected.head`; a canonical issue uses
`reviewPullRequest.number/head`. The PR must be open, target the base branch, be ready/non-draft, and match the exact head.

## Supervision and recovery

After starting a worker, CI run, contributor operation, bot command, or draft publication, observe after 15 minutes, again after 15
minutes, then hourly while incomplete. Store `nextObservationAt` in durable worker evidence. The same event-loop responsibility
selects due observations; do not leave the first check until an hour later.

Before expiring a lease, inspect the concrete worker/task/branch/PR and latest observable evidence. Routine waiting remains
`In progress`. Recover the exact existing worker/branch where possible; release only genuinely stale ownership.

## Telemetry

Every tick emits start/end/duration, adapter/scheduler, model/reasoning, snapshot identity, selected candidate, outcome, and exact
provider token counters when exposed. Missing counters are null with `usageSource: unavailable`; estimates must be explicitly
labelled and derived from measured bytes rather than presented as billing facts.

## Generic core and project profile

Shared Markdown defines lifecycle semantics, canonicalization, guarded ownership, supervision, verification, and merge authority.
`profile.yml` supplies repository/Project, identities, capacity, scheduler surface/cadence, model defaults, and routing choices.
Generated dispatch JSON is the runtime form. A profile must not redefine lifecycle meaning or weaken exact-head/no-merge rules.
