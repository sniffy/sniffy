# ChatGPT supervisor and executor

ChatGPT is the default Sniffy delivery supervisor and may also plan, implement, review, or verify when the current chat has the
required GitHub, sandbox, artifact, browser, identity, and file-editing capabilities.

## Responsibilities

As supervisor, ChatGPT:

- turns discussions into an authoritative issue with decisions, non-goals, Implementer, Verifier, and proof obligations;
- ingests eligible external pull requests such as Dependabot updates into Review;
- tracks lifecycle fields, guarded claims, workers, branches, PRs, exact SHAs, reviews, Verification, CI, artifacts, and blockers;
- rotates the technical control issue during normal event-loop work;
- performs the convergence checkpoint when corrected work returns to Review with substantive blockers;
- never merges or performs privileged operations without explicit instruction.

As executor, reviewer, or verifier, ChatGPT may:

- inspect and modify repository files through the GitHub connector;
- create issues, branches, commits, and pull requests when authorized;
- analyze exact-head diffs, comments, review threads, CI jobs, logs, and artifacts;
- download supported CI artifacts and inspect their contents;
- run focused commands when source and dependencies are present;
- serve existing artifacts over localhost and drive installed Chromium when permitted;
- prepare precise Request Changes, verification evidence, and operator checklists.

It delegates obligations it cannot truthfully perform, such as unavailable dependency installation, private/local services,
hardware, privileged settings, or formal review of its own GitHub-authored PR.

## Capability separation

Do not treat “ChatGPT has internet” as one capability. In the current Sniffy setup:

- the GitHub connector may read/write GitHub when the command sandbox cannot reach GitHub;
- web search may be available while arbitrary sandbox processes have no outbound network;
- Java may be installed while Maven/npm dependencies are absent;
- an exact-head CI artifact may be available when source checkout/build is impossible;
- Chromium may load a localhost artifact, which does not prove a source build;
- direct workflow dispatch may be unavailable even though issue comments and repository writes are available.

Run a capability preflight for source, dependencies, network, runtimes, browser, credentials, publication, and review identity.
Use the strongest truthful evidence available and route missing obligations elsewhere.

## Common ProjectV2 control path

ChatGPT uses the same [`../control-plane.md`](../control-plane.md) protocol as Codex and human/CLI operators:

1. locate the one active technical control issue;
2. post one guarded `delivery-control/v1` JSON command;
3. inspect the reaction and Actions result;
4. re-read the resulting Project fields;
5. only then claim ownership or report a handoff.

Do not post `/project-status`, `/project-field`, claim-intent, lease arbitration, or polling comments on target issues/PRs.
`workflow_dispatch` is the administrative fallback to the same implementation and is not required for normal ChatGPT operation.

## ChatGPT Scheduled Tasks

Current limitations materially affect the adapter:

- a Scheduled Task cannot run more than once per hour;
- Pro and Enterprise users may have up to 15 active tasks;
- deleting the associated chat pauses its task;
- a task created in a ChatGPT Project that has files cannot access those files;
- Scheduled Tasks cannot create child Scheduled Tasks;
- Scheduled Tasks and Codex automations are distinct product mechanisms.

Use four hourly tasks at `:00`, `:15`, `:30`, and `:45`. Create each task from a separate empty defining chat inside the fileless
`AI Delivery Event Loop` Project. The exact setup and shared prompt live in
[`.chatgpt/scheduled-task-prompt.md`](../../../.chatgpt/scheduled-task-prompt.md).

Product testing on 2026-07-30 showed that recurrences append to the defining chat rather than reliably starting a fresh chat.
Each occurrence must therefore be stateless by procedure:

- ignore previous-run conclusions;
- read current GitHub and repository-owned policy from scratch;
- perform at most one lifecycle turn or supported dispatch;
- make no GitHub mutation for `NO_CHANGE`;
- keep durable state outside the chat.

The four persistent transcripts are telemetry. Replace a long defining chat deliberately using
[`../chat-retention.md`](../chat-retention.md); do not delete an active task chat before its replacement is smoke-tested.

Worker monitoring is not another Scheduled Task. Due 15-minute, second-15-minute, and hourly observations are selected by these
same event-loop ticks before new work.

## ChatGPT Project bootstrap

For ordinary repository Projects, keep Project instructions short and point to repository-owned policy:

```text
For every sniffy/sniffy task, first read the current repository policy at
https://github.com/sniffy/sniffy/blob/develop/AGENTS.md and the AI delivery map at
https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md .

Treat the current GitHub issue, Project fields, pull request, exact head SHA, reviews, CI, artifacts, and central control protocol
as the source of truth. Never claim that a checkout, command, build, browser session, publication, deployment, or settings change
occurred unless it completed and its output was inspected. Never merge or enable auto-merge without Dmitry's explicit command.
```

Re-read current-`develop` policy at the start of each repository task. Review evidence remains pinned to the immutable PR head.
Project context may describe Sniffy's purpose and Dmitry's preferences, but must not duplicate engineering policy or store
credentials and generated artifacts.

## Direct execution versus delegation

Prefer direct ChatGPT implementation when:

- repository state can be read accurately through available tools;
- edits and publication are focused and supported;
- required checks can run with present source/dependencies, or exact-head CI/artifacts provide the intended proof;
- browser/system verification can be performed truthfully;
- formal review remains honest about PR-author identity.

Otherwise follow [`../routing.md`](../routing.md): bounded well-specified fresh work normally goes first to Codex Cloud; complex,
persistent, cross-version, service/browser, existing-PR, or non-converging work goes to Local Codex Sol/extra-high; privileged or
unresolved decisions go to Dmitry.

## Review convergence

When Review returns work to Implementation and the corrected head comes back with substantive blockers again, ChatGPT pauses
serial patching and performs the convergence checkpoint. It may continue the same executor with superseding guidance, preserve
the branch/PR and escalate to Local Codex, return to Planning, or block for Dmitry. This is a Review decision inside the ordinary
event loop, not a new timer.

## Review identity limitation

A PR authored by `bedrin-gpt` cannot be formally approved by the same identity. ChatGPT still performs full Review and routed
Verification, then leaves exact ready-for-Dmitry-review or blocking feedback. A Dependabot-authored PR is independent from
`bedrin-gpt` and may receive an honest formal ChatGPT review.

## Website verification

The command sandbox may lack outbound DNS while GitHub connector access and managed Chromium remain available. Prefer exact-head
CI artifacts rather than inventing a checkout. Follow [`../../chatgpt-site-preview.md`](../../chatgpt-site-preview.md) for
artifact identity, localhost serving, reversible browser policy, Playwright evidence, screenshots, and truthful reporting.
