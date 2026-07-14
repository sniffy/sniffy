#!/usr/bin/env bash
set -euo pipefail

# Optional Codex Cloud GitHub push bootstrap.
#
# The fine-grained PAT must be configured as a Codex Cloud *secret* named
# SNIFFY_GITHUB_PAT. Codex exposes secrets only to the setup phase, so this
# script persists a credential for the later agent phase. This deliberately
# weakens the normal Cloud secret boundary and must only be enabled with the
# narrowly scoped, short-lived token documented in docs/codex-workflow.md.

if [[ -z "${SNIFFY_GITHUB_PAT:-}" ]]; then
  echo "SNIFFY_GITHUB_PAT is not configured; GitHub push remains disabled."
  exit 0
fi

readonly repository="sniffy/sniffy"
readonly repository_url="https://github.com/${repository}.git"
readonly credential_dir="${HOME}/.config/git"
readonly credential_file="${credential_dir}/sniffy-codex-credentials"
readonly hooks_dir="${HOME}/.config/git/sniffy-codex-hooks"

mkdir -p "${credential_dir}" "${hooks_dir}"
chmod 700 "${credential_dir}" "${hooks_dir}"

# Never place the token in the remote URL: `git remote -v`, logs, and task
# summaries must remain safe to display.
printf 'https://x-access-token:%s@github.com\n' "${SNIFFY_GITHUB_PAT}" > "${credential_file}"
chmod 600 "${credential_file}"

git config --global credential.helper "store --file=${credential_file}"
git config --global core.hooksPath "${hooks_dir}"
git remote set-url origin "${repository_url}"

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
