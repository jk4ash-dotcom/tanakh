#!/usr/bin/env bash
# Shared paths and env for verify-tanakh helpers. Source this file; do not execute.

set -euo pipefail

_verify_tanakh_helpers_dir() {
  cd "$(dirname "${BASH_SOURCE[0]}")" && pwd
}

verify_tanakh_find_root() {
  if [[ -n "${VERIFY_TANAKH_ROOT:-}" && -f "$VERIFY_TANAKH_ROOT/settings.gradle.kts" ]]; then
    printf '%s\n' "$VERIFY_TANAKH_ROOT"
    return 0
  fi
  local dir
  dir="$(pwd)"
  while [[ "$dir" != "/" ]]; do
    if [[ -f "$dir/settings.gradle.kts" ]] && grep -q 'rootProject.name = "TanakhLearner"' "$dir/settings.gradle.kts"; then
      printf '%s\n' "$dir"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  # helpers/ -> verify-tanakh -> skills -> .cursor -> repo root
  local from_skill
  from_skill="$(cd "$(_verify_tanakh_helpers_dir)/../../../.." && pwd)"
  if [[ -f "$from_skill/settings.gradle.kts" ]] && grep -q 'TanakhLearner' "$from_skill/settings.gradle.kts"; then
    printf '%s\n' "$from_skill"
    return 0
  fi
  echo "verify-tanakh: cannot find TanakhLearner repo root (settings.gradle.kts)" >&2
  return 1
}

verify_tanakh_apply_env() {
  local root="$1"
  # README documents /workspace/.jdk/... and /workspace/android-sdk when the image ships them.
  if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ -d /workspace/.jdk/jdk-17.0.20.1+1 ]]; then
      export JAVA_HOME=/workspace/.jdk/jdk-17.0.20.1+1
    elif [[ -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
      export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    elif [[ -d /usr/lib/jvm/default-java ]]; then
      export JAVA_HOME=/usr/lib/jvm/default-java
    fi
  fi
  if [[ -z "${ANDROID_HOME:-}" ]]; then
    if [[ -d /workspace/android-sdk ]]; then
      export ANDROID_HOME=/workspace/android-sdk
    elif [[ -d "${HOME}/android-sdk" ]]; then
      export ANDROID_HOME="${HOME}/android-sdk"
    fi
  fi
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
    export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:${JAVA_HOME:-}/bin:$PATH"
  elif [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  if [[ -n "${ANDROID_HOME:-}" && ! -f "$root/local.properties" ]]; then
    printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$root/local.properties"
  fi
}

verify_tanakh_evidence_dir() {
  local root="$1"
  local run_id="${VERIFY_TANAKH_RUN_ID:-}"
  if [[ -z "$run_id" ]]; then
    run_id="$(date -u +%Y%m%dT%H%M%SZ)"
    export VERIFY_TANAKH_RUN_ID="$run_id"
  fi
  local base="${VERIFY_TANAKH_EVIDENCE_DIR:-$root/artifacts/verify-tanakh}"
  local dir="$base/$run_id"
  mkdir -p "$dir"
  printf '%s\n' "$dir"
}

verify_tanakh_state_dir() {
  local root="$1"
  local dir="${VERIFY_TANAKH_STATE_DIR:-$root/artifacts/verify-tanakh/.state}"
  mkdir -p "$dir"
  printf '%s\n' "$dir"
}

verify_tanakh_log() {
  local evidence="$1"
  shift
  local line
  line="[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*"
  printf '%s\n' "$line"
  printf '%s\n' "$line" >> "$evidence/session.log"
}
