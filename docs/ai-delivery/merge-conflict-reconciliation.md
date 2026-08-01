# Agent pull-request merge conflict reconciliation

## Purpose

An open implementation PR can become unmergeable after `develop` advances even when its exact-head CI and review were previously healthy. Merge conflicts are implementation work, not passive review or approval waiting. The delivery loop must discover them before ordinary queue selection and preserve the existing PR and branch while routing a continuation.

PR #780 is the motivating incident: it was open, non-draft, same-repository, agent-authored, and `CONFLICTING`, but the status snapshot contained no mergeability signal.

## Status signal

The status workflow exports GitHub's `mergeable` and `mergeStateStatus` values for repository PRs and attaches them to matching active Project PR items:

```json
{
  "mergeable": "CONFLICTING",
  "mergeStateStatus": "DIRTY"
}
```

These values are discovery hints. GitHub may temporarily report unknown mergeability while recomputing, and a snapshot can become stale. Before dispatch or Project mutation, the tick must re-read the live PR and verify that it remains open, targets `develop`, has the same exact head, and remains conflicted.

## Routing

Before ordinary queue selection:

- same-repository agent PR: preserve the exact branch and PR, route a continuation to an eligible implementation executor, and resolve conflicts by merging current `develop` into the feature branch; do not rebase, reset, force-push, or create a replacement PR;
- Dependabot or managed automation: use its documented rebase command and monitor the resulting head;
- fork PR: request contributor conflict resolution and monitor for a new head; do not push to the fork autonomously.

After concrete dispatch, observe after 15 minutes, again after 15 minutes, then hourly while incomplete. A new head must receive matching CI and normal review/verification before approval.

## Boundaries

Conflict detection never grants merge or auto-merge authority. It does not bypass unresolved review threads, required CI, verification, or human approval. Snapshot mergeability does not replace the live exact-head guard used by Project transitions.
