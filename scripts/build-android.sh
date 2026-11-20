#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 9.5.0 is required. Install a verified Gradle distribution or use Android Studio." >&2
  exit 2
fi
cd "$ROOT/apps/android"
gradle :app:assembleDebug --no-daemon
