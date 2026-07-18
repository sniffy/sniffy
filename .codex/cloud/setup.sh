#!/usr/bin/env bash
set -euo pipefail

# Codex Cloud setup script for Sniffy.
# Keep the Maven and Java runtimes provided by codex-universal. They are part of
# the platform environment and may include network/TLS integration that a
# separately downloaded toolchain would not inherit. Sniffy only adds Java 8,
# which is not present in the universal image.

JDKS_DIR="${HOME}/.jdks"
JDK8_HOME="${JDKS_DIR}/temurin-8"
ENV_FILE="${HOME}/.sniffy-codex-env"
GH_VERSION="2.96.0"
NODE_VERSION="24.15.0"
GH_BIN_DIR="${HOME}/.local/bin"
GH_INSTALL_DIR="${HOME}/.local/share/gh/${GH_VERSION}"

case "$(uname -m)" in
  x86_64)
    ADOPTIUM_ARCH="x64"
    GH_ARCH="amd64"
    GH_SHA256="83d5c2ccad5498f58bf6368acb1ab32588cf43ab3a4b1c301bf36328b1c8bd60"
    ;;
  aarch64|arm64)
    ADOPTIUM_ARCH="aarch64"
    GH_ARCH="arm64"
    GH_SHA256="06f86ec7103d41993b76cd78072f43595c34aaa56506d971d9860e67140bf909"
    ;;
  *)
    echo "Unsupported architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

for command_name in curl tar find git install ln sha256sum mise mvn; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command '${command_name}' is unavailable in the Codex image." >&2
    exit 1
  fi
done

install_github_cli() {
  if [[ ! -x "${GH_INSTALL_DIR}/bin/gh" ]]; then
    echo "Installing GitHub CLI ${GH_VERSION}..."
    local archive asset extracted unpack
    asset="gh_${GH_VERSION}_linux_${GH_ARCH}.tar.gz"
    archive="$(mktemp)"
    unpack="$(mktemp -d)"
    trap 'rm -rf "${archive:-}" "${unpack:-}"' RETURN

    curl --fail --location --retry 5 --retry-all-errors --retry-delay 2 \
      "https://github.com/cli/cli/releases/download/v${GH_VERSION}/${asset}" \
      --output "${archive}"
    printf '%s  %s\n' "${GH_SHA256}" "${archive}" | sha256sum --check --status

    tar --no-same-owner -xzf "${archive}" -C "${unpack}"
    extracted="${unpack}/gh_${GH_VERSION}_linux_${GH_ARCH}/bin/gh"
    if [[ ! -x "${extracted}" ]]; then
      echo "Could not locate the extracted GitHub CLI binary." >&2
      exit 1
    fi

    install -d "${GH_INSTALL_DIR}/bin"
    install -m 0755 "${extracted}" "${GH_INSTALL_DIR}/bin/gh"
    rm -rf "${archive}" "${unpack}"
    trap - RETURN
  else
    echo "GitHub CLI ${GH_VERSION} already installed."
  fi

  install -d "${GH_BIN_DIR}"
  ln -sfn "${GH_INSTALL_DIR}/bin/gh" "${GH_BIN_DIR}/gh"
  export PATH="${GH_BIN_DIR}:${PATH}"
  gh --version
}

install_github_cli

configure_github_auth() {
  if [[ -z "${GH_TOKEN:-}" ]]; then
    echo "GH_TOKEN is not configured; GitHub push and PR operations will be unavailable."
    return
  fi

  if ! command -v gh >/dev/null 2>&1; then
    echo "GH_TOKEN is configured, but GitHub CLI (gh) is unavailable in the Codex image." >&2
    exit 1
  fi

  echo "Configuring persistent GitHub authentication for the agent phase..."
  local github_token="${GH_TOKEN}"
  unset GH_TOKEN

  printf '%s\n' "${github_token}" |
    gh auth login --hostname github.com --git-protocol https --with-token --insecure-storage
  unset github_token

  gh auth setup-git --hostname github.com

  local auth_login probe_branch push_error
  auth_login="$(gh api user --jq '.login')"
  probe_branch="agent/codex-auth-check-$(git rev-parse --short=12 HEAD)"

  if ! push_error="$(git push --dry-run --porcelain https://github.com/sniffy/sniffy.git "HEAD:refs/heads/${probe_branch}" 2>&1)"; then
    echo "GH_TOKEN authenticates as ${auth_login}, but setup could not negotiate a dry-run push to sniffy/sniffy:" >&2
    printf '%s\n' "${push_error}" >&2
    echo "The repository .permissions.push flag only describes the account role; token permissions can be narrower." >&2
    echo "For a fine-grained PAT, make the token owner a sniffy organization member and grant Contents: write for sniffy/sniffy." >&2
    echo "GitHub does not support fine-grained PAT contributions from outside or repository collaborators." >&2
    exit 1
  fi

  echo "Verified setup-phase Git push endpoint access with a non-mutating dry run as ${auth_login}."
  echo "Agent-phase publication also requires github.com and api.github.com plus write HTTP methods in the Codex environment internet policy."
  gh auth status --hostname github.com
}

configure_github_auth

mkdir -p "${JDKS_DIR}" "${HOME}/.m2"

install_node24() {
  if ! mise where "node@${NODE_VERSION}" >/dev/null 2>&1; then
    echo "Installing Node.js ${NODE_VERSION} for Sniffy frontend and Codex Cloud tooling..."
    mise install "node@${NODE_VERSION}"
  else
    echo "Node.js ${NODE_VERSION} already installed."
  fi
}

install_jdk8() {
  if [[ -x "${JDK8_HOME}/bin/java" ]]; then
    echo "Temurin JDK 8 already installed."
    return
  fi

  echo "Installing latest Temurin JDK 8 GA release..."
  local archive unpack extracted
  archive="$(mktemp)"
  unpack="$(mktemp -d)"
  trap 'rm -rf "${archive:-}" "${unpack:-}"' RETURN

  curl --fail --location --retry 5 --retry-all-errors --retry-delay 2 \
    "https://api.adoptium.net/v3/binary/latest/8/ga/linux/${ADOPTIUM_ARCH}/jdk/hotspot/normal/eclipse?project=jdk" \
    --output "${archive}"
  tar -xzf "${archive}" -C "${unpack}"

  extracted="$(find "${unpack}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [[ -z "${extracted}" ]]; then
    echo "Could not locate extracted JDK 8." >&2
    exit 1
  fi

  rm -rf "${JDK8_HOME}"
  mv "${extracted}" "${JDK8_HOME}"
  rm -rf "${archive}" "${unpack}"
  trap - RETURN
}

builtin_jdk_home() {
  local feature="$1"
  local home
  home="$(mise where "java@${feature}")"
  if [[ -z "${home}" || ! -x "${home}/bin/java" ]]; then
    echo "Codex image does not provide a usable JDK ${feature}." >&2
    exit 1
  fi
  printf '%s\n' "${home}"
}

install_node24
install_jdk8

NODE24_HOME="$(mise where "node@${NODE_VERSION}")"

JDK11_HOME="$(builtin_jdk_home 11)"
JDK17_HOME="$(builtin_jdk_home 17)"
JDK21_HOME="$(builtin_jdk_home 21)"
JDK25_HOME="$(builtin_jdk_home 25)"

cat > "${ENV_FILE}" <<EOF_ENV
export SNIFFY_JDK8_HOME="${JDK8_HOME}"
export SNIFFY_JDK11_HOME="${JDK11_HOME}"
export SNIFFY_JDK17_HOME="${JDK17_HOME}"
export SNIFFY_JDK21_HOME="${JDK21_HOME}"
export SNIFFY_JDK25_HOME="${JDK25_HOME}"
export JAVA_HOME="${JDK25_HOME}"
export PATH="${NODE24_HOME}/bin:${JDK25_HOME}/bin:${GH_BIN_DIR}:\${PATH}"
export NODE_USE_ENV_PROXY="1"
EOF_ENV

for shell_file in "${HOME}/.bashrc" "${HOME}/.profile"; do
  touch "${shell_file}"
  if ! grep -Fq '.sniffy-codex-env' "${shell_file}"; then
    cat >> "${shell_file}" <<'EOF_SHELL'

# Sniffy Codex Cloud toolchain
[ -f "$HOME/.sniffy-codex-env" ] && source "$HOME/.sniffy-codex-env"
EOF_SHELL
  fi
done

# shellcheck disable=SC1090
source "${ENV_FILE}"

cat > "${HOME}/.m2/toolchains.xml" <<EOF_TOOLCHAINS
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain><type>jdk</type><provides><version>8</version></provides><configuration><jdkHome>${JDK8_HOME}</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>11</version></provides><configuration><jdkHome>${JDK11_HOME}</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>17</version></provides><configuration><jdkHome>${JDK17_HOME}</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>21</version></provides><configuration><jdkHome>${JDK21_HOME}</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>25</version></provides><configuration><jdkHome>${JDK25_HOME}</jdkHome></configuration></toolchain>
</toolchains>
EOF_TOOLCHAINS

printf 'Using Codex-provided Maven: %s\n' "$(command -v mvn)"
java -version
mvn -version
node -v

bash .codex/cloud/warm-maven-cache.sh

echo "Sniffy Codex Cloud environment is ready."
