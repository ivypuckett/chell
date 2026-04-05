#!/usr/bin/env bash
# Install the Android SDK components required to build :app.
#
# This is safe to run multiple times; it exits early if the SDK is already present.
#
# Requirements
# ────────────
# • Must be run as root (or via sudo -E to preserve the environment).
# • Network access to dl.google.com (HTTPS).
#
# Network note (Anthropic cloud sessions)
# ────────────────────────────────────────
# In Anthropic-hosted Claude Code sessions, dl.google.com is NOT in the
# egress proxy's allowed hosts list.  This script detects this early and
# exits with an error immediately rather than spending minutes on a doomed
# apt-get install.
#
# In local development or CI with unrestricted internet access this
# script works as-is.

set -euo pipefail

ANDROID_SDK_ROOT=/usr/lib/android-sdk
PLATFORM_DIR="$ANDROID_SDK_ROOT/platforms/android-34"
BUILD_TOOLS_DIR="$ANDROID_SDK_ROOT/build-tools/34.0.0"

# ── Already installed? ────────────────────────────────────────────────────────
if [ -d "$PLATFORM_DIR" ] && [ -d "$BUILD_TOOLS_DIR" ]; then
  echo "Android SDK already installed at $ANDROID_SDK_ROOT"
  export ANDROID_HOME="$ANDROID_SDK_ROOT"
  exit 0
fi

echo "Installing Android SDK (platform-34, build-tools-34.0.0)…"

# ── Pre-flight: check dl.google.com is reachable ──────────────────────────────
# The apt installer packages download SDK ZIPs from dl.google.com during their
# postinstall step.  If it is blocked, fail fast rather than waiting minutes for
# apt to install packages only to have the postinstall scripts time out.
echo "Checking connectivity to dl.google.com…"
if ! curl -fsS --max-time 10 \
       ${https_proxy:+--proxy "$https_proxy"} \
       -o /dev/null \
       -w "%{http_code}" \
       "https://dl.google.com/android/repository/" > /dev/null 2>&1; then
  echo "dl.google.com is not accessible from this environment." >&2
  echo "The Android SDK cannot be installed without access to dl.google.com." >&2
  echo "Options:" >&2
  echo "  • Build :app locally where dl.google.com is reachable." >&2
  echo "  • Run in a CI environment with unrestricted internet access." >&2
  echo "  • Only :core can be built/tested here (no Android SDK required)." >&2
  exit 1
fi

# ── Configure apt proxy ───────────────────────────────────────────────────────
# GLOBAL_AGENT_HTTP_PROXY is set by the Anthropic container runtime and contains
# the full authenticated proxy URL.  apt-get needs it explicitly.
if [ -n "${GLOBAL_AGENT_HTTP_PROXY:-}" ]; then
  APT_PROXY_CONF=/etc/apt/apt.conf.d/01egress-proxy
  cat > "$APT_PROXY_CONF" <<EOF
Acquire::http::Proxy "$GLOBAL_AGENT_HTTP_PROXY";
Acquire::https::Proxy "$GLOBAL_AGENT_HTTP_PROXY";
EOF
  trap 'rm -f "$APT_PROXY_CONF"' EXIT
fi

# ── Configure wget proxy (used by postinst as 'nobody' user) ─────────────────
# The postinst Makefile runs:
#   su nobody -s /bin/sh -c "wget --continue https://dl.google.com/..."
#
# Propagate the full authenticated proxy URL to /etc/wgetrc so the nobody user
# can authenticate.  We reached this point only after confirming dl.google.com
# is reachable, so credentials will be accepted.
WGETRC=/etc/wgetrc
if [ -n "${https_proxy:-}" ]; then
  WGET_PROXY="$https_proxy"
elif [ -n "${GLOBAL_AGENT_HTTP_PROXY:-}" ]; then
  WGET_PROXY="$GLOBAL_AGENT_HTTP_PROXY"
else
  WGET_PROXY=""
fi

if [ -n "$WGET_PROXY" ]; then
  grep -q "^https_proxy" "$WGETRC" 2>/dev/null && \
    sed -i "s|^https_proxy.*|https_proxy = $WGET_PROXY|" "$WGETRC" || \
    echo "https_proxy = $WGET_PROXY" >> "$WGETRC"
  grep -q "^http_proxy[[:space:]]" "$WGETRC" 2>/dev/null && \
    sed -i "s|^http_proxy[[:space:]].*|http_proxy  = $WGET_PROXY|" "$WGETRC" || \
    echo "http_proxy  = $WGET_PROXY" >> "$WGETRC"
fi

# ── Accept the Google Android SDK licence ────────────────────────────────────
echo "google-android-licenses google-android-licenses/accepted-google-android-sdk boolean true" \
  | debconf-set-selections

# ── Pre-select the download mirror ───────────────────────────────────────────
echo "google-android-installers google-android-installers/mirror select https://dl.google.com" \
  | debconf-set-selections

# ── Install packages ─────────────────────────────────────────────────────────
# These installer packages download the real SDK components from dl.google.com
# during their postinstall step.
PKGS="google-android-platform-34-installer google-android-build-tools-34.0.0-installer google-android-cmdline-tools-13.0-installer"
if DEBIAN_FRONTEND=noninteractive apt-get install -y $PKGS; then
  echo "Android SDK installed successfully."
  echo "ANDROID_HOME=$ANDROID_SDK_ROOT"
else
  # Clean up partially-configured packages so dpkg state stays consistent.
  dpkg --remove --force-remove-reinstreq $PKGS google-android-licenses 2>/dev/null || true
  echo "Installation failed." >&2
  exit 1
fi
