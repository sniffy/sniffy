# Sniffy local Codex scheduled-task prompt

Copy the text block below verbatim into the ChatGPT desktop app Scheduled task that dispatches the local Sniffy worker. The prompt supports both a fresh implementation and a continuation of an existing local-worker pull request.

```text
Run one Sniffy local-worker cycle in the selected WSL project and its dedicated worktree.

Use organization project 2 (https://github.com/orgs/sniffy/projects/2) owned by sniffy as the book of work. Select at most one Issue from sniffy/sniffy whose Status is "Ready for agent" and Executor is "Local Codex".

Prioritize eligible work in this order:

1. An explicitly re-queued continuation that already has an open local-worker pull request and actionable maintainer review feedback or required failing checks.
2. Otherwise, fresh work by highest project Priority and oldest issue number.

If there is no eligible issue, report a no-op and change nothing.

Before claiming, read AGENTS.md, docs/codex-workflow.md, the complete issue and all comments, all project fields, linked pull requests, review submissions and unresolved review threads, current CI, and the current remote develop branch. Confirm that the issue satisfies Definition of Ready and classify it as either fresh work or a continuation.

Apply the standard-tooling and infrastructure-approval gate in AGENTS.md before implementation. For common build, CI, release, dependency, or security problems, prefer maintained tools, official actions, platform features, and declarative configuration. Bespoke infrastructure or a materially new workflow/job is not ready unless the authoritative issue contains the required maintainer-approved alternatives, gap, ownership, security, test, upgrade, operational, and removal rationale. If that approval is absent, set Status to "Blocked" and report the missing decision instead of inventing infrastructure.

Fresh work has no active local claim and no implementation pull request.

A continuation is eligible when all of the following are true:

- the issue was deliberately returned to "Ready for agent";
- Executor is "Local Codex";
- the issue remains assigned to the dedicated local worker or can safely be assigned to it;
- an existing local-worker claim, agent:local label, writable agent/issue-N branch, and open implementation pull request are present;
- maintainer review feedback or required failed checks call for additional implementation.

For a continuation, the historical local claim, assignee, agent:local label, existing branch, and open pull request are required context. They are not reasons for a guarded no-op. The existence of an implementation pull request disqualifies only fresh work; it does not disqualify an explicitly re-queued continuation.

Do not continue a pull request owned by another executor or a branch the worker cannot update. Report that exact blocker instead.

Claim the selected item by changing project Status to "In progress". Add the dedicated worker account as assignee and add agent:local only when missing. Post a claim comment.

For fresh work, record the intended agent/issue-N branch.

For a continuation, post "Claimed continuation" and record the existing branch, pull-request number, current head SHA, timestamp, and feedback scope. Preserve the existing claim and pull request rather than creating replacements.

If any claim mutation fails, roll back mutations already made and stop before editing code.

After a successful fresh claim, create agent/issue-N from the latest origin/develop without force-pushing.

After a successful continuation claim, fetch and check out the exact existing pull-request head branch. Do not reset it to develop, rebase or rewrite its published history, create a replacement branch, or open a separate pull request. Verify that local HEAD, the remote branch, and the existing pull-request head agree before editing. Address all actionable review feedback and relevant failing checks on that pull request while preserving unrelated work already on the branch.

Follow AGENTS.md and the issue as the source of truth. Implement the fresh task or continuation fixes end to end, add or update tests and documentation, run all applicable focused checks as separately reported commands, inspect the final diff, commit, push, and create or update the pull request. Do not merge or enable auto-merge.

Verify the remote branch, full head SHA, pull-request URL, base/head branches, and pull-request head SHA. Move project Status to "Review" only after publication is verified and the requested continuation proof is satisfied. When the maintainer explicitly requires green remote CI, verify that CI before moving to Review.

If a genuine blocker remains, set Status to "Blocked" and post the exact blocker and smallest required decision. Never claim or implement more than one issue in this run.
```

## Continuation smoke test

Before enabling the recurring schedule, test the prompt against a disposable or intentionally re-queued issue with:

- `Status = Ready for agent`;
- `Executor = Local Codex`;
- an existing local claim and `agent:local` label;
- the local-worker assignee;
- an existing `agent/issue-N` branch and open pull request;
- actionable `CHANGES_REQUESTED` feedback.

The worker must claim the continuation, reuse the existing branch and pull request, and must not report a no-op merely because the claim or pull request already exists.
