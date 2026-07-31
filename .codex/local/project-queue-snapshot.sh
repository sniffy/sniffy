#!/usr/bin/env bash
set -euo pipefail

PROJECT_OWNER="${AI_DELIVERY_PROJECT_OWNER:-sniffy}"
PROJECT_NUMBER="${AI_DELIVERY_PROJECT_NUMBER:-2}"
REPOSITORY="${AI_DELIVERY_REPOSITORY:-sniffy/sniffy}"
LOCAL_ASSIGNEE="${AI_DELIVERY_LOCAL_ASSIGNEE:-bedrin-codex-local}"
INPUT_FILE=""

usage() {
  cat <<'EOF'
Usage: project-queue-snapshot.sh [--input FILE]

Without --input, fetches the GitHub Project exactly once through
`gh project item-list`, then emits one normalized JSON snapshot for Local Codex
dispatch. --input normalizes an existing raw snapshot for tests or inspection.
EOF
}

while (($#)); do
  case "$1" in
    --input)
      [[ $# -ge 2 ]] || { echo "--input requires a file" >&2; exit 2; }
      INPUT_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

raw="$(mktemp)"
err="$(mktemp)"
trap 'rm -f "$raw" "$err"' EXIT

if [[ -n "$INPUT_FILE" ]]; then
  [[ -r "$INPUT_FILE" ]] || { echo "Cannot read snapshot: $INPUT_FILE" >&2; exit 2; }
  cp "$INPUT_FILE" "$raw"
else
  command -v gh >/dev/null || { echo "gh is required" >&2; exit 2; }
  if ! gh project item-list "$PROJECT_NUMBER" --owner "$PROJECT_OWNER" --limit 1000 --format json >"$raw" 2>"$err"; then
    cat "$err" >&2
    if grep -Eqi 'rate.?limit|API rate limit|was submitted too quickly' "$err"; then
      exit 75
    fi
    exit 1
  fi
fi

command -v jq >/dev/null || { echo "jq is required" >&2; exit 2; }

jq -ce \
  --arg repository "$REPOSITORY" \
  --arg localAssignee "$LOCAL_ASSIGNEE" '
  def repository_name:
    if (.content.repository | type) == "string" then .content.repository
    elif (.content.repository | type) == "object" then
      (.content.repository.nameWithOwner // .content.repository.fullName // .content.repository.name // null)
    else (.repository // null)
    end;

  def assignee_logins:
    [(.assignees // [])[] |
      if type == "string" then .
      elif type == "object" then (.login // .name // empty)
      else empty
      end];

  def normalized:
    {
      id,
      type: (.content.type // .type // null),
      number: (.content.number // .number // null),
      title: (.title // .content.title // null),
      url: (.content.url // .url // null),
      repository: repository_name,
      status: (.status // null),
      execution: (.execution // null),
      executor: (.executor // null),
      implementer: (.implementer // null),
      verifier: (.verifier // null),
      priority: (.priority // null),
      readyTimestamp: (."ready timestamp" // .readyAt // null),
      assignees: assignee_logins,
      workerReference: (."worker reference" // null),
      linkedPullRequests: (."linked pull requests" // [])
    };

  def pool_eligible:
    (.assignees | length == 0) or (.assignees | index($localAssignee) != null);

  if (.items | type) != "array" then error("Project snapshot has no items array") else . end
  | ([.items[] | normalized | select(.repository == $repository)]) as $items
  | {
      totalCount: (.totalCount // (.items | length)),
      repositoryItemCount: ($items | length),
      ownedInProgress: [
        $items[]
        | select(.execution == "In progress" and .executor == "Local Codex")
      ] | sort_by(.number, .type),
      readyCandidates: [
        $items[]
        | select(
            .execution == "Ready" and
            .executor == "Local Codex" and
            (.status == "Implementation" or .status == "Verification") and
            pool_eligible
          )
      ] | sort_by(
          (if .priority == "Urgent" then 0 elif .priority == "High" then 1 elif .priority == "Medium" then 2 elif .priority == "Low" then 3 else 4 end),
          (.readyTimestamp // "9999-12-31T23:59:59Z"),
          .repository,
          .number,
          .type
        )
    }
' "$raw"
