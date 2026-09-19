#!/usr/bin/env bash
# Tear down instances this run created. Never kill by process name.
# Evidence under artifacts/verify-tanakh/<run-id>/ is kept.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

ROOT="$(verify_tanakh_find_root)"
verify_tanakh_apply_env "$ROOT"
EVIDENCE="$(verify_tanakh_evidence_dir "$ROOT")"
STATE="$(verify_tanakh_state_dir "$ROOT")"

verify_tanakh_log "$EVIDENCE" "cleanup start"

if [[ -f "$STATE/installed-by-us" && -f "$STATE/adb-serial" ]] && command -v adb >/dev/null 2>&1; then
  serial="$(cat "$STATE/adb-serial")"
  if [[ -n "$serial" ]]; then
    verify_tanakh_log "$EVIDENCE" "adb uninstall com.tanakhpoc.learner on $serial"
    adb -s "$serial" uninstall com.tanakhpoc.learner || true
  fi
  rm -f "$STATE/installed-by-us"
fi

if [[ -f "$STATE/emulator-pid" ]]; then
  pid="$(cat "$STATE/emulator-pid")"
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    verify_tanakh_log "$EVIDENCE" "stop emulator pid=$pid (started by this run)"
    kill "$pid" || true
  fi
  rm -f "$STATE/emulator-pid"
fi

if [[ -f "$STATE/gradle-daemon-started" ]]; then
  # Only stop the daemon if this run created local.properties scaffolding and asked for it.
  if [[ "${VERIFY_TANAKH_STOP_GRADLE:-}" == "1" ]]; then
    verify_tanakh_log "$EVIDENCE" "./gradlew --stop (VERIFY_TANAKH_STOP_GRADLE=1)"
    (cd "$ROOT" && ./gradlew --stop) || true
  fi
  rm -f "$STATE/gradle-daemon-started"
fi

# local.properties is gitignored and may have been written by apply_env.
# Leave it: removing it breaks the next Drive on this machine. Not session state.

rm -f "$STATE/launch-mode" "$STATE/adb-serial"

verify_tanakh_log "$EVIDENCE" "cleanup done; evidence kept at $EVIDENCE"
echo "CLEANUP ok — evidence remains at $EVIDENCE"
ls -la "$EVIDENCE"
