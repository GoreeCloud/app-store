# Native Linux client

GoreeCloud App Store for Linux is **Qt Quick 6.12.0** code, not an Electron/web wrapper.

It bundles a source-only snapshot of `catalog/apps.json` and requires **no API, installation privileges, telemetry, account or network access** for browsing. A source link opens the user's default browser by explicit click. It never runs shell commands, Flatpak, package managers, or arbitrary downloaded code.

## Build

```bash
cmake -S apps/linux -B build/linux -DCMAKE_BUILD_TYPE=Release
cmake --build build/linux
./build/linux/goreecloud-app-store
```

Requires Qt Quick + Qt Quick Controls 6.12.0, Qt tooling and a working graphical session. The QML material colors are read-only snapshots of semantic values from the pinned GoreeCloud UI source; re-synchronize and validate them against `GoreeCloud/gc-ui` when the shared native token SDK is finalized.

Run `python scripts/sync_catalog.py --check` to verify the bundled catalog matches the canonical manifest.

**Not yet established:** native packaging, physical Linux testing, screen-reader acceptance, cross-platform dark-mode parity, signed Flatpak remote or install/update control.
