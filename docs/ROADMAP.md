# App Store implementation roadmap

## Phase 0 — source catalog (current implementation)
- [x] Repository foundation, MIT license, source provenance notice.
- [x] Versioned manifest, bounded API, tests and HTTP health.
- [x] Searchable React/Vite web interface using pinned GC UI alpha source.
- [x] Bundled native Qt Quick 6.12 and Android Compose source-only browsers.
- [x] Offline snapshot parity checks and initial API/web CI scripts.
- [ ] Verify exact-head GitHub CI and Android/Qt native compilation using supported toolchains.
- [x] Add official-checksum-verified Android Gradle 9.5.0 wrapper; [ ] obtain passing native Android and Qt CI builds.
- [ ] Human visual/a11y and physical Linux/Android device qualification.

## Phase 1 — signed distribution metadata
- [ ] Identity-scoped publisher enrollment and review / release approval workflow.
- [ ] Signed TUF metadata, supported keys and proven rotation/expiry/rollback checks.
- [ ] Reproducible Android APK and Flatpak builds with SBOM, attestations and vulnerability scanning.
- [ ] Artifact checksums verified from downloaded bytes; publisher provenance and license review.
- [ ] Signed Flatpak remote + F-Droid-compatible publishing investigation.

## Phase 2 — controlled installers
- [ ] Linux Flatpak installation via verified, explicit user-approved OS facilities.
- [ ] Android PackageInstaller consent UI with signing-lineage validation and policy review.
- [ ] Download pause/resume, offline transport, uninstall/update prompts, recovery and revocation.
- [ ] Private catalogs and authenticated entitlements only after GoreeShield + Identity design review.

## Phase 3 — ecosystem integration and release
- [ ] GoreeCloud Mesh service discovery/contracts, Gateway routing, Identity, Wardveil scans, GoreeShield user choices, full GC UI native adoption.
- [ ] Cross-app deep links, search and release notifications via GoreeCloud Notify (when deployed).
- [ ] Independent backup and restore, monitoring, staging, beta distribution and founder approval.
- [ ] Stable release only after passing actual device, usability, security and recovery gates.

The checklist describes source work and planned gates, **not deployment, security certification or released installers**.
