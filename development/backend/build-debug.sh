#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ROOT}/.dev-backend/runtime.env"
if [[ ! -f "${ENV_FILE}" ]]; then
  bash "${ROOT}/development/backend/bootstrap.sh"
fi
# shellcheck disable=SC1090
source "${ENV_FILE}"
GRADLE_BIN="${GRADLE:-$(command -v gradle || true)}"
if [[ -z "${GRADLE_BIN}" && -x "${HOME}/GoreeCloud-work/.tooling/gradle-9.5.0/bin/gradle" ]]; then
  GRADLE_BIN="${HOME}/GoreeCloud-work/.tooling/gradle-9.5.0/bin/gradle"
fi
if [[ -z "${GRADLE_BIN}" || ! -x "${GRADLE_BIN}" ]]; then
  echo "Gradle 9.5.0 (or compatible repository Gradle) was not found." >&2
  exit 2
fi
if [[ -z "${JAVA_HOME:-}" && -d "${HOME}/GoreeCloud-work/.tooling/temurin17" ]]; then
  export JAVA_HOME="${HOME}/GoreeCloud-work/.tooling/temurin17"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -d "${HOME}/GoreeCloud-work/.android-sdk" ]]; then
  export ANDROID_SDK_ROOT="${HOME}/GoreeCloud-work/.android-sdk"
fi
cd "${ROOT}"
exec "${GRADLE_BIN}" --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
