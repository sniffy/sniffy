# Repository-owned Codex CLI executor

This adapter is the unattended Local Codex implementation for a dedicated Linux host. It starts from current `develop` and has no
runtime, prompt, branch, model, conversation, or rollout dependency on the app-native adapter. The CLI dispatcher is deterministic
shell and invokes no model for an empty queue; each claimed lifecycle generation runs in one isolated Git worktree with its own
recorded model profile.

## Components

- `.codex/local/cli-dispatch.sh` performs one queue read, targeted validation, guarded claim, systemd launch, and strong acknowledgement.
- `.codex/local/cli-launch-worker.sh` creates the generation worktree and runs `codex exec --json`.
- `.codex/local/cli-recover-worker.sh` reconciles one exact generation and never spawns a replacement.
- `.codex/local/cli-worker-state.js` validates branch/manifest identity and requires `thread.started` plus `turn.started`.
- `.codex/local/systemd/` contains the dispatcher timer/service and worker template.

The existing `.codex/local/project-queue-snapshot.sh`, lifecycle worker template, and `delivery-control/v1` remain authoritative.

## Per-generation model selection

The systemd services do not pin one model. Before claim, the dispatcher resolves one named profile from the authoritative issue or
pull-request body:

```markdown
Codex worker profile: economy
```

The accepted profiles are:

| Profile | Default model | Reasoning | Intended use |
| --- | --- | --- | --- |
| `economy` | `gpt-5.6-luna` | low | bounded mechanical work with strong executable proof |
| `balanced` | `gpt-5.6-terra` | medium | normal implementation and verification |
| `frontier` | `gpt-5.6-sol` | high | explicitly difficult architecture, convergence, or failure-composition work |

No directive uses `AI_DELIVERY_CODEX_DEFAULT_PROFILE`, normally `balanced`. Conflicting directives fail before claim. The resolved
profile, exact model, reasoning effort, and selection source are written to the manifest and passed explicitly to `codex exec`.
Environment variables may remap a named profile during a controlled model migration, but `CODEX_MODEL` and
`CODEX_REASONING_EFFORT` are deliberately ignored by the recurring launcher so a VM-wide default cannot silently override an
already claimed generation.

## Durable state

The default state root is:

```text
~/.local/state/sniffy-local-executor/
```

Each generation owns:

```text
generations/<generation>/
  manifest.json
  prompt.md
  events.jsonl
  stderr.log
  ack.json
  final.md
```

The local manifest stores the unit, process, worktree, terminal event, and exit details. Project `Worker reference` deliberately
stores only portable identity: adapter, state, generation, token, branch, lease, and confirmed Codex thread ID. Never publish local
paths, PIDs, or credentials to Project fields.

Manifest updates use a short file lock plus atomic replacement so the launcher, dispatcher, and recovery path cannot overwrite one
another's process, thread, or Project-reference evidence.

## Spawn protocol

1. The dispatcher runs the Project snapshot helper exactly once.
2. One Ready candidate is live-read and its branch is resolved:
   - exact continuation PR branch;
   - otherwise explicit authoritative issue/route branch;
   - otherwise deterministic fallback.
3. A guarded claim records `state=spawning` with a short provisional lease.
4. The dispatcher writes one atomic manifest and starts `sniffy-local-worker@<generation>.service`.
5. The launcher reads the generation's recorded profile and starts `codex exec --json` in one isolated worktree.
6. A worker is confirmed only after JSONL contains both `thread.started` with a thread ID and `turn.started`.
7. Only then does a second guarded update record `state=running`, the thread ID, and the normal lease.
8. The lifecycle worker performs its own guarded handoff. A failed or disappeared generation is recovered by exact manifest identity.

`DISPATCHED` is forbidden before step 7. An opaque queued command, PID, unit creation, directory, or `thread.started` without
`turn.started` is not sufficient. `turn.completed` records local Codex completion but does not by itself prove the GitHub lifecycle
handoff; current Project and remote PR/head state remain authoritative.

## Branch authority and continuation

Branch selection is ordered:

1. the exact branch of a canonical pull request;
2. an explicit `Branch:` value in the authoritative issue or maintainer decision;
3. only when neither exists, deterministic `agent/issue-<number>` fallback.

For an issue, the dispatcher checks whether the authoritative branch already has an open PR or remote ref. It then resumes that
branch as continuation rather than creating a duplicate generation. Conflicting explicit branch values fail closed.

## Supervision and recovery

Every timer tick inspects only a currently owned `codex-cli-v1` generation whose local manifest is known. This is a cheap local
systemd/manifest check, not provider-wide task inventory and not a model invocation.

- active unit plus `ack.json`: preserve the same generation and recover a lost running-reference update when necessary;
- active unit without acknowledgement: preserve spawning under the provisional lease;
- inactive unacknowledged unit: release after the short provisional lease, or immediately after a definitive launcher failure;
- inactive acknowledged unit: release immediately if Project ownership remains, regardless of the nominal four-hour lease;
- a Project/manifest mismatch fails closed instead of inventing a replacement worker;
- a clean completed worktree is removed; dirty or unpushed work is retained for exact-generation recovery.

Thus process death cannot create a four-hour phantom worker. A subsequent correction round can reuse the exact remote branch and PR.

## Install

Provision the recommended Ubuntu Server VM, toolchains, isolated identities, authentication, and systemd services with
[`codex-cli-linux-vm.md`](codex-cli-linux-vm.md). The final service configuration intentionally contains named profile mappings and
does not set one global `CODEX_MODEL`:

```bash
mkdir -p ~/.config/systemd/user ~/.config
cp .codex/local/systemd/*.service .codex/local/systemd/*.timer ~/.config/systemd/user/
cat > ~/.config/sniffy-local-executor.env <<'EOF'
AI_DELIVERY_REPOSITORY=sniffy/sniffy
AI_DELIVERY_BASE_BRANCH=develop
AI_DELIVERY_STATE_ROOT=/home/worker/.local/state/sniffy-local-executor
AI_DELIVERY_WORKTREE_ROOT=/home/worker/.local/state/sniffy-local-executor/worktrees
AI_DELIVERY_CODEX_DEFAULT_PROFILE=balanced
AI_DELIVERY_CODEX_ECONOMY_MODEL=gpt-5.6-luna
AI_DELIVERY_CODEX_ECONOMY_REASONING=low
AI_DELIVERY_CODEX_BALANCED_MODEL=gpt-5.6-terra
AI_DELIVERY_CODEX_BALANCED_REASONING=medium
AI_DELIVERY_CODEX_FRONTIER_MODEL=gpt-5.6-sol
AI_DELIVERY_CODEX_FRONTIER_REASONING=high
EOF
systemctl --user daemon-reload
systemctl --user enable --now sniffy-local-dispatch.timer
```

Use real absolute paths in the environment file; do not rely on systemd specifier expansion inside variable values. Keep the
machine awake and enable user lingering only inside the dedicated low-value worker VM.

## Security

Run only in the disposable VM or another isolated host. The worker uses `danger-full-access` and no interactive approval, so the
outer machine, repository-scoped credential, and server-side branch protection are the security boundary. Never mount employer or
personal data, and never grant organization administration, secrets administration, merge bypass, or force-push authority.

The dispatcher and CI validation use no repository write permission beyond the worker's explicitly configured GitHub identity and
the existing guarded control plane. The PR validation workflow has `contents: read` only and never executes runtime dispatch.

## Validation

Before enabling the timer, prove:

- empty queue creates no generation/worktree and invokes no Codex model;
- one Ready item creates one generation/unit/worktree/thread;
- missing either acknowledgement event leaves only a provisional spawn;
- concurrent dispatcher/launcher attempts have one `flock` winner;
- explicit branch `agent/demo-history-import` overrides fallback generation;
- `economy`, `balanced`, and `frontier` directives reach `codex exec` through the immutable generation manifest;
- a conflicting profile directive fails before claim, and a VM-wide `CODEX_MODEL` cannot replace the recorded choice;
- restart and failed process recover the same generation without duplicate branch/PR;
- completion performs the guarded handoff;
- no worker can merge or enable auto-merge.
