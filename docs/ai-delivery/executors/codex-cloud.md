# Codex Cloud executor

Use Codex Cloud for clean, isolated, fully specified tasks whose context and deterministic proof are available in the Cloud
environment. Shared engineering policy lives in `AGENTS.md`; this document covers Cloud-specific routing, setup, publication,
handoff, and recovery.

Codex Cloud may implement or verify. Its model is `provider-managed` unless the product exposes and the task records a verified
selection.

## Route to Cloud

Prefer Cloud when work is bounded fresh work from current `develop`, or an explicitly recoverable Cloud-owned continuation;
requirements and architecture are resolved; dependencies are public/prepared; proof fits the Cloud environment; and no private
network, local service/device, persistent interactive state, or history surgery is required.

Do not route an arbitrary existing PR to Cloud. Cloud must not open a duplicate branch/PR. Same-repository existing-PR continuation
normally goes to Local Codex; forks and Dependabot remain contributor/bot-owned.

## Repository environment and credentials

Use `.codex/cloud/setup.sh`, `maintenance.sh`, `use-jdk.sh`, and `warm-maven-cache.sh` as documented. Preserve the canonical
`origin` push URL and verify `gh auth status` before publication.

Prefer a dedicated revocable fine-grained repository token. Ordinary autonomous publication needs Metadata read, Contents
read/write, Pull requests read/write, and Issues read/write. Grant Workflows write only for explicitly authorized workflow edits.
Never expose credentials in URLs, commands, logs, commits, fixtures, or summaries. Repository protection remains the final boundary.

Before relying on a changed environment, prove versions/auth/remote, one reversible remote-ref write, required JDKs, one focused
test, and `git diff --check` without leaving a branch or PR.

## Common control protocol

Cloud uses the active control issue and one guarded `delivery-control/v1` command per claim or handoff. Every autonomous command
uses the human-readable Markdown wrapper from `control-plane.md`, exact start/end markers, and a lowercase `json` fence. The marked JSON is authoritative; never emit bare JSON or use a private Cloud-only Project mutation path.

A Review transition identifies the exact PR through target plus `expected.head` for a canonical PR, or
`reviewPullRequest.number/head` for a canonical issue. GitHub assignment, publication, PR state, reviews, CI, and Project mutation
remain separately verified facts.

## Implementation contract

A Cloud implementation worker:

1. verifies canonical target, claim/generation/lease, fresh or recoverable-continuation mode, and intended branch/PR;
2. rejects arbitrary existing-PR adoption rather than creating duplicate fresh work;
3. implements the smallest coherent approved scope and maps acceptance criteria to proof;
4. runs focused checks first, broader applicable checks separately, and inspects the complete final diff;
5. runs `git diff --check`, commits, and pushes the intended branch;
6. creates one intended PR for fresh work or updates the exact recoverable Cloud-owned PR;
7. keeps it draft only while implementation or Cloud-available proof is incomplete;
8. when complete, marks it ready for review and re-reads it as open, targeting `develop`, non-draft, and at the exact published head;
9. synchronizes the PR description and publishes exact evidence;
10. performs its own guarded handoff to `Review / Ready / ChatGPT`, clearing Cloud claim/lease ownership;
11. verifies terminal reaction, Project fields, bedrin-gpt assignment, and the non-draft exact-head PR;
12. never reviews its own implementation, merges, or enables auto-merge.

A missing `origin` is recoverable checkout configuration. Restore the canonical remote, verify authentication, and attempt the
authorized push before reporting a real network/permission/protection blocker. Preserve the existing workspace and commit.

## Verification contract

Cloud Verification uses the exact published head/artifact named by Review and performs outcome-centric proof the environment
actually supports. Publish environment/actions/results and route:

- pass -> `Approval / Ready / Human`;
- implementation defect -> deliberate `Implementation / Ready / <Implementer>`;
- evidence/harness defect -> `Verification / Ready / <Verifier>`;
- requirements/architecture/canonicalization defect -> `Planning / Ready / ChatGPT`;
- unavailable capability -> reroute Verification to a capable executor;
- exact Dmitry action -> `Verification / Blocked / Human`.

Do not modify production code inside Verification as an unrecorded shortcut.

## Supervised dispatch and lease recovery

Codex Cloud does not poll Project 2. ChatGPT supervises `Implementation / Ready / Codex Cloud` by claiming while preserving
Executor, posting one exact implementation trigger, requiring durable acknowledgement, and recording the concrete generation and
`leaseUntil`.

The Cloud worker owns completion signalling: it publishes evidence and performs the guarded handoff itself. A normal 15-minute
scheduler tick ignores an acknowledged Cloud worker while its lease is valid; it does not ask the worker whether it is still
running.

If Cloud is still legitimately working near expiry, it renews the same claim/generation before `leaseUntil`. If the trigger was not
submitted or was definitively rejected, release to Ready. If submission succeeded but acknowledgement is uncertain, preserve one
provisional generation under the shorter provisional lease and never redispatch it.

Only a missing/invalid/expired lease creates stale recovery. The next ordinary scheduler tick targets that exact generation: verify
worker/task/branch/PR evidence, extend the lease if the same worker is active, complete a lost handoff if publication is sufficient,
recover the same branch/workspace if possible, or release only when no active/recoverable work remains. Never create a duplicate.

After substantive corrected work returns to Review with blockers again, stop serial patch dispatch and apply the convergence
checkpoint. Preserve the exact branch/PR if routing moves to Local Codex.

Never merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged operations without Dmitry's
explicit instruction.
