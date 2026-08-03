# Compact AI delivery runtime contract

This is the only shared policy document that a dispatcher reads on every tick. Detailed documents in this directory are normative
reference material for humans, tests, and a selected lifecycle worker; they are not a mandatory dispatcher preamble.

## Sources of truth

Use, in order: Dmitry's explicit current decision; the canonical GitHub issue or pull request and later comments; the exact linked
PR head, reviews, CI, and artifacts; the nearest `AGENTS.md`; root `AGENTS.md`; this contract and `profile.yml`; the selected
executor runbook; the concrete task prompt. Historical chats and snapshots are telemetry, not authority.

The repository-owned status snapshot is a materialized selection view. It never authorizes a mutation. Every Project claim and
handoff uses the guarded `delivery-control/v1` protocol and a live target re-read.

## Dispatcher tick

A tick is selection and coordination, not lifecycle work. It must:

1. record `startedAt`, model/profile, and scheduler identity;
2. obtain exactly one current normalized snapshot using the adapter's documented read path;
3. use the snapshot's `dispatch` projections instead of asking the model to rescan or re-sort the whole Project and every PR;
4. choose at most one candidate in this order:
   - conflicting same-repository PR requiring deliberate continuation routing;
   - fork or Dependabot conflict requiring contributor feedback or a documented bot operation, never branch adoption;
   - PR intake/canonicalization or duplicate representation;
   - exact-head drift or invalid current route/assignee;
   - due owned `In progress` observation;
   - `Ready` work for an executor this supervisor dispatches;
   - `Ready` work for this dispatcher;
5. only after selection, perform the minimum live reads needed for that candidate: current target, formal closing links, exact
   branch/head/draft/ownership, current Project route, worker reference, and the newest active control issue;
6. claim or reconcile through one guarded control command, verify the terminal reaction, and then either perform one bounded
   lifecycle turn or create one durably acknowledged external worker;
7. publish human-useful evidence and one guarded handoff when work finishes;
8. emit compact telemetry and stop.

Live-read only the selected candidate. Do not read complete issue discussions, full diffs, review threads, CI logs, proof matrices,
or executor runbooks before a candidate is selected. The selected worker owns those reads. Do not re-read the full snapshot during
selection.

When no candidate needs action, perform no Project, source, worker, target-comment, or control-command mutation and return
`NO_CHANGE` plus telemetry.

## Canonical PR invariants

Scan every open pull request in the snapshot targeting the configured base branch, regardless of author, label, bot/provider, or
branch creator, but live-read only a programmatically selected attention candidate.

- Exactly one formal closing issue: that issue is the canonical lifecycle item.
- No formal closing issue: the non-draft PR itself is the canonical lifecycle item.
- Multiple formal closing issues: the non-draft PR is the canonical coordination item in Planning.
- Draft PR: do not start Review.
- Never review both an issue and its implementation PR as separate lifecycle work.
- Reuse an explicitly routed same-repository branch/PR. Never adopt a fork or Dependabot branch for direct correction.

A transition to Review requires an open PR targeting the configured base branch at the exact published head and non-draft/ready
for review. For a canonical issue, the command includes `reviewPullRequest.number` and `reviewPullRequest.head`; for a canonical PR,
the target and `expected.head` identify it.

## Control and ownership

Every new autonomous control comment uses the human-readable Markdown wrapper from `control-plane.md`, its exact start/end markers,
and a lowercase `json` fence. The marked JSON is authoritative; never emit bare JSON as a new autonomous command.

A claim guards current type, Status, `Execution = Ready`, Executor, and exact PR head when applicable, then sets `Execution = In
progress` with a unique token, owner, lease, concrete worker reference, and next observation point. A conflict means another actor
won or state changed; make no work mutation from the stale view.

A configured unclaimable `Execution = Ready` route whose Assignee belongs to a different executor pool is a reconciliation
candidate. Never silently filter out that mismatch. `Blocked / Human` is reserved for an exact Dmitry decision or action; routine
waiting remains `In progress`.

## Supervision cadence

After a worker, CI run, contributor operation, bot command, or draft publication is started, observe it after 15 minutes, again 15
minutes later, then hourly while incomplete. Store the next observation time in durable worker evidence. Do not create a separate
per-item hourly monitor before the two 15-minute observations.

For a supervised Codex Cloud dispatch: `Status = Implementation`, `Execution = Ready`, `Executor = Codex Cloud`. Codex Cloud does
not poll Project 2. Claim while preserving Executor, post one exact implementation trigger, require durable acknowledgement, and
record the first 15-minute observation point. If the trigger was not submitted or was definitively rejected, release to Ready. If
submission succeeded but acknowledgement is uncertain, preserve that provisional generation and never redispatch it.

## Lifecycle worker loading

After selection and claim, the worker reads:

- the complete canonical item and relevant comments;
- formally linked issues and the exact PR;
- nearest applicable `AGENTS.md` files;
- current reviews, unresolved threads, matching-head CI, and required artifacts;
- only the detailed lifecycle/runbook sections relevant to its current status and executor.

Implementation produces the smallest coherent change and implementer-owned proof. Review inspects the complete exact-head diff and
publishes one comprehensive outcome. Verification proves observable behavior in a representative environment. Approval and merge
remain human-owned unless Dmitry explicitly says otherwise.

When the reviewer is independent and blockers remain, submit comprehensive REQUEST_CHANGES. When the reviewer is the PR author,
publish the same findings as an ordinary PR comment with the identity limitation. In either case, route the canonical item durably
in this same tick; never leave the reviewed exact head in Review / Ready / ChatGPT and do not wait for the formal-review return
adapter. A technically acceptable self-authored PR proceeds to Verification or Approval / Ready / Human.

Never merge, enable auto-merge, bypass protection, rewrite shared history, expose credentials, or perform privileged operations
without Dmitry's explicit instruction.

## Telemetry

Every non-interactive tick and worker emits one final JSON object or one compact line containing:

- `startedAt`, `finishedAt`, and `durationSeconds`;
- adapter, scheduler/task identity, model, and reasoning effort when known;
- snapshot generation/run ID;
- selected candidate kind and canonical target, or `none`;
- outcome: `NO_CHANGE`, `CLAIMED`, `DISPATCHED`, `HANDOFF`, `BLOCKED`, `RATE_LIMITED`, `CONFLICT`, or `FAILED`;
- exact provider token counters when the runtime exposes them.

Use `null` and `usageSource: "unavailable"` when token counters are not exposed. Never invent an exact token count. A separately
labelled estimate may be emitted only from measured prompt/context bytes using a documented formula; it must never be presented as
provider billing data.
