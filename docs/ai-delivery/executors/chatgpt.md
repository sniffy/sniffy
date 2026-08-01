# ChatGPT supervisor and executor

ChatGPT is the default Sniffy delivery supervisor and may also plan, implement, review, or verify when the current chat has the
required GitHub, sandbox, artifact, browser, identity, and file-editing capabilities.

## Responsibilities

As supervisor, ChatGPT:

- turns discussions into an authoritative issue with decisions, non-goals, Implementer when Implementation is needed, Verifier,
  and proof obligations;
- scans every open PR targeting `develop`, regardless of author/label/provider, and canonicalizes issue versus PR before work;
- treats exactly one closing issue as canonical, standalone PRs as canonical, and multi-issue PRs as Planning items;
- ingests ready implementations into Review without inferring Implementer from authorship;
- suppresses duplicate issue/PR lifecycle representations and never reviews the same implementation twice;
- tracks lifecycle fields, guarded claims, workers, branches, PRs, exact SHAs, draft state, reviews, Verification, CI, artifacts,
  and blockers;
- routes same-repository existing-PR corrections to ChatGPT or Local Codex continuation without opening a duplicate;
- rotates the technical control issue during normal event-loop work;
- performs the convergence checkpoint when corrected work returns to Review with substantive blockers;
- never merges or performs privileged operations without explicit instruction.

As executor, reviewer, or verifier, ChatGPT may:

- inspect and modify repository files through the GitHub connector;
- create issues, branches, commits, and pull requests when authorized;
- continue an explicitly routed same-repository existing PR when connector-backed edits and validation are sufficient;
- analyze exact-head diffs, comments, review threads, CI jobs, logs, and artifacts;
- download supported CI artifacts and inspect their contents;
- run focused commands when source and dependencies are present;
- serve existing artifacts over localhost and drive installed Chromium when permitted;
- prepare precise Request Changes, verification evidence, and operator checklists.

It delegates obligations it cannot truthfully perform, such as unavailable dependency installation, private/local services,
hardware, privileged settings, complex existing-branch work, or formal review of its own GitHub-authored PR.

## Capability separation

Do not treat “ChatGPT has internet” as one capability. In the current Sniffy setup:

- the GitHub connector may read/write GitHub when the command sandbox cannot reach GitHub;
- web search may be available while arbitrary sandbox processes have no outbound network;
- Java may be installed while Maven/npm dependencies are absent;
- an exact-head CI artifact may be available when source checkout/build is impossible;
- Chromium may load a localhost artifact, which does not prove a source build;
- direct workflow dispatch may be unavailable even though issue comments and repository writes are available.

Run a capability preflight for canonical item, source, exact branch/head, dependencies, network, runtimes, browser, credentials,
publication, branch ownership, and review identity. Use the strongest truthful evidence available and route missing obligations
elsewhere.

## Common ProjectV2 control path

ChatGPT uses the same [`../control-plane.md`](../control-plane.md) protocol as Codex and human/CLI operators:

1. locate the one active technical control issue;
2. choose and re-read the canonical issue or PR;
3. post one guarded `delivery-control/v1` command using the human-readable Markdown wrapper from the control-plane document,
   preserving its exact markers and lowercase `json` fence, deriving the display-only prose from the authoritative marked JSON;
4. inspect the reaction and Actions result;
5. re-read the resulting Project fields and any PR state coupled to the transition;
6. only then claim ownership or report a handoff.

When the canonical item is a PR, guard its exact head. When an issue is canonical, keep its Worker reference pinned to the linked
PR URL and exact head. Omit optional fields that are not part of the transition. Universal PR intake leaves `Implementer`
untouched and accepts an empty value; it does not synthesize an author, bot, provider, IDE, or `Unknown` option.

For a command setting `Status = Review`, identify the review PR structurally. A canonical PR uses target plus `expected.head`; a
canonical issue includes `reviewPullRequest.number/head`. The control plane validates open/develop/exact-head identity, marks a
draft PR ready if necessary, and verifies non-draft state before writing Project Review.

Do not post `/project-status`, `/project-field`, claim-intent, lease arbitration, or polling comments on target issues/PRs.
`workflow_dispatch` is the administrative fallback to the same implementation and is not required for normal ChatGPT operation.

## Universal PR intake

Before ordinary queue work, each stateless tick scans all open PRs targeting `develop`:

- draft PRs are telemetry and may be monitored by an existing Implementation owner, but do not enter Review;
- one formal closing issue means that issue owns lifecycle state and the PR is exact-head implementation evidence;
- no formal closing issue means the non-draft PR itself becomes the Project item;
- several formal closing issues mean the PR enters Planning before Review;
- same-repository, fork, Dependabot, maintainer, IDE, and configured-agent PRs all enter discovery;
- author/repository ownership affects formal-review and correction policy, not eligibility.

If both an issue and its PR are materialized, ChatGPT reconciles one canonical active item and makes the duplicate non-claimable.
It does not invent a shadow issue for a standalone PR and does not create a replacement PR solely because an agent did not author
the existing branch.

See [`../pull-request-intake.md`](../pull-request-intake.md).

## Implementation publication and Review handoff

When ChatGPT performs Implementation directly, publication is complete only after all of the following are true:

1. the intended PR is open and targets `develop`;
2. its remote head equals the exact published commit inspected by ChatGPT;
3. implementation and all locally available proof are complete;
4. the PR has been marked ready for review and re-read with `draft = false`;
5. the PR description and evidence match that exact head.

Only then may ChatGPT submit the guarded `Status = Review` transition. For a canonical issue it includes
`reviewPullRequest.number/head`; for a canonical PR it guards target plus `expected.head`. The control plane may repair a remaining
draft as a final invariant, but ChatGPT must not rely on that repair instead of completing publication deliberately.

After the command's terminal reaction, ChatGPT re-reads both the PR as non-draft at the same exact head and the canonical Project
fields. It updates and verifies assignment separately. A green workflow, successful push, or `+1` reaction without matching PR
state is not a complete Review handoff.

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

Product testing on 2026-07-30 showed that recurrences append to the defining chat rather than reliably starting a fresh chat. Each
occurrence must therefore be stateless by procedure:

- ignore previous-run conclusions;
- read current GitHub and repository-owned policy from scratch;
- request and validate the configured status-snapshot read barrier;
- canonicalize arbitrary PRs before queue selection;
- reconcile configured Ready routes that are unclaimable because their Assignee belongs to another executor pool;
- perform at most one reconciliation, lifecycle turn, or supported dispatch, including a Ready Codex Cloud route for which
  ChatGPT is the configured dispatch owner;
- make no work-item, Project, source, or worker mutation for `NO_CHANGE`; the status-refresh request is read telemetry;
- keep durable state outside the chat.

The four persistent transcripts are telemetry. Replace a long defining chat deliberately using
[`../chat-retention.md`](../chat-retention.md); do not delete an active task chat before its replacement is smoke-tested.

Worker and PR-operation monitoring is not another Scheduled Task. Due 15-minute, second-15-minute, and hourly observations are
selected by these same event-loop ticks before new work.

Codex Cloud has no Project polling loop. When the current profile routes bounded fresh Implementation to
`Implementation / Ready / Codex Cloud`, a ChatGPT tick must claim that route without changing Executor, post exactly one supported
Cloud trigger, require durable acknowledgement, and record the first 15-minute observation point. If acknowledgement is absent,
release the provisional claim to Ready only when the trigger was not submitted or was definitively rejected. A submitted trigger
with uncertain acknowledgement remains one provisional generation for targeted recovery and must not be dispatched again. The
same ChatGPT loop monitors acknowledged or provisional Cloud `In progress` routes after 15 minutes, again after 15 minutes, then
hourly. Do not wait for Cloud to discover the Project item. Do not use this bridge for arbitrary existing-PR adoption; route that
continuation to Local Codex unless the existing Cloud branch is explicitly recoverable.

## ChatGPT Project bootstrap

For ordinary repository Projects, keep Project instructions short and point to repository-owned policy:

```text
For every sniffy/sniffy task, first read the current repository policy at
https://github.com/sniffy/sniffy/blob/develop/AGENTS.md and the AI delivery map at
https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md .

Treat the canonical GitHub issue or pull request, linked PR exact head and draft state, Project fields, reviews, CI, artifacts, and
central control protocol as the source of truth. Never claim that a checkout, command, build, browser session, publication,
deployment, or settings change occurred unless it completed and its output was inspected. Never merge or enable auto-merge
without Dmitry's explicit command.
```

Re-read current-`develop` policy at the start of each repository task. Review evidence remains pinned to the immutable PR head.
Project context may describe Sniffy's purpose and Dmitry's preferences, but must not duplicate engineering policy or store
credentials and generated artifacts.

## Direct execution versus delegation

Prefer direct ChatGPT implementation when:

- canonical repository state can be read accurately through available tools;
- edits and publication are focused and supported;
- a same-repository existing PR can be safely continued without complex checkout/service state;
- required checks can run with present source/dependencies, or exact-head CI/artifacts provide the intended proof;
- browser/system verification can be performed truthfully;
- formal review remains honest about PR-author identity.

Otherwise follow [`../routing.md`](../routing.md): bounded well-specified fresh work normally goes first to Codex Cloud; complex,
persistent, cross-version, service/browser, existing-PR, or non-converging work goes to Local Codex Sol/extra-high; privileged or
unresolved decisions go to Dmitry.

For same-repository correction, preserve the exact branch and PR. For a fork, leave contributor-facing findings and wait for a new
head or create a deliberately routed internal replacement. For Dependabot, use documented bot commands or linked replacement work.

A PR with empty Implementer may still be reviewed, monitored, verified, approved, superseded, or closed. If new code is required,
choose a supported Implementer before entering Implementation.

## Review convergence

When Review returns work to Implementation and the corrected head comes back with substantive blockers again, ChatGPT pauses
serial patching and performs the convergence checkpoint. It may continue the same executor, adopt/escalate the exact existing PR
to Local Codex, return to Planning, or block for Dmitry. This is a Review decision inside the ordinary event loop, not a new timer.

## Review identity limitation

A PR authored by `bedrin-gpt` cannot be formally approved or receive formal `REQUEST_CHANGES` from the same identity. This limits
the GitHub review submission, not the technical Review outcome or ChatGPT's lifecycle authority. For an independent author,
ChatGPT publishes one complete formal `REQUEST_CHANGES` when blockers remain. For its own PR, it publishes the same complete
blocking feedback as an ordinary PR comment and states the identity limitation. Both paths perform the guarded durable route in
the same tick; they never leave that exact head in `Review / Ready / ChatGPT` or wait for the formal-review return adapter.

Implementation, code, or design defects return to `Implementation / Ready` only after ChatGPT deliberately selects an eligible
Implementer and preserves the same same-repository branch, PR, and head. Requirements, architecture, or canonicalization defects
return to `Planning / Ready / ChatGPT`; `Blocked / Human` requires one exact Dmitry decision or action. If the self-authored PR is
technically acceptable, ChatGPT routes it to required Verification or `Approval / Ready / Human` rather than parking it in
Review. A Dmitry-, Dependabot-, external-contributor-, IDE-, or independently-agent-authored PR may receive an honest formal
ChatGPT review. Identity is determined from the actual PR author, not the optional Project Implementer field.

## Website verification

The command sandbox may lack outbound DNS while GitHub connector access and managed Chromium remain available. Prefer exact-head
CI artifacts rather than inventing a checkout. Follow [`../../chatgpt-site-preview.md`](../../chatgpt-site-preview.md) for artifact
identity, localhost serving, reversible browser policy, Playwright evidence, screenshots, and truthful reporting.
