#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_ICON_BLOB = "1e86041de7cbde9f92ae2ddb9a813b2585b5f788"
GLAZE_VERSION = "1.4.0"
GLAZE_TAG = "v1.4.0"
GLAZE_REVISION = "84cb3db4884042f0fa25ed6d475a127fb110f596"
GLAZE_SOURCE_ANCHOR = GLAZE_REVISION
GLAZE_ROLLBACK = "1.3.0"


def require(value: bool, message: str) -> None:
    if not value:
        raise SystemExit(f"Web Development validation failed: {message}")


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def git_blob_sha(path: str) -> str:
    data = (ROOT / path).read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


def main() -> None:
    contract = json.loads(read("contracts/web-distribution.json"))
    catalog = json.loads(read("catalog/development-catalog.json"))
    adoption = json.loads(read("contracts/glaze-ui-adoption.json"))

    require(contract["schemaVersion"] == 3, "contract schemaVersion mismatch")
    require(contract["application"] == "goreecloud-app-store", "application mismatch")
    require(contract["platform"] == "web", "platform mismatch")
    require(contract["lifecycle"] == "development", "lifecycle must remain development")
    require(contract["client"]["version"] == "0.1.0-dev", "web client version mismatch")
    require(contract["client"]["externalRuntimeDependencies"] is False, "external runtime dependencies must remain disabled")
    require(contract["catalog"]["sharedSource"] == "catalog/development-catalog.json", "web must use the shared catalog")
    require(contract["catalog"]["itemCount"] == 12, "contract item count mismatch")
    require(contract["catalog"]["authoritative"] is False, "Development catalog must remain non-authoritative")
    require(contract["security"]["analyticsEnabled"] is False, "analytics must remain disabled")
    require(contract["security"]["thirdPartyRuntimeCode"] is False, "third-party runtime code must remain disabled")
    require(contract["security"]["packageInstallationEnabled"] is False, "package installation must remain disabled")
    require(contract["security"]["serviceLaunchEnabled"] is False, "service launch must remain disabled")
    require(contract["productionAcceptance"] is False, "productionAcceptance must remain false")

    glaze = contract["glazeUi"]
    require(glaze["target"] == GLAZE_VERSION, "GLAZE target mismatch")
    require(glaze["stableReleaseTag"] == GLAZE_TAG, "GLAZE release tag mismatch")
    require(glaze["stableReleaseRevision"] == GLAZE_REVISION, "GLAZE release revision mismatch")
    require(glaze["sourceQualificationAnchor"] == GLAZE_SOURCE_ANCHOR, "GLAZE source pin mismatch")
    require(glaze["rollbackVersion"] == GLAZE_ROLLBACK, "GLAZE rollback version mismatch")
    require(glaze["systemShellScope"] == "Application", "GLAZE shell scope mismatch")
    require(glaze["environmentalSampling"] is False, "environmental sampling must remain disabled")
    require(glaze["environmentalMemoryInfluence"] == 0.0, "App Store environmental memory must remain disabled")
    require(glaze["adaptiveColorCarriesSemanticAuthority"] is False, "adaptive color must not carry semantic authority")
    require(glaze["reducedTransparencySolidAccessible"] is True, "Reduced Transparency must collapse trusted chrome to solid")
    require(glaze["forcedColorsSolidAccessible"] is True, "Forced Colors must collapse trusted chrome to solid")
    require(glaze["conformanceAccepted"] is False, "GLAZE conformance must remain unaccepted")

    acceptance = contract["acceptance"]
    for key in (
        "renderedBrowser",
        "accessibilityTreeNames",
        "forcedColorsAutomation",
        "rtlStructuralResilience",
        "allViewports200PercentTextReflow",
    ):
        require(acceptance[key] == "pending-v1.4-revalidation", f"{key} must require fresh V1.4 revalidation")
    require(acceptance["renderedBrowserEvidence"] is None, "prior rendered evidence must not transfer to V1.4")
    require(acceptance["localizationAcceptance"] == "pending", "RTL structure automation must not be represented as localization acceptance")
    require(acceptance["accessibilityAssistiveTechnology"] == "pending", "assistive-technology acceptance must not be inferred from browser automation")
    require(acceptance["crossBrowserAcceptance"] == "pending", "Chrome automation must not be represented as cross-browser acceptance")
    require(acceptance["humanVisualExcellence"] == "deferred-v1.4.1", "Human Visual Excellence must remain assigned to V1.4.1")
    require(acceptance["representativeTargetEnvironment"] == "pending", "representative Web target acceptance must remain pending")
    require(acceptance["productionHostingHeaders"] == "pending", "production hosting/header acceptance must remain pending")
    require(acceptance["rollbackAcceptance"] == "pending", "rollback acceptance must remain pending")

    require(catalog["schemaVersion"] == 2 and catalog["authoritative"] is False, "shared Development catalog mismatch")
    require(len(catalog["items"]) == 12, "reviewed Development catalog must contain 12 entries")
    require(contract["branding"]["canonicalBlob"] == EXPECTED_ICON_BLOB, "Web contract canonical App Store identity mismatch")
    require(git_blob_sha(contract["branding"]["repositorySource"]) == EXPECTED_ICON_BLOB, "approved App Store SVG provenance mismatch")

    html = read("web/index.html")
    styles = read("web/styles.css")
    app = read("web/app.mjs")
    entitlements = read("web/entitlements.mjs")
    rendered = read("web/rendered_acceptance.py")
    require("Content-Security-Policy" in html, "CSP source declaration missing")
    require("script-src 'self'" in html and "connect-src 'self'" in html, "CSP must keep runtime and catalog local")
    require("https://" not in html and "http://" not in html, "web entrypoint must not load external URLs")
    require("./catalog/development-catalog.json" in app, "client must load the built shared Development catalog")
    require("textContent" in app, "catalog presentation must use text-safe DOM assignment")
    require("visibleItems(state.catalog, state.identity)" in app, "entitlement filtering must precede discovery filtering")
    require("allowed.some" in entitlements, "explicit audience matching missing")
    require("canAccessReleaseChannel" in entitlements and "CHANNEL_CLAIMS" in entitlements, "explicit release-channel claim enforcement missing")
    require("--target-min: 48px" in styles, "48px interaction floor missing")
    require(":focus-visible" in styles, "keyboard focus styling missing")
    require("prefers-reduced-motion: reduce" in styles, "Reduced Motion mapping missing")
    require("prefers-reduced-transparency: reduce" in styles, "Reduced Transparency mapping missing")
    require("forced-colors: active" in styles, "Forced Colors mapping missing")
    require("--glaze-optical-memory-influence: 0" in styles, "App Store environmental memory must remain zero")
    require("--glaze-optical-warmth: 0" in styles, "App Store decorative warmth must remain zero")
    require(".topbar { position: static;" in styles, "compact topbar must remain non-sticky so navigation cannot be obscured after scrolling")
    require("backdrop-filter: blur(4px)" not in styles, "nested dialog backdrop blur must remain absent")
    require("forcedColorsAutomation" in rendered, "rendered browser report must retain Forced Colors evidence")
    require("rtlStructuralAutomation" in rendered, "rendered browser report must retain RTL structural evidence")
    require("allViewports200PercentTextReflow" in rendered, "rendered browser report must retain all-viewport 200% text evidence")
    require('"localizationAcceptance": False' in rendered, "rendered browser report must explicitly reject localization acceptance")
    require('"screenReaderAcceptance": False' in rendered, "rendered browser report must explicitly reject screen-reader acceptance")
    require('"crossBrowserAcceptance": False' in rendered, "rendered browser report must explicitly reject cross-browser acceptance")
    for literal in (
        "--gc-frost-white: #F7F9FC",
        "--gc-pearl: #EFF2F6",
        "--gc-ice-blue: #8DB5FF",
        "--gc-development-amber: #D9A35F",
        "--gc-deep-dark-canvas: #05070A",
    ):
        require(literal in styles, f"GLAZE V1.4 inherited source primitive missing: {literal}")
    require("--gc-deep-teal" not in styles, "historical Deep Teal substrate mapping must remain absent")

    web_mapping = adoption.get("webMapping", {})
    require(adoption.get("targetVersion") == GLAZE_VERSION, "GLAZE adoption version mismatch")
    require(adoption.get("stableReleaseRevision") == GLAZE_REVISION, "GLAZE adoption release revision mismatch")
    require(web_mapping.get("platform") == "Web", "GLAZE adoption web mapping missing")
    require(web_mapping.get("externalRuntimeDependencies") is False, "GLAZE web mapping must remain dependency-light")
    require(web_mapping.get("generalTargetFloorPx") == 48, "GLAZE web target floor mismatch")
    require(web_mapping.get("neutralMaterial") is True, "GLAZE V1.4 Web neutral material mapping missing")
    require(web_mapping.get("adaptiveColorCarriesSemanticAuthority") is False, "GLAZE V1.4 Web color authority boundary missing")
    print(
        "Web Development source contract validated: shared 12-item audience+release-channel entitlement-safe catalog, "
        "current canonical App Store identity, local runtime, GLAZE UI V1.4 source mapping, fresh rendered revalidation required, production=false"
    )


if __name__ == "__main__":
    main()
