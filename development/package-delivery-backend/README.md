# Development package-delivery backend

This directory contains the **Development-only** package-delivery service used to exercise the GoreeCloud App Store Android download and install path before production Identity, catalog, release, Wardveil, and distribution services are accepted.

## Safety boundary

The service binds to `127.0.0.1:8791` by default and is intended to be reached from an Android development device through `adb reverse tcp:8791 tcp:8791`. The ordinary production manifest keeps cleartext traffic disabled and does not request package-install authority. Those capabilities are added only to the debug source set.

Development fixture identities are re-authorized by this backend and receive short-lived HMAC-protected session tokens. This proves the client is not the only place applying the Development audience/channel rule, but it is **not production authentication**.

Package handoff remains fail-closed. The backend will serve a configured artifact only when:

- the exact file exists;
- its SHA-256 matches the configured immutable identity;
- the requesting Development session is entitled to the item and release channel;
- the loopback Wardveil/ClamAV engine reports the APK clean;
- the same scanner detects the controlled EICAR test signature.

The Android client independently verifies the downloaded bytes, package identity, version, and signing-certificate SHA-256 before evaluating the existing `PackageDeliveryPolicy` and opening Android's user-confirmed package installer.

## First enabled artifact

The first bounded artifact is GoreeCloud Gallery `0.8.11-dev` / versionCode `2000883`, package `com.goreecloud.gallery.dev`, from `GoreeCloud/android-app-defaults@d5461282867e7c5dea11d8ab342abfdf88ce2a8c`.

The governed owner-test artifact is expected at:

`.dev-packages/GoreeCloud-Gallery-0.8.11-dev-vc2000883.apk`

Expected SHA-256:

`5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a`

Expected Development signing certificate SHA-256:

`7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc`

## Running

A Wardveil-compatible ClamAV daemon must be reachable on loopback port 3310 (or configured through `WARDVEIL_CLAMAV_TCP_HOST` and `WARDVEIL_CLAMAV_PORT`).

Run:

`python3 development/package-delivery-backend/server.py`

Then, with a development device connected:

`adb reverse tcp:8791 tcp:8791`

The debug App Store uses `http://127.0.0.1:8791`. This loopback cleartext exception is intentionally absent from release builds.

## Production boundary

This implementation does not establish Production Acceptance, Stable status, production GoreeCloud Identity authentication, a production authoritative catalog, authenticated Evidence Plane transport, production Wardveil runtime acceptance, protected Internet distribution, automatic updates, rollback, or broad package-management authority. Those remain separate gates.
