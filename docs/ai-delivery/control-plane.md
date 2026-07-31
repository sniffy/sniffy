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

### Review handoff and pull-request identity

A command that sets `Status = Review` must identify the exact pull request that is becoming review input.

When the pull request itself is the canonical Project item, `target`, `expected.type = PullRequest`, and `expected.head` already
provide that identity:

```json
{
  "command": "delivery-control/v1",
  "target": {"repository": "sniffy/sniffy", "number": 765},
  "expected": {
    "type": "PullRequest",
    "head": "8f1be923b6c38e7afb20b7678914c65a8ca8a6c8",
    "fields": {
      "Status": "Implementation",
      "Execution": "In progress",
      "Executor": "ChatGPT"
    }
  },
  "set": {
    "Status": "Review",
    "Execution": "Ready",
    "Executor": "ChatGPT",
    "Worker reference": "PR #765; exact head 8f1be923b6c38e7afb20b7678914c65a8ca8a6c8"
  }
}
```

When an issue is canonical, the command additionally supplies a structured `reviewPullRequest` guard. The PR must formally close
that issue:

```json
{
  "command": "delivery-control/v1",
  "target": {"repository": "sniffy/sniffy", "number": 762},
  "expected": {
    "type": "Issue",
    "fields": {
      "Status": "Implementation",
      "Execution": "In progress",
      "Executor": "Local Codex"
    }
  },
  "reviewPullRequest": {
    "number": 763,
    "head": "6d45aff9902b8da1701300388be77e96c9b16a25"
  },
  "set": {
    "Status": "Review",
    "Execution": "Ready",
    "Executor": "ChatGPT",
    "Worker reference": "PR #763; exact head 6d45aff9902b8da1701300388be77e96c9b16a25"
  }
}
```

`reviewPullRequest` is not free-form evidence. It is accepted only on a transition that sets `Status = Review`. For a PR target,
any supplied value must exactly match the target number and `expected.head`. For an issue target it is mandatory.

### Required guards

Every command must include at least one guard under `expected`:

- `type`: `Issue` or `PullRequest`;
- `head`: exact pull-request head commit ID;
- `fields`: exact current ProjectV2 values, where `null` means cleared or absent.

Unguarded mutations are rejected. A command setting `Execution = In progress` is treated as a claim and must guard
`Execution = Ready`, current `Status`, current `Executor`, target type, and the exact head for a pull request. It must also set a
unique `Worker reference` containing token, owner, and lease.

A Review transition has stronger publication guards. Its PR must be open, target `develop`, have the exact guarded head, and be
non-draft. When the canonical target is an issue, the PR must formally close that issue.

### Mutations

- `set` maps text or single-select field names to complete values.
- `clear` lists fields to clear.
- A field cannot appear in both collections.
- A field omitted from both `set` and `clear` is not read as a required routing value and is left unchanged.
- A missing Project field value is valid lifecycle state. In particular, an empty `Implementer` means unknown or not deliberately
  selected; commands must omit it rather than inventing an `Unknown`, PR-author, bot, or provider value.
- `addIfMissing` permits external-PR intake to add the target to Project 2 before applying the guarded initial fields.
- Only supported ProjectV2 text and single-select fields may be changed.

GitHub assignment, review submission, source publication, CI, and issue/PR comments remain separate explicit operations. The one
repository-side mutation deliberately coupled to `Status = Review` is ready-for-review state: after all target and Project guards
pass, the control plane marks a draft PR ready and re-reads it before writing the Project status. After Project fields are written
and verified, it re-reads the review PR again after the Project mutation before returning success. Executors should already have
done this themselves; the automation is the final invariant and recovery path.

## Claim and transition semantics

The workflow has a parse/authorization job followed by a transition job. The transition job uses a concurrency group derived
from the target repository and item number, with `cancel-in-progress: false`.

For each command it:

1. validates the actor, control-issue label, JSON schema, repository, and guarded mutation;
2. enters the target-item concurrency group;
3. queries the target issue/PR, exact target head when applicable, Project 2, field definitions, Project item, and current values;
4. optionally adds a missing Project item only when `addIfMissing` is true;
5. compares every `expected` guard to the current state;
6. pre-validates every requested field, type, and single-select option before mutation; omitted fields need no option lookup;
7. for a Review transition, queries the structured review PR, verifies repository/base/open/head/formal-closing identity, marks it
   ready when draft, and re-reads it as non-draft at the same exact head;
8. applies the pre-validated Project set/clear operations inside the same serialized Actions job;
9. re-reads the Project item and verifies every requested final value;
10. re-reads the review PR again after the Project mutation and verifies open/non-draft/exact-head/formal-closing state;
11. records the PR readiness result and Project transition in the Actions summary.

Two concurrent claim commands may both parse, but only one can observe the guarded `Ready` state after entering the target-item
concurrency group. The other returns a non-mutating conflict. No claim-intent, winner, loser, or withdrawal comment is needed on
the target item.

GitHub Project field updates are not a database transaction. The protocol is best-effort atomic: it validates every expected state
before changing the PR or Project. If marking a PR ready succeeds but a later Project write unexpectedly fails, the safe one-way
publication change remains and the workflow receives `-1`; inspect the run and retry the same guarded transition after re-reading
state. If the final PR re-read detects a head or state race after the Project write, the workflow also receives `-1` so a later
reconciliation can restore consistent exact-head Review state. A draft PR is never hidden behind a successful `+1` Review result.

## Outcomes and audit

For commands posted to the control issue:

- `+1` reaction: PR readiness and Project transition were applied and verified, or both were already in the requested final state;
- `confused` reaction: expected-state or review-PR identity conflict, with no requested mutation;
- `-1` reaction: unexpected execution failure; inspect the Actions run.

The command comment, reaction, Actions run, PR state, and current Project state form the technical audit trail. The target
conversation contains only human-useful evidence or decisions.

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
`contents: read`, `issues: write` for the command reaction, and narrowly scoped `pull-requests: write` solely to mark the guarded
review PR ready. ProjectV2 GraphQL continues to use the existing `PROJECT_TOKEN`; the repository `GITHUB_TOKEN` is a separate
client and is not used for Project mutation. No contents write, branch write, merge, auto-merge, ruleset bypass, deployment,
environment, secret, or hosting permission is added.

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

After merge, extend the existing disposable control-plane smoke test:

1. transition a draft standalone PR target to Review and confirm it becomes non-draft before Project `Status = Review`;
2. transition a canonical issue using a formally closing draft PR and confirm the same invariant;
3. use a wrong head, wrong base, closed PR, and non-closing issue/PR pair and confirm non-mutating conflicts;
4. repeat a successful command and confirm idempotent `+1`;
5. verify the target issue/PR conversation received no technical command comment;
6. inspect the Actions summary, final PR state, and final Project fields.

Do not rely on the extended path for autonomous Review handoffs until these checks pass on the merged `develop` workflow.