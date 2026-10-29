#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PYTHONPATH="$ROOT/linux${PYTHONPATH:+:$PYTHONPATH}" exec python3 -m goreecloud_store
