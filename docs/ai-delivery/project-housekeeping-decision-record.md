# Project housekeeping decision record

## Decision

Use GitHub Projects built-in workflows for objective terminal events and delayed archival. Reserve custom Actions automation for
bounded anomaly detection and guarded reconciliation that GitHub Projects cannot express safely.

## Chosen built-in workflows

- issue closed -> `Status = Done`;
- pull request merged -> `Status = Done`;
- `Status = Done` and older than 14 days -> archive.

## Rejected broad automation

- any `Ready` item -> Done;
- any closed pull request -> complete its linked issue;
- reopened item -> automatically reset to Planning;
- delete old Project items;
- create one issue/comment per anomaly.

These rules are rejected because they erase canonicalization decisions, create duplicate routing, or destroy useful audit state.

## Custom automation threshold

Implement a repository workflow only after the built-in workflows are enabled and a fresh status snapshot still shows recurring
non-terminal closed/merged items or other drift that cannot be expressed in Project configuration. The custom workflow must use the
status snapshot as a read model and the existing guarded control plane for mutation.

## Review point

Review anomaly counts after two weeks of built-in workflow operation. If deterministic drift remains, open a focused implementation
issue with observed examples and an acceptance-to-proof matrix before adding the custom workflow.