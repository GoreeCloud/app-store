# GoreeCloud App Store Development Package Backend

This backend enables a bounded **Development-only** package-delivery path for GoreeCloud App Store. It does not create Production Acceptance, Stable/Anchor status, production GoreeCloud Identity integration, or a broad `Protected by Wardveil` claim.

## Security boundary

The backend:

- serves only releases explicitly listed in an untracked deployment registry;
- requires a high-entropy bearer token and an allowed Development identity subject;
- uses TLS and is intended to be consumed with the App Store debug build's exact certificate pin;
- validates the APK byte size and SHA-256 on every release/download request;
- validates package identity, versionName, versionCode, and signing-certificate SHA-256 with Android build tools;
- runs the exact APK through the current Wardveil ClamAV reference adapter and refuses clean delivery unless current scanner-health evidence permits a clean verdict;
- emits `protectionClaimAuthority=false` because this Development gate is not production Wardveil acceptance;
- fails closed on missing, stale, mismatched, unhealthy, malicious, or unavailable evidence;
- never serves arbitrary paths, redirects, third-party packages, or a general APK catalog.

The first configured package is GoreeCloud Gallery `0.8.11-dev` / versionCode `2000883`, using the persistent GoreeCloud Gallery Development signing identity.

## Runtime configuration

Do not commit runtime secrets, TLS private keys, local artifact paths, or the live release registry. Supply them through environment variables:

```text
GORECLOUD_APP_STORE_TOKEN
GORECLOUD_APP_STORE_HOST
GORECLOUD_APP_STORE_PORT
GORECLOUD_APP_STORE_RELEASES
GORECLOUD_APP_STORE_TLS_CERT
GORECLOUD_APP_STORE_TLS_KEY
GORECLOUD_WARDVEIL_REPO
GORECLOUD_AAPT
GORECLOUD_APKSIGNER
WARDVEIL_CLAMAV_TCP_HOST
WARDVEIL_CLAMAV_PORT
```

The Wardveil ClamAV endpoint must remain loopback-only on the backend host. Use the current Wardveil deployment baseline rather than exposing raw `clamd` over a network.

## Client configuration

The Android debug build reads these Gradle properties:

```text
goreecloudDevDeliveryBaseUrl
goreecloudDevDeliveryToken
goreecloudDevDeliveryTlsCertSha256
```

They are compiled only into the Development/debug build. Release builds receive empty values and keep package delivery disabled.

Example:

```bash
gradle \
  -PgoreecloudDevDeliveryBaseUrl=https://192.0.2.10:8443 \
  -PgoreecloudDevDeliveryToken='<development token>' \
  -PgoreecloudDevDeliveryTlsCertSha256='<server leaf certificate sha256>' \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Android install authority

Only the debug manifest requests `REQUEST_INSTALL_PACKAGES`, and only the exact seeded Development package is declared query-visible. Android user confirmation remains mandatory. The App Store verifies the downloaded APK digest, package/version identity, and signing certificate again before opening a `PackageInstaller` session.

## Endpoints

- `GET /healthz` — minimal liveness.
- `GET /readyz` — revalidates configured artifacts and current Wardveil clean-scan eligibility.
- `GET /v1/releases/<storeItemId>` — authenticated exact Development release metadata.
- `GET /v1/artifacts/<artifactId>` — authenticated exact APK bytes after fresh validation.

No endpoint accepts package uploads or mutations in this increment.
