# ChatGPT project setup for Sniffy

This document describes the one-time setup for a ChatGPT Project used to supervise and review work in
[`sniffy/sniffy`](https://github.com/sniffy/sniffy). It intentionally points at repository-owned instructions instead of
copying them into ChatGPT, so policy and runbooks do not drift between the repository and the project configuration.

## Project instruction to add

Add the following text to the ChatGPT Project instructions:

```text
For every sniffy/sniffy task, first read and follow the current repository instructions at
https://github.com/sniffy/sniffy/blob/develop/AGENTS.md .

For website preview, browser inspection, and screenshots, also read and follow
https://github.com/sniffy/sniffy/blob/develop/docs/chatgpt-site-preview.md .

Treat the current GitHub issue, pull request, exact head SHA, reviews, and CI as the source of truth. Use the GitHub
connector when the shell sandbox cannot reach github.com. Never claim that source was checked out, npm was run, a site
was built, or a browser opened a real HTTP page unless that exact action completed and its output was inspected. Never
merge or enable auto-merge without Dmitry's explicit command.
```

Re-read the linked files at the start of each repository task. Do not pin the project instruction to a commit SHA: the
purpose of the link is to follow the current `develop` policy. Exact-head review and artifact evidence must still use the
immutable pull-request SHA being reviewed.

## Recommended project context

The ChatGPT Project may also contain a short project overview describing Sniffy's purpose, current initiatives, and the
maintainer's operating preferences. That overview is context, not an engineering-policy replacement. When it conflicts
with the repository, the current issue, `AGENTS.md`, and the exact reviewed GitHub state take precedence.

Do not store GitHub tokens, package-registry credentials, browser policy backups, downloaded artifacts, or generated
screenshots in the ChatGPT Project instructions. Credentials stay in their dedicated connector or agent environment;
temporary files stay in the disposable task sandbox.

## Why the preview runbook is separate

The browser available to a ChatGPT sandbox may be managed with a global `URLBlocklist`. The preview runbook records the
safe, reproducible procedure for downloading an exact-head website artifact, serving it over real localhost HTTP, adding
only a temporary exact localhost allowlist exception, using Playwright with the installed system Chromium, collecting
browser evidence, and restoring the original policy. Keeping this procedure in the repository makes it reviewable and
prevents ad hoc HTML, CSS, or image inlining from being presented as a real site preview.
