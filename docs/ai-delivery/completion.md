# Merge completion and routing reconciliation

Merge completion is an objective source event. It should normally be processed by GitHub Actions without spending a ChatGPT
heartbeat on interpretation. The generated dispatcher projection remains a narrow repair path for missed events and missing
routes.

## Canonical completion

For a merged pull request targeting `develop`:

- exactly one formal closing issue -> that issue is canonical;
- no formal closing issue -> the pull request is canonical;
- multiple formal closing issues -> the pull request is the canonical coordination item; linked issue propagation remains the
  explicit Planning decision recorded before Review.

The completion adapter requires exactly one Project 2 representation for the canonical content. Missing or duplicate
representations fail closed. An item explicitly marked as a suppressed duplicate is never completed independently.

A verified merge applies one guarded `delivery-control/v1` transition:

```text
Status: Done
Execution: clear
Executor: clear
Worker reference: compact merged PR/head/merge-commit/time evidence
Assignee: clear and verify separately
```

`Implementer`, `Verifier`, model, and reasoning fields remain historical metadata. The adapter verifies the current base, merged
state, exact PR head, and merge commit before and after the Project transition. Repeated delivery of the same event is idempotent.

## Trusted-base workflow

`.github/workflows/delivery-completion.yml` uses `pull_request_target.closed` so a merged fork PR can update organization Project
state. This is safe only under these enforced boundaries:

- checkout is pinned to trusted `develop`;
- no PR-head file, action, script, build, or command is executed;
- top-level `permissions: {}` remains empty;
- the prepare job receives only `contents: read` and uses the existing Project token for Project/live metadata;
- the transition job receives `contents: read` and `issues: write`; repository `GITHUB_TOKEN` is used only to clear assignees;
- concurrency is serialized by canonical Project target;
- no branch write, merge, auto-merge, deployment, environment, repository-setting, or secret mutation is possible.

The workflow creates no target comments or control-log commands. Its Actions summary is the operational audit record.

## Dispatcher repair candidates

The status projection computes these candidates from the existing normalized snapshot and raw exports. It performs no new GitHub
calls and adds no recurring polling pass.

### `completion-drift`

A narrow backstop for the normal pre-merge state only:

```text
Status = Approval
Execution = Ready
Executor = Human
source PR is already merged
```

For an issue, exactly one linked PR must be present and merged. For a PR item, the item itself must be merged. Historical items in
Implementation, Draft, or an uninitialized state are deliberately excluded so the hourly ChatGPT loop does not spend many ticks
cleaning old Project archaeology.

### `uninitialized-item`

An open non-technical issue with no Status receives a deterministic suggested initialization:

```text
Status = Planning
Execution = Ready
Executor = unique known assignee route, otherwise ChatGPT
```

The dispatcher still live-reads only this selected issue and applies one guarded command. Technical control/status issues are
excluded.

### `default-planning-route`

An open issue in `Planning / Ready` with no Executor uses a unique configured assignee route when present. With no assignee, it
defaults to ChatGPT. This makes ordinary unowned Planning work visible without requiring the model to rescan Project 2.

### `route-ambiguity`

Unknown or multiple assignees are never guessed. They produce one explicit reconciliation candidate. Likewise, a Ready item with
multiple assignees remains a route mismatch even when one login happens to match the configured executor pool.

## Efficiency contract

A scheduled ChatGPT tick still:

1. requests one fresh status artifact;
2. reads `dispatch.orderedCandidates`;
3. selects at most the first candidate;
4. live-reads only that canonical target and relevant PR;
5. performs at most one guarded reconciliation or lifecycle turn;
6. returns `NO_CHANGE` immediately when the projection is empty.

Normal merge completion should therefore cost no ChatGPT tick. The loop pays for reconciliation only when the event adapter missed
or when a new/unrouted issue actually needs one bounded Planning action.

## Failure response and removal

A missing/duplicate canonical representation, suppressed item, stale expected state, exact-head mismatch, merge-commit mismatch,
control conflict, or failed assignment cleanup causes a visible workflow failure or skip without speculative mutation. Inspect the
workflow summary and the next generated dispatch projection.

Remove or simplify the adapter only when native GitHub Project automation can provide equivalent canonical resolution, guarded
multi-field mutation, exact merge verification, and assignment cleanup.
