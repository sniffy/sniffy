# Ubuntu slim capability experiment

This experiment evaluates whether the AI delivery status workflow can move from `ubuntu-24.04` to `ubuntu-slim` without installing
most of its toolchain on every run.

## Required publication tools

The current publication path requires:

- `git` for trusted `develop` checkout and source SHA;
- `gh` for Project, issue, and pull-request exports;
- `jq` for compact output extraction;
- `node` for normalization and pointer construction;
- `tar` for action/tool extraction.

Validation additionally uses Ruby for YAML parsing and Docker for the pinned actionlint container. Those validation-only
requirements do not imply that the secret-bearing publication job must remain on the same runner.

## Experiment sequence

1. restore both production jobs to `ubuntu-24.04`;
2. run a separate unprivileged `ubuntu-slim` capability probe with no checkout, secrets, or write permission;
3. record availability and versions of `git`, `gh`, `jq`, `node`, `ruby`, `docker`, `tar`, and `curl`;
4. if `gh`, `jq`, and the publication prerequisites are already available, migrate only the publish job and smoke-test a real
   scheduled/manual publication after merge;
5. if they are absent and setup cost removes the expected latency/cost benefit, close the experiment without changing production.

The validation job is not a migration target unless its Ruby and Docker-dependent checks are replaced by equally strong pinned
validation. Runner migration must not weaken YAML parsing, actionlint, shellcheck, or focused tests.

## Success criteria

- capability evidence comes from a real `ubuntu-slim` job;
- no secret is exposed to the probe;
- any production migration preserves artifact schema, digest, pointer publication, source SHA, and Project read behavior;
- the status workflow remains within its existing timeout and least-privilege boundary.

## Stop condition

Stop and retain `ubuntu-24.04` if the slim runner lacks `gh` or `jq`, requires repeated package installation, cannot support the
official actions used by publication, or produces no meaningful runtime/billing improvement.