# Sniffy status-snapshot instructions for ChatGPT ticks

Apply this supplement before Project intake, reconciliation, continuation, or ordinary queue selection when direct organization
ProjectV2 reads are unavailable or fail.

1. Search `sniffy/sniffy` for open issues labeled `ai-delivery-status`. Select the newest non-pull-request issue by issue number.
   Never hardcode its number. Do not claim, assign, close, discuss work in, or add control commands to this technical issue.
2. Parse the entire issue body as JSON. Require:
   - `schemaVersion = 1`;
   - `kind = ai-delivery-status/v1`;
   - `source.repository = sniffy/sniffy`;
   - `source.project.owner = sniffy` and `source.project.number = 2`;
   - `artifact.name = ai-delivery-status-project-2`;
   - a numeric `artifact.id` and file name `project-2-status.json`;
   - `generatedAt` no older than `statusSnapshot.maxAgeMinutes` in `docs/ai-delivery/profile.yml`.
3. Download that artifact by numeric ID with the default GitHub connector. Read `project-2-status.json` from the ZIP. Validate the
   same schema/kind/repository/Project identity, require its `generatedAt` to match the pointer, and reject inconsistent counts or
   an incomplete/malformed file.
4. Use only the snapshot's explicit `fields`/`fieldValues` as ProjectV2 read state. An absent field value is represented as `null`;
   do not infer it from prose, authorship, prior chat state, or defaults. The file contains active Sniffy items only. The raw files
   are diagnostic and are not the ordinary queue.
5. Before ordinary queue selection, inspect the snapshot's top-level `pullRequests` observations for `mergeable = CONFLICTING` or
   `mergeStateStatus = DIRTY`. This collection is independent of Project item type; use `closingIssueNumbers` to resolve the
   canonical issue-versus-PR identity before routing, without activating a duplicate PR lifecycle item. Treat each same-repository,
   non-Dependabot agent PR as a priority continuation/reconciliation obligation. Re-read the live PR first and require it still to
   be open, target `develop`, have the same exact head, and remain conflicted. Preserve the existing branch and PR; deliberately
   route conflict resolution to an eligible implementation executor, then start the normal 15-minute, second-15-minute, and hourly
   observation cycle. Fork PRs wait for their contributor, and Dependabot PRs use `@dependabot rebase`; never adopt or rewrite those
   branches through this rule.
6. Re-read current issue/PR open/draft state, exact branch/head, formal closing links, assignment, reviews, threads, matching CI,
   and mergeability through live GitHub operations before acting. Snapshot source metadata and mergeability are discovery hints,
   not mutation authority.
7. Use snapshot values to construct guarded `delivery-control/v1` expected fields. A successful mutation still depends on the
   control workflow's live serialized ProjectV2 comparison and verification.
8. On `confused`, perform no work mutation and do not retry from the same snapshot. On `-1`, inspect the failed control run. On
   `+1`, the control workflow's internal final-state verification is authoritative for that command.
9. Before a later dependent Project transition, obtain a snapshot generated after the preceding successful command. The status
   workflow is triggered after delivery-control completion and also runs four times per hour. Do not chain dependent Project writes
   from an older materialized view.
10. If the status issue, pointer, artifact, or snapshot is missing, expired, inaccessible, malformed, mismatched, or stale, make no
    snapshot-dependent Project claim or transition. Report the exact status-read blocker; do not guess.
11. Exclude issues labeled `ai-delivery-control` or `ai-delivery-status` from canonical work selection even if Project automation
    materialized them.
