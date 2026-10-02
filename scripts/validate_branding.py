#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "docs/BRANDING.md"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
UI = ROOT / "app/src/main/java/com/goreecloud/appstore/ui/AppStoreApp.kt"
CATALOG = ROOT / "app/src/main/assets/catalog/development-catalog.json"
PROVENANCE = ROOT / "app/src/main/assets/catalog/branding-provenance.json"
DRAWABLE = ROOT / "app/src/main/res/drawable"
GIT_BLOB = re.compile(r"^[0-9a-f]{40}$")

for required in (BRANDING, MANIFEST, UI, CATALOG, PROVENANCE):
    if not required.is_file():
        raise SystemExit(f"Missing mandatory branding input: {required.relative_to(ROOT)}")

manifest = MANIFEST.read_text(encoding="utf-8")
if 'android:icon="@drawable/goreecloud_app_store_icon"' not in manifest:
    raise SystemExit("App Store manifest is not wired to the official App Store icon derivative")

ui = UI.read_text(encoding="utf-8")
catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
provenance = json.loads(PROVENANCE.read_text(encoding="utf-8"))

if provenance.get("canonicalRepository") != "GoreeCloud/branding-assets":
    raise SystemExit("Branding provenance must use GoreeCloud/branding-assets")
if provenance.get("placeholderFallbackAllowed") is not False:
    raise SystemExit("Placeholder artwork must remain prohibited")
if not GIT_BLOB.fullmatch(str(provenance.get("sourceRevision", ""))):
    raise SystemExit("Branding provenance sourceRevision must pin an exact Git commit")

catalog_ids = {item.get("id") for item in catalog.get("items", [])}
records = provenance.get("items", [])
record_ids = {item.get("id") for item in records}
if catalog_ids != record_ids:
    raise SystemExit(
        "Every catalog item must have official branding provenance: "
        f"catalog={sorted(catalog_ids)}, branding={sorted(record_ids)}"
    )

if 'private fun StoreItem.artworkResource(): Int = when (id)' not in ui:
    raise SystemExit("Catalog artwork lookup must be total and non-null")
if 'else -> error("Missing official catalog artwork mapping for $id")' not in ui:
    raise SystemExit("Catalog artwork lookup must fail closed when a mapping is absent")

for forbidden in (
    'removePrefix("GoreeCloud ")',
    '.joinToString("") { it.take(1).uppercase() }',
    '"A" else "S"',
):
    if forbidden in ui:
        raise SystemExit(f"Prohibited placeholder/monogram artwork path remains in UI: {forbidden}")

for record in records:
    item_id = record.get("id")
    drawable = record.get("drawable")
    canonical = record.get("canonicalAsset")
    blob = str(record.get("gitBlob", ""))
    if not item_id or not drawable or not canonical:
        raise SystemExit(f"Incomplete branding provenance record: {record!r}")
    if not (canonical.startswith("products/") or canonical.startswith("services/")):
        raise SystemExit(f"Unsupported canonical branding path for {item_id}: {canonical}")
    if not GIT_BLOB.fullmatch(blob):
        raise SystemExit(f"Invalid canonical Git blob for {item_id}: {blob}")
    mapping = f'"{item_id}" -> R.drawable.{drawable}'
    if mapping not in ui:
        raise SystemExit(f"Missing official catalog artwork mapping: {mapping}")
    resource = DRAWABLE / f"{drawable}.xml"
    if not resource.is_file():
        raise SystemExit(f"Missing official Android branding derivative for {item_id}: {resource.relative_to(ROOT)}")
    contents = resource.read_text(encoding="utf-8")
    if "<vector" not in contents or "android:pathData=" not in contents:
        raise SystemExit(f"Invalid Android branding derivative for {item_id}: {resource.relative_to(ROOT)}")

if '"goreecloud.identity-center" -> R.drawable.goreecloud_identity_icon' in ui:
    raise SystemExit("Identity Center must use its approved service icon, not the parent Identity application icon")
if (DRAWABLE / "goreecloud_identity_icon.xml").exists():
    raise SystemExit("Obsolete full Identity application derivative remains in the App Store resource set")

branding = BRANDING.read_text(encoding="utf-8")
for required in (
    "GoreeCloud/branding-assets",
    "Official artwork is mandatory",
    "placeholders are prohibited",
    "ccfa74b3ffed12db285d32bcb5289821a1daf86e",
):
    if required not in branding:
        raise SystemExit(f"docs/BRANDING.md missing mandatory official-artwork contract: {required}")

print(f"GoreeCloud App Store branding validation passed: {len(records)} catalog items have official artwork and placeholders are prohibited.")
