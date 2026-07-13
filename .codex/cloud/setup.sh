#!/usr/bin/env bash
set -euo pipefail

# Codex Cloud setup script for Sniffy.
# Configure the environment setup command as:
#   bash .codex/cloud/setup.sh

MAVEN_VERSION="${MAVEN_VERSION:-3.9.11}"
JDK_FEATURES="${SNIFFY_JDK_FEATURES:-8 11 17 21 25}"
TOOLS_DIR="${HOME}/.local/share/sniffy-codex"
JDKS_DIR="${HOME}/.jdks"
MAVEN_HOME_DIR="${TOOLS_DIR}/apache-maven-${MAVEN_VERSION}"
ENV_FILE="${HOME}/.sniffy-codex-env"

case "$(uname -m)" in
  x86_64) ADOPTIUM_ARCH="x64" ;;
  aarch64|arm64) ADOPTIUM_ARCH="aarch64" ;;
  *)
    echo "Unsupported architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

mkdir -p "${TOOLS_DIR}" "${JDKS_DIR}" "${HOME}/.m2"

for command_name in curl tar git find python3; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command '${command_name}' is unavailable." >&2
    exit 1
  fi
done

install_jdk() {
  local feature="$1"
  local destination="${JDKS_DIR}/temurin-${feature}"

  if [[ -x "${destination}/bin/java" ]]; then
    echo "Temurin JDK ${feature} already installed."
    return
  fi

  echo "Installing latest Temurin JDK ${feature} GA release..."
  local archive unpack extracted
  archive="$(mktemp)"
  unpack="$(mktemp -d)"

  curl --fail --location --retry 5 --retry-all-errors --retry-delay 2 \
    "https://api.adoptium.net/v3/binary/latest/${feature}/ga/linux/${ADOPTIUM_ARCH}/jdk/hotspot/normal/eclipse?project=jdk" \
    --output "${archive}"
  tar -xzf "${archive}" -C "${unpack}"

  extracted="$(find "${unpack}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
  if [[ -z "${extracted}" ]]; then
    echo "Could not locate extracted JDK ${feature}." >&2
    exit 1
  fi

  rm -rf "${destination}"
  mv "${extracted}" "${destination}"
  rm -rf "${archive}" "${unpack}"
}

for feature in ${JDK_FEATURES}; do
  install_jdk "${feature}"
done

if [[ ! -x "${MAVEN_HOME_DIR}/bin/mvn" ]]; then
  echo "Installing Apache Maven ${MAVEN_VERSION}..."
  archive="$(mktemp)"
  curl --fail --location --retry 5 --retry-all-errors --retry-delay 2 \
    "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    --output "${archive}"
  tar -xzf "${archive}" -C "${TOOLS_DIR}"
  rm -f "${archive}"
fi

cat > "${ENV_FILE}" <<EOF_ENV
export SNIFFY_JDK8_HOME="${JDKS_DIR}/temurin-8"
export SNIFFY_JDK11_HOME="${JDKS_DIR}/temurin-11"
export SNIFFY_JDK17_HOME="${JDKS_DIR}/temurin-17"
export SNIFFY_JDK21_HOME="${JDKS_DIR}/temurin-21"
export SNIFFY_JDK25_HOME="${JDKS_DIR}/temurin-25"
export JAVA_HOME="${JDKS_DIR}/temurin-25"
export MAVEN_HOME="${MAVEN_HOME_DIR}"
export PATH="${MAVEN_HOME_DIR}/bin:${JDKS_DIR}/temurin-25/bin:\${PATH}"
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
  <toolchain><type>jdk</type><provides><version>8</version><vendor>temurin</vendor></provides><configuration><jdkHome>${JDKS_DIR}/temurin-8</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>11</version><vendor>temurin</vendor></provides><configuration><jdkHome>${JDKS_DIR}/temurin-11</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>17</version><vendor>temurin</vendor></provides><configuration><jdkHome>${JDKS_DIR}/temurin-17</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>21</version><vendor>temurin</vendor></provides><configuration><jdkHome>${JDKS_DIR}/temurin-21</jdkHome></configuration></toolchain>
  <toolchain><type>jdk</type><provides><version>25</version><vendor>temurin</vendor></provides><configuration><jdkHome>${JDKS_DIR}/temurin-25</jdkHome></configuration></toolchain>
</toolchains>
EOF_TOOLCHAINS

bash .codex/cloud/configure-maven-proxy.sh

java -version
mvn -version

bash .codex/cloud/warm-maven-cache.sh

echo "Sniffy Codex Cloud environment is ready."
