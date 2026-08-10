# Codex target helpers

This directory contains only Sniffy-specific Codex Cloud environment helpers:

- `cloud/setup.sh`;
- `cloud/maintenance.sh`;
- `cloud/use-jdk.sh`;
- `cloud/warm-maven-cache.sh`.

Executor routing, scheduler state, model profiles, leases, job manifests, worker prompts, telemetry, and local CLI/App adapters live
in the private [`bedrin-management/ledger`](https://github.com/bedrin-management/ledger). Do not recreate a repository-owned
dispatcher here.

Every Codex worker still follows the canonical issue/PR, root and nested `AGENTS.md`, and
[`../.ai-delivery/target.yml`](../.ai-delivery/target.yml). Repository merges do not rewrite prompts already embedded in Codex or
ChatGPT automations.
