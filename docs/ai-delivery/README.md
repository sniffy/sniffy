# AI-assisted delivery in Sniffy

This directory describes how Dmitry and ChatGPT plan, route, supervise, review, and verify work performed by ChatGPT, Codex
Cloud, Local Codex, an IDE-hosted coding agent, or a human. It is the control-plane documentation for delivery. Repository
engineering rules remain in the nearest applicable `AGENTS.md`.

## Terminology

| Term | Meaning | Sniffy example |
| --- | --- | --- |
| Role | A responsibility in the delivery process | supervisor, implementer, reviewer, verifier, operator |
| Executor | The product and environment performing a current action | ChatGPT, Codex Cloud, Local Codex, IDE agent, human |
| Implementer | Planned executor for the Implementation phase | Local Codex |
| Verifier | Planned executor coordinating the Verification phase | ChatGPT |
| Assignee | Concrete GitHub identity or human responsible now | `bedrin-gpt`, `bedrin-codex-local`, `bedrin` |
| Agent profile | A selectable provider-specific role/tool configuration | a future Copilot custom agent, only when intentionally added |
| Agent instance | One concrete running chat, task, process, or worker | one Codex CLI process and worktree for an issue |
| Repository instructions | Engineering policy applied by path | root or nested `AGENTS.md` |
| Runbook | Environment- or procedure-specific operating instructions | Codex Cloud setup, local worker, browser preview |
| Task prompt | One launch or continuation request | a rendered worker prompt for an issue |
| Skill | An optional reusable procedure | publishing reviewed PR screenshots |

Roles and executors are independent. ChatGPT is the default supervisor, but it may also implement, review, or verify a task.
Codex Cloud and Local Codex may perform the same implementer role in different environments. VS Code or IntelliJ IDEA is an
agent host; the selected Copilot, Codex, Junie, or other coding product is the executor.

## Lifecycle summary

Phases describe the kind of work; statuses describe whether the next action can run:

```text
Phase:  Draft -> Planning -> Implementation -> Review -> Verification (when required) -> Approval -> Done
Status: Ready | In progress | Blocked
```

Planning records both `Implementer` and `Verifier`. `Executor` materializes who performs the next current action, while
`Assignee` names the concrete identity or human. `Ready` may be pool-ready (unassigned worker queue) or directed-ready (for
example, `Planning / Ready / Human / bedrin` means Dmitry may review the plan).

See [`lifecycle.md`](lifecycle.md) for complete semantics and transitions.

## Control plane and execution plane

```text
Dmitry + ChatGPT decide outcome, risk, routing, and proof
                  |
                  v
 ChatGPT | Codex Cloud | Local Codex | IDE agent | human
                  |
                  v
     exact-head evidence and independent verification
                  |
                  v
       Dmitry authorizes privileged actions or merge
```

Dmitry owns product decisions, accepted risk, privileged repository/hosting operations, final acceptance, and merge
authorization. ChatGPT owns issue refinement, routing proposals, supervision, code review, verification coordination, and
clear handoff. An executor owns only the phase and proof explicitly routed to it.

## Sources of truth

Use this precedence when instructions differ:

1. Dmitry's explicit current decision, especially for product, risk, privileged operations, and merge.
2. The authoritative GitHub issue or pull request, including later comments that explicitly supersede older guidance.
3. The nearest applicable `AGENTS.md` for files being changed.
4. Root `AGENTS.md`.
5. The selected agent profile, executor runbook, or skill.
6. The concrete task prompt.

A task prompt should provide issue-specific inputs, not silently weaken repository policy. Historical retrospectives explain
why rules exist but do not override current policy.

## Documentation map

- [`lifecycle.md`](lifecycle.md) — phases, three statuses, planned routing fields, directed/pool Ready, and corrections.
- [`event-loop.md`](event-loop.md) — reusable clock/dispatcher/claim design, stateless ChatGPT ticks, and headless Codex CLI.
- [`routing.md`](routing.md) — choose Implementer, Verifier, current Executor, model, and human checkpoints.
- [`supervision.md`](supervision.md) — dispatch proof, monitoring, continuity, review convergence, and merge boundaries.
- [`verification.md`](verification.md) — implementer, reviewer, verifier, CI, browser/system proof, and capability routing.
- [`executors/chatgpt.md`](executors/chatgpt.md) — ChatGPT Project bootstrap, direct execution, scheduling, and limitations.
- [`executors/codex-cloud.md`](executors/codex-cloud.md) — Cloud routing, setup, credentials, publication, and troubleshooting.
- [`executors/codex-local.md`](executors/codex-local.md) — app-native and headless local worker topologies.
- [`executors/ide-agent.md`](executors/ide-agent.md) — generic VS Code/IntelliJ agent-host guidance.
- [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) — exact-head website artifact and Chromium verification.
- [`../retrospectives/`](../retrospectives/README.md) — historical incidents and durable lessons incorporated here.

The current ChatGPT event-loop adapter uses four hourly Scheduled Tasks offset by 15 minutes. Every occurrence starts a new,
stateless chat and performs at most one phase turn directly; Scheduled Task executions cannot create child Scheduled Tasks.
All durable context therefore lives in GitHub and repository-owned Markdown, not in ChatGPT Project memory or prior tick chats.

## Provider-specific files

- `.codex/` contains Codex environment scripts and launch prompts, not general repository policy.
- `.github/copilot-instructions.md` is a small Copilot adapter pointing to `AGENTS.md` and this directory.
- `.agents/skills/` contains optional reusable procedures that an executor invokes only when applicable.
- `.github/agents/` is intentionally absent. Add a selectable GitHub Copilot profile only for a real Copilot-specific role or
  tool set; do not use it as a generic role registry.
- IDE-specific rule directories should not duplicate `AGENTS.md`. Add them only for a proven client-specific gap.
