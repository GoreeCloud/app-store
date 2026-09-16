# Release Evidence Gate (Development)

## Purpose

This Development control adds fail-closed release-evidence prerequisites to GoreeCloud App Store package-delivery eligibility. It implements a bounded part of the September 16, 2026 App Store roadmap covering software supply-chain transparency, release transparency and revocation, release-evidence inspection, content-addressed package identity, and explainable install/update decisions.

A package candidate may become eligible for a future delivery handoff only when the package-delivery policy receives explicit accepted evidence for all of the following release facts:

- build provenance;
- software bill of materials (SBOM) evidence;
- release approval; and
- release revocation status.

`UNKNOWN` and `REJECTED` both fail closed. A missing `ReleaseEvidence` value defaults every release-evidence field to `UNKNOWN`, so old or incomplete call sites cannot accidentally bypass the gate.

## Exact artifact binding

Accepted release evidence must also be bound to the exact artifact SHA-256 carried by the package candidate. The candidate uses one canonical content-addressed identity form: exactly 64 lowercase hexadecimal characters representing SHA-256.

The policy rejects a candidate whose SHA-256 identity is malformed. It separately blocks release evidence when the evidence has no artifact SHA-256 binding or when the evidence binding is non-canonical or differs from the candidate artifact SHA-256. This prevents an otherwise accepted provenance/SBOM/release/revocation decision from being reused for a different package binary.

The policy compares identities only. It does not hash package bytes and does not establish that the supplied SHA-256 is truthful. A future authoritative download and verification path must calculate or otherwise obtain the artifact digest through an approved trust boundary, validate the actual package bytes, and supply accepted digest/evidence results to this policy.

## Explainable blockers

The pure policy exposes distinct blockers for release-evidence and artifact-binding failures:

- `BUILD_PROVENANCE_NOT_ACCEPTED`
- `SBOM_NOT_ACCEPTED`
- `RELEASE_APPROVAL_NOT_ACCEPTED`
- `REVOCATION_STATUS_NOT_ACCEPTED`
- `ARTIFACT_DIGEST_IDENTITY_INVALID`
- `RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING`
- `RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH`

These blockers are intended to support future user-visible and operator-visible explanations without converting the client into the authority that produces or approves the underlying evidence.

## Authority boundary

This increment consumes release-evidence acceptance results and an exact artifact identity only. It does **not**:

- hash or download package bytes;
- generate or validate build provenance;
- generate, fetch, parse, or approve an SBOM;
- approve a release;
- query an authoritative revocation service;
- replace digest, signature, catalog-binding, or Wardveil checks;
- prove that a caller-supplied SHA-256 matches a package binary;
- invoke Android `PackageInstaller`;
- request Android package-install authority;
- install, update, downgrade, roll back, or uninstall software;
- create production signing authority;
- establish production GoreeCloud Identity, Wardveil, Privacy Shield, Everkeep, Mesh, or Manager acceptance;
- establish Release Candidate, Production, or Stable status.

Future authoritative integrations must produce the accepted evidence and exact artifact digest from governed GoreeCloud release infrastructure and security authorities. Until those integrations exist, production package delivery remains unavailable.

## Roadmap relationship

This control advances the planned App Store v0.2 capability areas for:

- software supply-chain transparency;
- release transparency and revocation;
- release-evidence inspection;
- immutable/content-addressed package identity; and
- explainable install/update decisions.

The broader roadmap remains in progress and is not represented as implemented by this bounded policy change.

## Validation

The Development source validator `scripts/validate_release_evidence_gate.py` fails closed if the four release-evidence acceptance requirements, default-unknown model, canonical artifact SHA-256 identity, exact artifact-evidence binding, distinct blockers, documentation boundary, or no-install-authority boundary drift from this design.

Unit tests prove that each missing or rejected release-evidence fact blocks handoff, omitting release evidence blocks all release facts plus artifact binding, malformed artifact SHA-256 identities are rejected, missing artifact bindings are rejected, and accepted release evidence for one content digest cannot be reused for a different artifact digest.

## Rollback

The exact parent baseline for this artifact-binding increment is App Store Draft PR #19 head `af774127e637eeaeb2d9c4e923d1a5ec922f5e18`. Reverting this bounded child change restores the prior validated Development release-evidence gate without altering PR #19 or its parent stack.
