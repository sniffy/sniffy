#!/usr/bin/env bash
set -euo pipefail
# systemd worker body: create one isolated worktree and require strong Codex JSONL acknowledgement.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=cli-common.sh
source "$SCRIPT_DIR/cli-common.sh"

generation="${1:-}"
[[ "$generation" =~ ^[a-z0-9][a-z0-9._-]{7,159}$ ]] || { echo "Invalid generation." >&2; exit 2; }
for command_name in git jq node codex flock; do require_command "$command_name"; done

generation_path="$(generation_dir "$generation")"
manifest_file="$generation_path/manifest.json"
[[ -r "$manifest_file" ]] || { echo "Manifest not found." >&2; exit 2; }
node "$SCRIPT_DIR/cli-worker-state.js" reference "$manifest_file" spawning "$(jq -r .provisionalLeaseUntil "$manifest_file")" >/dev/null

exec 8>"$generation_path/worker.lock"
flock -n 8 || { echo "Generation already has a launcher." >&2; exit 73; }

launch_acknowledged=false
record_unacknowledged_failure() {
  local status=$?
  if [[ "$launch_acknowledged" != true && ! -s "$generation_path/ack.json" ]]; then
    manifest_update "$manifest_file" \
      '.state="launch-failed" | .launchFailedAt=$failedAt | .failureReason=$reason | .launcherExitCode=$exitCode' \
      --arg failedAt "$(now_iso)" --arg reason "launcher-exited-before-strong-ack" --argjson exitCode "$status" || true
    jq -n --arg generation "$generation" --arg failedAt "$(now_iso)" --argjson exitCode "$status" \
      '{generation:$generation,state:"launch-failed",failedAt:$failedAt,
        reason:"launcher exited before thread.started plus turn.started",exitCode:$exitCode}' \
      | atomic_json "$generation_path/launch-failed.json" || true
  fi
}
trap record_unacknowledged_failure EXIT

branch="$(jq -r .branch "$manifest_file")"
mode="$(jq -r .mode "$manifest_file")"
base_sha="$(jq -r .baseSha "$manifest_file")"
worktree="$WORKTREE_ROOT/$generation"
events="$generation_path/events.jsonl"
stderr_log="$generation_path/stderr.log"
final_message="$generation_path/final.md"

mkdir -p "$WORKTREE_ROOT"
git cat-file -e "${base_sha}^{commit}"
if [[ -e "$worktree" ]]; then
  echo "Worktree path already exists." >&2
  exit 1
fi

if [[ "$mode" == fresh ]]; then
  if git show-ref --verify --quiet "refs/heads/$branch" || git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
    echo "Fresh branch already exists; refusing duplicate generation." >&2
    exit 1
  fi
  git worktree add -b "$branch" "$worktree" "$base_sha"
else
  git fetch origin "$branch"
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    git worktree add "$worktree" "$branch"
  else
    git worktree add --track -b "$branch" "$worktree" "origin/$branch"
  fi
fi

worker_profile="$(jq -r .workerProfile "$manifest_file")"
model="$(jq -r .model "$manifest_file")"
reasoning="$(jq -r .reasoning "$manifest_file")"
: >"$events"
: >"$stderr_log"

set +e
codex \
  --model "$model" \
  --config "model_reasoning_effort=\"${reasoning}\"" \
  --ask-for-approval never \
  --sandbox danger-full-access \
  --search \
  exec --json --cd "$worktree" --output-last-message "$final_message" - \
  <"$generation_path/prompt.md" >"$events" 2>"$stderr_log" &
codex_pid=$!
set -e

manifest_update "$manifest_file" \
  '.pid=$pid | .worktree=$worktree | .processStartedAt=$startedAt' \
  --argjson pid "$codex_pid" --arg worktree "$worktree" --arg startedAt "$(now_iso)"

ack_deadline=$((SECONDS + SPAWN_TIMEOUT_SECONDS))
ack=""
while (( SECONDS < ack_deadline )); do
  if ack="$(node "$SCRIPT_DIR/cli-worker-state.js" ack "$events" 2>/dev/null)"; then
    break
  fi
  if ! kill -0 "$codex_pid" 2>/dev/null; then
    break
  fi
  sleep 1
done

if [[ -z "$ack" ]]; then
  kill "$codex_pid" 2>/dev/null || true
  wait "$codex_pid" 2>/dev/null || true
  exit 1
fi

thread_id="$(jq -r .threadId <<<"$ack")"
acknowledged_at="$(now_iso)"
manifest_update "$manifest_file" \
  '.state="running" | .threadId=$threadId | .acknowledgedAt=$acknowledgedAt' \
  --arg threadId "$thread_id" --arg acknowledgedAt "$acknowledged_at"
jq -n --arg generation "$generation" --arg threadId "$thread_id" --arg acknowledgedAt "$acknowledged_at" \
  --arg workerProfile "$worker_profile" --arg model "$model" --arg reasoning "$reasoning" \
  '{generation:$generation,threadId:$threadId,acknowledgedAt:$acknowledgedAt,
    workerProfile:$workerProfile,model:$model,reasoning:$reasoning,
    proof:["thread.started","turn.started"]}' | atomic_json "$generation_path/ack.json"
launch_acknowledged=true

set +e
wait "$codex_pid"
codex_status=$?
set -e
finished_at="$(now_iso)"
terminal_json="$(node "$SCRIPT_DIR/cli-worker-state.js" terminal "$events" 2>/dev/null || true)"
turn_state="$(jq -r '.state // "exited-unverified"' <<<"${terminal_json:-{}}")"
turn_event="$(jq -r '.event // "none"' <<<"${terminal_json:-{}}")"
worktree_clean=false
if [[ -d "$worktree" && -z "$(git -C "$worktree" status --porcelain)" ]]; then
  worktree_clean=true
fi
manifest_update "$manifest_file" \
  '.state=$turnState | .terminalEvent=$turnEvent | .exitCode=$exitCode |
   .processFinishedAt=$finishedAt | .worktreeClean=$worktreeClean' \
  --arg turnState "$turn_state" --arg turnEvent "$turn_event" --argjson exitCode "$codex_status" \
  --arg finishedAt "$finished_at" --argjson worktreeClean "$worktree_clean"

if [[ "$worktree_clean" == true ]]; then
  git worktree remove "$worktree"
  if ! git ls-remote --exit-code --heads origin "refs/heads/$branch" >/dev/null 2>&1 \
      && [[ "$(git rev-parse "$branch")" == "$base_sha" ]]; then
    git branch -D "$branch"
  fi
  manifest_update "$manifest_file" '.worktreeRemovedAt=$removedAt | del(.worktree)' --arg removedAt "$(now_iso)"
fi
exit "$codex_status"
