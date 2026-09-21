#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
COORDINATOR = ROOT / "app/src/main/java/com/goreecloud/appstore/delivery/PackageDeliveryPreflightCoordinator.kt"
TEST = ROOT / "app/src/test/java/com/goreecloud/appstore/delivery/PackageDeliveryPreflightCoordinatorTest.kt"

def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise SystemExit(f"{label} missing required boundary: {needle}")

def forbid(text: str, needle: str, label: str) -> None:
    if needle in text:
        raise SystemExit(f"{label} contains forbidden delivery authority: {needle}")

coordinator = COORDINATOR.read_text(encoding="utf-8")
test = TEST.read_text(encoding="utf-8")

for needle in (
    "InstalledPackageObservationGateway",
    "PackageDeliveryPolicy.evaluate",
    "artifact.packageName",
    "toDeviceState",
):
    require(coordinator, needle, "coordinator")

for needle in (
    "PackageInstaller",
    "REQUEST_INSTALL_PACKAGES",
    "startActivity",
    "openConnection",
    "HttpURLConnection",
    "download",
):
    forbid(coordinator, needle, "coordinator")

require(test, "unobservedPackageCannotBecomeAcceptedAbsenceForFreshInstall", "test")
require(test, "INSTALLATION_STATE_NOT_ACCEPTED", "test")
require(test, "acceptedPositiveObservationCanMakeUpdateEligibleWithoutPackageMutation", "test")

print("Package delivery preflight boundary validated.")
