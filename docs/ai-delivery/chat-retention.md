# Chat retention for the ChatGPT event loop

The ChatGPT adapter uses four defining Scheduled Task chats for the `:00`, `:15`, `:30`, and `:45` shards. Product testing on
2026-07-30 showed that each recurrence appends to its task's defining chat rather than reliably creating a fresh destination
chat. The four conversations are persistent execution logs, not durable delivery state.

## Four isolated task chats

Create each Scheduled Task from a separate empty chat inside the fileless `AI Delivery Event Loop` Project:

- `Event Loop 00`;
- `Event Loop 15`;
- `Event Loop 30`;
- `Event Loop 45`.

Do not create all four from one shared chat. Keeping one chat per shard limits transcript growth and makes a bad occurrence easier
to locate.

Every recurrence must behave statelessly despite the persistent transcript:

- ignore prior-run conclusions and remembered queue state;
- re-read current GitHub state, repository policy, and `docs/ai-delivery/profile.yml`;
- never use a previous turn as the only copy of a decision, worker reference, or evidence;
- output only `NO_CHANGE` for a no-op;
- for productive ticks, use a compact header and result summary after durable GitHub publication.

## Durable-state rule

Before an occurrence ends, every meaningful result must be visible in GitHub:

- guarded control command and verified Project transition;
- worker/task/process, branch, PR, and exact SHA;
- review or verification result;
- CI/artifact evidence;
- blocker or next human action.

A chat may explain what happened, but GitHub remains sufficient to resume from another chat, executor, or machine.

## Defining-chat lifecycle

Deleting a defining chat pauses its Scheduled Task. Do not archive or delete an active defining chat as ordinary cleanup.

Instead, replace a shard deliberately when its conversation becomes unwieldy or after a routine retention interval such as one
to two weeks:

1. confirm the shard has no unfinished occurrence whose only evidence remains in the chat;
2. create a replacement chat in the same fileless Project;
3. create the equivalent Scheduled Task with the same slot and current canonical prompt;
4. smoke-test one no-op or read-only occurrence;
5. pause the old task;
6. archive the old defining chat;
7. verify the other three shards and the new shard still form one logical 15-minute clock.

This is maintenance of the existing clock, not a fifth scheduler. Do not rotate all four simultaneously.

## Current supported cleanup

OpenAI documents manual chat archive and delete operations. Archive hides a chat from the active sidebar while retaining it under
normal account retention. Delete is irreversible and normally schedules permanent removal subject to platform policy.

For event-loop chats:

- archive replaced defining chats after durable handoff is confirmed;
- retain chats associated with an unresolved incident until the diagnosis and recovery are durable in GitHub;
- delete only under an explicit retention decision;
- do not use account-wide **Archive all chats** or **Delete all chats** for routine event-loop maintenance;
- do not use browser-click automation, private endpoints, or destructive bulk cleanup.

No supported Project-scoped bulk archive API or Scheduled Task cleanup operation is assumed.

## Context and performance expectations

Do not assume an indefinitely long chat preserves every historical detail in active model context. The product may compact old
turns, and the UI may become inconvenient before any documented hard limit is reached. Stateless instructions and periodic
replacement prevent correctness from depending on either behavior.

The event loop should remain correct even if all four historical transcripts are unavailable, because Project fields, the
central control log, issues, PRs, reviews, branches, exact SHAs, CI, and repository-owned Markdown contain the durable state.

## Independent control-log rotation

Chat retention and the GitHub control issue are separate concerns. The active control issue is rotated by the normal event loop
according to [`control-plane.md`](control-plane.md); replacing a ChatGPT task chat must not create, clear, or modify any Project
claim. Likewise, closing an old control-log issue does not pause a Scheduled Task.
