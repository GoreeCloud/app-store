# Android Installed-State Observation — Current-Main Recovery

## Status and scope

This Development tranche recovers one exact-package, read-only Android installation-state observation boundary onto the current App Store current-main stack. It exists only to provide accepted positive installed-package identity/version evidence to the already recovered pure package-delivery pre-handoff policy.

It does not authorize downloads, package installation, update execution, rollback execution, uninstall, package mutation, or broad installed-application inventory.

## Exact-package observation

The production-facing seam accepts one explicit package identity at a time. The Android adapter uses PackageManager.getPackageInfo() for that exact package name.

The observer does not call installed-application or installed-package enumeration APIs and the manifest does not request QUERY_ALL_PACKAGES or declare a <queries> block for this tranche.

Malformed, trim-dependent, control-bearing, mismatched, or invalid-version responses fail closed.

## Package-visibility boundary

Android package visibility means NameNotFoundException cannot safely be interpreted as proof that a package is absent. A visibility-related SecurityException has the same limitation.

Both conditions therefore become NotObserved / UNKNOWN. The adapter converts UNKNOWN observation into DeviceState.unobserved(sdkInt), so the package-delivery policy cannot treat an unobservable package as accepted absence.

This current Android exact-package lookup does not manufacture that negative authority. Fresh INSTALL remains blocked until a separately governed negative-observation authority can distinguish verified absence from visibility ambiguity without broad enumeration.

## Accepted positive observation

When Android returns the exact requested package identity with a non-negative version code, the observer can produce accepted installed-state evidence for update or rollback policy evaluation.

That positive observation does not itself authorize update or rollback. The package-delivery policy still requires exact catalog/artifact identity, release-channel authorization, digest/signature/Wardveil acceptance, coherent release evidence, version ordering, and rollback acceptance where applicable.

## Authority boundary

This tranche adds no PackageInstaller use, REQUEST_INSTALL_PACKAGES permission, download client, package bytes, signing authority, release approval, revocation service, trusted-time authority, broad app inventory, or production evidence transport.

The existing UnavailablePackageDeliveryGateway remains unavailable and this branch adds no UI/runtime call site that can mutate packages.

## Validation and follow-up

The repository validator must prove exact-package lookup only, no broad enumeration, no package-install authority, UNKNOWN handling for unobserved packages, and the retained fail-closed installation-state policy.

Unit tests must prove exact installed identity/version preservation, unobserved-to-UNKNOWN behavior, malformed identity rejection before platform lookup, mismatched response rejection, invalid version rejection, and platform failure remaining UNKNOWN.

A later tranche may wire this observer into a governed runtime decision path only after its source boundary is independently accepted. Fresh INSTALL still requires separate accepted negative-observation authority.
