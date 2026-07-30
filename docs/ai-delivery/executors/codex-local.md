# Local Codex executors

Local Codex has two supported shapes:

1. **app-native Local Codex** for app-owned conversations, worktrees, automations, and optional mobile Remote visibility;
2. **headless Local Codex CLI** for unattended Linux scheduling, persistent caches/services, and scalable worker processes.

Both follow `AGENTS.md`, the shared lifecycle, guarded control protocol, verification contract, and no-merge boundary. Current
Sniffy configuration in [`../profile.yml`](../profile.yml) uses **Sol** with **extra-high** reasoning for Local Codex.

## Route to Local Codex when

Prefer Local Codex for:

- complex architecture, concurrency, lifecycle, cleanup, or failure composition;
- cross-version Java and same-artifact compatibility;
- persistent dependencies, Docker, services, browsers, or representative local environments;
- existing branch/PR continuation and recovery;
- verification requiring local state or long-lived artifacts;
- a convergence checkpoint that identifies Cloud context/environment/capability limits.

Do not route an unresolved product or architecture decision merely because the local model/profile is strong. Planning and
maintainer authority still precede implementation.

## Fixed model/profile

The local scheduled automation selects its model and reasoning for the automation as a whole. The current Sniffy dispatcher and
workers therefore use Sol / extra-high consistently. Do not label Codex Cloud as Terra and do not pretend the local dispatcher can
switch models per issue unless a future product capability is explicitly smoke-tested and the profile is changed.

## App-native topology

```text
persistent dispatcher conversation (Sol / extra-high)
  -> empty queue: NO_CHANGE, no task/chat/worktree
  -> eligible issue: guarded claim through central control issue
       -> one one-time standalone worker task
       -> one dedicated issue conversation
       -> one isolated app-managed worktree
       -> worker publishes evidence and guarded lifecycle handoff
```

Use app-native when durable Codex conversation, interactive steering, app-managed worktree, or Remote visibility adds real value.
The Windows app runs on the disposable VM; code, terminal, GitHub CLI, Java, Maven, Node, Docker, and tests run in WSL2.

Before enabling app dispatch, smoke-test:

- an empty tick creates no worker conversation or worktree;
- a claimed item creates exactly one standalone worker;
- the worker uses the expected project/worktree and Sol / extra-high profile;
- failed child creation releases the guarded claim;
- repeated automation returns to the intended persistent dispatcher;
- concurrent dispatchers produce one successful claim and one conflict without target-item claim comments.

Canonical prompts are `.codex/local/scheduled-task-prompt.md` and `.codex/local/worker-task-prompt.md`. Do not assume nested
one-time task creation survives a product update without retesting.

## Headless Linux topology

```text
systemd timer or cron
  -> flock / host-local dispatcher lock
  -> load docs/ai-delivery/profile.yml
  -> query Status + Execution + Executor
  -> central guarded claim
  -> isolated git worktree
  -> codex exec with rendered lifecycle prompt
  -> tests, publication, evidence, guarded handoff
```

Use headless CLI when unattended reliability, Docker/services, persistent caches, several isolated workers, and machine-readable
logs matter more than app conversation visibility. A five- or fifteen-minute local timer avoids ChatGPT's hourly task limit.
Host-local `flock` prevents same-host overlap; the GitHub transition workflow serializes cross-provider claims by target item.

Each worker owns:

- one verified claim token and lifecycle generation;
- one isolated worktree;
- one branch and intended PR;
- one rendered prompt for the current lifecycle status;
- one structured log/evidence directory;
- a bounded process timeout and cleanup path.

A host may run several workers only when capacity, memory, disk, Maven/npm/Docker contention, and repository ownership are
explicitly configured. Never let two workers own the same task/branch.

## Dispatcher contract

Both adapters:

- read `Execution = Ready` items routed to Local Codex;
- respect directed Assignee or empty pool ownership;
- inspect lifecycle, profile, access, capacity, branch/PR, current worker, and due monitoring;
- use the one active technical control issue and one guarded `delivery-control/v1` command per claim/transition;
- inspect the reaction and re-read Project state before spawning;
- record concrete conversation/task or process/worktree references;
- release to `Ready` when spawn fails;
- set `Blocked` only when Dmitry must decide or act;
- create no source or GitHub mutation for an empty queue.

Worker observation after 15 minutes, another 15 minutes, then hourly is part of this same dispatcher loop. Do not add a separate
monitoring scheduler.

## Worker contract

A Local Codex worker performs only the routed lifecycle status:

- Planning is unusual and normally remains ChatGPT/human-owned;
- Implementation changes code, runs implementer-owned checks, inspects the diff, commits, pushes, and verifies publication;
- Verification runs outcome-centric system/integration/browser proof when selected;
- Review is performed only with an independent configured identity and explicit route.

For fresh work, branch from current `origin/develop`. For continuation, use the exact existing PR branch and never reset, rebase,
force-push, replace the PR, or discard useful work. Keep PRs draft only while implementation or locally available proof is
incomplete.

Publish human-useful evidence on the target item. Use the central technical issue for Project transitions and claim/lease state.
Never merge or enable auto-merge without Dmitry's explicit instruction.

## Security

Host policy defines filesystem, network, credentials, Docker, services, browsers, and sandbox/approval mode. Treat Docker group
or full filesystem/network access as privileged. Use a dedicated low-value VM or host account with repository-scoped GitHub
credentials and no unrelated personal/employer data.

Codex CLI still needs network access for model calls even when task-process network is restricted. External integration tests and
package downloads need separately configured policy. GitHub rulesets remain the final write boundary.
