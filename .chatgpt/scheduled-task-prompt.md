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
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/status-snapshot.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/event-loop.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/pull-request-intake.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/routing.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/supervision.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/verification.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/executors/chatgpt.md
- https://github.com/sniffy/sniffy/blob/develop/.chatgpt/status-snapshot-instructions.md

Do not create another Scheduled Task, child ChatGPT task, or promise future/background work. Use this task/chat identifier plus
the current UTC timestamp as the dispatcher reference.

ProjectV2 read protocol:
- Prefer a complete direct organization ProjectV2 read when the connected tool actually succeeds. If direct ProjectV2 access is
  unavailable, incomplete, or fails, apply .chatgpt/status-snapshot-instructions.md before intake, reconciliation, continuation,
  or ordinary queue selection.
- Discover the newest open non-PR issue labeled ai-delivery-status; never hardcode its issue number. Parse and validate its pure
  JSON pointer, require the configured protocol/repository/Project/artifact identity and freshness, then download the artifact by
  numeric artifact ID through the default GitHub connector and read project-2-status.json.
- Use only explicit snapshot field values, including null for absent values. Never infer Project state from prior chat turns,
  issue prose, PR authorship, or defaults. Reject a missing, expired, inaccessible, malformed, mismatched, or stale snapshot and
  make no snapshot-dependent claim or transition.
- The snapshot is an eventually consistent selection aid, not a mutation ledger or lease. Re-read live issue/PR state, assignment,
  formal closing links, branch/head, reviews, threads, and matching CI through GitHub before acting.
- Every Project mutation still goes through delivery-control/v1, whose serialized workflow re-reads live ProjectV2 and verifies
  expected and final values. A confused reaction means the snapshot lost a race; make no work mutation and do not retry from the
  same snapshot. Before a later dependent Project transition, obtain a snapshot generated after the preceding successful command.
- Never select issues labeled ai-delivery-control or ai-delivery-status as delivery work.

ProjectV2 control protocol:
- Every executor uses the same rotating technical control issue and delivery-control/v1 JSON commands documented in
  control-plane.md.
- Locate the newest open issue with the configured ai-delivery-control label; create it with that label if none exists. Do not
  post /project-status, /project-field, claim-intent, winner, loser, withdrawal, lease, or polling comments on the target issue or
  pull request.
- A claim or handoff is valid only after the command has the documented terminal reaction. A +1 reaction means the control
  workflow applied or observed the requested final state and verified it internally. When direct ProjectV2 reads are unavailable,
  require a status snapshot generated after that command before any later dependent Project transition.
- Use one guarded command for the complete multi-field claim or lifecycle transition. Include current Status, Execution,
  Executor, and exact PR head when applicable in expected state.
- A command setting Status=Review must identify the exact review PR. For a canonical PR this is target + expected.head; for a
  canonical issue include reviewPullRequest.number and reviewPullRequest.head. The control plane verifies open/develop/exact-head,
  marks a draft PR ready when necessary, and re-reads non-draft state before writing Project Review.
- Omitted fields are left unchanged. A missing Implementer is valid and means unknown or not deliberately selected; never invent
  an Unknown, PR-author, bot, or provider value merely to fill the field.
- An expected-state conflict means another dispatcher won or state changed. Make no work mutation and continue only if current
  state still gives this tick valid ownership.
- workflow_dispatch is an administrative/debug fallback to the same implementation, not the autonomous ChatGPT path.

Perform this protocol:
1. Reconcile pull-request intake before ordinary queue work. Scan every open pull request in sniffy/sniffy targeting develop,
   regardless of author, label, bot/provider, branch creator, or whether it originated from a Project issue. Verify author,
   same-repository versus fork, labels, draft state, exact head, formal closing issues, existing Project representation, review
   state, and current CI.
   - Draft PR: do not start Review. Continue monitoring only when an existing canonical Project item already owns that draft PR.
   - Exactly one formal closing issue: that issue is the canonical lifecycle item. Add or locate the issue idempotently and, when
     the PR is non-draft and implementation publication is complete, initialize or hand it off to Review / Ready / ChatGPT with
     the PR URL and exact head in Worker reference. Do not add the PR as a second active lifecycle item.
   - No formal closing issue: the non-draft PR itself is the canonical lifecycle item. Add or locate it idempotently through one
     guarded command with addIfMissing=true and initialize Review / Ready / ChatGPT.
   - Multiple formal closing issues: the non-draft PR is the canonical coordination item and enters Planning / Ready / ChatGPT.
     Record every linked issue and resolve scope, proof, completion propagation, and duplicate eligibility before Review.
   - If an issue and its PR were both accidentally materialized, do not perform duplicate Review. Prefer the canonical item above,
     make the duplicate non-claimable through a guarded reconciliation, and record the canonical link.
   Intake leaves Implementer unchanged/empty, chooses a Verifier from actual risk, assigns bedrin-gpt separately, and records PR
   URL, author, fork/same-repo status, branch, exact head, labels, linked issues, and security/dependency metadata. Intake is not
   approval. Dependabot is one specialization of this universal intake, not the only PR source.
2. Reconcile stale exact-head lifecycle state after intake and before continuing workers or claiming ordinary queue work. Inspect
   every open PR whose canonical issue or PR has completed Review, Verification, Approval, or a derived Blocked/Human handoff
   supported by a different head. Use one guarded command expecting current Status, Execution, Executor, and the PR's new exact
   head to set Review / Ready / ChatGPT, clear stale ownership, and retain the PR URL plus new head as evidence. Identify the PR
   structurally with target + expected.head when the PR is canonical, or reviewPullRequest.number/head when a canonical issue owns
   it. Verify the terminal reaction, PR draft/head, and canonical Project state directly or through the required post-command
   status snapshot. This supervisory reconciliation may select Approval or Blocked items outside the ordinary queue, consumes at
   most one item, and precedes due-worker continuation.
3. Before claiming new work, inspect ChatGPT-owned Execution=In progress items whose worker, CI, external dispatch, rebase, draft
   publication, or monitoring observation is due. Worker observation belongs to this same event loop: first after 15 minutes,
   again 15 minutes later, then hourly while incomplete. Continue or recover existing ownership before starting unrelated work.
4. If a needed control command would exceed the rotation threshold, rotate the active control issue using control-plane.md, then
   continue this tick. Do not create a separate cleanup scheduler.
5. Otherwise select at most one canonical Project 2 item where:
   - Execution = Ready;
   - Executor = ChatGPT;
   - Assignee is empty or bedrin-gpt;
   - Status is Planning, Implementation, Review, or Verification;
   - the item is not a duplicate representation or a linked issue suppressed by an open canonical multi-issue PR;
   - current ChatGPT tools and identity can truthfully complete the lifecycle turn or reach a safe durable handoff now.
   Eligibility must not require Implementer to be populated.
6. Select deterministically using security priority, Project priority, ready timestamp, then repository and item number. Never
   create a duplicate Project item, worker, branch, or pull request.
7. Claim with one guarded delivery-control/v1 command. Set Execution=In progress plus the concrete tick/worker reference and
   lease, inspect the terminal reaction, and verify ownership through direct ProjectV2 or the control workflow's internal +1
   verification before work. If execution or external dispatch cannot start, release to Ready. Set Blocked only when Dmitry must
   decide or act. Require a post-command status snapshot before a later dependent Project handoff.
8. Perform exactly one lifecycle turn:
   - Planning: resolve outcome, canonical item, linked issues, decisions, non-goals, risk axes, Implementer when a future
     Implementation turn is actually needed, Verifier, and proof obligations. Do not use a PR author as a substitute for a
     deliberate implementation route.
   - Implementation: first ask whether ChatGPT can honestly edit, test, inspect, publish, and verify the focused change. For a
     same-repository existing PR, preserve the exact branch and PR; do not create a replacement or restart from develop. Route
     complex/persistent/existing-PR continuation to Local Codex by default. Route bounded fresh work to Codex Cloud. Fork and
     Dependabot branches are not adopted for direct agent correction unless policy explicitly allows it; use contributor feedback,
     bot commands, or a linked replacement task instead. Implementation may start only after a concrete Implementer/Executor route
     has been selected. Before handing off to Review, mark the intended PR ready for review, then re-read and verify open state,
     base=develop, draft=false, and the exact published head. Do not claim Review publication while the PR remains draft.
   - Review: inspect the complete exact-head diff, authoritative issue(s), tests, prior review threads, and matching-head CI.
     Submit APPROVE only with independent identity. When blockers remain and the reviewer is independent, submit one
     comprehensive REQUEST_CHANGES; when the reviewer is the PR author, publish the same complete blocking feedback as an
     ordinary PR comment and state the identity limitation. In either case, route the canonical item durably in this same tick;
     never leave the reviewed exact head in Review / Ready / ChatGPT and do not wait for the formal-review return adapter. Route
     an implementation/code/design defect to Implementation / Ready only after deliberately selecting an eligible Implementer,
     preserving the same same-repository branch, PR, and head. Route unresolved requirements, architecture, or canonicalization
     to Planning / Ready / ChatGPT. Use Blocked / Human only for one exact Dmitry decision or action. A technically acceptable
     self-authored PR proceeds to required Verification or Approval / Ready / Human rather than remaining in Review because it
     cannot be self-approved.
     When corrected work returns to Review and still has substantive blockers, perform the routing.md convergence checkpoint
     before another implementation dispatch; do not mechanically issue another patch list.
   - Verification: validate the observable result against the authoritative issue or PR using the exact published head/artifact in
     a representative environment. Do not rename unit tests or green CI as system verification.
9. Publish human-useful evidence on the canonical target issue/PR. Then use one guarded control command for the complete next
   Status, Execution, Executor, and cleared worker ownership state. For Status=Review, include the structured review PR identity;
   when the canonical item is an issue use reviewPullRequest.number/head. Inspect the terminal reaction and re-read PR draft/head
   state. Verify Project completion directly or through the control workflow's internal +1 verification, and require a snapshot
   generated after this command before another dependent Project transition. Update GitHub Assignee separately when supported and
   verify it. When a canonical issue owns a PR, keep its Worker reference pinned to the PR URL and exact head through
   Review/Verification. Once a claimed Review concludes, publishing its technical outcome and completing its guarded lifecycle
   route are one logical outcome in the same tick, although GitHub review/comment publication, Project mutation, and assignment
   are separate operations whose results must each be verified.
10. Routine waiting for a concrete CI run, worker, contributor update, bot rebase, or draft publication remains In progress only
   with a durable reference and next observation point. Blocked always routes an exact action to Human/bedrin.
11. Never merge, enable auto-merge, bypass protection, rewrite shared history, expose credentials, or perform privileged
    repository/hosting operations without Dmitry's explicit instruction.
12. If no eligible intake, head reconciliation, due continuation, control-log rotation, claimable turn, or meaningful
    reconciliation exists, make no GitHub/source mutation and reply only NO_CHANGE. Otherwise finish with a compact summary
    containing selected canonical item,
    PR and exact head when applicable, lifecycle turn, durable evidence, and resulting Status / Execution / Executor / Assignee.
```

After setup, smoke-test one empty tick and one disposable/read-only eligible item. Replacing a long defining task chat is manual
maintenance described in `docs/ai-delivery/chat-retention.md`; never delete an active task chat before its replacement works.
