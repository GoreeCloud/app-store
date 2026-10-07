# Android Installed-State Observation — Development Package Delivery

## Status and scope

The App Store keeps installed-state observation exact-package and fail-closed. The ordinary/main and release-facing boundary does not enumerate installed applications and does not request `QUERY_ALL_PACKAGES`.

The bounded Development delivery source set adds visibility for one exact package identity, `com.goreecloud.gallery.dev`, through the debug manifest. That exact visibility declaration is the negative-observation authority for the first Development install path: Android `NameNotFoundException` may become accepted absence only for a package name the caller has explicitly declared observable.

This does not create broad installed-application inventory, production package authority, or permission to infer absence for arbitrary packages.

## Exact-package observation

The observer accepts one explicit package identity at a time and uses `PackageManager.getPackageInfo()` for that exact identity.

Malformed, trim-dependent, control-bearing, response-identity-mismatched, or invalid-version results fail closed. Installed-package enumeration APIs remain prohibited.

The main manifest does not request `QUERY_ALL_PACKAGES` and does not declare a package-visibility query. The debug manifest declares only the exact Gallery Development package used by the current delivery tranche.

## Package-visibility boundary

Android package visibility means `NameNotFoundException` is not globally equivalent to package absence.

Accordingly:

- for an exact package identity explicitly included in the lookup gateway's observable-package allowlist, `NameNotFoundException` becomes `InstalledPackageObservation.Absent` and then `DeviceState.observedAbsent(sdkInt)`;
- for every other package identity, the same result remains `NotObserved / UNKNOWN`;
- `SecurityException` always remains UNKNOWN;
- a platform failure always remains UNKNOWN.

This prevents package visibility ambiguity from silently authorizing fresh INSTALL while allowing the single query-visible Development package to prove absence without broad enumeration.

## Accepted positive observation

When Android returns the exact requested package identity with a non-negative version code, the observer produces accepted installed-state evidence. The delivery flow uses that evidence to distinguish an eligible UPDATE from a fresh INSTALL and to reject same-or-newer already-installed versions.

Positive observation alone never authorizes package mutation. Exact catalog/artifact identity, release-channel authorization, digest/signature/Wardveil acceptance, coherent release evidence, compatible SDK state, and applicable version-ordering requirements must still pass the existing `PackageDeliveryPolicy`.

## Development install boundary

Only the debug source set currently carries `REQUEST_INSTALL_PACKAGES`, and only the exact Gallery Development package is query-visible. The release build keeps the package-delivery gateway unavailable.

Before Android `PackageInstaller` is invoked, the Development client:

1. receives backend-re-authorized exact release metadata;
2. requires current Development Wardveil scan evidence;
3. downloads the artifact into private App Store cache;
4. verifies the exact APK SHA-256, package name, versionName/versionCode, and Development signing certificate;
5. evaluates the existing package-delivery preflight using the observed installed/absent state and correlated release evidence;
6. hands the package to Android only when the preflight is eligible.

Android user confirmation remains mandatory.

## Validation

Repository validation must continue to prove:

- no broad installed-package enumeration;
- no `QUERY_ALL_PACKAGES`;
- no package-visibility query in the main manifest;
- the debug query scope is exactly the bounded Gallery Development package;
- arbitrary unobserved packages remain UNKNOWN;
- explicitly observable exact absence may become ACCEPTED;
- release builds retain no package-install execution authority.

Unit tests cover exact installed identity/version preservation, observable absence, unobserved-to-UNKNOWN behavior, malformed identity rejection before platform lookup, mismatched response rejection, invalid version rejection, and platform failure remaining UNKNOWN.

This remains Development evidence only. Production installed-state authority, production package distribution, Production Acceptance, Seal/Anchor, and Stable qualification remain separate gates.
