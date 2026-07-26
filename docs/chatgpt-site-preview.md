# ChatGPT website preview and screenshot runbook

Use this runbook when a ChatGPT review needs to inspect a Sniffy website pull request in a real browser. The goal is to
serve an immutable exact-head build over localhost HTTP, load it normally in Chromium, and capture reproducible evidence
without pretending that a downloaded artifact was locally built or that an inlined document was the real site.

## Evidence and truthfulness contract

Before starting, record:

- repository and pull-request number;
- exact head SHA being reviewed;
- route or routes to inspect;
- CI run and website artifact name, ID, digest, and `head_sha` when using an artifact;
- requested viewport, theme, browser, and interaction state.

Never:

- inline the built CSS, JavaScript, images, or markup into a substitute HTML document;
- edit generated output to make it render;
- call a downloaded CI artifact a local npm build;
- call a `curl` response a browser test;
- reuse screenshots from a different head without saying so;
- clear all Chromium enterprise policies or leave a changed policy behind;
- publish screenshots without identifying route, viewport, theme, browser, and exact SHA.

If real localhost HTTP navigation cannot be made to work, stop and report that limitation. Use the exact-head CI visual
artifact or ask an agent with a supported browser environment to publish the evidence instead of fabricating a preview.

## Choose the input

### Preferred review path: exact-head CI artifact

For independent review, prefer the packaged website artifact produced by the exact-head `Website` job. This proves the
same bytes that CI packaged and verified.

1. Fetch the pull request and record its immutable `head_sha`.
2. Fetch workflow runs for that SHA and select the successful exact-head pull-request run.
3. Fetch the artifact named `sniffy-website-<head_sha>`.
4. Verify that the artifact metadata points to the same `head_sha`, is not expired, and has a digest.
5. Download it through the GitHub connector to a temporary path such as `/mnt/data/sniffy-site-<head_sha>.zip`.
6. Extract it to a fresh directory such as `/mnt/data/sniffy-site-<head_sha>/`.

Do not use an artifact from the base commit, a prior pull-request head, a rerun for another SHA, or an unverified local
folder.

### Source-build path

Use a source build only when the shell can actually obtain the repository at the exact SHA. Show and retain the command
results:

```bash
git clone https://github.com/sniffy/sniffy.git /tmp/sniffy
cd /tmp/sniffy
git checkout --detach <exact-head-sha>
git rev-parse HEAD
cd sniffy-ui
node --version
npm --version
npm ci
npm run build:site
npm run package:site-artifact
npm run verify:site-artifact
```

The checked-out SHA must equal the reviewed head. If DNS or outbound networking prevents `git clone` or package download,
do not claim that these commands ran. Fall back to the GitHub connector and the exact-head CI artifact.

## Serve the unmodified artifact over HTTP

Prefer the dependency-free preview launcher included in the packaged artifact when present and follow its README. A
portable fallback is:

```bash
site_dir=/mnt/data/sniffy-site-<exact-head-sha>
port=4173
python3 -m http.server "$port" \
  --bind 127.0.0.1 \
  --directory "$site_dir" \
  >/tmp/sniffy-site-preview.log 2>&1 &
server_pid=$!
```

Before opening a browser, prove the server and target route respond:

```bash
curl --fail --silent --show-error \
  --output /dev/null \
  --write-out 'HTTP %{http_code}\n' \
  "http://127.0.0.1:${port}/use-cases/example/"
```

A `200` response proves only HTTP serving. Browser rendering, scripts, styles, assets, console output, and failed requests
must still be checked in Chromium.

## Allow only the exact localhost preview in managed Chromium

The ChatGPT sandbox may install a managed Chromium policy containing:

```json
{
  "URLBlocklist": ["*"]
}
```

Do not remove or empty that blocklist. In a disposable sandbox where the process runs as root, temporarily add an exact
`URLAllowlist` exception for the chosen localhost port, keep every other policy unchanged, start a new Chromium process,
and restore the original file on every exit path.

The current sandbox policy is normally `/etc/chromium/policies/managed/000_policy_merge.json`. Discover and inspect it
rather than assuming:

```bash
policy_file=/etc/chromium/policies/managed/000_policy_merge.json
test -f "$policy_file"
python3 -m json.tool "$policy_file"
```

Use an exact port; an IP-address filter does not accept a wildcard port in Chromium's URL filter grammar.

```bash
policy_backup=$(mktemp)
cp --preserve=all "$policy_file" "$policy_backup"

restore_policy() {
  cp --preserve=all "$policy_backup" "$policy_file" 2>/dev/null || true
  rm -f "$policy_backup"
  if [[ -n "${server_pid:-}" ]]; then
    kill "$server_pid" 2>/dev/null || true
  fi
}
trap restore_policy EXIT INT TERM

python3 - "$policy_file" "$port" <<'PY'
import json
import sys

path = sys.argv[1]
port = int(sys.argv[2])
with open(path, encoding="utf-8") as source:
    policy = json.load(source)

if policy.get("URLBlocklist") != ["*"]:
    raise SystemExit("Refusing to modify an unexpected URLBlocklist policy")

allowed = list(policy.get("URLAllowlist", []))
for url in (f"http://127.0.0.1:{port}", f"http://localhost:{port}"):
    if url not in allowed:
        allowed.append(url)
policy["URLAllowlist"] = allowed

with open(path, "w", encoding="utf-8") as destination:
    json.dump(policy, destination, indent=2)
    destination.write("\n")
PY
```

This exception is safer than setting `URLBlocklist` to an empty list: all non-local navigation remains blocked. Never do
this on a user's workstation, a persistent shared host, or an environment whose policy ownership is unclear. Without root
or an exact reversible policy file, stop and use CI evidence.

After the browser exits, verify restoration explicitly:

```bash
python3 - "$policy_file" <<'PY'
import json
import sys
policy = json.load(open(sys.argv[1], encoding="utf-8"))
assert policy.get("URLBlocklist") == ["*"]
assert "URLAllowlist" not in policy or all(
    not entry.startswith("http://127.0.0.1:") and not entry.startswith("http://localhost:")
    for entry in policy["URLAllowlist"]
)
print('Chromium URL policy restored')
PY
```

## Open the real page with Playwright

The Python Playwright package can drive the installed `/usr/bin/chromium`; downloading another browser is unnecessary
when outbound DNS is unavailable.

The following script captures desktop/mobile and light/dark full-page screenshots from normal HTTP navigation. Adjust the
route and output directory, but do not alter the built site.

```bash
export SNIFFY_PREVIEW_URL="http://127.0.0.1:${port}/use-cases/example/"
export SNIFFY_PREVIEW_SHA="<exact-head-sha>"
export SNIFFY_SCREENSHOT_DIR="/mnt/data/sniffy-preview-${SNIFFY_PREVIEW_SHA}"
mkdir -p "$SNIFFY_SCREENSHOT_DIR"

python3 <<'PY'
import json
import os
from pathlib import Path
from playwright.sync_api import sync_playwright

url = os.environ["SNIFFY_PREVIEW_URL"]
sha = os.environ["SNIFFY_PREVIEW_SHA"]
out = Path(os.environ["SNIFFY_SCREENSHOT_DIR"])

variants = [
    ("desktop", 1440, 900),
    ("mobile", 390, 844),
]
themes = ["light", "dark"]
results = []

with sync_playwright() as playwright:
    browser = playwright.chromium.launch(
        executable_path="/usr/bin/chromium",
        headless=True,
        args=["--no-sandbox"],
    )

    for viewport_name, width, height in variants:
        for theme in themes:
            context = browser.new_context(
                viewport={"width": width, "height": height},
                color_scheme=theme,
                device_scale_factor=1,
            )
            page = context.new_page()
            console_errors = []
            failed_requests = []
            page.on(
                "console",
                lambda message: console_errors.append(message.text)
                if message.type == "error"
                else None,
            )
            page.on(
                "requestfailed",
                lambda request: failed_requests.append(
                    {"url": request.url, "failure": request.failure}
                ),
            )
            page.add_init_script(
                "theme => localStorage.setItem('theme', theme)",
                theme,
            )

            response = page.goto(url, wait_until="networkidle")
            if response is None or response.status != 200:
                raise RuntimeError(f"Navigation failed for {url}: {response}")

            page.locator("h1").first.wait_for(state="visible")
            title = page.title()
            heading = page.locator("h1").first.inner_text()
            dimensions = page.evaluate(
                """() => ({
                    documentWidth: document.documentElement.scrollWidth,
                    viewportWidth: window.innerWidth,
                    documentHeight: document.documentElement.scrollHeight,
                    theme: document.documentElement.dataset.theme
                })"""
            )
            if dimensions["documentWidth"] > dimensions["viewportWidth"]:
                raise RuntimeError(
                    f"Horizontal overflow: {dimensions['documentWidth']} > "
                    f"{dimensions['viewportWidth']}"
                )
            if dimensions["theme"] != theme:
                raise RuntimeError(
                    f"Theme mismatch: expected {theme}, rendered {dimensions['theme']}"
                )
            if console_errors or failed_requests:
                raise RuntimeError(
                    json.dumps(
                        {
                            "consoleErrors": console_errors,
                            "failedRequests": failed_requests,
                        },
                        indent=2,
                    )
                )

            filename = out / f"site-{sha}-{viewport_name}-{theme}.png"
            page.screenshot(path=str(filename), full_page=True)
            results.append(
                {
                    "sha": sha,
                    "url": url,
                    "browser": "system Chromium via Playwright",
                    "viewport": {"name": viewport_name, "width": width, "height": height},
                    "theme": theme,
                    "title": title,
                    "h1": heading,
                    "dimensions": dimensions,
                    "consoleErrors": console_errors,
                    "failedRequests": failed_requests,
                    "screenshot": str(filename),
                }
            )
            context.close()

    browser.close()

(out / "evidence.json").write_text(json.dumps(results, indent=2) + "\n")
print(json.dumps(results, indent=2))
PY
```

If `page.add_init_script` uses a different Playwright binding version, pass the script as a single JavaScript string with
the theme value JSON-encoded. Do not fall back to editing generated HTML.

## Review and report

Open every generated PNG and inspect it rather than relying only on dimensions or a screenshot-test exit code. Report:

- exact head SHA and artifact identity;
- whether input was a CI artifact or a locally built checkout;
- server command and HTTP result;
- browser executable and Playwright binding;
- route, viewport, theme, title, H1, document width, and viewport width;
- console errors and failed requests;
- screenshot paths;
- policy restoration result;
- any difference from the repository's asserted visual baselines.

The screenshots may be shown in ChatGPT with sandbox links. Publishing them into a GitHub pull request requires a GitHub
attachment-capable path or an agent environment that supports the repository screenshot publishing helper. Do not imply
that a local ChatGPT sandbox image was attached to GitHub when it was only shown in the conversation.

## Cleanup

The `trap` must restore the browser policy and stop the server even when Playwright fails. Then remove temporary extracted
artifacts and browser profiles when they are no longer needed. Retain only evidence intentionally shared with the
maintainer.
