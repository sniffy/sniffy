# Task routing

Dmitry and ChatGPT choose the planned `Implementer` and `Verifier` during Planning. Routing is a risk, capability, cost, and
evidence decision, not an estimate based on changed lines or a permanent assignment of roles to products.

## Roles

A task may assign these responsibilities independently:

- **Supervisor/router:** refines the issue, proposes routing, tracks delivery, and coordinates corrections. ChatGPT is the
  default.
- **Implementer:** changes code or documentation and proves the change in its own environment.
- **Reviewer:** inspects the complete exact-head code/configuration diff, scope, tests, review threads, and CI evidence.
  ChatGPT is the default unless independence or capability requires another reviewer.
- **Verifier:** validates the observable result against the issue and discussion in a representative environment.
- **Formal reviewer:** submits GitHub approval or Request Changes using an identity independent from the PR author.
- **Privileged operator:** changes DNS, custom domains, repository settings, secrets, rulesets, Pages environments, or other
  protected infrastructure.
- **Merger:** merges only after Dmitry's explicit instruction.

The same executor may hold several roles when independence is not required. When ChatGPT authors a PR through
`bedrin-gpt`, it may self-check and verify the work but cannot honestly formal-review that PR using the same identity.

## Routing fields

Planning records future routes:

- `Implementer` — executor selected for Implementation.
- `Verifier` — executor coordinating Verification.

The current lifecycle materializes:

- `Executor` — product/runtime responsible for the next current action.
- `Assignee` — concrete identity or human responsible now.
- `Worker reference` — concrete chat, process, task, branch/worktree, or PR ownership after claim.

`Implementer` and `Verifier` must remain visible before their phases begin. `Executor` is intentionally denormalized so every
dispatcher can use one query such as `Status = Ready AND Executor = Local Codex`.

## Executor matrix

| Executor | Prefer when | Avoid or escalate when |
| --- | --- | --- |
| ChatGPT | issue refinement, GitHub state, focused repository edits, CI diagnosis, exact-head artifacts, code review, browser verification from existing source/artifacts | a long writable checkout, unavailable dependencies, package downloads, private/local services, hardware, or sustained implementation is required |
| Codex Cloud | clean isolated branch, fully specified task, dependencies available through setup/cache, deterministic Cloud proof, useful parallelism | existing-branch surgery, persistent artifacts, privileged/local services, unresolved risk axes, or repeated cold-start loops |
| Local Codex app | persistent environment plus app-owned worktrees/conversations, interactive browser/service debugging, Remote visibility | app lifecycle or nested task creation is not proven, or headless execution is simpler |
| Local Codex CLI | unattended Linux worker, systemd/cron, persistent caches, Docker/services/browsers, existing PR continuation, several isolated worktrees | host credentials or network are unsafe, sandbox policy is unclear, or app conversation visibility is required |
| IDE agent | a human actively steers code in VS Code or IntelliJ and wants local context or interactive edits | unattended ownership, independent review, or reproducible publication is required but not configured |
| Human | unresolved product/architecture decisions, privileged operations, subjective acceptance, credentials/secrets, destructive migration, merge | routine bounded implementation another executor can prove safely |

## Capability preflight

Do not infer capabilities from a product name. Before routing a proof obligation, confirm the exact current environment can:

- obtain the required source and exact revision;
- use already-installed or downloadable dependencies;
- reach required network endpoints;
- start required runtimes, containers, browsers, or services;
- access credentials without exposing them;
- publish the expected branch/PR/evidence;
- provide independent review identity when required.

For the current Sniffy ChatGPT executor, GitHub connector access and sandbox execution are separate capabilities. ChatGPT may
be able to inspect GitHub, run Java or serve an existing artifact, yet still be unable to use sandbox outbound internet or
download Maven/npm dependencies. Route a source build or integration environment elsewhere rather than describing an
artifact inspection as a build.

Record important capability assumptions in the issue. Smoke-test platform composition such as nested Scheduled Task creation
or app-owned child automations before relying on it.

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

1. Ask whether ChatGPT can complete the current action truthfully with its available connectors, sandbox, source, dependencies,
   artifacts, browser, and identity. Use it directly only when the required evidence is actually available.
2. Use Codex Cloud for a clean, isolated, fully specified implementation with deterministic Cloud-available proof.
3. Use headless Local Codex for unattended persistent builds, services, browsers, Docker, existing PRs, or worker-pool scaling.
4. Use app-native Local Codex when app-owned conversation/worktree lifecycle or interactive Remote visibility adds real value.
5. Use an IDE agent when a human intentionally wants pair programming; do not infer unattended ownership from the IDE alone.
6. Keep privileged operations and unresolved product/risk decisions human-owned.
7. After two substantive review/fix rounds, or immediately after an architecture reversal, re-baseline and deliberately
   reroute instead of stacking another narrow prompt.

## Codex model selection

Model cost is an engineering constraint. Explicit issue/project routing overrides defaults.

- **Luna / low reasoning:** mechanical transformations with exact files and edits and no remaining design/proof decision.
- **Terra / medium reasoning:** ordinary well-scoped fixes, tests, documentation, CI follow-ups, dependency updates, and
  localized refactors. This is the default Local Codex route.
- **Sol / high reasoning:** architecture, lifecycle/concurrency, cross-version Java, subtle performance, first-of-kind
  patterns, or an unresolved part remaining after a focused Terra attempt.

Prefer staged escalation. Do not restart an entire task on the strongest model because one check failed. Record non-obvious
escalations for later cost calibration.

## GitHub Project fields

Recommended lifecycle and routing fields:

- `Phase`: Draft, Planning, Implementation, Review, Verification, Approval, Done.
- `Status`: Ready, In progress, Blocked.
- `Implementer`: planned implementation executor.
- `Verifier`: planned verification executor.
- `Executor`: current action executor.
- `Assignee`: use GitHub assignment as the concrete current identity/human.
- `Correction rounds`: substantive review/verification returns, excluding infrastructure noise.
- `Blocked reason`: exact external blocker and next action.

GitHub assignees are technical identities, not executor products. Existing `agent:local` and `agent:cloud` labels are
transitional metadata and must not override Project routing fields.

## Issue routing block

```md
## Delivery routing

- Supervisor: ChatGPT
- Implementer: <ChatGPT | Codex Cloud | Local Codex app | Local Codex CLI | IDE Agent | Human>
- Verifier: <ChatGPT | Codex Cloud | Local Codex app | Local Codex CLI | Human | Mixed>
- Default reviewer: ChatGPT
- Formal reviewer: <independent identity or human>
- Privileged operator: <none or named human>
- GitHub author identity: <account>
- Capability assumptions: <source, dependencies, network, services, browser, credentials>
- Base/head and existing PR constraints: <details>
- Rationale: <risk axes, evidence, latency, and cost>
```

See [`lifecycle.md`](lifecycle.md) for transition invariants and [`event-loop.md`](event-loop.md) for dispatch fields and
claim behavior.