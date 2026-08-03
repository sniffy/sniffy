# AI delivery status snapshot

The status snapshot is a repository-owned, read-only materialized view of Sniffy Project 2. It complements and never replaces the
guarded mutation protocol in [`control-plane.md`](control-plane.md).

## Publication flow

```text
Project 2 + repository issues/PRs/formal links
  -> hourly or exact /ai-delivery-status refresh trigger
  -> trusted develop workflow
  -> normalized active items + open PR observations
  -> deterministic dispatch projection
  -> short-retention Actions artifact
  -> newest open issue labeled ai-delivery-status contains JSON pointer
  -> +1/-1 on the exact refresh request
```

The issue is discovered by label, never hardcoded by number. Technical `ai-delivery-status` and `ai-delivery-control` issues are
excluded from delivery work.

## Triggers and read barrier

`.github/workflows/delivery-status.yml` runs hourly at minute 5, on an exact allowlisted refresh comment, through maintainer
`workflow_dispatch`, and in validation-only mode for relevant PRs. Publication is serialized with queued concurrency and always
checks out trusted `develop`.

A scheduled ChatGPT tick posts exactly `/ai-delivery-status refresh` and waits for a terminal reaction:

- `+1`: artifact and pointer were published;
- `-1`: publication failed;
- timeout/no reaction: unavailable.

A failed/unacknowledged request blocks snapshot-dependent selection. Do not retry in the same tick or use the pre-request pointer.
After `+1`, require matching protocol/repository/Project/artifact identity, acceptable age, `generatedAt` later than the request,
and normally exact refresh-comment provenance. A later serialized successful pointer is acceptable only when demonstrably newer.

## Artifact

The artifact `ai-delivery-status-project-2` contains:

- `project-2-status.json` — normalized active items, field definitions, open PR observations, and `dispatch` projection;
- `project-fields.raw.json` — diagnostic raw Project fields;
- `project-items.raw.json` — diagnostic raw Project items;
- `repository-issues.raw.json` — diagnostic live issue metadata;
- `repository-pull-requests.raw.json` — diagnostic live PR metadata.

The normalizer fails closed on incomplete pagination or missing live source state. Explicit absent Project field values are `null`;
clients must not infer them from prose, authorship, prior chats, or defaults.

## Open PR observations

Top-level `pullRequests` includes every open repository PR independently of canonical Project representation: number/URL/author,
same-repository or fork ownership, draft/base/branch/exact head, mergeability hint, merge-state hint, and formal closing issue
numbers. A PR observation does not create a second lifecycle item.

Canonical identity remains:

```text
one closing issue -> issue
no closing issue  -> PR
multiple issues   -> PR in Planning
```

## Dispatch projection

`.github/scripts/delivery-dispatch-view.js` adds deterministic runtime views so an LLM does not rescan or re-sort the entire
Project and PR list:

- `readyByExecutor`;
- `inProgressByExecutor` and due observations when `nextObservationAt` is available;
- Ready route/assignee mismatches;
- conflicting same-repository PRs;
- PR intake/canonicalization and duplicate-representation candidates;
- downstream lifecycle state pinned to an older exact PR head;
- `orderedCandidates`, the normal one-candidate attention queue.

The projection contains compact target evidence only. It is not a substitute for complete issue/PR discussion, diff, review, CI,
or artifacts; those are loaded after selection by the worker that needs them.

## Active-item semantics

The normalized view keeps open issues/PRs, Project draft issues, and source-closed items whose Project Status is not Done, exposing
lifecycle/source drift. It excludes technical control/status items and other repositories from the Sniffy active view. Raw exports
remain available for diagnosis.

## Pointer

The newest open status issue body is one JSON pointer with schema/kind, generated time, source repository/Project/workflow run,
optional refresh request, numeric artifact ID/name/digest/file names/retention, and counts. Readers reject unknown schema/kind,
wrong identity, nonnumeric artifact ID, stale/mismatched provenance, or inconsistent counts.

## ChatGPT procedure

The exact compact procedure lives in
[`.chatgpt/status-snapshot-instructions.md`](../../.chatgpt/status-snapshot-instructions.md): request one fresh barrier; validate and
download the artifact; select at most the first `dispatch.orderedCandidates` entry; live-read only that target; construct guarded
expected values; stop without work mutation when no candidate remains actionable.

## Consistency and mutation boundary

The snapshot is eventually consistent and safe only for discovery/selection. Every claim/handoff still goes through
`delivery-control/v1`, whose serialized workflow live-reads ProjectV2 and rejects stale expected state.

- `confused`: state/head lost a race; make no work mutation from that snapshot;
- `-1`: unexpected control failure; inspect the run;
- `+1`: requested final state was applied/observed and verified.

A later dependent Project transition requires a snapshot generated after the preceding successful command. Avoid speculative
refresh fan-out.

No client may mutate Project fields directly from the snapshot, use the status issue as a control-command channel, bypass guarded
exact-head checks, or treat generated candidates as claim ownership.

## Permissions and failures

The workflow begins with `permissions: {}`. Validation receives contents read. Publication receives contents read, issues write,
and pull-requests read; Project reads use the existing repository secret. It receives no branch write, merge, deployment, secret,
or Project mutation permission.

On failure inspect Project-token access, GitHub CLI output/schema/pagination, trusted checkout, normalizer/projection tests, artifact
upload, pointer update, and reaction write. Until a fresh valid pointer exists, snapshot-dependent autonomous selection stops.
