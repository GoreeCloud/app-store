#!/usr/bin/env python3
"""Validate the checked-in source catalog before a release can be reviewed."""
from __future__ import annotations

from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))
from goree_store.catalog import load_catalog

if __name__ == "__main__":
    data = load_catalog()
    print(f"Validated {len(data.apps)} app entries; {sum(len(a.releases) for a in data.apps)} artifacts")
