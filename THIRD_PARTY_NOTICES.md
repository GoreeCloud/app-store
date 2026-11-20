# Dependencies, licenses and attribution

The application source in this repository is original GoreeCloud development work licensed under MIT. The repository does **not** vendor upstream framework implementations.

| Component | Usage | Upstream | License (upstream; recheck before distribution) |
| --- | --- | --- | --- |
| FastAPI | Read-only catalog REST API | https://github.com/fastapi/fastapi | MIT |
| Pydantic | Strict request/catalog validation | https://github.com/pydantic/pydantic | MIT |
| Uvicorn | Development ASGI server | https://github.com/encode/uvicorn | BSD-3-Clause |
| React and React DOM | Web storefront | https://github.com/facebook/react | MIT |
| Vite | Web development and build tool | https://github.com/vitejs/vite | MIT |
| GoreeCloud UI | Web CSS/design-system package (commit pinned in web package manifest) | https://github.com/GoreeCloud/gc-ui | MIT |
| Qt 6 Quick / Qt Quick Controls | Native Linux UI; separately installed SDK | https://www.qt.io | LGPL-3.0 / GPL / commercial options; comply with applicable linkage and distribution obligations |
| Kotlin / Jetpack Compose / AndroidX Material 3 | Native Android implementation | https://github.com/JetBrains/kotlin and https://android.googlesource.com/platform/frameworks/support | Apache-2.0 |

No code from F-Droid, Flatpak or TUF is imported in this phase; these are researched interoperability and future signed-distribution candidates. Do not describe them as implemented runtime dependencies. Their licenses, build provenance and API compatibility must be checked if adopted.
