# Local Codex executor

Use the app-native Local Codex worker when work benefits from a persistent local environment, existing-branch continuity,
Docker, browsers, services, OS-specific debugging, long builds, or mobile Remote visibility. Shared engineering and
supervision policy lives in `AGENTS.md` and `docs/ai-delivery/`; this document describes the executor topology.

## Topology

```text
persistent dispatcher chat
  -> empty queue: NO_CHANGE, no task/chat/worktree
  -> eligible issue: one one-time standalone worker task
       -> one dedicated issue chat
       -> one isolated app-managed worktree
       -> one in-chat follow-up schedule: 15 min, 15 min, then hourly
```

The native ChatGPT/Codex app runs on the disposable Windows VM. Its coding agent, terminal, repository, GitHub CLI, Java,
Maven, Node, Docker, and tests run in WSL2. The full Hyper-V, Windows, WSL, Docker, credential, and Remote setup remains in
[`../../local-codex-worker.md`](../../local-codex-worker.md).

## Dispatcher

The dispatcher is a coordinator, not an implementer. It:

- reads the Project queue and selects at most one eligible `Local Codex` issue;
- prefers an explicitly re-queued continuation over fresh work;
- verifies Definition of Ready, exact executor field, existing branch/PR ownership, and no active duplicate;
- selects the routed model/reasoning;
- records the claim and confirms one app-owned child task exists;
- rolls back the claim when child creation fails;
- creates no source branch, worktree, chat, or GitHub mutation for an empty queue.

The canonical prompt remains `.codex/local/scheduled-task-prompt.md`. Its filename is intentionally preserved because an
installed Scheduled task may reference it.

## Worker

The one-issue worker:

- uses one dedicated chat and isolated worktree;
- creates a fresh branch from current `origin/develop` or continues the exact existing PR branch;
- never resets, rebases, force-pushes, or replaces a continuation branch/PR;
- implements, tests, inspects, commits, pushes, and verifies real GitHub publication;
- keeps the PR draft only while implementation or locally applicable proof is incomplete;
- creates one in-chat follow-up task and supervises the PR at the required cadence;
- pauses follow-up when complete or blocked;
- never merges or enables auto-merge.

The canonical template remains `.codex/local/worker-task-prompt.md` for compatibility with the dispatcher.

## Route to Local Codex when

- a published PR needs precise continuation;
- persistent Maven/npm/Docker/browser caches materially improve iteration;
- local services, special networking, hardware, OS behavior, or privileged development tools are required;
- browser inspection, screenshots, or interactive debugging are central;
- Cloud task windows or repeated cold starts are causing delivery friction;
- a first-of-kind architecture/test pattern needs sustained local inspection.

Do not route merely because a task is large in lines. Count risk and proof axes. Local execution does not authorize unresolved
product decisions or bespoke infrastructure.

## Manual and CLI fallback

Before enabling the recurring dispatcher, smoke-test that the installed app can create one one-time standalone child task
from an in-chat scheduled run. Prove an empty queue creates no clutter and failed child creation rolls back the claim.

When nested app-owned task creation is unavailable, pause the dispatcher and create the one-time worker manually in the app.
`.codex/local/run-issue.sh` remains an unattended CLI fallback for a disposable isolated environment. Do not launch nested
`codex exec`, shell UI automation, Python observers, or app-server integrations from the app-native dispatcher without a
separately approved architecture.

## Identity and security

Use a dedicated low-value VM and GitHub identity. The Windows VM is the security boundary; Docker-group or full agent access
inside it is effectively privileged and acceptable only because the VM contains no host files or unrelated credentials.
GitHub rulesets remain the final write boundary. Formal review must use an identity independent from the PR author.
