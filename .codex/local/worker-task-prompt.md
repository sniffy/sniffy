# Sniffy local Codex worker-task prompt

The dispatcher renders this template into one app-owned worker chat and isolated worktree. It supplies task-specific inputs;
shared engineering, routing, supervision, and verification policy remains in `AGENTS.md` and `docs/ai-delivery/`.

```text
Work autonomously on Sniffy issue #<ISSUE_NUMBER> in this dedicated worker chat and isolated app-managed worktree.

Issue title: <ISSUE_TITLE>
Issue URL: <ISSUE_URL>
Mode: <WORK_MODE>
Base branch: develop
Branch: <BRANCH_NAME>
Existing pull request: <PR_URL_OR_NONE>
Selected model: <MODEL>
Reasoning effort: <REASONING_EFFORT>

Read the complete issue and comments, project fields, linked PR, all review submissions and unresolved threads, current CI,
remote develop, AGENTS.md, docs/ai-delivery/supervision.md, and docs/ai-delivery/verification.md before editing.

Stop and record a Blocked state when a required product, API, architecture, compatibility, security, permission, privileged
operation, or bespoke-infrastructure decision is missing. Do not invent it.

Fresh work:
- verify no branch/PR/worker already owns the issue;
- create <BRANCH_NAME> from latest origin/develop without rewriting shared history.

Continuation work:
- verify <PR_URL_OR_NONE> is open and its head is <BRANCH_NAME>;
- check out the exact published head and verify local HEAD, remote branch, and PR head agree;
- preserve the branch and PR; never reset, rebase, force-push, replace them, or open a duplicate.

Then:
1. Convert every acceptance criterion into explicit proof and implement the smallest coherent solution.
2. Run focused checks first and broader applicable checks separately. Confirm named tests ran.
3. Inspect the complete final diff, generated/unrelated files, dependencies, and proof; run git diff --check.
4. Commit and push with the dedicated worker identity.
5. Create/update the intended PR. Keep it draft only while implementation or locally applicable proof is incomplete.
6. Verify remote branch, full SHA, PR URL, base/head refs, draft state, and matching PR head.
7. Update issue/PR evidence with exact commands, results, limitations, and resolvable links. Move to Review only after remote
   publication and required local proof are verified. Never close the issue automatically.
8. Never merge, enable auto-merge, bypass protection, or rewrite shared history without Dmitry's explicit instruction.

After publication, create or update one Scheduled task ATTACHED TO THIS WORKER CHAT named
"Sniffy #<ISSUE_NUMBER> follow-up". Check after 15 minutes, again 15 minutes later, then hourly while incomplete. Preserve
completed-check count and latest inspected head so the cadence is not reset.

On each follow-up:
- re-read issue/PR, new comments, review submissions, unresolved threads, complete diff, exact head, and relevant CI;
- do not infer work from a review/dispatch alone; require acknowledgement or a new commit;
- address all current actionable feedback together on the same branch/PR and rerun affected proof;
- do not approve from an old head or local summary;
- when complete, use an independent permitted identity for APPROVE; otherwise leave exact ready-for-human-review feedback;
- when blockers remain, use an independent permitted identity for one precise REQUEST_CHANGES or leave equivalent blocking
  feedback when self-review prevents a formal review;
- pause the follow-up task as soon as review completes or a human decision is required.

At completion, report final head, diff reviewed, review-thread state, CI/evidence checked, formal review action actually
submitted, residual risk, and follow-up-task state.
```

## Placeholder contract

| Placeholder | Meaning |
| --- | --- |
| `<ISSUE_NUMBER>` | Numeric Sniffy issue number |
| `<ISSUE_TITLE>` | Current issue title |
| `<ISSUE_URL>` | Resolvable issue URL |
| `<WORK_MODE>` | `fresh` or `continuation` |
| `<BRANCH_NAME>` | Fresh branch or exact existing PR head |
| `<PR_URL_OR_NONE>` | Existing continuation PR or `none` |
| `<MODEL>` | Exact selected app model |
| `<REASONING_EFFORT>` | Exact selected effort |

Do not launch a worker with unresolved placeholders.
