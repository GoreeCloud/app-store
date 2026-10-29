import copy
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "linux"))
from scripts.validate_catalog import validate
from goreecloud_store.releases import ReleaseAsset, download_verified, safe_url, fetch_latest, MAX_ASSET

class CatalogTests(unittest.TestCase):
    def setUp(self):
        self.catalog = json.loads((ROOT / "shared/catalog.json").read_text())

    def test_real_catalog(self):
        self.assertGreaterEqual(validate(self.catalog), 5)

    def test_owner_not_changeable(self):
        bad = copy.deepcopy(self.catalog)
        bad["owner"] = "Other"
        with self.assertRaises(AssertionError):
            validate(bad)

    def test_duplicate_product_rejected(self):
        bad = copy.deepcopy(self.catalog)
        bad["products"].append(bad["products"][0])
        with self.assertRaises(AssertionError):
            validate(bad)

    def test_bad_repo_rejected(self):
        bad = copy.deepcopy(self.catalog)
        bad["products"][0]["repo"] = "../../other"
        with self.assertRaises(AssertionError):
            validate(bad)

    def test_reject_untrusted_urls(self):
        for u in ("http://github.com/a", "https://github.com.evil.org/a",
                  "https://evil.org/a", "https://user:pw@github.com/a",
                  "file:///tmp/app.apk"):
            with self.subTest(url=u), self.assertRaises(ValueError):
                safe_url(u)

    def test_refuses_unverified_file(self):
        from tempfile import TemporaryDirectory
        with TemporaryDirectory() as d:
            with self.assertRaises(ValueError):
                download_verified(ReleaseAsset("update.apk", "https://github.com/a", 5, "none"), Path(d))

if __name__ == "__main__":
    unittest.main()
