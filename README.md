# GoreeCloud App Store

**Native Android + Linux development foundation** for discovering, downloading
and eventually updating first-party GoreeCloud applications and services.

**Status: early development; not Stable, production-ready or an accepted installer.**
Repository: https://github.com/GoreeCloud/app-store

## Product direction

GoreeCloud App Store is a dedicated, public-source storefront for the GoreeCloud
ecosystem. Its curated catalog identifies GoreeCloud products but does **not**
assert that a given app has a published installer. The store checks the official
GoreeCloud GitHub Release API when a user requests release details, and refuses
assets with missing SHA-256 digest metadata. The native clients do not embed a
website or launch arbitrary scripts.

- **Android:** Kotlin + Jetpack Compose app, API 29+; checks published stable
  GitHub releases, verifies APK assets, saves approved downloads to the Android
  Downloads collection. Installation uses Android's own consent flow only after
  a reviewed package ID is pinned in the catalog. No package IDs have been
  approved yet, so install buttons remain intentionally locked.
- **Linux:** Python + GTK4/Libadwaita native desktop UI, curated application
  and service sections, asynchronous release checks, and checksum-verified
  downloads to `~/Downloads/GoreeCloud`. The system software installer remains
  responsible for installation. Native Flatpak updates and service provisioning
  are not yet implemented or verified.
- **Shared:** versioned JSON catalog, schema validation, release integrity
  controls and automated unit tests.

## Repository layout

| Path | Purpose |
| --- | --- |
| `android/` | Native Android application and Gradle project |
| `linux/` | Native GTK4 / Libadwaita desktop source |
| `shared/catalog.json` | Curated platform-specific product directory |
| `scripts/`, `tests/` | Catalog checks and release-security regression tests |
| `docs/RELEASE_CONTRACT.md` | Proposed release metadata, signing, platform identities |
| `docs/SECURITY.md` | Trust model and current release limitations |

## Run Linux preview

Install distro-provided Python 3, PyGObject/GTK4 and Libadwaita typelibs.
For Debian/Ubuntu systems, packages are generally `python3-gi`,
`gir1.2-gtk-4.0` and `gir1.2-adw-1`. Then from this repository:

```bash
./linux/run.sh
```

## Build Android preview

Open `android/` in Android Studio with a compatible JDK 17, Android SDK 35,
Android Gradle Plugin 8.8.2 and Gradle 8.10.2. This repository does not yet
contain a Gradle wrapper or a signed APK. Compile and device acceptance
are **pending**. Never install test APKs from untrusted sources.

## Test shared and Linux logic

```bash
python3 scripts/validate_catalog.py
PYTHONPATH=.:linux python3 -m unittest discover -s tests -v
python3 -m compileall -q linux scripts tests
```

## Before a real release

The catalog needs verified Android package names and Flatpak app IDs, signed
distribution metadata, build provenance, rollback design, native-install
acceptance, accessibility review and GC UI design compliance. Publish products
as available only after each platform's real release assets are verified.
Android APK deliverables for owner installation/testing belong as standalone
APK files in the canonical Google Drive `GoreeCloud/Artifacts` folder.

This source is public for inspection. **No open-source license is granted yet**;
license selection and third-party notices are pending formal approval.
