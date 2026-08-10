# ChatGPT project setup for Sniffy

ChatGPT scheduler prompts and executor contracts moved to the private
[`bedrin-management/ledger`](https://github.com/bedrin-management/ledger). Repository changes cannot rewrite prompts already
embedded in scheduled tasks; the ledger migration checklist includes the required manual cutover and smoke tests.

Sniffy-specific engineering instructions remain in [`AGENTS.md`](../AGENTS.md).
