#!/usr/bin/env python3
"""Validate curated product identity and platform declarations.

A catalog entry is not evidence that an installable release exists.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "shared/catalog.json"
REPO = re.compile(r"^[A-Za-z0-9_.-]+$")
APP_ID = re.compile(r"^[a-z][a-z0-9_.-]*$")
ANDROID_PKG = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+$")

def validate(obj):
    assert obj.get("schema_version") == 1, "unsupported schema"
    assert obj.get("owner") == "GoreeCloud", "untrusted owner"
    assert isinstance(obj.get("products"), list), "products must be a list"
    ids = set()
    for p in obj["products"]:
        assert isinstance(p, dict), "product must be object"
        assert isinstance(p.get("id"), str) and APP_ID.fullmatch(p["id"]), "invalid product ID"
        assert p["id"] not in ids, "duplicate product"
        ids.add(p["id"])
        assert isinstance(p.get("repo"), str) and REPO.fullmatch(p["repo"]) and p["repo"] not in (".", ".."), "invalid repo"
        assert isinstance(p.get("name"), str) and 1 <= len(p["name"]) <= 100, "invalid name"
        assert isinstance(p.get("summary"), str) and 1 <= len(p["summary"]) <= 200, "invalid summary"
        assert p.get("kind") in ("application", "service"), "invalid product kind"
        platforms = p.get("platforms")
        assert isinstance(platforms, list) and len(platforms) > 0
        assert len(platforms) == len(set(platforms)) and set(platforms) <= {"android", "linux"}
        if "android_package" in p:
            assert ANDROID_PKG.fullmatch(p["android_package"]), "invalid Android identity"
        if "flatpak_id" in p:
            assert ANDROID_PKG.fullmatch(p["flatpak_id"]), "invalid Flatpak identity"
            assert "linux" in platforms, "Flatpak must target Linux"
    return len(ids)

def main():
    count = validate(json.loads(CATALOG.read_text(encoding="utf-8")))
    print(f"Valid GoreeCloud catalog: {count} curated products")

if __name__ == "__main__":
    try:
        main()
    except (AssertionError, ValueError, KeyError) as exc:
        sys.exit(f"Catalog invalid: {exc}")
