# Automatic Request Changes return

## Purpose

When an authorized independent reviewer submits a formal `REQUEST_CHANGES` review on an agent-implemented pull request, the next action is implementation correction. This adapter returns the existing Project 2 work item to the original agent pool so the normal event loop can claim and continue the same branch and pull request.

## Trigger and target selection

The `Return requested changes to implementer` workflow listens to submitted `pull_request_review` events. It runs only for same-repository pull requests targeting `develop`; it never checks out or executes the reviewed head.

The workflow queries the current aggregate `reviewDecision` and continues only while it is `CHANGES_REQUESTED`. It derives the canonical item from formal closing links before considering current Project representation:

- exactly one closing issue makes that issue canonical, even when the pull request was also auto-added;
- no closing issue makes the pull request canonical;
- several closing issues keep the pull request in Planning and are not returned automatically.

The canonical item must already be represented in Project 2. A missing canonical issue is a no-op until universal intake materializes it; a represented pull request is never substituted for that issue. Duplicate issue/PR representation does not redefine canonical identity or dispatch duplicate correction work. Plain issue mentions are intentionally insufficient; use GitHub's formal PR-to-issue link or a closing keyword.

## Transition

The adapter uses the existing `delivery-control/v1` command shape and `executeTransition` implementation. It guards the current target type, exact head when the target is the PR, and the current `Status`, `Execution`, `Implementer`, `Executor`, and `Worker reference` values. For an issue-canonical continuation, the serialized transition re-reads the pull-request head immediately before and after Project mutation so a head change cannot silently publish a stale continuation reference.

Eligible agent implementers are `ChatGPT`, `Codex Cloud`, and `Local Codex`. A successful formal Request Changes review applies:

```text
Status: Implementation
Execution: Ready
Executor: <current Implementer>
Worker reference for a canonical issue: existing PR URL/number + branch + exact head
Worker reference for a canonical PR: clear
GitHub assignees: cleared and re-read as empty
```

Replacing an issue's stale review claim with the durable PR/branch/head reference preserves adoption context while making the item pool-ready. A standalone PR carries its own identity and exact-head guard, so its stale Worker reference is cleared. Assignee release is complete only after a separate GitHub read confirms no assignee remains. An idempotent rerun still performs and verifies assignment release, including when Project mutation succeeded in an earlier attempt. The normal 15-minute event loop claims the item, continues the same branch and PR, and begins the standard 15-minute, 15-minute, then hourly supervision cycle after concrete dispatch.

`Blocked` work, human or automation implementers, fork pull requests, stale/superseded review decisions, and ambiguous Project representation are not automatically dispatched.

## Permissions and security

The workflow starts with `permissions: {}`. Validation receives `contents: read` and runs when the adapter, workflow, documentation, or imported `delivery-control.js` implementation changes. Runtime preparation checks out trusted `develop` and uses the existing `PROJECT_TOKEN` only to read Project and PR state. The serialized transition job receives `contents: read` and `issues: write`; Project mutation and pull-request head verification use `PROJECT_TOKEN`, while the repository `GITHUB_TOKEN` only clears and verifies assignees on the selected issue or PR.

The runtime path never checks out the reviewed head, never executes contributor code, does not merge, does not enable auto-merge, and does not run for forks. External contributors and Dependabot remain governed by pull-request intake policy rather than automatic correction dispatch.

## Blocking and failure semantics

This is a non-required lifecycle automation, not a source-validation check. A skipped event writes a job summary and makes no mutation. An unexpected error, Project guard conflict, pull-request head change, failed Project verification, or failed assignment update/verification fails the workflow for maintainer inspection. The shared per-item concurrency key serializes this transition with ordinary delivery-control commands.

The maintainer response is to inspect the Actions summary, current formal review decision, Project representation, and fields; correct the relationship or state; then re-run the failed job or perform one guarded manual transition. Do not create a duplicate work item or replacement PR merely to recover this automation.

## Ownership, noise, and lifecycle

ChatGPT owns supervision of this adapter and the subsequent correction dispatch. Expected signal is one workflow run per formal Request Changes submission, with no target-conversation comment and no control-log noise. The workflow should be removed or folded into a future native Project automation only when GitHub can atomically route a formally reviewed PR or its canonical linked issue back to a dynamically selected implementer with equivalent guards, permissions, and audit evidence.
