#!/usr/bin/env python3
"""Loopback-only GoreeCloud App Store Development package-delivery backend.

This service is intentionally Development-only. It re-authorizes the App Store's
fixture identities, validates the configured artifact digest on every delivery,
requires a live Wardveil/ClamAV clean result plus a controlled EICAR detection,
and serves only exact configured GoreeCloud Development artifacts.

It is not a production Identity, catalog, release, revocation, or Wardveil
authority. Production delivery must replace the Development fixtures and
loopback transport with accepted first-party services.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import secrets
import socket
import struct
import time
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
CONFIG_PATH = Path(__file__).resolve().with_name("release-catalog.json")
PACKAGE_ROOT = Path(os.environ.get("GORECLOUD_DEV_PACKAGE_ROOT", ROOT / ".dev-packages"))
HOST = os.environ.get("GORECLOUD_DEV_BACKEND_HOST", "127.0.0.1")
PORT = int(os.environ.get("GORECLOUD_DEV_BACKEND_PORT", "8791"))
SESSION_TTL_SECONDS = 10 * 60
SCAN_TTL_SECONDS = 10 * 60
MAX_JSON_BODY = 16 * 1024
EICAR = b"X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"

IDENTITIES: dict[str, tuple[str, ...]] = {
    "dev:standard": ("audience:standard", "channel:stable"),
    "dev:preview": (
        "audience:standard",
        "channel:stable",
        "channel:beta",
        "channel:rc",
    ),
    "dev:administrator": (
        "audience:standard",
        "audience:administrator",
        "channel:stable",
    ),
    "dev:developer": (
        "audience:standard",
        "audience:developer",
        "channel:stable",
        "channel:rc",
        "channel:beta",
        "channel:development",
        "channel:debug",
    ),
}


def _b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def _b64url_decode(value: str) -> bytes:
    return base64.urlsafe_b64decode(value + "=" * (-len(value) % 4))


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


@dataclass(frozen=True)
class Session:
    subject_id: str
    audiences: tuple[str, ...]
    expires_at: int


class TokenCodec:
    def __init__(self, secret: bytes):
        self._secret = secret

    def issue(self, subject_id: str, audiences: tuple[str, ...]) -> tuple[str, int]:
        now = int(time.time())
        expiry = now + SESSION_TTL_SECONDS
        payload = {
            "sub": subject_id,
            "aud": list(audiences),
            "iat": now,
            "exp": expiry,
            "nonce": secrets.token_hex(8),
            "environment": "development",
        }
        encoded = _b64url(json.dumps(payload, separators=(",", ":"), sort_keys=True).encode())
        signature = _b64url(hmac.new(self._secret, encoded.encode(), hashlib.sha256).digest())
        return f"{encoded}.{signature}", expiry

    def verify(self, token: str) -> Session:
        try:
            encoded, supplied = token.split(".", 1)
            expected = _b64url(hmac.new(self._secret, encoded.encode(), hashlib.sha256).digest())
            if not hmac.compare_digest(supplied, expected):
                raise ValueError("signature")
            payload = json.loads(_b64url_decode(encoded))
            subject_id = str(payload["sub"])
            expires_at = int(payload["exp"])
            audiences = tuple(str(value) for value in payload["aud"])
        except Exception as exc:
            raise ValueError("invalid development session token") from exc

        if expires_at <= int(time.time()):
            raise ValueError("expired development session token")
        if subject_id not in IDENTITIES or tuple(IDENTITIES[subject_id]) != audiences:
            raise ValueError("development session no longer authorized")
        return Session(subject_id=subject_id, audiences=audiences, expires_at=expires_at)


class WardveilDevelopmentScanner:
    """Minimal client for the loopback ClamAV engine documented by Wardveil."""

    def __init__(self) -> None:
        self.host = os.environ.get("WARDVEIL_CLAMAV_TCP_HOST", "127.0.0.1")
        self.port = int(os.environ.get("WARDVEIL_CLAMAV_PORT", "3310"))
        self.timeout = float(os.environ.get("WARDVEIL_CLAMAV_TIMEOUT_SECONDS", "30"))
        self._cache: dict[str, tuple[int, dict[str, Any]]] = {}

    def _command(self, command: str) -> str:
        with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
            sock.sendall(("z" + command + "\0").encode())
            return sock.recv(4096).decode(errors="replace").rstrip("\0\n")

    def _scan_chunks(self, chunks: Any) -> str:
        with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
            sock.sendall(b"zINSTREAM\0")
            for chunk in chunks:
                if chunk:
                    sock.sendall(struct.pack("!I", len(chunk)))
                    sock.sendall(chunk)
            sock.sendall(struct.pack("!I", 0))
            response = bytearray()
            while True:
                part = sock.recv(4096)
                if not part:
                    break
                response.extend(part)
                if b"\0" in response:
                    break
        return bytes(response).decode(errors="replace").rstrip("\0\n")

    def scan(self, artifact_path: Path, artifact_sha256: str) -> dict[str, Any]:
        now = int(time.time())
        cached = self._cache.get(artifact_sha256)
        if cached and now < cached[0]:
            return cached[1]

        try:
            version = self._command("VERSION")
            with artifact_path.open("rb") as handle:
                result = self._scan_chunks(iter(lambda: handle.read(256 * 1024), b""))
            control = self._scan_chunks([EICAR])
            clean = result.endswith(": OK")
            control_detected = "Eicar-Test-Signature FOUND" in control
            state = "clean" if clean and control_detected else "unknown"
            evidence = {
                "state": state,
                "engine": version,
                "artifactResult": result,
                "controlResult": control,
                "evaluatedAtEpochSeconds": now,
                "expiresAtEpochSeconds": now + SCAN_TTL_SECONDS,
                "scope": "development-owner-test",
            }
        except Exception as exc:
            evidence = {
                "state": "unknown",
                "errorType": exc.__class__.__name__,
                "evaluatedAtEpochSeconds": now,
                "expiresAtEpochSeconds": now + 30,
                "scope": "development-owner-test",
            }

        self._cache[artifact_sha256] = (evidence["expiresAtEpochSeconds"], evidence)
        return evidence


class ReleaseService:
    def __init__(self, catalog: dict[str, Any], scanner: WardveilDevelopmentScanner):
        self.catalog = {item["id"]: item for item in catalog["items"]}
        self.scanner = scanner
        self._validate_catalog_files()

    def _validate_catalog_files(self) -> None:
        for item in self.catalog.values():
            path = PACKAGE_ROOT / item["artifactFilename"]
            if not path.is_file():
                continue
            actual = _sha256_file(path)
            if actual != item["sha256"]:
                raise RuntimeError(
                    f"Configured artifact digest mismatch for {item['id']}: {actual}"
                )

    @staticmethod
    def _authorized(session: Session, item: dict[str, Any]) -> bool:
        required_audiences = set(item["access"]["anyAudience"])
        if required_audiences and not required_audiences.intersection(session.audiences):
            return False
        channel_claim = f"channel:{item['releaseChannel']}"
        return channel_claim in session.audiences

    def release(self, session: Session, item_id: str) -> dict[str, Any]:
        item = self.catalog.get(item_id)
        if item is None:
            raise KeyError(item_id)
        if not self._authorized(session, item):
            raise PermissionError(item_id)

        artifact_path = PACKAGE_ROOT / item["artifactFilename"]
        if not artifact_path.is_file():
            raise FileNotFoundError(item["artifactFilename"])
        actual_digest = _sha256_file(artifact_path)
        if actual_digest != item["sha256"]:
            raise RuntimeError("artifact digest changed")

        wardveil = self.scanner.scan(artifact_path, actual_digest)
        if wardveil.get("state") != "clean":
            raise RuntimeError("Wardveil Development scan is not accepted")

        now = int(time.time())
        expiry = min(now + SESSION_TTL_SECONDS, int(wardveil["expiresAtEpochSeconds"]))
        evidence_set_id = f"dev-{item_id}-{actual_digest[:16]}-{now}"
        base_record = {
            "state": "accepted",
            "producerId": "goreecloud.app-store.development-release-service",
            "authorityDomain": "development.app-store.delivery",
            "producerAuthority": "accepted",
            "subjectPackageName": item["packageName"],
            "artifactSha256": actual_digest,
            "evidenceSetId": evidence_set_id,
            "contractVersion": "development-package-delivery-v1",
            "createdAtEpochSeconds": now,
            "expiresAtEpochSeconds": expiry,
        }

        def record(kind: str, source: str) -> dict[str, Any]:
            return dict(base_record, type=kind, sourceReference=source)

        return {
            "environment": "development",
            "productionAcceptance": False,
            "itemId": item_id,
            "artifact": {
                "packageName": item["packageName"],
                "versionName": item["versionName"],
                "versionCode": item["versionCode"],
                "releaseChannel": item["releaseChannel"],
                "minSdk": item["minSdk"],
                "sha256": actual_digest,
                "signingCertificateSha256": item["signingCertificateSha256"],
                "sizeBytes": artifact_path.stat().st_size,
                "downloadPath": f"/v1/artifacts/{item_id}.apk",
            },
            "wardveil": wardveil,
            "releaseEvidence": {
                "buildProvenance": record("BUILD_PROVENANCE", item["evidence"]["buildProvenance"]),
                "sbom": record("SBOM", item["evidence"]["sbom"]),
                "releaseApproval": record("RELEASE_APPROVAL", item["evidence"]["releaseApproval"]),
                "revocationStatus": record("REVOCATION_STATUS", item["evidence"]["revocationStatus"]),
            },
        }

    def artifact_path(self, session: Session, item_id: str) -> Path:
        self.release(session, item_id)
        return PACKAGE_ROOT / self.catalog[item_id]["artifactFilename"]


class Handler(BaseHTTPRequestHandler):
    server_version = "GoreeCloudDevelopmentPackageDelivery/1"

    @property
    def app(self) -> "DevelopmentServer":
        return self.server  # type: ignore[return-value]

    def log_message(self, fmt: str, *args: Any) -> None:
        print(f"[development-package-delivery] {self.address_string()} {fmt % args}")

    def _headers(self, status: int, content_type: str, length: int | None = None) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-GoreeCloud-Environment", "development")
        if length is not None:
            self.send_header("Content-Length", str(length))
        self.end_headers()

    def _json(self, status: int, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, separators=(",", ":"), sort_keys=True).encode()
        self._headers(status, "application/json; charset=utf-8", len(body))
        self.wfile.write(body)

    def _read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_JSON_BODY:
            raise ValueError("invalid JSON body length")
        return json.loads(self.rfile.read(length))

    def _session(self) -> Session:
        header = self.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            raise ValueError("missing development session")
        return self.app.token_codec.verify(header.removeprefix("Bearer ").strip())

    def do_GET(self) -> None:  # noqa: N802
        route = urlparse(self.path).path
        if route == "/healthz":
            self._json(
                HTTPStatus.OK,
                {
                    "status": "development",
                    "productionAcceptance": False,
                    "artifactCount": len(self.app.release_service.catalog),
                    "packageRootPresent": PACKAGE_ROOT.is_dir(),
                },
            )
            return

        try:
            session = self._session()
        except ValueError as exc:
            self._json(HTTPStatus.UNAUTHORIZED, {"error": str(exc)})
            return

        if route.startswith("/v1/releases/"):
            item_id = route.removeprefix("/v1/releases/")
            try:
                self._json(HTTPStatus.OK, self.app.release_service.release(session, item_id))
            except KeyError:
                self._json(HTTPStatus.NOT_FOUND, {"error": "unknown item"})
            except PermissionError:
                self._json(HTTPStatus.FORBIDDEN, {"error": "release not authorized"})
            except FileNotFoundError:
                self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": "artifact unavailable"})
            except RuntimeError as exc:
                self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": str(exc)})
            return

        if route.startswith("/v1/artifacts/") and route.endswith(".apk"):
            item_id = route.removeprefix("/v1/artifacts/")[:-4]
            try:
                path = self.app.release_service.artifact_path(session, item_id)
            except KeyError:
                self._json(HTTPStatus.NOT_FOUND, {"error": "unknown item"})
                return
            except PermissionError:
                self._json(HTTPStatus.FORBIDDEN, {"error": "artifact not authorized"})
                return
            except (FileNotFoundError, RuntimeError) as exc:
                self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": str(exc)})
                return

            self._headers(
                HTTPStatus.OK,
                "application/vnd.android.package-archive",
                path.stat().st_size,
            )
            with path.open("rb") as handle:
                for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                    self.wfile.write(chunk)
            return

        self._json(HTTPStatus.NOT_FOUND, {"error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        route = urlparse(self.path).path
        if route != "/v1/development/session":
            self._json(HTTPStatus.NOT_FOUND, {"error": "not found"})
            return
        try:
            body = self._read_json()
            subject_id = str(body["subjectId"])
            audiences = IDENTITIES[subject_id]
        except (ValueError, KeyError, TypeError, json.JSONDecodeError):
            self._json(HTTPStatus.BAD_REQUEST, {"error": "unknown development identity"})
            return

        token, expiry = self.app.token_codec.issue(subject_id, audiences)
        self._json(
            HTTPStatus.OK,
            {
                "environment": "development",
                "productionAcceptance": False,
                "subjectId": subject_id,
                "audiences": list(audiences),
                "accessToken": token,
                "expiresAtEpochSeconds": expiry,
            },
        )


class DevelopmentServer(ThreadingHTTPServer):
    def __init__(
        self,
        address: tuple[str, int],
        token_codec: TokenCodec,
        release_service: ReleaseService,
    ):
        self.token_codec = token_codec
        self.release_service = release_service
        super().__init__(address, Handler)


def load_catalog() -> dict[str, Any]:
    with CONFIG_PATH.open(encoding="utf-8") as handle:
        catalog = json.load(handle)
    if catalog.get("environment") != "development" or catalog.get("productionAcceptance") is not False:
        raise RuntimeError("development catalog boundary is missing")
    return catalog


def main() -> None:
    secret_value = os.environ.get("GORECLOUD_DEV_BACKEND_HMAC_SECRET")
    if secret_value:
        secret = hashlib.sha256(secret_value.encode()).digest()
    else:
        secret = secrets.token_bytes(32)
        print("[development-package-delivery] using ephemeral session key for this process")

    server = DevelopmentServer(
        (HOST, PORT),
        token_codec=TokenCodec(secret),
        release_service=ReleaseService(load_catalog(), WardveilDevelopmentScanner()),
    )
    print(
        f"[development-package-delivery] listening on http://{HOST}:{PORT}; "
        "Development-only; productionAcceptance=false"
    )
    server.serve_forever()


if __name__ == "__main__":
    main()
