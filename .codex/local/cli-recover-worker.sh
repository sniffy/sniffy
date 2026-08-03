#!/usr/bin/env bash
set -euo pipefail
# Targeted generation recovery. It never spawns a replacement generation.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=cli-common.sh
source "$SCRIPT_DIR/cli-common.sh"

generation="${1:-}"
[[ "$generation" =~ ^[a-z0-9][a-z0-9._-]{7,159}$ ]] || { echo "Invalid generation." >&2; exit 2; }
generation_path="$(generation_dir "$generation")"
manifest_file="$generation_path/manifest.json"
[[ -r "$manifest_file" ]] || { echo "Manifest not found." >&2; exit 2; }
for command_name in jq node gh systemctl flock; do require_command "$command_name"; done

exec 7>"$generation_path/recovery.lock"
flock -n 7 || { echo '{"outcome":"RECOVERY_ALREADY_RUNNING"}'; exit 0; }

type="$(jq -r .type "$manifest_file")"
number="$(jq -r .number "$manifest_file")"
status="$(jq -r .status "$manifest_file")"
exact_head="$(jq -r '.exactHead // ""' "$manifest_file")"
spawning_reference="$(jq -r .spawningReference "$manifest_file")"
running_reference="$(jq -r '.runningReference // ""' "$manifest_file")"
unit="sniffy-local-worker@${generation}.service"

confirm_running() {
  local thread_id normal_lease reference payload comment_id
  thread_id="$(jq -r .threadId "$generation_path/ack.json")"
  normal_lease="$(future_iso "$NORMAL_LEASE_SECONDS")"
  reference="$(node "$SCRIPT_DIR/cli-worker-state.js" reference "$manifest_file" running "$normal_lease" "$thread_id")"
  payload="$generation_path/recovered-running.json"
  jq -n --arg type "$type" --argjson number "$number" --arg status "$status" \
    --arg spawningReference "$spawning_reference" --arg runningReference "$reference" --arg head "$exact_head" \
    '{
      command:"delivery-control/v1",
      target:{repository:"sniffy/sniffy",number:$number},
      expected:{type:$type,fields:{Status:$status,Execution:"In progress",Executor:"Local Codex","Worker reference":$spawningReference}},
      set:{"Worker reference":$runningReference},
      addIfMissing:false
    } | if $type == "PullRequest" then .expected.head=$head else . end' >"$payload"
  comment_id="$(post_control_command \
    "Recover confirmed CLI worker \`${generation}\` after strong JSONL acknowledgement." \
    "$payload")"
  if wait_control_reaction "$comment_id"; then
    manifest_update "$manifest_file" \
      '.projectReferenceState="running" | .runningReference=$reference | .leaseUntil=$lease | .projectAcknowledgedAt=$acknowledgedAt' \
      --arg reference "$reference" --arg lease "$normal_lease" --arg acknowledgedAt "$(now_iso)"
    printf '{"outcome":"RUNNING_REFERENCE_RECOVERED","generation":"%s","thread":"%s"}\n' "$generation" "$thread_id"
  else
    printf '{"outcome":"RUNNING_REFERENCE_CONFLICT","generation":"%s"}\n' "$generation"
  fi
}

if systemctl --user is-active --quiet "$unit"; then
  if [[ -s "$generation_path/ack.json" ]]; then
    if [[ -z "$running_reference" ]]; then
      confirm_running
    else
      printf '{"outcome":"ACTIVE_SAME_GENERATION","generation":"%s"}\n' "$generation"
    fi
  else
    printf '{"outcome":"SPAWNING_SAME_GENERATION","generation":"%s"}\n' "$generation"
  fi
  exit 0
fi

state="$(jq -r '.state // "unknown"' "$manifest_file")"
reference="${running_reference:-$spawning_reference}"
lease="$(sed -n 's/.*leaseUntil=\([^;]*\).*/\1/p' <<<"$reference")"
now_epoch="$(date -u +%s)"
lease_epoch="$(date -u -d "$lease" +%s 2>/dev/null || echo 0)"

if [[ "$state" == spawning && "$lease_epoch" -gt "$now_epoch" ]]; then
  printf '{"outcome":"WAIT_PROVISIONAL_LEASE","generation":"%s"}\n' "$generation"
  exit 0
fi

manifest_update "$manifest_file" '.recoveryStartedAt=$recoveryStartedAt' --arg recoveryStartedAt "$(now_iso)"
payload="$generation_path/release.json"
branch="$(jq -r .branch "$manifest_file")"
recovery_reference="Recovered inactive CLI generation ${generation}; branch=${branch}; adapter=codex-cli-v1; no active process remains"
jq -n --arg type "$type" --argjson number "$number" --arg status "$status" \
  --arg reference "$reference" --arg recoveryReference "$recovery_reference" --arg head "$exact_head" \
  '{
    command:"delivery-control/v1",
    target:{repository:"sniffy/sniffy",number:$number},
    expected:{type:$type,fields:{Status:$status,Execution:"In progress",Executor:"Local Codex","Worker reference":$reference}},
    set:{Execution:"Ready","Worker reference":$recoveryReference},
    addIfMissing:false
  } | if $type == "PullRequest" then .expected.head=$head else . end' >"$payload"

comment_id="$(post_control_command \
  "Release inactive CLI generation \`${generation}\` to Ready; preserve the same generation evidence and branch." \
  "$payload")"
if wait_control_reaction "$comment_id"; then
  manifest_update "$manifest_file" '.state="released" | .recoveredAt=$recoveredAt' --arg recoveredAt "$(now_iso)"
  printf '{"outcome":"RELEASE_REQUIRED_APPLIED","generation":"%s"}\n' "$generation"
else
  printf '{"outcome":"RELEASE_REQUIRED_CONFLICT","generation":"%s"}\n' "$generation"
fi
