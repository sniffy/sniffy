# Project 2 housekeeping setup runbook

This runbook translates [`project-housekeeping.md`](project-housekeeping.md) into maintainable GitHub Project configuration. These
steps are privileged organization operations and are performed by Dmitry in the Project UI.

## Built-in completion workflows

Open organization Project 2, choose **Workflows**, and inspect the default workflows. Enable or create rules with these outcomes:

| Source event | Filter | Project action |
| --- | --- | --- |
| Issue closed | canonical issue items | Set `Status` to `Done` |
| Pull request merged | canonical PR items | Set `Status` to `Done` |

Do not configure a generic pull-request-close rule until the closed-unmerged canonicalization cases in
[`project-housekeeping.md`](project-housekeeping.md) are covered. A closed implementation-evidence PR must not complete an open
canonical issue.

## Auto-archive workflow

Enable Project auto-archive with this initial filter:

```text
Status:Done updated:<@today-14d
```

If the UI requires lower-case field syntax, use the equivalent filter it previews. Verify the rule against a disposable completed
item before enabling it broadly.

The rule must archive rather than delete. Archived items remain available for audit and restoration.

## Smoke test

Use disposable items that do not close real delivery work:

1. add a disposable issue to Project 2 and set a non-terminal Status;
2. close it and verify the built-in workflow changes Status to `Done`;
3. add a disposable standalone PR item, merge it, and verify `Done`;
4. confirm no target-item automation comment was created;
5. temporarily use a short archive threshold on the disposable item and verify archival preserves fields;
6. restore the 14-day filter;
7. reopen the disposable issue and verify no automatic reverse lifecycle transition occurs.

## Existing backlog cleanup

Before relying on automation, perform one manual reconciliation of existing active-view drift:

- closed issues and merged canonical PRs that are complete -> `Done`;
- closed unmerged PRs -> inspect canonical links before changing;
- open items already in `Done` -> return to the event loop for deliberate reconciliation;
- technical status/control issues -> remove from active delivery views or ensure the normalizer excludes them;
- `Ready` items without Executor and agent routes with human-only assignment -> correct deliberately rather than marking Done.

After cleanup, generate a fresh `ai-delivery-status` snapshot and confirm active-item counts reflect only open work plus intentional
drift.

## Monitoring

Review the snapshot anomaly counts weekly until the built-in workflows have demonstrated stable behavior. A future daily custom
reconciler may automate only the deterministic terminal cases documented in the policy; ambiguous cases remain report-only.

If a built-in workflow performs an unexpected mass transition, disable it immediately, inspect the Project history and source
events, and restore only affected items. Do not compensate with a second broad workflow.