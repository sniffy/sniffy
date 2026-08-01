# Project 2 housekeeping setup runbook

This runbook translates [`project-housekeeping.md`](project-housekeeping.md) into maintainable GitHub Project configuration. These
steps are privileged organization operations and are performed by Dmitry in the Project UI.

## Built-in completion workflows

Open organization Project 2, choose **Workflows**, and inspect the default workflows. Enable or create rules with these outcomes:

| Source event | Intended scope | Project action |
| --- | --- | --- |
| Issue closed | issue items | Set `Status` to `Done` |
| Pull request merged | pull-request items | Set `Status` to `Done` |

Do not configure a generic pull-request-close-to-Done rule. A closed implementation-evidence PR must not complete an open canonical
issue, and a closed-unmerged standalone PR may require explicit abandonment or supersession evidence.

## Auto-archive workflow

GitHub auto-archive does not filter on custom Project fields. After the terminal workflows and initial backlog reconciliation have
been smoke-tested, enable Project auto-archive with the supported source-state filter:

```text
is:closed updated:<@today-14d
```

Verify the rule against a disposable completed item before enabling it broadly. The 14-day delay gives the status snapshot and event
loop time to detect a missed `Done` transition before archival.

The rule must archive rather than delete. Archived items remain available for audit and restoration, but still count toward the
Project's total item limit. If the Project approaches that platform limit, use a separately reviewed export-and-delete retention
procedure rather than broad automatic deletion.

## Smoke test

Use disposable items that do not close real delivery work:

1. add a disposable issue to Project 2 and set a non-terminal Status;
2. close it and verify the built-in workflow changes Status to `Done`;
3. add a disposable standalone PR item, merge it, and verify `Done`;
4. close an unmerged disposable PR and verify no broad rule incorrectly completes linked open work;
5. confirm no target-item automation comment was created;
6. temporarily use a short supported archive threshold on a completed disposable item and verify archival preserves fields;
7. restore the 14-day filter;
8. reopen the disposable issue and verify no automatic reverse lifecycle transition occurs.

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
