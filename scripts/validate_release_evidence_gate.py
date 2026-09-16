#!/usr/bin/env python3
"""Fail closed around package-delivery release-evidence prerequisites."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
POLICY = ROOT / "app/src/main/java/com/goreecloud/appstore/domain/PackageDeliveryPolicy.kt"
TEST = ROOT / "app/src/test/java/com/goreecloud/appstore/domain/PackageDeliveryPolicyTest.kt"
DOC = ROOT / "docs/release-evidence-policy.md"


def require(text: str, fragment: str, label: str) -> None:
    if fragment not in text:
        raise SystemExit(f"{label}: required fragment missing: {fragment!r}")


def forbid(text: str, fragment: str, label: str) -> None:
    if fragment in text:
        raise SystemExit(f"{label}: forbidden fragment present: {fragment!r}")


def main() -> None:
    manifest = MANIFEST.read_text(encoding="utf-8")
    policy = POLICY.read_text(encoding="utf-8")
    test = TEST.read_text(encoding="utf-8")
    doc = DOC.read_text(encoding="utf-8")

    require(policy, "data class ReleaseEvidenceRecord(", "delivery policy")
    require(policy, "data class ReleaseEvidence(", "delivery policy")
    require(policy, "data class EvidenceEvaluationContext(", "delivery policy")
    require(policy, "val evaluatedAtEpochSeconds: Long", "delivery policy")
    require(policy, "release: ReleaseEvidence = ReleaseEvidence()", "delivery policy")
    require(policy, "val sha256: String", "artifact candidate")
    require(policy, "Regex(\"^[0-9a-f]{64}$\")", "delivery policy")

    envelope_fields = [
        "val type: ReleaseEvidenceType",
        "val state: AcceptanceState = AcceptanceState.UNKNOWN",
        "val producerId: String? = null",
        "val authorityDomain: String? = null",
        "val producerAuthority: AcceptanceState = AcceptanceState.UNKNOWN",
        "val subjectPackageName: String? = null",
        "val artifactSha256: String? = null",
        "val contractVersion: String? = null",
        "val createdAtEpochSeconds: Long? = null",
        "val expiresAtEpochSeconds: Long? = null",
        "val sourceReference: String? = null",
    ]
    for field in envelope_fields:
        require(policy, field, "release evidence envelope")

    record_slots = {
        "buildProvenance: ReleaseEvidenceRecord? = null": "ReleaseEvidenceType.BUILD_PROVENANCE",
        "sbom: ReleaseEvidenceRecord? = null": "ReleaseEvidenceType.SBOM",
        "releaseApproval: ReleaseEvidenceRecord? = null": "ReleaseEvidenceType.RELEASE_APPROVAL",
        "revocationStatus: ReleaseEvidenceRecord? = null": "ReleaseEvidenceType.REVOCATION_STATUS",
    }
    for slot, expected_type in record_slots.items():
        require(policy, slot, "release evidence")
        require(policy, expected_type, "delivery policy")

    blockers = [
        "BUILD_PROVENANCE_NOT_ACCEPTED",
        "SBOM_NOT_ACCEPTED",
        "RELEASE_APPROVAL_NOT_ACCEPTED",
        "REVOCATION_STATUS_NOT_ACCEPTED",
        "ARTIFACT_DIGEST_IDENTITY_INVALID",
        "RELEASE_EVIDENCE_TYPE_MISMATCH",
        "RELEASE_EVIDENCE_PRODUCER_IDENTITY_MISSING",
        "RELEASE_EVIDENCE_AUTHORITY_DOMAIN_MISSING",
        "RELEASE_EVIDENCE_PRODUCER_AUTHORITY_NOT_ACCEPTED",
        "RELEASE_EVIDENCE_SUBJECT_SCOPE_MISSING",
        "RELEASE_EVIDENCE_SUBJECT_SCOPE_MISMATCH",
        "RELEASE_EVIDENCE_CONTRACT_VERSION_MISSING",
        "RELEASE_EVIDENCE_SOURCE_REFERENCE_MISSING",
        "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING",
        "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH",
        "EVIDENCE_EVALUATION_TIME_INVALID",
        "RELEASE_EVIDENCE_TIME_INVALID",
        "RELEASE_EVIDENCE_EXPIRED",
    ]
    for blocker in blockers:
        require(policy, blocker, "delivery policy")
        require(test, blocker, "unit tests")

    invariants = [
        "record.state != AcceptanceState.ACCEPTED",
        "record.type != expectedType",
        "record.producerId.isNullOrBlank()",
        "record.authorityDomain.isNullOrBlank()",
        "record.producerAuthority != AcceptanceState.ACCEPTED",
        "subjectPackageName != artifact.packageName",
        "record.contractVersion.isNullOrBlank()",
        "record.sourceReference.isNullOrBlank()",
        "releaseArtifactSha256 != artifact.sha256",
        "context.evaluatedAtEpochSeconds < createdAt",
        "context.evaluatedAtEpochSeconds >= expiresAt",
    ]
    for invariant in invariants:
        require(policy, invariant, "delivery policy")

    tests = [
        "missingReleaseEvidenceDefaultsMissingAndFailsClosed",
        "invalidArtifactDigestIdentityFailsClosed",
        "releaseEvidenceRequiresBoundArtifactDigest",
        "releaseEvidenceCannotBeReusedForDifferentArtifact",
        "releaseEvidenceRequiresProducerAttributionScopeContractAndSource",
        "releaseEvidenceTypeAndSubjectScopeMustMatchSlotAndCandidate",
        "expiredAndFutureDatedReleaseEvidenceFailsClosed",
        "evaluationTimeMustBeExplicitAndNonNegative",
    ]
    for name in tests:
        require(test, name, "unit tests")

    require(test, "development.release-evidence-fixture", "unit tests")
    require(test, "development-evidence-v1", "unit tests")
    forbid(policy, "fun acceptedFor(", "delivery policy")
    forbid(policy, "fun accepted(): ReleaseEvidence", "delivery policy")
    forbid(policy, "System.currentTimeMillis", "delivery policy")
    forbid(policy, "Instant.now", "delivery policy")

    doc_requirements = [
        "Missing, `UNKNOWN`, and `REJECTED` required evidence all fail closed",
        "producer/system identity",
        "producer authority domain",
        "explicit accepted producer-authority result",
        "evidence contract/version",
        "creation time",
        "expiry time",
        "evidence/source reference",
        "does not authenticate a producer",
        "does not read the Android/system wall clock",
        "production package delivery remains unavailable",
        "Platform Evidence Plane v1 — Integral Platform Systems",
    ]
    for fragment in doc_requirements:
        require(doc, fragment, "documentation")

    forbid(manifest, "android.permission.REQUEST_INSTALL_PACKAGES", "manifest")
    forbid(manifest, "android.permission.QUERY_ALL_PACKAGES", "manifest")

    print(
        "Release evidence gate validated: independent-records=4 "
        "artifact-sha256=canonical-and-bound producer-attribution=required "
        "producer-authority=required contract-and-source=required "
        "freshness-and-expiry=required explicit-time-context=true "
        "delivery-authority=false"
    )


if __name__ == "__main__":
    main()
