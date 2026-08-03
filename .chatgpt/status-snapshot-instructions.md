# ChatGPT status-snapshot read barrier

Use this once at the start of each scheduled Chat tick. It supplies a deterministic queue projection so the model does not need to
query the whole Project or inspect every open PR.

1. Find the newest open non-PR issue in `sniffy/sniffy` labelled `ai-delivery-status`. Never hardcode its number. Exclude technical
   `ai-delivery-control` and `ai-delivery-status` issues from work selection.
2. Add exactly one comment whose entire body is `/ai-delivery-status refresh`. Record its comment ID and `createdAt`. Wait at most
   `statusSnapshot.refreshTimeoutSeconds` from `docs/ai-delivery/profile.yml` for a terminal reaction on that exact comment:
   - `+1`: a newer pointer/artifact was published;
   - `-1`: publication failed;
   - timeout: refresh unavailable.
   On `-1` or timeout, make no snapshot-dependent claim or transition. Do not retry or use the pre-request pointer.
3. After `+1`, rediscover the newest open status issue and parse its body as JSON. Require schema 1, kind
   `ai-delivery-status/v1`, repository `sniffy/sniffy`, Project `sniffy/2`, artifact name `ai-delivery-status-project-2`, numeric
   artifact ID, snapshot file `project-2-status.json`, acceptable age, and `generatedAt` strictly later than the request. Normally
   require matching refresh comment provenance; a later serialized successful publication is acceptable when demonstrably newer.
4. Download that artifact by numeric ID and validate the snapshot has matching identity, generation, provenance, complete counts,
   top-level `pullRequests`, and `dispatch` projections. Reject malformed, inconsistent, incomplete, stale, or inaccessible data.
5. Use `dispatch.orderedCandidates` as the normal attention queue and select at most its first entry. Do not ask the model to
   rebuild `readyByExecutor`, `inProgressByExecutor`, canonical PR identity, route mismatches, conflicts, or stale exact-head
   candidates from raw arrays. Raw files are diagnostics only.
6. The top-level `pullRequests` observations and `dispatch.pullRequests` projections cover every open base-branch PR independently
   of Project item type. Use `closingIssueNumbers` to resolve the canonical issue-versus-PR identity. Fork PRs wait for their
   contributor. Dependabot PRs use provider bot operations such as `@dependabot rebase`; never adopt or rewrite those branches.
7. Only after selecting a candidate, re-read its current live issue/PR open/draft state, exact branch/head, formal closing links,
   repository ownership, assignment, Project route, worker reference, reviews/threads/CI/mergeability when relevant, and active
   control issue. Snapshot mergeability and lifecycle values are selection hints, not mutation authority.
8. Construct guarded `delivery-control/v1` expected state from the snapshot, but act only after the targeted live re-read. On a
   `confused` reaction, perform no work mutation and do not retry from the same snapshot. On `-1`, inspect the failed control run.
   On `+1`, the control workflow's final-state verification is authoritative for that command.
9. Before a later dependent Project transition, request one snapshot generated after the preceding successful command. Do not add
   speculative refreshes between independent reads or chain Project writes from an older materialized view.
10. If no candidate remains actionable after the live re-read, return `NO_CHANGE` plus telemetry without a target, source, worker,
    or control mutation.
