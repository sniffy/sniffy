# Task routing

Routing is a capability, risk, continuity, evidence, latency, and cost decision. Role, executor, model, GitHub identity, and PR
provenance are separate axes.

## Axes

- **Role:** supervisor, implementer, reviewer, verifier, privileged operator, merger.
- **Executor:** ChatGPT, Codex Cloud, Local Codex, IDE agent, automation, human.
- **Model/profile:** provider-managed or an explicit Local Codex model/reasoning pair.
- **Identity:** account performing comments, commits, or formal reviews.
- **Provenance:** who created the branch/PR and whether it is same-repository, fork, or managed automation.

PR authorship does not populate Implementer automatically. A strong model does not grant authority, branch ownership, credentials,
review independence, or permission to merge.

## Current model profiles

Model selection is proportional to work, not fixed to the strongest model:

| Purpose | Default |
| --- | --- |
| App-native Local queue heartbeat/dispatcher | `gpt-5.6-luna` / low |
| Repository-owned CLI dispatcher | deterministic shell / no model |
| CLI `economy` worker | `gpt-5.6-luna` / low |
| CLI `balanced` worker (default) | `gpt-5.6-terra` / medium |
| CLI `frontier` worker | `gpt-5.6-sol` / high, with a recorded reason |
| Exceptional unresolved frontier task | xhigh only with a recorded reason |
| Codex Cloud | provider-managed unless the product exposes a verified selection |
| ChatGPT scheduler | provider-managed Chat surface; never Work merely for heartbeat |

Escalation preserves the same canonical item, claim generation, branch, PR, proof, and no-merge boundary. It is not permission to
restart from develop or open a duplicate PR.

## Executor matrix

| Executor | Prefer | Avoid/escalate |
| --- | --- | --- |
| ChatGPT | canonicalization, Planning, GitHub state, focused edits, CI diagnosis, exact-head Review, connector/artifact/browser proof | long writable checkout/services, unavailable dependencies, complex existing branch, formal self-review |
| Codex Cloud | bounded well-specified fresh work with Cloud-available proof and useful parallelism | arbitrary existing-PR adoption, private/local state, unresolved architecture, persistent services, repeated non-convergence |
| Local Codex | complex/cross-version architecture, concurrency/lifecycle, persistent caches/services/Docker/browser, exact same-repository existing-PR continuation, representative local Verification | fork/Dependabot adoption, unsafe host policy, unresolved product/authority decision |
| IDE agent | deliberate interactive pair programming | unattended ownership or independent reproducible publication is required but unconfigured |
| Human | product/risk/privilege decisions, credentials, subjective acceptance, merge | routine bounded work another executor can prove safely |

Dependabot supplies a published implementation. ChatGPT reviews/monitors bot operations; a selected Verifier proves outcomes.

## Capability preflight

Before routing, verify the selected environment can obtain the canonical item/source/exact revision; use dependencies and required
network/services/browser/hardware; access credentials safely; preserve the intended branch/PR; publish commits/PR/artifacts/evidence;
and satisfy independent-review requirements.

For existing PR continuation additionally verify same-repository ownership, exact remote branch/head, push permission, no competing
owner, and an explicit continuation/adoption route. Never infer capability from a product/model name.

## Default implementation route

1. Require a deliberate Implementer before entering a new Implementation turn.
2. Classify fresh work versus exact existing-PR continuation.
3. Use ChatGPT directly only when it can truthfully edit, test, inspect, publish, and verify the focused change.
4. Prefer Codex Cloud for bounded well-specified fresh work.
5. Prefer Local Codex for complexity, persistent environments, cross-version proof, non-convergence, or same-repository existing PRs.
6. Route product/risk/privilege/credential decisions to Dmitry.
7. For the CLI adapter, record `economy`, `balanced`, or `frontier` on the authoritative item before claim; do not use Sol merely
   because the worker is Local Codex. The model-free dispatcher stores that exact selection in the generation manifest.

Fork and Dependabot branches remain contributor/bot-owned. Use contributor-facing findings, bot commands, or a deliberately linked
internal replacement task rather than direct autonomous adoption.

## Existing PR continuity

An adopted continuation preserves the canonical item and exact same-repository PR. The Project route deliberately selects the new
Implementer/Executor and Worker reference remains pinned to PR/branch/head. The worker fetches and updates that branch without
reset, rebase, force-push, discarded useful commits, replacement PR, or silent scope narrowing. After publication, the same
canonical item returns to `Review / Ready / ChatGPT` at the new exact head.

Changing executor or model never means discarding published work.

## Risk axes and escalation

Count independent decisions/proof obligations, not changed lines: public API/artifact, global state/concurrency/cleanup/failure
composition, multiple JDK/framework/OS variants, module placement, first-of-kind architecture/test, browser/service/hardware/network,
dependency/security compatibility, migration/rollback, branch/provenance, publication/review identity.

Several interacting axes require a read-only design/proof preflight. Sol/high may help after the preflight; it must not invent
missing requirements or maintainer decisions.

## Review convergence checkpoint

When Review returns substantive changes, corrected work returns, and the next complete Review still has substantive blockers, stop
serial narrow patching. Re-evaluate issue precision, architecture, acceptance/proof matrix, executor context/capability, branch
continuity, and whether another patch would preserve a flawed direction.

Choose one:

- continue the same executor with one coherent superseding correction;
- preserve/adopt the existing PR and escalate to Local Codex, with Terra or Sol selected deliberately;
- return to `Planning / Ready / ChatGPT` for re-baselining;
- use `Blocked / Human` for one exact Dmitry decision/action.

Infrastructure noise is not a substantive correction round.

## Editable profile values

`profile.yml` contains identities, scheduler surface/cadence, app and CLI model profiles, capacity, fresh/continuation
executor defaults, intake/canonicalization settings, convergence policy, supervision cadence, telemetry fields, and control/status
settings. Update shared policy only when lifecycle meaning, claim safety, canonicalization, review independence, verification, or
merge authority changes.
