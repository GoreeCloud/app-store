# GoreeCloud App Store User Manual

## Current status

GoreeCloud App Store is currently an **active-development Android application**. It is not production-ready and does not yet install applications or open GoreeCloud services.

The present application validates the native store experience, multi-user catalog behavior, first-party GoreeCloud artwork consumption, responsive store presentation, and GoreeCloud platform integration boundaries before real distribution is enabled.

`productionAcceptance` remains `false`.

## Development APKs

There is no production or Stable end-user App Store release yet.

The repository CI can produce a development/debug APK for an exact source revision after unit tests, Android lint, APK assembly, package/application-label verification, development signing-certificate verification, SHA-256 evidence generation, and artifact publication succeed. Treat those artifacts as test builds only.

Current development/debug builds use:

- application ID `com.goreecloud.appstore.dev`;
- Android label **GoreeCloud App Store Dev**;
- development version line `0.1.2-dev` / version code `3` at this checkpoint;
- the repository-managed, development-only signing certificate documented in `development/signing/README.md`.

The development package is intentionally separate from the reserved future production application ID `com.goreecloud.appstore`. The development signing identity is non-production test material and must never sign the production package or a Stable artifact.

### If an older bootstrap is still installed

Early bootstrap CI APKs used the production-reserved package name `com.goreecloud.appstore` while Android CI generated a different ephemeral debug certificate on each runner. Android therefore cannot replace one of those old bootstrap installations with a later CI APK signed by another runner.

If the device still shows the large **Development identity adapter** panel, letter-only G/I/M artwork, text-glyph bottom navigation, **Search what is available to you**, or the large **Platform integration checkpoint** inside normal browsing, that is the old bootstrap application.

Remove that older bootstrap from the test device, or leave it installed only if you intentionally want to compare it. Install and launch **GoreeCloud App Store Dev** for current testing. The `.dev` package can coexist with the old package because they have different Android application IDs.

Future development artifacts are intended to retain the same development package and signing identity so they can update earlier `.dev` installations, subject to normal Android version-code rules.

## Development account switcher

The persistent account control in the App Store header offers development-only identities such as **Standard demo**, **Administrator demo**, **Developer demo**, and **Signed out**.

These are not real GoreeCloud accounts, groups, or production roles. They are local fixtures used to demonstrate how different logins can receive different App Store catalogs while production GoreeCloud Identity integration is still pending.

Changing the development identity immediately recalculates which catalog entries are visible and returns the current section to its top. An entry for which the active session is not entitled is concealed from visible lists and search results.

On compact screens, a long active development identity name may be ellipsized in the persistent header so the account icon and menu affordance remain usable. The complete identity names remain available in the account menu.

## Store sections

### Discover

Shows all development catalog entries currently available to the active development identity. A compact development-status notice is shown instead of embedding platform diagnostics throughout the catalog.

The available-item count is presented below the section heading so compact-width and larger-text layouts do not force the count over the heading. Singular and plural labels are handled separately.

### Apps

Shows only entitled installable-application entries. Installation is currently disabled until secure release ingestion, artifact provenance, Wardveil verification, and Android package-delivery acceptance are implemented.

### Services

Shows only entitled GoreeCloud service entries. Opening services is currently disabled until production GoreeCloud Identity authorization and approved service-endpoint policy are connected.

### Updates

Shows a dedicated development unavailable state. Update discovery and delivery have not yet been connected.

### Library

Shows a dedicated development unavailable state. Per-identity installed/library history and Everkeep-backed recovery have not yet been connected.

## Search

Use **Search your available catalog** to filter the current entitled section by application/service name, summary, or category.

Search operates only on entries already available to the active development identity. It does not reveal entries that were filtered out by entitlement rules.

## Application and service artwork

Where approved assets exist, the Android client uses native VectorDrawable derivatives tied to canonical assets in `GoreeCloud/goreecloud-branding-assets`.

The branding repository remains authoritative. Copies in this App Store repository are implementation derivatives only. See `BRANDING.md` for the exact canonical asset and Git-blob mappings.

## Catalog cards and release channels

Catalog cards show artwork, name, summary, type/category metadata, release channel, and a product-navigation affordance.

The card layout gives the primary type/category metadata flexible width while keeping the release-channel capsule on one line. On compact widths, long metadata is ellipsized rather than squeezing labels such as **Development** into vertical single-character wrapping. Product titles may use up to two lines when needed.

## Product details

Select an application or service card to open its store-style development detail sheet. The sheet can show approved artwork, type/category, development release channel, version information, access state, and the unavailable primary action.

Detail metadata uses vertically stacked label/value presentation so long values remain readable on compact widths instead of competing with their labels in one horizontal row.

The **Install** or **Open** action remains disabled because package/service delivery is not yet trusted or connected.

## Development status and integral GoreeCloud systems

Open the account menu and choose **Development status**, or use the development-status affordance on Discover, to inspect current integration boundaries. These diagnostics are development state, not production trust badges.

Status names are given flexible width while state capsules remain single-line so compact layouts do not force status text into unreadable vertical wrapping.

The status surface covers:

- **Glaze UI** — current design-system target is 2.0.0; conformance is not yet claimed.
- **GoreeCloud Identity** — production authentication/authorization integration is not connected.
- **Wardveil Security** — package trust and verification integration is not connected.
- **Privacy Shield** — production privacy-policy integration is not connected; development analytics are off.
- **Everkeep** — library/history recovery integration is not connected.
- **GoreeCloud Mesh** — lifecycle/catalog coordination transport is not connected.

## Privacy and security behavior

The development client does not collect analytics. Cleartext application traffic is disabled. It does not request Android package-install authority.

The development account selector, audience labels, versions, catalog package names, and service endpoints are not production policy or release metadata. Production package identities and endpoints will be populated only from approved authoritative sources.

Client-side catalog filtering is not the future sole authorization boundary. Production artifact access and service launch must be re-authorized by the responsible backend.

## Current limitations

The application currently has no production login, server-authoritative production catalog service, APK download/install flow, service-launch flow, production update delivery, installed-library reconciliation, production signing, Wardveil package-verification acceptance, Privacy Shield runtime acceptance, Everkeep runtime recovery acceptance, Mesh runtime event transport, or Stable Glaze UI conformance acceptance.

The compact-width corrections described above are source-validated but still require continued real-device review across supported screen sizes and font-scale/accessibility settings before any form-factor or Glaze UI conformance claim is made.

These limitations are deliberate fail-closed boundaries, not hidden features.

## Build requirements for developers

The application is configured for JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.0, compileSdk 37, targetSdk 36, minSdk 26, Kotlin/Compose compiler plugin 2.4.10, and Jetpack Compose BOM 2026.08.00.

A Gradle wrapper is not yet committed. With the required SDK and Gradle available, validate with:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Support boundary

Until a GoreeCloud App Store release is formally accepted, this manual describes the development build only. Production installation, upgrade, account, recovery, and service-access instructions will be added only when those behaviors actually exist and are validated.
