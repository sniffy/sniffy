#!/usr/bin/env bash
set -euo pipefail

# Run only inside an isolated VM or dev container. This grants Codex unrestricted
# access as the current user and disables interactive approval prompts.

issue_number="${1:-}"
branch_name="${2:-}"

if [[ -z "${issue_number}" || ! "${issue_number}" =~ ^[0-9]+$ ]]; then
  echo "Usage: bash .codex/local/run-issue.sh ISSUE_NUMBER [BRANCH_NAME]" >&2
  exit 2
fi

for command_name in git gh codex; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "Required command '${command_name}' is unavailable." >&2
    exit 1
  }
done

gh auth status >/dev/null

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Refusing to mix unrelated changes." >&2
  exit 1
fi

repo="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
default_branch="$(gh repo view --json defaultBranchRef --jq .defaultBranchRef.name)"
issue_url="$(gh issue view "${issue_number}" --repo "${repo}" --json url --jq .url)"

if [[ -z "${branch_name}" ]]; then
  branch_name="agent/issue-${issue_number}"
fi

git fetch origin "${default_branch}" "${branch_name}" 2>/dev/null || git fetch origin "${default_branch}"

if git show-ref --verify --quiet "refs/heads/${branch_name}"; then
  git switch "${branch_name}"
elif git show-ref --verify --quiet "refs/remotes/origin/${branch_name}"; then
  git switch --track "origin/${branch_name}"
else
  git switch -c "${branch_name}" "origin/${default_branch}"
fi

issue_markdown="$(gh issue view "${issue_number}" --repo "${repo}" --json title,body,url --template '{{.title}}\n\n{{.body}}\n\nIssue: {{.url}}')"

model="${CODEX_MODEL:-gpt-5.6-sol}"
reasoning="${CODEX_REASONING_EFFORT:-xhigh}"

codex \
  --model "${model}" \
  --config "model_reasoning_effort=\"${reasoning}\"" \
  --ask-for-approval never \
  --sandbox danger-full-access \
  --search \
  exec - <<PROMPT
Work autonomously on the GitHub issue below in repository ${repo}.

Read AGENTS.md and inspect the repository before editing. The issue is authoritative. Implement the task end to end, add or update tests and documentation, run all locally applicable checks, review the final diff, commit, push ${branch_name} without force-pushing, and create or update a draft pull request linked to the issue. Do not ask for routine decisions or approval. Make conservative maintainable implementation decisions yourself. Do not merge the pull request. Stop only for a genuine blocker and report it precisely.

${issue_markdown}
PROMPT

printf 'Codex run finished for %s on branch %s.\n' "${issue_url}" "${branch_name}"
