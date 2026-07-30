# Pull-request-first intake

A Sniffy implementation may arrive before the delivery system knows about it. Dmitry may push from IntelliJ, ChatGPT or Codex may
publish a branch, Dependabot may open an update, or an external contributor may submit a fork PR. Every open PR targeting
`develop` is therefore a discovery source. Author, label, provider, and branch origin affect trust, review independence, and
correction strategy; they do not decide whether the PR belongs in the delivery process.

Issues and pull requests are both first-class Project items:

- an **issue** is normally the authoritative outcome, decisions, acceptance criteria, and proof contract;
- a **pull request** is the authoritative published implementation, exact head, CI, and review conversation;
- exactly one Project item is canonical for lifecycle routing at a time; the other object remains linked requirements or evidence.

Do not create a shadow issue merely because a PR exists without one. Do not create a second PR merely because the selected worker
did not author the existing branch.

## Canonical work-item selection

For every open PR targeting `develop`, inspect formal closing links and current Project representation before adding anything:

### Exactly one formal closing issue

The issue is the canonical lifecycle item, whether or not it is already in Project 2.

```text
Canonical Project item: Issue #123
Implementation evidence: PR #456 at exact head <sha>
```

Add or locate the issue idempotently. When the PR becomes non-draft and implementation publication is complete, initialize or
transition the issue to:

```text
Status: Review
Execution: Ready
Implementer: preserve existing value; empty is valid
Verifier: <chosen from actual risk>
Executor: ChatGPT
Assignee: bedrin-gpt
Worker reference: PR #456; branch; exact head; author; CI/evidence summary
```

Do not add the PR as a second active lifecycle item. If both objects were already materialized, prefer the issue, suppress the PR
item from ordinary claims through one guarded reconciliation, and record the canonical issue link. Never perform the same Review
twice merely because both GitHub objects appear in the Project.

### No formal closing issue

The PR itself is the canonical lifecycle item. A non-draft PR normally enters:

```text
Status: Review
Execution: Ready
Implementer: empty or preserved existing deliberate value
Verifier: <chosen from actual risk>
Executor: ChatGPT
Assignee: bedrin-gpt
Worker reference: exact branch/head, author, fork/same-repo, labels, linked context
```

The PR body is the requirement source unless Planning determines that a durable issue is needed for independently owned follow-up
work or unresolved product decisions.

### Multiple formal closing issues

The PR is the canonical coordination item and enters `Planning / Ready / ChatGPT`, not Review. Record every closing issue and
resolve:

- whether the combined scope is intentional;
- which issue decisions and acceptance criteria govern the PR;
- one combined review and verification proof matrix;
- how merge/closure evidence propagates to each linked issue;
- whether any issue should be removed from the combined PR and continue independently.

While the canonical multi-issue PR is open, the event loop must not independently claim a linked issue for duplicate Review or
Implementation of the same published change. Planning may split the work, but it must do so explicitly and preserve existing
branches/PRs where possible.

## Draft and publication semantics

A draft PR is visible implementation telemetry, not automatically `Review / Ready`:

- a new standalone draft PR is ignored by Review intake until it becomes ready for review;
- a draft linked to an already-owned canonical issue may remain `Implementation / In progress` and be monitored through its exact
  worker reference;
- `ready_for_review` does not need a separate event workflow because the next stateless tick re-scans all open PRs;
- an implementation worker must mark its PR ready when implementation and all locally available proof are complete, then perform
  the guarded canonical-item handoff to Review.

A green CI run on a draft PR does not substitute for the missing publication handoff.

## Authorship, provenance, and optional Implementer

The PR itself is authoritative for author and repository ownership. Intake records that provenance in Worker reference and uses it
for formal-review independence and correction policy. It does not copy the author login, bot name, IDE product, or newly observed
provider into the Project `Implementer` field.

`Implementer` is optional outside a deliberately routed future Implementation turn:

- empty means unknown, not applicable to the already-published implementation, or not yet selected;
- intake, Review, rebase monitoring, Verification, Approval, supersession, and closure must work with it empty;
- before new code work begins, Planning or Review deliberately selects a supported internal Implementer and copies it into
  `Executor` through one guarded transition.

Project 2 currently contains a `Dependabot` option for manual or historical classification. Universal intake deliberately leaves
it unchanged/empty, just as it does for Dmitry, external contributors, IDE agents, and future bots.

## GitHub Project automation

Use GitHub Projects' built-in auto-add for issues created by the delivery system or maintainers. This prevents newly planned tasks
from being orphaned before Implementation.

Do not rely on blind `is:pr is:open` auto-add as the only PR intake mechanism. A simple Project workflow cannot decide whether a
linked issue or the PR should be canonical and can create duplicate active lifecycle items. It is acceptable to retain the
existing dependency PR auto-add filter:

```text
is:pr is:open label:dependencies
```

because the event loop still canonicalizes every PR before work. Auto-add is discovery assistance, not lifecycle initialization,
review, or approval, and it is not retroactive.

## Universal event-loop intake adapter

Before ordinary queue selection, every ChatGPT tick scans all open PRs targeting `develop`, including same-repository PRs,
maintainer PRs, configured-agent PRs, Dependabot, future bots, and forks.

For each candidate it verifies:

1. repository, base branch, open/draft state, exact head, and mergeability/conflict state;
2. author and same-repository versus fork ownership;
3. labels, security/dependency metadata, and provider-specific controls;
4. formal closing issues and their Project representation;
5. whether the PR itself is already represented;
6. current lifecycle fields, assignees, worker reference, review decision, unresolved threads, and exact-head CI;
7. whether another canonical item or worker already owns the same implementation.

It then applies the canonicalization rules above with at most the guarded reconciliation it can verify now:

- exactly one closing issue -> add/locate and hand off that issue;
- no closing issue -> add/locate the PR;
- several closing issues -> add/locate the PR in Planning;
- draft -> monitor existing ownership or take no Review mutation;
- duplicate representation -> preserve one canonical active item and make the duplicate non-claimable;
- already correct -> no mutation.

A guarded intake command omits `Implementer` from both `set` and `clear`, chooses a Verifier from actual compatibility and
observable-risk obligations, and records the PR URL, exact head, branch, author, repository ownership, linked issues, and relevant
metadata. Intake is not approval.

## Review paths by PR ownership

### Same-repository PR

A PR from Dmitry, ChatGPT, Local/Cloud Codex, an IDE agent, or another trusted same-repository workflow can be reviewed normally.
If Review finds defects:

- submit one complete `REQUEST_CHANGES` when the reviewer identity is independent;
- deliberately select ChatGPT or Local Codex for correction;
- preserve and continue the exact branch and PR;
- use Local Codex by default for complex, persistent, or existing-PR continuation;
- never restart from `develop`, open a duplicate PR, rebase shared history, or force-push merely because a different executor now
  owns the correction.

A Project route may explicitly adopt an existing PR for Local Codex continuation even when the branch was created outside the
delivery system. Branch authorship alone neither grants nor prevents adoption; verified repository ownership, permissions, and the
guarded route do.

### Fork PR

Do not run privileged code from an untrusted head or silently take ownership of a contributor branch. Review the exact head and:

- submit contributor-facing `REQUEST_CHANGES` and wait for a new head;
- keep the canonical item in Review with ChatGPT owning the monitored wait when appropriate;
- create a linked replacement task only when Dmitry deliberately chooses to internalize the work;
- never push to the fork as an autonomous shortcut, even when GitHub exposes maintainer-edit capability.

### Dependabot or other managed automation

Dependabot is a specialization of the same intake:

- inspect the exact dependency/lockfile/workflow scope and update risk;
- for a stale base or conflict, use the documented bot rebase command and keep ChatGPT owning the observable wait;
- do not normally push repository-specific fixes onto the bot branch;
- when compatibility code is required, create and route a linked internal replacement task/PR;
- do not auto-approve or auto-merge merely because the author is trusted automation.

Future bots use their documented control surface when available; unknown automation is treated as provenance, not invented as a
new Project executor.

## Review and verification

Review always inspects the complete exact-head diff, authoritative issue(s), PR body/commits, tests, unresolved threads, matching
CI, generated output, permissions, dependencies, and compatibility. Formal APPROVE or REQUEST_CHANGES requires an identity
independent from the PR author. When `bedrin-gpt` authored the PR, ChatGPT may still complete technical Review but must record the
identity limitation and route final acceptance to Dmitry.

An acceptable PR advances to Verification when representative outcome proof remains or directly to `Approval / Ready / Human`
when existing evidence is sufficient. Risky browser, runtime, framework, cryptography, security, workflow, packaging, or
cross-version changes require the representative proof described in [`verification.md`](verification.md); green generic CI is not
system verification.

## Completion and linked issues

The canonical Project item remains `Approval / Ready` until Dmitry explicitly merges or closes. After an actual merge or deliberate
closure, the event loop reconciles the canonical item to Done and records the merge/closure SHA and outcome. For a canonical PR
with several linked issues, the same observed merge may then complete or reopen each issue according to the Planning decision; do
not mark them Done merely because they were mentioned.

No intake, review, correction, verification, or Project automation enables auto-merge. Merge authority remains with Dmitry.
