# Codex Cloud executor

Use Codex Cloud for clean, isolated, fully specified tasks whose required context and deterministic proof are available in
the Cloud environment. Shared engineering policy lives in `AGENTS.md`; this document covers Cloud-specific setup,
credentials, publication, phase handoff, and failure handling.

Codex Cloud may be selected as `Implementer` or `Verifier`. It does not perform Review of its own implementation unless a
separate independent reviewer identity and explicit route exist.

## Route to Cloud when

- work starts from current `develop` on a fresh branch, or an explicitly re-queued existing Cloud branch can be recovered;
- product, architecture, compatibility, module placement, and negative scope are resolved;
- dependencies are public or prepared by setup/maintenance;
- required tests fit the available JDKs, tools, network policy, and task window;
- no local service, device, private network, unusual history surgery, or persistent interactive artifact inspection is
  required;
- parallel asynchronous implementation or verification is useful.

After several interacting risk axes, require the preflight and proof matrix in [`../routing.md`](../routing.md). After two
substantive review/fix rounds, re-baseline and consider moving the same branch to Local Codex rather than cold-starting
another narrow Cloud task.

## Repository environment

The supported environment uses:

- `.codex/cloud/setup.sh` for initial setup and GitHub CLI authentication;
- `.codex/cloud/maintenance.sh` when a cached environment resumes on another branch;
- `.codex/cloud/use-jdk.sh <8|11|17|21|25>` for toolchain selection;
- the platform-provided Maven and JDK 11/17/21/25;
- repository-installed Temurin JDK 8;
- `GH_TOKEN` plus GitHub CLI as the single publication credential path.

Do not replace platform Maven or modern JDKs. Their proxy/TLS integration is part of the Cloud environment. The historical
failure mode and expected setup log are documented in
[`../../codex-cloud-troubleshooting.md`](../../codex-cloud-troubleshooting.md).

Reset the Codex environment cache after setup-script, secret, token-approval, or network-policy changes.

## Credentials and network

Prefer a dedicated, revocable, fine-grained PAT owned by a `sniffy` organization member and restricted to the repository.
Ordinary autonomous publication needs Metadata read, Contents read/write, Pull requests read/write, and Issues read/write.
Grant Workflows read/write only for explicitly authorized `.github/workflows/` edits.

Agent internet policy must allow the actual GitHub methods required by publication. Read-only access or a setup dry-run may
look healthy while `git-receive-pack` or GitHub API writes remain blocked.

Persisted credentials are readable by repository code, build plugins, dependencies, and agent processes. Never put tokens in
URLs, commands, logs, commits, summaries, or fixtures. Revoke and reset the cache after suspected exposure or unexpected
write. Server-side protection must block direct/force writes to `develop`; automation identities must not bypass it.

## Environment validation

Before relying on a new or changed environment, run a small reversible validation task that:

- reads `AGENTS.md` and this runbook;
- reports Java, Maven, Node when relevant, and `gh` versions;
- checks `gh auth status`;
- performs and removes a reversible remote-ref write;
- switches between JDK 8 and JDK 25;
- runs one focused test and `git diff --check`;
- leaves no implementation branch or PR.

A dry-run push is not proof of effective write permission.

## Implementation phase contract

A Cloud implementation task reads the authoritative issue/thread, lifecycle/routing fields, current `develop`, applicable
`AGENTS.md`, and delivery docs before editing. It must:

1. verify the intended claim/branch/PR and convert every acceptance criterion to implementer-owned proof;
2. implement only the approved scope;
3. run focused checks first, broader applicable checks separately, and available runtime/browser checks;
4. inspect the final diff, generated output, dependencies, documentation, and test discovery;
5. run `git diff --check`, commit, and push the intended branch;
6. create/update the intended PR, using draft only while implementation or Cloud-available proof is incomplete;
7. verify the remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head;
8. reconcile the PR description with the current head, commands, limitations, artifacts, and CI;
9. hand off to `Phase = Review`, `Status = Ready`, `Executor = ChatGPT`, and the configured ChatGPT Assignee;
10. clear the Cloud worker/lease only after the handoff is durably recorded;
11. never review/approve its own implementation, merge, or enable auto-merge.

If `origin` is absent, configure an explicit repository URL. If publication access fails, stop and preserve/recover the
existing workspace or commit instead of recreating the implementation.

## Verification phase contract

When selected as `Verifier`, Cloud must use the exact published implementation head or artifact named by Review. It performs
outcome-centric proof that the Cloud environment genuinely supports—such as same-artifact compatibility, a service/browser
journey, packaging behavior, or failure/rollback path—and records exact environment/actions/results.

It then classifies and hands off:

- pass -> `Approval / Ready / Human`;
- implementation defect -> `Implementation / Ready / Executor := Implementer`;
- verification harness/evidence defect -> `Verification / Ready / Executor := Verifier`;
- requirement/architecture defect -> `Planning / Ready / ChatGPT`;
- missing external capability -> `Verification / Blocked` with the exact reason or an explicitly rerouted Verifier.

Do not modify production code inside Verification as an unrecorded shortcut.

## Corrections and supervision

A formal review or PR mention may not dispatch Cloud implementation. Use the proven top-level explicit continuation trigger,
then apply the 15-minute, second 15-minute, and hourly cadence from [`../supervision.md`](../supervision.md). Verify a connector
acknowledgement or new commit before saying work resumed.

The exact GitHub branch, PR, SHA, reviews, and checks are authoritative. Codex UI associations are useful metadata but do not
prove publication or completion.
