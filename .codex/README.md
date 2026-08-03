# Codex executor files

This directory contains Codex-specific adapters, not a second repository policy. Every selected worker follows the canonical item,
nearest `AGENTS.md`, and `docs/ai-delivery/runtime-contract.md`; it loads detailed lifecycle/runbook sections only after selection.

## Model profiles

- dispatcher heartbeat: `gpt-5.6-luna` / low;
- normal lifecycle worker: `gpt-5.6-terra` / medium;
- explicit difficult-task escalation: `gpt-5.6-sol` / high;
- xhigh only with a recorded exceptional reason.

## Cloud

- `cloud/setup.sh`, `maintenance.sh`, `use-jdk.sh`, `warm-maven-cache.sh` — Cloud environment helpers.
- `docs/ai-delivery/executors/codex-cloud.md` — Cloud runbook.

## Local app

- `local/scheduled-task-prompt.md` — persistent Luna/low dispatcher prompt;
- `local/project-queue-snapshot.sh` — one bounded full-Project query and normalized Local Codex queue;
- `local/worker-task-prompt.md` — rendered Terra/medium lifecycle worker;
- `docs/local-codex-worker.md` — Windows/WSL/VM setup.

The dispatcher runs the snapshot helper exactly once per tick, selects from retained normalized JSON, and live-reads only one
candidate before the guarded claim. Do not repeatedly query ProjectV2 or enumerate all provider tasks on the normal Ready path.
Provider inventory is only targeted recovery for one ambiguous already-claimed generation.

Repository merges do not replace prompt text embedded in existing Codex automations. Copy the updated prompt, select the documented
model/reasoning profile, and repeat the smoke test.

## Headless

- `local/run-issue.sh` — focused CLI worker, Terra/medium by default with explicit environment-variable escalation;
- reusable unattended dispatch uses cron/systemd, `flock`, one normalized snapshot, guarded claims, isolated worktrees, and
  `codex exec`;
- `docs/ai-delivery/executors/codex-local.md` — topology, continuity, monitoring, telemetry, and security.

Do not duplicate lifecycle, compatibility, review, verification, or merge rules here. Never merge or enable auto-merge without
Dmitry's explicit instruction.
