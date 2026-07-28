# Chat retention for the ChatGPT event loop

The current ChatGPT adapter creates a fresh chat for every scheduled tick. With four hourly shards, the `AI Delivery Event Loop`
Project may accumulate up to 96 chats per day. Chat history is operational telemetry, not durable delivery state.

## Durable-state rule

Before a tick ends, every meaningful result must be written to GitHub: claim token, lifecycle handoff, issue/PR comment, exact
SHA, evidence, blocker, or `NO_CHANGE`. Never keep the only copy of a decision, diagnosis, or worker reference in a tick chat.

## Current supported cleanup

OpenAI currently documents manual chat archive and delete operations:

- [How to Delete and Archive Chats in ChatGPT](https://help.openai.com/en/articles/8809935-how-chat-retention-works-in-chatgpt)
- [Chat and File Retention Policies in ChatGPT](https://help.openai.com/en/articles/8983778-chatgpt-data-controls-faq)

Archive hides a chat from the active sidebar but retains it under normal account retention. Delete is irreversible and schedules
permanent deletion, normally within 30 days subject to documented exceptions.

For event-loop chats:

1. Confirm the tick has no active claim and its durable GitHub handoff exists.
2. Keep chats for active or ambiguous work until the claim, blocker, or correction is resolved.
3. Archive completed and `NO_CHANGE` chats from the chat menu.
4. Manage archived chats through **Settings -> Data controls -> Archived Chats**.
5. Delete only under an explicit retention policy; archive is the default cleanup action.

Do not use **Archive all chats** or **Delete all chats** for routine event-loop cleanup: OpenAI documents those controls as
account-wide and inclusive of chats inside Projects.

## Maintenance cadence

Until a supported project-scoped archive API or automation exists, cleanup is manual. A practical cadence is:

- daily when the event-loop Project becomes hard to navigate;
- otherwise weekly, retaining only chats linked to active claims, unresolved blockers, or current incident diagnosis.

Use GitHub timestamps and claim records, not chat ordering, to decide what remains active.

## Future automation boundary

A future maintenance worker may archive completed tick chats only after a supported ChatGPT capability is documented and
smoke-tested. Do not implement browser-click automation, private endpoints, or destructive bulk deletion merely to control
sidebar clutter. Chat retention must remain independent from the delivery lifecycle: archiving a transcript must never alter
GitHub Phase, Status, claims, issues, pull requests, or evidence.
