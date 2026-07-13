#!/usr/bin/env bash

# Source this file to switch the current shell, for example:
#   source .codex/cloud/use-jdk.sh 8

feature="${1:-}"
case "${feature}" in
  8|11|17|21|25) ;;
  *)
    echo "Usage: source .codex/cloud/use-jdk.sh {8|11|17|21|25}" >&2
    return 2 2>/dev/null || exit 2
    ;;
esac

jdk_home="${HOME}/.jdks/temurin-${feature}"
if [[ ! -x "${jdk_home}/bin/java" ]]; then
  echo "JDK ${feature} is not installed. Run bash .codex/cloud/setup.sh first." >&2
  return 1 2>/dev/null || exit 1
fi

export JAVA_HOME="${jdk_home}"
export PATH="${JAVA_HOME}/bin:${PATH}"
java -version
