# Chat retention for the ChatGPT event loop

The current ChatGPT adapter creates a fresh chat for every scheduled tick. With four hourly shards, the `AI Delivery Event Loop`
Project may accumulate up to 96 chats per day. Chat history is operational telemetry, not durable delivery state.

## Separate permanent and disposable chats

Keep these two classes distinct:

- **Task-definition chats:** the four chats/configurations that own the recurring `:00`, `:15`, `:30`, and `:45` Scheduled
  Tasks. Keep them stable and do not delete them; deleting a chat associated with a task pauses that task.
- **Tick chats:** the fresh destination chats created by each scheduled occurrence. These are disposable after their durable
  GitHub handoff is complete.

Use stable Scheduled Task names such as `Event Loop 00`, `Event Loop 15`, `Event Loop 30`, and `Event Loop 45`. At the start of
each tick, print a compact run header containing slot, UTC timestamp, selected repository/work item/phase, or `NO_CHANGE`.
Do not rely on the generated chat title as the only identifier.

## Durable-state rule

Before a tick ends, every meaningful result must be written to GitHub: claim token, lifecycle handoff, issue/PR comment, exact
SHA, evidence, blocker, or `NO_CHANGE`. Never keep the only copy of a decision, diagnosis, or worker reference in a tick chat.

A chat is eligible for archive only when:

- no active claim or lease points to it;
- the next Phase/Status/Executor/Assignee is durably recorded;
- any PR review, evidence, blocker, or external dispatch is visible in GitHub;
- the tick is complete rather than waiting for CI or another result inside that chat.

## Current supported cleanup

OpenAI currently documents manual chat archive and delete operations:

- [How to Delete and Archive Chats in ChatGPT](https://help.openai.com/en/articles/8809935-how-to-delete-and-archive-chats-in-chatgpt)
- [Chat and File Retention Policies in ChatGPT](https://help.openai.com/en/articles/8983778-chat-and-file-retention-policies-in-chatgpt)

Archive hides a chat from the active sidebar but retains it under normal account retention. Delete is irreversible: the chat is
removed from view immediately and normally scheduled for permanent deletion within 30 days, subject to documented legal,
security, or de-identification exceptions.

For event-loop tick chats:

1. Confirm the durable-state rule above.
2. Keep chats for active or ambiguous work until the claim, blocker, correction, or CI wait is resolved elsewhere.
3. Archive completed and `NO_CHANGE` chats from each chat's `...` menu.
4. Manage archived chats through **Settings -> Data controls -> Archived Chats**.
5. Delete only under an explicit retention policy; archive is the default cleanup action.

Do not use **Archive all chats** or **Delete all chats** for routine cleanup: OpenAI documents those controls as account-wide and
inclusive of chats inside Projects. No supported Project-scoped bulk archive API or Scheduled Task action is currently
documented.

## Rollout and maintenance cadence

Because 96 manual archive candidates per day is substantial, treat the new-chat topology as an operational pilot before relying
on it indefinitely:

1. Run all four shards for 24 hours.
2. Confirm statelessness, claim safety, useful throughput, and actual chat volume.
3. Confirm that active work is distinguishable from completed/no-op ticks and that manual cleanup is tolerable.
4. Review again after one week before considering the topology permanent.

Archive completed/no-op tick chats daily while volume is high. Retain only chats linked to active claims, unresolved blockers,
current incident diagnosis, or a handoff whose GitHub evidence is incomplete. Use GitHub timestamps and claim records, not chat
ordering, to decide what remains active.

Manual per-chat archive is unlikely to scale indefinitely at the maximum 96-chat/day rate. If cleanup is not sustainable, do
not hide the problem behind unsupported automation. Choose deliberately among reducing the polling window/cadence, accepting a
persistent-chat shard, or moving the clock to the headless Local Codex adapter so ChatGPT runs only when work actually requires
a ChatGPT phase.

## Future automation boundary

A future maintenance worker may archive completed tick chats only after OpenAI exposes and documents a supported selective or
Project-scoped capability and that capability is smoke-tested. Do not implement browser-click automation, private endpoints,
or destructive bulk deletion merely to control sidebar clutter.

Chat retention remains independent from delivery lifecycle: archiving or deleting a transcript must never alter GitHub Phase,
Status, claims, issues, pull requests, reviews, or evidence.
