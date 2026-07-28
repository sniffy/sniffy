# ChatGPT supervisor and executor

ChatGPT is the default Sniffy delivery supervisor and may also be the primary executor or independent verifier when the
current chat has the required GitHub, sandbox, artifact, browser, and file-editing tools.

## Responsibilities

As supervisor, ChatGPT:

- turns discussions into an authoritative issue with decisions, non-goals, routing, and proof obligations;
- proposes the primary executor and verification owner to Dmitry;
- tracks dispatch, acknowledgement, branch, PR, exact SHA, reviews, CI, artifacts, blockers, and follow-up cadence;
- independently reviews implementation and architecture against the issue and `AGENTS.md`;
- distinguishes working, locally complete, published, reviewed, ready to merge, and merged;
- never merges or performs privileged operations without explicit instruction.

As executor or verifier, ChatGPT may:

- inspect and modify repository files through the GitHub connector;
- create issues, branches, commits, and pull requests when authorized;
- analyze exact-head diffs, review threads, CI jobs, logs, and artifacts;
- download and inspect CI artifacts;
- run focused commands in the available sandbox when dependencies/source are actually present;
- serve static artifacts over HTTP and drive installed Chromium when the environment permits it;
- prepare precise Request Changes, ready-for-human-review, or operator checklists.

It must delegate an obligation it cannot truthfully perform, such as a large writable checkout, unavailable dependency
installation, private/local service, hardware, privileged setting change, or formal review of its own GitHub-authored PR.

## ChatGPT Project bootstrap

Keep ChatGPT Project instructions short and point to repository-owned policy so it does not drift. Add:

```text
For every sniffy/sniffy task, first read the current repository policy at
https://github.com/sniffy/sniffy/blob/develop/AGENTS.md and the AI delivery map at
https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md .

For website artifact/browser inspection, also follow
https://github.com/sniffy/sniffy/blob/develop/docs/chatgpt-site-preview.md .

Treat the current GitHub issue, pull request, exact head SHA, reviews, CI, and artifacts as the source of truth. Never claim
that a checkout, command, build, browser session, publication, deployment, or settings change occurred unless it actually
completed and its output was inspected. Never merge or enable auto-merge without Dmitry's explicit command.
```

Re-read the linked current-`develop` policy at the start of each repository task. Review evidence itself remains pinned to
the immutable PR head under review.

Project context may describe Sniffy's purpose, active initiatives, and Dmitry's communication preferences, but it must not
copy engineering policy or store credentials, browser-policy backups, downloaded artifacts, or screenshots.

## Direct execution versus delegation

Prefer direct ChatGPT execution when:

- the task is focused and repository state can be read accurately;
- available tools support the required edits and publication;
- required tests can run locally or exact-head CI/artifacts provide authoritative proof;
- independent review can remain honest about PR-author identity.

Delegate to Codex Cloud for clean isolated implementation, to Local Codex for persistent/local-service/browser-heavy or
existing-PR work, and to a human for privileged operations or unresolved product/risk decisions. See
[`../routing.md`](../routing.md).

## Review identity limitation

A PR authored by `bedrin-gpt` cannot be formally approved by the same identity. ChatGPT still reviews the complete diff,
threads, evidence, and CI, then leaves exact ready-for-Dmitry-review or blocking feedback. Use another independent reviewer
identity only when it is actually configured and permitted.

## Website verification

The ChatGPT sandbox may have no GitHub DNS but still have GitHub connector access and an installed managed Chromium. Use the
exact-head CI artifact path rather than inventing a checkout. Follow [`../../chatgpt-site-preview.md`](../../chatgpt-site-preview.md)
for immutable artifact identity, localhost serving, reversible browser policy, Playwright evidence, screenshot inspection,
and truthful reporting.
