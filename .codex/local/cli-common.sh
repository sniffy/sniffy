#!/usr/bin/env bash
set -euo pipefail

CLI_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY="${AI_DELIVERY_REPOSITORY:-sniffy/sniffy}"
BASE_BRANCH="${AI_DELIVERY_BASE_BRANCH:-develop}"
STATE_ROOT="${AI_DELIVERY_STATE_ROOT:-${XDG_STATE_HOME:-$HOME/.local/state}/sniffy-local-executor}"
WORKTREE_ROOT="${AI_DELIVERY_WORKTREE_ROOT:-$STATE_ROOT/worktrees}"
CONTROL_LABEL="${AI_DELIVERY_CONTROL_LABEL:-ai-delivery-control}"
CONTROL_TIMEOUT_SECONDS="${AI_DELIVERY_CONTROL_TIMEOUT_SECONDS:-120}"
SPAWN_TIMEOUT_SECONDS="${AI_DELIVERY_SPAWN_TIMEOUT_SECONDS:-90}"
PROVISIONAL_LEASE_SECONDS="${AI_DELIVERY_PROVISIONAL_LEASE_SECONDS:-300}"
NORMAL_LEASE_SECONDS="${AI_DELIVERY_NORMAL_LEASE_SECONDS:-14400}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command '$1' is unavailable." >&2
    exit 2
  }
}

now_iso() { date -u +%Y-%m-%dT%H:%M:%SZ; }
future_iso() { date -u -d "+${1} seconds" +%Y-%m-%dT%H:%M:%SZ; }

atomic_json() {
  local output="$1"
  local temp="${output}.tmp.$$"
  cat >"$temp"
  chmod 600 "$temp"
  mv "$temp" "$output"
}

generation_dir() { printf '%s/%s' "$STATE_ROOT/generations" "$1"; }

manifest_update() {
  local file="$1"
  local filter="$2"
  shift 2
  (
    flock -x 200
    jq "$@" "$filter" "$file" | atomic_json "$file"
  ) 200>"${file}.lock"
}

control_issue_number() {
  gh issue list --repo "$REPOSITORY" --state open --label "$CONTROL_LABEL" --limit 100 \
    --json number,createdAt --jq 'sort_by(.createdAt) | last | .number'
}

post_control_command() {
  local summary="$1"
  local payload_file="$2"
  local issue
  issue="$(control_issue_number)"
  [[ "$issue" =~ ^[0-9]+$ ]] || { echo "No active control issue." >&2; return 1; }
  local body
  body="$(cat <<EOF
### AI delivery control

${summary}

<details>
<summary>Machine-readable command</summary>

<!-- delivery-control-command:start -->
\`\`\`json
$(cat "$payload_file")
\`\`\`
<!-- delivery-control-command:end -->

</details>
EOF
)"
  gh api "repos/${REPOSITORY}/issues/${issue}/comments" -f body="$body" --jq '.id'
}

wait_control_reaction() {
  local comment_id="$1"
  local deadline=$((SECONDS + CONTROL_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    local terminal
    terminal="$(gh api "repos/${REPOSITORY}/issues/comments/${comment_id}/reactions" \
      -H 'Accept: application/vnd.github+json' \
      --jq '[.[] | select(.content == "+1" or .content == "-1")] | last | .content // empty')"
    case "$terminal" in
      +1) return 0 ;;
      -1) return 1 ;;
    esac
    sleep 2
  done
  return 2
}
