#!/usr/bin/env bash
set -euo pipefail

# Repository-owned Codex Cloud entrypoint for the PR screenshot publisher.
# The setup script persists Node 24 and proxy-aware fetch configuration here.
if [[ -f "${HOME}/.sniffy-codex-env" ]]; then
  # shellcheck disable=SC1090
  source "${HOME}/.sniffy-codex-env"
fi

export NODE_USE_ENV_PROXY="${NODE_USE_ENV_PROXY:-1}"

node_version="$(node -p 'process.versions.node')"
case "${node_version}" in
  24.*) ;;
  *)
    echo "publish-pr-screenshots requires Node.js 24.x in Codex Cloud; found ${node_version}. Run bash .codex/cloud/setup.sh first." >&2
    exit 1
    ;;
esac

exec node --env-file-if-exists=.env.local \
  .agents/skills/publish-pr-screenshots/scripts/publish-pr-screenshots.mjs \
  "$@"
