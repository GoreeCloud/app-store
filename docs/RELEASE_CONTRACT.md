# GoreeCloud App Store — Release contract (development)

This is a proposed integration contract, **not** a statement that listed applications
currently publish installable releases.

1. The curated `shared/catalog.json` owns product and supported-platform declarations.
   Never accept arbitrary repository IDs, GitHub organizations, or installer commands from remote metadata.
2. Each product publishes a **non-draft, non-prerelease** GitHub Release with platform-specific assets.
   Android: `.apk`. Linux: preferably signed Flatpak repository/bundle, or a downloadable
   `.flatpakref` / `.AppImage` for system-managed installation.
3. Every downloadable release asset must have an asset-level `digest` of
   `sha256:<64 lowercase hex digits>` and a positive byte size in the GitHub Releases API.
   The store fails closed if metadata is missing or inconsistent; this detects corruption
   but **is not a substitute for trusted signing and provenance**.
4. Android apps must have a separately reviewed and pinned `android_package` in the
   catalog before the APK installer is enabled. Android OS package signing checks remain mandatory.
   The app must request an ordinary user-approved install, never promise silent updates.
5. Linux updates should use the Flatpak user's existing trusted repository and
   signed metadata, with a reviewed `flatpak_id`; do not run downloaded scripts,
   elevate privilege, or replace a daemon through arbitrary binaries.
6. Release authenticity hardening before stable release: verified signing identities,
   update/channel policy, reproducible or attested builds, rollback and downgrade
   behavior, malware inspection, recovery tests, and independent device/VM validation.
7. No service installation merely by discovering a catalog entry. Server services require
   explicit host selection, least-privilege authorization, migration/rollback plan and audit.

All releases currently identified by this repo are discovered dynamically. The
catalog does not imply that a usable package is available.
