# Sniffy app-native Local Codex lifecycle-worker template

The dispatcher renders this template into one standalone app-owned worker conversation and isolated worktree. The worker owns
one lifecycle turn only. Shared policy lives in `AGENTS.md` and `docs/ai-delivery/`. The current Local Codex profile is Sol with
extra-high reasoning.

```text
Work autonomously on one lifecycle status of Sniffy issue #<ISSUE_NUMBER> in this dedicated worker conversation and isolated
worktree.

Issue title: <ISSUE_TITLE>
Issue URL: <ISSUE_URL>
Status: <STATUS>
Execution: In progress
Mode: <WORK_MODE>
Base branch: develop
Branch: <BRANCH_NAME>
Existing pull request: <PR_URL_OR_NONE>
Implementer: <IMPLEMENTER>
Verifier: <VERIFIER>
Claim token: <CLAIM_TOKEN>
Dispatch generation: <DISPATCH_GENERATION>
Local Codex profile: Sol / extra-high reasoning

Before any mutation, read the complete issue/comments, Project fields, worker record, linked PR, review submissions and unresolved
threads, current CI, remote develop, nearest AGENTS.md, and
`docs/ai-delivery/{profile,lifecycle,control-plane,routing,supervision,verification}.md`. Verify this task still owns the exact
claim token/generation and worker reference.

Use the common rotating technical control issue for every ProjectV2 transition. Never post /project-status, /project-field,
claim arbitration, lease, or polling comments on the target item. A handoff is complete only after the guarded
`delivery-control/v1` command has a terminal reaction and the Project state has been re-read.

Stop autonomous work when a required product, API, architecture, compatibility, permission, credential, privileged operation,
external service, or bespoke-infrastructure decision is missing. Keep current Status, set Execution=Blocked and Executor=Human
through one guarded command, assign bedrin separately, and record the smallest target-item decision/action Dmitry must take.
Routine waiting for CI or another observable operation remains In progress with a worker reference and next observation point.

Branch rules:
- fresh Implementation: verify no branch/PR/worker already owns the issue and create <BRANCH_NAME> from current origin/develop;
- continuation Implementation: reuse the exact open PR branch and verify local HEAD, remote branch, and PR head agree;
- Verification: use the exact published implementation head/artifact named by Review;
- never reset, rebase, force-push, replace an existing PR, discard unrelated work, or merge.

If <STATUS> is Implementation:
1. Convert every acceptance criterion into implementer-owned proof and implement the smallest coherent solution.
2. Run focused tests first, broader applicable checks separately, and available runtime/browser checks. Confirm named tests ran;
   record unsupported proof honestly.
3. Inspect complete final diff, generated/unrelated files, dependencies, documentation, and proof. Run git diff --check.
4. Commit and push with the dedicated worker identity. Create/update the intended PR and keep it draft only while implementation
   or locally available proof is incomplete.
5. Verify remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head.
6. Publish exact human-useful commands/results/limitations on the target issue/PR.
7. Hand off with one guarded command setting Status=Review, Execution=Ready, Executor=ChatGPT, clearing worker ownership, then assign
   bedrin-gpt separately and verify both state and assignment.
8. Do not review or approve your own implementation and do not create a reviewer task. The shared event loop owns Review.

If <STATUS> is Verification:
1. Re-read the authoritative issue and later discussion. Identify observable journey, negative cases, exact implementation
   head/artifact, and representative environment.
2. Perform outcome-centric system/integration/browser/compatibility proof. Do not merely repeat unit tests or trust implementer
   summary. Record runtime actions, logs, browser errors/requests, screenshots, cleanup, and artifact identity as applicable.
3. Classify the result and use one guarded handoff command:
   - pass -> Status=Approval, Execution=Ready, Executor=Human; assign bedrin separately;
   - implementation defect -> Status=Implementation, Execution=Ready, Executor=<IMPLEMENTER>;
   - verification harness/evidence defect -> Status=Verification, Execution=Ready, Executor=<VERIFIER>;
   - requirement/architecture defect -> Status=Planning, Execution=Ready, Executor=ChatGPT; assign bedrin-gpt separately;
   - Dmitry decision/action required -> keep Status=Verification, Execution=Blocked, Executor=Human; assign bedrin and record
     the exact request on the target issue.
4. Publish exact environment, source/artifact identity, commands/actions, results, and failure classification before handoff.
5. Do not modify production code as an unrecorded shortcut. A required correction returns to Implementation.

For either lifecycle status:
- leave no In progress item with a finished or missing worker;
- never close the issue, merge, enable auto-merge, bypass protection, or perform privileged operations without Dmitry's explicit
  instruction;
- finish by reporting final lifecycle handoff and exact remote/evidence state in this worker conversation.
```

## Placeholder contract

| Placeholder | Meaning |
| --- | --- |
| `<ISSUE_NUMBER>` | Numeric Sniffy issue number |
| `<ISSUE_TITLE>` | Current issue title |
| `<ISSUE_URL>` | Resolvable issue URL |
| `<STATUS>` | `Implementation` or `Verification` |
| `<WORK_MODE>` | `fresh` or `continuation` for Implementation; `published-head` for Verification |
| `<BRANCH_NAME>` | Fresh branch or exact existing PR head |
| `<PR_URL_OR_NONE>` | Existing/published PR or `none` for fresh Implementation |
| `<IMPLEMENTER>` | Planned Implementer field value |
| `<VERIFIER>` | Planned Verifier field value |
| `<CLAIM_TOKEN>` | Verified guarded claim token |
| `<DISPATCH_GENERATION>` | Current lifecycle dispatch generation |

Do not launch a worker with unresolved placeholders or an unverified claim.
