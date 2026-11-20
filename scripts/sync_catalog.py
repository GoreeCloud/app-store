#!/usr/bin/env python3
"""Keep read-only native app catalog snapshots identical to the canonical catalog."""
from __future__ import annotations
import argparse
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "catalog/apps.json").read_bytes()
targets = [
    root / "apps/linux/catalog.json",
    root / "apps/android/app/src/main/assets/catalog.json",
]
parser = argparse.ArgumentParser()
parser.add_argument("--check", action="store_true")
args = parser.parse_args()
for path in targets:
    if args.check:
        if not path.is_file() or path.read_bytes() != source:
            raise SystemExit(f"Catalog snapshot stale: {path.relative_to(root)}")
        print(f"OK: {path.relative_to(root)}")
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(source)
        print(f"Synced: {path.relative_to(root)}")
