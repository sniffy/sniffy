#!/usr/bin/env bash
set -euo pipefail
# Repository-owned Codex CLI dispatcher. It performs one Project query and launches at most one generation.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=cli-common.sh
source "$SCRIPT_DIR/cli-common.sh"

for command_name in git gh jq node flock systemctl sha256sum; do require_command "$command_name"; done
gh auth status >/dev/null
mkdir -p "$STATE_ROOT/generations" "$WORKTREE_ROOT"
exec 9>"$STATE_ROOT/dispatcher.lock"
flock -n 9 || { echo '{"outcome":"ALREADY_RUNNING"}'; exit 0; }

snapshot="$("$SCRIPT_DIR/project-queue-snapshot.sh")"
generated_at="$(jq -r '.generatedAt' <<<"$snapshot")"
owned_count="$(jq '.ownedInProgress | length' <<<"$snapshot")"
stale_count="$(jq '.staleOwnedInProgress | length' <<<"$snapshot")"

mapfile -t owned_cli_generations < <(
  jq -r '.ownedInProgress[]?.workerReference // empty' <<<"$snapshot" \
    | sed -n 's/.*adapter=codex-cli-v1;.*generation=\([^;]*\).*/\1/p'
)
if (( ${#owned_cli_generations[@]} > 1 )); then
  printf '{"outcome":"MULTIPLE_CLI_GENERATIONS","count":%s}\n' "${#owned_cli_generations[@]}"
  exit 1
fi
if (( ${#owned_cli_generations[@]} == 1 )); then
  generation="${owned_cli_generations[0]}"
  if [[ -d "$(generation_dir "$generation")" ]]; then
    exec "$SCRIPT_DIR/cli-recover-worker.sh" "$generation"
  fi
  printf '{"outcome":"ORPHANED_CLI_OWNERSHIP","generation":"%s"}\n' "$generation"
  exit 1
fi

if (( stale_count > 0 )); then
  stale_reference="$(jq -r '.staleOwnedInProgress[0].workerReference // ""' <<<"$snapshot")"
  generation="$(sed -n 's/.*generation=\([^;]*\).*/\1/p' <<<"$stale_reference")"
  if [[ -n "$generation" && -d "$(generation_dir "$generation")" ]]; then
    exec "$SCRIPT_DIR/cli-recover-worker.sh" "$generation"
  fi
  printf '{"outcome":"STALE_EXTERNAL_OWNERSHIP","generatedAt":"%s"}\n' "$generated_at"
  exit 0
fi

if (( owned_count > 0 )); then
  printf '{"outcome":"CAPACITY_FULL","generatedAt":"%s","owned":%s}\n' "$generated_at" "$owned_count"
  exit 0
fi

candidate="$(jq -c '.readyCandidates[0] // empty' <<<"$snapshot")"
if [[ -z "$candidate" ]]; then
  printf '{"outcome":"NO_CHANGE","generatedAt":"%s"}\n' "$generated_at"
  exit 0
fi

type="$(jq -r '.type' <<<"$candidate")"
number="$(jq -r '.number' <<<"$candidate")"
status="$(jq -r '.status' <<<"$candidate")"
expected_reference="$(jq -r '.workerReference // ""' <<<"$candidate")"
[[ "$type" == Issue || "$type" == PullRequest ]] || { echo "Unsupported candidate type." >&2; exit 1; }
[[ "$status" == Implementation || "$status" == Verification ]] || { echo "Unsupported candidate status." >&2; exit 1; }

git fetch origin "$BASE_BRANCH"
base_sha="$(git rev-parse "origin/$BASE_BRANCH")"
work_mode="fresh"
continuation_branch=""
exact_head=""
body=""

if [[ "$type" == PullRequest ]]; then
  pr_json="$(gh pr view "$number" --repo "$REPOSITORY" --json state,isDraft,baseRefName,headRefName,headRefOid,body)"
  [[ "$(jq -r .state <<<"$pr_json")" == OPEN ]] || { echo "Selected PR is not open." >&2; exit 1; }
  [[ "$(jq -r .isDraft <<<"$pr_json")" == false ]] || { echo "Selected PR is draft." >&2; exit 1; }
  [[ "$(jq -r .baseRefName <<<"$pr_json")" == "$BASE_BRANCH" ]] || { echo "Selected PR targets another base." >&2; exit 1; }
  continuation_branch="$(jq -r .headRefName <<<"$pr_json")"
  exact_head="$(jq -r .headRefOid <<<"$pr_json")"
  body="$(jq -r '.body // ""' <<<"$pr_json")"
  work_mode="continuation"
else
  issue_json="$(gh issue view "$number" --repo "$REPOSITORY" --json state,title,body,url)"
  [[ "$(jq -r .state <<<"$issue_json")" == OPEN ]] || { echo "Selected issue is not open." >&2; exit 1; }
  body="$(jq -r '.body // ""' <<<"$issue_json")"
fi

fallback_branch="agent/issue-${number}"
branch_input="$(mktemp)"
trap 'rm -f "$branch_input"' EXIT
jq -n --arg body "$body" --arg continuationBranch "$continuation_branch" --arg fallbackBranch "$fallback_branch" \
  '{body:$body, continuationBranch:(if $continuationBranch == "" then null else $continuationBranch end), fallbackBranch:$fallbackBranch}' >"$branch_input"
branch_json="$(node "$SCRIPT_DIR/cli-worker-state.js" branch "$branch_input")"
branch="$(jq -r .branch <<<"$branch_json")"
branch_source="$(jq -r .source <<<"$branch_json")"

if [[ "$type" == Issue && "$work_mode" == fresh ]]; then
  matching_prs="$(gh pr list --repo "$REPOSITORY" --state open --head "$branch" \
    --json number,isDraft,baseRefName,headRefName,headRefOid,url)"
  matching_count="$(jq 'length' <<<"$matching_prs")"
  if (( matching_count > 1 )); then
    echo "More than one open PR uses authoritative branch $branch." >&2
    exit 1
  elif (( matching_count == 1 )); then
    [[ "$(jq -r '.[0].baseRefName' <<<"$matching_prs")" == "$BASE_BRANCH" ]] || {
      echo "Existing PR on authoritative branch targets another base." >&2
      exit 1
    }
    continuation_branch="$branch"
    exact_head="$(jq -r '.[0].headRefOid' <<<"$matching_prs")"
    work_mode="continuation"
    branch_source="existing-pr-for-authoritative-branch"
  else
    remote_head="$(git ls-remote --heads origin "refs/heads/$branch" | awk 'NR==1 {print $1}')"
    if [[ -n "$remote_head" ]]; then
      continuation_branch="$branch"
      exact_head="$remote_head"
      work_mode="continuation"
      branch_source="existing-remote-authoritative-branch"
    fi
  fi
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ | tr '[:upper:]' '[:lower:]')"
generation="$(tr '[:upper:]' '[:lower:]' <<<"${status}-${number}-${timestamp}")"
token="$(printf '%s' "${generation}-$$-${RANDOM}" | sha256sum | cut -c1-32)"
claimed_at="$(now_iso)"
provisional_lease="$(future_iso "$PROVISIONAL_LEASE_SECONDS")"
generation_path="$(generation_dir "$generation")"
mkdir "$generation_path"
chmod 700 "$generation_path"

manifest_file="$generation_path/manifest.json"
spawning_reference="v=2;state=spawning;adapter=codex-cli-v1;generation=${generation};token=${token};branch=${branch};leaseUntil=${provisional_lease}"
jq -n \
  --arg generation "$generation" --arg token "$token" --arg type "$type" --argjson number "$number" \
  --arg status "$status" --arg mode "$work_mode" --arg branch "$branch" --arg branchSource "$branch_source" \
  --arg baseSha "$base_sha" --arg exactHead "$exact_head" --arg claimedAt "$claimed_at" \
  --arg provisionalLease "$provisional_lease" --arg spawningReference "$spawning_reference" \
  --arg expectedReference "$expected_reference" \
  '{generation:$generation,token:$token,type:$type,number:$number,status:$status,mode:$mode,branch:$branch,
    branchSource:$branchSource,baseSha:$baseSha,
    exactHead:(if $exactHead == "" then null else $exactHead end),claimedAt:$claimedAt,
    provisionalLeaseUntil:$provisionalLease,spawningReference:$spawningReference,
    expectedWorkerReference:(if $expectedReference == "" then null else $expectedReference end),state:"spawning"}
  | if $exactHead == "" then del(.exactHead) else . end' \
  | atomic_json "$manifest_file"

payload="$generation_path/claim.json"
jq -n \
  --arg type "$type" --argjson number "$number" --arg status "$status" \
  --arg expectedReference "$expected_reference" --arg spawningReference "$spawning_reference" --arg head "$exact_head" \
  '{
    command:"delivery-control/v1",
    target:{repository:"sniffy/sniffy",number:$number},
    expected:{
      type:$type,
      fields:{
        Status:$status,
        Execution:"Ready",
        Executor:"Local Codex",
        "Worker reference":($expectedReference | if length == 0 then null else . end)
      }
    },
    set:{
      Execution:"In progress",
      "Worker reference":$spawningReference
    },
    addIfMissing:false
  }
  | if $type == "PullRequest" then .expected.head=$head else . end' >"$payload"

comment_id="$(post_control_command \
  "Claim \`${REPOSITORY}#${number}\` for CLI generation \`${generation}\` on branch \`${branch}\`." \
  "$payload")"
if ! wait_control_reaction "$comment_id"; then
  rm -rf "$generation_path"
  echo '{"outcome":"CLAIM_REJECTED"}'
  exit 0
fi

issue_markdown="$(if [[ "$type" == Issue ]]; then gh issue view "$number" --repo "$REPOSITORY" --json title,body,url --template '{{.title}}\n\n{{.body}}\n\nCanonical: {{.url}}'; else gh pr view "$number" --repo "$REPOSITORY" --json title,body,url --template '{{.title}}\n\n{{.body}}\n\nCanonical: {{.url}}'; fi)"
cat >"$generation_path/prompt.md" <<EOF
Perform one autonomous ${status} lifecycle turn for canonical ${type} ${REPOSITORY}#${number}.

Claim token: ${token}
Generation: ${generation}
Adapter: codex-cli-v1
Mode: ${work_mode}
Base branch: ${BASE_BRANCH}
Base SHA: ${base_sha}
Branch: ${branch}
Exact existing PR head: ${exact_head:-none}
Lease until: ${provisional_lease}

Read AGENTS.md, nearest nested AGENTS.md files, docs/ai-delivery/runtime-contract.md,
docs/ai-delivery/executors/codex-local.md, and .codex/local/worker-task-prompt.md before mutation.
Verify this exact generation still owns Project state. Apply the lifecycle template, preserve the exact authoritative branch,
perform the guarded lifecycle handoff yourself, and never merge, enable auto-merge, reset, rebase, or force-push.

${issue_markdown}
EOF

if ! systemctl --user start "sniffy-local-worker@${generation}.service"; then
  manifest_update "$manifest_file" \
    '.state="launch-failed" | .launchFailedAt=$failedAt | .failureReason="systemd-start-failed"' \
    --arg failedAt "$(now_iso)"
  jq -n --arg generation "$generation" --arg failedAt "$(now_iso)" \
    '{generation:$generation,state:"launch-failed",failedAt:$failedAt,reason:"systemd start failed"}' \
    | atomic_json "$generation_path/launch-failed.json"
  exec "$SCRIPT_DIR/cli-recover-worker.sh" "$generation"
fi

deadline=$((SECONDS + SPAWN_TIMEOUT_SECONDS))
while (( SECONDS < deadline )); do
  if [[ -s "$generation_path/ack.json" ]]; then
    thread_id="$(jq -r .threadId "$generation_path/ack.json")"
    normal_lease="$(future_iso "$NORMAL_LEASE_SECONDS")"
    running_reference="$(node "$SCRIPT_DIR/cli-worker-state.js" reference "$manifest_file" running "$normal_lease" "$thread_id")"
    update_payload="$generation_path/running.json"
    jq -n --arg type "$type" --argjson number "$number" --arg status "$status" \
      --arg spawningReference "$spawning_reference" --arg runningReference "$running_reference" --arg head "$exact_head" \
      '{
        command:"delivery-control/v1",
        target:{repository:"sniffy/sniffy",number:$number},
        expected:{type:$type,fields:{Status:$status,Execution:"In progress",Executor:"Local Codex","Worker reference":$spawningReference}},
        set:{"Worker reference":$runningReference},
        addIfMissing:false
      } | if $type == "PullRequest" then .expected.head=$head else . end' >"$update_payload"
    update_comment="$(post_control_command \
      "Confirm CLI worker \`${generation}\` after \`thread.started\` and \`turn.started\`." \
      "$update_payload")"
    if wait_control_reaction "$update_comment"; then
      manifest_update "$manifest_file" \
        '.projectReferenceState="running" | .runningReference=$reference | .leaseUntil=$lease | .projectAcknowledgedAt=$acknowledgedAt' \
        --arg reference "$running_reference" --arg lease "$normal_lease" --arg acknowledgedAt "$(now_iso)"
      printf '{"outcome":"DISPATCHED","generation":"%s","thread":"%s","branch":"%s"}\n' "$generation" "$thread_id" "$branch"
      exit 0
    fi
    printf '{"outcome":"ACK_PROJECT_CONFLICT","generation":"%s"}\n' "$generation"
    exit 0
  fi
  if [[ -s "$generation_path/launch-failed.json" ]]; then
    exec "$SCRIPT_DIR/cli-recover-worker.sh" "$generation"
  fi
  sleep 2
done

printf '{"outcome":"SPAWN_UNCERTAIN","generation":"%s","leaseUntil":"%s"}\n' "$generation" "$provisional_lease"
