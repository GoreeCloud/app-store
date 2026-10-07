#!/usr/bin/env python3
"""Development-only GoreeCloud App Store package delivery backend."""

from __future__ import annotations

import hashlib
import hmac
import importlib
import json
import os
import re
import ssl
import subprocess
import sys
import time
import urllib.parse
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

CANONICAL_SHA256 = re.compile(r"^[0-9a-f]{64}$")
PACKAGE_NAME = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+$")
MAX_APK_BYTES = 64 * 1024 * 1024
# This first Development tranche is deliberately restricted to one audited artifact.
# New apps or channels require an explicit reviewed contract change.
AUDITED_GALLERY = {
    "store_item_id": "goreecloud.gallery",
    "artifact_id": "goreecloud-gallery-0.8.11-dev-vc2000883",
    "package_name": "com.goreecloud.gallery.dev",
    "version_name": "0.8.11-dev",
    "version_code": 2000883,
    "release_channel": "development",
    "sha256": "5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a",
    "certificate_sha256": "7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc",
    "size_bytes": 3315772,
    "min_sdk": 29,
    "allowed_subjects": frozenset({"dev:developer"}),
}


class BackendBlocked(RuntimeError):
    pass


@dataclass(frozen=True)
class Release:
    store_item_id: str
    artifact_id: str
    file_path: Path
    package_name: str
    version_name: str
    version_code: int
    release_channel: str
    min_sdk: int
    sha256: str
    certificate_sha256: str
    size_bytes: int
    allowed_subjects: frozenset[str]
    build_provenance_ref: str
    sbom_ref: str
    release_approval_ref: str
    revocation_status_ref: str

    @property
    def download_path(self) -> str:
        return f"/v1/artifacts/{urllib.parse.quote(self.artifact_id, safe='')}"

    @classmethod
    def from_json(cls, value: dict[str, Any]) -> "Release":
        release = cls(
            store_item_id=str(value["storeItemId"]),
            artifact_id=str(value["artifactId"]),
            file_path=Path(value["filePath"]).expanduser().resolve(),
            package_name=str(value["packageName"]),
            version_name=str(value["versionName"]),
            version_code=int(value["versionCode"]),
            release_channel=str(value["releaseChannel"]).lower(),
            min_sdk=int(value["minSdk"]),
            sha256=str(value["sha256"]).lower(),
            certificate_sha256=str(value["certificateSha256"]).lower(),
            size_bytes=int(value["sizeBytes"]),
            allowed_subjects=frozenset(str(v) for v in value["allowedSubjects"]),
            build_provenance_ref=str(value["buildProvenanceRef"]),
            sbom_ref=str(value["sbomRef"]),
            release_approval_ref=str(value["releaseApprovalRef"]),
            revocation_status_ref=str(value["revocationStatusRef"]),
        )
        release.validate_static()
        return release

    def validate_static(self) -> None:
        for key, expected in AUDITED_GALLERY.items():
            if getattr(self, key) != expected:
                raise BackendBlocked(f"unauthorized_development_{key}")
        if not self.store_item_id or len(self.store_item_id) > 160:
            raise BackendBlocked("invalid_store_item_id")
        if not self.artifact_id or len(self.artifact_id) > 160 or "/" in self.artifact_id:
            raise BackendBlocked("invalid_artifact_id")
        if not PACKAGE_NAME.fullmatch(self.package_name):
            raise BackendBlocked("invalid_package_name")
        if not self.version_name or len(self.version_name) > 80:
            raise BackendBlocked("invalid_version_name")
        if self.version_code <= 0 or self.min_sdk <= 0:
            raise BackendBlocked("invalid_version_metadata")
        if self.release_channel != "development":
            raise BackendBlocked("non_development_release_rejected")
        if not CANONICAL_SHA256.fullmatch(self.sha256):
            raise BackendBlocked("invalid_artifact_sha256")
        if not CANONICAL_SHA256.fullmatch(self.certificate_sha256):
            raise BackendBlocked("invalid_certificate_sha256")
        if self.size_bytes <= 0 or self.size_bytes > MAX_APK_BYTES:
            raise BackendBlocked("invalid_artifact_size")
        if not self.allowed_subjects:
            raise BackendBlocked("missing_allowed_subjects")
        if any(
            not value or len(value) > 1000
            for value in (
                self.build_provenance_ref,
                self.sbom_ref,
                self.release_approval_ref,
                self.revocation_status_ref,
            )
        ):
            raise BackendBlocked("invalid_release_evidence_reference")


class WardveilGate:
    def __init__(self, repo: Path):
        if not (repo / "reference" / "wardveil_clamav.py").is_file():
            raise BackendBlocked("wardveil_reference_not_found")
        sys.path.insert(0, str(repo))
        clamav = importlib.import_module("reference.wardveil_clamav")
        runtime = importlib.import_module("reference.wardveil_clamav_runtime")
        self._ClamAVClient = clamav.ClamAVClient
        self._config_from_env = runtime.config_from_env
        self._collect_health = runtime.collect_clamav_health
        self._gate_verdict = runtime.gate_clamav_verdict

    def scan(self, release: Release) -> dict[str, Any]:
        client = self._ClamAVClient(self._config_from_env())
        health = self._collect_health(client)
        verdict = client.scan_file(release.file_path)
        finding = self._gate_verdict(
            verdict,
            health,
            resource_id=f"{release.store_item_id}@{release.version_name}",
        )
        if not health.clean_verdicts_eligible:
            raise BackendBlocked("wardveil_scanner_health_not_eligible")
        if finding.result != "clean":
            raise BackendBlocked(f"wardveil_scan_{finding.result}")
        evidence_refs = tuple(finding.evidence_refs)
        digest_ref = next((ref for ref in evidence_refs if release.sha256 in ref), None)
        if not digest_ref:
            raise BackendBlocked("wardveil_evidence_digest_mismatch")
        valid_until = int(finding.valid_until.timestamp())
        if valid_until <= int(time.time()):
            raise BackendBlocked("wardveil_scan_expired")
        return {
            "result": "clean",
            "evidenceRef": digest_ref,
            "observedAtEpochSeconds": int(finding.observed_at.timestamp()),
            "validUntilEpochSeconds": valid_until,
            "protectionClaimAuthority": False,
        }


def build_release_evidence(
    release: Release,
    wardveil: dict[str, Any],
    *,
    now: int | None = None,
) -> dict[str, Any]:
    evaluated_at = int(time.time()) if now is None else now
    evidence_expiry = min(
        evaluated_at + 10 * 60,
        int(wardveil["validUntilEpochSeconds"]),
    )
    if evidence_expiry <= evaluated_at:
        raise BackendBlocked("release_evidence_expired")
    evidence_set_id = (
        f"development:{release.artifact_id}:{release.sha256[:16]}:{evaluated_at}"
    )

    def record(evidence_type: str, source_reference: str) -> dict[str, Any]:
        return {
            "type": evidence_type,
            "state": "accepted",
            "producerId": "goreecloud.app-store.development-backend",
            "authorityDomain": "development.app-store.package-delivery",
            "producerAuthority": "accepted",
            "subjectPackageName": release.package_name,
            "artifactSha256": release.sha256,
            "evidenceSetId": evidence_set_id,
            "contractVersion": "development-package-delivery-v1",
            "createdAtEpochSeconds": evaluated_at,
            "expiresAtEpochSeconds": evidence_expiry,
            "sourceReference": source_reference,
        }

    return {
        "buildProvenance": record("BUILD_PROVENANCE", release.build_provenance_ref),
        "sbom": record("SBOM", release.sbom_ref),
        "releaseApproval": record("RELEASE_APPROVAL", release.release_approval_ref),
        "revocationStatus": record(
            "REVOCATION_STATUS",
            release.revocation_status_ref,
        ),
    }


class ReleaseRegistry:
    def __init__(
        self,
        releases_file: Path,
        wardveil_repo: Path,
        aapt: Path,
        apksigner: Path,
    ):
        payload = json.loads(releases_file.read_text(encoding="utf-8"))
        if payload.get("environment") != "development":
            raise BackendBlocked("release_registry_environment_invalid")
        releases = [Release.from_json(v) for v in payload.get("releases", [])]
        if not releases:
            raise BackendBlocked("release_registry_empty")
        if len(releases) != 1:
            raise BackendBlocked("development_registry_must_have_one_release")
        self.by_item = {r.store_item_id: r for r in releases}
        self.by_artifact = {r.artifact_id: r for r in releases}
        if len(self.by_item) != len(releases) or len(self.by_artifact) != len(releases):
            raise BackendBlocked("duplicate_release_identity")
        self.wardveil = WardveilGate(wardveil_repo)
        self.aapt = aapt
        self.apksigner = apksigner
        self.validate_all()

    def validate_all(self) -> None:
        for release in self.by_item.values():
            self.validate_artifact(release, scan=False)

    @staticmethod
    def _sha256_file(path: Path) -> str:
        digest = hashlib.sha256()
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(64 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()

    def validate_artifact(self, release: Release, *, scan: bool = True) -> dict[str, Any] | None:
        if not release.file_path.is_file():
            raise BackendBlocked("artifact_missing")
        stat = release.file_path.stat()
        if stat.st_size != release.size_bytes:
            raise BackendBlocked("artifact_size_mismatch")
        if self._sha256_file(release.file_path) != release.sha256:
            raise BackendBlocked("artifact_digest_mismatch")

        badging = subprocess.run(
            [str(self.aapt), "dump", "badging", str(release.file_path)],
            check=True,
            capture_output=True,
            text=True,
            timeout=20,
        ).stdout.splitlines()
        if not badging:
            raise BackendBlocked("artifact_badging_missing")
        line = badging[0]
        package = re.search(r"name='([^']+)'", line)
        version_code = re.search(r"versionCode='([^']+)'", line)
        version_name = re.search(r"versionName='([^']+)'", line)
        if not package or package.group(1) != release.package_name:
            raise BackendBlocked("artifact_package_mismatch")
        if not version_code or int(version_code.group(1)) != release.version_code:
            raise BackendBlocked("artifact_version_code_mismatch")
        if not version_name or version_name.group(1) != release.version_name:
            raise BackendBlocked("artifact_version_name_mismatch")

        signer = subprocess.run(
            [str(self.apksigner), "verify", "--print-certs", str(release.file_path)],
            check=True,
            capture_output=True,
            text=True,
            timeout=20,
        ).stdout
        match = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F]{64})", signer)
        if not match or match.group(1).lower() != release.certificate_sha256:
            raise BackendBlocked("artifact_signing_certificate_mismatch")

        return self.wardveil.scan(release) if scan else None


class DevelopmentBackend(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address: tuple[str, int], registry: ReleaseRegistry, token: str):
        super().__init__(address, RequestHandler)
        self.registry = registry
        self.token = token


class RequestHandler(BaseHTTPRequestHandler):
    server: DevelopmentBackend
    server_version = "GoreeCloudAppStoreDevelopmentBackend/0.1"
    sys_version = ""

    def log_message(self, fmt: str, *args: Any) -> None:
        # Do not log authorization headers or subject identities.
        sys.stderr.write("%s - %s\n" % (self.log_date_time_string(), fmt % args))

    def _json(self, status: HTTPStatus, payload: dict[str, Any]) -> None:
        body = (json.dumps(payload, sort_keys=True, separators=(",", ":")) + "\n").encode()
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def _authorized_subject(self, release: Release) -> str:
        authorization = self.headers.get("Authorization", "")
        supplied = authorization.removeprefix("Bearer ").strip()
        if not supplied or not hmac.compare_digest(supplied, self.server.token):
            raise PermissionError("authorization_failed")
        subject = self.headers.get("X-GoreeCloud-Dev-Subject", "").strip()
        if subject not in release.allowed_subjects:
            raise PermissionError("subject_not_authorized")
        return subject

    def do_GET(self) -> None:
        parsed = urllib.parse.urlsplit(self.path)
        path = parsed.path
        try:
            if path == "/healthz":
                self._json(HTTPStatus.OK, {"status": "ok", "environment": "development"})
                return
            if path == "/readyz":
                for release in self.server.registry.by_item.values():
                    self.server.registry.validate_artifact(release, scan=True)
                self._json(HTTPStatus.OK, {"status": "ready", "environment": "development"})
                return
            if path.startswith("/v1/releases/"):
                item_id = urllib.parse.unquote(path.removeprefix("/v1/releases/"))
                release = self.server.registry.by_item.get(item_id)
                if release is None:
                    self._json(HTTPStatus.NOT_FOUND, {"error": "release_not_found"})
                    return
                self._authorized_subject(release)
                wardveil = self.server.registry.validate_artifact(release, scan=True)
                self._json(
                    HTTPStatus.OK,
                    {
                        "schemaVersion": 2,
                        "environment": "development",
                        "productionAcceptance": False,
                        "release": {
                            "storeItemId": release.store_item_id,
                            "artifactId": release.artifact_id,
                            "packageName": release.package_name,
                            "versionName": release.version_name,
                            "versionCode": release.version_code,
                            "releaseChannel": release.release_channel,
                            "minSdk": release.min_sdk,
                            "sha256": release.sha256,
                            "certificateSha256": release.certificate_sha256,
                            "sizeBytes": release.size_bytes,
                            "downloadPath": release.download_path,
                            "wardveil": wardveil,
                            "releaseEvidence": build_release_evidence(
                                release,
                                wardveil,
                            ),
                        },
                    },
                )
                return
            if path.startswith("/v1/artifacts/"):
                artifact_id = urllib.parse.unquote(path.removeprefix("/v1/artifacts/"))
                release = self.server.registry.by_artifact.get(artifact_id)
                if release is None:
                    self._json(HTTPStatus.NOT_FOUND, {"error": "artifact_not_found"})
                    return
                self._authorized_subject(release)
                self.server.registry.validate_artifact(release, scan=True)
                self.send_response(HTTPStatus.OK.value)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Length", str(release.size_bytes))
                self.send_header("Cache-Control", "private, no-store")
                self.send_header("ETag", f'"sha256:{release.sha256}"')
                self.send_header("X-Content-Type-Options", "nosniff")
                self.end_headers()
                with release.file_path.open("rb") as handle:
                    for chunk in iter(lambda: handle.read(64 * 1024), b""):
                        self.wfile.write(chunk)
                return
            self._json(HTTPStatus.NOT_FOUND, {"error": "not_found"})
        except PermissionError:
            self._json(HTTPStatus.UNAUTHORIZED, {"error": "not_authorized"})
        except (BackendBlocked, subprocess.SubprocessError, OSError, ValueError) as exc:
            code = str(exc)
            safe = code if re.fullmatch(r"[A-Za-z0-9_.:-]{1,160}", code) else "backend_not_ready"
            self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": safe})


def required_path(name: str) -> Path:
    value = os.environ.get(name, "").strip()
    if not value:
        raise BackendBlocked(f"missing_{name.lower()}")
    path = Path(value).expanduser().resolve()
    if not path.exists():
        raise BackendBlocked(f"missing_path_{name.lower()}")
    return path


def require_loopback_host(value: str) -> str:
    host = value.strip().lower()
    if host not in {"127.0.0.1", "localhost"}:
        raise BackendBlocked("development_backend_must_bind_loopback")
    return host


def main() -> int:
    token = os.environ.get("GOREECLOUD_APP_STORE_TOKEN", "").strip()
    if len(token) < 32:
        raise BackendBlocked("development_token_too_short")
    host = require_loopback_host(
        os.environ.get("GOREECLOUD_APP_STORE_HOST", "127.0.0.1")
    )
    port = int(os.environ.get("GOREECLOUD_APP_STORE_PORT", "8443"))
    releases_file = required_path("GOREECLOUD_APP_STORE_RELEASES")
    wardveil_repo = required_path("GOREECLOUD_WARDVEIL_REPO")
    aapt = required_path("GOREECLOUD_AAPT")
    apksigner = required_path("GOREECLOUD_APKSIGNER")
    tls_cert = required_path("GOREECLOUD_APP_STORE_TLS_CERT")
    tls_key = required_path("GOREECLOUD_APP_STORE_TLS_KEY")

    registry = ReleaseRegistry(releases_file, wardveil_repo, aapt, apksigner)
    server = DevelopmentBackend((host, port), registry, token)
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    tls.minimum_version = ssl.TLSVersion.TLSv1_2
    tls.load_cert_chain(certfile=tls_cert, keyfile=tls_key)
    server.socket = tls.wrap_socket(server.socket, server_side=True)

    print(f"GoreeCloud App Store Development backend listening on https://{host}:{port}")
    server.serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
