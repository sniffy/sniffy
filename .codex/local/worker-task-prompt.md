# Sniffy app-native Local Codex lifecycle-worker template

The dispatcher renders this template into one standalone app-owned worker conversation and isolated worktree. The worker owns
one lifecycle turn only. Shared policy lives in `AGENTS.md` and `docs/ai-delivery/`.

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
Selected model: <MODEL>
Reasoning effort: <REASONING_EFFORT>

Before any mutation, read the complete issue/comments, lifecycle and routing fields, claim/worker record, linked PR, all review
submissions and unresolved threads, current CI, remote develop, nearest AGENTS.md, and
`docs/ai-delivery/{lifecycle,supervision,verification}.md`. Verify this task still owns the exact claim token/generation.

Stop autonomous work when a required product, API, architecture, compatibility, permission, credential, privileged operation,
external service, or bespoke-infrastructure decision is missing. Keep the current Status, set Execution = Blocked,
Executor = Human, and Assignee = bedrin. Record the smallest decision or action Dmitry must take. Do not invent a decision or
create a replacement worker. Routine waiting for CI or another observable operation this worker started remains In progress.

Branch rules:
- fresh Implementation: verify no branch/PR/worker already owns the issue and create <BRANCH_NAME> from current origin/develop;
- continuation Implementation: reuse the exact open PR branch and verify local HEAD, remote branch, and PR head agree;
- Verification: use the exact published implementation head/artifact named by the Review handoff; do not create an unrelated
  replacement implementation branch;
- never reset, rebase, force-push, replace an existing PR, discard unrelated work, or merge.

If <STATUS> is Implementation:
1. Convert every acceptance criterion into implementer-owned proof and implement the smallest coherent solution.
2. Run focused tests first, broader applicable checks separately, and directly available runtime/browser checks. Confirm named
   tests actually ran; record unsupported proof honestly.
3. Inspect the complete final diff, generated/unrelated files, dependencies, documentation, and proof. Run git diff --check.
4. Commit and push with the dedicated worker identity. Create/update the intended PR and keep it draft only while
   implementation or locally available proof is incomplete.
5. Verify the remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head.
6. Publish exact commands/results/limitations and hand off:
   - Status = Review;
   - Execution = Ready;
   - Executor = ChatGPT;
   - Assignee = bedrin-gpt;
   - clear this worker/lease after the handoff is durably recorded.
7. Do not review or approve your own implementation and do not create a follow-up reviewer task. The shared event loop owns
   the Review lifecycle status.

If <STATUS> is Verification:
1. Re-read the authoritative issue and later discussion. Identify the observable user/developer journey, negative cases,
   exact implementation head/artifact, and representative environment.
2. Perform outcome-centric system/integration/browser/compatibility proof. Do not merely repeat unit tests or trust the
   implementer summary. Record runtime actions, logs, browser errors/requests, screenshots, cleanup, and artifact identity as
   applicable.
3. Classify the result:
   - pass -> Status = Approval, Execution = Ready, Executor = Human, Assignee = bedrin;
   - implementation defect -> Status = Implementation, Execution = Ready, Executor = <IMPLEMENTER>, Assignee empty unless
     directed;
   - verification harness/evidence defect -> Status = Verification, Execution = Ready, Executor = <VERIFIER>;
   - requirement/architecture defect -> Status = Planning, Execution = Ready, Executor = ChatGPT, Assignee = bedrin-gpt;
   - decision/action required from Dmitry -> keep Status = Verification, set Execution = Blocked, Executor = Human,
     Assignee = bedrin, and record the exact request.
4. Publish exact environment, source/artifact identity, commands/actions, results, and failure classification before handoff.
5. Do not modify production code as an unrecorded shortcut. A required implementation correction must return to Implementation.

For either lifecycle status:
- update lifecycle fields, Assignee, worker reference, and claim/lease on a best-effort atomic basis;
- leave no In progress item with a finished or missing worker;
- never close the issue, merge, enable auto-merge, bypass protection, or perform privileged operations without Dmitry's
  explicit instruction;
- finish by reporting the final lifecycle handoff and exact remote/evidence state in this worker conversation.
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
| `<CLAIM_TOKEN>` | Winning claim token |
| `<DISPATCH_GENERATION>` | Current lifecycle dispatch generation |
| `<MODEL>` | Exact selected app model |
| `<REASONING_EFFORT>` | Exact selected effort |

Do not launch a worker with unresolved placeholders or an unverified claim.
