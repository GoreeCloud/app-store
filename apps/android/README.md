# Native Android App Store

GoreeCloud App Store for Android is **Kotlin + Jetpack Compose / Material 3** source-only catalog browsing. It bundles `catalog/apps.json` and works offline. It has **no Internet permission** and does not ask to install packages or access device data. Opening a source repository is an explicit external browser action.

Versions follow the canonical GoreeCloud UI Android alpha build contract observed November 20, 2026: AGP 9.2.0, Kotlin Compose plugin 2.2.10, Compose UI/Foundation 1.13.0, Material 3 1.5.0, Android SDK 37.1 and JDK 17+. The Android theme is a source-pinned semantic token bridge, **not** direct reuse of the unpublished GoreeCloud Expressive adapter.

Open `apps/android` in Android Studio, provision the SDK, and run `./gradlew :app:assembleDebug`, or `bash scripts/build-android.sh` from the repository root. The Gradle wrapper JAR SHA-256 is `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, verified against the official Gradle 9.5.0 release record; the binary distribution SHA-256 is pinned in `gradle/wrapper/gradle-wrapper.properties`. Both are build tooling, not GoreeCloud original code.

**Not provided:** APK installation, update ownership, background downloads, package signature checking, F-Droid repo compatibility, publishing, signed APK distribution, real-device TalkBack acceptance or release authorization.
