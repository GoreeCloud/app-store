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
- Wardveil package acceptance is accepted.

`UNKNOWN` is never treated as accepted.

## Action-specific rules

### Install

Install eligibility requires that no installed package/version state is already present. An existing installation must be evaluated as an update or rollback instead of being silently overwritten as a fresh install.

### Update

Update eligibility requires an installed package with the same exact package identity and an artifact version code strictly greater than the installed version code.

### Rollback

Rollback eligibility requires an installed package with the same exact package identity, an artifact version code strictly lower than the installed version code, and independently accepted rollback evidence.

A lower version number alone never authorizes rollback.

## Authority boundary

This policy does not replace or grant:

- server-side release/channel re-authorization;
- production GoreeCloud Identity claims or session acceptance;
- catalog publication authority;
- artifact download authority;
- Wardveil runtime/package inspection;
- Android PackageInstaller authority;
- protected production signing or signing-key recovery;
- Privacy Shield acceptance;
- Everkeep recovery acceptance;
- release approval, production acceptance, or Stable qualification.

The current `UnavailablePackageDeliveryGateway` remains unavailable. This policy is therefore a Development prerequisite for future delivery, not activation of delivery.

## Follow-on work

1. Define a production release-artifact schema with immutable package identity, version code/name, channel, compatibility, digest, signing provenance, publication state, and rollback metadata.
2. Bind package evidence to authoritative server responses rather than Development fixtures.
3. Integrate Wardveil package verification with explicit accepted/rejected/unknown evidence.
4. Add a constrained download boundary with checksum verification before any installer handoff.
5. Add Android PackageInstaller integration only after source policy, permissions, user confirmation, interruption, failure, update, rollback, and recovery behavior are accepted.
6. Validate representative physical devices, accessibility, protected signing/provenance, Everkeep recovery, and release gates before production use.
