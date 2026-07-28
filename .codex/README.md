# Codex executor files

This directory contains Codex-specific environment scripts and launch prompts. It is not a second repository policy.

Every Codex task must follow:

- the current GitHub issue or pull request;
- the nearest applicable `AGENTS.md`;
- [`docs/ai-delivery/`](../docs/ai-delivery/README.md) for routing, supervision, and verification.

## Cloud

- `cloud/setup.sh` — one-time environment bootstrap and GitHub CLI authentication.
- `cloud/maintenance.sh` — refresh a cached environment after branch or dependency changes.
- `cloud/use-jdk.sh` — select a supported JDK.
- `cloud/warm-maven-cache.sh` — repository dependency warm-up helper.
- [`docs/ai-delivery/executors/codex-cloud.md`](../docs/ai-delivery/executors/codex-cloud.md) — canonical Cloud runbook.

## Local

- `local/scheduled-task-prompt.md` — persistent app-native dispatcher prompt.
- `local/worker-task-prompt.md` — rendered one-issue worker template.
- `local/run-issue.sh` — unattended CLI fallback, not the app-native default.
- [`docs/ai-delivery/executors/codex-local.md`](../docs/ai-delivery/executors/codex-local.md) — canonical routing and lifecycle.
- [`docs/local-codex-worker.md`](../docs/local-codex-worker.md) — detailed Windows/WSL/VM setup.

The local prompt filenames remain stable because configured Scheduled tasks may reference them. Do not duplicate build,
compatibility, review, or merge rules here; link to the canonical policy instead.
