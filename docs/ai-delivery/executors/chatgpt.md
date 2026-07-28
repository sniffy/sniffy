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

One logical ring may scan several external GitHub project profiles. Do not rely on files stored in a ChatGPT Project for the
dispatcher. OpenAI documentation does not guarantee that a scheduled run can create a new standalone worker chat in an
arbitrary target Project or inherit that Project's context. Treat nested one-time task creation as a smoke-tested adapter.
If worker creation is unsupported or fails, release the claim to `Ready` or record an exact blocker.

The reusable clock/claim/spawn design lives in [`../event-loop.md`](../event-loop.md).

## ChatGPT Project bootstrap

Keep Project instructions short and point to repository-owned policy:

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
engineering policy or store credentials, downloaded artifacts, browser-policy backups, or generated screenshots.

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