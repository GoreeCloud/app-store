#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ROOT}/.dev-backend/runtime.env"
if [[ ! -f "${ENV_FILE}" ]]; then
  bash "${ROOT}/development/backend/bootstrap.sh"
fi
# shellcheck disable=SC1090
source "${ENV_FILE}"
cd "${ROOT}"
exec gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
