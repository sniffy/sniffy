# AI delivery Project housekeeping

Project 2 is the durable book of work for the AI delivery loop. Source closure and merge events, however, can leave stale lifecycle
values behind unless terminal transitions and archival are automated. This document defines the preferred housekeeping layers and
the boundary between safe deterministic cleanup and ambiguous reconciliation.

## Goals

- keep closed issues and merged pull requests from accumulating in active lifecycle views;
- preserve enough recent completed work for audit and incident investigation;
- surface source/Project drift without silently inventing a new lifecycle route;
- prefer GitHub Projects built-in workflows before custom Actions code;
- keep every custom Project mutation behind the existing guarded `delivery-control/v1` protocol;
- avoid comments, duplicate issues, or noisy per-item housekeeping logs.

Housekeeping does not replace canonicalization, Review, Verification, or human acceptance. It reconciles objective terminal source
events and removes old closed items from ordinary views after a cooling-off window, without treating archival as completion.

## Layer 1: supported built-in terminal workflow

GitHub documents two default terminal workflows:

```text
Issues or pull requests closed -> Status = Done
Pull requests merged           -> Status = Done
```

The documented close workflow is not issue-only. Enabling it would also mark a closed-unmerged pull-request item `Done`, so keep it
disabled in Project 2. Enable only the pull-request-merged workflow. `Done` is appropriate for that event because the Sniffy
lifecycle defines it as an actually merged pull request or a deliberately closed task.

Until a verified Project capability can scope the close workflow to issues, detect closed canonical issues in the status snapshot
and reconcile each deterministic case through one guarded `delivery-control/v1` command. A future custom reconciler may automate
that same guarded case; the absence of a safe built-in workflow is not permission for direct or unguarded field edits.

Do not use a broad field-only rule such as `Execution = Ready -> Done`. Completion is derived from the issue/PR terminal event, not
from an agent's routing field.

### Closed unmerged pull requests

A closed unmerged pull request is not always equivalent to delivered work:

- a standalone PR intentionally abandoned may become `Done` with closure evidence;
- a PR that is implementation evidence for a still-open canonical issue must not complete that issue;
- a superseded PR may need its Project representation suppressed while a replacement issue remains active.

Do not enable the combined close workflow or another broad closed-PR-to-Done rule. Use the unambiguous merged-PR workflow and leave
closed-unmerged PR reconciliation to the anomaly layer below.

## Layer 2: delayed auto-archive

GitHub Project auto-archive filters support source qualifiers such as `is`, `reason`, and `updated`; they do not support arbitrary
custom fields such as `Status`. After the merged-PR workflow and initial reconciliation have been smoke-tested, configure the
supported source-state filter:

```text
is:closed updated:<@today-14d
```

This filter archives items from source state alone. It will therefore archive a closed-unmerged pull-request item after the cooling
window even when its Project `Status` is not `Done`. Fourteen days gives the status snapshot and event loop time to classify that
item and apply any deliberate guarded reconciliation before it leaves ordinary active views. Archival itself is not completion.

Archival is not deletion. Archived items retain their Project field values and may be restored for investigation. The complete
GitHub issue/PR and Actions history also remain authoritative evidence.

Archive also does not remove an item from the Project's total item limit. It is active-view housekeeping, not infinite storage. If
Project 2 approaches the platform item limit, define a separate, reviewed export-and-delete retention policy; do not silently delete
items as part of routine archival.

## Layer 3: daily anomaly reconciliation

A future repository workflow may inspect the read-only status snapshot once per day and classify drift. It should not blindly
rewrite every anomaly.

### Safe automatic repairs

The following cases are deterministic enough to repair through one guarded `delivery-control/v1` command:

```text
closed canonical issue + Status != Done -> Status = Done
merged canonical PR    + Status != Done -> Status = Done
```

The command must guard the current target type and current Project fields. A conflict is ordinary concurrency: do not retry from the
same snapshot.

### Report-only anomalies

The following require current canonicalization or routing decisions and must only be reported:

- open source item with `Status = Done`;
- reopened issue or pull request;
- closed unmerged PR whose canonical issue remains open;
- issue and pull request both represented as active canonical work;
- `Execution = Ready` with an empty `Executor`;
- agent `Executor` combined with a human-only Assignee;
- technical `ai-delivery-control` or `ai-delivery-status` issue materialized as work;
- stale Worker reference, branch, or exact-head evidence;
- several open status/control issues with the same technical label.

The anomaly list should be added to the existing `ai-delivery-status/v1` snapshot or its pointer metadata. Do not create one issue or
comment per anomaly.

### Reopened items

Reopening is not the inverse of completion. Do not automatically map `Done` back to `Planning` or `Implementation`. The event loop
must re-read the source, canonical links, replacement PRs, and current requirements, then deliberately choose the next lifecycle
state through the guarded control plane.

## Proposed custom workflow contract

A future implementation should use a separate failure domain, for example `AI delivery housekeeping`, with:

```yaml
on:
  schedule:
    - cron: '17 3 * * *'
  workflow_dispatch:

permissions: {}
```

Expected jobs:

1. read and validate the newest `ai-delivery-status` artifact;
2. classify deterministic repairs and report-only anomalies;
3. submit at most a bounded number of guarded terminal transitions;
4. publish aggregate counts and anomaly details in the Actions summary and next status snapshot.

The workflow must not receive branch, merge, deployment, environment, package, or secret-management permission. Project mutations
continue to use the existing `PROJECT_TOKEN` only through `delivery-control/v1`; the housekeeping reader itself should remain
read-only.

Use a concurrency group with `cancel-in-progress: false`. A failure is non-blocking for source CI but should leave the existing
snapshot unchanged and alert maintainers through the failed run. No routine target-item comments are posted.

## Operational checks

After enabling the supported built-in workflow:

1. close a disposable issue represented in Project 2 and verify the disabled combined close workflow does not change `Status`;
2. reconcile that closed issue through the guarded control plane and verify `Status = Done`;
3. merge a disposable PR represented as canonical and verify the merged-PR workflow sets `Status = Done`;
4. close an unmerged disposable PR and verify no built-in workflow marks it `Done`;
5. let an eligible disposable closed item satisfy the archive filter and verify it moves to the archive without losing fields;
6. reopen the disposable item and verify no unsafe automatic lifecycle reset occurs.

## Ownership and response

ChatGPT owns periodic anomaly review and proposes policy changes. Dmitry owns Project workflow configuration and any privileged
organization setting. On unexpected mass transitions, disable the affected built-in workflow, inspect Project history and source
events, and restore only the incorrectly changed items.

## Removal or migration

Retire the custom anomaly layer when GitHub Projects can express the same canonical-item-aware reconciliation natively. Keep the
supported merged-PR and archive workflows as long as Project 2 remains the delivery book of work.
