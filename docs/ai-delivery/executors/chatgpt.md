# ChatGPT supervisor and executor

ChatGPT is the default Sniffy delivery supervisor and may also plan, implement, review, or verify when the current chat has
the required GitHub, sandbox, artifact, browser, identity, and file-editing capabilities.

## Responsibilities

As supervisor, ChatGPT:

- turns discussions into an authoritative issue with decisions, non-goals, Implementer, Verifier, and proof obligations;
- tracks lifecycle fields, claims, worker references, branch, PR, exact SHA, Review, Verification, CI, artifacts, and blockers;
- coordinates corrections and re-baselining after substantive loops;
- never merges or performs privileged operations without explicit instruction.

As executor, reviewer, or verifier, ChatGPT may:

- inspect and modify repository files through the GitHub connector;
- create issues, branches, commits, and pull requests when authorized;
- analyze exact-head diffs, comments, review threads, CI jobs, logs, and artifacts;
- download supported CI artifacts and inspect their contents;
- run focused commands in the available sandbox when source and dependencies are actually present;
- run installed Java or other local runtimes;
- serve existing artifacts over localhost HTTP and drive installed Chromium when permitted;
- prepare precise Request Changes, verification evidence, and operator checklists.

It must delegate an obligation it cannot truthfully perform, such as unavailable dependency installation, sandbox internet,
private/local services, hardware, privileged settings, or formal review of its own GitHub-authored PR.

## Capability separation

Do not treat “ChatGPT has internet” as one capability. In the current Sniffy setup:

- the GitHub connector may read and write GitHub even when the command sandbox cannot resolve or reach GitHub directly;
- web search or monitoring may be available while arbitrary sandbox processes still have no outbound network;
- Java may be installed while Maven/npm dependencies are absent and cannot be downloaded;
- an exact-head CI artifact may be available even when source checkout/build is impossible;
- Chromium may load a localhost artifact after reversible policy handling, but that does not prove a source build.

Run a capability preflight for source, dependencies, network, runtimes, browser, credentials, publication, and review identity.
Use the strongest truthful evidence available and route missing obligations to another executor.

## ChatGPT Scheduled Tasks

Official current limitations materially affect the event-loop design:

- a Scheduled Task cannot run more than once per hour;
- Pro and Enterprise users may have up to 15 active tasks;
- deleting the associated chat pauses its task;
- a task created inside a ChatGPT Project that has files cannot access those project files;
- Scheduled Tasks and Codex automations are distinct product mechanisms.

See:

- [Scheduled Tasks in ChatGPT](https://help.openai.com/en/articles/10291617-scheduled-tasks-in-chatgpt)
- [Projects in ChatGPT](https://help.openai.com/en/articles/10169521-projects-in-chatgpt)

Manual tests on 2026-07-28 established two additional current-product constraints:

1. the available scheduling operation exposes no `project_id`, so a chat cannot target an arbitrary different ChatGPT Project;
2. a Scheduled Task execution cannot create another Scheduled Task.

Treat both as observed adapter constraints, not permanent platform guarantees.

## Stateless new-chat event loop

Use one dedicated fileless ChatGPT Project, such as `AI Delivery Event Loop`, containing four permanent hourly Scheduled
Tasks:

```text
:00 -> destination: new chat
:15 -> destination: new chat
:30 -> destination: new chat
:45 -> destination: new chat
```

Together they provide one logical 15-minute poll cadence. Every occurrence starts a fresh chat in the scheduler Project. The
fresh chat is both dispatcher tick and, when it claims ChatGPT-routed work, the phase worker. It must not attempt to create a
child task.

Each scheduled prompt must be fully self-contained and must point to the external project-profile inventory. Every tick:

1. reads the profile list and authoritative GitHub/repository Markdown;
2. inspects lifecycle fields, current claims, branches, PRs, reviews, and CI;
3. best-effort claims at most one eligible phase turn;
4. performs that phase directly when current ChatGPT capabilities are sufficient, or makes a supported external handoff;
5. writes a durable next-phase handoff and clears its claim;
6. returns `NO_CHANGE` when no work is eligible.

Do not claim work that cannot reach a safe durable handoff within the scheduled execution. Dependency-heavy implementation,
long builds, persistent services, and environment-specific verification normally belong to Codex Cloud, Local Codex, CI, or
a human.

The fresh-chat destination prevents a single dispatcher conversation from accumulating months of execution context, but it
creates volume: four hourly tasks can create up to 96 chats per day, including no-op runs. Keep no-op output minimal. Chat
archival/cleanup is a separate future maintenance workflow; do not claim it is automated until an available product action has
been tested.

The scheduler Project is an organizational folder only:

- do not rely on Project files, description, instructions, memory, or previous tick chats as required context;
- keep durable state in GitHub issues/comments/PRs and repository-owned Markdown;
- repeat critical repository/profile locations in all four Scheduled Task prompts;
- use the same claim protocol across the four shards because runs may overlap.

One scheduler Project may service several repositories and GitHub Projects. A separate ChatGPT Project per repository is not
needed and cannot be selected dynamically by the current scheduling tool.

The reusable clock/claim design lives in [`../event-loop.md`](../event-loop.md).

## ChatGPT Project bootstrap

For ordinary repository Projects, keep Project instructions short and point to repository-owned policy:

```text
For every sniffy/sniffy task, first read the current repository policy at
https://github.com/sniffy/sniffy/blob/develop/AGENTS.md and the AI delivery map at
https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md .

For website artifact/browser inspection, also follow
https://github.com/sniffy/sniffy/blob/develop/docs/chatgpt-site-preview.md .

Treat the current GitHub issue, lifecycle fields, pull request, exact head SHA, reviews, CI, and artifacts as the source of
truth. Never claim that a checkout, command, build, browser session, publication, deployment, or settings change occurred
unless it completed and its output was inspected. Never merge or enable auto-merge without Dmitry's explicit command.
```

Re-read current-`develop` policy at the start of each repository task. Review evidence itself remains pinned to the immutable
PR head under review.

Project context may describe Sniffy's purpose, active initiatives, and Dmitry's communication preferences, but must not copy
engineering policy or store credentials, downloaded artifacts, browser-policy backups, or generated screenshots. Scheduled
Tasks must still carry durable repository/profile pointers because Project files and implicit Project context are not a
supported dependency of the event loop.

## Direct execution versus delegation

Prefer direct ChatGPT execution when:

- repository state can be read accurately through available tools;
- edits and publication are focused and supported;
- required tests can run with already-present source/dependencies, or exact-head CI/artifacts provide the intended proof;
- browser/system verification can be performed truthfully;
- the phase can reach a durable handoff within the current fresh scheduled chat;
- formal review remains honest about PR-author identity.

Delegate to Codex Cloud for clean isolated implementation, to headless/app-native Local Codex for persistent dependencies,
services, Docker, browsers, or existing-PR work, and to a human for privileged operations or unresolved product/risk
questions. See [`../routing.md`](../routing.md).

## Review identity limitation

A PR authored by `bedrin-gpt` cannot be formally approved by the same identity. ChatGPT still performs the full Review and any
routed Verification, then leaves exact ready-for-Dmitry-review or blocking feedback. Use another independent reviewer only
when it is actually configured and permitted.

## Website verification

The ChatGPT sandbox may lack outbound DNS while still having GitHub connector access and installed managed Chromium. Prefer
the exact-head CI artifact path rather than inventing a checkout. Follow
[`../../chatgpt-site-preview.md`](../../chatgpt-site-preview.md) for immutable artifact identity, localhost serving,
reversible browser policy, Playwright evidence, screenshot inspection, and truthful reporting.
