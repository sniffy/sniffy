# Sniffy website architecture and migration contract

Status: approved implementation contract for [sniffy/sniffy#655](https://github.com/sniffy/sniffy/issues/655), under the
[website initiative](https://github.com/sniffy/management/issues/8). This document specifies later work; it does not
implement the website or alter either publishing system.

## Evidence and decision boundary

This contract was checked on 2026-07-17 against:

- this repository's `sniffy-documentation` Maven module, its AsciiDoc sources and assembly descriptor;
- the root Maven reactor, `.github/workflows/deploy.yml`, `.github/workflows/pr.yml`, and the repository's `develop`
  default branch;
- the private `sniffy-ui` npm workspace, its `apps/*` and `packages/*` layout, root scripts, and
  `packages/theme` exports and CSS custom properties;
- `sniffy/sniffy.github.io` at commit `7e9a5d3db7d23913f5d9e8be8e3f9bc2efc834ce`, including its Jekyll configuration,
  `CNAME`, legacy landing pages, checked-in `docs/` output, and commit history; and
- the GitHub Pages configuration for that repository: the legacy Pages build publishes the root of `master` with the
  custom domain `sniffy.io` (HTTPS enforcement was off when inspected).

Repository evidence agrees with the product decisions in management#8: Docusaurus can live in the existing frontend
workspace without changing the public-domain or ownership model. If later evidence requires a different public URL,
repository owner, or migration strategy, work must stop and return to management#8 rather than silently changing this
contract.

## Goals, MVP, and non-goals

The MVP provides:

1. a branded homepage and site shell;
2. current and next documentation, migrated to MDX with live, tagged source snippets;
3. stable current documentation, a reserved contract for later versioned archives, and compatibility for useful legacy
   documentation URLs and anchors;
4. a reusable use-case landing-page pattern and at least one complete landing page;
5. validation and production deployment independent of Sniffy artifact releases; and
6. a controlled `sniffy.io` cutover with a tested rollback.

Explicit non-goals are implementing the site in this task, changing production or generated assets, refactoring theme
tokens, migrating content, changing a deployment workflow, publishing a blog in the MVP, or retiring the legacy system
before stabilization. `demo.sniffy.io` and its application are excluded: no route, DNS, deployment, content, or runtime
change to the demo belongs to this initiative. Additional use-case pages, historical-version recovery, the Git-backed
blog, and Asciidoctor retirement are follow-up scope. Specifically, #670 (historical inventory) and #671 (including the
Sniffy 3.1 archive) are follow-up work and do not block MVP acceptance or cutover.

## Ownership and repository placement

### Application and design system

- The Docusaurus application is owned by the main `sniffy/sniffy` repository at `sniffy-ui/apps/site`. It participates
  in the existing private npm workspace (`apps/*`, `packages/*`) and uses the workspace's Node 24 baseline. Site source,
  configuration, MDX, static files, and site-only build helpers stay below that application unless a helper is genuinely
  shared by another frontend application.
- Shared product primitives remain owned by `sniffy-ui/packages/theme`. Today `@sniffy/theme` exports `tokens.css` and
  `tailwind.css`; its tokens cover product colors, typography, spacing, radii, shadow, and density. Later issues may
  deliberately evolve that package, but the site must consume it rather than copy it.
- A token used by two or more Sniffy applications, or expressing product identity/semantics, belongs in
  `packages/theme`. A Docusaurus component/layout token belongs in `apps/site` and must use a site-specific name. The
  site may alias shared tokens and add site-local tokens, but must not override the meaning of a shared token, expose
  site-local variables as a product contract, or make other applications import from `apps/site`.

### Content

| Content | Canonical owner after migration | Policy |
| --- | --- | --- |
| Homepage and global navigation/footer | `sniffy-ui/apps/site` | Product marketing content reviewed with the site shell. |
| Use-case landing pages | `sniffy-ui/apps/site` | Use the shared landing-page pattern; each page owns its copy and media. |
| Current and next docs | `sniffy-ui/apps/site` MDX | Main-repository source is authoritative and may include tagged source from the reactor. |
| Released/historical docs | Docusaurus versioned content in `sniffy-ui/apps/site` | Immutable snapshots; provenance and gaps are recorded when imported. |
| Future blog | Git-backed content in `sniffy-ui/apps/site` | Reserved but disabled for MVP; publication occurs through reviewed pull requests. |

Until cutover, AsciiDoc in `sniffy-documentation` and the published files in `sniffy/sniffy.github.io` remain the legacy
sources. During migration they are comparison inputs, not places to author the new site. The authoritative source changes
only at the cutover gate; retirement is a later, explicit issue.

## Current-state inventory

### AsciiDoc module and generated output

The root Maven reactor includes `sniffy-documentation`. Its Asciidoctor Maven execution runs in `generate-resources`,
uses HTML5 with a left-hand table of contents, sets `sniffy-version` from the Maven project version, and emits the plugin's
`target/generated-docs` output. The assembly packages that directory as both ZIP and tar.gz documentation classifiers.
Consequently documentation generation is currently coupled to Maven builds and artifact packaging.

`sniffy-documentation/src/main/asciidoc` contains 19 `.adoc` files:

```text
index.adoc
install.adoc
setup-spring.adoc        setup-datasource.adoc    setup-filter.adoc
setup-containers.adoc    configuration.adoc       nio-monitoring.adoc
test-api.adoc            test-shared-connection.adoc
test-junit.adoc          test-kotest.adoc          test-spring.adoc
test-testng.adoc         test-spock.adoc
network-issues.adoc      capture-traffic.adoc
migration-to-4.0.adoc    migration-to-3.1.adoc
```

There are three source images: `images/agent-ui.png`, `images/demo.gif`, and `images/network-connections.png`.
`index.adoc` is the composition root and includes the other topical files except `nio-monitoring.adoc`, which is included
by `configuration.adoc` behind the explicit `[[nio-monitoring]]` anchor.

The content also includes source outside the documentation module. These dependencies must become fixtures for the
tagged-snippet feature rather than copied code:

- JUnit overview and disabled-socket examples;
- capture-traffic overview (also included from `capture-traffic.adoc`);
- core API imperative, functional, and resource examples;
- shared-connection datasource usage;
- JUnit, Kotest, Spring, TestNG, and Spock usage tests; and
- the JBoss module descriptor.

Most headings rely on Asciidoctor-generated underscore-style IDs. One explicit cross-reference targets `_filter`, one
explicit ID is `nio-monitoring`, and a live source link targets `#_standalone_setup`. The complete generated anchor set,
not merely explicit source anchors, is therefore a compatibility input for #662.

### Existing publishing path and legacy site

The main repository's `Build and deploy` workflow runs on `develop` pushes and releases. Its final deployment job builds
and publishes Maven artifacts to Maven Central and GitHub Packages after the test matrix; it does not publish GitHub
Pages or copy files to `sniffy/sniffy.github.io`. The PR workflow validates the existing frontend workspace and Maven
reactor. This is evidence of current coupling of generated documentation to Maven, but not evidence of an automated site
copy step.

The separate `sniffy/sniffy.github.io` repository is the publication source currently served by GitHub Pages. It is a
Jekyll repository with `CNAME` set to `sniffy.io`, `baseurl` empty, and checked-in Asciidoctor HTML under `docs/latest`.
Its history records manual-looking documentation updates; no Pages deployment workflow is present. `docs/index.html`
redirects `/docs/` to `https://www.sniffy.io/docs/latest/`. The checked-in latest directory contains `index.html` plus
topic pages for capture traffic, configuration, installation, migration to 3.1, network issues, setup variants, test API,
and test integrations. The generated pages embed the Asciidoctor stylesheet and expose generated heading IDs.

The same repository owns the legacy homepage and useful landing URLs, including:

- `/continuous-java-profiling/`;
- `/sniffy-chaos-engineering/`;
- `/sniffy-traffic-capture/`; and
- `/sniffy-verify-number-of-sql-queries-in-unit-tests/`.

Other existing repository paths (for example `/chrome-extension/`, contact/credits pages, images, `demo.gif`, and the
Jekyll feed) must be inventoried before cutover. Absence from the MVP does not authorize an accidental 404: #662/#669
must explicitly preserve, redirect, or record retirement for every discovered public path.

## Public route and versioning contract

All paths below are on the canonical `https://sniffy.io` origin. `www.sniffy.io` must resolve consistently to the
canonical origin while preserving path, query, and fragment where the platform permits.

| Route | Contract |
| --- | --- |
| `/` | Branded homepage. It is not documentation and must not redirect to docs. |
| `/docs/` | Current stable documentation. This is the canonical stable-doc URL. |
| `/docs/next/` | Documentation for unreleased `develop`; clearly labelled non-stable and excluded from stable-version canonicalization. |
| `/docs/<version>/` | Reserved contract for follow-up #670/#671. Each route is an immutable released snapshot, with its normalized version label chosen by #670 (at minimum `/docs/3.1/` for the planned Sniffy 3.1 archive). Existing version URLs may never be silently repointed to different content. |
| `/docs/latest/` | Permanent compatibility alias that redirects to `/docs/`, retaining the remainder of a legacy topic path, query, and fragment where supported. It is not a separately authored version. |
| `/use-cases/<slug>/` | Canonical namespace for reusable landing pages. Initial slugs are owned by #666-#668; old landing paths redirect to their approved equivalents after parity review. |
| `/blog/` | Reserved. Before #673 enables a reviewed Git-backed blog, it must not expose sample posts or an accidental Docusaurus blog; return a deliberate not-found response or an explicit, non-indexed placeholder chosen in #673. |

Use trailing slashes for canonical directory routes. Internal links use canonical routes, not `/docs/latest/`. Stable
docs are snapshotted when a release is intentionally documented, but site deployment is not triggered by, blocked on,
or performed inside Maven artifact publication. `/docs/` may advance only after its chosen content passes the site
pipeline; `/docs/next/` tracks reviewed main-repository changes. Version selectors must distinguish current, next, and
archived content.

For old multi-page HTML URLs such as `/docs/latest/install.html`, #662 must supply an explicit redirect to the matching
new MDX page rather than depending on a catch-all. Fragment compatibility is tested against the generated legacy anchor
inventory. Prefer preserving IDs on the destination; where that is impossible, use a documented client-side fragment
translation that does not discard the fragment. Redirects must be single-hop, avoid loops, and preserve query strings.

## Build, validation, and deployment lifecycle

The site has a site-specific workflow and deployable artifact. It installs the locked npm workspace, builds Docusaurus,
validates links/routes/redirects/snippets, and publishes only the static site. It must not invoke Maven deployment, sign
artifacts, require Maven Central credentials, or wait for a Sniffy release event. Maven release workflows must not deploy
the site.

Pull requests that affect the site run site validation through #659. Production publication through #669 is authorized
from a reviewed main-repository revision using a concurrency guard and an auditable environment. The exact Pages host,
branch/artifact mechanism, DNS changes, permissions, and secrets belong to #669 and management#9; they must implement
this lifecycle without changing the route contract. A library release can supply a documentation snapshot in a normal
reviewed change, but release success and site availability remain independent failure domains.

## Migration, cutover, rollback, and retirement

### Stages and gates

1. **Record and baseline (#655).** Preserve inventories of current source includes, generated pages and anchors, public
   Pages paths, response behavior, and the external repository commit. No traffic changes. Historical-version inventory
   remains follow-up #670 and is not a cutover gate.
2. **Build foundations (#656-#660).** Refactor reusable tokens, add opt-in light theme, create the application and CI,
   and implement tagged snippets. The legacy site remains production.
3. **Migrate and prove current docs (#661-#662).** Port content without hand-copying snippet bodies. Produce a
   machine-checkable manifest mapping every legacy documentation page and anchor to a new destination; test all required
   routes and redirects against a preview artifact.
4. **Build the public experience (#663-#668).** Add shell, homepage, and landing system/content. Compare legacy landing
   paths and explicitly map them. The demo remains untouched.
5. **Prepare production (#669).** Establish independent deployment, immutable artifact identification, smoke checks,
   observability, permissions, and a rehearsed rollback. Deploy to a non-canonical preview and retain the last known-good
   legacy revision and new-site artifact.
6. **Cut over (management#9).** In a privileged, scheduled operation, record pre-change DNS/Pages settings and TTLs,
   publish the approved head, move the custom domain as required, and run acceptance probes for canonical host, required
   routes, redirects, assets, anchors, and TLS. Do not modify `demo.sniffy.io`.
7. **Follow up, stabilize, and retire (#670-#673).** Monitor through an agreed stabilization window. #670/#671 may
   inventory historical versions and publish the 3.1 archive without gating MVP acceptance or cutover. Only after the
   archive and stabilization gates are met may #672 remove the Maven Asciidoctor module and obsolete legacy publication;
   #673 may enable the blog independently after its prerequisites are stable.

### Rollback contract

Cutover is reversible without rebuilding either site. Before the change, record the exact new artifact and legacy
`sniffy.github.io` commit, export Pages/custom-domain/DNS settings, verify the legacy build is still deployable, and name
the rollback operator. Trigger rollback for a failed canonical-host/TLS check, broken required route or asset, redirect
loop, material anchor loss, or sustained error regression.

Rollback restores the prior Pages/custom-domain/DNS configuration and last known-good legacy revision, then probes `/`,
`/docs/`, `/docs/latest/`, representative topic pages/anchors, legacy landings, and the untouched demo hostname. Preserve
logs and the failed artifact for diagnosis. Content freeze and retry follow incident review; do not patch Maven releases
or change public routes as an emergency workaround. Keep legacy source, generated pages, domain configuration knowledge,
and rollback credentials until stabilization is formally accepted.

Legacy retirement requires: the stabilization window has passed; route/anchor and version manifests are green in
production; rollback is no longer dependent on the legacy repository; historical content required by #670/#671 is
published; monitoring shows no unresolved material legacy traffic; and maintainers approve #672. Retirement removes
obsolete generation/package/copy machinery in its own pull request, not redirects or compatibility tests that protect the
public contract.

## Dependency and sequencing contract

Arrows mean "must complete before." Parallel siblings still require separate authorization from the sequencing owner.

```text
#655 architecture ──> #656 shared tokens
#656 shared tokens ──> #657 light theme
#655 architecture + #656 shared tokens ──> #658 site app

#658 site app ──> #659 site CI
#658 site app ──> #660 snippets
#658 site app + #660 snippets ──> #661 MDX docs ──> #662 compatibility

#657 light theme + #658 site app ──> #663 shell
#663 shell ──> #664 homepage
#663 shell ──> #665 landing system
#665 landing system ──> #666 SQL/N+1
                    ├──> #667 network failures
                    └──> #668 traffic/TLS

#655 architecture ──> #670 historical inventory ──> #671 Sniffy 3.1 archive (also needs #658)

#669 independent deployment
  requires #659, #661, #662, #663, #664, and #665
  └──> management#9 cutover (also requires MVP acceptance)
       └──> stabilization ──> #672 retire Asciidoctor

#673 Git-backed blog requires stable #658, #659, #663, and #669
```

Issue #658 depends only on #655 and #656; #657 is not its prerequisite. #663 is the convergence point and requires both
#657 and #658. Issue #666 is the MVP's required complete landing; #667 and #668 are additional landing content and may
proceed in parallel after #665 when authorized. #670/#671, including the Sniffy 3.1 archive, are follow-up scope: #670
requires #655, and #671 may proceed after both #658 and #670, but neither issue blocks MVP acceptance or cutover. #672
is always post-cutover and post-stabilization.

## Acceptance-to-proof matrix

Each issue must add the listed durable proof. A green overall build does not replace discovery of the named checks.

| Issue | Acceptance obligation | Required proof |
| --- | --- | --- |
| #655 | Architecture reflects both repositories; routes, lifecycle, sequence, and rollback are decided without implementation | Review this document against the evidence list; `git diff --check`; diff contains only this documentation file. |
| #656 | Shared tokens are reusable without application regressions | Package export/consumer tests, token inventory diff, profiler and agent visual checks; no site-only token promoted without a second consumer. |
| #657 | Light theme is opt-in and dark behavior remains compatible | Theme-selection unit tests, contrast/accessibility checks, and dark/light visual baselines for existing consumers. |
| #658 | Docusaurus app exists at the contracted workspace path and required routes are scaffolded | Locked install, typecheck, production build, workspace-boundary check, and route smoke test; `/blog/` remains disabled/reserved. |
| #659 | Site changes receive focused CI without coupling artifact release | Required workflow run showing lint/typecheck/tests/build/link checks; workflow-trigger and permission assertions; no deploy credentials on PRs. |
| #660 | Tagged repository snippets are live, safe, and deterministic | Fixture tests for selection, missing/duplicate/malformed tags, escaping, path boundaries, and stale-content detection. |
| #661 | Current content and media migrate with snippet and navigation parity | Page-by-page source-to-MDX inventory, image check, successful snippet expansion, link checker, and reviewed content parity report. |
| #662 | Legacy pages and generated anchors remain useful | Version-controlled URL/anchor manifest derived from legacy output; automated single-hop redirect, query, fragment, canonical, and 404 tests. |
| #663 | Branded shell consumes shared theme and is accessible/responsive | Component/interaction tests, accessibility audit, viewport visual baselines, and proof site-only tokens remain local. |
| #664 | `/` is the approved homepage | Content/CTA tests, link check, accessibility/performance evidence, and desktop/mobile visual review. |
| #665 | Landing system is reusable and owns `/use-cases/*` | Schema/component tests, representative fixture, invalid-content failure, responsive/accessibility proof, and canonical metadata test. |
| #666-#668 | Each landing is complete, accurate, and mapped from any legacy equivalent | Maintainer content review, CTA/link tests, canonical route check, visual/a11y proof, and explicit old-route mapping. #666 supplies the MVP landing. |
| #669 | Site publishes independently and repeatably | Workflow run tied to an immutable SHA, preview URL, permission/concurrency review, cache/404/redirect smoke tests, artifact-release independence proof, and rollback rehearsal. |
| management#9 | Canonical cutover preserves the contract and demo | Signed checklist with before/after DNS and Pages state, timestamps, deployed SHA, TLS/host/route/anchor probes, demo probe, monitoring, and rollback decision. |
| #670 | Historical versions and provenance are known | Version/path/source/provenance inventory with gaps, checksums or commit references, and normalized-version decisions. |
| #671 | Sniffy 3.1 archive is immutable and navigable | Content checksum/provenance, `/docs/3.1/` crawl, version-selector tests, noindex/canonical decision, and legacy redirect proof. |
| #672 | Legacy pipeline retires only after stabilization | Recorded gate approvals and traffic evidence; reactor/build and release checks after removal; compatibility redirects/tests remain green; rollback runbook updated. |
| #673 | Blog is Git-backed and review-gated | Content schema/date/author tests, draft/future-post behavior, feed and route checks, PR preview proof, and no external CMS/runtime dependency. |

For every later pull request, its description must identify the applicable row, link the resulting reports/manifests, list
checks not run, and state whether it changes dependencies, public routes, deployment permissions, or rollback risk.
