#!/usr/bin/env bash
set -uo pipefail

# Warm the Maven cache during Codex Cloud setup/maintenance.
#
# Setup and maintenance run with internet access, but all outbound traffic goes
# through a proxy and transient Maven Central failures can occur. Retry with
# backoff instead of immediately failing the whole environment build.

MAX_ATTEMPTS="${SNIFFY_MAVEN_WARMUP_ATTEMPTS:-6}"
BASE_DELAY_SECONDS="${SNIFFY_MAVEN_WARMUP_DELAY_SECONDS:-10}"
CENTRAL_PROBE_URL="${SNIFFY_MAVEN_CENTRAL_PROBE_URL:-https://repo.maven.apache.org/maven2/org/apache/felix/maven-bundle-plugin/5.1.1/maven-bundle-plugin-5.1.1.pom}"

if ! [[ "${MAX_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_ATTEMPTS must be a positive integer." >&2
  exit 2
fi

if ! [[ "${BASE_DELAY_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_DELAY_SECONDS must be a non-negative integer." >&2
  exit 2
fi

maven_args=(
  -T 1C
  -B
  de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
  -U
  -P ci
  -Dmaven.wagon.http.retryHandler.count=5
)

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  echo "Warming Maven cache (attempt ${attempt}/${MAX_ATTEMPTS})..."

  if command -v curl >/dev/null 2>&1; then
    if ! curl \
      --fail \
      --silent \
      --show-error \
      --location \
      --retry 3 \
      --retry-all-errors \
      --retry-delay 2 \
      --connect-timeout 15 \
      --max-time 90 \
      "${CENTRAL_PROBE_URL}" \
      --output /dev/null; then
      echo "Maven Central connectivity probe failed; Maven may still succeed." >&2
    fi
  fi

  if mvn "${maven_args[@]}"; then
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
echo "The toolchain was installed, but the Cloud task would not be reliable with agent internet disabled." >&2
echo "Retry after resetting the environment cache. For diagnosis only, increase SNIFFY_MAVEN_WARMUP_ATTEMPTS." >&2
exit 1
