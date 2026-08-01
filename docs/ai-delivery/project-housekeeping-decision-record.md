# Project housekeeping decision record

## Decision

Use the GitHub Projects built-in merged-PR workflow for its objective terminal event and use delayed source-state archival. Keep the
combined issue-or-PR close workflow disabled, and reconcile closed issues through the guarded control plane until an issue-only
built-in capability is verified. Reserve custom Actions automation for bounded anomaly detection and guarded reconciliation that
GitHub Projects cannot express safely.

## Chosen built-in workflows and filters

- pull request merged -> `Status = Done`;
- issues or pull requests closed -> `Status = Done`: disabled because it also covers closed-unmerged PRs;
- `is:closed updated:<@today-14d` -> archive, regardless of Project `Status`.

The archive filter also removes stale closed-unmerged PR items from active views. The 14-day interval is therefore the window for
the status snapshot and guarded reconciliation to classify them; archival does not assert that their work was delivered.

## Platform evidence

- GitHub's [built-in automations documentation](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations)
  describes a combined close workflow and a separate merged-PR workflow.
- GitHub's [auto-archive documentation](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/archiving-items-automatically)
  limits workflow filters to `is`, `reason`, and `updated`.

## Rejected broad automation

- any `Ready` item -> Done;
- any closed pull request -> complete its linked issue;
- reopened item -> automatically reset to Planning;
- delete old Project items;
- create one issue/comment per anomaly.

These rules are rejected because they erase canonicalization decisions, create duplicate routing, or destroy useful audit state.

## Custom automation threshold

Keep closed-issue reconciliation explicit while its volume is low. Implement a repository workflow only after the supported
merged-PR and archive workflows are enabled and fresh status snapshots show that recurring deterministic drift justifies it. The
custom workflow must use the status snapshot as a read model and the existing guarded control plane for mutation.

## Review point

Review anomaly counts after two weeks of supported workflow operation. If deterministic drift remains, open a focused
implementation issue with observed examples and an acceptance-to-proof matrix before adding the custom workflow.
