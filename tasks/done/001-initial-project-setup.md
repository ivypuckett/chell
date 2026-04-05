# Initial Android Launcher Project Setup

## Goal
Bootstrap the Chell Android launcher project with a buildable skeleton that registers as a HOME launcher.

## Acceptance Criteria
- Multi-module Gradle project compiles cleanly
- App registered as HOME launcher in AndroidManifest.xml
- `core` module contains domain model (AppInfo) and repository interface
- Unit tests in `core` pass on JVM without Android SDK: `./gradlew :core:test`

## Implementation Notes
- Package: `dev.chell.launcher`
- minSdk 26 (Android 8.0), targetSdk 35
- `core/` — pure Kotlin JVM module (no Android dependencies)
- `app/` — Android application module, depends on `core`
- MainActivity registered with HOME + DEFAULT + LAUNCHER categories
- No analytics, no permissions beyond what the launcher requires

## Status
Done
