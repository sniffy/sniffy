# AI delivery status workflow checkout optimization

The status workflow executes small repository-owned scripts, but a default checkout materializes the whole repository and preserves
credentials even though each job needs only a bounded file set. This change narrows checkout without changing the runner, Project
queries, artifact schema, permissions, or scheduling semantics.

## Design

- use non-cone sparse checkout for the exact files consumed by each job;
- keep `fetch-depth: 1`;
- set `persist-credentials: false` because later steps authenticate explicitly through `GH_TOKEN` or action inputs;
- preserve trusted `ref: develop` for secret-bearing publication triggered by `workflow_run`;
- keep validation on `ubuntu-24.04`; runner migration is evaluated separately.

Do not combine `filter` with `sparse-checkout`: `actions/checkout` documents that the partial-clone filter overrides sparse checkout.
The bounded worktree is the intended optimization in this change.

The validation job requires the workflow, normalizer, focused tests, policy tests, profile, prompts, and documentation checked by
those tests. The publication job requires only `.github/scripts/delivery-status.js` from trusted `develop`.

## Expected effect

This reduces worktree materialization and avoids persisting checkout credentials. It does not eliminate action download time, Node
setup, GitHub API latency, git object negotiation, or artifact upload time. The optimization is intentionally independent from
`ubuntu-slim` so performance and compatibility results can be attributed to one axis.

## Failure semantics

Missing a required sparse path must fail the existing tests or publication step. Do not broaden the sparse set preemptively. If a
future test reads another file, add that path in the same change that introduces the dependency.

## Verification

- PR validation runs the existing focused tests, YAML parse, actionlint, and embedded shellcheck;
- inspect checkout logs for the expected sparse-checkout configuration;
- after merge, inspect one scheduled and one `workflow_run` publication;
- verify pointer digest, artifact contents, source SHA, and counts remain unchanged in meaning.

## Removal condition

Remove sparse checkout if GitHub checkout behavior makes it unreliable or if the workflow evolves to require most of the
repository. Keep `persist-credentials: false` while no later git operation needs checkout credentials.