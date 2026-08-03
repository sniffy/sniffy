# Sniffy ChatGPT Scheduled Task

Create this scheduler in **Chat**, not **Work**. Work is for a selected long-running lifecycle task, not a recurring heartbeat.
Keep the defining ChatGPT Project fileless.

Choose one clock:

- **budget:** one hourly task at `:00`;
- **full 15-minute queue latency:** four hourly tasks at `:00`, `:15`, `:30`, and `:45`, each from its own empty defining chat.

Use the same compact prompt below. Do not paste the detailed AI-delivery documents into the task definition.

```text
Run one logically stateless Sniffy AI Delivery dispatcher tick now.

Repository: sniffy/sniffy
Base branch: develop
Project: organization sniffy, number 2
Executor/supervisor: ChatGPT
GitHub assignee: bedrin-gpt

At start record startedAt and scheduler identity. Ignore conclusions from earlier occurrences. Read only:
- .chatgpt/status-snapshot-instructions.md
- docs/ai-delivery/runtime-contract.md
Do not read the full policy set before a candidate is selected.

1. Apply the status-snapshot instructions once. Use project-2-status.json.dispatch.orderedCandidates as the normal queue. Do not
   rescan/re-sort all Project items or live-read every PR. Select at most the first candidate. If none exists, return NO_CHANGE and
   telemetry.
2. Only for the selected candidate, re-read the minimum current GitHub state required by runtime-contract.md. A stale snapshot,
   changed head, guarded conflict, or no-longer-actionable candidate causes no work mutation.
3. Scan every open pull request in sniffy/sniffy targeting develop through the generated snapshot, regardless of author, label,
   bot/provider, branch creator, but live-read only the selected PR-attention candidate. Preserve these invariants:
   - Exactly one formal closing issue: that issue is the canonical lifecycle item.
   - No formal closing issue: the non-draft PR itself is the canonical lifecycle item.
   - Multiple formal closing issues: the non-draft PR is the canonical coordination item in Planning.
   - Draft PR: do not start Review.
   - Reuse a deliberately routed same-repository PR. Fork and Dependabot branches are contributor/bot-owned.
4. An unclaimable `Execution = Ready` route whose Assignee belongs to a different executor pool is a reconciliation candidate.
   Never silently filter out that mismatch.
5. For a due owned observation, follow the durable worker reference. Observe after 15 minutes, again 15 minutes later, then hourly.
   Do not create duplicate workers or monitoring tasks.
6. For a supervised Codex Cloud dispatch: require Status = Implementation, Execution = Ready, Executor = Codex Cloud. Codex Cloud
   does not poll Project 2. Claim while preserving Executor, post one exact implementation trigger, require durable acknowledgement,
   and record the first 15-minute observation point. If the trigger was not submitted or was definitively rejected, release to
   Ready. If submission succeeded but acknowledgement is uncertain, preserve that generation and never redispatch that generation.
   ChatGPT-supervised Codex Cloud `Execution = In progress` items are due-owned observations.
7. Every claim or handoff uses delivery-control/v1 on the active control issue. Emit the human-readable Markdown wrapper with the
   exact start/end markers and lowercase `json` fence; the marked JSON is authoritative. Never emit bare JSON. Guard current type,
   Status, Execution, Executor, and exact PR head when applicable. Verify the terminal reaction before work.
8. Perform at most one bounded lifecycle turn or one external dispatch. Load the canonical item, nearest AGENTS.md, exact PR,
   relevant reviews/threads/CI/artifacts, and detailed lifecycle/runbook sections only after claim.
9. Before Review, the PR must be open, target develop, be ready for review/non-draft, and match the exact published head. For a
   canonical issue include reviewPullRequest.number and reviewPullRequest.head; for a canonical PR guard expected.head.
10. In Review inspect the complete exact-head diff and matching-head evidence. If the reviewer is independent and blockers remain,
    submit one comprehensive REQUEST_CHANGES. If the reviewer is the PR author, publish the same complete findings as an ordinary
    PR comment and state the identity limitation. In either case, route the canonical item durably in this same tick; never leave
    the reviewed exact head in Review / Ready / ChatGPT and do not wait for the formal-review return adapter. Implementation/code/
    design defects go to Implementation / Ready after selecting an eligible Implementer; requirement/architecture/canonicalization
    defects go to Planning / Ready / ChatGPT; Blocked / Human requires one exact Dmitry action. A technically acceptable self-
    authored PR proceeds to Verification or Approval / Ready / Human.
11. Publish human-useful evidence, perform one guarded handoff, verify Project/assignment/PR state, and stop. Never merge, enable
    auto-merge, rewrite shared history, or perform privileged operations without Dmitry's explicit instruction.
12. Finish with one compact telemetry JSON object: startedAt, finishedAt, durationSeconds, adapter, scheduler, model/reasoning when
    exposed, snapshotRunId, selectedCandidate, outcome, and provider token counters when exposed. Otherwise set token fields to
    null and usageSource to unavailable; never invent exact token usage.
```

Smoke-test one empty tick and one disposable/read-only candidate before enabling recurrence. Replacing an existing task requires
manual setup because changing repository files does not rewrite a task's embedded prompt.
