#!/usr/bin/env bash
# Prepare a verification instance.
#   launch.sh [--mode jvm|emulator]
# jvm (default): no long-lived process. Ready = doctor exit 0 and gradle wrapper present.
# emulator: assembleDebug, adb install, start MainActivity. Ready = UI dump contains "Tanakh Learner".

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

MODE="jvm"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="${2:-}"; shift 2 ;;
    --mode=*) MODE="${1#--mode=}"; shift ;;
    *) echo "usage: launch.sh [--mode jvm|emulator]" >&2; exit 2 ;;
  esac
done

ROOT="$(verify_tanakh_find_root)"
verify_tanakh_apply_env "$ROOT"
EVIDENCE="$(verify_tanakh_evidence_dir "$ROOT")"
STATE="$(verify_tanakh_state_dir "$ROOT")"

verify_tanakh_log "$EVIDENCE" "launch mode=$MODE root=$ROOT"

case "$MODE" in
  jvm)
    if ! "$SCRIPT_DIR/doctor.sh"; then
      echo "launch jvm: doctor failed" >&2
      exit 1
    fi
    verify_tanakh_log "$EVIDENCE" "READY jvm — no process to keep alive; Drive uses ./gradlew unit tests / APK asset tasks"
    printf '%s\n' "jvm" > "$STATE/launch-mode"
    echo "READY (jvm): run helpers/doctor.sh then helpers/drive.sh pack-sanity"
    ;;
  emulator)
    if ! command -v adb >/dev/null 2>&1; then
      echo "launch emulator: adb not found. Install platform-tools or attach a device. JVM Drive does not need this." >&2
      exit 2
    fi
    mapfile -t DEVS < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
    if [[ ${#DEVS[@]} -eq 0 ]]; then
      echo "launch emulator: no adb device. Start an AVD or plug in a device. Do not claim UI proof without this." >&2
      exit 2
    fi
    if [[ ${#DEVS[@]} -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
      echo "launch emulator: multiple devices and ANDROID_SERIAL unset — refuse to pick one (isolation)." >&2
      printf '%s\n' "${DEVS[@]}" >&2
      exit 1
    fi
    SERIAL="${ANDROID_SERIAL:-${DEVS[0]}}"
    export ANDROID_SERIAL="$SERIAL"
    if adb -s "$SERIAL" shell pm path com.tanakhpoc.learner >/dev/null 2>&1; then
      if [[ "${VERIFY_TANAKH_ALLOW_SHARED:-}" != "1" ]]; then
        echo "launch emulator: com.tanakhpoc.learner already installed on $SERIAL." >&2
        echo "Refuse to drive a shared user session. Uninstall it, use a fresh AVD, or set VERIFY_TANAKH_ALLOW_SHARED=1." >&2
        exit 1
      fi
      verify_tanakh_log "$EVIDENCE" "WARN shared package on $SERIAL (VERIFY_TANAKH_ALLOW_SHARED=1)"
    fi
    if [[ -z "${ANDROID_HOME:-}" ]]; then
      echo "launch emulator: ANDROID_HOME required for assembleDebug" >&2
      exit 1
    fi
    (
      cd "$ROOT"
      ./gradlew :app:assembleDebug --no-daemon
    ) | tee "$EVIDENCE/assemble-debug.log"
    APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
    if [[ ! -f "$APK" ]]; then
      echo "launch emulator: missing $APK" >&2
      exit 1
    fi
    adb -s "$SERIAL" install -r "$APK" | tee "$EVIDENCE/adb-install.log"
    printf '%s\n' "$SERIAL" > "$STATE/adb-serial"
    printf '%s\n' "1" > "$STATE/installed-by-us"
    adb -s "$SERIAL" logcat -c || true
    adb -s "$SERIAL" shell am start -n com.tanakhpoc.learner/.MainActivity
    verify_tanakh_log "$EVIDENCE" "started MainActivity on $SERIAL"
    ready=0
    for _ in $(seq 1 30); do
      sleep 2
      dump="$EVIDENCE/uidump-launch.xml"
      if adb -s "$SERIAL" shell uiautomator dump /sdcard/verify-tanakh-dump.xml >/dev/null 2>&1; then
        adb -s "$SERIAL" pull /sdcard/verify-tanakh-dump.xml "$dump" >/dev/null 2>&1 || true
      fi
      if [[ -f "$dump" ]] && grep -q 'Tanakh Learner' "$dump"; then
        if grep -q 'Could not load catalog' "$dump"; then
          echo "launch emulator: catalog failed to load" >&2
          grep -o 'text="[^"]*"' "$dump" | head >&2 || true
          exit 1
        fi
        ready=1
        break
      fi
    done
    adb -s "$SERIAL" logcat -d -t 200 > "$EVIDENCE/logcat-launch.txt" || true
    if [[ "$ready" -ne 1 ]]; then
      echo "launch emulator: timed out waiting for Home title 'Tanakh Learner' (catalog load may still be spinning: 'Loading Tanakh catalog…')" >&2
      exit 1
    fi
    printf '%s\n' "emulator" > "$STATE/launch-mode"
    verify_tanakh_log "$EVIDENCE" "READY emulator serial=$SERIAL title=Tanakh Learner"
    echo "READY (emulator): $SERIAL — Home shows Tanakh Learner"
    ;;
  *)
    echo "unknown --mode $MODE (jvm|emulator)" >&2
    exit 2
    ;;
esac
