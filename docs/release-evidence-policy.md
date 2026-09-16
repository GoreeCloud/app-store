# Release Evidence Gate (Development)

## Purpose

This Development control adds a fail-closed release-evidence prerequisite to GoreeCloud App Store package-delivery eligibility. It is the first implementation increment from the September 16, 2026 App Store roadmap expansion covering software supply-chain transparency, release transparency and revocation, and release-evidence inspection.

A package candidate may become eligible for a future delivery handoff only when the package-delivery policy receives explicit accepted evidence for all of the following release facts:

- build provenance;
- software bill of materials (SBOM) evidence;
- release approval; and
- release revocation status.

`UNKNOWN` and `REJECTED` both fail closed. A missing `ReleaseEvidence` value defaults every release-evidence field to `UNKNOWN`, so old or incomplete call sites cannot accidentally bypass the new gate.

## Explainable blockers

The pure policy exposes a distinct blocker for each missing acceptance result:

- `BUILD_PROVENANCE_NOT_ACCEPTED`
- `SBOM_NOT_ACCEPTED`
- `RELEASE_APPROVAL_NOT_ACCEPTED`
- `REVOCATION_STATUS_NOT_ACCEPTED`

These blockers are intended to support future user-visible and operator-visible explanations without converting the client into the authority that produces or approves the evidence.

## Authority boundary

This increment consumes release-evidence acceptance results only. It does **not**:

- generate or validate build provenance;
- generate, fetch, parse, or approve an SBOM;
- approve a release;
- query an authoritative revocation service;
- replace digest, signature, catalog-binding, or Wardveil checks;
- download a package;
- invoke Android `PackageInstaller`;
- request Android package-install authority;
- install, update, downgrade, roll back, or uninstall software;
- create production signing authority;
- establish production GoreeCloud Identity, Wardveil, Privacy Shield, Everkeep, Mesh, or Manager acceptance;
- establish Release Candidate, Production, or Stable status.

Future authoritative integrations must produce the four acceptance results from governed GoreeCloud release infrastructure and security authorities. Until those integrations exist, production package delivery remains unavailable.

## Roadmap relationship

This increment begins implementation of the planned App Store v0.2 capability areas for:

- software supply-chain transparency;
- release transparency and revocation;
- release-evidence inspection; and
- explainable install/update decisions.

The broader roadmap remains in progress and is not represented as implemented by this bounded policy change.

## Validation

The Development source validator `scripts/validate_release_evidence_gate.py` fails closed if the four release-evidence blockers, default-unknown evidence model, explicit acceptance checks, documentation boundary, or no-install-authority boundary drift from this design.

Unit tests additionally prove that each missing or rejected release-evidence fact blocks handoff and that omitting release evidence blocks all four release-evidence requirements.

## Rollback

The exact parent baseline for this increment is App Store Draft PR #15 head `c6bf04c74d0fb13828fec1ce53b388005ef66f9c`. Reverting this bounded child change restores that prior Development source state without altering the parent branch.
