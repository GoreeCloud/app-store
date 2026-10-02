# GoreeCloud App Store — Implemented Features

> **Authority:** Repository-native implemented-feature record, seeded from the existing `FEATURES.md`. Existing Development/Partial/Planned boundaries remain controlling.

## GoreeCloud App Store Features

## Implemented in the native bootstrap

- Native Android application shell using Kotlin and Jetpack Compose.
- Glaze-oriented tangible cards/surfaces, capsule-shaped search/account controls, adaptive Compose layout, and accessible 48dp-class controls.
- Discover, Apps, Services, Updates, and Library navigation.
- Search constrained to the already-entitled client catalog.
- Application and service item models.
- Development JSON catalog loader.
- Multi-user development session switcher.
- Explicit entitlement filtering with no implicit administrator bypass.
- Product-detail dialog with deliberately unavailable install/open action until delivery is trusted.
- Mandatory three-step first-use guidance with durable interruption/resume state, replay, globally disableable contextual hints, and bounded dismissal/reset controls.
- Per-development-identity, device-local **Save for later** state for currently entitled catalog items, with exact opaque identity-subject namespacing and no raw subject embedded in preference-key metadata.
- Explicit **Clear saved for later** behavior scoped to the active development identity without uninstalling software or changing entitlement/account history.
- Fail-closed package-delivery policy, exact-package installed-state observation, and read-only delivery preflight that cannot manufacture accepted installation absence or invoke package mutation/install authority.
- Glaze UI V1.6 presentation-policy mapping retained as Development source evidence; application-specific rendered/device/production acceptance remains separate.
- Platform-integration checkpoint for Glaze UI, Identity, Wardveil, Privacy Shield, Everkeep, and Mesh.
- Unit tests and Android CI.

## Next functional milestones

- Production GoreeCloud Identity OIDC/session adapter.
- Server-authoritative entitlement/catalog API.
- Authenticated/signed catalog snapshots and rollback/revocation semantics.
- GoreeCloud application release ingestion pipeline.
- Package provenance, digest, signing-certificate, and Wardveil verification.
- Secure APK download and Android package installation.
- Update detection, staged download, user-visible release notes, and rollback-safe state.
- Installed Library scoped by identity and device.
- Service endpoint/deep-link launch with allowlisting and service-side reauthorization.
- Privacy Shield policies for search/history/recommendations/diagnostics.
- Everkeep protection contract and recovery evidence for library/history/catalog configuration.
- GoreeCloud Mesh lifecycle/capability events.
- Rich app pages: screenshots, changelog, source/license, permissions, compatibility, privacy, security, continuity, support.
- Categories, collections, editorial surfaces, recommendations, richer saved-item organization/synchronization where separately accepted, and notification preferences where privacy policy permits.
- Multiple release channels with per-user/channel entitlements.
- Device compatibility and architecture filtering.
- Download/install queue and resilient retry state.
- Per-account update policy and optional automatic-update controls where Android policy permits.
- Accessibility, tablet, foldable, keyboard/mouse, and large-window acceptance.
