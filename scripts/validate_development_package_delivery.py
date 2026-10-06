#!/usr/bin/env python3
"""Fail-closed source guardrails for Development-only App Store package delivery."""

from __future__ import annotations

import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> int:
    main_manifest = ROOT / "app/src/main/AndroidManifest.xml"
    debug_manifest = ROOT / "app/src/debug/AndroidManifest.xml"
    backend = ROOT / "development/backend/server.py"
    example = ROOT / "development/backend/releases.example.json"
    catalog_path = ROOT / "app/src/main/assets/catalog/development-catalog.json"
    build_file = ROOT / "app/build.gradle.kts"
    release_factory = ROOT / (
        "app/src/release/java/com/goreecloud/appstore/delivery/PackageDeliveryGatewayFactory.kt"
    )
    debug_gateway = ROOT / (
        "app/src/debug/java/com/goreecloud/appstore/delivery/DevelopmentPackageDeliveryGateway.kt"
    )

    for path in (
        main_manifest,
        debug_manifest,
        backend,
        example,
        catalog_path,
        build_file,
        release_factory,
        debug_gateway,
    ):
        require(path.is_file(), f"missing required Development delivery source: {path}")

    main_root = ET.parse(main_manifest).getroot()
    main_permissions = {
        node.attrib.get(ANDROID_NS + "name")
        for node in main_root.findall("uses-permission")
    }
    require(
        "android.permission.REQUEST_INSTALL_PACKAGES" not in main_permissions,
        "REQUEST_INSTALL_PACKAGES must not enter the main/release manifest",
    )

    debug_root = ET.parse(debug_manifest).getroot()
    debug_permissions = {
        node.attrib.get(ANDROID_NS + "name")
        for node in debug_root.findall("uses-permission")
    }
    require(
        "android.permission.REQUEST_INSTALL_PACKAGES" in debug_permissions,
        "debug manifest must declare explicit Android install-source authority",
    )
    query_packages = {
        node.attrib.get(ANDROID_NS + "name")
        for queries in debug_root.findall("queries")
        for node in queries.findall("package")
    }
    require(
        query_packages == {"com.goreecloud.gallery.dev"},
        "Development package visibility must remain exact and bounded to Gallery",
    )

    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    gallery = next(item for item in catalog["items"] if item["id"] == "goreecloud.gallery")
    require(gallery["packageName"] == "com.goreecloud.gallery.dev", "Gallery package drift")
    require(gallery["version"] == "0.8.11-dev", "Gallery Development version drift")
    require(gallery["releaseChannel"] == "development", "Gallery channel drift")

    registry = json.loads(example.read_text(encoding="utf-8"))
    require(registry.get("environment") == "development", "backend registry must be Development")
    require(len(registry.get("releases", [])) == 1, "first delivery tranche must stay single-release")
    release = registry["releases"][0]
    require(release["storeItemId"] == gallery["id"], "registry/catalog item mismatch")
    require(release["packageName"] == gallery["packageName"], "registry/catalog package mismatch")
    require(release["versionName"] == gallery["version"], "registry/catalog version mismatch")
    require(release["allowedSubjects"] == ["dev:developer"], "backend subject scope broadened")
    require(
        release["sha256"] == "5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a",
        "Gallery artifact digest drift",
    )
    require(
        release["certificateSha256"] == "7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc",
        "Gallery Development signing identity drift",
    )

    build = build_file.read_text(encoding="utf-8")
    require('versionCode = 15' in build and 'versionName = "0.1.14-dev"' in build, "APK version drift")
    for property_name in (
        "goreecloudDevDeliveryBaseUrl",
        "goreecloudDevDeliveryToken",
        "goreecloudDevDeliveryTlsCertSha256",
    ):
        require(property_name in build, f"missing debug delivery property {property_name}")

    release_source = release_factory.read_text(encoding="utf-8")
    require("override val isAvailable: Boolean = false" in release_source, "release delivery enabled")
    require("PackageInstaller" not in release_source, "release source must not invoke PackageInstaller")

    debug_source = debug_gateway.read_text(encoding="utf-8")
    for marker in (
        "HttpsURLConnection",
        "DEVELOPMENT_DELIVERY_TLS_CERT_SHA256",
        "wardveil_development_scan_not_clean",
        "apk_signing_certificate_mismatch",
        "PackageInstaller.SessionParams",
    ):
        require(marker in debug_source, f"missing debug delivery fail-closed marker: {marker}")

    backend_source = backend.read_text(encoding="utf-8")
    compile(backend_source, str(backend), "exec")
    for marker in (
        "X-GoreeCloud-Dev-Subject",
        "hmac.compare_digest",
        "certificate SHA-256 digest",
        "self.wardveil.scan",
        "protectionClaimAuthority",
    ):
        require(marker in backend_source, f"missing backend trust marker: {marker}")
    require("def do_POST" not in backend_source, "Development backend must remain read-only")

    print("Development package delivery boundary validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
