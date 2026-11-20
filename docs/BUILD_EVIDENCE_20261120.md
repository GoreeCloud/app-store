# Build evidence — November 20, 2026

## GitHub PR and source history

- Foundation: [PR #1](https://github.com/GoreeCloud/app-store/pull/1), merged as commit `5d131eb46d364abf249bc56db6322c67bead9bff`. Required `api-tests` and `web-build` jobs both passed on exact PR source and on postmerge main ([run](https://github.com/GoreeCloud/app-store/actions/runs/44814628728)).
- Verified Gradle wrapper: [PR #3](https://github.com/GoreeCloud/app-store/pull/3), merged as `de3b055fb75f97141d458031f63cc83853951251`, official Gradle 9.5.0 JAR and distribution checksums independently checked; read-only GitHub CI validation passed.

## Local development build

Authorized device: `personal-laptop-ideapad3-01`. Source branch commit `9866143881502943ee154ce74e75072f6676a08e`, equivalent Android app source to PR #3.

- API: `python -m pytest -q` → **6 passed**; catalog validation, native manifest/static invariants and catalog snapshot parity also passed.
- Web: `npm test` → **2 passed**; `npm run build` → Vite production build succeeded.
- Android: `cd apps/android && ./gradlew :app:assembleDebug --offline --no-daemon --max-workers=2` → **BUILD SUCCESSFUL in 3m 33s, 36 actionable tasks executed**. Debug APK located at `apps/android/app/build/outputs/apk/debug/app-debug.apk` on the laptop, approx 13 MiB, SHA-256 `6f117865f32261e09bae2df66196bc6ab22137aba96a589b8f5ab90500c64763`. This is a **local debug output**, not a signed/released/downloadable consumer package.
- Android build warning: AGP 9.2.0 has been tested up to compile SDK 37.0, while the project uses 37.1 to match current GoreeCloud UI alpha. Build succeeded with warning; monitor compatibility and upgrade AGP only after verifying canonical requirements.
- Android manifest inspection: no `INTERNET`, `REQUEST_INSTALL_PACKAGES`, sensitive-data or privileged install permission. AndroidX generated an internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; claiming literally zero merged manifest permissions would be inaccurate.
- ADB enumerated `OnePlusN200 (DE2117)` as connected, **but the APK was not installed or run** and no user data/device configuration was changed.
- Linux Qt: original laptop had no Qt Quick 6.12 development kit; local compilation and physical Linux runtime not yet verified. Dedicated nonpublishing CI has been added for initial build qualification and requires observed passing results.

## Security boundary

No production API deployment, installation privilege, signed APK/Flatpak repository, TUF root metadata, publisher approval, consent UI, vulnerability certification or release approval. GitHub CI is read-only, with exact source checkout and no publishing token.
