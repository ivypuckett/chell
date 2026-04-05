#!/usr/bin/env bash
# Session-start hook for Claude Code on the web.
# Attempts to install the Android SDK so :app can be built.
# Exits 0 even on failure so the session still starts.

set -euo pipefail

# Only run in remote (web) sessions.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ANDROID_SDK_ROOT=/usr/lib/android-sdk
PLATFORM_DIR="$ANDROID_SDK_ROOT/platforms/android-34"
BUILD_TOOLS_DIR="$ANDROID_SDK_ROOT/build-tools/34.0.0"

if [ -d "$PLATFORM_DIR" ] && [ -d "$BUILD_TOOLS_DIR" ]; then
  echo "Android SDK already present."
  echo "export ANDROID_HOME=$ANDROID_SDK_ROOT" >> "${CLAUDE_ENV_FILE:-/dev/null}"
  exit 0
fi

echo "Attempting Android SDK installation…"
if sudo -E bash "${CLAUDE_PROJECT_DIR}/scripts/setup-android-sdk.sh"; then
  echo "export ANDROID_HOME=$ANDROID_SDK_ROOT" >> "${CLAUDE_ENV_FILE:-/dev/null}"
  echo "Android SDK installed. :app is now buildable."
else
  echo "Android SDK installation failed (dl.google.com may be blocked in this environment)."
  echo ":core is still fully testable without the SDK."
fi
