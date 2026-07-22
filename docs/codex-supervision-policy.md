# Codex supervision policy

This document defines the mandatory maintainer workflow for work delegated to Codex Cloud, the local Codex worker, or another implementation agent.

## Dispatch and monitoring

Every time an agent receives a new task, retry, continuation request, or Request Changes follow-up, the supervising ChatGPT workflow must immediately schedule:

1. a first check after 15 minutes;
2. a second check 15 minutes later;
3. hourly monitoring after that while the task remains incomplete.

The first check must never be delayed until the hourly monitor. Returning an existing pull request to the agent counts as a new supervision cycle and requires fresh 15-minute checks even if an earlier monitor was stopped after approval.

Each check should verify that the intended agent claimed the task, continues on the expected branch and pull request, and has not silently stalled or opened a duplicate pull request.

## Completion review

After the agent reports completion, the supervisor must independently inspect:

- the complete diff at the exact head commit;
- unresolved review comments and conversations;
- the relevant functional behavior, including local or artifact verification when applicable;
- all required CI checks for that exact commit.

Approve only when the implementation, review comments, and CI are all satisfactory. Otherwise, leave precise Request Changes and explicitly return the task to the agent in the same branch and pull request when possible. Starting that follow-up work requires a new 15-minute, 15-minute, then hourly supervision cycle.

## Merge boundary

Agents and the supervising ChatGPT workflow must never merge or enable auto-merge without an explicit maintainer instruction.
