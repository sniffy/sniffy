# Project 2 housekeeping setup runbook

This runbook translates [`project-housekeeping.md`](project-housekeeping.md) into maintainable GitHub Project configuration. Project
workflow configuration is a privileged organization operation performed by Dmitry in the Project UI. ProjectV2 field transitions
used for reconciliation still go through the common control issue.

## Built-in completion workflows

Open organization Project 2, choose **Workflows**, and inspect the default workflows. Apply these settings:

| Documented workflow | Actual scope | Project 2 setting |
| --- | --- | --- |
| Issues or pull requests closed | issue and pull-request items | Leave disabled |
| Pull requests merged | pull-request items | Enable and set `Status` to `Done` |

GitHub does not document an issue-only scope for the combined close workflow. Do not enable it or configure another generic
pull-request-close-to-Done rule. Reconcile closed canonical issues separately through the guarded control plane. A closed
implementation-evidence PR must not complete an open canonical issue, and a closed-unmerged standalone PR may require explicit
abandonment or supersession evidence.

## Auto-archive workflow

GitHub auto-archive does not filter on custom Project fields. After the merged-PR workflow and initial backlog reconciliation have
been smoke-tested, enable Project auto-archive with the supported source-state filter:

```text
is:closed updated:<@today-14d
```

Verify the rule against an eligible disposable closed item before enabling it broadly. The source-state filter also matches
closed-unmerged PRs regardless of their Project `Status`; the 14-day delay is the reconciliation window, not a completion signal.

The rule must archive rather than delete. Archived items remain available for audit and restoration, but still count toward the
Project's total item limit. If the Project approaches that platform limit, use a separately reviewed export-and-delete retention
procedure rather than broad automatic deletion.

## Smoke test

Use disposable items that do not close real delivery work:

1. add a disposable issue to Project 2 and set a non-terminal Status;
2. close it and verify the disabled combined close workflow leaves Status unchanged;
3. reconcile the closed issue through one guarded `delivery-control/v1` command and verify `Done`;
4. add a disposable standalone PR item, merge it, and verify the merged-PR workflow sets `Done`;
5. close an unmerged disposable PR and verify no built-in workflow marks it `Done`;
6. confirm no target-item automation comment was created;
7. let an eligible disposable closed item satisfy `updated:<@today-14d` and verify archival preserves fields;
8. reopen the disposable issue and verify no automatic reverse lifecycle transition occurs.

## Existing backlog cleanup

Before relying on automation, perform one explicit reconciliation of existing active-view drift. Submit every Project field
transition through the common control issue as a guarded `delivery-control/v1` command:

- closed issues and merged canonical PRs that are complete -> `Done`;
- closed unmerged PRs -> inspect canonical links before changing;
- open items already in `Done` -> return to the event loop for deliberate reconciliation;
- technical status/control issues -> remove from active delivery views or ensure the normalizer excludes them;
- `Ready` items without Executor and agent routes with human-only assignment -> correct deliberately rather than marking Done.

After cleanup, generate a fresh `ai-delivery-status` snapshot and confirm active-item counts reflect only open work plus intentional
drift.

## Monitoring

Review the snapshot anomaly counts weekly until the merged-PR and archive workflows have demonstrated stable behavior. A future
daily custom reconciler may automate only the deterministic terminal cases documented in the policy; ambiguous cases remain
report-only.

If a built-in workflow performs an unexpected mass transition, disable it immediately, inspect the Project history and source
events, and restore only affected items. Do not compensate with a second broad workflow.
