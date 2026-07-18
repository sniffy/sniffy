---
name: publish-pr-screenshots
description: Publish reviewed Sniffy UI visual baselines or explicitly selected Playwright failure screenshots to Cloudinary, then update the matching GitHub pull request description or add a diagnostic comment. Use after visual review or when browser-test screenshots need to be visible directly in a PR.
---

# Publish PR screenshots

Use the bundled script rather than calling Cloudinary or editing the PR body ad hoc:

```bash
node .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs --help
```

The workflow uses an unsigned, restricted Cloudinary upload preset. It never needs a Cloudinary API secret and must not mark a pull request ready, enable auto-merge, or merge.

## Required configuration

Set these variables in both local Codex and the Codex Cloud environment:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_UPLOAD_PRESET
```

Do not commit their values. The upload preset name is not a cryptographic secret, but anyone who obtains it may be able to consume the restricted preset, so keep it in environment configuration and rotate it if abused.

The configured preset owns the Cloudinary asset folder, allowed formats, file-size limit, unique naming, and overwrite policy. `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, the Cloudinary product-environment ID, and a separate asset-folder variable are not used by this skill.

For Codex Cloud, enable agent internet access and allow:

| Domain | Methods | Purpose |
| --- | --- | --- |
| `api.cloudinary.com` | `POST` | Upload PNG files with the unsigned preset. |
| `res.cloudinary.com` | `GET`, `HEAD` | Verify each returned CDN URL before editing GitHub. |
| `api.github.com` | `GET`, `POST`, `PATCH` | Resolve the PR and edit its body or create a comment through `gh`. |
| `github.com` | `GET`, `HEAD` | Resolve repository and commit links used by `gh` and generated Markdown. |

GitHub authentication must already be available to `gh`. Follow `docs/codex-workflow.md` for `GH_TOKEN` persistence and GitHub agent-internet policy.

See [Cloudinary setup](references/cloudinary-setup.md) for local and cloud examples.

## Mode: reviewed baselines

Use this after the relevant visual tests pass and the new baselines have been inspected and intentionally approved.

```bash
node .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs \
  baselines --repo sniffy/sniffy --pr <number>
```

Without `--file`, the script publishes changed PNG files under:

```text
sniffy-ui/tests/e2e/visual-baselines/
```

Explicit files may be selected by repeating `--file`, but baseline mode still restricts them to that directory.

Baseline mode requires a clean worktree. It replaces only the PR-description block between:

```text
<!-- sniffy-ui-screenshots:start -->
<!-- sniffy-ui-screenshots:end -->
```

If the block is absent, the script appends it. It rejects malformed or duplicate markers.

## Mode: failure diagnostics

Use this to make useful browser-test failure PNGs visible directly in a PR comment:

```bash
node .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs \
  diagnostics --repo sniffy/sniffy --pr <number> \
  --heading "WebKit compact-trigger failure" \
  --file path/to/expected.png \
  --file path/to/actual.png \
  --file path/to/diff.png
```

Diagnostics mode requires explicitly selected `--file` arguments. Inspect every image before upload. Useful inputs include Playwright expected, actual, diff, and `test-failed-*.png` screenshots.

Do not automatically upload a whole `test-results` directory. Playwright traces, HAR files, HTML reports, logs, archives, request payloads, credentials, user data, or private infrastructure details are outside this PNG-only skill and must remain GitHub artifacts unless separately inspected and approved.

## Safety and publication checks

The script must complete these checks before changing GitHub:

1. Resolve the repository and open PR with `gh`.
2. Verify local `HEAD` exactly matches the remote PR head SHA.
3. Accept only non-empty PNG files inside the repository, no larger than 10 MiB.
4. Upload all selected files through the unsigned preset.
5. Require Cloudinary HTTPS URLs under the configured cloud name.
6. Verify every returned URL responds as an image.
7. Only then update the marked PR-description section or create the diagnostic comment.

Cloudinary uploads are not transactional. If a later upload or GitHub update fails, already uploaded assets may remain unused; report every successfully returned URL so they can be inspected or removed manually.

Use `--dry-run` to validate PR resolution, head SHA, paths, and file selection without uploading or editing GitHub.

## Verification

Run the focused tests after changing this skill:

```bash
node --test .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.test.mjs
```
