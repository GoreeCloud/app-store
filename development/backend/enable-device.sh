#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ADB="${ADB:-$(command -v adb || true)}"
if [[ -z "${ADB}" ]]; then
  echo "adb was not found." >&2
  exit 2
fi
DEVICE_COUNT="$("${ADB}" devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "${DEVICE_COUNT}" -ne 1 ]]; then
  echo "Exactly one authorized Android development device must be connected; found ${DEVICE_COUNT}." >&2
  exit 2
fi
"${ADB}" reverse tcp:8443 tcp:8443
echo "Android device loopback is now mapped to the Development package backend."
echo "Open GoreeCloud App Store Dev, switch to Developer demo, then open GoreeCloud Gallery."
