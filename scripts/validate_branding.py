#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "docs/BRANDING.md"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
UI = ROOT / "app/src/main/java/com/goreecloud/appstore/ui/AppStoreApp.kt"
CATALOG = ROOT / "app/src/main/assets/catalog/development-catalog.json"
DRAWABLE = ROOT / "app/src/main/res/drawable"

required_files = {
    "App Store launcher derivative": DRAWABLE / "goreecloud_app_store_icon.xml",
    "Identity Center service derivative": DRAWABLE / "goreecloud_identity_center_icon.xml",
    "Mesh Center service derivative": DRAWABLE / "goreecloud_mesh_center_icon.xml",
}

if not BRANDING.is_file():
    raise SystemExit("Missing mandatory docs/BRANDING.md")
for label, path in required_files.items():
    if not path.is_file():
        raise SystemExit(f"Missing {label}: {path.relative_to(ROOT)}")

branding = BRANDING.read_text(encoding="utf-8")
for required in [
    "GoreeCloud/goreecloud-branding-assets",
    "products/app-store/app-icon.svg",
    "1e86041de7cbde9f92ae2ddb9a813b2585b5f788",
    "services/identity-center/service-icon.svg",
    "36922e5a747817267a27f640bb4234b8d59ab2a5",
    "services/mesh-center/service-icon.svg",
    "2628ff825549847398e98d9768f8f57b30aa378a",
    "android:icon=\"@drawable/goreecloud_app_store_icon\"",
    "GoreeCloud Launcher",
    "GoreeCloud Search",
]:
    if required not in branding:
        raise SystemExit(f"docs/BRANDING.md missing required approved identity boundary: {required}")

manifest = MANIFEST.read_text(encoding="utf-8")
if 'android:icon="@drawable/goreecloud_app_store_icon"' not in manifest:
    raise SystemExit("App Store manifest is not wired to the approved launcher derivative")

ui = UI.read_text(encoding="utf-8")
expected_catalog_mappings = {
    "goreecloud.browser": "goreecloud_browser_icon",
    "goreecloud.messenger": "goreecloud_messenger_icon",
    "goreecloud.location": "goreecloud_location_icon",
    "goreecloud.manager": "goreecloud_manager_icon",
    "goreecloud.identity-center": "goreecloud_identity_center_icon",
    "goreecloud.mesh-center": "goreecloud_mesh_center_icon",
}

catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
catalog_ids = {item.get("id") for item in catalog.get("items", [])}
if catalog_ids != set(expected_catalog_mappings):
    raise SystemExit(
        "Development catalog identity set changed without an explicit branding mapping review: "
        f"expected {sorted(expected_catalog_mappings)}, got {sorted(catalog_ids)}"
    )
for item_id, drawable in expected_catalog_mappings.items():
    mapping = f'"{item_id}" -> R.drawable.{drawable}'
    if mapping not in ui:
        raise SystemExit(f"Missing approved catalog artwork mapping: {mapping}")
    if not (DRAWABLE / f"{drawable}.xml").is_file():
        raise SystemExit(f"Mapped catalog artwork resource is missing: {drawable}.xml")

if '"goreecloud.identity-center" -> R.drawable.goreecloud_identity_icon' in ui:
    raise SystemExit("Identity Center regressed to the full GoreeCloud Identity application icon")
if (DRAWABLE / "goreecloud_identity_icon.xml").exists():
    raise SystemExit("Obsolete full Identity application derivative remains in the App Store resource set")

app_store = required_files["App Store launcher derivative"].read_text(encoding="utf-8")
for token in [
    '#3B82F6', '#174EA6',
    'M18,23H46C48.76,23 51,25.24 51,28V45',
    'M24,23V19C24,14.58 27.58,11 32,11',
    'M32,30V40M27.5,35.5L32,40L36.5,35.5M24,45H40',
]:
    if token not in app_store:
        raise SystemExit(f"App Store derivative drifted from reviewed canonical geometry/color: {token}")

identity_center = required_files["Identity Center service derivative"].read_text(encoding="utf-8")
for token in [
    '#3B82F6', '#7C3AED',
    'M56,32A24,24',
    'M43,20V44',
]:
    if token not in identity_center:
        raise SystemExit(f"Identity Center derivative drifted from approved service identity: {token}")

mesh_center = required_files["Mesh Center service derivative"].read_text(encoding="utf-8")
for token in [
    '#0E7490', '#4338CA', '#7E22CE',
    'M58,32A26,26',
    'M14,32C20.5,21.5 28.5,20.5 32,24.5',
    'android:rotation="120"',
    'M36.5,32A4.5,4.5',
]:
    if token not in mesh_center:
        raise SystemExit(f"Mesh Center derivative drifted from approved Interlace service identity: {token}")

print("GoreeCloud App Store approved branding validation passed.")
