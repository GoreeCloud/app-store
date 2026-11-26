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
    record = next(a for a in load_catalog().apps if a.slug == "backup")
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
    record = next(a for a in load_catalog().apps if a.slug == "backup")
    with pytest.raises(ValidationError):
        StoreApp.model_validate({
            **record.model_dump(mode="json"),
            "status": "packages-available",
        })


@pytest.mark.parametrize("url", [
    "http://github.com/GoreeCloud/backup",
    "https://github.com/GoreeCloud/backup?x=1",
    "https://github.com/GoreeCloud/backup#x",
    "https://github.com.evil.example/GoreeCloud/backup",
    "https://user@github.com/GoreeCloud/backup",
])
def test_reject_unsafe_source_urls(url):
    record = next(a for a in load_catalog().apps if a.slug == "backup").model_dump(mode="json")
    with pytest.raises(ValidationError):
        StoreApp.model_validate({**record, "source_url": url})


def release_record():
    return dict(version="1.0.0", platform="android", kind="apk",
                download_url="https://github.com/GoreeCloud/backup/releases/download/v1/app.apk",
                sha256="a" * 64, size_bytes=12, published_at="2026-11-20")


@pytest.mark.parametrize("changes", [
    {"download_url": "http://github.com/GoreeCloud/backup/releases/download/v1/app.apk"},
    {"download_url": "https://github.com/GoreeCloud/backup/releases/download/v1/app.apk?x=1"},
    {"download_url": "https://github.com/GoreeCloud/backup/releases/download/v1/app.zip"},
    {"published_at": "2026-02-30"},
])
def test_reject_invalid_release_metadata(changes):
    with pytest.raises(ValidationError):
        Release.model_validate({**release_record(), **changes})


def test_hash_alone_never_enables_distribution():
    record = next(a for a in load_catalog().apps if a.slug == "backup").model_dump(mode="json")
    candidate = StoreApp.model_validate({**record, "status": "packages-available", "releases": [release_record()]})
    with pytest.raises(ValidationError, match="distribution disabled"):
        Catalog(catalog_version=1, apps=[candidate])
    with pytest.raises(ValidationError, match="repository must match"):
        StoreApp.model_validate({**record, "status": "packages-available", "releases": [
            {**release_record(), "download_url": "https://github.com/GoreeCloud/vault/releases/download/v1/app.apk"}
        ]})


def test_duplicate_json_keys_and_bounded_reads(tmp_path):
    path = tmp_path / "catalog.json"
    path.write_text('{"catalog_version":1,"catalog_version":1,"apps":[]}')
    with pytest.raises(ValueError, match="duplicate JSON"):
        load_catalog(path)
    path.write_bytes(b" " * 2_000_001)
    with pytest.raises(ValueError, match="exceeds"):
        load_catalog(path)


def test_readiness_fails_closed(monkeypatch):
    assert client.get("/readyz").json() == {"status": "ready"}
    def invalid():
        raise ValueError("private diagnostic")
    monkeypatch.setattr("goree_store.api.current_catalog", invalid)
    result = client.get("/readyz")
    assert result.status_code == 503
    assert result.json() == {"status": "unavailable"}
