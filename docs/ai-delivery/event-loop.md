# Reusable event loop and claim protocol

This design separates a provider-neutral queue engine from ChatGPT, Codex, GitHub Project, and repository-specific adapters.
Sniffy is the first profile, not the framework itself.

## Components

```text
Clock
  -> Dispatcher tick
      -> Intake adapters
          -> Claim protocol
              -> Phase worker
```

- **Clock** starts one dispatcher tick. It knows only cadence and slot identity.
- **Dispatcher tick** loads one or more project profiles, queries eligible work, and selects a deterministic candidate.
- **Intake adapters** materialize external work such as Dependabot pull requests into the same lifecycle.
- **Claim protocol** provides best-effort cross-dispatcher ownership.
- **Phase worker** performs one phase turn, publishes evidence, and hands the task to the next phase as `Ready`.

Some adapters may split dispatch and work into separate processes or conversations. Others, including the current ChatGPT
adapter, execute the claimed phase directly in the same fresh tick. Child-worker spawning is optional, not a requirement of the
generic protocol.

A no-op tick creates no GitHub mutation, source branch, worktree, or worker claim. A provider may still create an execution
transcript such as a fresh ChatGPT chat for that tick.

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

Use four permanent hourly Scheduled Tasks:

```text
slot-00 -> hourly at :00 -> new chat
slot-15 -> hourly at :15 -> new chat
slot-30 -> hourly at :30 -> new chat
slot-45 -> hourly at :45 -> new chat
```

Together they form one logical 15-minute clock while consuming four active tasks. Configure every task to start in a **new
chat** inside one dedicated scheduler Project. Each occurrence is a fresh, stateless dispatcher/worker tick; it does not reuse
or append to a long-lived dispatcher conversation.

A manual product test on 2026-07-28 confirmed that a Scheduled Task execution cannot create another Scheduled Task. Treat
ChatGPT child-task spawning as `unsupported` in the current adapter. The scheduled tick itself must therefore:

1. read its self-contained prompt and external project profiles;
2. run idempotent intake for eligible external pull requests when configured;
3. inspect GitHub issues, pull requests, lifecycle fields, claims, and evidence;
4. select and best-effort claim at most one eligible phase turn;
5. perform that phase directly in the same fresh chat, or make a supported external handoff such as a Codex Cloud dispatch;
6. durably write the next Phase, Status, Executor, Assignee, evidence, and cleared claim;
7. return `NO_CHANGE` when there is no eligible work.

A tick must never claim work that cannot reasonably finish or reach a safe durable handoff within that scheduled execution.
Route long implementation, dependency-heavy builds, persistent services, and environment-specific verification to Codex Cloud,
Local Codex, CI, or a human instead.

This design intentionally trades chat volume for isolation. Four hourly tasks create up to 96 tick chats per day, including
no-op ticks. Keep output minimal and machine-readable where practical. Follow [`chat-retention.md`](chat-retention.md): archive
only after durable GitHub handoff, use manual archive as the current supported cleanup path, and never let chat cleanup alter
delivery state.

### Dedicated scheduler Project and observed Project scoping

A manual product test on 2026-07-28 attempted to create a Scheduled Task for the `Kerb4j` ChatGPT Project from a chat outside
that Project. The available scheduler operation exposed no `project_id` or equivalent destination parameter, so it created a
global task instead. Treat this as an observed capability of the current product surface, not as a permanent API guarantee.

Consequences:

- a scheduled or ordinary chat cannot select an arbitrary destination ChatGPT Project through the available scheduling tool;
- a Scheduled Task execution cannot create a child Scheduled Task at all;
- all durable repository/task context must live in GitHub issues, comments, pull requests, and repository-owned Markdown;
- ChatGPT Project descriptions, files, instructions, and implicit memory are convenience context only;
- every scheduled prompt must be self-contained enough to locate the project profiles and authoritative GitHub state.

Use one dedicated fileless ChatGPT Project, such as `AI Delivery Event Loop`, as an organizational folder for the four
Scheduled Tasks and the fresh chats produced by their runs. The Project is a UI namespace only; it is not the source of
configuration or task context. Keep its Project instructions minimal and repeat critical repository/profile locations in every
Scheduled Task prompt.

### Global ChatGPT loop across repositories

The four tasks may scan several GitHub repositories or Projects because queue state is external. Repository-specific filters
live in profiles referenced explicitly by each scheduled prompt. A separate ChatGPT Project per repository is unnecessary and
cannot be targeted dynamically by the current scheduling surface.

For `Executor = ChatGPT`, the fresh tick may own Planning, Review, artifact-based Verification, focused direct implementation,
or supervision when its current capabilities are sufficient. For other executors it may prepare or perform only a supported
handoff. It must not leave `In progress` without a real worker/dispatch reference.

At a desired five-minute cadence, twelve hourly shards would consume most of the 15-task Pro limit and create up to 288 chats
per day. Prefer a native future sub-hour schedule or an external/local clock instead of multiplying ChatGPT tasks indefinitely.

### Pull-request intake adapters

A repository profile may define PR-first sources. Before claiming ordinary work, the tick scans for open PRs matching the
source and not already represented in the GitHub Project. Repository and PR number are the idempotency key.

For Dependabot, match the bot author and dependency label, distinguish security from routine version updates, and add the PR
itself as `Review / Ready / Executor = ChatGPT`. Do not create a shadow issue unless review discovers independently owned
implementation work. See [`pull-request-intake.md`](pull-request-intake.md).

Intake is not approval. The tick still performs exact-head Review, selects Verification, and respects the human merge boundary.
When a compatibility fix or replacement PR is required, the source bot PR remains durably linked and blocked/superseded rather
than silently rewritten.

### Codex app automation adapter

Codex automations can run on schedules, and some can return to the same conversation. Local automations require the computer
to be awake and the Codex app running. See [Codex automations](https://openai.com/academy/codex-automations/).

Use one persistent dispatcher conversation when native cadence and child-task composition are proven; otherwise use a
provider-appropriate stateless tick or the headless Linux adapter. App-native Remote visibility is optional, not a requirement
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

The work item may be an issue or a pull request. The dispatcher also checks phase compatibility, required access, worker-pool
capacity, existing branch/PR ownership, and absence of a live claim. `Implementer` and `Verifier` are future routing decisions;
phase transitions copy the relevant value into `Executor`.

Use deterministic selection such as security priority, project priority, `readySince`, then repository/item number. A
dispatcher should claim at most its available capacity and must not create duplicate workers or Project items.

## Best-effort atomic claim

GitHub comments and Project field updates are not one transaction. Use a claim generation and ordered intent protocol:

1. Query an eligible `Ready` item.
2. Re-read its Phase, Status, Executor, Assignee, dispatch generation, branch, PR, and existing claim immediately.
3. Post a claim-intent comment containing a unique token, generation, dispatcher slot, executor, and timestamp.
4. Re-read valid intents for the same generation. The lowest GitHub comment ID wins.
5. Only the winner writes `Status = In progress`, the concrete Assignee, claim token, lease time, and worker/tick reference.
6. Re-read and verify that its token/generation still owns the task.
7. Perform the phase directly or confirm a supported external dispatch.
8. Record the concrete chat/task/process, branch/worktree, and expected PR or artifact.
9. If execution/dispatch cannot start, release the claim and restore `Ready`, or set `Blocked` with the exact external reason.

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

One worker or stateless tick owns one phase turn. It must:

- re-read the authoritative issue or PR, current lifecycle fields, nearest `AGENTS.md`, branch/PR, and exact head;
- verify its claim before mutating GitHub or source;
- perform only the routed phase responsibility;
- publish exact evidence and update the next Phase, Status, Executor, and Assignee atomically on a best-effort basis;
- stop without spawning a replacement worker when blocked;
- never merge or enable auto-merge without explicit authorization.

## Generic core versus project profile

The generic documents define lifecycle, intake, claim, leases, correction limits, and adapter contracts. A project profile
supplies repository-specific configuration, for example:

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
intake:
  dependabot:
    enabled: true
    initialPhase: Review
    reviewer: ChatGPT
branches:
  newWorkPrefix: agent/
```

Profiles may add filters, model routing, capacity, issue templates, or required evidence, but must not redefine the shared
meaning of Phase, Status, claims, publication, verification, or merge authority. Start with documented/declarative profiles;
do not build a bespoke dispatcher framework until platform composition has been smoke-tested and a concrete gap remains.
