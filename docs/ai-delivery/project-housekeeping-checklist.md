# Project housekeeping rollout checklist

- [ ] Leave the combined issues-or-PRs-closed -> `Done` workflow disabled.
- [ ] Enable pull-request-merged -> `Done` in organization Project 2.
- [ ] Reconcile closed canonical issues through guarded `delivery-control/v1` commands.
- [ ] Run disposable closed-issue, merged-PR, and closed-unmerged-PR smoke tests.
- [ ] Reconcile the existing stale Project backlog once through the common control issue.
- [ ] Configure supported auto-archive filter `is:closed updated:<@today-14d`.
- [ ] Verify the filter also archives stale closed-unmerged PRs without treating archival as completion.
- [ ] Verify archival preserves fields and that archived items still count toward the Project item limit.
- [ ] Generate a fresh `ai-delivery-status` snapshot and record active/anomaly counts.
- [ ] Review anomaly counts after two weeks.
- [ ] Open a focused implementation issue only if deterministic drift still recurs.
