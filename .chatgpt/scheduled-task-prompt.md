# Sniffy ChatGPT Scheduled Task prompt

Create a fileless ChatGPT Project named `AI Delivery Event Loop`. Create each recurring task from its own empty defining chat so
the four task transcripts remain separate:

```text
Event Loop 00 -> hourly at :00
Event Loop 15 -> hourly at :15
Event Loop 30 -> hourly at :30
Event Loop 45 -> hourly at :45
```

Use the same prompt below for all four tasks. Only the task name and schedule differ.

```text
Run one logically stateless Sniffy AI Delivery Event Loop tick now.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Executor: ChatGPT
GitHub assignee: bedrin-gpt

This recurring task may append to its existing defining chat. Ignore prior-run conclusions, remembered queue state, and mutable
conversation context. Re-read current GitHub and repository state from scratch. The transcript is telemetry, not durable state.

Before any claim or mutation, read the current versions of:
- https://github.com/sniffy/sniffy/blob/develop/AGENTS.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/profile.yml
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/lifecycle.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/control-plane.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/event-loop.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/pull-request-intake.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/routing.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/supervision.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/verification.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/executors/chatgpt.md

Do not create another Scheduled Task, child ChatGPT task, or promise future/background work. Use this task/chat identifier plus
the current UTC timestamp as the dispatcher reference.

ProjectV2 control protocol:
- Every executor uses the same rotating technical control issue and delivery-control/v1 JSON commands documented in
  control-plane.md.
- Locate the newest open issue with the configured ai-delivery-control label; create it with that label if none exists. Do not
  post /project-status, /project-field, claim-intent, winner, loser, withdrawal, lease, or polling comments on the target issue or
  pull request.
- A claim or handoff is valid only after the command has the documented terminal reaction and the resulting Project fields have
  been re-read.
- Use one guarded command for the complete multi-field claim or lifecycle transition. Include current Status, Execution,
  Executor, and exact PR head when applicable in expected state.
- An expected-state conflict means another dispatcher won or state changed. Make no work mutation and continue only if current
  state still gives this tick valid ownership.
- workflow_dispatch is an administrative/debug fallback to the same implementation, not the autonomous ChatGPT path.

Perform this protocol:
1. Reconcile external pull-request intake before ordinary queue work. Find eligible open sniffy/sniffy dependency PRs missing
   from Project 2, verify actual author, labels, target, draft state, and exact head, and materialize them idempotently through one
   guarded command with addIfMissing=true. Intake is not approval.
2. Reconcile stale exact-head lifecycle state before continuing workers or claiming ordinary queue work. Inspect every open
   Project PR whose current Review, Verification, Approval, or derived Blocked/Human handoff relies on completed exact-head
   evidence. If its current head differs from the head supporting that state, use one guarded command expecting the current
   Status, Execution, Executor, and current head to set Review / Ready / ChatGPT and clear stale Worker reference. Verify the
   terminal reaction and resulting Project state. This supervisory reconciliation may select Approval or Blocked items that are
   not otherwise in the ordinary ChatGPT queue and consumes at most one item in the tick.
3. Before claiming new work, inspect ChatGPT-owned Execution=In progress items whose worker, CI, external dispatch, or monitoring
   observation is due. Worker observation belongs to this same event loop: first after 15 minutes, again 15 minutes later, then
   hourly while incomplete. Continue or recover existing ownership before starting unrelated work.
4. If a needed control command would exceed the rotation threshold, rotate the active control issue using control-plane.md, then
   continue this tick. Do not create a separate cleanup scheduler.
5. Otherwise select at most one Project 2 item where:
   - Execution = Ready;
   - Executor = ChatGPT;
   - Assignee is empty or bedrin-gpt;
   - Status is Planning, Implementation, Review, or Verification;
   - current ChatGPT tools and identity can truthfully complete the lifecycle turn or reach a safe durable handoff now.
6. Select deterministically using security priority, Project priority, ready timestamp, then repository and item number. Never
   create a duplicate Project item, worker, branch, or pull request.
7. Claim with one guarded delivery-control/v1 command. Set Execution=In progress plus the concrete tick/worker reference and
   lease, inspect the terminal reaction, then re-read and verify ownership before work. If execution or external dispatch cannot
   start, release to Ready. Set Blocked only when Dmitry must decide or act.
8. Perform exactly one lifecycle turn:
   - Planning: resolve outcome, decisions, non-goals, risk axes, Implementer, Verifier, and proof obligations.
   - Implementation: first ask whether ChatGPT can honestly edit, test, inspect, publish, and verify the focused change. If not,
     route bounded fresh work to Codex Cloud or complex/persistent/existing-PR work to Local Codex according to profile.yml.
   - Review: inspect the complete exact-head diff, issue decisions, tests, prior review threads, and matching-head CI. Submit
     APPROVE only with independent identity. Otherwise submit one comprehensive REQUEST_CHANGES or record the identity limit.
     When a corrected head returns to Review and still has substantive blockers, perform the routing.md convergence checkpoint
     before another implementation dispatch; do not mechanically issue another patch list.
   - Verification: validate the observable result against the authoritative issue using the exact published head/artifact in a
     representative environment. Do not rename unit tests or green CI as system verification.
9. Publish human-useful evidence on the target issue/PR. Then use one guarded control command for the complete next Status,
   Execution, Executor, and cleared worker ownership state. Update GitHub Assignee separately when supported and verify it.
10. Routine waiting for a concrete CI run or worker remains In progress only with a durable reference and next observation point.
    Blocked always routes an exact action to Human/bedrin.
11. Never merge, enable auto-merge, bypass protection, rewrite shared history, expose credentials, or perform privileged
    repository/hosting operations without Dmitry's explicit instruction.
12. If no eligible intake, stale-head reconciliation, due continuation, control-log rotation, claimable turn, or meaningful
    reconciliation exists, make no GitHub/source mutation and reply only NO_CHANGE. Otherwise finish with a compact summary
    containing selected item, lifecycle turn, durable evidence, and resulting Status / Execution / Executor / Assignee.
```

After setup, smoke-test one empty tick and one disposable/read-only eligible item. Replacing a long defining task chat is manual
maintenance described in `docs/ai-delivery/chat-retention.md`; never delete an active task chat before its replacement works.
