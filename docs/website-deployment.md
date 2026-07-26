# Website deployment and rollback

The `Deploy website` GitHub Actions workflow publishes the reviewed Docusaurus site independently
of Maven Central, GitHub Packages, and Sniffy release events. It uses GitHub's official Pages
configuration, artifact, and deployment actions; it does not copy content to another repository or
call a hosting API directly.

## Activation and operator boundary

Repository maintainers own the protected `github-pages` environment and the repository's Pages
settings. Before the first deployment, they must review the existing Pages and custom-domain state,
configure the Pages build source as **GitHub Actions**, and apply any required environment reviewers.
Changing the Pages source, custom domain, DNS, `CNAME`, or HTTPS settings is a privileged maintainer
operation and is not performed by the workflow. The custom-domain cutover remains exclusively
authorized by [management issue #9](https://github.com/sniffy/management/issues/9).

The deployment job receives only:

- `contents: read` to check out an immutable reviewed revision;
- `pages: write` to create the Pages deployment; and
- `id-token: write` for GitHub's deployment attestation.

It does not receive package, issue, pull-request, secret, or workflow-write permissions. The
`github-pages` environment exposes GitHub's deployment URL and provides the audit trail.

## Triggers and validation

A reviewed push to `develop` triggers deployment when the site, shared theme, locked frontend
workspace, site source-snippet inputs, product version, or deployment workflow changes. Operators
can also dispatch the workflow manually. A manual `revision` must be a full commit SHA that is
already an ancestor of `origin/develop`; an unmerged branch cannot be deployed.

Before upload, the workflow repeats the complete site contract with Node 24.18.0 and a locked
`npm ci`: lint, formatting, typecheck, focused contract tests, broken-link and image validation,
search-index verification, desktop/mobile browser tests, portable artifact packaging, and packaged
artifact browser verification. It then rebuilds with the origin and base path reported by GitHub
Pages.

The official Pages artifact is named `sniffy-pages-<full-commit-sha>`. A
`.sniffy-deployment.json` manifest inside it records the deployed commit, Pages origin/base path,
and a SHA-256 digest of the static content. The workflow summary records that digest, the GitHub
artifact ID, and the deployed URL.

## Concurrency and failure response

All production runs share the `sniffy-website-production` concurrency group with
`cancel-in-progress: true`. A newer reviewed revision cancels an older queued or running revision,
so a stale run cannot publish after it.

A failed validation or deployment leaves the last successful Pages deployment live. Do not bypass
validation, patch a Maven release, or rerun a superseded commit. Inspect the failed Actions run,
fix the problem through a reviewed pull request, and deploy the corrected `develop` revision.

## Roll back a site revision

Rollback republishes a previously successful `develop` commit without publishing or rebuilding any
Maven artifact:

1. Open the earlier successful `Deploy website` run and record its full commit, artifact name and
   ID, content digest, and deployed URL from the workflow summary.
2. Confirm the commit is still an ancestor of `develop` and that its run passed the complete site
   validation.
3. Dispatch the current `Deploy website` workflow and enter that full 40-character commit SHA as
   `revision`.
4. Approve the protected `github-pages` environment deployment when required.
5. Verify the new run reports the expected commit and content digest, then check the deployment
   root, `docs/`, `docs/latest/`, representative documentation and use-case routes, assets,
   redirects, and the branded 404 below the reported non-canonical Pages base URL.
6. Preserve both the failed and restored run URLs in the incident record.

This procedure rebuilds only the deterministic static site from the known-good Git revision and
publishes it through the isolated Pages workflow. Restoring or changing the canonical
`sniffy.io` domain, DNS, TLS, or the legacy site is a separate cutover rollback owned by management
issue #9 and the architecture contract in
[`website-architecture.md`](website-architecture.md#rollback-contract).
