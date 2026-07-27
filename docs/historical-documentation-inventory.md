# Historical documentation inventory

Status: research report only. This report does not publish an archive, enable
Docusaurus versioning, or change a route. It records the repository evidence used
to make those later decisions.

## Decision summary

Publish exactly two maintained documentation lines when versioning is implemented:

1. **Next (4.x development)** from the current `develop` documentation.
2. **3.1 archive** frozen from `v3.1.14`.

Do **not** publish 2.x or 1.x. Defer 3.0 rather than including it in the initial
selector. The 3.1 archive is recoverable, substantially richer than 3.0, and is the
only tagged historical line with a self-contained documentation module. Earlier
lines are single README files, contain obsolete coordinates and infrastructure,
and would add several selector entries while providing little help beyond what a
release-tag link can provide.

The recommendation is about historical fidelity, not supported-product status.
Every archive must be visibly labelled unsupported and must preserve old dependency
examples rather than silently substituting the current version.

## Scope, refs, and reproducible investigation

The authorization baseline and current `develop` at inspection time were both
`ade8e64a20d38ab104014015610a970daeb6d64c`. All 51 release tags advertised by the
repository were fetched. Annotated tags are deliberately resolved with `^{commit}`;
the commit, rather than the tag-object SHA, is reported below.

Commands were run from the repository root on 2026-07-27:

```text
git remote add origin https://github.com/sniffy/sniffy.git
git fetch origin develop --tags --force
git rev-parse origin/develop
git tag --sort=version:refname \
  --format='%(refname:short) %(objectname) %(*objectname) %(creatordate:short)'
git log --all --date=short --format='%h %ad %d %s' -- \
  README.md sniffy-documentation sniffy-ui/apps/site/docs
git ls-tree -r --name-only <tag> -- README.md sniffy-documentation
git diff --stat <older-tag> <newer-tag> -- README.md sniffy-documentation
git grep -n -E '(include::|image::|https?://|<version>[0-9])' <tag> -- \
  README.md sniffy-documentation/src/main/asciidoc sniffy-documentation/pom.xml
```

The inspected release-tag range is complete from `1.0` through `v3.1.14`:

```text
1.0 1.1 1.2 1.3 1.4
2.0 2.1 2.2 2.2.1 2.2.2 2.2.3
2.3 2.3.1 2.3.2 2.3.3 2.3.4 2.3.5
3.0.0 3.0.1 3.0.2 3.0.3 3.0.5 3.0.6 3.0.7
3.1.0-RC1 3.1.0-RC2 3.1.0-RC3 3.1.0-RC3.1 3.1.0-RC4
3.1.0-RC5 3.1.0-RC6 3.1.0-RC7 3.1.0-RC8 3.1.0-RC9
3.1.0-RC10 3.1.0-RC11
3.1.0 3.1.1 3.1.2 3.1.3 3.1.4 3.1.5
v3.1.6 v3.1.7 v3.1.8 v3.1.9 v3.1.10 v3.1.11 v3.1.12
v3.1.13 v3.1.14
```

There is no release tag after `v3.1.14`. Repository history has a 3.2 snapshot
commit and now a 4.0 snapshot, but neither is evidence of an independently released
documentation line. They are therefore part of **Next**, not invented archives.

### Fingerprint method and patch evidence

For each tag, a deterministic fingerprint was made from the `git ls-tree -r`
records (path, mode, and blob object ID) for `README.md` plus
`sniffy-documentation`, then piped to `git hash-object --stdin`. This detects every
tracked content/path change without copying old trees into this branch. The command
is:

```bash
for t in $(git tag --sort=version:refname); do
  c=$(git rev-parse "$t^{commit}")
  { git ls-tree -r "$t" -- README.md
    git ls-tree -r "$t" -- sniffy-documentation; } |
    git hash-object --stdin
done
```

Patch releases do not all have identical fingerprints: version strings, badges,
release notes, and sometimes real documentation were changed in patches. Lines are
grouped by user-facing architecture and concepts, not by falsely claiming byte
identity. Exact identity was observed only for `2.3.2`/`2.3.3`, `3.0.2`/`3.0.3`,
and several 3.1 release candidates. Salient results follow:

| Tag       | Resolved commit                            | Files | Fingerprint                                |
| --------- | ------------------------------------------ | ----: | ------------------------------------------ |
| `1.4`     | `d4e3709287365dfd4c51a6c12e5383db2e5bbf4f` |     1 | `0c2a6fdf6ef37b8cecd2496cd001e6c887ae3399` |
| `2.0`     | `ae1c23de3933ba5b1dd7b357d44d5b4968f1966c` |     1 | `c22b39fedd8a2f399de94446ea534cd704716b91` |
| `2.1`     | `cc705bd7696ccc16ae0c48886f07a511fe0a3da4` |     1 | `04bc0add151e29c645fbb56e96a28caae11197b0` |
| `2.2.3`   | `facb9cfcf0da00ef2cbf25d977e8800961c6f828` |     1 | `9e999fedffde9f4fcce4545dcc61823121a1bed3` |
| `2.3.5`   | `b78f4da62138bc1b7c41cdc8b2acb500fb26b35a` |     1 | `d7ab77e0be3538fdeb3f1cebbf0ecdf687069faf` |
| `3.0.7`   | `04f873f0321fc74138aa3b5eb844e9b41572c970` |     1 | `dfb4c50c32235539b74cf289d801d45e72ecb3c7` |
| `3.1.0`   | `b17b31d689fffa1df05c0c82175a18bf583a1f99` |    17 | `69470e99a2dd799fd9da17d953544ae3b4fb559d` |
| `3.1.5`   | `07eac328d3af12eb8dc33b8a519b3af5175c0512` |    20 | `f953ac5ed176d22f09e388cfb8cac480fae43241` |
| `v3.1.9`  | `9df6ac827d9e748a235f09a595ce8b218b1d494c` |    22 | `017c7e52106ee476a257e29314db3672c8d4d552` |
| `v3.1.12` | `f2f840533096a147f563d54ecc34380361d32c69` |    23 | `a50012a1272425c39a44d96f1aada2aed3a69200` |
| `v3.1.13` | `8f64a885f57ab66df8a688535dae323051476030` |    23 | `5142a1ede7b0c4cec25d5475625ce2a602e03a18` |
| `v3.1.14` | `55f744f6a32dd68ee8d5d31c54796e9b5d75aad1` |    23 | `cbc8261b902fcc368cbed13b9b348287b0ad066d` |

Representative diff evidence shows why these are lines rather than patches:

| Comparison         | Documentation diff summary              | Interpretation                            |
| ------------------ | --------------------------------------- | ----------------------------------------- |
| `1.4..2.0`         | README: 71 insertions, 68 deletions     | coordinate/API generation change          |
| `2.0..2.1`         | README: 29 insertions, 17 deletions     | distinct 2.1 guidance                     |
| `2.1..2.2.3`       | README: 12 insertions, 8 deletions      | modest minor-line update                  |
| `2.2.3..2.3.5`     | README: 62 insertions, 6 deletions      | profiler/demo material added              |
| `2.3.5..3.0.7`     | README: 47 insertions, 43 deletions     | Sniffy rename and `io.sniffy` coordinates |
| `3.0.7..3.1.0`     | 17 files; 545 insertions, 179 deletions | dedicated AsciiDoc manual begins          |
| `3.1.0..3.1.5`     | 14 files; 213 insertions, 25 deletions  | additive evolution within 3.1             |
| `3.1.5..v3.1.6`    | 5 files; 79 insertions, 12 deletions    | shared-connection page added              |
| `v3.1.6..v3.1.9`   | 6 files; 42 insertions, 7 deletions     | Kotest/configuration added                |
| `v3.1.9..v3.1.12`  | 5 files; 98 insertions, 2 deletions     | traffic capture added                     |
| `v3.1.12..v3.1.13` | 6 files; 93 insertions, 8 deletions     | TLS/configuration expansion               |
| `v3.1.13..v3.1.14` | POM version only: 1/1                   | latest patch is content-equivalent        |

Thus `v3.1.14` is the best 3.1 representative: it is the final release tag and its
manual content is identical to 3.1.13 except for the parent version, while containing
all additions accumulated across 3.1. Using 3.1.0 would omit later 3.1 features.
Latest-patch representatives are likewise used for earlier lines because their
examples name the final patch and include the most complete recoverable README.

## Candidate inventories

### Next (current 4.x development) — publish

**Authority.** `develop` at
`ade8e64a20d38ab104014015610a970daeb6d64c`; unlike the archives, this line moves
with reviewed documentation changes. It represents Next because the parent is a
4.0 snapshot and no 4.x release tag exists.

**Sources and tooling.** Twenty-one MDX files live under
`sniffy-ui/apps/site/docs`, with `sidebars.ts`, Docusaurus 3.10.2, React 19, and the
local-search plugin in the private npm workspace. It builds through root workspace
scripts, not the historical Maven Asciidoctor module.

**Pages/assets.** Overview; installation; configuration and NIO; Spring,
datasource, filter, and container setup; API, JUnit/Jupiter, Kotest,
shared-connection, Spock, Spring, and TestNG testing; network fault emulation and
traffic capture; migration to 3.1 and 4; plus maintainer-only migration/snippet
mapping pages. Static documentation assets are `agent-ui.png`, `demo.gif`, and
`network-connections.png`; the site also owns branding, homepage, use-case, and
favicon assets.

**Includes and risks.** Seventeen `<SourceSnippet>` uses currently resolve tagged
regions from Java, Kotlin, Groovy, and XML files in the same `develop` checkout.
That is correct only for Next. It is unsafe for archives. The current line is fully
recoverable from the branch, but as moving development documentation it must not be
presented as a released version until a corresponding release exists. Historical
dependency examples already use Docusaurus substitutions for the current project
version and must not leak into an archive.

### 3.1 — publish from `v3.1.14`

**Authority.** Tag `v3.1.14`, commit
`55f744f6a32dd68ee8d5d31c54796e9b5d75aad1`. It is the final recoverable 3.1 tag;
the only change from 3.1.13 in the documentation scope is the POM version.

**Sources and tooling.** `sniffy-documentation/src/main/asciidoc/index.adoc` is the
root. The root plus sixteen included `.adoc` pages, three images, `pom.xml`, and
`src/assembly/bin.xml` make 23 tracked files. Maven's
`asciidoctor-maven-plugin` generates HTML5 with CodeRay, a left TOC, and the
`sniffy-version=${project.version}` attribute; `maven-assembly-plugin` packages it.

**Pages/assets.** Overview and installation; Spring/datasource/filter/container
setup; configuration; API/shared-connection/JUnit/Kotest/Spring/TestNG/Spock
testing; network fault emulation; traffic capture; and migration to 3.1. Assets are
`demo.gif`, `network-connections.png`, and `agent-ui.png`. All are present in the
tag, although the README embeds the demo over HTTP rather than using the local copy.

**Includes/snippets.** The root includes all section files. Twelve external source
files are also included: core API and capture-traffic tests; JUnit overview,
connectivity, and usage tests; Kotest, Spring (two examples), common
shared-connection, TestNG, and Spock tests; and the WildFly `module.xml`. Inspection
confirmed every referenced file exists at `v3.1.14`. Tagged regions include
`CaptureTrafficOverview`, `JUnitOverview`, `DisableSocketsOverview`, three core API
regions, and `sharedConnectionDataSourceUsage`; several includes intentionally use
whole files. Region pairing was not independently rendered with the old toolchain,
so conversion must validate every tag and whole-file boundary.

**Historical values to preserve.** Maven/Gradle coordinates and version 3.1.14,
the javax-era servlet/Spring integrations, JUnit 4 rule/runner, TestNG and Spock
APIs, Kotest integration, WildFly module content, header/property names, and the
3.0-to-3.1 migration table are historical facts. Do not rewrite them to 4.x/Jakarta
or current testing APIs; add an archive warning instead.

**Missing/ambiguous and migration risks.** No tracked include or image is missing.
The tag does not contain the generated deployed site, so historical redirect and
anchor behavior cannot be proven from this repository alone. External links include
the old `master` branch, Maven badge service, retired Travis/Codacy/Gitter/Waffle or
Sonar-era services in earlier patches, `groovy.codehaus.org`, old Spring reference
anchors, an HTTP demo image, and `demo.sniffy.io`; these require a link audit and
must be retained as labelled historical references, replaced with an archived
target, or removed if decorative. `releases/latest` is especially unsafe because it
does not mean 3.1.14. **Recommendation: publish**, after deterministic conversion,
snippet freezing, archive banners, and link classification.

### 3.0 — defer (`3.0.7`)

**Authority.** Tag `3.0.7`, commit
`04f873f0321fc74138aa3b5eb844e9b41572c970`, the last 3.0 tag and most complete 3.0
README. Patch fingerprints vary, although 3.0.2 and 3.0.3 are identical.

**Inventory/tooling.** The only documentation source is the 209-line Markdown
`README.md`; there is no documentation module, local documentation asset, include,
or tagged snippet dependency. It covers purpose, profiler/demo, Maven dependency and
downloads, datasource/filter configuration, testing API, framework wiki links,
build/contribution, and license.

**Historical values/risks.** Preserve `io.sniffy:sniffy:3.0.7`, 3.0 API/configuration
examples, and the separate `sniffy-ui` statement. Bintray downloads, Travis,
Coveralls, old Maven badges, HTTP `sniffy.io`/demo URLs, old wiki pages, `master`
links, and live-demo assumptions are stale or ambiguous. The remotely embedded
demo is not recoverable from the tag. **Recommendation: defer**: it is recoverable
as text, but 3.1's migration guide plus richer manual offers more value. Reassess
only if support traffic demonstrates a 3.0 need.

### 2.3 — do not publish (`2.3.5`)

**Authority.** Tag `2.3.5`, commit
`b78f4da62138bc1b7c41cdc8b2acb500fb26b35a`, the last 2.3 tag. It includes material
profiler/demo additions over 2.2; 2.3.2 and 2.3.3 are byte-identical in scope.

**Inventory/tooling.** One Markdown README, with no build tooling, local assets,
includes, or snippet tags. It covers JDBC query counting, profiler UI/demo,
`com.github.bedrin:jdbc-sniffer:2.3.5`, downloads, datasource/filter setup, core API,
framework wiki links, and the separately maintained UI.

**Gaps/risks.** The demo GIF is remote and absent from the tag. The old
`bedrin/jdbc-sniffer` and `jdbc-sniffer-ui` identities, Heroku demo, Bintray,
Gitter, Travis, Coveralls, badge service, wiki, and `master` links are historical or
stale. **Recommendation: do not publish**: the rename and obsolete distribution
surface make an archive more confusing than a direct release-tag README.

### 2.2 — do not publish (`2.2.3`)

**Authority.** Tag `2.2.3`, commit
`facb9cfcf0da00ef2cbf25d977e8800961c6f828`, the last 2.2 patch. Its README differs
from the earlier patches primarily through patch versions/download details.

**Inventory/tooling.** One Markdown README; purpose, installation/download,
datasource/filter setup, core API, and framework wiki links. No local assets,
includes, snippet regions, or docs build exists.

**Historical values/risks.** Preserve `com.github.bedrin:jdbc-sniffer:2.2.3` and its
API examples if cited. Bintray, `bedrin` GitHub paths, Travis, Coveralls, Gitter,
Maven badges, wiki, and `master` links are stale migration liabilities.
**Recommendation: do not publish**; 2.3 supersedes it even for historical research.

### 2.1 — do not publish (`2.1`)

**Authority.** Tag `2.1`, commit
`cc705bd7696ccc16ae0c48886f07a511fe0a3da4`; there are no 2.1 patch tags.

**Inventory/tooling.** A single Markdown README covering motivation, Maven/download
installation, datasource/filter integration, API usage, and framework wiki links;
no local assets, includes, snippets, or build. Preserve the 2.1 coordinates and API
only as historical examples. The same retired badges/Bintray/wiki/`bedrin`/`master`
surfaces apply. **Recommendation: do not publish** because later 2.x is more useful
and none has a structured manual.

### 2.0 — do not publish (`2.0`)

**Authority.** Tag `2.0`, commit
`ae1c23de3933ba5b1dd7b357d44d5b4968f1966c`; there are no patch tags.

**Inventory/tooling.** One Markdown README with Maven/download setup,
datasource/filter wiring, and query-count assertions. No assets/includes/build.
Preserve `com.github.bedrin:jdbc-sniffer:2.0` if quoted. Old repository, release,
badge, and `master` links are migration risks. **Recommendation: do not publish**;
its small surface is recoverable directly from the tag.

### 1.x — do not publish (`1.4`)

**Authority.** Tag `1.4`, commit
`d4e3709287365dfd4c51a6c12e5383db2e5bbf4f`, is the last 1.x tag and the most
complete of the five materially changing README snapshots (`1.0`–`1.4`).

**Inventory/tooling.** One Markdown README; Maven/download setup, datasource proxy,
and early query-counting API. No docs build, assets, includes, or snippet tags.
Preserve `com.github.bedrin:jdbc-sniffer:1.4` only as history. Notably, its three
1.4-labelled release links point to `jdbc-sniffer-1.1*.jar`, an internally broken
source example that must not be repaired without clearly recording the correction.
Old `bedrin` repository paths, Travis/Coveralls/Maven badges, and `master` references
add further risk. **Recommendation: do not publish** because usefulness is very low
and ambiguity is high.

## Proposed route and selector matrix

This is a specification for a later implementation, not a route change in this task.

| Selector label       | Canonical route | Source ownership                          | Visibility                                                                                                |
| -------------------- | --------------- | ----------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `Next (4.x)`         | `/docs/next/`   | moving `develop` MDX                      | selector default until 4.0 is released; development banner                                                |
| `3.1 (archived)`     | `/docs/3.1/`    | immutable snapshot derived from `v3.1.14` | selector entry; unsupported/archive banner                                                                |
| `Latest` alias       | `/docs/latest/` | redirect/alias policy, not copied content | while no newer release exists, resolve deliberately to 3.1; after a 4.x release, point to its frozen line |
| unversioned `/docs/` | `/docs/`        | landing/version chooser                   | describe Next versus supported/released archives; do not silently alias                                   |
| `3.0`, `2.x`, `1.x`  | none            | release-tag README links only             | absent from selector                                                                                      |

The `Latest` rule avoids calling unreleased 4.x documentation stable. A future 4.x
release should add one frozen `4.x` line and may retire or retain Next independently.
Patch versions must not become selector entries.

## Frozen-snippet strategy

### Options considered

1. **Resolve archive MDX against the checkout being built.** Rejected: the site is
   built from `develop`, so paths and tagged regions drift and can show current code.
2. **Fetch/checkout a tag during every site build.** Rejected: it adds network and
   Git availability to otherwise deterministic builds, complicates preview and
   supply-chain review, and still permits a mutable ref mistake.
3. **Link every example to a tag on GitHub.** Useful as provenance, but rejected as
   the renderer: pages become network-dependent and raw links do not preserve the
   intended excerpt or formatting.
4. **Materialize the rendered excerpts beside the archived pages.** Selected: small,
   reviewable, deterministic, and independent of current production sources.

### Selected ownership and update rules

The future archive conversion should own frozen snippets inside its versioned docs
snapshot, for example `sniffy-ui/apps/site/versioned_docs/version-3.1/_snippets/`.
Each excerpt should be a text/code file named for its page and region. A manifest in
that same directory must record: source tag (`v3.1.14`), resolved commit
(`55f744f6a32dd68ee8d5d31c54796e9b5d75aad1`), repository-relative source path,
region or whole-file marker, blob SHA, excerpt SHA-256, and language. Archive MDX
must import/render only those owned files; it must never invoke the live
`<SourceSnippet>` resolver.

Generation may be a one-time migration command that reads the exact commit. Normal
site builds must only verify manifest hashes and render committed excerpts—never
fetch or regenerate. Changes are allowed only in an explicitly reviewed archive
correction PR. Such a PR must retain the source tag/commit, explain the correction,
update the manifest hash, and must not synchronize from `develop`. Product changes
on `develop` update only Next snippets. Release creation freezes a new snapshot from
the release's exact commit under a new owner directory.

Whole-file historical includes deserve special review: freeze the intended file as
it appeared at the tag, but trim license/fixture scaffolding only if the conversion
records that editorial transformation. Add a link to the exact commit/path for
provenance, not as runtime content.

## Migration proof obligations and unresolved uncertainties

Before #671 or another implementation publishes anything, it should prove:

- every 3.1 section, image, include, and tagged region converts without falling
  back to `develop`;
- dependency/version literals remain 3.1.14 and archived pages have a conspicuous
  unsupported banner;
- local and external links are classified, with redirects/archives used only when
  their destination is verified;
- canonical URLs, search results, sitemap entries, selector state, and the
  `/docs/latest/` policy match the route matrix; and
- visual/accessibility evidence covers archive banners, desktop/mobile navigation,
  and both themes.

Uncertainties not resolvable from this repository history are: the exact historical
contents of wiki pages and the separate UI repository; whether remote demo assets
or Heroku/Bintray downloads were preserved elsewhere; the routes/anchors actually
deployed from generated AsciiDoc; historical support demand by product line; and
which external sites intentionally maintain redirects. Network responses today are
not proof of historical content, so this report labels obvious retired/moving
services as risks rather than inventing replacement facts.

## Acceptance-to-proof matrix

| Issue criterion                                             | Inspected refs                                                                  | Proof in this report                                                                                 |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Based on Git history and tags                               | complete `1.0..v3.1.14` tag set plus `develop@ade8e64a`; documentation log      | command transcript, tag enumeration, resolved commits, fingerprints                                  |
| Distinguish major/minor lines from patches                  | latest patch of every stable line; intermediate 2.2, 2.3, 3.0, and 3.1 tags/RCs | fingerprint method, identity exceptions, diff-summary table, representative rationale                |
| Every recommended version recoverable with risks identified | `develop@ade8e64a`; `v3.1.14@55f744f6`                                          | Next and 3.1 inventories, include resolution, historical values, risk lists, frozen-snippet plan     |
| Decide whether 2.x or older is useful                       | `1.0..1.4`, `2.0..2.3.5`                                                        | explicit **no** for 2.x and 1.x with per-line evidence; 3.0 deferred                                 |
| Recommend smallest version/route set                        | current tree and final 3.1 tag                                                  | two-line decision and route/selector matrix                                                          |
| No historical site content published                        | branch diff                                                                     | this report is the only deliverable; no versioning, routes, archived content, or generated artifacts |
