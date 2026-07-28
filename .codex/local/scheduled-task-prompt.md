# Sniffy local Codex dispatcher prompt

Copy the text block below into one Scheduled task attached to a persistent dispatcher chat in the ChatGPT/Codex desktop
app. Run it every 15 minutes against the local Sniffy project in WSL. This file is an executor launch prompt; shared policy
lives in `AGENTS.md` and `docs/ai-delivery/`.

```text
Run one Sniffy dispatcher cycle in this existing dispatcher chat.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Ready status: Ready for agent
Executor: Local Codex
Worker template: .codex/local/worker-task-prompt.md

This chat coordinates only. Never edit source, create an implementation branch, run tests, launch nested agents, merge, or
enable auto-merge.

1. Read AGENTS.md, docs/ai-delivery/README.md, docs/ai-delivery/routing.md,
   docs/ai-delivery/supervision.md, this prompt, and the worker template.
2. Inspect project 2 and select at most one sniffy/sniffy issue whose Status is "Ready for agent" and Executor is
   "Local Codex". Prefer an explicitly re-queued continuation with an existing local-worker PR; otherwise use Project
   priority and oldest issue number.
3. Confirm Definition of Ready, standard-tooling approval, routing, proof matrix, intended branch/PR, and that no local
   worker is genuinely active. Do not infer ownership from assignee or transitional labels.
4. If no eligible issue exists, create no task, chat, worktree, branch, or GitHub mutation. Reply only NO_CHANGE.
5. Classify the item as fresh or continuation. A continuation must reuse its exact open writable branch and PR. Never
   continue work owned by another executor or create a duplicate.
6. Select model/reasoning from explicit routing: agent:model:luna -> GPT-5.6 Luna/low; agent:model:sol -> GPT-5.6 Sol/high;
   otherwise GPT-5.6 Terra/medium.
7. Claim before spawning: set Status to In progress; ensure the dedicated local-worker assignee and agent:local label; post
   a claim comment with mode, branch, PR, model/reasoning, and ISO timestamp. Roll back every claim mutation if any step or
   child creation fails.
8. Render every placeholder in .codex/local/worker-task-prompt.md. Create one NEW one-time standalone app-owned task named
   "Sniffy #<issue-number>: <issue-title>" using the current project, a new isolated worktree, selected model/reasoning,
   and the rendered prompt. Never create it inside this dispatcher chat.
9. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh for this
   app-native path.
10. After confirming the child task exists, post its exact title and timestamp on the issue and report the routing here.
    If child creation fails, restore Ready for agent and record the exact failure.

Never dispatch the same issue twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling the recurring dispatcher, verify with a disposable/read-only case that an empty queue creates no chat or
worktree, one eligible issue creates exactly one WSL worker chat/worktree, the title contains the issue number, and failed
child creation rolls the claim back. Repeat after material desktop-app Scheduled-task changes. When nested task creation is
unavailable, pause the dispatcher and create the child manually; do not substitute external UI automation.
