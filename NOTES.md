# GoreeCloud App Store — Development Notes

## Current stabilization context

- Repository lifecycle remains Development; Production Acceptance, Release Candidate, deployment, and Stable qualification are not established.
- Canonical GitHub repository identity is `GoreeCloud/app-store`.
- Authoritative `main` remains the current repository/documentation line. The validated package-delivery and release-evidence implementation work remains preserved in the older stacked Development PR lineage and is not accepted into `main`.
- Draft PR #28 proves that the preserved Development stack does not cleanly consolidate onto current `main`; it must not be force-merged or treated as current-main authority.
- Draft PR #25 remains exact-head Development evidence for the release-evidence-set correlation policy and Platform Contract 0.4 reconciliation on its own stacked lineage. Its green CI does not transfer automatically to current `main`.
- Current-main source is Android-only and still carries a historical/pre-reset `Glaze UI 2.0.0` label. This candidate adds a fresh Platform Contract 0.4 manifest and nine-system integration record, but it deliberately keeps GLAZE UI `applicable-migration-required` until V1.6 / 1.6.0 is implemented and accepted.
- Future implementation recovery should use fresh current-main branches and bounded independently validated tranches, preserving useful behavior without importing stale history wholesale.

## Active stabilization gates

- Current child candidate `stabilize/package-delivery-policy-current-main-20260921` recovers only the pure package-delivery pre-handoff policy and its unit tests onto the green current-main control-plane line. It adds no runtime call site, download client, PackageInstaller authority, installed-package observer, accepted absence authority, or production evidence transport. Exact-head CI is required independently of historical stacked validation.

- Recover required App Store implementation onto current authoritative `main` in bounded reviewable tranches.
- Reconcile the implemented presentation layer with current Official Stable GLAZE UI V1.6 / 1.6.0 and obtain application-specific rendered/accessibility/device acceptance.
- Establish accepted GoreeCloud Identity, Privacy Shield, Wardveil Security, Everkeep, Mesh, Manager, Policy, and Observability runtime integrations where applicable.
- Keep package-delivery decisions fail closed until authoritative catalog, artifact digest/signature, release evidence, revocation, installation-state, and rollback evidence are accepted.
- Complete representative Android runtime acceptance, recovery, protected signing, release provenance, deployment, Release Candidate qualification, and Stable qualification for current main. Recover Linux/Web only through separate governed current-main tranches if they remain required product clients.

## Maintenance boundary

Use this file for repository-local working observations and unresolved maintenance context. Promote durable requirements or authoritative decisions to their governed records. Do not record credentials, signing secrets, private tokens, or other reusable sensitive values here.
- Stacked child `stabilize/android-installed-state-observation-current-main-20260921` recovers only exact-package read-only Android installed-state observation on top of the package-policy child. NameNotFoundException and visibility-related SecurityException remain UNKNOWN rather than accepted absence; no QUERY_ALL_PACKAGES, <queries>, broad enumeration, PackageInstaller, download, or package-mutation authority is added. Fresh INSTALL therefore remains blocked on separate governed negative-observation authority. Exact-head CI is required independently.
- Stacked child `stabilize/package-delivery-preflight-current-main-20260921` composes the exact-package observer with the pure delivery policy as a read-only preflight. It can evaluate positive installed-state UPDATE/ROLLBACK eligibility but cannot manufacture accepted absence, so fresh INSTALL remains blocked. No PackageInstaller, download, package mutation, broad inventory, or runtime delivery call site is added. Exact-head CI is required independently.
