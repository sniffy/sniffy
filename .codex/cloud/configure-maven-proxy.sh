#!/usr/bin/env bash
set -euo pipefail

# Translate the standard proxy environment variables used by Codex Cloud into
# Maven's supported ~/.m2/settings.xml proxy configuration.
#
# curl honours HTTPS_PROXY/HTTP_PROXY automatically. Maven does not reliably do
# so, and Maven's official proxy configuration lives in settings.xml.

command -v python3 >/dev/null 2>&1 || {
  echo "python3 is required to configure Maven proxy settings." >&2
  exit 1
}

python3 - <<'PY'
import os
import sys
import urllib.parse
import xml.etree.ElementTree as ET
from pathlib import Path

PROXY_ID = "codex-cloud-env-proxy"
DEFAULT_NS = "http://maven.apache.org/SETTINGS/1.0.0"
XSI_NS = "http://www.w3.org/2001/XMLSchema-instance"
SCHEMA_LOCATION = (
    "http://maven.apache.org/SETTINGS/1.0.0 "
    "https://maven.apache.org/xsd/settings-1.0.0.xsd"
)


def first_env(*names):
    for name in names:
        value = os.environ.get(name)
        if value:
            return value
    return None


def namespace_of(tag):
    if tag.startswith("{"):
        return tag[1:].split("}", 1)[0]
    return ""


def normalize_no_proxy(value):
    if not value:
        return ""
    result = []
    for token in value.split(","):
        token = token.strip()
        if not token:
            continue
        if token.startswith("."):
            token = "*" + token
        result.append(token)
    return "|".join(result)


proxy_url = first_env("HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy")
settings_path = Path.home() / ".m2" / "settings.xml"

if not proxy_url and not settings_path.exists():
    print("No proxy environment variable is set; Maven proxy configuration is not required.")
    sys.exit(0)

settings_path.parent.mkdir(parents=True, exist_ok=True)
ET.register_namespace("", DEFAULT_NS)
ET.register_namespace("xsi", XSI_NS)

if settings_path.exists():
    try:
        tree = ET.parse(settings_path)
    except ET.ParseError as exc:
        print(f"Refusing to overwrite invalid Maven settings at {settings_path}: {exc}", file=sys.stderr)
        sys.exit(1)
    root = tree.getroot()
    namespace = namespace_of(root.tag)
else:
    namespace = DEFAULT_NS
    root = ET.Element(
        f"{{{namespace}}}settings",
        {f"{{{XSI_NS}}}schemaLocation": SCHEMA_LOCATION},
    )
    tree = ET.ElementTree(root)


def qname(local):
    return f"{{{namespace}}}{local}" if namespace else local


def child(parent, name, text=None):
    element = ET.SubElement(parent, qname(name))
    if text is not None:
        element.text = text
    return element


proxies = root.find(qname("proxies"))
if proxies is None:
    proxies = child(root, "proxies")

for existing in list(proxies):
    proxy_id = existing.find(qname("id"))
    if proxy_id is not None and proxy_id.text == PROXY_ID:
        proxies.remove(existing)

if not proxy_url:
    if len(proxies) == 0:
        root.remove(proxies)
    ET.indent(tree, space="  ")
    tree.write(settings_path, encoding="UTF-8", xml_declaration=True)
    settings_path.chmod(0o600)
    print("Removed stale Codex Cloud Maven proxy configuration.")
    sys.exit(0)

if "://" not in proxy_url:
    proxy_url = "http://" + proxy_url

try:
    parsed = urllib.parse.urlsplit(proxy_url)
    hostname = parsed.hostname
    port = parsed.port or (443 if parsed.scheme == "https" else 80)
except ValueError as exc:
    print(f"Invalid proxy URL: {exc}", file=sys.stderr)
    sys.exit(1)

if not hostname:
    print("Proxy URL does not contain a hostname.", file=sys.stderr)
    sys.exit(1)

protocol = parsed.scheme or "http"
username = urllib.parse.unquote(parsed.username) if parsed.username else None
password = urllib.parse.unquote(parsed.password) if parsed.password else None
non_proxy_hosts = normalize_no_proxy(first_env("NO_PROXY", "no_proxy"))

# Maven supports only one active proxy. Keep any existing definitions, but make
# the Cloud environment proxy the active one for this disposable environment.
for existing in list(proxies):
    active = existing.find(qname("active"))
    if active is not None and (active.text or "").strip().lower() == "true":
        active.text = "false"

proxy = child(proxies, "proxy")
child(proxy, "id", PROXY_ID)
child(proxy, "active", "true")
child(proxy, "protocol", protocol)
child(proxy, "host", hostname)
child(proxy, "port", str(port))
if username is not None:
    child(proxy, "username", username)
if password is not None:
    child(proxy, "password", password)
if non_proxy_hosts:
    child(proxy, "nonProxyHosts", non_proxy_hosts)

ET.indent(tree, space="  ")
tree.write(settings_path, encoding="UTF-8", xml_declaration=True)
settings_path.chmod(0o600)
print(f"Configured Maven proxy {hostname}:{port} from the Cloud proxy environment.")
PY
