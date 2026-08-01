# Sniffy status-snapshot instructions for ChatGPT ticks

Apply this supplement at the start of every scheduled ChatGPT tick, before Project intake, reconciliation, continuation, or
ordinary queue selection. A complete direct organization ProjectV2 read may remain the selected read path, but it does not skip
this on-demand refresh barrier: the refreshed artifact is the deterministic fallback and durable read evidence for the tick.

1. Search `sniffy/sniffy` for open issues labeled `ai-delivery-status`. Select the newest non-pull-request issue by issue number.
   Never hardcode its number. Do not claim, assign, close, discuss work in, or add delivery-control commands to this technical
   issue.
2. Add exactly one comment whose entire body is `/ai-delivery-status refresh`. Record its comment ID and `createdAt`; do not add
   prose, Markdown wrappers, or another request for the same tick. Wait no more than `statusSnapshot.refreshTimeoutSeconds` from
   `docs/ai-delivery/profile.yml` for a terminal reaction on that exact comment:
   - `+1` means the workflow published a new artifact and pointer, then acknowledged this request;
   - `-1` means publication failed; report the refresh blocker and perform no snapshot-dependent claim or transition;
   - no terminal reaction before the deadline is an unavailable refresh, not permission to use the pre-request pointer. Do not
     retry in the same tick.
3. After `+1`, rediscover the newest open status issue and parse its entire body as JSON. Require:
   - `schemaVersion = 1`;
   - `kind = ai-delivery-status/v1`;
   - `source.repository = sniffy/sniffy`;
   - `source.project.owner = sniffy` and `source.project.number = 2`;
   - `artifact.name = ai-delivery-status-project-2`;
   - a numeric `artifact.id` and file name `project-2-status.json`;
   - `generatedAt` strictly later than the refresh request's `createdAt` and no older than `statusSnapshot.maxAgeMinutes`;
   - normally, `source.workflowRun.event = issue_comment` and `source.workflowRun.refreshRequest.commentId` equals the exact
     request comment ID. Because publication is serialized, a pointer subsequently replaced by another successful trigger is also
     acceptable when its `generatedAt` is strictly later than the request; it is necessarily at least as fresh.
4. Download that artifact by numeric ID with the default GitHub connector. Read `project-2-status.json` from the ZIP. Validate the
   same schema/kind/repository/Project identity, require its `generatedAt` and workflow-run provenance to match the pointer, and
   reject inconsistent counts or an incomplete/malformed file.
5. Use only the snapshot's explicit `fields`/`fieldValues` as ProjectV2 read state. An absent field value is represented as `null`;
   do not infer it from prose, authorship, prior chat state, or defaults. The file contains active Sniffy items only. The raw files
   are diagnostic and are not the ordinary queue.
6. Before ordinary queue selection, inspect the snapshot's top-level `pullRequests` observations for `mergeable = CONFLICTING` or
   `mergeStateStatus = DIRTY`. This collection is independent of Project item type; use `closingIssueNumbers` to resolve the
   canonical issue-versus-PR identity before routing, without activating a duplicate PR lifecycle item. Treat each same-repository,
   non-Dependabot agent PR as a priority continuation/reconciliation obligation. Re-read the live PR first and require it still to
   be open, target `develop`, have the same exact head, and remain conflicted. Preserve the existing branch and PR; deliberately
   route conflict resolution to an eligible implementation executor, then start the normal 15-minute, second-15-minute, and hourly
   observation cycle. Fork PRs wait for their contributor, and Dependabot PRs use `@dependabot rebase`; never adopt or rewrite those
   branches through this rule.
7. Re-read current issue/PR open/draft state, exact branch/head, formal closing links, assignment, reviews, threads, matching CI,
   and mergeability through live GitHub operations before acting. Snapshot source metadata and mergeability are discovery hints,
   not mutation authority.
8. Use snapshot values to construct guarded `delivery-control/v1` expected fields. A successful mutation still depends on the
   control workflow's live serialized ProjectV2 comparison and verification.
9. On `confused`, perform no work mutation and do not retry from the same snapshot. On `-1`, inspect the failed control run. On
   `+1`, the control workflow's internal final-state verification is authoritative for that command.
10. Before a later dependent Project transition, obtain a snapshot generated after the preceding successful command. The status
    workflow is triggered after delivery-control completion; request another on-demand refresh only when that event did not yield
    a sufficiently new pointer. Do not chain dependent Project writes from an older materialized view.
11. If the status issue, refresh acknowledgement, pointer, artifact, or snapshot is missing, expired, inaccessible, malformed,
    mismatched, or stale, make no snapshot-dependent Project claim or transition. Report the exact status-read blocker; do not
    guess.
12. Exclude issues labeled `ai-delivery-control` or `ai-delivery-status` from canonical work selection even if Project automation
    materialized them.
