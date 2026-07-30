# Codex Cloud executor

Use Codex Cloud for clean, isolated, fully specified tasks whose context and deterministic proof are available in the Cloud
environment. Shared engineering policy lives in `AGENTS.md`; this document covers Cloud-specific setup, credentials,
publication, lifecycle handoff, and failure handling.

Codex Cloud may be selected as Implementer or Verifier. It does not Review its own implementation unless a separate independent
reviewer identity and explicit route exist. Its model is recorded as `provider-managed` unless the product exposes and the task
explicitly selects a verified value; do not assume Cloud means Terra or Sol.

## Route to Cloud when

- work starts from current `develop` on a fresh branch, or an explicitly re-queued existing Cloud branch can be recovered;
- product, architecture, compatibility, module placement, and negative scope are resolved;
- dependencies are public or prepared by setup/cache;
- required tests fit available JDKs, tools, network policy, and task window;
- no local service, device, private network, unusual history surgery, or persistent interactive artifact inspection is required;
- parallel asynchronous implementation or verification is useful.

Cloud is the preferred first external implementation executor for suitable bounded work after ChatGPT's direct-capability check.
That is an environment/routing decision, not a model claim.

Several interacting risk axes require the preflight and proof matrix in [`../routing.md`](../routing.md). When corrected Cloud work
returns to Review and still has substantive blockers, ChatGPT performs the convergence checkpoint before another dispatch. The
checkpoint may keep Cloud with rewritten guidance, preserve the branch/PR and escalate continuation to Local Codex, return to
Planning, or block for Dmitry.

## Repository environment

The supported environment uses:

- `.codex/cloud/setup.sh` for initial setup and GitHub CLI authentication;
- `.codex/cloud/maintenance.sh` when a cached environment resumes on another branch;
- `.codex/cloud/use-jdk.sh <8|11|17|21|25>` for toolchain selection;
- platform-provided Maven and JDK 11/17/21/25;
- repository-installed Temurin JDK 8;
- `GH_TOKEN` plus GitHub CLI as the publication credential path.

Do not replace platform Maven or modern JDKs. Their proxy/TLS integration is part of the Cloud environment. Historical failure
modes and expected setup logs are documented in `../../codex-cloud-troubleshooting.md`.

Reset the Codex environment cache after setup-script, secret, token-approval, or network-policy changes.

## Credentials and network

Prefer a dedicated, revocable, fine-grained PAT owned by a `sniffy` organization member and restricted to the repository.
Ordinary autonomous publication needs Metadata read, Contents read/write, Pull requests read/write, and Issues read/write.
Grant Workflows read/write only for explicitly authorized `.github/workflows/` edits.

Agent internet policy must allow the actual GitHub methods required by publication. Read-only access or setup dry-run may look
healthy while `git-receive-pack` or API writes remain blocked.

Persisted credentials are readable by repository code, build plugins, dependencies, and agent processes. Never put tokens in
URLs, commands, logs, commits, summaries, or fixtures. Revoke and reset cache after suspected exposure. Server-side protection
must block direct/force writes to `develop`; automation identities must not bypass it.

## Environment validation

Before relying on a new or changed environment, run a small reversible validation task that:

- reads `AGENTS.md`, [`../profile.yml`](../profile.yml), and this runbook;
- reports Java, Maven, Node when relevant, and `gh` versions;
- checks `gh auth status`;
- performs and removes a reversible remote-ref write;
- switches between JDK 8 and JDK 25;
- runs one focused test and `git diff --check`;
- leaves no implementation branch or PR.

A dry-run push is not proof of effective write permission.

## Common control protocol

Cloud uses the same [`../control-plane.md`](../control-plane.md) protocol as every executor. It locates the active technical
control issue, posts one guarded `delivery-control/v1` command for a claim or handoff, inspects the reaction, and re-reads Project
state. It must not update Project fields through a private Cloud-only GraphQL path or post field/claim comments on the target
item.

GitHub assignment, source publication, PR creation, reviews, and CI remain separately verified operations.

## Implementation lifecycle contract

A Cloud implementation task reads the authoritative issue/thread, lifecycle/routing fields, current `develop`, applicable
`AGENTS.md`, and delivery docs before editing. It must:

1. verify intended claim/branch/PR and convert each acceptance criterion to implementer-owned proof;
2. implement only approved scope;
3. run focused checks first, broader applicable checks separately, and available runtime/browser checks;
4. inspect final diff, generated output, dependencies, documentation, and test discovery;
5. run `git diff --check`, commit, and push the intended branch;
6. create/update the intended PR, using draft only while implementation or Cloud-available proof is incomplete;
7. verify remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head;
8. reconcile the PR description with current head, commands, limitations, artifacts, and CI;
9. publish human-useful implementation evidence on the target item;
10. hand off with one guarded command to `Status = Review`, `Execution = Ready`, `Executor = ChatGPT`, clearing Cloud
    worker ownership; update the ChatGPT Assignee separately and verify both operations;
11. never review/approve its own implementation, merge, or enable auto-merge.

If `origin` is absent, configure an explicit repository URL. If publication access fails, preserve/recover the existing workspace
or commit instead of recreating implementation.

## Verification lifecycle contract

When selected as Verifier, Cloud uses the exact published implementation head or artifact named by Review. It performs
outcome-centric proof the Cloud environment genuinely supports, such as same-artifact compatibility, service/browser journey,
packaging behavior, or failure/rollback path, and records exact environment/actions/results.

It then publishes evidence and uses one guarded handoff command:

- pass -> `Approval / Ready / Human`;
- implementation defect -> `Implementation / Ready / Executor := Implementer`;
- verification harness/evidence defect -> `Verification / Ready / Executor := Verifier`;
- requirement/architecture defect -> `Planning / Ready / ChatGPT`;
- capability unavailable in Cloud but available elsewhere -> keep `Ready` and reroute Executor/Verifier;
- Dmitry action required -> `Verification / Blocked / Human` with exact request.

Do not modify production code inside Verification as an unrecorded shortcut.

## Corrections and supervision

A formal review or PR mention may not dispatch Cloud implementation. Use the proven explicit continuation trigger, then apply the
15-minute, second-15-minute, and hourly monitoring cadence from [`../supervision.md`](../supervision.md). Those observations are
performed by the normal event loop; do not create a separate monitoring scheduler.

Verify a connector acknowledgement or new commit before saying work resumed. If the corrected head returns to Review with
substantive blockers, stop automatic serial correction and wait for the convergence checkpoint route.

The exact GitHub branch, PR, SHA, reviews, checks, control-command result, and Project state are authoritative. Codex UI
associations are useful metadata but do not prove publication or completion.
