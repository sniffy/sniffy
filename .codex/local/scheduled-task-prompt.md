# Sniffy local Codex dispatcher prompt

Copy the text block below into a Scheduled task **attached to one persistent dispatcher chat** in the ChatGPT/Codex desktop app. Configure the task to run every 15 minutes against the local Sniffy project checkout, not a new worktree. The desktop app must use WSL for the Codex agent so all repository and GitHub commands run in Linux.

The dispatcher is intentionally read-only with respect to source code. A no-op cycle stays in the same dispatcher chat. An eligible issue is handed to a one-time standalone Scheduled task, which creates exactly one dedicated worker chat and one isolated worktree for that issue.

```text
Run one Sniffy dispatcher cycle in this existing dispatcher chat.

Repository: sniffy/sniffy
Book of work: organization project 2, https://github.com/orgs/sniffy/projects/2
Base branch: develop
Ready status: Ready for agent
Executor: Local Codex
Canonical worker template: .codex/local/worker-task-prompt.md

This chat is a coordinator only. Never edit source files, create an implementation branch, run tests, launch codex exec, or implement an issue in this dispatcher chat.

1. Read AGENTS.md, docs/codex-workflow.md, this prompt, and the worker template before making queue decisions.
2. Inspect project 2 and select at most one Issue from sniffy/sniffy whose Status is "Ready for agent" and Executor is "Local Codex".
3. Do not dispatch a second issue while another local-worker item is still genuinely active. Inspect the issue, project state, claim comments, linked pull request, and current remote state instead of relying on a stale label alone.
4. Prefer an explicitly re-queued continuation with an existing local-worker pull request and actionable maintainer feedback or failing required checks. Otherwise choose fresh work by project Priority and oldest issue number.
5. Confirm that the selected issue satisfies the Definition of Ready in docs/codex-workflow.md. Read the complete issue and comments, relevant project fields, linked pull requests, review submissions and unresolved threads, and current CI. Apply the standard-tooling and infrastructure-approval gate in AGENTS.md.
6. If no eligible issue exists, create no task, no chat, no worktree, no branch, and no GitHub mutation. Reply only NO_CHANGE.
7. Classify the selected item as fresh work or an explicitly re-queued continuation. A continuation must have an open writable local-worker pull request and branch plus actionable feedback or required failing checks. Do not continue work owned by another executor.
8. Select the worker model and reasoning effort from explicit issue or project routing. Use these defaults when no stronger instruction exists:
   - agent:model:luna -> GPT-5.6 Luna with low reasoning;
   - agent:model:sol -> GPT-5.6 Sol with high reasoning;
   - otherwise -> GPT-5.6 Terra with medium reasoning.
9. Claim the issue before spawning the worker:
   - set project Status to "In progress";
   - add the dedicated local-worker account as assignee when missing;
   - add agent:local when missing;
   - post a claim comment with worker name, fresh/continuation mode, intended or existing branch, existing pull request when applicable, selected model and reasoning, and an ISO-8601 timestamp.
10. If any claim mutation fails, roll back mutations already made and stop before creating a task.
11. Read .codex/local/worker-task-prompt.md and replace every placeholder with the selected issue, branch, pull request, model, and reasoning values. Do not leave unresolved placeholders in the child prompt.
12. Using the desktop app's native Scheduled task capability, create one NEW ONE-TIME STANDALONE task:
    - title: "Sniffy #<issue-number>: <issue-title>";
    - run once as soon as possible;
    - destination: a new standalone chat, never this dispatcher chat;
    - project: the current local Sniffy project;
    - environment: a new isolated Git worktree;
    - model and reasoning: the values selected above;
    - prompt: the fully rendered worker template.
13. Do not use shell UI automation, Python observers, codex app-server, codex exec, or .codex/local/run-issue.sh to create the worker. The child must be an app-owned task so its chat remains visible in the desktop app and Remote.
14. After the child task is confirmed created, post a second issue comment containing the exact child task title and timestamp. Keep Status "In progress". Report the issue number, worker task title, model, reasoning, and fresh/continuation mode in this dispatcher chat.
15. If child-task creation fails, remove the local claim changes, restore Status "Ready for agent", post the exact failure on the issue, and report the error here. Never leave an issue silently claimed without a confirmed worker task.

Never dispatch the same issue twice. Never merge or enable auto-merge.
```

## Required smoke test

Before enabling the recurring dispatcher, use a disposable issue or a read-only child prompt to verify that a run of an in-chat Scheduled task can create one one-time standalone child task in the current desktop-app version. The smoke test must prove:

- an empty queue adds no chat and no worktree;
- one eligible issue creates exactly one worker chat;
- the worker chat uses the expected WSL project and a separate worktree;
- the task title contains the issue number;
- a failed child creation rolls the claim back.

Repeat this smoke test after material desktop-app Scheduled-task changes. If nested task creation is unavailable, pause the dispatcher and use the documented manual app workflow; do not silently replace it with external UI automation.
