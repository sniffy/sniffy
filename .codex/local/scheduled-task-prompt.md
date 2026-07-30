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

1. Read AGENTS.md, docs/ai-delivery/{README,profile,lifecycle,control-plane,event-loop,pull-request-intake,routing,supervision,verification}.md,
   this prompt, and the worker template.
2. Before new work, inspect Local-Codex-owned Execution=In progress items whose worker or monitoring observation is due. The
   normal event loop performs the 15-minute, second-15-minute, and hourly observations; do not create another monitor.
3. Otherwise choose at most one canonical Project item where:
   - Execution = Ready;
   - Executor = Local Codex;
   - Status = Implementation or Verification;
   - Assignee is empty or already matches the dedicated local-worker identity;
   - the item is not a duplicate PR/issue representation or a linked issue suppressed by an open canonical multi-issue PR.
4. Re-read the canonical issue or PR, all formally linked issues, routing, proof matrix, Project fields, exact branch/PR/head,
   existing worker reference, review state, and CI. Prefer a valid re-queued continuation over fresh work. Never create a duplicate
   worker, branch, or pull request.
5. An existing same-repository PR may be an adopted continuation even when Dmitry, ChatGPT, an IDE agent, or another configured
   worker created it. Adoption is valid only when the verified Project route explicitly sets Implementer/Executor=Local Codex for
   that canonical item. Reuse the exact branch and PR. Do not adopt fork or Dependabot branches for direct correction.
6. If no eligible item or due continuation exists, create no task, worktree, branch, target comment, or Project mutation. Reply
   only NO_CHANGE.
7. Select deterministically by Project priority, ready timestamp, repository, item type, then item number. The Local Codex profile
   remains Sol / extra-high; executor routing has already decided that this work merits the strong local environment.
8. Claim through the one active technical control issue using one guarded delivery-control/v1 command. Guard current Status,
   Execution=Ready, Executor=Local Codex, and exact PR head when the canonical item is a PR; set Execution=In progress plus claim
   token/lease inside the provisional Worker reference. Inspect the reaction and re-read Project state before spawning.
9. Render every placeholder in .codex/local/worker-task-prompt.md, including canonical work-item type/URL, authoritative linked
   issue, exact existing PR branch/head, repository ownership, author, and fresh/continuation/adopted-continuation mode.
10. Create exactly one NEW one-time standalone app-owned task named "Sniffy <work-item-type> #<number> <status>: <title>" using
    the current local project, a new isolated worktree, Sol, extra-high reasoning, and the rendered prompt. Never create it in this
    dispatcher conversation.
11. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh for this
    app-native adapter.
12. After confirming the child task exists, publish one guarded control command updating the final worker reference, then verify
    it. If child creation fails, release to Ready through the same protocol. Set Blocked only when Dmitry must decide or act.

Never dispatch the same lifecycle generation twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling the recurring dispatcher, prove with disposable/read-only items that:

- an empty tick stays in this conversation and creates no worktree;
- one eligible issue item creates exactly one standalone worker conversation and isolated WSL worktree;
- one explicitly routed same-repository PR continuation reuses its exact branch and PR without creating a duplicate;
- fork and Dependabot PRs are rejected for adopted direct correction;
- the worker uses Sol / extra-high and receives intended canonical item, Status, routing, and exact-head fields;
- concurrent guarded claims produce one success and one conflict without target-item claim comments;
- failed child creation releases the claim;
- a completed worker hands off the canonical item to the next lifecycle status rather than creating its own reviewer conversation.

Repeat after material Codex automation changes. If nested one-time task creation is unavailable, pause this adapter and use a
manual app worker or the headless Linux adapter; do not substitute external UI automation.
