# Local Codex executors

Local Codex supports an app-native dispatcher/worker topology and a headless CLI topology. Both follow root/nested `AGENTS.md`, the
compact runtime contract, the shared lifecycle/control protocol, exact PR continuity, truthful proof, and the no-merge boundary.

## Model profiles

- persistent dispatcher/heartbeat: `gpt-5.6-luna` / low;
- normal implementation or verification worker: `gpt-5.6-terra` / medium;
- explicit difficult-task escalation: `gpt-5.6-sol` / high;
- xhigh only for an exceptional recorded need.

The dispatcher is intentionally cheap because it performs deterministic queue selection, not code reasoning. Changing model does
not change authority, claim ownership, branch continuity, review independence, or proof obligations.

## Route to Local Codex

Prefer Local Codex for complex architecture/concurrency/lifecycle/failure composition, cross-version Java, persistent dependencies
or services, Docker/browser/local environments, exact same-repository existing-PR continuation, representative Verification, and
non-convergence caused by environment/context limitations.

Do not send unresolved product, architecture, canonicalization, credential, or privilege decisions to a stronger model. Do not
adopt fork or Dependabot branches for direct autonomous correction.

## App-native topology

```text
persistent Luna/low dispatcher
  -> one normalized Project snapshot
  -> empty: NO_CHANGE + telemetry
  -> selected canonical candidate + guarded claim
      -> one standalone Terra/medium worker and isolated worktree
      -> optional explicit Sol/high escalation
      -> exact branch/PR evidence and guarded handoff
```

The dispatcher reads its compact prompt, `runtime-contract.md`, profile, worker template, and one queue snapshot. It does not read
complete discussions/diffs/reviews/CI or detailed policy before selection. Normal capacity comes from `ownedInProgress` plus
`maxConcurrentWorkers`; provider-wide task inventory is only targeted recovery for one ambiguous already-claimed generation.

Canonical prompts are `.codex/local/scheduled-task-prompt.md` and `.codex/local/worker-task-prompt.md`. Repository merges do not
rewrite embedded Codex automations; replace and smoke-test them manually.

## Headless topology

```text
systemd timer or cron
  -> flock prevents host overlap
  -> one normalized queue snapshot and guarded claim
  -> isolated worktree
  -> codex exec with rendered Terra/medium lifecycle prompt
  -> exact branch/PR publication and guarded handoff
```

`.codex/local/run-issue.sh` defaults to Terra/medium. Override explicitly for an exceptional task, for example
`CODEX_MODEL=gpt-5.6-sol CODEX_REASONING_EFFORT=high`; record why normal worker capability was insufficient.

## Dispatcher contract

The Local Codex dispatcher:

- selects issue or PR canonical items routed `Execution = Ready`, `Executor = Local Codex`, and empty/local assignee;
- checks due owned work before Ready work;
- uses the retained normalized snapshot once and deterministic ordering;
- derives capacity from current owned items;
- live-reads only the selected target's type/open state, formal links, exact PR branch/head/draft/ownership, route, worker reference,
  and active control issue;
- suppresses duplicate representations and multi-issue linked work;
- claims through one guarded command before spawning;
- creates exactly one worker/generation;
- releases a definitely failed spawn, preserves an ambiguous generation for targeted recovery, and blocks only for Dmitry;
- creates no source/Project/worker mutation for an empty queue.

Every new command uses the control-plane human-readable Markdown wrapper, exact markers, and lowercase `json` fence; the marked JSON
is authoritative. Never emit bare JSON. Inspect reaction and re-read current Project state before worker creation or handoff.

## Existing PR continuation

An adopted continuation is valid only when an open same-repository PR exists and the canonical Project route deliberately selects
Local Codex. Worker evidence names the exact PR/branch/head; push permission and competing ownership are checked. Reuse the exact
branch and PR even if Dmitry, ChatGPT, an IDE agent, or another configured worker created it.

Never reset, rebase, force-push, discard useful commits, open a replacement PR, or silently narrow scope. Fork and Dependabot PRs
receive contributor/bot operations or a deliberate internal replacement task.

## Worker contract

A worker performs one routed lifecycle status:

- Implementation converts acceptance criteria to proof, changes the repository, tests, inspects the complete diff, commits/pushes,
  and publishes one intended PR;
- Verification proves observable behavior in a representative environment;
- Review requires an independent configured identity and explicit route;
- Planning normally remains ChatGPT/human-owned.

For fresh work create one branch/PR. For continuation update the exact existing branch/PR. When Implementation is complete, mark the
PR ready for review and re-read it as open, targeting develop, non-draft, and at the exact published head. Then publish evidence and
use one guarded handoff to `Review / Ready / ChatGPT`; a canonical issue includes `reviewPullRequest.number/head`, while a canonical
PR guards `expected.head`. Verify reaction, Project state, bedrin-gpt assignment, and exact-head PR state.

Routine waiting remains In progress. Observe after 15 minutes, again 15 minutes later, then hourly. Store the next observation in
Worker reference. Leave no finished/missing worker In progress.

## Telemetry and security

Every dispatcher/worker records start/end/duration, model/reasoning, snapshot/candidate, outcome, and exact provider token counters
when exposed. Otherwise counters are null with `usageSource: unavailable`; never fabricate exact usage.

Use a dedicated low-value VM/account with repository-scoped credentials. Docker/full filesystem/network access is privileged host
policy. Never merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged operations without
Dmitry's explicit instruction.
