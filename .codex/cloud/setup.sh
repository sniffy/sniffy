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

case "$(uname -m)" in
  x86_64) ADOPTIUM_ARCH="x64" ;;
  aarch64|arm64) ADOPTIUM_ARCH="aarch64" ;;
  *)
    echo "Unsupported architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

for command_name in curl tar find mise mvn; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command '${command_name}' is unavailable in the Codex image." >&2
    exit 1
  fi
done

mkdir -p "${JDKS_DIR}" "${HOME}/.m2"

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

install_jdk8

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
export PATH="${JDK25_HOME}/bin:\${PATH}"
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

bash .codex/cloud/warm-maven-cache.sh

echo "Sniffy Codex Cloud environment is ready."
