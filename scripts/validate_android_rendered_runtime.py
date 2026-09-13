#!/usr/bin/env python3
"""Bounded rendered/native Android evidence for GoreeCloud App Store Development.

This validator exercises the exact checked-out 0.1.5-dev APK on one deterministic
Android emulator across compact, dark, 200% text, and tablet-sized cases. Native
Compose instrumentation owns semantic click-target and category-filter acceptance,
including content that is intentionally below the launch viewport. Emulator evidence
is not TalkBack certification, physical-device acceptance, Human Visual
Excellence, production signing/distribution acceptance, or Stable qualification.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / ".artifacts" / "android-rendered"
PACKAGE = "com.goreecloud.appstore.dev"
ACTIVITY = f"{PACKAGE}/com.goreecloud.appstore.MainActivity"
EXPECTED_VERSION = "0.1.5-dev"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def run(*args: str, text: bool = True, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(args, check=check, text=text, capture_output=True)


def adb(serial: str, *args: str, text: bool = True, check: bool = True) -> subprocess.CompletedProcess:
    return run("adb", "-s", serial, *args, text=text, check=check)


def exact_source_revision() -> str:
    revision = run("git", "-C", str(ROOT), "rev-parse", "HEAD").stdout.strip()
    if not SHA40.fullmatch(revision):
        raise SystemExit(f"invalid checked-out revision: {revision!r}")
    expected = os.environ.get("APP_STORE_SOURCE_REVISION", "").strip()
    if expected and expected != revision:
        raise SystemExit(f"expected exact head {expected}, checked out {revision}")
    return revision


def serial_from_adb() -> str:
    explicit = os.environ.get("ANDROID_SERIAL", "").strip()
    if explicit:
        return explicit
    devices = []
    for line in run("adb", "devices").stdout.splitlines()[1:]:
        columns = line.split()
        if len(columns) >= 2 and columns[1] == "device":
            devices.append(columns[0])
    if len(devices) != 1:
        raise SystemExit(f"expected one ready emulator, found {devices}")
    return devices[0]


def installed_version(serial: str) -> str:
    output = adb(serial, "shell", "dumpsys", "package", PACKAGE).stdout
    match = re.search(r"\bversionName=([^\s]+)", output)
    if not match:
        raise SystemExit("could not resolve installed versionName")
    return match.group(1)


def configure(serial: str, width: int, height: int, font_scale: float, night: bool) -> None:
    adb(serial, "shell", "wm", "size", f"{width}x{height}")
    adb(serial, "shell", "wm", "density", "160")
    adb(serial, "shell", "settings", "put", "system", "font_scale", str(font_scale))
    adb(serial, "shell", "cmd", "uimode", "night", "yes" if night else "no")
    time.sleep(0.8)


def launch(serial: str) -> None:
    adb(serial, "shell", "am", "force-stop", PACKAGE)
    result = adb(serial, "shell", "am", "start", "-W", "-n", ACTIVITY).stdout
    if "Status: ok" not in result:
        raise SystemExit(f"activity launch failed:\n{result}")
    time.sleep(1.0)


def dump_ui(serial: str, filename: str) -> ET.Element:
    remote = "/sdcard/goreecloud-app-store-ui.xml"
    adb(serial, "shell", "uiautomator", "dump", remote)
    raw = adb(serial, "exec-out", "cat", remote).stdout
    (OUT / filename).write_text(raw, encoding="utf-8")
    return ET.fromstring(raw)


def node_text(node: ET.Element) -> str:
    return " ".join(
        value for value in (
            node.attrib.get("text", ""),
            node.attrib.get("content-desc", ""),
        ) if value
    )


def require_fragment(root: ET.Element, fragment: str) -> None:
    if not any(fragment in node_text(node) for node in root.iter("node")):
        raise SystemExit(f"required rendered fragment missing: {fragment!r}")


def bounds_for_text(root: ET.Element, text: str) -> tuple[int, int, int, int]:
    for node in root.iter("node"):
        if node.attrib.get("text") == text:
            match = BOUNDS.fullmatch(node.attrib.get("bounds", ""))
            if not match:
                break
            return tuple(map(int, match.groups()))
    raise SystemExit(f"rendered text bounds missing: {text!r}")


def overlaps(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> bool:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    return max(ax1, bx1) < min(ax2, bx2) and max(ay1, by1) < min(ay2, by2)


def assert_navigation_non_overlapping(root: ET.Element) -> None:
    labels = ["Discover", "Apps", "Services", "Updates", "Library"]
    rects = [(label, bounds_for_text(root, label)) for label in labels]
    for index, (left_name, left) in enumerate(rects):
        for right_name, right in rects[index + 1 :]:
            if overlaps(left, right):
                raise SystemExit(f"navigation labels overlap: {left_name!r}, {right_name!r}")


def screenshot(serial: str, filename: str) -> str:
    result = adb(serial, "exec-out", "screencap", "-p", text=False)
    payload = result.stdout
    if not payload.startswith(b"\x89PNG\r\n\x1a\n"):
        raise SystemExit(f"invalid screenshot payload for {filename}")
    path = OUT / filename
    path.write_bytes(payload)
    return hashlib.sha256(payload).hexdigest()


def rendered_case(
    serial: str,
    *,
    case_id: str,
    width: int,
    height: int,
    font_scale: float,
    night: bool,
    required_fragments: tuple[str, ...],
) -> dict[str, object]:
    configure(serial, width, height, font_scale, night)
    launch(serial)
    root = dump_ui(serial, f"{case_id}.xml")
    for fragment in ("GoreeCloud", "App Store", "Development", *required_fragments):
        require_fragment(root, fragment)
    assert_navigation_non_overlapping(root)
    digest = screenshot(serial, f"{case_id}.png")
    return {
        "id": case_id,
        "sizeDp": [width, height],
        "densityDpi": 160,
        "fontScale": font_scale,
        "nightMode": night,
        "navigationLabelsVisibleAndNonOverlapping": True,
        "requiredFragments": list(required_fragments),
        "screenshotSha256": digest,
    }


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    revision = exact_source_revision()
    serial = serial_from_adb()
    version = installed_version(serial)
    if version != EXPECTED_VERSION:
        raise SystemExit(f"installed version {version!r} != expected {EXPECTED_VERSION!r}")
    if os.environ.get("APP_STORE_COMPOSE_NAV_ACCEPTED", "").strip().lower() != "true":
        raise SystemExit("Compose-native navigation/category acceptance was not established")

    # Rendered cases intentionally assert only content guaranteed inside the initial viewport.
    # Category controls are lazy content and may remain below the launch viewport on both compact
    # and larger form factors. Their existence, 48dp semantics, horizontal scrolling, filtering,
    # and restoration to All are independently exercised by the Compose instrumentation gate.
    cases = [
        rendered_case(
            serial,
            case_id="phone-light-standard",
            width=390,
            height=844,
            font_scale=1.0,
            night=False,
            required_fragments=("Search apps and services",),
        ),
        rendered_case(
            serial,
            case_id="phone-dark-standard",
            width=390,
            height=844,
            font_scale=1.0,
            night=True,
            required_fragments=("Search apps and services",),
        ),
        rendered_case(
            serial,
            case_id="phone-large-text",
            width=390,
            height=844,
            font_scale=2.0,
            night=False,
            required_fragments=(),
        ),
        rendered_case(
            serial,
            case_id="tablet-light-standard",
            width=800,
            height=1280,
            font_scale=1.0,
            night=False,
            required_fragments=("Search apps and services",),
        ),
    ]

    evidence = {
        "schemaVersion": 1,
        "application": "goreecloud-app-store",
        "applicationVersion": EXPECTED_VERSION,
        "sourceRevision": revision,
        "package": PACKAGE,
        "instrumentation": {
            "accepted": True,
            "method": "androidx-compose-ui-test-junit4",
            "scope": "bottom navigation and category-filter semantic targets/behavior",
            "minimumTargetDp": 48,
        },
        "cases": cases,
        "boundaries": {
            "emulatorOnly": True,
            "physicalDeviceAccepted": False,
            "talkBackCertified": False,
            "humanVisualExcellenceAccepted": False,
            "glazeConformanceAccepted": False,
            "productionAcceptance": False,
            "stableEligible": False,
        },
    }
    output = OUT / "android-rendered-evidence.json"
    output.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")
    print(
        "Android rendered Development evidence validated: "
        f"revision={revision} version={EXPECTED_VERSION} cases={len(cases)} "
        "productionAcceptance=false"
    )


if __name__ == "__main__":
    main()
