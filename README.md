# GoreeCloud App Store

GoreeCloud App Store is the official GoreeCloud-owned application for discovering, obtaining, updating, and opening GoreeCloud applications and services that an authenticated identity is authorized to access.

This repository is an **original native GoreeCloud implementation**. Google Play, Apple App Store, and F-Droid are product inspirations only; their application code, architecture, branding, and UI are not the implementation foundation.

## Current checkpoint

Status: **Active Development — native Android application**  
Production acceptance: **false**

The current development branch establishes:

- a native Android/Jetpack Compose store application;
- a native Compose presentation layer with a bounded source-policy mapping to Official Stable GLAZE UI V1.6 / 1.6.0; complete rendered/native application acceptance remains migration-required;
- a persistent App Store/account header and Material bottom navigation;
- a per-session entitlement engine that filters the catalog before presentation;
- development-only multi-user identity fixtures behind an explicit `IdentityGateway` boundary;
- distinct application and service catalog entries;
- Discover, Apps, Services, Updates, and Library surfaces;
- search constrained to the already-entitled catalog;
- store-style application/service cards and product-detail bottom sheets;
- approved first-party artwork derivatives tied to canonical assets in `GoreeCloud/goreecloud-branding-assets`;
- development-status diagnostics separated from ordinary catalog browsing;
- compact-width safeguards for account controls, catalog headings, item metadata, release-channel labels, detail metadata, and platform-status rows;
- explicit source boundaries for GoreeCloud Identity, Wardveil Security, Privacy Shield, Everkeep, and GoreeCloud Mesh;
- current Platform Contract 0.4 control-plane records covering all nine Integral Platform Systems, with Manager, Policy, and Observability explicitly blocked rather than omitted;
- a machine-readable platform-integration record;
- unit tests that prevent implicit administrator bypass of catalog audience rules;
- exact-source Android CI for tests, lint, APK assembly, package/application-label validation, signing-certificate verification, SHA-256 evidence, and development artifact publication.

The interface is being iterated with real-device screenshots from Android development builds. The first review removed oversized internal diagnostics from normal browsing, replaced placeholder artwork/navigation glyphs, fixed tab/account scroll behavior, and moved item details to store-style sheets. A second compact-phone review then exposed width-allocation defects: catalog counts could overlap section headings, long metadata could squeeze the Development channel into a vertical single-character stack, and account/status metadata could compete for insufficient horizontal space. The current source constrains those elements explicitly, gives primary text flexible width, keeps status/channel capsules single-line, and uses vertical metadata presentation where horizontal pairing is not resilient.

## Development APK identity

CI/debug builds install as `com.goreecloud.appstore.dev` with the Android label **GoreeCloud App Store Dev**. They are signed with one repository-managed development-only certificate so successive development builds can update each other instead of receiving a new ephemeral Android debug identity from every CI runner.

The current development version line is `0.1.2-dev` with version code `3`.

The reserved future production application ID remains `com.goreecloud.appstore`. The development signing key MUST NOT sign that production package or any artifact represented as production-approved or Stable. See `development/signing/README.md` for the explicit boundary and certificate fingerprint.

Older bootstrap APKs used `com.goreecloud.appstore` with ephemeral runner-generated debug certificates. Those builds cannot be upgraded in place by later CI APKs and should be removed from test devices before using the new development package.

## Important acceptance boundary

`main` is an Android-only Development implementation at this checkpoint. Historical Linux/Web work remains outside current-main implementation authority and must be recovered deliberately if those clients remain in product scope.

The current Compose theme now pins the mandatory V1.6 / 1.6.0 authority through a bounded presentation policy at accepted release source `a7180679ea851389e0f3004515f9a25f420e716d`. This corrects the historical/pre-reset `2.0.0` source label but does not establish rendered/native GLAZE UI conformance, accessibility, representative-device, performance, rollback, Human Visual Excellence, release, or production acceptance.

The account switcher is **not** a production GoreeCloud Identity login. It uses development fixtures only so multi-user entitlement behavior can be built and tested while the application-facing GoreeCloud Identity runtime remains unaccepted.

The development catalog also does not assert production package identities, service endpoints, versions, or audience taxonomy. Those values must come from approved authoritative release, service, and Identity metadata.

Package download, APK installation, service launch, production update delivery, Wardveil package-verification acceptance, production Privacy Shield policy evaluation, Everkeep library recovery, and Mesh lifecycle transport are deliberately unavailable until their real integrations are implemented and validated. The UI must not imply otherwise.

## Authorization model

The App Store separates three decisions:

1. **Identity authentication and platform authority** — owned by GoreeCloud Identity.
2. **Catalog entitlement** — an App Store domain decision evaluated from approved Identity claims/policy inputs; an item not entitled to a user is concealed rather than merely disabled.
3. **Artifact/service authorization** — must be enforced again by the package or service delivery backend. Client-side filtering is defense in depth, not the security boundary.

No role receives an undocumented superuser bypass. Administrative access must be explicitly granted by policy.

## Branding contract

`GoreeCloud/goreecloud-branding-assets` is the canonical branding repository. Android VectorDrawable copies in this repository are consumer derivatives only and do not become new branding authorities.

See `BRANDING.md` for the exact canonical asset paths and Git-blob mappings currently consumed for Browser, Messenger, Location, Identity, and Manager artwork.

No App Store-specific official icon/logo is established here. Any future official App Store artwork must originate in the canonical branding repository first.

## Build foundation

The application is pinned to current stable Android tooling as of August 29, 2026:

- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- JDK 17
- compileSdk 37
- targetSdk 36
- minSdk 26
- Kotlin / Compose compiler plugin 2.4.10
- Jetpack Compose BOM 2026.08.00

A Gradle wrapper is not yet committed. With JDK 17 and Gradle 9.5.0 installed:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

CI installs the pinned Gradle distribution directly, checks out and records the exact source revision, validates the generated development APK package/version/application label and development signing certificate, generates SHA-256 evidence, and publishes the development APK/evidence bundle.

## Repository records

- `SPECIFICATIONS.md` — product and engineering requirements
- `ARCHITECTURE.md` — authority boundaries and runtime design
- `FEATURES.md` — implemented and planned capabilities
- `BENEFITS.md` — intended user/platform value
- `COMPETITIVE-OBJECTIVES.md` — inspiration translated into GoreeCloud-native objectives
- `BRANDING.md` — canonical branding-consumer mappings
- `USER-MANUAL.md` — current user/developer behavior and limitations
- `development/signing/README.md` — development package/signing boundary
- `goreecloud.platform.yaml` — Platform Contract 0.4 current conformance declaration
- `contracts/platform-integrations.json` — machine-readable current integration truth
- `app/src/main/assets/catalog/development-catalog.json` — non-authoritative development fixture catalog

## License

GNU Affero General Public License v3.0. See `LICENSE`.
