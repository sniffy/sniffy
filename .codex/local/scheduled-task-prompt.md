# Sniffy app-native Local Codex dispatcher prompt

Copy the text block below into one automation attached to a persistent dispatcher conversation in the Codex app. Use the
best supported cadence and test it after app updates. This is one Sniffy adapter for the generic lifecycle and claim protocol
in `docs/ai-delivery/`; it is not a second policy.

```text
Run one Sniffy app-native Local Codex dispatcher tick in this existing dispatcher conversation.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Executor: Local Codex app
Worker template: .codex/local/worker-task-prompt.md

This conversation coordinates only. Never edit source, create a work branch, run implementation/verification commands,
launch nested CLI agents, review the worker's PR, merge, or enable auto-merge.

1. Read AGENTS.md and docs/ai-delivery/{README,lifecycle,event-loop,routing,supervision,verification}.md plus this prompt and
   the worker template.
2. Inspect project 2 and choose at most one sniffy/sniffy item where:
   - Execution = Ready;
   - Executor = Local Codex app;
   - Status = Implementation or Verification;
   - Assignee is empty or already matches the dedicated local-worker identity.
3. Re-read the issue, routing, proof matrix, Status/Execution/Implementer/Verifier/Executor, branch/PR, existing claims, worker
   references, review state, and CI. Prefer a valid re-queued continuation over fresh work. Never create a duplicate worker.
4. If no eligible item exists, create no task, conversation, worktree, branch, comment, or field mutation. Reply only
   NO_CHANGE.
5. Select a deterministic candidate by Project priority, ready timestamp, then issue number. Classify fresh versus
   continuation and choose model/reasoning from explicit routing.
6. Claim using docs/ai-delivery/event-loop.md: create a unique intent for the current dispatch generation, verify the winning
   intent, then set Execution = In progress, concrete Assignee, claim token/lease, and provisional worker state. If any claim
   mutation or later child creation fails, release to Ready. Set Blocked only when Dmitry must decide or act; then set
   Executor = Human, Assignee = bedrin, and record the exact requested action.
7. Render every placeholder in .codex/local/worker-task-prompt.md, including <STATUS>, <IMPLEMENTER>, and <VERIFIER>.
8. Create exactly one NEW one-time standalone app-owned task named "Sniffy #<issue-number> <status>: <issue-title>" using the
   current local project, a new isolated worktree, selected model/reasoning, and the rendered prompt. Never create it in this
   dispatcher conversation.
9. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh for this
   app-native adapter.
10. After confirming the child task exists, record its exact title/reference, claim token, lifecycle status, worktree, branch,
    and timestamp. If child creation fails, leave no In progress item without a worker reference.

Never dispatch the same lifecycle generation twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling the recurring dispatcher, prove with disposable/read-only items that:

- an empty tick stays in this conversation and creates no worktree;
- one eligible item creates exactly one standalone worker conversation and isolated WSL worktree;
- the worker receives the intended Status and routing fields;
- concurrent dispatcher attempts produce one winning claim;
- failed child creation releases the claim;
- a completed worker hands off to the next lifecycle status rather than creating its own reviewer conversation.

Repeat after material Codex automation changes. If nested one-time task creation is unavailable, pause this adapter and use a
manual app worker or the headless Linux adapter; do not substitute external UI automation.
