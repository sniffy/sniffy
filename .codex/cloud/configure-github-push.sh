#!/usr/bin/env bash
set -euo pipefail

# Optional Codex Cloud GitHub push bootstrap.
#
# The fine-grained PAT must be configured as a Codex Cloud *secret* named
# SNIFFY_GITHUB_PAT. Codex exposes secrets only to the setup phase, so this
# script persists a credential for the later agent phase. This deliberately
# weakens the normal Cloud secret boundary and must only be enabled with the
# narrowly scoped, short-lived token documented in
# docs/codex-cloud-pat-push.md.

if [[ -z "${SNIFFY_GITHUB_PAT:-}" ]]; then
  echo "SNIFFY_GITHUB_PAT is not configured; GitHub push remains disabled."
  exit 0
fi

readonly repository="sniffy/sniffy"
readonly repository_url="https://github.com/${repository}.git"
readonly credential_dir="${HOME}/.config/git"
readonly credential_file="${credential_dir}/sniffy-codex-credentials"
readonly hooks_dir="${HOME}/.config/git/sniffy-codex-hooks"
readonly cloud_name="Sniffy Codex Cloud"
readonly cloud_email="codex-cloud@sniffy.invalid"

mkdir -p "${credential_dir}" "${hooks_dir}"
chmod 700 "${credential_dir}" "${hooks_dir}"

# Never place the token in the remote URL: `git remote -v`, logs, and task
# summaries must remain safe to display.
printf 'https://x-access-token:%s@github.com\n' "${SNIFFY_GITHUB_PAT}" > "${credential_file}"
chmod 600 "${credential_file}"

git config --global credential.helper "store --file=${credential_file}"
git config --global core.hooksPath "${hooks_dir}"
git remote set-url origin "${repository_url}"

# Give commits an explicit, non-human author/committer identity. The .invalid
# address is intentionally not associated with a GitHub account, avoiding any
# false claim that the commit was authored by Dmitry or a registered bot.
git config --global user.name "${cloud_name}"
git config --global user.email "${cloud_email}"
git config --global user.useConfigOnly true
git config --global commit.gpgsign false

cat > "${hooks_dir}/commit-msg" <<'EOF_COMMIT_MSG'
#!/usr/bin/env bash
set -euo pipefail

message_file="$1"

# Keep provenance stable while avoiding duplicate trailers when a commit is
# amended or the hook is invoked more than once.
if ! git interpret-trailers --parse "${message_file}" | grep -Fq 'Agent-Executor: Codex Cloud'; then
  git interpret-trailers --in-place \
    --trailer 'Agent-Executor: Codex Cloud' \
    --trailer 'Execution-Environment: OpenAI Codex Cloud' \
    --trailer 'Publication-Credential: bedrin fine-grained PAT' \
    "${message_file}"
fi
EOF_COMMIT_MSG
chmod 700 "${hooks_dir}/commit-msg"

cat > "${hooks_dir}/pre-push" <<'EOF_HOOK'
#!/usr/bin/env bash
set -euo pipefail

zero=0000000000000000000000000000000000000000

while read -r local_ref local_sha remote_ref remote_sha; do
  case "${remote_ref}" in
    refs/heads/develop|refs/heads/main|refs/heads/master)
      echo "Refusing direct push to protected/default branch ${remote_ref}." >&2
      exit 1
      ;;
  esac

  if [[ "${local_sha}" == "${zero}" ]]; then
    echo "Refusing branch deletion for ${remote_ref}." >&2
    exit 1
  fi

  if [[ "${remote_sha}" != "${zero}" ]] && ! git merge-base --is-ancestor "${remote_sha}" "${local_sha}"; then
    echo "Refusing non-fast-forward update for ${remote_ref}." >&2
    exit 1
  fi
done
EOF_HOOK
chmod 700 "${hooks_dir}/pre-push"

# Confirm that the credential can read the selected repository without
# printing the token or embedding it in process arguments.
git ls-remote --exit-code origin HEAD >/dev/null

unset SNIFFY_GITHUB_PAT

echo "Configured guarded GitHub push access for ${repository}."
echo "Git commit identity: ${cloud_name} <${cloud_email}>"
echo "GitHub push actor remains the PAT owner; see docs/codex-cloud-pat-push.md."
