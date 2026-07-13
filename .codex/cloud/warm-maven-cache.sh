#!/usr/bin/env bash
set -uo pipefail

# Warm the Maven cache during Codex Cloud setup/maintenance.
#
# Codex Cloud exposes outbound access through standard proxy environment
# variables. configure-maven-proxy.sh translates them into Maven settings before
# this script runs. Retries remain useful for individual repository failures, but
# they are not a substitute for configuring Maven's proxy.

MAX_ATTEMPTS="${SNIFFY_MAVEN_WARMUP_ATTEMPTS:-3}"
BASE_DELAY_SECONDS="${SNIFFY_MAVEN_WARMUP_DELAY_SECONDS:-5}"
CENTRAL_PROBE_URL="${SNIFFY_MAVEN_CENTRAL_PROBE_URL:-https://repo.maven.apache.org/maven2/org/apache/felix/maven-bundle-plugin/5.1.1/maven-bundle-plugin-5.1.1.pom}"
SETTINGS_FILE="${HOME}/.m2/settings.xml"

if ! [[ "${MAX_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_ATTEMPTS must be a positive integer." >&2
  exit 2
fi

if ! [[ "${BASE_DELAY_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_DELAY_SECONDS must be a non-negative integer." >&2
  exit 2
fi

proxy_url="${HTTPS_PROXY:-${https_proxy:-${HTTP_PROXY:-${http_proxy:-}}}}"
if [[ -n "${proxy_url}" ]]; then
  if [[ ! -f "${SETTINGS_FILE}" ]] || ! grep -Fq 'codex-cloud-env-proxy' "${SETTINGS_FILE}"; then
    echo "Cloud proxy variables are set, but Maven proxy settings are missing." >&2
    echo "Run: bash .codex/cloud/configure-maven-proxy.sh" >&2
    exit 1
  fi
fi

maven_args=(
  -T 1C
  -B
  de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
  -U
  -P ci
  -Dmaven.wagon.http.retryHandler.count=3
)

curl_probe_succeeded=false
if command -v curl >/dev/null 2>&1; then
  if curl \
    --fail \
    --silent \
    --show-error \
    --location \
    --retry 2 \
    --retry-all-errors \
    --retry-delay 1 \
    --connect-timeout 15 \
    --max-time 90 \
    "${CENTRAL_PROBE_URL}" \
    --output /dev/null; then
    curl_probe_succeeded=true
  else
    echo "Maven Central connectivity probe failed; Maven may still succeed." >&2
  fi
fi

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  echo "Warming Maven cache (attempt ${attempt}/${MAX_ATTEMPTS})..."

  if ((attempt == MAX_ATTEMPTS)); then
    if mvn -e "${maven_args[@]}"; then
      echo "Maven cache warm-up completed."
      exit 0
    fi
  elif mvn "${maven_args[@]}"; then
    echo "Maven cache warm-up completed."
    exit 0
  fi

  if ((attempt < MAX_ATTEMPTS)); then
    delay=$((BASE_DELAY_SECONDS * attempt))
    echo "Maven cache warm-up failed; retrying in ${delay} seconds." >&2
    sleep "${delay}"
  fi
done

echo "Maven cache warm-up failed after ${MAX_ATTEMPTS} attempts." >&2
if [[ "${curl_probe_succeeded}" == true ]]; then
  echo "curl reached Maven Central through the environment proxy; inspect the Maven exception above and ~/.m2/settings.xml." >&2
else
  echo "Both the direct connectivity probe and Maven resolution failed; inspect Cloud internet policy and proxy variables." >&2
fi
echo "The toolchain was installed, but the Cloud task would not be reliable with agent internet disabled." >&2
exit 1
