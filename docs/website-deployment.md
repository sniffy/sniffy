# Website deployment, production cutover, and rollback

The `Deploy website` GitHub Actions workflow publishes the reviewed Docusaurus site independently
of Maven Central, GitHub Packages, and Sniffy release events. It uses GitHub's official Pages
configuration, artifact, and deployment actions; it does not copy content to another repository or
call a hosting API directly.

This is also the operator runbook for a future direct `sniffy.io` cutover. It does **not** authorize
that cutover. DNS, Pages custom domains, HTTPS enforcement, environments, deployments, and rollback
dispatches are privileged operations owned by
[management issue #9](https://github.com/sniffy/management/issues/9). `preview.sniffy.io` is not a
current step, proposal, or prerequisite.

The procedures below follow GitHub's primary documentation for
[custom domains][github-custom-domain], [domain verification][github-domain-verification],
[HTTPS][github-pages-https], and [custom Pages workflows][github-pages-workflows]. DNS-provider
instructions are conditional on the authoritative nameservers: use
[DigitalOcean's DNS record UI][digitalocean-dns-records] for the currently delegated zone, or
[Hover's DNS record UI][hover-dns-records] only if a fresh nameserver lookup reports Hover.

## Current topology and observed status

The facts in this section were observed read-only on **2026-07-29**. They are a dated baseline, not
a substitute for repeating the preflight during the real cutover.

| Component                                  | Current owner and observed state                                                                                                                                                                                                                                                                                               |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `sniffy/sniffy`                            | Owns the Docusaurus source, validation, immutable Pages artifact, and `Deploy website` workflow. Observed `origin/develop` was `04d0f8cd11b2e15f4c357ee433ed240a027a63d7`. Repository Pages uses `build_type: workflow`, reports `http://sniffy.io/sniffy/`, has no repository-level `cname`, and does not enforce HTTPS.      |
| `sniffy/sniffy` `github-pages` environment | Allows the workflow deployment and has a custom branch policy restricting deployments to `develop`. The workflow itself also rejects a revision that is not an ancestor of `origin/develop`.                                                                                                                                   |
| `sniffy/sniffy.github.io`                  | Owns the legacy organization Pages site. Its Pages source is `master:/` at `7e9a5d3db7d23913f5d9e8be8e3f9bc2efc834ce`, its `CNAME` file and Pages custom domain are `sniffy.io`, `build_type` is `legacy`, and HTTPS enforcement is off. Preserve this repository and revision through the stabilization window.               |
| GitHub Pages default host                  | `https://sniffy.github.io/sniffy/` redirects to `http://sniffy.io/sniffy/` because the project site inherits the organization site's custom domain. Following redirects ends at `https://sniffy.io/sniffy/`, which returns 404.                                                                                                |
| Authoritative DNS                          | `sniffy.io` delegates to `ns1.digitalocean.com`, `ns2.digitalocean.com`, and `ns3.digitalocean.com`. Hover may be the registrar, but it is not the active DNS provider while this delegation remains.                                                                                                                          |
| Current external hosting                   | Apex `A` records resolve to `162.159.140.98` and `172.66.0.96`; apex `AAAA` records resolve to `2606:4700:7::60` and `2a06:98c1:58::60`. These are the [documented DigitalOcean App Platform ingress addresses][digitalocean-app-platform-ip]. `www.sniffy.io` is a CNAME to `sniffy-static-website-kdvn2.ondigitalocean.app`. |
| Current public site                        | `http://sniffy.io/` redirects to HTTPS. `https://sniffy.io/` and `/docs/` return 200 from the existing DigitalOcean-backed site; `/sniffy/` returns 404.                                                                                                                                                                       |
| Preview status                             | There is **no externally reachable PR or project preview**. `preview.sniffy.io` is NXDOMAIN. Reviewable artifacts are verified inside CI or after download; they are not a public deployment.                                                                                                                                  |

Google Public DNS and Cloudflare DNS returned the same records above. Before using them as rollback
values, export the authoritative zone and confirm the current App Platform domain configuration;
public resolver output alone is not a configuration backup.

### Proven Pages deployment evidence

Successful [`Deploy website` run 30405754824][known-pages-run] provides the baseline for the
new-site artifact:

- event `push`, branch `develop`, exact source
  `ccc62a13b5a7b8e5e6a976b1448f5f772cea78b2`;
- job `Validate and deploy website` passed every validation, packaging, Pages build, artifact
  verification, upload, and deployment step;
- Pages artifact
  `sniffy-pages-ccc62a13b5a7b8e5e6a976b1448f5f772cea78b2`, ID `8706582243`, archive size
  `6,265,344` bytes, archive digest
  `sha256:bdada0427c443a537b8fa033b05652d807991ce54d7b1ca39c46830e7c115f20`;
- `.sniffy-deployment.json` records source
  `ccc62a13b5a7b8e5e6a976b1448f5f772cea78b2`, static-content digest
  `sha256:3e8e09b89d72730a1a017d2c2563c365adff45473f42a2a1de6a274c9a24267c`,
  origin `http://sniffy.io`, and base path `/sniffy`;
- deployment `5648747776` reached success as status `16062675098` at the reported URL
  `http://sniffy.io/sniffy/`. A later deployment made that historical deployment inactive; it did
  not invalidate the recorded artifact.

This proves that the workflow built, checked, uploaded, and handed the exact artifact to Pages. It
does **not** prove public reachability: the reported URL still follows the current custom-domain
and DigitalOcean routing to the 404 described above.

## Activation and operator boundary

Repository maintainers own the protected `github-pages` environment and the repository's Pages
settings. For `sniffy/sniffy`, maintainers have configured the Pages build source as **GitHub
Actions** and restricted the `github-pages` environment to deployments from `develop`. The workflow
does not change those repository settings or the environment's protection rules.

The canonical `https://sniffy.io/` site is published separately from `sniffy/sniffy.github.io`,
whose `CNAME` records `sniffy.io`. This workflow creates the `sniffy/sniffy` project-site
deployment; it does not publish to that repository or change its content, custom domain, DNS,
`CNAME`, or HTTPS settings. The custom-domain cutover remains exclusively authorized by
[management issue #9](https://github.com/sniffy/management/issues/9).

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
Pages. It does not hard-code the project-site URL: `actions/configure-pages` supplies the build
origin and base path, and `actions/deploy-pages` supplies the deployed URL. That URL may reflect an
organization Pages custom domain and path; it is deployment evidence, not a root-domain cutover.

Because the deployment workflow and its `develop`-only environment policy do not permit an
unmerged feature branch to publish, the first production proof occurs only after this workflow is
reviewed and merged to `develop`.

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
`sniffy.io` domain, DNS, TLS, or the separate `sniffy/sniffy.github.io` site is a cutover rollback
owned by management issue #9 and the architecture contract in
[`website-architecture.md`](website-architecture.md#rollback-contract).

## Future production cutover to `sniffy.io`

Run this section only in an explicitly approved management#9 change window. One operator performs
the changes while a second operator records evidence and watches the legacy and new origins. Do not
combine this operation with a release, application change, nameserver migration, or
`preview.sniffy.io` creation.

### 1. Read-only preflight

Complete and attach this evidence before changing anything:

1. Record the change-window start/end, primary operator, observer, rollback operator, and decision
   authority. Confirm access to Sniffy organization Pages settings, both repositories' Pages
   settings, the `github-pages` environment, the authoritative DNS account, and DigitalOcean App
   Platform.
2. Freeze unrelated site deployments for the window. Record the latest `origin/develop`, the exact
   approved new-site SHA, its successful `Check Pull Request` run, and its downloaded review
   artifact. Name a separate previously successful site SHA as the content-rollback candidate. Both
   it and the approved SHA must be rebuilt and verified with base path `/` after the Pages claim
   transfers; the historical `/sniffy` artifact above is evidence, but it is not a valid root
   rollback.
3. Export or screenshot, without secrets:
   - `sniffy/sniffy` Pages build type, custom domain, HTTPS state, and environment rules;
   - `sniffy/sniffy.github.io` Pages source, custom domain, HTTPS state, and current `master` SHA;
   - every `sniffy.io` DNS record and TTL, including `A`, `AAAA`, `CNAME`, `TXT`, `CAA`, `MX`, and
     any wildcard;
   - the DigitalOcean App Platform domain mapping and its current healthy deployment.
4. Query authoritative nameservers and at least two public resolvers:

   ```bash
   dig +short NS sniffy.io
   dig @ns1.digitalocean.com sniffy.io A +noall +answer
   dig @ns1.digitalocean.com sniffy.io AAAA +noall +answer
   dig @1.1.1.1 sniffy.io A +noall +answer
   dig @8.8.8.8 sniffy.io A +noall +answer
   dig www.sniffy.io CNAME +noall +answer
   dig _github-pages-challenge-sniffy.sniffy.io TXT +noall +answer
   dig sniffy.io CAA +noall +answer
   ```

   If the `NS` response is not the provider expected by this runbook, stop and use that
   authoritative provider's documented UI. Do not change nameservers as part of this cutover.

5. Capture status, redirect chain, certificate issuer/expiry, key response headers, and a checksum
   or screenshot for the legacy homepage and required routes:

   ```bash
   curl --fail --show-error --silent --location --output /dev/null \
     --write-out '%{url_effective} %{http_code}\n' https://sniffy.io/
   curl --fail --show-error --silent --location --output /dev/null \
     --write-out '%{url_effective} %{http_code}\n' https://sniffy.io/docs/
   curl --fail --show-error --silent --location --output /dev/null \
     --write-out '%{url_effective} %{http_code}\n' https://demo.sniffy.io/
   openssl s_client -connect sniffy.io:443 -servername sniffy.io </dev/null 2>/dev/null \
     | openssl x509 -noout -issuer -subject -dates
   ```

6. In **Organization settings → Pages**, verify `sniffy.io` for the `sniffy` organization before
   pointing any DNS record at GitHub Pages. Add the exact GitHub-provided TXT name/value through the
   authoritative DNS provider, wait until authoritative and public resolvers return it, click
   **Verify**, and retain the TXT record. GitHub documents that verification protects the apex and
   its immediate subdomains from takeover.
7. Confirm there is no wildcard DNS record. If one exists, stop and remove it only under separate,
   explicit authority after identifying every consumer. GitHub warns that wildcard records preserve
   takeover risk even when the apex is verified.
8. Check `CAA` records. They must allow the certificate authority GitHub currently requires; do not
   guess or delete restrictive records. Resolve any mismatch before the window.
9. Lower only the records that will change to the approved cutover TTL at least one old-TTL period
   before the window. Record both old and new TTLs. A low TTL reduces future caching; it does not
   flush already cached answers.
10. Rehearse the content artifact locally over HTTP. Exercise the routes and searches in
    [Post-cutover verification](#post-cutover-verification), inspect browser console/network errors,
    and confirm `.sniffy-deployment.json` matches the approved SHA and content digest.

Abort preflight if access, domain verification, known-good artifacts, legacy restoration details,
DNS export, CAA compatibility, or an independent observer is missing.

### 2. Select the authoritative DNS UI

As observed on 2026-07-29, the zone is delegated to DigitalOcean. In that state, use
**DigitalOcean Control Panel → Networking → Domains → `sniffy.io`**. Open each record's menu to edit
it, or choose **Create a record** for a missing value. DigitalOcean's primary documentation explains
the fields and TTL behavior in [How to create, edit, and delete DNS records][digitalocean-dns-records].

Do **not** edit Hover DNS while the nameservers are delegated elsewhere. Hover's
[nameserver documentation][hover-nameservers] states that its DNS record UI controls a zone only
when the domain uses `ns1.hover.com` and `ns2.hover.com`. If a fresh lookup does return those two
nameservers:

1. Sign in to Hover with MFA, open the domain, select **DNS**, and choose **Add a record** or edit an
   existing record.
2. Use `@` for the apex hostname, `www` for the `www` subdomain, and
   `_github-pages-challenge-sniffy` for the GitHub-provided verification TXT record.
3. Save and re-query the authoritative Hover nameservers before proceeding.

Do not switch from DigitalOcean nameservers to Hover merely to follow these instructions. A
nameserver move is a separate DNS migration and must recreate every record before delegation.

### 3. Prepare the GitHub Pages claim while legacy DNS still serves

GitHub recommends verifying the domain first, assigning the custom domain to the target Pages site
second, and only then pointing DNS at GitHub. The current legacy Pages site already claims
`sniffy.io`, so transfer that claim in a short, observed interval while DNS still points to
DigitalOcean:

1. Reconfirm the organization-level verified-domain badge and retained TXT record.
2. In `sniffy/sniffy.github.io` **Settings → Pages**, record the current values, then remove
   `sniffy.io` from **Custom domain**. Do not change its repository, `master` branch, or content.
3. Immediately open `sniffy/sniffy` **Settings → Pages**:
   - keep **Build and deployment → Source** set to **GitHub Actions**;
   - set **Custom domain** to `sniffy.io` and save;
   - leave **Enforce HTTPS** off.
4. Confirm the target repository now reports `cname: sniffy.io` and the source remains a workflow.
   If the target claim fails, restore `sniffy.io` to the legacy repository before continuing and
   abort the window. DNS still points at DigitalOcean, so public legacy traffic should remain
   unchanged.
5. Manually dispatch the current `Deploy website` workflow with the named rollback-candidate SHA.
   The custom-domain change must cause `actions/configure-pages` to produce origin `sniffy.io` and
   base path `/`. Require every step to pass, download the artifact, inspect its manifest, serve its
   exact bytes locally, and record this run as the known-good root-base-path content rollback.
6. Dispatch the workflow again with the approved new-site SHA. Require every validation and
   deployment step to pass. Download the new artifact and inspect `.sniffy-deployment.json`; its
   commit must be the approved SHA, its base path must be `/`, and its content digest must match the
   run summary. Serve the downloaded bytes locally and repeat the route/search/browser checks. A
   green run with `/sniffy` in either manifest is an abort, not a production candidate.

At this point GitHub owns the verified Pages custom-domain claim and has a root-base-path artifact,
but public traffic still reaches DigitalOcean. Certificate issuance may remain pending until DNS
points to Pages; that is expected.

### 4. Change DNS to GitHub Pages

Use the values from GitHub's current [apex-domain table][github-custom-domain], not copied values
from an old incident. As of this runbook revision, the intended record set is:

| Type    | Hostname | Value                 |
| ------- | -------- | --------------------- |
| `A`     | `@`      | `185.199.108.153`     |
| `A`     | `@`      | `185.199.109.153`     |
| `A`     | `@`      | `185.199.110.153`     |
| `A`     | `@`      | `185.199.111.153`     |
| `AAAA`  | `@`      | `2606:50c0:8000::153` |
| `AAAA`  | `@`      | `2606:50c0:8001::153` |
| `AAAA`  | `@`      | `2606:50c0:8002::153` |
| `AAAA`  | `@`      | `2606:50c0:8003::153` |
| `CNAME` | `www`    | `sniffy.github.io`    |

Replace the current DigitalOcean App Platform apex `A`/`AAAA` values and `www` CNAME; do not touch
MX, TXT, CAA, `demo.sniffy.io`, or unrelated hostnames. The `www` target must not include a
protocol or `/sniffy` path. Do not add wildcard records.

Record each change, provider audit/event ID if available, and authoritative answer. Query all three
authoritative DigitalOcean nameservers plus public resolvers until the complete set is internally
consistent. Mixed old/new answers are an expected propagation state, not proof of completion.

### 5. Wait for TLS, then enforce HTTPS

After DNS points to Pages:

1. Watch the target repository's Pages settings and DNS check. Wait for the custom domain to be
   recognized and the certificate to be issued. GitHub notes that DNS propagation may take up to
   24 hours and that HTTPS availability can lag custom-domain configuration.
2. During propagation, some resolvers may still reach the legacy DigitalOcean site while others
   reach Pages. HTTP may be available before the Pages certificate is ready. Do not call the
   cutover complete, remove the legacy site, or bypass a certificate warning.
3. From multiple networks/resolvers, confirm the certificate covers `sniffy.io` (and `www` if
   configured), is valid, and serves the approved artifact over HTTPS.
4. Only after clean HTTPS probes and GitHub's certificate state is ready, select **Enforce HTTPS**
   in `sniffy/sniffy` **Settings → Pages**.
5. Confirm `http://sniffy.io/<path>` redirects to the same
   `https://sniffy.io/<path>` without losing path, query, or fragment where the platform permits.

If the certificate is unavailable beyond the approved window, DNS checks are inconsistent, or any
resolver serves an unexpected origin, execute
[Cutover rollback](#cutover-rollback-dns-custom-domain-and-https). Do not disable browser
verification or accept a certificate error as a temporary success.

## Deployment operations

### Automatic deployment

A push to `develop` starts `Deploy website` only when a configured site, theme, frontend workspace,
snippet input, product-version, or workflow path changes. A documentation-only change to this
runbook does not deploy the site. The triggered run still verifies the exact pushed SHA is on
`develop`.

### Manual full-SHA deployment or content rollback

1. Open **Actions → Deploy website → Run workflow**.
2. Choose `develop` as the workflow ref and enter the approved full 40-character `revision`.
3. Confirm the revision is an ancestor of current `origin/develop`; the workflow rejects anything
   else.
4. Wait for any required `github-pages` environment approval.
5. If a newer run starts, the shared `sniffy-website-production` concurrency group cancels the
   older run. Never use a cancelled or superseded run as evidence.

For content rollback, select a previously successful, known-good full SHA built for the **current**
origin/base-path configuration. Dispatching it rebuilds and republishes site content without
changing DNS, custom-domain, or HTTPS settings. Do not use a pre-cutover `/sniffy` artifact after
the root-domain cutover.

### Decide whether a run is genuinely live

`conclusion: success` is necessary but insufficient. Record all of the following:

- requested revision, run `head_sha`, checked-out SHA, and `origin/develop` ancestry agree;
- every validation, root-base-path artifact, upload, and deployment step passed at that SHA;
- artifact name/ID, archive digest, manifest content digest, origin, and base path agree;
- the deployment status is `success` and its environment URL is the expected canonical origin;
- authoritative/public DNS, certificate, and HTTP redirect checks are correct;
- the exact live `.sniffy-deployment.json` contains the expected SHA/digest;
- the browser and route probes below pass from more than one network.

If the workflow is green but the environment URL is unreachable, routed elsewhere, serving a
different manifest, or failing required behavior, classify it as not live.

## Post-cutover verification

Use no-cache requests and a real browser. Keep the downloaded exact artifact available for
byte/behavior comparison.

### DNS, TLS, and identity

- Authoritative and at least two public resolvers return only the intended GitHub Pages `A`/`AAAA`
  values; `www` resolves by CNAME to `sniffy.github.io`.
- The organization verification TXT record remains present.
- `sniffy.io` and `www.sniffy.io` present valid, unexpired certificates and HTTP redirects to HTTPS.
- `https://sniffy.github.io/sniffy/` and `https://sniffy.io/` do not form a redirect loop.
- `https://demo.sniffy.io/` remains healthy and unchanged.
- `.sniffy-deployment.json` returns 200 and its commit/content digest equal the approved run.

### Routes and assets

Require the expected status, heading, canonical URL, navigation, and same-origin assets for:

- `/`;
- `/docs/`;
- `/docs/3.1/` and a representative archived topic;
- `/use-cases/database-query-testing/`;
- `/use-cases/sql-profiling/`;
- `/use-cases/network-fault-testing/`;
- `/use-cases/traffic-capture/`;
- `/docs/latest/`, which must redirect to the corresponding current `/docs/` route;
- `/img/brand/sniffy-social.svg` and representative documentation images;
- a deliberate missing route, which must return the branded 404 without a redirect loop.

In desktop and mobile browser sizes:

- search current docs and confirm results/navigations remain under `/docs/`;
- search the 3.1 archive and confirm results remain under `/docs/3.1/`;
- inspect console errors, failed requests, mixed content, canonical/OG metadata, focus/keyboard
  behavior, and layout;
- compare critical pages with the downloaded exact artifact.

Do not retire the legacy site after a single successful probe. Monitor DNS consistency, Pages
deployments, certificate state, HTTP error rate, and required journeys for the management#9
stabilization window.

### Abort criteria

Stop or roll back for any of the following:

- domain verification/custom-domain ownership is missing or unexpected;
- DNS answers are incomplete, wildcarded, or point to an unknown service;
- certificate issuance misses the approved window, a certificate is invalid, or mixed content
  blocks functionality;
- the live manifest SHA/digest differs from the approved run;
- `/`, current docs, 3.1 docs, any required use-case page, search, assets, redirect, or branded 404
  fails;
- a redirect loop, material anchor/canonical regression, elevated error rate, or unexplained
  environment/deployment state appears;
- rollback access or the healthy legacy target is lost.

## Cutover rollback: DNS, custom domain, and HTTPS

This restores the previous hosting topology; it is different from content rollback.

1. Declare rollback, stop new site deployments, and preserve the failed run, artifact, logs,
   browser evidence, and current DNS/Pages/TLS state.
2. While `sniffy/sniffy` still claims the verified custom domain, restore the exported legacy DNS
   records at the authoritative provider:
   - apex `A`: `162.159.140.98`, `172.66.0.96`;
   - apex `AAAA`: `2606:4700:7::60`, `2a06:98c1:58::60`;
   - `www` CNAME: `sniffy-static-website-kdvn2.ondigitalocean.app`;
   - original TTLs.
3. Query every authoritative server and at least two public resolvers. Accept that caches may show
   mixed answers for up to their previous TTL. Wait at least one full cutover-TTL interval after the
   authoritative restoration and until the monitored public resolvers all return DigitalOcean.
   Verify the DigitalOcean App Platform domain remains attached and its legacy deployment is
   healthy.
4. Require the legacy `https://sniffy.io/`, `/docs/`, representative routes, assets, redirects,
   and certificate to pass from the rollback observers. If the legacy target is not healthy, do
   not remove the new Pages claim; escalate to the named incident owner.
5. After DNS is observably returning to DigitalOcean, turn off **Enforce HTTPS** on
   `sniffy/sniffy` only if needed to restore the recorded pre-cutover state. Remove `sniffy.io` from
   that repository's Pages custom domain.
6. Restore `sniffy.io` as the `sniffy/sniffy.github.io` Pages custom domain and its recorded HTTPS
   state while leaving its `master` content unchanged. The organization verification TXT record
   stays in DNS.
7. Repeat the legacy route/TLS probes until the rollback acceptance window passes. Record
   authoritative/public DNS answers, Pages settings, final legacy SHA, timestamps, and incident
   owner.

The safety property is deliberate: DNS is moved away from GitHub while the verified target claim
still exists, and only then is the claim returned to the legacy Pages repository. Never leave DNS
pointing at GitHub Pages after removing an unverified custom-domain claim; that creates a dangling
domain/takeover risk.

Keep `sniffy/sniffy.github.io`, its deployable legacy source, the DigitalOcean App Platform
configuration, the DNS export, and rollback credentials until management#9 formally ends the
stabilization window.

## Cutover evidence template

Copy this checklist into the management#9 change record and replace every placeholder:

```text
Change window
- Start/end UTC:
- Authority / management#9 decision:
- Operator / observer / rollback operator:
- Stabilization deadline:

Approved revisions
- origin/develop:
- New-site SHA:
- Check Pull Request run:
- Root-base-path Deploy website run:
- Known-good root-base-path rollback SHA/run:
- Legacy sniffy.github.io master SHA:

Pre-change state
- Authoritative nameservers:
- Full DNS export location and checksum:
- Apex A / AAAA / TTL:
- www CNAME / TTL:
- Verification TXT:
- CAA / wildcard findings:
- sniffy/sniffy Pages source, custom domain, HTTPS:
- sniffy/sniffy.github.io Pages source, custom domain, HTTPS:
- github-pages environment policy:
- DigitalOcean App Platform domain/deployment:

Artifact and deployment
- Artifact name / ID / bytes / archive digest:
- Manifest commit / content digest / origin / base path:
- Deployment ID / status ID / environment URL:
- All validation steps passed:

Cutover
- Organization domain verified at:
- Custom domain removed from legacy repo at:
- Custom domain attached to target repo at:
- Root artifact deployed at:
- DNS record changes and provider audit IDs:
- Authoritative/public resolver convergence:
- Certificate issuer / validity:
- Enforce HTTPS enabled at:

Acceptance
- Live manifest:
- Route and asset probe results:
- Current and archive search:
- Desktop/mobile browser, console, network:
- Redirects, canonical URLs, branded 404:
- Monitoring/error-rate result:
- Final accept/rollback decision and authority:

Rollback, if used
- Trigger and timestamp:
- Preserved failed evidence:
- DNS restored:
- Legacy App Platform verified:
- Target custom domain/HTTPS removed:
- Legacy Pages custom domain/HTTPS restored:
- Resolver/TLS/route stabilization result:
- Incident owner and follow-up:
```

[digitalocean-dns-records]: https://docs.digitalocean.com/products/networking/dns/how-to/manage-records/
[digitalocean-app-platform-ip]: https://docs.digitalocean.com/products/app-platform/how-to/add-ip-address/
[github-custom-domain]: https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site
[github-domain-verification]: https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/verifying-your-custom-domain-for-github-pages
[github-pages-https]: https://docs.github.com/en/pages/getting-started-with-github-pages/securing-your-github-pages-site-with-https
[github-pages-workflows]: https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages
[hover-dns-records]: https://support.hover.com/support/solutions/articles/201000064728-managing-dns-records
[hover-nameservers]: https://support.hover.com/support/solutions/articles/201000064742-changing-your-domain-nameservers
[known-pages-run]: https://github.com/sniffy/sniffy/actions/runs/30405754824
