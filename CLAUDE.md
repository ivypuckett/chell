# Chell – Agent Guide

## Quick start

```bash
# Always available (pure JVM, no Android SDK needed):
./gradlew :core:test

# Requires Android SDK (see below):
./gradlew :app:assembleDebug
```

## Project layout

| Path | What it is |
|------|-----------|
| `core/` | Pure Kotlin/JVM module – no Android deps |
| `app/` | Android launcher application |
| `tasks/` | Work-item backlog (new → reviewed → done) |
| `scripts/` | Helper scripts |

## Android SDK

The `:app` module requires the Android SDK (`compileSdk 34`, `minSdk 26`).
The `:core` module does **not** need it – tests run on the JVM.

### Installing the SDK

```bash
sudo -E bash scripts/setup-android-sdk.sh
```

After a successful install, set:
```bash
export ANDROID_HOME=/usr/lib/android-sdk
```

Then enable `:app` by editing `settings.gradle.kts`:
```kotlin
include(":core")
include(":app")   // remove the comment
```

### Network constraint in Anthropic-hosted sessions

The setup script downloads platform and build-tool ZIPs from `dl.google.com`.
In Anthropic cloud sessions, `dl.google.com` is **not** in the egress proxy's
allowed hosts list, so the download step fails with
`Proxy tunneling failed: Forbidden`.

**Consequence:** only `:core` can be built and tested in these sessions.
`:app` must be compiled locally or in a CI environment with unrestricted
internet access.

The session-start hook (`/.claude/hooks/session-start.sh`) attempts the
install automatically; it exits with code 0 even when the download fails so
the session still starts cleanly.

## Running tests

```bash
./gradlew :core:test          # JVM unit tests – always works
./gradlew test                # all modules (requires Android SDK for :app)
```

## Coding conventions

* Files must stay under 400 lines.
* Prefer standard algorithms, design patterns, and packages.
* Keep footprint low – add no feature beyond what is explicitly needed.
