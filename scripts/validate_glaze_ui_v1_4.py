#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.4.0"
TAG = "v1.4.0"
REVISION = "84cb3db4884042f0fa25ed6d475a127fb110f596"
ROLLBACK_VERSION = "1.3.0"
ROLLBACK_REVISION = "ff34f232f295c9dcb07e4c681f66d4104d0b9323"
MATERIAL_RULE = (
    "Neutral glass remains the material foundation. Optical adaptation is contextual, bounded, "
    "and subordinate to meaning, accessibility, privacy, security, and task completion."
)
APP_VERSION = "0.1.5-dev"
APP_VERSION_CODE = "6"


def require(value: bool, message: str) -> None:
    if not value:
        raise SystemExit(f"GLAZE UI V1.4 validation failed: {message}")


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def main() -> None:
    adoption = json.loads(read("contracts/glaze-ui-adoption.json"))
    integrations = json.loads(read("contracts/platform-integrations.json"))
    web_contract = json.loads(read("contracts/web-distribution.json"))

    require(adoption.get("status") == "adoption-candidate", "status must remain adoption-candidate")
    require(adoption.get("targetVersion") == VERSION, "targetVersion mismatch")
    require(adoption.get("requiredTargetVersion") == VERSION, "requiredTargetVersion mismatch")
    require(adoption.get("stableReleaseTag") == TAG, "release tag mismatch")
    require(adoption.get("stableReleaseRevision") == REVISION, "release revision mismatch")
    require(adoption.get("sourceQualificationAnchor") == REVISION, "exact V1.4 source pin mismatch")
    require(adoption.get("stableContract") == "GLAZE_UI_V1_4.md", "Stable contract mismatch")
    require(adoption.get("materialRule") == MATERIAL_RULE, "material rule mismatch")
    require(adoption.get("platforms") == ["android", "linux", "web"], "supported Glaze platform set mismatch")

    rollback = adoption.get("rollback", {})
    require(rollback.get("version") == ROLLBACK_VERSION, "rollback version mismatch")
    require(rollback.get("stableReleaseRevision") == ROLLBACK_REVISION, "rollback revision mismatch")
    require(rollback.get("verifiedOnThisConsumerRevision") is False, "rollback must remain unverified")

    optics = adoption.get("opticalIntelligence", {})
    require(optics.get("localDeterministicResolver") is True, "local optical resolver boundary missing")
    require(optics.get("telemetryRequired") is False, "optics must not require telemetry")
    require(optics.get("cameraRequired") is False, "optics must not require camera data")
    require(optics.get("remoteContextRequired") is False, "optics must not require remote context")
    require(optics.get("environmentalMemoryInfluence") == 0.0, "App Store environmental memory must remain disabled")
    require(optics.get("maximumAllowedMemoryInfluence") == 0.08, "V1.4 memory influence cap mismatch")
    require(optics.get("catalogOrPackageStateMayDriveOptics") is False, "catalog/package state must not drive optics")
    require(optics.get("identityOrEntitlementStateMayDriveOptics") is False, "identity/entitlement state must not drive optics")
    require(optics.get("securityOrPrivacyStateMayDriveOptics") is False, "security/privacy state must not drive optics")
    require(optics.get("forcedColorsSolidAccessible") is True, "Forced Colors must collapse optics")
    require(optics.get("reducedTransparencySolidAccessible") is True, "Reduced Transparency must collapse optics")
    require(optics.get("increasedContrastSuppressesDecorativeTint") is True, "Increased Contrast must suppress decorative tint")

    acceptance = adoption.get("acceptance", {})
    require(acceptance.get("productionEligible") is False, "must not be production eligible")
    require(acceptance.get("conformanceAccepted") is False, "conformance must remain unaccepted")
    for key in (
        "renderedAcceptance",
        "supportedFormFactorAcceptance",
        "linuxRenderedAcceptance",
        "webRenderedAcceptance",
    ):
        require(acceptance.get(key) == "pending-v1.4-revalidation", f"{key} must require V1.4 revalidation")
    require(acceptance.get("humanVisualExcellence") == "deferred-v1.4.1", "human visual acceptance must remain deferred to V1.4.1")

    native = adoption.get("nativeMapping", {})
    require(native.get("systemShellScope") == "Application", "Android shell scope must remain Application")
    require(native.get("generalTargetFloorDp") == 48, "Android interaction floor mismatch")
    require(native.get("touchAssistanceTargetFloorDp") == 56, "Android Touch Assistance floor mismatch")
    require(native.get("neutralMaterial") is True, "Android neutral material mapping missing")
    require(native.get("adaptiveColorCarriesSemanticAuthority") is False, "Android adaptive color must not carry truth authority")
    require(native.get("environmentalSampling") is False, "Android environmental sampling must remain off")
    require(native.get("opticalPolicy") == "app/src/main/java/com/goreecloud/appstore/ui/GlazeOpticalV14.kt", "Android V1.4 optical policy path mismatch")

    linux = adoption.get("linuxMapping", {})
    require(linux.get("platform") == "Linux", "Linux mapping missing")
    require(linux.get("neutralHostMaterial") is True, "Linux neutral host material mapping missing")
    require(linux.get("adaptiveColorCarriesSemanticAuthority") is False, "Linux adaptive color must not carry truth authority")
    require(linux.get("environmentalSampling") is False, "Linux environmental sampling must remain off")

    web = adoption.get("webMapping", {})
    require(web.get("platform") == "Web", "Web mapping missing")
    require(web.get("systemShellScope") == "Application", "Web shell scope must remain Application")
    require(web.get("neutralMaterial") is True, "Web neutral material mapping missing")
    require(web.get("adaptiveColorCarriesSemanticAuthority") is False, "Web adaptive color must not carry truth authority")
    require(web.get("nestedBackdropBlur") is False, "Web nested backdrop blur must remain disabled")
    require(web.get("environmentalSampling") is False, "Web environmental sampling must remain off")
    require(web.get("externalRuntimeDependencies") is False, "Web mapping must not add third-party runtime dependencies")

    glaze = integrations.get("integrations", {}).get("glazeUi", {})
    require(glaze.get("target") == VERSION, "platform target mismatch")
    require(glaze.get("stableReleaseTag") == TAG, "platform release tag mismatch")
    require(glaze.get("stableReleaseRevision") == REVISION, "platform revision mismatch")
    require(glaze.get("sourceQualificationAnchor") == REVISION, "platform source pin mismatch")
    require(glaze.get("rollbackVersion") == ROLLBACK_VERSION, "platform rollback mismatch")
    require(glaze.get("materialRule") == MATERIAL_RULE, "platform material rule mismatch")
    require(glaze.get("opticalIntelligenceLocal") is True, "platform optical policy must remain local")
    require(glaze.get("environmentalMemoryInfluence") == 0.0, "platform environmental memory must remain disabled")
    require(integrations.get("productionAcceptance") is False, "productionAcceptance must remain false")

    web_glaze = web_contract.get("glazeUi", {})
    require(web_glaze.get("target") == VERSION, "Web distribution target mismatch")
    require(web_glaze.get("stableReleaseTag") == TAG, "Web distribution tag mismatch")
    require(web_glaze.get("stableReleaseRevision") == REVISION, "Web distribution revision mismatch")
    require(web_glaze.get("sourceQualificationAnchor") == REVISION, "Web distribution source pin mismatch")
    require(web_glaze.get("rollbackVersion") == ROLLBACK_VERSION, "Web rollback mismatch")
    require(web_glaze.get("materialRule") == MATERIAL_RULE, "Web material rule mismatch")
    require(web_glaze.get("environmentalSampling") is False, "Web environmental sampling must remain disabled")
    require(web_glaze.get("environmentalMemoryInfluence") == 0.0, "Web environmental memory must remain disabled")
    require(web_glaze.get("adaptiveColorCarriesSemanticAuthority") is False, "Web color must not become truth authority")
    require(web_contract.get("productionAcceptance") is False, "Web production acceptance must remain false")
    for key in (
        "renderedBrowser",
        "accessibilityTreeNames",
        "forcedColorsAutomation",
        "rtlStructuralResilience",
        "allViewports200PercentTextReflow",
    ):
        require(web_contract.get("acceptance", {}).get(key) == "pending-v1.4-revalidation", f"{key} must require V1.4 revalidation")

    gateways = read("app/src/main/java/com/goreecloud/appstore/platform/PlatformGateways.kt")
    for literal in (VERSION, TAG, REVISION, ROLLBACK_VERSION, MATERIAL_RULE, 'SYSTEM_SHELL_SCOPE = "Application"', "CONFORMANCE_ACCEPTED = false"):
        require(literal in gateways, f"PlatformGateways.kt missing {literal}")

    theme = read("app/src/main/java/com/goreecloud/appstore/ui/GlazeTheme.kt")
    for literal in ("GLAZE UI V1.4", VERSION, REVISION, "GlazeFoundation", "FrostWhite", "Pearl", "IceBlue", "DEEP_DARK"):
        require(literal in theme, f"Android V1.4 source mapping missing {literal}")

    optical = read("app/src/main/java/com/goreecloud/appstore/ui/GlazeOpticalV14.kt")
    for literal in (VERSION, REVISION, "MAX_MEMORY_INFLUENCE = 0.08f", "environmentalMemoryInfluence = 0f", "SOLID_ACCESSIBLE"):
        require(literal in optical, f"Android optical policy missing {literal}")
    for forbidden in ("catalog", "entitlement", "package trust", "telemetry", "camera input", "remote context"):
        require(forbidden in optical.lower(), f"Android optical privacy boundary missing {forbidden}")

    linux_styles = read("linux/resources/style.css")
    require("GLAZE UI V1.4" in linux_styles, "Linux V1.4 mapping marker missing")
    require("Environmental Color Memory/tint is disabled" in linux_styles, "Linux zero-memory optical boundary missing")
    require("goreecloud_deep_teal" not in linux_styles, "Linux Deep Teal substrate mapping must remain absent")
    require("@card_bg_color" in linux_styles, "Linux neutral host material mapping missing")

    web_styles = read("web/styles.css")
    for literal in (
        "GLAZE UI V1.4",
        "--glaze-optical-memory-influence: 0",
        "--glaze-optical-warmth: 0",
        "@media (prefers-reduced-transparency: reduce)",
        "@media (forced-colors: active)",
        "--target-min: 48px",
    ):
        require(literal in web_styles, f"Web V1.4 source mapping missing {literal}")
    require("--gc-deep-teal" not in web_styles, "historical Deep Teal substrate mapping must remain absent")
    require("backdrop-filter: blur(4px)" not in web_styles, "nested dialog backdrop blur must remain absent")

    gradle = read("app/build.gradle.kts")
    require(f'versionName = "{APP_VERSION}"' in gradle, "versionName mismatch")
    require(f"versionCode = {APP_VERSION_CODE}" in gradle, "versionCode mismatch")

    print(
        f"GLAZE UI V1.4 source mapping validated for Android + Linux + Web: "
        f"{VERSION} @ {REVISION}; environmental-memory=0; conformance=false production=false"
    )


if __name__ == "__main__":
    main()
