# Package Delivery Read-Only Preflight — Current-Main Recovery

## Status and scope

This Development tranche composes the already recovered exact-package installed-state observer with the pure package-delivery policy.

The coordinator performs one read-only package observation and returns a policy decision. It does not download package bytes, invoke Android PackageInstaller, request install-source permission, mutate packages, enumerate installed applications, or create release/production authority.

## Evaluation order

The coordinator:

1. observes only the exact artifact package identity;
2. converts that observation into PackageDeliveryPolicy.DeviceState;
3. evaluates the existing fail-closed policy using the supplied session, catalog item, artifact, evidence, action, SDK level, and explicit evidence evaluation time;
4. returns both the observation and the decision.

No delivery action is performed even when the decision is eligible for a future handoff.

## Fresh-install boundary

The current Android observation gateway cannot prove package absence because Android package visibility can hide an installed package.

NotObserved therefore remains UNKNOWN. When the coordinator evaluates INSTALL from that state, the pure policy produces INSTALLATION_STATE_NOT_ACCEPTED. The coordinator cannot manufacture DeviceState.observedAbsent() and cannot authorize fresh installation.

A later governed negative-observation authority remains required before fresh INSTALL can become eligible.

## Update and rollback boundary

An exact positive installed-package observation can provide accepted package/version state for UPDATE or ROLLBACK evaluation.

Eligibility still depends on all existing policy requirements, including catalog/artifact identity, channel authorization, SDK compatibility, digest/signature/Wardveil evidence, complete coherent release evidence, version ordering, and explicit rollback acceptance where applicable.

An eligible decision remains only a pre-handoff result. There is still no package mutation implementation.

## Remaining authority gaps

Authoritative catalog/artifact transport, package-byte hashing, trusted evidence producers, trusted time, Wardveil runtime verification, negative absence authority, PackageInstaller, recovery execution, release signing/provenance, representative-device acceptance, applicable Integral Platform Systems, Production Acceptance, and Stable qualification remain open.
