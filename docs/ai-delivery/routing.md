# Task routing

Dmitry and ChatGPT choose the primary executor and verification owner per task. Routing is a risk and evidence decision, not
an estimate based on line count or a permanent assignment of roles to products.

## Roles

A task may assign these roles independently:

- **Supervisor/router:** refines the issue, selects the executor, tracks delivery, and coordinates corrections. ChatGPT is
  the default.
- **Implementer:** changes code or documentation.
- **Test executor:** runs focused, integration, browser, platform, or manual checks.
- **Independent verifier:** checks the exact-head diff, review threads, evidence, and CI independently from the implementer.
- **Formal reviewer:** submits GitHub approval or Request Changes using an identity independent from the PR author.
- **Privileged operator:** changes DNS, custom domains, repository settings, secrets, rulesets, Pages environments, or other
  protected infrastructure.
- **Merger:** merges only after Dmitry's explicit instruction.

The same executor may hold several roles when independence is not required. When ChatGPT authors a PR through
`bedrin-gpt`, it may inspect and verify the work but cannot honestly formal-review that PR using the same identity.

## Executor matrix

| Executor | Prefer when | Avoid or escalate when |
| --- | --- | --- |
| ChatGPT | issue refinement, research, GitHub state, focused documentation/workflow edits, CI diagnosis, exact-head artifacts, browser verification, PR review | a long writable checkout, large builds, private/local services, hardware, or sustained interactive implementation is required |
| Codex Cloud | clean isolated branch, fully specified task, public dependencies, deterministic Cloud-available proof, useful parallelism | existing-branch surgery, persistent artifacts, privileged/local services, many unresolved risk axes, or repeated cold-start review loops |
| Local Codex | existing PR continuation, Docker/browsers/services, long builds, persistent caches/artifacts, OS-specific debugging, Remote visibility | the VM is offline, dispatcher/task lifecycle is not proven, or a small direct ChatGPT/Cloud task is cheaper |
| IDE agent | a human is actively steering code in VS Code or IntelliJ and wants local context or interactive edits | unattended ownership, independent review, or reproducible remote publication is required but not configured |
| Human | unresolved product/architecture decisions, privileged operations, subjective acceptance, credentials/secrets, destructive migration, merge | routine bounded implementation that another executor can prove safely |

ChatGPT may directly implement and test work when its current tools can obtain the exact source, edit safely, run or inspect
required proof, and publish a reviewable PR. It should delegate rather than imitate unavailable capabilities.

## Risk-axis routing

Count independent decisions and proof obligations, not changed lines. Relevant axes include:

- public APIs or published artifacts;
- global state, concurrency, ownership, cleanup, and failure composition;
- several JDK, framework, engine, OS, or architecture versions;
- module/reactor placement and same-artifact compatibility;
- first-of-kind architecture or test patterns;
- browser, visual, local-service, hardware, credential, or network proof;
- dependency/security compatibility;
- migration and rollback;
- Git history or existing PR constraints;
- publication and review identity.

Several interacting axes require a read-only design/proof preflight before implementation. Cloud may still be appropriate
when the issue resolves every axis and provides a complete proof matrix. Local execution does not fix an ambiguous issue.

## Default routing heuristics

1. Ask whether ChatGPT can complete and independently verify the task with available GitHub, sandbox, artifact, and browser
   tools. Use ChatGPT directly when the answer is yes and the change remains focused.
2. Use Codex Cloud for a clean, isolated, fully specified implementation with deterministic Cloud proof.
3. Use Local Codex for an existing branch/PR, persistent or privileged development tooling, browser/service-heavy work, or
   repeated Cloud cold-start friction.
4. Use an IDE agent when a human intentionally wants interactive pair programming; do not infer unattended ownership from
   the IDE alone.
5. Keep privileged operations and unresolved product/risk decisions human-owned.
6. After two substantive review/fix rounds, or immediately after an architecture reversal, re-baseline the issue and route
   the unresolved work deliberately instead of stacking another narrow prompt.

## Codex model selection

Model cost is an engineering constraint. Explicit issue/project routing overrides defaults.

- **Luna / low reasoning:** mechanical transformations with exact files and edits, no remaining design, compatibility,
  failure-analysis, or proof decision.
- **Terra / medium reasoning:** ordinary well-scoped fixes, tests, documentation, CI follow-ups, dependency updates, and
  localized refactors. This is the default Local Codex route.
- **Sol / high reasoning:** architecture, lifecycle/concurrency, cross-version Java, subtle performance, first-of-kind
  patterns, or an unresolved part that remained after a focused Terra attempt.

Prefer staged escalation. Do not restart an entire task on the strongest model because one check failed. Record non-obvious
escalations in the issue or PR for later cost calibration.

## GitHub Project fields

Use the Project `Executor` field as the source of truth for the primary executor. GitHub assignees are technical identities,
not executor products. Existing `agent:local` and `agent:cloud` labels are transitional metadata and must not override the
Project field.

Recommended values:

- `ChatGPT`
- `Codex Cloud`
- `Local Codex`
- `IDE Agent` or a named product when operationally relevant
- `Human`

A separate `Verification owner` field is useful with values such as `ChatGPT`, `Executor`, `Human`, or `Mixed`. The current
repository does not require automation to create these fields; document the chosen route in the issue even when the Project
does not yet expose them.

## Issue routing block

```md
## Delivery routing

- Supervisor: ChatGPT
- Primary executor: <ChatGPT | Codex Cloud | Local Codex | IDE Agent | Human>
- Verification owner: <ChatGPT | Executor | Human | Mixed>
- Formal reviewer: <independent identity or human>
- Privileged operator: <none or named human role>
- GitHub author identity: <account>
- Base/head and existing PR constraints: <details>
- Rationale: <environment, risk axes, evidence, and cost>
```
