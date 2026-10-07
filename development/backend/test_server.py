#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("goreecloud_dev_backend", HERE / "server.py")
backend = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = backend
SPEC.loader.exec_module(backend)


def valid_release_payload(file_path: str) -> dict:
    return {
        "storeItemId": "goreecloud.gallery",
        "artifactId": "goreecloud-gallery-0.8.11-dev-vc2000883",
        "filePath": file_path,
        "packageName": "com.goreecloud.gallery.dev",
        "versionName": "0.8.11-dev",
        "versionCode": 2000883,
        "releaseChannel": "development",
        "minSdk": 29,
        "sha256": "5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a",
        "certificateSha256": "7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc",
        "sizeBytes": 3315772,
        "allowedSubjects": ["dev:developer"],
        "buildProvenanceRef": "source@example",
        "sbomRef": "sbom@example",
        "releaseApprovalRef": "approval@example",
        "revocationStatusRef": "revocation@example",
    }


class ReleaseValidationTest(unittest.TestCase):
    def test_valid_development_release_parses(self) -> None:
        release = backend.Release.from_json(valid_release_payload("/tmp/example.apk"))
        self.assertEqual("development", release.release_channel)
        self.assertEqual(frozenset({"dev:developer"}), release.allowed_subjects)

    def test_unaudited_artifact_and_identity_are_rejected(self) -> None:
        changes = {
            "storeItemId": "goreecloud.other-app",
            "artifactId": "other-package",
            "packageName": "com.goreecloud.other.dev",
            "versionName": "0.8.12-dev",
            "versionCode": 2000884,
            "sha256": "0" * 64,
            "certificateSha256": "1" * 64,
            "sizeBytes": 3315773,
            "minSdk": 30,
            "allowedSubjects": ["dev:admin"],
        }
        for key, value in changes.items():
            with self.subTest(key=key):
                payload = valid_release_payload("/tmp/example.apk")
                payload[key] = value
                with self.assertRaises(backend.BackendBlocked):
                    backend.Release.from_json(payload)

    def test_non_development_channel_fails_closed(self) -> None:
        payload = valid_release_payload("/tmp/example.apk")
        payload["releaseChannel"] = "stable"
        with self.assertRaises(backend.BackendBlocked):
            backend.Release.from_json(payload)

    def test_missing_evidence_reference_fails_closed(self) -> None:
        payload = valid_release_payload("/tmp/example.apk")
        payload["sbomRef"] = ""
        with self.assertRaises(backend.BackendBlocked):
            backend.Release.from_json(payload)


class ReleaseEvidenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.release = backend.Release.from_json(valid_release_payload("/tmp/example.apk"))

    def test_records_are_artifact_bound_and_correlated(self) -> None:
        records = backend.build_release_evidence(
            self.release,
            {"validUntilEpochSeconds": 1_700_001_000},
            now=1_700_000_000,
        )
        self.assertEqual(
            {
                "BUILD_PROVENANCE",
                "SBOM",
                "RELEASE_APPROVAL",
                "REVOCATION_STATUS",
            },
            {record["type"] for record in records.values()},
        )
        evidence_sets = {record["evidenceSetId"] for record in records.values()}
        self.assertEqual(1, len(evidence_sets))
        for record in records.values():
            self.assertEqual(self.release.package_name, record["subjectPackageName"])
            self.assertEqual(self.release.sha256, record["artifactSha256"])
            self.assertEqual("accepted", record["state"])
            self.assertEqual("accepted", record["producerAuthority"])

    def test_expired_wardveil_window_blocks_release_evidence(self) -> None:
        with self.assertRaises(backend.BackendBlocked):
            backend.build_release_evidence(
                self.release,
                {"validUntilEpochSeconds": 1_700_000_000},
                now=1_700_000_000,
            )


class ArtifactSnapshotTest(unittest.TestCase):
    def test_exact_delivery_bytes_are_captured_and_verified(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "governed.apk"
            content = b"audited-development-apk-bytes"
            path.write_bytes(content)
            self.assertEqual(
                content,
                backend.load_verified_artifact_bytes(
                    path, len(content), hashlib.sha256(content).hexdigest()
                ),
            )

    def test_replaced_bytes_fail_closed_before_response_headers(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "governed.apk"
            original = b"original-exact-bytes"
            expected_digest = hashlib.sha256(original).hexdigest()
            path.write_bytes(b"tampered-exact-bytes")
            self.assertEqual(len(original), path.stat().st_size)
            with self.assertRaisesRegex(backend.BackendBlocked, "artifact_digest_mismatch"):
                backend.load_verified_artifact_bytes(
                    path, len(original), expected_digest
                )

    def test_truncated_or_extended_artifacts_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "governed.apk"
            original = b"expected-binary-data"
            expected_digest = hashlib.sha256(original).hexdigest()
            for content in (original[:-1], original + b"x"):
                with self.subTest(size=len(content)):
                    path.write_bytes(content)
                    with self.assertRaisesRegex(backend.BackendBlocked, "artifact_size_mismatch"):
                        backend.load_verified_artifact_bytes(
                            path, len(original), expected_digest
                        )


class RegistryExampleTest(unittest.TestCase):
    def test_example_registry_remains_single_release_and_nonproduction(self) -> None:
        payload = json.loads((HERE / "releases.example.json").read_text())
        self.assertEqual("development", payload["environment"])
        self.assertIs(payload["productionAcceptance"], False)
        self.assertEqual(1, len(payload["releases"]))
        release = payload["releases"][0]
        self.assertEqual(["dev:developer"], release["allowedSubjects"])
        self.assertEqual(
            "5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a",
            release["sha256"],
        )


class DevelopmentBindBoundaryTest(unittest.TestCase):
    def test_loopback_hosts_are_accepted(self) -> None:
        self.assertEqual("127.0.0.1", backend.require_loopback_host("127.0.0.1"))
        self.assertEqual("localhost", backend.require_loopback_host("LOCALHOST"))

    def test_non_loopback_host_fails_closed(self) -> None:
        for host in ("0.0.0.0", "192.168.1.10", "::"):
            with self.subTest(host=host):
                with self.assertRaises(backend.BackendBlocked):
                    backend.require_loopback_host(host)


if __name__ == "__main__":
    unittest.main()
