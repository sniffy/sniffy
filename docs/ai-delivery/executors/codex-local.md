# Local Codex executors

Local Codex supports an app-native dispatcher/worker topology and a headless CLI topology. Both follow root/nested `AGENTS.md`, the
compact runtime contract, shared lifecycle/control protocol, exact PR continuity, truthful proof, and the no-merge boundary.

## Model profiles

- persistent dispatcher: `gpt-5.6-luna` / low;
- normal implementation or verification worker: `gpt-5.6-terra` / medium;
- explicit difficult-task escalation: `gpt-5.6-sol` / high;
- xhigh only for an exceptional recorded need.

Model choice does not change authority, claim ownership, branch continuity, review independence, or proof obligations.

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

## Headless topology

```text
systemd timer or cron
  -> flock prevents host overlap
  -> one normalized queue snapshot and guarded claim
  -> isolated worktree
  -> codex exec with Terra/medium lifecycle prompt
  -> exact branch/PR publication and guarded handoff
```

`.codex/local/run-issue.sh` defaults to Terra/medium. Override to Sol/high only for an exceptional task and record why normal worker
capability was insufficient.

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

Every autonomous command uses the control-plane human-readable Markdown wrapper, exact start/end markers, and lowercase `json` fence; the marked JSON is authoritative. Never emit bare JSON. Inspect the terminal reaction and re-read Project state before
worker creation, recovery, or handoff.

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

The worker owns completion signalling. It may renew the same claim/generation before lease expiry if still legitimately running; it
must not merely wait for the dispatcher to discover completion.

## Lease recovery

Normal ticks do not poll active workers. If a lease is missing/invalid/expired, target that exact generation. Extend only after
proving the same worker remains active; complete a lost handoff when publication is sufficient; recover the same worktree/branch
where possible; release only when no active/recoverable work remains. Never duplicate the task, generation, branch, or PR.

## Telemetry and security

Every dispatcher/worker records start/end/duration, model/reasoning, snapshot/candidate, outcome, and exact provider token counters
when exposed. Otherwise counters are null with `usageSource: unavailable`; never fabricate exact usage.

Use a dedicated low-value VM/account with repository-scoped credentials. Docker/full filesystem/network access is privileged host
policy. Never merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged operations without
Dmitry's explicit instruction.
