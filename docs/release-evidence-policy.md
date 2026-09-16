# Release Evidence Gate (Development)

## Purpose

This Development control adds fail-closed release-evidence prerequisites to GoreeCloud App Store package-delivery eligibility. It implements a bounded part of the September 16, 2026 App Store roadmap covering software supply-chain transparency, release transparency and revocation, release-evidence inspection, content-addressed package identity, explainable install/update decisions, and evidence provenance/freshness.

A package candidate may become eligible for a future delivery handoff only when the package-delivery policy receives explicit accepted evidence for all of the following release facts:

- build provenance;
- software bill of materials (SBOM) evidence;
- release approval; and
- release revocation status.

Missing, `UNKNOWN`, and `REJECTED` required evidence all fail closed. `ReleaseEvidence()` contains no implicitly accepted records, and the production domain model intentionally provides no helper that manufactures accepted release evidence.

## Exact artifact binding

Every release-evidence record must be bound to the exact artifact SHA-256 carried by the package candidate. The candidate uses one canonical content-addressed identity form: exactly 64 lowercase hexadecimal characters representing SHA-256.

The policy rejects a candidate whose SHA-256 identity is malformed. It separately blocks release evidence when an evidence record has no artifact SHA-256 binding or when the evidence binding is non-canonical or differs from the candidate artifact SHA-256. This prevents an otherwise accepted provenance/SBOM/release/revocation decision from being reused for a different package binary.

The policy compares identities only. It does not hash package bytes and does not establish that the supplied SHA-256 is truthful. A future authoritative download and verification path must calculate or otherwise obtain the artifact digest through an approved trust boundary, validate the actual package bytes, and supply accepted digest/evidence results to this policy.

## Producer-attributed evidence envelope

Each of the four required release facts is carried as an independent `ReleaseEvidenceRecord`. A record must preserve the minimum local envelope properties needed by the current GoreeCloud Platform Evidence Plane and mandatory Platform-System evidence rules:

- the expected release-evidence type;
- the producer/system identity;
- the producer authority domain;
- an explicit accepted producer-authority result supplied by a governed integration;
- the subject package identity;
- the exact artifact SHA-256 scope;
- the evidence contract/version;
- creation time;
- expiry time;
- and an evidence/source reference.

The App Store does not authenticate a producer merely because a string identifies one, and it does not decide that a producer owns an authority domain. `producerAuthority` is itself an acceptance result that must come from a future governed Identity/Mesh/security/release-evidence integration. The policy only fails closed when that acceptance is missing or non-passing and checks that the remaining envelope fields are present and consistent with the candidate.

The four release facts remain independent records. A passing build-provenance record does not upgrade a missing, rejected, unknown, stale, expired, or malformed SBOM, release-approval, or revocation-status record.

## Freshness and time boundary

Evidence creation and expiry are evaluated against an explicit caller-supplied `EvidenceEvaluationContext`. The pure policy does not read the Android/system wall clock.

A release-evidence record fails closed when:

- creation or expiry time is missing;
- creation time is negative;
- expiry is not later than creation;
- the evaluation time precedes creation;
- the evaluation time is at or after expiry; or
- the evaluation context itself uses a negative epoch value.

This prevents stale evidence replay inside the policy boundary while keeping clock acquisition and trusted-time decisions outside the App Store domain model. Future runtime integration remains responsible for supplying an appropriate current-time value through an approved trust boundary.

## Explainable blockers

The pure policy exposes distinct blockers for release-fact acceptance, content identity, producer attribution, authority, scope, contract/source metadata, and freshness failures. They include:

- `BUILD_PROVENANCE_NOT_ACCEPTED`
- `SBOM_NOT_ACCEPTED`
- `RELEASE_APPROVAL_NOT_ACCEPTED`
- `REVOCATION_STATUS_NOT_ACCEPTED`
- `ARTIFACT_DIGEST_IDENTITY_INVALID`
- `RELEASE_EVIDENCE_TYPE_MISMATCH`
- `RELEASE_EVIDENCE_PRODUCER_IDENTITY_MISSING`
- `RELEASE_EVIDENCE_AUTHORITY_DOMAIN_MISSING`
- `RELEASE_EVIDENCE_PRODUCER_AUTHORITY_NOT_ACCEPTED`
- `RELEASE_EVIDENCE_SUBJECT_SCOPE_MISSING`
- `RELEASE_EVIDENCE_SUBJECT_SCOPE_MISMATCH`
- `RELEASE_EVIDENCE_CONTRACT_VERSION_MISSING`
- `RELEASE_EVIDENCE_SOURCE_REFERENCE_MISSING`
- `RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING`
- `RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH`
- `EVIDENCE_EVALUATION_TIME_INVALID`
- `RELEASE_EVIDENCE_TIME_INVALID`
- `RELEASE_EVIDENCE_EXPIRED`

These blockers are intended to support future user-visible and operator-visible explanations without converting the client into the authority that produces, authenticates, or approves the underlying evidence.

## Authority boundary

This increment consumes producer-attributed release-evidence acceptance results, exact artifact identity, and explicit time context only. It does **not**:

- hash or download package bytes;
- generate or validate build provenance;
- generate, fetch, parse, or approve an SBOM;
- approve a release;
- query an authoritative revocation service;
- authenticate an evidence producer;
- grant producer authority or establish that an authority-domain label is truthful;
- obtain trusted time or prove that caller-supplied time is authoritative;
- dereference, retrieve, or validate an evidence source reference;
- replace digest, signature, catalog-binding, or Wardveil checks;
- prove that a caller-supplied SHA-256 matches a package binary;
- invoke Android `PackageInstaller`;
- request Android package-install authority;
- install, update, downgrade, roll back, or uninstall software;
- create production signing authority;
- establish production GoreeCloud Identity, Wardveil, Privacy Shield, Everkeep, Mesh, Manager, or Platform Evidence Plane acceptance;
- establish Release Candidate, Production, or Stable status.

Future authoritative integrations must produce and authenticate evidence, establish producer authority, calculate/verify the exact artifact digest, provide appropriate source references and trusted-time context, and supply accepted results from governed GoreeCloud release infrastructure and Platform Systems. Until those integrations exist and are accepted, production package delivery remains unavailable.

## Platform Evidence Plane relationship

This Development model consumes a bounded subset of the envelope properties defined by `Platform Evidence Plane v1 — Integral Platform Systems` v1.1: producer identity/authority domain, subject scope, evidence type, contract/version, result, creation/expiry, and source reference.

It does not claim full Platform Evidence Plane runtime integration. There is no authenticated evidence transport, GoreeCloud Mesh production delivery, GoreeCloud Identity producer verification, GoreeCloud Manager aggregation, or production producer adapter in this increment. The local model exists so future integrations cannot pass a bare favorable boolean into package-delivery eligibility without preserving the evidence context required to evaluate it safely.

## Roadmap relationship

This control advances the planned App Store capability areas for:

- software supply-chain transparency;
- release transparency and revocation;
- release-evidence inspection;
- immutable/content-addressed package identity;
- evidence provenance and freshness; and
- explainable install/update decisions.

The broader roadmap remains in progress and is not represented as implemented by this bounded policy change.

## Validation

The Development source validator `scripts/validate_release_evidence_gate.py` fails closed if the four independent release-evidence records, producer attribution, accepted producer authority, contract/source metadata, package/digest scope, explicit evaluation-time model, freshness/expiry checks, documentation boundary, or no-install-authority boundary drift from this design.

Unit tests prove that each missing or rejected release fact blocks handoff; omitted release evidence blocks all four required facts; malformed artifact SHA-256 identities are rejected; release evidence for one content digest cannot be reused for another; producer identity/domain/authority, subject scope, contract version, and source references are mandatory; type/scope mismatches fail closed; future-dated and expired records fail closed; and evaluation time must be explicit and non-negative.

## Rollback

The exact parent baseline for this evidence-envelope increment is App Store Draft PR #21 head `8b24d12c643fc8f9bb74a99cb59e6a566a8fde47`. Reverting this bounded child change restores the prior validated artifact-bound release-evidence policy without altering PR #21 or its parent stack.
