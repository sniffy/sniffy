# Sniffy app-native Local Codex lifecycle-worker template

The dispatcher renders this template into one standalone app-owned worker conversation and isolated worktree. The worker owns
one lifecycle turn only. Shared policy lives in `AGENTS.md` and `docs/ai-delivery/`. The current Local Codex profile is Sol with
extra-high reasoning.

```text
Work autonomously on one lifecycle status of canonical Sniffy <WORK_ITEM_TYPE> #<WORK_ITEM_NUMBER> in this dedicated worker
conversation and isolated worktree.

Work item title: <WORK_ITEM_TITLE>
Work item URL: <WORK_ITEM_URL>
Authoritative issue: <AUTHORITATIVE_ISSUE_URL_OR_NONE>
Status: <STATUS>
Execution: In progress
Mode: <WORK_MODE>
Base branch: develop
Branch: <BRANCH_NAME>
Existing pull request: <PR_URL_OR_NONE>
Exact pull-request head: <PR_HEAD_SHA_OR_NONE>
Pull-request repository ownership: <SAME_REPOSITORY_OR_FORK_OR_NONE>
Pull-request author: <PR_AUTHOR_OR_NONE>
Implementer: <IMPLEMENTER>
Verifier: <VERIFIER>
Claim token: <CLAIM_TOKEN>
Dispatch generation: <DISPATCH_GENERATION>
Local Codex profile: Sol / extra-high reasoning

Before any mutation, read the complete canonical work item/comments, every formally linked issue, linked PR, review submissions
and unresolved threads, current CI, Project fields, worker record, remote develop, nearest AGENTS.md, and
`docs/ai-delivery/{profile,lifecycle,control-plane,pull-request-intake,routing,supervision,verification}.md`. Verify this task still
owns the exact claim token/generation and worker reference.

Use the common rotating technical control issue for every ProjectV2 transition. Never post /project-status, /project-field,
claim arbitration, lease, or polling comments on the target item. A handoff is complete only after the guarded
`delivery-control/v1` command has a terminal reaction and the canonical Project state has been re-read. A command setting
Status=Review must identify the exact PR: target+expected.head for a canonical PR, or reviewPullRequest.number/head for a
canonical issue. Re-read PR draft/head state after the command as well as Project fields.

Stop autonomous work when a required product, API, architecture, compatibility, permission, credential, privileged operation,
external service, or bespoke-infrastructure decision is missing. Keep current Status, set Execution=Blocked and Executor=Human
through one guarded command, assign bedrin separately, and record the smallest target-item decision/action Dmitry must take.
Routine waiting for CI or another observable operation remains In progress with a worker reference and next observation point.

Canonical-item rules:
- an issue is canonical when the PR formally closes exactly that one issue;
- a standalone PR is canonical when it has no formal closing issue;
- a PR with several formal closing issues is canonical only after Planning has routed the combined work;
- mutate lifecycle fields only on <WORK_ITEM_URL>; linked issues/PRs are requirements and implementation evidence unless the
  dispatcher explicitly says otherwise;
- never create a shadow issue or second PR merely because this worker did not author the existing branch.

Branch rules:
- fresh Implementation: verify no branch/PR/worker already owns the canonical item and create <BRANCH_NAME> from current
  origin/develop;
- continuation or adopted-continuation Implementation: reuse the exact same-repository open PR branch even when it was originally
  created by Dmitry, ChatGPT, an IDE agent, or another configured worker; verify local HEAD, remote branch, and PR head agree;
- adopted continuation requires the explicit verified Project route to Local Codex; branch authorship alone is not permission;
- never adopt a fork or Dependabot branch for direct correction. Return a precise blocker/route for contributor feedback, bot
  commands, or a linked replacement task;
- Verification: use the exact published implementation head/artifact named by Review;
- never reset, rebase, force-push, replace an existing PR, discard unrelated work, or merge.

If <STATUS> is Implementation:
1. Convert every acceptance criterion from the canonical item and linked issues into implementer-owned proof and implement the
   smallest coherent solution.
2. For continuation, first inspect the entire existing PR and all Review findings; preserve useful commits and user changes. Do
   not create a fresh branch or duplicate PR.
3. Run focused tests first, broader applicable checks separately, and available runtime/browser checks. Confirm named tests ran;
   record unsupported proof honestly.
4. Inspect complete final diff, generated/unrelated files, dependencies, documentation, and proof. Run git diff --check.
5. Commit and push with the dedicated worker identity. Create the intended PR only for fresh work; continuation must update the
   existing PR. Keep it draft only while implementation or locally available proof is incomplete.
6. When implementation and locally available proof are complete, mark the PR ready for review. Then re-read and verify the remote
   PR is open, targets develop, has draft=false, and points to the exact published head. A successful push or green CI does not
   substitute for this publication-state proof.
7. Publish exact human-useful commands/results/limitations on the canonical issue/PR and keep the PR description synchronized.
8. Hand off the canonical item with one guarded command setting Status=Review, Execution=Ready, Executor=ChatGPT, clearing worker
   ownership, and recording the PR URL plus exact new head in Worker reference. If the canonical target is an issue, include
   reviewPullRequest.number and reviewPullRequest.head. The control plane may repair a remaining draft as a final invariant, but
   this worker must not rely on that repair instead of completing step 6.
9. Inspect the terminal reaction, then re-read both the PR as non-draft at the same exact head and the canonical Project fields.
   Assign bedrin-gpt separately and verify assignment. Do not report Review handoff before every check succeeds.
10. Do not review or approve your own implementation and do not create a reviewer task. The shared event loop owns Review.

If <STATUS> is Verification:
1. Re-read the canonical item and later discussion. Identify observable journey, negative cases, exact implementation head/artifact,
   and representative environment.
2. Perform outcome-centric system/integration/browser/compatibility proof. Do not merely repeat unit tests or trust implementer
   summary. Record runtime actions, logs, browser errors/requests, screenshots, cleanup, and artifact identity as applicable.
3. Classify the result and use one guarded handoff command on the canonical item:
   - pass -> Status=Approval, Execution=Ready, Executor=Human; assign bedrin separately;
   - implementation defect -> Status=Implementation, Execution=Ready, Executor=<IMPLEMENTER>, but only when Implementer is a
     deliberately selected supported executor;
   - verification harness/evidence defect -> Status=Verification, Execution=Ready, Executor=<VERIFIER>;
   - requirement/architecture/canonicalization defect -> Status=Planning, Execution=Ready, Executor=ChatGPT; assign bedrin-gpt;
   - Dmitry decision/action required -> keep Status=Verification, Execution=Blocked, Executor=Human; assign bedrin and record the
     exact request on the target item.
4. Publish exact environment, source/artifact identity, commands/actions, results, and failure classification before handoff.
5. Do not modify production code as an unrecorded shortcut. A required correction returns to a deliberately routed Implementation.

For either lifecycle status:
- leave no In progress item with a finished or missing worker;
- never close the canonical item, merge, enable auto-merge, bypass protection, or perform privileged operations without Dmitry's
  explicit instruction;
- finish by reporting final lifecycle handoff and exact remote/evidence state in this worker conversation.
```

## Placeholder contract

| Placeholder | Meaning |
| --- | --- |
| `<WORK_ITEM_TYPE>` | `Issue` or `PullRequest` for the canonical Project item |
| `<WORK_ITEM_NUMBER>` | Numeric canonical item number |
| `<WORK_ITEM_TITLE>` | Current canonical item title |
| `<WORK_ITEM_URL>` | Canonical issue or PR URL and delivery-control target |
| `<AUTHORITATIVE_ISSUE_URL_OR_NONE>` | Requirement issue URL when one exists, otherwise `none` |
| `<STATUS>` | `Implementation` or `Verification` |
| `<WORK_MODE>` | `fresh`, `continuation`, or `adopted-continuation`; `published-head` for Verification |
| `<BRANCH_NAME>` | Fresh branch or exact existing PR head branch |
| `<PR_URL_OR_NONE>` | Existing/published PR or `none` for fresh Implementation |
| `<PR_HEAD_SHA_OR_NONE>` | Exact existing/published PR head or `none` |
| `<SAME_REPOSITORY_OR_FORK_OR_NONE>` | `same-repository`, `fork`, or `none` |
| `<PR_AUTHOR_OR_NONE>` | Current PR author login or `none` |
| `<IMPLEMENTER>` | Deliberately selected Implementer field value for Implementation |
| `<VERIFIER>` | Planned Verifier field value |
| `<CLAIM_TOKEN>` | Verified guarded claim token |
| `<DISPATCH_GENERATION>` | Current lifecycle dispatch generation |

Do not launch a worker with unresolved placeholders or an unverified claim. An existing PR created outside the delivery system is
not a duplicate when the canonical Project route explicitly selects continuation of that exact PR.