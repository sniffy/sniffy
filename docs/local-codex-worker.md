# Local Codex workers

Local Codex CLI/App registration, capability routing, immutable job manifests, Linux/macOS workspace selection, systemd or runner
transport, model profiles, heartbeat, health probes, and exact-generation recovery moved to the private
[`bedrin-management/ledger`](https://github.com/bedrin-management/ledger).

The Windows App adapter remains disabled until its runtime is reliable. A Linux or macOS worker may serve multiple projects; its
machine wrapper chooses an isolated worktree for each portable job. Sniffy-specific requirements are declared in
[`.ai-delivery/target.yml`](../.ai-delivery/target.yml).

This compatibility entry point remains for historical links.
