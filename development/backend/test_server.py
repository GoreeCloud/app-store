#!/usr/bin/env python3
from __future__ import annotations

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
        "sha256": "5" * 64,
        "certificateSha256": "7" * 64,
        "sizeBytes": 1234,
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
