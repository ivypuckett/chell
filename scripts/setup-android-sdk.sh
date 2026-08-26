#!/usr/bin/env bash
# Install the Android SDK components required to build :app.
#
# This is safe to run multiple times; it exits early if the SDK is already present.
#
# Requirements
# ────────────
# • Network access to dl.google.com (HTTPS).
# • No root required — the SDK is installed into a user-writable directory.
#
# Previously this script installed Debian "google-android-*-installer" packages
# as root.  Those packages cap out at platform 34 / build-tools 34.0.0, which is
# below the compileSdk this project targets, so it now drives Google's own
# sdkmanager instead.

set -euo pipefail

# NOTE: android-37 appears in Google's repository index but has no downloadable
# platform package yet; 36 is the newest platform that actually installs.
PLATFORM_VERSION="${PLATFORM_VERSION:-36}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-37.0.0}"
CMDLINE_TOOLS_BUILD="${CMDLINE_TOOLS_BUILD:-16111833}"

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
PLATFORM_DIR="$ANDROID_SDK_ROOT/platforms/android-$PLATFORM_VERSION"
BUILD_TOOLS_DIR="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

# ── Already installed? ────────────────────────────────────────────────────────
if [ -d "$PLATFORM_DIR" ] && [ -d "$BUILD_TOOLS_DIR" ]; then
  echo "Android SDK already installed at $ANDROID_SDK_ROOT"
  exit 0
fi

echo "Installing Android SDK (platform-$PLATFORM_VERSION, build-tools-$BUILD_TOOLS_VERSION)…"
echo "Target directory: $ANDROID_SDK_ROOT"

# ── A JDK is required to run sdkmanager ──────────────────────────────────────
if ! command -v java > /dev/null 2>&1; then
  echo "java not found on PATH; sdkmanager cannot run." >&2
  echo "Install a JDK first." >&2
  exit 1
fi

# ── Fetch command-line tools if needed ───────────────────────────────────────
if [ ! -x "$SDKMANAGER" ]; then
  ZIP_NAME="commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT

  echo "Downloading $ZIP_NAME…"
  curl -fsSL --max-time 300 \
    -o "$TMP_DIR/$ZIP_NAME" \
    "https://dl.google.com/android/repository/$ZIP_NAME"

  echo "Unpacking command-line tools…"
  # The archive expands to a top-level "cmdline-tools" directory; sdkmanager
  # requires it to live at <sdk>/cmdline-tools/latest.
  unzip -q "$TMP_DIR/$ZIP_NAME" -d "$TMP_DIR"
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$TMP_DIR/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
fi

# ── Install SDK components ───────────────────────────────────────────────────
echo "Accepting licences and installing components…"
yes 2>/dev/null | "$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" --licenses > /dev/null || true

"$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "platforms;android-$PLATFORM_VERSION" \
  "build-tools;$BUILD_TOOLS_VERSION"

# ── Verify: sdkmanager exits 0 even when a package is missing ────────────────
missing=""
[ -d "$PLATFORM_DIR" ]    || missing="$missing platforms;android-$PLATFORM_VERSION"
[ -d "$BUILD_TOOLS_DIR" ] || missing="$missing build-tools;$BUILD_TOOLS_VERSION"
if [ -n "$missing" ]; then
  echo "Installation incomplete; missing:$missing" >&2
  exit 1
fi

echo
echo "Android SDK installed successfully."
echo "Set the following in your environment:"
echo "  export ANDROID_HOME=$ANDROID_SDK_ROOT"
