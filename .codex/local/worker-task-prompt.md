# Local Codex lifecycle-worker template

The dispatcher renders this into one standalone worker and isolated worktree. Default profile: **`gpt-5.6-terra` / medium**.
Use Sol/high only when the rendered task contains an explicit escalation reason.

```text
Perform one autonomous lifecycle turn for canonical Sniffy <WORK_ITEM_TYPE> #<WORK_ITEM_NUMBER>.

Title: <WORK_ITEM_TITLE>
Canonical URL: <WORK_ITEM_URL>
Authoritative issue: <AUTHORITATIVE_ISSUE_URL_OR_NONE>
Status: <STATUS>
Execution: In progress
Mode: <WORK_MODE>
Base: develop
Branch: <BRANCH_NAME>
Existing PR: <PR_URL_OR_NONE>
Exact PR head: <PR_HEAD_SHA_OR_NONE>
Ownership: <SAME_REPOSITORY_OR_FORK_OR_NONE>
PR author: <PR_AUTHOR_OR_NONE>
Implementer: <IMPLEMENTER>
Verifier: <VERIFIER>
Claim token: <CLAIM_TOKEN>
Generation: <DISPATCH_GENERATION>
Claimed at: <CLAIMED_AT>
Lease until: <LEASE_UNTIL>
Model/reasoning: <MODEL> / <REASONING>
Escalation reason: <ESCALATION_REASON_OR_NONE>

Record startedAt. Before mutation, read the complete canonical item and relevant comments, formal links, exact PR, current
reviews/threads/CI, remote develop, nearest applicable AGENTS.md files, docs/ai-delivery/runtime-contract.md, and only the detailed
lifecycle/runbook sections needed for <STATUS>. Verify the claim token/generation and worker reference still belong to this task.

Use the active control issue for every Project transition. Emit the human-readable Markdown wrapper with exact start/end markers
and lowercase `json` fence; the marked JSON is authoritative. Never emit bare JSON. A handoff is complete only after the terminal
reaction and current Project/assignment/PR state are re-read.

This worker owns completion signalling. When its lifecycle turn finishes, it must publish evidence and perform the guarded handoff
itself; do not rely on the dispatcher to discover normal completion. If legitimate work will exceed <LEASE_UNTIL>, renew the same
claim/generation before expiry through a guarded Worker reference update. Never let the lease expire silently while still working.

Canonical/branch rules:
- one closing issue -> issue canonical; no closing issue -> PR canonical; several -> PR Planning;
- mutate lifecycle fields only on <WORK_ITEM_URL>;
- fresh Implementation creates one branch from current origin/develop only when no valid branch/PR/worker owns the item;
- continuation or adopted-continuation must reuse the exact same-repository open PR branch and preserve useful commits;
- for continuation, do not create a fresh branch or duplicate PR;
- never adopt a fork or Dependabot branch for direct correction;
- never reset, rebase, force-push, replace the PR, discard unrelated work, merge, or enable auto-merge.

If <STATUS> is Implementation:
1. Convert acceptance criteria into implementer-owned proof and implement the smallest coherent solution. For continuation, first
   inspect the whole existing PR and all Review findings.
2. Run focused checks first and broader applicable checks separately. Confirm named tests ran; record unsupported proof honestly.
3. Inspect the complete final diff, generated/unrelated files, dependencies, docs, and proof. Run git diff --check.
4. Commit and push the dedicated branch. Create a PR only for fresh work; continuation updates the existing PR. Keep it draft only
   while implementation or locally available proof is incomplete.
5. When complete, mark the PR ready for review and re-read it as open, targeting develop, non-draft, and at the exact published
   head. Synchronize its description and publish exact human-useful evidence.
6. Hand off through one guarded command to Review / Ready / ChatGPT with PR URL and exact head, clearing claim/lease ownership. For
   a canonical issue include reviewPullRequest.number and reviewPullRequest.head; for a canonical PR guard expected.head. Verify
   reaction, Project state, assignment to bedrin-gpt, and non-draft exact-head PR state.
7. Do not review your own implementation or create a reviewer task.

If <STATUS> is Verification:
1. Identify the observable journey, negative cases, exact implementation head/artifact, and representative environment.
2. Perform outcome-centric integration/system/browser/compatibility proof rather than repeating unit tests. Record environment,
   actions, logs, requests/errors, screenshots, cleanup, and artifact identity as applicable.
3. Publish the result and route with one guarded command, clearing claim/lease ownership:
   - pass -> Approval / Ready / Human;
   - implementation defect -> Implementation / Ready / <IMPLEMENTER>, only after deliberate supported routing;
   - evidence/harness defect -> Verification / Ready / <VERIFIER>;
   - requirement/architecture/canonicalization defect -> Planning / Ready / ChatGPT;
   - exact Dmitry action required -> Verification / Blocked / Human.
4. Do not change production code as an unrecorded verification shortcut.

For either status, a genuine Dmitry decision/action may use Blocked / Human. Routine CI or publication waiting remains In progress
under the same claim and lease; either finish the handoff in this worker or renew before expiry. Leave no finished worker in
In progress. Never perform privileged operations or merge without Dmitry's explicit instruction.

Finish with final lifecycle/evidence state and one telemetry JSON object containing startedAt, finishedAt, durationSeconds, model,
reasoning, canonical target, outcome, exact provider token counters when exposed, or null counters with usageSource=unavailable.
Never invent exact token usage.
```

## Placeholder contract

`<WORK_ITEM_TYPE>` is `Issue` or `PullRequest`; `<WORK_MODE>` is `fresh`, `continuation`, `adopted-continuation`, or
`published-head`; branch/PR/head/ownership/author fields describe the exact live implementation; Implementer/Verifier are deliberate
routes; claim/generation identify ownership; claimed/lease timestamps come from the verified guarded claim; model/reasoning come
from the profile; escalation reason is `none` unless Sol/high was deliberately selected. Do not launch with unresolved placeholders
or an unverified claim.
