#!/usr/bin/env bash
# Optional: install a local Android SDK so gradle test/assemble can run.
# Not required for doctor (source/catalog checks) or for reading this skill.
# Default prefix: /workspace/android-sdk (README) or $HOME/android-sdk.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

ROOT="$(verify_tanakh_find_root)"
PREFIX="${1:-${ANDROID_HOME:-/workspace/android-sdk}}"
ZIP_URL="${VERIFY_TANAKH_CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip}"

mkdir -p "$PREFIX"
if [[ ! -x "$PREFIX/cmdline-tools/latest/bin/sdkmanager" ]]; then
  tmp="$(mktemp -d)"
  echo "downloading commandlinetools to $tmp"
  curl -fsSL "$ZIP_URL" -o "$tmp/cmdline-tools.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  mkdir -p "$PREFIX/cmdline-tools"
  rm -rf "$PREFIX/cmdline-tools/latest"
  # zip root is cmdline-tools/
  mv "$tmp/cmdline-tools" "$PREFIX/cmdline-tools/latest"
  rm -rf "$tmp"
fi

export ANDROID_HOME="$PREFIX"
export ANDROID_SDK_ROOT="$PREFIX"
export PATH="$PREFIX/cmdline-tools/latest/bin:$PREFIX/platform-tools:$PATH"

yes | sdkmanager --sdk_root="$PREFIX" --licenses >/dev/null
sdkmanager --sdk_root="$PREFIX" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "platform-tools"

if [[ ! -f "$ROOT/local.properties" ]]; then
  printf 'sdk.dir=%s\n' "$PREFIX" > "$ROOT/local.properties"
fi

echo "ANDROID_HOME=$PREFIX"
echo "bootstrap-android-sdk: ready"
