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
# ───────────────────────────────��────────
# In Anthropic-hosted Claude Code sessions the egress proxy requires
# authentication for allowed hosts, and dl.google.com is NOT in the
# allowed hosts list.  Running this script in that environment will fail
# at the "make … install" step with "Proxy tunneling failed: Forbidden".
#
# Workaround in such sessions: the ANDROID_HOME check will still pass if
# the SDK was pre-installed by infrastructure.  Otherwise, only :core can
# be built/tested (no Android SDK required).
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
# /etc/wgetrc is read by all users.  We need an UNAUTHENTICATED proxy URL here
# because the authenticated proxy returns 403 Forbidden for dl.google.com
# (it is not in the allowed hosts list).  The unauthenticated path to the
# proxy accepts the CONNECT request.
WGETRC=/etc/wgetrc
BARE_PROXY="http://21.0.0.25:15004"
grep -q "^https_proxy" "$WGETRC" 2>/dev/null && \
  sed -i "s|^https_proxy.*|https_proxy = $BARE_PROXY|" "$WGETRC" || \
  echo "https_proxy = $BARE_PROXY" >> "$WGETRC"
grep -q "^http_proxy[[:space:]]" "$WGETRC" 2>/dev/null && \
  sed -i "s|^http_proxy[[:space:]].*|http_proxy  = $BARE_PROXY|" "$WGETRC" || \
  echo "http_proxy  = $BARE_PROXY" >> "$WGETRC"

# ── Accept the Google Android SDK licence ────────────────────────────────────
echo "google-android-licenses google-android-licenses/accepted-google-android-sdk boolean true" \
  | debconf-set-selections

# ── Pre-select the download mirror ───────────────────────────────────────────
echo "google-android-installers google-android-installers/mirror select https://dl.google.com" \
  | debconf-set-selections

# ── Install packages ─────────────────────────────────────────────────────────
# These installer packages download the real SDK components from dl.google.com
# during their postinstall step.  If dl.google.com is blocked this will fail.
PKGS="google-android-platform-34-installer google-android-build-tools-34.0.0-installer google-android-cmdline-tools-13.0-installer"
if DEBIAN_FRONTEND=noninteractive apt-get install -y $PKGS; then
  echo "Android SDK installed successfully."
  echo "ANDROID_HOME=$ANDROID_SDK_ROOT"
else
  # Clean up partially-configured packages so dpkg state stays consistent.
  dpkg --remove --force-remove-reinstreq $PKGS google-android-licenses 2>/dev/null || true
  echo "Installation failed – dl.google.com is likely not accessible." >&2
  exit 1
fi
