# GoreeCloud App Store

An open-source, self-hostable **GoreeCloud application catalog** with a read-only REST API, a responsive React storefront, a native Qt Quick Linux client, and a Kotlin/Jetpack Compose Android client.

**State: development foundation.** Catalog entries point to verified project source repositories, not automatically to installable packages. No signing, release ingestion, updater, unattended installation, production deployment, or device acceptance is claimed.

## Features in this source
- Validated, versioned JSON catalog and search/filter REST API.
- Responsive, accessible web discovery UI using the **pinned GoreeCloud UI** alpha package.
- Native Linux **Qt Quick 6.12.0** catalog client (read-only).
- Native Android **Jetpack Compose + Material 3** catalog client (read-only).
- Package metadata contract requiring HTTPS artifact URLs and SHA-256 before a release may be listed.
- API unit tests, catalog validation and CI for web/API builds.

## Quick start

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -e '.[dev]'
python scripts/validate_catalog.py
python -m pytest -q
uvicorn goree_store.api:app --host 127.0.0.1 --port 8787
```

Open http://127.0.0.1:8787/docs for the OpenAPI interface.

```bash
cd apps/web
npm ci
npm run build
npm run dev
```

The development frontend proxies `/api` to the local backend. For production, place both applications behind an authenticated, TLS-terminating reverse proxy and use the same origin; do not expose development servers publicly.

## Clients

- **Web:** `apps/web` — React + Vite, GoreeCloud UI styles pinned to `GoreeCloud/gc-ui` commit.
- **Linux:** `apps/linux` — Qt 6.12 Qt Quick / CMake, run `cmake -S apps/linux -B build/linux && cmake --build build/linux` in a Qt 6.12 SDK environment. The Linux client bundles an offline catalog snapshot and opens source links on explicit user action.
- **Android:** `apps/android` — Android Studio / Gradle project. Run `gradle :app:assembleDebug` using a verified local Gradle 9.5.0 installation (or use `bash scripts/build-android.sh`). The app has internet permission only; it does **not** request APK-install authority. The Android client also uses the bundled offline snapshot and needs no network permission; source links open externally on explicit user action.

## Distribution safety

A release appears only after a human-reviewed manifest change. A `release` must reference an HTTPS GitHub release asset and its verified SHA-256. This first iteration includes **no package releases**, so clients link to source only. A SHA-256 field alone does not authenticate publisher identity. Before enabling actual installation or updates, implement signed metadata (TUF), verified publisher keys, APK signing-certificate continuity, signed Flatpak repositories, explicit consent, rollback prevention, release revocation, and an isolated end-to-end update test.

[Architecture](docs/ARCHITECTURE.md) · [Security model](docs/SECURITY.md) · [Roadmap](docs/ROADMAP.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)

Copyright © 2026 GoreeCloud. Source license: [MIT](LICENSE). Dependencies have independent licenses.
