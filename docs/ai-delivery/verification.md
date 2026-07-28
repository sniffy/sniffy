# Verification ownership and evidence

Verification is planned separately from implementation. The primary executor must prove its own change, but important or
high-risk work also receives independent verification by ChatGPT, CI, a human, or a different environment.

## Proof owners

| Proof type | Primary owner | Independent check |
| --- | --- | --- |
| Focused unit or module behavior | implementer/test executor | exact test discovery and result inspection |
| Cross-JDK/platform compatibility | executor plus GitHub Actions | ChatGPT reviews the exact matrix jobs and relevant logs |
| Static policy, configuration, or workflow contract | executor | ChatGPT inspects exact-head diff and actual run when available |
| Browser/UI behavior | executor with Playwright or supported browser | ChatGPT reviews exact-head artifact, screenshots, errors, and routes |
| Generated resources and bundle limits | frontend executor | CI plus exact-head generated/bundle evidence |
| Publication | executor | ChatGPT verifies remote branch, full SHA, PR, refs, and head |
| Subjective product/visual acceptance | ChatGPT may pre-review | Dmitry decides |
| DNS, Pages settings, secrets, rulesets, or destructive migration | human operator | ChatGPT supplies and checks the operator checklist |
| Formal PR review | independent GitHub identity or human | exact-head state recorded |

The issue should name a `Verification owner` even when the GitHub Project does not yet expose that field.

## Acceptance-to-proof matrix

Before implementation, map each requirement or risk to:

- the allowed module/API/configuration boundary;
- a named unit, integration, browser, compatibility, or failure-path test;
- a specific CI job, static assertion, artifact inspection, or documented manual check;
- the environment and exact artifact under test;
- negative proof for explicitly disabled or forbidden behavior.

For complex tasks, distinguish setup, user/body failure, framework failure, cleanup failure, and combined failure. State
whether compatibility tests execute the same published artifact or rebuild sources.

## Exact-head rule

Evidence belongs to one immutable commit. Before completion:

1. record the current PR head SHA;
2. inspect the complete diff relative to current base;
3. verify review comments and unresolved threads for that head;
4. select workflow runs whose `head_sha` matches exactly;
5. verify artifacts, screenshots, logs, and summaries belong to that run/head;
6. rerun affected proof after every code or meaningful base merge.

An earlier green run does not prove a newer head. Codecov, bot comments, local summaries, and PR descriptions are supporting
evidence; reconcile them with current GitHub state.

## Honest execution claims

Never claim that a checkout, command, build, test, deployment, browser navigation, screenshot publication, or settings change
occurred unless that exact action completed and its output was inspected.

- A compile is not a test.
- `curl` is not a browser test.
- A downloaded artifact is not a local source build.
- A screenshot displayed in ChatGPT is not attached to GitHub.
- A dry-run push or task summary is not publication.
- A static workflow diff is not a successful production deployment.
- A retry is a separate attempt and must not conceal the original result.

When proof is unavailable, state the limitation and route the missing obligation to an executor or human that can perform it.

## Commands and test integrity

- Run focused tests first and independent obligations as separate commands with explicit exit status.
- Use clean outputs or isolated worktrees when switching JDKs.
- Confirm named tests in Surefire, Vitest, Playwright, or CI output.
- Do not weaken assertions, add timing sleeps/retries, skip tests, ignore failures, or update baselines/generated output to
  manufacture green evidence.
- A time-bounded command must use an explicit timeout and be reported as timed out when it does.
- Run `git diff --check` before publication.

Repository-specific commands remain in root and nested `AGENTS.md`; executor runbooks describe environment setup, not a
second build policy.

## Browser, artifact, and visual review

Prefer exact-head CI artifacts for independent website review. Serve unmodified output over real HTTP, load it with a
supported browser, record JavaScript errors and same-origin request failures, and inspect every generated screenshot.

Use [`../chatgpt-site-preview.md`](../chatgpt-site-preview.md) for the ChatGPT sandbox procedure. Frontend visual evidence
requirements live in [`../../sniffy-ui/AGENTS.md`](../../sniffy-ui/AGENTS.md). The screenshot-publishing skill is optional
publication tooling and does not replace visual inspection.

## Completion checklist

A task is ready to merge only when:

- every acceptance criterion has current proof;
- the implementation is remotely published at the recorded exact head;
- the complete diff is within scope and preserves compatibility/safety;
- all actionable comments and review threads are resolved or explicitly accepted;
- required exact-head CI passes;
- artifacts, browser behavior, or manual checks required by the issue were independently inspected;
- limitations and residual risk are documented;
- formal review state is honest about identity constraints;
- supervision/continuation tasks are stopped;
- Dmitry has not yet been bypassed: ready-to-merge remains distinct from merge.
