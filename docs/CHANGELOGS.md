# GoreeCloud App Store — Changelogs

## 2026-10-02 — Onboarding and status refinement

- Rebuilt the three-step first-use experience from representative-device review: centered content, explicit progress segments, shorter copy, and structured capability rows replace the oversized document-like cards.
- Preserved durable onboarding resume/replay state and contextual-tip controls while reducing startup friction.
- Reworked Development Status into compact integration cards for faster scanning on phones.
- Migrated the current user-facing design-system name from Glaze UI to Glaze while preserving the historical GLAZE UI V1.6 / 1.6.0 Official Anchor label in provenance-sensitive copy.
- Corrected the stale Development Status reference to a 2.0.0 consumer contract; the UI now reflects the authoritative 1.6.0 Official Anchor boundary.
- Increased catalog density again by reducing row artwork/padding and using single-line summaries while retaining type/category metadata.
- Advanced the persistent Development package to `0.1.7-dev` / versionCode `8`.
- Fresh exact-head Android validation and Android 16 onboarding/runtime evidence are required before this candidate is treated as validated Development evidence.

## 2026-10-02 — Representative-device mobile UI refinement

- Refined the Android browsing experience from owner-supplied physical-device screenshots.
- Prioritized the Discover catalog hero/search/category controls and reduced Development status to a compact secondary capsule.
- Reduced hero, stat-chip, Featured shelf, section-heading, and catalog-row visual weight so more actual catalog content is visible per screen.
- Removed duplicate Apps/Services browse headings and made their result counts reflect active search/category filters.
- Added section-specific search prompts for Discover, Apps, and Services.
- Shortened compact account labels while preserving full development-session names in the selector.
- Suppressed the redundant Development release pill on Development catalog rows while retaining channel pills for future non-Development entries and detail surfaces.
- Replaced oversized catalog guidance, Updates, Library, and catalog-empty panels with compact utility states while preserving authority/privacy explanations.
- Improved large-text resilience by allowing search height to expand and making hero statistics horizontally scrollable rather than forcing compression.
- Advanced the persistent Development package to `0.1.6-dev` / versionCode `7`.
- Fresh exact-head Android validation and Android 16 runtime evidence are required before this materially changed UI candidate is treated as validated Development evidence.

## 2026-10-02 — Official catalog artwork completeness

- Replaced the expanded-catalog monogram fallback with mandatory official artwork for every App Store application and service.
- Bound all 34 current catalog entries to canonical `GoreeCloud/branding-assets` provenance at exact branding main `ccfa74b3ffed12db285d32bcb5289821a1daf86e`.
- Added Android derivatives for 27 entries that previously fell back to generated initials; the seven already mapped official identities remain in place.
- Consumed newly established canonical Dialer, Camera, PDF Manager, and GitHub Dashboard identities from branding-assets PR #31.
- Added `branding-provenance.json` plus fail-closed validation requiring a total non-null catalog mapping and rejecting placeholder/monogram rendering paths.
- Advanced the Development package to `0.1.5-dev` / versionCode `6` for update continuity.
- Fresh exact-head Android validation and Android 16 runtime evidence are required for this materially changed candidate.
## 2026-10-02 — Catalog expansion and browsing UI rebuild

- Replaced the six-entry bootstrap catalog fixture with a 34-entry non-production GoreeCloud portfolio fixture while preserving explicit audience filtering and non-authoritative package/version/endpoint boundaries.
- Rebuilt the main Android browsing hierarchy with a compact branded header, portfolio summary hero, search-first discovery, category filters, a horizontally browsable Featured shelf, and clearer all-items sections.
- Preserved entitlement concealment, device-local Favorites/Save for later isolation, unavailable package-delivery authority, and Development-only platform-status truth.
- Advanced the Development APK identity to `0.1.4-dev` / version code `5` so validated builds can update the previous persistent Development package in place.
- Fresh exact-head Android validation is required before this candidate is treated as validated Development evidence.

## 2026-09-30 — Repository baseline governance completion

- Added `docs/PRIVACY.md`, `.github/SECURITY.md`, and `.editorconfig` to satisfy the current repository baseline without widening runtime authority.
- Updated README, documentation navigation, and Platform Contract evidence to reference the new canonical privacy/security records.
- Added `scripts/validate_repository_structure.py` and CI enforcement so root cleanliness and mandatory repository records fail closed on future changes.
- Privacy and security records explicitly preserve the Development-only Identity, package-delivery, signing, Privacy Shield, Wardveil, recovery, production, and Stable boundaries.

## 2026-09-30 — Repository root-cleanliness migration

- Moved canonical human-readable repository records from the repository root into `docs/` in accordance with current GoreeCloud repository-structure governance.
- Kept `README.md`, build manifests, `.gitignore`, `LICENSE`, `goreecloud.platform.yaml`, and other technically justified entry-point controls at root.
- Updated README navigation, Platform Contract evidence paths, and branding validation to use canonical `docs/` locations.
- Corrected stale Development version metadata to `0.1.3-dev` / version code `4`, matching the authoritative Android build configuration on this candidate.
- No application runtime authority, package-install authority, lifecycle state, Production Acceptance, or Stable qualification changed.

## 2026-09-30 — Stabilization candidate consolidation and local-state hardening

- Consolidated the current-main package-delivery/Glaze stabilization lineage and repository-native feature-authority reconciliation into the active onboarding candidate, preserving both histories as explicit merge ancestry.
- Retargeted the candidate to current authoritative `main` and removed the retired `FEATURE-ROADMAP.md` so repository-native feature authority remains `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md`.
- Added mandatory first-use guidance, durable interruption/resume/replay state, contextual-hint controls, and bounded device-local **Save for later** behavior.
- Hardened saved-item identity isolation so preference namespaces hash the exact opaque development identity subject rather than trimming it and potentially collapsing distinct subjects.
- Preserved fail-closed package-delivery policy, exact-package installed-state observation, and read-only preflight boundaries; no package installation, production Identity/catalog authority, release authority, or Stable status is created by this candidate.
- Fresh exact-head CI remains required after this documentation reconciliation before the candidate can be treated as validated Development evidence.

## 2026-09-29 — Repository-native feature-authority reconciliation

- Corrected `PLANNED-FEATURES.md` from the retired `GoreeCloud/goreecloud-app-store` identity to current `GoreeCloud/app-store`.
- Removed stale header/purpose references that still described a synchronized Google Drive Feature Roadmap as planned-feature authority.
- Bound the detailed planned capability source to the already-migrated repository file `docs/plans/goreecloud-app-store-update.md`.
- Preserved the existing September 27 Drive-retirement boundary: `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` remain the repository-native feature-state records.
- No Android/Linux/Web runtime behavior, package-delivery authority, lifecycle status, production acceptance, Release Candidate, or Stable state changed.


## 2026-09-27 — Drive feature-roadmap migration

- Retired the synchronized Google Drive roadmap after Drive/repository parity verification.
- Moved feature-state authority to `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and this `CHANGELOGS.md`.
- Existing source-state and lifecycle boundaries remain unchanged; no lifecycle promotion is implied.
