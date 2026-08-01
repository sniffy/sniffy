# Project housekeeping rollout checklist

- [ ] Enable issue-closed -> `Done` in organization Project 2.
- [ ] Enable pull-request-merged -> `Done` in organization Project 2.
- [ ] Do not enable a broad closed-PR-to-Done rule.
- [ ] Run disposable issue, merged-PR, and closed-unmerged-PR smoke tests.
- [ ] Manually reconcile the existing stale Project backlog once.
- [ ] Configure supported auto-archive filter `is:closed updated:<@today-14d`.
- [ ] Verify archival preserves fields and note that archived items still count toward the Project item limit.
- [ ] Generate a fresh `ai-delivery-status` snapshot and record active/anomaly counts.
- [ ] Review anomaly counts after two weeks.
- [ ] Open a focused implementation issue only if deterministic drift still recurs.
