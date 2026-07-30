# AI-assisted delivery in Sniffy

This directory describes how Dmitry and ChatGPT plan, route, supervise, review, and verify work performed by ChatGPT, Codex
Cloud, Local Codex, an IDE-hosted coding agent, automation such as Dependabot, or a human. It is the control-plane
documentation for delivery. Repository engineering rules remain in the nearest applicable `AGENTS.md`.

The current Sniffy routing knobs live in [`profile.yml`](profile.yml). Shared semantics belong in Markdown; the profile selects
current executors, identities, Local Codex model/effort, control-log rotation thresholds, and default routes without redefining
the lifecycle.

## Terminology

| Term | Meaning | Sniffy example |
| --- | --- | --- |
| Work item | An issue or pull request represented in the delivery Project | feature issue, Dependabot PR |
| Role | A responsibility in the delivery process | supervisor, implementer, reviewer, verifier, operator |
| Executor | The product and environment performing a current action | ChatGPT, Codex Cloud, Local Codex, human |
| Implementer | Planned executor or source author for Implementation | Local Codex, Dependabot |
| Verifier | Planned executor coordinating Verification | ChatGPT |
| Assignee | Concrete GitHub identity or human responsible now | `bedrin-gpt`, `bedrin-codex-local`, `bedrin` |
| Agent instance | One concrete running chat, task, process, or worker | one Codex task and worktree |
| Repository instructions | Engineering policy applied by path | root or nested `AGENTS.md` |
| Runbook | Environment- or procedure-specific operating instructions | Codex Cloud setup, local worker |
| Task prompt | One launch or continuation request | a rendered worker prompt for an issue |
| Control command | Guarded provider-neutral ProjectV2 transition | one `delivery-control/v1` JSON comment |

Roles and executors are independent. ChatGPT is the default supervisor, but it may also implement, review, or verify a task.
Codex Cloud and Local Codex may perform the same implementer role in different environments. Model selection is a separate
configuration axis: Cloud is provider-managed, while the current Local Codex profile uses Sol with extra-high reasoning.

## Lifecycle summary

`Status` describes the kind of work; `Execution` describes whether the next action can run:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification (when required) -> Approval -> Done
Execution: Ready | In progress | Blocked
```

Planning records both `Implementer` and `Verifier`. `Executor` materializes who performs the next current action, while
`Assignee` names the concrete identity or human. An external pull request may enter directly at `Review / Ready` when its
implementation already exists and its scope is clear.

A worker owns one lifecycle turn. A corrected implementation that returns to Review with substantive blockers does not
immediately receive another narrow patch request: ChatGPT first performs the convergence checkpoint in [`routing.md`](routing.md)
and [`supervision.md`](supervision.md).

## Control plane and execution plane

```text
Dmitry + ChatGPT decide outcome, risk, routing, and proof
                  |
                  v
   guarded control command + exact worker dispatch
                  |
                  v
 ChatGPT | Codex Cloud | Local Codex | automation | human
                  |
                  v
      exact-head evidence and independent verification
                  |
                  v
       Dmitry authorizes privileged actions or merge
```

All configured executors use the same central control-issue protocol for ProjectV2 transitions. The target issue/PR conversation
is reserved for human-useful plans, reviews, evidence, blockers, and handoffs. See [`control-plane.md`](control-plane.md).

Dmitry owns product decisions, accepted risk, privileged repository/hosting operations, final acceptance, and merge
authorization. ChatGPT owns issue refinement, external-PR intake, routing proposals, supervision, code review, verification
coordination, control-log rotation, and clear handoff. An executor owns only the lifecycle turn and proof explicitly routed to it.

## Sources of truth

Use this precedence when instructions differ:

1. Dmitry's explicit current decision, especially for product, risk, privileged operations, and merge.
2. The authoritative GitHub issue or pull request, including later comments that explicitly supersede older guidance.
3. The nearest applicable `AGENTS.md` for files being changed.
4. Root `AGENTS.md`.
5. This directory and [`profile.yml`](profile.yml).
6. The selected executor runbook or skill.
7. The concrete task prompt.

Historical tick chats, Codex conversations, and Actions logs are telemetry and evidence, not the only copy of durable delivery
state.

## Documentation map

- [`profile.yml`](profile.yml) — current Sniffy Project, field, identity, routing, Local Codex, and rotation configuration.
- [`lifecycle.md`](lifecycle.md) — lifecycle statuses, execution states, planned routing fields, and corrections.
- [`control-plane.md`](control-plane.md) — one guarded mutation protocol, control issue, concurrency, reactions, and rotation.
- [`event-loop.md`](event-loop.md) — reusable clock/dispatcher/intake/claim design and provider adapters.
- [`.chatgpt/scheduled-task-prompt.md`](../../.chatgpt/scheduled-task-prompt.md) — exact prompt for four ChatGPT Scheduled Tasks.
- [`pull-request-intake.md`](pull-request-intake.md) — external PRs, Dependabot review, rebase, verification, and replacement flow.
- [`chat-retention.md`](chat-retention.md) — persistent Scheduled Task chats, compact outputs, and bounded manual rotation.
- [`routing.md`](routing.md) — role/executor/model separation, routing heuristics, and convergence checkpoint.
- [`supervision.md`](supervision.md) — dispatch proof, worker monitoring, review convergence, and merge boundaries.
- [`verification.md`](verification.md) — implementer, reviewer, verifier, CI, browser/system proof, and capability routing.
- [`executors/chatgpt.md`](executors/chatgpt.md) — ChatGPT direct execution, scheduling, and limitations.
- [`executors/codex-cloud.md`](executors/codex-cloud.md) — Cloud routing, setup, credentials, publication, and troubleshooting.
- [`executors/codex-local.md`](executors/codex-local.md) — app-native and headless Local Codex worker topologies.
- [`executors/ide-agent.md`](executors/ide-agent.md) — generic VS Code/IntelliJ agent-host guidance.
- [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) — exact-head website artifact and Chromium verification.
- [`../retrospectives/`](../retrospectives/README.md) — historical incidents and durable lessons incorporated here.

## Current adapters

The ChatGPT clock uses four hourly Scheduled Tasks offset by 15 minutes. Product testing on 2026-07-30 showed that recurrences
append to each task's defining chat rather than reliably creating a new chat. Every occurrence must therefore behave statelessly
by instruction and re-read GitHub/repository state, while the four defining chats are retained and periodically replaced as a
maintenance action. No Scheduled Task may rely on previous chat turns as state.

A no-op tick creates no GitHub mutation. A productive tick performs at most one lifecycle turn directly or makes one supported
external dispatch. Scheduled ChatGPT ticks cannot create child Scheduled Tasks.

Dependabot PRs are first-class Project items rather than shadow issues. GitHub's built-in auto-add workflow discovers newly
created or updated dependency PRs; the event loop backfills existing untracked PRs, initializes Review fields through the central
control protocol, verifies the author, and routes any required compatibility implementation to a linked agent-owned issue/PR.

## Provider-specific files

- `.chatgpt/` contains copy-paste Scheduled Task prompts, not durable delivery state.
- `.codex/` contains Codex environment scripts and launch prompts, not general repository policy.
- `.github/workflows/delivery-control.yml` and `.github/scripts/delivery-control.js` implement the provider-neutral mutation bridge.
- `.github/copilot-instructions.md` is a small Copilot adapter pointing to `AGENTS.md` and this directory.
- `.agents/skills/` contains optional reusable procedures that an executor invokes only when applicable.
- IDE-specific rule directories should not duplicate `AGENTS.md`; add them only for a proven client-specific gap.
