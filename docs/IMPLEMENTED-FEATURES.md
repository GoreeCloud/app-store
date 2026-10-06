# GoreeCloud App Store — Implemented Features

> **Authority:** Repository-native implemented-feature record, seeded from the existing `FEATURES.md`. Existing Development/Partial/Planned boundaries remain controlling.

## GoreeCloud App Store Features

## Implemented in the native bootstrap

- Native Android application shell using Kotlin and Jetpack Compose.
- Glaze-oriented tangible cards/surfaces, capsule-shaped search/account controls, adaptive Compose layout, and accessible 48dp-class controls.
- Discover, Apps, Services, Updates, and Library navigation.
- Search constrained to the already-entitled client catalog.
- Category filtering for entitled Discover, Apps, and Services entries.
- Featured horizontal catalog shelf plus denser all-items browsing for a larger portfolio.
- Expanded 34-entry non-production portfolio fixture replacing the original six-entry bootstrap catalog.
- Complete official catalog artwork coverage for all 34 entries, sourced from `GoreeCloud/branding-assets`; empty, generic, initial, and monogram fallbacks are prohibited and repository validation fails closed on missing mappings/resources.
- Device-review mobile layout refinement with compact account/status/hero/search surfaces, smaller Featured cards, denser catalog rows, section-specific search prompts, filtered result counts, suppressed redundant Development pills, and compact Updates/Library states.
- Representative-device onboarding/status refinement with a top-anchored progress-led three-step first-use flow, bottom-anchored navigation, structured availability summaries, current Glaze naming, compact integration cards, and additional catalog-row density improvements.
- Follow-up compact-phone polish: active-identity checkmark and concise account labels, redundant Discover status-strip removal, identity-scoped available-count wording, tighter category/Featured browsing, explicit clear-search action, centered compact disconnected Updates state, and a compact Library installed-history row.
- One-tap empty-result recovery, tighter Featured/category geometry, compact top-bar spacing, actionable unavailable-state rows, and consolidated product-detail Favorite/Save actions with one local-state explanation.
- Search IME completion behavior, explicit selected-category clear affordance, catalog-row Favorite/Saved state glyphs, an explicitly labeled development-identity account control, and shorter first-use guidance ending in **Start browsing**.
- Session-local **Recently opened** Library recency with per-identity separation, bounded ordering, Library-wide search, compact collection counts/actions, and Discover **Continue browsing** while keeping persistent history disconnected.
- Deterministic catalog sorting for entitled Discover browse-all, Apps, and Services results: authoritative catalog order, name, or category-then-name, exposed through compact accessible sort controls.
- Actionable product availability status cards that replace inactive delivery controls and route to Development status while preserving fail-closed delivery authority.
- Release-channel metadata visibility enforced through explicit Development identity channel grants; unauthorized channel/version fields are concealed even when the catalog item itself is entitled.
- Application and service item models.
- Development JSON catalog loader.
- Multi-user development session switcher.
- Explicit entitlement filtering with no implicit administrator bypass.
- Product-detail sheet with explicit unavailable installation/service-launch status until delivery is trusted, plus a direct Development-status explanation path.
- Mandatory three-step first-use guidance with durable interruption/resume state, replay, globally disableable contextual hints, and bounded dismissal/reset controls.
- Per-development-identity, device-local **Save for later** state for currently entitled catalog items, with exact opaque identity-subject namespacing and no raw subject embedded in preference-key metadata.
- Explicit **Clear saved for later** behavior scoped to the active development identity without uninstalling software or changing entitlement/account history.
- Fail-closed package-delivery policy, exact-package installed-state observation, and read-only delivery preflight that cannot manufacture accepted installation absence or invoke package mutation/install authority.
- Bounded debug-only Development package delivery for the exact GoreeCloud Gallery `0.8.11-dev` artifact: backend subject re-authorization, byte/package/signing validation, current Wardveil ClamAV reference clean-scan gating, pinned HTTPS, private-cache staging, client-side digest/package/signing re-verification, and Android PackageInstaller handoff with user confirmation. Release builds remain unavailable and no production package authority is claimed.
- Historical GLAZE UI V1.6 / 1.6.0 presentation-policy mapping retained as Development source evidence while current user-facing system naming uses Glaze; application-specific rendered/device/production acceptance remains separate.
- Platform-integration checkpoint for Glaze, Identity, Wardveil, Privacy Shield, Everkeep, and Mesh.
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
- Editorial collections, recommendations, richer saved-item organization/synchronization where separately accepted, and notification preferences where privacy policy permits.
- Multiple release channels with per-user/channel entitlements.
- Device compatibility and architecture filtering.
- Download/install queue and resilient retry state.
- Per-account update policy and optional automatic-update controls where Android policy permits.
- Accessibility, tablet, foldable, keyboard/mouse, and large-window acceptance.

