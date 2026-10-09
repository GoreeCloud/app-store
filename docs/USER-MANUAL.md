# GoreeCloud App Store User Manual

## Current status

GoreeCloud App Store is currently an **active-development Android application**. It is not production-ready. This Development candidate implements one bounded Gallery APK download/install path when the debug build is configured for the authorized backend and the active fixture identity has the Development-channel grant; GoreeCloud service launch and production software distribution remain unavailable.

The present application validates the native store experience, multi-user catalog behavior, first-party GoreeCloud artwork consumption, responsive store presentation, and GoreeCloud platform integration boundaries before real distribution is enabled.

`productionAcceptance` remains `false`.

## Development APKs

There is no production or Stable end-user App Store release yet.

The repository CI can produce a development/debug APK for an exact source revision after unit tests, Android lint, APK assembly, package/application-label verification, development signing-certificate verification, SHA-256 evidence generation, and artifact publication succeed. Treat those artifacts as test builds only.

Current development/debug builds use:

- application ID `com.goreecloud.appstore.dev`;
- Android label **GoreeCloud App Store Dev**;
- development candidate version line `0.1.15-dev` / version code `16`;
- the repository-managed, development-only signing certificate documented in `development/signing/README.md`.

The development package is intentionally separate from the reserved future production application ID `com.goreecloud.appstore`. The development signing identity is non-production test material and must never sign the production package or a Stable artifact.

### If an older bootstrap is still installed

Early bootstrap CI APKs used the production-reserved package name `com.goreecloud.appstore` while Android CI generated a different ephemeral debug certificate on each runner. Android therefore cannot replace one of those old bootstrap installations with a later CI APK signed by another runner.

If the device still shows the large **Development identity adapter** panel, letter-only G/I/M artwork, text-glyph bottom navigation, **Search what is available to you**, or the large **Platform integration checkpoint** inside normal browsing, that is the old bootstrap application.

Remove that older bootstrap from the test device, or leave it installed only if you intentionally want to compare it. Install and launch **GoreeCloud App Store Dev** for current testing. The `.dev` package can coexist with the old package because they have different Android application IDs.

Future development artifacts are intended to retain the same development package and signing identity so they can update earlier `.dev` installations, subject to normal Android version-code rules.

## First-use guidance

The first launch presents a concise three-step guide with visible progress near the top of the usable screen. It explains the entitled catalog, distinguishes currently available browsing/status functions from install/update/production-service actions that remain disconnected, and lets you enable or disable contextual tips. Back/Continue/**Start browsing** navigation stays anchored to the bottom safe area while step content remains independently scrollable for constrained or accessibility-scaled layouts. Step 1 omits the inactive Back action. Progress is persisted so an interrupted setup resumes at the last durable step, and the guide can be replayed later from **Guidance & setup**.

## Development account switcher

The persistent account control in the App Store header offers concise Development labels such as **Standard**, **Preview**, **Admin**, **Developer**, and **Signed out**. The active identity is marked in the account menu.

These are not real GoreeCloud accounts, groups, or production roles. They are local fixtures used to demonstrate how different logins can receive different App Store catalogs while production GoreeCloud Identity integration is still pending.

Changing the development identity immediately recalculates which catalog entries are visible and returns the current section to its top. An entry for which the active session is not entitled is concealed from visible lists and search results.

A separate unmerged Development security-hardening candidate also closes product details on identity changes, resolves selected details from the new identity's entitled catalog, and ignores stale delivery responses from previous sessions or an earlier opening of the same product. This is client-side defense in depth; it does not replace backend release authorization.

The active Development identity is shown in the compact header subtitle, while the account menu trigger is icon-sized so labels such as **Standard** do not truncate on narrow phones. The same concise labels remain in the menu and still map to local Development fixture subjects; they are not production roles.

## Store sections

### Discover

Shows all development catalog entries currently available to the active development identity. The hero count is labeled **available** because it reflects the active identity’s entitlement-scoped view rather than the full Development catalog. Development status is available from the account menu instead of consuming Discover browsing space.

The available-item count is presented below the section heading so compact-width and larger-text layouts do not force the count over the heading. Singular and plural labels are handled separately.

### Apps

Shows only entitled application entries. Most entries remain browsing-only. The first bounded Development delivery entry is GoreeCloud Gallery `0.8.11-dev`; it becomes installable only for the **Developer** fixture identity when the debug build has an authorized backend configuration and every backend/client verification gate passes.

#### Development Gallery installation

To exercise the first delivery path:

1. Use a GoreeCloud App Store Dev build compiled with the Development backend base URL, bearer token, and exact TLS certificate SHA-256 pin.
2. Switch the local account fixture to **Developer**. Other fixtures do not have the `channel:development` grant and the backend independently rejects subjects other than `dev:developer`.
3. Open **GoreeCloud Gallery**. The detail sheet checks the exact Development release and current Wardveil scan evidence.
4. Select **Download & install**. The APK is stored only in private App Store cache while its size, SHA-256, package name, versionName/versionCode, and Development signing certificate are verified.
5. If Android asks whether GoreeCloud App Store Dev may install unknown apps, explicitly enable that Android-controlled permission for the Development App Store, return to the detail sheet, and select the install action again.
6. Complete Android's installation confirmation. The App Store cannot silently bypass the platform confirmation. Return to the detail sheet and use **Check install status** to reconcile Android's installed package/version state.

The backend is read-only and does not accept uploads. A failed authorization, stale/unhealthy Wardveil result, digest mismatch, signing mismatch, TLS-pin mismatch, incompatible SDK, missing permission, or Android install failure blocks the flow. This path does not make the Gallery artifact Production Accepted, Stable, Sealed, or Anchored.

### Services

Shows only entitled GoreeCloud service entries. Opening services is currently disabled until production GoreeCloud Identity authorization and approved service-endpoint policy are connected.

### Updates

Shows a centered compact disconnected-state card rather than repeating the same warning at the top of the screen. Update discovery and delivery have not yet been connected; authenticated release metadata and package delivery remain required before update actions appear.

### Library

Shows device-local **Favorites** and **Saved for later** collections for the active Development identity, plus a bounded **Recently opened** collection for the current App Store session and a compact **Installed history** status row. The Library includes compact collection counts and a single search field across local collections. Recently opened items remain in memory only, are separated by Development identity, are re-filtered through current entitlements, and are not restored as durable browsing history. Installed-library history and Everkeep-backed cross-device recovery have not yet been connected.

## Search

Use the section-specific search field (**Search apps and services**, **Search apps**, or **Search services**) to filter the current entitled section by application/service name, summary, or category. The Android Search keyboard action dismisses the keyboard while keeping the current live-filtered results in place. Use the compact sort control beside **Browse all**, **Apps**, or **Services** to switch between authoritative catalog order, alphabetical name order, and category-then-name order. Sorting never widens entitlement visibility. When a query is active, use the trailing clear-search action to reset it immediately. A selected category shows a close glyph and can be tapped again to clear it. If search or category filtering produces no matches, use **Reset** in the empty-result card to clear both filters in one action.

Search operates only on entries already available to the active development identity. It does not reveal entries that were filtered out by entitlement rules. **Search your library** applies the same entitlement boundary while filtering Favorites, Saved for later, and Recently opened together.

## Application and service artwork

Every current catalog application and service uses a native VectorDrawable derivative tied to a canonical official asset in `GoreeCloud/branding-assets`; empty, lettered, monogram, and generic placeholder artwork is prohibited.

The branding repository remains authoritative. Copies in this App Store repository are implementation derivatives only. See `BRANDING.md` for the exact canonical asset and Git-blob mappings.

## Catalog cards and release channels

Catalog cards show official artwork, name, a compact summary, type/category metadata, local Favorite/Saved-for-later state glyphs when applicable, and a product-navigation affordance. Development channel pills are suppressed on Development rows because the entire build already carries that context; non-Development channel pills remain available for future mixed-channel catalogs. Channel/version metadata is additionally gated by the active Development identity's explicit channel grant; catalog entitlement or an administrator fixture alone does not reveal a channel the identity is not authorized to inspect.

The card layout gives primary text flexible width and uses a single-line summary plus a compact metadata line. On compact widths, long metadata is ellipsized rather than forcing wrapping or pushing navigation controls off screen. Product titles may use up to two lines when needed.

## Product details

Select an application or service card to open its store-style development detail sheet. The sheet can show approved artwork, type/category, development release channel, version information, access state, compact Favorite/Save controls, and an actionable availability-status card.

Detail metadata uses vertically stacked label/value presentation so long values remain readable on compact widths instead of competing with their labels in one horizontal row. Favorites and saved items share one concise device-local identity boundary instead of repeating separate explanatory blocks. If the active identity lacks the item's channel grant, version and channel fields are omitted rather than exposing restricted release metadata.

For a delivery-enabled Development application, the detail sheet checks backend authorization and current security evidence before showing **Download & install**. The current first package is GoreeCloud Gallery `0.8.11-dev`. The backend verifies exact artifact metadata and requires a current clean Wardveil ClamAV reference result; the Android client then re-verifies the downloaded SHA-256, package/version identity, and Development signing certificate before opening Android PackageInstaller. Android user confirmation is still required. Apps without an explicit Development release, identities without the release-channel grant, and all services remain unavailable and surface the applicable boundary instead of pretending an action is possible.

## Development status and integral GoreeCloud systems

Open the account menu and choose **Development status**, or use a contextual Development-status action from disconnected Updates/Library surfaces, to inspect current integration boundaries. These diagnostics are development state, not production trust badges.

Status entries use compact integration cards. System names retain flexible width while state capsules remain single-line so compact layouts do not force status text into unreadable vertical wrapping.

The status surface covers:

- **Glaze** — current user-facing system identity; this App Store remains targeted at the historical **GLAZE UI V1.6 / 1.6.0 Official Anchor**, and rendered conformance is not yet claimed.
- **GoreeCloud Identity** — production authentication/authorization integration is not connected.
- **Wardveil Security** — the bounded Gallery Development path consumes the current Wardveil ClamAV reference adapter and scanner-health gate; production Wardveil runtime acceptance and broader package-protection authority remain unconnected.
- **Privacy Shield** — production privacy-policy integration is not connected; development analytics are off.
- **Everkeep** — library/history recovery integration is not connected.
- **GoreeCloud Mesh** — lifecycle/catalog coordination transport is not connected.

## Privacy and security behavior

The development client does not collect analytics. Cleartext application traffic is disabled. The ordinary/main and release manifests do not request Android package-install authority; the debug-only Development delivery source set requests `REQUEST_INSTALL_PACKAGES` solely for the exact bounded package-delivery path and still requires Android user approval.

The development account selector, audience labels, versions, catalog package names, and service endpoints are not production policy or release metadata. Production package identities and endpoints will be populated only from approved authoritative sources.

Client-side catalog filtering is not the future sole authorization boundary. Production artifact access and service launch must be re-authorized by the responsible backend.

## Current limitations

The application currently has no production login, server-authoritative production catalog service, production package-distribution flow, service-launch flow, production update delivery, installed-library reconciliation, production signing, production Wardveil acceptance, Privacy Shield runtime acceptance, Everkeep runtime recovery acceptance, Mesh runtime event transport, or Stable Glaze UI conformance acceptance. The Development Gallery path is deliberately narrower: one exact package, one Development identity subject, pinned TLS, exact digest/package/signing checks, a current Wardveil ClamAV reference scan, and Android user-approved PackageInstaller execution.

The compact-width and onboarding/status refinements described above remain Development work and still require continued real-device review across supported screen sizes and font-scale/accessibility settings before any form-factor or Glaze conformance claim is made.

These limitations are deliberate fail-closed boundaries, not hidden features.

## Build requirements for developers

The application is configured for JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.0, compileSdk 37, targetSdk 36, minSdk 26, Kotlin/Compose compiler plugin 2.4.10, and Jetpack Compose BOM 2026.08.00.

A Gradle wrapper is not yet committed. With the required SDK and Gradle available, validate with:

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Support boundary

Until a GoreeCloud App Store release is formally accepted, this manual describes the development build only. Production installation, upgrade, account, recovery, and service-access instructions will be added only when those behaviors actually exist and are validated.
