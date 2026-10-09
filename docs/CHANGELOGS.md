# GoreeCloud App Store — Changelogs

## 2026-10-09 — session-scoped catalog detail hardening (Development candidate)

- Re-resolve product details from the *current identity's* entitled catalog before presenting a sheet; an old selection cannot retain restricted metadata after an identity/catalog transition.
- Ignore stale clicks on no-longer-entitled catalog rows and clear the detail sheet and delivery presentation state when changing Development identities.
- Bind asynchronous package-delivery callbacks and detail-sheet actions to the identity, selected item, and a per-opening revision, preventing delayed responses from an earlier opening of the same product from replacing current state.
- Add JVM tests for hidden prior-identity details and refreshed current metadata. This is presentation-layer defense in depth; backend authorization is independently required.
- **Verification:** source committed on a distinct child Development branch of PR #51; fresh exact-head Android CI, representative physical-device tests, protected integration, and Production/Stable acceptance remain pending.

## 2026-10-09 — normalized catalog and Library search (Development candidate)

- Unified search behavior across entitled Discover/Apps/Services entries and the active Development identity's local Library.
- Added multi-term, order-independent, case-insensitive, and accent-insensitive matching over names, summaries, and categories; retained Library-only application/service type matches.
- Preserved category restrictions, explicit sort choices, source-order behavior, entitlement-first filtering, and existing no-match recovery.
- Added pure JVM regression cases. No backend lookup, history capture, entitlement expansion, identity integration, package installation, or release authority is added.
- **Verification:** source is on a stacked Development candidate branch; exact-head CI and on-device acceptance remain pending.


## 2026-10-06 — bounded Development package delivery

- Added a read-only Development package backend with bearer-token and exact fixture-subject re-authorization.
- Bound the first installable Development entry to GoreeCloud Gallery `0.8.11-dev` / versionCode `2000883`, exact APK SHA-256 `5516f03092252ca053a54b3240ec0c95e97d1d12ccda1089c4bba377a81dcd0a`, and Development signing-certificate SHA-256 `7976b1035c5c1b259682eb384ce5cd7e3fbc49c6611911182a112724825b9cbc`.
- Added backend byte/package/version/signing validation and current Wardveil ClamAV reference clean-scan gating before release metadata or APK bytes are served.
- Added pinned-HTTPS Android debug delivery, private-cache staging, client-side digest/package/version/signing re-verification, and Android PackageInstaller handoff with mandatory user confirmation.
- Kept `REQUEST_INSTALL_PACKAGES` out of the main/release manifest; the debug manifest is bounded to the exact Gallery Development package.
- Added release-build fail-closed gateway behavior and CI guards that reject permission, scope, digest, signing, backend-mutation, and version-boundary drift.
- Advanced the App Store Development package to `0.1.14-dev` / versionCode `15`.
- This is Draft PR #48 stacked on PR #47. Fresh exact-head CI/runtime, configured backend runtime reachability, representative-device install/update acceptance, protected distribution, production platform integrations, Production Acceptance, and Seal/Anchor/Stable gates remain open.

## 2026-10-03 — Catalog presentation improvements

- Added deterministic catalog ordering options: source order, name, and category.
- Added compact accessible sorting controls to Discover, Apps, and Services.
- Added unit coverage for sorting plus query/category filtering.
- Reworked product-detail availability into an actionable status surface linked to Development status.
- Concealed channel/version metadata from identities that lack the item's explicit release-channel grant; broad catalog entitlement and administrator fixtures do not create a channel bypass.
- Updated first-use guidance for the new browse controls.
- Refined the Android 16 session-recency runtime check so it validates recency rather than unrelated sheet layout.
- Advanced the Development package to `0.1.13-dev` / versionCode `14`.
- Fresh exact-head Android validation remains required.

## 2026-10-02 — Library recency and compact-account pass

- Added a bounded, session-local **Recently opened** Library collection that moves revisited items to the front without creating durable browsing history.
- Kept recent items separated by the active Development identity and re-filtered them through current entitlements.
- Added Library-wide search across Favorites, Saved for later, and Recently opened, with one-tap reset when no collection matches; active searches now hide unmatched collection sections instead of filling the page with redundant no-match cards.
- Added compact collection-count chips, moved clear actions into section headings, and show Recently opened only when the session actually contains recent items.
- Added **Continue browsing** on Discover when session recency exists, without showing it while catalog search/category filters are active.
- Moved the active Development identity into the header subtitle and reduced the account trigger to an icon-sized control so compact phones no longer truncate labels such as **Standard**.
- Updated onboarding capability copy and Android runtime coverage for the revised Library flow.
- Advanced the persistent Development package to `0.1.12-dev` / versionCode `13`.
- Fresh exact-head Android validation and Android 16 runtime evidence are required before this candidate is treated as validated Development evidence.

## 2026-10-02 — Mobile interaction and accessibility polish

- Shortened first-use guidance copy, simplified the progress label, and changed the final action from **Finish setup** to **Start browsing** while preserving durable resume/replay behavior.
- Added Android search-keyboard completion behavior so the Search IME action clears focus/keyboard without changing live filtering semantics.
- Added an explicit close glyph to the selected category chip so the toggle-off behavior is visually obvious without introducing a second interaction target.
- Added compact Favorite and Saved-for-later state glyphs to catalog rows so local Library state is visible before opening a detail sheet.
- Added an explicit accessibility label for the development-identity account control while keeping already-labeled bottom-navigation icons decorative to avoid duplicate spoken labels.
- Advanced the persistent Development package to `0.1.10-dev` / versionCode `11`.
- Fresh exact-head Android validation and Android 16 onboarding/runtime evidence are required before this candidate is treated as validated Development evidence.

## 2026-10-02 — Catalog interaction polish

- Reduced Discover guidance from a tinted full-width capsule to a lightweight inline tip so catalog content appears sooner.
- Tightened the persistent header, hero, category spacing, and Featured shelf geometry while preserving accessible interaction targets.
- Added one-tap **Reset** recovery when search/category filters produce no catalog matches.
- Made disconnected-state cards directly actionable with a trailing affordance instead of embedding a second text action inside the card.
- Consolidated product-detail Favorite and Save controls into one compact action row and replaced duplicate local-library explanations with one concise boundary statement.
- Narrowed Featured cards and artwork again so horizontal browsing exposes the next item more intentionally on compact phones.
- Advanced the persistent Development package to `0.1.9-dev` / versionCode `10`.
- Fresh exact-head Android validation and Android 16 runtime evidence are required before this candidate is treated as validated Development evidence.

## 2026-10-02 — Representative-device polish pass

- Moved first-use progress/content to the top usable region and anchored Back/Continue/Finish navigation to the bottom safe area so onboarding no longer floats in the middle of tall phone screens.
- Removed the disabled Back action from step 1 while preserving durable resume/replay behavior and large-text scrolling.
- Shortened the catalog guidance hint and reduced its action footprint.
- Simplified the account menu to concise identity labels and added an active-identity checkmark while retaining the same Development fixture subjects and entitlement behavior.
- Removed the redundant Discover Development-status strip; status remains accessible from the account menu and contextual disconnected states.
- Clarified the Discover hero count from “total” to “available” so identity-scoped counts are not confused with the full 34-entry Development catalog.
- Tightened category-chip typography/end padding, refined Featured shelf card width/spacing/trailing padding for a more deliberate horizontal peek, and added an explicit clear-search affordance when a query is active.
- Removed duplicate Updates messaging, centered and compacted the disconnected-state card within the available viewport, and replaced Library’s large installed-history warning with a compact status row.
- Simplified Guidance & setup copy.
- Advanced the persistent Development package to `0.1.8-dev` / versionCode `9`.
- Fresh exact-head Android validation and Android 16 onboarding/runtime evidence are required before this candidate is treated as validated Development evidence.

## 2026-10-02 — Onboarding and status refinement

- Rebuilt the three-step first-use experience from representative-device review: centered content, explicit progress segments, shorter copy, structured capability rows, and in-flow navigation actions replace the oversized document-like cards and avoid an intermittent accessibility-tree loss of the final setup action.
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
