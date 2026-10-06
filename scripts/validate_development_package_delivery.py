#!/usr/bin/env python3
"""Fail-closed source checks for the Development package-delivery boundary."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, needle: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    if needle not in text:
        raise SystemExit(f"{path}: missing required boundary: {needle}")


def forbid(path: str, needle: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    if needle in text:
        raise SystemExit(f"{path}: prohibited boundary present: {needle}")


def main() -> int:
    main_manifest = "app/src/main/AndroidManifest.xml"
    debug_manifest = "app/src/debug/AndroidManifest.xml"
    network_config = "app/src/debug/res/xml/development_network_security_config.xml"
    client = "app/src/main/java/com/goreecloud/appstore/delivery/DevelopmentPackageDeliveryGateway.kt"
    server = "development/package-delivery-backend/server.py"
    catalog = "development/package-delivery-backend/catalog.json"

    forbid(main_manifest, "android.permission.REQUEST_INSTALL_PACKAGES")
    require(debug_manifest, "android.permission.REQUEST_INSTALL_PACKAGES")
    require(debug_manifest, 'android:exported="false"')
    require(debug_manifest, "development_network_security_config")

    require(network_config, '<base-config cleartextTrafficPermitted="false"')
    require(network_config, ">localhost<")
    require(network_config, ">127.0.0.1<")

    require(client, "if (!BuildConfig.DEBUG) return null")
    require(client, 'uri.host in setOf("127.0.0.1", "localhost")')
    require(client, "connection.instanceFollowRedirects = false")
    require(client, "Development artifact SHA-256 verification failed.")
    require(client, "Downloaded APK signing identity does not match release metadata.")
    require(client, "Downloaded APK package identity does not match release metadata.")
    require(client, "PackageInstaller.SessionParams")
    forbid(client, "QUERY_ALL_PACKAGES")

    require(server, '"127.0.0.1"')
    require(server, "Development backend is intentionally loopback-only")
    require(server, "artifact_integrity_changed")
    require(server, "development_catalog_not_authorized")
    require(server, "def log_message")
    forbid(server, '"0.0.0.0"')

    require(catalog, '"itemId": "goreecloud.gallery"')
    require(catalog, '"packageName": "com.goreecloud.gallery.dev"')
    require(catalog, '"versionCode": 2000883')
    require(
        catalog,
        '"sha256": "5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a"',
    )
    require(
        catalog,
        '"signerSha256": "7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc"',
    )

    print("Development package-delivery boundary validation passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
