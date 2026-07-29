# Local Codex executors

Local Codex has two supported architectural shapes:

1. **app-native Local Codex** for app-owned conversations, worktrees, automations, and optional mobile Remote visibility;
2. **headless Local Codex CLI** for unattended Linux scheduling, persistent caches/services, and scalable worker processes.

Both follow `AGENTS.md`, the shared lifecycle, event-loop claim protocol, verification contract, and no-merge boundary.

## Choose the shape

Use app-native when a durable Codex conversation, interactive steering, app-managed worktree, or Remote visibility adds real
value. Use headless CLI when the important properties are unattended reliability, systemd/cron cadence, Docker/services,
persistent dependencies, several isolated workers, and machine-readable logs.

Remote control from mobile is optional. It is not required for the generic local-worker design.

## App-native topology

```text
persistent dispatcher conversation
  -> empty queue: NO_CHANGE, no task/chat/worktree
  -> eligible issue: one one-time standalone worker task
       -> one dedicated issue conversation
       -> one isolated app-managed worktree
       -> worker publishes lifecycle evidence and handoff
```

The native ChatGPT/Codex app runs on the disposable Windows VM. Its coding agent, terminal, repository, GitHub CLI, Java,
Maven, Node, Docker, and tests run in WSL2. The Hyper-V/Windows/WSL/Docker runbook remains in
[`../../local-codex-worker.md`](../../local-codex-worker.md).

Codex automations may return to the same conversation; local automations require the computer awake and the app running.
See [Codex automations](https://openai.com/academy/codex-automations/).

Before enabling app dispatch, smoke-test:

- an empty tick creates no new conversation or worktree;
- a claimed item creates exactly one standalone worker;
- the worker uses the expected project/worktree;
- failed child creation releases the claim;
- repeated automation returns to the intended persistent dispatcher.

The canonical compatibility paths remain `.codex/local/scheduled-task-prompt.md` and
`.codex/local/worker-task-prompt.md`. Do not assume nested child-task creation survives a product update without retesting.

## Headless Linux topology

```text
systemd timer or cron
  -> flock / host-local dispatcher lock
  -> load one or more project profiles
  -> query Status + Execution + Executor
  -> GitHub best-effort claim
  -> isolated git worktree
  -> codex exec with rendered lifecycle prompt
  -> tests, publication, evidence, and next-status handoff
```

The Codex CLI can read, modify, and run local code, and `codex exec` is intended for shell workflows. See:

- [OpenAI Codex CLI – Getting Started](https://help.openai.com/en/articles/11096431)
- [Codex is now generally available](https://openai.com/index/codex-now-generally-available/)

A five- or fifteen-minute timer is straightforward on Linux and avoids the hourly ChatGPT Scheduled Task limit. `flock`
prevents overlapping dispatcher ticks on one machine; the GitHub claim protocol prevents ordinary cross-machine/provider
duplicates.

Each worker owns:

- one claim token and lifecycle generation;
- one isolated worktree;
- one branch and intended PR;
- one rendered prompt for the current lifecycle status;
- one structured log/evidence directory;
- a bounded process timeout and cleanup path.

A host may run several workers only when capacity, memory, disk, Maven/npm/Docker contention, and repository ownership are
explicitly configured. Never let two workers own the same task/branch.

## Dispatcher contract

Both adapters use [`../event-loop.md`](../event-loop.md). The dispatcher:

- reads `Execution = Ready` items routed to its executor type;
- respects directed Assignee or empty pool ownership;
- checks lifecycle status, project profile, access, capacity, branch/PR, and existing claims;
- claims on a best-effort atomic basis before creating a worker;
- records the concrete conversation/task or process/worktree reference;
- releases to `Ready` when spawn fails;
- sets `Blocked` only when Dmitry must decide or act, with `Executor = Human`, `Assignee = bedrin`, and the exact request;
- creates no source mutation for an empty queue.

## Worker contract

A Local Codex worker performs only the routed lifecycle status:

- Planning is unusual and should normally remain ChatGPT/human-owned;
- Implementation changes code, runs implementer-owned tests/browser checks, inspects the diff, commits, pushes, and verifies
  exact publication;
- Verification runs outcome-centric system/integration/browser proof when Local Codex is the selected Verifier;
- Review is performed only when the configured reviewer identity is independent and the task explicitly routes it there.

For fresh work, create the branch from current `origin/develop`. For continuation, use the exact existing PR branch and never
reset, rebase, force-push, replace the PR, or discard unrelated work. Keep PRs draft only while implementation or locally
available proof is incomplete.

## Environment and security

Host policy defines filesystem, network, credentials, Docker, services, browsers, and sandbox/approval mode. Treat Docker
group or full filesystem/network access as privileged. Use a dedicated low-value VM or host account with repository-scoped
GitHub credentials and no unrelated personal/employer data.

Codex CLI execution still requires network access for model calls even when the command sandbox restricts task-process
network. External integration tests and package downloads need separately configured host/sandbox policy. Record the actual
capability, not an assumed product default.

GitHub rulesets remain the final write boundary. Formal review must use an identity independent from the PR author. No local
worker merges or enables auto-merge without Dmitry's explicit instruction.
