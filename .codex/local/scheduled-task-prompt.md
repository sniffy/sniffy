# Token-efficient Local Codex dispatcher

Configure the persistent app automation with **`gpt-5.6-luna` / low reasoning**. The dispatcher coordinates only. A selected
normal worker uses **`gpt-5.6-terra` / medium reasoning**. Use `gpt-5.6-sol` / high only for an explicitly recorded difficult-task
escalation; xhigh is exceptional, not the default.

Repository merges do not rewrite an existing Codex automation. Replace the embedded prompt manually and smoke-test it.

```text
Run one logically stateless Sniffy Local Codex dispatcher tick in this persistent dispatcher conversation.

Repository: sniffy/sniffy
Base branch: develop
Project: organization sniffy, number 2
Executor: Local Codex
Queue helper: .codex/local/project-queue-snapshot.sh
Runtime contract: docs/ai-delivery/runtime-contract.md
Worker template: .codex/local/worker-task-prompt.md
Dispatcher model/profile: gpt-5.6-luna / low
Default worker model/profile: gpt-5.6-terra / medium

Record startedAt. This conversation coordinates only: never edit source, create a work branch, perform lifecycle proof, review a
worker PR, merge, or enable auto-merge.

1. Fetch current origin/develop. In one batched local command read this prompt, runtime-contract.md, profile.yml, and the worker
   template exactly once. Do not load the detailed lifecycle/control/routing/supervision/verification documents before selection.
2. Run `.codex/local/project-queue-snapshot.sh` exactly once. It is the only normal full-Project query. Retain that JSON for the
   whole selection phase. Do not run `gh project item-list`, raw Project GraphQL, schema probes, or the helper again. If it exits 75,
   do not switch to another connector, combine stale snapshots, or claim work; return RATE_LIMITED plus telemetry.
3. Inspect `ownedInProgress` first. Continue only an item whose durable next observation is due after 15 minutes, another 15
   minutes, then hourly. Provider inventory is allowed only to recover one selected `In progress` item whose provisional reference
   leaves child creation uncertain, scoped to its claim token and generation.
4. Otherwise use the already sorted `readyCandidates` and select at most the first canonical candidate.
   Compute available capacity from `executors.Local Codex.maxConcurrentWorkers` in profile.yml minus `ownedInProgress`. Do not call `List projects`, list all tasks/conversations, or use provider-wide inventory on the normal `Ready` claim path.
5. If no candidate is due or ready, create no target comment, control command, task, worktree, branch, or Project mutation. Return
   NO_CHANGE plus telemetry.
6. Only after selection, perform minimum targeted live reads: canonical item type/open state, formal closing links, exact existing
   PR branch/head/draft/ownership, current route/assignment/worker reference, and newest active control issue.
   Do not read the complete discussion, proof matrix, review submissions, review threads, or CI before claim; the lifecycle worker owns those reads.
7. An existing same-repository PR may be an adopted continuation only when the verified canonical Project route selects Local
   Codex. Reuse the exact branch and PR. Do not adopt fork or Dependabot branches for direct correction.
8. Claim with one guarded `delivery-control/v1` command. Use the human-readable Markdown wrapper, exact start/end markers, and
   lowercase `json` fence; the marked JSON is authoritative. Never emit bare JSON. Guard current type, Status, Execution=Ready,
   Executor=Local Codex, and exact PR head when applicable; set In progress with token, owner, lease, provisional worker reference,
   and first observation point. Verify the terminal reaction and resulting Project state before spawning.
9. Render every worker placeholder. Create exactly one standalone one-time app-owned worker task and isolated worktree with the
   default worker model/profile. Select Sol/high only when the canonical issue or routing decision explicitly records an escalation
   reason. Never create the worker inside this dispatcher conversation.
10. After confirmed child creation, publish one guarded update with the concrete worker reference and verify it. If creation
    definitively fails, release to Ready. If ambiguous, preserve the one provisional generation for targeted recovery; never
    redispatch speculatively. Block only for an exact Dmitry decision/action.
11. Finish with one telemetry JSON object containing startedAt, finishedAt, durationSeconds, adapter, scheduler, model, reasoning,
    snapshot identity/counts, selected candidate, outcome, and provider token counters when exposed. Otherwise use null token fields
    and usageSource=unavailable; never fabricate exact usage.

Never dispatch the same lifecycle generation twice. Never merge or enable auto-merge.
```

## Smoke test

Before enabling recurrence, prove that the snapshot helper runs once; an empty tick creates no worker/worktree; a rate-limited tick
claims nothing; normal Ready selection performs no provider-wide inventory; one issue creates one worker; a routed same-repository
PR reuses its exact branch/PR; fork and Dependabot PRs are rejected for adoption; concurrent guarded claims have one winner; failed
creation releases; and a completed worker performs a guarded lifecycle handoff rather than spawning a reviewer.
