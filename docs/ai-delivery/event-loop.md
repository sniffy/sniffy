# Reusable event loop and claim protocol

This design separates a provider-neutral queue engine from ChatGPT, Codex, GitHub Project, and repository-specific adapters.
Sniffy is the first profile, not the framework itself.

## Components

```text
Clock
  -> Dispatcher
      -> Claim protocol
          -> Worker spawner
              -> Worker
```

- **Clock** wakes a persistent dispatcher. It knows only cadence and slot identity.
- **Dispatcher** loads one or more project profiles, queries eligible work, and selects a deterministic candidate.
- **Claim protocol** provides best-effort cross-dispatcher ownership.
- **Worker spawner** creates a provider-specific standalone worker or starts a local process.
- **Worker** performs one phase, publishes evidence, and hands the task to the next phase as `Ready`.

A no-op tick stays in the same dispatcher conversation/process and creates no chat, task, branch, or worktree.

## Desired cadence

The logical queue should be checked every 15 minutes. The core design accepts a configurable cadence so a future deployment
may use five-minute ticks without changing lifecycle or claim semantics.

### ChatGPT Scheduled Tasks adapter

Current official ChatGPT limits require sharding the logical clock:

- Scheduled Tasks cannot run more than once per hour.
- Pro and Enterprise accounts may have up to 15 active tasks.
- Deleting the chat associated with a task pauses that task.
- A task created in a ChatGPT Project that has files cannot access those project files.

See the official OpenAI documentation:

- [Scheduled Tasks in ChatGPT](https://help.openai.com/en/articles/10291617-scheduled-tasks-in-chatgpt)
- [Projects in ChatGPT](https://help.openai.com/en/articles/10169521-projects-in-chatgpt)

Use four permanent hourly dispatcher slots:

```text
slot-00 -> hourly at :00
slot-15 -> hourly at :15
slot-30 -> hourly at :30
slot-45 -> hourly at :45
```

Together they form one logical 15-minute clock while consuming four active tasks. Each task is attached to one persistent
chat, remembers earlier runs, and returns `NO_CHANGE` without creating another chat.

Do not rely on ChatGPT Project files for dispatcher configuration. Store durable task context in GitHub issues, repository
files, and project profiles that the run can actually read.

### Observed ChatGPT Project scoping

A manual product test on 2026-07-28 attempted to create a Scheduled Task for the `Kerb4j` ChatGPT Project from a chat outside
that Project. The available scheduler operation exposed no `project_id` or equivalent destination parameter, so it created a
global task instead. Treat this as an observed capability of the current product surface, not as a permanent API guarantee.

Consequences:

- a dispatcher cannot select an arbitrary destination ChatGPT Project when creating a task;
- all durable repository/task context must live in GitHub issues, comments, pull requests, and repository-owned Markdown;
- ChatGPT Project descriptions, files, and implicit memory are convenience context only and must not be required by a worker;
- every scheduled prompt must be self-contained enough to locate the repository profile and authoritative issue.

Use one dedicated fileless ChatGPT Project, such as `AI Delivery Scheduler`, as an organizational folder for the four
persistent dispatcher chats. The Project groups scheduler history; it is not the source of configuration or task context.
Keep its Project instructions minimal, and repeat critical repository/profile locations in every scheduled dispatcher prompt.

Creating a child task in the **same current Project** remains a separate capability that must be smoke-tested. From a scheduled
run inside the scheduler Project, create one disposable one-time child task and verify that it appears as a separate chat in
that same Project. If the child becomes global, reuses the dispatcher chat, or cannot be created, set ChatGPT worker spawning
to `unsupported`; keep ChatGPT responsible for polling, Planning, Review, Verification from available artifacts, and
notifications, while routing standalone Implementation to Codex Cloud, Local Codex CLI/app, or a manually created ChatGPT
worker.

### Global ChatGPT dispatcher across repositories

One logical ring may scan several GitHub repositories or Projects because the queue state is external. Repository-specific
filters live in profiles loaded by the dispatcher. This does not require or imply one ChatGPT Project per repository.

Do **not** assume a Scheduled Task can create a new worker chat inside an arbitrary ChatGPT Project, select that Project, or
inherit its files. OpenAI documentation does not guarantee that composition, and the cross-Project test above failed. Treat
worker spawning as a feature-tested adapter capability:

```text
spawnWorker(task, currentSchedulerProject) -> supported | unsupported | failed
```

A possible implementation is to ask the dispatcher to create a one-time standalone task several minutes later, but this
must pass the same-Project smoke test in the installed product version. The test must prove that the child does not reuse the
dispatcher chat, receives all required repository/issue context, and leaves no claimed task without a worker. On unsupported
or failed spawn, release the claim to `Ready` or mark an exact capability blocker; never leave `In progress` with no worker
reference.

Until same-Project spawning is proven, the global ChatGPT ring is safe for polling, routing, Review, notifications, and work
that can run in the dispatcher context. Route standalone implementation to Codex Cloud, headless Local Codex, or a manually
created ChatGPT worker rather than assuming Project-aware thread creation.

At a desired five-minute cadence, twelve hourly shards would consume most of the 15-task Pro limit. Prefer a native future
sub-hour schedule or an external/local clock instead of multiplying ChatGPT tasks indefinitely.

### Codex app automation adapter

Codex automations can run on schedules, and some can return to the same conversation. Local automations require the computer
to be awake and the Codex app running. See [Codex automations](https://openai.com/academy/codex-automations/).

Use one persistent dispatcher conversation when native cadence permits; otherwise use the same staggered-slot pattern.
Nested creation of a standalone child task must be smoke-tested. App-native Remote visibility is optional, not a requirement
of the generic protocol.

### Headless Local Codex adapter

For unattended local execution, prefer a normal Linux service when app-owned chats are not needed:

```text
systemd timer or cron (for example every 5 minutes)
  -> flock prevents overlapping ticks on one host
  -> dispatcher loads project profiles
  -> GitHub claim protocol
  -> isolated git worktree
  -> codex exec with rendered worker prompt
  -> commit, push, PR, and evidence
```

The Codex CLI can read, modify, and run code locally, and `codex exec` is intended for shell workflows. See:

- [OpenAI Codex CLI – Getting Started](https://help.openai.com/en/articles/11096431)
- [Codex is now generally available](https://openai.com/index/codex-now-generally-available/)

Host policy controls filesystem, network, credentials, services, Docker, browsers, and worker capacity. The dispatcher may run
several workers only when each owns a distinct claim and isolated worktree.

## Eligible work

A dispatcher queries the materialized current route, not planned roles alone:

```text
Status = Ready
Executor = <dispatcher executor type>
Assignee is empty OR Assignee belongs to this dispatcher pool
```

It also checks phase compatibility, required access, worker-pool capacity, existing branch/PR ownership, and absence of a live
claim. `Implementer` and `Verifier` are future routing decisions; phase transitions copy the relevant value into `Executor`.

Use deterministic selection such as priority, then `readySince`, then issue number. A dispatcher should claim at most its
available capacity and must not create duplicate workers for a continuation.

## Best-effort atomic claim

GitHub issue comments and Project field updates are not one transaction. Use a claim generation and ordered intent protocol:

1. Query an eligible `Ready` item.
2. Re-read its Phase, Status, Executor, Assignee, dispatch generation, branch, PR, and existing claim immediately.
3. Post a claim-intent comment containing a unique token, generation, dispatcher slot, executor, and timestamp.
4. Re-read valid intents for the same generation. The lowest GitHub comment ID wins.
5. Only the winner writes `Status = In progress`, the concrete Assignee, claim token, lease time, and provisional worker state.
6. Re-read and verify that its token/generation still owns the task.
7. Spawn the worker.
8. Record the concrete worker reference, branch/worktree, and expected PR.
9. If spawning fails, remove/release the claim and restore `Ready`, or set `Blocked` with the exact external reason.

This is best-effort coordination, not a distributed database transaction, but it prevents ordinary duplicate polls from both
starting work. A same-host `flock` complements rather than replaces the GitHub-level protocol.

## Lease and stale recovery

A claim records:

```text
claimToken
claimedAt
leaseUntil
workerReference
lastObservableEvidence
```

Before expiring a lease, inspect the worker record, issue, branch, PR, commits, and CI. Lack of a recent comment alone does not
prove inactivity. Recover the existing branch/worker where possible. Release to `Ready` only when ownership is genuinely
stale; mark `Blocked` when a human or external capability is required.

## Worker contract

One worker owns one phase turn. It must:

- re-read the authoritative issue, current lifecycle fields, nearest `AGENTS.md`, branch/PR, and exact head;
- verify its claim before mutating GitHub or source;
- perform only the routed phase responsibility;
- publish exact evidence and update the next Phase, Status, Executor, and Assignee atomically on a best-effort basis;
- stop without spawning a replacement worker when blocked;
- never merge or enable auto-merge without explicit authorization.

## Generic core versus project profile

The generic documents define lifecycle, claim, leases, correction limits, and adapter contracts. A project profile supplies
repository-specific configuration, for example:

```yaml
schemaVersion: 1
project:
  repository: sniffy/sniffy
  baseBranch: develop
  githubProject: sniffy/2
fields:
  phase: Phase
  status: Status
  implementer: Implementer
  verifier: Verifier
  executor: Executor
executors:
  ChatGPT:
    assignee: bedrin-gpt
  Codex Cloud:
    assignee: bedrin-codex-cloud
  Local Codex:
    assignee: bedrin-codex-local
  Human:
    assignee: bedrin
branches:
  newWorkPrefix: agent/
```

Profiles may add filters, model routing, capacity, issue templates, or required evidence, but must not redefine the shared
meaning of Phase, Status, claims, publication, verification, or merge authority. Start with documented/declarative profiles;
do not build a bespoke dispatcher framework until platform composition has been smoke-tested and a concrete gap remains.
