# Sniffy app-native Local Codex dispatcher prompt

Configure the Codex automation and its worker tasks with the current Sniffy Local Codex profile: **Sol** model and
**extra-high** reasoning. The scheduler-level model is fixed; the dispatcher does not dynamically substitute Terra or another
model for individual items.

Copy the text block below into one automation attached to a persistent dispatcher conversation in the Codex app. Repository
merges do not update the text embedded in an existing automation: replace that text manually whenever this canonical prompt
changes, then repeat the smoke test below.

This is one adapter for the generic lifecycle and control protocol in `docs/ai-delivery/`; it is not a second policy.

```text
Run one Sniffy app-native Local Codex dispatcher tick in this existing dispatcher conversation.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Executor: Local Codex
Queue snapshot helper: .codex/local/project-queue-snapshot.sh
Worker template: .codex/local/worker-task-prompt.md
Model/profile: Sol / extra-high reasoning

This conversation coordinates only. Never edit source, create a work branch, run implementation/verification commands, review
the worker's PR, merge, or enable auto-merge.

1. Fetch current origin/develop. Read AGENTS.md,
   docs/ai-delivery/{README,profile,lifecycle,control-plane,event-loop,pull-request-intake,routing,supervision,verification}.md,
   this prompt, and the worker template exactly once from that revision. Use one batched local command. Do not probe file lengths,
   search for a nonexistent profile.md, or re-read whole documents unless one named section is genuinely missing.
2. Run `.codex/local/project-queue-snapshot.sh` exactly once and retain its normalized JSON for the entire selection phase. This
   helper is the only normal full-Project query. Do not run `gh project item-list`, raw Project GraphQL, schema-discovery jq probes,
   or the helper again in the same tick.
3. If the helper exits 75 because GitHub GraphQL is rate-limited, make no claim or Project/source mutation. Do not switch to another
   connector, combine stale snapshots, or infer eligibility from issue/PR state alone. Report `RATE_LIMITED: no claim made` in this
   dispatcher conversation and stop.
4. Inspect `ownedInProgress` from the retained snapshot first. Continue only an item whose worker or monitoring observation is due
   under the 15-minute, second-15-minute, then hourly cadence. Do not create another monitor. A provider-wide Codex project/task
   inventory is allowed only to recover one selected `In progress` item whose provisional Worker reference leaves child creation
   uncertain; scope the result to that claim token and generation.
5. Otherwise use the already sorted `readyCandidates` array and choose at most its first candidate, subject to canonical duplicate
   suppression and available capacity. Compute available capacity from `executors.Local Codex.maxConcurrentWorkers` in the loaded
   profile minus the length of `ownedInProgress` in the retained snapshot. Treat this as the single-dispatcher scheduling limit,
   not a distributed semaphore. Do not call `List projects`, list tasks/conversations, or use any other provider-wide inventory on
   the normal `Ready` claim path. Eligible work has Execution=Ready, Executor=Local Codex, Status=Implementation or Verification,
   and an empty or local-worker Assignee.
6. If neither a due owned item nor a ready candidate exists, create no task, worktree, branch, target comment, control command, or
   Project mutation. Reply only NO_CHANGE.
7. Only after one candidate is selected, perform the minimum targeted live reads needed to construct the guarded claim and render
   its worker: canonical item type/open state, formal closing links, exact existing PR branch/head and repository ownership when
   applicable, plus the newest active control issue. Do not read the complete discussion, proof matrix, review submissions,
   review threads, or CI before claim; the lifecycle worker owns those reads. Never refetch the full Project snapshot or query
   provider-wide Codex inventory for duplicate-worker evidence. Per-target guarded claim serialization is the duplicate-generation
   authority.
8. An existing same-repository PR may be an adopted continuation even when Dmitry, ChatGPT, an IDE agent, or another configured
   worker created it. Adoption is valid only when the retained snapshot and targeted reads show the canonical Project route sets
   Implementer/Executor=Local Codex. Reuse the exact branch and PR. Do not adopt fork or Dependabot branches for direct correction.
9. Select deterministically by Project priority, ready timestamp, repository, item type, then item number. The helper has already
   normalized and sorted candidates; do not create an alternative ordering from raw Project data.
10. Claim through the one active technical control issue using one guarded delivery-control/v1 command. Guard current Status,
    Execution=Ready, Executor=Local Codex, and exact PR head when the canonical item is a PR; set Execution=In progress plus
    claim token/lease inside the provisional Worker reference. The guarded transition is the authoritative current-state recheck.
    Inspect the reaction and re-read the resulting target Project state before spawning.
11. Render every placeholder in .codex/local/worker-task-prompt.md, including canonical work-item type/URL, authoritative linked
    issue, exact existing PR branch/head, repository ownership, author, and fresh/continuation/adopted-continuation mode.
12. Create exactly one NEW one-time standalone app-owned task named "Sniffy <work-item-type> #<number> <status>: <title>" using
    the current local project, a new isolated worktree, Sol, extra-high reasoning, and the rendered prompt. Never create it in this
    dispatcher conversation.
13. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh for this
    app-native adapter.
14. After confirming the child task exists, publish one guarded control command updating the final worker reference, then verify
    it. If child creation definitively fails, release to Ready through the same protocol. If the creation result is ambiguous, keep
    the provisional `In progress` ownership for targeted recovery under step 4; never release and redispatch speculatively. Set
    Blocked only when Dmitry must decide or act.

Never dispatch the same lifecycle generation twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling or replacing the recurring dispatcher, prove with disposable/read-only items that:

- one tick invokes `project-queue-snapshot.sh` once and never invokes `gh project item-list` directly;
- a normal `Ready` claim derives capacity from `ownedInProgress` plus `maxConcurrentWorkers` and performs no provider-wide Codex
  project/task inventory;
- recovery may inspect provider inventory only for one provisional `In progress` claim token/generation after ambiguous child
  creation;
- an empty snapshot stays in this conversation and creates no worktree;
- a rate-limited snapshot creates no claim and does not switch data sources;
- one eligible issue item creates exactly one standalone worker conversation and isolated WSL worktree;
- one explicitly routed same-repository PR continuation reuses its exact branch and PR without creating a duplicate;
- fork and Dependabot PRs are rejected for adopted direct correction;
- the worker uses Sol / extra-high and receives intended canonical item, Status, routing, and exact-head fields;
- concurrent guarded claims produce one success and one conflict without target-item claim comments;
- failed child creation releases the claim;
- a completed worker hands off the canonical item to the next lifecycle status rather than creating its own reviewer conversation.

Repeat after material Codex automation changes. If nested one-time task creation is unavailable, pause this adapter and use a
manual app worker or the headless Linux adapter; do not substitute external UI automation.
