# Ubuntu slim capability experiment

This experiment evaluates whether the AI delivery status workflow can move from `ubuntu-24.04` to `ubuntu-slim` without installing
most of its toolchain on every run.

## Required publication tools

The publication path requires:

- `git` for trusted `develop` checkout and source SHA;
- `gh` for Project, issue, and pull-request exports;
- `jq` for compact output extraction;
- `node` for normalization and pointer construction;
- `tar` and standard network/archive tooling used by official actions.

Validation additionally uses Ruby for YAML parsing and Docker-hosted actionlint. Those validation-only requirements do not imply
that the secret-bearing publication job must remain on the same runner.

## Probe

A temporary unprivileged workflow ran on `ubuntu-slim` with no checkout, repository secret, or write permission:

- run: `30678590810`;
- job: `91310799879`;
- runner image: `ubuntu:24.04`, slim image version `20260728.2.1`.

Observed tools:

| Tool | Result |
| --- | --- |
| `git` | present, 2.54.0 |
| `gh` | present, 2.96.0 |
| `jq` | present, 1.7 |
| `node` | present, 24.18.0 |
| `npm` | present, 11.16.0 |
| `tar` | present, GNU tar 1.35 |
| `curl` | present, 8.5.0 |
| `unzip` | present, 6.00 |
| `docker` CLI | present, 29.6.2 |
| `ruby` | missing |

The result proves that the recurring publication toolchain is already available without package installation. It does not prove
that the full validation toolchain is suitable: Ruby is absent, and the probe only checked the Docker CLI rather than relying on a
Docker daemon.

## Decision

- migrate only the `publish` job to `ubuntu-slim`;
- keep `validate` on `ubuntu-24.04` with the existing Ruby YAML parse and pinned actionlint container;
- remove the temporary probe workflow after recording the evidence;
- add a policy test so future edits do not accidentally move validation or both jobs together.

## Post-merge proof

The first scheduled or manual publication after merge must verify:

1. trusted `develop` checkout succeeds on slim;
2. `gh project field-list`, `gh project item-list`, issue export, and PR export succeed;
3. the Node normalizer and `jq` output extraction succeed;
4. `actions/upload-artifact` publishes the complete artifact;
5. `actions/github-script` updates the `ai-delivery-status` issue with matching artifact ID and digest;
6. source SHA, counts, artifact schema, and freshness semantics remain unchanged.

Until that smoke run succeeds, the capability probe is strong environment evidence but not complete production-path verification.

## Rollback

If the slim publication fails or is not materially faster/cheaper, restore only `publish.runs-on` to `ubuntu-24.04`. Do not weaken
validation, install packages on every run, or alter the snapshot/control semantics to preserve the migration.