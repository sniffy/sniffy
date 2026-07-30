# Reusable event loop and claim protocol

This design separates a provider-neutral queue engine from ChatGPT, Codex, GitHub Project, and repository-specific adapters.
Sniffy is the first profile, not the framework itself. Current Sniffy configuration lives in [`profile.yml`](profile.yml).

## Components

```text
Clock
  -> Dispatcher tick
      -> Intake adapters
          -> Guarded claim/transition protocol
              -> Lifecycle worker
```

- **Clock** starts one dispatcher tick. It knows only cadence and slot identity.
- **Dispatcher tick** loads one or more project profiles, queries eligible work, and selects a deterministic candidate.
- **Intake adapters** materialize external work such as Dependabot pull requests into the same lifecycle.
- **Control protocol** serializes guarded ProjectV2 claims and transitions through one technical control issue.
- **Lifecycle worker** performs one lifecycle turn, publishes evidence, and hands the task to the next status as `Ready`.

Some adapters split dispatch and work into separate processes or conversations. Others, including the current ChatGPT adapter,
execute the claimed lifecycle turn directly in the same scheduled occurrence. Child-worker spawning is optional, not a
requirement of the generic protocol.

A no-op tick creates no GitHub mutation, source branch, worktree, control command, or worker claim. A provider may still append a
compact execution transcript to its own scheduler conversation.

## Desired cadence

The logical queue should be checked every 15 minutes. Worker follow-up is part of the same event-loop responsibility; it is not a
second independent scheduler. When an existing worker is due for observation, a tick continues or recovers that ownership before
claiming unrelated work.

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

Product testing on 2026-07-30 showed that recurrences append to the defining task chat rather than reliably creating a new chat.
Treat each occurrence as logically stateless even though the transcript is persistent:

1. ignore prior-run conclusions and mutable conversational context;
2. read current GitHub state, repository policy, and [`profile.yml`](profile.yml) from scratch;
3. reconcile eligible external PR intake;
4. continue one due owned worker or select at most one eligible lifecycle turn;
5. submit one guarded control command to claim it;
6. verify the successful reaction and resulting Project state before doing work;
7. perform the lifecycle turn directly or make one supported external dispatch;
8. publish human-useful evidence on the target item;
9. submit one guarded control command for the lifecycle handoff and verify the result;
10. return `NO_CHANGE` when no intake, continuation, rotation, or eligible work exists.

A tick must never claim work it cannot reasonably complete or hand off durably during that occurrence. Route long
implementation, dependency-heavy builds, persistent services, and environment-specific verification to Codex Cloud, Local
Codex, CI, or a human.

Scheduled Task executions cannot create child Scheduled Tasks. They may dispatch a supported external worker, but `In progress`
is valid only after a concrete worker/task/branch/process reference exists.

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

### Pull-request intake adapters

A repository profile may define PR-first sources. Prefer GitHub Projects' built-in auto-add workflow for newly created or updated
PRs. For Sniffy, select repository `sniffy/sniffy` and filter `is:pr is:open label:dependencies`. The auto-add filter cannot
identify the author and does not backfill existing matching PRs, so ticks still reconcile the source queue.

Before claiming ordinary work, a tick:

1. scans for open matching PRs not already represented in the Project;
2. verifies the actual author, labels, target, draft state, and exact head;
3. submits a guarded control command with `addIfMissing: true`;
4. initializes `Review / Ready / Executor = ChatGPT` plus the planned Implementer/Verifier fields;
5. re-reads the Project item before treating intake as complete.

For Dependabot, use the bot author plus dependency label, distinguish security from routine updates, and set the PR itself as the
work item. Do not create a shadow issue unless review discovers independently owned implementation work. Intake is not approval.

### Control-issue adapter

All executors use [`control-plane.md`](control-plane.md). They find the one active control issue, post one guarded
`delivery-control/v1` command, inspect its terminal reaction, and re-read Project state. No target-item field command or claim
arbitration comment is permitted.

Control-log rotation is ordinary event-loop maintenance. A tick needing to post a command may rotate an old/full log first, then
continue. Rotation does not require an additional Scheduled Task.

### Codex app automation adapter

Codex automations can run on schedules, and some can return to the same conversation. Local automations require the computer to
be awake and the Codex app running.

Use one persistent dispatcher conversation when app-owned one-time worker creation is proven. The current Sniffy Local Codex
profile is fixed to Sol with extra-high reasoning. The dispatcher selects work and routing; it does not pretend Cloud or Local
model choice is the same axis as executor choice.

### Headless Local Codex adapter

For unattended local execution, prefer a normal Linux service when app-owned chats are not needed:

```text
systemd timer or cron
  -> flock prevents overlapping ticks on one host
  -> dispatcher loads project profiles
  -> central guarded claim
  -> isolated git worktree
  -> codex exec with rendered worker prompt
  -> commit, push, PR, evidence, and guarded handoff
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

The work item may be an issue or pull request. The dispatcher also checks lifecycle compatibility, required access, worker-pool
capacity, existing branch/PR ownership, and absence of a valid current worker. `Implementer` and `Verifier` are future routing
decisions; lifecycle transitions copy the relevant value into `Executor`.

Use deterministic selection such as security priority, Project priority, ready timestamp, then repository/item number. Claim at
most available capacity and never create duplicate workers or Project items.

## Guarded claim

A claim is one ordinary `delivery-control/v1` transition, not a public comment-election protocol:

1. query an eligible `Ready` item;
2. re-read its Status, Execution, Executor, Assignee, exact PR head, branch/PR, worker reference, and lease;
3. construct a unique token and worker/tick reference;
4. post one command guarding at least the current Status, `Execution = Ready`, current Executor, and exact PR head when applicable;
5. set `Execution = In progress`, one Worker reference containing claim token, lease time, concrete owner, and any configured routing fields in that same command;
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

Before expiring a lease, inspect the worker record, issue, branch, PR, commits, review state, and CI. Lack of a recent target
comment does not prove inactivity. Recover the existing branch/worker where possible. Release to `Ready` only when ownership is
genuinely stale. Routine waiting remains `In progress`; use `Blocked` only when Dmitry must intervene.

Worker monitoring follows [`supervision.md`](supervision.md): first observation after 15 minutes, a second 15 minutes later, then
hourly while incomplete. Those due observations are selected by the same dispatcher ticks; no extra monitoring scheduler is
required.

## Worker contract

One worker or direct ChatGPT tick owns one lifecycle turn. It must:

- re-read the authoritative issue/PR, current lifecycle fields, nearest `AGENTS.md`, branch/PR, and exact head;
- verify its Project ownership before mutating source or GitHub;
- perform only the routed lifecycle responsibility;
- publish exact human-useful evidence;
- use the common guarded control protocol for the next lifecycle state;
- stop without spawning a replacement worker when blocked and route the exact next action to Dmitry;
- never merge or enable auto-merge without explicit authorization.

## Generic core versus project profile

Generic documents define lifecycle, intake, guarded claims, leases, correction limits, publication, verification, and merge
authority. [`profile.yml`](profile.yml) supplies current Sniffy configuration, including Project fields, identities, default
routes, Local Codex Sol/extra-high settings, and control-log thresholds.

Profiles may add filters, capacity, evidence requirements, or future executor settings, but must not redefine the shared meaning
of Status, Execution, publication, verification, or merge authority. Update the profile when a routing preference changes; edit
shared policy only when the semantics themselves change.
