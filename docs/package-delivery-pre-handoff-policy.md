# Package Delivery Pre-Handoff Policy — Current-Main Recovery

## Status and scope

This Development tranche recovers the pure GoreeCloud App Store package-delivery eligibility policy onto the current-main control-plane line. It does not connect the policy to the Android user interface or any runtime package-delivery gateway.

A positive policy result means only that a candidate has satisfied the local pre-handoff invariants represented by the policy. It is not permission to download, install, update, downgrade, roll back, uninstall, publish, sign, or release software.

## Fail-closed inputs

The policy requires explicit accepted evidence for catalog/artifact binding, digest verification, signature verification, Wardveil acceptance, installation-state evidence, and four release-evidence records: build provenance, SBOM, release approval, and revocation status.

Required release-evidence records must preserve producer attribution, authority acceptance, package/artifact scope, contract version, source reference, creation/expiry time, and one coherent non-empty evidence-set identity. Unknown, missing, rejected, expired, future-dated, malformed, mismatched, or mixed-set evidence blocks handoff.

## Artifact and action invariants

The candidate artifact must use a canonical lowercase 64-character SHA-256 identity, exactly match catalog package/version/channel identity, and satisfy minimum SDK requirements.

Fresh install, update, and rollback are distinct decisions. Installation state must be explicitly accepted and internally consistent. Updates require the same package and a strictly newer version code. Rollbacks require the same package, a strictly older version code, and separate rollback acceptance.

## Authority boundary

This tranche deliberately adds no Android PackageInstaller call, install-source permission, download client, package mutation, broad installed-package enumeration, accepted negative installation-state observer, trusted-time provider, evidence producer authentication, package-byte hashing, signing authority, release approval, revocation service, or production platform-system integration.

The existing UnavailablePackageDeliveryGateway remains unavailable. No runtime call site is added by this tranche.

## Follow-on gates

A later bounded tranche must establish an accepted exact-package installation-state observation boundary before runtime update/rollback use. Fresh INSTALL additionally needs governed negative-observation authority that does not treat Android package-visibility ambiguity as verified absence.

Authoritative catalog/artifact delivery, Wardveil package verification, authenticated evidence transport, trusted time, Privacy Shield, Identity, Everkeep, Mesh, Manager, Policy, Observability, protected signing, representative-device acceptance, recovery/rollback, Release Candidate, Production Acceptance, and Stable qualification remain open.
