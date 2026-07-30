# Automatic Request Changes return

## Purpose

When an authorized independent reviewer submits a formal `REQUEST_CHANGES` review on an agent-implemented pull request, the next action is implementation correction. This adapter returns the existing Project 2 work item to the original agent pool so the normal event loop can claim and continue the same branch and pull request.

## Trigger and target selection

The `Return requested changes to implementer` workflow listens to submitted `pull_request_review` events. It runs only for same-repository pull requests targeting `develop`; it never checks out or executes the reviewed head.

The workflow queries the current aggregate `reviewDecision` and continues only while it is `CHANGES_REQUESTED`. It then requires exactly one represented Project 2 work item:

- the pull request itself, for PR-first intake such as an agent-owned standalone PR; or
- one formally linked closing issue, for issue-first implementation work.

Zero represented items are a no-op. More than one represented item is an error because dispatching duplicate correction work would violate single-work-item ownership. Plain issue mentions are intentionally insufficient; use GitHub's formal PR-to-issue link or a closing keyword.

## Transition

The adapter uses the existing `delivery-control/v1` command shape and `executeTransition` implementation. It guards the current target type, exact head when the target is the PR, and the current `Status`, `Execution`, `Implementer`, `Executor`, and `Worker reference` values.

Eligible agent implementers are `ChatGPT`, `Codex Cloud`, and `Local Codex`. A successful formal Request Changes review applies:

```text
Status: Implementation
Execution: Ready
Executor: <current Implementer>
Worker reference: clear
GitHub assignees: clear
```

Clearing assignees makes the item pool-ready for the selected executor. The normal 15-minute event loop claims it, continues the same branch and PR, and begins the standard 15-minute, 15-minute, then hourly supervision cycle after concrete dispatch.

`Blocked` work, human or automation implementers, fork pull requests, stale/superseded review decisions, and ambiguous Project representation are not automatically dispatched.

## Permissions and security

The workflow starts with `permissions: {}`. Validation receives `contents: read`. Runtime preparation checks out trusted `develop` and uses the existing `PROJECT_TOKEN` only to read Project and PR state. The serialized transition job receives `contents: read` and `issues: write`; Project mutation uses `PROJECT_TOKEN`, while the repository `GITHUB_TOKEN` only clears assignees on the selected issue or PR.

The runtime path never checks out the reviewed head, never executes contributor code, does not merge, does not enable auto-merge, and does not run for forks. External contributors and Dependabot remain governed by pull-request intake policy rather than automatic correction dispatch.

## Blocking and failure semantics

This is a non-required lifecycle automation, not a source-validation check. A skipped event writes a job summary and makes no mutation. An unexpected error, Project guard conflict, ambiguous target, failed Project verification, or failed assignee update fails the workflow for maintainer inspection. The shared per-item concurrency key serializes this transition with ordinary delivery-control commands.

The maintainer response is to inspect the Actions summary, current formal review decision, Project representation, and fields; correct the relationship or state; then re-run the failed job or perform one guarded manual transition. Do not create a duplicate work item or replacement PR merely to recover this automation.

## Ownership, noise, and lifecycle

ChatGPT owns supervision of this adapter and the subsequent correction dispatch. Expected signal is one workflow run per formal Request Changes submission, with no target-conversation comment and no control-log noise. The workflow should be removed or folded into a future native Project automation only when GitHub can atomically route a formally reviewed PR or its canonical linked issue back to a dynamically selected implementer with equivalent guards, permissions, and audit evidence.
