# AI-assisted delivery in Sniffy

This directory describes how Dmitry and ChatGPT plan, canonicalize, route, supervise, review, and verify work performed by ChatGPT,
Codex Cloud, Local Codex, an IDE-hosted coding agent, automation such as Dependabot, an external contributor, or a human. It is the
control-plane documentation for delivery. Repository engineering rules remain in the nearest applicable `AGENTS.md`.

The current Sniffy routing knobs live in [`profile.yml`](profile.yml). Shared semantics belong in Markdown; the profile selects
current executors, identities, Local Codex model/effort, control-log rotation thresholds, default routes, and universal PR intake
behavior without redefining the lifecycle.

## Terminology

| Term | Meaning | Sniffy example |
| --- | --- | --- |
| Work item | An issue or pull request represented in the delivery Project | feature issue, standalone PR |
| Canonical item | The one Project item that owns lifecycle state for one published change | linked issue or standalone PR |
| Implementation evidence | Exact PR branch/head, diff, CI, and review conversation linked to the canonical item | PR #763 at `6d45aff...` |
| Role | A responsibility in the delivery process | supervisor, implementer, reviewer, verifier, operator |
| Executor | The product and environment performing a current action | ChatGPT, Codex Cloud, Local Codex, human |
| Implementer | Optional planned executor for a future Implementation turn | Local Codex; empty for an already-published PR |
| Verifier | Planned executor coordinating Verification | ChatGPT |
| Assignee | Concrete GitHub identity or human responsible now | `bedrin-gpt`, `bedrin-codex-local`, `bedrin` |
| Agent instance | One concrete running chat, task, process, or worker | one Codex task and worktree |
| Repository instructions | Engineering policy applied by path | root or nested `AGENTS.md` |
| Runbook | Environment- or procedure-specific operating instructions | Codex Cloud setup, local worker |
| Task prompt | One launch or continuation request | a rendered worker prompt for an issue or PR |
| Control command | Guarded provider-neutral ProjectV2 transition | one `delivery-control/v1` JSON comment |

Roles, executors, identities, and PR provenance are independent. ChatGPT is the default supervisor, but it may also implement,
review, or verify a task. Codex Cloud and Local Codex may perform the same implementer role in different environments. Model
selection is a separate configuration axis: Cloud is provider-managed, while the current Local Codex profile uses Sol with
extra-high reasoning.

PR authorship is provenance, not a routing decision. A PR created by Dmitry, ChatGPT, an IDE agent, Dependabot, an external
contributor, or a future bot is eligible for intake. Its author controls formal-review independence and branch-correction policy,
but intake does not copy that identity into `Implementer`.

## Issue and PR relationship

Issues and pull requests are both first-class Project items, but one item owns lifecycle state for one published change:

```text
one formal closing issue -> issue is canonical; PR is implementation evidence
no formal closing issue  -> PR is canonical
several closing issues   -> PR is canonical in Planning
```

This lets Dmitry push a ready PR from IntelliJ without first creating an issue, while preserving an existing issue as the durable
requirements item when one already exists. The event loop must not review both an issue and its PR as duplicate work.

## Lifecycle summary

`Status` describes the kind of work; `Execution` describes whether the next action can run:

```text
Status:    Draft -> Planning -> Implementation -> Review -> Verification (when required) -> Approval -> Done
Execution: Ready | In progress | Blocked
```

Planning records `Implementer` when a future Implementation turn is needed and records `Verifier` when Verification may be needed.
`Executor` materializes who performs the next current action, while `Assignee` names the concrete identity or human. A non-draft
PR may enter directly at `Review / Ready` with an empty Implementer when its implementation already exists and its scope is clear.
A multi-issue PR enters Planning first.

A worker owns one lifecycle turn. A corrected implementation that returns to Review with substantive blockers does not immediately
receive another narrow patch request: ChatGPT first performs the convergence checkpoint in [`routing.md`](routing.md) and
[`supervision.md`](supervision.md).

## Control plane and execution plane

```text
Dmitry + ChatGPT decide canonical item, outcome, risk, routing, and proof
                              |
                              v
             guarded control command + exact worker dispatch
                              |
                              v
        ChatGPT | Codex Cloud | Local Codex | automation | human
                              |
                              v
          exact PR-head evidence and independent verification
                              |
                              v
               Dmitry authorizes privileged actions or merge
```

All configured executors use the same central control-issue protocol for ProjectV2 transitions. The canonical issue/PR conversation
is reserved for human-useful plans, reviews, evidence, blockers, and handoffs. Omitted Project fields remain unchanged, and an empty
optional field is valid state rather than a reason to invent a new select option. See [`control-plane.md`](control-plane.md).

Dmitry owns product decisions, accepted risk, privileged repository/hosting operations, final acceptance, and merge authorization.
ChatGPT owns issue refinement, universal PR intake/canonicalization, routing proposals, supervision, code review, verification
coordination, control-log rotation, and clear handoff. An executor owns only the lifecycle turn and proof explicitly routed to it.

## Sources of truth

Use this precedence when instructions differ:

1. Dmitry's explicit current decision, especially for product, risk, privileged operations, and merge.
2. The authoritative canonical GitHub issue or pull request, including later comments that explicitly supersede older guidance.
3. The linked implementation PR exact branch/head, reviews, CI, and artifacts.
4. The nearest applicable `AGENTS.md` for files being changed.
5. Root `AGENTS.md`.
6. This directory and [`profile.yml`](profile.yml).
7. The selected executor runbook or skill.
8. The concrete task prompt.

Historical tick chats, Codex conversations, and Actions logs are telemetry and evidence, not the only copy of durable delivery
state.

## Documentation map

- [`profile.yml`](profile.yml) — current Sniffy Project, field, identity, routing, Local Codex, intake, and rotation configuration.
- [`lifecycle.md`](lifecycle.md) — statuses, execution states, canonical issue/PR rules, optional planned routing, and corrections.
- [`control-plane.md`](control-plane.md) — one guarded mutation protocol, optional/omitted fields, control issue, concurrency,
  reactions, and rotation.
- [`event-loop.md`](event-loop.md) — reusable clock, universal PR intake/canonicalization, dispatcher, claims, and provider adapters.
- [`.chatgpt/scheduled-task-prompt.md`](../../.chatgpt/scheduled-task-prompt.md) — exact prompt for four ChatGPT Scheduled Tasks.
- [`pull-request-intake.md`](pull-request-intake.md) — arbitrary PR discovery, canonical issue/PR selection, drafts, forks,
  Dependabot, review, correction, and completion.
- [`chat-retention.md`](chat-retention.md) — persistent Scheduled Task chats, compact outputs, and bounded manual rotation.
- [`routing.md`](routing.md) — role/executor/model/provenance separation, fresh versus existing-PR routing, and convergence.
- [`supervision.md`](supervision.md) — canonical handoff, dispatch proof, worker/PR monitoring, review convergence, and merge boundaries.
- [`verification.md`](verification.md) — implementer, reviewer, verifier, CI, browser/system proof, and capability routing.
- [`executors/chatgpt.md`](executors/chatgpt.md) — ChatGPT direct execution, scheduling, and limitations.
- [`executors/codex-cloud.md`](executors/codex-cloud.md) — Cloud fresh-work routing, setup, credentials, publication, and troubleshooting.
- [`executors/codex-local.md`](executors/codex-local.md) — app-native/headless workers and adopted existing-PR continuation.
- [`executors/ide-agent.md`](executors/ide-agent.md) — generic VS Code/IntelliJ agent-host guidance.
- [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) — exact-head website artifact and Chromium verification.
- [`../retrospectives/`](../retrospectives/README.md) — historical incidents and durable lessons incorporated here.

## Current adapters

The ChatGPT clock uses four hourly Scheduled Tasks offset by 15 minutes. Product testing on 2026-07-30 showed that recurrences
append to each task's defining chat rather than reliably creating a new chat. Every occurrence must therefore behave statelessly by
instruction and re-read GitHub/repository state, while the four defining chats are retained and periodically replaced as a
maintenance action. No Scheduled Task may rely on previous chat turns as state.

A no-op tick creates no GitHub mutation. A productive tick performs at most one canonicalization/reconciliation or lifecycle turn
directly, or makes one supported external dispatch. Scheduled ChatGPT ticks cannot create child Scheduled Tasks.

Every open PR targeting `develop` is scanned. A single linked issue remains canonical; a standalone PR becomes the Project item;
a multi-issue PR enters Planning. Same-repository corrections can be adopted by ChatGPT or Local Codex on the exact existing
branch/PR. Forks and managed-bot branches use contributor/bot operations or a linked replacement task.

GitHub Project auto-add remains useful for issues created by agents and dependency PR discovery, but it does not replace
canonicalization. Project 2 currently retains a `Dependabot` Implementer option for manual/historical classification; normal
intake does not select it.

## Provider-specific files

- `.chatgpt/` contains copy-paste Scheduled Task prompts, not durable delivery state.
- `.codex/` contains Codex environment scripts and launch prompts, not general repository policy.
- `.github/workflows/delivery-control.yml` and `.github/scripts/delivery-control.js` implement the provider-neutral mutation bridge.
- `.github/scripts/delivery-policy.test.js` protects the canonical universal-intake and adopted-continuation prompt contract.
- `.github/copilot-instructions.md` is a small Copilot adapter pointing to `AGENTS.md` and this directory.
- `.agents/skills/` contains optional reusable procedures that an executor invokes only when applicable.
- IDE-specific rule directories should not duplicate `AGENTS.md`; add them only for a proven client-specific gap.
