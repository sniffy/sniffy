# ChatGPT website preview and screenshot runbook

Use this runbook when ChatGPT needs to inspect a Sniffy website pull request in a real browser. Serve immutable exact-head
output over localhost HTTP, load it normally in Chromium, and capture reproducible evidence. Never substitute an altered
or inlined document for the built site.

## Evidence and truthfulness contract

Record the repository, pull request, exact head SHA, route, viewport, theme, browser, and interaction state. When using CI
output, also record the workflow run and artifact name, ID, digest, and `head_sha`.

Never:

- inline CSS, JavaScript, images, or markup into a substitute HTML document;
- edit generated output to make it render;
- call a downloaded CI artifact a local npm build;
- call a `curl` response a browser test;
- reuse screenshots from a different head without saying so;
- empty the managed Chromium `URLBlocklist` or leave a changed policy behind;
- claim that an image was attached to GitHub when it was only shown in ChatGPT.

If real localhost HTTP navigation cannot be made to work, stop and use exact-head CI evidence or an agent with a supported
browser environment. Do not fabricate a preview.

## Choose the input

### Preferred review path: exact-head CI artifact

For independent review, prefer the packaged website artifact produced by the exact-head `Website` job:

1. Fetch the pull request and record its immutable `head_sha`.
2. Fetch workflow runs for that SHA and select the successful exact-head pull-request run.
3. Fetch the artifact named `sniffy-website-<head_sha>`.
4. Verify that its metadata has the same `head_sha`, a digest, and has not expired.
5. Download it through the GitHub connector to `/mnt/data/sniffy-site-<head_sha>.zip`.
6. Extract it into a fresh `/mnt/data/sniffy-site-<head_sha>/` directory.

Do not use an artifact from the base commit, an earlier PR head, or another SHA.

### Source-build path

Use a source build only when the shell can actually obtain the repository at the exact SHA. Preserve the output of:

```bash
git clone https://github.com/sniffy/sniffy.git /tmp/sniffy
cd /tmp/sniffy
git checkout --detach <exact-head-sha>
test "$(git rev-parse HEAD)" = "<exact-head-sha>"
cd sniffy-ui
node --version
npm --version
npm ci
npm run build:site
npm run package:site-artifact
npm run verify:site-artifact
```

If DNS or outbound networking prevents checkout or package download, do not claim these commands ran. Use the GitHub
connector and exact-head CI artifact instead.

## Serve the unmodified artifact over HTTP

Prefer the dependency-free preview launcher and README included in the packaged artifact. A portable fallback is:

```bash
site_dir=/mnt/data/sniffy-site-<exact-head-sha>
route=/use-cases/example/
port=4173

python3 -m http.server "$port" \
  --bind 127.0.0.1 \
  --directory "$site_dir" \
  >/tmp/sniffy-site-preview.log 2>&1 &
server_pid=$!

for attempt in $(seq 1 50); do
  if curl --fail --silent "http://127.0.0.1:${port}${route}" >/dev/null 2>&1; then
    break
  fi
  sleep 0.1
done

curl --fail --silent --show-error \
  --output /dev/null \
  --write-out 'HTTP %{http_code}\n' \
  "http://127.0.0.1:${port}${route}"
```

A `200` response proves only HTTP serving. Chromium must still load the actual scripts, styles, images, and page.

## Allow only the exact localhost preview in managed Chromium

The ChatGPT sandbox may have `/etc/chromium/policies/managed/000_policy_merge.json` with
`"URLBlocklist": ["*"]`. Do not remove or empty it. In a disposable sandbox running as root, temporarily append exact
`URLAllowlist` entries for the selected port while preserving every other policy.

Discover and inspect the file rather than assuming it:

```bash
policy_file=/etc/chromium/policies/managed/000_policy_merge.json
test -f "$policy_file"
python3 -m json.tool "$policy_file"
```

Use an exact port. Chromium's URL filter grammar does not accept a wildcard port for an IP address.

```bash
policy_backup=$(mktemp)
cp --preserve=all "$policy_file" "$policy_backup"
original_policy_hash=$(sha256sum "$policy_file" | cut -d' ' -f1)

cleanup() {
  cp --preserve=all "$policy_backup" "$policy_file" 2>/dev/null || true
  rm -f "$policy_backup"
  if [[ -n "${server_pid:-}" ]]; then
    kill "$server_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

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

Start a new Chromium process after changing the policy. This exact allowlist exception is safer than clearing the
blocklist: non-local navigation remains blocked. Never modify browser policy on a user's workstation, a persistent shared
host, or an environment whose policy ownership is unclear. Without root and a reversible policy file, stop and use CI
evidence.

## Open the real page with Playwright

Python Playwright can drive the installed `/usr/bin/chromium`; no browser download is required when outbound DNS is
unavailable. This tested procedure captures desktop/mobile × light/dark full-page screenshots from normal HTTP
navigation.

```bash
export SNIFFY_PREVIEW_URL="http://127.0.0.1:${port}${route}"
export SNIFFY_PREVIEW_SHA="<exact-head-sha>"
export SNIFFY_SCREENSHOT_DIR="/mnt/data/sniffy-preview-${SNIFFY_PREVIEW_SHA}"
mkdir -p "$SNIFFY_SCREENSHOT_DIR"

python3 <<'PY'
import json
import os
from pathlib import Path
from urllib.parse import urlsplit
from playwright.sync_api import sync_playwright

url = os.environ["SNIFFY_PREVIEW_URL"]
sha = os.environ["SNIFFY_PREVIEW_SHA"]
out = Path(os.environ["SNIFFY_SCREENSHOT_DIR"])
parts = urlsplit(url)
origin = f"{parts.scheme}://{parts.netloc}"
variants = [("desktop", 1440, 900), ("mobile", 390, 844)]
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
            page_errors = []
            failed_requests = []
            page.on(
                "console",
                lambda message: console_errors.append(message.text)
                if message.type == "error"
                else None,
            )
            page.on("pageerror", lambda error: page_errors.append(str(error)))
            page.on(
                "requestfailed",
                lambda request: failed_requests.append(
                    {
                        "url": request.url,
                        "failure": request.failure,
                        "sameOrigin": request.url.startswith(origin),
                    }
                ),
            )
            page.add_init_script(
                f"localStorage.setItem('theme', {json.dumps(theme)});"
            )

            response = page.goto(url, wait_until="load", timeout=15_000)
            if response is None or response.status != 200:
                raise RuntimeError(f"Navigation failed for {url}: {response}")
            page.locator("h1").first.wait_for(state="visible", timeout=10_000)
            page.wait_for_timeout(500)

            dimensions = page.evaluate(
                """() => ({
                    documentWidth: document.documentElement.scrollWidth,
                    viewportWidth: window.innerWidth,
                    documentHeight: document.documentElement.scrollHeight,
                    theme: document.documentElement.dataset.theme
                })"""
            )
            local_failures = [item for item in failed_requests if item["sameOrigin"]]
            if dimensions["documentWidth"] > dimensions["viewportWidth"]:
                raise RuntimeError(
                    f"Horizontal overflow: {dimensions['documentWidth']} > "
                    f"{dimensions['viewportWidth']}"
                )
            if dimensions["theme"] != theme:
                raise RuntimeError(
                    f"Theme mismatch: expected {theme}, rendered {dimensions['theme']}"
                )
            if page_errors or local_failures:
                raise RuntimeError(
                    json.dumps(
                        {
                            "pageErrors": page_errors,
                            "sameOriginRequestFailures": local_failures,
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
                    "viewport": {
                        "name": viewport_name,
                        "width": width,
                        "height": height,
                    },
                    "theme": theme,
                    "title": page.title(),
                    "h1": page.locator("h1").first.inner_text(),
                    "dimensions": dimensions,
                    "consoleErrors": console_errors,
                    "pageErrors": page_errors,
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

Console errors and failed external requests are retained in `evidence.json` for inspection. JavaScript page errors and
failed same-origin requests are blockers. Do not work around a browser failure by editing or inlining the generated site.

## Restore and verify policy

After Playwright completes, restore before reporting success:

```bash
cleanup
trap - EXIT INT TERM
restored_policy_hash=$(sha256sum "$policy_file" | cut -d' ' -f1)
test "$restored_policy_hash" = "$original_policy_hash"
echo 'Chromium policy restored byte-for-byte'
```

The cleanup path must also run after a failed browser launch, timeout, or interrupted task.

## Review and report

Open every generated PNG and inspect it. Report:

- exact head SHA and artifact identity;
- whether the input was a CI artifact or a locally built checkout;
- server command and HTTP result;
- browser executable and Playwright binding;
- route, viewport, theme, title, H1, document width, and viewport width;
- console errors, JavaScript page errors, and failed requests;
- screenshot and `evidence.json` paths;
- byte-for-byte policy restoration result;
- any difference from repository visual baselines.

Screenshots may be shown in ChatGPT with sandbox links. Publishing into a GitHub PR requires an attachment-capable GitHub
path or the repository screenshot publishing helper in an agent environment.
