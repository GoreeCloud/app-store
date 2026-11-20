#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cmake -S "$ROOT/apps/linux" -B "$ROOT/build/linux" -DCMAKE_BUILD_TYPE=Release
cmake --build "$ROOT/build/linux" --parallel
