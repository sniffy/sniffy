# Sniffy ChatGPT Scheduled Task prompt

Create a fileless ChatGPT Project named `AI Delivery Event Loop`. Inside that Project, create four hourly Scheduled Tasks whose
destination is **new chat**:

```text
Event Loop 00 -> every hour at :00
Event Loop 15 -> every hour at :15
Event Loop 30 -> every hour at :30
Event Loop 45 -> every hour at :45
```

Paste the following block **unchanged** into each task. The four tasks use exactly the same prompt; only their schedules and names
differ.

```text
Run one stateless Sniffy AI Delivery Event Loop tick now.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Executor: ChatGPT
GitHub assignee: bedrin-gpt

Use current GitHub state as the source of truth. Before any claim or mutation, read the current versions of:
- https://github.com/sniffy/sniffy/blob/develop/AGENTS.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/README.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/lifecycle.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/event-loop.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/pull-request-intake.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/routing.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/supervision.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/verification.md
- https://github.com/sniffy/sniffy/blob/develop/docs/ai-delivery/executors/chatgpt.md

This occurrence is one fresh, self-contained tick. Do not rely on previous chats, ChatGPT Project files, implicit memory, or an
unpublished local state. Do not create another Scheduled Task, child ChatGPT task, or promise future/background work. Use the
current Scheduled Task name as the dispatcher slot; if it is unavailable, use this chat/task identifier plus the current UTC
timestamp.

Perform the following protocol:

1. Reconcile external pull-request intake before ordinary queue work. Find eligible open sniffy/sniffy dependency PRs missing
   from Project 2, verify their actual author, labels, target, draft state, and exact head, and materialize them idempotently as
   documented. Intake is not approval.
2. Before claiming new work, inspect any ChatGPT-owned `Execution = In progress` item whose recorded worker, CI, external
   dispatch, or monitoring follow-up is due. Continue or recover that existing ownership rather than starting a duplicate.
3. Otherwise select at most one Project 2 item where:
   - `Execution = Ready`;
   - `Executor = ChatGPT`;
   - `Assignee` is empty or `bedrin-gpt`;
   - `Status` is Planning, Implementation, Review, or Verification;
   - the current ChatGPT tools and identity can truthfully complete the lifecycle turn or reach a safe durable handoff now.
4. Select deterministically using security priority, Project priority, ready timestamp, then repository and item number. Never
   create a duplicate Project item, worker, branch, or pull request.
5. Claim using the best-effort protocol in event-loop.md: re-read Status, Execution, Executor, Assignee, generation, branch, PR,
   worker reference, and existing claims; publish a unique claim intent; determine the winning intent; set
   `Execution = In progress`, `Assignee = bedrin-gpt`, claim/lease metadata, and this tick's concrete reference; then re-read and
   verify ownership before doing work. If the claim cannot be established, make no work mutation.
6. Perform exactly one lifecycle turn according to the current issue or PR and the repository policy:
   - Planning: resolve the outcome, decisions, non-goals, risk axes, Implementer, Verifier, and proof obligations, then publish a
     precise handoff.
   - Implementation: only perform a focused change when source, dependencies, tests, publication, and identity are genuinely
     available in this execution. Otherwise route to a capable Implementer without pretending implementation occurred.
   - Review: inspect the complete exact-head diff, issue decisions, tests, review comments and threads, and matching-head CI.
     Submit APPROVE only when the implementation is acceptable and the GitHub identity is independent from the PR author.
     Otherwise submit one precise REQUEST_CHANGES or record the identity limitation. Never approve bedrin-gpt's own PR.
   - Verification: validate the observable result against the authoritative issue using the exact published head or artifact in
     a representative environment. Do not relabel unit tests or green CI as system verification.
7. Publish durable evidence in GitHub before ending the tick. Update the next `Status`, `Execution`, `Executor`, and `Assignee`,
   clear completed claim/lease ownership, and record exact branch, PR, SHA, commands, CI, artifacts, review, verification, and
   limitations as applicable.
8. Use `Execution = Blocked` only when Dmitry must decide or act. Keep the current Status, set `Executor = Human`,
   `Assignee = bedrin`, and record the smallest exact requested action. Routine waiting for a concrete CI run, worker, or
   external operation remains `In progress` only when a durable reference and next monitoring point are recorded. Never leave
   `In progress` without real current ownership or observable work.
9. Never merge, enable auto-merge, bypass protection, rewrite shared history, expose credentials, or perform privileged
   repository/hosting operations without Dmitry's explicit instruction.
10. If no eligible intake, due continuation, claimable lifecycle turn, or meaningful reconciliation exists, make no GitHub or
    source mutation and reply only `NO_CHANGE`. Otherwise finish with a compact summary containing the selected item, performed
    lifecycle turn, durable evidence, and resulting Status / Execution / Executor / Assignee.
```

After creating the four tasks, smoke-test one empty tick and one disposable/read-only eligible item before relying on the loop.
Deleting a task's defining chat pauses that task; keep the four task-definition chats stable.