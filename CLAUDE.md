# Chell – Agent Guide

## Quick start

Common commands are wrapped in `Taskfile.yml` (go-task). Run `task` to list
them. Note that this is unrelated to the `tasks/` directory, which is the
work-item backlog, not runnable commands.

```bash
task test      # unit tests
task build     # everything, lint included
task run       # install, set as home, show it
task emu       # start the emulator in a window
```

Or with Gradle directly:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Project layout

| Path | What it is |
|------|-----------|
| `app/` | The launcher – the only module |
| `app/src/main/kotlin/dev/chell/launcher/core/` | Domain logic, deliberately free of Android imports |
| `tasks/` | Work-item backlog (new → reviewed → done) |
| `scripts/` | Helper scripts |

## Toolchain versions

| Component | Version |
|-----------|---------|
| JDK | 21 |
| Gradle | 9.7.1 |
| Kotlin | 2.2.0 (AGP's built-in compiler – not chosen here) |
| Android Gradle Plugin | 9.3.2 |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 26 |

## Kotlin version

There is one module and no Kotlin Gradle plugin: AGP 9 brings its own Kotlin
compiler, which is **2.2.0**. Any Kotlin artifact added by hand must match it —
`kotlin-test-junit` is pinned to 2.2.0 in `app/build.gradle.kts` for exactly
this reason. A newer one fails the build with:

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.4.0, expected version is 2.2.0.
```

## Android SDK

Everything here needs the Android SDK; there is no SDK-free build path.

### Installing the SDK

```bash
bash scripts/setup-android-sdk.sh
```

**No root required.** The script drives Google's `sdkmanager` and installs into
`$HOME/Android/Sdk` by default (override with `ANDROID_SDK_ROOT`). It replaces
an earlier approach that installed Debian `google-android-*-installer` packages
as root; those cap out at platform 34 / build-tools 34.0.0, below this project's
`compileSdk`.

Afterwards:
```bash
export ANDROID_HOME="$HOME/Android/Sdk"
```

A `local.properties` with `sdk.dir` also works and is gitignored.

### Why `compileSdk` is 36, not 37

Google's repository index lists `platforms;android-37`, but no downloadable
platform package exists for it yet — `sdkmanager` reports
`Package platforms/android-37 not found`. Build-tools 37.0.0 *does* exist, which
makes 37 look available when it is not. 36 is the newest platform that installs.

This also caps `androidx.core:core-ktx` at **1.18.0**; 1.19.0 declares
`minCompileSdk=37` and fails `checkDebugAarMetadata`. To find the newest
compatible version of an AndroidX artifact, read `minCompileSdk` from its AAR:

```bash
unzip -p core-1.18.0.aar META-INF/com/android/build/gradle/aar-metadata.properties
```

### Network requirements

The setup script downloads from `dl.google.com`, and Gradle resolves AGP and
AndroidX from there too. Where that host is blocked (some sandboxed CI and
Anthropic-hosted sessions), only `:core` can be built.

The script's pre-flight check probes a **real file**
(`.../repository/repository2-3.xml`), not the directory
`https://dl.google.com/android/repository/`. That directory returns **404 in
every environment** because Google serves no directory listings — an earlier
version of this check used it and therefore reported the host as unreachable
*unconditionally*, even with full network access.

The session-start hook (`.claude/hooks/session-start.sh`) attempts the install
automatically; it exits 0 even on failure so the session still starts cleanly.

## Running tests

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew build                    # the above plus lint
```

Tests run on Robolectric, so they need no emulator — but they do need the
Android SDK. The tests under `dev.chell.launcher.core` are plain JVM tests
with no Robolectric runner, and stay that way: pure logic belongs in that
package, where a test costs nothing to write. `GridMetrics` lives there for
exactly that reason even though only the Android layer uses it.

### Running on an emulator

```bash
task setup:avd    # one-time: download the system image and create the AVD
task emu          # start it in a window (task emu:headless for no window)
task emu:wait     # block until it has booted
task run          # install, set as home, and show it
```

`task emu` uses `setsid` rather than a bare `&`: go-task kills its process
group when a task exits, which takes a plain background emulator down with it.

To check which launcher is currently default, press Home and read
`dumpsys activity activities` — `cmd package get-home-activities` does not
exist on this API level.

## Git workflow

**Commit directly to `main`. Do not create branches**, and do not open pull
requests for ordinary work — commit to `main` and push.

## Coding conventions

* Files must stay under 400 lines.
* Prefer standard algorithms, design patterns, and packages.
* Keep footprint low – add no feature beyond what is explicitly needed.
