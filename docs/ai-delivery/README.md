# AI-assisted delivery in Sniffy

This directory describes how Dmitry and ChatGPT route, supervise, and verify work performed by ChatGPT, Codex Cloud, the
local Codex worker, an IDE-hosted coding agent, or a human. It is the control-plane documentation for delivery. Repository
engineering rules remain in the nearest applicable `AGENTS.md`.

## Terminology

| Term | Meaning | Sniffy example |
| --- | --- | --- |
| Role | A responsibility in the delivery process | supervisor, implementer, test executor, verifier, reviewer, operator |
| Executor | The product and environment performing work | ChatGPT, Codex Cloud, Local Codex, IDE agent, human |
| Agent profile | A selectable provider-specific role/tool configuration | a future Copilot custom agent, only when intentionally added |
| Agent instance | One concrete running chat, task, or worker | the Local Codex worker chat for one issue |
| Technical identity | The account used to write to GitHub | `bedrin-gpt`, `bedrin-codex-local` |
| Repository instructions | Engineering policy applied by path | root or nested `AGENTS.md` |
| Runbook | Environment- or procedure-specific operating instructions | Codex Cloud setup, local VM setup, browser preview |
| Task prompt | One launch or continuation request | a rendered local worker prompt for an issue |
| Skill | An optional reusable procedure | publishing reviewed PR screenshots |

Roles and executors are independent. ChatGPT is the default supervisor, but it may also implement, test, or verify a task.
Codex Cloud and Local Codex may perform the same implementer role in different environments. VS Code or IntelliJ IDEA is
an agent host; the selected Copilot, Codex, Junie, or other coding product is the executor.

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

Dmitry owns product decisions, accepted risk, privileged repository/hosting operations, and merge authorization. ChatGPT
owns issue refinement, executor selection, supervision, independent review, and clear handoff. An executor owns only the
implementation and proof explicitly routed to it.

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

- [`routing.md`](routing.md) — choose the primary executor, verification owner, model, and human checkpoints.
- [`supervision.md`](supervision.md) — delivery states, dispatch proof, 15/15/hourly monitoring, review convergence, and
  merge boundaries.
- [`verification.md`](verification.md) — proof ownership, exact-head evidence, CI/artifact/browser review, and completion.
- [`executors/chatgpt.md`](executors/chatgpt.md) — ChatGPT Project bootstrap, direct execution, review, and limitations.
- [`executors/codex-cloud.md`](executors/codex-cloud.md) — Cloud routing, setup, credentials, publication, and troubleshooting.
- [`executors/codex-local.md`](executors/codex-local.md) — app-native local dispatcher/worker lifecycle and VM runbook.
- [`executors/ide-agent.md`](executors/ide-agent.md) — generic VS Code/IntelliJ agent-host guidance.
- [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) — exact-head website artifact and Chromium verification.
- [`../retrospectives/`](../retrospectives/README.md) — historical incidents and the durable lessons incorporated here.

## Provider-specific files

- `.codex/` contains Codex environment scripts and launch prompts, not general repository policy.
- `.github/copilot-instructions.md` is a small Copilot adapter pointing to `AGENTS.md` and this directory.
- `.agents/skills/` contains optional reusable procedures that an executor invokes only when applicable.
- `.github/agents/` is intentionally absent. Add a selectable GitHub Copilot custom profile only when Sniffy actually wants
  a Copilot-specific role with a distinct prompt or tool set; do not use it as a generic role registry.
- IDE-specific rule directories should not duplicate `AGENTS.md`. Add them only for a proven client-specific gap.
