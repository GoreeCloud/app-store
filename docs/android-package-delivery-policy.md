# Android Package Delivery Eligibility Policy

**Status:** Development source policy  
**Scope:** Android application package pre-handoff only

## Purpose

The GoreeCloud App Store must never interpret catalog presence or a visible download channel as permission to install software.

`PackageDeliveryPolicy` is a pure pre-handoff decision boundary. It decides only whether a package candidate has enough accepted evidence to be considered by a future package-delivery implementation. It does not download, install, update, downgrade, roll back, or launch a package.

## Required positive evidence

Before an application package is eligible for handoff, all of the following must be true:

- the catalog item is an application rather than a service;
- the active session is explicitly entitled to the catalog item's release channel;
- the catalog has a non-empty package identity;
- artifact package identity exactly matches the catalog package identity;
- artifact version name exactly matches the catalog version;
- artifact release channel exactly matches the catalog release channel;
- the device API level satisfies the artifact minimum SDK requirement;
- catalog-to-artifact binding evidence is accepted;
- artifact digest verification is accepted;
- signing verification is accepted;
- Wardveil package acceptance is accepted;
- the installation-state observation used by the action is explicitly accepted and internally consistent.

`UNKNOWN` is never treated as accepted.

## Installed-state observation

`InstalledPackageObservationGateway` is a read-only prerequisite for future delivery. It accepts one explicit catalog package identity at a time and does not enumerate installed applications.

The Android lookup uses `PackageManager.getPackageInfo()` for that exact package identity. It does **not** request `QUERY_ALL_PACKAGES`, call installed-application/package enumeration APIs, install software, mutate package state, or grant PackageInstaller authority.

Android package visibility is intentionally treated as an authority boundary. `NameNotFoundException` or a visibility-related `SecurityException` is represented as **NotObserved / UNKNOWN**, not as proof that the package is absent. On modern Android, an installed package can be hidden from another application. Therefore an unobservable package must never become a fresh-install authorization by accident.

An exact visible installed package can contribute accepted package identity and version-code evidence for update/rollback evaluation. A future accepted source of negative installation-state evidence is still required before a fresh INSTALL can rely on observed absence. The current Android exact-package lookup does not manufacture that negative authority.

## Action-specific rules

### Install

Install eligibility requires **accepted evidence that no installed package/version is present**. Null installed fields without accepted installation-state evidence are UNKNOWN and block handoff. An existing installation must be evaluated as an update or rollback instead of being silently overwritten as a fresh install.

### Update

Update eligibility requires accepted installed-state evidence for an installed package with the same exact package identity and an artifact version code strictly greater than the installed version code.

### Rollback

Rollback eligibility requires accepted installed-state evidence for an installed package with the same exact package identity, an artifact version code strictly lower than the installed version code, and independently accepted rollback evidence.

A lower version number alone never authorizes rollback.

## Consistency rules

Installed package name and installed version code are an atomic observation. A partial state—package name without version code or version code without package name—is rejected as inconsistent even if the surrounding observation is labeled accepted.

## Authority boundary

This policy and observer do not replace or grant:

- server-side release/channel re-authorization;
- production GoreeCloud Identity claims or session acceptance;
- catalog publication authority;
- artifact download authority;
- broad installed-application inventory access;
- Wardveil runtime/package inspection;
- Android PackageInstaller authority;
- package install/update/uninstall mutation;
- protected production signing or signing-key recovery;
- Privacy Shield acceptance;
- Everkeep recovery acceptance;
- release approval, production acceptance, or Stable qualification.

The current `UnavailablePackageDeliveryGateway` remains unavailable. These components are Development prerequisites for future delivery, not activation of delivery.

## Follow-on work

1. Define an accepted source for negative installation-state evidence that can distinguish verified absence from Android package-visibility ambiguity without broad package enumeration.
2. Define a production release-artifact schema with immutable package identity, version code/name, channel, compatibility, digest, signing provenance, publication state, and rollback metadata.
3. Bind package evidence to authoritative server responses rather than Development fixtures.
4. Integrate Wardveil package verification with explicit accepted/rejected/unknown evidence.
5. Add a constrained download boundary with checksum verification before any installer handoff.
6. Add Android PackageInstaller integration only after source policy, visibility/installation-state evidence, permissions, user confirmation, interruption, failure, update, rollback, and recovery behavior are accepted.
7. Validate representative physical devices, accessibility, protected signing/provenance, Everkeep recovery, and release gates before production use.
