#!/usr/bin/env python3
import importlib.util
import json
import time
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("development_delivery_server", HERE / "server.py")
server = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(server)


class TokenCodecTest(unittest.TestCase):
    def test_round_trip_and_tamper_rejection(self):
        codec = server.TokenCodec(b"x" * 32)
        token, expiry = codec.issue("dev:developer", server.IDENTITIES["dev:developer"])
        session = codec.verify(token)
        self.assertEqual("dev:developer", session.subject_id)
        self.assertGreater(expiry, int(time.time()))
        with self.assertRaises(ValueError):
            codec.verify(token[:-1] + ("A" if token[-1] != "A" else "B"))

    def test_unknown_identity_cannot_become_authorized_session(self):
        codec = server.TokenCodec(b"y" * 32)
        now = int(time.time())
        payload = {
            "sub": "dev:invented",
            "aud": ["audience:administrator", "channel:development"],
            "iat": now,
            "exp": now + 600,
            "nonce": "test",
            "environment": "development",
        }
        encoded = server._b64url(json.dumps(payload, separators=(",", ":"), sort_keys=True).encode())
        signature = server._b64url(
            server.hmac.new(b"y" * 32, encoded.encode(), server.hashlib.sha256).digest()
        )
        with self.assertRaises(ValueError):
            codec.verify(f"{encoded}.{signature}")


class ReleaseAuthorizationTest(unittest.TestCase):
    def setUp(self):
        self.item = {
            "releaseChannel": "development",
            "access": {"anyAudience": ["audience:standard"]},
        }

    def test_developer_fixture_is_authorized_for_development_channel(self):
        session = server.Session(
            "dev:developer",
            server.IDENTITIES["dev:developer"],
            int(time.time()) + 60,
        )
        self.assertTrue(server.ReleaseService._authorized(session, self.item))

    def test_standard_fixture_cannot_bypass_development_channel(self):
        session = server.Session(
            "dev:standard",
            server.IDENTITIES["dev:standard"],
            int(time.time()) + 60,
        )
        self.assertFalse(server.ReleaseService._authorized(session, self.item))


class CatalogEvidenceTest(unittest.TestCase):
    def test_first_release_is_exactly_pinned_and_nonproduction(self):
        catalog = json.loads((HERE / "release-catalog.json").read_text())
        self.assertEqual("development", catalog["environment"])
        self.assertIs(catalog["productionAcceptance"], False)
        self.assertEqual(1, len(catalog["items"]))
        item = catalog["items"][0]
        self.assertEqual("goreecloud.gallery", item["id"])
        self.assertEqual("com.goreecloud.gallery.dev", item["packageName"])
        self.assertEqual(2000883, item["versionCode"])
        self.assertEqual(64, len(item["sha256"]))
        self.assertEqual(64, len(item["signingCertificateSha256"]))

    def test_bounded_sbom_is_bound_to_release_digest(self):
        catalog = json.loads((HERE / "release-catalog.json").read_text())
        item = catalog["items"][0]
        sbom = json.loads((HERE / "evidence" / "gallery-0.8.11-dev.sbom.json").read_text())
        properties = {
            entry["name"]: entry["value"]
            for entry in sbom["metadata"]["component"]["properties"]
        }
        self.assertEqual(item["sha256"], properties["goreecloud.apk.sha256"])
        self.assertEqual(item["sourceCommit"], properties["goreecloud.source.commit"])


if __name__ == "__main__":
    unittest.main()
