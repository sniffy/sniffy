#!/usr/bin/env bash
set -uo pipefail

# Warm the Maven cache during Codex Cloud setup/maintenance using the Maven and
# JDK supplied by codex-universal. Do not replace these tools or synthesize proxy
# settings here; the platform owns their network/TLS integration.

MAX_ATTEMPTS="${SNIFFY_MAVEN_WARMUP_ATTEMPTS:-2}"
BASE_DELAY_SECONDS="${SNIFFY_MAVEN_WARMUP_DELAY_SECONDS:-5}"

if ! [[ "${MAX_ATTEMPTS}" =~ ^[1-9][0-9]*$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_ATTEMPTS must be a positive integer." >&2
  exit 2
fi

if ! [[ "${BASE_DELAY_SECONDS}" =~ ^[0-9]+$ ]]; then
  echo "SNIFFY_MAVEN_WARMUP_DELAY_SECONDS must be a non-negative integer." >&2
  exit 2
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is unavailable; the Codex universal image is incomplete." >&2
  exit 1
fi

if [[ -n "${SNIFFY_JDK25_HOME:-}" ]]; then
  export JAVA_HOME="${SNIFFY_JDK25_HOME}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

maven_args=(
  -T 1C
  -B
  de.qaware.maven:go-offline-maven-plugin:resolve-dependencies
  -U
  -P ci
  -Dmaven.wagon.http.retryHandler.count=3
)

printf 'Warming dependencies with Maven %s and JAVA_HOME=%s\n' "$(command -v mvn)" "${JAVA_HOME:-<unset>}"
mvn -version

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  echo "Warming Maven cache (attempt ${attempt}/${MAX_ATTEMPTS})..."

  if ((attempt == MAX_ATTEMPTS)); then
    if mvn -e -X "${maven_args[@]}"; then
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

echo "Maven cache warm-up failed with the Codex-provided Maven/JDK." >&2
echo "Inspect the final Maven transport exception; do not reinstall Maven or replace the platform JDK as a workaround." >&2
exit 1
