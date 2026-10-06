# Development package-delivery backend

This backend is a **Development-only**, loopback-bound package transport for GoreeCloud App Store. It exists so the Android Development client can exercise real backend re-authorization, APK download, byte-integrity verification, signing-identity verification, and Android package-install handoff without representing the flow as Production Accepted.

It is intentionally not a production service. It does not implement GoreeCloud Identity, Wardveil production acceptance, production release approval, trusted-time evidence, production catalog authority, or Stable/Anchor distribution.

## Start locally

Place only governed Development APKs in `.dev-packages/`. The repository ignores that directory so APK binaries are not committed.

Set a long random Development bearer credential and start the loopback server:

```bash
export GOREECLOUD_APP_STORE_DEV_TOKEN='<at-least-32-characters>'
python3 development/package-delivery-backend/server.py
```

The default endpoint is `http://127.0.0.1:47831`. The server refuses non-loopback bind addresses.

For a USB-connected Android device:

```bash
adb reverse tcp:47831 tcp:47831
```

Build the debug client with matching Development-only properties:

```bash
gradle \
  -PGOREECLOUD_APP_STORE_DEV_BACKEND_URL=http://127.0.0.1:47831 \
  -PGOREECLOUD_APP_STORE_DEV_TOKEN="$GOREECLOUD_APP_STORE_DEV_TOKEN" \
  :app:assembleDebug
```

Do not put the credential in Git, documentation, screenshots, logs, or release artifacts.

## Current seed

The catalog currently identifies the governed GoreeCloud Gallery 0.8.11-dev / versionCode 2000883 Development artifact. The APK must be supplied separately in `.dev-packages/` and must match the catalog SHA-256 before the backend starts.

The backend does not log identity subjects, audience claims, client IPs, or download history.
