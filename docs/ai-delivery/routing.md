# Task routing

Dmitry and ChatGPT choose planned roles during Planning or when Review discovers new implementation work. Routing is a risk,
capability, evidence, latency, continuity, and cost decision. Current Sniffy defaults are editable in [`profile.yml`](profile.yml);
this document defines the semantics and decision process.

## Separate axes: role, executor, model, identity, provenance

Do not collapse these independent choices:

- **Role** — supervisor, implementer, reviewer, verifier, privileged operator, or merger.
- **Executor** — ChatGPT, Codex Cloud, Local Codex, IDE agent, automation, or human.
- **Model/profile** — provider-managed, Sol, reasoning effort, or another executor-specific setting.
- **GitHub identity** — the account that publishes, comments, or formally reviews.
- **Provenance** — who created the current PR/branch and whether it is same-repository, fork, or managed automation.

Codex Cloud is not assumed to be Terra, Sol, or any other model. Its current model is recorded as `provider-managed` unless the
product exposes and the task explicitly selects a verified value. The current Local Codex profile is intentionally fixed to Sol
with extra-high reasoning; the dispatcher does not pretend to choose a cheaper model dynamically for that automation.

## Roles

A task may assign responsibilities independently:

- **Supervisor/router:** canonicalizes issues and PRs, refines Planning, proposes routing, tracks delivery, rotates the control log,
  and coordinates corrections. ChatGPT is the default.
- **Implementer:** changes code/docs and proves the change in its own environment after Sniffy deliberately routes an
  `Implementation` turn.
- **Reviewer:** inspects the complete exact-head diff, scope, tests, review threads, and CI evidence. ChatGPT is the default.
- **Verifier:** validates the observable result against the issue/PR in a representative environment.
- **Formal reviewer:** submits APPROVE or REQUEST_CHANGES using an identity independent from the PR author.
- **Privileged operator:** changes DNS, domains, repository settings, secrets, rulesets, Pages, or protected infrastructure.
- **Merger:** merges only after Dmitry's explicit instruction.

The same executor may hold several roles when independence is unnecessary. ChatGPT may implement through `bedrin-gpt`, but that
identity cannot formally approve its own PR. A Dmitry-, Dependabot-, IDE-, or contributor-authored PR remains provenance; it is not
automatically copied into the Project Implementer field.

## Routing fields

Planning records future routes when they are actually needed:

- `Implementer` — optional planned implementation executor. Empty means unknown, not applicable to the already-published change,
  or not yet deliberately selected;
- `Verifier` — planned verification executor.

The current lifecycle materializes:

- `Executor` — product/runtime responsible for the next action;
- `Assignee` — concrete GitHub identity or human responsible now;
- `Worker reference` — concrete canonical item, chat, task, process, worktree, branch, PR, exact head, or direct tick ownership.

Universal PR intake does not infer `Implementer` from author, bot, IDE, or provider identity. All flows must tolerate an empty value
and route from current `Status`, `Execution`, and `Executor`. Before entering Implementation, Planning or Review must deliberately
select a supported Implementer and copy that route into `Executor`.

The canonical Project item may be an issue or PR. Routing must first apply the canonicalization rules in
[`pull-request-intake.md`](pull-request-intake.md); do not send two executors to an issue and its PR as if they were independent
implementations.

All executors mutate Project fields through [`control-plane.md`](control-plane.md). Assignment and source/review operations remain
separate verified GitHub actions.

## Executor matrix

| Executor | Prefer when | Avoid or escalate when |
| --- | --- | --- |
| ChatGPT | issue/PR canonicalization, Planning, GitHub state, focused connector-backed edits, CI diagnosis, exact-head artifacts, Review, browser verification from available artifacts | required source/dependencies cannot be obtained, long writable checkout or service state is needed, or formal self-review would be dishonest |
| Codex Cloud | bounded, well-specified fresh-branch implementation; public dependencies; deterministic Cloud-available proof; useful parallelism; explicitly recoverable existing Cloud branch | arbitrary existing-PR adoption, branch surgery, persistent services/artifacts, private/local access, unresolved architecture, or repeated non-convergence |
| Local Codex app/CLI | complex architecture, lifecycle/concurrency, cross-version Java, persistent caches/services/Docker/browser, existing same-repository PR continuation/adoption, representative local verification, or Cloud escalation | fork/Dependabot branch adoption, unsafe host credentials/network, sandbox ambiguity, or a simpler executor can finish truthfully |
| IDE agent | Dmitry intentionally wants interactive pair programming in VS Code/IntelliJ | unattended ownership, independent review, or reproducible publication is required but not configured |
| Human | product/risk decisions, privileged operations, subjective acceptance, credentials/secrets, destructive migration, merge | routine bounded implementation or proof another executor can perform safely |

Dependabot normally supplies an already-published implementation rather than serving as current Executor. The PR itself records
that fact. ChatGPT owns Review and bot-operation monitoring, while the chosen Verifier owns outcome proof. Project 2 retains a
`Dependabot` Implementer option for manual/historical use; normal intake leaves it empty.

## Capability preflight

Before routing, confirm the exact environment can:

- obtain the canonical item, required source, and exact revision;
- use installed or downloadable dependencies;
- reach required network endpoints;
- start required runtimes, containers, browsers, or services;
- access credentials without exposing them;
- preserve or continue the required branch/PR;
- publish expected commits, PR updates, artifacts, and evidence;
- provide independent review identity when required.

For existing PR continuation also confirm:

- same-repository ownership rather than fork/managed-bot branch;
- the exact remote head branch and SHA;
- effective push permission for the selected executor;
- no competing worker or user session currently owns the branch;
- the route explicitly says continuation/adoption rather than fresh work.

Do not infer capability from a product name. ChatGPT's GitHub connector, web access, command sandbox, browser, and local runtimes
are separate capabilities. Codex Cloud's clean environment does not make an ambiguous task well specified. Local persistence does
not repair a missing product decision or grant permission to modify a contributor branch.

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
- Git history, branch ownership, existing PR, or fork constraints;
- publication and review identity.

Several interacting axes require a read-only design/proof preflight. Local Codex is the current strong execution route after the
preflight because it is configured for Sol/extra-high and a persistent environment, but executor strength is not permission to
invent unresolved decisions.

## Default implementation routing

Use this order for a new Implementation turn:

1. **Require a deliberate route.** If `Implementer` is empty, first choose a supported implementation executor through Planning or
   Review. Never derive it from the current PR author.
2. **Classify fresh versus continuation.** A published same-repository PR normally continues on its exact branch and PR. A fork or
   managed-bot PR normally remains contributor/bot-owned or is superseded by a linked internal task.
3. **ChatGPT direct capability check.** Can the current ChatGPT execution truthfully edit, test, inspect, publish, and verify the
   focused change with available source, dependencies, connectors, branch access, and identity? If yes, it may implement directly.
4. **Codex Cloud first for suitable fresh work.** Prefer Cloud for well-specified fresh-branch fixes, tests, documentation,
   dependency updates, CI follow-ups, and localized refactors whose proof fits the Cloud environment. Do not send an arbitrary
   existing PR to Cloud merely because Cloud is available.
5. **Local Codex for complexity, persistence, or existing PRs.** Route complex/cross-version/service/browser work and
   same-repository existing-PR continuation to Local Codex by default. The worker reuses the exact branch/PR even when Dmitry,
   ChatGPT, or an IDE agent originally created it.
6. **Human for authority or privilege.** Route unresolved product/risk decisions and privileged operations to Dmitry.

“Cloud first” is an executor/environment decision for fresh suitable work, not a model claim. “Local Sol/extra-high” is explicit
current Sniffy configuration, not a generic rule for all repositories.

## Existing PR adoption and continuity

A same-repository PR may enter the delivery process after being created outside it. Adoption means the canonical Project item
explicitly routes correction to a configured Implementer; it does not mean opening a new issue/branch/PR.

For an adopted continuation:

- identify the canonical issue or PR and every formal closing issue;
- guard current lifecycle state and exact PR head when the PR itself is canonical;
- set `Status = Implementation`, `Execution = Ready`, `Implementer = <selected executor>`, and matching `Executor`;
- keep Worker reference pinned to the exact existing PR and branch;
- selected worker fetches and updates that branch without reset, rebase, force-push, or replacement PR;
- all useful user/agent commits and unrelated-but-intentional changes are preserved;
- after publication, the same canonical item returns to `Review / Ready / ChatGPT` with the new exact head.

Do not adopt fork or Dependabot branches for direct autonomous correction. Use contributor-facing Request Changes, provider bot
commands, or a linked internal replacement task. A future policy may relax this only with explicit trust/permission rules.

## Review convergence checkpoint

Worker activity monitoring and review convergence are different mechanisms. The 15-minute/15-minute/hourly cadence observes an
already dispatched worker. It does not decide whether repeated code changes are conceptually converging.

Trigger a **convergence checkpoint** when all are true:

1. Review returned the task to Implementation with substantive Request Changes;
2. the implementer published a corrected head and handed it back to Review;
3. the next complete Review still finds substantive blockers.

Before sending another implementation request, ChatGPT must critically re-evaluate:

- Is the authoritative issue/PR still correct and sufficiently precise?
- Did the implementation follow the intended architecture, or is the direction itself wrong?
- Are the remaining findings local mistakes, misunderstood acceptance criteria, or an executor capability/context gap?
- Would another narrow patch preserve a flawed design or accumulate contradictory instructions?
- Does the proof matrix require a different environment?
- Should the task return to Planning, remain with the same implementer, adopt/escalate the existing PR to Local Codex, or block?

The checkpoint produces one explicit result:

- **Continue same executor:** direction is sound; supersede ambiguous guidance and issue one coherent correction request.
- **Adopt/escalate existing PR:** preserve the exact branch/PR and route continuation to Local Codex.
- **Return to Planning:** architecture, requirements, canonicalization, scope, or proof strategy must be re-baselined.
- **Block for Dmitry:** a product, risk, privilege, or branch-ownership decision is required.

Do not count infrastructure-only noise as a substantive correction cycle. Do not mechanically escalate merely because a test
failed; the checkpoint is about repeated substantive Review blockers.

## Executor change and continuity

Changing executor does not mean discarding published work:

- keep the same canonical item and linked requirements;
- continue the exact existing same-repository branch and PR when safe;
- preserve useful commits and evidence;
- explicitly mark superseded guidance;
- update `Implementer` only when a new implementation route is deliberately selected, update `Executor`, Worker reference, and
  correction metadata through one guarded control command;
- start the normal worker-monitoring cadence only after the new dispatch is concretely acknowledged.

Do not reset, rebase, force-push, open a duplicate PR, or restart from `develop` merely because work moved between ChatGPT, IDE,
Cloud, and Local Codex.

## Editable Sniffy defaults

Use [`profile.yml`](profile.yml) for operational choices Dmitry may tune without redefining shared policy, including:

- executor identities;
- direct-ChatGPT preference;
- first external implementation executor for fresh work;
- complex/persistent and existing-PR continuation executor;
- Local Codex model and reasoning effort;
- universal PR intake and canonicalization behavior;
- implementer inference policy;
- convergence trigger and preferred escalation executor;
- control-log rotation thresholds.

If a profile value conflicts with an explicit current issue or Dmitry's instruction, the issue/instruction wins. If a desired
change alters lifecycle meaning, claim safety, review independence, canonicalization, or merge authority, update the policy rather
than hiding it in the profile.

## Issue routing block

```md
## Delivery routing

- Supervisor: ChatGPT
- Canonical Project item: <Issue URL | PR URL>
- Existing PR/branch/head: <URL | none> / <branch | none> / <SHA | none>
- Work mode: <fresh | continuation | adopted-continuation>
- Implementer: <ChatGPT | Codex Cloud | Local Codex | IDE Agent | Human>
- Verifier: <ChatGPT | Codex Cloud | Local Codex | Human | Mixed>
- Default reviewer: ChatGPT
- Formal reviewer: <independent identity or human>
- Privileged operator: <none or named human>
- GitHub author identity: <account>
- Executor model/profile: <provider-managed | Sol / extra-high | other verified value>
- Capability assumptions: <source, dependencies, network, services, browser, credentials, branch permissions>
- Rationale: <risk axes, evidence, continuity, latency, and cost>
```

This block selects an Implementer before a new Implementation turn. Universal PR intake may leave it empty because no correction
has yet been routed.
