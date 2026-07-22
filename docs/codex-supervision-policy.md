# Codex supervision policy

This document defines the mandatory maintainer workflow for work delegated to Codex Cloud, the local Codex worker, or another implementation agent.

## Execution identity and source of truth

GitHub assignment, labels, and project fields serve different purposes and must not be treated as interchangeable.

- The GitHub assignee is a technical identity used by an agent to interact with issues and pull requests. It is not the source of truth for the implementation runtime or agent product. The same GitHub account may be reused by local Codex, local Claude, or another local worker.
- The GitHub Project `Executor` field is the source of truth for which executor is expected to perform the work, such as Local Codex, Codex Cloud, local Claude, or another explicitly named agent.
- Workflow status describes the delivery stage of the issue. A separate execution-request field may later distinguish start, continue, and retry requests without overloading status.
- Agent labels such as `agent:local` and `agent:cloud` are currently retained as transitional operational metadata. They may help filtering or compatibility with existing dispatch scripts, but they must not override or be used to infer the Project `Executor` value.

Do not remove or redesign the current agent labels as part of unrelated work. Their eventual retirement or replacement should be handled as an explicit workflow change after the Project field model and dispatchers are finalized.

## Dispatch and monitoring

Every time an agent receives a new task, retry, continuation request, or Request Changes follow-up, the supervising ChatGPT workflow must immediately schedule:

1. a first check after 15 minutes;
2. a second check 15 minutes later;
3. hourly monitoring after that while the task remains incomplete.

The first check must never be delayed until the hourly monitor. Returning an existing pull request to the agent counts as a new supervision cycle and requires fresh 15-minute checks even if an earlier monitor was stopped after approval.

Each check should verify that the intended executor claimed the task, continues on the expected branch and pull request, and has not silently stalled or opened a duplicate pull request. Executor verification must use the GitHub Project `Executor` field rather than inferring the executor from assignee or labels.

## Completion review

After the agent reports completion, the supervisor must independently inspect:

- the complete diff at the exact head commit;
- unresolved review comments and conversations;
- the relevant functional behavior, including local or artifact verification when applicable;
- all required CI checks for that exact commit.

Approve only when the implementation, review comments, and CI are all satisfactory. Otherwise, leave precise Request Changes and explicitly return the task to the agent in the same branch and pull request when possible. Starting that follow-up work requires a new 15-minute, 15-minute, then hourly supervision cycle.

## Merge boundary

Agents and the supervising ChatGPT workflow must never merge or enable auto-merge without an explicit maintainer instruction.
