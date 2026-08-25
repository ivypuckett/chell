# Chell – Agent Guide

## Quick start

```bash
# Pure JVM, no Android SDK needed (but see JDK note below):
./gradlew :core:test

# Requires Android SDK (see below):
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
```

## Project layout

| Path | What it is |
|------|-----------|
| `core/` | Pure Kotlin/JVM module – no Android deps |
| `app/` | Android launcher application |
| `tasks/` | Work-item backlog (new → reviewed → done) |
| `scripts/` | Helper scripts |

## Toolchain versions

| Component | Version |
|-----------|---------|
| JDK | 25 |
| Gradle | 9.7.1 |
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 9.3.2 |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 26 |

## JDK requirement

`core/build.gradle.kts` declares `jvmToolchain(25)`, so **a JDK 25 must be
installed — a JRE is not enough** (Gradle needs `javac`). Without one, even
`./gradlew :core:test` fails with:

```
Cannot find a Java installation on your machine ... matching: {languageVersion=25, ...}
```

Install it with:

```bash
sudo apt-get install -y openjdk-25-jdk-headless
```

## Android SDK

The `:app` module requires the Android SDK. The `:core` module does **not** —
its tests run on the JVM.

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

`:app` is enabled in `settings.gradle.kts` by default. A `local.properties`
with `sdk.dir` also works and is gitignored.

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
./gradlew :core:test          # JVM unit tests – needs a JDK 25 toolchain
./gradlew :app:testDebugUnitTest   # Robolectric tests (requires Android SDK)
./gradlew build               # all modules incl. lint (requires Android SDK)
```

`:app` tests run on Robolectric, so they need no emulator — but they do need
the Android SDK. Pure logic belongs in `core`, where tests are cheapest;
`GridMetrics` lives there for exactly that reason even though only `:app`
uses it.

### Running on an emulator

```bash
sdkmanager "emulator" "system-images;android-36;google_apis;x86_64"
avdmanager create avd -n chell-test -k "system-images;android-36;google_apis;x86_64" -d pixel_6
emulator -avd chell-test -no-window -no-audio -gpu swiftshader_indirect
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity dev.chell.launcher/.MainActivity
```

## Git workflow

**Commit directly to `main`. Do not create branches**, and do not open pull
requests for ordinary work — commit to `main` and push.

## Coding conventions

* Files must stay under 400 lines.
* Prefer standard algorithms, design patterns, and packages.
* Keep footprint low – add no feature beyond what is explicitly needed.
