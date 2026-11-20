#!/usr/bin/env python3
"""Static invariant checks; not a substitute for compiling native clients."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
manifest = (root / "apps/android/app/src/main/AndroidManifest.xml").read_text()
assert "uses-permission" not in manifest, "Unexpected Android permission"
assert 'android:usesCleartextTraffic="false"' in manifest
android = (root / "apps/android/app/build.gradle.kts").read_text()
assert 'id("org.jetbrains.kotlin.plugin.compose")' in android
assert 'material3:1.5.0' in android
qt = (root / "apps/linux/CMakeLists.txt").read_text()
assert re.search(r"find_package\(Qt6 6\.12\.0 EXACT", qt)
assert 'qt_add_qml_module' in qt
print("Native source invariants passed (not compiled runtime acceptance)")
