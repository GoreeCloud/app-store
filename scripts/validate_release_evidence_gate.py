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

    require(policy, "data class ReleaseEvidence(", "delivery policy")
    require(policy, "buildProvenance: AcceptanceState = AcceptanceState.UNKNOWN", "delivery policy")
    require(policy, "sbom: AcceptanceState = AcceptanceState.UNKNOWN", "delivery policy")
    require(policy, "releaseApproval: AcceptanceState = AcceptanceState.UNKNOWN", "delivery policy")
    require(policy, "revocationStatus: AcceptanceState = AcceptanceState.UNKNOWN", "delivery policy")
    require(policy, "artifactSha256: String? = null", "delivery policy")
    require(policy, "release: ReleaseEvidence = ReleaseEvidence()", "delivery policy")
    require(policy, "val sha256: String", "artifact candidate")
    require(policy, "Regex(\"^[0-9a-f]{64}$\")", "delivery policy")
    require(policy, "ARTIFACT_DIGEST_IDENTITY_INVALID", "delivery policy")
    require(policy, "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING", "delivery policy")
    require(policy, "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH", "delivery policy")
    require(policy, "fun acceptedFor(artifact: ArtifactCandidate): ReleaseEvidence", "delivery policy")
    require(policy, "artifactSha256 = artifact.sha256", "delivery policy")
    require(policy, "releaseArtifactSha256 != artifact.sha256", "delivery policy")
    forbid(policy, "fun accepted(): ReleaseEvidence", "delivery policy")

    checks = {
        "evidence.release.buildProvenance != AcceptanceState.ACCEPTED": "BUILD_PROVENANCE_NOT_ACCEPTED",
        "evidence.release.sbom != AcceptanceState.ACCEPTED": "SBOM_NOT_ACCEPTED",
        "evidence.release.releaseApproval != AcceptanceState.ACCEPTED": "RELEASE_APPROVAL_NOT_ACCEPTED",
        "evidence.release.revocationStatus != AcceptanceState.ACCEPTED": "REVOCATION_STATUS_NOT_ACCEPTED",
    }
    for expression, blocker in checks.items():
        require(policy, expression, "delivery policy")
        require(policy, blocker, "delivery policy")
        require(test, blocker, "unit tests")

    require(test, "missingReleaseEvidenceDefaultsUnknownAndFailsClosed", "unit tests")
    require(test, "ReleaseEvidence.acceptedFor(artifact)", "unit tests")
    require(test, "invalidArtifactDigestIdentityFailsClosed", "unit tests")
    require(test, "releaseEvidenceRequiresBoundArtifactDigest", "unit tests")
    require(test, "releaseEvidenceCannotBeReusedForDifferentArtifact", "unit tests")
    require(test, "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING", "unit tests")
    require(test, "RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH", "unit tests")

    require(doc, "`UNKNOWN` and `REJECTED` both fail closed", "documentation")
    require(doc, "exact artifact SHA-256", "documentation")
    require(doc, "content-addressed identity", "documentation")
    require(doc, "does not hash package bytes", "documentation")
    require(doc, "generate or validate build provenance", "documentation")
    require(doc, "query an authoritative revocation service", "documentation")
    require(doc, "production package delivery remains unavailable", "documentation")

    forbid(manifest, "android.permission.REQUEST_INSTALL_PACKAGES", "manifest")
    forbid(manifest, "android.permission.QUERY_ALL_PACKAGES", "manifest")

    print(
        "Release evidence gate validated: provenance=required sbom=required "
        "release-approval=required revocation-status=required "
        "artifact-sha256=canonical-and-bound delivery-authority=false"
    )


if __name__ == "__main__":
    main()
