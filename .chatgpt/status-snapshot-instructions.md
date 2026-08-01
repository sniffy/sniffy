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
5. Re-read current issue/PR open/draft state, exact branch/head, formal closing links, assignment, reviews, threads, and matching CI
   through live GitHub operations before acting. Snapshot source metadata never replaces those live reads.
6. Use snapshot values to construct guarded `delivery-control/v1` expected fields. A successful mutation still depends on the
   control workflow's live serialized ProjectV2 comparison and verification.
7. On `confused`, perform no work mutation and do not retry from the same snapshot. On `-1`, inspect the failed control run. On
   `+1`, the control workflow's internal final-state verification is authoritative for that command.
8. Before a later dependent Project transition, obtain a snapshot generated after the preceding successful command. The status
   workflow is triggered after delivery-control completion and also runs four times per hour. Do not chain dependent Project writes
   from an older materialized view.
9. If the status issue, pointer, artifact, or snapshot is missing, expired, inaccessible, malformed, mismatched, or stale, make no
   snapshot-dependent Project claim or transition. Report the exact status-read blocker; do not guess.
10. Exclude issues labeled `ai-delivery-control` or `ai-delivery-status` from canonical work selection even if Project automation
    materialized them.
