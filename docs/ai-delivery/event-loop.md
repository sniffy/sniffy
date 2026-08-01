# Reusable event loop and claim protocol

This design separates a provider-neutral queue engine from ChatGPT, Codex, GitHub Project, and repository-specific adapters.
Sniffy is the first profile, not the framework itself. Current Sniffy configuration lives in [`profile.yml`](profile.yml).

## Components

```text
Clock
  -> Dispatcher tick
      -> Intake and canonicalization adapters
          -> Guarded claim/transition protocol
              -> Lifecycle worker
```

- **Clock** starts one dispatcher tick. It knows only cadence and slot identity.
- **Dispatcher tick** loads one or more project profiles, reconciles source systems, queries eligible work, and selects a
  deterministic candidate.
- **Intake adapters** materialize arbitrary pull requests and other external work into the same lifecycle.
- **Canonicalization** chooses one active Project item when an issue and PR describe the same delivery.
- **Control protocol** serializes guarded ProjectV2 claims and transitions through one technical control issue.
- **Lifecycle worker** performs one lifecycle turn, publishes evidence, and hands the task to the next status as `Ready`.

Some adapters split dispatch and work into separate processes or conversations. Others, including the current ChatGPT adapter,
execute the claimed lifecycle turn directly in the same scheduled occurrence. Child-worker spawning is optional, not a
requirement of the generic protocol.

A no-op tick creates no GitHub mutation, source branch, worktree, control command, or worker claim. A provider may still append a
compact execution transcript to its own scheduler conversation.

## Desired cadence

The logical queue should be checked every 15 minutes. Worker follow-up is part of the same event-loop responsibility; it is not a
second independent scheduler. When an existing worker or observable PR operation is due for observation, a tick continues or
recovers that ownership before claiming unrelated work.

### ChatGPT Scheduled Tasks adapter

Current documented ChatGPT limits require sharding the logical clock:

- Scheduled Tasks cannot run more than once per hour;
- Pro and Enterprise accounts may have up to 15 active tasks;
- deleting a task's associated chat pauses the task;
- a task created in a ChatGPT Project that has files cannot access those files.

Use four permanent hourly Scheduled Tasks:

```text
slot-00 -> hourly at :00
slot-15 -> hourly at :15
slot-30 -> hourly at :30
slot-45 -> hourly at :45
```

Copy the exact prompt from [`.chatgpt/scheduled-task-prompt.md`](../../.chatgpt/scheduled-task-prompt.md). Create each task from a
separate defining chat inside one dedicated fileless `AI Delivery Event Loop` Project so four task logs remain isolated from one
another.

Product testing on 2026-07-30 showed that recurrences append to the defining task chat rather than reliably starting a new chat.
Treat each occurrence as logically stateless even though the transcript is persistent:

1. ignore prior-run conclusions and mutable conversational context;
2. read current GitHub state, repository policy, and [`profile.yml`](profile.yml) from scratch;
3. reconcile every open PR targeting the configured base branch and choose one canonical issue/PR item;
4. reconcile a canonical issue/PR whose current head no longer matches the exact head supporting its downstream lifecycle state;
5. continue one due owned worker/PR operation or select at most one eligible lifecycle turn;
6. submit one guarded control command to claim it;
7. verify the successful reaction and resulting Project state before doing work;
8. perform the lifecycle turn directly or make one supported external dispatch;
9. publish human-useful evidence on the canonical target item;
10. submit one guarded control command for the lifecycle handoff and verify the result;
11. return `NO_CHANGE` when no intake, head reconciliation, continuation, rotation, or eligible work exists.

A tick must never claim work it cannot reasonably complete or hand off durably during that occurrence. Route long
implementation, dependency-heavy builds, persistent services, existing-PR continuation, and environment-specific verification to
Codex Cloud, Local Codex, CI, or a human as documented in [`routing.md`](routing.md).

Scheduled Task executions cannot create child Scheduled Tasks. They may dispatch a supported external worker, but `In progress`
is valid only after a concrete worker/task/branch/process/reference exists.

The four chats are operational telemetry, not durable state. Keep output compact and follow [`chat-retention.md`](chat-retention.md)
for bounded replacement of long task chats. Chat rotation must never alter GitHub lifecycle state.

### Dedicated scheduler Project

Use one dedicated fileless ChatGPT Project, such as `AI Delivery Event Loop`, as a UI namespace for the four task definitions and
their persistent transcripts. The Project is not configuration or task memory. Critical repository/profile locations must be
present in every task prompt.

The available scheduler operation cannot reliably target an arbitrary different ChatGPT Project, and a Scheduled Task cannot
create another Scheduled Task. All durable context therefore lives in GitHub and repository-owned files.

The four tasks may scan several repositories because queue state is external. Repository-specific filters and executor settings
belong in explicit profiles, not in prior chat history.

## Universal pull-request intake adapter

Every open PR targeting the configured base branch is a discovery source, regardless of author, label, bot/provider, branch
creator, or whether an issue was created first. The adapter scans drafts as telemetry but only starts Review for non-draft PRs.

Before ordinary work, a tick re-reads for each PR:

- repository, base branch, draft/open state, exact head, mergeability, and same-repository/fork ownership;
- author, labels, dependency/security metadata, and provider-specific controls;
- formal closing issues and whether each issue/PR is already represented in Project 2;
- current lifecycle fields, assignees, Worker reference, review decision, unresolved threads, and exact-head CI;
- whether another canonical item or worker already owns the implementation.

Canonicalization is deterministic:

```text
exactly one formal closing issue -> issue is canonical
no formal closing issue          -> PR is canonical
multiple formal closing issues   -> PR is canonical in Planning
```

For exactly one closing issue, add or locate that issue idempotently and attach the PR URL/exact head as implementation evidence.
Do not add the PR as a second active lifecycle item. For a standalone PR, add or locate the PR itself. For a multi-issue PR, add or
locate the PR as `Planning / Ready / ChatGPT` and suppress duplicate work on linked issues until Planning resolves combined scope
and proof.

A draft PR does not enter Review. It may remain linked to an existing canonical issue in `Implementation / In progress` and be
monitored. Once it becomes ready for review, the next tick performs the canonical handoff; no separate event workflow is required.

If both an issue and its PR were accidentally materialized, the tick does not review both. It prefers the canonical item above,
makes the duplicate non-claimable through a guarded reconciliation when possible, and records the canonical link. The backfill key
is repository plus item number; existing representation or ownership prevents duplication.

A guarded intake/handoff:

1. targets the canonical issue or PR;
2. sets `Review / Ready / Executor = ChatGPT` for a reviewable implementation, or `Planning / Ready / ChatGPT` for a multi-issue or
   ambiguous PR;
3. chooses a Verifier from actual compatibility and observable-risk obligations;
4. omits `Implementer`, preserving any deliberate existing value while treating absence as valid;
5. records PR URL, branch, exact head, author, fork/same-repo status, linked issues, labels, and relevant security/dependency data;
6. assigns `bedrin-gpt` separately and re-reads both Project state and GitHub assignment.

PR authorship is provenance used for trust, formal-review independence, and provider-specific commands. It is not automatically a
planned implementation route. Dependabot is one specialization of universal intake; trusted automation is not approval.
See [`pull-request-intake.md`](pull-request-intake.md).

GitHub Projects auto-add may assist discovery, especially for issues created by agents and dependency PRs, but it cannot perform
canonicalization. Do not rely on blind PR auto-add as the only intake path.

## Exact-head reconciliation

A lifecycle result is valid only for the exact pull-request head named by its evidence. After universal PR intake and
canonicalization, but before continuing owned work or selecting ordinary queue work, a supervising tick inspects every open PR
whose canonical issue or PR state depends on completed Review, Verification, Approval, or a human handoff derived from one of
those statuses.

If the current PR head differs from the exact head supporting that state, the tick must:

1. stop relying on the stale review, verification packet, approval handoff, blocked decision, or worker reference;
2. submit one guarded `delivery-control/v1` command targeting the canonical item and expecting its current Status, Execution,
   Executor, and the PR's exact current head;
3. set `Status = Review`, `Execution = Ready`, and `Executor = ChatGPT`, clear stale worker ownership, and record the PR URL plus
   new exact head as evidence in the same command;
4. identify the review PR structurally: use target plus `expected.head` when the PR is canonical, or
   `reviewPullRequest.number/head` when a canonical issue owns the PR;
5. inspect the terminal reaction, then re-read both PR draft/head state and the canonical Project item before treating it as
   re-queued;
6. leave technical reconciliation details on the control issue rather than posting legacy field or claim comments on the PR.

This reconciliation is supervisory work and may select a canonical item even when its current Status is not otherwise eligible
for the ChatGPT queue, including `Approval` or `Blocked / Human`. It consumes at most one item in a tick and precedes due-worker
continuation or a new claim. A conflict means state changed concurrently; re-read and do not overwrite the newer route. A later
Review must inspect the complete diff and matching-head CI again; an approval or verification result for an older head is never
inherited automatically. When a canonical issue owns the PR, keep the PR URL and new exact head in its Worker reference through
Review and Verification.

## Control-issue adapter

All executors use [`control-plane.md`](control-plane.md). They find the one active control issue, post one guarded
`delivery-control/v1` command, inspect its terminal reaction, and re-read Project state. No target-item field command or claim
arbitration comment is permitted.

Control-log rotation is ordinary event-loop maintenance. A tick needing to post a command may rotate an old/full log first, then
continue. Rotation does not require an additional Scheduled Task.

## Codex app automation adapter

Codex automations can run on schedules, and some can return to the same conversation. Local automations require the computer to
be awake and the Codex app running.

Use one persistent dispatcher conversation when app-owned one-time worker creation is proven. The current Sniffy Local Codex
profile is fixed to Sol with extra-high reasoning. The dispatcher selects work and routing; it does not pretend Cloud or Local
model choice is the same axis as executor choice.

The dispatcher may receive an issue or PR canonical item. An explicitly routed same-repository existing PR is a valid adopted
continuation: reuse the exact branch and PR even when it was created by Dmitry, ChatGPT, an IDE agent, or another worker. Fork and
Dependabot branches are not adopted for direct correction.

### Headless Local Codex adapter

For unattended local execution, prefer a normal Linux service when app-owned chats are not needed:

```text
systemd timer or cron
  -> flock prevents overlapping ticks on one host
  -> dispatcher loads project profiles
  -> central guarded claim
  -> isolated git worktree
  -> codex exec with rendered lifecycle prompt
  -> commit/update exact PR, evidence, and guarded handoff
```

A five- or fifteen-minute timer is straightforward locally. Host-local `flock` complements the GitHub target-item concurrency
group; neither replaces the other.

## Eligible work

A dispatcher queries the materialized current route, not planned roles alone:

```text
Execution = Ready
Executor = <dispatcher executor type>
Assignee is empty OR Assignee belongs to this dispatcher pool
```

The canonical work item may be an issue or pull request. The dispatcher also checks lifecycle compatibility, required access,
worker-pool capacity, existing branch/PR ownership, and absence of a valid current worker. It excludes duplicate representations
and linked issues suppressed by an open canonical multi-issue PR.

When a profile defines a worker limit, the normal dispatcher derives used capacity from owned `In progress` items in the same
retained authoritative queue snapshot used for selection. Provider project/task/conversation inventory is not a normal pre-claim
capacity or duplicate-worker check: it is slower, can be stale, and cannot make `inventory -> claim` atomic. Use provider inventory
only for targeted recovery of an already claimed `In progress` generation when provisional worker evidence leaves child creation
uncertain. Per-target duplicate-generation exclusion comes from the guarded claim. A strict cross-target global pool limit across
several concurrent dispatchers requires a separately serialized semaphore; provider inventory is not that semaphore.

Eligibility must not require `Implementer` or `Verifier` to be populated when the current status does not use them. A blank
`Implementer` means unknown/not deliberately selected, while `Executor` remains the authoritative current route. Entering
Implementation requires a non-empty deliberately selected Implementer; Review, PR monitoring, Verification, Approval, and closure
do not.

Use deterministic selection such as security priority, Project priority, ready timestamp, then repository, item type, and item
number. Claim at most available capacity and never create duplicate workers or Project items.

## Guarded claim

A claim is one ordinary `delivery-control/v1` transition, not a public comment-election protocol:

1. query an eligible canonical `Ready` item;
2. re-read its Status, Execution, Executor, Assignee, exact PR head when the item is a PR, linked branch/PR, worker reference, and
   lease;
3. construct a unique token and worker/tick reference;
4. post one command guarding at least current Status, `Execution = Ready`, current Executor, canonical target type, and exact PR
   head when applicable;
5. set `Execution = In progress`, one Worker reference containing claim token, lease time, concrete owner, and relevant linked
   PR/head in that same command;
6. inspect the command reaction and re-read Project state;
7. only the verified winner performs work or confirms an external dispatch.

The transition workflow serializes commands by target work item. Concurrent contenders may both submit commands, but after the
first succeeds the second sees a guarded-state conflict and performs no mutation. A conflict is normal coordination, not a
blocker and not a reason to post on the target item.

If execution or dispatch cannot start, submit a guarded release back to `Ready`. Set `Blocked` only when Dmitry must decide or
act; then set `Executor = Human`, route the Assignee separately to `bedrin`, and record the exact requested action.

## Lease and stale recovery

A live ownership record includes:

```text
claimToken
claimedAt
leaseUntil
workerReference
lastObservableEvidence
```

Before expiring a lease, inspect the worker record, canonical item, linked issues/PR, branch, commits, review state, and CI. Lack
of a recent target comment does not prove inactivity. Recover the exact existing branch/worker where possible. Release to `Ready`
only when ownership is genuinely stale. Routine waiting for CI, contributor update, bot rebase, or draft publication remains
`In progress`; use `Blocked` only when Dmitry must intervene.

Worker monitoring follows [`supervision.md`](supervision.md): first observation after 15 minutes, a second 15 minutes later, then
hourly while incomplete. Those due observations are selected by the same dispatcher ticks; no extra monitoring scheduler is
required.

## Worker contract

One worker or direct ChatGPT tick owns one lifecycle turn. It must:

- re-read the canonical issue/PR, all linked requirements, current lifecycle fields, nearest `AGENTS.md`, exact branch/PR/head,
  review threads, and CI;
- verify its Project ownership before mutating source or GitHub;
- perform only the routed lifecycle responsibility;
- preserve and continue an explicitly adopted same-repository existing PR rather than creating a duplicate;
- publish exact human-useful evidence on the canonical target;
- use the common guarded control protocol for the next lifecycle state;
- stop without spawning a replacement worker when blocked and route the exact next action to Dmitry;
- never merge or enable auto-merge without explicit authorization.

## Generic core versus project profile

Generic documents define lifecycle, canonicalization, intake, guarded claims, leases, correction limits, publication,
verification, and merge authority. [`profile.yml`](profile.yml) supplies current Sniffy configuration, including Project fields,
identities, default routes, Local Codex Sol/extra-high settings, universal PR intake, and control-log thresholds.

Profiles may add filters, capacity, evidence requirements, or future executor settings, but must not redefine the shared meaning
of Status, Execution, publication, verification, or merge authority. Update the profile when a routing preference changes; edit
shared policy only when the semantics themselves change.
