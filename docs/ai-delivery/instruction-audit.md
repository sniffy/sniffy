# Instruction and scheduler audit

Baseline reviewed: `develop` at `790a990b9a7f1a51403ee43148a3c8daa3085233` (`gptflow-v1` candidate).

## Entry points before this refactor

| Adapter | Scheduler entry point | Worker entry point | Previous recurring load |
| --- | --- | --- | --- |
| ChatGPT Scheduled Tasks | `.chatgpt/scheduled-task-prompt.md` | same occurrence or supervised Codex Cloud | embedded event-loop policy plus many AI-delivery documents on every tick |
| Local Codex app | `.codex/local/scheduled-task-prompt.md` | `.codex/local/worker-task-prompt.md` | many policy documents, full Project read, then Sol/xhigh worker |
| Local Codex CLI | `.codex/local/run-issue.sh` | inline worker prompt | Sol/xhigh by default |
| Codex Cloud | connector/GitHub trigger | Cloud task | bounded fresh implementation, provider-managed model |
| GitHub status workflow | `.github/workflows/delivery-status.yml` | `.github/scripts/delivery-status*.js` | deterministic Project/issue/PR snapshot |

The largest avoidable cost was repeating human documentation and asking an LLM to perform queue filtering that trusted automation
can perform deterministically. Periodic “is the worker still running?” polling was another avoidable cost once workers already own
completion handoff.

## Instruction layers after this refactor

### Always loaded by a dispatcher

- short adapter prompt;
- `docs/ai-delivery/runtime-contract.md`;
- one normalized snapshot/local queue projection;
- `profile.yml` only for adapter settings not already compiled into runtime data.

### Loaded after candidate selection

- current canonical item and formal links;
- exact branch/head/draft/ownership and Project route;
- active control issue;
- selected executor's short runbook/template.

### Loaded by the lifecycle worker

- nearest `AGENTS.md` files;
- complete relevant discussion;
- full exact-head diff, reviews, threads, CI, and artifacts;
- only the detailed lifecycle/routing/verification/environment sections needed for that status.

Human/reference documents remain normative but are not a recurring dispatcher prefix.

## What `profile.yml` does

The profile remains useful as project-specific declarative configuration for repository, Project, identities, routes, capacity,
model defaults, leases, snapshot settings, and scheduler policy. Shared lifecycle semantics remain Markdown. Trusted automation
should compile the profile once into versioned runtime JSON beside the generated queue rather than asking every dispatcher to parse
YAML and prose independently.

## Scheduler setup

### ChatGPT

Create recurring dispatcher tasks in **Chat**, not **Work**. Use one hourly budget clock or four compact hourly tasks at `:00`,
`:15`, `:30`, and `:45` for 15-minute queue latency. Both use the same short prompt and generated snapshot.

The 15-minute clock checks for new actionable GitHub/Project state. It does not poll every active worker. Valid leased ownership is
ignored until the worker hands off or the lease becomes missing, invalid, or expired.

### Local Codex

- dispatcher: `gpt-5.6-luna` / low;
- normal worker: `gpt-5.6-terra` / medium;
- explicit difficult-task escalation: `gpt-5.6-sol` / high;
- xhigh only for an exceptional recorded reason.

Model selection is an operational cost/capability setting, not a lifecycle role.

## Lease-based stale recovery

Every claim records a concrete worker/task/process/branch reference, token/generation, `claimedAt`, and `leaseUntil`. The worker owns
normal completion signalling and guarded handoff. It may renew the same claim before expiry.

A dispatcher does not query a healthy worker. Only missing/invalid worker evidence or expired lease enters the attention queue.
Targeted recovery preserves the exact generation and either extends the active lease, completes a lost handoff, recovers the same
workspace/branch, or releases only when no active/recoverable work remains. There is no per-worker observation schedule and no
separate monitoring task.

## Telemetry and token accounting

Every tick/worker reports start/end, duration, model/reasoning, snapshot generation, selected candidate, outcome, and provider token
counters when exposed. Otherwise counters are `null`; billing precision is never manufactured. Useful aggregation dimensions are
adapter/profile, no-op versus productive outcome, model/reasoning, snapshot/live-read counts, lifecycle/executor, failures,
rate limits, guarded conflicts, and stale-recovery results.

## Preserved behavior

The compact contract retains universal PR intake, canonical issue/PR selection, exact-head validity, guarded Project mutations,
non-draft Review publication, independent-review rules, same-repository PR continuation, fork/Dependabot boundaries, Codex Cloud
supervised dispatch, convergence checkpoints, truthful verification, human-only privileged actions, and the explicit no-merge
boundary.

## Baseline tag

```bash
git fetch origin develop
test "$(git rev-parse origin/develop)" = 790a990b9a7f1a51403ee43148a3c8daa3085233
git tag -a gptflow-v1 790a990b9a7f1a51403ee43148a3c8daa3085233 \
  -m "GPTFlow v1 before token-efficiency refactor"
git push origin refs/tags/gptflow-v1
```

The connected GitHub tool can create branches/commits/PRs but does not expose tag-ref creation, so the exact tag remains a
maintainer command rather than being approximated by a branch.
