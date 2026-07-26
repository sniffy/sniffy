# Sniffy local Codex worker-task prompt

This is the canonical template rendered by the persistent dispatcher when it creates a one-time standalone Scheduled task. The dispatcher must replace all angle-bracket placeholders before creating the child task.

```text
Work autonomously on Sniffy GitHub issue #<ISSUE_NUMBER> in this dedicated worker chat and isolated app-managed worktree.

Issue title: <ISSUE_TITLE>
Issue URL: <ISSUE_URL>
Mode: <WORK_MODE>
Base branch: develop
Branch: <BRANCH_NAME>
Existing pull request: <PR_URL_OR_NONE>
Selected model: <MODEL>
Reasoning effort: <REASONING_EFFORT>

The issue and its complete comment thread are authoritative. Read AGENTS.md, docs/codex-workflow.md, the issue, project fields, linked pull requests, all review submissions and unresolved review threads, current CI, and the current remote develop branch before editing.

Confirm the Definition of Ready and apply the standard-tooling and infrastructure-approval gate in AGENTS.md. If a material product, public-API, architecture, security, permissions, or bespoke-infrastructure decision is missing, do not invent it. Set project Status to "Blocked", post the exact blocker and smallest required decision, pause any follow-up task, and stop.

For fresh work:
- verify that no implementation pull request or active worker branch already owns the issue;
- create <BRANCH_NAME> from the latest origin/develop without force-pushing or rewriting shared history.

For continuation work:
- verify that <PR_URL_OR_NONE> is open and that its head is <BRANCH_NAME>;
- fetch and check out that exact published head branch;
- verify local HEAD, the remote branch, and pull-request head agree before editing;
- preserve the existing pull request and branch;
- do not reset to develop, rebase, force-push, create a replacement branch, or open a duplicate pull request.

Then:

1. Convert every acceptance criterion into an explicit proof obligation before implementation.
2. Implement the smallest coherent solution and avoid unrelated refactoring.
3. Add or update focused tests and documentation where required.
4. Run focused checks first. Run broader relevant checks when practical, using separate commands and explicit JDKs as required by AGENTS.md.
5. Inspect the complete final diff, generated files, dependency changes, and test discovery/execution evidence.
6. Run git diff --check.
7. Commit and push the intended branch using the dedicated worker identity.
8. Create or update the intended pull request. Keep it draft while implementation or locally applicable validation is incomplete, then mark it ready for review unless the issue explicitly requires a draft handoff.
9. Verify the remote branch, full head SHA, pull-request URL, base/head branches, draft state, and matching pull-request head SHA. A local commit or task summary is not publication.
10. Update the issue with exact commands and results, the full SHA, and resolvable branch and pull-request links. Move project Status to "Review" only after publication is verified and the requested local proof is complete. Never close the issue automatically.

After publication, create or update one Scheduled task ATTACHED TO THIS CURRENT WORKER CHAT. Name it "Sniffy #<ISSUE_NUMBER> follow-up". Do not create a new standalone continuation chat.

Continuation cadence and state:

- Schedule the first check 15 minutes after publication or after an explicit correction dispatch.
- If work is still incomplete, keep the same task for a second check 15 minutes after the first.
- After the second incomplete check, update this same task to run hourly.
- Record the completed check count and latest inspected pull-request head in this chat so the cadence is not reset by every run.
- Pause the continuation task as soon as the review cycle is complete or a human/product decision is required.

On every continuation check:

1. Re-read the authoritative issue, new issue and pull-request comments, review submissions, unresolved review threads, latest complete diff, current head SHA, and every relevant CI job.
2. Do not infer that work started from a review alone. Verify an explicit dispatch/acknowledgement or a new commit when another agent was asked to fix something.
3. Address all actionable feedback together, preserve unrelated work, run the affected proof matrix, inspect the new complete diff, commit, push, and verify the new remote head.
4. If required CI is pending, record that state and continue at the configured cadence. Do not approve from a local summary or an older green head.
5. When the complete diff satisfies the issue and AGENTS.md, all actionable review threads are resolved, named tests actually ran, and required CI is green:
   - use a configured reviewer identity that is permitted to review and is not the pull-request author to submit APPROVE;
   - if no such identity is available, report that formal approval is impossible, leave an exact ready-for-human-approval comment, and pause the task rather than pretending approval was submitted.
6. If blocking defects remain, use a configured independent reviewer identity to submit one precise REQUEST_CHANGES review covering all current findings. If the active identity cannot formally review its own pull request, leave the same precise blocking feedback as a pull-request comment and report the identity limitation.
7. Never merge, enable auto-merge, close the issue, bypass branch protection, or rewrite the published branch without an explicit instruction from Dmitry.

At completion, summarize the final head SHA, diff reviewed, review-thread state, CI checked, formal review action actually submitted, and any residual risk. Then pause the in-chat follow-up task.
```

## Placeholder contract

The dispatcher must provide:

| Placeholder | Meaning |
| --- | --- |
| `<ISSUE_NUMBER>` | Numeric Sniffy issue number |
| `<ISSUE_TITLE>` | Current issue title |
| `<ISSUE_URL>` | Resolvable GitHub issue URL |
| `<WORK_MODE>` | `fresh` or `continuation` |
| `<BRANCH_NAME>` | New `agent/issue-N` branch or the exact existing PR head |
| `<PR_URL_OR_NONE>` | Existing continuation PR URL or `none` |
| `<MODEL>` | Exact model selected in the app |
| `<REASONING_EFFORT>` | Exact reasoning effort selected in the app |

Do not launch a worker when any placeholder cannot be resolved safely.
