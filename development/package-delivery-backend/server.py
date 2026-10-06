#!/usr/bin/env python3
"""Loopback-only GoreeCloud App Store Development package-delivery backend."""

from __future__ import annotations

import argparse
import hashlib
import hmac
import json
import os
import re
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
ITEM_ID_RE = re.compile(r"^[a-z0-9][a-z0-9._-]{1,127}$")
ARTIFACT_ID_RE = re.compile(r"^[a-z0-9][a-z0-9._-]{1,159}$")

# Mirrors only the current local Development identity fixtures. The client sends the opaque
# fixture subject; authorization-relevant audiences are resolved here rather than trusted from
# client-supplied audience headers.
DEVELOPMENT_IDENTITIES = {
    "dev:standard": {
        "authenticated": True,
        "audiences": frozenset({"audience:standard"}),
    },
    "dev:preview": {
        "authenticated": True,
        "audiences": frozenset({"audience:standard"}),
    },
    "dev:administrator": {
        "authenticated": True,
        "audiences": frozenset({"audience:standard", "audience:administrator"}),
    },
    "dev:developer": {
        "authenticated": True,
        "audiences": frozenset({"audience:standard", "audience:developer"}),
    },
    "dev:signed-out": {
        "authenticated": False,
        "audiences": frozenset(),
    },
}


@dataclass(frozen=True)
class Release:
    item_id: str
    artifact_id: str
    file_name: str
    package_name: str
    version_name: str
    version_code: int
    min_sdk: int
    release_channel: str
    sha256: str
    signer_sha256: str
    allowed_audiences: frozenset[str]
    size_bytes: int
    file_path: Path

    def public_payload(self) -> dict:
        return {
            "schemaVersion": 1,
            "environment": "development-local",
            "itemId": self.item_id,
            "artifactId": self.artifact_id,
            "packageName": self.package_name,
            "versionName": self.version_name,
            "versionCode": self.version_code,
            "minSdk": self.min_sdk,
            "releaseChannel": self.release_channel,
            "sha256": self.sha256,
            "signerSha256": self.signer_sha256,
            "sizeBytes": self.size_bytes,
            "artifactPath": f"/v1/artifacts/{self.artifact_id}",
            "authority": "development-local-only",
        }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _require_string(entry: dict, key: str) -> str:
    value = entry.get(key)
    if not isinstance(value, str) or not value:
        raise ValueError(f"{key} must be a non-empty string")
    return value


def load_catalog(catalog_path: Path, package_root: Path) -> tuple[dict[str, Release], dict[str, Release]]:
    raw = json.loads(catalog_path.read_text(encoding="utf-8"))
    if raw.get("schemaVersion") != 1:
        raise ValueError("catalog schemaVersion must be 1")
    if raw.get("environment") != "development-local":
        raise ValueError("catalog environment must be development-local")

    by_item: dict[str, Release] = {}
    by_artifact: dict[str, Release] = {}
    root = package_root.resolve()

    for entry in raw.get("items", []):
        item_id = _require_string(entry, "itemId")
        artifact_id = _require_string(entry, "artifactId")
        file_name = _require_string(entry, "fileName")
        package_name = _require_string(entry, "packageName")
        version_name = _require_string(entry, "versionName")
        release_channel = _require_string(entry, "releaseChannel").lower()
        sha256 = _require_string(entry, "sha256").lower()
        signer_sha256 = _require_string(entry, "signerSha256").lower()
        audiences = entry.get("allowedAudiences")

        if not ITEM_ID_RE.fullmatch(item_id):
            raise ValueError(f"invalid itemId: {item_id}")
        if not ARTIFACT_ID_RE.fullmatch(artifact_id):
            raise ValueError(f"invalid artifactId: {artifact_id}")
        if Path(file_name).name != file_name:
            raise ValueError(f"fileName must be a basename: {file_name}")
        if not SHA256_RE.fullmatch(sha256) or not SHA256_RE.fullmatch(signer_sha256):
            raise ValueError(f"invalid SHA-256 identity for {artifact_id}")
        if release_channel not in {"development", "debug"}:
            raise ValueError("development backend only permits development/debug channels")
        if not isinstance(audiences, list) or not audiences or not all(isinstance(v, str) and v for v in audiences):
            raise ValueError(f"allowedAudiences must be a non-empty string list for {artifact_id}")

        version_code = entry.get("versionCode")
        min_sdk = entry.get("minSdk")
        if not isinstance(version_code, int) or version_code <= 0:
            raise ValueError(f"versionCode must be positive for {artifact_id}")
        if not isinstance(min_sdk, int) or min_sdk <= 0:
            raise ValueError(f"minSdk must be positive for {artifact_id}")

        file_path = (root / file_name).resolve()
        if file_path.parent != root:
            raise ValueError(f"artifact escaped package root: {file_name}")
        if not file_path.is_file():
            raise ValueError(f"artifact file missing: {file_path}")
        actual_sha256 = sha256_file(file_path)
        if not hmac.compare_digest(actual_sha256, sha256):
            raise ValueError(f"artifact digest mismatch for {artifact_id}")

        release = Release(
            item_id=item_id,
            artifact_id=artifact_id,
            file_name=file_name,
            package_name=package_name,
            version_name=version_name,
            version_code=version_code,
            min_sdk=min_sdk,
            release_channel=release_channel,
            sha256=sha256,
            signer_sha256=signer_sha256,
            allowed_audiences=frozenset(audiences),
            size_bytes=file_path.stat().st_size,
            file_path=file_path,
        )
        if item_id in by_item or artifact_id in by_artifact:
            raise ValueError("duplicate itemId or artifactId")
        by_item[item_id] = release
        by_artifact[artifact_id] = release

    return by_item, by_artifact


class DevelopmentDeliveryServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(
        self,
        address: tuple[str, int],
        handler,
        *,
        token: str,
        by_item: dict[str, Release],
        by_artifact: dict[str, Release],
    ) -> None:
        super().__init__(address, handler)
        self.token = token
        self.by_item = by_item
        self.by_artifact = by_artifact


class Handler(BaseHTTPRequestHandler):
    server: DevelopmentDeliveryServer
    protocol_version = "HTTP/1.1"

    def log_message(self, _format: str, *_args) -> None:
        # Do not persist device IPs, identity subjects, audience claims, or download history.
        return

    def _json(self, status: HTTPStatus, payload: dict) -> None:
        body = json.dumps(payload, separators=(",", ":"), sort_keys=True).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def _authorized_release(self, release: Release) -> bool:
        auth = self.headers.get("Authorization", "")
        expected = f"Bearer {self.server.token}"
        if not hmac.compare_digest(auth, expected):
            self._json(HTTPStatus.UNAUTHORIZED, {"error": "development_backend_auth_required"})
            return False

        subject = self.headers.get("X-GoreeCloud-Development-Subject", "").strip()
        identity = DEVELOPMENT_IDENTITIES.get(subject)
        if identity is None or not identity["authenticated"]:
            self._json(HTTPStatus.FORBIDDEN, {"error": "development_identity_not_authorized"})
            return False
        if release.allowed_audiences.isdisjoint(identity["audiences"]):
            self._json(HTTPStatus.FORBIDDEN, {"error": "development_catalog_not_authorized"})
            return False
        return True

    def do_GET(self) -> None:
        path = unquote(urlparse(self.path).path)
        if path == "/v1/health":
            self._json(
                HTTPStatus.OK,
                {
                    "status": "ok",
                    "environment": "development-local",
                    "artifactCount": len(self.server.by_artifact),
                },
            )
            return

        if path.startswith("/v1/items/"):
            item_id = path.removeprefix("/v1/items/")
            release = self.server.by_item.get(item_id)
            if release is None:
                self._json(HTTPStatus.NOT_FOUND, {"error": "item_not_available"})
                return
            if not self._authorized_release(release):
                return
            self._json(HTTPStatus.OK, release.public_payload())
            return

        if path.startswith("/v1/artifacts/"):
            artifact_id = path.removeprefix("/v1/artifacts/")
            release = self.server.by_artifact.get(artifact_id)
            if release is None:
                self._json(HTTPStatus.NOT_FOUND, {"error": "artifact_not_available"})
                return
            if not self._authorized_release(release):
                return
            if not hmac.compare_digest(sha256_file(release.file_path), release.sha256):
                self._json(HTTPStatus.CONFLICT, {"error": "artifact_integrity_changed"})
                return
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Length", str(release.size_bytes))
            self.send_header("Content-Disposition", f'attachment; filename="{release.file_name}"')
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            with release.file_path.open("rb") as handle:
                for chunk in iter(lambda: handle.read(256 * 1024), b""):
                    self.wfile.write(chunk)
            return

        self._json(HTTPStatus.NOT_FOUND, {"error": "not_found"})


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default=os.environ.get("GOREECLOUD_APP_STORE_DEV_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("GOREECLOUD_APP_STORE_DEV_PORT", "47831")))
    parser.add_argument(
        "--catalog",
        type=Path,
        default=Path(os.environ.get("GOREECLOUD_APP_STORE_DEV_CATALOG", "development/package-delivery-backend/catalog.json")),
    )
    parser.add_argument(
        "--package-root",
        type=Path,
        default=Path(os.environ.get("GOREECLOUD_APP_STORE_PACKAGE_ROOT", ".dev-packages")),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.host not in {"127.0.0.1", "::1", "localhost"}:
        raise SystemExit("Development backend is intentionally loopback-only")
    token = os.environ.get("GOREECLOUD_APP_STORE_DEV_TOKEN", "")
    if len(token) < 32:
        raise SystemExit("GOREECLOUD_APP_STORE_DEV_TOKEN must contain at least 32 characters")

    by_item, by_artifact = load_catalog(args.catalog, args.package_root)
    server = DevelopmentDeliveryServer(
        (args.host, args.port),
        Handler,
        token=token,
        by_item=by_item,
        by_artifact=by_artifact,
    )
    print(
        json.dumps(
            {
                "status": "listening",
                "host": args.host,
                "port": args.port,
                "artifactCount": len(by_artifact),
            },
            separators=(",", ":"),
        ),
        flush=True,
    )
    server.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
