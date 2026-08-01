# Agent pull-request merge conflict reconciliation

## Purpose

An open implementation PR can become unmergeable after `develop` advances even when its exact-head CI and review were previously healthy. Merge conflicts are implementation work, not passive review or approval waiting. The delivery loop must discover them before ordinary queue selection and preserve the existing PR and branch while routing a continuation.

PR #780 is the motivating incident: it was open, non-draft, same-repository, agent-authored, and `CONFLICTING`, but the status snapshot contained no mergeability signal.

## Status signal

The status workflow exports a normalized top-level `pullRequests` collection for every open repository PR, independently of which
issue or PR is the canonical Project item. Each observation includes the PR number and URL, author, same-repository/fork ownership,
base branch, head branch and exact SHA, `mergeable`, `mergeStateStatus`, and formal closing issue numbers:

```json
{
  "number": 783,
  "repositoryOwnership": "same-repository",
  "headRefOid": "42594d311f6077d5557653c95f1942b5b224a5ae",
  "mergeable": "CONFLICTING",
  "mergeStateStatus": "DIRTY",
  "closingIssueNumbers": [782]
}
```

This keeps issue-canonical implementations visible without reactivating their duplicate PR Project items. The values remain
discovery hints: GitHub may temporarily report unknown mergeability while recomputing, and a snapshot can become stale. Before
dispatch or Project mutation, the tick must resolve canonical identity from the formal closing links, then re-read the live PR and
verify that it remains open, targets `develop`, has the same exact head, and remains conflicted.

## Routing

Before ordinary queue selection:

- same-repository agent PR: preserve the exact branch and PR, route a continuation to an eligible implementation executor, and resolve conflicts by merging current `develop` into the feature branch; do not rebase, reset, force-push, or create a replacement PR;
- Dependabot or managed automation: use its documented rebase command and monitor the resulting head;
- fork PR: request contributor conflict resolution and monitor for a new head; do not push to the fork autonomously.

After concrete dispatch, observe after 15 minutes, again after 15 minutes, then hourly while incomplete. A new head must receive matching CI and normal review/verification before approval.

## Boundaries

Conflict detection never grants merge or auto-merge authority. It does not bypass unresolved review threads, required CI, verification, or human approval. Snapshot mergeability does not replace the live exact-head guard used by Project transitions.
