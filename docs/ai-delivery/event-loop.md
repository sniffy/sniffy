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

Create ChatGPT heartbeats in **Chat**, not **Work**. The 15-minute clock controls queue latency; it is not a command to poll every
active worker every 15 minutes.

## Programmatic selection

The status workflow publishes active Project items, every open base-branch PR, formal closing links, exact heads, ownership,
mergeability hints, and deterministic projections:

- `readyByExecutor`;
- `inProgressByExecutor`, split into active and stale ownership;
- route/assignee mismatches;
- PR conflict/intake/duplicate candidates;
- stale exact-head candidates;
- `orderedCandidates`.

Local Codex uses `.codex/local/project-queue-snapshot.sh` once per tick for equivalent selection. A future generic framework may
compile `profile.yml` once into versioned runtime JSON. Dispatchers consume generated JSON instead of interpreting policy prose.

Snapshot selection never authorizes mutation. After selecting one candidate, the dispatcher live-reads only that target and uses
the guarded control workflow as the authoritative compare-and-set.

## Candidate order

A dispatcher handles at most one outcome per tick:

1. conflicting same-repository PR requiring deliberate routing;
2. managed fork/Dependabot operation or universal PR intake/canonicalization;
3. stale exact-head downstream state;
4. Ready route/assignee mismatch;
5. stale `In progress` ownership with missing/invalid/expired lease;
6. Ready work for a supervised executor;
7. Ready work for the dispatcher itself.

If no candidate remains actionable after targeted re-read, return `NO_CHANGE`. Do not load whole discussions, diffs, review
threads, CI logs, artifacts, or detailed runbooks merely to prove the queue is empty.

## Universal PR intake

Every open PR targeting the configured base branch is represented in the generated snapshot regardless of author, label,
bot/provider, or branch creator. Canonicalization remains:

```text
one formal closing issue -> issue canonical
no formal closing issue  -> PR canonical
two or more issues       -> PR canonical in Planning
```

Draft PRs do not enter Review. If issue and PR are both materialized, keep one canonical active lifecycle item. Same-repository
corrections preserve the exact branch/PR. Fork and Dependabot branches remain contributor/bot-owned or are superseded by a
deliberate internal task.

## Exact-head and route reconciliation

Review, Verification, Approval, or a derived human handoff is valid only for the exact PR head named in durable evidence. A changed
head is programmatically flagged and routed back to `Review / Ready / ChatGPT` after targeted live verification.

A Ready route whose Assignee belongs to another executor pool is a supervisory reconciliation obligation. Correct assignment or
route deliberately; never silently filter the item or infer a Human blocker from stale assignment alone.

## Supervised executors

The current profile marks Codex Cloud `dispatchMode: supervised` with ChatGPT as its dispatch owner. Codex Cloud does not poll
Project 2. A ChatGPT tick may select `Implementation / Ready / Codex Cloud`, claim while preserving Executor, submit one exact
Cloud trigger, require durable acknowledgement, record generation plus lease, and stop.

A trigger never submitted or definitively rejected is released to Ready. A submitted trigger with uncertain acknowledgement stays
one provisional generation under a shorter lease. Never dispatch a duplicate. Arbitrary existing-PR continuation normally routes
to Local Codex unless it is an explicitly recoverable Cloud-owned branch.

## Polling executors and capacity

A polling dispatcher selects `Execution = Ready`, its Executor pool, and an empty/pool Assignee. Capacity is derived from all owned
`In progress` items in the retained snapshot, including stale ownership until recovery resolves it. Provider-wide task inventory is
not a normal capacity or duplicate-generation check; it is allowed only for one targeted stale recovery.

## Guarded claim

A claim:

1. live-reads current canonical type, Status, Execution, Executor, assignment, worker reference, and exact PR head when applicable;
2. creates a unique token/generation, owner, concrete/provisional worker reference, `claimedAt`, and `leaseUntil`;
3. posts one `delivery-control/v1` command using the human-readable Markdown wrapper from `control-plane.md`;
4. preserves exact start/end markers, lowercase `json` fence, and authoritative marked JSON;
5. guards current type, Status, `Execution = Ready`, Executor, and exact PR head;
6. sets `Execution = In progress` and ownership in the same command;
7. starts work only for the verified winner.

A guarded conflict is normal coordination. Make no source mutation from a stale view. If execution cannot start, release to Ready;
use `Blocked / Human` only for one exact Dmitry decision/action.

## Worker contract

One worker owns one lifecycle turn. It reads the complete canonical item, relevant comments/formal links, exact PR,
nearest `AGENTS.md`, current reviews/threads/CI/artifacts, and only the detailed runbook sections it needs.

Implementation publishes one intended ready-for-review/non-draft PR at the exact head and performs the guarded Review handoff.
Review publishes one comprehensive exact-head outcome with honest identity handling. Verification proves observable behavior in a
representative environment. Approval and merge remain human-owned.

## Lease and stale recovery

A worker owns completion signalling and may renew its own lease before expiry. While `leaseUntil` is valid, scheduler ticks ignore
the item and do not inspect the worker.

Missing worker reference, missing/invalid lease, or expired `leaseUntil` creates one stale-recovery candidate. The dispatcher then:

1. re-reads the exact worker/task/generation and branch/PR/head evidence;
2. stops if handoff already completed;
3. extends the same lease only when the same worker is demonstrably active;
4. completes a lost deterministic handoff when evidence is sufficient;
5. recovers the same workspace/branch/generation when possible;
6. releases to Ready only when nothing active or recoverable remains;
7. never starts a duplicate worker, generation, branch, or PR.

External waits such as CI, contributors, bot operations, rebases, and draft publication use bounded operation leases. New GitHub
state may create another actionable candidate before expiry; otherwise recovery waits for lease expiry. There is no separate
monitoring scheduler or per-worker observation cadence.

## Telemetry and profile

Every tick emits start/end/duration, adapter/scheduler, model/reasoning, snapshot identity, selected candidate, outcome, and exact
provider token counters when exposed. Missing counters are null with `usageSource: unavailable`.

Shared Markdown defines lifecycle semantics; `profile.yml` supplies repository/Project, identities, capacity, scheduler surface,
model defaults, lease budgets, and routes. Generated dispatch JSON is the runtime form. A profile must not weaken exact-head,
review-independence, verification, or no-merge rules.
