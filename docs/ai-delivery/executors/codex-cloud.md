# Codex Cloud executor

Use Codex Cloud for clean, isolated, fully specified tasks whose context and deterministic proof are available in the Cloud
environment. Shared engineering policy lives in `AGENTS.md`; this document covers Cloud-specific setup, credentials,
publication, lifecycle handoff, and failure handling.

Codex Cloud may be selected as Implementer or Verifier. It does not Review its own implementation unless a separate independent
reviewer identity and explicit route exist. Its model is recorded as `provider-managed` unless the product exposes and the task
explicitly selects a verified value; do not assume Cloud means Terra or Sol.

## Route to Cloud when

- work starts from current `develop` on a fresh branch, or an explicitly re-queued existing **Cloud-owned** branch can be recovered;
- product, architecture, compatibility, module placement, canonical item, and negative scope are resolved;
- dependencies are public or prepared by setup/cache;
- required tests fit available JDKs, tools, network policy, and task window;
- no local service, device, private network, unusual history surgery, or persistent interactive artifact inspection is required;
- parallel asynchronous implementation or verification is useful.

Cloud is the preferred first external implementation executor for suitable bounded **fresh work** after ChatGPT's
direct-capability check. That is an environment/routing decision, not a model claim.

Do not route an arbitrary existing PR from Dmitry, ChatGPT, an IDE agent, external contributor, Dependabot, or another worker to
Cloud as if it were a fresh task. Cloud must not open a duplicate branch/PR. Unless the branch is an explicitly recoverable
Cloud-owned continuation, return routing to the supervisor; same-repository existing-PR continuation normally goes to Local Codex.

Several interacting risk axes require the preflight and proof matrix in [`../routing.md`](../routing.md). When corrected Cloud work
returns to Review and still has substantive blockers, ChatGPT performs the convergence checkpoint before another dispatch. The
checkpoint may keep Cloud with rewritten guidance, preserve the branch/PR and escalate continuation to Local Codex, return to
Planning, or block for Dmitry.

## Repository environment

The supported environment uses:

- `.codex/cloud/setup.sh` for initial setup, GitHub CLI authentication, and canonical `origin` push configuration;
- `.codex/cloud/maintenance.sh` to restore the toolchain and canonical `origin` push URL when a cached environment resumes on
  another branch;
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
- confirms `origin` has `https://github.com/sniffy/sniffy.git` as its push URL;
- performs and removes a reversible remote-ref write;
- switches between JDK 8 and JDK 25;
- runs one focused test and `git diff --check`;
- leaves no implementation branch or PR.

A dry-run push is not proof of effective write permission.

## Common control protocol

Cloud uses the same [`../control-plane.md`](../control-plane.md) protocol as every executor. It locates the active technical
control issue, posts one guarded `delivery-control/v1` command for a claim or handoff, inspects the reaction, and re-reads Project
state. Every new command comment uses the control-plane document's human-readable Markdown wrapper with exact markers and a
lowercase `json` fence; its prose is derived display text and the marked JSON remains authoritative. Cloud must not emit bare JSON
for autonomous issue comments, update Project fields through a private Cloud-only GraphQL path, or post field/claim comments on
the target item.

The canonical target may be an issue or PR. For fresh work it is normally an issue; for an explicitly recoverable Cloud-owned PR
continuation the worker reference must name the exact branch/head and no duplicate PR may be created. A command setting
`Status = Review` must identify the exact PR: target plus `expected.head` when the PR is canonical, or structured
`reviewPullRequest.number/head` when an issue is canonical.

GitHub assignment, source publication, PR creation, reviews, and CI remain separately verified operations. The control plane's
draft-to-ready repair is a final invariant, not a substitute for Cloud completing publication itself.

## Implementation lifecycle contract

A Cloud implementation task reads the canonical issue/PR, every linked requirement, lifecycle/routing fields, current `develop`,
applicable `AGENTS.md`, and delivery docs before editing. It must:

1. verify intended canonical target, fresh/continuation mode, claim, branch, and PR;
2. reject arbitrary existing-PR adoption and return to supervisor instead of creating a duplicate;
3. convert each acceptance criterion to implementer-owned proof and implement only approved scope;
4. run focused checks first, broader applicable checks separately, and available runtime/browser checks;
5. inspect final diff, generated output, dependencies, documentation, and test discovery;
6. run `git diff --check`, commit, and push the intended branch;
7. create the intended PR for fresh work or update the explicitly recoverable Cloud-owned PR; use draft only while implementation
   or Cloud-available proof is incomplete;
8. verify remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head;
9. when implementation and Cloud-available proof are complete, mark the PR ready for review, then re-read and verify it is open,
   targets `develop`, has `draft = false`, and still points at the exact published head;
10. reconcile the PR description with current head, commands, limitations, artifacts, and CI;
11. publish human-useful implementation evidence on the canonical target;
12. hand off the canonical item with one guarded command to `Status = Review`, `Execution = Ready`, `Executor = ChatGPT`, recording
    PR URL/exact head and clearing Cloud worker ownership. If an issue is canonical, include `reviewPullRequest.number/head`;
13. inspect the terminal reaction, then re-read the PR as non-draft at the same exact head and the canonical Project fields. Update
    the ChatGPT Assignee separately and verify assignment before reporting handoff;
14. never review/approve its own implementation, merge, or enable auto-merge.

A missing `origin` is recoverable checkout configuration, not evidence that setup authentication failed. Setup persists `gh`
authentication for the agent phase, while the repository URL contains no credential. Before reporting a publication blocker,
restore the canonical remote, verify auth, and attempt the authorized push:

```bash
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url --push origin https://github.com/sniffy/sniffy.git
else
  git remote add origin https://github.com/sniffy/sniffy.git
fi
gh auth status --hostname github.com
git push --set-upstream origin HEAD
```

Only an actual authentication, network-policy, permission, or branch-protection failure after that attempt is a publication
blocker. Preserve/recover the existing workspace or commit instead of recreating implementation.

## Verification lifecycle contract

When selected as Verifier, Cloud uses the exact published implementation head or artifact named by Review. It performs
outcome-centric proof the Cloud environment genuinely supports, such as same-artifact compatibility, service/browser journey,
packaging behavior, or failure/rollback path, and records exact environment/actions/results.

It then publishes evidence and uses one guarded handoff command on the canonical item:

- pass -> `Approval / Ready / Human`;
- implementation defect -> `Implementation / Ready / Executor := Implementer` only when a deliberate Implementer is selected;
- verification harness/evidence defect -> `Verification / Ready / Executor := Verifier`;
- requirement/architecture/canonicalization defect -> `Planning / Ready / ChatGPT`;
- capability unavailable in Cloud but available elsewhere -> keep `Ready` and reroute Executor/Verifier;
- Dmitry action required -> `Verification / Blocked / Human` with exact request.

Do not modify production code inside Verification as an unrecorded shortcut.

## Corrections and supervision

A formal review or PR mention may not dispatch Cloud implementation. Use the proven explicit continuation trigger, then apply the
15-minute, second-15-minute, and hourly monitoring cadence from [`../supervision.md`](../supervision.md). Those observations are
performed by the normal event loop; do not create a separate monitoring scheduler.

Verify a connector acknowledgement or new commit before saying work resumed. If the corrected head returns to Review with
substantive blockers, stop automatic serial correction and wait for the convergence checkpoint route. Arbitrary existing-PR
continuation should be preserved and routed to Local Codex rather than recreated in Cloud.

The exact canonical item, GitHub branch, PR, SHA, draft state, reviews, checks, control-command result, and Project state are
authoritative. Codex UI associations are useful metadata but do not prove publication or completion.
