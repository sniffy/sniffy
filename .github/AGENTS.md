# GitHub automation instructions

These rules apply to files under `.github/` in addition to the repository root instructions.

## Prefer platform capabilities

- Use GitHub-native features, maintained official actions, and declarative configuration before custom scripts or services.
- Add a new workflow or materially new job only when the authoritative issue contains the infrastructure rationale required
  by root `AGENTS.md` and explains why an existing workflow or job cannot host the behavior.
- Keep pull-request validation, production deployment, release publication, dependency submission, and scheduled health
  signals in separate failure domains unless the issue explicitly approves coupling them.

## Permissions and lifecycle

- Start with `permissions: {}` and grant the smallest job-level permissions required. Do not add write access, secrets,
  packages, issues, pull requests, or workflow permissions merely for convenience.
- Document triggers, concurrency, environment protection, blocking semantics, expected noise, owner, maintainer response to
  failure, and removal or migration conditions for new automation.
- Pin runtimes and security-sensitive third-party actions consistently with current repository policy. Do not replace an
  official action with bespoke API code without explicit approval.
- Repository, Pages, environment, custom-domain, DNS, CNAME, HTTPS, ruleset, and secret changes are privileged operator
  actions. A workflow change must not perform or imply them unless the issue and Dmitry explicitly authorize them.

## Validation

- Preserve existing required-check names and semantics unless the issue explicitly changes branch protection expectations.
- Run the relevant YAML parser, `actionlint`, formatting checks, focused workflow-contract tests, and `git diff --check`.
- Inspect the exact-head workflow diff and resulting GitHub Actions jobs. A green unrelated PR workflow does not prove a new
  production or scheduled workflow path before that path actually runs.
- Keep issue templates provider-neutral unless a template is intentionally tied to one product. Executor choice belongs to
  delivery routing, not to repository engineering policy.
