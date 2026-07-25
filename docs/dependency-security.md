# Dependency security policy

Sniffy evaluates pull requests by the dependency risk they introduce. A newly published advisory for an unchanged
dependency on `develop` is important repository-health information, but it is not permission to expand an unrelated
feature pull request into dependency or toolchain remediation.

This document is the authoritative policy for npm dependency inputs, vulnerability comparisons, exceptions, overrides,
Dependabot, and default-branch monitoring. Java and Maven dependencies remain outside this policy.

## Pull-request classification

A pull request is dependency-impacting when its diff changes any npm manifest or workspace manifest, lockfile,
`npm-shrinkwrap.json`, `.npmrc`, dependency override, dependency-security exception/override registry, dependency
comparison implementation, or workflow step that controls npm installation or audit behavior. The executable detector
is `sniffy-ui/scripts/dependency-security.mjs`; its focused offline contracts are run by
`npm run dependency-security:test`.

The `Check Pull Request` workflow uses the actual merge base between the current PR base and head:

1. If no dependency input changed, the blocking audit comparison is skipped with an explicit job summary. Current
   repository vulnerabilities remain visible through the independent `Dependency Security` workflow.
2. If a dependency input changed, CI creates clean detached worktrees for the current base and the proposed merge
   result. It runs `npm ci` and `npm audit --json` for both with the same pinned Node 24/npm toolchain and current
   advisory data.
3. CI fails if the proposed result adds an advisory ID, raises severity, adds a vulnerable package/version or logical
   dependency route, or increases the vulnerable occurrence/route count, unless every affected finding is covered by a
   valid, explicitly authorized exception. Logical routes are represented by their exact reachable lockfile edges,
   which keeps hoisted and cyclic npm graphs finite without losing a newly introduced route.

The comparison deliberately does not store an old advisory snapshot. Running both audits with current advisory data
means newly disclosed risk in an unchanged graph appears on both sides instead of being falsely attributed to the PR.
A dependency-remediation issue may define a stronger zero-vulnerability acceptance criterion.

Run the deterministic contracts and registry validation with:

```bash
cd sniffy-ui
npm run dependency-security:test
npm run dependency-security:validate
```

The live PR comparison is CI-oriented because it creates clean Git worktrees and queries the current npm advisory
service:

```bash
npm run dependency-security:pr -- \
  --base <current-base-sha> \
  --head <pull-request-head-sha> \
  --audit-head <proposed-merge-sha>
```

## Explicit vulnerability exceptions

An exception is allowed only after a maintainer records the decision in the authoritative issue and pull request. Add
one narrow finding to `sniffy-ui/dependency-security-exceptions.json`; blanket advisory or severity suppression is not
supported. Every entry must record:

- advisory ID, vulnerable package, exact installed version, and exact logical dependency path;
- why no safe compatible alternative exists;
- risk and exposure analysis;
- owner and future review/expiry date;
- linked remediation issue;
- exact removal condition;
- authorizing issue and pull request.

The validator rejects missing fields, non-Sniffy authorization links, duplicates, and stale review dates. Applied
exceptions are printed in the PR job summary; they never hide findings from the scheduled repository-health signal.

## npm overrides

Every leaf override in `sniffy-ui/package.json` must have an exact matching entry in
`sniffy-ui/dependency-security-overrides.json`. The registry records rationale, dependency path, compatibility proof,
upstream tracking link, owner, review date, removal condition, and the authorizing pull request. Validation fails for an
undocumented manifest override, a version mismatch, a removed override that remains documented, duplicate selectors, or
an overdue review date.

Overrides should be parent-scoped and major-compatible whenever possible. A cross-major override requires explicit
maintainer authorization plus focused compatibility proof. Prefer upgrading or removing the parent dependency, and
remove the override as soon as its recorded removal condition is met.

The two existing `serialize-javascript@7.0.7` overrides are the parent-scoped compatibility exceptions approved in
[PR #681](https://github.com/sniffy/sniffy/pull/681). Their current proof and removal conditions are recorded in the
registry.

## Ongoing repository health

`.github/workflows/dependency-security.yml` runs weekly, on manual dispatch, and after dependency-input changes land on
`develop` or `master`. It performs a clean install, validates the registries, and reports the complete current
`npm audit` result. Any vulnerability makes that dedicated signal fail visibly. The workflow does not create or update
issues, avoiding automated duplicates and comment spam; Dependabot findings and a maintainer-owned remediation issue are
the durable work records.

Dependabot checks the `/sniffy-ui` npm workspace weekly. Patch and minor updates are grouped for review; major updates
remain separate so ecosystem migrations can carry their own compatibility proof.

## Agent and reviewer rules

- Classify dependency impact during issue preflight and again from the published diff.
- Do not repair unrelated repository-wide advisory drift in a feature pull request.
- Create or link a separate remediation issue when unchanged dependencies become vulnerable.
- Require dependency-impacting PRs to pass the merge-base comparison or carry a complete authorized exception.
- Keep one independently reviewable concern per pull request and report every dependency change, override, exception,
  and residual risk in the PR body.

Under this policy, [PR #710](https://github.com/sniffy/sniffy/pull/710) is non-dependency work: its published diff does
not change a manifest dependency, lockfile, override, exception, or install/audit behavior. Its PR comparison therefore
skips as an unchanged graph, while the current `brace-expansion` advisory remains visible in the independent health
workflow and belongs to issue #712.
