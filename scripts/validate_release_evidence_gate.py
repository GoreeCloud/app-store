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
    require(policy, "release: ReleaseEvidence = ReleaseEvidence()", "delivery policy")

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
    require(test, "release = ReleaseEvidence.accepted()", "unit tests")

    require(doc, "`UNKNOWN` and `REJECTED` both fail closed", "documentation")
    require(doc, "generate or validate build provenance", "documentation")
    require(doc, "query an authoritative revocation service", "documentation")
    require(doc, "production package delivery remains unavailable", "documentation")

    forbid(manifest, "android.permission.REQUEST_INSTALL_PACKAGES", "manifest")
    forbid(manifest, "android.permission.QUERY_ALL_PACKAGES", "manifest")

    print(
        "Release evidence gate validated: provenance=required sbom=required "
        "release-approval=required revocation-status=required delivery-authority=false"
    )


if __name__ == "__main__":
    main()
