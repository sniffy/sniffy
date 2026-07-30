# Sniffy app-native Local Codex dispatcher prompt

Configure the Codex automation and its worker tasks with the current Sniffy Local Codex profile: **Sol** model and
**extra-high** reasoning. The scheduler-level model is fixed; the dispatcher does not dynamically substitute Terra or another
model for individual items.

Copy the text block below into one automation attached to a persistent dispatcher conversation in the Codex app. Use the best
supported cadence and test it after app updates. This is one adapter for the generic lifecycle and control protocol in
`docs/ai-delivery/`; it is not a second policy.

```text
Run one Sniffy app-native Local Codex dispatcher tick in this existing dispatcher conversation.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Executor: Local Codex
Worker template: .codex/local/worker-task-prompt.md
Model/profile: Sol / extra-high reasoning

This conversation coordinates only. Never edit source, create a work branch, run implementation/verification commands, review
the worker's PR, merge, or enable auto-merge.

1. Read AGENTS.md, docs/ai-delivery/{README,profile,lifecycle,control-plane,event-loop,routing,supervision,verification}.md,
   this prompt, and the worker template.
2. Before new work, inspect Local-Codex-owned Execution=In progress items whose worker or monitoring observation is due. The
   normal event loop performs the 15-minute, second-15-minute, and hourly observations; do not create another monitor.
3. Otherwise choose at most one item where:
   - Execution = Ready;
   - Executor = Local Codex;
   - Status = Implementation or Verification;
   - Assignee is empty or already matches the dedicated local-worker identity.
4. Re-read the issue, routing, proof matrix, Project fields, branch/PR, existing worker reference, review state, and CI. Prefer a
   valid re-queued continuation over fresh work. Never create a duplicate worker.
5. If no eligible item or due continuation exists, create no task, worktree, branch, target comment, or Project mutation. Reply
   only NO_CHANGE.
6. Select deterministically by Project priority, ready timestamp, then issue number. The Local Codex profile remains Sol /
   extra-high; executor routing has already decided that this work merits the strong local environment.
7. Claim through the one active technical control issue using one guarded delivery-control/v1 command. Guard current Status,
   Execution=Ready, Executor=Local Codex, and exact PR head when applicable; set Execution=In progress plus claim token/lease inside the provisional Worker reference. Inspect the reaction and re-read Project state before spawning.
8. Render every placeholder in .codex/local/worker-task-prompt.md.
9. Create exactly one NEW one-time standalone app-owned task named "Sniffy #<issue-number> <status>: <issue-title>" using the
   current local project, a new isolated worktree, Sol, extra-high reasoning, and the rendered prompt. Never create it in this
   dispatcher conversation.
10. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh for this
    app-native adapter.
11. After confirming the child task exists, publish one guarded control command updating the final worker reference, then verify
    it. If child creation fails, release to Ready through the same protocol. Set Blocked only when Dmitry must decide or act.

Never dispatch the same lifecycle generation twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling the recurring dispatcher, prove with disposable/read-only items that:

- an empty tick stays in this conversation and creates no worktree;
- one eligible item creates exactly one standalone worker conversation and isolated WSL worktree;
- the worker uses Sol / extra-high and receives intended Status/routing fields;
- concurrent guarded claims produce one success and one conflict without target-item claim comments;
- failed child creation releases the claim;
- a completed worker hands off to the next lifecycle status rather than creating its own reviewer conversation.

Repeat after material Codex automation changes. If nested one-time task creation is unavailable, pause this adapter and use a
manual app worker or the headless Linux adapter; do not substitute external UI automation.
