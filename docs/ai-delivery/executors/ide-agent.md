# IDE-hosted coding agents

VS Code and IntelliJ IDEA are agent hosts, not executors by themselves. The selected GitHub Copilot, Codex, Junie, Claude,
or other coding product is the executor. Route it as `IDE Agent` or by its specific product name when that distinction is
operationally important.

## Repository instructions

- The nearest applicable `AGENTS.md` is the provider-neutral engineering contract.
- The current issue or pull request supplies task scope, decisions, routing, and proof obligations.
- IDE- or provider-specific instruction files are adapters only. They must not copy build commands, compatibility claims,
  test exceptions, or delivery rules already owned by `AGENTS.md` and `docs/ai-delivery/`.
- Before relying on nested instructions, smoke-test the installed client/version. When a client does not support a required
  scope, keep human oversight and provide the missing context explicitly rather than creating a conflicting policy tree.

## GitHub Copilot

`.github/copilot-instructions.md` is intentionally a short always-on adapter. It points Copilot to `AGENTS.md` and the current
issue.

`.github/agents/*.agent.md` is reserved for intentionally selectable **Copilot-specific agent profiles** with distinct role,
prompt, tools, or MCP configuration. Do not create files there merely to list generic Sniffy roles. Reviewer and tester are
roles dynamically assigned by delivery routing; today ChatGPT normally performs independent review and verification.

Add a Copilot custom agent only when all are true:

- Sniffy wants that profile to appear in the Copilot agent selector;
- its role/tool boundary is materially different from the default coding agent;
- the profile cannot be expressed by the current issue plus `AGENTS.md`;
- an owner and compatibility/retirement plan exist;
- the profile has been tested in the intended GitHub/VS Code/JetBrains surface.

## JetBrains

Coding agents inside JetBrains should follow `AGENTS.md`. Do not add `.junie/guidelines.md` or `.aiassistant/rules/` as a
second repository policy. Add a JetBrains-specific adapter only for a proven AI Assistant chat limitation that cannot be
addressed by the current issue or `AGENTS.md`.

## Delivery and proof

An IDE session is normally human-steered. The human owns task continuity unless an explicit unattended publication workflow
is configured. Before handoff:

- verify the exact branch and PR;
- run the issue's proof matrix and repository checks;
- inspect generated/unrelated changes;
- publish with the configured technical identity;
- keep the PR draft only while implementation or locally available proof is incomplete;
- when implementation is complete, mark the PR ready for review and re-read it as open, targeting `develop`, non-draft, and at
  the exact published head;
- record exact-head CI and limitations;
- if the canonical Project item is an issue, include `reviewPullRequest.number` and `reviewPullRequest.head` in the guarded
  `Status = Review` command; for a canonical PR, guard the target and `expected.head`;
- inspect the terminal reaction and re-read both PR draft/head state and Project fields before claiming Review handoff;
- use an independent reviewer identity;
- never merge or enable auto-merge without Dmitry's instruction.

The control plane may mark a remaining draft ready as a final invariant, but an IDE executor must not rely on that repair instead
of completing publication deliberately. Do not infer remote publication, independent review, or unattended supervision merely
because the IDE displays an agent conversation.