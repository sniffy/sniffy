# Local Codex executors

Local Codex supports an app-native dispatcher/worker topology and a headless CLI topology. Both follow root/nested `AGENTS.md`, the
compact runtime contract, shared lifecycle/control protocol, exact PR continuity, truthful proof, and the no-merge boundary.

## Model profiles

- app-native dispatcher: `gpt-5.6-luna` / low;
- CLI dispatcher: deterministic shell, no model;
- CLI worker: one recorded per-generation profile: `economy` (`gpt-5.6-luna` / low), `balanced`
  (`gpt-5.6-terra` / medium), or `frontier` (`gpt-5.6-sol` / high);
- app-native worker and default CLI profile: `gpt-5.6-terra` / medium;
- xhigh only for an exceptional recorded need.

The CLI profile is resolved before claim, stored in the generation manifest, and passed explicitly to `codex exec`; a systemd-wide
model setting is not authoritative. Model choice does not change authority, claim ownership, branch continuity, review
independence, or proof obligations.

## Route to Local Codex

Prefer Local Codex for complex architecture/concurrency/lifecycle/failure composition, cross-version Java, persistent dependencies
or services, Docker/browser/local environments, exact same-repository existing-PR continuation, representative Verification, and
non-convergence caused by environment/context limitations.

Do not send unresolved product/architecture/canonicalization/credential/privilege decisions to a stronger model. Never adopt fork
or Dependabot branches for direct autonomous correction.

## App-native topology

```text
persistent Luna/low dispatcher
  -> one normalized Project snapshot
  -> empty: NO_CHANGE + telemetry
  -> selected Ready or stale-lease candidate
      -> targeted live read + guarded claim/recovery
      -> one standalone Terra/medium worker and isolated worktree
      -> optional explicit Sol/high escalation
      -> exact branch/PR evidence and guarded handoff
```

The dispatcher reads its compact prompt, `runtime-contract.md`, profile, worker template, and one queue snapshot. It does not read
complete discussions/diffs/reviews/CI before selection. Provider-wide task inventory is only targeted recovery for one selected
missing/invalid/expired lease.

Canonical prompts are `.codex/local/scheduled-task-prompt.md` and `.codex/local/worker-task-prompt.md`. Repository merges do not
rewrite embedded automations; replace and smoke-test them manually.

## Repository-owned CLI topology

```text
systemd timer
  -> deterministic shell dispatcher, one normalized Project snapshot
  -> empty: no Codex invocation
  -> guarded state=spawning claim with short lease
  -> one generation manifest + isolated worktree
  -> codex exec --json with the generation's selected profile
  -> thread.started + turn.started strong acknowledgement
  -> guarded state=running reference
  -> worker-owned exact branch/PR publication and lifecycle handoff
```

The complete install, manifest, acknowledgement, recovery, and security contract is
[`codex-cli.md`](codex-cli.md). `.codex/local/run-issue.sh` remains a legacy manually invoked one-shot helper; the systemd adapter is
the recurring production path.

The CLI dispatcher checks only the exact locally owned generation on later ticks. It never invokes a model for an empty queue and
never performs provider-wide task discovery. An inactive acknowledged process is recovered immediately even when its nominal
Project lease is still in the future, so process death cannot create a four-hour phantom worker.

## Dispatcher contract

The dispatcher:

- selects canonical issue/PR work routed `Execution = Ready`, `Executor = Local Codex`, with empty/local assignee;
- counts every owned `In progress` item against capacity;
- ignores owned work with a valid future lease without opening its worker;
- selects stale recovery only for missing/invalid worker evidence or expired `leaseUntil`;
- uses the retained normalized snapshot once and deterministic ordering;
- live-reads only the selected target;
- suppresses duplicate representations and multi-issue linked work;
- claims Ready work before spawning exactly one worker/generation;
- preserves an ambiguous spawn as one provisional generation rather than redispatching;
- creates no source/Project/worker mutation for an empty queue.

Branch authority is ordered:

1. exact branch of a verified same-repository continuation PR;
2. explicit branch in the authoritative issue, Project route, or maintainer decision;
3. only if neither exists, deterministic fresh `agent/issue-<number>` derivation after the targeted read.

An explicit branch always wins. A conflict returns to Planning or a precise maintainer decision; it never authorizes an invented
replacement branch. The CLI adapter also checks the authoritative branch for an existing open PR or remote ref and resumes it
instead of creating a duplicate.

Every autonomous command uses the control-plane human-readable Markdown wrapper, exact start/end markers, and lowercase `json` fence;
the marked JSON is authoritative. Never emit bare JSON. Inspect the terminal reaction and re-read Project state before source
mutation, recovery, or handoff.

## Existing PR continuation

An existing same-repository PR may be an adopted continuation only when the canonical Project route deliberately selects Local
Codex. Reuse the exact same-repository open PR branch and PR even if Dmitry, ChatGPT, an IDE agent, or another worker created it.
Do not create a fresh branch or duplicate PR. Never reset, rebase, force-push, discard useful commits, or silently narrow scope.
Never adopt a fork or Dependabot branch.

## Worker contract

A worker performs one routed status. Implementation maps acceptance criteria to proof, changes code, tests, inspects the complete
diff, commits/pushes, and publishes one intended PR. Verification proves observable behavior in a representative environment.
Review requires an independent configured identity.

For fresh work create one branch/PR. For continuation update the exact existing branch/PR. When Implementation is complete, mark
the PR ready for review and re-read it as open, targeting develop, non-draft, and at the exact published head. Then publish evidence
and use one guarded handoff to `Review / Ready / ChatGPT`; a canonical issue includes `reviewPullRequest.number/head`, while a
canonical PR guards `expected.head`. Verify reaction, Project state, bedrin-gpt assignment, and exact-head PR state.

The lifecycle owner may renew the same claim/generation before lease expiry. It must perform its own completion signalling and must
not leave finished work in In progress. A local `turn.completed` event is useful supervisor evidence but is not a substitute for the
verified GitHub handoff.

## Recovery boundaries

App-native recovery remains task/reference scoped and follows the existing dispatcher contract. CLI recovery is independent from
that app behavior and does not require its prompt, model, conversation, worktree, or worker API.

CLI recovery is generation/process/worktree scoped: only `codex-cli-v1` references with an exact local manifest may be inspected.
Active units are preserved; a strong acknowledgement may repair a lost running-reference update; inactive or failed generations
are guardedly released to Ready unless a worker already completed the handoff. Neither adapter may adopt ownership from the other
merely because the Executor field says Local Codex.

## Telemetry and security

Every dispatcher/worker records start/end/duration, model/reasoning, snapshot/candidate, outcome, and exact provider token counters
when exposed. Otherwise counters are null with `usageSource: unavailable`; never fabricate exact usage.

Use a dedicated low-value VM/account with repository-scoped credentials. Docker/full filesystem/network access is privileged host
policy. Never merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged operations without
Dmitry's explicit instruction.
