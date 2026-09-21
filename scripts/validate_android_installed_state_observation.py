#!/usr/bin/env python3
"""Fail closed around Android installed-package observation and delivery-state evidence."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
OBSERVER = ROOT / "app/src/main/java/com/goreecloud/appstore/delivery/InstalledPackageObservationGateway.kt"
POLICY = ROOT / "app/src/main/java/com/goreecloud/appstore/domain/PackageDeliveryPolicy.kt"
DOC = ROOT / "docs/android-package-delivery-policy.md"


def require(text: str, fragment: str, label: str) -> None:
    if fragment not in text:
        raise SystemExit(f"{label}: required fragment missing: {fragment!r}")


def forbid(text: str, fragment: str, label: str) -> None:
    if fragment in text:
        raise SystemExit(f"{label}: forbidden fragment present: {fragment!r}")


def main() -> None:
    manifest = MANIFEST.read_text(encoding="utf-8")
    observer = OBSERVER.read_text(encoding="utf-8")
    policy = POLICY.read_text(encoding="utf-8")
    doc = DOC.read_text(encoding="utf-8")

    forbid(manifest, "android.permission.QUERY_ALL_PACKAGES", "manifest")
    forbid(manifest, "<queries>", "manifest")

    require(observer, "fun observe(packageName: String)", "observer")
    require(observer, "getPackageInfo(packageName", "observer")
    require(observer, "InstalledPackageLookupResult.NotObserved", "observer")
    require(observer, "NOT_FOUND_OR_NOT_VISIBLE", "observer")
    require(observer, "DeviceState.unobserved(sdkInt)", "observer")
    for forbidden in (
        "getInstalledApplications",
        "getInstalledPackages",
        "queryIntentActivities",
        "QUERY_ALL_PACKAGES",
        "PackageInstaller",
        "requestInstallPackages",
    ):
        if forbidden == "QUERY_ALL_PACKAGES":
            # The explanatory source comment may name the permission; executable authority may not.
            continue
        forbid(observer, forbidden, "observer")

    require(policy, "INSTALLATION_STATE_NOT_ACCEPTED", "delivery policy")
    require(policy, "INSTALLATION_STATE_INCONSISTENT", "delivery policy")
    require(policy, "installationState: AcceptanceState = AcceptanceState.UNKNOWN", "delivery policy")
    require(policy, "fun observedAbsent", "delivery policy")
    require(policy, "fun observedInstalled", "delivery policy")
    require(policy, "fun unobserved", "delivery policy")
    require(policy, "if (!installationStateAccepted)", "delivery policy")
    require(policy, "if (!installationStateConsistent)", "delivery policy")

    require(doc, "NameNotFoundException", "documentation")
    require(doc, "NotObserved / UNKNOWN", "documentation")
    require(doc, "does **not** request `QUERY_ALL_PACKAGES`", "documentation")
    require(doc, "current Android exact-package lookup does not manufacture that negative authority", "documentation")

    print(
        "Android installed-state boundary validated: exact-package lookup only; "
        "broad-enumeration=false negative-observation=false package-installer=false"
    )


if __name__ == "__main__":
    main()
