#!/usr/bin/env bash
set -euo pipefail

# Codex runs this when resuming a cached cloud environment on a task branch.

if [[ ! -f "${HOME}/.sniffy-codex-env" ]]; then
  exec bash .codex/cloud/setup.sh
fi

# shellcheck disable=SC1090
source "${HOME}/.sniffy-codex-env"

# Proxy endpoints are task-environment details and may change when a cached
# container is resumed, so refresh Maven's settings before resolving anything.
bash .codex/cloud/configure-maven-proxy.sh

# Resolve dependencies again after checkout. With a warm ~/.m2 cache this is
# cheap, while still downloading dependencies introduced by the selected branch.
bash .codex/cloud/warm-maven-cache.sh
