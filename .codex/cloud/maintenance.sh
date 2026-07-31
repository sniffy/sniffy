#!/usr/bin/env bash
set -euo pipefail

# Codex runs this when resuming a cached cloud environment on a task branch.

if [[ ! -f "${HOME}/.sniffy-codex-env" ]]; then
  exec bash .codex/cloud/setup.sh
fi

# shellcheck disable=SC1090
source "${HOME}/.sniffy-codex-env"

GITHUB_REPOSITORY_URL="https://github.com/sniffy/sniffy.git"

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url --push origin "${GITHUB_REPOSITORY_URL}"
else
  git remote add origin "${GITHUB_REPOSITORY_URL}"
fi

if [[ "$(git remote get-url --push origin)" != "${GITHUB_REPOSITORY_URL}" ]]; then
  echo "Could not configure the canonical origin push URL." >&2
  exit 1
fi

# Resolve dependencies again after checkout. With a warm ~/.m2 cache this is
# cheap, while still downloading dependencies introduced by the selected branch.
bash .codex/cloud/warm-maven-cache.sh
