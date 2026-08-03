# Local Codex executors

Local Codex has two explicit adapter modes:

- **app-native same-thread**: the persistent Codex Automation performs selection and one lifecycle turn in the same conversation;
- **headless CLI**: a repository-owned process launches isolated `codex exec` workers.

Both follow root/nested `AGENTS.md`, the compact runtime contract, shared lifecycle/control protocol, exact PR continuity, truthful
proof, and the no-merge boundary. They must not impersonate one another in Worker reference evidence.

## Model profiles

- app-native same-thread automation: `gpt-5.6-terra` / medium;
- headless CLI dispatcher: deterministic shell/no model;
- headless lifecycle worker: `gpt-5.6-terra` / medium;
- explicit difficult-task escalation: `gpt-5.6-sol` / high;
- xhigh only for an exceptional recorded need.

The app-native adapter cannot cheaply use Luna for empty ticks while switching to Terra for the claimed lifecycle turn: the
automation occurrence has one effective model/profile. Correctness therefore requires Terra/medium for the complete app-native
automation. The separate CLI adapter is the path for restoring a model-free dispatcher.

## Route to Local Codex

Prefer Local Codex for complex architecture/concurrency/lifecycle/failure composition, cross-version Java, persistent dependencies
or services, Docker/browser/local environments, exact same-repository existing-PR continuation, representative Verification, and
non-convergence caused by environment/context limitations.

Do not send unresolved product/architecture/canonicalization/credential/privilege decisions to a stronger model. Never adopt fork
or Dependabot branches for direct autonomous correction.

## App-native same-thread topology

```text
persistent Terra/medium automation conversation
  -> one normalized Project snapshot
  -> empty: NO_CHANGE + telemetry
  -> same-thread owned generation: resume it
  -> otherwise one selected Ready candidate
      -> targeted live read + guarded claim
      -> Implementation or Verification in this same conversation
      -> exact branch/PR evidence and guarded handoff
```

The app-native adapter does not create a standalone child thread, child task, hidden subagent, or child worktree. An opaque
`client-new-thread:*` acknowledgement is never worker evidence. The claim identifies
`adapter=codex-app-same-thread-v1; conversation=self`, and a later occurrence of this same persistent automation resumes that exact
generation before selecting new Ready work.

Canonical prompts are `.codex/local/scheduled-task-prompt.md` and `.codex/local/worker-task-prompt.md`. The automation loads the
worker template after claim and applies it in the same conversation. Repository merges do not rewrite an embedded automation;
replace its prompt and model manually and smoke-test it.

## Headless topology

```text
systemd timer or cron
  -> flock prevents host overlap
  -> one normalized queue snapshot and guarded claim
  -> isolated worktree
  -> codex exec with Terra/medium lifecycle prompt
  -> exact branch/PR publication and guarded handoff
```

`.codex/local/run-issue.sh` remains the legacy one-shot headless entry point. The durable CLI dispatcher/supervisor is delivered
separately under issue #814.

## Shared selection and branch authority

Each adapter:

- selects canonical issue/PR work routed `Execution = Ready`, `Executor = Local Codex`, with empty/local assignee;
- uses one retained normalized snapshot and deterministic ordering;
- live-reads only the selected target before claim;
- suppresses duplicate representations and multi-issue linked work;
- claims before lifecycle mutation;
- creates no source/Project mutation for an empty queue;
- allows at most one active Local Codex generation during the initial rollout.

Branch authority is ordered:

1. exact branch of a verified same-repository continuation PR;
2. explicit branch in the authoritative issue, Project route, or maintainer decision;
3. only if neither exists, deterministic fresh `agent/issue-<number>-<short-slug>` derivation after the targeted read.

An explicit branch always wins. A conflict returns to Planning or a precise maintainer decision; it never authorizes an invented
replacement branch.

Every autonomous command uses the control-plane human-readable Markdown wrapper, exact start/end markers, and lowercase `json`
fence; the marked JSON is authoritative. Never emit bare JSON. Inspect the terminal reaction and re-read Project state before
source mutation, recovery, or handoff.

## Existing PR continuation

An existing same-repository PR may be an adopted continuation only when the canonical Project route deliberately selects Local
Codex. Reuse its exact branch and PR even if Dmitry, ChatGPT, an IDE agent, or another worker created it. Do not create a fresh
branch or duplicate PR. Never reset, rebase, force-push, discard useful commits, or silently narrow scope. Never adopt a fork or
Dependabot branch.

## Lifecycle contract

An app-native automation or headless worker performs one routed status. Implementation maps acceptance criteria to proof, changes
code, tests, inspects the complete diff, commits/pushes, and publishes one intended PR. Verification proves observable behavior in
a representative environment. Review requires an independent configured identity.

For fresh work create one branch/PR. For continuation update the exact existing branch/PR. When Implementation is complete, mark
the PR ready for review and re-read it as open, targeting develop, non-draft, and at the exact published head. Then publish evidence
and use one guarded handoff to `Review / Ready / ChatGPT`; a canonical issue includes
`reviewPullRequest.number/head`, while a canonical PR guards `expected.head`. Verify reaction, Project state, bedrin-gpt
assignment, and exact-head PR state.

The lifecycle owner may renew the same claim/generation before lease expiry. It must perform its own completion signalling and must
not leave finished work in In progress.

## Recovery

App-native same-thread recovery is conversation-scoped: only the same persistent automation may resume a reference naming
`codex-app-same-thread-v1` and `conversation=self`. It never lists or invents child tasks.

Headless recovery is generation/process/worktree scoped and is specified by issue #814. Neither adapter may adopt ownership from the
other merely because the Executor field says Local Codex.

## Telemetry and security

Every automation/worker records start/end/duration, adapter, model/reasoning, snapshot/candidate, outcome, and exact provider token
counters when exposed. Otherwise counters are null with `usageSource: unavailable`; never fabricate exact usage.

Use a dedicated low-value VM/account with repository-scoped credentials. Docker/full filesystem/network access is privileged host
policy. Never merge, enable auto-merge, bypass protection, rewrite shared history, or perform privileged operations without
Dmitry's explicit instruction.
