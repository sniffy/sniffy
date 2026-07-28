# Codex executor files

This directory contains Codex-specific environment scripts and launch prompts. It is not a second repository policy.

Every Codex task must follow:

- the current GitHub issue or pull request;
- the nearest applicable `AGENTS.md`;
- [`docs/ai-delivery/`](../docs/ai-delivery/README.md) for lifecycle, routing, event-loop, supervision, and verification.

## Cloud

- `cloud/setup.sh` — one-time environment bootstrap and GitHub CLI authentication.
- `cloud/maintenance.sh` — refresh a cached environment after branch or dependency changes.
- `cloud/use-jdk.sh` — select a supported JDK.
- `cloud/warm-maven-cache.sh` — repository dependency warm-up helper.
- [`docs/ai-delivery/executors/codex-cloud.md`](../docs/ai-delivery/executors/codex-cloud.md) — canonical Cloud runbook.

## Local app adapter

- `local/scheduled-task-prompt.md` — persistent app-native dispatcher prompt.
- `local/worker-task-prompt.md` — rendered one-issue app worker template.
- [`docs/local-codex-worker.md`](../docs/local-codex-worker.md) — detailed Windows/WSL/VM setup.

The prompt filenames remain stable because configured automations may reference them.

## Headless local adapter

- `local/run-issue.sh` — current one-issue CLI launch helper.
- A reusable unattended dispatcher should use systemd/cron, host-local `flock`, the GitHub claim protocol, isolated worktrees,
  and `codex exec` as described in
  [`docs/ai-delivery/event-loop.md`](../docs/ai-delivery/event-loop.md).
- [`docs/ai-delivery/executors/codex-local.md`](../docs/ai-delivery/executors/codex-local.md) — app-native versus headless topology,
  capabilities, and security boundaries.

Do not duplicate lifecycle, build, compatibility, review, verification, or merge rules here; link to canonical policy instead.