# GoreeCloud App Store — Project Specifications

**Repository:** `GoreeCloud/app-store`  
**Former repository identity in the Drive source:** `GoreeCloud/goreecloud-app-store`  
**Project type:** First-party GoreeCloud software discovery, distribution, update, and lifecycle-control application  
**Lifecycle:** Active Development / non-Production / non-Stable  
**Repository visibility:** Public  
**Default branch:** `main`  
**Migration baseline:** `e8ed0f0d9be4a6f5d2358e621d927a134045db9e`  
**License:** AGPL-3.0-only  
**Primary accepted client on baseline `main`:** Native Android  
**Production Android application ID:** `com.goreecloud.appstore`  
**Development Android application ID:** `com.goreecloud.appstore.dev`  
**Canonical authority:** This file is the authoritative project specification once accepted on the default branch.

## Migration and authority boundary

This specification reconciles Google Drive **Project Specification — App Store** with verified repository state and the live GoreeCloud Glaze UI authority.

The Drive source is an Internal project-governance record and names the former repository `GoreeCloud/goreecloud-app-store`. The current authoritative repository is `GoreeCloud/app-store`.

At the migration baseline, authoritative `main` is `e8ed0f0d9be4a6f5d2358e621d927a134045db9e`. Open Draft pull requests, including the long App Store Development stack and PR #33, remain candidate evidence only until accepted, merged, and read back from `main`.

The Drive source describes native Android, native Linux, and first-party Web clients as Development directions. On authoritative baseline `main`, the accepted user-facing client is Android. Linux/Web client work remains Draft/candidate-only and must not be represented as current accepted implementation.

## Glaze UI reconciliation

The live `GoreeCloud/glaze-ui` repository establishes **GLAZE UI V1.6 / 1.6.0** as the current Official Stable consumer authority. V1.7 is Development-only and non-consumer-eligible.

Accordingly:
- the Drive source's V1.5.1 Stable target is historical;
- baseline App Store documentation/metadata that calls 2.0.0 Stable is stale and is corrected by this migration;
- App Store consumer conformance remains unaccepted until exact-revision application evidence exists; and
- upstream Glaze Stable status does not automatically certify App Store rendering, accessibility, performance, device behavior, release readiness, or Production acceptance.

## Drive-source coverage

The former Drive project specification contains 18 top-level sections covering:
1. product role;
2. multi-user and entitlement model;
3. catalog model;
4. native Android architecture;
5. user experience;
6. Glaze UI;
7. GoreeCloud Identity;
8. Wardveil Security;
9. Privacy Shield;
10. Everkeep;
11. GoreeCloud Mesh;
12. package delivery and installation;
13. updates and library behavior;
14. a September 1, 2026 source checkpoint;
15. current restrictions;
16. Production and Stable promotion gates;
17. long-term direction; and
18. a September 4, 2026 Development successor candidate.

Still-applicable normative requirements from those sections are consolidated below. Source-era exact-version, branch, CI, and candidate statements are preserved as history in `PROJECT-RECORD.md` rather than promoted to current `main`.

## Product role

GoreeCloud App Store is the official distribution and discovery surface for GoreeCloud applications and services. It is not a general third-party marketplace in its initial scope.

## Development model

The App Store must be original GoreeCloud-owned native software. Store patterns may be informed by Google Play, Apple App Store, and F-Droid, but their product implementation must not be forked or reproduced as the GoreeCloud application architecture.

## Primary client

The first client is a native Android application written in Kotlin with Jetpack Compose. Android framework, Jetpack, Kotlin, Gradle, and mature cryptographic/transport primitives are supporting foundations, not substitute product implementations.

## Development package and signing boundary

The Android namespace remains `com.goreecloud.appstore`. The application identities have distinct development and production responsibilities:

- `com.goreecloud.appstore.dev` is the development/debug installation identity used by CI and device-review APKs.
- `com.goreecloud.appstore` is reserved for a future production-approved application and must not be signed with the repository-managed development key.

Development APKs use one stable repository-managed PKCS12 development certificate so successive CI artifacts do not receive unrelated ephemeral Android debug identities. This development private key is intentionally non-production test material and is not a production security authority.

The development signing certificate SHA-256 fingerprint is recorded in `development/signing/README.md`. CI must verify the expected development package ID, label, version metadata, APK SHA-256, and signing certificate before publishing a development artifact.

Development version codes must advance when required for Android upgrade semantics. A future production release requires a separate controlled signing identity, custody and recovery policy, explicit signing provenance, and production acceptance. Development signing material must never be promoted into that boundary.

Older bootstrap APKs that used `com.goreecloud.appstore` with ephemeral CI debug certificates are not an accepted update lineage and may require removal from test devices.

## Multi-user and entitlement requirements

- Every production user session must originate from GoreeCloud Identity or an explicitly approved local/offline identity path.
- The catalog must be personalized from authoritative policy inputs associated with the active identity.
- Different users may receive different sets of applications, services, channels, versions, or administrative tools.
- Concealed items must not leak through search, recommendations, counts, update lists, deep links, cached catalog metadata, or service launch affordances.
- Administrator status must not create an implicit bypass. Access must be explicit in the applicable policy.
- The delivery service must re-authorize artifact download/service launch independently of client rendering.
- Account disablement and session revocation must invalidate protected catalog and delivery access according to the Identity contract.
- A future multi-account device mode must keep per-identity library/history state separated.

The current audience labels in `development-catalog.json` are fixtures only and do not establish the production GoreeCloud group taxonomy.

## Catalog model

Each catalog item must have a stable GoreeCloud identifier and declare at minimum:

- item type: application or service;
- display metadata;
- category;
- lifecycle/release channel;
- entitlement policy reference or normalized access requirements;
- application package identity or service endpoint identity as applicable;
- artifact/version provenance when delivery is enabled;
- privacy, security, continuity, and platform-integration evidence references when available.

Production catalog metadata must be authenticated, versioned, rollback-aware, and delivered over approved secure transport. A stale or unverifiable catalog must not silently become trusted production truth.

## Applications

Application entries will eventually support:

- compatible release discovery;
- signed artifact metadata;
- checksum and signature/provenance validation;
- Wardveil pre-install verification;
- Android package installation through an explicit user-authorized workflow;
- updates, release notes, channels, rollback information, and installed-state reconciliation.

The app must not request Android package-install authority until installation is implemented and the permission is justified by the approved release scope.


## Package delivery and installation

Before a production package handoff or install flow is enabled, the exact candidate must require:

- a protected artifact endpoint with backend re-authorization;
- immutable release and artifact identifiers;
- SHA-256 or stronger approved artifact integrity evidence;
- expected application signing identity/provenance;
- Wardveil verification according to accepted policy;
- secure transport with no unauthorized redirects or cleartext fallback;
- explicit Android user authorization for package installation;
- install/update result reconciliation;
- rollback, failure, downgrade, and recovery behavior; and
- auditable linkage from the distributed artifact to approved GoreeCloud source/release evidence.

Client-side catalog entitlement is not the artifact-delivery security boundary. Missing, stale, malformed, mismatched, unavailable, or negative required evidence must fail closed.

## Services

Service entries represent GoreeCloud capabilities that are opened rather than installed. Launch must use a policy-approved endpoint/deep link and must not treat catalog visibility as service authorization.

## Integral platform systems

### Glaze UI

Current GoreeCloud design-system authority: **GLAZE UI V1.6 / 1.6.0 Official Stable**. The App Store must substantively implement the applicable interaction, accessibility, responsive, state, material, navigation, target-size, motion, resilience, and fallback requirements. Current repository source/metadata is migration-required and does not claim App Store conformance until exact-revision acceptance exists. V1.7 is Development-only and is not a consumer Stable target.

### GoreeCloud Identity

Identity owns authentication, accounts, sessions, devices, credentials, and platform authority. The App Store owns store-domain entitlement decisions using approved Identity inputs. Production integration should prefer the approved OIDC/OAuth application integration path where appropriate and must validate login/logout, redirects, session expiry, user mapping, role/group mapping, disablement, failure behavior, and rollback.

### Wardveil Security

Wardveil owns package/security trust outcomes. Before installation is enabled, the App Store must validate artifact provenance and required Wardveil checks and must fail closed when required verification is missing, stale, malformed, unavailable, or negative.

### Privacy Shield

Privacy Shield owns consent, minimization, data-use, retention, sharing, and user-control policy. Store analytics are off in the development client. Any future recommendations, diagnostics, personalization telemetry, search history, or cross-device library data must have a documented purpose and Privacy Shield treatment before collection.

### Everkeep

Everkeep owns continuity/recoverability truth. The App Store must define a protection contract for important catalog configuration, user library/history state, and recovery metadata. Sync and backup must remain conceptually distinct. A library backup must not be presented as recoverable without applicable evidence.

### GoreeCloud Mesh

Mesh owns platform coordination, capability discovery, governance, and events. The App Store should use Mesh contracts for minimized application/service lifecycle events and catalog coordination when the production contract exists, without making Mesh the source of Identity, Privacy, Wardveil, Everkeep, or Glaze authority.

## Store UX

The product should provide:

- Discover/home recommendations based only on authorized catalog data;
- Apps and Services sections;
- search that never returns unauthorized items;
- Updates and Library surfaces scoped to the active identity/device;
- detailed product pages with version/channel, compatibility, release notes, privacy, permissions, security/provenance, continuity state, source/license information, and support links where authoritative data exists;
- clear account switching with no cross-account metadata leakage;
- accessible adaptive layouts for phones, tablets, foldables, desktop-class Android windows, and other supported Android form factors as validated;
- compact-width and enlarged-text behavior that keeps navigation, account affordances, section headings, counts, release/status capsules, and metadata readable without overlap or pathological single-character vertical wrapping;
- layout priority rules that give primary descriptive text flexible space while preserving bounded controls and status capsules, using truncation or vertical label/value presentation where horizontal pairing would become unreadable;
- clear negative/unknown states rather than fabricated positive badges.

Real-device screenshots are acceptance inputs for responsive behavior, but a single device or screenshot set does not establish Glaze UI or form-factor conformance across the supported matrix.

## Release and production gates

Stable qualification requires all of the following for the exact release revision:

- reproducible or otherwise controlled build provenance;
- passing CI, unit/integration tests, lint, and package validation;
- a controlled production application-signing identity distinct from development signing;
- real GoreeCloud Identity integration and entitlement enforcement acceptance;
- authenticated production catalog delivery;
- backend re-authorization for protected artifact/service access;
- Wardveil package-verification acceptance for install flows;
- Privacy Shield acceptance for data processing and telemetry;
- Everkeep application-specific protection/recovery contract and required evidence;
- GoreeCloud Mesh integration where applicable to the accepted release scope;
- current Glaze UI consumer conformance evidence;
- Android device/runtime validation for supported API levels, font scales, and form factors;
- documented installation/update rollback and failure behavior;
- canonical project specification, changelog, README, and user documentation reconciled to the validated revision.

## Planned first-party software control center direction

The detailed September 2026 App Store expansion remains migration input in the legacy `FEATURE-ROADMAP.md` and former Drive feature-roadmap material. Current project requirements belong here; detailed repository-native feature state must be completed through the separate mandatory feature/changelog migration.

The planned product direction keeps the dedicated App Store **first-party only**. Its catalog is intended for official GoreeCloud applications, services, system components, operating-system components, desktop/web/TV/server software, self-hosted services, networking software, extensions, plugins, integrations, developer tools, command-line utilities, background services, firmware, device components, themes, optional feature packages, and official preview/beta software. It is not intended to become a Google Play, F-Droid, Samsung Galaxy Store, community-repository, third-party APK-catalog, or general external-developer marketplace replacement.

Planned scope includes a canonical GoreeCloud software catalog and GoreeCloud Verified authenticity model; Glaze UI Today, Apps, Services, System, Discover, Search, Updates, and Library experiences; rich product, privacy, security, lifecycle, accessibility, documentation, and support metadata; platform-aware and device-aware software delivery; Privacy Shield and Wardveil intelligence; cryptographic package verification; dependency-aware Safe Update Mode; rollback; staged rollout; official release channels and beta programs; Everkeep-backed restoration; multi-device library and GoreeCloud Identity-authorized remote installation; GoreeCloud Manager administration; self-hosted mirrors and offline operation; download management and security alerts; centralized release infrastructure; automated quality validation; GoreeCloud Mesh-assisted trusted local distribution; one-product/multiple-platform identity; and GoreeCloud ecosystem-graph relationships.

This planned direction is not implementation evidence. A capability remains Planned until the applicable source, tests, integration evidence, runtime behavior, security/privacy acceptance, release evidence, and production gates establish a stronger state. Repository documentation must not convert the roadmap into a shipped, Production-accepted, Release Candidate, or Stable claim merely by describing it here.


## Client/platform acceptance boundary

Android is the accepted baseline user-facing Development client on authoritative `main`.

Linux and Web are permitted first-party client targets, but each requires its own accepted source, build, runtime, accessibility, Glaze UI, security/privacy, packaging/deployment, rollback, and release evidence. Draft source or successful CI on a candidate stack does not establish accepted baseline implementation.

## Current accepted implementation boundary

At the migration baseline, repository `main` establishes a native Android Development application with:
- development package/signing identity separation;
- a per-session entitlement engine over development fixtures;
- application/service catalog entries;
- Discover, Apps, Services, Updates, and Library surfaces;
- entitled-catalog search;
- store-style item cards and detail presentation;
- first-party branding derivatives;
- explicit source boundaries for Identity, Wardveil Security, Privacy Shield, Everkeep, and Mesh;
- machine-readable integration-state metadata; and
- Android CI for source/test/lint/APK/signing/digest evidence.

The baseline does **not** establish production Identity, protected package download/install, service authorization, production updates, Wardveil package-verification acceptance, Privacy Shield production-policy acceptance, Everkeep recovery acceptance, Mesh lifecycle transport, Linux/Web accepted clients, Production Acceptance, Release Candidate status, or Stable status.

## Repository-native governance

This project still requires the separate repository-native feature/changelog migration required by GoreeCloud governance. Until that migration is accepted:
- `FEATURE-ROADMAP.md` is temporary non-authoritative migration input;
- former Drive roadmap/changelog sources must not be treated as active parallel authority;
- current implementation claims must come from accepted repository evidence; and
- this project-specification migration must not fabricate `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, or `CHANGELOGS.md` completion.

## Maintenance

Update this specification whenever product role, catalog/entitlement authority, supported clients, package-delivery security, platform-system boundaries, Glaze requirements, production signing, release channels, deployment model, lifecycle status, or retirement materially changes.

## Related repository documentation

- [README.md](README.md)
- [PROJECT-RECORD.md](PROJECT-RECORD.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [FEATURES.md](FEATURES.md)
- [FEATURE-ROADMAP.md](FEATURE-ROADMAP.md) — temporary migration input pending repository-native feature/changelog governance.
- [RELEASE-CHANNELS.md](RELEASE-CHANNELS.md)
- [BRANDING.md](BRANDING.md)
- [USER-MANUAL.md](USER-MANUAL.md)
- [LICENSE](LICENSE)
