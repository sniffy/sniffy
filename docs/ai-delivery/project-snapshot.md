# Read-only ProjectV2 snapshot workflow

The AI delivery control plane deliberately separates ProjectV2 reads from guarded mutations. A dispatcher must never infer a CAS
precondition from an earlier command outcome, and it must never use an idempotent `delivery-control/v1` write as a substitute for
reading current Project state.

When an executor has a maintained direct ProjectV2 read API, use that API. When ChatGPT or an operator cannot read ProjectV2 custom
fields directly, `.github/workflows/project-snapshot.yml` is the administrative on-demand fallback.

## Invocation

Run **AI delivery Project snapshot** with `workflow_dispatch` and supply:

- `repository`: canonical issue or pull-request repository in `owner/name` form;
- `number`: canonical issue or pull-request number.

The workflow is not scheduled, is not a required check, and does not claim or transition lifecycle work. Direct workflow dispatch
may be unavailable to some connected ChatGPT sessions; in that case a maintainer or another authorized GitHub client starts the
run and provides its run URL or ID for inspection.

## Result

A successful run publishes one normalized JSON document in two places:

1. the job summary, for immediate human and connector inspection;
2. a seven-day workflow artifact named `ai-delivery-project-snapshot-<repository>-<number>-<run-id>`.

The JSON includes:

- schema and observation timestamp;
- Project owner, number, ID, and title;
- target repository, number, type, URL, title, and state;
- pull-request head, base, and draft state when applicable;
- Project item ID;
- every supported lifecycle field with explicit `null` when absent;
- a deterministic `sha256:` revision over the normalized state, excluding the observation timestamp.

The artifact contains `ai-delivery-project-snapshot.json`. A missing target, inaccessible Project, or target not represented in
Project 2 fails the run instead of emitting a partial success.

## Safe CAS use

Every `expected.fields` value in a later `delivery-control/v1` command must come verbatim from one fresh snapshot or an equivalent
direct read. `null` means the reader observed an absent value; it must not mean “the dispatcher does not know.”

The revision is evidence that one normalized state was observed, not a replacement for field guards. The current mutation protocol
continues to compare explicit target type, exact pull-request head where applicable, and current Project fields inside its
serialized Actions job.

## Permissions and failure response

The workflow starts with `permissions: {}`. Its jobs receive only `contents: read` for checkout. Project and target reads use the
existing `PROJECT_TOKEN`; no issue, pull-request, contents, Actions, branch, review, merge, or Project mutation permission is
requested by the workflow.

On failure, inspect the run summary and logs, repair the input or `PROJECT_TOKEN` read access, and rerun manually. The workflow does
not post issue comments, reactions, or retry loops, and it does not write a repository cache.

## Lifecycle and removal

Sniffy AI delivery maintainers own the workflow. Retain it while a supported dispatcher lacks reliable direct ProjectV2 reads.
Remove it after all supported executors can obtain an equivalent fresh normalized snapshot through a maintained connector, MCP, or
API and no runbook depends on the Actions fallback.
