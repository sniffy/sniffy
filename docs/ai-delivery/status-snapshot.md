# AI delivery status snapshot

The status snapshot is a repository-owned, read-only materialized view of Sniffy organization Project 2 for clients that can read
repository issues and Actions artifacts but cannot reliably query organization ProjectV2 directly. It complements, and never
replaces, the guarded mutation protocol in [`control-plane.md`](control-plane.md).

## Components

```text
Project 2
  -> AI delivery status workflow
      -> raw field and item exports
      -> normalized active-item JSON
      -> short-retention Actions artifact
      -> newest open issue labeled ai-delivery-status
           contains the latest artifact pointer as JSON
```

The status issue is discovered by label. Its number is intentionally not configuration. The workflow creates the label and issue
when no open labeled issue exists and otherwise updates the newest open labeled issue. Older duplicates are not silently deleted;
the workflow warns in its summary and readers still choose the newest open issue by number.

The status issue is technical control-plane metadata. It is not a delivery work item, must not be claimed, and is excluded from the
normalized active queue together with issues labeled `ai-delivery-control`.

## Workflow triggers and freshness

[`.github/workflows/delivery-status.yml`](../../.github/workflows/delivery-status.yml) runs:

- at minute `5`, `20`, `35`, and `50` of every hour, ahead of the four ChatGPT event-loop slots;
- after non-PR completions of the `AI delivery control` workflow, so guarded mutations are materialized promptly;
- through `workflow_dispatch` for maintainer-authorized diagnosis or recovery;
- in validation-only mode for pull requests changing this adapter.

The publish job is serialized with `cancel-in-progress: false`. It always checks out trusted `develop`, including for
`workflow_run`, so a completed pull-request validation run cannot inject untrusted code into the secret-bearing publication job.

A ChatGPT tick accepts only the protocol, repository, Project number, artifact name, and files configured in
[`profile.yml`](profile.yml). The pointer and snapshot `generatedAt` timestamps must be no older than the configured maximum age.
A stale, missing, malformed, expired, or inaccessible snapshot blocks snapshot-dependent autonomous Project selection. It does not
justify guessing Project fields from prior chats, issue prose, or remembered state.

## Artifact contents

Every successful run uploads one artifact named `ai-delivery-status-project-2` with two-day retention:

- `project-2-status.json` — normalized active items and complete field definitions;
- `project-fields.raw.json` — unmodified `gh project field-list` JSON;
- `project-items.raw.json` — unmodified `gh project item-list` JSON;
- `repository-issues.raw.json` — live issue state and metadata from `gh issue list --state all`;
- `repository-pull-requests.raw.json` — live pull-request state, draft/head/base/ownership metadata, and labels from `gh pr list --state all`.

The normalizer fails when GitHub reports more fields or items than were returned, rather than publishing a silently truncated view.
Each normalized item contains:

- Project item ID;
- source type, repository, number, URL, title, state, draft flag, author, timestamps, and labels when available;
- a `fields` object with every current Project field name represented, using `null` when the item has no value;
- a `fieldValues` array preserving each field ID, name, data type, and value.

Field definitions preserve IDs, data types, options, and configuration returned by GitHub CLI. Readers use field names for policy
semantics and retain IDs/options for diagnosis; they must not invent values for missing fields.

## Active-item semantics

The normalized file includes:

- open issues;
- open pull requests, including drafts as source telemetry;
- Project draft issues;
- closed or merged source items whose Project `Status` is not `Done`, so lifecycle/source drift remains visible;
- open source items even when their Project status is terminal, so inverse drift remains visible.

`Done` is the only terminal Project lifecycle status for this filter. In particular, a closed source issue that remains in Project
`Draft`, `Planning`, `Implementation`, `Review`, `Verification`, or `Approval` stays visible for reconciliation.

It excludes technical items labeled `ai-delivery-control` or `ai-delivery-status`. The complete raw export remains available for
incident investigation.

The normalized snapshot is scoped to `sniffy/sniffy`. Project 2 may later contain other repositories; those items remain in the raw
export but are not eligible in the Sniffy active view.

## Pointer format

The status issue body is one JSON object:

```json
{
  "schemaVersion": 1,
  "kind": "ai-delivery-status/v1",
  "generatedAt": "2026-08-01T00:05:00Z",
  "source": {
    "repository": "sniffy/sniffy",
    "project": {"owner": "sniffy", "number": 2},
    "workflowRun": {
      "id": 123456789,
      "attempt": 1,
      "event": "schedule",
      "upstreamRunId": null,
      "headSha": "..."
    }
  },
  "artifact": {
    "id": 987654321,
    "name": "ai-delivery-status-project-2",
    "url": "...",
    "digest": "sha256:...",
    "snapshotFile": "project-2-status.json",
    "rawFieldsFile": "project-fields.raw.json",
    "rawItemsFile": "project-items.raw.json",
    "rawIssuesFile": "repository-issues.raw.json",
    "rawPullRequestsFile": "repository-pull-requests.raw.json",
    "retentionDays": 2
  },
  "counts": {
    "fieldCount": 8,
    "exportedItemCount": 100,
    "activeItemCount": 12
  }
}
```

Readers parse the body as JSON, reject unknown `kind` or `schemaVersion`, validate repository and Project identity, and use the
numeric artifact ID with the default GitHub connector's artifact download operation. The artifact URL is diagnostic; it is not a
credential and is not the primary download mechanism.

## ChatGPT read procedure

The exact Scheduled Task supplement lives in
[`.chatgpt/status-snapshot-instructions.md`](../../.chatgpt/status-snapshot-instructions.md). In summary, every stateless tick:

1. searches open `sniffy/sniffy` issues with label `ai-delivery-status` and selects the newest issue by number;
2. parses and validates the pointer body and freshness;
3. downloads the artifact by `artifact.id` through the default GitHub connector;
4. reads `project-2-status.json`, validates its identity, timestamp, and counts, and then uses its explicit fields for Project
   selection and guarded expected values;
5. re-reads live issue/PR, branch, head, review, assignment, and CI state from GitHub before any claim or source mutation.

The snapshot is not a lease and does not prove current ownership. A candidate may have changed after generation.

## Consistency and mutation boundary

The snapshot is eventually consistent. It is safe for discovery and deterministic candidate selection because every Project
mutation still goes through `delivery-control/v1`, whose serialized workflow re-reads live ProjectV2 and rejects stale expected
values before changing anything.

For a snapshot-backed client:

- `confused` means the snapshot/expected state lost a race; perform no work mutation and refresh later;
- `-1` means an unexpected control failure; inspect the control run and do not infer final state;
- `+1` means the control workflow applied or observed the requested final values and verified them internally.

After `+1`, a later dependent Project transition must use a status snapshot generated after that command. Work that does not require
another immediate Project write may proceed from the verified claim reaction plus live source state. The `workflow_run` trigger
normally refreshes the pointer promptly after control completion, while the scheduled runs provide bounded recovery if that event is
missed.

No client may mutate Project fields directly from the snapshot, treat the status issue as a command channel, or bypass exact-head
and expected-field guards.

## Permissions and failure response

The workflow starts with `permissions: {}`:

- validation receives `contents: read` only;
- publication receives `contents: read`, `issues: write`, and `pull-requests: read` only;
- Project reads use the existing `PROJECT_TOKEN` through GitHub CLI;
- artifact upload uses the Actions runtime and does not grant branch, merge, deployment, secret, or Project mutation authority.

On repeated failure, inspect:

- missing, expired, unapproved, or insufficient `PROJECT_TOKEN` Project access;
- GitHub CLI Project output/schema changes;
- pagination/truncation failures;
- artifact upload failure;
- repository issue or label write failure.

The snapshot workflow is a non-required operational signal, not source CI. A failed run prevents snapshot-dependent autonomous
selection until a fresh pointer exists; maintainers may use `workflow_dispatch` after correcting the cause.

## Removal condition

Remove this adapter when the default connected client can reliably read complete organization ProjectV2 state and discover the
latest artifact directly, or when another repository-owned read model provides equivalent freshness, least privilege, auditability,
and guarded-mutation compatibility.
