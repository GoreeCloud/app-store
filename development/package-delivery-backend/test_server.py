#!/usr/bin/env python3
import hashlib
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path

from server import DevelopmentDeliveryServer, Handler, load_catalog


class BackendTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.apk = root / "sample.apk"
        self.apk.write_bytes(b"goreecloud-development-apk")
        digest = hashlib.sha256(self.apk.read_bytes()).hexdigest()
        catalog = {
            "schemaVersion": 1,
            "environment": "development-local",
            "items": [{
                "itemId": "goreecloud.sample",
                "artifactId": "sample-1",
                "fileName": "sample.apk",
                "packageName": "com.goreecloud.sample.dev",
                "versionName": "1.0-dev",
                "versionCode": 1,
                "minSdk": 26,
                "releaseChannel": "development",
                "sha256": digest,
                "signerSha256": "a" * 64,
                "allowedAudiences": ["audience:standard"],
            }],
        }
        self.catalog_path = root / "catalog.json"
        self.catalog_path.write_text(json.dumps(catalog), encoding="utf-8")
        by_item, by_artifact = load_catalog(self.catalog_path, root)
        self.server = DevelopmentDeliveryServer(
            ("127.0.0.1", 0),
            Handler,
            token="x" * 40,
            by_item=by_item,
            by_artifact=by_artifact,
        )
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = f"http://127.0.0.1:{self.server.server_port}"

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def request(self, path, *, authenticated=True, audiences="audience:standard"):
        request = urllib.request.Request(self.base + path)
        if authenticated:
            request.add_header("Authorization", "Bearer " + "x" * 40)
        request.add_header("X-GoreeCloud-Development-Subject", "fixture-standard")
        request.add_header("X-GoreeCloud-Development-Audiences", audiences)
        return urllib.request.urlopen(request, timeout=2)

    def test_health(self):
        with urllib.request.urlopen(self.base + "/v1/health", timeout=2) as response:
            payload = json.load(response)
        self.assertEqual(payload["status"], "ok")
        self.assertEqual(payload["artifactCount"], 1)

    def test_item_requires_backend_authorization(self):
        with self.assertRaises(urllib.error.HTTPError) as context:
            self.request("/v1/items/goreecloud.sample", authenticated=False)
        self.assertEqual(context.exception.code, 401)

    def test_item_reauthorizes_audience(self):
        with self.assertRaises(urllib.error.HTTPError) as context:
            self.request("/v1/items/goreecloud.sample", audiences="audience:developer")
        self.assertEqual(context.exception.code, 403)

    def test_item_and_artifact_round_trip(self):
        with self.request("/v1/items/goreecloud.sample") as response:
            payload = json.load(response)
        self.assertEqual(payload["packageName"], "com.goreecloud.sample.dev")
        with self.request(payload["artifactPath"]) as response:
            body = response.read()
        self.assertEqual(body, b"goreecloud-development-apk")

    def test_catalog_rejects_digest_mismatch(self):
        raw = json.loads(self.catalog_path.read_text(encoding="utf-8"))
        raw["items"][0]["sha256"] = "0" * 64
        self.catalog_path.write_text(json.dumps(raw), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "digest mismatch"):
            load_catalog(self.catalog_path, Path(self.tmp.name))


if __name__ == "__main__":
    unittest.main()
