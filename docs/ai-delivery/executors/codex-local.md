# Local Codex executors

Local Codex has two supported shapes:

1. **app-native Local Codex** for app-owned conversations, worktrees, automations, and optional mobile Remote visibility;
2. **headless Local Codex CLI** for unattended Linux scheduling, persistent caches/services, and scalable worker processes.

Both follow `AGENTS.md`, universal issue/PR canonicalization, the shared lifecycle, guarded control protocol, verification contract,
and no-merge boundary. Current Sniffy configuration in [`../profile.yml`](../profile.yml) uses **Sol** with **extra-high** reasoning
for Local Codex.

## Route to Local Codex when

Prefer Local Codex for:

- complex architecture, concurrency, lifecycle, cleanup, or failure composition;
- cross-version Java and same-artifact compatibility;
- persistent dependencies, Docker, services, browsers, or representative local environments;
- existing same-repository branch/PR continuation, adoption, and recovery;
- verification requiring local state or long-lived artifacts;
- a convergence checkpoint that identifies Cloud context/environment/capability limits.

Do not route an unresolved product, canonicalization, or architecture decision merely because the local model/profile is strong.
Planning and maintainer authority still precede implementation.

Do not adopt a fork or Dependabot branch for direct autonomous correction. Those remain contributor/bot-owned or are superseded by
a deliberately routed internal task.

## Fixed model/profile

The local scheduled automation selects its model and reasoning for the automation as a whole. The current Sniffy dispatcher and
workers therefore use Sol / extra-high consistently. Do not label Codex Cloud as Terra and do not pretend the local dispatcher can
switch models per item unless a future product capability is explicitly smoke-tested and the profile is changed.

## App-native topology

```text
persistent dispatcher conversation (Sol / extra-high)
  -> empty queue: NO_CHANGE, no task/chat/worktree
  -> eligible canonical issue or PR: guarded claim through central control issue
       -> one one-time standalone worker task
       -> one dedicated canonical-item conversation
       -> one isolated app-managed worktree
       -> fresh branch or exact adopted existing PR branch
       -> worker publishes evidence and guarded lifecycle handoff
```

Use app-native when durable Codex conversation, interactive steering, app-managed worktree, or Remote visibility adds real value.
The Windows app runs on the disposable VM; code, terminal, GitHub CLI, Java, Maven, Node, Docker, and tests run in WSL2.

Before enabling app dispatch, smoke-test:

- an empty tick creates no worker conversation or worktree;
- a claimed issue item creates exactly one standalone worker;
- an explicitly routed same-repository PR continuation reuses the exact branch/PR without a duplicate;
- fork and Dependabot PRs cannot be adopted for direct correction;
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
  -> query canonical Status + Execution + Executor
  -> central guarded claim
  -> isolated git worktree
  -> codex exec with rendered lifecycle prompt
  -> update exact branch/PR, tests, evidence, guarded handoff
```

Use headless CLI when unattended reliability, Docker/services, persistent caches, several isolated workers, and machine-readable
logs matter more than app conversation visibility. A five- or fifteen-minute local timer avoids ChatGPT's hourly task limit.
Host-local `flock` prevents same-host overlap; the GitHub transition workflow serializes cross-provider claims by target item.

Each worker owns:

- one canonical issue or PR and every linked requirement/evidence object;
- one verified claim token and lifecycle generation;
- one isolated worktree;
- one fresh branch/intended PR or exact adopted existing branch/PR;
- one rendered prompt for the current lifecycle status;
- one structured log/evidence directory;
- a bounded process timeout and cleanup path.

A host may run several workers only when capacity, memory, disk, Maven/npm/Docker contention, and repository ownership are
explicitly configured. Never let two workers own the same canonical item or branch.

## Dispatcher contract

Both adapters:

- read canonical `Execution = Ready` items routed to Local Codex;
- accept issue and PR Project items;
- respect directed Assignee or empty pool ownership;
- inspect canonicalization, lifecycle, profile, access, capacity, exact branch/PR/head, current worker, and due monitoring;
- exclude duplicate representations and linked issues suppressed by an open canonical multi-issue PR;
- use the one active technical control issue and one guarded `delivery-control/v1` command per claim/transition;
- inspect the reaction and re-read Project state before spawning;
- record concrete conversation/task or process/worktree and exact PR references;
- release to `Ready` when spawn fails;
- set `Blocked` only when Dmitry must decide or act;
- create no source or GitHub mutation for an empty queue.

Worker observation after 15 minutes, another 15 minutes, then hourly is part of this same dispatcher loop. Do not add a separate
monitoring scheduler.

## Fresh versus adopted continuation

Fresh Implementation starts from current `origin/develop` only when no existing branch/PR owns the canonical item.

An adopted continuation is valid when all are true:

- an open same-repository PR already contains the intended implementation;
- the canonical Project item explicitly routes `Implementer = Local Codex`, `Executor = Local Codex`;
- the worker reference names the exact PR, branch, and head;
- effective push permission and absence of competing ownership are verified;
- the correction request is complete enough for one implementation turn.

The worker then fetches and updates the exact existing branch. It does not reset, rebase, force-push, discard useful commits,
open a replacement PR, or silently narrow the PR to only its latest review comments. Branch authorship by Dmitry, ChatGPT, or an
IDE agent is neither a blocker nor sufficient authorization; the guarded Project route is authoritative.

## Worker contract

A Local Codex worker performs only the routed lifecycle status:

- Planning is unusual and normally remains ChatGPT/human-owned;
- Implementation changes code, runs implementer-owned checks, inspects the full diff, commits, pushes, and verifies publication;
- Verification runs outcome-centric system/integration/browser proof when selected;
- Review is performed only with an independent configured identity and explicit route.

For fresh work, create one branch and intended PR. For continuation, use the exact existing same-repository PR branch. Mark the PR
ready for review when implementation and locally available proof are complete. Publish human-useful evidence on the canonical
item and synchronize the PR description.

Implementation completion is not just a push: the worker must verify the exact remote head and use one guarded command to hand the
canonical item to `Review / Ready / ChatGPT`, recording the PR URL/head and clearing worker ownership. It then assigns
`bedrin-gpt` separately and verifies both state and assignment.

Never merge or enable auto-merge without Dmitry's explicit instruction.

## Security

Host policy defines filesystem, network, credentials, Docker, services, browsers, and sandbox/approval mode. Treat Docker group
or full filesystem/network access as privileged. Use a dedicated low-value VM or host account with repository-scoped GitHub
credentials and no unrelated personal/employer data.

Codex CLI still needs network access for model calls even when task-process network is restricted. External integration tests and
package downloads need separately configured policy. GitHub rulesets remain the final write boundary.
