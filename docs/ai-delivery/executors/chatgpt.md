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

Official current limitations materially affect a dispatcher design:

- a Scheduled Task cannot run more than once per hour;
- Pro and Enterprise users may have up to 15 active tasks;
- deleting the associated chat pauses its task;
- a task created inside a ChatGPT Project that has files cannot access those project files;
- Scheduled Tasks and Codex automations are distinct product mechanisms.

See:

- [Scheduled Tasks in ChatGPT](https://help.openai.com/en/articles/10291617-scheduled-tasks-in-chatgpt)
- [Projects in ChatGPT](https://help.openai.com/en/articles/10169521-projects-in-chatgpt)

To obtain one logical 15-minute poll cadence, use four permanent hourly dispatcher chats at `:00`, `:15`, `:30`, and `:45`.
Each no-op tick stays in its existing chat and returns `NO_CHANGE`.

## Dedicated scheduler Project

A manual test on 2026-07-28 attempted to create a Scheduled Task for the `Kerb4j` ChatGPT Project from a chat outside that
Project. The scheduler operation exposed no `project_id` or equivalent destination field and created a global task instead.
Therefore the current ChatGPT adapter must not claim that it can target an arbitrary Project.

Use one dedicated fileless ChatGPT Project, such as `AI Delivery Scheduler`, to group the four persistent dispatcher chats.
This Project is an organizational namespace only:

- do not rely on Project files, description, instructions, memory, or chat history as required worker context;
- keep durable context in GitHub issues/comments/PRs and repository-owned Markdown;
- make each dispatcher and child-task prompt self-contained enough to locate the repository profile and authoritative issue;
- keep Project instructions minimal and repeat critical repository/profile paths in every Scheduled Task prompt.

One logical scheduler Project may poll several repositories and GitHub Projects. Repository-specific filters live in external
profiles; a separate ChatGPT Project per repository is unnecessary.

Creating a child Scheduled Task in the **same current scheduler Project** has not yet been proven. Smoke-test it explicitly:

1. run a disposable scheduled dispatcher inside `AI Delivery Scheduler`;
2. ask it to create one one-time child task several minutes later;
3. verify that the child is a separate chat inside the same Project;
4. verify that it received all repository/issue context without Project files;
5. verify that failed creation releases the GitHub claim.

If the child becomes global, reuses the dispatcher chat, or cannot be created, mark ChatGPT spawning `unsupported`. The
scheduler may still perform polling, Planning, Review, artifact-based Verification, and notifications; route standalone
Implementation to Codex Cloud, Local Codex app/CLI, or a manually created ChatGPT worker.

The reusable clock/claim/spawn design lives in [`../event-loop.md`](../event-loop.md).

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
Tasks must still carry their own durable repository/issue pointers because Project files are unavailable to them.

## Direct execution versus delegation

Prefer direct ChatGPT execution when:

- repository state can be read accurately through available tools;
- edits and publication are focused and supported;
- required tests can run with already-present source/dependencies, or exact-head CI/artifacts provide the intended proof;
- browser/system verification can be performed truthfully;
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
