from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from goree_store.api import app
from goree_store.catalog import Catalog, Release, StoreApp, load_catalog

client = TestClient(app)


def test_manifest_is_valid_and_source_only():
    catalog = load_catalog()
    assert len(catalog.apps) >= 10
    assert len({a.slug for a in catalog.apps}) == len(catalog.apps)
    assert all(not a.releases and a.status == "source-only" for a in catalog.apps)


def test_search_and_filters():
    result = client.get("/api/v1/apps", params={"q": "notes", "platform": "linux"})
    assert result.status_code == 200
    assert [a["slug"] for a in result.json()["items"]] == ["notebook"]
    result = client.get("/api/v1/apps", params={"limit": 2, "offset": 2})
    assert result.status_code == 200
    assert len(result.json()["items"]) == 2
    assert result.json()["total"] >= 10


def test_not_found_and_invalid_input():
    assert client.get("/api/v1/apps/missing").status_code == 404
    assert client.get("/api/v1/apps?limit=1000").status_code == 422
    assert client.get("/api/v1/apps?platform=windows").status_code == 422
    assert client.get("/api/v1/meta").json()["package_downloads_enabled"] is False
    assert client.get("/healthz").json()["status"] == "ok"


def test_no_duplicate_slugs():
    record = load_catalog().apps[0]
    with pytest.raises(ValidationError):
        Catalog(catalog_version=1, apps=[record, record])


def test_unsigned_or_untrusted_artifacts_fail_closed():
    release = {
        "version": "1.0.0", "platform": "android", "kind": "apk",
        "download_url": "https://evil.example/app.apk",
        "sha256": "1" * 64, "size_bytes": 12, "published_at": "2026-11-20",
    }
    with pytest.raises(ValidationError):
        Release.model_validate(release)
    release["download_url"] = "https://github.com/GoreeCloud/app-store/releases/download/v1/app.apk"
    release["sha256"] = "not-a-hash"
    with pytest.raises(ValidationError):
        Release.model_validate(release)
    release["sha256"] = "a" * 64
    release["kind"] = "appimage"
    with pytest.raises(ValidationError):
        Release.model_validate(release)


def test_source_only_cannot_claim_packages():
    record = load_catalog().apps[0]
    with pytest.raises(ValidationError):
        StoreApp.model_validate({
            **record.model_dump(mode="json"),
            "status": "packages-available",
        })
