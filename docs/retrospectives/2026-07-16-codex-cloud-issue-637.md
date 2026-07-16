# Codex Cloud delivery retrospective: issue #637

**Date:** 2026-07-16  
**Scope:** [issue #637](https://github.com/sniffy/sniffy/issues/637), [PR #643](https://github.com/sniffy/sniffy/pull/643)

## Outcome

The Vert.x TLS test was stabilized and published in PR #643. The final implementation preserved the original asynchronous failure, bounded request and shutdown waits, and made WebClient/Vert.x cleanup lifecycle-safe. The complete 24-job matrix passed, including the original failing Windows Server 2025 + JDK 25 entry, and the PR was approved.

The code outcome was good, but delivery took repeated attempts because task dispatch, Cloud-local completion, GitHub publication, review state, and final approval were treated as if they were the same state.

## What went well

- Issue #637 contained a completed diagnosis, explicit non-goals, and testable acceptance criteria. This kept the fix focused and prevented retries, sleeps, weakened assertions, or unrelated TLS changes.
- Repository review guidance exposed a real cleanup edge case before approval: WebClient construction initially occurred outside the scope that guaranteed Vert.x cleanup.
- The final review checked the complete diff, issue, AGENTS.md, review state, all 24 jobs, and the actual Windows/JDK 25 test log rather than relying on the agent summary.
- The GitHub environment now has a dedicated token, persistent gh authentication, explicit agent-network write access, and a reversible publication probe.

## What went wrong

1. **Dispatch was declared before it was observable.** Creating an issue or intending to delegate work was treated as if Codex had started. A task is dispatched only after a real top-level `@codex` trigger has a connector reaction or task link.
2. **Local completion was confused with publication.** Several tasks reported local branches, commits, or `make_pr` metadata as if GitHub had received them. The branches, commits, and pull requests did not resolve remotely.
3. **Publication prerequisites were discovered serially.** The image lacked gh; secrets disappeared after setup; the checkout had no origin; the fine-grained PAT required organization approval; agent internet initially blocked write methods; and a dry-run push produced false confidence. Each gap caused another full task attempt.
4. **Completed work was repeatedly recreated.** When publication failed, follow-up prompts sometimes rebuilt the implementation instead of first recovering the latest workspace/commit or stopping after a failed write probe.
5. **The review follow-up used the wrong trigger surface.** An `@codex` mention inside REQUEST_CHANGES did not start a task. A top-level PR comment also did not trigger while repository code review integration was unavailable. The top-level issue comment was the proven fallback.
6. **Cloud UI and GitHub state diverged.** A PR created directly with gh can be valid on GitHub while the Codex Cloud UI still says no PR was created for the task. The UI association is useful metadata, not proof of publication.
7. **The handoff stopped at draft.** After implementation and locally applicable checks were complete, the PR remained draft. Normal autonomous delivery should use draft during work and mark ready for review at handoff unless the task explicitly requests a draft result.
8. **Monitoring and status language were too optimistic.** Status updates sometimes said an agent was working, a PR existed, or delivery was complete before remote evidence supported those claims.

## Root causes

- No explicit delivery state machine separated dispatched, working, locally complete, published, ready for review, approved, and merged.
- Completion criteria emphasized implementation and tests but did not require remote branch/commit/PR verification and the final draft-to-ready transition.
- Environment validation checked role metadata and dry-run behavior before an actual reversible write round trip was available.
- Trigger behavior differed between issue comments, formal reviews, and PR comments, but the workflow did not name a reliable fallback.

## Corrective actions

### Delivery lead

- Record the trigger comment and wait for a connector reaction or task link before reporting that work started.
- Track each delivery state independently. Never infer publication from a local commit or infer readiness/approval from publication.
- Require resolvable branch, full SHA, PR URL, base/head refs, draft state, and matching head SHA before reporting publication.
- Review the remote diff and CI, not the Cloud summary. Inspect relevant job logs, especially the matrix entry that motivated the issue.
- Use the authoritative issue thread for correction dispatch when PR triggers are unavailable.
- Keep monitoring until the requested terminal state, then disable monitoring.

### Codex Cloud task

- Start from current develop and read AGENTS.md, docs/codex-workflow.md, and the complete issue thread.
- If publication access is uncertain, perform one reversible real remote-ref round trip before implementation. Stop on failure instead of rebuilding the change.
- Recover an existing workspace/commit when possible; do not recreate completed work merely because publication failed.
- Use an explicit repository URL when origin is absent.
- Push and verify real GitHub state. `make_pr` metadata and local PR descriptions are not publication.
- Create/update the PR as draft while work is incomplete; mark it ready for review after implementation and locally applicable verification finish unless told to keep it draft.
- Report exact command results and exact resolvable URLs. Link the task, issue, branch, commit, and PR.

### Environment

- Keep gh installation, persistent authentication, token scope/organization approval, write-enabled agent network policy, and the reversible publication validation documented and tested.
- Reset the Cloud cache after setup-script, secret, or environment-policy changes.

## State model

1. **Ready for agent:** authoritative issue and acceptance criteria are complete.
2. **Dispatched:** a top-level trigger exists and Codex has reacted or posted a task link.
3. **Working:** the task is running; no publication claim yet.
4. **Published:** remote branch, full commit, and PR resolve and the PR head matches.
5. **Ready for review:** implementation and locally applicable checks are complete and the PR is no longer draft unless a draft handoff was explicitly requested.
6. **Approved:** remote diff, comments, review threads, and required CI have been reviewed successfully.
7. **Done:** merged or deliberately closed, with issue/project state updated.

## Follow-up

The mandatory rules are encoded in AGENTS.md and the operational dispatch/review checklist is maintained in docs/codex-workflow.md. Future retrospectives should update those files when a new repeated failure mode appears.
