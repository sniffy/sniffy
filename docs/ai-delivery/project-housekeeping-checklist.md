# Project housekeeping rollout checklist

- [ ] Enable issue-closed -> `Done` in organization Project 2.
- [ ] Enable pull-request-merged -> `Done` in organization Project 2.
- [ ] Verify closed-unmerged pull requests are not broadly completed without canonicalization.
- [ ] Configure auto-archive for `Status:Done updated:<@today-14d`.
- [ ] Run disposable issue and pull-request smoke tests.
- [ ] Manually reconcile the existing stale Project backlog once.
- [ ] Generate a fresh `ai-delivery-status` snapshot and record active/anomaly counts.
- [ ] Review anomaly counts after two weeks.
- [ ] Open a focused implementation issue only if deterministic drift still recurs.
