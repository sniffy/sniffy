# Delivery retrospectives

These documents are historical records of the repository and delivery system at the time of each incident. They explain why
current rules exist; they do not override the current issue, `AGENTS.md`, or the current control-plane contracts in
[`bedrin-management/ledger`](https://github.com/bedrin-management/ledger).

## Retrospectives

- [`2026-07-16-codex-cloud-issue-637.md`](2026-07-16-codex-cloud-issue-637.md) — separates dispatch, local completion,
  publication, review, approval, and merge; establishes remote GitHub state and real write probes as evidence.
- [`2026-07-issue-238-junit-jupiter.md`](2026-07-issue-238-junit-jupiter.md) — requires design/proof preflight, explicit module
  ownership, named test execution, authoritative issue updates, comprehensive first review, and re-baselining after repeated
  correction rounds.
- [`2026-07-18-docusaurus-cloud-vs-local.md`](2026-07-18-docusaurus-cloud-vs-local.md) — routes by independent risk/proof axes
  rather than line count, strengthens negative scope and exact commands, and distinguishes Cloud from persistent local work.

## Durable lessons and current homes

| Lesson | Current canonical location |
| --- | --- |
| Remote branch/SHA/PR state proves publication | root `AGENTS.md`, ledger exact-head contract |
| Delivery states must remain distinct | ledger lifecycle and lease contracts |
| First review covers the whole acceptance-to-proof matrix | root `AGENTS.md`, ledger worker contract |
| Tests must be discovered and execute the intended artifact | root `AGENTS.md`, target manifest |
| Product/global-state/module decisions are resolved before coding | root `AGENTS.md`, provider-neutral agent task template |
| Route by risk and evidence, not changed-line count | ledger deployment profile |
| After two substantive fix rounds, re-baseline instead of stacking prompts | ledger worker contract |
| Agent-authored PRs preserve independent formal review | root `AGENTS.md`, ledger worker contract |
| Standard tooling and platform features precede bespoke infrastructure | root and `.github/AGENTS.md` |

Future retrospectives should preserve incident facts, then update the appropriate canonical policy, template, runbook, or
skill when a repeated failure mode requires a durable change.
