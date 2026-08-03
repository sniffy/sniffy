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

The status issue is discovered by label, never hardcoded. Technical status/control issues are excluded from delivery work.

## Triggers and read barrier

`.github/workflows/delivery-status.yml` runs hourly at minute 5, on an exact allowlisted refresh comment, through maintainer
`workflow_dispatch`, and in validation-only mode for relevant PRs. Publication is serialized and checks out trusted `develop`.

A scheduled ChatGPT tick posts exactly `/ai-delivery-status refresh` and waits for a terminal reaction:

- `+1`: artifact/pointer published;
- `-1`: publication failed;
- timeout: unavailable.

Failure blocks snapshot-dependent selection. Do not retry in the same tick or use the pre-request pointer. After `+1`, require
matching protocol/repository/Project/artifact identity, acceptable age, `generatedAt` later than the request, and matching or
demonstrably newer serialized provenance.

## Artifact

`ai-delivery-status-project-2` contains normalized `project-2-status.json` plus diagnostic raw Project, issue, and PR exports. The
normalizer fails closed on incomplete pagination or missing live state. Explicit absent Project field values are `null`; clients do
not infer them from prose, authorship, earlier chats, or defaults.

Top-level `pullRequests` includes every open repository PR independently of canonical Project representation: author, ownership,
draft/base/branch/exact head, mergeability hints, and formal closing issues. A PR observation does not create a second lifecycle
item.

## Dispatch projection

`.github/scripts/delivery-dispatch-view.js` adds deterministic runtime views:

- `readyByExecutor`;
- `inProgressByExecutor`;
- `activeInProgressByExecutor` for valid future leases;
- `staleInProgressByExecutor` for missing/invalid worker evidence or expired lease;
- Ready route/assignee mismatches;
- same-repository and managed PR conflicts;
- PR intake/canonicalization and duplicate candidates;
- downstream state pinned to an older exact PR head;
- `orderedCandidates`.

Active leased ownership is diagnostic/capacity state, not an attention candidate. It is not polled. Stale ownership becomes one
`stale-owned-recovery` candidate for targeted recovery of the exact generation.

An already routed `Implementation / Ready|In progress` correction remains with its selected executor instead of being reclassified
as generic intake/conflict handling.

## Active-item and consistency semantics

The normalized view keeps open issues/PRs, Project draft issues, and source-closed items whose Project Status is not Done, exposing
lifecycle/source drift. It excludes technical items and other repositories from the Sniffy active view.

The snapshot is eventually consistent and safe only for discovery/selection. Every claim/handoff still goes through
`delivery-control/v1`, whose serialized workflow live-reads ProjectV2 and rejects stale expected state:

- `confused`: state/head lost a race; make no work mutation from that snapshot;
- `-1`: unexpected control failure; inspect the run;
- `+1`: requested final state was applied/observed and verified.

A later dependent Project transition requires a snapshot generated after the preceding successful command. Avoid speculative
refresh fan-out.

## ChatGPT procedure

The compact procedure lives in [`.chatgpt/status-snapshot-instructions.md`](../../.chatgpt/status-snapshot-instructions.md): request
one fresh barrier; validate/download the artifact; select at most the first `dispatch.orderedCandidates` entry; live-read only that
target; construct guarded expected values; stop without work mutation when no candidate remains actionable.

No client may mutate Project fields directly from the snapshot, use the status issue as a control channel, bypass guarded exact-head
checks, or treat a generated candidate as claim ownership.

## Permissions and failures

The workflow begins with `permissions: {}`. Validation receives contents read. Publication receives contents read, issues write, and
pull-requests read; Project reads use the repository secret. It receives no branch write, merge, deployment, secret, or Project
mutation permission.

On failure inspect Project-token access, CLI schema/pagination, trusted checkout, normalizer/projection tests, artifact upload,
pointer update, and reaction write. Until a fresh valid pointer exists, snapshot-dependent autonomous selection stops.
