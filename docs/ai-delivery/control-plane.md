# AI delivery control plane

This document defines the one provider-neutral mutation protocol used by ChatGPT, Codex Cloud, Local Codex, and human/CLI
operators to update Sniffy ProjectV2 delivery state. It replaces target-item `/project-status` and `/project-field` command
comments and the pull-request claim-intent journal.

The lifecycle model remains in [`lifecycle.md`](lifecycle.md). This document only defines how a guarded state transition is
requested, serialized, applied, audited, and verified.

## Why the protocol exists

The connected executors do not all have reliable direct ProjectV2 mutation support. Previously, ChatGPT posted one command
comment per field to the target issue or pull request, while local tools could use GraphQL directly. That created two problems:

- human review conversations filled with technical field updates and claim arbitration;
- different executors used different mutation, validation, and concurrency rules.

All configured executors now use the same control issue command. `workflow_dispatch` is an administrative/debug entry point to
the same parser and transition implementation, not a separate executor protocol.

## Active control issue

Maintain one open technical issue with the label `ai-delivery-control`. Use a human-readable title such as
`AI Delivery Control Log — 2026-07`.

The issue is machine-oriented:

- post only complete JSON commands;
- do not discuss product or review decisions there;
- keep meaningful reviews, verification evidence, blockers, and human handoffs on the target issue or pull request;
- do not delete completed commands merely to reduce visual noise.

Executors query open issues with the configured label and use the newest one by issue number. If none exists, create one with the
label. Immediately before posting, re-read the issue and confirm that it is still open and still the newest open labeled issue.

## Command format

A command comment is one valid JSON object. Pretty-printed JSON is allowed.

```json
{
  "command": "delivery-control/v1",
  "target": {
    "repository": "sniffy/sniffy",
    "number": 651
  },
  "expected": {
    "type": "PullRequest",
    "head": "46c47e02e8a5236cf1e7348fc6202da11ea1bbd3",
    "fields": {
      "Status": "Review",
      "Execution": "Ready",
      "Executor": "ChatGPT"
    }
  },
  "set": {
    "Execution": "In progress",
    "Worker reference": "Sniffy Tick :00; token review-651-46c47e0; lease 2026-07-30T10:15:00Z"
  },
  "addIfMissing": false
}
```

### Required guards

Every command must include at least one guard under `expected`:

- `type`: `Issue` or `PullRequest`;
- `head`: exact pull-request head commit ID;
- `fields`: exact current ProjectV2 values, where `null` means cleared or absent.

Unguarded mutations are rejected. A command setting `Execution = In progress` is treated as a claim and must guard
`Execution = Ready`, current `Status`, current `Executor`, target type, and the exact head for a pull request. It must also set a
unique `Worker reference` containing token, owner, and lease.

### Mutations

- `set` maps text or single-select field names to complete values.
- `clear` lists fields to clear.
- A field cannot appear in both collections.
- A field omitted from both `set` and `clear` is not read as a required routing value and is left unchanged.
- A missing Project field value is valid lifecycle state. In particular, an empty `Implementer` means unknown or not deliberately
  selected; commands must omit it rather than inventing an `Unknown`, PR-author, bot, or provider value.
- `addIfMissing` permits external-PR intake to add the target to Project 2 before applying the guarded initial fields.
- Only supported ProjectV2 text and single-select fields may be changed.

The command updates Project fields only. GitHub assignment, review submission, source publication, CI, and issue/PR comments remain
separate explicit operations whose success must be verified before the lifecycle transition claims them as evidence.

## Claim and transition semantics

The workflow has a parse/authorization job followed by a transition job. The transition job uses a concurrency group derived
from the target repository and item number, with `cancel-in-progress: false`.

For each command it:

1. validates the actor, control-issue label, JSON schema, repository, and guarded mutation;
2. enters the target-item concurrency group;
3. queries the target issue/PR, exact PR head when applicable, Project 2, field definitions, Project item, and current field values;
4. optionally adds a missing Project item only when `addIfMissing` is true;
5. compares every `expected` guard to the current state;
6. pre-validates every requested field, type, and single-select option before mutation; omitted fields need no option lookup;
7. applies the pre-validated set/clear operations inside the same serialized Actions job;
8. re-reads the Project item and verifies every requested final value;
9. records a structured Actions job summary.

Two concurrent claim commands may both parse, but only one can observe the guarded `Ready` state after entering the target-item
concurrency group. The other returns a non-mutating conflict. No claim-intent, winner, loser, or withdrawal comment is needed on
the target item.

GitHub Project field updates are not a database transaction. The protocol is best-effort atomic: it pre-validates all requested
changes, applies them only inside one serialized job, then verifies the complete post-state. An unexpected partial or unverifiable result
fails the workflow and must be inspected before retrying.

## Outcomes and audit

For commands posted to the control issue:

- `+1` reaction: transition applied and verified, or the guarded target was already in the requested final state;
- `confused` reaction: expected-state conflict, with no requested field mutation;
- `-1` reaction: unexpected execution failure; inspect the Actions run.

The command comment, reaction, Actions run, and current Project state form the technical audit trail. The target conversation
contains only human-useful evidence or decisions.

`workflow_dispatch` accepts the same JSON object in its `command` input and invokes the same implementation. Use it only for
maintainer-authorized debugging or recovery when posting through the control issue is unavailable. Autonomous executors must
not silently choose a different path.

## Authorized actors and permissions

Built-in command actors are:

- `bedrin`;
- `bedrin-gpt`;
- `bedrin-codex-cloud`;
- `bedrin-codex-local`.

Additional logins may be supplied through the comma-separated repository variable `AI_DELIVERY_COMMAND_ACTORS`; the existing
`PRODUCT_MANAGER_LOGIN` variable is also honored.

The workflow starts with `permissions: {}`. The parse job receives only `contents: read` for checkout. The transition job receives
`contents: read` and `issues: write` for the command reaction. ProjectV2 GraphQL uses the existing repository secret
`PROJECT_TOKEN`. No merge, auto-merge, branch write, ruleset bypass, deployment, environment, secret, or hosting permission is
added.

## Control-log rotation

Rotation is normal event-loop maintenance, not a separate scheduled task. ChatGPT as supervisor rotates the log when either:

- the active issue has approximately 500 command comments; or
- the issue has been active for about 30 days and a new command is needed.

Rotation procedure:

1. verify that every existing command has a terminal reaction or an inspected failed run;
2. create a successor issue with the same label and the next period in its title;
3. close the old issue without deleting comments;
4. query open issues with the label and verify that the successor is the newest one;
5. post the next command only after that verification.

A normal queue tick may perform rotation immediately before a needed command. No independent cleanup clock, browser automation,
private API, or bulk deletion is permitted.

## Target-conversation policy

Post on the target issue or pull request only when a maintainer or contributor benefits from the information, including:

- the authoritative plan or later superseding decision;
- one comprehensive review or precise Request Changes;
- implementation publication and exact-head proof;
- verification result;
- a real blocker requiring Dmitry;
- a linked replacement issue/PR or other meaningful lifecycle handoff.

Do not post field commands, claim tokens, lease arbitration, winner/loser messages, or routine polling notices there.

## Rollout and smoke test

The old `project-status.yml` and `project-field.yml` workflows are removed with this protocol. After merge:

1. ensure the `ai-delivery-control` label exists and create the first active control issue with it;
2. run one successful guarded multi-field transition on a disposable item;
3. repeat the same command and confirm idempotent success;
4. use a stale expected value and confirm a non-mutating conflict;
5. submit two concurrent guarded claims and confirm one success plus one conflict;
6. verify the target issue/PR conversation received no technical command comment;
7. inspect the Actions summaries and final Project fields.

Do not rely on the new path for autonomous claims until these checks pass on the merged default-branch workflow.
