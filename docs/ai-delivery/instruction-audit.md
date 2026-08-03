# Instruction and scheduler audit

Baseline reviewed: `develop` at `790a990b9a7f1a51403ee43148a3c8daa3085233` (`gptflow-v1` candidate).

## Entry points before this refactor

| Adapter | Scheduler entry point | Worker entry point | Effective recurring load |
| --- | --- | --- | --- |
| ChatGPT Scheduled Tasks | `.chatgpt/scheduled-task-prompt.md` | the same scheduled occurrence or supervised Codex Cloud | the embedded event-loop prompt plus `AGENTS.md`, 10 AI-delivery documents, the ChatGPT runbook, and the snapshot supplement on every tick |
| Local Codex app | `.codex/local/scheduled-task-prompt.md` | `.codex/local/worker-task-prompt.md` | `AGENTS.md`, nine AI-delivery documents, dispatcher prompt, worker template, Project snapshot, then a Sol/extra-high worker |
| Local Codex CLI | `.codex/local/run-issue.sh` | its inline prompt plus repository instructions discovered by Codex | Sol/xhigh by default for every issue |
| Codex Cloud | GitHub `@codex`/connector dispatch plus `docs/ai-delivery/executors/codex-cloud.md` | Cloud task | bounded fresh implementation, provider-managed model |
| GitHub status workflow | `.github/workflows/delivery-status.yml` | `.github/scripts/delivery-status*.js` | programmatic Project, issue, PR, formal-closing-link, and mergeability snapshot |

The largest avoidable cost was not the lifecycle itself. It was repeating human documentation and asking an LLM to perform queue
filtering that the status workflow or a local snapshot helper can perform deterministically.

## Instruction layers after this refactor

### Always loaded by a dispatcher

- the scheduler's short adapter prompt;
- `docs/ai-delivery/runtime-contract.md`;
- one normalized snapshot or local queue projection;
- `profile.yml` only when a concrete adapter setting cannot be carried in the generated snapshot.

### Loaded only after candidate selection

- current canonical issue/PR and formal links;
- current exact branch/head/draft/ownership and Project route;
- active control issue;
- the selected executor's short runbook or worker template.

### Loaded only by the lifecycle worker

- nearest `AGENTS.md` files;
- complete relevant issue/PR discussion;
- full exact-head diff, reviews, threads, CI, artifacts;
- only the lifecycle, routing, supervision, verification, or environment sections needed for that status.

### Human/reference documentation

`README.md`, `lifecycle.md`, `control-plane.md`, `pull-request-intake.md`, `routing.md`, `supervision.md`, `verification.md`,
retrospectives, architecture notes, and setup guides remain valuable. They are not a mandatory repeated prompt prefix.

## What `profile.yml` really does

The profile is useful and should stay. It is the project-specific declarative configuration for repository, Project, identities,
routes, executor capacity, model defaults, snapshot settings, and scheduler policy. Before this refactor it was only partly consumed:
several values were read by humans/prompts or protected by tests, while prompts still duplicated the same choices in prose.

The realistic path is:

1. keep shared lifecycle semantics in Markdown;
2. keep project-specific values in `profile.yml`;
3. have repository automation compile normalized queue and PR-attention projections into the status artifact;
4. let each adapter consume those generated projections rather than interpreting raw Project state;
5. later extract the generic scripts and schema into a reusable repository/tool, with one profile per project.

This PR deliberately does not add a home-grown YAML parser to every dispatcher. The GitHub workflow currently passes the small
runtime identity map explicitly, while the profile remains the reviewed source of configuration. A later framework extraction can
compile YAML once in trusted automation and publish a versioned runtime JSON profile beside the queue snapshot.

## Scheduler setup

### ChatGPT

Create recurring dispatcher tasks in **Chat**, not **Work**. Work is reserved for a selected long-running investigation or
lifecycle turn, not the heartbeat.

Two supported clocks:

- budget clock: one compact Chat task hourly;
- 15-minute clock: four compact Chat tasks hourly at `:00`, `:15`, `:30`, and `:45` when the faster queue latency justifies the
  extra usage.

Both use the same short prompt and generated snapshot. Do not copy the detailed documentation into the task definition. The
15-minute worker-supervision rule still applies after a real dispatch; when using the budget clock, use explicit two 15-minute
follow-up checks for the selected worker and then return to hourly monitoring.

### Local Codex

- dispatcher/heartbeat: `gpt-5.6-luna`, low reasoning;
- normal implementation or verification worker: `gpt-5.6-terra`, medium reasoning;
- explicit difficult-task escalation: `gpt-5.6-sol`, high reasoning;
- xhigh is exceptional and requires a recorded reason.

Model selection is an operational cost/capability setting, not a lifecycle role. Existing branches, claims, review independence,
and proof requirements do not change when the model changes.

## Telemetry and token accounting

Every tick and worker should report start/end time, duration, model/reasoning, snapshot generation, selected candidate, and outcome.
Exact token counters should be included when the product/runtime exposes them. Otherwise report `null`; do not manufacture billing
precision. Duration and candidate counts are always available and are sufficient to find runaway no-op loops.

Recommended aggregation dimensions:

- scheduler/adapter and repository profile;
- no-op versus productive outcome;
- model and reasoning effort;
- number of full snapshots and targeted live reads;
- selected lifecycle status/executor;
- provider token counters when exposed;
- failures, rate limits, guarded conflicts, and duplicate-generation recoveries.

## Preserved behavior

The compact runtime contract retains universal PR intake, canonical issue/PR selection, exact-head validity, guarded Project
mutations, non-draft Review publication, independent-review rules, existing same-repository PR continuation, fork/Dependabot
boundaries, Codex Cloud supervised dispatch, 15/15/hourly monitoring, convergence checkpoints, truthful verification, human-only
privileged actions, and the explicit no-merge boundary.

## Baseline tag

Create the baseline annotated tag after verifying the target SHA:

```bash
git fetch origin develop
test "$(git rev-parse origin/develop)" = 790a990b9a7f1a51403ee43148a3c8daa3085233
git tag -a gptflow-v1 790a990b9a7f1a51403ee43148a3c8daa3085233 \
  -m "GPTFlow v1 before token-efficiency refactor"
git push origin refs/tags/gptflow-v1
```

The connected GitHub tool used for this audit can create branches and commits but does not expose tag-ref creation, so the tag is a
maintainer command rather than being silently approximated by a branch.
